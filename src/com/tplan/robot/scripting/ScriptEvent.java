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
import java.lang.ref.WeakReference;

/**
 * Script events describe events related to test script execution like start,
 * stop and pause, execution of particular commands etc.
 *
 * @product.signature
 */
public class ScriptEvent extends java.util.EventObject {

    /**
     * Fired when script compilation finishes.
     */
    public static final int SCRIPT_COMPILATION_FINISHED = 1;
    /**
     * Fired when script execution is starting.
     */
    public static final int SCRIPT_EXECUTION_STARTED = 2;
    /**
     * Fired when execution of a procedure is starting.
     */
    public static final int SCRIPT_PROCEDURE_STARTED = 3;
    /**
     * Fired when execution of a procedure finishes.
     */
    public static final int SCRIPT_PROCEDURE_FINISHED = 4;
    /**
     * Fired when execution jumps to another line of code. This type of event
     * is typically consumed by editors which highlight the executed line of code.
     * The element itself is available through the context.
     */
    public static final int SCRIPT_EXECUTED_LINE_CHANGED = 5;
    /**
     * Fired when a displayable message is available. This type of event
     * is typically consumed by GUI components like status bar.
     */
    public static final int SCRIPT_MESSAGE_AVAILABLE = 6;
    /**
     * Fired when variables stored in the context have changed.
     */
    public static final int SCRIPT_VARIABLES_UPDATED = 7;
    /**
     * Fired when script compilation finishes and list of errors is available.
     * This type of event is typically consumed by editors which highlight
     * invalid lines of code.
     */
    public static final int SCRIPT_ERRORS_AVAILABLE = 8;
    /**
     * Fired when execution gets stopped manually.
     */
    public static final int SCRIPT_EXECUTION_STOPPED = 9;
    /**
     * Fired when execution gets stopped manually.
     */
    public static final int SCRIPT_EXECUTION_FINISHED = 10;
    /**
     * Notify listeners that either Include or Run command is to be executed. It is typically
     * consumed by GUI components which for example open the file in editor.
     * Note that this event type uses the event custom object to pass a File
     * instance representing the included file.
     */
    public static final int SCRIPT_EXECUTING_INCLUDE = 11;
    /**
     * Notify listeners that an Include or Run command failed to open the specified file.
     */
    public static final int SCRIPT_INCLUDE_FAILED = 12;
    /**
     * Notify listeners that execution was paused either by the Pause command or
     * manually by the user. Note that this event type uses the event custom object
     * to store a String instance representing reason of the pause.
     */
    public static final int SCRIPT_EXECUTION_PAUSED = 13;
    /**
     * Notify listeners that previously paused execution has resumed.
     */
    public static final int SCRIPT_EXECUTION_RESUMED = 14;
    /**
     * Notify listeners that the pause flag has changed.
     */
    public static final int SCRIPT_PAUSE_FLAG_CHANGED = 15;
    /**
     * This event type indicates that the script handler is going to run a
     * line of code. If any of the listeners throws a PauseRequestException,
     * the script handler will interpret it as a request to pause execution
     * on that particular line. This mechanism is used by the script editor
     * to implement breakpoints.
     */
    public static final int SCRIPT_GOING_TO_RUN_LINE = 16;
    /**
     * Notify listeners that a new remote desktop client was created.
     */
    public static final int SCRIPT_CLIENT_CREATED = 17;
    /**
     * Notify listeners that the compile process was started.
     */
    public static final int SCRIPT_COMPILATION_STARTED = 18;
    /**
     * Notify listeners that a nested interpret was created.
     */
    public static final int SCRIPT_NESTED_INTERPRET_CREATED = 19;
    /**
     * Notify listeners that the script was destroyed and should not be used any more..
     */
    public static final int SCRIPT_INTERPRET_DESTROYED = 99;

    private int type;
    private String message;
    private Object customObject;
    private TestScriptInterpret interpret;
    private ScriptingContext context;

    public ScriptEvent(Object source, TestScriptInterpret interpret, ScriptingContext context, int type) {
        super(new WeakReference(source));
        this.type = type;
        this.interpret = interpret;
        this.context = context;
    }

    public ScriptEvent(Object source, TestScriptInterpret interpret, ScriptingContext context, String message) {
        this(source, interpret, context, SCRIPT_MESSAGE_AVAILABLE);
        this.message = message;
    }

    public ScriptEvent(TestScriptInterpret interpret, ScriptingContext context, int type) {
        this(interpret, interpret, context, type);
    }

    public ScriptEvent(TestScriptInterpret interpret, ScriptingContext context, String message) {
        this(interpret, interpret, context, SCRIPT_MESSAGE_AVAILABLE);
        this.message = message;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @return the context
     */
    public ScriptingContext getContext() {
        return context;
    }

    /**
     * @return the wrapper
     */
    public TestWrapper getWrapper() {
        if (getSource() instanceof TestWrapper) {
            return (TestWrapper) getSource();
        }
        if (context != null){
            return (TestWrapper) context.get(ScriptingContext.CONTEXT_CURRENT_SCRIPT_WRAPPER);
        }
        return null;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the customObject
     */
    public Object getCustomObject() {
        return customObject;
    }

    /**
     * @param customObject the customObject to set
     */
    public void setCustomObject(Object customObject) {
        this.customObject = customObject;
    }

    /**
     * Get test script interpret associated with this event
     * @return the test script interpret associated with this event.
     */
    public TestScriptInterpret getInterpret() {
        if (interpret == null) {
            if (context == null) {
                return null;
            }
            return (TestScriptInterpret) context.get(ScriptingContext.CONTEXT_INTERPRET);
        }
        return interpret;
    }

    /**
     * Get script manager associated with this event
     * @return the script manager associated with this event.
     */
    public ScriptManager getScriptManager() {
        if (getSource() instanceof ScriptManager) {
            return (ScriptManager) getSource();
        }
        if (context != null) {
            return context.getScriptManager();
        }
        if (getInterpret() != null) {
            return getInterpret().getScriptManager();
        }
        return null;
    }

    @Override
    public Object getSource() {
        Object o = super.getSource();
        if (o instanceof WeakReference) {
            return ((WeakReference)o).get();
        }
        return o;
    }
}
