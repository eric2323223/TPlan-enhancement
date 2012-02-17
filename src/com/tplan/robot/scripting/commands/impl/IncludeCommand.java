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
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import javax.swing.*;
import java.util.List;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Handler implementing functionality of the {@doc.cmd Include} command.
 * @product.signature
 */
public class IncludeCommand extends AbstractCommandHandler implements ScriptListener, AdvancedCommandHandler {

    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private int returnValue = 0;

    public void validate(List args, Map values, Map variableContainer, ScriptingContext repository) throws SyntaxErrorException {
        fireCommandEvent(this, repository, "include", args);
    }

    public String[] getCommandNames() {
        return new String[]{"include"};
    }

    public int execute(List args, Map values, ScriptingContext repository) throws SyntaxErrorException {
        returnValue = 0;
        ScriptManager handler = repository.getScriptManager();
        handler.addScriptListener(this);
        fireCommandEvent(this, repository, "include", args);
        handler.removeScriptListener(this);
        return returnValue;
    }

    public String getContextArgument() {
        return ApplicationSupport.getString("include.argument");
    }

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    public boolean isGlobalPrerequisity(String command) {
        return true;
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns true as no desktop connection is needed for this command.
     */
    public boolean canRunWithoutConnection() {
        return true;
    }

    public void scriptEvent(ScriptEvent event) {
        if (event.getType() == ScriptEvent.SCRIPT_INCLUDE_FAILED) {
            returnValue = 1;
        }
    }

    public List getArguments(String command, ScriptingContext context) {
        List l = new ArrayList();
        List params = new ArrayList();
        context.getParser().parseParameters(command, params);
        List objectList = new ArrayList();
        objectList.add(RunCommand.getScriptFileChooser(params.size() > 0 ? (String) params.get(0) : null, context));
        try {
            File dummy = ((ProprietaryTestScriptInterpret) context.getInterpret()).resolveScriptFile("dummy", context);
            objectList.add(dummy.getParentFile());
        } catch (IOException e) {
        }
        objectList.add(new Boolean(true));
        l.add(objectList);
        return l;
    }

    public List getParameters(String command, ScriptingContext context) {
        return null;
    }

    public List getParameterValues(String paramName, String command, ScriptingContext context) {
        return null;
    }
}
