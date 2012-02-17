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
 * <p>Hextile image encoding (code 5).
 * See the <code>FramebufferUpdate</code> RFB message for more information.</p>
 *
 * <p>Note that this class extends the <code>RawEncoding</code> class because
 * hextile image data can be also received in Raw encoding.</p>
 *
 * @product.signature
 */
public class HextileEncoding extends RawEncoding {

    private long time;
    private int size, subrectSize;

    /**
     * Update the remote desktop image with the data to be read from the input stream
     * in the Hextile encoding. It is similar to RRE/CoRRE but the tile size is fixed
     * to 16 pixels32bit.
     * <p>
     * A detailed Hextile encoding specification can be found on page 32 of
     * <a href=http://www.realvnc.com/docs/rfbproto.pdf>RFB Protocol version 3.8</a>.
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
        lastUpdateMessageSize = 0;
        size = 0;
        int tileWidth, tileHeight;
        int subenc;  // Subencoding byte with flags

        Color bgColor = new Color(0);
        Color fgColor = new Color(0);
        Graphics g = image.getGraphics();

        // Break the rectangle defined by [x,y,w,h] into 16x16 pixel tiles
        for (int tileY = y; tileY < y + h; tileY += 16) {
            tileHeight = Math.min(y + h - tileY, 16);

            // For each 16-pixel or smaller tile
            for (int tileX = x; tileX < x + w; tileX += 16) {
                tileWidth = Math.min(x + w - tileX, 16);

                // The first byte contains bit flags
                subenc = inStream.readUnsignedByte();
                size++;

                // If the Raw bit is set, other bits are irrelevant and a rectangle
                // in Raw encoding follows.
                if ((subenc & ENCODING_HEXTILE_RAW) != 0) {
                    super.updateImage(image, inStream, pixelFormat, tileX, tileY, tileWidth, tileHeight);
                    size += pixelFormat.getBytesPerPixel() * tileWidth * tileHeight;
                } else { // Other than Raw encodings

                    // If the Background bit is set, a background color follows.
                    // If the bit is not set, the bg color of the last tile will be used.
                    if ((subenc & ENCODING_HEXTILE_BG_SPECIFIED) != 0) {
                        bgColor = pixelFormat.readColor(inStream);
                        size += pixelFormat.getBytesPerPixel();
                    }

                    // Draw the background
                    g.setColor(bgColor);
                    g.fillRect(tileX, tileY, tileWidth, tileHeight);

                    // If the Foreground bit is set, a foreground color follows and
                    // the SubrectsColoured flag must be off.
                    // Question: can the server send the fg color even if the number
                    // of subrectangles is zero? Though it doesn't make sense,
                    // the protocol allows it
                    if ((subenc & ENCODING_HEXTILE_FG_SPECIFIED) != 0) {
                        fgColor = pixelFormat.readColor(inStream);
                        size += pixelFormat.getBytesPerPixel();
                    }

                    // If there are subrectangles, process them.
                    // If there are no subrectangles, the whole rect is just
                    // solid bg color.
                    if ((subenc & ENCODING_HEXTILE_ANY_SUBRECTS) != 0) {
                        boolean subrectsColored = (subenc & ENCODING_HEXTILE_SUBRECTS_COLORED) != 0;
                        updateHextileSubrect(image, inStream, pixelFormat,
                                tileX, tileY, tileWidth, tileHeight, subrectsColored, fgColor);
                        size += subrectSize;
                    }
                }
            }
        }

        lastUpdateMessageSize = size;
        lastUpdateProcessingTime = System.currentTimeMillis() - time;
    }

    /**
     * This method implements drawing of subrectangles in the Hextile encoding.
     */
    private void updateHextileSubrect(BufferedImage image, DataInputStream inStream,
            PixelFormat pixelFormat, int tx, int ty, int tw, int th, boolean subrectsColored, Color fg)
            throws IOException {

        // Read number of subrectangles
        int subrectCnt = inStream.readUnsignedByte();
        subrectSize = 1;

        if (subrectCnt <= 0) {
            return; // This shouldn't happen but the protocol is not specific
        }

        final int bytesPerPixel = pixelFormat.getBytesPerPixel();
        int subX, subY, subWidth, subHeight;

        // Helper variables
        int bt;

        Graphics g = image.getGraphics();

        // Colored subrectangles are defined by color (<bytesPerPixel> bytes)
        // followed by 2 bytes of the x, y, w, h coordinates (4 bits each).
        if (subrectsColored) {
            final int rectLength = bytesPerPixel + 2;
            byte[] buf = new byte[rectLength * subrectCnt];
            subrectSize += buf.length;
            int offset;

            inStream.readFully(buf);

            for (int i = 0; i < buf.length; i += rectLength) {

                // Read the pixel color from the buffer
                fg = pixelFormat.readColor(buf, i);
                offset = i + bytesPerPixel;

                // Read the subrect x and y coordinates from the first byte
                bt = buf[offset] & BYTEMASK;
                subX = tx + (bt >> 4);
                subY = ty + (bt & 0xf);

                // Read the subrect width and height coordinates from the second byte
                bt = buf[offset + 1] & BYTEMASK;
                subWidth = (bt >> 4) + 1;
                subHeight = (bt & 0x0f) + 1;

                // Draw the subrectangle
                g.setColor(fg);
                g.fillRect(subX, subY, subWidth, subHeight);
            }

        } else {
            // Subrectangles are of the same foreground color.
            // Each subrectangle is defined by 2 bytes which represent
            // the x, y, w, h coordinates (4 bits each).
            g.setColor(fg);

            byte[] buf = new byte[2 * subrectCnt];
            subrectSize += buf.length;
            inStream.readFully(buf);


            for (int i = 0; i < buf.length; i += 2) {

                // Read the subrect x and y coordinates from the first byte
                bt = buf[i] & BYTEMASK;
                subX = tx + (bt >> 4);
                subY = ty + (bt & 0xf);

                // Read the subrect width and height coordinates from the second byte
                bt = buf[i + 1] & BYTEMASK;
                subWidth = (bt >> 4) + 1;
                subHeight = (bt & 0x0f) + 1;

                // Draw the subrectangle
                g.fillRect(subX, subY, subWidth, subHeight);
            }
        }
    }

    /**
     * Get a human readable name of the encoding. This implementation returns
     * "Hextile".
     *
     * @return encoding name.
     */
    public String getDisplayName() {
        return "Hextile";
    }

    /**
     * Get numeric code of the encoding. This implementation returns 5 (see
     * Hextile encoding specification in RFB 3.3 specification).
     *
     * @return numeric code of Hextile encoding.
     */
    public Integer getEncodingCode() {
        return new Integer(ENCODING_HEXTILE);
    }

    /**
     * Get description of the encoding module.
     * @return encoding description.
     */
    public String getDescription() {
        return "Hextile encoding of the RFB 3.3 protocol.";
    }

    /**
     * Get unique ID for this encoding plugin.
     * @return a unique ID.
     */
    public String getUniqueId() {
        return "VNCRobot_native_RFB_Encoding_Hextile";
    }
}
