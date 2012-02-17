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
import java.util.List;

/**
 * Plugin factory for objects which implementing the {@link DynamicVariable}
 * interface.
 *
 * @product.signature
 */
public class DynamicVariableFactory extends PluginFactory {

    // This class implements the singleton pattern
    private static DynamicVariableFactory instance;

    // Private constructor follows the singleton pattern
    private DynamicVariableFactory() {}

    /**
     * Get a shared instance of this factory. The method never returns null.
     * @return the instance
     */
    public static DynamicVariableFactory getInstance() {
        if (instance == null) {
            instance = new DynamicVariableFactory();
        }
        return instance;
    }

    /**
     * Get a list of available dynamic variable names (plugin codes).
     * @return list of variable names.
     */
    public List<String> getVariableNames() {
        List<String> l = getAvailablePluginCodes(DynamicVariable.class);
        return l;
    }

    /**
     * Get a dynamic variable object.
     * @param variableName variable name.
     * @return dynamic variable object.
     */
    public DynamicVariable getVariable(String variableName) {
        return (DynamicVariable)getPluginByCode(variableName, DynamicVariable.class);
    }
}
