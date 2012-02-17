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
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.util.Utils;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.dialogs.KeyBrowserDialog;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.remoteclient.capabilities.KeyTransferCapable;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.scripting.ScriptManagerImpl;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.TokenParser;

import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.commands.AdvancedCommandHandler;
import com.tplan.robot.scripting.commands.CommandEvent;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import javax.help.CSH;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Handler implementing functionality of the {@doc.cmd Press} command.
 * @product.signature
 */
public class PressCommand extends AbstractCommandHandler implements AdvancedCommandHandler {

    private final String PARAM_KEYCODE = "keycode";
    private final String PARAM_KEYCHAR = "keychar";
    private final DisplaySupportedKeysAction keyBrowserAction = new DisplaySupportedKeysAction();
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static Map contextAttributes;
    public static final String PARAM_LOCATION = "location";
    public static final String PARAM_DELAY = "delay";
    public static final String PARAM_RELEASE = "release";
    // Key location constants
    public static final String PARAM_LOCATION_STANDARD = "standard";
    public static final String PARAM_LOCATION_NUMPAD = "numpad";
    public static final String PARAM_LOCATION_LEFT = "left";
    public static final String PARAM_LOCATION_RIGHT = "right";

    /**
     * Get a map with context attributes.
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes() {
        if (contextAttributes == null) {
            contextAttributes = new HashMap();
            ResourceBundle res = ApplicationSupport.getResourceBundle();
            contextAttributes.put(PARAM_WAIT, res.getString("command.param.wait"));
            contextAttributes.put(PARAM_COUNT, res.getString("command.param.count"));
            contextAttributes.put(PARAM_LOCATION, res.getString("press.param.location"));
            contextAttributes.put(PARAM_DELAY, res.getString("press.param.delay"));
            contextAttributes.put(PARAM_RELEASE, "true|false");
        }
        return contextAttributes;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as this command has a mandatory argument,
     * which is a key identifier.
     *
     * @return description of the mandatory argument.
     */
    public String getContextArgument() {
        return ApplicationSupport.getString("press.argument");
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext context) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        String parName;
        Object value;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        if (args.size() < 1) {
            throw new SyntaxErrorException(res.getString("press.syntaxErr.generic"));
        }

        // Process the first argument which should be in the form of '<modifier_1>+...+<modifier_N>+<key>'
        String keys[];
        try {
            parName = args.get(0).toString();
            keys = parName.split("\\+");

            // Bug 2934936 fix in 2.0.2: Check if the character is "+" or "<modifier>++"
            if (parName.equals("+") || parName.endsWith("++")) {
                int l = keys.length;
                String newKeys[] = new String[l + 1];
                System.arraycopy(keys, 0, newKeys, 0, l);
                newKeys[l] = "+";
                keys = newKeys;
            }

            // Process the last token -> it must be the key like F1 to F12, A, B, ...
            value = keys[keys.length - 1];
            String key = value.toString();
            char c = KeyEvent.CHAR_UNDEFINED;
            int keyCode = KeyEvent.VK_UNDEFINED;

            // If the key id is one char, it is for sure an ASCII one.
            // We don't populate the code in the event in such a case.
            if (key.length() == 1) {
                c = key.charAt(0);
            } else {
                keyCode = Utils.getKeyCode(key);
                if (keyCode == KeyEvent.VK_UNDEFINED) {
                    String s = res.getString("press.syntaxErr.unknownKeyOrModifier");
                    throw new SyntaxErrorException(MessageFormat.format(s, key));
                }
            }
            vt.put(PARAM_KEYCODE, new Integer(keyCode));
            vt.put(PARAM_KEYCHAR, c);

        } catch (Exception ex) {
            throw new SyntaxErrorException(res.getString("press.syntaxErr.generic"));
        }

        String token;

        // Check all the modifiers and also the key
        for (int i = 0; i < keys.length - 1; i++) {
            token = keys[i].toUpperCase();

            // This condition enables to pass commands without a key, such as 'Press Ctrl' etc.
            if (!token.equals(SHIFT) && !token.equals(CTRL) && !token.equals(ALT) && !token.equals(WINDOWS)) {
                // Invalid modifier
                String s = res.getString("press.syntaxErr.unknownModifier");
                throw new SyntaxErrorException(MessageFormat.format(s, keys[i]));
            }
            vt.put(token, token);
        }

        TokenParser parser = context.getParser();

