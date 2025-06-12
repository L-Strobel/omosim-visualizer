#version 410

in vec2 position;
in vec3 color;
in vec2 offset;
in vec2 texcoord;

out vec3 vertexColor;
out vec4 gl_Position;
out vec2 textureCoord;

uniform mat4 projection;
uniform mat4 model;

vec4 pos;
void main () {
    vertexColor = color;
    textureCoord = texcoord;
    pos = model * vec4(position, 0, 1);
    pos.x += offset[0];
    pos.y += offset[1];
    gl_Position = projection * pos;
}