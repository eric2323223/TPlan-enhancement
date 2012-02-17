/*
 * T-Plan Robot, automated testing tool based on remote desktop technologies.
 * Copyright (C) 2009-2011 T-Plan Limited (http://www.t-plan.co.uk),
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
package com.tplan.robot;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.JComponent;

/**
 * Interface declaring methods of a dispatcher able to map help keys onto help
 * topics and display them in an appropriate GUI component (such as the JavaHelp
 * window or a web/HTML browser).
 *
 * @product.signature
 */
public interface HelpDispatcher {
    /**
     * Initialize the help dispatcher for the given GUI component. This method will
     * be called once when the main GUI is created.
     *
     * @param owner default owner for the help window (this makes sense only when
     * the help window is a Java one).
     */
    void init(Component owner);

    /**
     * Set help ID of a GUI component to enable contextual help. This method should
     * be implemented only if contextual help is enabled (the {@link #isContextualHelpSupported()}
     * method returns true). The contextual help is a feature allowing to set on the
     * "Contextual Help" tool bar button and then click onto a GUI component to display
     * its help topic.
     * @param component a GUI component.
     * @param helpId help ID to be associated with the GUI component.
     */
    void setHelpId(JComponent component, String helpId);

    /**
     * Show the help topic associated with the specified ID (help key).
     * @param helpID topic ID (help key).
     * @param owner optional window owner.
     * @param modal modality mode (makes sense only when
     * the help window is a Java one).
     */
    void show(String helpID, Component owner, Boolean modal);

    /**
     * Define whether the contextual help is supported by the dispatcher or not.
     * The contextual help is a feature allowing to set on the
     * "Contextual Help" tool bar button and then click onto a GUI component to display
     * its help topic.
     * @return true if contextual help is supported, false if not.
     */
    boolean isContextualHelpSupported();

    /**
     * Indicates whether the dispatcher has succeeded to find any help topics or not.
     * GUI components should call this method and disable their Help buttons if
     * it returns false.
     * @return true if help is available, false if not.
     */
    boolean isHelpAvailable();

    /**
     * Initialize the default set of contextual help IDs. This method should
     * be implemented only if contextual help is enabled (the {@link #isContextualHelpSupported()}
     * method returns true).
     */
    void initComponentHelpIds();

    /**
     * Show contextual help of a component. This method should
     * be implemented only if contextual help is enabled (the {@link #isContextualHelpSupported()}
     * method returns true).
     * @param e action event originating from the "Contextual Help" tool bar button.
     */
    void contextShow(ActionEvent e);
}
