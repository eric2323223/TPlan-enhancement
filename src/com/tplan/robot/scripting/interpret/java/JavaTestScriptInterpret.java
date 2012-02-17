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
package com.tplan.robot.scripting.interpret.java;

import com.tplan.robot.scripting.interpret.*;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.scripting.*;
import com.tplan.robot.util.DocumentUtils;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/**
 * Interpret of Java test scripts.
 * @product.signature
 */
public class JavaTestScriptInterpret extends AbstractTestScriptInterpret implements JavaElementMapper {

    private DefaultStyledDocument document = null;
    private static int unnamedDocCounter = 1;
    private JavaTestScript testInstance = null;

    // This flag is used to make sure that we throw the interpret error of
    // missing compiler just once
    private static boolean compilerMissingErrorThrown = false;

    // On-the-fly Java compiler
    private JavaCodeCompiler compiler;

    // Compiler error handler, by default this class but may be customized.
    private JavaElementMapper errorHandler = this;
    private boolean debug = System.getProperty("javainterpret.debug") != null;

    public int getType() {
        return TYPE_JAVA;
    }

    public String[] getSupportedFileExtensions() {
        return new String[]{"java", "class"};
    }

    public StyledDocument getDocument() {
        if (document == null) {
            document = new DefaultStyledDocument();
        }
        return document;
    }

    public String getDisplayName() {
        return ApplicationSupport.getString("interpret.java.pluginName");
    }

    public String getDescription() {
        return ApplicationSupport.getString("interpret.java.pluginDesc");
    }

    public String getUniqueId() {
        return "native_Java_test_script_interpret";
    }

    private void debugPath(String path) {
        String s[] = path.split(File.pathSeparator);
        File f;
        File ff[];
        for (String l : s) {
            f = new File(l);
            if (f.exists() && f.isDirectory()) {
                System.out.println("  " + l + " (directory):");
                ff = f.listFiles();
                for (File f2 : ff) {
                    System.out.println("    " + f2.getName());
                }
            } else {
                System.out.println("  " + l);
            }
        }
    }

