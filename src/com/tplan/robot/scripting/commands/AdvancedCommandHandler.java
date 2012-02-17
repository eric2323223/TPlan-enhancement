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

import com.tplan.robot.scripting.ScriptingContext;
import java.util.List;

/**
 * Interface allowing the commands to expose context dependent list of supported
 * arguments, parameters and values. It is intended to help users design commands
 * through the script editor and command wizard GUI components.
 * 
 * @product.signature
 */
public interface AdvancedCommandHandler extends CommandHandler {

    /**
     * Get the list of supported arguments. For example, the Mouse command should
     * return a list containing "click", "move" etc. Commands which have no
     * argument and rely just on parameters (param=value) should return null.
     * @param command the current command.
     * @param context a context.
     * @return list of supported argument values.
     */
    List getArguments(String command, ScriptingContext context);

    /**
     * Get the list of supported parameters. Command handlers are free to
     * parse the current command and return a filtered list depending on
     * what is already specified. Parameters in the list should be String
     * instances. If they are other objects than String, they may or may not
     * be handled well depending on what the GUI supports.
     *
     * @param command the current conmmand text (complete).
     * @param context a context.
     * @return list of supported parameter names.
     */
    List getParameters(String command, ScriptingContext context);

    /**
     * Get values of a particular parameter. This is to be used for parameters
     * which have a fixed set of acceptable values. The command wizard typically
     * displays the values as a list.
     *
     * @param paramName parameter name.
     * @param command the current conmmand text (complete).
     * @param context a context.
     * @return list of supported parameter values.
     */
    List getParameterValues(String paramName, String command, ScriptingContext context);
}
