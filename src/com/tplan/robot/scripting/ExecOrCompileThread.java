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

import com.tplan.robot.gui.*;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.scripting.interpret.InterpretErrorException;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import javax.swing.JOptionPane;

/**
 * A thread allowing to run a test script compilation or execution as a separate
 * thread.
 * @product.signature
 */
public class ExecOrCompileThread extends Thread {

    private TestScriptInterpret ts;
    private boolean execute;
    private MainFrame outer;
    private InterpretErrorException ex = null;

    public ExecOrCompileThread(TestScriptInterpret ts, boolean execute, MainFrame outer) {
        super();
        this.outer = outer;
        this.ts = ts;
        this.execute = execute;
    }

    @Override
    public void run() {
        try {
            if (execute) {
                ts.execute(null);
            } else {
                ts.compile(null);
            }
        } catch (InterpretErrorException ex) {
            this.ex = ex;
            if (outer != null) {
                String str = execute ? "com.tplan.robot.gui.MainFrame.msgExecuteErrorTitle" : "com.tplan.robot.gui.MainFrame.msgCompileErrorTitle";
                JOptionPane.showMessageDialog(outer, ex.getMessage(), ApplicationSupport.getString(str), JOptionPane.ERROR_MESSAGE);
            } 
        }
    }

    /**
     * @return the ex
     */
    public InterpretErrorException getException() {
        return ex;
    }
}
