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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.capabilities.PointerTransferCapable;
import com.tplan.robot.scripting.commands.CommandHandler;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.interpret.java.JavaTestScriptInterpret;
import com.tplan.robot.util.DocumentUtils;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

/**
 * Base class for the default Java test script. It defines infrastructure
 * needed to reach the scripting language command handlers.
 *
 * @product.signature
 */
public abstract class AbstractJavaTestScript implements TestWrapper, JavaTestScript {

    private ScriptManager scriptManager;
    private ScriptingContext context;
    private TestScriptInterpret interpret;
    private boolean debug = false;
    private int lineNumber = -1;
    private String testSource = null;
    private StyledDocument document;

    /**
     * Resume execution (set the pause indicator to false).
     */
    public void resume() {
        if (interpret != null) {
            interpret.setPause(this, true, null);
        }
    }

    /**
     * Find out whether execution of the script is paused.
     * @return true if paused, false if not.
     */
    public boolean isPaused() {
        return interpret != null && interpret.isPause();
    }

    /**
     * Allows to send low level mouse event (a MouseEvent instance) to the
     * underlying remote desktop client. It should be only used to implement a
     * script behavior where the standard mouse methods supported by the
     * scripting API do not suffice. When there is no client, the method does
     * nothing.
     *
     * @param event a mouse event.
     * @throws java.io.IOException on an I/O error or when the client is not connected.
     */
    public void mouseEvent(MouseEvent event) throws IOException {
        RemoteDesktopClient client = getContext().getClient();
        if (client != null) {
            if (client instanceof PointerTransferCapable && ((PointerTransferCapable) client).isPointerTransferSupported()) {
                ((PointerTransferCapable) client).sendPointerEvent(event, false);
            } else {
                throw new UnsupportedOperationException(MessageFormat.format(ApplicationSupport.getString("mouse.syntaxErr.pointerNotSupportedByClient"), client.getDisplayName()));
            }
        }
    }


    /**
     * Generic method to execute a scripting language command.
     * @param commandName command name. If the command is not known (there's no
     * command handler registered for the name in the map maintained by the script
     * managers) the method throws an IllegalArgumentException.
     *
     * @param params command parameters in the correct order (if applicable).
     * @param values map of parameters and their values.
     * @return exit value returned by the executed command.
     * @throws java.lang.IllegalArgumentException on unknown command name.
     * @throws java.io.IOException when the exception is thrown by the command
     * handler. This in general applies just to commands which participate
     * in the client-server communication. Other command declare the exception
     * but never throw it.
     * @throws com.tplan.robot.scripting.StopRequestException when a request to
     * stop execution of the script has been registered.
     */
    public final int runScriptCommand(String commandName, List params, Map values) throws IllegalArgumentException, IOException, StopRequestException {
        if (isDebug()) {
            System.out.println(convertToCommand(commandName, params, values));
        }

        if (interpret.isStop()) {
            // To interrupt the current run() method we take advantage of
            // a custom RuntimeException
            throw new StopRequestException();
        }

        Map cmds = scriptManager.getCommandHandlers();
        CommandHandler ch = (CommandHandler) cmds.get(commandName.toUpperCase());
        if (ch != null) {
            if (!ch.canRunWithoutConnection() && (getContext().getClient() == null || !getContext().getClient().isConnected())) {
                throw new IOException(MessageFormat.format(ApplicationSupport.getString("scriptHandler.syntaxError.commandRequiresConnection"), commandName.toUpperCase()));
            }
            if (document != null) {
                lineNumber = getLineNumber(commandName) - 1;
                Element e = DocumentUtils.getElementForLine(document, lineNumber, document.getDefaultRootElement());

                // Enhancement in 2.0.3/2.1 - allow custom remapping of document elements.
                // It allows to run content of one document and map the code
                // onto another document, usually the one displayed in the script editor.
                // This mapping makes the Editor handle execution tracking and break points correctly.
                if (interpret instanceof JavaTestScriptInterpret) {
                    JavaTestScriptInterpret jti = (JavaTestScriptInterpret) interpret;
                    e = jti.getElementMapper().map(jti, e);
                }
                if (isDebug()) {
                    System.out.println("Cmd: " + commandName + ", element '" + DocumentUtils.getElementText(e) + "', line #" + lineNumber);
                }
                getContext().put(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT, e);
                scriptManager.fireScriptEvent(new ScriptEvent(scriptManager, null, getContext(), ScriptEvent.SCRIPT_EXECUTED_LINE_CHANGED));

                PauseRequestException ex = scriptManager.fireScriptEvent(new ScriptEvent(scriptManager, null, getContext(), ScriptEvent.SCRIPT_GOING_TO_RUN_LINE));
                if (ex != null) {
                    interpret.setPause(null, true, ex.getMessage());
                }

                // If the pause flag is set, enter the pause loop.
                // It usually indicates that a breakpoint is set on the command in editor.
                while (interpret.isPause()) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ex1) {
                    }
                }
            }
            int value = 100;
            try {
                value = ch.execute(params, values, getContext());
            } catch (SyntaxErrorException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }

