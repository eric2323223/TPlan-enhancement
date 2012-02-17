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

import com.tplan.robot.scripting.commands.CommandEditAction;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.imagecomparison.ImageComparisonModule;
import com.tplan.robot.remoteclient.rfb.RfbClient;
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.remoteclient.RemoteDesktopServerListener;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.GUIConstants;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.scripting.ScriptManagerImpl;
import com.tplan.robot.scripting.wrappers.TextBlockWrapper;
import com.tplan.robot.imagecomparison.ImageComparisonModuleFactory;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.util.Stoppable;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.commands.AdvancedCommandHandler;
import com.tplan.robot.scripting.commands.BreakAction;
import com.tplan.robot.scripting.commands.ExtendedParamsObject;
import com.tplan.robot.scripting.commands.TimerAction;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import com.tplan.robot.util.CaseTolerantHashMap;
import com.tplan.robot.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import static com.tplan.robot.scripting.commands.impl.CompareToCommand.*;
import static com.tplan.robot.scripting.ScriptingContext.*;

/**
 * Handler implementing functionality of the {@doc.cmd WaitFor} command.
 * @product.signature
 */
public class WaitforCommand extends AbstractCommandHandler implements AdvancedCommandHandler {

    public static final String PARAM_TIMEOUT = "timeout";
    public static final String PARAM_ONTIMEOUT = "ontimeout";
    public static final String PARAM_AREA = "area";
    public static final String PARAM_EXTENT = "extent";
    final String PARAM_EVENT = "event";
    public static final String PARAM_CUMULATIVE = "cumulative";
    public static final String PARAM_TEMPLATE = "template";
    public static final String PARAM_INTERVAL = "interval";
    final String PARAM_EXTENT_PERCENTAGE = "extent_percentage";
    final String PARAM_EXTENT_PIXELS = "extent_pixels";
    final String PARAM_UPDATE_AREA_PIXEL_COUNT = "update_pixels";
    public static final String EVENT_BELL = "BELL";
    public static final String EVENT_UPDATE = "UPDATE";
    public static final String EVENT_MATCH = "MATCH";
    public static final String EVENT_MISMATCH = "MISMATCH";
    public static final String EVENT_CLIPBOARD = "CLIPBOARD";
    public static final String ACTION_EDIT_WAITFOR = "waitforProperties";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static Map contextAttributes;
    private static final String PARAM_MODULEPARAMS = "modparams";

    /**
     * Get a map with context attributes.
     *
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes() {
        if (contextAttributes == null) {
            contextAttributes = new HashMap();
            ResourceBundle res = ApplicationSupport.getResourceBundle();
            contextAttributes.put(PARAM_TIMEOUT, res.getString("command.param.wait"));
            contextAttributes.put(PARAM_ONPASS, res.getString("command.param.onpass"));
            contextAttributes.put(PARAM_ONTIMEOUT, res.getString("command.param.onpass"));
            contextAttributes.put(PARAM_AREA, res.getString("waitfor.param.area"));
            contextAttributes.put(PARAM_WAIT, res.getString("command.param.wait"));
            contextAttributes.put(PARAM_COUNT, res.getString("command.param.count"));
            contextAttributes.put(PARAM_EXTENT, res.getString("waitfor.param.extent"));
            contextAttributes.put(PARAM_CUMULATIVE, "true|false");
            contextAttributes.put(PARAM_TEMPLATE, res.getString("screenshot.param.template"));
            contextAttributes.put(PARAM_PASSRATE, res.getString("compareto.param.passrate"));
            contextAttributes.put(PARAM_INTERVAL, res.getString("waitfor.param.interval"));
            String methods = "";
            for (Object s : ImageComparisonModuleFactory.getInstance().getAvailableModules()) {
                methods += s.toString() + "|";
            }
            if (methods.endsWith("|")) {
                methods = methods.substring(0, methods.length() - 1);
            }
            contextAttributes.put(PARAM_METHOD, methods);
            contextAttributes.put(PARAM_METHODPARAMS, res.getString("compareto.param.methodparams"));
            contextAttributes.put(PARAM_CMPAREA, res.getString("compareto.param.area"));
        }
        return contextAttributes;
    }

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as this command has a mandatory argument,
     * which is an event identifier.
     *
     * @return description of the mandatory argument.
     */
    public String getContextArgument() {
        return EVENT_BELL.toLowerCase() + "|" + EVENT_UPDATE.toLowerCase() + "|" +
                EVENT_MATCH.toLowerCase() + "|" + EVENT_MISMATCH.toLowerCase() + "|" + EVENT_CLIPBOARD.toLowerCase();
    }

