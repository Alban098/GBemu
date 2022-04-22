//Example vertex shaders
//this shader will flip the UVs
#version 330

layout(location = 0) in vec2 position;  //the vertex coords

out vec2 pass_textureCoords;            //the UVs that are passed to the fragment

void main() {
   pass_textureCoords = position * 0.5 + 0.5;           //the UVs are calculated from the vertex coords
   gl_Position = vec4(position, 0, 1.0);
}