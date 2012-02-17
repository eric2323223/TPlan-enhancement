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
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.ScriptListener;

import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.List;

/**
 * <p>Custom hook assuring a clean application shut down when Java
 * Virtual Machine (JVM) gets terminated.</p>
 *
 * <p>An instance of this class is created and hooked into the
 * JVM for each automated thread. It gets invoked by Java when a request
 * to terminate the JVM is detected, e.g. through Ctrl+C in the system console.
 * The hook takes care of clean shutdown of the executed test script (if any)
 * and proper termination of any existing server connection.</p>
 * @product.signature
 */
public class ShutdownHook extends Thread implements ScriptListener {
    boolean go = false;

    private WeakReference scriptManager;
    private WeakReference client;

    /**
     * Constructor.
     * @param scriptManager a script manager instance.
     * @param client a desktop client.
     */
    public ShutdownHook(ScriptManager scriptManager, RemoteDesktopClient client) {
        this.scriptManager = new WeakReference(scriptManager);
        this.client = new WeakReference(client);
    }

    /**
     * Implementation of the <code>java.lang.Runnable</code> interface. This method
     * gets executed when the encapsulating thread is started.</p>
     */
    public void run() {
        final boolean cli = getScriptHandler().isConsoleMode();

        // Stop the running script execution
        List<TestScriptInterpret> l = getScriptHandler().getExecutingTestScripts();

        if (l.size() > 0) {
//            getScriptHandler().setManuallyStopped(true);

            if (cli) {
                System.out.println(ApplicationSupport.getString("cli.stoppingScriptExecution"));
            }
//            getScriptHandler().setStop(true);
            final String stopReason = ApplicationSupport.getString("cli.stopReason");
            for (TestScriptInterpret ti : l) {
                boolean manual = ti.isExecuting();
                ti.setStop(this, true, manual, manual ? stopReason : null);
            }

            // First stop the script execution. It will recreate the report with the correct status (Manually Stopped By User)
            int counter = 0;
            try {
                // Wait no longer than 5 seconds for script execution to stop
                while (!go && counter < 5000) {
                    Thread.sleep(5);
                    counter += 5;
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }

        // Close connection to the desktop (if connected)
        RemoteDesktopClient client = getClient();
        if (client != null && (client.isConnected() || client.isConnecting())) {
            if (cli) {
                String str = ApplicationSupport.getResourceBundle().getString("cli.closingConnection");
                Object args[] = { client.getConnectString() };
                System.out.println(MessageFormat.format(str, args));
            }

            // Close the RFB connection
            try {
                client.close();
            } catch (Exception ex) {
            }

            if (cli) {
//                System.out.println(ApplicationSupport.getResourceBundle().getString("cli.closingConnectionDone"));
            }
        }
    }

    /**
     * Get script handler associated with this hook.
     * @return script handler instance.
     */
    public ScriptManager getScriptHandler() {
        return scriptManager == null ? null : (ScriptManager)scriptManager.get();
    }

    /**
     * Get RFB client module associated with this hook.
     * @return RFB client module.
     */
    public RemoteDesktopClient getClient() {
        return client == null ? null : (RemoteDesktopClient)client.get();
    }

    public void scriptEvent(ScriptEvent event) {
       if (event.getType() == ScriptEvent.SCRIPT_EXECUTION_FINISHED) {
           go = getScriptHandler().getExecutingTestScripts().size() == 0;
       }
    }

}
