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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

/**
 * Java file manager able to handle Java classes stored in memory.
 * @product.signature
 */
public class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    private MemoryClassLoader classLoader;
    private final Map<URI, JavaFileObject> fileObjects = new HashMap<URI, JavaFileObject>();

    public MemoryFileManager(JavaFileManager fileManager, MemoryClassLoader classLoader) {
        super(fileManager);
        this.classLoader = classLoader;
    }

    /**
     * Set class loader for this file manager.
     * @param cl a class loader.
     */
    public void setClassLoader(MemoryClassLoader cl) {
        this.classLoader = cl;
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName,
            String relativeName) throws IOException {
        FileObject o = fileObjects.get(toUri(location, packageName, relativeName));
        return o != null ? o : super.getFileForInput(location, packageName, relativeName);
    }

    /**
     * Add an input file. Called by the compiler to add the compiled code to the
     * list of available file objects.
     * @param location a location.
     * @param packageName package name.
     * @param relativeName relative file name.
     * @param file a Java file.
     */
    public void addInputFile(StandardLocation location, String packageName,
            String relativeName, JavaFileObject file) {
        fileObjects.put(toUri(location, packageName, relativeName), file);
    }

    private URI toUri(Location location, String packageName, String relativeName) {
        return URI.create(location.getName() + '/' + packageName + '/' + relativeName);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String qualifiedName,
            Kind kind, FileObject outputFile) throws IOException {
        JavaFileObject file = new MemoryJavaSourceObject(qualifiedName, kind);
        classLoader.addToClassMap(qualifiedName, file);
        return file;
    }

    /**
     * Overriden method to return our custom class loader.
     * @param location location (always ignored by this method).
     * @return class loader.
     */
    @Override
    public ClassLoader getClassLoader(JavaFileManager.Location location) {
        return classLoader;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject container) {
        return (container instanceof MemoryJavaSourceObject) ? container.getName() : super.inferBinaryName(location, container);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName,
            Set<Kind> kinds, boolean recurse) throws IOException {
        ArrayList<JavaFileObject> files = new ArrayList<JavaFileObject>();
        if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {
            for (JavaFileObject file : fileObjects.values()) {
                if (file.getKind() == Kind.CLASS && file.getName().startsWith(packageName)) {
                    files.add(file);
                }
            }
            files.addAll(classLoader.getClassMap().values());
        } else if (location == StandardLocation.SOURCE_PATH && kinds.contains(JavaFileObject.Kind.SOURCE)) {
            for (JavaFileObject file : fileObjects.values()) {
                if (file.getKind() == Kind.SOURCE && file.getName().startsWith(packageName)) {
                    files.add(file);
                }
            }
        }
        Iterable<JavaFileObject> parentFiles = super.list(location, packageName, kinds, recurse);
        for (JavaFileObject file : parentFiles) {
            files.add(file);
        }
        return files;
    }
}