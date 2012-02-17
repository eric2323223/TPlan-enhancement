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


import javax.swing.*;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Implementation of a filterable pop up menu.
 *
 * @product.signature
 */
public class FilteredPopupMenuImpl extends JPopupMenu implements MenuKeyListener {

    public FilteredPopupMenuImpl() {
        setForeground(Color.black);
        setBackground(Color.white);
        setFocusable(true);
    }

    public void setVisible(boolean visible) {
        setFocusable(visible);
        if (getInvoker() != null) {
            getInvoker().setFocusable(!visible);
        }
        super.setVisible(visible);
    }

    public JMenuItem add(JMenuItem item) {
        item.removeMenuKeyListener(this);
        item.addMenuKeyListener(this);
        item.setForeground(Color.black);
        item.setBackground(Color.white);
        return super.add(item);
    }


    public void menuKeyPressed(MenuKeyEvent e) {
        char c = e.getKeyChar();
        if (Character.isLetterOrDigit(c) || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            firePropertyChange("characterTyped", this, e);
            e.consume();
        }
    }

    public void menuKeyReleased(MenuKeyEvent e) {

    }

    public void menuKeyTyped(MenuKeyEvent e) {
        firePopupMenuCanceled();
    }
}