    /**
     * Validate that the command has correct syntax.
     * This method is also supposed to parse command parameters and save them into member variables of this class.
     *
     * @throws SyntaxErrorException An exception is thrown when the command syntax is incorrect.
     */
    public void validate(List args, Map values, Map variableContainer, ScriptingContext ctx) throws SyntaxErrorException {
        values = new CaseTolerantHashMap(values);  // Make the parsed map case tolerant
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        String parName;
        Object value;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        if (args.size() < 1) {
            String s = res.getString("waitfor.syntaxErr.generic");
            throw new SyntaxErrorException(MessageFormat.format(s, getContextArgument()));
        }

        // Process the first argument which should be a valid event name
        parName = args.get(0).toString();
        value = values.get(parName);
        value = value == null ? "" : value;
        if (value instanceof String) {
            value = ((String) value).trim();
        }

        if (!(parName.equalsIgnoreCase(EVENT_BELL) || parName.equalsIgnoreCase(EVENT_UPDATE) ||
                parName.equalsIgnoreCase(EVENT_MATCH) || parName.equalsIgnoreCase(EVENT_MISMATCH) ||
                parName.equalsIgnoreCase(EVENT_CLIPBOARD))) {
            String s = res.getString("waitfor.syntaxErr.invalidEvent");
            throw new SyntaxErrorException(MessageFormat.format(s, parName, getContextArgument().replace("|", ", ")));
        }
        vt.put(PARAM_EVENT, parName);

        UserConfiguration cfg = ctx.getConfiguration();
        TokenParser parser = ctx.getParser();
        ScriptManager handler = ctx.getScriptManager();
        boolean ignoreMissingTemplate = cfg.getBoolean("CompareToCommand.ignoreMissingTemplates").booleanValue();

        ImageComparisonModule mod;
        Object m = values.get(PARAM_METHOD);
        if (m instanceof ImageComparisonModule) {
            mod = (ImageComparisonModule) m;
        } else {
            try {
                mod = ImageComparisonModuleFactory.getInstance().getModule((String) m);
                if (mod == null) {
                    String s = res.getString("compareto.syntaxErr.invalidMethod");
                    throw new SyntaxErrorException(MessageFormat.format(s, m));
                }
            } catch (IllegalArgumentException ex) {
                throw new SyntaxErrorException(res.getString("compareto.syntaxErr.methodEmpty"));
            }
        }
        List<String> modParams = new ArrayList();
        if (mod instanceof ExtendedParamsObject) {
            modParams.addAll(((ExtendedParamsObject) mod).getParameters());
        }
        Map<String, String> modParamMap = new HashMap();
        vt.put(PARAM_MODULEPARAMS, modParamMap);

        // Now proceed to other arguments
        for (int j = 1; j < args.size(); j++) {
            parName = args.get(j).toString().toLowerCase();
            value = values.get(parName);
            value = value == null ? "" : value;
            if (value instanceof String) {
                value = ((String) value).trim();
            }

            if (parName.equals(PARAM_TIMEOUT)) {
                vt.put(PARAM_TIMEOUT, value instanceof Number ? value : parser.parseTime(value, PARAM_TIMEOUT));
            } else if (parName.equals(PARAM_WAIT)) {
                vt.put(PARAM_WAIT, value instanceof Number ? value : parser.parseTime(value, PARAM_WAIT));
            } else if (parName.equals(PARAM_ONTIMEOUT)) {
                vt.put(PARAM_ONFAIL, value);
            } else if (parName.equals(PARAM_METHOD)) {
                vt.put(PARAM_METHOD, mod);
            } else if (parName.equals(PARAM_METHODPARAMS)) {
                vt.put(PARAM_METHODPARAMS, value);
            } else if (parName.equals(CompareToCommand.PARAM_CMPAREA)) {
                vt.put(CompareToCommand.PARAM_CMPAREA, value instanceof Rectangle ? value : parser.parseRectangle(value, CompareToCommand.PARAM_CMPAREA));
            } else if (parName.equals(PARAM_ONPASS)) {
                vt.put(PARAM_ONPASS, value);
            } else if (parName.equals(PARAM_COUNT)) {
                vt.put(PARAM_COUNT, value instanceof Number ? value : parser.parseNumber(value, PARAM_COUNT));
            } else if (parName.equals(PARAM_CUMULATIVE)) {
                vt.put(PARAM_CUMULATIVE, value instanceof Boolean ? value : parser.parseBoolean(value, PARAM_CUMULATIVE));
            } else if (parName.equals(PARAM_TEMPLATE)) {
                // Will be validated later on
                vt.put(PARAM_TEMPLATE, value);
            } else if (parName.equals(PARAM_PASSRATE)) {
                vt.put(PARAM_PASSRATE, value instanceof Number ? value : parser.parsePercentage(value, PARAM_PASSRATE));
            } else if (parName.equals(PARAM_INTERVAL)) {
                vt.put(PARAM_INTERVAL, value instanceof Number ? value : parser.parseTime(value, PARAM_INTERVAL));
            } else if (parName.equals(PARAM_EXTENT)) {
                String ex = value.toString();
                if (ex.endsWith("%")) {
                    vt.put(PARAM_EXTENT_PERCENTAGE, parser.parsePercentage(value, PARAM_EXTENT));
                } else {
                    if (ex.endsWith("px")) {
                        value = ex.substring(0, ex.indexOf("px"));
                    } else if (ex.endsWith("p")) {
                        value = ex.substring(0, ex.indexOf("p"));
                    }
                    vt.put(PARAM_EXTENT_PIXELS, parser.parseNumber(value, PARAM_EXTENT));
                }
            } else if (parName.equals(PARAM_AREA)) {
                Rectangle rectangle = value instanceof Rectangle ? (Rectangle) value : parser.parseRectangle(value, PARAM_AREA);
                vt.put(PARAM_AREA, rectangle);

                // Validate the rectangle
                RemoteDesktopClient rfb = ctx.getClient();
                if (rfb.isConnected()) {
                    if (rectangle.x + rectangle.width > rfb.getDesktopWidth()) {
                        String s = res.getString("screenshot.syntaxErr.widthExceeded");
                        throw new SyntaxErrorException(MessageFormat.format(s, parName, rfb.getDesktopWidth()));
                    }
                    if (rectangle.y + rectangle.height > rfb.getDesktopHeight()) {
                        String s = res.getString("screenshot.syntaxErr.heightExceeded");
                        throw new SyntaxErrorException(MessageFormat.format(s, parName, rfb.getDesktopHeight()));
                    }
                }

            } else if (Utils.containsIgnoreCase(modParams, parName)) {
                modParamMap.put((String) parName, value.toString());
                try {
                    ((ExtendedParamsObject) mod).setParameters(modParamMap);
                } catch (Exception e) {
                    throw new SyntaxErrorException(e.getMessage());
                }
            } else {
                String s = res.getString("command.syntaxErr.unknownParam");
                throw new SyntaxErrorException(MessageFormat.format(s, parName));
            }
        }

        if (mod == null) {
            String s = res.getString("compareto.syntaxErr.invalidMethod");
            throw new SyntaxErrorException(MessageFormat.format(s, value));
        }

        // Validate existence of obligatory parameters
        String event = (String) vt.get(PARAM_EVENT);
        if (EVENT_MATCH.equalsIgnoreCase(event) || EVENT_MISMATCH.equalsIgnoreCase(event)) {
            if (mod == null || (mod.isSecondImageRequired() && !vt.containsKey(PARAM_TEMPLATE))) {
                String s = res.getString("waitfor.syntaxErr.paramMandatory");
                throw new SyntaxErrorException(MessageFormat.format(s, PARAM_TEMPLATE));
            }

            if (vt.containsKey(PARAM_TEMPLATE)) {
                vt.put(PARAM_TEMPLATE, CompareToCommand.parseAndValidateTemplates(vt.get(PARAM_TEMPLATE), ctx, !ignoreMissingTemplate));
            }
        }

        validateOnPassAndOnFail(ctx, vt);

        // Update the implicit variables with dummy ones
        Map vars = ctx.getVariables();
        vars.put(ScriptingContext.WAITUNTIL_TIMEOUT, "false");
        if (vt.get(PARAM_EVENT).toString().equalsIgnoreCase(EVENT_UPDATE)) {
            updateRectVariables(ctx, vars, new Rectangle());
        }
    }

