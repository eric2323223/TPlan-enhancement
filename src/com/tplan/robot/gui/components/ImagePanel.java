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
 * A panel which displays an image.
 * @product.signature
 */
public class ImagePanel extends JPanel {

    Image image;

    /**
     * Default constructor.
     */
    public ImagePanel() {
        this.setOpaque(false);
    }

    /**
     * Get the image displayed by the panel.
     * @return image displayed by the panel.
     */
    public Image getImage() {
        return image;
    }

    /**
     * Set the image to be displayed by the panel.
     * @param image an image to be displayed or null.
     */
    public void setImage(Image image) {
        this.image = image;
        int width = image == null ? 0 : image.getWidth(this);
        int height = image == null ? 0 : image.getHeight(this);
        
        setPreferredSize(new Dimension(width, height));
        setSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
        setPreferredSize(new Dimension(width, height));
        revalidate();
        repaint();
    }

    /**
     * The overriden paint method takes care of painting of the image.
     * @param g
     */
    public void paint(Graphics g) {
        if (image != null) {
            g.drawImage(image, 0, 0, null);
        }
        super.paint(g);
    }
}
