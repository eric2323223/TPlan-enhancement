/*
 * T-Plan Robot, automated testing tool based on remote desktop technologies.
 * Copyright (C) 2009-2011 T-Plan Limited (http://www.t-plan.co.uk),
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
package com.tplan.robot.scripting.interpret;

import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.SyntaxErrorException;

/**
 * Optional methods of test script interpret allowing to optimize performance,
 * for example through a conditional compilation when there are script or dependency changes.
 *
 * @product.signature
 */
public interface OptimizedInterpret {

    /**
     * Compile source code of the test script. If the script is in a scripting language,
     * the method should check the code syntax. If the script is in a compilable
     * language (for example Java test scripts), the method should compile the code
     * and make it ready for execution.
     *
     * @param customContext customized context for this validation. If the argument
     * is null, the interpret is supposed to obtain a preinitialized context instance
     * from the script manager through {@link ScriptManager#createDefaultContext()}.
     * @return true if the script compiled fine, false if there were errors.
     * Compilation errors are to be saved as a list of {@link SyntaxErrorException}
     * instances to the compilation context. The interpret is also supposed to fire
     * appropriate compilation script events through the associated script manager.
     * The context should be made available after compilation through
     * the {@link #getCompilationContext()} method.
     *
     * @throws InterpretErrorException should be thrown on an internal error or
     * illegal interpret state. The exception message gets typically displayed
     * or otherwise reported to the user.
     */
    boolean compile(ScriptingContext customContext, boolean force) throws InterpretErrorException;
}
