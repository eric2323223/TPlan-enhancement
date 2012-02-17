/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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

/**
 * Plugin install exception thrown on attempt to install a plugin which requires
 * higher application version than the currently installed one.
 * @product.signature
 */
public class UnsupportedVersionException extends Exception {

    private PluginInfo pluginInfo;
    private int[] currentVersion;
    private static final String msg = ApplicationSupport.getResourceBundle().getString("com.tplan.robot.plugin.unsupportedVersionEx");

    /**
     * Constructor.
     * @param pluginInfo plugin info of the plugin being installed.
     * @param currentVersion application version.
     */
    public UnsupportedVersionException(PluginInfo pluginInfo, int[] currentVersion) {
        super(MessageFormat.format(msg, pluginInfo.getDisplayName(),
                pluginInfo.getVersionString(),
                ApplicationSupport.APPLICATION_NAME,
                PluginInfo.getVersionString(pluginInfo.getLowestSupportedVersion()),
                ApplicationSupport.APPLICATION_VERSION));
        this.pluginInfo = pluginInfo;
        this.currentVersion = currentVersion;
    }

    /**
     * Get plugin info of the plugin which is being installed.
     * @return the pluginInfo plugin info of the plugin being installed.
     */
    public PluginInfo getPluginInfo() {
        return pluginInfo;
    }

    /**
     * Get application version. To get the lowest version required by the
     * installed plugin use {@link #getPluginInfo()}.{@link PluginInfo#getLowestSupportedVersion()}
     * @return version of the current application.
     */
    public int[] getApplicationVersion() {
        return currentVersion;
    }
}
