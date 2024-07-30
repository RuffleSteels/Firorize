package com.oscimate.oscimate_soulflame;

import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ColorizeMath {
    public static void create(int[] colors) {
        try {
            for (int i = 0; i < 2; i++) {

                Path path = MinecraftClient.getInstance().runDirectory.toPath().toAbsolutePath();
                Path path2 = TextureUtil.getDebugTexturePath(path);

                BufferedImage inputImage = ImageIO.read(new File((Paths.get("").toAbsolutePath().toString() + "/config/oscimate_soulflame/blank_fire_"+i+".png")));

                Color c = new Color(colors[0]);
                float[] vertexColor = {c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f,1f};
                float fogStart = 0.5f;
                float fogEnd = 10.0f;
                float[] fogColor = {0.5f, 0.5f, 0.5f, 1.0f};

                BufferedImage outputImage = processImage(inputImage, vertexColor, fogStart, fogEnd, fogColor);

                BufferedImage iinputImage = ImageIO.read(new File((Paths.get("").toAbsolutePath().toString() + "/config/oscimate_soulflame/blank_fire_overlay_"+i+".png")));

                Color cc = new Color(colors[1]);
                float[] vvertexColor = {cc.getRed()/255f, cc.getGreen()/255f, cc.getBlue()/255f,1f};

                BufferedImage ooutputImage = processImage(iinputImage, vvertexColor, fogStart, fogEnd, fogColor);

                BufferedImage img1 = outputImage;
                BufferedImage img2 = ooutputImage;

                int width = Math.max(img1.getWidth(), img2.getWidth());
                int height = Math.max(img1.getHeight(), img2.getHeight());

                BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

                Graphics2D g = combined.createGraphics();

                g.drawImage(img1, 0, 0, null);

                g.drawImage(img2, 0, 0, null);

                g.dispose();
                TextureUtil.writeAsPNG(path, new Identifier("oscimate_soulflame:fires/fire"+i+"_"+ Math.abs(colors[0]) + "_" + Math.abs(colors[1])).toUnderscoreSeparatedString(), 0,1, inputImage.getWidth(), inputImage.getHeight());
//                ImageIO.write(combined, "png", new File(Paths.get("").toAbsolutePath().toString() + "/config/oscimate_soulflame/fires/fire"+i+"_"+colors[0] + "_" + colors[1] +".png"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage processImage(BufferedImage inputImage, float[] vertexColor, float fogStart, float fogEnd, float[] fogColor) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = inputImage.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;

                float[] textureColor = {red / 255.0f, green / 255.0f, blue / 255.0f, alpha / 255.0f};

                float[] finalColor = applyColorization(textureColor, vertexColor);

                int finalARGB = ((int)(finalColor[3] * 255) << 24) |
                        ((int)(finalColor[0] * 255) << 16) |
                        ((int)(finalColor[1] * 255) << 8) |
                        ((int)(finalColor[2] * 255));
                outputImage.setRGB(x, y, finalARGB);
            }
        }

        return outputImage;
    }

    public static int[] convert(float[] floatArray) {
        int[] intArray = new int[floatArray.length];
        for (int i = 0; i < floatArray.length; i++) {
            intArray[i] = Math.round(floatArray[i] * 255);
        }
        return intArray;
    }
    public static float[] applyColorization(float[] textureColor, float[] vertexColor) {
        float[] initialHSV = RGBtoHSV(vertexColor);

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
            return new float[] {0, 0, 0, 0}; // discard
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
