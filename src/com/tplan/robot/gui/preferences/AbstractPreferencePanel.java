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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.preferences.UserConfiguration;
import javax.swing.*;
import javax.swing.event.*;
import java.beans.*;
import java.text.MessageFormat;
import java.util.Vector;

/**
 * Preference panel base class. A preference panel is a container which displays
 * a set of user preferences. It gets displayed in the right
 * part of the {@link PreferenceDialog} when its associated tree node gets
 * selected.
 * @product.signature
 */
public abstract class AbstractPreferencePanel extends JPanel implements PreferencePanel {

    /** 
     * List of property change listeners
     */
    private Vector propertyChangeListeners;
    /** 
     * List of change listeners
     */
    private Vector changeListeners;
    /** 
     * Indicates whether there are any changes in preferences this panel displays
     */
    protected boolean changed = false;
    /** 
     * Indicates whether preference values of displayed in this panel are valid
     */
    protected boolean valid = true;

    /**
     * Load values of preferences displayed by this panel from configuration.
     *
     * @param configuration user configuration.
     */
    public void loadPreferences(Object configuration) {
        changed = false;
    }

    /**
     * Get the values from the panel components and save them into
     * the <code>configuration</code>. This implementation does nothing.
     *
     * @param configuration a user configuration.
     */
    public void savePreferences(Object configuration) {
    }

    /**
     * Get the change status. True indicates that user has changed at least
     * one of the preference values displayed by this panel.
     * @return true when user has changed a preference value, false otherwise.
     */
    public boolean isChanged() {
        return changed;
    }

    /**
     * Get consistency of preference values. True means that all values
     * in the panel are valid while false indicates at least one invalid
     * preference value.
     *
     * @return true when all preferences displayed by the panel are valid, false
     * otherwise.
     */
    public boolean isContentValid() {
        return valid;
    }

    /**
     * Clean Up
     */
    public void destroy() {
    }

    /**
     * Reset content of the component to default values (i.e. discard all
     * user edits).
     * @param configuration a configuration object
     */
    public void resetToDefaults(Object configuration) {
    }

    /**
     * Remove a PropertyChangeListener from the list of registered listeners.
     * @param l a PropertyChangeListener
     */
    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        super.removePropertyChangeListener(l);
        if (propertyChangeListeners != null && propertyChangeListeners.contains(l)) {
            Vector v = (Vector) propertyChangeListeners.clone();

            v.removeElement(l);
            propertyChangeListeners = v;
        }
    }

    /**
     * Add a property change listener to the list of listeners. As each update of a
     * single preference value fires a PropertyChangeEvent, objects depending on user
     * preference values should use this method to receive notifications
     * of configuration changes.
     *
     * @param l a PropertyChangeListener.
     */
    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        super.addPropertyChangeListener(l);
        Vector v = propertyChangeListeners == null ? new Vector(2) : (Vector) propertyChangeListeners.clone();

        if (!v.contains(l)) {
            v.addElement(l);
            propertyChangeListeners = v;
        }
    }

    /**
     * Fire a ChangeEvent which indicates that user has changed a value.
     * 
     * @param e a PropertyChangeEvent where property name is the preference key.
     */
    protected void firePropertyChange(PropertyChangeEvent e) {
        if (propertyChangeListeners != null) {
            Vector listeners = propertyChangeListeners;
            int count = listeners.size();

            for (int i = 0; i < count; i++) {
                ((PropertyChangeListener) listeners.elementAt(i)).propertyChange(e);
            }
        }
    }

    /**
     * Remove a change listener from the list of registered listeners.
     * @param l a ChangeListener
     */
    public synchronized void removeChangeListener(ChangeListener l) {
        if (changeListeners != null && changeListeners.contains(l)) {
            Vector v = (Vector) changeListeners.clone();

            v.removeElement(l);
            changeListeners = v;
        }
    }

    /**
     * Add a change listener to the list of listeners. As each update of a
     * single preference value fires a ChangeEvent, objects maintaining the
     * configuration change status (i.e. changed x unchanged) should use
     * this method to receive notifications of pending user edits.
     *
     * @param l a ChangeListener
     */
    public synchronized void addChangeListener(ChangeListener l) {
        Vector v = changeListeners == null ? new Vector(2) : (Vector) changeListeners.clone();

        if (!v.contains(l)) {
            v.addElement(l);
            changeListeners = v;
        }
    }

    /**
     * Send a change event notifying of a single preference change to to all
     * registered listeners.
     * @param e a ChangeEvent
     */
    protected void fireStateChanged(ChangeEvent e) {
        if (changeListeners != null) {
            Vector listeners = changeListeners;
            int count = listeners.size();

            for (int i = 0; i < count; i++) {
                ((ChangeListener) listeners.elementAt(i)).stateChanged(e);
            }
        }
    }

    public void setShowKeysInToolTips(boolean show) {
    }
}