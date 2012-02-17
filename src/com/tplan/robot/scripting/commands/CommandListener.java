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

import com.tplan.robot.scripting.ScriptManager;

/**
 * <p>Command listener interface allows objects to receive events from individual
 * scripting command handlers. Events are usually fired by command handlers
 * during script execution. Events may be theoretically also fired from a
 * command handler constructor, i.e. when a command handler instance gets created
 * by the script handler. Typical consumers of such events are report
 * providers which refresh reports whenever a new output like screenshot,
 * comment, warning or image comparison result is generated.</p>
 *
 * <p>Objects implementing this interface may register for command events with
 * the appropriate {@link ScriptManager} instance.</p>
 * @product.signature
 */
public interface CommandListener {

    /**
     * This method is called when a command handler fires an event.
     * @param e
     */
    void commandEvent(CommandEvent e);
}