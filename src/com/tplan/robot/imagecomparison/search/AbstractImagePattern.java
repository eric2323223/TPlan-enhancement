/*
 * T-Plan Robot, automated testing tool based on remote desktop technologies.
 * Copyright (C) 2009  T-Plan Limited (http://www.t-plan.co.uk),
 * Tolvaddon Energy Park, Cornwall, TR14 0HX, United Kingdom
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.tplan.robot.imagecomparison.search;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.util.Vector;

/**
 * Abstract image pattern. It is a wrapper which encapsulates pixels of a
 * template image and implements necessary infrastructure allowing to iterate
 * a comparator over locations in a base image.
 * @product.signature
 */
public abstract class AbstractImagePattern {

    /** Image pattern pixels */
    protected int[] pixels;

    /** Pattern rectangle */
    protected Rectangle r;

    protected boolean stop;

    protected int pxcnt = 0;

    protected int numberOfNonAlphaPixels = 0;


    public void stop() {
        stop = true;
    }

    public boolean isStopped() {
        return stop;
    }

    /** A string identifying pattern type */
    public abstract String getType();

    /**
     * Find out if the pattern matches with source pixels at a specified offset.
     *
     * @param source source pixels (usually pixels of a certain area of the remote desktop image)
     * @param offset from which pixel array index to compare
     * @param sourceRect rectangle identifying the area from to which the source pixels belong to
     *
     * @return true if the pixels at the specified offset match the pattern, false otherwise
     */
    public abstract boolean matches(int source[], int offset, Rectangle sourceRect, float passRate);


// -- LOAD AND CONVERSION METHODS -------------------------------------------------

    /**
     * Set the pixels of the pattern directly.
     * @param pixels pattern pixels
     * @param r pattern rectangle
     */
    public void setPixels(int[] pixels, Rectangle r) {
        this.pixels = pixels;
        this.r = new Rectangle(r);
        numberOfNonAlphaPixels = r.width * r.height;
    }

    /**
     * Get the pattern pixels.
     */
    public int[] getPixels() {
        return pixels;
    }

    /**
     * Get the pattern rectangle.
     */
    public Rectangle getRectangle() {
        return r;
    }

    /**
     * Load the pattern pixels from a certain rectangle of an image.
     * @param img an image to load the pixels from.
     * @param r a rectangle to load the pixels from. The rectangle must not
     * be bigger than the image size.
     * @return true if the image contains transparency, false otherwise.
     */
    public boolean setPixelsFromImage(Image img, Rectangle r) {
        int width = r.width;
        int height = r.height;
        int ai[]; // = new int[width * height];
        numberOfNonAlphaPixels = width * height;
        boolean isTransparent = false;

        if (img instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage)img;
            ai = ((BufferedImage) img).getRGB(r.x, r.y, width, height, (int[])null, 0, width);
            isTransparent = bi.getColorModel().hasAlpha();

        } else {
            ai = new int[width * height];
            PixelGrabber pixelgrabber =
                    new PixelGrabber(img, r.x, r.y, width, height, ai, 0, width);
            try {
                pixelgrabber.grabPixels();
            } catch (InterruptedException interruptedexception) {
                interruptedexception.printStackTrace();
            }
            isTransparent = pixelgrabber.getColorModel().hasAlpha();
        }
        this.pixels = ai;
        this.r = r;
        return isTransparent;
    }

    /**
     * Convert the pattern to a customary String format. It is not currently
     * used and this method is reserved for future use.
     */
    public String getStampAsString() {
        StringBuffer s = new StringBuffer();
        if (pixels != null) {
            s.append(Integer.toString(r.width, 16)+":");
            int count = pixels.length - 1;
            for (int i = 0; i <= count; i++) {
                s.append(Integer.toString(pixels[i], 16));
                if (i < count) {
                    s.append(":");
                }
            }
        }
        return s.toString();
    }

    /**
     * Load pattern pixels from a string.
     * This method is not currently used and it is reserved for future use.
     */
    public void parseStampFromString(String s) {
        // TODO: validate stamp
        String tokens[] = s.split(":");
        int count = tokens.length-1;
        int width = Integer.parseInt(tokens[0], 16);
        int height = count / width;
        this.r = new Rectangle(width, height);
        int[] stamp = new int[count];
        for (int i=0; i<count; i++) {
            stamp[i] = Integer.parseInt(tokens[i+1], 16);
        }
        pixels = stamp;
    }

// -- COORDINATE METHODS ------------------------------------------------------

    protected int getDx(int index, Rectangle r) {
        return index % r.width;
    }

    protected int getDy(int index, Rectangle r) {
        return index / r.width;
    }

    protected int getIndex(int x, int y, Rectangle r) {
        return y*r.width + x;
    }

    protected Point getPointFromPixelIndex(int pixelIndex, int width, Point p) {
        if (p == null) {
            return new Point(pixelIndex % width, pixelIndex / width);
        }
        return new Point((pixelIndex % width) + p.x, (pixelIndex / width) + p.y);
    }

// -- SEARCH METHODS ------------------------------------------------------

    /**
     * Look for occurencies of a pattern in the given array of pixels.
     * @param source an array of pixels representing an image.
     *
     * @param sourceRect geometry (rectangle) of the image.
     * @param maxHitCount maximum number of hits. Once the number is reached,
     * searching stops.
     * @return a vector containing coordinates (Point instances) where the
     * pattern was found.
     */
    public Vector findPattern(int[] source, Rectangle sourceRect, int maxHitCount, float passRate) {
        Vector v = new Vector();

        int pos = indexOf(source, 0, sourceRect, passRate);

        while (pos >= 0 && (maxHitCount <= 0 || v.size() <= maxHitCount)) {
            v.add(new Integer(pos));
            pos = indexOf(source, pos + 1, sourceRect, passRate);
        }

        return v;
    }


    /**
     * Search an integer array for a pattern. The <code>source</code> integer
     * array typically represents an array of image pixels. Searching will
     * start from <code>fromIndex</code>. The <code>sourceRect</code> argument
     * should contain rectangle specifying the source image geometry, i.e.
     * image width and height.
     *
     * @param source       the array being searched.
     * @param fromIndex    the index to begin searching from.
     * @param sourceRect source image geometry (width and height).
     */
    protected int indexOf(int[] source, int fromIndex, Rectangle sourceRect, float passRate) {
        int sourceCount = source.length;
        int targetCount = pixels.length;
        int srcWidth = sourceRect.width;

        // fromIndex is greater than the length of array
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        int maxX = srcWidth - r.width;
        int maxY = sourceRect.height - r.height;
        int maxOffset = getIndex(maxX, maxY, sourceRect);
        int currX;

        for (int i = fromIndex; i <= maxOffset && !stop; i++) {
            //
            currX = i % srcWidth;
            if (currX <= maxX && matches(source, i, sourceRect, passRate)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get details of this pattern as a String. If the pattern has less than 20
     * pixels, they are also included in the customary format specified by the
     * <code>getStampAsString()</code> method.
     */
    public String toString() {
        return "[" + this.getClass().getName() + ": type=" + getType()
        + (r != null ? (", rectangle=" + r) : "")
        + (pixels != null && pixels.length < 20 ? (", pixels=" + getStampAsString()) : "")
        + "]";
    }

    public int getNumberOfNonAlphaPixels() {
        return numberOfNonAlphaPixels;
    }


}
