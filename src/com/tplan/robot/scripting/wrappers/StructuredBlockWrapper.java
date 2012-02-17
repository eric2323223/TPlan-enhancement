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

import com.tplan.robot.util.DocumentUtils;
import com.tplan.robot.scripting.DocumentWrapper;

import com.tplan.robot.scripting.ScriptingContext;
import javax.swing.text.Element;
import java.util.Map;
import javax.swing.text.StyledDocument;

/**
 * Wrapper implementing common functionality infrastructure for structured expressions
 * like {@doc.cmd if} and {@doc.cmd for}.
 * @product.signature
 */
public class StructuredBlockWrapper extends GenericWrapper {

    protected boolean executionAllowed = true;

    /**
     * Default constructor.
     */
    public StructuredBlockWrapper() {
    }

    /**
     * Constructor.
     */
    public StructuredBlockWrapper(StyledDocument document, DocumentWrapper parentWrapper, Element startElement, Map repository, boolean validationMode) {
        super(document, 0, document.getLength(), false);
        this.parentWrapper = parentWrapper;
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

        String text = DocumentUtils.getElementText(e);

        // An element is within selection if it is between start and end of selection
        if (e != null && text != null) {
            boolean b = e.getStartOffset() >= selectionStartElement.getStartOffset();
            if (selectionEndElement != null) {
                b = b && e.getEndOffset() <= selectionEndElement.getEndOffset();
            }
            return b;
        }
        return false;
    }

    /**
     * Get the the first document element to be executed. An element of a StyledDocument represents one line of a text.
     * As we always parse all commands from the script beginning this method always returns the very first document element.
     *
     * @return the very first document element because we always start processing the script from the beginning.
     */
    public Element getStartElement() {
        return selectionStartElement;
    }


    public int getWrapperType() {
        return WRAPPER_TYPE_SCRIPT;
    }

    public boolean shouldExitWrapperOnThisElement(Element e, String text, ScriptingContext repository, boolean validationMode, boolean isWithinSelection) {
        return false;
    }

    public boolean isExecutionAllowed() {
        boolean isExit = isExit();
        if (getParentWrapper() instanceof StructuredBlockWrapper) {
            boolean isExecAllowed = ((StructuredBlockWrapper)parentWrapper).isExecutionAllowed();
            return !isExit && executionAllowed && isExecAllowed;
        }
        return !isExit && executionAllowed;
    }

    public String toString() {
        return "StructuredBlockWrapper";
    }
}
