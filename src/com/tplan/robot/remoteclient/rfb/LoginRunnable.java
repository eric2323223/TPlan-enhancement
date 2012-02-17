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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.util.Utils;
import java.text.MessageFormat;

/**
 * <p>A wrapper around the desktop connection procedure. It handles verification
 * of the environment (e.g. when user tries to run the application on the same Windows
 * desktop with VNC server) and eventual error messages. When the class fails to
 * connect or login to the desktop, it sets its status flag and saves
 * the exception. Reporting of the error is then on the module using this 
 * instance - while a GUI module typically displays the error in a window, the CLI
 * one prints out the error to the console window.</p>
 * 
 * @product.signature
 */
public class LoginRunnable implements Runnable, RfbConstants, ConfigurationKeys {

    private RemoteDesktopClient client;

    private boolean loginFailed = false;

    private Throwable connectThrowable;

    private UserConfiguration cfg;

    private boolean throwExceptions = false;

    /**
     * Constructor.
     * @param client a desktop client instance.
     * @param cfg an instance of UserConfiguration, typically obtained through the 
     * {@link com.tplan.robot.preferences.UserConfiguration#getInstance() UserConfiguration.getInstance()} method.
     */
    public LoginRunnable(RemoteDesktopClient client, UserConfiguration cfg) {
        this.client = client;
        this.cfg = cfg;
    }

    /**
     * Implementation of the <code>java.lang.Runnable</code> interface. This method 
     * gets executed when the encapsulating thread is started. It verifies 
     * the environment and invokes the {@link com.tplan.robot.remoteclient.RemoteDesktopClient#connect()} method
     * to connect to the specified desktop. If the connection
     * fails, an error is printed out to the console and the class sets its <code>loginFailed</code>
     * flag to true to indicate that the login failed.
     * </p>
     */
    public void run() {
        String host = client.getHost();
        int port = client.getPort();

        // Bug 2960634 fix - Local connection warning message can't be switched off
        Integer displayWinMsg = cfg.getInteger("warning.displayWinLocalWarning");
        if (client.getProtocol().equalsIgnoreCase("rfb") && (displayWinMsg == null || displayWinMsg.intValue() < 0)) {
            boolean isWinLocalWarningNeeded = Utils.isWindowsLocalhost(host, port);

            if (isWinLocalWarningNeeded) {
                System.out.println(ApplicationSupport.getResourceBundle().getString("cli.Warning")+
                        "\n" + MessageFormat.format(ApplicationSupport.getResourceBundle().getString("com.tplan.robot.gui.LoginDlg.localWindowsWarning"), ApplicationSupport.APPLICATION_NAME));
            }
        }
        if (client.isConnected()) {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

//        String server = client.getProtocol() + "://"+host;
//        if (port >= 0) {
//            server += ":" + port;
//        }
        cfg.updateListOfRecents(client.getConnectString(), IO_RECENT_SERVERS, MAX_DYNAMIC_MENU_ITEMS, false);
        UserConfiguration.saveConfiguration();

        try {
            client.connect();
        } catch (PasswordRequiredException ex) {
            handleConnectError(ex);
        } catch (Exception ex) {
            handleConnectError(ex);
        }
    }

    /**
     * Get the exception which was thrown during an attempt to connect to 
     * the desktop. If the connection succeeded, the method returns null.
     * @return exception thrown during attempt to connect to the desktop, null
     * if there was no problem.
     */
    public Throwable getConnectThrowable() {
        return connectThrowable;
    }

    /**
     * Handle an exception from the connection/login process.
     * @param ex exception thrown during attempt to connect..
     */
    private void handleConnectError(Throwable ex) {
        connectThrowable = ex;
        loginFailed = true;
        try {
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(ex.getMessage());
    }

    /**
     * Indicate whether connection to the desktop succeeded
     * or failed.
     * @return true if the connection or login to the desktop failed, false if
     * it succeeded or the thread hasn't been run yet.
     */
    public boolean isLoginFailed() {
        return loginFailed;
    }

    /**
     * @return the throwExceptions
     */
    public boolean isThrowExceptions() {
        return throwExceptions;
    }

    /**
     * @param throwExceptions the throwExceptions to set
     */
    public void setThrowExceptions(boolean throwExceptions) {
        this.throwExceptions = throwExceptions;
    }
}
