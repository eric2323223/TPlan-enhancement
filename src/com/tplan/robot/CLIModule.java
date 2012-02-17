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

import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.remoteclient.RemoteDesktopServerListener;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.DesktopViewer;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.rfb.*;
import com.tplan.robot.scripting.ExecOrCompileThread;
import com.tplan.robot.scripting.JavaTestScript;
import com.tplan.robot.scripting.ScriptListener;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.interpret.java.JavaTestScriptInterpret;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.interpret.TestScriptInterpretFactory;
import com.tplan.robot.util.Utils;

import java.io.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * <p>Module for automated test script execution in CLI mode.</p>
 *
 * <p>This class implements functionality needed to execute one automated task 
 * in CLI mode. The input parameters must contain RFB server name, port, password (if needed) 
 * and name of the script to execute. The module does the following steps:
 * <ul>
 * <li>An instance of {@link com.tplan.robot.gui.DesktopViewer} is created. As this class
 * defines methods which map Java key and mouse events onto the remote desktop
 * client-to-server method calls, we use it even in CLI testing when no graphical 
 * display is employed.</li>
 *
 * <li>An instance of {@link com.tplan.robot.remoteclient.rfb.ShutdownHook} is created and
 * hooked into the Java Virtual Machine (JVM). It gets invoked by Java when a request 
 * to terminate the JVM is detected, e.g. through Ctrl+C in the system console. 
 * The hook takes care of clean shutdown of the executed test script (if running) 
 * and proper termination of the eventual server connection.</li>
 *
 * <li>If the input parameters contain a proper URL with a valid protocol, server host name, port and
 * eventual authentication data (password), the module creates a client associated with the given protocol
 * and invokes its {@link com.tplan.robot.remoteclient.RemoteDesktopClient#connect()} method.
 * If the parameters are missing or connection fails, an error 
 * message gets printed into the console and the module exits.</li>
 *
 * <li>Before starting the execution 
 * the module waits for the predefined amount of time specified in the user preferences (parameter 
 * <code>com.tplan.robot.scripting.delayBeforeAutomaticExecutionSeconds</code>). The purpose of this 
 * delay is to allow the server to come online cleanly. The module then creates an instance of {@link TestScriptInterpret} and
 * invokes its {@link TestScriptInterpret#execute(com.tplan.robot.scripting.ScriptingContext) execute(ScriptingContext)}
 * method to start it.</li>
 *
 * <li>When the script execution thread finishes, the module waits for the specified amount of time 
 * (parameter 
 * <code>com.tplan.robot.scripting.delayAfterAutomaticExecutionSeconds</code>) and shuts down connection 
 * to the server. If the <code>exitOnFinish</code> flag is on, it also 
 * terminates the JVM with the exit code equal to what was returned by the script execution.</li>
 * </ul>
 *
 * @product.signature
 */
public class CLIModule implements RfbConstants, RemoteDesktopServerListener, Runnable, ScriptListener {

    /** Exit code. Will be used to save the result of the script execution. */
    private int exitCode = 0;
    /** Script handler instance. */
    private ScriptManager scriptManager;
    private boolean exitOnFinish;
    private RemoteDesktopClient client;
    private PrintStream logStream;
    private boolean stop = false;
    private Thread execThread = null;
    /** Post-execution timeout. */
    private long shutdownTimeout;
    /** Pre-execution timeout. */
    private long startupTimeout;
    /** User configuration instance. */
    private UserConfiguration cfg;
    private TestScriptInterpret interpret;

    /**
     * Constructor. It initializes the object variables and loads values of the 
     * pre-execution and post-execution timeouts.
     *
     * @param client a client (instance of RemoteDesktopClient).
     * @param scriptManager an instance of script handler.
     * @param logStream a stream for logs from the test script execution, i.e. the messages which are printed 
     * out into console in CLI mode, will be written to the <code>logStream</code>. To print the logs into the console window use
     * <code>System.out</code> as value. A <code>null</code> value of this argument will suppress all log messages.
     *
     * @param exitOnFinish indicates whether the process should terminate using
     * <code>System.exit(exitCode)</code> after the automated task gets finished. Third party applications should
     * use the value of <code>false</code> to prevent termination of their Java VM.
     * @param cfg an instance of UserConfiguration, typically obtained through the 
     * {@link com.tplan.robot.preferences.UserConfiguration#getInstance() UserConfiguration.getInstance()} method.
     * 
     */
    public CLIModule(RemoteDesktopClient client, ScriptManager scriptManager, PrintStream logStream,
            boolean exitOnFinish, UserConfiguration cfg) {
        this.cfg = cfg;
        this.scriptManager = scriptManager;
        this.client = client;
        this.exitOnFinish = exitOnFinish;
        this.logStream = logStream;
        try {
            Integer param = cfg.getInteger("scripting.delayBeforeAutomaticExecutionSeconds");
            int timeout = param == null ? 15 : param.intValue();
            startupTimeout = 1000 * timeout;

            param = cfg.getInteger("scripting.delayAfterAutomaticExecutionSeconds");
            timeout = param == null ? 15 : param.intValue();
            shutdownTimeout = 1000 * timeout;
        } catch (Exception e) {
            startupTimeout = 15000;
            shutdownTimeout = 15000;
        }
    }

    /**
     * Implementation of the <code>java.lang.Runnable</code> interface. This method 
     * gets executed when the encapsulating thread is started.</p>
     */
    public void run() {

        String str = ApplicationSupport.getResourceBundle().getString("cli.versionAndCopyright");
        Object args[] = {Utils.getProductNameAndVersion(), Utils.getCopyright()};
        Utils.writeLog(logStream, MessageFormat.format(str, args) + "\n");
        DesktopViewer pnl = new DesktopViewer(client, scriptManager, cfg);
        scriptManager.setDesktopViewer(pnl);
        if (client != null) {
            client.addServerListener(this);
        }
        scriptManager.addScriptListener(this);

        Thread shutdownHook = new ShutdownHook(scriptManager, client);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // If there's a host passed via CLI parameter -c, run the login to connect to it.
        // If there's no host, we run it without logging and we suppose that the script contains a connect command.
        if (client != null && client.hasSufficientConnectInfo()) {

            LoginRunnable loginRunnable = new LoginRunnable(client, cfg);
            loginRunnable.run();

            if (!client.isConnected() || loginRunnable.isLoginFailed()) {
                str = ApplicationSupport.getResourceBundle().getString("cli.failedToConnect");
                Object arg[] = {client.getConnectString(), loginRunnable.getConnectThrowable().getMessage()};
                Utils.writeLog(logStream, MessageFormat.format(str, arg));
                exitCode = 1;
                if (exitOnFinish) {
                    System.exit(exitCode);
                }

                destroy();

                // Bug fix 13039: thread memory leaks
                // Bug fix in 1.3.16: fixing IllegalStateException thrown when a running hook was being removed
                safeRemove(shutdownHook);
                return;
            }
        }

        runCliScript();
        safeRemove(shutdownHook);
        stop();
    }

    /**
     * Remove a shutdown hook from the runtime. If an IllegalStateException is thrown
     * during removal meaning that the hook is currently being executed, the
     * method gets to sleep for a period of 50 miliseconds and than it retries.
     * This is performed in a loop until the hook is successfuly removed or a limit of
     * 10 attempts is reached.
     *
     * @param shutdownHook a thread which was previously added to the Java Runtime
     * as a shutdown hook (see the <code>Runtime.getRuntime().addShutdownHook()</code>. method).
     */
    private void safeRemove(Thread shutdownHook) {
        boolean removed = false;
        int i = 10;  // Max number of attempts
        while (!removed && i > 0) {
            try {
                i--;
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
                removed = true;
            } catch (IllegalStateException e) {
            }
            if (!removed) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    /**
     * Implementation of the <code>ScriptListener</code> listener. It receives
     * exit code and handles pause request.
     *
     * @param event a script event.
     */
    public void scriptEvent(ScriptEvent event) {
        switch (event.getType()) {
            case ScriptEvent.SCRIPT_CLIENT_CREATED:
                if (this.client != null) {
                    this.client.removeServerListener(this);
                }
                this.client = event.getContext().getClient();
                this.client.addServerListener(this);
                break;
            case ScriptEvent.SCRIPT_EXECUTION_FINISHED:
                this.exitCode = event.getContext().getExitCode();
                break;
            case ScriptEvent.SCRIPT_EXECUTION_PAUSED:
                pause((String) event.getCustomObject());
                break;
        }
    }

    /**
     * Pause the script execution.
     * @param reason explanation of why the execution was paused.
     */
    private void pause(String reason) {
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        if (reason == null) {
            reason = res.getString("cli.noPauseReason");
        }
        String str = res.getString("cli.executionPaused");
        Object args[] = {reason};
        Utils.writeLog(logStream, MessageFormat.format(str, args));
        Utils.writeLog(logStream, res.getString("cli.howToResume"));

        //  open up standard input
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        int response = -1;

        try {
            response = br.read();
        } catch (IOException ioe) {
            System.out.println(res.getString("cli.ioErrorWhenReadingAnswer"));
        }
        interpret.setPause(this, false, res.getString("cli.execResumedFromCli"));
        Utils.writeLog(logStream, res.getString("cli.execResumed"));
    }

    /**
     * Stop the script execution associated with this CLI module. This is just 
     * a wrapper thread which calls the {@link TestScriptInterpret#setStop(java.lang.Object, boolean)}
     * method of the associated script handler.
     */
    public void stop() {
        stop = true;
        if (interpret != null && interpret.isExecuting()) {
            interpret.setStop(this, true, false, null);
        }
    }

    /**
     * Perform the exit routine - stop the script execution, wait for the 
     * specified post-execution amount of time, close the connection and 
     * exit JVM with the correct exit code if the instance of this module 
     * was created with the <code>exitOnFinish</code> value of true.
     */
    private void exitRoutine() {
        if (execThread != null && execThread.isAlive()) {
            interpret.setStop(this, true, false, null);
            try {
                execThread.join();
            } catch (InterruptedException e) {
            }
        }

        ResourceBundle r = ApplicationSupport.getResourceBundle();
        Integer param = cfg.getInteger("scripting.delayAfterAutomaticExecutionSeconds");
        int timeout = param == null ? 15 : param.intValue();

        String result = exitCode == 0 ? r.getString("cli.execResultPass") : r.getString("cli.execResultFail");
        String str = r.getString("cli.execFinished");
        Object arg[] = {result, exitCode};
        Utils.writeLog(logStream, MessageFormat.format(str, arg));

        if (!interpret.getExecutionContext().containsKey(ScriptingContext.CONTEXT_STOP_REASON)) {
            if (shutdownTimeout > 0) {
                str = r.getString("cli.exitTimeout");
                Object a2[] = {Utils.getTimePeriodForDisplay(shutdownTimeout, true)};
                Utils.writeLog(logStream, MessageFormat.format(str, a2));
                safeWait(shutdownTimeout);
            }
        }

        try {
            if (client != null) {
                str = r.getString("cli.closingConnection");
                Object a2[] = {client.getConnectString()};
                Utils.writeLog(logStream, MessageFormat.format(str, a2));
                client.close();
                Utils.writeLog(logStream, r.getString("cli.closingConnectionDone"));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        destroy();

        if (exitOnFinish) {
            System.exit(this.exitCode);
        }
    }

    private void destroy() {
//        System.out.println("CLIModule.exitRoutine()");
        if (client != null) {
            client.removeServerListener(this);
        }
        client = null;
        if (scriptManager != null) {
            scriptManager.removeScriptListener(this);
        }
        scriptManager.destroy();
        scriptManager = null;
        cfg = null;
        if (interpret != null) {
            try {
                interpret.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        interpret = null;
        execThread = null;
    }

    /**
     * Implementation of the {@link com.tplan.robot.remoteclient.RemoteDesktopServerListener}
     * interface. This method just prints out a message about successful connection
     * to the server and then removes itself from the list of listeners.
     * @param evt a RemoteDesktopServerEvent specifying the delivered server-to-client message.
     */
    public void serverMessageReceived(RemoteDesktopServerEvent evt) {
        int type = evt.getMessageType();
        RemoteDesktopClient rfb = (RemoteDesktopClient) evt.getSource();

        if (type == RemoteDesktopServerEvent.SERVER_CONNECTED_EVENT) {
            String str = ApplicationSupport.getResourceBundle().getString("cli.connectedTo");
            Object arg[] = {rfb.getConnectString(), rfb.getDesktopName()};
            Utils.writeLog(logStream, MessageFormat.format(str, arg));
        } else if (type == RemoteDesktopServerEvent.SERVER_UPDATE_EVENT) {
            rfb.removeServerListener(this);
        }
    }

    /**
     * Load & run the specified script in CLI mode.
     */
    private void runCliScript() {

        if (interpret == null || !interpret.isExecuting()) {
            // Look if there is a script to be run automatically
            Object script = scriptManager.getScriptToRun();
            if (script != null) {
                ResourceBundle r = ApplicationSupport.getResourceBundle();
                try {

                    if (script instanceof String) {

                        // The following piece of code tries to open the script file
                        // related to the product install location.
                        String installPath = Utils.getInstallPath();

                        File f = new File((String) script);
                        if (!f.isAbsolute() && installPath != null) {
                            String path = installPath;
                            if (!path.endsWith(File.separator)) {
                                path += File.separator;
                            }
                            f = new File(path + script);
                        }
                        f = f.getCanonicalFile();
                        String str = r.getString("cli.scriptToRun");
                        Object arg[] = {f.getAbsolutePath()};
                        Utils.writeLog(logStream, MessageFormat.format(str, arg));

//                        Editor ed = new Editor(null, cfg);
//                        ed.setPage(f.toURI().toURL());
//
//                        wrapper = new ScriptWrapper((StyledDocument) ed.getStyledDocument(), new File((String) script));
                        interpret = TestScriptInterpretFactory.getInstance().createByExtension(f.toURI());
                        interpret.setScriptManager(scriptManager);
                        interpret.setURI(f.toURI(), true);
                    } else if (script instanceof JavaTestScript) {
                        JavaTestScript javaTest = (JavaTestScript) script;
                        interpret = new JavaTestScriptInterpret();
                        interpret.setScriptManager(scriptManager);
                        ((JavaTestScriptInterpret) interpret).setTestInstance((JavaTestScript) script);
                    }

                    if (interpret == null) {
                        Utils.writeLog(logStream, MessageFormat.format(r.getString("cli.unsupportedTestObject"), script));
                        if (exitOnFinish) {
                            System.exit(exitCode);
                        }
                        exitRoutine();
                    }

                    if (startupTimeout > 0 && !stop) {
                        String str = r.getString("cli.startupTimeout");
                        Object a2[] = {Utils.getTimePeriodForDisplay(startupTimeout, true)};
                        Utils.writeLog(logStream, MessageFormat.format(str, a2));
                        safeWait(startupTimeout);
                    }

                    if (!stop) {
                        Utils.writeLog(logStream, MessageFormat.format(r.getString("cli.execStarting"), new Object[]{script}));
//                        execThread = wrapper == null ? scriptManager.execute(javaTest) : scriptManager.execute(wrapper);
                        execThread = new ExecOrCompileThread(interpret, true, null);
                        execThread.start();

                        while (!stop && execThread.isAlive()) {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                            }
                        }
                        Exception exc = ((ExecOrCompileThread) execThread).getException();
                        if (exc != null) {
                            throw exc;
                        }
                    }
                    exitRoutine();
                } catch (Exception ex) {
                    exitCode = 1;
                    Object a2[] = {ex.getMessage(), new Integer(exitCode)};
                    Utils.writeLog(logStream, MessageFormat.format(r.getString("cli.errorAndExit"), a2));
                    if (exitOnFinish) {
                        System.exit(exitCode);
                    }
                    exitRoutine();
                }
            }
        }
    }

    private void safeWait(long time) {
        long endTime = System.currentTimeMillis() + time;
        while (!stop && System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(2);
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Get the exit code.
     * @return exit code resulting from the executed test script.
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Get the shutdown timeout. When execution of test script finishes, the module
     * waits for the specified amount of time before it terminates. The shutdown timeout value
     * is populated from user configuration in the constructor.
     *
     * @return shutdown timeout.
     */
    public long getShutdownTimeout() {
        return shutdownTimeout;
    }

    /**
     * Set the shutdown timeout. The method allows to redefine the timeout
     * value populated from the user configuration in the class constructor.
     *
     * @param shutdownTimeout a new value of shutdown timeout in miliseconds.
     */
    public void setShutdownTimeout(long shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    /**
     * Get the startup timeout. The module
     * waits for the specified amount of time before it executes the test script.
     * The startup timeout value
     * is populated from user configuration in the constructor.
     *
     * @return startup timeout.
     */
    public long getStartupTimeout() {
        return startupTimeout;
    }

    /**
     * Set the startup timeout. The method allows to redefine the timeout
     * value populated from the user configuration in the class constructor.
     *
     * @param startupTimeout a new value of startup timeout in miliseconds.
     */
    public void setStartupTimeout(long startupTimeout) {
        this.startupTimeout = startupTimeout;
    }
}