    /**
     * Compile the Java source code contained in the document.
     *
     * @param customContext custom context for the compilation. If null is provided, the
     * class asks the script manager to create a context instance for the compilation.
     *
     * @return true if the compilation succeeds, false if it fails.
     *
     * @throws com.tplan.robot.scripting.interpret.InterpretErrorException thrown when
     * the compiler is not available. In most cases it is caused by users running
     * Java Runtime Environment (JRE) instead of Java Development Kit (JDK).
     */
    public boolean compile(ScriptingContext customContext) throws InterpretErrorException {
        if (debug) {
//            System.out.println("JavaTestScriptInterpret.compile(): Environment:");
//            System.out.println(" java.library.path");
//            debugPath(System.getProperty("java.library.path"));
//            System.out.println(" ==================== \n sun.boot.class.path");
//            debugPath(System.getProperty("sun.boot.class.path"));
//            System.out.println(" ==================== \n sun.boot.library.path");
//            debugPath(System.getProperty("sun.boot.library.path"));
//            System.out.println(" ====================\n");

//                    "\n java.library.path="+System.getProperty("java.library.path") +
//                    "\n sun.boot.class.path="+System.getProperty("sun.boot.class.path") +
//                    "\n sun.boot.library.path="+System.getProperty("sun.boot.library.path") +
//                    "");
        }
        if (customContext != null) {
            compilationContext = customContext;
            scriptManager = customContext.getScriptManager();
        } else {
            if (scriptManager == null) {
                if (debug) {
                    System.out.println("JavaTestScriptInterpret: The interpret has no reference to the script manager. Set it through setScriptManager() first.");
                }
                throw new InterpretErrorException(this, "The interpret has no reference to the script manager. Set it through setScriptManager() first.");
            }
            compilationContext = scriptManager.createDefaultContext();
        }
        compilationContext.put(ScriptingContext.CONTEXT_INTERPRET, this);
        compilationContext.put(ScriptingContext.CONTEXT_COMPILATION_FLAG, new Boolean(true));
        scriptManager.fireScriptEvent(new ScriptEvent(this, compilationContext, ScriptEvent.SCRIPT_COMPILATION_STARTED));

        List<SyntaxErrorException> errors = compilationContext.getCompilationErrors();
        if (errors == null) {
            errors = new ArrayList();
            compilationContext.put(ScriptingContext.CONTEXT_COMPILATION_ERRORS, errors);
        }
        boolean isCompiler = false;

        try {
            instantiateCompiler();
            isCompiler = true;
        } catch (Exception ex) {
            if (debug) {
                System.out.println("JavaTestScriptInterpret: Failed to instantiate compiler. Exception:\n");
                ex.printStackTrace();
            }
            if (compilerMissingErrorThrown) { // We have already thrown the exception, just add an error
                errors.add(new SyntaxErrorException(ex.getMessage()));
            } else {  // First time reported, display the message to user
                compilerMissingErrorThrown = true;
                throw new InterpretErrorException(this, ex.getMessage(), ex);
            }
        }

        if (isCompiler) {
            final DiagnosticCollector<JavaFileObject> errs = new DiagnosticCollector<JavaFileObject>();

            String className;
            String basePath = null;
            if (uri == null) {
                className = ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.unnamedDocument") + unnamedDocCounter++;
            } else {
                File f = new File(uri.getPath());
                className = f.getName().substring(0, f.getName().lastIndexOf("."));
                try {
                    compilationContext.setVariable(ScriptingContext.IMPLICIT_VARIABLE_SCRIPT_DIR, f.getCanonicalFile().getParent());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            MemoryJavaSourceObject mo = null;
            try {
                String text = "";
                try {
                    Document doc = getDocument();
                    text = doc.getText(0, doc.getLength());
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }

                // Parse the package and class name from the Java source code
                String info[] = compiler.parsePackageAndClassName(text);
                String pkg = info[0];
                className = info[1];

                if (pkg == null) {
//                    errors.add(new SyntaxErrorException("WARNING: Could not parse package name."));
                }
                if (className == null) {
                    errors.add(new SyntaxErrorException(ApplicationSupport.getString("interpret.java.cannotParseClassName")));
                    if (debug) {
                        System.out.println("JavaTestScriptInterpret: Failed to parse class name.");
                    }
                    return false;
                }
                final String fullName = (pkg == null || pkg.trim().length() == 0) ? className : pkg + "." + className;

                // Base path
                if (uri == null) {
                    basePath = System.getProperty("user.home");
                } else {
                    File f;
                    try {
                        f = new File(uri).getCanonicalFile();
                    } catch (Exception e) {
                        f = new File(uri.getPath());
                    }

                    // Get the package folder of the current .java file
                    f = f.getParentFile();

                    String pkgs[] = pkg.split("\\.");
                    for (int i = pkgs.length - 1; i >= 0; i--) {
                        if (f.getName().equals(pkgs[i])) {
                            f = f.getParentFile();
                        }
                    }
                    basePath = f.getAbsolutePath();
                }


                // Compile the code
                mo = new MemoryJavaSourceObject(className, this);
                Class<JavaTestScript> compiledCode = compiler.compile(fullName, basePath, mo, errs);

                if (compiledCode != null) {
                    setTestInstance(compiledCode.newInstance());
                    modified = false;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                errors.add(new SyntaxErrorException(MessageFormat.format(ApplicationSupport.getString("interpret.java.compilationFailed"), ex.getMessage())));
            }
            // Convert the Java compiler diagnostic messages to our SyntaxErrorException instances
            errorHandler.processErrors(this, errs, errors, mo);
            if (debug && errors.size() > 0) {
                System.out.println("JavaTestScriptInterpret: Failed to compile Java code. Errors:\n");
                int i = 0;
                for (SyntaxErrorException e : errors) {
                    System.out.println("  Line " + e.getLineIndex() + ": " + e.getMessage());
                }
            }
        }

        scriptManager.fireScriptEvent(new ScriptEvent(this, compilationContext, ScriptEvent.SCRIPT_COMPILATION_FINISHED));
        return isCompiler && errors.size() == 0;
    }

    public void processErrors(JavaTestScriptInterpret interpret, DiagnosticCollector<JavaFileObject> src, List<SyntaxErrorException> target, SimpleJavaFileObject mo) {
        SyntaxErrorException e;
        int line;
        for (Diagnostic d : src.getDiagnostics()) {
            Object o = d.getSource();
            // Bug 2873049 - Process only Java errors, ignore warnings (TODO)
            // Filter out all messages unrelated to our Java class (represented by MemoryJavaSourceObject)
            if (o != null && o.equals(mo) && d.getKind() == Kind.ERROR) {
                e = new SyntaxErrorException(d.getMessage(null));
                line = (int) d.getLineNumber() - 1;
                e.setLineIndex(line);
                e.setElement(DocumentUtils.getElementForLine(document, line, document.getRootElements()[0]));
                target.add(e);
            }
        }
    }

    private void instantiateCompiler() throws Exception {
        if (compiler == null) {
            try {
                compiler = new JavaCodeCompiler<JavaTestScript>();
            } catch (IllegalStateException e) {
                throw new Exception(MessageFormat.format(ApplicationSupport.getString("interpret.java.compilerMissing"), System.getProperty("java.home")));
            }
        }
    }

    public int execute(ScriptingContext customContext) throws InterpretErrorException {
        // TODO: if the document has been compiled and hasn't been modified, skip compilation
        // TODO: save the instance to a .class format
        boolean canExecute = true;
        executing = true;

        // Initialize context
        executionContext = customContext != null ? customContext
                : scriptManager.createDefaultContext();
        executionContext.put(ScriptingContext.CONTEXT_INTERPRET, this);
        executionContext.remove(SFLAG);

        // Fire an event about start of the execution
        scriptManager.fireScriptEvent(new ScriptEvent(this, null, executionContext, ScriptEvent.SCRIPT_EXECUTION_STARTED));

        try {
            if (getURI() != null) {
                try {
                    File f = new File(getURI().getPath());
                    executionContext.setVariable(ScriptingContext.IMPLICIT_VARIABLE_SCRIPT_DIR, f.getCanonicalFile().getParent());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                canExecute = compile(null);
            } else {
                // There's no URI -> check if we have the test instance at least.
                // This is the case when the test script interpret gets created programatically
                // and an instance of a Java test script is passed to it.
                canExecute = testInstance != null;
            }
        } catch (Exception ex) {
            // Fire an event about start of the execution
            scriptManager.fireScriptEvent(new ScriptEvent(this, null, executionContext, ScriptEvent.SCRIPT_EXECUTION_FINISHED));
        }

        if (canExecute) {
            try {

                // Initialize the test instance with script manager and context
                testInstance.setInterpret(this);
                testInstance.setContext(executionContext);

                if (testInstance instanceof DefaultJavaTestScript) {
                    DefaultJavaTestScript jt = (DefaultJavaTestScript) testInstance;
                    jt.setDocument(document);
                    jt.setTestSource(jt.getClass().getName());
                }
                if (testInstance instanceof TestWrapper) {
                    executionContext.put(ScriptingContext.CONTEXT_CURRENT_SCRIPT_WRAPPER, testInstance);
                }

                try {
                    // Call the test() method of the Java test script
                    testInstance.test();
                } catch (StopRequestException ex) {
                    executing = false;
                    // This is OK, it means that the execution was manually stopped
                    executionContext.put(ScriptingContext.CONTEXT_STOP_REASON, ex.getMessage());
//                    if (scriptManager.isManuallyStopped()) {
                    // Fire an event indicating that the execution was stopped
                    scriptManager.fireScriptEvent(new ScriptEvent(this, null, executionContext, ScriptEvent.SCRIPT_EXECUTION_STOPPED));
//                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                // Fire an event about end of the execution
                executing = false;
                scriptManager.fireScriptEvent(new ScriptEvent(this, null, executionContext, ScriptEvent.SCRIPT_EXECUTION_FINISHED));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            executing = false;
            executionContext.remove(PFLAG);
            return executionContext.getExitCode();
        }
        executing = false;
        executionContext.remove(PFLAG);

        // If compilation failed for unknown error, throw an InterpretErrorException
        scriptManager.fireScriptEvent(new ScriptEvent(this, null, executionContext, ScriptEvent.SCRIPT_EXECUTION_FINISHED));
        throw new InterpretErrorException(this, ApplicationSupport.getString("interpret.java.cannotExecute"));
    }

    public boolean isPartialExecutionAllowed() {
        return false;
    }

    public JavaTestScript getTestInstance() {
        return testInstance;
    }

    public void setSelection(int startOffset, int endOffset) throws IllegalStateException {
        throw new IllegalStateException("Java interpret does not support partial executions.");
    }

    public void resetSelection() throws IllegalStateException {
        throw new IllegalStateException("Java interpret does not support partial executions.");
    }

    /**
     * Set the test instance. It is typically used from a program to execute an
     * already existing instance of a test script.
     *
     * @param testInstance a Java test instance.
     */
    public void setTestInstance(JavaTestScript testInstance) {
        this.testInstance = testInstance;
    }

    /**
     * @return the errorHandler
     */
    public JavaElementMapper getElementMapper() {
        return errorHandler;
    }

    /**
     * @param errorHandler the errorHandler to set
     */
    public void setElementMapper(JavaElementMapper errorHandler) {
        this.errorHandler = errorHandler;
    }

    public Element map(JavaTestScriptInterpret interpret, Element e) {
        return e;
    }

    public void setFireEventsEnabled(boolean enableEvents) {
        doNotFireEvents = !enableEvents;
    }

    public void destroy() {
        super.destroy();
        testInstance = null;
        document = null;
        compiler = null;
        errorHandler = null;
    }
}