    public String[] getCommandNames() {
        return new String[]{"waitfor"};
    }

    public int execute(List args, Map values, ScriptingContext repository) throws SyntaxErrorException {

        Map params = new HashMap();

        // Validate
        validate(args, values, params, repository);

        try {
            return handleWaitUntilEvent(repository, params);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 2;
    }

    protected int handleWaitUntilEvent(ScriptingContext repository, Map params) throws InterruptedException {
        int delay = params.containsKey(PARAM_WAIT) ? ((Number) params.get(PARAM_WAIT)).intValue() : -1;

        WaitForListener l = new WaitForListener(params, repository);
        ScriptManager handler = repository.getScriptManager();

        if (l.conditionReached && delay > 0) {

            // Wait for the required amount of miliseconds
            wait(repository, delay);
        }

        if (l.conditionReached && params.containsKey(PARAM_ONPASS) && repository.getInterpret() instanceof ProprietaryTestScriptInterpret) {
            ((ProprietaryTestScriptInterpret) repository.getInterpret()).runBlock(
                    new TextBlockWrapper((String) params.get(PARAM_ONPASS), true), repository);
        }

        // Clear the status bar
        UserConfiguration cfg = repository.getConfiguration();
        if (cfg.getBoolean("WaitUntilCommand.showCountDown").booleanValue()) {
            handler.fireScriptEvent(new ScriptEvent(this, null, repository, ""));
        }

        if (l.templateNotFound) {
            return 2;
        } else {
            return l.conditionReached ? 0 : 1;
        }
    }

    protected void updateRectVariables(ScriptingContext ctx, Map variables, Rectangle r) {
        if (r != null) {
            variables.put(ScriptingContext.WAITUNTIL_X, new Integer(r.x));
            variables.put(ScriptingContext.WAITUNTIL_Y, new Integer(r.y));
            variables.put(ScriptingContext.WAITUNTIL_W, new Integer(r.width));
            variables.put(ScriptingContext.WAITUNTIL_H, new Integer(r.height));
        } else { // Remove variables
            variables.remove(ScriptingContext.WAITUNTIL_X);
            variables.remove(ScriptingContext.WAITUNTIL_Y);
            variables.remove(ScriptingContext.WAITUNTIL_W);
            variables.remove(ScriptingContext.WAITUNTIL_H);
        }
        ctx.getScriptManager().fireScriptEvent(new ScriptEvent(this, null, ctx, ScriptEvent.SCRIPT_VARIABLES_UPDATED));
    }

    public List<Preference> getPreferences() {
        List v = new ArrayList();
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        Preference o = new Preference("WaitUntilCommand.showCountDown", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.displayCountdownMsg.name"),
                null);
        o.setPreferredContainerName(res.getString("options.waitfor.group.countDown"));
        v.add(o);

        o = new Preference("WaitUntilCommand.defaultInterval", Preference.TYPE_INT,
                res.getString("options.waitfor.defaultInterval.name"),
                res.getString("options.waitfor.defaultInterval.desc"));
        o.setPreferredContainerName(res.getString("options.waitfor.group.matchPrefs"));
        o.setMinValue(1);
        v.add(o);
        return v;
    }

    public List getStablePopupMenuItems() {
        List v = new ArrayList();
        Action action = new CommandEditAction(this, ACTION_EDIT_WAITFOR);
        v.add(action);
        action = new ConfigureAction(this);
        v.add(action);
        return v;
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns true as no VNC connection is needed for the waitfor command.
     */
    public boolean canRunWithoutConnection() {
        return true;
    }

    public List getArguments(String command, ScriptingContext context) {
        return Arrays.asList(new String[]{EVENT_BELL.toLowerCase(),
                    EVENT_CLIPBOARD.toLowerCase(), EVENT_MATCH.toLowerCase(),
                    EVENT_MISMATCH.toLowerCase(), EVENT_UPDATE.toLowerCase()});
    }

    public List getParameters(String command, ScriptingContext context) {
        List params = new ArrayList();
        Map<String, String> t = new CaseTolerantHashMap(context.getParser().parseParameters(command, params));
        if (params.size() > 0) {
            String arg = (String) params.get(0);
            List common = new ArrayList(Arrays.asList(new String[]{PARAM_WAIT, PARAM_TIMEOUT, PARAM_ONPASS, PARAM_ONTIMEOUT}));
            if (arg.equalsIgnoreCase(EVENT_BELL)) {
                common.addAll(Arrays.asList(new String[]{PARAM_COUNT}));
                return common;
            } else if (arg.equalsIgnoreCase(EVENT_UPDATE)) {
                common.addAll(Arrays.asList(new String[]{PARAM_COUNT, PARAM_EXTENT, PARAM_AREA, PARAM_CUMULATIVE}));
                return common;
            } else if (arg.equalsIgnoreCase(EVENT_MATCH) || arg.equalsIgnoreCase(EVENT_MISMATCH)) {
                common.addAll(Arrays.asList(new String[]{PARAM_CMPAREA, PARAM_INTERVAL, PARAM_PASSRATE, PARAM_TEMPLATE, PARAM_METHOD}));
                ImageComparisonModule m = ImageComparisonModuleFactory.getInstance().getModule(t.get(PARAM_METHOD));
                if (m != null) {
                    if (m.isMethodParamsSupported()) {
                        common.add(PARAM_METHODPARAMS);
                    }
                    if (m instanceof ExtendedParamsObject) {
                        common.addAll(((ExtendedParamsObject) m).getParameters());
                    }
                }
                return common;
            } else if (arg.equalsIgnoreCase(EVENT_CLIPBOARD)) {
                return common;
            }
        }
        return null;

    }

    public List getParameterValues(String paramName, String command, ScriptingContext context) {
        if (paramName != null) {
            Map<String, String> t = new CaseTolerantHashMap(context.getParser().parseParameters(command));
            if (paramName.equalsIgnoreCase(PARAM_CUMULATIVE)) {
                return Arrays.asList(new String[]{"true", "false"});
            } else if (paramName.equalsIgnoreCase(CompareToCommand.PARAM_METHOD)) {
                List<String> l = ImageComparisonModuleFactory.getInstance().getAvailableModules();
                for (int i = 0; i < l.size(); i++) {
                    l.set(i, l.get(i).toLowerCase());
                }
                return l;
            } else if (paramName.equalsIgnoreCase(CompareToCommand.PARAM_CMPAREA) || paramName.equalsIgnoreCase(PARAM_AREA)) {
                List l = new ArrayList();
                l.add(new Rectangle());
                return l;
            } else if (paramName.equalsIgnoreCase(PARAM_TEMPLATE)) {
                List l = new ArrayList();
                List objectList = new ArrayList();
                objectList.add(getTemplateFileChooser(t.get(paramName), context, context.getTemplateDir()));
                try {
                    File dummy = context.getTemplateDir().getCanonicalFile();
                    objectList.add(dummy);
                } catch (IOException e) {
                }
                objectList.add(new Boolean(true));  // Force relative resolution
                l.add(objectList);
                return l;
            } else if (!getContextAttributes().containsKey(paramName.toLowerCase())) {
                ImageComparisonModule m = ImageComparisonModuleFactory.getInstance().getModule(t.get(PARAM_METHOD));
                if (m instanceof ExtendedParamsObject) {
                    return ((ExtendedParamsObject) m).getParameterValues(paramName);
                }
            }
        }
        return null;
    }

    private class WaitForListener
            implements RemoteDesktopServerListener, GUIConstants, ActionListener, PropertyChangeListener, Runnable {

        private int counter = 0;
        private int count = 1;
        private String event = null;
        private int pixels = 0;
        private Rectangle rectangle;
        private Rectangle cmpArea;
        private List cumulativeRects;
        private float percentage = 100;
        private int delay = 0;
        private int updateAreaPixelCount = 0;
        private Map variables;
        private long endTime = 0;
        private Timer updateTimer = null;
        public boolean conditionReached = false;
        private boolean cumulative;
        private float passrate = 95f;
        boolean ignoreMissingTemplate;
        private long interval;
        private Timer comparisonTimer = null;
        private List<ImageComparisonModule> comparisonModules;
        private String methodParams;
        private ScriptingContext context;
        private RemoteDesktopClient client;
        private UserConfiguration cfg;
        private boolean match = true;
        private boolean debug = System.getProperty("debug.waitfor") != null;
        private Thread comparisonThread = null;
        private boolean templateNotFound = false;
        private List inputTemplates;
        private List<Image> templateImages;

        WaitForListener(Map params, ScriptingContext context) {
            this.context = context;
            cfg = context.getConfiguration();
            try {
                String method = null;
                Object o = params.get(PARAM_METHOD);
                if (o instanceof ImageComparisonModule) {
                    method = ((ImageComparisonModule) o).getMethodName();
                } else if (o != null) {
                    method = o.toString();
                }
                if (method == null) {
                    method = UserConfiguration.getInstance().getString("CompareToCommand.defaultComparisonModule");
                }
                method = method.toLowerCase();
                if ("search".equalsIgnoreCase(method)) {
                    passrate = cfg.getDouble("CompareToCommand.defaultSearchPassRate").floatValue();
                } else {
                    passrate = cfg.getDouble("CompareToCommand.defaultPassRate").floatValue();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                passrate = 95f;
            }
            try {
                ignoreMissingTemplate = cfg.getBoolean("CompareToCommand.ignoreMissingTemplates").booleanValue();
            } catch (Exception ex) {
                ex.printStackTrace();
                ignoreMissingTemplate = true;
            }

            try {
                interval = cfg.getInteger("WaitUntilCommand.defaultInterval").intValue() * 1000;
            } catch (Exception ex) {
                ex.printStackTrace();
                interval = 3000;
            }

            count = params.containsKey(PARAM_COUNT) ? ((Number) params.get(PARAM_COUNT)).intValue() : 1;
            event = (String) params.get(PARAM_EVENT);
            rectangle = (Rectangle) params.get(PARAM_AREA);
            percentage = params.containsKey(PARAM_EXTENT_PERCENTAGE) ? ((Number) params.get(PARAM_EXTENT_PERCENTAGE)).floatValue() : 100f;
            pixels = params.containsKey(PARAM_EXTENT_PIXELS) ? ((Number) params.get(PARAM_EXTENT_PIXELS)).intValue() : 0;
            delay = params.containsKey(PARAM_WAIT) ? ((Number) params.get(PARAM_WAIT)).intValue() : 0;
            updateAreaPixelCount = params.containsKey(PARAM_UPDATE_AREA_PIXEL_COUNT)
                    ? ((Number) params.get(PARAM_UPDATE_AREA_PIXEL_COUNT)).intValue() : 0;
            cumulative = params.containsKey(PARAM_CUMULATIVE) ? ((Boolean) params.get(PARAM_CUMULATIVE)).booleanValue() : false;
            interval = params.containsKey(PARAM_INTERVAL) ? ((Number) params.get(PARAM_INTERVAL)).intValue() : interval;
            passrate = params.containsKey(PARAM_PASSRATE) ? ((Number) params.get(PARAM_PASSRATE)).floatValue() : passrate;

            variables = context.getVariables();
            updateRectVariables(context, variables, null);
            variables.put(ScriptingContext.WAITUNTIL_TIMEOUT, "false");
            methodParams = (String) params.get(PARAM_METHODPARAMS);
            cmpArea = (Rectangle) params.get(CompareToCommand.PARAM_CMPAREA);
            inputTemplates = (List) params.get(PARAM_TEMPLATE);

            client = context.getClient();

            Object currentElement = context.get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT);

            String event = (String) params.get(PARAM_EVENT);
            String onTimeout = (String) params.get(PARAM_ONFAIL);
            rectangle = params.containsKey(PARAM_AREA) ? (Rectangle) params.get(PARAM_AREA) : new Rectangle();
            params.put(PARAM_AREA, rectangle);

            ScriptManager handler = context.getScriptManager();
            TestScriptInterpret interpret = context.getInterpret();

            // If the width and height param haven't been set, update them to the frame buffer width/height
            int w = client.getDesktopWidth();
            int h = client.getDesktopHeight();
            if (event.equalsIgnoreCase(EVENT_UPDATE)) {
                if (rectangle.width == 0) {
                    rectangle.width = w - rectangle.x;
                }
                if (rectangle.height == 0) {
                    rectangle.height = h - rectangle.y;
                }
                updateAreaPixelCount = rectangle.width * rectangle.height;
            }

            if (cmpArea != null) {
                Rectangle r = new Rectangle(0, 0, w, h);
                cmpArea = cmpArea.intersection(r);
            }
            boolean matching = false;

            // If the timeout is set, set the end time
            if (params.containsKey(PARAM_TIMEOUT)) {
                endTime = ((Number) params.get(PARAM_TIMEOUT)).intValue() + System.currentTimeMillis();
            }

            if (event.equalsIgnoreCase(EVENT_MATCH) || event.equalsIgnoreCase(EVENT_MISMATCH)) {
                matching = true;
                match = event.equalsIgnoreCase(EVENT_MATCH);

                // TODO: Check if the comparison module requires templates.
                templateImages = null;
                try {
                    templateImages = CompareToCommand.convertToImageList((List) params.get(PARAM_TEMPLATE), context);
                } catch (SyntaxErrorException ex) {
                    ex.printStackTrace();
                }

                // Null means that at least one template failed to be read while
                // ignoring of missing templates was off
                if (templateImages == null) {
                    templateNotFound = true;
                    if (debug) {
                        System.out.println("- Waitfor " + (match ? "match" : "mismatch") + " leaving (main thread is #" + Thread.currentThread().getId() + "), template image not found");
                    }
                    return;
                }

                // There might be 1 to N templates. We create a comparison module
                // for each template for performance reasons because we set the
                // template as comparison base image.
                ImageComparisonModule comparisonModule;
                String method = null;
                Object mod = params.get(PARAM_METHOD);

                if (mod instanceof ImageComparisonModule) {
                    comparisonModule = (ImageComparisonModule) mod;
                    method = comparisonModule.getMethodName();
                } else {
                    method = (String) mod;
                    comparisonModule = ImageComparisonModuleFactory.getInstance().getModule(method);
                }
                List<ImageComparisonModule> modules = new ArrayList();
                if (!comparisonModule.isSecondImageRequired()) {
                    modules.add(comparisonModule);
                } else {
                    if (templateImages.size() == 0) {
                        templateNotFound = true;
                        return;
                    }
                    Map<String, String> modParams = (Map<String, String>) params.get(PARAM_MODULEPARAMS);
                    modules.add(comparisonModule);
                    comparisonModule.setBaseImage(templateImages.get(0));
                    if (comparisonModule instanceof ExtendedParamsObject && modParams != null) {
                        ((ExtendedParamsObject) comparisonModule).setParameters(modParams);
                    }
                    for (int i = 1; i < templateImages.size(); i++) {
                        comparisonModule = ImageComparisonModuleFactory.getInstance().getModule(method);
                        modules.add(comparisonModule);
                        comparisonModule.setBaseImage(templateImages.get(i));
                        if (comparisonModule instanceof ExtendedParamsObject && modParams != null) {
                            ((ExtendedParamsObject) comparisonModule).setParameters(modParams);
                        }
                    }
                }
                this.comparisonModules = modules;

                // Image comparison is handled by a timer which fires an action event
                // at scheduled intervals
                comparisonTimer = new Timer((int) interval, this);
                comparisonTimer.setRepeats(true);
                comparisonTimer.start();
                actionPerformed(new ActionEvent(comparisonTimer, 0, ""));
            }

            BreakAction action = new BreakAction(new TimerAction(endTime, context));

            try {
                // Handle the press key event
                fireCommandEvent(this, context, EVENT_ADD_CUSTOM_ACTION_MSG, action);

                // If timeout is set, start the timer which will display countdown in the status bar
                if (endTime > 0) {
                    updateTimer = new Timer(1000, this);
                    updateTimer.setRepeats(true);
                    updateTimer.start();
                }

                client.addServerListener(this);

                // First proceed the list of events
                List v = (List) context.get(ScriptingContext.CONTEXT_RFB_EVENT_LIST);
                for (int i = 0; i < v.size(); i++) {
                    if (!conditionReached) {
                        serverMessageReceived((RemoteDesktopServerEvent) v.get(i));
                    }
                }

                // Stay in the waiting loop until the script gets stopped, the condition of the event are met or
                // timeout is reached
                while (!interpret.isStop() && !conditionReached && !action.isBreak()) {

                    // If waiting is interrupted by user, behave as if passed
                    if (action.breakCountDown) {
                        conditionReached = true;
                    }

                    if (endTime > 0 && System.currentTimeMillis() > endTime) {
                        // Set up one last comparison
                        if (matching) {
                            comparisonTimer.stop();
                            if (debug) {
                                System.out.println("-- Thread #" + Thread.currentThread().getId() + " (main thread): Timeout reached, performing one last comparison");
                            }
                            stopAllComparisons(interpret);
                            comparisonThread = null;
                            actionPerformed(new ActionEvent(comparisonTimer, 0, ""));
                            do {
                                Thread.sleep(5);
                            } while (comparisonThread != null && !interpret.isStop());
                            if (interpret.isStop() && comparisonModules != null) {
                                if (debug) {
                                    System.out.println("-- Thread #" + Thread.currentThread().getId() + " (main thread): Stop requested, interrupting image comparison");
                                }
                                stopAllComparisons(interpret);
                            }
                        }
                        if (!conditionReached && !interpret.isStop()) {
                            variables.put(ScriptingContext.WAITUNTIL_TIMEOUT, "true");
                            client.removeServerListener(this);

                            if (onTimeout != null && this.context.getInterpret() instanceof ProprietaryTestScriptInterpret) {
                                ((ProprietaryTestScriptInterpret) this.context.getInterpret()).runBlock(new TextBlockWrapper(onTimeout, true), this.context);
                            }
                            break;
                        }
                    }
                    Thread.sleep(5);
                }
                if (interpret.isStop() && comparisonModules != null) {
                    if (debug) {
                        System.out.println("-- Thread #" + Thread.currentThread().getId() + "(main thread): Stop requested, interrupting image comparison");
                    }
                    stopAllComparisons(interpret);
                }

            } catch (Exception ex) {
                System.out.println(ApplicationSupport.getString("waitfor.internalError"));
                ex.printStackTrace();
            }
            fireCommandEvent(this, context, EVENT_REMOVE_CUSTOM_ACTION_MSG, action);
            client.removeServerListener(this);

            // Stop the countdown timer if it exists
            if (updateTimer != null) {
                updateTimer.stop();
                updateTimer.removeActionListener(this);
                updateTimer = null;

                // Clear the status bar
                handler.fireScriptEvent(new ScriptEvent(this, null, context, ""));
            }

            if (comparisonTimer != null) {
                comparisonTimer.stop();
                comparisonTimer.removeActionListener(this);
                comparisonTimer = null;
            }

            synchronized (this) {
                comparisonModules = null;
            }
        }

        private void stopAllComparisons(TestScriptInterpret interpret) {
            if (comparisonModules != null) {
                for (ImageComparisonModule comparisonModule : comparisonModules) {
                    if (comparisonModule instanceof Stoppable) {
                        ((Stoppable) comparisonModule).stop();
                        do {
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException ex) {
                            }
                        } while (comparisonThread != null && !interpret.isStop());
                    }
                }
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(updateTimer)) {
                if (endTime >= System.currentTimeMillis()) {
                    if (cfg.getBoolean("WaitUntilCommand.showCountDown").booleanValue()) {
                        Object params[] = {
                            Utils.getTimePeriodForDisplay(endTime - System.currentTimeMillis() + 1000, false),
                            ApplicationSupport.getString("command.WaitCommand.continueLabel")
                        };
                        context.getScriptManager().fireScriptEvent(new ScriptEvent(
                                this, null, context, MessageFormat.format(ApplicationSupport.getString("command.WaitCommand.waitStatusBarMsg"), params)));
                    }
                } else {
                    synchronized (this) {
                        if (updateTimer != null) {
                            updateTimer.stop();
                        }
                    }
                    // Clear the status bar
                    context.getScriptManager().fireScriptEvent(new ScriptEvent(this, null, context, ""));
                }
            } else if (e.getSource().equals(comparisonTimer)) {
                if (comparisonModules != null) {
                	
                    if (comparisonThread == null) {
                        Thread t = new Thread(this);
                        t.start();
                    } else if (debug) {
                        System.out.println("-- Thread #" + Thread.currentThread().getId() + ": Skipping comparison after interval " + interval + "ms because the previous one hasn't finished yet");
                    }
                } else {
                    if (ignoreMissingTemplate) {
                        this.conditionReached = true;
                    } else {
                        endTime = System.currentTimeMillis() - 1;
                    }
                }
            }
        }

        public void run() {
            comparisonThread = Thread.currentThread();

            // Compare the images and get the result as percentage.
            long time = System.currentTimeMillis();
            if (debug) {
                String p = client.getLastMouseEvent() == null
                        ? "[unknown]"
                        : "[" + client.getLastMouseEvent().getX() + "," + client.getLastMouseEvent().getY() + "]";
                String method = comparisonModules != null && comparisonModules.size() > 0 ? comparisonModules.get(0).getMethodName() : "<unknown>";
                System.out.println("-- Thread #" + Thread.currentThread().getId() + ": Starting comparison, pass rate=" + passrate + "%, method=" +
                        method + ", interval=" + interval + ", mouse at " + p + ", cmparea=" + cmpArea);
            }

            float rate = 0;

            // Bug 2941023 - WaitFor match throws NPE intermittently
            // It was caused by another thread setting the list of modules to null.
            ImageComparisonModule comparisonModule;
            int i = 0;
            for (; comparisonModules != null && i < comparisonModules.size(); i++) {

                synchronized (this) {
                    try {
                        comparisonModule = comparisonModules.get(i);
                    } catch (NullPointerException ex) {
                        ex.printStackTrace();
                        i = -1;
                        conditionReached = false;
                        break;
                    }

                    // Security update in 2.0.2 - handle exceptions thrown by the module correctly
                    try {
                        rate = 100 * comparisonModule.compareToBaseImage(client.getImage(), cmpArea, methodParams, context, passrate / 100f);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        rate = 0;
                    }

                    if (debug) {
                        System.out.println("-- Thread #" + Thread.currentThread().getId() + ": Comparison finished, result=" + rate + "%, comparison time=" + (System.currentTimeMillis() - time) + "ms, required pass rate=" + passrate);
                    }

                    if (match) {   // We are waiting for a match
                        if (rate >= passrate) {
                            if (debug) {
                                System.out.println("-- Thread #" + Thread.currentThread().getId() + ": Pass rate is greater than the given passrate " + passrate + "%. Finishing the Waitfor match listener.");
                            }
                            this.conditionReached = true;
                            if (comparisonTimer != null) {
                                comparisonTimer.stop();
                            }
                            break;
                        }
                    } else {   // We are waiting for a mismatch
                        if (rate < passrate) {
                            if (debug) {
                                System.out.println("-- Thread #" + Thread.currentThread().getId() + ": Pass rate is lower than the given passrate " + passrate + "%. Finishing the Waitfor mismatch listener.");
                            }
                            this.conditionReached = true;
                            if (comparisonTimer != null) {
                                comparisonTimer.stop();
                            }
                            break;
                        }
                    }
                }
            }

            // Bug fix: Populate the _COMPARETO* variables
            Map vars = context.getVariables();
            vars.remove(COMPARETO_TEMPLATE);
            vars.remove(COMPARETO_TEMPLATE_INDEX);
            vars.remove(COMPARETO_TEMPLATE_WIDTH);
            vars.remove(COMPARETO_TEMPLATE_HEIGHT);
            vars.put(COMPARETO_TIME_IN_MS, "" + (System.currentTimeMillis() - time));

            if (conditionReached && i >= 0) {

                // Update the implicit variables
                vars.put(COMPARETO_RESULT, "" + rate);
                vars.put(COMPARETO_PASS_RATE, "" + passrate);
                vars.put(COMPARETO_TEMPLATE_INDEX, "" + i);

                if (inputTemplates != null && inputTemplates.size() > i) {
                    Object o = inputTemplates.get(i);
                    if (o instanceof String || o instanceof File) {
                        vars.put(COMPARETO_TEMPLATE, o.toString());
                    }
                }
                if (templateImages != null && templateImages.size() > i) {
                    Image ti = templateImages.get(i);
                    vars.put(COMPARETO_TEMPLATE_WIDTH, "" + ti.getWidth(context.getEventSource()));
                    vars.put(COMPARETO_TEMPLATE_HEIGHT, "" + ti.getHeight(context.getEventSource()));

                }

            } else {
                vars.put(COMPARETO_RESULT, "0");
            }
            comparisonThread = null;
        }

        public void serverMessageReceived(RemoteDesktopServerEvent evt) {

            boolean conditionReached = false;
            if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_BELL_EVENT && event.equalsIgnoreCase(EVENT_BELL)) {
                counter++;
                if (counter >= count) {   // Specified number of BELL messages was reached
                    conditionReached = true;
                }
            } else if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_UPDATE_EVENT && event.equalsIgnoreCase(EVENT_UPDATE)) {
                Rectangle overlap = evt.getUpdateRect().intersection(rectangle);

                if (!overlap.isEmpty()) {
                    long updatedPixelCount;

                    // We are waiting for a cumulative update
                    if (cumulative) {
                        if (cumulativeRects == null) {
                            cumulativeRects = new ArrayList();
                        }
                        long time = System.currentTimeMillis();
                        updatedPixelCount = calculateCumulativeUpdate(rectangle, overlap);

                        if (System.getProperty("debug.waitfor") != null) {
                            time = System.currentTimeMillis() - time;
                            // Calculate the percent change
                            float updatePercentage = 100 * ((float) updatedPixelCount / updateAreaPixelCount);
                            System.out.println("Waitfor: Cumulative update info:\n   count time = " + time + " ms\n   result=" + updatedPixelCount + "px" +
                                    "\n   calculated percentage update=" + updatePercentage + "%");
                        }
                    } else {  // We are looking for a regular, one-time update
                        updatedPixelCount = overlap.width * overlap.height;
                    }

                    // Pixels limit set
                    if (pixels > 0) {

                        // If the number of updated pixels exceeds the given limit, update the counter
                        if (updatedPixelCount >= pixels) {
                            updateRectVariables(context, variables, evt.getUpdateRect());
                            counter++;
                        }

                    // Percentage rate set
                    } else if (percentage > 0) {

                        // Calculate the percent change
                        float updatePercentage = 100 * ((float) updatedPixelCount / updateAreaPixelCount);

                        // If the change exceeds the given limit, update the counter
                        if (updatePercentage >= percentage) {
                            updateRectVariables(context, variables, evt.getUpdateRect());
                            if (cfg.getBoolean("WaitUntilCommand.showCountDown").booleanValue()) {
                                String t = ApplicationSupport.getString("waitfor.statusBarMsg.updatePassed");
                                String s = MessageFormat.format(t, overlap.x, overlap.y, overlap.width, overlap.height, updatePercentage);
                                context.getScriptManager().fireScriptEvent(new ScriptEvent(this, null, context, s));
                                if (System.getProperty("debug.waitfor") != null) {
                                    System.out.println(s);
                                }
                            }
                            counter++;
                        }
                    }

                    if (counter >= count) {   // Specified number of SCREENUPDATE messages was reached
                        conditionReached = true;
                        ln = null;
                    }
                }
            } else if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_CLIPBOARD_EVENT && event.equalsIgnoreCase(EVENT_CLIPBOARD)) {
                conditionReached = true;
            }

