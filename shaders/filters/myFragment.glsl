//Example fragment shaders
//this shader will replace color that are brighter than a threshold with black
#version 330

in vec2 pass_textureCoords; //UVs
out vec4 fragColor;         //pixel color

uniform sampler2D tex;      //sampled texture (Name is important)
uniform int demo_int;
uniform int demo_bool;
uniform float demo_float;   //threshold
uniform vec3 demo_vec;
uniform mat3 demo_mat;

void main() {
    vec3 color = texture2D(tex, pass_textureCoords).rgb; //sample the texture at the passed UVs coords
    if (0.3 * color.r + 0.59 * color.g + 0.11 * color.b > demo_float)
    color = vec3(0);
    fragColor = vec4(color, 1.0);
}