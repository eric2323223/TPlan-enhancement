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

import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.scripting.commands.CommandListener;

/**
 * <p>Pluggable interface for objects which need to attach to a script execution.
 * Classes implementing this interface and plugged in to {@product.name} through
 * the standard {@link PluginManager Plugin API} will be instantiated when
 * the very first script execution starts.</p>
 *
 * <p>Code returned by the {@link Plugin#getCode()} method plays a role in the
 * instantiation order. When a script execution is being started, the {@link ScriptManager}
 * loads a list of plugins implementing this interface sorted in ascending order
 * by its code. Each class in the list is then instantiated and its
 * {@link #executionStarted(com.tplan.robot.scripting.ScriptingContext)} is called.
 * Should you want to instantiate the classes in a certain order, design the
 * plugin codes to be in the ascending alphabetic order reflecting the desired
 * instantiation order.</p>
 *
 * <p>All necessary execution related objects such as the test script interpret
 * or script manager are available through the argument context. Classes
 * interested in script and/or command events may implement the {@link ScriptListener}
 * resp. {@link CommandListener} interfaces and register with the script manager
 * available through the context ({@link ScriptingContext#getScriptManager()}).
 * An event of the execution end may be received through the {@link ScriptListener}
 * interface with the script event code equal to {@link ScriptEvent#SCRIPT_EXECUTION_FINISHED}.</p>
 *
 * @product.signature
 */
public interface ExecutionListener {
    /**
     * This method gets called just once when the very first script execution
     * is started. Objects which need to receive events of other script executions
     * must take advantage of this method to register as {@link ScriptListener}
     * with the script manager available through the context.
     *
     * @param context context of the very first test script execution.
     */
    void executionStarted(ScriptingContext context);
}
