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
import com.tplan.robot.scripting.SyntaxErrorException;

import com.tplan.robot.scripting.ScriptingContext;
import java.io.IOException;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Handler implementing functionality of the {@doc.cmd TypeLine} command.
 * @product.signature
 */
public class TypeLineCommand extends TypeCommand {

    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);

    public void validate(List args, Map values, Map variableContainer, ScriptingContext context) throws SyntaxErrorException {
        super.validate(args, values, variableContainer, context);
    }

    public String[] getCommandNames() {
        return new String[]{"typeline"};
    }

    public int execute(List args, Map values, ScriptingContext context) throws SyntaxErrorException, IOException {

        // Validate
        Map typeParams = new HashMap();
        validate(args, values, typeParams, context);

        Map pressParams = new HashMap();
        List l = new ArrayList(3);
        l.add("enter");
        super.validatePress(l, null, pressParams, context);

        // Read the count number and remove it from the type command parameter table
        int count = typeParams.containsKey(PARAM_COUNT) ? ((Number) typeParams.get(PARAM_COUNT)).intValue() : 1;
        typeParams.remove(PARAM_COUNT);

        // Read the Wait parameter and remove it from the Type command param table
        Number wait = null;
        if (typeParams.containsKey(PARAM_WAIT)) {
            wait = (Number) typeParams.get(PARAM_WAIT);
            typeParams.remove(PARAM_WAIT);
        }

        // Read the Wait parameter and remove it from the Type command param table
        Number delay = null;
        if (typeParams.containsKey(PARAM_DELAY)) {
            delay = (Number) typeParams.get(PARAM_DELAY);
            typeParams.remove(PARAM_DELAY);
        }

        if (System.getProperty("vncrobot.keyevent.debug") != null) {
            System.out.println("TYPELINE: '" + values + "'");
        }

        try {
            for (int i = 0; i < count; i++) {
                handleTypeEvent(context, typeParams);

                // Add the 'wait' param to the last execution of the Press command
                if (i == count - 1 && wait != null) {
                    pressParams.put(PARAM_WAIT, wait);
                } else if (delay != null) {
                    // If there's a delay set, add it as a 'wait' parameter to all Press calls except the last one
                    pressParams.put(PARAM_WAIT, delay);
                }
                handlePressEvent(args, pressParams, context);
            }
            return 0;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public List getPreferences() {
        return null;
    }

    public List getStablePopupMenuItems() {
        return null;
    }

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }
}
