#version 150
in vec3 Position;
in vec4 Color;
uniform mat4 ModelViewMat, ProjMat;
uniform vec3 SpaceLens;
uniform float Background, DepthFar;
// GPU twin of dev.kolustar.sky.AtmosphereScattering.sky. The sky dome evaluates the single
// scattering integral per vertex here; fog, star extinction and remote limbs still call the CPU
// copy. Every constant below must match dev.kolustar.sky.AtmosphereSettings.
uniform float SkyScatter, SkyRadius, SkyDensity, SkyTransmission, SkyScale;
uniform vec3 SkyEye, SkySun, SkyTint;
out float spaceFacing;
out vec4 vertexColor;

#define SCATTER_VIEW_SAMPLES 6
#define SCATTER_SUN_SAMPLES 4
#define SCATTER_SCALE_HEIGHT 48.0
#define SCATTER_SHELL_HEIGHTS 6.0
#define SCATTER_RAYLEIGH 1.05
#define SCATTER_MIE 0.012
#define SCATTER_MIE_G 0.6
#define SCATTER_EXPOSURE 20.0
#define SCATTER_TINT_POWER 2.0
#define SCATTER_PI 3.141592653589793

// x/y are entry/exit distance along a unit ray, z is 1 when the sphere is hit.
vec3 sphereInterval(vec3 p, vec3 d, float r) {
    float b = dot(p, d);
    float pLength = length(p);
    // (|p|-r)(|p|+r) instead of dot(p,p)-r*r: no cancellation near the tangent shell.
    float disc = b * b - (pLength - r) * (pLength + r);
    if (disc < 0.0) return vec3(0.0);
    float root = sqrt(disc);
    return vec3(-b - root, -b + root, 1.0);
}

float atmosphereDensity(vec3 p) {
    return SkyDensity * exp(-max(0.0, length(p) - SkyRadius) / SCATTER_SCALE_HEIGHT);
}

float scatterChannel(float beta, float depth, float phaseR, float phaseM) {
    return exp(-(beta + SCATTER_MIE) * depth) * (beta * phaseR + SCATTER_MIE * phaseM);
}

float exposed(float x) { return 1.0 - exp(-SCATTER_EXPOSURE * x); }

float display(float x) {
    float v = clamp(x, 0.0, 1.0);
    return v <= 0.0031308 ? 12.92 * v : 1.055 * pow(v, 1.0 / 2.4) - 0.055;
}

vec3 scatterColor(vec3 direction) {
    vec3 ray = normalize(direction);
    vec3 sun = normalize(SkySun);
    float outer = SkyRadius + SCATTER_SCALE_HEIGHT * SCATTER_SHELL_HEIGHTS;
    vec3 segment = sphereInterval(SkyEye, ray, outer);
    if (segment.z < 0.5) return vec3(0.0);
    float start = max(0.0, segment.x);
    float end = segment.y;
    vec3 ground = sphereInterval(SkyEye, ray, SkyRadius);
    if (ground.z > 0.5 && ground.y > 1e-5) {
        if (ground.x > 1e-5) end = min(end, ground.x);
        else if (length(SkyEye) < SkyRadius - 1e-5) return vec3(0.0);
    }
    if (end <= start) return vec3(0.0);
    float ds = (end - start) / float(SCATTER_VIEW_SAMPLES);
    vec3 beta =
            vec3(pow(SkyTint.r, SCATTER_TINT_POWER), pow(SkyTint.g, SCATTER_TINT_POWER),
                 pow(SkyTint.b, SCATTER_TINT_POWER)) * SCATTER_RAYLEIGH;
    float mu = clamp(dot(ray, sun), -1.0, 1.0);
    float phaseR = 3.0 * (1.0 + mu * mu) / (16.0 * SCATTER_PI);
    float g = SCATTER_MIE_G;
    float phaseM = (1.0 - g * g) / (4.0 * SCATTER_PI * pow(1.0 + g * g - 2.0 * g * mu, 1.5));
    float viewDepth = 0.0;
    vec3 sum = vec3(0.0);
    for (int i = 0; i < SCATTER_VIEW_SAMPLES; i++) {
        vec3 p = SkyEye + ray * (start + (float(i) + 0.5) * ds);
        float local = atmosphereDensity(p) * ds / SCATTER_SCALE_HEIGHT;
        vec3 shadow = sphereInterval(p, sun, SkyRadius);
        if (shadow.z < 0.5 || shadow.y <= 1e-5) {
            vec3 exitShell = sphereInterval(p, sun, outer);
            float step = exitShell.z < 0.5
                    ? 0.0
                    : max(0.0, exitShell.y) / float(SCATTER_SUN_SAMPLES);
            float sunDepth = 0.0;
            for (int j = 0; j < SCATTER_SUN_SAMPLES; j++)
                sunDepth +=
                        atmosphereDensity(p + sun * ((float(j) + 0.5) * step)) * step
                                / SCATTER_SCALE_HEIGHT;
            float depth = viewDepth + local * 0.5 + sunDepth;
            sum +=
                    vec3(scatterChannel(beta.r, depth, phaseR, phaseM),
                                 scatterChannel(beta.g, depth, phaseR, phaseM),
                                 scatterChannel(beta.b, depth, phaseR, phaseM))
                            * local;
        }
        viewDepth += local;
    }
    vec3 encoded =
            vec3(exposed(sum.r * SkyTransmission), exposed(sum.g * SkyTransmission),
                 exposed(sum.b * SkyTransmission));
    return vec3(display(encoded.r), display(encoded.g), display(encoded.b));
}

vec4 spaceProject(vec4 view) {
    vec4 ordinary=ProjMat*view;
    spaceFacing=-view.z;
    if(SpaceLens.x<=0.0) return ordinary;
    float r=max(0.000001,length(view.xyz));
    // Bound the lens chart by its screen-corner ray, not by an arbitrary hemisphere.
    // Rear antipodal triangles must never wrap across the finite front viewport.
    vec2 corner=1.0/SpaceLens.yz;
    float rho2=dot(corner,corner);
    float cosine=(4.0-rho2)/(4.0+rho2);
    spaceFacing=-view.z-r*cosine;
    float d=max(r*(1.0+cosine),r-view.z);
    // Radial depth remains monotonic along each ray, including off-axis rays.
    vec4 stereo=vec4(2.0*SpaceLens.yz*view.xy*r/d,-ProjMat[2][2]*r+ProjMat[3][2],r);
    return mix(ordinary,stereo,SpaceLens.x);
}
void main() {
    gl_Position=spaceProject(ModelViewMat*vec4(Position,1));
    gl_Position.z=mix(gl_Position.z,gl_Position.w*(1.0-0.000001)*DepthFar,Background);
    // Position is the dome direction scaled by 128; the rim keeps its baked vertex colours.
    vertexColor=SkyScatter>0.5?vec4(scatterColor(Position)*SkyScale,1.0):Color;
}
