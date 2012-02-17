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
package com.tplan.robot;

import com.tplan.robot.remoteclient.rfb.RfbConstants;
import com.tplan.robot.remoteclient.RemoteDesktopServerListener;
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import static com.tplan.robot.remoteclient.RemoteDesktopClient.*;
import com.tplan.robot.remoteclient.RemoteDesktopClientFactory;
import com.tplan.robot.remoteclient.rfb.RfbRefreshDaemon;
import com.tplan.robot.scripting.JavaTestScript;
import com.tplan.robot.scripting.ScriptManagerImpl;
import com.tplan.robot.util.Utils;

import java.awt.Image;
import java.io.*;
import java.text.MessageFormat;
import java.util.Map;

/** 
 * <p>Implementation of a remote desktop automated runnable.</p>
 *
 * <p>The tool is designed as a multithreaded application and it can run multiple
 * automated testing threads. Each such a thread contains its own desktop client
 * ({@link com.tplan.robot.remoteclient.RemoteDesktopClient} instance) and a script manager
 * ({@link com.tplan.robot.scripting.ScriptManager} instance) and it is able to
 * handle an independent automated task, e.g. running of a script on 
 * one server instance. This class implements such a functionality and it
 * can be executed as a thread through the <code>Runnable</code> interface.</p>
 *
 * <p>Each thread can be initiated with different CLI options. See the 
 * {@link com.tplan.robot.ApplicationSupport#APPLICATION_CLI_OPTIONS} array. Thread behavior
 * strongly depends on whether it runs in CLI (invoked with the <code>-n</code> option) 
 * or GUI mode. See the {@link #run()} method for more info. 
 * While there's no limitation on threads running on CLI mode, 
 * there can be just one thread running in GUI mode within one Java Virtual Machine (JVM) 
 * because the current GUI design is not capable to handle multiple frames. This 
 * limitation is clearly documented in the {@link com.tplan.robot.AutomatedRunnable}
 * interface.</p>
 *
 * <p>The application is by default started in a single thread mode. It means that only
 * one thread is created regardless of the CLI/GUI mode flag. The ability of 
 * running multiple threads in CLI mode can be exploited only through custom 
 * Java programs where the application JAR file and it's APIs serve as a library.
 * For an example of such a program see the {@link com.tplan.robot.AutomatedRunnable}
 * interface documentation.</p>
 * 
 * @product.signature
 */
public class AutomatedRunnableImpl implements AutomatedRunnable, RfbConstants, RemoteDesktopServerListener {

    private MainFrame mainFrame;
    private ScriptManager scriptManager;
    private RemoteDesktopClient client;
    private boolean consoleMode = false;
    private CLIModule cliModule;
    private Map clientParams;
    private Map scriptVariables;
    private Map options;
    private Map guiParams;
    private Map scriptingParams;
    private String id;
    private boolean exitOnFinish;
    private PrintStream logWriter;
    private UserConfiguration cfg;
    private Image logo = null;
    private RfbRefreshDaemon daemon;

