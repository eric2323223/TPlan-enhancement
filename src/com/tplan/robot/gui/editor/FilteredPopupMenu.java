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
package com.tplan.robot.gui.editor;

import javax.swing.text.Element;
import java.awt.*;

/**
 * Interface for pop up menus which are capable of filtering of their menu items
 * depending on a text of a document element.
 *
 * @product.signature
 */
public interface FilteredPopupMenu {

    /**
     * Filter menu items based on the provided document element and display the menu at the specified point.
     * @param owner menu owner (invoking component).
     * @param e a document element representing the active editor line of text.
     * @param p a point to display the menu at.
     */
    public void display(Component owner, Element e, Point p);
}
