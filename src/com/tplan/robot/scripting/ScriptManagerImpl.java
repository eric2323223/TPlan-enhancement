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
package com.tplan.robot.scripting;

import com.tplan.robot.preferences.ConfigurationChangeEvent;
import com.tplan.robot.scripting.commands.*;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.plugin.PluginEvent;
import com.tplan.robot.remoteclient.rfb.RfbConstants;
import com.tplan.robot.remoteclient.rfb.RfbClient;
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.remoteclient.RemoteDesktopServerListener;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.DesktopViewer;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.plugin.PluginListener;
import com.tplan.robot.preferences.ConfigurationChangeListener;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.commands.CommandListener;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.util.Utils;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import static com.tplan.robot.scripting.ScriptingContext.*;

/**
 * Script manager implementation.
 *
 * @product.signature
 */
public class ScriptManagerImpl extends JPanel implements ScriptManager, CommandListener,
        RfbConstants, RemoteDesktopServerListener, PluginListener, ConfigurationChangeListener {

    private List<CommandListener> commandListeners = new ArrayList();
    private final List<ScriptListener> scriptListeners = new ArrayList();
    private boolean running = false;
    private boolean stop = false;
    private Map<String, CommandHandler> commandMap = initCommandHandlers();
    private Map<String, String> cliVariables = new HashMap();
    private ScriptingContext context;
    private List rfbServerEvents = new ArrayList();
    private boolean rfbServerEventRecording = false;
    private boolean resetRfbServerEventRecording = false;
    private Map<String, Object> scriptingParams = null;
    private long startTime;
    RemoteDesktopClient client;
    ResourceBundle res = ApplicationSupport.getResourceBundle();
    List<TestScriptInterpret> interprets = new ArrayList();
    private static List<ExecutionListener> execInterestedObjects = null;
    public boolean debugEvents = false;
    /**
     * The following array lists regexp patterns and values of dynamically created variables.
     * Any occurence of a variable will be replaced with it's default fullName provided that
     * no other fullName is available. The purpose of this code is to prevent the script validator
     * from reporting errors in code which uses these variables. See bug 13030.
     */
    private final static Object DEFAULT_VARS[][] = {
        {"_SEARCH_X", "-1"},
        {"_SEARCH_Y", "-1"},
        {"_SEARCH_X_[0-9]*", "-1"},
        {"_SEARCH_Y_[0-9]*", "-1"},
        {"_SEARCH_MATCH_COUNT", "0"},
        {"[0-9]*", "0"}
    };
    /**
     * The following array lists regexp patterns and values of dynamically created variables
     * which should override variables existing in the execution context. A good example is
     * current time variable _CURTIME which should be always replaced by current time.
     */
    public final DynamicVariable DEFAULT_DYNAMIC_VARS[] = {
        new TimeObj(),
        new RandomVariableObject(),
        new RGBColorObject(),
        new DateObj(),
        new MouseCoordObj(true),
        new MouseCoordObj(false),};

    public ScriptManagerImpl() {
        setContext(createDefaultContext());
        PluginManager.getInstance().addPluginListener(this);
        UserConfiguration.getInstance().addConfigurationListener(this);
    }

//    private boolean debug = "true".equals(System.getProperty("vncrobot.execution.debug"));
    public ScriptManagerImpl(Map<String, Object> scriptingParams, Map<String, String> cliVariables, RemoteDesktopClient client) {
        this();
        this.scriptingParams = scriptingParams;
        setCliVariables(cliVariables);
        setClient(client);
    }

    public void setDesktopViewer(DesktopViewer fbPanel) {
        addCommandListener(fbPanel);
    }

    public ScriptingContext createDefaultContext() {
        ScriptingContext r = new ScriptingContextImpl();
        r.put(ScriptingContext.CONTEXT_SCRIPT_MANAGER, this);
        r.put(ScriptingContext.CONTEXT_EVENT_SOURCE, this);
        r.put(ScriptingContext.CONTEXT_CLI_VARIABLE_MAP, new HashMap(cliVariables));
        r.put(ScriptingContext.CONTEXT_PROCEDURE_MAP, new HashMap());
        r.put(ScriptingContext.CONTEXT_RFB_EVENT_LIST, rfbServerEvents);
        r.put(ScriptingContext.CONTEXT_EXECUTION_START_DATE, new Date());
        r.put(ScriptingContext.CONTEXT_COMPILATION_ERRORS, new ArrayList());
        r.put(ScriptingContext.CONTEXT_PARSER, new TokenParserImpl());
        r.put(ScriptingContext.CONTEXT_USER_CONFIGURATION, UserConfiguration.getInstance());
        r.put(ScriptingContext.CONTEXT_USER_CONFIGURATION, UserConfiguration.getInstance());
        if (client != null) {
            r.put(ScriptingContextImpl.CONTEXT_CLIENT, client);
        }
        initImplicitVariables(r, true);
        return r;
    }

    private void initImplicitVariables(ScriptingContext context, boolean validateOnly) {
        // Insert the variables into a new HashMap first to prevent the map from firing
        // of too many change messages
        Map<String, Object> variables = new HashMap();
        updateHostVariables(variables, context);

        Date st = (Date) context.get(CONTEXT_EXECUTION_START_DATE);
        st = st == null ? new Date() : st;
        if (st != null) {
            variables.put(IMPLICIT_VARIABLE_TIMESTAMP, Utils.getTimeStamp(st));
            variables.put(IMPLICIT_VARIABLE_DATESTAMP, Utils.getDateStamp(st));
        }

        // Default output path is configurable in 2.0
        // Empty string switches on user home folder.
        String outputPath = Utils.getDefaultOutputPath();
        variables.put(IMPLICIT_VARIABLE_REPORT_DIR, outputPath);
        updateFileNameVariables(null, context);

        // Default template path is configurable in 2.0
        // Empty string switches on user home folder.
        String templatePath = Utils.getDefaultTemplatePath();
        variables.put(IMPLICIT_VARIABLE_TEMPLATE_DIR, templatePath);

        TestWrapper wr = context.getMasterWrapper();
        if (wr != null && wr.getScriptFile() != null) {
            String path;
            if (wr.getScriptFile().getParentFile() != null) {
                path = wr.getScriptFile().getParentFile().getAbsolutePath();
            } else {
                path = wr.getScriptFile().getAbsolutePath();
                path = path.substring(0, path.lastIndexOf(File.separator));
            }
            variables.put(IMPLICIT_VARIABLE_SCRIPT_DIR, path);
        }
        variables.put(IMPLICIT_VARIABLE_EXIT_CODE, new Integer(0));
        variables.put(IMPLICIT_VARIABLE_WARNING_COUNT, new Integer(0));

        // New to 2.0Beta
        variables.put(IMPLICIT_VARIABLE_PRODUCT_VERSION_SHORT, ApplicationSupport.APPLICATION_VERSION);
        variables.put(IMPLICIT_VARIABLE_PRODUCT_VERSION_LONG, ApplicationSupport.APPLICATION_BUILD);
        variables.put(IMPLICIT_VARIABLE_PRODUCT_NAME, ApplicationSupport.APPLICATION_NAME);
        variables.put(IMPLICIT_VARIABLE_PRODUCT_HOME_PAGE, ApplicationSupport.APPLICATION_HOME_PAGE);
        variables.put(IMPLICIT_VARIABLE_PRODUCT_INSTALL_DIR, Utils.getInstallPath());

        // New to 2.0.3 - auto population of default values through dynamic objects
        DynamicVariable dobj;
        for (int i = 0; i < DEFAULT_DYNAMIC_VARS.length; i++) {
            dobj = (DynamicVariable) DEFAULT_DYNAMIC_VARS[i];
            dobj.contextCreated(context);
        }

        // New to 2.0.3 - process also dynamic variables submitted through the plug in interface
        DynamicVariableFactory f = DynamicVariableFactory.getInstance();
        List<String> dynVars = f.getVariableNames();
        if (dynVars != null && dynVars.size() > 0) {
            for (String name : dynVars) {
                try {
                    f.getVariable(name).contextCreated(context);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        // Save the map in one batch to the context's variable map
        Utils.putAll(context.getVariables(), variables);
//        context.getVariables().putAll(variables);
    }

    /**
     * Update host and desktop variables in the context.
     *
     * @param context scripting context
     */
    private void updateHostVariables(Map<String, Object> variables, ScriptingContext context) {
        RemoteDesktopClient cl = context.getClient();
        if (cl != null && cl.isConnected()) {
            String host = (String) cl.getHost();
            variables.put(IMPLICIT_VARIABLE_MACHINE_NAME, host);
            if (cl instanceof RfbClient) {
                String display = host + ":" + Utils.numberFormat.format(cl.getPort() - RFB_PORT_OFFSET);
                variables.put(IMPLICIT_VARIABLE_DISPLAY, display);
            }
            variables.put(IMPLICIT_VARIABLE_DESKTOP_HEIGHT, Integer.toString(cl.getDesktopHeight()));
            variables.put(IMPLICIT_VARIABLE_DESKTOP_WIDTH, Integer.toString(cl.getDesktopWidth()));

            // New in 2.0
            String port = "";
            if (cl.getPort() >= 0) {
                port = "" + cl.getPort();
            } else if (cl.getDefaultPort() >= 0) {
                port = "" + cl.getDefaultPort();
            }

//            variables.put(IMPLICIT_VARIABLE_URL, cl.getProtocol() + "://" + cl.getHost() + (port.length() > 0 ? ":" + port : ""));
            variables.put(IMPLICIT_VARIABLE_URL, cl.getConnectString());
            variables.put(IMPLICIT_VARIABLE_PORT, port);
            variables.put(IMPLICIT_VARIABLE_PROTOCOL, cl.getProtocol());
        } else {
            variables.put(IMPLICIT_VARIABLE_MACHINE_NAME, "");
            variables.put(IMPLICIT_VARIABLE_DISPLAY, "");
            variables.put(IMPLICIT_VARIABLE_DESKTOP_HEIGHT, "0");
            variables.put(IMPLICIT_VARIABLE_DESKTOP_WIDTH, "0");
            variables.put(IMPLICIT_VARIABLE_URL, "");
            variables.put(IMPLICIT_VARIABLE_PORT, "");
            variables.put(IMPLICIT_VARIABLE_PROTOCOL, "");
        }
    }

    /**
     * Update implicit file name variables in the context.
     *
     * @param wr a test wrapper.
     * @param context a scripting context
     */
    private void updateFileNameVariables(TestWrapper wr, ScriptingContext context) {
        String fullName = "", shortName = "";
        if (wr != null) {
            File f = wr.getScriptFile();
            if (f != null) {
                fullName = Utils.getFullPath(f);
                shortName = f.getName();
            }
        }
        Map variables = context.getVariables();
        variables.put(IMPLICIT_VARIABLE_FILE_NAME, fullName);
        variables.put(IMPLICIT_VARIABLE_FILE_NAME_SHORT, shortName);
        fireScriptEvent(new ScriptEvent(wr == null ? this : wr, null, context, ScriptEvent.SCRIPT_VARIABLES_UPDATED));
    }

    private Map<String, CommandHandler> initCommandHandlers() {
        Map commandHandlers = new HashMap();
        CommandHandler command;
        CommandFactory fact = CommandFactory.getInstance();
        List<String> cmds = fact.getAvailableCommandNames();
        for (Object name : cmds) {
            command = fact.getCommandHandler(name.toString());
            command.addCommandListener(this);

            String names[] = command.getCommandNames();
            for (int j = 0; names != null && j < names.length; j++) {
                commandHandlers.put(names[j].toUpperCase(), command);
            }
        }
        return commandHandlers;
    }

    public Map<String, CommandHandler> getCommandHandlers() {
        return new HashMap(commandMap);
    }

    public void commandEvent(CommandEvent evt) {
        fireCommandEvent(evt);
    }

    public void addCommandListener(CommandListener listener) {
        if (!commandListeners.contains(listener)) {
            commandListeners.add(listener);
        }
    }

    public void removeCommandListener(CommandListener listener) {
        if (commandListeners.contains(listener)) {
            commandListeners.remove(listener);
        }
    }

    protected void fireCommandEvent(CommandEvent e) {
        List<CommandListener> lcl = new ArrayList(commandListeners);
        int size = lcl.size();
        CommandListener l;
        for (int i = 0; i < size && !e.isConsumed(); i++) {
            try {
                l = lcl.get(i);
                l.commandEvent(e);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public PauseRequestException fireScriptEvent(ScriptEvent evt) {
        PauseRequestException pe = null;
        switch (evt.getType()) {
            case ScriptEvent.SCRIPT_EXECUTION_STARTED:
                if (debugEvents) {
                    System.out.println("ScriptManagerImpl: firing ScriptEvent.SCRIPT_EXECUTION_STARTED (" +
                            ScriptEvent.SCRIPT_EXECUTION_STARTED + "), interpret=" + evt.getInterpret());
                }
                TestScriptInterpret ti = evt.getInterpret();
                if (ti != null && !interprets.contains(ti)) {
                    interprets.add(ti);
                }

                running = true;
                stop = false;
                startTime = System.currentTimeMillis();

                if (execInterestedObjects == null) {
                    execInterestedObjects = new ArrayList();
                    ExecutionListenerFactory ef = ExecutionListenerFactory.getInstance();
                    List<String> codes = ef.getSortedPluginCodes();
                    ExecutionListener eo;
                    for (String code : codes) {
                        try {
                            eo = ef.create(code);
                            eo.executionStarted(context);
                            execInterestedObjects.add(eo);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                break;
            case ScriptEvent.SCRIPT_EXECUTION_FINISHED:
                if (debugEvents) {
                    System.out.println("ScriptManagerImpl: firing ScriptEvent.SCRIPT_EXECUTION_FINISHED (" +
                            ScriptEvent.SCRIPT_EXECUTION_FINISHED + "), interpret=" + evt.getInterpret());
                }
                running = false;
                evt.getContext().put(CONTEXT_EXECUTION_DURATION, new Long(System.currentTimeMillis() - startTime));
                break;
            case ScriptEvent.SCRIPT_EXECUTION_STOPPED:
                if (debugEvents) {
                    System.out.println("ScriptManagerImpl: firing ScriptEvent.SCRIPT_EXECUTION_STOPPED (" +
                            ScriptEvent.SCRIPT_EXECUTION_STOPPED + "), interpret=" + evt.getInterpret());
                }
                running = false;
                evt.getContext().put(CONTEXT_EXECUTION_DURATION, new Long(System.currentTimeMillis() - startTime));
                break;
            case ScriptEvent.SCRIPT_EXECUTION_PAUSED:
                if (debugEvents) {
                    System.out.println("ScriptManagerImpl: firing ScriptEvent.SCRIPT_EXECUTION_PAUSED (" +
                            ScriptEvent.SCRIPT_EXECUTION_PAUSED + "), interpret=" + evt.getInterpret());
                }
                break;
            case ScriptEvent.SCRIPT_EXECUTION_RESUMED:
                break;
            case ScriptEvent.SCRIPT_EXECUTED_LINE_CHANGED:
                break;
            case ScriptEvent.SCRIPT_COMPILATION_STARTED:
                ti = evt.getInterpret();
                if (ti != null && !interprets.contains(ti)) {
                    interprets.add(ti);
                }
                break;
            case ScriptEvent.SCRIPT_INTERPRET_DESTROYED:
                interprets.remove(evt.getInterpret());
        }

        synchronized (scriptListeners) {
            List<ScriptListener> l = new ArrayList(scriptListeners);
            for (ScriptListener listener : l) {
                try {
                    listener.scriptEvent(evt);
                } catch (PauseRequestException pex) {
                    pe = pex;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return pe;
    }

    public void addScriptListener(ScriptListener listener) {
        synchronized (scriptListeners) {
            if (!scriptListeners.contains(listener)) {
                scriptListeners.add(listener);
            }
        }
    }

    public void removeScriptListener(ScriptListener listener) {
        synchronized (scriptListeners) {
            if (scriptListeners.contains(listener)) {
                scriptListeners.remove(listener);
            }
        }
    }

    public Map getCliVariables() {
        return cliVariables;
    }

    public void setCliVariables(Map<String, String> variables) {
        if (variables != null) {
            cliVariables.putAll(variables);
        }
    }

    public void serverMessageReceived(RemoteDesktopServerEvent evt) {
        int type = evt.getMessageType();
        TestScriptInterpret ti = getClientOwner(evt.getClient());
        if (ti != null) {
            ScriptingContext ctx = ti.getExecutionContext();
            if (ctx != null) {
                if (type == RemoteDesktopServerEvent.SERVER_CONNECTED_EVENT || type == RemoteDesktopServerEvent.SERVER_DISCONNECTED_EVENT) {
                    updateHostVariables(ctx.getVariables(), ctx);
                } else if (type == RemoteDesktopServerEvent.SERVER_CLIPBOARD_EVENT) {
                    Map variables = (Map) ctx.getVariables();

                    String s = evt.getClipboardText();
                    s = s == null ? "" : s;

                    variables.put(IMPLICIT_VARIABLE_SERVER_CLIPBOARD_CONTENT, s);
                    rfbServerEvents.add(evt);

                } else if (running && !stop && isRfbServerEventRecording()) { // TODO: move to RFB client
                    rfbServerEvents.add(evt);
                }
            }
        }
    }

    public TestScriptInterpret getClientOwner(RemoteDesktopClient client) {
        List<TestScriptInterpret> l = getExecutingTestScripts();
        for (TestScriptInterpret ti : l) {
            if (ti.getExecutionContext() != null && client.equals(ti.getExecutionContext().getClient())) {
                return ti;
            }
        }
        return null;
    }

    public boolean isRfbServerEventRecording() {
        return rfbServerEventRecording;
    }

    public void setRfbServerEventRecording(boolean rfbServerEventRecording) {
        this.rfbServerEventRecording = rfbServerEventRecording;
        rfbServerEvents.clear();
        resetRfbServerEventRecording = false;
    }

    public Object getScriptToRun() {
        return getScriptingParams() == null ? null : getScriptingParams().get("run");
    }

    public void setScriptToRun(Object scriptToRun) {
        if (getScriptingParams() != null) {
            if (scriptToRun == null) {
                getScriptingParams().remove("run");
                getScriptingParams().remove("tolabel");
                getScriptingParams().remove("fromlabel");
            } else {
                getScriptingParams().put("run", scriptToRun);
            }
        }
    }

    public boolean isOutputEnabled() {
        return (getScriptingParams() == null ? true : !scriptingParams.containsKey("nooutput"));
    }

    public void setOutputEnabled(boolean outputEnabled) {
        if (getScriptingParams() == null) {
            scriptingParams = new HashMap();
        }

        if (outputEnabled) {
            getScriptingParams().remove("nooutput");
        } else {
            getScriptingParams().put("nooutput", "true");
        }
    }

    public boolean isConsoleMode() {
        return client instanceof RfbClient ? ((RfbClient) client).isConsoleMode() : false;
    }

    public Object getDynamicVariableValue(String name, Map cliVars, Map variables, TestWrapper wr, ScriptingContext ctx) {

        // New to 2.0.3 - auto population of default values through dynamic objects
        for (DynamicVariable dobj : DEFAULT_DYNAMIC_VARS) {
            if (name.matches(dobj.getCode())) {
                return dobj.toString(name, cliVars, variables, wr, ctx);
            }
        }

        // New to 2.0.3 - process also dynamic variables submitted through the plug in interface
        // Improved later on - plugin names are compared using matches() to support
        // dynamic variables with dynamical names
        List<String> names = DynamicVariableFactory.getInstance().getVariableNames();
        if (names != null && names.size() > 0) {
            for (String code : names) {
                if (name.matches(code)) {
                    DynamicVariable dobj = DynamicVariableFactory.getInstance().getVariable(code);
                    if (dobj != null) {
                        return dobj.toString(name, cliVars, variables, wr, ctx);
                    }
                }
            }
        }
        return null;
    }

    String getApplicableValue(String name, Map cliVars, TestWrapper wr, Map variables, ScriptingContext ctx) {

        Object o = getDynamicVariableValue(name, cliVars, variables, wr, ctx);

        if (o == null) {
            // CLI variables override everything
            o = cliVars.get(name);

            // If there's no overriden fullName from CLI, try to load a variable through the wrapper method
            // getVariable(). If the result is null, look in the global variable context
            if (o == null) {
                if (wr != null && wr instanceof DocumentWrapper) {
                    o = ((DocumentWrapper) wr).getVariable(name);
                }

                if (o == null) {
                    o = variables.get(name);
                }

                // Bugfix 13030
                if (o == null) {
                    for (int i = 0; i <
                            DEFAULT_VARS.length; i++) {
                        if (name.matches(DEFAULT_VARS[i][0].toString())) {
                            o = DEFAULT_VARS[i][1];
                        }

                    }
                }
            }
        }
        return o == null ? null : getDisplayValue(o);
    }

    /**
     * Format an object for output. This method makes sure that if the object is an integer number, it gets
     * displayed without the floating point part. The method returns for any other object result
     * of the Object.toString() method.
     *
     * @param o an object.
     * @return formatted output.
     */
    private String getDisplayValue(Object o) {
        if (o instanceof Number) {
            Number n = (Number) o;
            if (n.intValue() == n.doubleValue()) {
                return Integer.toString(n.intValue());
            }

        }
        return o == null ? null : o.toString();
    }

    public String assembleFileName(String fileName, ScriptingContext repository, String variableName) {
        String implicitPath = "";
        Map cli = repository.getCommandLineVariables();
        TestWrapper wr = repository.getMasterWrapper();
        Map variables = repository.getVariables();
        implicitPath = getApplicableValue(variableName, cli, wr, variables, repository);

        if (fileName != null && implicitPath != null && !implicitPath.equals("")) {
            File f = new File(fileName);
            if (!f.isAbsolute()) {
                f = new File(implicitPath, fileName);
                fileName = Utils.getFullPath(f);
            }
        }
        return fileName;
    }

    public void setClient(RemoteDesktopClient client) {
        setClient(client, null);
    }

    public void setClient(RemoteDesktopClient client, ScriptingContext context) {
        if (this.client != null) {
            this.client.removeServerListener(this);
        }

        this.client = client;
        if (client != null) {
            client.addServerListener(this);
            if (context == null) {
                context = new ScriptingContextImpl();
            }
            context.put(CONTEXT_CLIENT, client);
            fireScriptEvent(new ScriptEvent(this, null, context, ScriptEvent.SCRIPT_CLIENT_CREATED));
        }
    }

    public RemoteDesktopClient getClient() {
        return client;
    }

    public void pluginEvent(PluginEvent e) {
        this.commandMap = initCommandHandlers();
    }

    public String replace(ScriptingContext ctx, String text, String variableName, int startOffset, int endOffset, String value) {
        return text.substring(0, startOffset) + value + text.substring(endOffset + 1);
    }

    /**
     * @param context the context to set
     */
    private void setContext(ScriptingContext context) {
        this.context = context;
    }

    /**
     * Get active test script interprets, i.e. test scripts which have been
     * opened and compiled and/or executed.
     * @return list of active test script interprets known to this script manager instance.
     */
    public List<TestScriptInterpret> getActiveTestScripts() {
        return new ArrayList<TestScriptInterpret>(interprets);
    }

    /**
     * Get list of test script interprets which are being executed.
     * @return list of active test script interprets known to this script
     * manager instance which are being executed.
     */
    public List<TestScriptInterpret> getExecutingTestScripts() {
        List<TestScriptInterpret> l = new ArrayList();
        for (TestScriptInterpret t : interprets) {
            if (t.isExecuting()) {
                l.add(t);
            }
        }
        return l;
    }

    /**
     * @return the scriptingParams
     */
    public Map<String, Object> getScriptingParams() {
        return scriptingParams;
    }

    public void configurationChanged(ConfigurationChangeEvent evt) {
        String key = evt.getPropertyName();
        if (key.equals("scripting.defaultOutputPath") || key.equals("scripting.defaultTemplatePath")) {
            for (TestScriptInterpret ti : getActiveTestScripts()) {
                if (ti.getType() == TestScriptInterpret.TYPE_PROPRIETARY && !ti.isExecuting()) {
                    ti.setScriptManager(this);
                    ti.compile(null);
                }
            }
        }
    }

    public void removeInterpret(TestScriptInterpret interpret) {
        interprets.remove(interpret);
        if (commandListeners.contains(interpret)) {
            removeCommandListener((CommandListener) interpret);
        }
        if (scriptListeners.contains(interpret)) {
            removeScriptListener((ScriptListener) interpret);
        }
    }

    public void destroy() {
        cliVariables.clear();
        if (client != null) {
            client.removeServerListener(this);
        }
        client = null;
        commandMap.clear();
        context = null;
        interprets.clear();
        rfbServerEvents.clear();
        scriptListeners.clear();
        UserConfiguration.getInstance().removeConfigurationListener(this);
    }

    private class TimeObj implements DynamicVariable {

        public String toString(String name, Map cliVars, Map variables, TestWrapper wr, ScriptingContext ctx) {
            return "" + System.currentTimeMillis();
        }

        public void contextCreated(ScriptingContext ctx) {
            ctx.setVariable(IMPLICIT_VARIABLE_CURTIME, toString(IMPLICIT_VARIABLE_CURTIME, null, null, null, ctx));
        }

        public String getCode() {
            return IMPLICIT_VARIABLE_CURTIME;
        }
    }

    private class MouseCoordObj implements DynamicVariable {

        boolean isX;

        public MouseCoordObj(boolean isX) {
            this.isX = isX;
        }

        public String toString(String name, Map cliVars, Map variables, TestWrapper wr, ScriptingContext ctx) {
            if (ctx != null && ctx.getClient() != null && ctx.getClient().isConnected()) {
                MouseEvent e = ctx.getClient().getLastMouseEvent();
                if (e != null) {
                    return isX ? Integer.toString(e.getX()) : Integer.toString(e.getY());
                }
            }
            return "0";
        }

        public void contextCreated(ScriptingContext ctx) {
            ctx.setVariable(getCode(), toString(getCode(), null, null, null, ctx));
        }

        public String getCode() {
            return isX ? IMPLICIT_VARIABLE_MOUSE_X : IMPLICIT_VARIABLE_MOUSE_Y;
        }
    }

    private class DateObj implements DynamicVariable {

        public String toString(String name, Map cliVars, Map variables, TestWrapper wr, ScriptingContext ctx) {
            DateFormat df = null;
            String s = null;
            if (ctx != null) {
                Object o = ctx.getVariable(IMPLICIT_VARIABLE_CURDATE_FORMAT);
                s = o == null ? null : o.toString();
            }
            if (s == null || s.trim().isEmpty()) {
                s = UserConfiguration.getInstance().getString("scripting.curdateFormat");
            }
            if (s == null || s.trim().isEmpty()) {
                return new Date().toString();
            } else {
                df = new SimpleDateFormat(s);
            }
            return df.format(new Date());
        }

        public void contextCreated(ScriptingContext ctx) {
            ctx.setVariable(IMPLICIT_VARIABLE_CURDATE, toString(getCode(), null, null, null, ctx));
            String s = UserConfiguration.getInstance().getString("scripting.curdateFormat");
            s = s == null ? "" : s;
            ctx.setVariable(IMPLICIT_VARIABLE_CURDATE_FORMAT, s);
        }

        public String getCode() {
            return IMPLICIT_VARIABLE_CURDATE;
        }
    }

    private class RandomVariableObject implements DynamicVariable {

        public final static int RANDOM_MIN = 0;
        public final static int RANDOM_MAX = 100000;
        TokenParserImpl parser = new TokenParserImpl();
        Random random = new Random();

        public String toString(String name, Map cliVars, Map variables, TestWrapper wr, ScriptingContext ctx) {
            int min = getVar(IMPLICIT_VARIABLE_RANDOM_MIN, cliVars, variables, RANDOM_MIN);
            int max = getVar(IMPLICIT_VARIABLE_RANDOM_MAX, cliVars, variables, RANDOM_MAX);
            return getRandom(min, max);
        }

        private String getRandom(int min, int max) {
            int span = max - min;
            int rand = (int) Math.round((double) (Math.random() * (double) span)) + min;
            return "" + rand;
        }

        private int getVar(String name, Map cliVars, Map variables, int defaultValue) {
            try {
                if (cliVars.containsKey(name)) {
                    return parser.parseNumber(cliVars.get(name), "").intValue();
                }
                if (variables.containsKey(name)) {
                    return parser.parseNumber(variables.get(name), "").intValue();
                }
            } catch (Exception ex) {
            }
            return defaultValue;
        }

        @Override
        public void contextCreated(ScriptingContext ctx) {
            ctx.setVariable(IMPLICIT_VARIABLE_RANDOM_MIN, RANDOM_MIN);
            ctx.setVariable(IMPLICIT_VARIABLE_RANDOM_MAX, RANDOM_MAX);
            ctx.setVariable(IMPLICIT_VARIABLE_RANDOM, getRandom(RANDOM_MIN, RANDOM_MAX));
        }

        public String getCode() {
            return IMPLICIT_VARIABLE_RANDOM;
        }
    }

    private class RGBColorObject implements DynamicVariable {

        public final static int RGB_X = 0;
        public final static int RGB_Y = 0;
        TokenParserImpl parser = new TokenParserImpl();

        public String toString(String name, Map cliVars, Map variables, TestWrapper wr, ScriptingContext ctx) {
            int x = getVar(IMPLICIT_VARIABLE_RGB_X, cliVars, variables, RGB_X);
            int y = getVar(IMPLICIT_VARIABLE_RGB_Y, cliVars, variables, RGB_Y);
            return getRGB(x, y, ctx);
        }

        private String getRGB(int x, int y, ScriptingContext ctx) {
            if (ctx != null && ctx.getClient() != null && ctx.getClient().isConnected() && ctx.getClient().getImage() instanceof BufferedImage) {
                BufferedImage img = (BufferedImage) ctx.getClient().getImage();
                if (x < img.getWidth() && y < img.getHeight()) {
                    return parser.colorToString(new Color(img.getRGB(x, y)));
                }
            }
            return "";
        }

        private int getVar(String name, Map cliVars, Map variables, int defaultValue) {
            try {
                if (cliVars.containsKey(name)) {
                    return parser.parseNumber(cliVars.get(name), "").intValue();
                }
                if (variables.containsKey(name)) {
                    return parser.parseNumber(variables.get(name), "").intValue();
                }
            } catch (Exception ex) {
            }
            return defaultValue;
        }

        @Override
        public void contextCreated(ScriptingContext ctx) {
            ctx.setVariable(IMPLICIT_VARIABLE_RGB_X, RGB_X);
            ctx.setVariable(IMPLICIT_VARIABLE_RGB_Y, RGB_Y);
            ctx.setVariable(IMPLICIT_VARIABLE_RGB, getRGB(RGB_X, RGB_Y, ctx));
        }

        public String getCode() {
            return IMPLICIT_VARIABLE_RGB;
        }
    }
}


