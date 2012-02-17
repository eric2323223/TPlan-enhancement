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

/**
 * <p>A listener interface which is to be implemented by all
 * objects that are interested in changes in the user configuration. See also
 * {@link AbstractUserConfiguration#addConfigurationListener AbstractUserConfiguration.addConfigurationListener()}.
 *
 * @see com.tplan.robot.preferences.AbstractUserConfiguration
 * @product.signature
 */

public interface ConfigurationChangeListener extends java.util.EventListener {
    
    /**
     * This method gets called when a configuration parameter is changed.
     * @param evt A ConfigurationChangeEvent object describing the event source 
     * and the parameter that has changed.
     */
    void configurationChanged(ConfigurationChangeEvent evt);
    
}
