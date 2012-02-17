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
import com.tplan.robot.AutomatedRunnable;
import com.tplan.robot.plugin.DependencyMissingException;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.plugin.PluginInfo;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.preferences.Configurable;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.scripting.commands.CommandEvent;
import com.tplan.robot.scripting.commands.CommandListener;
import com.tplan.robot.scripting.commands.impl.ExitCommand;
import com.tplan.robot.scripting.commands.impl.WaitforCommand;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.wrappers.ForWrapper;
import com.tplan.robot.scripting.wrappers.GenericWrapper;
import com.tplan.robot.scripting.wrappers.IfWrapper;
import com.tplan.robot.scripting.wrappers.IncludeWrapper;
import com.tplan.robot.scripting.wrappers.NestedBlockInterpret;
import com.tplan.robot.scripting.wrappers.ProcedureWrapper;
import com.tplan.robot.scripting.wrappers.RunWrapper;
import com.tplan.robot.util.Utils;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.StyledDocument;
import static com.tplan.robot.scripting.ScriptDebuggingEvent.*;
import static com.tplan.robot.scripting.ScriptingContext.*;

/**
 * <p>Default converter from the proprietary scripting language to Java. It is a
 * fairly simple implementation which doesn't have ambitions to convert everything
 * perfectly. The resulting Java code may be a bit ugly and it may contain syntax
 * errors which have to be resolved manually.</p>
 *
 * <p>Known unsupported conversion scenarios are:
 *
 * <ul>
 * <li>Nested variables, such as <code>{VAR_{NUMBER}}</code></li>
 * <li>Exit command with <code>scope=block</code> used to exit an <code>if/else</code> structure (TODO: can it be implemented using another if?)</li>
 * <li>Exit command with <code>scope=file</code></li>
 * <li>Run and Include commands are not supported directly. Users are advised to
 * convert the files manually to Java and then link them back to their Java code.</li>
 * <li><code>_CURTIME</code> variable is not available. Users are advised to get the current time through <code>System.currentTimeMillis()</code>.</li>
 * </ul>
 * </p>
 *
 * @product.signature
 */
