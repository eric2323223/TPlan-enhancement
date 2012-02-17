package com.sybase.robot.scripting.commands.impl;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Util {

    /**
     * Maximum luminance.
     */
    public static final int L = 256;

    public static final int COLOR_WHITE = 255;
    public static final int COLOR_BLACK = 0;

    public static double componentY(int rgb) {
            Color color = new Color(rgb);
            return
            0.299 * color.getRed() +
            0.587 * color.getGreen() +
            0.114 * color.getBlue() +
            0.5;
    }

    public static int q(int x, int y, BufferedImage image) {
            Color color = new Color(image.getRGB(x, y));
            return (int)((color.getRed() + color.getGreen() + color.getBlue()) / 3.0);
    }
}
