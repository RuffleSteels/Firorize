#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 textureColor = texture(Sampler0, texCoord0);

    vec3 darkenedTexture1 = smoothstep(0, 1, vec3(textureColor.rgb));
    vec3 darkenedTexture2 = smoothstep(0.2, 1, vec3(darkenedTexture1.rgb));
    vec3 darkenedTextureRGB = smoothstep(0.2, 1, vec3(darkenedTexture2.rgb));


    vec4 color = vec4(darkenedTextureRGB + vertexColor.rgb - darkenedTextureRGB * vertexColor.rgb, textureColor.a * vertexColor.a);
    if (color.a < 0.1) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}