public class DefaultJavaTestScriptConverter extends NumericEvaluator
        implements JavaTestScriptConverter, ConversionEvaluator, ScriptListener, CommandListener, Plugin, Configurable {

    // TODO: procedures with parameters in form of expressions
    // TODO: default x,y,w,h in rectanges and points (through context variables)
    // TODO: change all wait params to String
    // TODO: design a full self-test with all commands & command combinations
    private final String template =
            "/**\n"
            + " * Generated on @date\n"
            + " * " + Utils.getProductNameAndVersion() + "\n"
            + " * " + this.getDisplayName() + " version " + PluginInfo.getVersionString(getVersion()) + "\n"
            + " */\n"
            + "@package\n\n"
            + "@imports"
            + "\n"
            + "public class @className extends @superClassName implements " + JavaTestScript.class.getSimpleName() + " {\n"
            + "@vars"
            + "\n"
            + "   public void test() {\n"
            + "@commands\n"
            + "   }\n"
            + "\n"
            + "@methods\n"
            + "}";
    /** Main method template */
    private final String mainMethodTemplate =
            "public static void main(String args[]) {\n"
            + "@instance\n"
            + "   " + ApplicationSupport.class.getSimpleName() + " robot = new " + ApplicationSupport.class.getSimpleName() + "();\n"
            + "   " + AutomatedRunnable.class.getSimpleName() + " t = robot.createAutomatedRunnable(test, \"javatest\", args, System.out, false);\n"
            + "   new Thread(t).start();\n"
            + "}\n";
    // Default Java test script superclass (may be customized)
    private Class superClass = DefaultJavaTestScript.class;
    // Name of the array of arguments of generated Java procedures
    private final String procedureArgumentName = "args";
    // Target code buffer for declaration of Java variables
    private final static String BUF_VARS = "vars";
    // Target code buffer for declaration of commands inside the test() method
    private final static String BUF_COMMANDS = "commands";
    // Target code buffer for declaration of methods created from procedures
    private final static String BUF_METHODS = "methods";
    // Map of all target code buffers
    private Map<String, StringBuffer> code = new HashMap();
    // Map used for indens of individual code buffers
    private Map<String, Number> indentMap = new HashMap();
    // Map of script vars converted to Java native variables
    Map<String, Variable> variableMap = new HashMap();
    // Map of Java imports
    Map<String, String> importMap = new HashMap();
    // Map of exceptions which need to be caught
    Map<Class, String> exceptionMap = new HashMap();
    // Context variable map
    private Map contextVariables = new HashMap();
    private int mapInstanceCounter = 1;
    private int forInstanceCounter = 1;
    boolean inProcedure = false;
    String procedureName = null;
    boolean canWrite = true;
    boolean needsTryAndCatch = false;
    boolean procedureCallInProgress = false;
    boolean procedureReturnAlreadyThere = false;
    boolean runAndIncludeSupported = false;
    private JavaTestScriptMethodMapper methodMapper;
    ScriptingContext context;
    String precedingComment = null;
    TestScriptInterpret interpret;

    public DefaultJavaTestScriptConverter() {
        indentMap.put(BUF_VARS, 1);
        indentMap.put(BUF_COMMANDS, 2);
        indentMap.put(BUF_METHODS, 1);
        setSuperClassName(DefaultJavaTestScript.class);
        String sc = UserConfiguration.getInstance().getString("javaconverter.superClassName");
        if (sc != null && sc.trim().length() > 0) {
            try {
                setSuperClassName(Class.forName(sc));
            } catch (ClassNotFoundException ex) {
                System.out.println("Superclass " + sc + " not found, defaulting to " + this.superClass);
                ex.printStackTrace();
            }
        }
        importMap.put(JavaTestScript.class.getName(), "");
    }

    /**
     * <p>Convert a document containing test script in the proprietary language
     * into Java test script.</p>
     *
     * <p>The method first attaches as a listener to various interfaces provided
     * by ScriptManager. Then it invokes script compilation in the debug mode
     * (the mode is set in fact in the scriptEvent() class) and uses the
     * incoming script events to construct components of Java code and insert
     * them into its internal maps and code buffers.</p>
     *
     * <p>When script compilation/validation finishes, the method detaches from all
     * interfaces and generates the resulting Java class from the maps and code
     * buffers. This part of functionality is provided by the write() method.</p>
     *
     * @param interpret a test script interpret.
     * @param className desired name for the target class.
     * @param packageName package name for the target class.
     * @param doc document containing a test script to be converted to Java.
     * @param wr a Writer instance to write the output to.
     * @throws java.io.IOException if an I/O error happens when writing to the writer.
     */
    public void convert(TestScriptInterpret interpret, String className, String packageName, StyledDocument doc, Writer wr) throws IOException {
        this.interpret = interpret;
        code.clear();
        mapInstanceCounter = 1;
        forInstanceCounter = 1;
        needsTryAndCatch = false;

        ScriptManager sm = interpret.getScriptManager();
        context = sm.createDefaultContext();
        context.put(ScriptingContext.CONTEXT_DEBUG_MODE_FLAG, true);

        methodMapper = getMethodMapper(context);
        runAndIncludeSupported = methodMapper.supports("run") && methodMapper.supports("include");

        sm.addScriptListener(this);
        sm.addCommandListener(this);

        // Compile the script in debugging mode.
        interpret.compile(context);

        sm.removeCommandListener(this);
        sm.removeScriptListener(this);

        write(className, packageName, wr);
    }

    private String convertExit(List args, Map values, ScriptingContext ctx) {
        String scope = (String) values.get(ExitCommand.PARAM_SCOPE);
        if (scope != null) {
            TestWrapper w = (TestWrapper) ctx.get(CONTEXT_CURRENT_SCRIPT_WRAPPER);
            // If the exit has the scope of 'procedure' and we are in a procedure,
            // return from the Java procedure with the given return code.
            if (inProcedure && ExitCommand.PARAM_SCOPE_PROCEDURE.equalsIgnoreCase(scope)) {
                if (w == null || w instanceof ProcedureWrapper) {
                    procedureReturnAlreadyThere = true;
                }
                return "return " + values.get("argument") + ";";
            }

            // If we are in a 'for' loop and the scope is 'block', insert the break Java cmd
            if (w != null && w instanceof ForWrapper && ExitCommand.PARAM_SCOPE_BLOCK.equalsIgnoreCase(scope)) {
                return "break;";
            }

            if (scope.equalsIgnoreCase(ExitCommand.PARAM_SCOPE_PROCESS)) {
                args.remove(args.size() - 1);
                values.remove(ExitCommand.PARAM_SCOPE);
                return getMethodMapper(ctx).convert(args, values, ctx, exceptionMap, importMap);
            }
        }
        return null;  // Null returned here will cause the command to be mapped Java test class exit() method
    }

    /**
     * Write the Java code. Thi method will take the available code templates
     * and replace the pseudovariables with the code generated from the output
     * buffers and maps. There are several code sources which correspond to particular parts
     * of the Java class, for example a map of imports, a buffer of methods, a buffer
     * of the test commands etc.
     *
     * @param className desired name for the target class.
     * @param packageName package name for the target class.
     * @param wr a Writer instance to write the output to.
     * @throws java.io.IOException if an I/O error happens when writing to the writer.
     */
    private void write(String className, String packageName, Writer wr) throws IOException {
        String h = template;

        // If the main method is requested, append it to the buffer of methods
        Boolean addMainMethod = UserConfiguration.getInstance().getBoolean("javaconverter.createMainMethod");
        if (addMainMethod != null && addMainMethod) {
            String s = mainMethodTemplate.replace("@instance", "   " + className + " test = new " + className + "();");
            append(BUF_METHODS, s);
            importMap.put(ApplicationSupport.class.getName(), "");
            importMap.put(AutomatedRunnable.class.getName(), "");
        }

        // If we used at least one call throwing IOException, we have to add try/catch
        if (exceptionMap.size() > 0) {
            StringBuffer cmds = code.remove(BUF_COMMANDS);
            StringBuffer newCmds = new StringBuffer();
            code.put(BUF_COMMANDS, newCmds);
            append(BUF_COMMANDS, "try {\n");
            indentMap.put(BUF_COMMANDS, 1);
            if (cmds != null) {
                append(BUF_COMMANDS, cmds.toString());
            }
            increaseIndent(BUF_COMMANDS);

            // TODO: parse exception classes and fix the order (subclass exceptions must be first)
            for (Class c : exceptionMap.keySet()) {
                append(BUF_COMMANDS, "} catch (" + c.getSimpleName() + " ex) {\n   ex.printStackTrace();\n}\n");
                importMap.put(c.getName(), "");
            }
        }

        // Insert the import statements
        String imports = "";
        if (importMap.size() > 0) {
            String a[] = new String[importMap.size()];
            a = importMap.keySet().toArray(a);
            Arrays.sort(a);
            for (String s : a) {
                imports += "import " + s + ";\n";
            }
        }
        h = h.replace("@imports", imports);

        h = h.replace("@package", packageName != null && packageName.trim().length() > 0 ? "package " + packageName + ";" : "");
        h = h.replace("@date", new Date().toString());
        h = h.replace("@className", className);
        h = h.replace("@superClassName", superClass.getSimpleName());
        h = h.replace("@" + BUF_COMMANDS, getBuf(BUF_COMMANDS));
        h = h.replace("@" + BUF_METHODS, getBuf(BUF_METHODS));
        h = h.replace("@" + BUF_VARS, getBuf(BUF_VARS));
        wr.write(h);
    }

    /**
     * Get test superclass. Default is com.tplan.robot.scripting.DefaultJavaTestScript.
     * @return the superclass.
     */
    public Class getSuperClass() {
        return superClass;
    }

    /**
     * Set the test superclass. Default is <code>com.tplan.robot.scripting.DefaultJavaTestScript</code>.
     * This method is supposed to let users to customize the Java code generator
     * to extend a custom test class.
     *
     * @param superClass new superclass.
     */
    public void setSuperClassName(Class superClass) {
        if (this.superClass != null) {
            importMap.remove(this.superClass.getName());
        }
        this.superClass = superClass;
        importMap.put(this.superClass.getName(), "");
    }

    /**
     * Method of {@link ScriptListener}. To avoid parsing of test scripts when
     * generating the Java code, this class rather uses extended debugging
     * mechanism of {@link ScriptManager} which fires events about language
     * structure during the validation (compilation) process. Such events
     * are processed and mapped onto the corresponding Java code.
     *
     * @param event a script event.
     */
    public void scriptEvent(ScriptEvent event) {
        String line, bufName;
        ScriptManager sm = event.getContext().getScriptManager();
        canWrite = canInsert(event) && !procedureCallInProgress;
        switch (event.getType()) {
            case SCRIPT_COMPILATION_STARTED:
                // When a validation starts, set the debug flag to true
                event.getContext().put(CONTEXT_DEBUG_MODE_FLAG, true);
                contextVariables = new HashMap(event.getContext().getVariables());
                break;
            case SCRIPT_DEBUG_PROCEDURE_WRAPPER_CREATED:
                // A procedure header was reached
                procedureReturnAlreadyThere = false;
                ProcedureWrapper w = (ProcedureWrapper) event.getCustomObject();

                // TODO: check if the procedure name conflicts with any test method
                // TODO: method doesn't necessarily have to throw the IOException
                append(BUF_METHODS, "private int " + w.getProcedureName() + "(String... " + procedureArgumentName + ") throws IOException {\n");
                increaseIndent(BUF_METHODS);
                inProcedure = true;
                procedureName = w.getProcedureName();
                break;
            case SCRIPT_DEBUG_PROCEDURE_DEFINITION_END_REACHED:
                if (!procedureReturnAlreadyThere) {
                    append(BUF_METHODS, "// Return exit code of the last executed command\nreturn getContext().getExitCode();\n");
                }
                decreaseIndent(BUF_METHODS);
                append(BUF_METHODS, "}\n");
                inProcedure = false;
                break;
            case SCRIPT_DEBUG_PROCEDURE_CALL_REACHED:
                bufName = inProcedure ? BUF_METHODS : BUF_COMMANDS;
                w = (ProcedureWrapper) event.getCustomObject();
                Map args = w.getArguments();
                String key,
                 arg;
                String name = w.getProcedureName();

                // Bug 2886388 fix;
                // The first argument is always the procedure name (we need
                // to comply with the scripting language spec)
                line = name + "(\"" + w.getProcedureName() + "\", ";
                if (runAndIncludeSupported) {
                    // Test if the procedure is from this file or not
                    if (!w.getStartElement().getDocument().equals(interpret.getDocument())) {
                        line = "callProcedure(\"" + name + "\", \"" + name + "\", ";
                    }
                }
                if (args != null && args.size() > 1) {
                    // Procedure was called with arguments
                    ScriptingContext ctx = new ScriptingContextImpl();
                    ctx.putAll(event.getContext());
                    ctx.put(CONTEXT_CURRENT_SCRIPT_WRAPPER, w);

                    for (int i = 1; i < args.size(); i++) {
                        key = i + "";
                        if (args.containsKey(key)) {
                            arg = convertToExpression(convertToExpressionElements(args.get(key).toString(), ctx));
                            line += arg + ", ";
                        }
                    }
                }
                if (line.endsWith(", ")) {
                    line = line.substring(0, line.length() - 2);
                }
                line += ");\n";
                append(bufName, line);

                // Disable writing of commands within the procedure call.
                // This is necessary because the script manager validates
                // content of a procedure for each individual procedure call.
                procedureCallInProgress = true;
                break;
            case SCRIPT_DEBUG_PROCEDURE_CALL_ENDED:
                // Procedure call ended, reenable writing into the buffer
                procedureCallInProgress = false;
                break;
            case SCRIPT_DEBUG_STRUCTURE_WRAPPER_CREATED:
                GenericWrapper wr = (GenericWrapper) event.getWrapper();
                bufName = inProcedure ? BUF_METHODS : BUF_COMMANDS;

                if (wr instanceof IfWrapper) {
                    IfWrapper iw = (IfWrapper) wr;
                    append(bufName, convertIfStatement(iw, event.getContext()));
                } else if (wr instanceof ForWrapper) {
                    ForWrapper fw = (ForWrapper) wr;
//                    System.out.println("ForWrapper: isConditionMode=" + fw.isConditionMode() + ", condition=" + fw.getCondition() + "" +
//                            ", loopExpression=" + fw.getLoopExpression() + ", values=" + fw.getValues());
                    append(bufName, convertForStatement(fw, event.getContext()));
                }
                increaseIndent(bufName);
                break;
            case SCRIPT_DEBUG_STRUCTURE_WRAPPER_CONTINUED:
                if (event.getWrapper() instanceof IfWrapper) {
                    bufName = inProcedure ? BUF_METHODS : BUF_COMMANDS;
                    decreaseIndent(bufName);
                    IfWrapper iw = (IfWrapper) event.getWrapper();
                    append(bufName, convertElseStatement(iw, event.getContext()));
                    increaseIndent(bufName);
                }
                break;
            case SCRIPT_DEBUG_STRUCTURE_END_REACHED:
                // This means that a single closing brace '}' was reached
                bufName = inProcedure ? BUF_METHODS : BUF_COMMANDS;
                decreaseIndent(bufName);
                append(bufName, "}\n");
                break;
            case SCRIPT_DEBUG_COMMAND_IN_PROCEDURE_REACHED:
            case SCRIPT_DEBUG_VALIDATING_LINE:
                // This case corresponds to a single command line either in the
                // main script body or inside of a procedure definition.
                if (canWrite) {
                    line = (String) event.getCustomObject();
                    bufName = inProcedure ? BUF_METHODS : BUF_COMMANDS;
                    append(bufName, convertCommand(line, event.getContext()));
                }
                break;
            case SCRIPT_DEBUG_NESTED_BLOCK_CREATED:
                if (canWrite && event.getSource() instanceof NestedBlockInterpret) {
                    String txt = ((NestedBlockInterpret) event.getSource()).convertContent(TestScriptInterpret.TYPE_JAVA);
                    bufName = inProcedure ? BUF_METHODS : BUF_COMMANDS;
                    append(bufName, txt);
                }
                break;
            case SCRIPT_DEBUG_LINE_SKIPPED:
                // A line is being skipped. It is either a comment or empty line
                line = (String) event.getCustomObject();
                bufName = inProcedure ? BUF_METHODS : BUF_COMMANDS;
                if (line != null && line.startsWith("#")) {
                    // A comment is present
//                    append(bufName, "//" + line.substring(1) + "\n");
                    // Bug fix in 2.0.1 - do not insert comments immediately because
                    // they may
                    if (precedingComment == null) {
                        precedingComment = "";
                    }
                    precedingComment += "//" + line.substring(1) + "\n";
                } else {
                    // An empty line
//                    append(bufName, "\n");
//                    if (precedingComment == null) {
//                        precedingComment = "\n";
//                    } else {
//                        precedingComment += "\n";
//                    }
                }
                break;
        }
    }

    /**
     * Detemine whether Java code can be generated for the current context.
     * There are situations where we need to switch off the generator to prevent
     * it from handling some unwanted code. This method is called after and
     * script event received and checks whether we should process it.
     * @param event script event.
     * @return true indicates that we can generate Java code and insert it to
     * the buffers, false indicates that we should disable writing temporarily.
     */
    private boolean canInsert(ScriptEvent event) {
        if (event.getType() != ScriptDebuggingEvent.SCRIPT_DEBUG_STRUCTURE_END_REACHED) {
            TestWrapper wr = (TestWrapper) event.getContext().get(CONTEXT_CURRENT_SCRIPT_WRAPPER);

            if (runAndIncludeSupported) {
                boolean isRunOrInclude = wr instanceof RunWrapper || wr instanceof IncludeWrapper;
                TestWrapper wr2 = wr;
                while (wr != null && !isRunOrInclude && (wr2 = wr2.getParentWrapper()) != null) {
                    isRunOrInclude = wr2 instanceof RunWrapper || wr2 instanceof IncludeWrapper;
                }
                if (isRunOrInclude) {
                    return false;
                }
                TestScriptInterpret ti = event.getContext().getInterpret();
                if (ti != null && !ti.equals(interpret)) {
                    return false;
                }
            }

            // As the for structure with predefined set of values gets
            // validated for all listed values, we have to disable generation
            // of Java code for all loops except the very first one.
            ForWrapper fw = findForWrapper(wr);
            if (fw != null && fw.getLoopNumber() > 0) {
                return false;
            }
        }
        return true;
    }

    private ForWrapper findForWrapper(TestWrapper wr) {
        if (wr == null) {
            return null;
        }
        if (wr instanceof ForWrapper) {
            return (ForWrapper) wr;
        }
        return findForWrapper(wr.getParentWrapper());
    }

    /**
     * Get a buffer by its name. If it doesn't exist, it is created and inserted into
     * the code buffer map. The method is null-safe and always returns an instance of
     * StringBuffer.
     *
     * @param name output buffer name. Acceptable values are defined by
     * constants with the 'BUF_' prefix defined in this class.
     * @return a code buffer (StringBuffer instance) associated with the name.
     */
    private StringBuffer getBuf(String name) {
        StringBuffer buf = code.get(name);
        if (buf == null) {
            buf = new StringBuffer();
            code.put(name, buf);
        }
        return buf;
    }

    /**
     * Append a string (one or more lines) to the buffer associated with
     * the argument name. The method makes sure that all inserted lines get
     * properly indented.
     *
     * @param bufName output buffer name. Acceptable values are defined by
     * constants with the 'BUF_' prefix defined in this class.
     * @param code Java code to be appended.
     */
    private void append(String bufName, String code) {
        if (canWrite) {
            StringBuffer buf = getBuf(bufName);
            String indent = getIndentString(bufName);
            String s;

            if (precedingComment != null) {
                code = precedingComment + code;
                precedingComment = null;
            }

            BufferedReader r = new BufferedReader(new StringReader(code));
            try {
                while ((s = r.readLine()) != null) {
                    buf.append(indent + s + "\n");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Increase indent for a buffer.
     * @param bufName output buffer name. Acceptable values are defined by
     * constants with the 'BUF_' prefix defined in this class.
     */
    private void increaseIndent(String bufName) {
        int indent = indentMap.get(bufName).intValue() + 1;
        indentMap.put(bufName, indent);
    }

    /**
     * Decrease indent for a buffer.
     * @param bufName output buffer name. Acceptable values are defined by
     * constants with the 'BUF_' prefix defined in this class.
     */
    private void decreaseIndent(String bufName) {
        int indent = Math.max(indentMap.get(bufName).intValue() - 1, 0);
        indentMap.put(bufName, indent);
    }

    /**
     * Get the indent string associated with an output buffer. It is a string containing
     * one or more white characters (spaces or tabelators) which should be added
     * as a prefix to Java code lines inserted into the argument buffer to
     * ensure proper indent.
     *
     * @param bufName output buffer name. Acceptable values are defined by
     * constants with the 'BUF_' prefix defined in this class.
     * @return an indent string to be added to the beginning of the line inserted into the buffer.
     */
    private String getIndentString(String bufName) {
        String s = "";
        int indent = indentMap.get(bufName).intValue();
        for (int i = 0; i < indent; i++) {
            s += "   ";
        }
        return s;
    }

    /**
     * Convert a single command of the proprietary language to Java code.
     *
     * @param line a command from the test script, typically one line of code.
     * @param event validation context.
     * @return Java code to be appended to the associated code buffer.
     */
    private String convertCommand(String line, ScriptingContext ctx) {

        // First parse the command line into a list of arguments and argument->value map
        List args = new ArrayList();
        Map values = ctx.getParser().parse(line, args);
        final String command = args.get(0).toString().toUpperCase();

        // Proceed only if the list of args is greater than zero.
        // The first argument is the command name like Var, Eval, Screenshot etc.
        if (args.size() > 0) {
            String s = "";

            Boolean saveCmdAsComment = UserConfiguration.getInstance().getBoolean("javaconverter.insertCommandsAsComments");
            if (saveCmdAsComment != null && saveCmdAsComment) {
                s += "// " + line + "\n";
            }

            final boolean hasArgument = args.size() > 1 && !values.containsKey(args.get(1));
            if (hasArgument) {
                String argValue = args.get(1).toString();
                args.set(1, "argument");
                values.put("argument", argValue);
            }

            // Conversion can be done in three ways:
            // 1. replacement with Java native construction (Var, Eval, for, if/else, Exit)
            // 2. method mapping - maps the command to a method in DefaultJavaTestScript
            // 3. failover generic method

//            System.out.println("Cmd: '" + line + "', \n  args=" + args + ",\n  values=" + values);

            if (command.equalsIgnoreCase("var") || command.equalsIgnoreCase("variable")) {
                args.remove(0);  // Remove the command name
                return s + convertVariables(args, values, ctx);
            } else if (command.equalsIgnoreCase("eval")) {
                args.remove(0);  // Remove the command name
                return s + convertEval(args, values, ctx);
            } else if (command.equalsIgnoreCase("exit")) {
                String es = convertExit(args, values, ctx);
                if (es != null) {
                    return s + es;
                }
            }

            // From here on we need to enclose the code withe try-catch because
            // code generated below throws an I/O exception
//            needsTryAndCatch = true;

            // If the command contains an 'onpass', 'onfail' or 'ontimeout' parameter,
            // embed it into an 'if' or 'if/else' structure.
            String onPass = null;
            String onFail = null;

            List t = new ArrayList(args);
            for (Object o : args) {
                if (AbstractCommandHandler.PARAM_ONPASS.equalsIgnoreCase(o.toString())) {
                    onPass = values.get(AbstractCommandHandler.PARAM_ONPASS).toString();
                    t.remove(o);
                    values.remove(o);
                } else if (WaitforCommand.PARAM_ONTIMEOUT.equalsIgnoreCase(o.toString())) {
                    onPass = values.get(WaitforCommand.PARAM_ONTIMEOUT).toString();
                    t.remove(o);
                    values.remove(o);
                } else if (AbstractCommandHandler.PARAM_ONFAIL.equalsIgnoreCase(o.toString())) {
                    onFail = values.get(AbstractCommandHandler.PARAM_ONFAIL).toString();
                    t.remove(o);
                    values.remove(o);
                }
            }
            args = t;

            // 2. Map it onto a method in the existing Java API
            boolean hasOneLine = true;
            String cmd = getMethodMapper(ctx).convert(args, values, ctx, exceptionMap, importMap);

            if (cmd == null) {

                // 3. Failover conversion to generic code using the runScriptCommand() method
                cmd = convertCommandToFailoverCode(args, values);
//                needsTryAndCatch = true;
                exceptionMap.put(IOException.class, "");
                hasOneLine = false;
            }

            // This part of code handles the 'onpass', 'onfail' and 'ontimeout' parameters
            // and converts them to if and if/else statements
            if (onPass != null || onFail != null) {
                String temp = "";

                if (hasOneLine) {
                    if (cmd.endsWith(";\n")) {
                        temp = cmd.substring(0, cmd.length() - 2);
                    }
                }
                final String condition = hasOneLine ? temp : "getContext().getExitCode()";

                // Command has both onpass and onfail -> convert it to if/else
                if (onPass != null && onFail != null) {
                    temp = "if (" + condition + " == 0) {\n"
                            + "  " + convertCommand(onPass, ctx)
                            + "} else {\n"
                            + "  " + convertCommand(onFail, ctx)
                            + "}";
                } else if (onPass != null) {  // Command has just onpass
                    temp = "if (" + condition + " == 0) {\n"
                            + "  " + convertCommand(onPass, ctx)
                            + "}";
                } else {   // Command has just onfail
                    temp = "if (" + condition + " != 0) {\n"
                            + "  " + convertCommand(onFail, ctx)
                            + "}";
                }
                if (!hasOneLine) {
                    temp = cmd + "\n" + temp;
                }
                cmd = temp;
            }

            return s + cmd;
        }
        return "// Unconverted: please convert to Java manually\n" + line + "\n";
    }

    private String convertCommandToFailoverCode(List args, Map values) {
        String listName = "l" + mapInstanceCounter;
        String s = "List<String> " + listName + " = new ArrayList();\n";
        if (args.size() == 2) {
            s += listName + ".add(\"" + args.get(1) + "\");\n";
        } else if (args.size() > 2) {
            String array = "new String[] { ";
            for (int i = 1; i < args.size(); i++) {
                array += "\"" + args.get(i) + "\", ";
            }
            array += "}";
            s += listName + ".addAll(Arrays.asList(" + array + "));\n";
        }
        String mapName = "m" + mapInstanceCounter;
        s += "Map " + mapName + " = new HashMap();\n";
        for (Object o : values.keySet()) {
            s += mapName + ".put(\"" + o + "\", \"" + values.get(o) + "\");\n";
        }
        s += "runScriptCommand(\"" + args.get(0) + "\", " + listName + ", " + mapName + ");\n";
        mapInstanceCounter++;

        // add necessary imports of List, ArrayList, Map and HashMap
        importMap.put(List.class.getName(), "");
        importMap.put(ArrayList.class.getName(), "");
        importMap.put(Map.class.getName(), "");
        importMap.put(HashMap.class.getName(), "");
        importMap.put(Arrays.class.getName(), "");
        return s;
    }

    public void commandEvent(CommandEvent e) {
    }

    /**
     * Convert a 'for' statement to Java code.
     *
     * @param fw an instance of ForWrapper
     * @param event execution context
     * @return
     */
    private String convertForStatement(ForWrapper fw, ScriptingContext ctx) {
        String s = "";
        if (fw.isConditionMode()) {
            // Condition mode is like for (index=0; {index}<6; index={index}+1) {
            String init = fw.getInitialExpression();
            List args = new ArrayList();
            Map values = ctx.getParser().parse(init, args);
            init = convertEval(args, values, ctx);
            if (init.endsWith(";\n")) {
                init = init.substring(0, init.length() - 2);
            }

            String condition = fw.getCondition();
            condition = evaluateNumericExpression(condition, Boolean.class, ctx).toString();
            if (condition.endsWith(";\n")) {
                condition = condition.substring(0, condition.length() - 2);
            }

            String expr = fw.getLoopExpression();
            args.clear();
            values = ctx.getParser().parse(expr, args);
            expr = convertEval(args, values, ctx);
            if (expr.endsWith(";\n")) {
                expr = expr.substring(0, expr.length() - 2);
            }

            s = "for (" + init + "; " + condition + "; " + expr + ") {";
        } else {
            List values = fw.getValues();
            String varName = fw.getVariable();
            String arrayName = "values" + forInstanceCounter;
            s = "String " + arrayName + "[] = new String[] { ";
            for (Object val : values) {
                s += "\"" + val.toString() + "\", ";
            }
            s += "};\n";
            s += "for (String " + varName + " : " + arrayName + ") {";
            forInstanceCounter++;
        }
        return s;
    }

    /**
     * Convert an 'if' statement.
     * @param iw document wrapper of the 'if' structure to Java.
     * @param context validation context.
     * @return corresponding Java code.
     */
    private String convertIfStatement(IfWrapper iw, ScriptingContext context) {
        String condition = iw.getConditionExpression();
        condition = evaluateNumericExpression(iw.getConditionExpression(), Boolean.class).toString();
        String s = "if (" + condition + ") {";
        return s;
    }

    /**
     * Convert an 'else' or 'else if' statement to Java.
     * @param iw document wrapper of the 'if' structure.
     * @param context validation context.
     * @return corresponding Java code.
     */
    private String convertElseStatement(IfWrapper iw, ScriptingContext context) {
        String condition = iw.getConditionExpression();
        if (condition != null) {
            return "} else " + convertIfStatement(iw, context);
        } else {
            return "} else {";
        }
    }

    private String convertEval(List args, Map values, ScriptingContext ctx) {
        // One Eval command may contain one or more variable definitions -> iterate
        List<CharSequence> l;
        String jValue = "";
        String s = "";
        Object value;

        for (Object var : args) {
            value = values.get(var);
            l = convertToExpressionElements((String) value, ctx);
            jValue = evaluateNumericExpression((String) value, Float.class).toString();

            if (variableMap.containsKey(var.toString())) {
                Variable v = variableMap.get(var.toString());
//                System.out.println("EVAL: Modifying already existing variable '" + var + "'");

                // TODO: show a list of variables to user and let him confirm the type
                s += var.toString() + " = " + jValue + ";\n";
                v.expressionComponents = l;
                v.value = value;
                v.jValue = jValue;

            } else if (contextVariables.containsKey(var.toString()) || var.toString().startsWith("_")) {
                // This is a default (explicit) variable provided by the context.
                // We have to modify it in the context's variable table.
                s += "getContext().setVariable(\"" + var + "\", " + jValue + ");\n";
            } else {
//                System.out.println("EVAL: Defining a new variable '" + var + "'");
                Variable v = new Variable();

                // Find out if the expression elements contain at least one
                // float variable or a string variable which looks like a float number
                boolean containsFloat = false;
                for (CharSequence cs : l) {
                    if (cs instanceof StringBuffer) {
                        Variable vv = variableMap.get(cs.toString());
                        if (vv != null) {
                            if (vv.type == Variable.TYPE_FLOAT) {
                                containsFloat = true;
                                break;
                            } else if (vv.type == Variable.TYPE_STRING && vv.jValue != null && vv.jValue.matches("[0-9].[0-9]")) {
                                containsFloat = true;
                                break;
                            }
                        }
                    }
                }
                String jType;
                if (containsFloat) {
                    v.type = Variable.TYPE_FLOAT;
                    jType = "float";
                } else {
                    v.type = Variable.TYPE_INT;
                    jType = "int";
                }
                v.name = var.toString();
                variableMap.put(var.toString(), v);
                s += jType + " " + var.toString() + " = " + jValue + ";\n";
                v.expressionComponents = l;
                v.value = value;
                v.jValue = jValue;
            }
        }
        return s;
    }

    private String convertVariables(List args, Map values, ScriptingContext ctx) {
        String s = "";
        Object value;
        String jValue = "";
        List<CharSequence> l;
        int actualType = Variable.TYPE_UNKNOWN;

        // One Var command may contain one or more variable definitions -> iterate
        for (Object var : args) {
            value = guessVariableType(values.get(var), ctx);
            l = null;

            if (value instanceof String) {
                // TODO: Handling of nested variables
                l = convertToExpressionElements(value.toString(), ctx);
                jValue = convertToExpression(l);
                actualType = Variable.TYPE_STRING;
            } else if (value instanceof Number) {
                // TODO: Result of parsing is always a Double. We might use int, long and float as well
                Number n = (Number) value;
                if (n.intValue() == n.floatValue()) {
                    value = new Integer(n.intValue());
                    actualType = Variable.TYPE_INT;
                } else {
                    value = new Float(n.floatValue());
                    actualType = Variable.TYPE_FLOAT;
                }
                jValue = "" + value;
            } else if (value instanceof Point) {
                jValue = "new Point(" + ((Point) value).getX() + ", " + ((Point) value).getX() + ")";
                importMap.put(Point.class.getName(), "");
            } else if (value instanceof Rectangle) {
                Rectangle r = (Rectangle) value;
                jValue += "new Rectangle(" + r.x + ", " + r.y + ", " + r.width + ", " + r.height + ")";
                importMap.put(Rectangle.class.getName(), "");
            }

            if (variableMap.containsKey(var.toString())) {
                Variable v = variableMap.get(var.toString());
//                System.out.println("Modifying already existing variable '" + var + "'");

                // The type was guessed when the variable was defined for the first time.
                // Now we have to perform conversions of other types to the original type.
                // For example, when a variable is first defined as "Var INDEX=1", it will
                // be defined in Java as "int INDEX = 1;". When the variable is assigned
                // another value later on and it doesn't look like an int, we have to make
                // sure to generate Java code which converts the value to an integer.
                switch (v.type) {
                    case Variable.TYPE_FLOAT:
                        switch (actualType) {
                            case Variable.TYPE_STRING:  // String to float conversion
                                jValue = "Float.parseFloat(" + jValue + ")";
                                break;
                        }
                        break;
                    case Variable.TYPE_INT:
                        switch (actualType) {
                            case Variable.TYPE_STRING:  // String to int conversion
                                jValue = "Integer.parseInt(" + jValue + ")";
                                break;
                            case Variable.TYPE_FLOAT:   // Float to int conversion
                                // This might be a source of errors. See if this can be reported to the user.
                                // Alternatively offer a switch forcing all variables to String and
                                // all Eval results to float or double.
                                jValue = "(int)" + jValue;
                                break;
                        }
                        break;
                    case Variable.TYPE_STRING:
                    case Variable.TYPE_UNKNOWN:
                        break;
                }
                // TODO: check the type and perform a conversion if necessary
                // TODO: show a list of variables to user and let him confirm the type
                s += var.toString() + " = " + jValue + ";\n";
                v.expressionComponents = l;
                v.value = value;
                v.jValue = jValue;

            } else if (contextVariables.containsKey(var.toString()) || var.toString().startsWith("_")) {
                // This is a default (explicit) variable provided by the context.
                // We have to modify it in the context's variable table.
                s += "getContext().setVariable(\"" + var + "\", " + jValue + ");\n";
            } else {
//                System.out.println("Defining a new variable '" + var + "'");
                Variable v = new Variable();
                v.name = var.toString();
                variableMap.put(var.toString(), v);
                String type;
                if (value instanceof Integer) {
                    type = "int";
                    v.type = Variable.TYPE_INT;
                } else if (value instanceof Float) {
                    type = "float";
                    v.type = Variable.TYPE_FLOAT;
                } else if (value instanceof String) {
                    type = value.getClass().getSimpleName();
                    v.type = Variable.TYPE_STRING;
                } else {
                    type = value.getClass().getSimpleName();
                }
                s += type + " " + var.toString() + " = " + jValue + ";\n";
                v.expressionComponents = l;
                v.value = value;
                v.jValue = jValue;
            }
        }
        return s;
    }

    public Object guessVariableType(Object value, ScriptingContext ctx) {
        if (value instanceof StringBuffer) {
            if (variableMap.containsKey(value)) {
                // Java variable
                Variable v = variableMap.get(value);
                return v.value;
            } else {
                // Context variable
                return guessVariableType(ctx.getVariable(value.toString()), ctx);
            }
        }
        if (value == null) {
            return "";
        }
        if (!(value instanceof String)) {
            return value;
        }
        TokenParser parser = ctx.getParser();
        try {
            return parser.parseRectangle(value, "");
        } catch (Exception ex) {
        }
        try {
            return parser.parsePoint(value, "");
        } catch (Exception ex) {
        }
        try {
            return parser.parseNumber(value, "");
        } catch (Exception ex) {
        }

        return value.toString();
    }

    /**
     * <p>Convert a list of expression components into a Java expression. The
     * argument list is expected to contain String and StringBuffer instances.
     * Strings represent string constants. StringBuffer elements represent names
     * of variables which are either defined in the generated Java code or
     * they are present in the variable table of the associated context.</p>
     *
     * <p>The latter case applies to explicit variables which are generated
     * internally by the test framework. See specification of the Var command
     * for more information.</p>
     *
     * @param l list of expression elements.
     * @return Java expression corresponding to the list of elements.
     */
    public String convertToExpression(List<CharSequence> l) {
        String expr = "";
        if (l.size() > 0) {
            boolean hasString = false;
            for (CharSequence s : l) {

                // Instances of StringBuffer represent variable names
                if (s instanceof StringBuffer) {
                    String name = s.toString();

                    // It is a variable name
                    if (variableMap.containsKey(name)) {
                        // Java variable
                        expr += name + "+";

                        Variable v = variableMap.get(name);
                        if (v.type == Variable.TYPE_STRING) {
                            hasString = true;
                        }
                    } else {
                        if (name.equals("_CURTIME")) {
                            expr += "System.currentTimeMillis()+";
                        } else {
                            expr += "getContext().getVariable(\"" + name + "\")+";
                        }
                    }
                    // Instances of StringBuilder represent procedure (method) arguments
                } else if (s instanceof StringBuilder) {
                    expr += procedureArgumentName + "[" + s + "]+";
                    hasString = true;
                } else {
                    expr += "\"" + s + "\"+";
                    hasString = true;
                }
            }
            // Remove the extra '+' from the end
            expr = expr.substring(0, expr.length() - 1);

            // If there is no String variable, we have to add empty string to the beginning
            if (!hasString) {
                expr = "\"\"+" + expr;
            }
        }
        return expr;
    }

    /**
     * Get the method mapper class.
     * @param ctx scripting context.
     * @return the methodMapper
     */
    private JavaTestScriptMethodMapper getMethodMapper(ScriptingContext ctx) {
        if (methodMapper == null) {
            methodMapper = new JavaTestScriptMethodMapper(this, ctx.getParser());
        }
        return methodMapper;
    }

    public String getCode() {
        return "default";
    }

    public String getDisplayName() {
        return ApplicationSupport.getString("javaconverter.default.pluginName");
    }

    public String getDescription() {
        return MessageFormat.format(ApplicationSupport.getString("javaconverter.default.pluginDesc"), ApplicationSupport.APPLICATION_NAME);
    }

    public String getUniqueId() {
        return "native_default_Java_converter";
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
        return JavaTestScriptConverter.class;
    }

    public boolean requiresRestart() {
        return false;
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
     * Check whether all dependencies are installed. As report providers are
     * closely integrated with the Report command, the method throws a DependencyMissingException
     * if the command handler is not installed.
     *
     * @param manager plugin manager instance.
     * @throws com.tplan.robot.plugin.DependencyMissingException when one or more required dependencies is not installed.
     */
    public void checkDependencies(PluginManager manager) throws DependencyMissingException {
    }

    public void setConfiguration(UserConfiguration cfg) {
    }

    public List<Preference> getPreferences() {
        List<Preference> v = new ArrayList();

        String containerName = ApplicationSupport.getString("options.scripting.javaConverter.javaPrefs");

        Preference o = new Preference("javaconverter.packageName", Preference.TYPE_STRING,
                ApplicationSupport.getString("options.javaConverter.packageName"),
                null);
        o.setPreferredContainerName(containerName);
        v.add(o);

        o = new Preference("javaconverter.className", Preference.TYPE_STRING,
                ApplicationSupport.getString("options.javaConverter.className"),
                null);
        o.setPreferredContainerName(containerName);
        v.add(o);

        o = new Preference("javaconverter.superClassName", Preference.TYPE_STRING,
                ApplicationSupport.getString("options.javaConverter.superClassName"),
                null);
        o.setPreferredContainerName(containerName);
        v.add(o);

        o = new Preference("javaconverter.createMainMethod", Preference.TYPE_BOOLEAN,
                ApplicationSupport.getString("options.javaConverter.createMainMethod"),
                null);
        o.setPreferredContainerName(containerName);
        v.add(o);

        o = new Preference("javaconverter.insertCommandsAsComments", Preference.TYPE_BOOLEAN,
                ApplicationSupport.getString("options.javaConverter.insertCommandsAsComments"),
                null);
        o.setPreferredContainerName(containerName);
        v.add(o);

        return v;
    }

    public List<CharSequence> convertToExpressionElements(String expr, ScriptingContext ctx) {
        return convertToExpressionElements(expr, ctx, inProcedure, false);
    }

    @Override
    public List parseExpression(String expression) {
        if (context == null) {
            throw new IllegalArgumentException("Context not set!");
        }
        return convertToExpressionElements(expression, context, inProcedure, true);
    }

    public List<CharSequence> convertToExpressionElements(String expr, ScriptingContext ctx, boolean insideProcedure, boolean full) {

        List<CharSequence> l = new ArrayList();

        // First get the variable hash table from the execution context. Create an empty one if it doesn't exist.
        Map variables = new HashMap(ctx.getVariables());

        // Apply variables passed via CLI if there are any
        Map cliVars = ctx.getCommandLineVariables();
        if (cliVars != null) {
            variables.putAll(cliVars);
        }

        // Apply the procedure arguments
        TestWrapper wrapper = (TestWrapper) ctx.get(CONTEXT_CURRENT_SCRIPT_WRAPPER);

        // Find out if ve are in a procedure or any parent wrapper is procedure
        boolean inProcedure = false;
        while (wrapper != null) {
            if (wrapper instanceof ProcedureWrapper) {
                inProcedure = true;
                break;
            }
            wrapper = wrapper.getParentWrapper();
        }

        if (inProcedure) {
            Map args = ((ProcedureWrapper) wrapper).getArguments();
            if (args != null) {
                variables.putAll(args);
            }
        }

        List ll;
        if (full) {
            ll = super.parseExpression(expr);
        } else {
            ll = new ArrayList();
            ll.add(expr);
        }

        int position = 0;
        int start = 0;
        String pre, post, name, ex;
        CharSequence val;
        for (Object e : ll) {
            ex = e.toString();
            if ((position = ex.indexOf('}', position)) >= 0) {

                // Find the matching left curly brace
                start = ex.substring(0, position).lastIndexOf('{');

                // If there's a matching left curly brace, get the var name and try to get its fullName
                if (start >= 0 && expr.length() > start + 1) {
                    name = ex.substring(start + 1, position);
                    post = ex.substring(position + 1);
                    val = ex.substring(0, position + 1);

                    // Bug fix - if the variable name starts with '_', we don't check existence of the variable
                    boolean isVariable = variableMap.containsKey(name) || variables.containsKey(name) || name.startsWith("_");
                    boolean isProcArg = false;

                    // Special handling in procedures - expressions like {1} etc. are arguments
                    if (!isVariable && insideProcedure) {
                        try {
                            int index = Integer.parseInt(name);
                            if (index == 0) {  // Procedure argument #0 is the procedure name
                                val = procedureName;
                            } else {
                                val = new StringBuilder(name);
                            }
                            isProcArg = true;
                        } catch (Exception exc) {
                            // Ignore any parsing exceptions
                        }
                    }
                    if (isVariable || isProcArg) {
                        pre = ex.substring(0, start);
                        ex = pre + name + post;
                        if (pre.length() > 0) {
                            l.add(pre);
                        }
                    }
                    if (isVariable) {
                        l.add(new StringBuffer(name));
                    } else {
                        l.add(val);
                    }
                    if (post.length() > 0) {
                        List<CharSequence> l2 = convertToExpressionElements(post, ctx, insideProcedure, full);
                        l.addAll(l2);
                    }
                }
            } else {
                // Bug 2955616 fix - Values with backslash character not converted correctly
                l.add(ex.replace("\\", "\\\\"));
            }
        }
        return l;
    }

    protected Object evaluate(Object var1, Object var2, Object operand) {

        // Return value (will be a Number or Boolean instance)
        Object o = null;

        // Second argument is null -> must be an unary operator
        // and the operand must be converted to a number
        if (var1 == null) {
            if (unaryOperators.containsKey(operand.toString())) {
                Object s = convertTokenToType(var1, Float.class);
                if (operand.equals("unary" + OPERATOR_MINUS)) {
                    o = new ExpressionElement("-" + o, Float.class);
                } else {
                    o = new ExpressionElement("" + o, Float.class);
                }
            }

        } // Numeric operators - plus, minus, multiply, divide
        else if (numericOperators.containsKey(operand)) {
            Object o1 = convertTokenToType(var1, Float.class);
            Object o2 = convertTokenToType(var2, Float.class);
            o = new ExpressionElement("" + o1 + operand + o2, Float.class);

        } // Boolean operators - equals, not equals, greater than, less than, AND, OR
        else if (booleanOperators.containsKey(operand)) {
            // OR and AND operators always requires two boolean arguments
            if (operand.equals(OPERATOR_OR) || operand.equals(OPERATOR_AND)) {
                Object o1 = convertTokenToType(var1, Boolean.class);
                Object o2 = convertTokenToType(var2, Boolean.class);
                o = new ExpressionElement("(" + o1 + ")" + operand + "(" + o2 + ")", Boolean.class);

            } else {
                // Other boolean operators allow comparison between numbers/strings
                // Test if we should convert it to String.equals() or String.compare()

                boolean forceToFloat = false;
                if (var1 instanceof ExpressionElement) {
                    Class c = ((ExpressionElement) var1).type;
                    forceToFloat = c.equals(Float.class) || c.equals(float.class) || c.equals(Integer.class) || c.equals(int.class);
                }
                if (!forceToFloat && var2 instanceof ExpressionElement) {
                    Class c = ((ExpressionElement) var2).type;
                    forceToFloat = c.equals(Float.class) || c.equals(float.class) || c.equals(Integer.class) || c.equals(int.class);
                }
                if (!forceToFloat) {
                    boolean b1 = !(guessVariableType(var1, context) instanceof Number);
                    boolean b2 = !(guessVariableType(var2, context) instanceof Number);
                    forceToFloat = !b1 && !b2;
                }

                Object o1, o2;
                if (forceToFloat) {
                    o1 = convertTokenToType(var1, Float.class);
                    o2 = convertTokenToType(var2, Float.class);
                    o = new ExpressionElement("" + o1 + operand + o2, Boolean.class);
                } else {
                    o1 = convertTokenToType(var1, String.class);
                    o2 = convertTokenToType(var2, String.class);

                    // Improvement: if one of the arguments is a plain value,
                    // put it to the first place. This makes the code more robust
                    // against NPE caused by an attempt to call a method on null variable value
                    if (var2 instanceof String && var1 instanceof StringBuffer) {
                        Object temp = o2;
                        o2 = o1;
                        o1 = temp;
                    }
                    if (operand.equals(OPERATOR_EQUALS)) {
                        o = new ExpressionElement("" + o1 + ".equals(" + o2 + ")", Boolean.class);
                    } else if (operand.equals(OPERATOR_EQUALS_NOT)) {
                        o = new ExpressionElement("!" + o1 + ".equals(" + o2 + ")", Boolean.class);
                    } else if (operand.equals(OPERATOR_GREATER_THAN)) {
                        o = new ExpressionElement("" + o1 + ".compareTo(" + o2 + ") > 0", Boolean.class);
                    } else if (operand.equals(OPERATOR_LOWER_THAN)) {
                        o = new ExpressionElement("" + o1 + ".compareTo(" + o2 + ") < 0", Boolean.class);
                    }
                }
            }
        }
        if (o == null) {
            throw new IllegalArgumentException("Illegal operator '" + operand + "'.");
        }
        return o;
    }

    private Object convertTokenToType(Object token, Class type) {
        if (type.equals(String.class)) {
            if (token instanceof ExpressionElement && ((ExpressionElement) token).type.equals(String.class)) {
                return token;
            }
            // Check if the value is an existing variable or not
            if (token instanceof StringBuffer) {
                String name = token.toString();

                // It is a variable name
                if (variableMap.containsKey(name)) {
                    // Java variable
                    Variable v = variableMap.get(name);
                    if (v.type == Variable.TYPE_STRING) {
                        return new ExpressionElement(name, type);
                    }
                    return new ExpressionElement("(\"\"+" + name + ")", type);
                }
                if (name.equals("_CURTIME")) {
                    return new ExpressionElement("\"\"+System.currentTimeMillis()", type);
                }

                return new ExpressionElement("getVariableAsString(\"" + name + "\")", type);
            }
            return new ExpressionElement("\"" + token + "\"", type);

        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            if (token instanceof ExpressionElement) {
                ExpressionElement e = (ExpressionElement) token;
                if (e.type.equals(Float.class) || e.type.equals(float.class)) {
                    return new ExpressionElement("(int)" + e, type);
                } else if (e.type.equals(Integer.class) || e.type.equals(int.class)) {
                    return e;
                }
            }
            // Check if the value is an existing variable or not
            if (token instanceof StringBuffer) {
                String name = token.toString();

                // It is a variable name
                if (variableMap.containsKey(name)) {
                    // Java variable
                    Variable v = variableMap.get(name);
                    if (v.type == Variable.TYPE_INT) {
                        return new ExpressionElement(name, type);
                    } else if (v.type == Variable.TYPE_FLOAT) {
                        // Float needs to be retyped to prevent the "Possible loss of precision" Java error
                        return new ExpressionElement("(int)" + name, type);
                    }
                    if (name.equals("_CURTIME")) {
                        return new ExpressionElement("(int)System.currentTimeMillis()", type);
                    }
                    return new ExpressionElement("Integer.parseInt(\"\"+" + name + ")", type);
                }
                return new ExpressionElement("getVariableAsInt(\"" + name + "\")", type);
            }
            Object value = guessVariableType(token, context);
            if (value instanceof Number) {
                return new ExpressionElement("" + ((Number) value).intValue(), type);
            } else {
                return new ExpressionElement("Integer.parseInt(\"" + value + "\")", type);
            }
        } else if (type.equals(Float.class) || type.equals(float.class)) {
            if (token instanceof ExpressionElement) {
                ExpressionElement e = (ExpressionElement) token;
                if (e.type.equals(Float.class) || e.type.equals(float.class) || e.type.equals(Integer.class) || e.type.equals(int.class)) {
                    return e;
                }
            }
            // Check if the value is an existing variable or not
            if (token instanceof StringBuffer) {
                String name = token.toString();

                // It is a variable name
                if (variableMap.containsKey(name)) {
                    // Java variable
                    Variable v = variableMap.get(name);
                    if (v.type == Variable.TYPE_FLOAT || v.type == Variable.TYPE_INT) {
                        return new ExpressionElement(name, type);
                    }
                    if (name.equals("_CURTIME")) {
                        return new ExpressionElement("(float)System.currentTimeMillis()", type);
                    }
                    return new ExpressionElement("Float.parseFloat(\"\"+" + name + ")", type);
                }
                return new ExpressionElement("getVariableAsFloat(\"" + name + "\")", type);
            }
            Object value = guessVariableType(token, context);
            if (value instanceof Number) {
                // Be smart and do not generate a float value if it can be integer
                Number n = (Number) value;
                if (n.intValue() == n.floatValue()) { // Value is integer
                    return new ExpressionElement("" + n.intValue(), Integer.class);
                }
                return new ExpressionElement("" + n.floatValue(), type);
            } else {
                return new ExpressionElement("Float.parseFloat(\"" + value + "\")", type);
            }
        } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            if (token instanceof ExpressionElement) {
                ExpressionElement e = (ExpressionElement) token;
                if (e.type.equals(Boolean.class) || e.type.equals(boolean.class)) {
                    return e;
                }
            }
            // Check if the value is an existing variable or not
            if (token instanceof StringBuffer) {
                String name = token.toString();

                // It is a variable name
                if (variableMap.containsKey(name)) {
                    // Java variable
                    Variable v = variableMap.get(name);
                    return new ExpressionElement("new Boolean(" + name + ")", type);
                }
                return new ExpressionElement("new Boolean(getContext().getVariable(\"" + name + "\"))", type);
            }
            Object value = guessVariableType(token, context);
            if (value instanceof Boolean) {
                return new ExpressionElement(value.toString(), type);
            } else {
                return new ExpressionElement("new Boolean(\"" + value + "\")", type);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported class " + type.getName());
        }
    }

    @Override
    protected Object convertResult(Object lastStackItem, Class resultType, boolean isSingleOperandExpression) {
        if (isSingleOperandExpression) {
            return convertTokenToType(lastStackItem, resultType);
        }
        return lastStackItem;
    }

    /**
     * Expression element is fragment of a Java boolean or numeric expression.
     * It is basically just a String accompanied by an class indicator helping
     * to decide whether the element needs to be retyped or parsed to meet the
     * object type required by the higher expression element or the expression
     * as a whole.
     */
    private class ExpressionElement {

        public String expression = "";
        public Class type = String.class;

        ExpressionElement(String expression, Class type) {
            this.expression = expression;
            this.type = type;
        }

        @Override
        public String toString() {
            return expression;
        }
    }

    /**
     * The Variable class represents a variable defined either through the Var
     * or Eval command which has been converted to a native Java variable.
     * For example, "Eval i=0" may be converted to Java as "int i=0;".
     * All such variables are maintained in a map.
     */
    private class Variable {

        static final int TYPE_UNKNOWN = 0;
        static final int TYPE_INT = 1;
        static final int TYPE_FLOAT = 2;
        static final int TYPE_STRING = 3;
        String name;
        Object value;
        List<CharSequence> expressionComponents;
        String jValue;
        int type;
    }
}