    /**
     * Constructor. It has just a package access to disable direct instantiation 
     * by users who are supposed to use the 
     * {@link com.tplan.robot.ApplicationSupport#createAutomatedRunnable(java.lang.String, java.lang.String[], java.io.PrintStream, boolean) ApplicationSupport.createAutomatedRunnable()}
     * method which also provides parsing of the CLI options into separate tables.
     *
     * @param id an Id for the thread.
     * @param consoleMode true indicates CLI mode, false means GUI mode.
     * @param exitOnFinish a value of <code>true</code> will cause the thread 
     * to terminate the JVM with the given exit code when the execution finishes. 
     * This flag should be used only for automated script executions in a single 
     * threaded environment (i.e. standard execution in CLI mode).
     * @param logStream a stream to print the log messages to.
     *
     * @param rfbParams a table with RFB specific parameters resulting 
     * from parsing of the options, e.g. host name, port, passwords etc. See the 
     * <code>parseParameters()</code> private method of the {@link com.tplan.robot.ApplicationSupport}
     * class for the code related to parsing into tables.
     *
     * @param scriptVariables a table with script variables passed through 
     * the <code>-v</code> CLI option. See the 
     * <code>parseParameters()</code> private method of the {@link com.tplan.robot.ApplicationSupport}
     * class for the code related to parsing into tables.
     *
     * @param options a table with overriden user options passed through 
     * the <code>-o</code> CLI option. See the 
     * <code>parseParameters()</code> private method of the {@link com.tplan.robot.ApplicationSupport}
     * class for the code related to parsing into tables.
     *
     * @param guiParams a table with GUI specific parameters resulting 
     * from parsing of the CLI options. See the 
     * <code>parseParameters()</code> private method of the {@link com.tplan.robot.ApplicationSupport}
     * class for the code related to parsing into tables.
     *
     * @param scriptingParams a table to store scripting specific parameters resulting 
     * from parsing of the options, e.g. disabling of output (option <code>--nooutput</code>). 
     * See the 
     * <code>parseParameters()</code> private method of the {@link com.tplan.robot.ApplicationSupport}
     * class for the code related to parsing into tables.
     */
    AutomatedRunnableImpl(String id, boolean consoleMode, boolean exitOnFinish, PrintStream logStream,
            Map<String, Object> clientParams, Map<String, String> scriptVariables, Map<String, String> options,
            Map<String, String> guiParams, Map<String, Object> scriptingParams, JavaTestScript testScript) {
        this.id = id;
        this.exitOnFinish = exitOnFinish;
        this.clientParams = clientParams;
        this.scriptVariables = scriptVariables;
        this.options = options;
        this.guiParams = guiParams;
        this.scriptingParams = scriptingParams;
        this.logWriter = logStream;
        UserConfiguration.getInstance().setOverrideTable(options);
        init(consoleMode, logStream, clientParams, scriptVariables, options, guiParams, scriptingParams, testScript);
    }

    AutomatedRunnableImpl(String id, boolean consoleMode, boolean exitOnFinish, PrintStream logStream,
            Map<String, Object> rfbParams, Map<String, String> scriptVariables, Map<String, String> options,
            Map<String, String> guiParams, Map<String, Object> scriptingParams) {
        this(id, consoleMode, exitOnFinish, logStream, rfbParams, scriptVariables, options, guiParams, scriptingParams, null);
    }

    /**
     * Initialize the thread variables and structures. See the constructor doc for description of the 
     * method arguments.
     */
    private void init(boolean consoleMode, PrintStream logStream, Map<String, Object> clientParams,
            Map<String, String> scriptVariables, Map<String, String> options, Map<String, String> guiParams, Map<String, Object> scriptingParams, JavaTestScript testScript) {

        // If we are running in console mode, create a copy of the user configuration because there might be more threads
        cfg = consoleMode ? UserConfiguration.getCopy() : UserConfiguration.getInstance();
        cfg.setOverrideTable(options);

        this.consoleMode = consoleMode;

        // Create a new client
        try {
            if (clientParams.containsKey(LOGIN_PARAM_URI)) {
                client = RemoteDesktopClientFactory.getInstance().getClientForURI(clientParams.get(LOGIN_PARAM_URI).toString());

                if (client != null) {
                    client.setLoginParams(clientParams);
//                    if (client.getImage() != null) {
//                        // Load the product logo and paint the default image
//                        try {
//                            logo = ApplicationSupport.getImageIcon("logo_small.png").getImage();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                        Utils.paintDefaultImage(client.getImage(), logo,
//                                ApplicationSupport.getResourceBundle().getString("com.tplan.robot.defaultImage.textDisconnected"));
//                    }
                    client.addServerListener(this);
                }
            }
        } catch (InternalError err) {
            // This typically happens when the display is not redirected properly.
            Utils.writeLog(logStream, "Error: " + err.getMessage() + "\n");
            System.exit(10);
        }

        // Create a new script manager
        scriptManager = new ScriptManagerImpl(scriptingParams, scriptVariables, client);
        if (testScript != null) {
            scriptManager.setScriptToRun(testScript);
        }

        if (client != null) {
            daemon = new RfbRefreshDaemon(client, scriptManager, cfg);
        }
    }

