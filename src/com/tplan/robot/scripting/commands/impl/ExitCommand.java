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
package com.tplan.robot.scripting.commands.impl;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.scripting.*;
import com.tplan.robot.scripting.commands.AdvancedCommandHandler;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.wrappers.ProcedureWrapper;
import com.tplan.robot.scripting.wrappers.RunWrapper;
import com.tplan.robot.scripting.wrappers.ScriptWrapper;
import com.tplan.robot.scripting.wrappers.StructuredBlockWrapper;

import javax.swing.*;
import java.util.List;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler implementing functionality of the {@doc.cmd Exit} command.
 * @product.signature
 */
public class ExitCommand extends AbstractCommandHandler implements AdvancedCommandHandler {

    private final String PARAM_CODE = "code";
    public static final String PARAM_SCOPE = "scope";
    public static final String PARAM_SCOPE_PROCESS = "process";
    public static final String PARAM_SCOPE_FILE = "file";
    public static final String PARAM_SCOPE_PROCEDURE = "procedure";
    public static final String PARAM_SCOPE_BLOCK = "block";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static Map contextAttributes;

    /**
     * Get a map with context attributes.
     *
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes() {
        if (contextAttributes == null) {
            contextAttributes = new HashMap();
            contextAttributes.put(PARAM_SCOPE, PARAM_SCOPE_PROCESS + "|" +
                    PARAM_SCOPE_FILE + "|" + PARAM_SCOPE_PROCEDURE + "|" + PARAM_SCOPE_BLOCK);
        }
        return contextAttributes;
    }

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as this command has a mandatory argument,
     * which is a numeric exit code.
     *
     * @return description of the mandatory argument.
     */
    public String getContextArgument() {
        return ApplicationSupport.getString("exit.argument");
    }

    public void validate(List paramNames, Map paramValues, Map variableContainer, ScriptingContext repository) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        Object parName;
        String value;

        if (paramNames.size() >= 1) {
            parName = paramNames.get(0);

            if (parName instanceof Number) {
                vt.put(PARAM_CODE, new Integer(((Number) parName).intValue()));
            } else {
                try {
                    vt.put(PARAM_CODE, new Integer(Integer.parseInt(parName.toString())));
                } catch (Exception ex) {
                    throw new SyntaxErrorException("Exit code must be an integer number.");
                }
            }

            for (int i = 1; i < paramNames.size(); i++) {
                parName = paramNames.get(i).toString().toLowerCase();
                value = (String) paramValues.get(parName);
                value = value == null ? "" : value;

                if (parName.equals(PARAM_SCOPE)) {
                    value = value.toLowerCase();
                    if (value.equals(PARAM_SCOPE_PROCESS) || value.equals(PARAM_SCOPE_FILE) ||
                            value.equals(PARAM_SCOPE_PROCEDURE) || value.equals(PARAM_SCOPE_BLOCK)) {
                        vt.put(PARAM_SCOPE, value);
                    } else {
                        throw new SyntaxErrorException("Unsupported scope value '" + value + "'. Supported values are " + getContextAttributes().get(PARAM_SCOPE));
                    }
                } else {
                    throw new SyntaxErrorException("Unsupported parameter '" + parName + "'");
                }
            }
        } else {
            throw new SyntaxErrorException("Incorrect number of parameters. Correct format is 'Exit [<error_code_number>] scope=[<scope>]'.");
        }
    }

    public String[] getCommandNames() {
        return new String[]{"exit"};  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int execute(List paramNames, Map paramValues, ScriptingContext repository) throws SyntaxErrorException {

        Map t = new HashMap();

        // Validate
        validate(paramNames, paramValues, t, repository);

        return handleExitEvent(repository, t);
    }

    private int handleExitEvent(ScriptingContext repository, Map params) {
        try {

            ScriptManager handler = repository.getScriptManager();
            TestScriptInterpret interpret = repository.getInterpret();
            int exitCode = params.containsKey(PARAM_CODE) ? ((Number) params.get(PARAM_CODE)).intValue() : 0;
            repository.getVariables().put(ScriptingContext.IMPLICIT_VARIABLE_EXIT_CODE, exitCode);

            if (interpret.isExecuting()) {
                String scope = (String) params.get(PARAM_SCOPE);
                TestWrapper wrapper = repository.getMasterWrapper();

                if (scope == null || scope.equals("process")) {
                    interpret.setStop(this, true, false, null);
                } // Exit from file
                else if (scope.equals("file")) {

                    // Recursively look if ve are in a run wrapper
                    while (wrapper != null) {
                        if (wrapper instanceof RunWrapper || wrapper instanceof ScriptWrapper) {
//                            System.out.println(wrapper.toString()+": setting the exit flag to true");
                            ((DocumentWrapper) wrapper).exit();
                            break;
                        }
                        wrapper = wrapper.getParentWrapper();
                    }
                } // Exit from a procedure
                else if (scope.equals("procedure")) {
                    // Recursively look if ve are in a procedure wrapper
                    while (wrapper != null) {
                        if (wrapper instanceof ProcedureWrapper) {
                            ((DocumentWrapper) wrapper).exit();
                            break;
                        }
                        if (wrapper instanceof RunWrapper || wrapper instanceof ScriptWrapper) {
                            break;
                        }
                        wrapper = wrapper.getParentWrapper();
                    }
                } // Exit from the innermost structured block of code
                else if (scope.equals("block")) {
                    // Recursively look if ve are in a structured block wrapper
                    while (wrapper != null) {
                        if (wrapper instanceof StructuredBlockWrapper) {
                            ((DocumentWrapper) wrapper).exit();
                            break;
                        }
                        if (wrapper instanceof RunWrapper || wrapper instanceof ScriptWrapper || wrapper instanceof ProcedureWrapper) {
                            break;
                        }
                        wrapper = wrapper.getParentWrapper();
                    }
                }
            }

            return exitCode;

        } catch (Exception ex) {
            ex.printStackTrace();
            return 1;
        }
    }

    public List getStablePopupMenuItems() {
        return null;
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns true as no desktop connection is needed for the exit command.
     */
    public boolean canRunWithoutConnection() {
        return true;
    }

    public List getArguments(String command, ScriptingContext context) {
        return Arrays.asList(new String[] {"\"<"+getContextArgument()+">\""});
    }

    public List getParameters(String command, ScriptingContext context) {
        return Arrays.asList(new String[] {PARAM_SCOPE});
    }

    public List getParameterValues(String paramName, String command, ScriptingContext context) {
        if (paramName.equalsIgnoreCase(PARAM_SCOPE)) {
            return Arrays.asList(new String[] {PARAM_SCOPE_BLOCK, PARAM_SCOPE_FILE, PARAM_SCOPE_PROCEDURE, PARAM_SCOPE_PROCESS});
        }
        return null;
    }
}
