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

import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

/**
 * <p>User configuration base class. It defines basic methods of
 * manipulation with the configuration parameters. The class is abstract and
 * all successors must implement the {@link #get get()} and {@link #put put()}
 * methods.</p>
 *
 * <p>Each parameter value is stored as a string. The class provides methods
 * such as {@link #getDouble getDouble()}, {@link #setDouble setDouble()},
 * {@link #getInteger getInteger()} and {@link #setInteger setInteger()} that
 * perform conversion between number and string.</p>
 *
 * <p>The configuration is also capable of saving a set of numbers as one parameter.
 * The numbers are converted to a string which contains the numbers formatted
 * by US number format separated by semicolon. The associated methods are
 * {@link #getListOfNumbers} and
 * {@link #setListOfNumbers}. The numbers are always saved and
 * restored in the same order (the same as in the vector).</p>
 *
 * <p>If a component depends on any option value in this class, it should
 * register itself using the method
 * {@link #addConfigurationListener addConfigurationListener()} and it will be notified
 * about any changes in the configuration immediately when a set method is
 * called.</p>
 *
 * @product.signature
 */
public abstract class AbstractUserConfiguration {

    /** Vector of configuration change listeners. */
    private transient List changeListeners;
    /** Private number format. */
    private NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
    /** Separator of number values in a list of numbers. */
    protected final String valueSeparator = ";";
    /** A flag showing whether the configuration has been changed or not */
    protected boolean modified = false;

    /**
     * Constructor.
     */
    public AbstractUserConfiguration() {
    }

    /**
     * Set value of a parameter. Needs to be implemented by all descendants.
     * @param key a key (parameter name) under which the value will be stored.
     * @param value parameter value.
     * @return the stored value.
     */
    protected abstract Object put(Object key, Object value);

    /**
     * Get a parameter. Needs to be implemented by all descendants
     * @param key a key (parameter name) under which the value is stored.
     * @return the parameter value.
     */
    protected abstract Object get(Object key);

    /**
     * Remove a configuration change listener
     * @param l a configuration change listener
     */
    public synchronized void removeConfigurationListener(ConfigurationChangeListener l) {
        if (changeListeners != null && changeListeners.contains(l)) {
            List v = new ArrayList(changeListeners);
            v.remove(l);
            changeListeners = v;
        }
    }

    /**
     * Add a configuration change listener
     * @param l a configuration change listener
     */
    public synchronized void addConfigurationListener(ConfigurationChangeListener l) {
        List v = changeListeners == null ? new ArrayList(2) : new ArrayList(changeListeners);

        if (!v.contains(l)) {
            v.add(l);
            changeListeners = v;
        }
    }

    /**
     * Fire a ChangeEvent to all registered listeners when the configuration
     * gets changed. This method is to be called every time a parameter value
     * is changed.
     * @param e a change event.
     */
    public void fireConfigurationChanged(ConfigurationChangeEvent e) {
        if (changeListeners != null) {
            List listeners = changeListeners;
            int count = listeners.size();

            for (int i = 0; i < count; i++) {
                ((ConfigurationChangeListener) listeners.get(i)).configurationChanged(e);
            }
        }
    }

    /**
     * Get value of a parameter
     * @param name parameter name
     * @return value of the parameter
     */
    protected Object getParameter(String name) {
        return get(name);
    }

    /**
     * Set value of a parameter. Method fires a configuration change event to
     * all registered listeners.
     *
     * @param name parameter name
     * @param value a new value for the parameter
     */
    protected void setParameter(String name, Object value) {
        Object oldValue = getParameter(name);
        if (oldValue == null || !oldValue.equals(value)) {
            modified = true;
            put(name, value);
            fireConfigurationChanged(
                    new ConfigurationChangeEvent(this, name, oldValue, value));
        }
    }

    /**
     * Get a parameter as a Double.
     *
     * @param parameterName parameter name
     * @return parameter value as a Double, null if the parameter is not in the
     * configuration
     * @exception  NumberFormatException  if the parameter cannot be converted
     * to Double
     */
    public Double getDouble(String parameterName) {
        Object obj = getParameter(parameterName);
        return convertToDouble(obj);
    }

