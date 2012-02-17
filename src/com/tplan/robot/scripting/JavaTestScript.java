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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;

/**
 * Java test script interface. Objects implementing this interface may get
 * plugged into an automated testing thread through the {@link ApplicationSupport#createAutomatedRunnable(com.tplan.robot.scripting.JavaTestScript, java.lang.String, java.lang.String[], java.io.PrintStream, boolean)}
 * method and get their {@link #test()} method executed when user invokes the
 * appropriate action in the {@product.name} GUI or starts the associated automated
 * test thread programatically from a Java program.
 *
 * @product.signature
 */
public interface JavaTestScript {

    /**
     * Test method with the test script code. It will be executed when
     * user selects the "Execute" action in the GUI (through a tool bar button
     * or menu item) or if the automated testing thread containing this test script
     * gets started programatically.`
     */
    void test();

    /**
     * <p>Set context. Scripting context is a map holding various objects and
     * structures which may be needed for script compilation and execution.
     * A new context instance is passed to this test class before
     * every execution (i.e. before the {@link #test()} method is called).
     * Context instances are typically created by {@link ScriptManager}.
     *
     * @param context a context instance.
     */
    void setContext(ScriptingContext context);

    /**
     * <p>Associate a test script interpret with this Java test script.
     * This method will be called just once shortly after
     * this test script gets plugged into an automated test thread through
     * the {@link ApplicationSupport#createAutomatedRunnable(com.tplan.robot.scripting.JavaTestScript, java.lang.String, java.lang.String[], java.io.PrintStream, boolean)} method.
     * Object implementing this interface are
     * recommended to save a reference to the interpret in the method body
     * and use it to access objects from the execution context and/or register for
     * script or command events.</p>
     *
     * @param interpret instance of {@link TestScriptInterpret} which will interpret the code and control execution of this script.
     */
    void setInterpret(TestScriptInterpret interpret);
}
