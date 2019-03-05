package org.cephalus.jogl;

import java.awt.*;
import java.awt.image.BufferedImage;

import static java.lang.Math.min;
import static org.junit.Assert.assertEquals;

public class ImageComparator {

    public static BufferedImage getDifferenceImage(BufferedImage img1, BufferedImage img2) {
        int width1 = img1.getWidth(); // Change - getWidth() and getHeight() for BufferedImage
        int width2 = img2.getWidth(); // take no arguments
        int height1 = img1.getHeight();
        int height2 = img2.getHeight();

        assertEquals("Different dimensions", new Dimension(width1, height1), new Dimension(width2, height2));

        BufferedImage outImg = new BufferedImage(width1, height1, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < height1; i++) {
            for (int j = 0; j < width1; j++) {
                int rgb1 = img1.getRGB(j, i);
                int rgb2 = img2.getRGB(j, i);
                int r1 = (rgb1 >> 16) & 0xff;
                int g1 = (rgb1 >> 8) & 0xff;
                int b1 = (rgb1) & 0xff;
                int r2 = (rgb2 >> 16) & 0xff;
                int g2 = (rgb2 >> 8) & 0xff;
                int b2 = (rgb2) & 0xff;

                int r = Math.abs(r1 - r2);
                int g = Math.abs(g1 - g2);
                int b = Math.abs(b1 - b2);

                int result = (r << 16) | (g << 8) | b;
                outImg.setRGB(j, i, result);
            }
        }

        return outImg;
    }

    public static float calculateDivergence(BufferedImage diff) {
        int width = diff.getWidth();
        int height = diff.getHeight();

        long all = 0xFF * width * height;
        long sum = 0;

        for(int y = 0; y < height; ++y) {
            for(int x = 0; x < width; ++x) {
                int rgb = diff.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = (rgb) & 0xff;
                sum += min(r + g + b, 0xFF);
            }
        }

        return sum / (float) all;
    }
}
