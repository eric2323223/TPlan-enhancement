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
package com.tplan.robot.remoteclient;

import com.tplan.robot.plugin.PluginFactory;
import com.tplan.robot.util.Utils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Desktop client factory is central point of instantiation of desktop clients and
 * makes it possible to deliver clients as standalone plugins.
 *
 * @product.signature
 */
public class RemoteDesktopClientFactory extends PluginFactory {

    // This class implements the singleton pattern
    private static RemoteDesktopClientFactory instance;
    /**
     * Constant for Remote Frame Buffer protocol ({@value}).
     */
    public static final String PROTOCOL_RFB = "RFB";
    /**
     * Constant for Remote Desktop Protocol ({@value}).
     */
    public static final String PROTOCOL_RDP = "RDP";
    /**
     * Constant for native Java client ({@value}).
     */
    public static final String PROTOCOL_JAVA = "JAVA";
    private List<RemoteDesktopServerListener> serverListeners = new ArrayList();
    private List<RemoteDesktopClientListener> clientListeners = new ArrayList();

    private RemoteDesktopClientFactory() {
    }

    /**
     * Get a desktop client for a particular protocol.
     * @param protocol protocol identifier. Supported values are: {@value #PROTOCOL_RFB}.
     * There might be more clients delivered as plugins.
     * @return a new instance of client able to handle communication in the selected protocol.
     */
    public RemoteDesktopClient getClient(String protocol) {
        if (protocol == null) {
            protocol = PROTOCOL_RFB;  // Default protocol is RFB
        }
        protocol = protocol.toUpperCase();
        RemoteDesktopClient client = (RemoteDesktopClient) getPluginByCode(protocol, RemoteDesktopClient.class);
        if (client != null) {
            for (RemoteDesktopServerListener l : serverListeners) {
                client.addServerListener(l);
            }
            for (RemoteDesktopClientListener l : clientListeners) {
                client.addClientListener(l);
            }
        }
        return client;
    }

    /**
     * Get a desktop client for a particular connection URI. The method parses
     * the protocol from the URI, creates a clien for that protocol and initializes
     * it with any other URI components like host and port.
     * @param uri connection URI in form of &lt;protocol&gt;://&lt;host&gt;:&lt;port&gt;
     * @return a new instance of client able to handle communication in the selected protocol.
     */
    public RemoteDesktopClient getClientForURI(String uri) {
        try {
            URI u = Utils.getURI(uri);
            RemoteDesktopClient client = getClient(u.getScheme().toLowerCase());
            if (client != null) {
                Map<String, Object> m = new HashMap();
                m.put(RemoteDesktopClient.LOGIN_PARAM_URI, u.toString());
                client.setLoginParams(m);
                for (RemoteDesktopServerListener l : serverListeners) {
                    client.addServerListener(l);
                }
                for (RemoteDesktopClientListener l : clientListeners) {
                    client.addClientListener(l);
                }
                return client;
            }
        } catch (URISyntaxException ex) {
        }
        return null;
    }

    /**
     * Get list of supported protocol identifiers (in upper case).
     * @return list of supported protocols.
     */
    public List<String> getSupportedProtocols() {
        return getAvailablePluginCodes(RemoteDesktopClient.class);
    }

    /**
     * Get shared instance of this desktop client factory.
     * @return shared factory instance.
     */
    public static RemoteDesktopClientFactory getInstance() {
        if (instance == null) {
            instance = new RemoteDesktopClientFactory();
        }
        return instance;
    }

    /**
     * Add a remote desktop server listener to all clients which will be created
     * by this factory.
     * @param listener a remote desktop server listener.
     */
    public synchronized void addRemoteDesktopServerListener(RemoteDesktopServerListener listener) {
        serverListeners.add(listener);
    }

    /**
     * Remove remote desktop server listener from the list of listeners. The
     * method will cause the factory to stop adding this listener to the newly
     * created clients. It will not remove the listener from clients created
     * in the past.
     * @param listener a remote desktop server listener.
     */
    public synchronized void removeRemoteDesktopServerListener(RemoteDesktopServerListener listener) {
        serverListeners.remove(listener);
    }

    /**
     * Add a remote desktop client listener to all clients which will be created
     * by this factory.
     * @param listener a remote desktop client listener.
     */
    public synchronized void addRemoteDesktopClientListener(RemoteDesktopClientListener listener) {
        clientListeners.add(listener);
    }

    /**
     * Remove remote desktop client listener from the list of listeners. The
     * method will cause the factory to stop adding this listener to the newly
     * created clients. It will not remove the listener from clients created
     * in the past.
     * @param listener a remote desktop client listener.
     */
    public synchronized void removeRemoteDesktopClientListener(RemoteDesktopClientListener listener) {
        clientListeners.remove(listener);
    }
}
