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
package com.tplan.robot.scripting;

import java.awt.Dimension;
import java.awt.Point;

/**
 * <p>Extension of java.awt.Point class allowing to specify
 * the coordinates as percentages instead of absolute numbers. This supports handling
 * of requests like "click at the point which is at 50% of the screen width and 20% of
 * screen height.<p>
 *
 * <p>The class can be used as a standard Point instance with absolute coordinates x ,y
 * passed through the constructor or superclass methods. To create a relative point
 * set at least one of the percentages to a non-negative value and invoke the
 * <code>updateCoordinates()</code> method. It recalculates the real x, y coordinates
 * with regard to the screen resolution.</p>.
 *
 * <p>A typical usage is:</p>
 *
 * <blockquote>
 * <pre>
 * RelativePoint p = new RelativePoint(0, 300);
 * p.setXPercentage(10f);
 * ...
 *
 * // The following method changes p.x to 64 (10 * 640/100).
 * // The p.y coordinate will remain 300 because it has no percentage defined.
 * p.updateCoordinates(new Dimension(640, 480));
 * </pre>
 * </blockquote>
 */
public class RelativePoint extends Point {

    private float xPercentage = -1.0F;
    private float yPercentage = -1.0F;

    /**
     * Default constructor. It creates a standard Point instance with coordinates
     * initialized to [0,0].
     */
    public RelativePoint() {
        super();
    }

    /**
     * Default constructor. It creates a standard Point instance with coordinates
     * initialized to those of the argument point. If the argument is a RelativePoint
     * instance, the percentages are initialized as well.
     */
    public RelativePoint(Point p) {
        super(p);
        if (p instanceof RelativePoint) {
            setXPercentage(((RelativePoint)p).getXPercentage());
            setYPercentage(((RelativePoint)p).getYPercentage());
        }
    }

    /**
     * Constructor. It creates a standard Point instance with coordinates
     * initialized to [x,y].
     */
    public RelativePoint(int x, int y) {
        super(x, y);
    }

    /**
     * Update relative coordinates with regard to the screen resolution. If the x or y
     * coordinate (or both) have a defined percentage, it will be recalculated.
     * @param resolution screen resolution.
     */
    public void updateCoordinates(Dimension resolution) {
        if (getXPercentage() >= 0) {
            x = Math.max(0, Math.round(getXPercentage() * resolution.width / 100) - 1);
        }
        if (getYPercentage() >= 0) {
            y = Math.max(0, Math.round(getYPercentage() * resolution.height / 100) - 1);
        }
    }

    /**
     * Get value of the x coordinate percentage. Default value is -1 meaning
     * that the coordinate is absolute rather than relative.
     *
     * @return the xPercentage
     */
    public float getXPercentage() {
        return xPercentage;
    }

    /**
     * @return the xPercentage
     */
    public boolean isXRelative() {
        return xPercentage >= 0;
    }

    /**
     * @param xPercentage the xPercentage to set
     */
    public void setXPercentage(float xPercentage) {
        this.xPercentage = xPercentage;
    }

    /**
     * Get value of the y coordinate percentage. Default value is -1 meaning
     * that the coordinate is absolute rather than relative.
     *
     * @return the yPercentage
     */
    public float getYPercentage() {
        return yPercentage;
    }

    /**
     * @return the yPercentage
     */
    public boolean isYRelative() {
        return yPercentage >= 0;
    }

    /**
     * @param yPercentage the yPercentage to set
     */
    public void setYPercentage(float yPercentage) {
        this.yPercentage = yPercentage;
    }
}
