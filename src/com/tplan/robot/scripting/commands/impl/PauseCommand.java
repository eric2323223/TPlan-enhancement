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
import com.tplan.robot.remoteclient.rfb.RfbConstants;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.ScriptManager;

import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler implementing functionality of the {@doc.cmd Pause} command.
 * @product.signature
 */
public class PauseCommand extends AbstractCommandHandler implements RfbConstants {

    final String PARAM_DESC = "desc";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as this command has a mandatory argument,
     * which is time in miliseconds.
     *
     * @return description of the mandatory argument.
     */
    public String getContextArgument() {
        return ApplicationSupport.getString("pause.argument");
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext repository) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        String parName;

        if (args.size() > 1) {
            throw new SyntaxErrorException(ApplicationSupport.getString("pause.syntaxErr"));
        }

        // Process the first argument which should be a number
        if (args.size() == 1) {
            parName = args.get(0).toString();
            vt.put(PARAM_DESC, parName);
        }
    }

    public String[] getCommandNames() {
        return new String[]{"pause"};
    }

    public int execute(List args, Map values, ScriptingContext repository) throws SyntaxErrorException {

        Map vt = new HashMap();

        // Validate
        validate(args, values, vt, repository);
        try {
            TestScriptInterpret interpret = (TestScriptInterpret) repository.get("CONTEXT_MASTER_INTERPRET");
            if (interpret == null) {
                interpret = repository.getInterpret();
            }
            String desc = (String) vt.get(PARAM_DESC);

            interpret.setPause(this, true, desc);
            return 0;

        } catch (Exception ex) {
            ex.printStackTrace();
            return 1;
        }
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns true as no desktop connection is needed for the connect command.
     */
    public boolean canRunWithoutConnection() {
        return true;
    }
}