            // Bug 2893211: Java commands fail to populate the exit code variable
            // Set the _EXIT_CODE variable in the context
            getContext().setVariable(ScriptingContext.IMPLICIT_VARIABLE_EXIT_CODE, value);

            // If the pause flag is set, enter the pause loop.
            // It happens when the test script calls the pause() method.
            while (interpret.isPause()) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex1) {
                }
            }
            return value;
        } else {
            throw new IllegalArgumentException(MessageFormat.format(ApplicationSupport.getString("scriptHandler.syntaxError.unsupportedCommand"), commandName));
        }

    }

    private String convertToCommand(String commandName, List params, Map values) {
        String cmd = commandName;
        Object v;

        if (params != null) {
            for (Object p : params) {
                v = values.get(p);
                if (v != null) {  // Parameter in form of param=<value>
                    if (v instanceof Point) {
                        v = getContext().getParser().pointToString((Point) v);
                    }

                    cmd += " " + p + "=\"" + v + "\"";
                } else {
                    cmd += " \"" + p + "\"";  // Argument, must be enclosed in double quotes
                }

            }
        }
        return cmd;
    }

    protected int getLineNumber(String methodName) {
        StringWriter wr = new StringWriter();
        new Exception().printStackTrace(new PrintWriter(wr));
        BufferedReader bs = new BufferedReader(new StringReader(wr.toString()));
        String line = "", number;
        String testClassName = null;
        if (interpret instanceof JavaTestScriptInterpret) {
            testClassName = ((JavaTestScriptInterpret) interpret).getTestInstance().getClass().getSimpleName();
        }
        int i1, i2;
        while (line != null) {
            try {
                line = bs.readLine();
            } catch (IOException ex) {
                break;
            }

            // The line we are looking for is like:
            // at <JavaTestScript_subclass>.<methodName>(<JavaTestScript_subclass>.java:<lineNumber>)
            if (line != null) {
                i1 = line.indexOf('(' + testClassName + ".java");
                if (i1 >= 0) {
                    i2 = line.lastIndexOf(':');
                    if (i2 >= 0) {
                        number = line.substring(i2 + 1);
                        if (number.endsWith(")")) {
                            number = number.substring(0, number.length() - 1);
                        }
                        return Integer.parseInt(number);
                    }
                }
            }
        }
        return lineNumber;
    }

