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

/**
 * <p>Declaration of automated runnable methods.</p>
 *
 * <p>An automated runnable typically performs one automated task usually defined by a test script
 * or a Java class. Life cycle of such a runnable is:
 * <ul>
 * <li>Init phase & parsing of input parameters</li>
 * <li>Create necessary object instances (desktop client, GUI, script manager)</li>
 * <li>Connect to the specified server/desktop</li>
 * <li>Execute the specified script & generate results (screenshots, HTML report)</li>
 * <li>Close connection to the server/desktop</li>
 * <li>Shutdown</li>
 * </ul>
 *
 * <p>The tool is designed as a multithreaded application and it can run multiple
 * automated testing threads. Each such a thread contains its own desktop client
 * ({@link com.tplan.robot.remoteclient.RemoteDesktopClient} instance) and a script manager
 * ({@link com.tplan.robot.scripting.ScriptManager} instance). Each thread is typically able to
 * handle an independent automated task, e.g. running of a script on
 * one server instance. This class implements such a functionality and it
 * can be executed as a thread through the {@link java.lang.Runnable Runnable} interface.</p>
 *
 * <p>Each runnable can be initiated with different CLI options. See the
 * {@link com.tplan.robot.ApplicationSupport#APPLICATION_CLI_OPTIONS} array. Thread behavior
 * strongly depends on whether it runs in CLI (invoked with the <code>-n</code> option)
 * or GUI mode. See the {@link #run()} method for more info.
 * While there's no limitation on threads running on CLI mode,
 * there can be just one thread running in GUI mode within one Java Virtual Machine (JVM)
 * because the current GUI design is not capable to handle multiple frames.</p>
 *
 * <p>The application is by default started in a single thread mode. It means that only
 * one thread is created regardless of the CLI/GUI mode flag. The ability of
 * running multiple threads in CLI mode can be exploited only through custom
 * Java programs where the application JAR file and it's APIs serve as a library.
 * </p>
 *
 * <p>To execute an automated process instantiate the {@link com.tplan.robot.ApplicationSupport} class and call one of its
 * <code>createAutomatedRunnable()</code> methods. Then execute the <code>run()</code> method of the returned runnable.
 * To run multiple threads encapsulate the runnable with a {@link java.lang.Thread Thread}
 * and start it instead.</p>
 *
 * <p>The following example starts two automated threads. The first one connects to VNC server at localhost:1 and
 * executes script <code>/root/thread1.txt</code>. The other one connects to VNC server localhost:2 and executes
 * another script <code>/root/thread2.txt</code>. Note that both the threads will be executed simultaneously and
 * the program exits when the last thread finishes.
 *
 * <blockquote>
 * <pre>
 * import {@product.package}.ApplicationSupport;
 * import {@product.package}.AutomatedRunnable;
 *
 * public class TwoTasks {
 *
 *    public static void main(String[] argv) {
 *       ApplicationSupport robot = new ApplicationSupport();
 *       String args1[] = { "-c", "localhost:1", "-p", "welcome", "-n", "-r", "/root/thread1.txt" };
 *       AutomatedRunnable runnable1 = robot.createAutomatedRunnable("cli-1", args1, System.out, false);
 *       Thread t1 = new Thread(runnable1);
 *       t1.start();
 *
 *       String args2[] = { "-c", "localhost:2", "-p", "welcome", "-n", "-r", "/root/thread2.txt" };
 *       AutomatedRunnable runnable2 = robot.createAutomatedRunnable("cli-2", args2, System.out, false);
 *       Thread t2 = new Thread(runnable2);
 *       t2.start();
 *    }
 * }
 * </pre>
 * </blockquote>
 *
 * <b>IMPORTANT NOTE:</b> This is version has one major limitation:
 * <ul>
 * <li>Multithreaded executions in GUI mode are not supported. Only one GUI
 * thread may be created and executed at a time and it will then terminate the Java VM machine. Note that though
 * the API will let you to create and start multiple GUI threads, they will not work correctly. Please use multiple
 * threads just in CLI mode, i.e. always specify <code>-n</code> or <code>--nodisplay</code> among the thread parameters.</li>
 * </ul>
 * 
 * @product.signature
 **/
public interface AutomatedRunnable extends Runnable {

    /**
     * Returns true if the thread is running in console/CLI mode (i.e. either "-n" or "--nodisplay" was passed among
     * the thread parameters).
     *
     * @return true if the thread is running in the console/CLI mode, false if in the GUI one.
     */
    public boolean isConsoleMode();

    /**
     * Returns true if the thread is executing a script, false if not. The value of <code>false</code> may indicate
     * that the thread is not started, it failed or finished, or it is in the initial or shutdown countdown phase.
     * <p>
     * Note that if you need to find out whether the thread is started or has finished, you should rather use
     * the <code>Thread.isAlive()</code> method.
     *
     * @return true if the thread is executing a script, false if not.
     */
//    public boolean isRunning();

    /**
     * Returns true if the thread is connected to a server/desktop.
     *
     * @return true if the thread is connected to a desktop, false if not.
     */
    public boolean isConnected();

    /**
     * Stop the thread. This will initiate the shutdown phase. If a test script is being executed, the execution
     * is finished without the shutdown timeout sequence, connection to the desktop is closed and the thread exits.
     * If your test script is configured to generate an HTML report, it will show as <b>"Manually Stopped By User"</b>.
     */
    public void stop();

    /**
     * <p>Get the thread exit code which should reflect result of the script execution. Value of zero usually indicates
     * successful execution, non-zero values mean failures.  See the <code>Exit</code> command in the Scripting Language
     * Specification document for more info on exit codes.
     *
     * <p>Threads stopped by the <code><a href=#stop>stop()</a></code> method always return zero unless an internal error occurs.
     *
     * @return thread exit code.
     */
    public int getExitCode();

    /**
     * Get the thread ID assigned during the thread creation.
     * @return thread ID (name).
     */
    public String getId();
}
