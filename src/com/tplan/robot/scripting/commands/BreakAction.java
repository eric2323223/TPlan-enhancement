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
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * Break action encapsulates a flag which allows user to manually
 * break the command (make it stop).
 *
 * @product.signature
 */
public class BreakAction extends AbstractAction {

    public boolean breakCountDown = false;
    public TimerAction timerAction = null;

    public BreakAction(TimerAction timerAction) {
        this.timerAction = timerAction;
        String label = ApplicationSupport.getString("command.WaitCommand.continueLabel");
        putValue(SHORT_DESCRIPTION, label);
        putValue(NAME, label);
    }

    public void actionPerformed(ActionEvent e) {
        timerAction.breakCountDown = true;
    }

    public boolean isBreak() {
        return timerAction.breakCountDown;
    }
}
