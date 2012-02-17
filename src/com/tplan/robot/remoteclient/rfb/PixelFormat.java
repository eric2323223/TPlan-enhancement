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
package com.tplan.robot.remoteclient.rfb;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Generic pixel format decoder and encoder conforming to the <a href=http://www.realvnc.com/docs/rfbproto.pdf>RFB Protocol version 3.3 specification</a>.
 * @product.signature
 */
public class PixelFormat implements RfbConstants {

    int desktopWidth;
    int desktopHeight;
    private byte bitsPerPixel;
    private int bytesPerPixel;
    private int colorDepthInBytes;
    private byte colorDepth;
    private boolean bigEndian;
    private boolean trueColor;
    int redMax;
    int greenMax;
    int blueMax;
    byte redShift;
    byte greenShift;
    byte blueShift;
    private ColorModel colorModel;
    private boolean initialized = false;

    public PixelFormat(int desktopWidth, int desktopHeight, byte bitsPerPixel,
            byte colorDepth, boolean bigEndian, boolean trueColor,
            int redMax, int greenMax, int blueMax,
            byte redShift, byte greenShift, byte blueShift) {

        this.desktopWidth = desktopWidth;
        this.desktopHeight = desktopHeight;
        this.bitsPerPixel = bitsPerPixel;
        this.colorDepth = colorDepth;
        this.bigEndian = bigEndian;
        this.trueColor = trueColor;
        this.redMax = redMax;
        this.greenMax = greenMax;
        this.blueMax = blueMax;
        this.redShift = redShift;
        this.greenShift = greenShift;
        this.blueShift = blueShift;
        bytesPerPixel = (int) bitsPerPixel >> 3;
        colorDepthInBytes = (int) colorDepth >> 3;
    }

    public PixelFormat(int desktopWidth, int desktopHeight, List<? extends Number> v) {

        if (v.size() != 10) {
            throw new IllegalArgumentException("Pixel format requires a list of 10 numbers but received "+v.size()+": "+v);
        }
        this.desktopWidth = desktopWidth;
        this.desktopHeight = desktopHeight;
        this.bitsPerPixel = v.get(0).byteValue();
        this.colorDepth = v.get(1).byteValue();
        this.bigEndian = v.get(2).byteValue() != 0;
        this.trueColor = v.get(3).byteValue() != 0;
        this.redMax = v.get(4).intValue();
        this.greenMax = v.get(5).intValue();
        this.blueMax = v.get(6).intValue();
        this.redShift = v.get(7).byteValue();
        this.greenShift = v.get(8).byteValue();
        this.blueShift = v.get(9).byteValue();
        bytesPerPixel = (int) bitsPerPixel >> 3;
        colorDepthInBytes = (int) colorDepth >> 3;
    }

    /** Creates a new instance of PixelFormat */
    public PixelFormat(int desktopWidth, int desktopHeight, byte params[], int offset) {
        this.desktopWidth = desktopWidth;
        this.desktopHeight = desktopHeight;

        bitsPerPixel = params[offset];
        colorDepth = params[offset + 1];
        bigEndian = (params[offset + 2] != 0);
        trueColor = (params[offset + 3] != 0);
        redMax = (params[offset + 4] & BYTEMASK) << 8 | (params[offset + 5] & BYTEMASK);
        greenMax = (params[offset + 6] & BYTEMASK) << 8 | (params[offset + 7] & BYTEMASK);
        blueMax = (params[offset + 8] & BYTEMASK) << 8 | (params[offset + 9] & BYTEMASK);
        redShift = params[offset + 10];
        greenShift = params[offset + 11];
        blueShift = params[offset + 12];
        bytesPerPixel = (int) bitsPerPixel >> 3;
        colorDepthInBytes = (int) colorDepth >> 3;
    }