            // If the delay param is set, wait the specified amount of time. Otherwise set the conditionReached flag directly.
            if (conditionReached) {
                ((RfbClient) evt.getSource()).removeServerListener(this);
                if (delay > 0) {

                    // If the endTime is set, update it only if our end time is lower. This will preserve
                    // the functionality of preset timeout.
                    endTime = System.currentTimeMillis() + delay;
                    this.conditionReached = true;
                } else {
                    this.conditionReached = true;
                }
            }
        }
        private List[] ln;

        private long calculateCumulativeUpdate(Rectangle rect, Rectangle overlap) {
            if (ln == null) {
                ln = new List[rect.height];
            }

            int end = overlap.y - rect.y + overlap.height;
            List v;
            for (int i = overlap.y - rect.y; i < end; i++) {
                v = ln[i];
                if (v == null) {
                    v = new ArrayList();
                    ln[i] = v;
                }
                adjustLine(v, new Interval(overlap.x, overlap.x + overlap.width));
            }

            return countPixels(ln);
        }

        private long countPixels(List[] lines) {
            long count = 0;
            List v;
            Interval iv;
            for (int i = 0; lines != null && i < lines.length; i++) {
                v = (List) lines[i];
                for (int j = 0; v != null && j < v.size(); j++) {
                    iv = (Interval) v.get(j);
                    count += iv.end - iv.start + 1;
                }
            }
            return count;
        }

        private void adjustLine(List v, Interval p) {
            Interval cp = null;

            if (v.size() > 0) {
                int size = v.size();
                for (int i = 0; i < size;) {
                    cp = (Interval) v.get(i);
                    if (cp.intersects(p)) {
                        v.remove(cp);
                        p.join(cp);
                        if (!v.contains(p)) {
                            v.add(i, p);
                        } else {
                            i--;
                        }
                    } else if (p.isBelow(cp) && !v.contains(p)) {
                        v.add(i, p);
                    }
                    size = v.size();
                    i++;
                }
                if (!v.contains(p)) {
                    v.add(p);
                }
            } else {
                v.add(p);
            }
        }

        private class Interval {

            int start;
            int end;

            Interval(int start, int end) {
                this.start = start;
                this.end = end;
            }

            Interval join(Interval i) {
                if (!intersects(i)) {
                    return null;
                }
                start = Math.min(start, i.start);
                end = Math.max(end, i.end);
                return this;
            }

            boolean contains(Interval i) {
                return start <= i.start && end >= i.end;
            }

            boolean intersects(Interval i) {
                return (i.start >= start && i.start <= end) || (i.end >= start && i.end <= end) || (end >= i.start && end <= i.end);
            }

            boolean isBelow(Interval i) {
                return end < i.start;
            }

            boolean isAbove(Interval i) {
                return start > i.end;
            }

            public String toString() {
                return "[" + start + "," + end + "]";
            }
        }
