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

/**
 * Dependency is a class which describes dependency of a plugin (called "dependent plugin")
 * on another plugin ("dependency plugin"). When a plugin is being installed,
 * the Plugin Manager invokes its {@link Plugin#checkDependencies(com.tplan.robot.plugin.PluginManager)}
 * method. If one or more of the required dependencies are not installed, the plugin
 * is supposed to construct dependency criteria represented by instances of this class
 * and throw them through a {@link DependencyMissingException} back to the Plugin Manager.
 * 
 * @product.signature
 */
public class PluginDependency {

    private String code;
    private String uniqueId;
    private int[] version;
    private Plugin plugin;
    private Class depInterface;

    /**
     * Constructor with arguments referring to attributes of a plugin which
     * is required as a dependency.
     *
     * @param dependentPlugin source of the dependency, a plugin which is depending on
     * certain other plugin functionality described by the other parameters.
     * @param dependencyCode dependency plugin code. This parameter must not be null.
     * @param dependencyInterface dependency plugin interface. This parameter must not be null.
     * @param dependencyUniqueId unique ID desired unique ID of the dependency plugin (optional).
     * It allows to specify dependency on a plugin from particular provider (vendor). If null
     * is provided, it is taken as that the dependent plugin can do with a dependency one from any provider.
     * @param dependencyVersion desired version of the dependency plugin (optional). If null
     * is provided, it is taken as that the dependent plugin can do with any version
     * of the dependency one.
     */
    public PluginDependency(Plugin dependentPlugin, String dependencyCode,
            Class dependencyInterface, String dependencyUniqueId, int[] dependencyVersion) {
        if (dependencyCode == null || dependencyInterface == null) {
            throw new IllegalArgumentException("Dependency plugin code and interface must not be null.");
        }
        this.code = dependencyCode;
        this.version = dependencyVersion;
        this.uniqueId = dependencyUniqueId;
        this.depInterface = dependencyInterface;
    }

    /**
     * Get code of the dependency plugin.
     * @return code of the dependency plugin. As this parameter is mandatory, the method never returns null.
     */
    public String getDependencyCode() {
        return code;
    }

    /**
     * Get required unique ID of the dependency plugin.
     * @return unique ID of the dependency plugin. As this parameter is optional, the method may return null.
     */
    public String getDependencyUniqueId() {
        return uniqueId;
    }

    /**
     * Get required version of the dependency plugin.
     * @return version of the dependency plugin. As this parameter is optional, the method may return null.
     */
    public int[] getDependencyVersion() {
        return version;
    }

    /**
     * Get the dependent plugin (source of this dependency).
     * @return dependent plugin.
     */
    public Plugin getDependentPlugin() {
        return plugin;
    }

    /**
     * Get the exposed functional interface implemented by the dependency plugin.
     * @return functional interface of the dependency plugin. As this parameter is mandatory, the method never returns null.
     */
    public Class getDependencyInterface() {
        return depInterface;
    }
}
