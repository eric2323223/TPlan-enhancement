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


import java.awt.*;

/**
 * A panel which displays an image and allows user to define a rectangle of
 * interest using mouse drags.
 * @product.signature
 */
public class ImageDrawPanel extends ImagePanel {

    DrawPanel pnlDraw;

    /**
     * Default constructor.
     */
    public ImageDrawPanel() {
        pnlDraw = new DrawPanel();
        pnlDraw.setEnableDragRect(true);
        this.setLayout(new BorderLayout(1, 1));
        this.add(pnlDraw, BorderLayout.CENTER);
    }

    /**
     * Get the component which allows drawing of a rectangle through mouse drags.
     * @return draw panel.
     */
    public DrawPanel getDrawPanel() {
        return pnlDraw;
    }
}
