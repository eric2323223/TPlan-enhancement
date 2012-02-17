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
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * CoRRE image encoding (code 4).
 * See the <code>FramebufferUpdate</code> RFB message for more information.
 *
 * @product.signature
 */
public class CoRREEncoding extends AbstractEncoding {

    private long time;

    /**
     * Update the remote desktop image with the data to be read from the input stream in CoRRE encoding
     * (Compact Rise-and-Run-Length). It is very similar to RRE where the subrect size is max 255x255.
     * Note that this encoding has been deprecated and is supported for backward compatibility only.
     * <p/>
     * This method is called when a FramebufferUpdate message when a rectangle
     * with CoRRE encoding type is received. See the FramebufferUpdate and CoRRE
     * chapters of the RFB 3.3 specification for more info.
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

        final int bytesPerPixel = pixelFormat.getBytesPerPixel();

        // Read number of subrectangles
        int subrectCnt = inStream.readInt();

        Graphics g = image.getGraphics();

        // Read the background color
        g.setColor(pixelFormat.readColor(inStream));
        g.fillRect(x, y, w, h);

        int sx, sy, sw, sh, offset;
        Color pix;

        // Read the subrectangle data. Each subrectangle is defined by pixel
        // color (size of <bytesPerPixel> bytes) and the rest 4 bytes are
        // subrectangle coordinates [x, y, width, height].
        final int rectLength = 4 + bytesPerPixel;
        byte[] buf = new byte[subrectCnt * rectLength];
        inStream.readFully(buf);

        // Proceed for each rectangle
        for (int i = 0; i < buf.length; i += rectLength) {

            // Read the pixel color from the buffer
            pix = pixelFormat.readColor(buf, i);
            offset = i + bytesPerPixel;

            // Read the 4 coordinates (byte values)
            sx = x + (buf[offset] & BYTEMASK);
            sy = y + (buf[offset + 1] & BYTEMASK);
            sw = buf[offset + 2] & BYTEMASK;
            sh = buf[offset + 3] & BYTEMASK;

            // Fill the subrectangle with the defined color
            g.setColor(pix);
            g.fillRect(sx, sy, sw, sh);
        }

        // Size == bg color size + subrect counter size (1B) + buf size
        lastUpdateMessageSize = bytesPerPixel + 1 + buf.length;
        lastUpdateProcessingTime = System.currentTimeMillis() - time;
    }

    /**
     * Get a human readable name of the encoding. This implementation returns
     * "CoRRE".
     *
     * @return encoding name.
     */
    public String getDisplayName() {
        return "CoRRE";
    }

    /**
     * Get numeric code of the encoding. This implementation returns 4 (see
     * CoRRE encoding specification in RFB 3.3 specification).
     *
     * @return numeric code of CoRRE encoding.
     */
    public Integer getEncodingCode() {
        return new Integer(ENCODING_CORRE);
    }

    /**
     * Get description of the encoding module.
     * @return encoding description.
     */
    public String getDescription() {
        return "CoRRE encoding of the RFB 3.3 protocol.";
    }

    /**
     * Get unique ID for this encoding plugin.
     * @return a unique ID.
     */
    public String getUniqueId() {
        return "VNCRobot_native_RFB_Encoding_CoRRE";
    }
}
