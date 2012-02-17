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
 * Plugin install exception thrown on attempt to install a plugin of the
 * same code as one of the already installed plugins.
 * @product.signature
 */
public class CodeConflictException extends Exception {

    private PluginInfo pluginInfo;
    private PluginInfo installedPluginInfo;
    private static final String msg = ApplicationSupport.getResourceBundle().getString("com.tplan.robot.plugin.codeConflictEx");

    /**
     * Constructor.
     * @param pluginInfo plugin info of the plugin being installed.
     * @param installedPluginInfo plugin info of the already installed plugin.
     */
    public CodeConflictException(PluginInfo pluginInfo, PluginInfo installedPluginInfo) {
        super(MessageFormat.format(msg, pluginInfo.getDisplayName(),
                pluginInfo.getVersionString(),
                installedPluginInfo.getDisplayName(),
                installedPluginInfo.getVersionString()));
        this.pluginInfo = pluginInfo;
        this.installedPluginInfo = installedPluginInfo;
    }

    /**
     * Get plugin info of the plugin which is being installed.
     * @return the pluginInfo plugin info of the plugin being installed.
     */
    public PluginInfo getPluginInfo() {
        return pluginInfo;
    }

    /**
     * Get plugin info of the plugin which is already installed.
     * @return the installedPluginInfo plugin info of the already installed plugin.
     */
    public PluginInfo getInstalledPluginInfo() {
        return installedPluginInfo;
    }
}
