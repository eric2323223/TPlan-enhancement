/*
 * T-Plan Robot, automated testing tool based on remote desktop technologies.
 * Copyright (C) 2010  T-Plan Limited (http://www.t-plan.co.uk),
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
 * Methods called on a menu and/or tool bar state change. This interface is
 * intended to be used together with the {@link GuiComponent} one to allow
 * pluggable components to enable or disable any owned menu or tool bar items
 * whenever the application state changes.
 */
public interface MenuStateListener {

    /**
     * Availability of menu or tool bar button has changed. This method is called
     * whenever the state of the application changes and it is necessary to enable
     * or disable particular menu items and/or tool bar buttons. If the component
     * implementing this interface adds some items to the menu or tool bar, this
     * method should enable/disable them.
     * @param mainFrame
     */
    void menuStateChanged(MainFrame mainFrame);

}
