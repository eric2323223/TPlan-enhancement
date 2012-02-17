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
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.remoteclient.capabilities.KeyTransferCapable;
import com.tplan.robot.scripting.SyntaxErrorException;

import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.commands.CommandEvent;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Handler implementing functionality of the {@doc.cmd Type} command.
 * @product.signature
 */
public class TypeCommand extends PressCommand {

    private final String PARAM_TEXT = "text";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static Map contextAttributes;

    /**
     * Get a map with context attributes.
     *
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes() {
        if (contextAttributes == null) {
            contextAttributes = new HashMap();
            ResourceBundle res = ApplicationSupport.getResourceBundle();
            contextAttributes.put(PARAM_WAIT, res.getString("command.param.wait"));
            contextAttributes.put(PARAM_COUNT, res.getString("command.param.count"));
            contextAttributes.put(PARAM_LOCATION, res.getString("press.param.location"));
        }
        return contextAttributes;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as this command has a mandatory argument,
     * which is a text to be typed.
     *
     * @return description of the mandatory argument.
     */
    public String getContextArgument() {
        return ApplicationSupport.getString("type.argument");
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext context) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        String parName;
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        Object value;

        if (args.size() < 1) {
            throw new SyntaxErrorException(res.getString("type.syntaxErr.generic"));
        }

        // Process the first argument which should be a text
        parName = args.get(0).toString();
        value = (String) values.get(parName);

        if (value != null && !value.toString().trim().equals("")) {
            // If there's a param=value pair instead of a plain argument, report an error
            throw new SyntaxErrorException(res.getString("type.syntaxErr.invalidText"));
        }
        vt.put(PARAM_TEXT, parName);

        TokenParser parser = context.getParser();

