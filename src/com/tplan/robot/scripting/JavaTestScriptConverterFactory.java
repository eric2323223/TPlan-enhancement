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
import com.tplan.robot.plugin.PluginInfo;
import java.util.List;

/**
 * Pluggable factory of converters from the proprietary scripting language to
 * Java.
 * @product.signature
 */
public class JavaTestScriptConverterFactory extends PluginFactory {

    // Shared factory instance
    private static JavaTestScriptConverterFactory instance;

    private JavaTestScriptConverterFactory() {}

    /**
     * Get the shared factory instance.
     * @return the shared instance.
     */
    public static JavaTestScriptConverterFactory getInstance() {
        if (instance == null) {
            instance = new JavaTestScriptConverterFactory();
        }
        return instance;
    }

    /**
     * Get a Java converter of the specified code.
     *
     * @param code plugin code.
     * @return Java converter instance or null if there's no converter plugin
     * associated with this code.
     */
    public JavaTestScriptConverter getConverter(String code) {
        return (JavaTestScriptConverter)getPluginByCode(code, JavaTestScriptConverter.class);
    }

    public List<String> getAvailableCodes() {
        return getAvailablePluginCodes(JavaTestScriptConverter.class);
    }
    public List<PluginInfo> getAvailablePlugins() {
        return getAvailablePluginInfos(JavaTestScriptConverter.class);
    }
}
