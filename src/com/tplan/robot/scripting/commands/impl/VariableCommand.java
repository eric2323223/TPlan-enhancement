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
import com.tplan.robot.scripting.DocumentWrapper;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.commands.CommandEvent;
import com.tplan.robot.scripting.wrappers.StructuredBlockWrapper;
import static com.tplan.robot.scripting.ScriptingContext.*;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.Element;

/**
 * Handler implementing functionality of the {@doc.cmd Var} command.
 * @product.signature
 */
public class VariableCommand extends AbstractCommandHandler {

    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private boolean useLocalVariables = true;

    @Override
    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    /**
     * Validate if the command is correct and complies with the command syntax.
     *
     * @param args a list of parameters.
     * @param values a map of [param, value] pairs resulted from parsing of the command.
     * @param variableContainer output map for values.
     * @param context execution context.
     * @throws SyntaxErrorException when the command doesn't meet the required syntax.
     */
    public void validate(List args, Map values, Map variableContainer, ScriptingContext context) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;

        if (values == null || values.size() < 1) {
            throw new SyntaxErrorException(ApplicationSupport.getString("var.syntaxErr.generic"));
        }

        boolean validateOnly = context.isCompilationContext();

        Object o = context.get(CONTEXT_CURRENT_SCRIPT_WRAPPER);
        if (o instanceof StructuredBlockWrapper && !context.containsKey(CONTEXT_PROCEDURE_DECLARATION_FLAG)) {
            // Bug fix: see mail from Christian from 11/29/07, condition extended with '|| validateOnly'
            if (((StructuredBlockWrapper) o).isExecutionAllowed() || validateOnly) {
                defineVariables(args, values, context, validateOnly);
            }
        } else {
            defineVariables(args, values, context, validateOnly);
        }
    }

    public String[] getCommandNames() {
        return new String[]{"var", "variable"};
    }

    @Override
    public boolean isGlobalPrerequisity(String command) {
        return true;
    }

    public int execute(List args, Map values, ScriptingContext repository) throws SyntaxErrorException {

        Map t = new HashMap();

        // Validate
        validate(args, values, t, repository);

//        defineVariables(command, context);
        repository.getScriptManager().fireScriptEvent(new ScriptEvent(this, null, repository, ScriptEvent.SCRIPT_VARIABLES_UPDATED));
        return 0;
    }

    protected Object getProposedVariableValue(String variableName, Map values, ScriptingContext context) throws SyntaxErrorException {
        return values.get(variableName);
    }

    private void defineVariables(List args, Map values, ScriptingContext ctx, boolean validateOnly) throws SyntaxErrorException {
        Map o = ctx.getVariables();

        try {
            useLocalVariables = !ctx.getConfiguration().getBoolean("scripting.globalVariablesCompatMode").booleanValue();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String name;
        Object value;
        boolean debugMode = ctx.containsKey(CONTEXT_DEBUG_MODE_FLAG);
        for (int i = 0; i < args.size(); i++) {
            name = (String) args.get(i);
            value = getProposedVariableValue(name, values, ctx);
            if (value == null) {
                String s = ApplicationSupport.getString("var.syntaxErr.invalidDefinition");
                throw new SyntaxErrorException(MessageFormat.format(s, name));
            }
            if (name.equals(IMPLICIT_VARIABLE_REPORT_DIR)) {
                // Bug fix PRB-3416 - Changed the context object from Element to
                // a linked hash map [Element, DocumentWrapper]. This is intended
                // to help the GUI tools such as the Path Panel to determine
                // which element corresponds to the active editor.
                LinkedHashMap<Element, DocumentWrapper> l = (LinkedHashMap) ctx.get(CONTEXT_OUTPUT_PATH_ELEMENT);
                if (l == null) {
                    l = new LinkedHashMap();
                    ctx.put(CONTEXT_OUTPUT_PATH_ELEMENT, l);
                }
                Element el = (Element) ctx.get(CONTEXT_CURRENT_DOCUMENT_ELEMENT);
                if (el != null) {
                    l.put(el, (DocumentWrapper) ctx.get(CONTEXT_CURRENT_SCRIPT_WRAPPER));
                }
            } else if (name.equals(IMPLICIT_VARIABLE_TEMPLATE_DIR)) {
                // Bug fix PRB-3416 Changed the context object from Element to
                // a linked hash map [Element, DocumentWrapper]. This is intended
                // to help the GUI tools such as the Path Panel to determine
                // which element corresponds to the active editor.
                LinkedHashMap<Element, DocumentWrapper> l = (LinkedHashMap) ctx.get(CONTEXT_TEMPLATE_PATH_ELEMENT);
                if (l == null) {
                    l = new LinkedHashMap();
                    ctx.put(CONTEXT_TEMPLATE_PATH_ELEMENT, l);
                }
                Element el = (Element) ctx.get(CONTEXT_CURRENT_DOCUMENT_ELEMENT);
                if (el != null) {
                    l.put(el, (DocumentWrapper) ctx.get(CONTEXT_CURRENT_SCRIPT_WRAPPER));
                }
            }

            // If the "Use local variables" mode is on, we need to check if there's a context of local variables.
            // Only structure and procedure wrappers provide a non-null local context.
            if (useLocalVariables) {
                DocumentWrapper wr = (DocumentWrapper) ctx.get(CONTEXT_CURRENT_SCRIPT_WRAPPER);
                wr.setVariable(ctx, name, value);
                if (debugMode) {
                    fireCommandEvent(this, ctx, CommandEvent.LOCAL_VARIABLE_EVENT, name);
                }
            } else {
                o.put(name, value);
                if (debugMode) {
                    fireCommandEvent(this, ctx, CommandEvent.GLOBAL_VARIABLE_EVENT, name);
                }
            }
        }
    }

    @Override
    public List getStablePopupMenuItems() {
        return null;
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns true as no desktop connection is needed for the variable command.
     */
    @Override
    public boolean canRunWithoutConnection() {
        return true;
    }
}
