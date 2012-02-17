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
import java.io.DataInputStream;
import java.io.IOException;

/**
 * CopyRect image encoding (code 1).
 * See the <code>FramebufferUpdate</code> RFB message for more information.
 *
 * @product.signature
 */
public class CopyRectEncoding extends AbstractEncoding  {

    private long time;

    /**
     * Copy a rectangle of the remote desktop image to a given position.
     * This method is called when a FramebufferUpdate message with the CopyRect
     * encoding type is received. See the FramebufferUpdate and CopyRect
     * chapters of the RFB 3.3 specification for more info.
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

        // Read the x,y coordinates of the source rectangle
        int cpX = inStream.readUnsignedShort();
        int cpY = inStream.readUnsignedShort();

        // Copy the specified area to the delta coordinates
        image.getGraphics().copyArea(cpX, cpY, w, h, x - cpX, y - cpY);
        lastUpdateProcessingTime = System.currentTimeMillis() - time;
    }

    /**
     * Get a human readable name of the encoding. This implementation returns
     * "CopyRect".
     *
     * @return encoding name.
     */
    public String getDisplayName() {
        return "CopyRect";
    }

    /**
     * Get numeric code of the encoding. This implementation returns 1 (see
     * CopyRect encoding in RFB 3.3 specification).
     *
     * @return numeric code of CopyRect encoding.
     */
    public Integer getEncodingCode() {
        return new Integer(ENCODING_COPY_RECT);
    }

    /**
     * Get description of the encoding module.
     * @return encoding description.
     */
    public String getDescription() {
        return "CopyRect encoding of the RFB 3.3 protocol.";
    }

    /**
     * Get unique ID for this encoding plugin.
     * @return a unique ID.
     */
    public String getUniqueId() {
        return "VNCRobot_native_RFB_Encoding_CopyRect";
    }

    public int getLastMessageSize() {
        return 4;
    }
}
