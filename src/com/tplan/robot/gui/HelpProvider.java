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

import javax.swing.*;

/**
 * Interface identifying an online help provider.
 * @product.signature
 */
public interface HelpProvider {
    /**
     * Set a help ID associated with a GUI component. A help ID is a key in
     * the map of help topics which identifies a particular help page. This method
     * allows to define a relationship like "if help for this GUI component is
     * requested, display the help page associated with this help key".
     *
     * @param component a GUI component.
     * @param helpKey a valid help ID identifying a particular help page. See
     * your help set *.jhm file for the list of valid keys.
     *
     * @see com.tplan.robot.CustomHelpBroker
     */
    public void setHelpId(JComponent component, String helpKey);

    /**
     * Show the Online Help (OLH) window and display the page associated with
     * a particular help key (help ID).
     *
     * @param helpKey a help key (also called help ID)
     * @param owner window owner, must be a top level GUI component like
     * Frame/JFrame or Dialog/JDialog.
     */
    public void showHelpDialog(String helpKey, Object owner);
}
