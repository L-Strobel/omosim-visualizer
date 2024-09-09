#version 410

in vec2 position;
in vec3 color;
in vec2 offset;

out vec3 vertexColor;
out vec4 gl_Position;

uniform mat3 model;

vec3 pos2d;
void main () {
    vertexColor = color;
    pos2d = model * vec3(position, 1);
    pos2d.x += offset[0];
    pos2d.y += offset[1];
    gl_Position = vec4(pos2d.x, pos2d.y, 0, 1);
}