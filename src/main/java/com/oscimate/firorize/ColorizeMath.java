package com.oscimate.firorize;

public class ColorizeMath {
    public static float[] applyColorization(float[] textureColor, float[] vertexColor) {
        float[] initialHSV = RGBtoHSV(vertexColor);

        if (initialHSV[1] == 1.0F) {
            initialHSV[1] = 0.99F;
        }

        float v = initialHSV[1] * 100 - 50;
        float hue = initialHSV[0];
        float brightness = RGBtoHSV(textureColor)[2] * 100;
        float lightness = initialHSV[2];
        boolean finalL = false;

        if (lightness <= 0.5) {
            float luminance = dot(textureColor, new float[] {0.299f, 0.587f, 0.114f});
            float adjustment = (float) Math.pow(luminance, 1 - lightness * 2);
            float[] adjustedColor = multiply(textureColor, adjustment);
            brightness = RGBtoHSV(adjustedColor)[2] * 100;
        } else {
            lightness = 1 - (1 - lightness) * 2;
            finalL = true;
        }

        float h = 74.935f + v / 50 * 25.065f;
        float g = ((100 - h) / (100 - (h / (v / 50 * 0.46f + 1.54f))));
        float y = (g * brightness) - (100 * g) + 100;
        if (y <= h) {
            y = (v / 50 * 0.46f + 1.54f) * brightness;
        }

        float saturation = v + 50;
        float z = (float) Math.sqrt(Math.pow(-213.806, 2) - Math.pow(192.676 - saturation, 2)) - 92.6765f;
        if (brightness > 50) {
            saturation = (-(z / 50)) * brightness + 2 * z;
        } else if (brightness == 100) {
            saturation = 0;
        } else {
            saturation = Math.round(z);
        }

        float thingy;
        if (!finalL) {
            thingy = y / 100;
        } else {
            thingy = y / 100 + (1 - y / 100) * lightness;
        }

        if (vertexColor[0] == vertexColor[1] && vertexColor[1] == vertexColor[2]) {
            hue = 0;
        }

        float[] rrr = HSVtoRGB(new float[] {hue, saturation / 100, thingy});

        float[] finalColor = {rrr[0], rrr[1], rrr[2], textureColor[3]};
        if (textureColor[3] < 0.1) {
            return new float[] {0, 0, 0, 0};
        }

        return finalColor;
    }

    private static float[] RGBtoHSV(float[] rgb) {
        float epsilon = 1e-10f;
        float max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        float min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        float delta = max - min;
        float h = 0, s, v = max;

        if (max != 0) {
            s = delta / max;
        } else {
            s = 0;
            h = -1;
            return new float[] {h, s, v};
        }

        if (rgb[0] == max) {
            h = (rgb[1] - rgb[2]) / delta;
        } else if (rgb[1] == max) {
            h = 2 + (rgb[2] - rgb[0]) / delta;
        } else {
            h = 4 + (rgb[0] - rgb[1]) / delta;
        }

        h *= 60;
        if (h < 0) h += 360;

        return new float[] {h / 360, s, v};
    }

    private static float[] HSVtoRGB(float[] hsv) {
        float h = hsv[0] * 360;
        float s = hsv[1];
        float v = hsv[2];

        int i = (int) Math.floor(h / 60) % 6;
        float f = h / 60 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        switch (i) {
            case 0: return new float[] {v, t, p};
            case 1: return new float[] {q, v, p};
            case 2: return new float[] {p, v, t};
            case 3: return new float[] {p, q, v};
            case 4: return new float[] {t, p, v};
            default: return new float[] {v, p, q};
        }
    }

    private static float dot(float[] vec1, float[] vec2) {
        return vec1[0] * vec2[0] + vec1[1] * vec2[1] + vec1[2] * vec2[2];
    }

    private static float[] multiply(float[] vec, float scalar) {
        return new float[] {vec[0] * scalar, vec[1] * scalar, vec[2] * scalar, vec[3]};
    }

}
