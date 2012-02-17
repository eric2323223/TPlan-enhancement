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
 * RRE image encoding (code 2).
 * See the <code>FramebufferUpdate</code> RFB message for more information.
 *
 * @product.signature
 */
public class RREEncoding extends AbstractEncoding {

    private long time;

    /**
     * <p>Update the remote desktop image with the data to be read from the input stream in RRE encoding
     * (Rise-and-Run-Length).
     * </p>
     * This method is called when a FramebufferUpdate message with RRE encoding type is received.
     * See the FramebufferUpdate and RRE chapters of the RFB 3.3 specification for more info.
     *
     * @param image remote desktop image (BufferedImage instance).
     * @param inStream input stream of the network socket.
     * @param pixelFormat pixel format.
     * @param x updated rectangle x coordinate.
     * @param y updated rectangle y coordinate.
     * @param w updated rectangle width.
     * @param h updated rectangle height.
     * @throws IOException when an I/O exception ocurrs during the communication with the RFB server.
     */
    public void updateImage(BufferedImage image, DataInputStream inStream,
            PixelFormat pixelFormat, int x, int y, int w, int h) throws IOException {
        time = System.currentTimeMillis();
        lastUpdateProcessingTime = -1;
        final int bytesPerPixel = pixelFormat.getBytesPerPixel();

        // Read number of subrectangles
        int subrectCnt = inStream.readInt();
        Graphics g = image.getGraphics();

        // Fill in the background color
        g.setColor(pixelFormat.readColor(inStream));
        g.fillRect(x, y, w, h);

        int sx, sy, sw, sh, offset;
        Color pix;

        // Read the subrectangle data. Each subrectangle is defined by 9 to 12 bytes
        // where the first <bytesPerPixel> bytes represent the pixel color and
        // the rest 8 bytes are the subrectangle coordinates [x, y, width, height].
        final int rectLength = 8 + bytesPerPixel;
        byte[] buf = new byte[subrectCnt * rectLength];
        inStream.readFully(buf);

        // Proceed for each rectangle
        for (int i = 0; i < buf.length; i += rectLength) {

            // Read the pixel color from the buffer
            pix = pixelFormat.readColor(buf, i);
            offset = i + bytesPerPixel;

            // Read the 4 coordinates (unsigned shorts)
            sx = x + ((buf[offset] & BYTEMASK) << 8 | (buf[offset+1] & BYTEMASK));
            sy = y + ((buf[offset+2] & BYTEMASK) << 8 | (buf[offset+3] & BYTEMASK));
            sw = ((buf[offset+4] & BYTEMASK) << 8 | (buf[offset+5] & BYTEMASK));
            sh = ((buf[offset+6] & BYTEMASK) << 8 | (buf[offset+7] & BYTEMASK));

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
     * "RRE".
     *
     * @return encoding name.
     */
    public String getDisplayName() {
        return "RRE";
    }

    /**
     * Get numeric code of the encoding. This implementation returns 2 (see
     * RRE encoding specification in RFB 3.3 specification).
     *
     * @return numeric code of RRE encoding.
     */
    public Integer getEncodingCode() {
        return new Integer(ENCODING_RRE);
    }

    /**
     * Get description of the encoding module.
     * @return encoding description.
     */
    public String getDescription() {
        return "RRE encoding of the RFB 3.3 protocol.";
    }

    /**
     * Get unique ID for this encoding plugin.
     * @return a unique ID.
     */
    public String getUniqueId() {
        return "VNCRobot_native_RFB_Encoding_RRE";
    }
}
