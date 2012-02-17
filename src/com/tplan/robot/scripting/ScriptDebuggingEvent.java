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

import com.tplan.robot.scripting.interpret.TestScriptInterpret;

/**
 * Script event used for additional messages fired in debugging mode.
 * To set execution or validation to a debug mode put
 * a key ScriptEvent.REPOSITORY_OBJ_DEBUG_FLAG to the context.
 *
 * @author robert
 */
public class ScriptDebuggingEvent extends ScriptEvent {

    /**
     * Notify listeners that a procedure wrapper was created.
     */
    public static final int SCRIPT_DEBUG_VALIDATION_STARTED = 1000;

    /**
     * Notify listeners that a procedure wrapper was created.
     */
    public static final int SCRIPT_DEBUG_PROCEDURE_WRAPPER_CREATED = 1001;

    /**
     * Notify listeners that end of a procedure definition was reached.
     */
    public static final int SCRIPT_DEBUG_PROCEDURE_DEFINITION_END_REACHED = 1002;

    /**
     * Notify listeners that a procedure call was detected.
     */
    public static final int SCRIPT_DEBUG_PROCEDURE_CALL_REACHED = 1003;

    /**
     * Notify listeners that a procedure call was detected.
     */
    public static final int SCRIPT_DEBUG_PROCEDURE_CALL_ENDED = 1004;
    /**
     * Notify listeners that a procedure call was detected.
     */
    public static final int SCRIPT_DEBUG_COMMAND_IN_PROCEDURE_REACHED = 1005;
    /**
     * Notify listeners that an 'if' wrapper was created.
     */
    public static final int SCRIPT_DEBUG_STRUCTURE_WRAPPER_CREATED = 1010;

    /**
     * Notify listeners that an 'else' or 'else if' branch was reached.
     */
    public static final int SCRIPT_DEBUG_STRUCTURE_WRAPPER_CONTINUED = 1011;
    /**
     * Notify listeners that an 'if' wrapper was created.
     */
    public static final int SCRIPT_DEBUG_STRUCTURE_END_REACHED = 1012;
    /**
     * Notify listeners that an 'if' wrapper was created.
     */
    public static final int SCRIPT_DEBUG_NESTED_BLOCK_CREATED = 1013;
    /**
     * Notify listeners that a comment line was skipped.
     */
    public static final int SCRIPT_DEBUG_LINE_SKIPPED = 1020;

    /**
     * Notify listeners that a comment line was skipped.
     */
    public static final int SCRIPT_DEBUG_VALIDATING_LINE = 1021;

    public ScriptDebuggingEvent(Object source, TestScriptInterpret interpret, ScriptingContext context, int type) {
        super(source, interpret, context, type);
    }

    public ScriptDebuggingEvent(Object source, TestScriptInterpret interpret, ScriptingContext context, String message) {
        super(source, interpret, context, message);
    }
}
