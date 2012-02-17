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
package com.tplan.robot.gui.components;


import javax.swing.*;
import java.awt.*;

/**
 * A panel which draws a "chessboard" consiting of light gray and dark gray squares.
 * It is used as background of components displaying an image.
 * @product.signature
 */
public class ChessboardBackgroundPanel extends JPanel {

    // Square size in pixels
    private final int SQUARE_SIZE = 8;

    // Square colors
    private final Color COLOR1 = Color.GRAY;
    private final Color COLOR2 = Color.LIGHT_GRAY;
    
    private int x, y, i, j, vCnt, hCnt;

   /**
    * Default constructor.
    */
    public ChessboardBackgroundPanel() {
        setBackground(Color.RED);
        setOpaque(false);
    }

    /**
     * Overriden method which paints the chessboard background.
     * @param g a graphics context.
     */
    public void paint(Graphics g) {
        paint(g, getVisibleRect());
    }

    public void paint(Graphics g, Rectangle r) {
        g.setClip(r);
        vCnt = (r.height / SQUARE_SIZE) + 1;
        hCnt = (r.width / SQUARE_SIZE) + 1;
        x = r.x;
        y = r.y;

        for (i = 0; i < vCnt; i++) {
            // Alternate color
            g.setColor(i % 2 > 0 ? COLOR1 : COLOR2);

            for (j = 0; j < hCnt; j++) {
                // Alternate color
                g.setColor((i+j) % 2 > 0 ? COLOR1 : COLOR2);
                g.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE);
                x += SQUARE_SIZE;
            }
            y += SQUARE_SIZE;
            x = r.x;
        }
        g.setClip(null);
        super.paint(g);
    }
}
