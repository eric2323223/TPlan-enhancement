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
package com.tplan.robot.remoteclient.rfb.encoding;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.plugin.DependencyMissingException;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.plugin.PluginDependency;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.RemoteDesktopClientFactory;
import com.tplan.robot.util.Utils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Base class for RFB image encodings.
 * @product.signature
 */
public abstract class AbstractEncoding implements Encoding, Plugin {

    protected int lastUpdateMessageSize = -1;
    protected long lastUpdateProcessingTime = -1;

    public String getVendorName() {
        return ApplicationSupport.APPLICATION_NAME;
    }

    public String getSupportContact() {
        return ApplicationSupport.APPLICATION_SUPPORT_CONTACT;
    }

    public int[] getVersion() {
        return Utils.getVersion();
    }

    public Class getImplementedInterface() {
        return Encoding.class;
    }

    public boolean requiresRestart() {
        return false;
    }

    public String getVendorHomePage() {
        return ApplicationSupport.APPLICATION_HOME_PAGE;
    }

    public Date getDate() {
        return Utils.getReleaseDate();
    }

    public int[] getLowestSupportedVersion() {
        return Utils.getVersion();
    }

    public String getMessageBeforeInstall() {
        return null;
    }

    public String getMessageAfterInstall() {
        return null;
    }

    /**
     * Get the plugin code.
     * @return plugin code. It is just the numeric code returned
     * by {@link #getEncodingCode()} converted to String.
     */
    public String getCode() {
        return getEncodingCode().toString();
    }

    /**
     * Check whether all dependencies are installed. As encodings are RFB/VNC specific,
     * the method throws a DependencyMissingException if no RFB client is installed.
     *
     * @param manager plugin manager instance.
     * @throws com.tplan.robot.plugin.DependencyMissingException when one or more required dependencies is not installed.
     */
    public void checkDependencies(PluginManager manager) throws DependencyMissingException {
        if (!RemoteDesktopClientFactory.getInstance().getSupportedProtocols().contains(RemoteDesktopClientFactory.PROTOCOL_RFB)) {
            List<PluginDependency> l = new ArrayList();
            PluginDependency d = new PluginDependency(this, RemoteDesktopClientFactory.PROTOCOL_RFB, RemoteDesktopClient.class, null, null);
            l.add(d);
            throw new DependencyMissingException(this, l);
        }
    }

    /**
     * Get the size of the last processed update data. The method returns value
     * of a protected member variable <code>lastUpdateMessageSize</code>. It is
     * up to the superclass to either populate the variable within a call of the
     * {@link Encoding#updateImage(java.awt.image.BufferedImage, java.io.DataInputStream, com.tplan.robot.remoteclient.rfb.PixelFormat, int, int, int, int)}
     * method or override the method with another implementation. The method is
     * intended to provide data for debugging of compression efficiency and
     * monitoring of volume of data transferred from the server to the client.
     *
     * @return size of data in bytes read from the server socket for the last
     * processed image. Encodings which do not wish to provide this data should
     * return the value of -1 which is interpreted as "no data available".
     */
    public int getLastMessageSize() {
        return lastUpdateMessageSize;
    }

    /**
     * Get the processing time of the last update message. The method returns value
     * of a protected member variable <code>lastUpdateProcessingTime</code>. It is
     * up to the superclass to either populate the variable within a call of the
     * {@link Encoding#updateImage(java.awt.image.BufferedImage, java.io.DataInputStream, com.tplan.robot.remoteclient.rfb.PixelFormat, int, int, int, int)}
     * method or override the method with another implementation. The method is
     * intended to provide data for debugging of compression efficiency and
     * monitoring of time spent by the encoding.
     *
     * @return time in milliseconds elapsed by the last call of
     * {@link Encoding#updateImage(java.awt.image.BufferedImage, java.io.DataInputStream, com.tplan.robot.remoteclient.rfb.PixelFormat, int, int, int, int)}.
     * Encodings which do not wish to provide this data should
     * return the value of -1 which is interpreted as "no data available".
     */
    public long getLastMessageProcessingTime() {
        return lastUpdateProcessingTime;
    }
}
