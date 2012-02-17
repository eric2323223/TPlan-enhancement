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
package com.tplan.robot.scripting;

import java.util.Map;

/**
 * This intefrace allows to add custom dynamic variables through the plug in
 * interface. A dynamic variable is a variable whose value is calculated every
 * time the variable gets called from a script. For example, one of the default
 * dynamic variables called "_CURTIME" calls internally the <code>System.currentTimeMillis()</code>
 * method to return actual time in milliseconds. For the list of such variables
 * see the {@doc.cmd curtime Var command documentation}.
 * @product.signature
 */
public interface DynamicVariable {

    /**
     * Get the variable name.
     * @return variable name. Make sure it doesn't conflict with any default variable.
     */
    String getCode();

    /**
     * Get the variable value. This method will be called any time the variable
     * gets referenced from the test script. As script variables are generally
     * String values, the result must be a String. This method is not supposed
     * to add itself to the variable map - this will be handled by the script manager.
     *
     * @param varName variable name
     * @param cliVars map of variables [name, value] passed in to the script through the
     * <code>-v/--variable</code> CLI option.
     * @param variables current map of variables [name, value].
     * @param wr current test wrapper.
     * @param ctx scripting context.
     * @return variable value in form of String.
     */
    String toString(String varName, Map cliVars, Map variables, TestWrapper wr, ScriptingContext ctx);

    /**
     * This method gets called whenever a new scripting context is created.
     * Dynamic variables which have default values are welcome to populate
     * themselves in the context through the {@link ScriptingContext#setVariable(java.lang.String, java.lang.Object)}
     * method.
     *
     * @param ctx newly created context.
     */
    void contextCreated(ScriptingContext ctx);
}
