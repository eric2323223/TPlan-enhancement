/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.tools.JavaFileObject;

/**
 * Customized class loader able to load classes from byte code stored in memory.
 * @product.signature
 */
public class MemoryClassLoader extends ClassLoader {

    // Map of Java class names (fully qualified) and their corresponding Java file object instances
    private final Map<String, JavaFileObject> classMap = new HashMap<String, JavaFileObject>();
    private String basePath = null;

    /**
     * Get the map of classes which this class loader instantiates.
     *
     * @return map of instantiable classes. For security reasons the method
     * returns just a copy of the loader's map.
     */
    public Map<String, JavaFileObject> getClassMap() {
        Map<String, JavaFileObject> map = new HashMap(classMap);
        return map;
    }

    @Override
    protected Class<?> findClass(final String qualifiedClassName)
            throws ClassNotFoundException {
        JavaFileObject file = classMap.get(qualifiedClassName);
        if (file != null) {
            byte[] bytes = ((MemoryJavaSourceObject) file).getCompiledCode().toByteArray();
            return defineClass(qualifiedClassName, bytes, 0, bytes.length);
        }
        try {
            return Class.forName(qualifiedClassName);
        } catch (ClassNotFoundException nf) {
            return super.findClass(qualifiedClassName);
        }
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        if (name.endsWith(".class")) {
            String qualifiedClassName = name.substring(0, name.length() - ".class".length()).replace('/', '.');
            MemoryJavaSourceObject file = (MemoryJavaSourceObject) classMap.get(qualifiedClassName);
            if (file != null) {
                return new ByteArrayInputStream(file.getCompiledCode().toByteArray());
            }
        } else if (basePath != null) {
            File f = new File(basePath, name);
//            System.out.println("Looking for file "+f.getAbsolutePath());
            if (f.exists() && f.canRead()) {
                try {
                    return new FileInputStream(f);
                } catch (FileNotFoundException ex) {
                }
            }
        }
        return super.getResourceAsStream(name);
    }

    /**
     * Add a class mapping to the class map.
     * @param qualifiedClassName qualified (full) class name.
     * @param javaFile a Java file object.
     */
    void addToClassMap(String qualifiedClassName, JavaFileObject javaFile) {
        classMap.put(qualifiedClassName, javaFile);
    }

    /**
     * @return the basePath
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * @param basePath the basePath to set
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}

