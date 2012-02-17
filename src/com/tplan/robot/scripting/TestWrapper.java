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

import java.io.File;
import javax.swing.text.StyledDocument;

/**
 * High level interface for the family of test script wrappers.
 *
 * @product.signature
 */
public interface TestWrapper {
    public static final int WRAPPER_TYPE_UNKNOWN = 100;
    public static final int WRAPPER_TYPE_SCRIPT = 0;
    public static final int WRAPPER_TYPE_PROCEDURE = 1;
    public static final int WRAPPER_TYPE_INCLUDE = 2;
    public static final int WRAPPER_TYPE_RUN = 3;
    public static final int WRAPPER_TYPE_BLOCK = 4;
    
    public static final int WRAPPER_TYPE_JAVA = 10;

    String getTestSource();
    int getWrapperType();
    int getLineNumber(ScriptingContext ctx);
    TestWrapper getParentWrapper();
    File getScriptFile();
    void setDocument(StyledDocument document);
    /**
     * Get the document associated with this wrapper.
     * @return a StyledDocument instance. Never returns null.
     */
    StyledDocument getDocument();
}
