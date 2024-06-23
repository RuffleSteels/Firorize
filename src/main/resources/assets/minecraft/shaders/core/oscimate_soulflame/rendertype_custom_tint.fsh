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
        vec3 initialHSV = RGBtoHSV(vertexColor.rgb);
        float v = initialHSV.y * 100 - 50;
        float hue = initialHSV.x;
        float result;
        float brightness = RGBtoHSV(textureColor.rgb).z*100;
        float lightness = initialHSV.z;
        bool finalL = false;

        if (lightness <= 0.5) {
            float luminance = dot(textureColor.rgb, vec3(0.299, 0.587, 0.114));

            float adjustment = pow(luminance, 1-lightness*2);

            vec3 adjustedColor = textureColor.rgb * adjustment;

            brightness = (RGBtoHSV(adjustedColor.rgb).z) * 100;
        } else {
            lightness = 1 - (1-lightness)*2;
            finalL = true;
        }

//        if (lightness >= 0.5) {
//            lightness = (lightness-0.5)*2;
//
//            float luminance = dot(textureColor.rgb, vec3(0.299, 0.587, 0.114));
//
//            float adjustment = pow(luminance, 1-lightness);
//
//            vec3 adjustedColor = textureColor.rgb * adjustment;
//
//            brightness = (RGBtoHSV(adjustedColor.rgb).z) * 100;
//        }

//        if (saturation >= 0) {
            float h = 74.935+v/50*25.065;

            float g = ((100-h)/(100-(h/(v/50*0.46 + 1.54))));

            float y = (g*brightness) - (100*g) + 100;
            if (y <= h) {
                y = (v/50*0.46 + 1.54)*brightness;
            }

            float saturation = v+50;

            float z = sqrt(pow((-213.806), 2)-pow((192.676-saturation), 2)) - 92.6765;
            if (brightness > 50) {
                saturation = (-(z/50)) * brightness + 2*z;
            } else if (brightness == 100) {
                saturation = 0;
            } else {
                saturation = round(z);
            }


//            float z = v/100*69.8;
//            if (y < 3.4) {
//                result = (1-v/50) * (2.45*y) + 55 + v/4.95 + z;
//            } else if (y < 73.733 + v/50*27.28) {
//                float radiansVal = radians((360.0 / 5.49) * y + 100.0);
//                float sinVal = sin(radiansVal);
//                result = ((1-v/50)*(1/y)*sinVal)/0.1 + 65.1 + z + (1-v/50)*(y/130);
//            } else {
//                result = (y-100)/((25.44-(v/50)*25.43)/(0-(z+63.5)));
//            }

//        }
//        else {
//            saturation = saturation + 50;
//            if (brightness > 48.654) {
//                brightness = brightness/2 + 50.6;
//            } else {
//                brightness = brightness*1.54;
//            }
//
//            if (brightness > 74.56) {
//                result = (brightness-100)/((100-74.56)/(0-(saturation + 65.84)));
//            } else if (brightness > 72.719) {
//                result = pow((brightness-74.8)/(-0.3), (1/3.5)) + 64.9 + saturation;
//            } else if (brightness > 3.4) {
//                float brightnessInv = 1.0 / brightness;
//                float radiansVal = radians((360.0 / 5.49) * brightness + 100.0);
//                float sinVal = sin(radiansVal);
//                result = (brightnessInv * sinVal / 0.1) + 66.1 + (brightness / 130.0) + saturation;
//            } else {
//                result = 2.75*brightness + 55 + saturation;
//            }
//        }
        float thingy;
        if (!finalL) {
            thingy =  y/100;
        } else {
            thingy =  y/100 + (1-y/100)*lightness;
        }
        vec3 rrr = HSVtoRGB(vec3(hue,  saturation/100, thingy));


        vec4 final = vec4(rrr.r, rrr.g, rrr.b, textureColor.a);

        if (textureColor.a < 0.1) {
            discard;
        }

        fragColor = linear_fog(final, vertexDistance, FogStart, FogEnd, FogColor);
    }


}