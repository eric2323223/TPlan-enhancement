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
package com.tplan.robot.gui.preferences.color;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * <p>Custom list cell renderer to be used for drop downs displaying colors.
 * The color is represented by a name and a square filled with that color.
 * The renderer is used for a drop down with Color instances.
 * @product.signature
 */
public class ColorListCellRenderer extends DefaultListCellRenderer {

    /**
     * A buffered image used to paint the color square
     */
    private BufferedImage image = new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);

    /**
     * Default constructor
     */
    public ColorListCellRenderer() {
    }

    /**
     * Implementation of the ListCellRenderer interface. Return a component
     * that has been configured to display the specified color.
     * That component's <code>paint</code> method is then called to
     * "render" the cell.
     *
     * @param list         The JList we're painting.
     * @param value        The value returned by list.getModel().getElementAt(index).
     * @param index        The cells index.
     * @param isSelected   True if the specified cell was selected.
     * @param cellHasFocus True if the specified cell has the focus.
     * @return A component whose paint() method will render the specified value.
     */
    @Override
    public Component getListCellRendererComponent(JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list,
                value,
                index,
                isSelected,
                cellHasFocus);

        if (value instanceof Color && comp instanceof JLabel) {
            JLabel label = (JLabel) comp;
            Color c = (Color) value;
            int inset = 1;
            Graphics2D g = image.createGraphics();

            g.setColor(c);
            g.fill(new Rectangle(inset,
                    inset,
                    image.getWidth() - inset - 1,
                    image.getHeight() - inset - 1));
            label.setIcon(new ImageIcon(image));
            label.setText(value.toString());
        }
        return this;
    }
}