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
import com.tplan.robot.gui.components.CustomHyperlinkListener;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.TokenParserImpl;
import java.awt.Color;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

/**
 * The <code>Preference</code> class is a wrapper around a single configuration parameter. It
 * defines metadata needed to display and edit the parameter value in the GUI. This
 * metadata includes short name of the preference (called "label"), description,
 * value type (string, boolean, integer, float, color, enumerated type or a keystroke),
 * parameter key in the configuration file or a parameter set.
 *
 * @product.signature
 */
public class Preference implements ConfigurationKeys {

    public static final int TYPE_STRING = 0;
    public static final int TYPE_INT = 1;
    public static final int TYPE_FLOAT = 2;
    public static final int TYPE_BOOLEAN = 3;
    public static final int TYPE_COLOR = 4;
    public static final int TYPE_KEYSTROKE = 5;
    /**
     * List of semicolon separated string values.
     */
    public static final int TYPE_STRINGLIST = 6;
    /**
     * Integer value which should be represented by a checkbox.
     * True represents value&lt;0, false is value&gt;=0. This option is intended
     * to serve as the "Don't show this message againg" flags in the app warning messages.
     */
    public static final int TYPE_INT_DISPLAYED_AS_BOOLEAN = 7;
    public static final int TYPE_FILE = 8;
    public static final int TYPE_DIRECTORY = 9;
    /**
     * Password type - a String whose value should be masked when displayed in the GUI.
     */
    public static final int TYPE_PASSWORD = 10;
    /**
     * List of files.
     */
    public static final int TYPE_FILELIST = 11;
    /**
     * Multiline text, usually represented in the GUI by a text editor.
     */
    public static final int TYPE_TEXT = 12;
    /**
     * Multiline text, usually represented in the GUI by a text editor.
     */
    public static final int TYPE_COLOR_FROM_IMAGE = 13;
    /**
     * Dummy preference which represents no value. It may be used to display
     * descriptions in GUI where a preference description is usually displayed.
     */
    public static final int TYPE_DUMMY = 99;
    private int type;
    private List displayValues;
    private List values;
    private Map displayValuesTable;
    private String label;
    private String description;
    private String configurationKey;
    private boolean selectOnly = true;
    private int maxValue = Integer.MAX_VALUE;
    private int minValue = Integer.MIN_VALUE;
    private String preferredContainerName = null;
    private String dependentOption = null;
    private String resourceKey;
    private String descriptionKey;
    private String acceptedFileExtensions[];
    private String acceptedFileExtensionsDescKey;
    private boolean acceptEmptyValue = true;
    private int textRowsToDisplay = -1;
    private Object defaultValue = null;
    private boolean reportFileAsURI = false;
    private boolean mandatory = false;
    private static TokenParser parser = new TokenParserImpl();
    private JComponent fileAccessory;
    private ColorChooserImageProvider imageProvider;
    private String fileToSearchFor, defaultSearchPath;
    private boolean useRegularExpressions, ignoreCase, forceValueToFound;
    private String descriptionOfAvailableValues;
    private String buttonGroup;

    /**
     * Default parameterless constructor for TYPE_STRING preferences.
     */
    public Preference() {
    }

