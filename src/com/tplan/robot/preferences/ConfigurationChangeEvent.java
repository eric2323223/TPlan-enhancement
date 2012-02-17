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
 * Created by IntelliJ IDEA.
 * User: root
 * Date: Apr 4, 2005
 * Time: 12:26:39 PM
 * To change this template use File | Settings | File Templates.
 */
import java.beans.PropertyChangeEvent;

/**
 * <p>An event that gets fired when a value in the user
 * configuration changes. It references the configuration as the source of the
 * event, name of the changed parameter and its old and new value.
 *
 * <p>These events are used in
 * {@link com.tplan.robot.preferences.ConfigurationChangeListener ConfigurationChangeListener}
 * interface.
 * @product.signature
 */

public class ConfigurationChangeEvent extends PropertyChangeEvent {
    
    /**
     * Constructs a new <code>ConfigurationChangeEvent</code>.
     *
     * @param source  The bean that fired the event.
     * @param parameterName  The programatic name of the property
     *		that was changed.
     * @param oldValue  The old value of the property.
     * @param newValue  The new value of the property.
     */
    public ConfigurationChangeEvent(Object source, String parameterName,
            Object oldValue, Object newValue) {
        super(source, parameterName, oldValue, newValue);
    }
}