// ===================== Get and Set Methods ===============================
    /**
     * Get context associated with this test script. The context instance is
     * created and set by the test framework before every execution and/or
     * compilation.
     *
     * @return a context instance or null if the script has been neither compiled
     * nor executed yet.
     */
    public ScriptingContext getContext() {
        if (context == null) {
            if (interpret != null) {
                return interpret.getCompilationContext();
            }
        }
        return context;
    }

    /**
     * Set the context. This method is employed by the test framework and should
     * not be used programatically.
     * @param context a context instance.
     */
    public void setContext(ScriptingContext context) {
        this.context = context;
        setInterpret(context.getInterpret());
    }

    /**
     * Indicate whether the debug mode is on.
     * @return true if the debug mode is on, false otherwise.
     */
    public boolean isDebug() {
        return debug || System.getProperty("javatest.debug") != null;
    }

    /**
     * Set the debug mode flag. If it is on (debug=true), the script prints out
     * debug messages to the console. As the script prints out scripting
     * language commands resulting from the Java API method calls, it may be used
     * as a very approximate conversion of Java test script to a proprietary
     * language one.
     *
     * @param debug the debug to set
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getTestSource() {
        if (testSource != null) {
            return testSource;
        }
        return getClass().getCanonicalName().replace('.', File.separatorChar) + ".java";
    }

    public int getWrapperType() {
        return WRAPPER_TYPE_JAVA;
    }

    /**
     * Get number of the currently executed command line.
     * @param ctx context instance.
     * @return executed command line number.
     */
    public int getLineNumber(ScriptingContext ctx) {
        return lineNumber;
    }

    /**
     * Get the parent wrapper. As this test script is always handled as a
     * top level test wrapper, it has no parent and the method always returns null.
     * @return parent test wrapper (always null in this implementation).
     */
    public TestWrapper getParentWrapper() {
        return null;   // This wrapper is top level and has no parent
    }

    // TODO: reimplement
    /**
     * Get the script file.
     * @return script file.
     */
    public File getScriptFile() {
        String s = getClass().getCanonicalName().replace('.', File.separatorChar) + ".class";
        return new File(s);
    }

    /**
     * @param testSource the testSource to set
     */
    public void setTestSource(String testSource) {
        this.testSource = testSource;
    }

    /**
     * Get the document associated with this wrapper.
     * @return a StyledDocument instance. Never returns null.
     */
    public StyledDocument getDocument() {
        return document;
    }

    /**
     * Set the document associated with this wrapper.
     * @param document a new document for this wrapper.
     */
    public void setDocument(StyledDocument document) {
        this.document = document;
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName();
    }

    /**
     * Get script manager associated with this test script and its interpret.
     * @return the scriptManager
     */
    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    /**
     * Set the test script interpret.
     * @param interpret interpret instance able to handle Java source code.
     */
    public void setInterpret(TestScriptInterpret interpret) {
        this.interpret = interpret;
        this.scriptManager = interpret.getScriptManager();
    }

    /**
     * Convenience method to get a context variable value. It is equivalent to a call
     * like <code>getContext().getVariable()</code>.
     * @param name variable name.
     * @return variable value or null if the variable doesn't exist in the context.
     */
    public Object getVariable(String name) {
        return context != null ? context.getVariable(name) : null;
    }

    /**
     * Convenience method to get a context variable value and convert it to String.
     *
     * @param name variable name.
     * @return variable value as a String or null if the variable doesn't exist in the context.
     */
    public String getVariableAsString(String name) {
        Object v = getVariable(name);
        if (v != null) {
            if (v instanceof String) {
                return (String)v;
            }
            return v.toString();
        }
        return null;
    }

    /**
     * Convenience method to get a context variable value and convert it to Float number.
     * It is roughly equivalent to a call like <code>Float.parseFloat(getContext().getVariable().toString))</code>.
     * As the method doesn't catch any exceptions, it may throw a NumberFormatException
     * if the variable is not a number.
     *
     * @param name variable name.
     * @return variable value as a Float or null if the variable doesn't exist in the context.
     */
    public Float getVariableAsFloat(String name) {
        Object v = getVariable(name);
        if (v != null) {
            if (v instanceof Number) {
                return ((Number) v).floatValue();
            }
            return Float.parseFloat(v.toString());
        }
        return null;
    }

    /**
     * Convenience method to get a context variable value and convert it to Integer number.
     * As the method doesn't catch any exceptions, it may throw a NumberFormatException
     * if the variable is not a number.
     *
     * @param name variable name.
     * @return variable value as an Integer or null if the variable doesn't exist in the context.
     */
    public Integer getVariableAsInt(String name) {
        Float f = getVariableAsFloat(name);
        return f == null ? null : f.intValue();
    }
}
