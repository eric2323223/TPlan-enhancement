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
package com.tplan.robot.scripting;

import com.tplan.robot.plugin.PluginFactory;
import java.util.Arrays;
import java.util.List;

/**
 * Plugin factory for objects which implementing the {@link ExecutionListener}
 * interface.
 *
 * @product.signature
 */
public class ExecutionListenerFactory extends PluginFactory {

    // This class implements the singleton pattern
    private static ExecutionListenerFactory instance;

    // Private constructor follows the singleton pattern
    private ExecutionListenerFactory() {}

    /**
     * Get a shared instance of this factory. The method never returns null.
     * @return the instance
     */
    public static ExecutionListenerFactory getInstance() {
        if (instance == null) {
            instance = new ExecutionListenerFactory();
        }
        return instance;
    }

    /**
     * Get a list of plugin codes sorted in ascending order.
     * @return list of plugin codes sorted in ascending order.
     */
    public List<String> getSortedPluginCodes() {
        List<String> l = getAvailablePluginCodes(ExecutionListener.class);
        String array[] = l.toArray(new String[0]);
        Arrays.sort(array);
        return Arrays.asList(array);
    }

    public ExecutionListener create(String pluginCode) {
        return (ExecutionListener)getPluginByCode(pluginCode, ExecutionListener.class);
    }

}
