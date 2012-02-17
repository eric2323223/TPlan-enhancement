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

import com.tplan.robot.plugin.JavaLibrary;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject.Kind;

/**
 * Java code compiler for on-the-fly compilation of Java classes.
 * @product.signature
 */
public class JavaCodeCompiler<T> {

    // Custom class loader to load classes from the compiled byte code
    private MemoryClassLoader classLoader;

    // Internal Java compiler
    private final JavaCompiler compiler;

    // Custom memory file manager
    private final MemoryFileManager fileManager;

    /**
     * Default constructor.
     *
     * @throws java.lang.IllegalStateException when the compiler is not available,
     * which typically happens when user runs JRE instead of JDK.
     */
    public JavaCodeCompiler() throws IllegalStateException {
        if ((compiler = ToolProvider.getSystemJavaCompiler()) == null) {
            throw new IllegalStateException();
        }
        classLoader = new MemoryClassLoader();
        fileManager = new MemoryFileManager(compiler.getStandardFileManager(null, null, null), classLoader);
    }

    public synchronized Class<T> compile(String fullName, String basePath, JavaFileObject source, DiagnosticCollector<JavaFileObject> diagnostics) throws IOException, ClassNotFoundException {

        // It is essential to create a new class loader for each compilation.
        // JVM otherwise doesn't reload the byte code of the class which has
        // been already compiled and loaded previously.
        classLoader = new MemoryClassLoader();
        classLoader.setBasePath(basePath);
        fileManager.setClassLoader(classLoader);

        if (fullName.endsWith(".java")) {
            fullName = fullName.substring(0, fullName.length() - ".java".length());
        }
        String pkg, className;
        if (fullName.contains(".")) {
            pkg = fullName.substring(0, fullName.lastIndexOf("."));
            className = fullName.substring(fullName.lastIndexOf(".") + 1);
        } else {
            pkg = "";
            className = fullName;
        }

        // Save the Java source object to the file loader
        fileManager.addInputFile(StandardLocation.SOURCE_PATH, pkg, className + Kind.SOURCE.extension, source);

        // Create a list of sources
        List<JavaFileObject> l = new ArrayList();
        l.add(source);

        // Create a list of compiler options.
        List<String> options = null;
        String path = System.getProperty(JavaLibrary.PLUGIN_LIBRARY_PATH_KEY);
        if (path != null && !path.isEmpty()) {
            options = new ArrayList();
            options.add("-classpath");
            options.add(System.getProperty("java.class.path", ".") + File.pathSeparator + path);
        }
//        options.add("-classpath");
//        options.add(System.getProperty("java.class.path", "."));
//        System.out.println("class path: "+System.getProperty("java.class.path", "."));

        // Obtain a comilation task from the compiler and execute it
        Boolean result = compiler.getTask(null, fileManager, diagnostics, options, null, l).call();

        // If compilation failed -> return null
        if (result == null || !result) {
            return null;
        }

        // Use the custom class loader to define the compiled class
        return (Class<T>) classLoader.loadClass(fullName);
    }

    /**
     * Parse package name from the source code. The method is quite stupid and
     * expects the package statement to be in one single line. TODO: improve
     *
     * @param sourceCode Java source code.
     * @return package string. If there's no package the method returns empty string "".
     */
    public String[] parsePackageAndClassName(String sourceCode) {
        String data[] = new String[2];
        data[0] = "";

        // Remove comments from the code
        sourceCode = sourceCode.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
        BufferedReader reader = new BufferedReader(new StringReader(sourceCode));
        String line;
        int index;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains("//")) {  // remove all // comments
                    line = line.substring(0, line.indexOf("//"));
                }
                if (line.startsWith("package")) {
                    data[0] = line.substring("package".length(), line.indexOf(";")).trim();
                } else {
                    index = line.indexOf("class ");
                    if (index >= 0 && (index == 0 || Character.isWhitespace(line.charAt(index - 1)))) {
                        line = line.substring(index + "class ".length()).trim();
                        index = 0;
                        char c;
                        StringBuilder b = new StringBuilder();
                        do {
                            c = line.charAt(index++);
                            if (Character.isLetter(c) || Character.isDigit(c)) {
                                b.append(c);
                            } else {
                                break;
                            }
                        } while (true);
                        data[1] = b.toString();
                        return data;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();  // Should not happen as we are reading from a string
        }
        return data;
    }
}



