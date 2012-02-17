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
package com.tplan.robot.preferences;

import com.tplan.robot.ApplicationSupport;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.List;
import java.util.Map;

/**
 * <p>A container for user preferences. Defines variables which affect the
 * behavior of {@product.name} and can be set by user.
 * <p/>
 * <p>Apart from get- and set- methods inherited from the base class, the
 * configuration allows to save a Color instance or even an array of colors.
 * Each color is converted to a vector of numbers where there are three integer
 * numbers representing Red, Green and Blue portions as provided by the Color's
 * RGB model. The vector of numbers is then saved using the {@link
 * com.tplan.robot.preferences.AbstractUserConfiguration#setListOfNumbers setListOfNumbers()}
 * method which converts the vector of numbers into a String.
 * <p/>
 * <p>If a component depends on any option value in this class, it should
 * register itself using the method {@link #addConfigurationListener
 * addConfigurationListener()} and it will be notified about any changes in
 * the configuration.</p>
 * @product.signature
 */
public class UserConfiguration extends AbstractUserConfiguration {

    /**
     * Instance of this class.
     */
    private static UserConfiguration instance = null;
    /**
     * A Map which will store user configuration in the memory.
     */
    protected Map configuration;
    /**
     * A Map which will store default user configuration in the memory.
     */
    protected Map defaults;
    protected Map overrideTable;

    /**
     * Constructor. It is protected as the classes outside this package are
     * supposed to share one instance of this configuration class.
     */
    protected UserConfiguration() {
        configuration = new HashMap();
        defaults = new HashMap();
    }

    /**
     * Get a shared instance of this configuration.
     *
     * @return a UserConfiguration instance.
     */
    public static UserConfiguration getInstance() {
        if (instance == null) {
            instance = new UserConfiguration();
        }
        return instance;
    }

    /**
     * Get a shared instance of this configuration.
     *
     * @return a UserConfiguration instance.
     */
    public static UserConfiguration getCopy() {
        UserConfiguration cfg = new UserConfiguration();
        cfg.configuration.putAll(getInstance().configuration);
        cfg.defaults.putAll(getInstance().defaults);
        return cfg;
    }

    /**
     * Load the configuration from an input stream.
     *
     * @param inStream an input stream to load from.
     * @param config   a Map to store the input stream values to.
     * @throws IOException when in I/O error occurs, e.g. cannot read from file etc.
     */
    private static void load(InputStream inStream, Map config) throws IOException {
        Properties props = new Properties();
        props.load(inStream);
        config.putAll(props);
    }

    /**
     * Load the configuration from an input stream.
     *
     * @param inStream an input stream to load from.
     * @throws IOException when in I/O error occurs, e.g. cannot read from file etc.
     */
    public static void load(InputStream inStream) throws IOException {
        load(inStream, getInstance().configuration);
    }

    /**
     * Load the default configuration from an input stream.
     *
     * @param inStream an input stream to load from.
     * @throws IOException when in I/O error occurs, e.g. cannot read from file etc.
     */
    public static void loadDefaults(InputStream inStream) throws IOException {
        load(inStream, getInstance().defaults);
        getInstance().configuration.putAll(getInstance().defaults);
    }

    /**
     * Save the configuration into an output stream.
     *
     * @param outStream an output stream to save to.
     * @throws IOException when in I/O error occurs, e.g. cannot write to a file etc.
     */
    public static void save(OutputStream outStream) throws IOException {
        synchronized (getInstance()) {
            Properties props = new Properties();
            props.putAll(getInstance().configuration);
            props.store(outStream, "");
        }
    }

