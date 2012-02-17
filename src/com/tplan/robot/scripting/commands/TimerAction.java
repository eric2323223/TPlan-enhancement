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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.util.Utils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;

/**
 * Timer action is used by timeout based commands (such as Wait or WaitFor) to
 * display count down messages on the status bar.
 * 
 * @product.signature
 */
public class TimerAction implements ActionListener {

    public boolean breakCountDown = false;
    private long endTime = 0;
    private ScriptingContext context;

    public TimerAction(long endTime, ScriptingContext context) {
        this.endTime = endTime;
        this.context = context;
    }

    public void actionPerformed(ActionEvent e) {
        Object params[] = {
            Utils.getTimePeriodForDisplay(endTime - System.currentTimeMillis() + 1000, false),
            ApplicationSupport.getString("command.WaitCommand.continueLabel")
        };
        context.getScriptManager().fireScriptEvent(new ScriptEvent(
                this, null, context, MessageFormat.format(ApplicationSupport.getString("command.WaitCommand.waitStatusBarMsg"), params)));

    }
}
