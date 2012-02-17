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

import com.tplan.robot.remoteclient.capabilities.ImageOwner;
import com.tplan.robot.util.Utils;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Base abstract class for desktop clients which provides common infrastructure
 * for server and client listeners.
 *
 * @product.signature
 */
public abstract class AbstractRemoteDesktopClient implements RemoteDesktopClient, ImageOwner {

    /**
     * List of listeners which are interested in received server messages.
     */
    private final List<RemoteDesktopServerListener> rfbServerListeners = new ArrayList<RemoteDesktopServerListener>();
    /**
     * List of listeners which are interested in received server messages.
     */
    private final List<RemoteDesktopClientListener> rfbClientListeners = new ArrayList<RemoteDesktopClientListener>();

    /**
     * Add a server listener to the client. Each registered listener will
     * receive an event whenever a server message is received.
     *
     * @param listener an object implementing the <code>RemoteServerListener</code>
     * interface.
     */
    public synchronized void addServerListener(RemoteDesktopServerListener listener) {
        if (!rfbServerListeners.contains(listener)) {
            rfbServerListeners.add(listener);
        }
    }

    /**
     * Remove an object from the list of server listeners. If the argument
     * object is not registered in the list, the method should do nothing.
     *
     * @param listener an object implementing the <code>RemoteDesktopServerListener</code>
     * interface.
     */
    public synchronized void removeServerListener(RemoteDesktopServerListener listener) {
        if (rfbServerListeners.contains(listener)) {
            rfbServerListeners.remove(listener);
        }
    }

    /**
     * Fire a server event to all registered listeners.
     * @param evt a server client event
     */
    protected synchronized void fireRemoteServerEvent(RemoteDesktopServerEvent evt) {
        // Create a copy of the current list not to be affected by listeners
        // who remove themselves after they receive the event (fix in 2.3)
        List<RemoteDesktopServerListener> l = new ArrayList(rfbServerListeners);
        for (int i = 0; i < l.size(); i++) {
            try {
                l.get(i).serverMessageReceived(evt);
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add a client listener. Each registered listener will
     * receive an event whenever the client sends one of the selected
     * messages to the server, such as PointerEvent, KeyEvent etc.
     *
     * @param listener an object implementing the <code>RemoteDesktopClientListener</code>
     * interface.
     */
    public synchronized void addClientListener(RemoteDesktopClientListener listener) {
        if (!rfbClientListeners.contains(listener)) {
            rfbClientListeners.add(listener);
        }
    }

    /**
     * Remove an object from the list of client listeners. If the argument
     * object is not registered in the list, the method should do nothing.
     *
     * @param listener an object implementing the <code>RemoteDesktopClientListener</code>
     * interface.
     */
    public synchronized void removeClientListener(RemoteDesktopClientListener listener) {
        if (rfbClientListeners.contains(listener)) {
            rfbClientListeners.remove(listener);
        }
    }

    /**
     * Fire a client event to all registered listeners.
     * @param evt a desktop client event
     */
    protected void fireRemoteClientEvent(RemoteDesktopClientEvent evt) {
        // Create a copy of the current list not to be affected by listeners
        // who remove themselves after they receive the event (fix in 2.3)
        List<RemoteDesktopClientListener> l = new ArrayList(rfbClientListeners);
        for (int i = 0; i < l.size(); i++) {
            try {
                l.get(i).clientMessageSent(evt);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Destroy the client and make it ready for garbage collection.
     */
    public void destroy() {
        rfbServerListeners.clear();
        rfbClientListeners.clear();
    }

    public boolean isConnectedTo(String connectString) {
        try {
            URI uri = Utils.getURI(connectString);
            return isConnected() && Utils.isHostEqual(uri.getHost(), uri.getPort(), this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * The client is by default considered passive (the method returns false).
     * Override this method for active clients.
     * @return false by default.
     */
    public boolean isActivelyUpdating() {
        return false;
    }

    /**
     * The client is by default considered dynamic (the method returns false).
     * Override this method for static image clients.
     * @return false by default.
     */
    public boolean isStatic() {
        return false;
    }
}
