#version 150
uniform sampler2D Sampler0;
uniform vec2 TexelSize;
uniform vec2 Direction;
in vec2 texCoord;
out vec4 fragColor;
const float WEIGHTS[5]=float[5](0.227027,0.194594,0.121621,0.054054,0.016216);
void main(){
    vec3 sum=texture(Sampler0,texCoord).rgb*WEIGHTS[0];
    for(int i=1;i<5;i++){
        vec2 offset=TexelSize*Direction*float(i)*1.5;
        sum+=texture(Sampler0,texCoord+offset).rgb*WEIGHTS[i];
        sum+=texture(Sampler0,texCoord-offset).rgb*WEIGHTS[i];
    }
    fragColor=vec4(sum,1.0);
}