    public byte[] toMessage() {
        byte[] buf = new byte[20];
        buf[0] = (byte) MSG_C2S_SET_PIXEL_FORMAT;         // Message ID
        buf[1] = buf[2] = buf[3] = (byte) 0;               // Padding
        buf[4] = bitsPerPixel;    // Bits per pixel
        buf[5] = colorDepth;    // Color depth
        buf[6] = bigEndian ? (byte) 1 : (byte) 0;    // Big endian flag
        buf[7] = trueColor ? (byte) 1 : (byte) 0;    // True color flag
        buf[8] = (byte) ((redMax >> 8) & BYTEMASK);
        buf[9] = (byte) (redMax & BYTEMASK);
        buf[10] = (byte) ((greenMax >> 8) & BYTEMASK);
        buf[11] = (byte) (greenMax & BYTEMASK);
        buf[12] = (byte) ((blueMax >> 8) & BYTEMASK);
        buf[13] = (byte) (blueMax & BYTEMASK);
        buf[14] = redShift;     // Red shift (at which bit position the red starts)
        buf[15] = greenShift;     // Green shift
        buf[16] = blueShift;     // Blue shift
        buf[17] = buf[18] = buf[19] = 0;
        return buf;
    }
    // Init imaging objects based on the params
    private void init() {

        // If the true color flag is true, we are in classic mode where each
        // updated image pixel is defined by its RGB color.
        if (trueColor) {

            // Create a Color Model
            colorModel = new DirectColorModel(getBitsPerPixel(),
                    redMax << redShift, greenMax << greenShift, blueMax << blueShift);

        } else {
            if (isByte()) {
                colorModel = new DirectColorModel(8, 224, 28, 4);
            } else {
                colorModel = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
            }
        }
        initialized = true;
    }

    public void setColorMap(int offset, int length, byte[] colorData) {
        final int cnt = colorData.length;
        final int size = cnt / 6;
        byte[] red = new byte[size];
        byte[] green = new byte[size];
        byte[] blue = new byte[size];
        int index;

        for (int i = 0; i < size; i++) {
            index = i * 6;
            if (bigEndian) {
                red[i] = (byte) ((colorData[index + 1] << 8) + colorData[index]);
                green[i] = (byte) ((colorData[index + 3] << 8) + colorData[index + 2]);
                blue[i] = (byte) ((colorData[index + 5] << 8) + colorData[index + 4]);
            } else {
                red[i] = (byte) ((colorData[index] << 8) + colorData[index + 1]);
                green[i] = (byte) ((colorData[index + 2] << 8) + colorData[index + 3]);
                blue[i] = (byte) ((colorData[index + 4] << 8) + colorData[index + 5]);
            }
        }
        colorModel = new IndexColorModel(bitsPerPixel, red.length, red, green, blue);
    }

    /**
     * Read a color from the given output stream.
     */
    public Color readColor(DataInputStream is) throws IOException {
        byte buf[] = new byte[getBytesPerPixel()];
        is.readFully(buf);
        return readColor(buf, 0);
    }

    // Temporary debug code
//    boolean debug = System.getProperty("rfb.debug.rawEncoding") != null;

    /**
     * Read a color from the buffer and offset.
     */
    public int readRgb(byte[] buf, int offset) throws IOException {
        int p = 0, k;

//        if (debug && offset == 0) {
//            Utils.debugArray(buf, buf.length, "Pixels");
//        }

        // Convert bytes to integer.
        // The big endian flag controls how we read the bytes of ARGB.
        if (bigEndian) {
            int endOff = offset + bytesPerPixel - 1;
            for (k = 0; k < colorDepthInBytes; k++) {
                p |= (buf[endOff - k] & BYTEMASK) << k * 8;
            }
        } else {
            for (k = 0; k < colorDepthInBytes; k++) {
                p |= (buf[k + offset] & BYTEMASK) << k * 8;
            }
        }

        // Return color obtained through the color model
        return getColorModel().getRGB(p);
    }

    /**
     * Read a color from the buffer and offset.
     */
    public Color readColor(byte[] buf, int offset) throws IOException {
        return new Color(readRgb(buf, offset));
    }

    public boolean isByte() {
        return bitsPerPixel <= 8;
    }

    public int getColor(byte value) {
        return getColorModel().getRGB(value);
    }

    public byte getBitsPerPixel() {
        return bitsPerPixel;
    }

    public int getBytesPerPixel() {
        return bytesPerPixel;
    }

    public byte getColorDepth() {
        return colorDepth;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    public boolean isTrueColor() {
        return trueColor;
    }

    public boolean isColorMap() {
        return !trueColor;
    }

    public ColorModel getColorModel() {
        if (!initialized) {
            init();
        }
        return colorModel;
    }

    public String toString() {
        String msg = "   Bits per pixel: " + bitsPerPixel;
        msg += "\n   Color depth:    " + colorDepth;
        msg += "\n   Big endian:     " + bigEndian;
        msg += "\n   True color:     " + trueColor;
        msg += "\n   Red max:        " + redMax;
        msg += "\n   Green max:      " + greenMax;
        msg += "\n   Blue max:       " + blueMax;
        msg += "\n   Red shift:      " + redShift;
        msg += "\n   Green shift:    " + greenShift;
        msg += "\n   Blue shift:     " + blueShift;
        return msg;
    }

    /**
     * @return the colorDepthInBytes
     */
    public int getColorDepthInBytes() {
        return colorDepthInBytes;
    }
}
