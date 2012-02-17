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
import com.tplan.robot.gui.GUIConstants;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.util.Utils;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.SyntaxErrorException;

import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.commands.BreakAction;
import com.tplan.robot.scripting.commands.TimerAction;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Handler implementing functionality of the {@doc.cmd Wait} command.
 * @product.signature
 */
public class WaitCommand extends AbstractCommandHandler implements GUIConstants {

    final String PARAM_TIME = "time";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);

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
        return ApplicationSupport.getString("command.param.wait");
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext repository) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        String parName;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        if (args.size() != 1) {
            throw new SyntaxErrorException(res.getString("wait.syntaxErr.generic"));
        }

        // Process the first argument which should be a number
        Object o = args.get(0);
        Number time;
        if (o instanceof Number) {
            time = (Number) o;
        } else {
            parName = args.get(0).toString();

            try {
                TokenParser parser = repository.getParser();
                time = parser.parseTime(parName, "");
            } catch (SyntaxErrorException e) {
                String s = res.getString("wait.syntaxErr.invalidTime");
                throw new SyntaxErrorException(MessageFormat.format(s, parName));
            }
        }
        if (time.floatValue() < 0) {
            throw new SyntaxErrorException(res.getString("wait.syntaxErr.timeValueNegative"));
        }
        vt.put(PARAM_TIME, time);
    }

    public String[] getCommandNames() {
        return new String[]{"wait"};
    }

    public int execute(List args, Map values, ScriptingContext repository) throws SyntaxErrorException {

        // Validate
        Map params = new HashMap();
        validate(args, values, params, repository);

        try {
            handleWaitEvent(repository, params);
            return 0;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 1;
    }

    protected void handleWaitEvent(ScriptingContext repository, Map params) throws InterruptedException {
        new WaitListener(params, repository, repository.getConfiguration());
    }

    public List<Preference> getPreferences() {
        List v = new ArrayList();
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        Preference o = new Preference(
                "WaitCommand.showCountDown",
                Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.displayCountdownMsg.name"),
                null);
        o.setPreferredContainerName(res.getString("options.waitfor.group.countDown"));
        v.add(o);
        return v;
    }

    private class WaitListener implements ActionListener {

        private long endTime;
        ScriptingContext repository;

        WaitListener(Map params, ScriptingContext context, UserConfiguration cfg) {
            try {
                this.repository = context;
                long time = ((Number) params.get(PARAM_TIME)).longValue();
                endTime = System.currentTimeMillis() + time;
                ScriptManager handler = context.getScriptManager();
                Object currentElement = context.get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT);
                BreakAction action = new BreakAction(new TimerAction(endTime, context));
                TestScriptInterpret interpret = context.getInterpret();
                fireCommandEvent(this, context, EVENT_ADD_CUSTOM_ACTION_MSG, action);

                Timer updateTimer = new Timer(1000, this);
                updateTimer.setRepeats(true);
                updateTimer.start();

                while (System.currentTimeMillis() < endTime && !interpret.isStop() && !action.isBreak()) {
                    Thread.sleep(5);
                }

                updateTimer.stop();
                fireCommandEvent(this, context, EVENT_REMOVE_CUSTOM_ACTION_MSG, action);

                // Clear the status bar
                handler.fireScriptEvent(new ScriptEvent(this, null, context, ""));

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void actionPerformed(ActionEvent e) {
            Boolean b = repository.getConfiguration().getBoolean("WaitCommand.showCountDown");
            if (b == null || b.booleanValue()) {
                Object params[] = {
                    Utils.getTimePeriodForDisplay(endTime - System.currentTimeMillis() + 1000, false),
                    ApplicationSupport.getString("command.WaitCommand.continueLabel")
                };
                repository.getScriptManager().fireScriptEvent(new ScriptEvent(
                        this, null, repository, MessageFormat.format(ApplicationSupport.getString("command.WaitCommand.waitStatusBarMsg"), params)));
            }
        }
    }

    public void registerStablePopupMenuItems() {
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns true as no VNC connection is needed for the wait command.
     */
    public boolean canRunWithoutConnection() {
        return true;
    }
}
