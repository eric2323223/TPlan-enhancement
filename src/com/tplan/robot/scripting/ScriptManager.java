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
package com.tplan.robot.scripting;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.scripting.commands.CommandHandler;
import com.tplan.robot.gui.DesktopViewer;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.commands.CommandListener;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;

import java.util.List;
import java.util.Map;

/**
 * Script manager public interface.
 *
 * @product.signature
 */
public interface ScriptManager {

    static final String OUTPUT_DISABLED_FLAG = ApplicationSupport.APPLICATION_NAME + ".nooutput";

    /**
     * Get the map of available command handlers. The key is the command name in
     * upper case, the value is the command handler instance.
     * @return map of available command handlers.
     */
    Map<String, CommandHandler> getCommandHandlers();

    /**
     * Get a script to be executed which was passed from CLI. This method
     * returns the script interpret instance for a script passed through the -r/--run
     * CLI option. If the option was not specified during the tool start or the
     * script has been already started, the method returns null.
     * @return script to execute.
     */
    Object getScriptToRun();

    /**
     * Set the script to be executed after the tool startup.
     * @param scriptToRun a script to be executed automatically.
     */
    void setScriptToRun(Object scriptToRun);

    /**
     * Create a scripting context instance and populate it with default data.
     * @return a new scripting context instance populated with necessary object
     * references and applicable default variables.
     */
    ScriptingContext createDefaultContext();

    /**
     * Get the map of script variables overriden through the -v/--variable CLI option.
     * @return map of CLI variables.
     */
    Map<String, String> getCliVariables();

    /**
     * Set the map of CLI variables.
     * @param cliVars map of variables to override.
     */
    void setCliVariables(Map<String, String> cliVars);

    /**
     * Associate the script manager with a desktop viewer GUI component.
     * @param fbPanel a desktop viewer component.
     */
    void setDesktopViewer(DesktopViewer fbPanel);

    /**
     * Set the currently used desktop client.
     * @param client a desktop client.
     */
    void setClient(RemoteDesktopClient client);

    /**
     * Set the currently used desktop client. The context argument is usually null
     * and it is only populated by the Connect command to indicate that the
     * client was created through a running script. The context reference
     * is then passed to the script event which is fired to all registered
     * ScriptListener objects.
     *
     * @param client a desktop client.
     */
    void setClient(RemoteDesktopClient client, ScriptingContext context);

    /**
     * Get the currently used desktop client.
     * @return desktop client.
     */
    RemoteDesktopClient getClient();

    /**
     * Get the map of scripting parameters passed from the CLI, such as -r/--run,
     * --fromlabel, --tolabel and --nooutput. The key is the CLI option without
     * the minus (or --) prefix.
     * @return map of scripting parameters passed through the CLI.
     */
    Map<String, Object> getScriptingParams();

    /**
     * Add a script listener. Script events are sent to notify of events associated
     * with script control and handling, for example when a script gets compiled,
     * when execution starts, pauses or finishes and so on.
     *
     * @param listener a script listener.
     */
    void addScriptListener(ScriptListener listener);

    /**
     * Remove a script listener. The method does nothing if the listener
     * is not registered with this script manager.
     *
     * @param listener a script listener.
     */
    void removeScriptListener(ScriptListener listener);

    /**
     * Add a command listener to all available command handlers.
     * @param listener a command listener.
     */
    void addCommandListener(CommandListener listener);

    /**
     * Remove a command listener from all available command handlers.
     * @param listener a command listener.
     */
    void removeCommandListener(CommandListener listener);

    /**
     * Find out whether we run in the GUI or console mode.
     * @return true if the mode is the console one, false indicates the GUI mode.
     */
    boolean isConsoleMode();  // CANNOT REMOVE NOW, THE FLAG MAY VARY FOR THREADS CREATED PROGRAMATICALLY


    /**
     * Fire a script event to all registered script listeners.
     * @param evt a script event.
     * @return if any of the registered listeners requests pausing of a script
     * execution, it throws a PauseRequestException and this method returns it.
     */
    PauseRequestException fireScriptEvent(ScriptEvent evt);

    /**
     * Resolve a file path with regard to a variable which is expected to contain
     * a directory.
     * @param fileName file name.
     * @param repository a scripting context (needed for variables).
     * @param variableName variable to get the target directory from.
     * @return absolute path to the file specified by the fileName argument.
     */
    String assembleFileName(String fileName, ScriptingContext repository, String variableName);

    // New functionality
    /**
     * Get active test script interprets, i.e. test scripts which have been
     * opened and compiled and/or executed.
     * @return list of active test script interprets known to this script manager instance.
     */
    List<TestScriptInterpret> getActiveTestScripts();

    /**
     * Get list of test script interprets which are being executed.
     * @return list of active test script interprets known to this script
     * manager instance which are being executed.
     */
    List<TestScriptInterpret> getExecutingTestScripts();

    /**
     * Get owner of a desktop client instance.
     * @param client a client
     * @return test script interpret owning the client or null if no one owns it.
     */
    TestScriptInterpret getClientOwner(RemoteDesktopClient client);

    /**
     * Remove an interpret from the internal list of active interprets. While
     * the list gets populated automatically with any compilation and/or execution
     * performed through the script manager, there's no way to remove the interpret
     * from the internal structures except this method which should be called by
     * interprets in their {@link TestScriptInterpret#destroy()} method.
     * @param interpret
     */
    void removeInterpret(TestScriptInterpret interpret);

    /**
     * Get dynamic value for a variable (if defined), such as the current time
     * or randiom number. If the name doesn't correspond to any known dynamic
     * variable the method returns null.
     * @param name variable name.
     * @param cliVars command line variable table.
     * @param variables standard variable table.
     * @param wr test wrapper requesting the value.
     * @param ctx scripting context.
     * @return dynamic variable value or null if the name doesn't correspond to any known dynamic
     * variable.
     */
    Object getDynamicVariableValue(String name, Map cliVars, Map variables, TestWrapper wr, ScriptingContext ctx);

    /**
     * Destroy the script manager. This method is intended to break references
     * to make the instance ready for garbage collection.
     */
    void destroy();
}
