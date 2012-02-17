package com.sybase.robot.scripting.commands.impl;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class CannyEdgeDetection{

    public static final double T1 = 0.2 * Util.L;
    public static final double T2 = 0.4 * T1;

    private static final int[][] GX = {
                    {-1, 0, 1},
                    {-2, 0, 2},
                    {-1, 0, 1}
    };
    private static final int[][] GY = {
                    {1, 2, 1},
                    {0, 0, 0},
                    {-1, -2, -1},
    };

    private static final int MARGIN = 1;

    /**
     * Edge strength.
     */
    private int[][] pixelMagnitudes;
    private int[][] pixelEdgeDirections;

    public BufferedImage processImage(BufferedImage inputImage) {
//    	System.out.print("Image processing started... ");
            BufferedImage processedImage =
                    new GaussianBlur().processImage(inputImage);
            pixelMagnitudes = pixelMagnitudes(processedImage);
            pixelEdgeDirections = pixelEdgeDirections(processedImage);
            for (int y = MARGIN + 1; y < inputImage.getHeight() - MARGIN - 1; y++) {
                    for (int x = MARGIN + 1; x < inputImage.getWidth() - MARGIN - 1; x++) {
                            int q;
                            boolean isSuppressed =
                                    nonMaximumSuppression(x, y, processedImage);
                            boolean isEdge = hysteresis(x, y, processedImage);
                            if (isEdge && !isSuppressed) {
                                    q = Util.COLOR_BLACK;
                            } else {
                                    q = Util.COLOR_WHITE;
                            }
                            processedImage.setRGB(x, y, new Color(q, q, q).getRGB());
                    }
            }
//            System.out.println("finished");
            return processedImage;
    }

    private static int[][] pixelMagnitudes(BufferedImage image) {
            int[][]  pixelMagnitudes = new int[image.getHeight()][];
            for (int y = MARGIN; y < image.getHeight() - MARGIN; y++) {
                    pixelMagnitudes[y] = new int[image.getWidth()];
                    for (int x = MARGIN; x < image.getWidth() - MARGIN; x++) {
                            pixelMagnitudes[y][x] = pixelMagnitude(x, y, image);
                    }
            }
            return pixelMagnitudes;
    }

    private static int[][] pixelEdgeDirections(BufferedImage image) {
            int[][]  pixelEdgeDirections = new int[image.getHeight()][];
            for (int y = MARGIN; y < image.getHeight() - MARGIN; y++) {
                    pixelEdgeDirections[y] = new int[image.getWidth()];
                    for (int x = MARGIN; x < image.getWidth() - MARGIN; x++) {
                            pixelEdgeDirections[y][x] = pixelEdgeDirection(x, y, image);
                    }
            }
            return pixelEdgeDirections;
    }

    private static int pixelMagnitude(int x, int y, BufferedImage image) {
            int Gx = applyMask(GX, x, y, image);
            int Gy = applyMask(GY, x, y, image);
            int magnitude = Math.abs(Gx) + Math.abs(Gy);
            magnitude = Math.max(0, magnitude);
            magnitude = Math.min(Util.L - 1, magnitude);
            return magnitude;
    }

    private static int pixelEdgeDirection(int x, int y, BufferedImage image) {
            double Gx = applyMask(GX, x, y, image);
            double Gy = applyMask(GY, x, y, image);
            double theta;
            if (Gx != 0) {
                    theta = Math.toDegrees(Math.atan(Gy / Gx) + Math.PI / 2);
            } else if (Gy == 0) {
                    theta = 0;
            } else {
                    theta = 90;
            }
            if (theta < 0 || theta > 180) {
                    throw new AssertionError("oops...");
            }
            int direction;
            if (theta < 22.5 || theta > 157.5) {
                    direction = 0;
            } else if (theta < 67.5) {
                    direction = 45;
            } else if (theta < 112.5) {
                    direction = 90;
            } else {
                    direction = 135;
            }
            return direction;
    }

    private boolean nonMaximumSuppression(int x, int y, BufferedImage image) {
            int magnitude = pixelMagnitudes[y][x];
            int magnitudeNeighbor0;
            int magnitudeNeighbor1;
            switch (pixelEdgeDirections[y][x]) {
            case 0:
                    magnitudeNeighbor0 = pixelMagnitudes[y - 1][x];
                    magnitudeNeighbor1 = pixelMagnitudes[y + 1][x];
                    break;
            case 90:
                    magnitudeNeighbor0 = pixelMagnitudes[y][x - 1];
                    magnitudeNeighbor1 = pixelMagnitudes[y][x + 1];
                    break;
            case 135:
                    magnitudeNeighbor0 = pixelMagnitudes[y - 1][x + 1];
                    magnitudeNeighbor1 = pixelMagnitudes[y + 1][x - 1];
                    break;
            case 45:
                    magnitudeNeighbor0 = pixelMagnitudes[y - 1][x - 1];
                    magnitudeNeighbor1 = pixelMagnitudes[y + 1][x + 1];
                    break;
            default:
                    throw new AssertionError(
                                    "oops...");
            }
            boolean isSuppressed =
                    magnitude <= magnitudeNeighbor0 || magnitude <= magnitudeNeighbor1;
            return isSuppressed;
    }

    private boolean hysteresis(int x, int y, BufferedImage image) {
            boolean isEdge = false;
            if (pixelMagnitudes[y][x] > T1) {
                    isEdge = true;
            } else if (pixelMagnitudes[y][x] > T2) {
                    // is edge if a neighbor has magnitude > T1
                    int[] neighborsMagnitudes = neighborsMagnitudes(x, y);
                    for (int i = 0; i < neighborsMagnitudes.length; i++) {
                            if (neighborsMagnitudes[i] > T1) {
                                    isEdge = true;
                            }
                    }
            }
            return isEdge;
    }

    private int[] neighborsMagnitudes(int x, int y) {
            int[] neighbors = new int[] {
                            pixelMagnitudes[y - 1][x - 1],
                            pixelMagnitudes[y - 1][x],
                            pixelMagnitudes[y - 1][x + 1],
                            pixelMagnitudes[y][x - 1],
                            pixelMagnitudes[y][x + 1],
                            pixelMagnitudes[y + 1][x - 1],
                            pixelMagnitudes[y + 1][x],
                            pixelMagnitudes[y + 1][x + 1],
            };
            return neighbors;
    }

    private static int applyMask(int[][] mask, int x, int y, BufferedImage image) {
            int sum = 0;
            for (int i = 0; i < mask.length; i++) {
                    for (int j = 0; j < mask[i].length; j++) {
                            sum += mask[i][j]
                                            * Util.q(x + j - mask.length / 2,
                                                            y + i - mask.length / 2, image);
                    }
            }
            return sum;
    }
}
