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

import javax.swing.*;
import javax.swing.tree.*;

/**
 * Implementation of a preference tree node.
 * @product.signature
 */
public class PreferenceTreeNodeImpl extends DefaultMutableTreeNode
        implements PreferenceTreeNode, Comparable {
    
    /**
     * Component to show when this node gets selected
     */
    protected JComponent displayComponent = null;
    
    /**
     * Constructor.
     *
     * @param component to display in the right part of the
     * user preferences dialog for this tree node. May be null.
     * @param userObject a label (text) for the node.
     */
    public PreferenceTreeNodeImpl(JComponent component, Object userObject) {
        this.displayComponent = component;
        setUserObject(userObject);
    }
    
    /**
     * Get the node display component. A null value is typically returned for
     * folder nodes and should be interpreted as "there's nothing to display".
     *
     * @return node component.
     */
    public JComponent getDisplayComponent() {
        return displayComponent;
    }
    
    /**
     * Returns true if the component contains valid preference values. If user
     * has edited some preferences which are not valid, the method is supposed
     * to return false.
     *
     * @return true if the component contains valid preference values, false
     * otherwise.
     */
    public boolean isContentValid() {
        if (displayComponent != null && displayComponent instanceof PreferencePanel) {
            return ((PreferencePanel)displayComponent).isContentValid();
        }
        return true;
    }
    
    /**
     * Set the node display component. If the argument is null, there will be
     * no component displayed when the node gets selected.
     *
     * @param component a display component for this node.
     */
    public void setDisplayComponent(JComponent component) {
        this.displayComponent = component;
    }

    /**
     * Implementation of the Comparable interface allowing to sort the nodes
     * alphabetically by their display names.
     * @param o an object to compareto.
     * @return comparison result
     */
    public int compareTo(Object o) {
        String s1;
        if (o instanceof DefaultMutableTreeNode) {
            Object n = ((DefaultMutableTreeNode)o).getUserObject();
            s1 = n == null ? "" : n.toString();
        } else {
            s1 = o.toString();
        }
        String s2 = getUserObject() == null ? "" : getUserObject().toString();
        return s2.compareTo(s1);
    }
    
}
