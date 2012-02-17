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

import com.tplan.robot.scripting.SyntaxErrorException;

import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.ScriptingContext;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.Map;

/**
 * Handler implementing functionality of the {@doc.cmd Eval} command.
 * @product.signature
 */
public class EvalCommand extends VariableCommand {

    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    public String[] getCommandNames() {
        return new String[]{"eval"};
    }

    public boolean isGlobalPrerequisity(String command) {
        return true;
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns true as no desktop connection is needed for the variable command.
     */
    public boolean canRunWithoutConnection() {
        return true;
    }

    @Override
    protected Object getProposedVariableValue(String variableName, Map values, ScriptingContext repository) throws SyntaxErrorException {
        Object value = values.get(variableName);
        try {
            TokenParser parser = repository.getParser();
            value = parser.evaluateNumericExpression(value.toString(), null, repository);
        } catch (IllegalArgumentException e) {
            throw new SyntaxErrorException(e.getMessage());
        }
        return value;
    }
}
