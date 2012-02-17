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
package com.tplan.robot.remoteclient.rfb;

import com.tplan.robot.preferences.Preference;
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.remoteclient.RemoteDesktopServerListener;
import com.tplan.robot.remoteclient.RemoteDesktopClientListener;
import com.tplan.robot.remoteclient.RemoteDesktopClientEvent;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.preferences.Configurable;
import com.tplan.robot.preferences.ConfigurationChangeEvent;
import com.tplan.robot.preferences.ConfigurationChangeListener;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.capabilities.PointerTransferCapable;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Refresh daemon is a standalone thread preventing the remote RFB desktop
 * from locking of idle sessions.
 *
 * @product.signature
 */
public class RfbRefreshDaemon implements ActionListener, Configurable,
        ConfigurationChangeListener, RemoteDesktopServerListener, RemoteDesktopClientListener, RfbConstants {

    UserConfiguration cfg;
    ScriptManager scriptHandler;
    RemoteDesktopClient rfbClient;
    private boolean enabled = true;
    private int maxIdleTimeSeconds = 5;
    private boolean allowRefreshDuringExecution = false;

    public RfbRefreshDaemon(RemoteDesktopClient rfbClient, ScriptManager scriptHandler, UserConfiguration cfg) {
        if (rfbClient instanceof PointerTransferCapable && ((PointerTransferCapable) rfbClient).isPointerTransferSupported()) {
            this.rfbClient = rfbClient;
            rfbClient.addServerListener(this);
            rfbClient.addClientListener(this);
            this.scriptHandler = scriptHandler;
            setConfiguration(cfg);
        } else {
            setEnabled(false);
        }
    }

    public RfbRefreshDaemon(RemoteDesktopClient rfbClient, ScriptManager scriptHandler) {
        this(rfbClient, scriptHandler, null);
    }
    /**
     * Refresh timer. It is a thread which invokes the refresh daemon
     * at scheduled intervals.
     */
    private Timer refreshTimer;

    private void setUpRefreshDaemon() {
        if (isEnabled()) {
            if (refreshTimer != null) {
                refreshTimer.stop();
            }
            refreshTimer = new Timer(getMaxIdleTimeSeconds() * 1000, this);
            refreshTimer.setRepeats(true);
            refreshTimer.start();
        } else if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }

    /**
     * Implementation of the ActionListener interface. This method gets called by the refresh
     * daemon timer and invokes the <code>writeRefreshDaemonEvent()</code> method.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof Timer) {
//            System.out.println("" + (new Date()) + ": refresh invoked");
            writeRefreshDaemonEvent();
        }
    }

    /**
     * Refresh daemon is a background process which keeps the VNC session alive.
     * If you don't move your mouse or type a key for a long time, the remote server usually runs
     * a screensaver or locks up the desktop after a certain time. Also some VNC servers are known
     * to close the connection after a period of idle time.
     *
     * <p>This method is called by the refresh daemon thread at scheduled intervals. It verifies whether
     * the feature is enabled and it sends two false mouse events to the server - move from the current
     * mouse position by 1 pixel and back.
     *
     * <p>If this behavior causes problems with your automated script, use the application preferences
     * to switch it off.
     */
    private void writeRefreshDaemonEvent() {
        if (scriptHandler != null && scriptHandler.getExecutingTestScripts().size() == 0 && !isAllowRefreshDuringExecution()) {
//          System.out.println("" + (new Date()) + ": refresh canceled because of execution");
            return;
        }
        if (isEnabled() && rfbClient != null && rfbClient.isConnected()) {
            int x = 0, y = 0;
            MouseEvent lastMouseEvent = rfbClient.getLastMouseEvent();
            if (lastMouseEvent != null) {
                x = lastMouseEvent.getX();
                y = lastMouseEvent.getY();
            }
            int dx = x > 1 ? -2 : 2;
            try {
                // Remove this from the client message listeners to avoid restart
                // due to the mouse events we send
                rfbClient.removeClientListener(this);
                Component c = new JPanel();
                MouseEvent e = new MouseEvent(c, MouseEvent.MOUSE_MOVED,
                        System.currentTimeMillis(), MouseEvent.NOBUTTON, x + dx, y, 0, false);
                ((PointerTransferCapable)rfbClient).sendPointerEvent(e, false);
                e = new MouseEvent(c, MouseEvent.MOUSE_MOVED,
                        System.currentTimeMillis(), MouseEvent.NOBUTTON, x, y, 0, false);
                ((PointerTransferCapable)rfbClient).sendPointerEvent(e, false);
                rfbClient.addClientListener(this);
            } catch (Exception ex) {
            }
        }
    }

    public void destroy() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer.removeActionListener(this);
            refreshTimer = null;
        }
        rfbClient.removeServerListener(this);
        rfbClient.removeClientListener(this);
        if (cfg != null) {
            cfg.removeConfigurationListener(this);
            cfg = null;
        }
        scriptHandler = null;
        rfbClient = null;
    }

    public void setConfiguration(UserConfiguration configuration) {
        if (this.cfg != null) {
            cfg.removeConfigurationListener(this);
        }
        this.cfg = configuration;
        if (configuration != null) {
            configuration.addConfigurationListener(this);
        }
        setUpRefreshDaemon();
    }

    public void configurationChanged(ConfigurationChangeEvent evt) {
        if (evt.getPropertyName().startsWith("rfb.RefreshDaemon")) {
            setUpRefreshDaemon();
        }
    }

    public void serverMessageReceived(RemoteDesktopServerEvent evt) {
        if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_DISCONNECTED_EVENT || evt.getMessageType() == RemoteDesktopServerEvent.SERVER_CONNECTED_EVENT) {
            setUpRefreshDaemon();
        }
    }

    public void clientMessageSent(RemoteDesktopClientEvent evt) {
        if (evt.getMessageType() == RemoteDesktopClientEvent.CLIENT_POINTER_EVENT || evt.getMessageType() == RemoteDesktopClientEvent.CLIENT_KEY_EVENT) {
            if (refreshTimer != null) {
//                System.out.println("" + (new Date()) + ": refresh timer restarted");
                refreshTimer.restart();
            }
        }
    }

    public boolean isEnabled() {
        if (cfg != null) {
            Boolean b = cfg.getBoolean("rfb.RefreshDaemon.enable");
            return b == null ? allowRefreshDuringExecution : b;
        }
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (cfg != null) {
            throw new IllegalStateException("Parameter 'enable' of the refresh daemon is currently controlled through user configuration. " +
                    "Use setConfiguration(null) to allow manual configuration.");
        }
        setUpRefreshDaemon();
        this.enabled = enabled;
    }

    public int getMaxIdleTimeSeconds() {
        if (cfg != null) {
            Number n = cfg.getInteger("rfb.RefreshDaemon.maxIdleTimeInSec");
            return n == null ? maxIdleTimeSeconds : n.intValue();
        }
        return maxIdleTimeSeconds;
    }

    public void setMaxIdleTimeSeconds(int maxIdleTimeSeconds) {
        if (cfg != null) {
            throw new IllegalStateException("Parameter 'maxIdleTimeSeconds' of the refresh daemon is controlled through user configuration. " +
                    "Use setConfiguration(null) to allow manual configuration.");
        }
        this.maxIdleTimeSeconds = maxIdleTimeSeconds;
        setUpRefreshDaemon();
    }

    public boolean isAllowRefreshDuringExecution() {
        if (cfg != null) {
            Boolean b = cfg.getBoolean("rfb.RefreshDaemon.enableDuringExecution");
            return b == null ? allowRefreshDuringExecution : b;
        }
        return allowRefreshDuringExecution;
    }

    public void setAllowRefreshDuringExecution(boolean allowRefreshDuringExecution) {
        if (cfg != null) {
            throw new IllegalStateException("Parameter 'enableDuringExecution' of the refresh daemon is controlled through user configuration. " +
                    "Use setConfiguration(null) to allow manual configuration.");
        }
        this.allowRefreshDuringExecution = allowRefreshDuringExecution;
    }

    public List<Preference> getPreferences() {
        // TBD - this class is not a plugin so we don't have to return anything
        return null;
    }
}
