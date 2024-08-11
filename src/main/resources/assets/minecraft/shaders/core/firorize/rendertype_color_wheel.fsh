#version 150

uniform vec4 ColorModulator;
uniform float Value;

out vec4 fragColor;

in vec4 vertexColor;
in vec2 texCoord0;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec3 wheelColor = vec3(0.0);
    float dist = distance(texCoord0, vec2(0.5));
    if (dist <= 0.5) {
        float hue = atan(texCoord0.x - 0.5, texCoord0.y - 0.5) / (2 * 3.141592653589793) + 0.5;
        float saturation = dist * 2;
        wheelColor = hsv2rgb(vec3(hue, saturation, Value));
    } else {
        discard;
    }
    fragColor = vec4(wheelColor, 1.0) * ColorModulator;
}