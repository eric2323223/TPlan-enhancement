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

import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.util.ListenerHashMapImpl;
import com.tplan.robot.util.ListenerMap;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scripting context implementation.
 *
 * @product.signature
 */
public class ScriptingContextImpl extends HashMap implements ScriptingContext {

    /**
     * Get the current desktop client employed by the test script.
     * @return desktop client.
     */
    public RemoteDesktopClient getClient() {
        return (RemoteDesktopClient) get(CONTEXT_CLIENT);
    }

    /**
     * Get shared instance of user configuration.
     * @return user configuration.
     */
    public UserConfiguration getConfiguration() {
        return (UserConfiguration) get(CONTEXT_USER_CONFIGURATION);
    }

    /**
     * Get script manager owning the current test script.
     * @return script manager.
     */
    public ScriptManager getScriptManager() {
        return (ScriptManager) get(CONTEXT_SCRIPT_MANAGER);
    }

    /**
     * Get the map with test script variables. It contains both implicit
     * variables.
     * @return map of test script variables. The method never returns null.
     */
    public ListenerMap<String, Object> getVariables() {
        ListenerMap<String, Object> vars = (ListenerMap<String, Object>) get(CONTEXT_VARIABLE_MAP);
        if (vars == null) {
            vars = new ListenerHashMapImpl();
            put(CONTEXT_VARIABLE_MAP, vars);
        }
        return vars;
    }

    public Map getScriptParams() {
        Map vars = (Map) get(CONTEXT_CLI_VARIABLE_MAP);
        if (vars == null) {
            vars = new HashMap();
            put(CONTEXT_CLI_VARIABLE_MAP, vars);
        }
        return vars;
    }

    public TokenParser getParser() {
        return (TokenParser) get(CONTEXT_PARSER);
    }

    public void setVariable(String variableName, Object value) {
        getVariables().put(variableName, value);
    }

    public Object getVariable(String variableName) {
        return getVariables().get(variableName);
    }

    public int getExitCode() {
        Number n = ((Number) getVariables().get(IMPLICIT_VARIABLE_EXIT_CODE));
        return n == null ? 0 : n.intValue();
    }

    public File getScriptFile() {
        return getMasterWrapper().getScriptFile();
    }

    public File getOutputDir() {
        Object s = getVariables().get(IMPLICIT_VARIABLE_REPORT_DIR);
        if (s != null) {
            return s instanceof File ? (File) s : new File(s.toString());
        }
        return null;
    }

    public void setOutputDir(File directory) {
        getVariables().put(IMPLICIT_VARIABLE_REPORT_DIR, directory);
    }

    public File getTemplateDir() {
        Object s = getVariables().get(IMPLICIT_VARIABLE_TEMPLATE_DIR);
        if (s != null) {
            return s instanceof File ? (File) s : new File(s.toString());
        }
        return null;
    }

    public void setTemplateDir(File directory) {
        getVariables().put(IMPLICIT_VARIABLE_TEMPLATE_DIR, directory);
    }

    public String getServerClipboardContent() {
        return (String) getVariables().get(IMPLICIT_VARIABLE_SERVER_CLIPBOARD_CONTENT);
    }

    public List<Point> getSearchHits() {
        return (List<Point>) get(CONTEXT_IMAGE_SEARCH_POINT_LIST);
    }

    public Component getEventSource() {
        return (Component) get(CONTEXT_EVENT_SOURCE);
    }

    public TestWrapper getMasterWrapper() {
        return (TestWrapper) get(CONTEXT_CURRENT_SCRIPT_WRAPPER);
    }

    public Map<String, String> getCommandLineVariables() {
        return (Map) get(CONTEXT_CLI_VARIABLE_MAP);
    }

    public Number getComparisonResult() {
        Object o = getVariables().get(COMPARETO_RESULT);
        if (o != null) {
            try {
                return getParser().parseNumber(o, "");
            } catch (SyntaxErrorException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public boolean isCompilationContext() {
        Object o = get(CONTEXT_COMPILATION_FLAG);
        if (o != null) {
            if (o instanceof Boolean) {
                return (Boolean) o;
            } else {
                return Boolean.parseBoolean(o.toString());
            }
        }
        // Default value = true
        return true;
    }

    public TestScriptInterpret getInterpret() {
        return (TestScriptInterpret)get(CONTEXT_INTERPRET);
    }

    public List<SyntaxErrorException> getCompilationErrors() {
        List<SyntaxErrorException> l = (List<SyntaxErrorException>) get(CONTEXT_COMPILATION_ERRORS);
        if (l == null) {
            l = new ArrayList();
            put(CONTEXT_COMPILATION_ERRORS, l);
        }
        return l;
    }

    public List getOutputObjects() {
        List l = (List) get(CONTEXT_OUTPUT_OBJECTS);
        if (l == null) {
            l = new ArrayList();
            put(CONTEXT_OUTPUT_OBJECTS, l);
        }
        return l;
    }

    public void dispose() {
        getVariables().removePropertyChangeListeners();
        getCompilationErrors().clear();
        getOutputObjects().clear();
        clear();
    }

    public Throwable getConnectError() {
        return (Throwable)get(CONTEXT_CONNECT_THROWABLE);
    }

    public int getSearchHitTemplateIndex() {
        Object o = getVariable(COMPARETO_TEMPLATE_INDEX);
        if (o != null) {
            try {
                return getParser().parseInteger(o, COMPARETO_TEMPLATE_INDEX).intValue();
            } catch (SyntaxErrorException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }
        return -1;
    }

    public Dimension getSearchHitTemplateSize() {
        Object o1 = getVariable(COMPARETO_TEMPLATE_WIDTH);
        Object o2 = getVariable(COMPARETO_TEMPLATE_HEIGHT);
        if (o1 != null && o2 != null) {
            try {
                return new Dimension(
                        getParser().parseInteger(o1, COMPARETO_TEMPLATE_WIDTH).intValue(),
                        getParser().parseInteger(o1, COMPARETO_TEMPLATE_HEIGHT).intValue()
                        );
            } catch (SyntaxErrorException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }
        return null;
    }
}
