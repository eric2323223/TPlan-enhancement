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

import com.tplan.robot.ApplicationSupport;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/**
 * Plugin install exception thrown on attempt to install a plugin while one of
 * the dependencies is not installed.
 * @product.signature
 */
public class DependencyMissingException extends Exception {

    private Plugin plugin;
    private static final String msg = ApplicationSupport.getResourceBundle().getString("com.tplan.robot.plugin.missingDependencyEx");
    private List<PluginDependency> dependencies;

    /**
     * Constructor with the default exception message.
     * @param plugin plugin info of the plugin being installed.
     * @param dependencies list of missing dependencies required by the plugin.
     */
    public DependencyMissingException(Plugin plugin, List<PluginDependency> dependencies) {
        super(MessageFormat.format(msg, depsToString(dependencies)));
        this.plugin = plugin;
        this.dependencies = dependencies;
    }

    /**
     * Constructor allowing to set a custom exception message text.
     * @param message custom exception message text.
     * @param plugin plugin info of the plugin being installed.
     * @param dependencies list of missing dependencies required by the plugin.
     */
    public DependencyMissingException(String message, Plugin plugin, List<PluginDependency> dependencies) {
        super(message);
        this.plugin = plugin;
        this.dependencies = dependencies;
    }
    /**
     * Get plugin info of the plugin which is being installed.
     * @return the pluginInfo plugin info of the plugin being installed.
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Convert a list of dependencies to a displayable string. The method is likely
     * to be extended in the future to search a remote plugin database for availability
     * and description of plugins which meet the dependency criteria.
     * @param dependencies
     * @return
     */
    private static String depsToString(List<PluginDependency> dependencies) {
        String t = ApplicationSupport.getString("com.tplan.robot.plugin.dependencyTemplate");
        String any = ApplicationSupport.getString("com.tplan.robot.plugin.dependencyUnspecified");
        String s = "";
        String groupName;
        Map<Class, String> m = PluginManager.getInstance().getInterfaceMap();
        for (PluginDependency d : dependencies) {
            groupName = m.get(d.getDependencyInterface());
            s += MessageFormat.format(t, d.getDependencyCode(),
                    groupName == null ? d.getDependencyInterface().getName() : groupName,
                    d.getDependencyVersion() == null ? any : PluginInfo.getVersionString(d.getDependencyVersion()),
                    d.getDependencyUniqueId() == null ? any : d.getDependencyUniqueId()) + "\n";
        }
        return s;
    }

    /**
     * Get list of missing dependencies.
     * @return list of missing dependencies.
     */
    public List<PluginDependency> getDependencies() {
        return dependencies;
    }
}
