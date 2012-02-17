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
package com.tplan.robot.scripting.interpret;

import com.tplan.robot.plugin.PluginFactory;
import com.tplan.robot.plugin.PluginInfo;
import com.tplan.robot.util.Utils;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Pluggable test script factory.
 *
 * @product.signature
 */
public class TestScriptInterpretFactory extends PluginFactory {

    private static TestScriptInterpretFactory instance;

    // key=file extension in lower case, value=integer code converted to string
    private Map<String, String> extensionMap = new HashMap();

    // Singleton pattern
    private TestScriptInterpretFactory() {
        TestScriptInterpret t;
        for (PluginInfo pi : getAvailablePluginInfos(TestScriptInterpret.class)) {
            try {
                t = (TestScriptInterpret) pi.getPluginInstance();
                String s[] = t.getSupportedFileExtensions();
                if (s != null) {
                    for (String ext : s) {
                        extensionMap.put(ext.toLowerCase(), "" + t.getType());
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Get shared instance of this factory.
     * @return shared factory instance
     */
    public static TestScriptInterpretFactory getInstance() {
        if (instance == null) {
            instance = new TestScriptInterpretFactory();
        }
        return instance;
    }

    /**
     * Create a test script by type.
     *
     * @param type an integer type code.
     * @return a new instance of test script of the specified type. If there's
     * no test script implementation associated with this type, the method returns null.
     */
    public TestScriptInterpret createByType(int type) {
        return (TestScriptInterpret) getPluginByCode("" + type, TestScriptInterpret.class);
    }

    /**
     * Create a test script by file extension. For example, if an URI to a
     * .java or .class file is passed as argument, the method returns the Java
     * test script instance. If the argument is null, an instance of the
     * proprietary test script with unspecified URI is returned.
     *
     * @param testURI test script URI (a file or another resource).
     * @return a new instance of test script if the file extension is recognized
     * by at least one test script plugin. Otherwise the method returns a test
     * script instance of the proprietary language type.
     */
    public TestScriptInterpret createByExtension(URI testURI) {
        if (testURI != null) {
            String extension = Utils.getExtension(testURI.getPath());
            if (extension != null) {
                extension = extension.toLowerCase();
                if (extensionMap.containsKey(extension)) {
                    TestScriptInterpret ti = (TestScriptInterpret) getPluginByCode(extensionMap.get(extension), TestScriptInterpret.class);
                    return ti;
                }
            }
        }
        // Default: return the proprietary language type
        return createByType(TestScriptInterpret.TYPE_PROPRIETARY);
    }
}
