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

/**
 * Command events are fired by command handlers and indicate an action taken
 * on the desktop client (pointer event, key event, ...) or a change to one
 * of the objects in the context (new/modified variables, new screenshot
 * generated, ...).
 *
 * @product.signature
 */
public class CommandEvent extends java.util.EventObject {

    /**
     * Predefined action code for a pointer event. If an event with this code
     * is created, the custom object is expected to contain a valid MouseEvent
     * instance.
     */
    public static final String POINTER_EVENT = "POINTER_EVENT";

    /**
     * Predefined action code for a key event. If an event with this code
     * is created, the custom object is expected to contain a valid KeyEvent
     * instance.
     */
    public static final String KEY_EVENT = "KEY_EVENT";

    /**
     * Predefined action code for an event when a new output like screenshot,
     * warning or comment was generated.
     */
    public static final String OUTPUT_CHANGED_EVENT = "OUTPUT_CHANGED_EVENT";

    /**
     * Predefined action code for a log entry gets generated. The custom object
     * must be set to a string with the log text.
     */
    public static final String LOG_EVENT = "LOG_EVENT";

    /**
     * Predefined action code which indicates that a global variable was created.
     */
    public static final String GLOBAL_VARIABLE_EVENT = "GLOBAL_VARIABLE_EVENT";

    /**
     * Predefined action code which indicates that a local variable was created.
     */
    public static final String LOCAL_VARIABLE_EVENT = "LOCAL_VARIABLE_EVENT";

    private Object customObject;
    private String actionCode;
    private ScriptingContext context;
    private boolean consumed;

    public CommandEvent(Object source, ScriptingContext context, String actionCode, Object customObject) {
        super(source);
        this.actionCode = actionCode;
        this.customObject = customObject;
        this.context = context;
    }

    public CommandEvent(Object source,  ScriptingContext context, String actionCode) {
        this(source, context, actionCode, null);
    }

    /**
     * Get custom object. It is just a helper variable which allows to
     * encapsulate an object (for example a value) into the event.
     *
     * @return value of the custom object field or null iv this event contains
     * no custom object.
     */
    public Object getCustomObject() {
        return customObject;
    }

    /**
     * Get action code. It is a string which should identify what happened or
     * type of the event. Action codes are up to particular command
     * implementations.
     *
     * @return an action code.
     */
    public String getActionCode() {
        return actionCode;
    }

    /**
     * Get context associated with this event.
     * @return the context
     */
    public ScriptingContext getContext() {
        return context;
    }

    /**
     * Find out whether the even is consumed or not. A "consumed" event means
     * that it has already reached the listener it was targeted to and it should
     * not be forwarded to other listeners.
     *
     * @return the event state. The value of "true" corresponds to "consumed".
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * Consume the event. See the {@link #isConsumed()} method for details.
     * @param consumed true sets the event state to "consumed".
     */
    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }
}