    /**
     * Perform conversion from an object to a Double.
     *
     * @param obj an object (a user parameter value)
     * @return a new Double, null if the parameter can't be converted.
     */
    protected Double convertToDouble(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return new Double(((Number) obj).doubleValue());
        }
        Number value = parseNumber(obj.toString());
        return value == null ? null : new Double(value.doubleValue());
    }

    /**
     * Get a parameter as an Integer.
     *
     * @param parameterName parameter name
     * @return parameter value as an Integer, null if the parameter is not
     * in the configuration
     * @exception  NumberFormatException  if the parameter cannot be converted
     * to Integer
     */
    public Integer getInteger(String parameterName) {
        Double d = getDouble(parameterName);

        if (d != null) {
            return new Integer(d.intValue());
        }
        return null;
    }

    public void setDouble(String parameterName, Number number) {
        setParameter(parameterName, formatNumber(number));
    }

    /**
     * Set the parameter as an Integer
     * @param parameterName parameter name
     * @param number a number to be saved as integer under the parameter name
     */
    public void setInteger(String parameterName, Number number) {
        setParameter(parameterName, formatNumber(number));
    }

    /**
     * Get a parameter as a Boolean. Any of the values of "1", "true" or "yes" is considered to be the value of true.
     *
     * @param parameterName parameter name
     * @return parameter value as a Boolean, null if the parameter is not in the
     * configuration
     */
    public Boolean getBoolean(String parameterName) {
        Object obj = getParameter(parameterName);
        Boolean b = null;
        if (obj != null) {
            b = new Boolean(obj.equals("1") || obj.equals("true") || obj.equals("yes"));
        }
        return b;
    }

    /**
     * Set the parameter as an Boolean
     * @param parameterName parameter name
     * @param b a boolean value to be saved
     */
    public void setBoolean(String parameterName, Boolean b) {
        setParameter(parameterName, b.toString());
    }

    /**
     * Format a number to a string.
     * @param n a Number.
     * @return the number formatted to a string, null when the argument is null.
     */
    private String formatNumber(Number n) {
        if (n != null) {
            return numberFormat.format(n);
        }
        return null;
    }

    /**
     * Parser a number from a string.
     * @param str a number in form of a string.
     * @return the parsed Number.
     */
    private Number parseNumber(String str) {
        try {
            return numberFormat.parse(str);
        } catch (Exception ex) {
            //            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Get the value of a parameter as a vector of String values. The parameter
     * value is expected to be a string or a string containing number
     * values separated by a semicolon.
     *
     * @param parameterName parameter name.
     * @return a Vector which contains String instances.
     */
    public List<String> getListOfStrings(String parameterName) {
        Object obj = getParameter(parameterName);
        if (obj == null) {
            return null;
        }

        List v = new ArrayList();

        if (obj.toString().trim().equals("")) {
            return v;
        }
        StringTokenizer tokenizer =
                new StringTokenizer(obj.toString(), valueSeparator);

        while (tokenizer.hasMoreTokens()) {
            v.add(tokenizer.nextToken().toString());
        }

        return v;
    }

    /**
     * Get the value of a parameter as a vector of double values. The parameter
     * value is expected to be a single number or a string containing number
     * values separated by a semicolon.
     *
     * @param parameterName parameter name.
     * @return a Vector which contains Number instances.
     */
    public List<? extends Number> getListOfNumbers(String parameterName) {
        Object obj = getParameter(parameterName);
        return convertToListOfNumbers(obj);
    }

    /**
     * Get the value of a parameter as an array of integers. The parameter
     * value is expected to be a single number or a string containing number
     * values separated by a semicolon.
     *
     * @param parameterName parameter name.
     * @return an array of integers.
     */
    public int[] getArrayOfInts(String parameterName) {
        Object obj = getParameter(parameterName);
        List<? extends Number> l = convertToListOfNumbers(obj);
        if (l == null) {
            return null;
        }
        int b[] = new int[l.size()];
        int i = 0;
        for (Number n : l) {
            b[i++] = n.intValue();
        }
        return b;
    }

    /**
     * Get the value of a parameter as an array of integers. The parameter
     * value is expected to be a single number or a string containing number
     * values separated by a semicolon.
     *
     * @param parameterName parameter name.
     * @return an array of integers.
     */
    public byte[] getArrayOfBytes(String parameterName) {
        Object obj = getParameter(parameterName);
        List<? extends Number> l = convertToListOfNumbers(obj);
        if (l == null) {
            return null;
        }
        byte b[] = new byte[l.size()];
        int i = 0;
        for (Number n : l) {
            b[i++] = n.byteValue();
        }
        return b;
    }

    /**
     * Perform conversion from an object to a list of numbers. The argument
     * is expected to be a single number or a string containing number
     * values separated by a semicolon.
     *
     * @param obj an object (a user parameter value)
     * @return a new vector of numbers
     */
    public List<? extends Number> convertToListOfNumbers(Object obj) {
        if (obj == null) {
            return null;
        }

        List<Number> v = Collections.synchronizedList(new ArrayList());

        if (obj.toString().trim().equals("")) {
            return v;
        }
        StringTokenizer tokenizer =
                new StringTokenizer(obj.toString(), valueSeparator);
        Number n;

        while (tokenizer.hasMoreTokens()) {
            n = parseNumber(tokenizer.nextToken());
            if (n != null) {
                v.add(n);
            }
        }

        return v;
    }

    /**
     * Set a parameter as a vector of double values. The vector will be
     * converted into a string containing number values separated by a semicolon.
     *
     * @param parameterName parameter name.
     * @param values a Vector which contains Number instances.
     */
    public void setListOfNumbers(String parameterName, List<? extends Number> values) {
        setListOfObjects(parameterName, values);
    }

    /**
     * Set a parameter as a vector of objects. The vector will be
     * converted into a string containing values separated by a semicolon.
     *
     * @param parameterName parameter name.
     * @param values a Vector.
     */
    public void setListOfObjects(String parameterName, List values) {
        if (values == null) {
            setParameter(parameterName, "");
        } else {
            String str = "";
            int size = values.size();

            for (int i = 0; i < size; i++) {
                Object obj = values.get(i);

                if (obj instanceof Number) {
                    obj = formatNumber((Number) obj);
                }
                str += obj == null ? "" : obj.toString();
                str = (i != size - 1) ? str + valueSeparator : str;
            }
            setParameter(parameterName, str);
        }
    }

    /**
     * Get a parameter as a String.
     * @param parameterName parameter name.
     * @return the value of the requested parameter.
     */
    public String getString(String parameterName) {
        Object obj = getParameter(parameterName);

        if (obj != null) {
            return obj.toString();
        }
        return null;
    }

    /**
     * Set a String parameter.
     * @param parameterName parameter name.
     * @param value a String value to be saved under the parameter name.
     */
    public void setString(String parameterName, String value) {
        setParameter(parameterName, value);
    }

    /**
     * Get value of the modification flag.
     * @return true if anything in the configuration has changed, false if not
     */
    public boolean isModified() {
        return modified;
    }
}