        // Now proceed to other arguments
        for (int j = 1; j < args.size(); j++) {
            parName = args.get(j).toString().toLowerCase();
            value = values.get(parName);
            value = value == null ? "" : value;

            if (parName.equals(PARAM_COUNT)) {
                vt.put(PARAM_COUNT, parser.parseNumber(value, PARAM_COUNT));
            } else if (parName.equals(PARAM_WAIT)) {
                vt.put(PARAM_WAIT, parser.parseTime(value, PARAM_WAIT));
            } else if (parName.equals(PARAM_DELAY)) {
                vt.put(PARAM_DELAY, parser.parseTime(value, PARAM_WAIT));
            } else if (parName.equals(PARAM_RELEASE)) {
                vt.put(PARAM_RELEASE, parser.parseBoolean(value, PARAM_RELEASE));
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

    public String[] getCommandNames() {
        return new String[]{"press"};
    }

    public int execute(List args, Map values, ScriptingContext context) throws SyntaxErrorException, IOException {

        // Validate
        Map t = new HashMap();
        validate(args, values, t, context);

        try {
            if (System.getProperty("vncrobot.keyevent.debug") != null) {
                System.out.println("PRESS: '" + values + "'");
            }

            handlePressEvent(args, t, context);
            return 0;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 1;
    }

    protected void handlePressEvent(List args, Map params, ScriptingContext context) throws InterruptedException, IOException {
        KeyEvent evt = null, pressEvt = null;

        int keyCode = ((Number) params.get(PARAM_KEYCODE)).intValue();
        char character = params.containsKey(PARAM_KEYCHAR) ? ((Character) params.get(PARAM_KEYCHAR)).charValue() : KeyEvent.CHAR_UNDEFINED;
        int location = params.containsKey(PARAM_LOCATION) ? (Integer) params.get(PARAM_LOCATION) : KeyEvent.KEY_LOCATION_STANDARD;
        Boolean release = (Boolean) params.get(PARAM_RELEASE);

        // Fake the shift only if it is configured.
        // Fix in 2.0.1: Do not fake if there are any modifiers because
        // then for example Ctrl+A would translate to Ctrl+Shift+a.
        boolean shouldFakeShift = false;

        // Update in 2.3.1/2.0.6 - Loading of parameters made more robust
        boolean fakeShiftPreference = getBooleanSafely(context.getConfiguration(), "PressCommand.fakeShift", true);
        if (fakeShiftPreference) {
            shouldFakeShift = character >= 'A' && character <= 'Z';

            // Bug 2900718 fix: Do not fake shift when there are other modifiers
            if (params.containsKey(ALT) || params.containsKey(CTRL) || params.containsKey(WINDOWS)) {
                shouldFakeShift = false;
            }
        }

        int modifiers = 0;

        if (params.containsKey(ALT)) {
            modifiers = modifiers | KeyEvent.ALT_MASK;
            shouldFakeShift = false;  // Do not fake shift if there's Alt
        }
        if (params.containsKey(CTRL)) {
            modifiers = modifiers | KeyEvent.CTRL_MASK;
            shouldFakeShift = false;  // Do not fake shift if there's Ctrl
        }
        if (params.containsKey(WINDOWS)) {
            modifiers = modifiers | KeyEvent.META_MASK;
            shouldFakeShift = false;  // Do not fake shift if there's Meta
        }
        if (shouldFakeShift || params.containsKey(SHIFT)) {
            modifiers = modifiers | KeyEvent.SHIFT_MASK;
        }

        int count = 1;
        if (params.containsKey(PARAM_COUNT)) {
            count = ((Number) params.get(PARAM_COUNT)).intValue();
        }

        Component component = context.getEventSource();
        ScriptManager handler = context.getScriptManager();
        KeyTransferCapable client = (KeyTransferCapable) context.getClient();
        TestScriptInterpret interpret = context.getInterpret();

        UserConfiguration cfg = context.getConfiguration();
        int pressReleaseDelay;
        if (params.containsKey(PARAM_DELAY)) {
            pressReleaseDelay = ((Number) params.get(PARAM_DELAY)).intValue();
        } else {
            pressReleaseDelay = getIntegerSafely(context.getConfiguration(), "PressCommand.pressReleaseDelay", 50);
        }

        int multipleKeysDelay = getIntegerSafely(context.getConfiguration(), "PressCommand.multiKeyDelay", 200);
        boolean valid = keyCode != KeyEvent.VK_UNDEFINED || character != KeyEvent.CHAR_UNDEFINED;

        if (valid && (interpret == null || !interpret.isStop())) {
            for (int j = 0; j < count && (interpret == null || !interpret.isStop()); j++) {
                if (release == null || !release) {
                    evt = new KeyEvent(component,
                            KeyEvent.KEY_PRESSED,
                            System.currentTimeMillis(),
                            modifiers,
                            keyCode,
                            character,
                            location);
                    pressEvt = evt;
                    if (j == count - 1 && handler != null && handler instanceof ScriptManagerImpl) {
                        ((ScriptManagerImpl) handler).setRfbServerEventRecording(true);
                    }
                    client.sendKeyEvent(evt);
                    fireCommandEvent(this, context, CommandEvent.KEY_EVENT, evt);
                }
                if (release == null) {
                    Thread.sleep(pressReleaseDelay);
                }

                if (release == null || release) {
                    evt = new KeyEvent(component,
                            KeyEvent.KEY_RELEASED,
                            System.currentTimeMillis(),
                            modifiers,
                            keyCode,
                            character,
                            location);
                    client.sendKeyEvent(evt);
                    fireCommandEvent(this, context, CommandEvent.KEY_EVENT, evt);
                    if (j < count - 1) {
                        Thread.sleep(multipleKeysDelay);
                    }
                }

                if (release != null) {
                    break;  // No logical way to press or release the key multiple times
                }
            }
        }

        if (pressEvt != null) {
            context.put(ScriptingContext.CONTEXT_LAST_GENERATED_CLIENT_EVENT, pressEvt);
        }

        // If the 'delay' parameter has ben specified, wait for the specified amount of time
        if (params.containsKey(PARAM_WAIT) && (interpret == null || !interpret.isStop())) {
            int sleep = ((Number) params.get(PARAM_WAIT)).intValue();
            wait(context, sleep);
        }
    }

    @Override
    public List<Preference> getPreferences() {
        List v = new ArrayList();
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        Preference o = new Preference("PressCommand.pressReleaseDelay",
                Preference.TYPE_INT,
                res.getString("options.press.pressReleaseDelay.name"),
                MessageFormat.format(res.getString("options.press.pressReleaseDelay.desc"), ApplicationSupport.APPLICATION_NAME));
        o.setPreferredContainerName(res.getString("options.press.group.calibration"));
        o.setMinValue(0);
        v.add(o);

        o = new Preference("PressCommand.multiKeyDelay",
                Preference.TYPE_INT,
                res.getString("options.press.keyDelay.name"),
                null);
        o.setPreferredContainerName(res.getString("options.press.group.calibration"));
        o.setMinValue(0);
        v.add(o);

        o = new Preference("PressCommand.fakeShift",
                Preference.TYPE_BOOLEAN,
                res.getString("options.press.fakeShifts.name"),
                res.getString("options.press.fakeShifts.desc"));
        o.setPreferredContainerName(res.getString("options.press.group.prefs"));
        o.setMinValue(0);
        v.add(o);
        return v;
    }

    public List getStablePopupMenuItems() {
        List v = super.getStablePopupMenuItems();
        v.add(0, keyBrowserAction);
        return v;
    }

    public DisplaySupportedKeysAction getKeyBrowserAction() {
        return keyBrowserAction;
    }

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    public List getArguments(String command, ScriptingContext context) {
        return Arrays.asList(new String[]{"\"<" + getContextArgument() + ">\""});
    }

    public List getParameters(String command, ScriptingContext context) {
        return Arrays.asList(new String[]{PARAM_COUNT, PARAM_LOCATION, PARAM_WAIT, PARAM_DELAY, /*PARAM_RELEASE*/});
    }

    public List getParameterValues(String paramName, String command, ScriptingContext context) {
        if (paramName != null) {
            if (paramName.equalsIgnoreCase(PARAM_LOCATION)) {
                return Arrays.asList(new String[]{PARAM_LOCATION_LEFT, PARAM_LOCATION_NUMPAD, PARAM_LOCATION_RIGHT, PARAM_LOCATION_STANDARD});
            } else if (paramName.equalsIgnoreCase(PARAM_RELEASE)) {
                return Arrays.asList(new String[]{"true", "false"});

            }
        }
        return null;
    }

    public class DisplaySupportedKeysAction extends AbstractAction {

        KeyBrowserDialog dlg;

        public DisplaySupportedKeysAction() {
            String label = ApplicationSupport.getString("press.action.keyBrowserMenuItem"); //MainFrame.getResourceString("command.WaitCommand.continueLabel");
            putValue(SHORT_DESCRIPTION, label);
            putValue(NAME, label);
        }

        public void actionPerformed(ActionEvent e) {
            if (dlg == null) {
                dlg = new KeyBrowserDialog(MainFrame.getInstance(), ApplicationSupport.getString("press.action.keyBrowserWindowTitle"), false);
                CSH.setHelpIDString(dlg, "gui.keybrowser");
            }
            final String columns[] = {
                ApplicationSupport.getString("press.action.keyBrowserNameColumn"),
                ApplicationSupport.getString("press.action.keyBrowserCodeColumn")
            };
            dlg.setValues(Utils.getKeyCodeTable(), columns, true);
            dlg.pack();
            dlg.setLocationRelativeTo(MainFrame.getInstance());
            dlg.setVisible(true);
        }
    }
}
