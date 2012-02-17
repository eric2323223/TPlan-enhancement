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
package com.tplan.robot.scripting.commands;


import com.tplan.robot.scripting.*;
import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Exposed functional interface of command handlers which deliver functionality
 * of particular scripting language commands.
 * @product.signature
 */
public interface CommandHandler {

    /**
     * Validate if the command complies with the command syntax.The command is
     * already pre-parsed to a list of parameter names and a map of [name, value]
     * pairs.
     *
     * @param params a list of parameters.
     * @param values a map of [param, value] pairs resulted from parsing of the command.
     * @param variableContainer output map for values.
     * @param context execution context.
     * @throws SyntaxErrorException when the command doesn't meet the required syntax.
     */
    public void validate(List params, Map values, Map variableContainer, ScriptingContext context) throws SyntaxErrorException;

    /**
     * <p>Get command names. A command name is the first word in a script line, e.g. "Type" or "Press". Though
     * most commands have just one name, you may use this method to define any number of command aliases. You may
     * even use one class to implement more commands if you want. In such a case you need to define more command
     * names and implement a different behavior for each command.
     *
     * <p>Please note that command name parsing is NOT case sensitive. You don't have to define the names as e.g.
     * { "MyCommand", "mycommand" }. Script parser will always parse the command name in a script and convert it to
     * upper case using the String.toUpperCase(). Such a command name will be then used to look for a command
     * implementation in the command table.
     *
     * @return array of command names.
     */
    public String[] getCommandNames();

    /**
     * <p>This method should return true if the command needs to be executed prior to running part of a test script.
     *
     * <p>Imagine a following situation. User creates a script:<br>
     * <code>
     * Var PATH=/usr/java
     * Type {PATH}/bin/java
     * Press Enter
     * </code>
     *
     * <p>User then selects just the last two commands to be executed. It would of course fail because the PATH variable
     * is not defined. If this method returns true, the command will be executed before running selected commands are
     * executed.
     *
     * @param command a command with parameters to be processed.
     * @return true if the command needs to be executed prior to running of part of the script, false othewise.
     */
    public boolean isGlobalPrerequisity(String command);

    /**
     * <p>Execute the command.
     *
     * <p>Argument <code>context</code> will contain all necessary objects that the command may possibly use,
     * for example the com.tplan.robot.gui.FrameBufferPanel and com.tplan.robot.api.rfb.RfbModule instances etc. If the command e.g. needs to send
     * some key events to the RFB server, you should save the reference to the RfbModuleImpl instance and use
     * its methods to fire the required key events.
     * @param params a list of parameters.
     * @param values a map of [param, value] pairs resulted from parsing of the command.
     * @param context execution context.
     * @return command exit code.
     * @throws SyntaxErrorException when the command doesn't meet the required syntax.
     * @throws IOException an instance of I/O exception may be thrown if an error occurs
     * in communication with the underlying desktop client.
     */
    public int execute(List params, Map values, ScriptingContext context) throws SyntaxErrorException, IOException;

    /**
     * Add a <code>CommandListener</code> to the listener list.
     *
     * @param listener  a <code>CommandListener</code> to be added.
     */
    public void addCommandListener(CommandListener listener);

    /**
     * Removes a <code>CommandListener</code> from the listener list.
     *
     * @param listener  the <code>CommandListener</code> to be removed
     */
    public void removeCommandListener(CommandListener listener);

    /**
     * Get a list of stable actions. They will be used to build up a pop up menu
     * displayed on a right mouse click onto the commend in the editor. Though it
     * is not explicitly declared, the list must contain javax.swing.Action instances.
     * @return list of stable context pop up menu actions.
     */
    public List getStablePopupMenuItems();

    /**
     * Get the dummy command argument. It is inserted as argument value to commands
     * generated through the command wizard in GUI. It is usually a short description
     * of the expected argument value and the user is expected to rewrite it with
     * a real value. If the command has no argument (just parameters in form of param=value), the
     * method should return null.
     *
     * @return dummy context argument value.
     */
    public String getContextArgument();

    /**
     * Get a map with context attributes.
     * <p>
     * This method is used to create a context menu which contains all parameters supported by the command.
     * Method should return a map where parameter name is a key (String) and corresponding value is a text
     * to be displayed as a hint for the parameter value.
     * <p>
     * A good example is the the Press command. It supports parameters <code>count</code> and <code>wait</code>.
     * The hash table generated by this method should then contain e.g. these two entries:
     *
     * 1. Key: "count", Value: "number"
     * 2. Key: "wait", Value: "time in ms"
     * <p>
     * When user types Press in the editor and invokes the completion wizard, the list of these two parameters gets
     * displayed. When user selects one of them, the editor then creates a parameter like count=&lt;number&gt; or
     * wait=&lt;time in ms&gt; and inserts it into the edited line.
     *
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes();

    /**
     * Get preferred hot key for the GUI command wizard. When such a key is
     * pressed in the editor, the GUI inserts the command template into the editor 
     * document. The key returned by this method is just a recommendation and the
     * GUI may decide to assign another one to the command, for example when
     * there is a conflict with another existing one.
     * 
     * @return hot key invoking insertion of a command template into the GUI editor..
     */
    public KeyStroke getContextShortcut();

    /**
     * This method should return true if the command can be executed while
     * the tool is not connected to any local or remote desktop.
     *
     * @return true if no desktop connection is necessary for execution of this command, false otherwise.
     */
    public boolean canRunWithoutConnection();

}