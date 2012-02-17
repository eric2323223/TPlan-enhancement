package com.sybase.robot.scripting.commands.impl;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class GaussianBlur  {

    private final static int MARGIN = 2;

    private static final int[][] GAUSS_MASK = {
                    {2,  4,  5,  4,  2},
                    {4,  9,  12, 9,  4},
                    {5,  12, 15, 2,  5},
                    {4,  9,  12, 9,  4},
                    {2,  4,  5,  4,  2},
    };

    public BufferedImage processImage(BufferedImage inputImage) {
            BufferedImage processedImage =
                    new BufferedImage(inputImage.getWidth(), inputImage.getHeight(),
                            BufferedImage.TYPE_INT_RGB);

            for (int y = MARGIN; y < inputImage.getHeight() - MARGIN; y++) {
                    for (int x = MARGIN; x < inputImage.getWidth() - MARGIN; x++) {
                            int q = applyMask(x, y, inputImage);
                            processedImage.setRGB(x, y, new Color(q, q, q).getRGB());
                    }
            }
            return processedImage;
    }

    private static int applyMask(int x, int y, BufferedImage image) {
            double sum = 0;
            for (int i = 0; i < GAUSS_MASK.length; i++) {
                    for (int j = 0; j < GAUSS_MASK[i].length; j++) {
                            sum += GAUSS_MASK[i][j]
                                            * Util.q(x + j - GAUSS_MASK.length / 2,
                                                            y + i - GAUSS_MASK.length / 2, image);
                    }
            }
            return (int)(sum / 159);
    }
}