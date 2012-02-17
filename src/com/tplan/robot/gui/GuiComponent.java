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
package com.tplan.robot.gui;

/**
 * Interface of a pluggable GUI component. If the component installs any menu
 * items and/or tool bar buttons, it may also implement the {@link MenuStateListener}
 * interface to get notified of application status changes.
 * @product.signature
 */
public interface GuiComponent {

    /**
     * Set the main frame. This method will be called just once when the main
     * application GUI is created and initialized and is about to become visible.
     * The component implementing this interface is supposed to add itself to
     * the menu and/or tool bar accessible through the {@link MainFrame#getJMenuBar()}
     * and {@link MainFrame#getToolBar()} methods.
     *
     * @param mainFrame main application GUI instance.
     */
    void setMainFrame(MainFrame mainFrame);

}