    public static void saveConfiguration() {
        if (UserConfiguration.getInstance().isModified()) {
            try {
                String conf = ApplicationSupport.APPLICATION_CONFIG_FILE;
                UserConfiguration.save(new FileOutputStream(conf));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Set a parameter.
     *
     * @param key   the parameter name.
     * @param value the value.
     * @return the object
     */
    protected Object put(Object key, Object value) {
        return configuration.put(key, value);
    }

    /**
     * Get a parameter.
     *
     * @param key key to the parameter (parameter name)
     * @return parameter value, null if not found
     */
    protected Object get(Object key) {
        if (key != null) {
            if (overrideTable != null && overrideTable.containsKey(key)) {
                return overrideTable.get(key);
            }
            return configuration.get(key);
        }
        return null;
    }

    /**
     * Get default value for the given parameter
     *
     * @param key parameter name
     * @return default value or null when there's none
     */
    public String getDefaultValue(String key) {
        Object value = null;
        if (defaults != null) {
            value = defaults.get(key);
        }
        return value == null ? null : value.toString();
    }

    /**
     * Get default value of a parameter as an Integer.
     *
     * @param key parameter name
     * @return default value or null when there's none
     */
    public Double getDefaultValueAsDouble(String key) {
        return convertToDouble(getDefaultValue(key));
    }

    /**
     * Get default value of a parameter as a vector of numbers.
     *
     * @param key parameter name
     * @return default value or null when there's none
     */
    public List getDefaultValueAsListOfNumbers(String key) {
        return convertToListOfNumbers(getDefaultValue(key));
    }

    /**
     * Get default value of a parameter as an array of Color instances.
     *
     * @param key parameter name
     * @return default value or null when there's none
     */
    public Color[] getDefaultValueAsArrayOfColors(String key) {
        return convertToColorArray(convertToListOfNumbers(getDefaultValue(key)));
    }

    /**
     * Get default value of a parameter as a Color instance.
     *
     * @param key parameter name
     * @return default value or null when there's none
     */
    public Color getDefaultValueAsColor(String key) {
        Color[] c = convertToColorArray(convertToListOfNumbers(getDefaultValue(key)));
        if (c != null && c.length > 0) {
            return c[0];
        }
        return null;
    }

    /**
     * Create a new vector and fill it with integers from 0 to argument value.
     *
     * @param maxValue maximum integer value to be added.
     * @return a new vector containing integers from 0 to argument value.
     */
    protected List prefillList(int maxValue) {
        List v = new ArrayList(maxValue + 2);

        for (int i = 0; i <= maxValue; i++) {
            v.add(new Integer(i));
        }
        return v;
    }

    /**
     * Get a parameter value and try to convert it to an array of colors.
     * See {@link #setColors setColors()} method for more information.
     *
     * @param parameterName name of the parameter.
     * @return an array with Color instances.
     */
    public Color[] getColors(String parameterName) {
        List v = getListOfNumbers(parameterName);
        return convertToColorArray(v);
    }

    /**
     * Perform conversion from a vector of numbers to an array of Color
     * instances. See {@link #setColors setColors()} method for more
     * information.
     *
     * @param v a vector of numbers.
     * @return a new Color array, null if the argument is null.
     */
    protected Color[] convertToColorArray(List v) {
        if (v != null) {
            Color[] col = new Color[(int) (v.size() / 3)];

            for (int i = 0; i < v.size(); i++) {
                col[i] = new Color(((Number) v.get(0)).intValue(),
                        ((Number) v.get(1)).intValue(),
                        ((Number) v.get(2)).intValue());
                v.remove(2);
                v.remove(1);
                v.remove(0);
            }
            return col;
        }
        return null;
    }

    /**
     * Get a parameter value and try to convert it to a Color instance.
     * See {@link #setColors setColors()} method for more information.
     *
     * @param parameterName name of the parameter.
     * @return the parameter value as a Color instance, null if conversion fails.
     */
    public Color getColor(String parameterName) {
        List v = getListOfNumbers(parameterName);

        if (v != null && v.size() >= 3) {
            try {
                return new Color(((Number) v.get(0)).intValue(),
                        ((Number) v.get(1)).intValue(),
                        ((Number) v.get(2)).intValue());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Save an array of colors to the configuration.
     * Each color is converted to a vector of numbers where there are three integer
     * numbers representing Red, Green and Blue portions as defined by the Color's
     * RGB model. The vector of numbers is then saved using the
     * {@link com.tplan.robot.preferences.AbstractUserConfiguration#setListOfNumbers setListOfNumbers()}
     * method which converts the vector of numbers into a String.
     *
     * @param parameterName parameter name.
     * @param colors        an array of Color instances.
     */
    public void setColors(String parameterName, Color[] colors) {
        List v = new ArrayList();

        for (int i = 0; i < colors.length; i++) {
            Color c = colors[i];

            if (c != null) {
                v.add(new Integer(c.getRed()));
                v.add(new Integer(c.getGreen()));
                v.add(new Integer(c.getBlue()));
            }
        }
        setListOfNumbers(parameterName, v);
    }

    /**
     * This method will try to load a vector of strings using the given key.
     * If the vector already contains the new item, it gets moved to the first
     * position. Otherwise it is inserted into the first position.
     * <p/>
     * If you need to keep the vector up to a certain size long, you may use
     * the third argument to specify max vector size. A negative number disables
     * this feature.
     * <p/>
     * <p>This method is used by dynamic menus that list
     * e.g. recently open documents. Whenever a document is opened, it needs
     * to be placed into the first position in the list. Such a list is usually
     * of limited size, it e.g. lists just five most recently opened documents.
     *
     * @param newItem   a new item. If it already exists in the vector, it will
     *                  be just moved.
     * @param configKey a key identifying the configuration vector.
     * @param sizeLimit maximum size of the vector. If the vector exceeds this size,
     *                  it wil be trimmed to the required size. Any negative number disables
     *                  this feature.
     */
    public void updateListOfRecents(String newItem, String configKey, int sizeLimit, boolean caseSensitive) {
        List v = UserConfiguration.getInstance().getListOfStrings(configKey);
        if (caseSensitive) {
            if (v.contains(newItem)) {
                v.remove(newItem);
            }
        } else {
            for (Object o : v.toArray()) {
                if (o.toString().equalsIgnoreCase(newItem)) {
                    v.remove(o);
                    // We intentionally do not break here to fix the previously saved lists
                }
            }
        }
        v.add(0, newItem);
        if (sizeLimit > 0 && v.size() > sizeLimit) {
            v = new ArrayList(v.subList(0, sizeLimit));
        }
        setListOfObjects(configKey, v);
    }

    public void updateListOfRecents(String newItem, String configKey, int sizeLimit) {
        updateListOfRecents(newItem, configKey, sizeLimit, true);
    }

    public Map getOverrideTable() {
        return overrideTable;
    }

    public void setOverrideTable(Map overrideTable) {
        this.overrideTable = overrideTable;
    }

    public void putRawValues(Map values) {
        this.configuration.putAll(values);
        modified = true;
    }

    public void remove(String configKey) {
        configuration.remove(configKey);
    }
}
