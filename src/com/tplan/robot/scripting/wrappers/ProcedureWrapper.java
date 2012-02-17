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
package com.tplan.robot.scripting.wrappers;

import com.tplan.robot.scripting.DocumentWrapper;
import com.tplan.robot.scripting.ScriptingContext;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.Element;
import java.util.Map;
import javax.swing.text.StyledDocument;

/**
 * Wrapper implementing functionality of {@doc.cmd procedures procedures}. 
 * @product.signature
 */
public class ProcedureWrapper extends GenericWrapper {
    private String procedureName;

    private Element parentElement;

    private List argumentList = new ArrayList();

    public ProcedureWrapper(DocumentWrapper parent, StyledDocument document, String procedureName, boolean selectionMode) {
        this.selectionMode = selectionMode;
        this.procedureName = procedureName;
        this.parentWrapper = parent;
        this.document = document;
    }

    public Element getStartElement() {
        return selectionStartElement == null ? super.getStartElement() : selectionStartElement;
    }

    /**
     * Find out if an element is within execution selection, i.e. if it is to be executed.
     * The method runs in two modes:
     * <li>We run the entire script - the method returns <code>true</code> for all elements.
     * <li>We run just selected commands - the method returns <code>true</code> if the argument element
     * is within selection and false if not.
     *
     * @param e a document element. Each element represents a single line in the document.
     * @return true if the element is to be executed, false if not.
     */
    public boolean isWithinSelection(Element e, boolean validationMode, ScriptingContext repository) {

        if (isExit()) {
            return false;
        }

        // If there's no selection, we process all document elements
        if (!selectionMode) {
            return true;
        }

        // An element is within selection if it is between start and end of selection
        if (e != null && selectionStartElement != null && selectionEndElement != null) {
            return e.getStartOffset() >= selectionStartElement.getStartOffset() && e.getEndOffset() <= selectionEndElement.getEndOffset();
        }

        // Bug fix. Return false if the end element is null. It happens when somebody executes a procedure with missing
        // end curly brace '}'
        return selectionEndElement != null;
    }

    public int getWrapperType() {
        return WRAPPER_TYPE_PROCEDURE;
    }

//    public void setParentWrapper(DocumentWrapper parentWrapper) {
//        // Do not allow to set the parent wrapper; it's rather passed in the constructor
//    }

    public String getProcedureName() {
        return procedureName;
    }

    public Element getParentElement() {
        return parentElement;
    }

    public void setParentElement(Element parentElement) {
        this.parentElement = parentElement;
    }

    public void pushArguments(Map<String, String> t) {
        argumentList.add(0, t);
    }

    public void popArguments() {
        if (argumentList.size() > 0) {
            argumentList.remove(0);
        }
    }

    public Map<String, String> getArguments() {
        if (argumentList.size() > 0) {
            return (Map<String, String>) argumentList.get(0);
        }
        return null;
    }

    public String toString() {
        return "ProcedureWrapper";
    }

}
