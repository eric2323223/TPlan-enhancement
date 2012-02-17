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

/**
 * Raw image encoding (code 0).
 * See the <code>FramebufferUpdate</code> RFB message for more information.
 *
 * @product.signature
 */
public class RawEncoding extends AbstractEncoding {

    private long time;

    /**
     * Update the remote desktop image with the data to be read from the input stream in Raw encoding.
     * This is the less effective encoding when server directly sends pixels32bit of the updated rectangle.
     * This method is called when a FramebufferUpdate message with Raw encoding type is received.
     * See the FramebufferUpdate and Raw chapters of the RFB 3.3 specification for more info.
     *
     * @param image remote desktop image (BufferedImage instance).
     * @param inStream input stream of the network socket.
     * @param pxf pixel format.
     * @param x updated rectangle x coordinate.
     * @param y updated rectangle y coordinate.
     * @param w updated rectangle width.
     * @param h updated rectangle height.
     * @throws IOException when an I/O exception occurs during the communication with the RFB server.
     */
    public void updateImage(BufferedImage image, DataInputStream inStream,
            PixelFormat pxf, int x, int y, int w, int h) throws IOException {
        time = System.currentTimeMillis();
        lastUpdateProcessingTime = -1;
        final int fw = image.getWidth();
        final int bytesPerPixel = pxf.getBytesPerPixel();

        // Buffer for row pixels32bit (width * 4 bytes)
        final int rowlen = w * bytesPerPixel;
        byte[] buf = new byte[rowlen];
        int j, offsetX, offsetBuf;
        Object o = image.getRaster().getDataBuffer();

        // Branch for INT buffer
        if (o instanceof DataBufferInt) {
            int px[] = ((DataBufferInt) o).getData();

            // For each row; index i is the y coordinate
            for (int i = y; i < y + h; i++) {
                inStream.readFully(buf);
                offsetX = i * fw + x;

                // For each pixel of the row
                if (bytesPerPixel > 1) {
                    for (j = 0; j < w; j++) {
                        offsetBuf = j * bytesPerPixel;
                        px[offsetX + j] = pxf.readRgb(buf, offsetBuf);
                    }
                } else {
                    for (j = 0; j < w; j++) {

                        // We read the byte
                        px[offsetX + j] = pxf.getColor(buf[j]);
                    }
                }
            }
        } else if (o instanceof DataBufferByte) { // Branch for BYTE buffer
            // For each row; index i is the y coordinate
            byte px[] = ((DataBufferByte) o).getData();
            final int bands = image.getData().getNumBands();
            int rgb = 0, temp;
            ComponentColorModel cm = (ComponentColorModel) image.getColorModel();
            byte b[] = new byte[bands], k;

            // For each row; index i is the y coordinate
            for (int i = y; i < y + h; i++) {
                inStream.readFully(buf);
                offsetX = bands * (i * fw + x);

                for (j = 0; j < w; j++) {
                    temp = pxf.readRgb(buf, j * bytesPerPixel);
                    if (rgb != temp || j == 0) {
                        cm.getDataElements(temp, b);
                        rgb = temp;
                    }
                    for (k = 0; k < bands; k++) {
                        px[offsetX++] = b[k];
                    }
                }
            }
        }
        lastUpdateMessageSize = rowlen * h;
        lastUpdateProcessingTime = System.currentTimeMillis() - time;
    }

    /**
     * Get a human readable name of the encoding. This implementation returns
     * "Raw".
     *
     * @return encoding name.
     */
    public String getDisplayName() {
        return "Raw";
    }

    /**
     * Get numeric code of the encoding. This implementation returns 0 (see
     * Raw encoding in RFB 3.3 specification).
     *
     * @return numeric code of CopyRect encoding.
     */
    public Integer getEncodingCode() {
        return new Integer(ENCODING_RAW);
    }

    /**
     * Get description of the encoding module.
     * @return encoding description.
     */
    public String getDescription() {
        return "Raw encoding of the RFB 3.3 protocol.";
    }

    /**
     * Get unique ID for this encoding plugin.
     * @return a unique ID.
     */
    public String getUniqueId() {
        return "VNCRobot_native_RFB_Encoding_Raw";
    }
}
