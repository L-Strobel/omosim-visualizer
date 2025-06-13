#version 400
in vec4 vertexColor;

out vec4 fragColor;

void main () {
    fragColor = vec4(vertexColor);
}