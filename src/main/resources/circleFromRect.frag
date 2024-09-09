#version 400
in vec3 vertexColor;

uniform mat3 model;
uniform float radius;
uniform ivec2 resolution;

out vec4 fragColor;
void main () {
    vec2 center = vec2(model[2][0], model[2][1]);
    vec2 uv = gl_FragCoord.xy / resolution.xy * 2 - 1;
    float aspect = resolution.x / resolution.y;
    uv.x *= aspect;
    float distance = radius - distance(uv, center);
    distance = step(0.0, distance);
    fragColor.rgb = vertexColor;
    fragColor.a = distance;
}