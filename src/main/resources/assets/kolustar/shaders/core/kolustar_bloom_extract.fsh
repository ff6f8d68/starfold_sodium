#version 150
uniform sampler2D Sampler0;
uniform float Threshold;
uniform float Knee;
in vec2 texCoord;
out vec4 fragColor;
void main(){
    vec3 color=texture(Sampler0,texCoord).rgb;
    float luminance=max(max(color.r,color.g),color.b);
    float weight=smoothstep(Threshold,Threshold+Knee,luminance);
    fragColor=vec4(color*weight,1.0);
}
