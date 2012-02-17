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
package com.tplan.robot.plugin;

import java.util.List;

/**
 * <p>Base class for plugin factories. To allow loading of plugins implementing
 * a particular interface extend this class and define your own get-
 * methods based on the ones defined by this class.</p>
 *
 * <p>The class is abstract and it is not intended to be instantiated directly.
 * The point is to give developers an option to hide the functional interface from
 * the method arguments and customize the get- methods, for example with
 * availability checks and error handling.</p>
 *
 * <p>An example: You want to enable loading of plugins which implement
 * <code>com.myproject.MyPlugin</code> interface. If the interface is already known
 * to the {@link PluginManager plugin manager}, a simple corresponding plugin
 * factory may look like:
 *
 * <blockquote>
 * <pre>
 * package com.myproject;
 *
 * import java.util.List;
 * import {@product.package}.plugin.*;
 *
 * public class MyPluginFactory extends PluginFactory {
 *
 *      // Get a plugin by its code.
 *      // @param code a plugin code geturned by the getCode() plugin method.
 *      // @return an instance of the plugin or null if there's no plugin
 *      //   associated with this code.
 *     public MyPlugin getMyPlugin(String code) {
 *         return (MyPlugin)getPluginByCode(code, MyPlugin.class);
 *     }
 * }
 * </pre>
 * </blockquote>
 * @product.signature
 */
public abstract class PluginFactory {

    /**
     * Get a plugin instance by code. The methods uses shared instance of {@link PluginManager}
     * to load and instantiate plugin associated with the argument code.
     *
     * @param code plugin code (string returned by the {@link Plugin#getCode()} method).
     * @param requiredInterface functional interface implemented by the plugin. It must be
     * one of the known exposed functional interfaces. See the {@link PluginManager} class
     * and the XML plugin map documentation for the list of available interfaces.
     * @return a new plugin instance. If there's no plugin associated with the given
     * functional interface and code, the method returns null.
     */
    protected Plugin getPluginByCode(Object code, Class requiredInterface) {
        return PluginManager.getInstance().getPluginInstance(code, requiredInterface);
    }

    /**
     * Get list of available plugin codes for a particular functional interface.
     * @param requiredInterface functional interface implemented by the plugin. It must be
     * one of the known exposed functional interfaces. See the {@link PluginManager} class
     * and the XML plugin map documentation for the list of available interfaces.
     * @return list of available plugin codes.
     */
    protected List<String> getAvailablePluginCodes(Class requiredInterface) {
        return PluginManager.getInstance().getPluginCodes(requiredInterface);
    }

    /**
     * Get list of available plugin info wrappers for a particular functional interface.
     * @param requiredInterface functional interface implemented by the plugin. It must be
     * one of the known exposed functional interfaces. See the {@link PluginManager} class
     * and the XML plugin map documentation for the list of available interfaces.
     * @return list of available plugin info wrappers.
     */
    protected List<PluginInfo> getAvailablePluginInfos(Class requiredInterface) {
        return PluginManager.getInstance().getPlugins(requiredInterface, true);
    }
}
