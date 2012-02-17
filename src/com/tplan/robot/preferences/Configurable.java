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
package com.tplan.robot.preferences;

import java.util.List;

/**
 * Interface for objects which have configurable parameters stored 
 * in or intend to store to the application configuration.
 * 
 * @product.signature
 */
public interface Configurable {

    /**
     * <p>If an object implementing this interface is a plugin (i.e. implements also
     * the Plugin interface) and is instantiated through a supported plugin
     * factory, the Plugin Manager calls this method right after an instance
     * of this object is created.</p>
     *
     * <p>Custom objects which do not already have their configuration parameters in
     * the default configuration file should take advantage of this method
     * to store their configuration into the shared User Configuration instance.
     * It is recommended to call the <code>UserConfiguration.saveConfiguration()</code>
     * method in the end to save the configuration to the hard drive.</p>
     *
     * <p>Objects wishing to be notified of changes of configuration parameters
     * should implement the ConfigurationChangeListener interface and register
     * with the UserConfiguration instance through the
     * <code>addConfigurationChangeListener()</code> method.</p>
     *
     * @param cfg global shared instance of user configuration preloaded with
     * parameters from the default and user configuration files.
     */
    public void setConfiguration(UserConfiguration cfg);

    /**
     * Get metadata of displayable/editable configurable parameters.
     * This method should declare a list of metadata for all configurable
     * parameters which may be editable in the GUI. If the returned list is not
     * null and contains at least one parameter, it gets picked up by the Preferences
     * dialog which creates a panel with GUI components allowing to edit
     * the declared configuration parameters.
     *
     * @return a list of metadata for all public editable configuration parameters.
     */
    public List<Preference> getPreferences();

}
