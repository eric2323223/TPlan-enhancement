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

import java.util.Date;

/**
 * Interface declaring methods of a {@product.name} plugin. See the {@link PluginManager}
 * documentation for more information on the {@product.name} Plugin Framework and general
 * requirements on plugins.
 * @product.signature
 */
public interface Plugin {

    /**
     * <p>Get plugin code. This string serves as an identifier of the functionality
     * delivered by the plugin. For example, desktop client plugins return protocol
     * name like "RFB" or "RDP" as their code.</p>
     *
     * <p>Plugin code is used by pluggable instance factories to identify
     * a particular plugin. It may be used to replace internal plugins with
     * third party code. For
     * example if you develop a class which implements this and the
     * <code>com.tplan.robot.remoteclient.RemoteDesktop</code> interfaces and the
     * <code>getCode()</code> method returns "RFB", it will replace the internal
     * implementation of the RFB (VNC) client.</p>
     *
     * @return plugin code (identifier). The name must not be null.
     */
    String getCode();

    /**
     * Get short plugin name to be displayed in the GUI.
     *
     * @return short plugin name.
     */
    String getDisplayName();

    /**
     * Get plugin description to be displayed in the GUI.
     *
     * @return plugin description. May be null.
     */
    String getDescription();

    /**
     * Get vendor (provider) name to be displayed in the GUI.
     *
     * @return vendor name. May be null.
     */
    String getVendorName();

    /**
     * Get the vendor home page. If the contact is a valid HTTP link like "http://&lt;link&gt;",
     * the application may follow it with appropriate program (typically
     * web browser).
     * @return vendor home page URL.
     */
    String getVendorHomePage();

    /**
     * Get support contact. If the contact is a valid mail link like
     * "mailto:&lt;mailaddress&gt;" or an HTTP link like "http://&lt;link&gt;",
     * the application may follow it with appropriate program (mail client or
     * web browser).
     *
     * @return support contact. May be null or a text to be displayed by the GUI
     * or a valid URI.
     */
    String getSupportContact();

    /**
     * Get plugin version in form of an integer array. Major version numbers are
     * first. For example, version 1.2.3 should be represented as <code>new int[] { 1, 2, 3 }</code>.
     * 
     * @return version number.
     */
    int[] getVersion();

    /**
     * <p>Get unique ID associated with the plugin. The plugin manager uses the ID
     * together with the version string to identify whether a plugin is already
     * installed and whether a newer version of the same plugin is available.</p>
     *
     * <p>The unique ID in fact identifies a particular plugin delivered by
     * a particular vendor. Plugin developers are recommended to choose an ID and keep it constant
     * for all versions of one particular plugin. The ID is never displayed in
     * the GUI so it doesn't have to be a readable text. To avoid conflicts with other
     * vendors it is recommended to elaborate vendor or author name and feature
     * description into the ID, for example "custom RFB client implemented by John Doe".</p>
     * 
     * @return unique plugin ID.
     */
    String getUniqueId();

    /**
     * Get plugin release date.
     *
     * @return plugin release date.
     */
    Date getDate();

    /**
     * Get Class of the exposed functional interface that this plugin implements. For
     * example remote desktop clients return <code>com.tplan.robot.remoteclient.RemoteDesktopClient.class</code>.
     *
     * @return class instance of the implemented functional interface.
     */
    Class getImplementedInterface();

    /**
     * Indicate whether installation of this plugin requires application restart.
     * If it returns true, users are asked to restart after plugin installation via GUI.
     *
     * @return true if installation of the plugin requires application restart,
     * false otherwise.
     */
    boolean requiresRestart();

    /**
     * Get the lowest required version of {@product.name}. If user attempts to install
     * the plugin on a lower version, an error is reported.
     * @return the lowest {@product.name} version supported by this plugin.
     */
    int[] getLowestSupportedVersion();

    /**
     * Get text of a message to be displayed before installation of this plugin.
     * It may contain any relevant user information.
     * @return message to be displayed before installation.
     */
    String getMessageBeforeInstall();

    /**
     * Get text of a message to be displayed after installation of this plugin.
     * It may contain any relevant user information.
     * @return message to be displayed after installation.
     */
    String getMessageAfterInstall();

    /**
     * Check whether the current product installation contains all dependencies
     * (other plugins) required to install this plugin. This method is called
     * before the plugin is installed and it should throw a {@link DependencyMissingException}
     * if one or more dependencies are missing.
     * 
     * @param manager shared instance of the plugin manager.
     * @throws DependencyMissingException when one or more dependencies requested by this plugin is missing.
     */
    void checkDependencies(PluginManager manager) throws DependencyMissingException;
}
