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
package com.tplan.robot.scripting.commands;

import com.tplan.robot.scripting.*;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.Configurable;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.GUIConstants;
import com.tplan.robot.plugin.DependencyMissingException;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.interpret.java.JavaTestScriptInterpret;
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import com.tplan.robot.scripting.wrappers.TextBlockWrapper;
import com.tplan.robot.util.Utils;
import java.lang.reflect.InvocationTargetException;

import javax.swing.*;
import javax.swing.text.Element;
import java.awt.event.*;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.List;

/**
 * Base class for command handlers which implement functionality of individual
 * commands of the {@product.name} proprietary scripting language.
 *
 * @product.signature
 */
public abstract class AbstractCommandHandler implements CommandHandler, GUIConstants, Plugin, Configurable {

    private List<CommandListener> commandListeners = new ArrayList();
    public static final String PARAM_WAIT = "wait";
    public static final String PARAM_COUNT = "count";
    public static final String PARAM_ONFAIL = "onfail";
    public static final String PARAM_ONPASS = "onpass";
    protected static final String SHIFT = "SHIFT";
    protected static final String CTRL = "CTRL";
    protected static final String ALT = "ALT";
    protected static final String WINDOWS = "WINDOWS";

    public void validateOnPassAndOnFail(ScriptingContext repository, Map params) throws SyntaxErrorException {
        boolean exit = true;
        if (exit) {
            return;
        }

        ScriptManagerImpl sh = (ScriptManagerImpl) repository.getScriptManager();
        Element e = (Element) repository.get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT);
        DocumentWrapper wrapper = (DocumentWrapper) repository.get(ScriptingContext.CONTEXT_CURRENT_SCRIPT_WRAPPER);