    /**
     * <p>Implementation of the <code>java.lang.Runnable</code> interface. This method 
     * gets executed when the encapsulating thread is started.</p>
     *
     * <p>When the thread executes in CLI mode, the method creates an instance of 
     * the {@link com.tplan.robot.CLIModule} class and invokes its <code>run()</code> method.</p>
     *
     * <p>When the thread executes in GUI mode, the method initializes the application help
     * and then it creates and displays an instance of the main application frame 
     * ({@link com.tplan.robot.gui.MainFrame} instance).
     */
    public void run() {
        try {
            System.setProperty("user.dir", System.getProperty("user.home"));

            // Create a CLI module
            if (consoleMode) {
                // Console mode is only allowed when there's a script to execute
                if (scriptManager.getScriptToRun() != null) {
                    cliModule = new CLIModule(client, scriptManager, logWriter, exitOnFinish, cfg);
                    cliModule.run();
                }
                destroy();
            } // GUI mode - create a new frame and display it
            else {
                mainFrame = new MainFrame(this, guiParams, cfg);
                mainFrame.setVisible(true);
            }
        } catch (Error err) {
            String msg = ApplicationSupport.getResourceBundle().getString("com.tplan.robot.failedToDisplayGui");

            if (isConsoleMode() && err instanceof NoClassDefFoundError) {
                Utils.writeLog(logWriter, err.getMessage() + msg);
            } else {
                err.printStackTrace();
            }
            if (exitOnFinish) {
                System.exit(1);
            }
        }
    }

    private void destroy() {
//        System.out.println("AutomatedRunnableImpl.destroy(), client="+client);
        if (daemon != null) {
            daemon.destroy();
        }
        daemon = null;
        mainFrame = null;
        if (client != null) {
            client.destroy();
        }
        client = null;
        cliModule = null;
        cfg = null;
        logo = null;
        scriptManager = null;
    }

    /**
     * Find out whether the thread runs in CLI or GUI mode.
     * @return true if the client runs in CLI mode, false if in GUI mode.
     */
    public boolean isConsoleMode() {
        return consoleMode;
    }

    /**
     * Find out whether a script execution is in progress.
     * @return true if a script execution is in progress, false otherwise.
    //     */
//    public boolean isRunning() {
//        return scriptManager.isRunning();
//    }
    /**
     * Find out whether the thread is connected to a desktop. It calls just
     * the {@link com.tplan.robot.remoteclient.RemoteDesktopClient#isConnected} method of the
     * underlying RFB module.
     * @return true if the client is connected to an RFB server, i.e. if 
     * there's an active connection which has already passed the authentication 
     * and init phase (after ServerInit message is received from the server).
     */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * Get the thread ID (name).
     * @return thread ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Stop the thread. Note that this method is applicable only to CLI threads 
     * and the method performs nothing when the thread is running in GUI mode.
     */
    public void stop() {
        if (cliModule != null) {
            cliModule.stop();
        }
    }

    /**
     * Get the thread exit code. It usually returns result of the test script 
     * execution. Note that this method is applicable only to threads running 
     * in CLI mode and it returns always zero in GUI mode.
     * @return thread exit code in CLI mode, zero in GUI mode.
     */
    public int getExitCode() {
        if (cliModule != null) {
            return cliModule.getExitCode();
        }
        return 0;
    }

    /**
     * Get script handler owned by this thread.
     * @return script handler instance.
     */
    public ScriptManager getScriptHandler() {
        return scriptManager;
    }

    /**
     * Get desktop client owned by this thread.
     * @return desktop client module.
     */
    public RemoteDesktopClient getClient() {
        return client;
    }

    public void serverMessageReceived(RemoteDesktopServerEvent evt) {
        if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_DISCONNECTED_EVENT) {
            Utils.paintDefaultImage(client.getImage(), logo,
                    ApplicationSupport.getResourceBundle().getString("com.tplan.robot.defaultImage.textDisconnected"));
        } else if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_INIT_EVENT) {
            Utils.paintDefaultImage(client.getImage(), logo,
                    ApplicationSupport.getResourceBundle().getString("com.tplan.robot.defaultImage.textDisconnected"));
        } else if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_CONNECTING_EVENT) {
            Object args[] = {client.getConnectString()};
            String s = MessageFormat.format(ApplicationSupport.getResourceBundle().getString("com.tplan.robot.defaultImage.textConnecting"), args);
            Utils.paintDefaultImage(client.getImage(), logo, s);
        }
    }
}
