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

float Epsilon = 1e-10;

vec3 RGBtoHCV(in vec3 RGB)
{
    vec4 P = (RGB.g < RGB.b) ? vec4(RGB.bg, -1.0, 2.0/3.0) : vec4(RGB.gb, 0.0, -1.0/3.0);
    vec4 Q = (RGB.r < P.x) ? vec4(P.xyw, RGB.r) : vec4(RGB.r, P.yzx);
    float C = Q.x - min(Q.w, Q.y);
    float H = abs((Q.w - Q.y) / (6 * C + Epsilon) + Q.z);
    return vec3(H, C, Q.x);
}


vec3 RGBtoHSV(in vec3 RGB)
{
    vec3 HCV = RGBtoHCV(RGB);
    float S = HCV.y / (HCV.z + Epsilon);
    return vec3(HCV.x, S, HCV.z);
}

vec3 HUEtoRGB(in float H)
{
    float R = abs(H * 6 - 3) - 1;
    float G = 2 - abs(H * 6 - 2);
    float B = 2 - abs(H * 6 - 4);
    return clamp(vec3(R,G,B), 0.0, 1.0);
}

vec3 HSVtoRGB(in vec3 HSV)
{
    vec3 RGB = HUEtoRGB(HSV.x);
    return ((RGB - 1) * HSV.y + 1) * HSV.z;
}

void main() {
    vec4 textureColor = texture(Sampler0, texCoord0);

    if (vertexColor.rgb == vec3(0, 0, 0)) {
        if (textureColor.a < 0.1) {
            discard;
        }
        fragColor = linear_fog(textureColor, vertexDistance, FogStart, FogEnd, FogColor);
    } else {
        float saturation = 25;
        float hue = RGBtoHSV(vertexColor.rgb).x;
        float result;

        float brightness = RGBtoHSV(textureColor.rgb).z * 100;

        if (brightness > 48.654) {
            brightness = brightness/2 + 50.6;
        } else {
            brightness = brightness*1.54;
        }

        if (brightness > 74.56) {
            result = (brightness-100)/((100-74.56)/(0-(saturation + 65.84)));
        } else if (brightness > 72.719) {
            result = pow((brightness-74.8)/(-0.3), (1/3.5)) + 64.9 + saturation;
        } else if (brightness > 3.4) {
            float brightnessInv = 1.0 / brightness;
            float radiansVal = radians((360.0 / 5.49) * brightness + 100.0);
            float sinVal = sin(radiansVal);
            result = (brightnessInv * sinVal / 0.1) + 66.1 + (brightness / 130.0) + saturation;
        } else {
            result = 2.75*brightness + 55 + saturation;
        }

        vec3 rrr = HSVtoRGB(vec3(hue, result/100, brightness/100));

        vec4 final = vec4(rrr.r, rrr.g, rrr.b, textureColor.a);

        if (textureColor.a < 0.1) {
            discard;
        }

        fragColor = linear_fog(final, vertexDistance, FogStart, FogEnd, FogColor);
    }


}