        String command;
        if (repository.getInterpret() instanceof ProprietaryTestScriptInterpret) {
            ProprietaryTestScriptInterpret pti = (ProprietaryTestScriptInterpret) repository.getInterpret();
            if (params.containsKey(PARAM_ONPASS)) {
                command = params.get(PARAM_ONPASS).toString();
                pti.runBlock(new TextBlockWrapper(wrapper, e, command, true), repository);
            }
            if (params.containsKey(PARAM_ONFAIL)) {
                command = params.get(PARAM_ONFAIL).toString();
                pti.runBlock(new TextBlockWrapper(wrapper, e, command, true), repository);
            }
        }
    }

    public void executeFallBackCodeOrProcedure(String code, String procedure, String[] procedureArgs, ScriptingContext ctx) {

        // Continue only if the interpret is a proprietary one
        if (ctx.getInterpret() instanceof ProprietaryTestScriptInterpret) {

            // If the code is null, no ONFAIL or ONTIMEOUT parameter has been specified.
            // In such a case look for the known name of the default comparison failure procedure.
            if (code == null) {
                if (procedure == null || procedure.isEmpty()) {
                    return;
                }
                procedure = procedure.toUpperCase();

                Map procedureMap = (Map) ctx.get(ScriptingContext.CONTEXT_PROCEDURE_MAP);
                if (procedureMap.containsKey(procedure)) {
                    code = procedure;
                    if (procedureArgs != null) {
                        for (String arg : procedureArgs) {
                            code += " \""+arg+"\"";
                        }
                    }
                } else {
                    return;
                }
            } else if (code.trim().isEmpty()) {
                return;
            }

            ((ProprietaryTestScriptInterpret) ctx.getInterpret()).runBlock(new TextBlockWrapper(code, true), ctx);
        } else if (ctx.getInterpret() instanceof JavaTestScriptInterpret) {
            JavaTestScript ti = ((JavaTestScriptInterpret) ctx.getInterpret()).getTestInstance();
            if (ti != null && Utils.implementsInterface(ti.getClass(), "com.tplan.robot.scripting.ComparisonFailureListener")) {
                String err = "Failed to execute image comparison fall back method:";
                try {
                    String[] args = procedureArgs != null ? procedureArgs : new String[0];
                    Method m = ti.getClass().getMethod("comparisonFailureFallback", args.getClass());
                    m.invoke(ti, new Object[]{args});
                } catch (IllegalAccessException ex) {
                    System.out.println(err);
                    ex.printStackTrace();
                } catch (IllegalArgumentException ex) {
                    System.out.println(err);
                    ex.printStackTrace();
                } catch (InvocationTargetException ex) {
                    System.out.println(err);
                    ex.printStackTrace();
                } catch (NoSuchMethodException ex) {
                    System.out.println(err);
                    ex.printStackTrace();
                } catch (SecurityException ex) {
                    System.out.println(err);
                    ex.printStackTrace();
                }
                // Any other exception will be thrown
            }
        }
    }

    public boolean isGlobalPrerequisity(String command) {
        return false;
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

    public void fireCommandEvent(Object source, ScriptingContext context, String actionCode, Object customObject) {
        CommandEvent e = new CommandEvent(source, context, actionCode, customObject);
        for (CommandListener l : Collections.unmodifiableList(commandListeners)) {
            try {
                l.commandEvent(e);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public List<Preference> getPreferences() {
        return null;
    }

    public void setConfiguration(UserConfiguration cfg) {
    }

    protected void wait(ScriptingContext context, int delay) {
        long endTime = System.currentTimeMillis() + delay;
        TimerAction a = new TimerAction(endTime, context);
        Timer updateTimer = new Timer(1000, a);
        updateTimer.setRepeats(true);
        updateTimer.start();

        BreakAction action = new BreakAction(a);
        ScriptManager handler = context.getScriptManager();
        TestScriptInterpret interpret = context.getInterpret();
        fireCommandEvent(this, context, EVENT_ADD_CUSTOM_ACTION_MSG, action);

        // Stay in the waiting loop until the script gets broken, the condition of the event are met or
        // timeout is reached
        try {
            while (!interpret.isStop() && !a.breakCountDown) {
                if (System.currentTimeMillis() > endTime) {
                    break;
                }
                Thread.sleep(5);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fireCommandEvent(this, context, EVENT_REMOVE_CUSTOM_ACTION_MSG, action);
        updateTimer.stop();
        handler.fireScriptEvent(new ScriptEvent(this, null, context, ""));
    }

    public List getStablePopupMenuItems() {
        List v = new ArrayList();

        // Bug 2866578 - Editor context menu shows unnecessary Configure item
        // Add the Configure action only when the preference list is not empty
        List l = getPreferences();
        if (l != null && l.size() > 0) {
            Action action = new ConfigureAction(this);
            v.add(action);
        }
        return v;
    }

    public String getContextArgument() {
        return null;
    }

    /**
     * Get a map with context attributes.
     *
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes() {
        return null;
    }

    public KeyStroke getContextShortcut() {
        return null;
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns false. Commands which do not require a VNC connection for execution
     *         need to reimplement this method.
     */
    public boolean canRunWithoutConnection() {
        return false;
    }

    public String getCode() {
        return getCommandNames()[0];
    }

    public String getVendorName() {
        return ApplicationSupport.APPLICATION_NAME;
    }

    public String getSupportContact() {
        return ApplicationSupport.APPLICATION_SUPPORT_CONTACT;
    }

    public int[] getVersion() {
        return Utils.getVersion();
    }

    public Class getImplementedInterface() {
        return CommandHandler.class;
    }

    public boolean requiresRestart() {
        return true;
    }

    public String getVendorHomePage() {
        return ApplicationSupport.APPLICATION_HOME_PAGE;
    }

    public java.util.Date getDate() {
        return Utils.getReleaseDate();
    }

    public int[] getLowestSupportedVersion() {
        return Utils.getVersion();
    }

    public String getMessageBeforeInstall() {
        return null;
    }

    public String getMessageAfterInstall() {
        return null;
    }

    /**
     * Get a generic display name.
     * @return generic display name which is derived from the {@link #getCmdDisplayName()} method.
     */
    public String getDisplayName() {
        String s = ApplicationSupport.getString("command.plugin.defaultDisplayName");
        return MessageFormat.format(s, getCmdDisplayName());
    }

    public String getDescription() {
        String s = ApplicationSupport.getString("command.plugin.defaultDesc");
        return MessageFormat.format(s, getCmdDisplayName(), ApplicationSupport.APPLICATION_NAME, ApplicationSupport.APPLICATION_VERSION);
    }

    public String getUniqueId() {
        return "standard_" + getCommandNames()[0].toLowerCase() + "_cmd_handler";
    }

    public void checkDependencies(PluginManager manager) throws DependencyMissingException {
    }
    // Get command name. It may consist of more names so we concatentate them.

    /**
     * Retrieve an integer parameter from user configuration in a safe,
     * exception-free way. If the parameter is missing or invalid, the method
     * prints out an error message and returns the default value.
     * 
     * @param cfg user configuration (may not be null).
     * @param parameter parameter name.
     * @param defaultValue default value to be returned in case of any failure.
     * @return parameter value or the default one if loading fails.
     */
    protected int getIntegerSafely(UserConfiguration cfg, String parameter, int defaultValue) {
        try {
            return cfg.getInteger(parameter);
        } catch (Exception e) {
            Object o = cfg.getString(parameter);
            if (o == null) {
                System.out.println(MessageFormat.format(ApplicationSupport.getString("preference.missing"), parameter, defaultValue));
            } else {
                System.out.println(MessageFormat.format(ApplicationSupport.getString("preference.invalid"), parameter, defaultValue, o));
            }
        }
        return defaultValue;
    }

    /**
     * Retrieve a boolean parameter from user configuration in a safe,
     * exception-free way. If the parameter is missing or invalid, the method
     * prints out an error message and returns the default value.
     *
     * @param cfg user configuration (may not be null).
     * @param parameter parameter name.
     * @param defaultValue default value to be returned in case of any failure.
     * @return parameter value or the default one if loading fails.
     */
    protected boolean getBooleanSafely(UserConfiguration cfg, String parameter, boolean defaultValue) {
        try {
            return cfg.getBoolean(parameter);
        } catch (Exception e) {
            Object o = cfg.getString(parameter);
            if (o == null) {
                System.out.println(MessageFormat.format(ApplicationSupport.getString("preference.missing"), parameter, defaultValue));
            } else {
                System.out.println(MessageFormat.format(ApplicationSupport.getString("preference.invalid"), parameter, defaultValue, o));
            }
        }
        return defaultValue;
    }

    private String getCmdDisplayName() {
        String name = "";
        for (String n : getCommandNames()) {
            n = Character.toUpperCase(n.charAt(0)) + n.substring(1).toLowerCase();
            name += n + "/";
        }
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    protected class ConfigureAction extends AbstractAction {

        CommandHandler h;

        public ConfigureAction(CommandHandler h) {
            this.h = h;
            String label = getCommandNames()[0]; //ApplicationSupport.getString("command.WaitCommand.continueLabel");
            String s = ApplicationSupport.getResourceBundle().getString("commandHandler.configureActionDesc");
            label = MessageFormat.format(s, label.substring(0, 1).toUpperCase() + label.substring(1, label.length()));
            putValue(SHORT_DESCRIPTION, label);
            putValue(NAME, label);
        }

        public void actionPerformed(ActionEvent e) {
            AbstractCommandHandler.this.fireCommandEvent(h, null, EVENT_DISPLAY_PREFERENCES, ((Plugin) h).getDisplayName());
        }
    }

}
