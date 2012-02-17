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
package com.tplan.robot.remoteclient.rfb.encoding;

import com.tplan.robot.remoteclient.rfb.PixelFormat;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Zlib image encoding (code 6).
 * See the <code>FramebufferUpdate</code> RFB message for more information.
 *
 * @product.signature
 */
public class ZlibEncoding extends AbstractEncoding {

    private long time;

    /**
     * Inflater used for the zlib encoding.
     */
    private Inflater inflater;

    /**
     * Update a rectangle encoded in Zlib encoding. This encoding is not specified in the
     * RFB protocol version 3.8. It is a combination of Raw encoding and Zlib compression.
     * It is used by some RFB servers like TightVNC.
     * <p>
     * First four bytes define length of the zlib encoded data followed by the data itself.
     * The data is basically just zlib compressed pixels32bit in the raw encoding.
     * <p>
     * For information on the zlib compression see
     * <a href=http://www.gzip.org/zlib>http://www.gzip.org/zlib</a>.
     *
     * @param image remote desktop image (BufferedImage instance).
     * @param inStream input stream of the network socket.
     * @param pixelFormat pixel format.
     * @param x updated rectangle x coordinate.
     * @param y updated rectangle y coordinate.
     * @param w updated rectangle width.
     * @param h updated rectangle height.
     * @throws IOException when an I/O exception occurs during the communication with the RFB server.
     */
    public void updateImage(BufferedImage image, DataInputStream inStream,
            PixelFormat pixelFormat, int x, int y, int w, int h) throws IOException {

        time = System.currentTimeMillis();
        lastUpdateProcessingTime = -1;

        final int fw = image.getWidth();
        final int bytesPerPixel = pixelFormat.getBytesPerPixel();

        // First 4 bytes define length of the following zlib data
        int length = inStream.readInt();
        lastUpdateMessageSize = length;

        // Read the zlib data into the buffer
        byte[] zlibBuf = new byte[length];
        inStream.readFully(zlibBuf, 0, length);

        // Create a zlib inflater
        if (inflater == null) {
            inflater = new Inflater(false);
        }
        inflater.setInput(zlibBuf, 0, length);

        // Buffer for a row of pixels
        byte[] buf = new byte[w * bytesPerPixel];
        int i, j, offsetX, offsetBuf;

        Object o = image.getRaster().getDataBuffer();

        // Branch for INT buffer
        if (o instanceof DataBufferInt) {
            int px[] = ((DataBufferInt) o).getData();

            // For each pixel of the row
            for (int rowY = y; rowY < y + h; rowY++) {

                // Decompress the row of pixels
                try {
                    inflater.inflate(buf);
                } catch (DataFormatException ex) {
                    throw new IOException("DataFormatException thrown when decompressing data:\n" + ex.getMessage());
                }
                offsetX = rowY * fw + x;

                for (i = 0; i < w; i++) {
                    offsetBuf = i * bytesPerPixel;
                    px[offsetX + i] = pixelFormat.readRgb(buf, offsetBuf);
                }
            }
        } else if (o instanceof DataBufferByte) { // Branch for BYTE buffer
            // For each row; index i is the y coordinate
            byte px[] = ((DataBufferByte) o).getData();
            final int bands = image.getData().getNumBands();
            int rgb;
            ComponentColorModel cm = (ComponentColorModel) image.getColorModel();
            byte b[] = new byte[bands], k;

            // For each row; index i is the y coordinate
            for (i = y; i < y + h; i++) {
                // Decompress the row of pixels
                try {
                    inflater.inflate(buf);
                } catch (DataFormatException ex) {
                    throw new IOException("DataFormatException thrown when decompressing data:\n" + ex.getMessage());
                }

                offsetX = bands * (i * fw + x);

                for (j = 0; j < w; j++) {
                    rgb = pixelFormat.readRgb(buf, j * bytesPerPixel);
                    cm.getDataElements(rgb, b);
                    for (k = 0; k < bands; k++) {
                        px[offsetX++] = b[k];
                    }
                }
            }
        }
        lastUpdateProcessingTime = System.currentTimeMillis() - time;
    }

    /**
     * Get a human readable name of the encoding. This implementation returns
     * "Zlib".
     *
     * @return encoding name.
     */
    public String getDisplayName() {
        return "Zlib";
    }

    /**
     * Get numeric code of the encoding. This implementation returns 6.
     *
     * @return numeric code of Zlib encoding.
     */
    public Integer getEncodingCode() {
        return new Integer(ENCODING_ZLIB);
    }

    /**
     * Get description of the encoding module.
     * @return encoding description.
     */
    public String getDescription() {
        return "Zlib encoding of the RFB 3.3 protocol.";
    }

    /**
     * Get unique ID for this encoding plugin.
     * @return a unique ID.
     */
    public String getUniqueId() {
        return "VNCRobot_native_RFB_Encoding_Zlib";
    }
}
