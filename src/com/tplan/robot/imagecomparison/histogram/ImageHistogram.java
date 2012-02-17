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
package com.tplan.robot.imagecomparison.histogram;

import com.tplan.robot.util.Measurable;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.PixelGrabber;
import java.util.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JLabel;

/**
 * Image color histogram.
 * @product.signature
 */
public class ImageHistogram implements Measurable {

    private Map<Integer, int[]> pointers;
    long length;
    protected int pxcnt = 0;
    /**
     * Initial histogram size, i.e. size of the array that holds the color counters
     */
    int counterArraySize = 4096;
    /**
     * Histogram increment, i.e. by how much to increase the histogram size when the number of color counters exceeds
     * the array size
     */
    int counterArrayIncrement = 4096;
    /**
     * Maximum number of horizontal pixel rows which will be processed at a time. A higher number will result in faster
     * execution but will consume more memory.
     */
    int MAX_LOADED_PIXEL_ROWS = 200;
    int transparentCounter = 0;
    private int minAlpha = 0xFF;
    private int comparablePixels = 0;
    /**
     * An image observer used to determine the image size.
     */
    private JLabel imageObserver = new JLabel();

    public ImageHistogram() {
    }

    @Override
    public String toString() {
        String s = "[" + this.getClass().getName() + ": colorCount=" + getPointers().size() + "]";
        return s;
    }

    public Map<Integer, int[]> getPointers() {
        return pointers;
    }

    public long getLength() {
        return length;
    }

    public ImageHistogram load(Image img, Rectangle rect) {
        pxcnt = 0;
        Map t = new HashMap();

        Integer px;
        int[] colorCnt;

        int pixels[] = null;

        int imgWidth = rect == null ? img.getWidth(imageObserver) : rect.width;
        int imgHeight = rect == null ? img.getHeight(imageObserver) : rect.height;

        Rectangle r = new Rectangle(rect.x, rect.y, imgWidth, Math.min(imgHeight, MAX_LOADED_PIXEL_ROWS));
        int plen;
        int cnt = 0, alpha;

        while (r.y <= imgHeight && r.height > 0) {
            pixels = getPixels(img, r);
            plen = pixels.length;
            cnt += plen;

            for (int i = 0; i < plen; i++) {
                pxcnt++;
                px = new Integer(pixels[i]);
                alpha = (px >> 24) & 0xFF;
                if (alpha >= minAlpha) {
                    comparablePixels++;
                }
                colorCnt = (int[]) t.get(px);

                if (colorCnt == null) {
                    t.put(px, new int[]{1});
                    if (alpha < minAlpha) {
                        transparentCounter++;
                    }
                } else {
                    colorCnt[0]++;
                }
            }

            r.y += MAX_LOADED_PIXEL_ROWS;
            r.height = Math.min(imgHeight - r.y, MAX_LOADED_PIXEL_ROWS);
        }
        this.length = imgWidth * imgHeight;
        this.pointers = t;

        return this;
    }

    public ImageHistogram load(int pixels[]) {
        pxcnt = 0;
        Map t = new HashMap();

        Integer px;
        int counter[];

        int plen = pixels.length;
        int alpha;
        for (int i = 0; i < plen; i++) {
            pxcnt++;
            px = new Integer(pixels[i]);
            alpha = (px >> 24) & 0xFF;
            if (alpha >= minAlpha) {
                comparablePixels++;
            }
            counter = (int[]) t.get(px);

            if (counter == null) {
                t.put(px, new int[]{1});
                if (alpha < minAlpha) {
                    transparentCounter++;
                }
            } else {
                counter[0]++;
            }
        }
        this.length = pixels.length;
        this.pointers = t;

        return this;
    }

