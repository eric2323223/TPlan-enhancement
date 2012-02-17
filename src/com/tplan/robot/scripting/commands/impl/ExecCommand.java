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
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.scripting.ScriptManagerImpl;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import com.tplan.robot.scripting.wrappers.TextBlockWrapper;
import com.tplan.robot.util.InputStreamDrain;
import com.tplan.robot.util.Utils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Handler implementing functionality of the {@doc.cmd Exec} command.
 * @product.signature
 */
public class ExecCommand extends AbstractCommandHandler {

    public static final String PARAM_COMMAND = "command";
    public static final String PARAM_OUTPUT_STREAM = "outfile";
    final String VAR_EXEC_VALUE = "_EXEC_VALUE";
    final String VAR_EXEC_COMMAND = "_EXEC_COMMAND";
    final String VAR_EXEC_OUTPUT = "_EXEC_OUTPUT";
    final String VAR_EXEC_ERROR = "_EXEC_ERROR";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }
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
            contextAttributes.put(PARAM_ONPASS, res.getString("command.param.onpass"));
            contextAttributes.put(PARAM_ONFAIL, res.getString("command.param.onpass"));
            contextAttributes.put(PARAM_WAIT, res.getString("command.param.wait"));
            contextAttributes.put(PARAM_COUNT, res.getString("command.param.count"));
            contextAttributes.put(PARAM_OUTPUT_STREAM, res.getString("exec.param.outfile"));
        }
        return contextAttributes;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as this command has a mandatory argument,
     * which is a numeric exit code.
     *
     * @return description of the mandatory argument.
     */
    public String getContextArgument() {
        return ApplicationSupport.getResourceBundle().getString("exec.argument");
    }

    public String[] getCommandNames() {
        return new String[]{"exec"};
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext repository) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        String parName;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        if (args.size() < 1) {
            throw new SyntaxErrorException(res.getString("exec.syntaxErr.syntaxError"));
        }
        String cmd = (String) args.get(0);
        if (cmd.trim().length() == 0) {
            throw new SyntaxErrorException(res.getString("exec.syntaxErr.syntaxError"));
        }
        vt.put(PARAM_COMMAND, cmd);

        // Now proceed to other arguments
        Object value;
        TokenParser parser = repository.getParser();

        for (int j = 1; j < args.size(); j++) {
            parName = args.get(j).toString().toLowerCase();
            value = values.get(parName);
            value = value == null ? "" : value;

            if (parName.equals(PARAM_ONFAIL)) {
                vt.put(PARAM_ONFAIL, value);
            } else if (parName.equals(PARAM_ONPASS)) {
                vt.put(PARAM_ONPASS, value);
            } else if (parName.equals(PARAM_OUTPUT_STREAM)) {
                vt.put(PARAM_OUTPUT_STREAM, value);
            } else if (parName.equals(PARAM_COUNT)) {
                vt.put(PARAM_COUNT, parser.parseNumber(value, PARAM_COUNT));
            } else if (parName.equals(PARAM_WAIT)) {
                vt.put(PARAM_WAIT, parser.parseTime(value, PARAM_WAIT));
            } else {
                String s = res.getString("command.syntaxErr.unknownParam");
                throw new SyntaxErrorException(MessageFormat.format(s, parName));
            }
        }

        validateOnPassAndOnFail(repository, vt);

        Map variables = repository.getVariables();
        variables.put(VAR_EXEC_OUTPUT, "");
    }

    public int execute(List args, Map values, ScriptingContext context) throws SyntaxErrorException {

        Map t = new HashMap();
        int returnValue = 0;

        // Validate
        validate(args, values, t, context);

        String cmd = (String) t.get(PARAM_COMMAND);
        Map variables = context.getVariables();
        ScriptManager handler = context.getScriptManager();
        TestScriptInterpret interpret = context.getInterpret();

        try {
//            String osName = System.getProperty("os.name");
//            if (osName.startsWith("Windows")) {
//
//                // Windows 95/98/ME still use the old command.com interpreter
//                // - See http://lopica.sourceforge.net/os.html for a list of Java os.name property values
//                // - See http://en.wikipedia.org/wiki/COMMAND.COM for more info on command.com and cmd.exe
//                if (osName.equals("Windows 95") || osName.equals("Windows 98") || osName.equals("Windows Me")) {
//                    cmd = "command.com /C " + cmd;
//                } else {
//                    // Newer Windows versions like NT/XP/2000/2003 use cmd.exe
//                    cmd = "cmd.exe /C " + cmd;
//                }
//            }
            variables.put(VAR_EXEC_COMMAND, cmd);

            Runtime rt = Runtime.getRuntime();
//            System.out.println("Execing " + cmd);

            int count = 1;
            if (t.containsKey(PARAM_COUNT)) {
                count = ((Integer) t.get(PARAM_COUNT)).intValue();
            }

            PrintWriter pw = null;
            if (t.containsKey(PARAM_OUTPUT_STREAM)) {
                Object stream = t.get(PARAM_OUTPUT_STREAM);
                if (stream instanceof OutputStream) {
                    pw = new PrintWriter((OutputStream) stream);
                } else if (stream instanceof Writer) {
                    pw = new PrintWriter((Writer) stream);
                } else {
                    try {
                        pw = new PrintWriter(new FileOutputStream(stream.toString()));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            int overallExitVal = 0;
            int exitVal = 0;

            for (int i = 0; i < count; i++) {
                Process proc = null;
                exitVal = 0;
                try {
                    proc = rt.exec(cmd);
                } catch (IOException ex) {
                    exitVal = 2;
                    variables.put(VAR_EXEC_ERROR, ex.getMessage());
                }

                if (proc != null) {
                    InputStreamDrain errDrain = new InputStreamDrain(proc.getErrorStream());
                    InputStreamDrain stdDrain = new InputStreamDrain(proc.getInputStream());

                    errDrain.start();
                    stdDrain.start();

                    exitVal = proc.waitFor();

                    // Save variable values to the context
                    while (errDrain.isAlive() || stdDrain.isAlive()) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                        }
                    }

                    String errOut = errDrain.getOutput();
                    if (errOut != null) {
                        variables.put(VAR_EXEC_ERROR, errOut);
                    }

                    String stdOut = stdDrain.getOutput();
                    if (stdOut != null) {
                        variables.put(VAR_EXEC_OUTPUT, stdOut);
                        if (pw != null) {
                            pw.println(stdOut);
                        }
                    }

                }
                overallExitVal |= exitVal;
            }

            if (pw != null) {
                pw.flush();
                pw.close();
            }

            variables.put(VAR_EXEC_VALUE, Utils.numberFormat.format(exitVal));
            returnValue = overallExitVal;

            // Execute eventual on pass/on fail actions
            if (overallExitVal != 0) { // Unsuccessful run
                if (t.containsKey(PARAM_ONFAIL)) {
                    String failCmd = (String) t.get(PARAM_ONFAIL);
                    if (failCmd != null && !"".equals(failCmd.trim())) {
                        if (context.getInterpret() instanceof ProprietaryTestScriptInterpret) {
                            ((ProprietaryTestScriptInterpret) context.getInterpret()).runBlock(
                                    new TextBlockWrapper(failCmd, true), context);
                        }
                    }
                }
            } else if (t.containsKey(PARAM_ONPASS)) {
                String failCmd = (String) t.get(PARAM_ONPASS);
                if (failCmd != null && !"".equals(failCmd.trim())) {
                    if (context.getInterpret() instanceof ProprietaryTestScriptInterpret) {
                        ((ProprietaryTestScriptInterpret) context.getInterpret()).runBlock(
                                new TextBlockWrapper(failCmd, true), context);
                    }
                }
            }

        } catch (Throwable th) {
            th.printStackTrace();
            returnValue = 2;
        }

        // If the 'delay' parameter has ben specified, wait for the specified amount of time
        if (t.containsKey(PARAM_WAIT) && (t == null || !interpret.isStop())) {
            int delay = ((Number) t.get(PARAM_WAIT)).intValue();
            wait(context, delay);
        }
        return returnValue;
    }

    public List getStablePopupMenuItems() {
        return null;
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns true as no desktop connection is needed for the exec command.
     */
    public boolean canRunWithoutConnection() {
        return true;
    }
}
