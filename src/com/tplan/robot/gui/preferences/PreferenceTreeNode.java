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
package com.tplan.robot.gui.preferences;

import javax.swing.JComponent;
import java.io.Serializable;
import javax.swing.tree.MutableTreeNode;

/**
 * Interface declaring methods of a tree node which holds a panel with preferences.
 * @product.signature
 */
public interface PreferenceTreeNode extends MutableTreeNode, Serializable {
    
    /**
     * Get the node display component.
     *
     * @return a component to be displayed when the tree node gets selected. May
     * return null.
     */
    public JComponent getDisplayComponent();
    
    /**
     * Set the node display component.
     *
     * @param displayComponent graphical component to be displayed when
     * the tree node gets selected. May be null.
     */
    public void setDisplayComponent(JComponent displayComponent);
    
    /**
     * Returns true if the displayComponent contains valid preference values. If user
     * has entered some invalid preference values into the node graphical component,
     * the method is supposed to return false.
     *
     * @return true if the displayComponent contains valid preference values, false
     * otherwise.
     */
    public boolean isContentValid();
}