    public ImageHistogram load(int pixels[], Rectangle histRect, Rectangle sourceRect) {
        pxcnt = 0;
        Map t = new HashMap();

        Integer px;
        int[] colorCnt;

        int index, alpha;

        for (int i = 0; i < histRect.height; i++) {
            index = histRect.x + (histRect.y + i) * sourceRect.width;
            for (int j = 0; j < histRect.width; j++) {
                pxcnt++;
                px = new Integer(pixels[index + j]);
                alpha = (px >> 24) & 0xFF;
                if (alpha >= minAlpha) {
                    comparablePixels++;
                }
                colorCnt = (int[]) t.get(px);

                if (colorCnt == null) {
                    t.put(px, new int[]{1});
                    if (alpha < minAlpha) {
                        transparentCounter++;
                    }
                } else {
                    colorCnt[0]++;
                }
            }
        }
        this.length = histRect.width * histRect.height;
        this.pointers = t;
        return this;
    }

    /**
     * Return an array of colors sorted ascending based on pixel count.
     * @param maxColors maximum length of the array.
     * @return array of colors sorted ascending based on pixel count.
     */
    public java.util.List<Integer> getSortedListOfMajorColors(int maxColors) {
        Iterator e = pointers.keySet().iterator();
        Integer color;
        int cnt, temp;
        int cutOffCnt = 0;
        ArrayList<Integer> lc = new ArrayList();
        ArrayList<Integer> lp = new ArrayList();

        while (e.hasNext()) {
            color = (Integer) e.next();
            cnt = ((int[]) pointers.get(color))[0];
            if (cnt > cutOffCnt) {
                if (lc.size() > 0) {
                    for (int i = 0; i < lc.size(); i++) {
                        temp = lc.get(i);
                        if (cnt > temp) {
                            // Swap
                            lc.add(i, cnt);
                            lp.add(i, color);
                            break;
                        }
                    }
                } else {
                    lc.add(cnt);
                    lp.add(color);
                }
                cutOffCnt = lc.get(lc.size() - 1);
            }
            if (lc.size() > maxColors) {
                lc.remove(lc.size() - 1);
                lp.remove(lp.size() - 1);
            }
        }

        return lp;
    }

    /**
     * Return an array of colors sorted descending based on pixel count.
     * @param maxColors maximum length of the array.
     * @return array of colors sorted descending based on pixel count.
     */
    public int[] getSortedListOfColors(int maxColors) {
        int ratio = (int) ((float) this.length / (pointers.size() - transparentCounter)) + 1; //this.pointers.size()) + 1;
        int array[] = new int[maxColors];
        int loopCnt = 0;

        Map t = new HashMap(pointers);
        Iterator e = pointers.keySet().iterator();
        Number color;
        int cnt[];

        // First get all colors which have a counter of 1 (unique colors)
        while (e.hasNext() && loopCnt < maxColors) {
            color = (Number) e.next();
            cnt = (int[]) pointers.get(color);
            if (cnt[0] == 1 && ((color.intValue() >> 24) & 0xFF) >= minAlpha) {
                array[loopCnt++] = color.intValue();
                t.remove(color);
            }
        }

        // Second get all colors which are below the ratio.
        // Also add the colors from the end.
        int endIndex = maxColors - 1;
        e = t.keySet().iterator();
        while (e.hasNext() && loopCnt < maxColors) {
            color = (Number) e.next();
            cnt = (int[]) pointers.get(color);
            if (cnt[0] <= ratio && ((color.intValue() >> 24) & 0xFF) >= minAlpha) {
                array[loopCnt++] = color.intValue();
            } else if (endIndex > 0) {
                array[endIndex--] = color.intValue();
            }
        }

        return array;
    }

    private int[] getPixels(Image img, Rectangle r) {
        int width = r.width;
        int height = r.height;
        int ai[] = new int[width * height];

        PixelGrabber pixelgrabber =
                new PixelGrabber(img, r.x, r.y, width, height, ai, 0, width);
        try {
            pixelgrabber.grabPixels();
        } catch (InterruptedException interruptedexception) {
            interruptedexception.printStackTrace();
        }
        return ai;
    }

    public float getProgress() {
        return length > 0 ? (float) pxcnt / length : 0;
    }

    public int getNumberOfComparablePixels() {
        return comparablePixels;
    }

    /**
     * @return the minAlpha
     */
    public int getMinAlpha() {
        return minAlpha;
    }

    /**
     * @param minAlpha the minAlpha to set
     */
    public void setMinAlpha(int minAlpha) {
        this.minAlpha = minAlpha;
    }
}
