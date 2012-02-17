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

import java.awt.*;

/**
 * {@link java.awt.Color} enhanced with color name.
 * @product.signature
 */
public class NamedColor extends Color {
    
    /** Color name or description */
    String name;
    
    /**
     * Constructor
     * @param color a color
     * @param name a name for the color
     */
    public NamedColor(Color color, String name) {
        super(color.getRGB());
        this.name = name;
    }
    
    /**
     * Overriden method. Returns name of the color.
     * @return color name
     */
    public String toString() {
        return name;
    }
    
    /**
     * Overriden method. Returns true when the two objects are instances of
     * java.awt.Color and have the same RGB values.
     *
     * @param obj an object to compare (another color)
     * @return true when the two objects are instances of java.awt.Color and
     * have the same RGB values, false otherwise
     */
    public boolean equals(Object obj) {
        if (obj instanceof Color) {
            Color c = (Color)obj;
            
            return c.getRed() == getRed()
            && c.getGreen() == getGreen()
            && c.getBlue() == getBlue();
        }
        return false;
    }
}