        // Now proceed to other arguments
        for (int j = 1; j < args.size(); j++) {
            parName = args.get(j).toString().toLowerCase();
            value = values.get(parName);
            value = value == null ? "" : value;

            if (parName.equals(PARAM_WAIT)) {
                vt.put(PARAM_WAIT, parser.parseTime(value, PARAM_WAIT));
            } else if (parName.equals(PARAM_COUNT)) {
                vt.put(PARAM_COUNT, parser.parseNumber(value, PARAM_COUNT));
            } else if (parName.equals(PARAM_DELAY)) {
                vt.put(PARAM_DELAY, parser.parseTime(value, PARAM_DELAY));
            } else if (parName.equals(PARAM_LOCATION)) {
                int loc;
                if (value instanceof Number) {
                    loc = ((Number) value).intValue();
                } else {
                    loc = parser.parseKeyLocation(value.toString());
                }
                vt.put(PARAM_LOCATION, loc);
            } else {
                String s = res.getString("command.syntaxErr.unknownParam");
                throw new SyntaxErrorException(MessageFormat.format(s, parName));
            }
        }
        if (context.getClient() != null && (!(context.getClient() instanceof KeyTransferCapable)
                || !((KeyTransferCapable) context.getClient()).isKeyTransferSupported())) {
            String s = res.getString("press.syntaxErr.pressNotSupportedByClient");
            throw new SyntaxErrorException(MessageFormat.format(s, context.getClient().getDisplayName()));
        }
    }

    protected void validatePress(List args, Map values, Map variableContainer, ScriptingContext context) throws SyntaxErrorException {
        super.validate(args, values, variableContainer, context);
    }

    public String[] getCommandNames() {
        return new String[]{"type"};
    }

    public int execute(List args, Map values, ScriptingContext context) throws SyntaxErrorException, IOException {

        Map t = new HashMap();

        // Validate
        validate(args, values, t, context);

        try {
            if (System.getProperty("vncrobot.keyevent.debug") != null) {
                System.out.println("TYPE: '" + values + "'");
            }

            handleTypeEvent(context, t);
            return 0;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return 1;
        }
    }

    protected void handleTypeEvent(ScriptingContext context, Map params) throws InterruptedException, IOException {
        KeyEvent evt = null;
        TestScriptInterpret interpret = context.getInterpret();
        Component component = context.getEventSource();
        String strToType = (String) params.get(PARAM_TEXT);
        int length = strToType.length();
        int count = params.containsKey(PARAM_COUNT) ? ((Number) params.get(PARAM_COUNT)).intValue() : 1;
        int location = params.containsKey(PARAM_LOCATION) ? (Integer) params.get(PARAM_LOCATION) : KeyEvent.KEY_LOCATION_UNKNOWN;
        UserConfiguration cfg = context.getConfiguration();

        // Update in 2.3.1/2.0.6 - Loading of parameters made more robust
        int delay = 25;
        if (params.containsKey(PARAM_DELAY)) {
            delay = ((Number) params.get(PARAM_DELAY)).intValue();
        } else {
            delay = getIntegerSafely(cfg, "TypeCommand.charDelay", 25);
        }
        
        int pressReleaseDelay = getIntegerSafely(cfg, "TypeCommand.pressReleaseDelay", 25);
        int multipleKeysDelay = getIntegerSafely(cfg, "TypeCommand.multiKeyDelay", 25);

        KeyTransferCapable client = (KeyTransferCapable) context.getClient();

        // Fake the shift only if it is configured
        boolean fakeShiftPreference = getBooleanSafely(cfg, "PressCommand.fakeShift", true);
        int modifier;

        for (int j = 0; j < count; j++) {
            for (int i = 0; i < length && !interpret.isStop(); i++) {
                char c = strToType.charAt(i);
                modifier = 0;
                if (Character.isLetter(c) && c >= 'A' && c <= 'Z' && fakeShiftPreference) {
                    modifier = KeyEvent.SHIFT_MASK;
                }

                // Bug 2951673: Support variable values with new line characters
                int keyCode = c == 0xa ? 0xff0d : KeyEvent.VK_UNDEFINED;
                c = c == 0xa ? 0xff0d : c;
                // ---

                evt = new KeyEvent(component,
                        KeyEvent.KEY_PRESSED,
                        System.currentTimeMillis(),
                        modifier,
                        keyCode,
                        c,
                        location);
                client.sendKeyEvent(evt);
                fireCommandEvent(this, context, CommandEvent.KEY_EVENT, evt);
                Thread.sleep(pressReleaseDelay);
                evt = new KeyEvent(component,
                        KeyEvent.KEY_RELEASED,
                        System.currentTimeMillis(),
                        modifier,
                        keyCode,
                        c,
                        location);
                client.sendKeyEvent(evt);
                fireCommandEvent(this, context, CommandEvent.KEY_EVENT, evt);
                if (i < length - 1 && delay > 0) {
                    Thread.sleep(delay);
                }
            }
            if (j < count - 1) {
                Thread.sleep(multipleKeysDelay);
            }

        }

        if (evt != null) {
            context.put(ScriptingContext.CONTEXT_LAST_GENERATED_CLIENT_EVENT, evt);
        }

        // If the 'delay' parameter has ben specified, wait for the specified amount of time
        if (params.containsKey(PARAM_WAIT) && !interpret.isStop()) {
            int wait = ((Number) params.get(PARAM_WAIT)).intValue();
            wait(context, wait);
        }

    }

    @Override
    public List<Preference> getPreferences() {
        List v = new ArrayList();
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        String containerName = res.getString("options.type.group.calibration");

        Preference o = new Preference("TypeCommand.pressReleaseDelay",
                Preference.TYPE_INT,
                res.getString("options.type.pressReleaseDelay.name"),
                MessageFormat.format(res.getString("options.type.pressReleaseDelay.desc"), ApplicationSupport.APPLICATION_NAME));
        o.setPreferredContainerName(containerName);
        o.setMinValue(0);
        v.add(o);

        o = new Preference("TypeCommand.charDelay",
                Preference.TYPE_INT,
                res.getString("options.type.characterDelay.name"),
                null);
        o.setPreferredContainerName(containerName);
        o.setMinValue(0);
        v.add(o);

        o = new Preference("TypeCommand.multiKeyDelay",
                Preference.TYPE_INT,
                res.getString("options.type.countDelay.name"),
                null);
        o.setPreferredContainerName(containerName);
        o.setMinValue(0);
        v.add(o);
        return v;
    }

    public List getStablePopupMenuItems() {
        Action action = new ConfigureAction(this);
        List v = new ArrayList();
        v.add(action);
        return v;
    }

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }
}
