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
import javax.swing.text.Element;
import java.util.Map;

/**
 * Document wrapper encapsulates a styled document with a test script or its part specified by
 * a start and end document elements.
 * @product.signature
 */
public interface DocumentWrapper extends TestWrapper {


    /**
     * This method should return false if whole document content is to be executed or validated.
     * A value of true indicates that this class wraps just a part of the document specified
     * by the start and end elements.
     *
     * @return false if whole document content is to be executed, true otherwise.
     */
    public boolean isSelectionMode();

    /**
     * Get the the first document element (i.e. line of text) to be executed.
     *
     * @return the first element to be executed.
     */
    public Element getStartElement();

    /**
     * Find out if an element is within execution selection, i.e. if it is to be executed.
     *
     * @param e a document element. Each element represents a single line of text in the document.
     * @param validationMode true indicates that we are just validating (compiling) the code, false means that we are executing it.
     * @param context execution or validation context.
     * @return true if the element is to be executed, false if not.
     */
    public boolean isWithinSelection(Element e, boolean validationMode, ScriptingContext context);

    /**
     * Get an element which follows the argument one in the DOM (Document Object Model). Most implementations in fact
     * return the next line of the document.
     *
     * @param element a document element.
     * @param validationMode true indicates that we are just validating (compiling) the code, false means that we are executing it.
     * @param context execution or validation context.
     * @return next element defined in the styled document or null if end of the document is reached.
     */
    public Element getNextElement(Element element, boolean validationMode, ScriptingContext context);

    /**
     * Get index of an element in the DOM (Document Object Model). Each StyledDocument instance has a root element
     * which contains an array of Element instances. One Element instance represents a line of text. In other words
     * this method actually returns index of a line of text in the document.
     *
     * @param e a document element.
     * @return index of the element in the DOM or -1 if the element is not found.
     */
    public int getElementIndex(Element e);

    public void setParentWrapper(DocumentWrapper parentWrapper);

    public void setVariable(ScriptingContext ctx, String name, Object value);

    public Object getVariable(String name);

    public Map getLocalVariables();

    public void setLocalVariables(Map table);

    public boolean isExecutionAllowed();

    public boolean canPause();

    public void exit();

    public boolean isExit();
    public File getScriptFile();

}
