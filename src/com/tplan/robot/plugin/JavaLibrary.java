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
 * Interface allowing plugins to declare that they provide a Java API for use
 * in Java test scripts and that their containing JAR file or class path
 * should be put on the on-the-fly Java compiler class path.
 * @product.signature
 */
public interface JavaLibrary {

    /**
     * This key is used to store class path to plugin Java libraries in the
     * system property table (System.getProperty()). The Plugin Manager will
     * populate this path with list of all plugin packages which are Java libraries
     * (meaning they implement this interface and their isLibrary() method returns
     * true). The Java compiler retrieves the path and adds it to the class path
     * whenever a Java test script is being compiled.
     */
    public static final String PLUGIN_LIBRARY_PATH_KEY = "plugin.library.path";

    /**
     * Plugins which are Java libraries should return true. This method allows 
     * to specify dynamically whether the plugin is a library or not. It makes 
     * sense where the plugin delivers both new functionality as well as the API
     * and the user may select (for example in Preferences) whether the API 
     * is used or not. As adding the plugin package to the class path delays 
     * the compiler, it makes sense to switch it of if not used to improve 
     * performance.
     * @return true if the plugin package serves also as a Java library, false 
     * if not.
     */
    public boolean isLibrary();
}
