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

import java.beans.*;
import javax.swing.event.*;

/**
 * Preference panel interface. A preference panel is a container which displays
 * a set of user preferences. It gets displayed in the right
 * part of the {@link PreferenceDialog} when its associated tree node gets
 * selected.
 * @author robert
 */
public interface PreferencePanel {
    
    /**
     * Get consistency of preference values. True means that all values
     * in the panel are valid while false indicates at least one invalid
     * preference value.
     *
     * @return true when all preferences displayed by the panel are valid, false
     * otherwise.
     */
    public boolean isContentValid();
    
    /**
     * Get the change status. True indicates that user has changed at least
     * one of the preference values displayed by this panel.
     * @return true when user has changed a preference value, false otherwise.
     */
    public boolean isChanged();
    
   
    /**
     * Load values from configuration into the component. This is called before
     * the component gets displayed.
     * @param configuration user configuration.
     */
    public void loadPreferences(Object configuration);
    
    /**
     * Save values from the component into configuration. This is called when
     * user hits OK or Apply in the Preferences dialog.
     * @param configuration user configuration.
     */
    public void savePreferences(Object configuration);
    
    /**
     * Reset content of the component to default values (i.e. discard all
     * user edits).
     * @param configuration a configuration object
     */
    public void resetToDefaults(Object configuration);
    
    /**
     * Destroy the component. It is supposed to break references which may be
     * difficult for garbage collector to clean up.
     */
    public void destroy();

    /**
     * Add a change listener to the component. The component is supposed to fire
     * a ChangeEvent to all registered listeners when user updates any preference
     * value displayed by the component. This notifies the preferenced dialog of
     * the change.
     *
     * @param l a ChangeListener.
     */
    public void addChangeListener(ChangeListener l);
    
    /**
     * Add a property change listener to this component. Property change events
     * will be sent to all other components in the preference tree. It may be
     * used to ensure cross validations of dependent preferences
     *
     * @param l a ChangeListener.
     */
    public void addPropertyChangeListener(PropertyChangeListener l);
}