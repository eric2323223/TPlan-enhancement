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
import com.tplan.robot.scripting.SyntaxErrorException;

import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.commands.CommandEvent;
import com.tplan.robot.scripting.commands.OutputObject;
import javax.swing.*;
import java.util.Date;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler implementing functionality of the {@doc.cmd Warning} command.
 * @product.signature
 */
public class WarningCommand extends AbstractCommandHandler {

    final static String PARAM_DESC = "desc";
    public final static String PARAM_IMAGE = "image";
    private static Map contextAttributes;
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as this command has a mandatory argument,
     * which is a numeric exit code.
     *
     * @return description of the mandatory argument.
     */
    public String getContextArgument() {
        return ApplicationSupport.getString("warning.argument");
    }

    /**
     * Get a map with context attributes.
     *
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes() {
        if (contextAttributes == null) {
            contextAttributes = new HashMap();
            contextAttributes.put(PARAM_IMAGE, ApplicationSupport.getString("warning.param.image"));
        }
        return contextAttributes;
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext repository) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        String parName;
        Object value;

        if (args.size() < 1) {
            throw new SyntaxErrorException(ApplicationSupport.getString("warning.syntaxErr.generic"));
        }

        vt.put(PARAM_DESC, args.get(0));

        for (int i = 1; i < args.size(); i++) {
            parName = args.get(i).toString();
            value = values.get(parName);

            if (parName.equals(PARAM_IMAGE)) {
                vt.put(PARAM_IMAGE, value);
            } else {
                String s = ApplicationSupport.getString("command.syntaxErr.unknownParam");
                throw new SyntaxErrorException(MessageFormat.format(s, parName));
            }
        }
    }

    public String[] getCommandNames() {
        return new String[]{"warning"};  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int execute(List args, Map values, ScriptingContext repository) throws SyntaxErrorException {

        Map t = new HashMap();

        // Validate
        validate(args, values, t, repository);

        handleWarning(repository, t);
        return 0;
    }

    private void handleWarning(ScriptingContext repository, Map params) {
        try {
            List<OutputObject> v = repository.getOutputObjects();

            WarningInfo wi = new WarningInfo();
            wi.description = (String) params.get(PARAM_DESC);
            Object o = params.get(PARAM_IMAGE);
            if (o != null) {
                if (o instanceof File) {
                    File f = (File) o;
                    wi.associatedImage = f.isAbsolute() ? f.getAbsolutePath() : f.toString();
                } else {
                    wi.associatedImage = o.toString();
                }
            }
            wi.associatedImage = (String) params.get(PARAM_IMAGE);
            wi.date = new Date();

            v.add(wi);

            Map variables = repository.getVariables();
            int warningCount = 1;
            try {
                warningCount = Integer.parseInt(variables.get(ScriptingContext.IMPLICIT_VARIABLE_WARNING_COUNT).toString()) + 1;
            } catch (Exception ex) {
            }
            variables.put(ScriptingContext.IMPLICIT_VARIABLE_WARNING_COUNT, new Integer(warningCount));

            fireCommandEvent(this, repository, CommandEvent.OUTPUT_CHANGED_EVENT, params);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List getStablePopupMenuItems() {
        return null;
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns true as no VNC connection is needed for this command.
     */
    public boolean canRunWithoutConnection() {
        return true;
    }

    public class WarningInfo implements OutputObject {

        public String description;
        public String associatedImage;
        public Date date;

        public int getType() {
            return TYPE_WARNING;
        }

        public String getDescription() {
            return description;
        }

        public Date getDate() {
            return date;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap();
            put(m, "desc", description);
            put(m, "image", associatedImage);
            put(m, "date", date);
            put(m, "type", getType());
            put(m, "typedesc", "warning");
            return m;
        }

        private void put(Map<String, Object> m, String s, Object o) {
            if (o != null) {
                m.put(s, o);
            }
        }

        public String getCode() {
            return "log";
        }
    }
}