    /**
     * Constructor for numeric types (TYPE_INT, TYPE_FLOAT).
     *
     * @param configurationKey preference/parameter name.
     * @param label label (short description). In GUI representation of this
     * preference the label is displayed next to the editable component. If the
     * preference is a boolean type (TYPE_BOOLEAN or TYPE_INT_DISPLAYED_AS_BOOLEAN),
     * the label will serve as the check box text.
     * @param description optional preference description (may be null).
     * In GUI representation of this preference it is typically displayed in a text pane above the editable component.
     * The description may be plain text or HTML. In the latter case and it may also
     * contain enhanced application links supported by the {@link CustomHyperlinkListener} class.
     * @param maxValue maximum acceptable value. Use Integer.MAX_VALUE to indicate that
     * the limit should not be set.
     * @param minValue minimum acceptable value. Use Integer.MIN_VALUE to indicate that
     * the limit should not be set.
     */
    public Preference(String configurationKey, String label, String description, int maxValue, int minValue) {
        this(configurationKey, TYPE_INT, label, description);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Generic constructor for any preference type.
     *
     * @param configurationKey preference/parameter name.
     * @param type preference value type - one of the numeric codes defined by
     * the <code>TYPE_</code> prefixed constants defined in this class
     * @param label label (short description). In GUI representation of this
     * preference the label is displayed next to the editable component. If the
     * preference is a boolean type (TYPE_BOOLEAN or TYPE_INT_DISPLAYED_AS_BOOLEAN),
     * the label will serve as the check box text.
     * @param description optional preference description (may be null).
     * In GUI representation of this preference it is typically displayed in a text pane above the editable component.
     * The description may be plain text or HTML. In the latter case and it may also
     * contain enhanced application links supported by the {@link CustomHyperlinkListener} class.
     */
    public Preference(String configurationKey, int type, String label, String description) {
        this.configurationKey = configurationKey;
        this.type = type;
        this.label = label;
        this.description = description;
    }

    /**
     * <p>Constructor allowing to pass resource bundle keys for the preference
     * name and description rather than plain text. It should be used for
     * preferences created by objects which may experience change of the
     * underlying resource bundle, for example objects which exist at the time
     * when user may select to change the display language.</p>
     *
     * <p>A good example are desktop clients which should use exclusively this
     * constructor. As their preferences are displayed as logon parameters in
     * the Login Dialog which allows to change the language, they must allow
     * the dialog to reload the texts from the new resource bundle.</p>
     *
     * @param type preference value type - one of the numeric codes defined by
     * the <code>TYPE_</code> prefixed constants defined in this class
     * @param configurationKey preference/parameter name.
     * @param resourceKey message key of the preference name (label).
     * @param descriptionKey message key of the preference description (may be null).
     */
    public Preference(int type, String configurationKey, String resourceKey, String descriptionKey) {
        this.configurationKey = configurationKey;
        this.descriptionKey = descriptionKey;
        this.type = type;
        this.resourceKey = resourceKey;
    }

    /**
     * Get the preference type (numeric code).
     * @return See the <code>TYPE_</code> prefixed constants
     * defined in this class for possible return values.
     */
    public int getType() {
        return type;
    }

    /**
     * Get the fixed set of values displayable in the GUI. See {@link #setDisplayValues(java.util.List)}.
     * @return fixed set of values displayable in the GUI.
     */
    public List getDisplayValues() {
        return displayValues;
    }

    /**
     * <p>This method allows together with {@link #setValues(java.util.List)}
     * to define enumerated values for parameter of the TYPE_STRING type. The GUI then typically doesn't allow to edit
     * the value in a generic editor and rather displays a fixed list (JComboBox)
     * allowing to select only one of the predefined values.</p>
     *
     * <p>The absolute minimum to define a value set is to call this
     * <code>setDisplayValues()</code> method with a list of acceptable values.
     * This is fine where we want to display the values as they are.
     * Though the list is a generic one and may contain any objects, the values
     * may be internally converted to String instances and the object consuming
     * the parameters should handle the stored parameter value with regard to this.</p>
     *
     * <p>The {@link #setValues(java.util.List)} method is optional and allows
     * to define raw values to be assigned internally to the displayed descriptions.
     * As the values are matched to descriptions based on the index, the lists
     * must naturally have the same length. If the value list is defined, the component will
     * populate the parameter with the raw value rather than the
     * one displayed in the GUI.</p>
     *
     * @param displayValues a list of enumerated values good for displaying in the GUI.
     */
    public void setDisplayValues(List displayValues) {
        this.displayValues = displayValues;
    }

    /**
     * Get the list of raw values to be mapped to descriptions returned by
     * {@link #getDisplayValues()}. Applies to enumerated string parameters only (TYPE_STRING).
     * @return list of raw parameter values.
     */
    public List getValues() {
        if (values == null) {
            return displayValues;
        }
        return values;
    }

    /**
     * Set the list of raw values to be mapped to descriptions defined through
     * {@link #setDisplayValues(java.util.List)}. Applies to enumerated string parameters only (TYPE_STRING).
     * @param values list of raw parameter values.
     */
    public void setValues(List values) {
        this.values = values;
    }

    /**
     * Get the map of descriptions and their values. See {@link #setDisplayValuesTable(java.util.Map)}.
     * Applies to enumerated string list parameters only (TYPE_STRINGLIST).
     * @return
     */
    public Map getDisplayValuesTable() {
        return displayValuesTable;
    }

    /**
     * <p>This method allows to define enumerated values for a TYPE_STRINGLIST parameter.
     * The GUI then typically doesn't allow to edit the value in a generic editor
     * and rather displays a fixed list (JList) allowing to select only one or more of
     * the predefined values in a particular order.</p>
     *
     * <p>The principle is very similar to the one described in
     * {@link #setDisplayValues(java.util.List)}. As TYPE_STRINGLIST however
     * allows to change order of the displayed values, the mapping must be realized
     * through a Map instead of a pair of List instances. The map should contain
     * the [value, description] pairs where descriptions are displayed in the GUI
     * and the parameter is then populated with the corresponding raw value. </p>
     *
     * @param displayValuesTable map of descriptions and values where values
     * are the keys and descriptions are the map values. If you want to
     * make the GUI display and store the same value set, create a map where
     * description and value are the same for each acceptable value.
     */
    public void setDisplayValuesTable(Map displayValuesTable) {
        this.displayValuesTable = displayValuesTable;
    }

    /**
     * Get the preference/parameter label (short description). In GUI representation the
     * label is displayed next to the editable component. If the
     * preference is a boolean type (TYPE_BOOLEAN or TYPE_INT_DISPLAYED_AS_BOOLEAN),
     * the label serves as the check box text.
     * @return preference/parameter label.
     */
    public String getLabel() {
        if (resourceKey != null) {
            return ApplicationSupport.getString(resourceKey);
        }
        return label;
    }

    /**
     * Get the preference/parameter description. It is optional and may be null.
     * In GUI representation of this preference it is typically displayed in a text pane above the editable component.
     * The description may be plain text or HTML. In the latter case and it may also
     * contain enhanced application links supported by the {@link CustomHyperlinkListener} class.
     * @return preference/parameter description.
     */
    public String getDescription() {
        if (descriptionKey != null) {
            return ApplicationSupport.getString(descriptionKey);
        }
        return description;
    }

    /**
     * Get the preference/parameter name (configuration key).
     * @return preference/parameter name (configuration key).
     */
    public String getConfigurationKey() {
        return configurationKey;
    }

    /**
     * Find out if the preference/parameter is a number type or not.
     * @return true if the preference is either TYPE_INT or TYPE_FLOAT, false otherwise.
     */
    public boolean isNumber() {
        return (type == TYPE_FLOAT) || (type == TYPE_INT);
    }

    /**
     * Indicate whether the values of this parameter are enumerated. See {@link #setSelectOnly(boolean)}.
     * @return true if the values are enumerated, false if not.
     */
    public boolean isSelectOnly() {
        return selectOnly;
    }

    /**
     * Indicate whether the values of this parameter are enumerated (select-only, one-of).
     * If the flag is set to true, the GUI typically doesn't allow to edit
     * the value in a generic editor and rather displays a fixed list (JComboBox, JList etc.)
     * allowing to select one of the predefined values. See the {@link #setDisplayValues(java.util.List)}
     * method for description of how to define a set of enumerated values.
     * @param selectOnly true if the parameter values are enumerated, false if not.
     */
    public void setSelectOnly(boolean selectOnly) {
        this.selectOnly = selectOnly;
    }

    /**
     * Get the maximum value. Applies just to numeric types such as TYPE_INT or
     * TYPE_FLOAT.
     * @return maximum numeric value of the parameter. Defaults to Integer.MAX_VALUE.
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * Set the maximum value. Applies just to numeric types such as TYPE_INT or
     * TYPE_FLOAT.
     * @param maxValue maximum numeric value of the parameter or Integer.MAX_VALUE
     * to unset this limit.
     */
    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Get the minimum value. Applies just to numeric types such as TYPE_INT or
     * TYPE_FLOAT.
     * @return minimum numeric value of the parameter. Defaults to Integer.MIN_VALUE.
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * Set the minimum value. Applies just to numeric types such as TYPE_INT or
     * TYPE_FLOAT.
     * @param minValue maximum numeric value of the parameter or Integer.MIN_VALUE
     * to unset this limit.
     */
    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    /**
     * Get the preferred container name. See {@link #setPreferredContainerName(java.lang.String)}.
     * @return preferred container name.
     */
    public String getPreferredContainerName() {
        return preferredContainerName;
    }

    /**
     * Set the preferred container name. The GUI will create separate titled
     * and frames panel for each unique container name declared by the displayed preferences.
     * If you want multiple preferences to form a logical group and appear in the
     * same container, give them the same container name using this method.
     *
     * @param preferredContainerName container name (title). If the name is null,
     * the GUI container this preference displays in will not be titled.
     */
    public void setPreferredContainerName(String preferredContainerName) {
        this.preferredContainerName = preferredContainerName;
    }

    /**
     * Get the preference name (configuration key) that this preference depends
     * on. See {@link #setDependentOption(java.lang.String)}.
     * @return dependent parameter name or null if no such relationship has been
     * defined through the {@link #setDependentOption(java.lang.String)} method.
     */
    public String getDependentOption() {
        return dependentOption;
    }

    /**
     * Declare that this preference/parameter is only valid if the specified
     * parameter is TYPE_BOOLEAN and its value is true. This flag is used in GUI
     * to implement parameter pairs representing a parameter which can be set
     * on or off.
     * @param dependentOption name (configuration key) of the preference that
     * this one depends on.
     */
    public void setDependentOption(String dependentOption) {
        this.dependentOption = dependentOption;
    }

    /**
     * Get the list of accepted file extensions for the TYPE_FILE and
     * TYPE_FILELIST preferences. See the {@link #setAcceptedFileExtensions(java.lang.String[], java.lang.String)} method.
     * @return string array of accepted file extensions,
     * for example <code>new String[] {"txt"}</code>.
     */
    public String[] getAcceptedFileExtensions() {
        return acceptedFileExtensions;
    }

    /**
     * Set the list of accepted file extensions for the TYPE_FILE and
     * TYPE_FILELIST preferences. The optional description will be displayed
     * in the filter drop down of the file chooser.
     * @param acceptedFileExtensions string array of accepted file extensions,
     * for example <code>new String[] {"txt"}</code>.
     * @param descriptionKey filter description or resource bundle key of
     * the description.
     */
    public void setAcceptedFileExtensions(String[] acceptedFileExtensions, String descriptionKey) {
        this.acceptedFileExtensions = acceptedFileExtensions;
        this.acceptedFileExtensionsDescKey = descriptionKey;
    }

    /**
     * Get description of the file extension based filter for the TYPE_FILE
     * preferences. The description is first searched in the application resource
     * bundles by the description key provided in the {@link #setAcceptedFileExtensions(java.lang.String[], java.lang.String)}
     * method. If it is not found, the method returns the key itself.
     *
     * @return file filter description.
     */
    public String getAcceptedFileExtensionsDesc() {
        ResourceBundle r = ApplicationSupport.getResourceBundle();
        if (acceptedFileExtensionsDescKey != null && r.containsKey(acceptedFileExtensionsDescKey)) {
            return r.getString(acceptedFileExtensionsDescKey);
        }
        return acceptedFileExtensionsDescKey;
    }
    private final static String ttmsg = ApplicationSupport.getString("preference.tooltip");
    private final static String ttmsg2 = ApplicationSupport.getString("preference.tooltip2");

    /**
     * Get a simple HTML summary with the preference details. Used by the GUI to
     * display raw parameter properties as tool tip messages. This method should
     * not be used by parametrized objects.
     *
     * @return HTML code with preference raw data summary (name/preference key,
     * type, raw value stored in user configuration).
     */
    public String getSummary() {
        Object defaultValue = UserConfiguration.getInstance().getDefaultValue(getConfigurationKey());
        if (defaultValue == null) {
            defaultValue = getDefaultValue();
        }
        Object value = UserConfiguration.getInstance().get(getConfigurationKey());
        if (value == null) {
            value = "&lt;not specified&gt;";
        }
        if (defaultValue == null) {
            return MessageFormat.format(ttmsg, getConfigurationKey(), getTypeDesc(), value);
        }
        return MessageFormat.format(ttmsg2, getConfigurationKey(), getTypeDesc(), value, defaultValue);
    }

    /**
     * Get description of a preference type (see {@link #getType()})..
     * @return type description.
     */
    public String getTypeDesc() {
        switch (type) {
            case TYPE_STRING:
                if (values != null && values.size() > 0) {
                    if (selectOnly) {
                        return MessageFormat.format(ApplicationSupport.getString("preference.type.string.fixed"), values.toString());
                    }
                    return MessageFormat.format(ApplicationSupport.getString("preference.type.string.fixedOrCustom"), values.toString());
                }
                return ApplicationSupport.getString("preference.type.string");
            case TYPE_INT:
                if (getMinValue() != Integer.MIN_VALUE || getMaxValue() < Integer.MAX_VALUE) {
                    return MessageFormat.format(ApplicationSupport.getString("preference.type.intWithRange"), "&lt;" + getMinValue() + "; " + getMaxValue() + "&gt;");
                }
                return ApplicationSupport.getString("preference.type.int");
            case TYPE_FLOAT:
                if (getMinValue() != Integer.MIN_VALUE || getMaxValue() < Integer.MAX_VALUE) {
                    return MessageFormat.format(ApplicationSupport.getString("preference.type.floatWithRange"), "&lt;" + getMinValue() + "; " + getMaxValue() + "&gt;");
                }
                return ApplicationSupport.getString("preference.type.float");
            case TYPE_BOOLEAN:
                return ApplicationSupport.getString("preference.type.boolean");
            case TYPE_COLOR:
                return ApplicationSupport.getString("preference.type.color");
            case TYPE_KEYSTROKE:
                return ApplicationSupport.getString("preference.type.keystroke");
            case TYPE_STRINGLIST:
                return MessageFormat.format(ApplicationSupport.getString("preference.type.stringlist"), getDisplayValuesTable().keySet());
            case TYPE_INT_DISPLAYED_AS_BOOLEAN:
                return ApplicationSupport.getString("preference.type.intAsBoolean");
            case TYPE_FILE:
                if (acceptedFileExtensions != null) {
                    return MessageFormat.format(ApplicationSupport.getString("preference.type.fileWithExt"), Arrays.asList(acceptedFileExtensions));
                }
                return ApplicationSupport.getString("preference.type.file");
            case TYPE_DIRECTORY:
                return ApplicationSupport.getString("preference.type.directory");
            case TYPE_PASSWORD:
                return ApplicationSupport.getString("preference.type.password");
            case TYPE_FILELIST:
                if (acceptedFileExtensions != null) {
                    return MessageFormat.format(ApplicationSupport.getString("preference.type.filelistWithExt"), Arrays.asList(acceptedFileExtensions));
                }
                return ApplicationSupport.getString("preference.type.filelist");
        }
        return ApplicationSupport.getString("preference.type.unknown");
    }

    /**
     * Indicates whether the empty value ("") should be accepted as a valid one
     * and saved or not. Applies only to the TYPE_STRING preferences.
     * @return value of the accept flag.
     */
    public boolean isAcceptEmptyValue() {
        return acceptEmptyValue;
    }

    /**
     * Defines whether the empty value ("") should be accepted as a valid one
     * and saved or not. Applies only to the TYPE_STRING preferences. Default
     * value is true.
     * @param acceptEmptyValue the acceptEmptyValue to set
     */
    public void setAcceptEmptyValue(boolean acceptEmptyValue) {
        this.acceptEmptyValue = acceptEmptyValue;
    }

    /**
     * Get the number of visible text rows to be displayed if the parameter
     * is displayed in a text pane in the GUI. This property makes sense only
     * for the {@link #TYPE_TEXT} type and it is ignored by other types.
     * @return number of text rows to display if the parameter
     * is displayed in GUI.
     */
    public int getTextRowsToDisplay() {
        return textRowsToDisplay;
    }

    /**
     * Set the number of visible text rows to be displayed if the parameter
     * is displayed in a text pane in the GUI. This property makes sense only
     * for the {@link #TYPE_TEXT} type and it is ignored by other types.
     * @param textRowsToDisplay number of text rows to display if the parameter
     * is displayed in GUI.
     */
    public void setTextRowsToDisplay(int textRowsToDisplay) {
        this.textRowsToDisplay = textRowsToDisplay;
    }

    /**
     * Get the default value of this preference (parameter). If the default value
     * is null and the preference type is a primitive one such as int, float
     * or boolean, the real default value equals to the default Java one for the
     * primitive type (zero for int/float, false for boolean).
     * @return the defaultValue
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Set the default value of this parameter. Since the GUI typically doesn't
     * store parameters whose value is equal to the default one, do not set
     * the default value for mandatory parameters which must be always stored.
     *
     * @param defaultValue default parameter value. It is not validated so make
     * sure it is of the declared type and meets the rules and relationships
     * declared in this parameter.
     */
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Find out whether the {@link #TYPE_FILE} preference values may be provided
     * in form of URI.
     * @return true the parameter value should be converted from URI
     * or false otherwise.
     */
    public boolean isAcceptFileAsUri() {
        return reportFileAsURI;
    }

    /**
     * Controls whether the {@link #TYPE_FILE} preference should accept files
     * in form of a URL or URI, for example "file://C:\test.png".
     * @param reportFileAsURI true indicates that the parameter value might
     * be in URI/URL format.
     */
    public void setAcceptFileAsUri(boolean reportFileAsURI) {
        this.reportFileAsURI = reportFileAsURI;
    }

    /**
     * Check if a value meets the rules defined in this instance and is acceptable
     * for this preference (parameter). For example if the preference is of the
     * {@link #TYPE_INT} type, the method checks whether the value is a number or
     * whether it may be converted to a number. If the minimum and/or maximum
     * values are set through the {@link #setMinValue(int)} and {@link #setMaxValue(int)}
     * methods, the value is also checked against these limits.
     * @param value
     * @throws java.lang.IllegalArgumentException
     */
    public void checkValue(Object value) throws IllegalArgumentException {
        if (value == null) {
            if (!acceptEmptyValue) {
                throw new IllegalArgumentException("Parameter '" + configurationKey + "': Value may not be null.");
            }
            return;
        }
        switch (type) {
            case TYPE_BOOLEAN:
                if (!(value instanceof Boolean)) {
                    String s = value.toString();
                    if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("Parameter '" + configurationKey + "': Value '" + value + "' is not a valid boolean value.");
                    }
                }
                break;
            case TYPE_FLOAT:
            case TYPE_INT:
            case TYPE_INT_DISPLAYED_AS_BOOLEAN:
                Number n;
                if (!(value instanceof Number)) {
                    String s = value.toString();
                    try {
                        n = Float.parseFloat(s);
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Parameter '" + configurationKey + "': Value '" + value + "' is not a valid number.");
                    }
                } else {
                    n = (Number) value;
                }
                if (type != TYPE_FLOAT && n.intValue() != n.floatValue()) {
                    throw new IllegalArgumentException("Parameter '" + configurationKey + "': Value '" + value + "' is a float number while the parameter must be integer.");
                }
                if (n.floatValue() < minValue) {
                    throw new IllegalArgumentException("Parameter '" + configurationKey + "': Value '" + value + "' is lower than the allowed minimum of " + minValue + ".");
                }
                if (n.floatValue() > maxValue) {
                    throw new IllegalArgumentException("Parameter '" + configurationKey + "': Value '" + value + "' is greater than the allowed maximum of " + maxValue + ".");
                }
                break;
            case TYPE_COLOR:
                if (!(value instanceof Color)) {
                    try {
                        parser.parseColor(value.toString());
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Parameter '" + configurationKey + "': Value '" + value + "' is not a valid color.");
                    }
                }
                break;
        }
    }

    /**
     * Find out if the preference is mandatory and must be specified. This flag
     * is ignored by user preferences. It however plays a role when the preference
     * object is reused for a parametrized object which may declare through this
     * property that the parameter is mandatory and its value must be specified.
     * @return true if the preference (parameter) is mandatory, false if not.
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * Set whether the preferece is mandatory and must be specified. This flag
     * is ignored by user preferences. It however plays a role when the preference
     * object is reused for a parametrized object which may declare through this
     * property that the parameter is mandatory and its value must be specified.
     * @param mandatory true if the preference (parameter) is mandatory, false if not.
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Get the file accessory component. Applies just to {@link #TYPE_FILE}, {@link #TYPE_FILELIST} and {@link #TYPE_DIRECTORY}
     * preferences. It is an optional component for the file chooser which may provide
     * a preview of the file (for example an image thumbnail).
     * See the {@link JFileChooser#setAccessory(javax.swing.JComponent)}
     * method for more information.
     * @return the file chooser accessory component.
     */
    public JComponent getFileAccessory() {
        return fileAccessory;
    }

    /**
     * Set the file accessory component. Applies just to {@link #TYPE_FILE}, {@link #TYPE_FILELIST} and {@link #TYPE_DIRECTORY}
     * preferences. It is an optional component for the file chooser which may provide
     * a preview of the file (for example an image thumbnail).
     * See the {@link JFileChooser#setAccessory(javax.swing.JComponent)}
     * method for more information.
     * @param fileAccessory the file chooser accessory component.
     */
    public void setFileAccessory(JComponent fileAccessory) {
        this.fileAccessory = fileAccessory;
    }

    /**
     * @return the imageProvider
     */
    public ColorChooserImageProvider getImageProvider() {
        return imageProvider;
    }

    /**
     * @param imageProvider the imageProvider to set
     */
    public void setImageProvider(ColorChooserImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    /**
     * <p>This method allows to set verification options for TYPE_FILE and
     * TYPE_DIRECTORY preferences. As these preference types are
     * often used to store install locations of integrated third party component,
     * this is intended to provide a quick verification mechanism like "check if the
     * path selected by user contains this specific file".</p>
     *
     * <p>The <code>fileToSearchFor</code> parameter may
     * specify the required file name or a regular expression (pattern) the selected
     * file must comply with. If the preference is of the TYPE_DIRECTORY type,
     * the underlying GUI component is expected to search the selected directory
     * recursively and verify whether it contains a file complying with the
     * specified name or pattern.</p>
     * @param fileToSearchFor file name or a {@link Pattern} compliant regular expression.
     * @param defaultSearchPath default directory. It may be used for resolution
     * of relative paths. It may be null.
     * @param useRegularExpressions true matches the specified file name using
     * regular expressions, false sets regular string comparison.
     * @param ignoreCase whether the regular comparison should be done in a case
     * sensitive way or not. If the value is true, the file names will be
     * compared with the <code>fileToSearchFor</code> parameter using {@link String#equalsIgnoreCase(java.lang.String)}
     * instead of standard {@link String#equals(java.lang.Object)}. If regular
     * expressions are on (the <code>useRegularExpressions</code> parameter is true),
     * this parameter is ignored.
     * @param forceValueToFound Indicates whether the preference value should
     * be forced to the located file (TYPE_FILE) or its parent folder (TYPE_DIRECTORY).
     * The value of true forces the file search mechanism to set the value to the file found,
     * false leaves the value as it was selected by the user.
     */
    public void setDirFileToSearchFor(String fileToSearchFor, String defaultSearchPath,
            boolean useRegularExpressions, boolean ignoreCase, boolean forceValueToFound) {
        this.fileToSearchFor = fileToSearchFor;
        this.useRegularExpressions = useRegularExpressions;
        this.ignoreCase = ignoreCase;
        this.defaultSearchPath = defaultSearchPath;
        this.forceValueToFound = forceValueToFound;
    }

    /**
     * Get the file name to verify existence of in a TYPE_DIRECTORY or TYPE_FILE
     * preference. See {@link #setDirFileToSearchFor(java.lang.String, java.lang.String, boolean, boolean)}.
     * @return file name or regular expression to be verified.
     */
    public String getDirFileToSearchFor() {
        return fileToSearchFor;
    }

    /**
     * Get the default path for verification of value of a TYPE_DIRECTORY or TYPE_FILE
     * preference. See {@link #setDirFileToSearchFor(java.lang.String, java.lang.String, boolean, boolean)}.
     * @return file name or regular expression to be verified.
     */
    public String getDirDefaultSearchPath() {
        return defaultSearchPath;
    }

    /**
     * @return the useRegularExpressions
     */
    public boolean isDirSearchUseRegularExpressions() {
        return useRegularExpressions;
    }

    /**
     * @return the ignoreCase
     */
    public boolean isDirSearchIgnoreCase() {
        return ignoreCase;
    }

    /**
     * Indicates whether the preference value should be forced to the value of
     * the verification file search. See {@link #setDirFileToSearchFor(java.lang.String, java.lang.String, boolean, boolean)}.
     * @return true forces the file search mechanism to set the value to the file found,
     * false leaves the value as selected by the user.
     */
    public boolean isDirSearchForceValueToFound() {
        return forceValueToFound;
    }

    /**
     * Get description of available values. This text is used just by the
     * {@link #TYPE_STRINGLIST} preference type and it is displayed as a
     * description of the list of available (disabled) values on the right side
     * of the GUI component.
     * @return label text for available values.
     */
    public String getDescriptionOfAvailableValues() {
        return descriptionOfAvailableValues;
    }

    /**
     * Set description of available values. This text is used just by the
     * {@link #TYPE_STRINGLIST} preference type and it is displayed as a
     * description of the list of available (disabled) values on the right side
     * of the GUI component.
     * @param descriptionOfAvailableValues label text for available values.
     */
    public void setDescriptionOfAvailableValues(String descriptionOfAvailableValues) {
        this.descriptionOfAvailableValues = descriptionOfAvailableValues;
    }

    /**
     * @return the buttonGroup
     */
    public String getButtonGroup() {
        return buttonGroup;
    }

    /**
     * @param buttonGroup the buttonGroup to set
     */
    public void setButtonGroup(String buttonGroup) {
        this.buttonGroup = buttonGroup;
    }
}