//        private void testCumulativeMethod() {
//            List v = new ArrayList();
//            v.add(new Interval(5,10));
//            v.add(new Interval(50,80));
//            v.add(new Interval(90,100));
//            v.add(new Interval(150,180));
//
//            adjustLine(v, new Interval(100,180));
//            System.out.println("Test #1: "+v);
//            adjustLine(v, new Interval(3,12));
//            System.out.println("Test #2: "+v);
//            adjustLine(v, new Interval(190,200));
//            System.out.println("Test #3: "+v);
//            adjustLine(v, new Interval(180,190));
//            System.out.println("Test #4: "+v);
//            adjustLine(v, new Interval(0,220));
//            System.out.println("Test #5: "+v);
//        }
//        private long calculateCumulativeUpdate(Rectangle rect, Rectangle overlap, List cumulativeRects) {
////            System.out.println("cumulative update");
//            if (px == null) {
//                px = new byte[rect.height][rect.width];
//                for (int i = 0; i < px.length; i++) {
//                    Arrays.fill(px[i], (byte) 0);
//                }
//            }
//            int end = overlap.y - rect.y + overlap.height;
////            System.out.println(" -- fill from "+(overlap.y-rect.y)+" to "+end);
//            for (int i = overlap.y - rect.y; i < end; i++) {
////                System.out.println(" -- filling row #"+i+" start="+(overlap.x-rect.x)+" end="+(overlap.x-rect.x+overlap.width));
//                Arrays.fill(px[i], overlap.x - rect.x, overlap.x - rect.x + overlap.width, (byte) 1);
//            }
//
//            long cnt = 0;
//            for (int i = 0; i < rect.height; i++) {
//                for (int j = 0; j < rect.width; j++) {
//                    cnt += px[i][j];
//                }
//            }
////            System.out.println(" -- cnt="+cnt);
//            return cnt;
//        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("replayFinished")) {
                if (updateTimer != null && updateTimer.isRunning()) {
                    updateTimer.stop();
                }
                if (comparisonTimer != null && comparisonTimer.isRunning()) {
                    comparisonTimer.stop();
                }
                ln = null;
                if (evt.getSource() instanceof ScriptManagerImpl) {
                    ((ScriptManagerImpl) evt.getSource()).removePropertyChangeListener(this);
                }
            }
        }
    }
}
