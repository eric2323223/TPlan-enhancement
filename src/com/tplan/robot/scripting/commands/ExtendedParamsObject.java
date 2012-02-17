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
package com.tplan.robot.scripting.commands;

import com.tplan.robot.gui.components.CustomHyperlinkListener;
import com.tplan.robot.preferences.Preference;
import java.util.List;
import java.util.Map;

/**
 * Allows objects created by commands such as image comparison modules or report
 * providers declare parameters and support them through the handling command.
 * @product.signature
 */
public interface ExtendedParamsObject {

    /**
     * Get the list of parameter names supported by the object implementing this
     * interface.
     * @return list of supported parameter names.
     */
    List<String> getParameters();

    /**
     * Get the list of acceptable values of a particular parameter (if known).
     * The method should return null for parameters which are not enumerated
     * "one of" type.
     * @param parameterName parameter name
     * @return list of acceptable parameter values or null if the parameter value
     * is not limited to a particular value set.
     */
    List getParameterValues(String parameterName);

    /**
     * Set parameters and their values in the object. It will be called by the
     * command and the map will contain parameters parsed on the command line.
     * Parsing of the parameter values should be performed in a way which is
     * not case sensitive (for example through encapsulating the map through the
     * {@link com.tplan.robot.util.CaseTolerantHashMap} class);
     *
     * @param paramsAndValues map of parameters and their values.
     */
    void setParameters(Map<String, String> paramsAndValues);

    /**
     * Get the list of supported parameters encapsulated in {@link com.tplan.robot.preferences.Preference}
     * instances. It allows to specify a larger set of parameter properties such as
     * parameter name ("configuration key" in the Preferences object), label (short description),
     * description, expected value type and optional value limits and relationships with other
     * parameters. This allows the GUI to build and display the parameters in form of GUI components
     * in command property dialogs and validate the values entered by users..
     * @return list of supported parameters in form of {@link Preference} instances.
     */
    List<Preference> getVisualParameters();

    /**
     * Get short description which describes in a few words purpose and role of
     * the object. The text should be single line in plain text. This is optional
     * and the method may return null. The short description is typically
     * displayed by the GUI by the object name or ID.
     *
     * @return short object description.
     */
    String getShortDescription();

    /**
     * Get long description of the object - purpose, usage, parameter syntax etc.
     * The text may be multiline plain text or HTML with extended link syntax
     * described in {@link CustomHyperlinkListener}. This is optional
     * and the method may return null. The long description is typically
     * displayed by the GUI when user selects to view details of the object.
     *
     * @return long object description.
     */
    String getLongDescription();
}
