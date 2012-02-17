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
package com.tplan.robot.l10n;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Customized ResourceBundle class able to load properties from multiple
 * resources. It supports the localization mechanism provided by the project.
 * Behavior of this class is very similar to <code>java.util.PropertyResourceBundle</code>
 * save that it is possible to load the messages repeatedly through the <code>load()</code>
 * methods. This way we can load the default resource bundle first and then overload
 * it by any customized resource bundle so that the messages missing in the
 * customized one will be provided from the default one and the application
 * doesn't crash when some strings are missing (this is default behavior of
 * <code>java.util.ResourceBundle</code>).
 */
public class CustomPropertyResourceBundle extends ResourceBundle {

    private Hashtable lookup = new Hashtable();

    /**
     * Creates a new custom property resource bundle.
     */
    public CustomPropertyResourceBundle() {
    }

    /**
     * Load messages from an input stream.
     * @param stream an input stream.
     */
    public void load(InputStream stream) throws IOException {
        Properties properties = new Properties();
        properties.load(stream);
        lookup.putAll(properties);
    }

    /**
     * Load messages from another resource bundle.
     * @param r another resource bundle.
     */
    public void load(ResourceBundle r) {
        Enumeration e = r.getKeys();
        Object o;
        while (e.hasMoreElements()) {
            o = e.nextElement();
            lookup.put(o, r.getObject(o.toString()));
        }
    }

    public boolean contains(String key) {
        return lookup.containsKey(key);
    }

    /**
     * Implements java.util.ResourceBundle.handleGetObject.
     */
    public Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        Object o = lookup.get(key);
        if (o == null) {
//            (new Exception()).printStackTrace();
            return "[NOT FOUND: " + key + "]";
        }
        return o;
    }

    /**
     * Implementation of ResourceBundle.getKeys.
     */
    public Enumeration getKeys() {
        return lookup.keys();
    }

    public void put(String key, String message) {
        lookup.put(key, message);
    }
}
