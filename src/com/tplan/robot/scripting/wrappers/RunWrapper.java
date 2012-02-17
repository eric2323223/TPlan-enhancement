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

import com.tplan.robot.scripting.ScriptingContext;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

/**
 * Wrapper implementing functionality of the {@doc.cmd Run} command.
 * @product.signature
 */
public class RunWrapper extends GenericWrapper {

    private Element parentElement;

    public RunWrapper(StyledDocument document, int selectionStart, int selectionEnd, boolean selectionMode) {
        super(document, selectionStart, selectionEnd, selectionMode);
        localVariables = null;
    }

    public boolean isWithinSelection(Element e, boolean validationMode, ScriptingContext repository) {
        return !exit;
    }

    public int getWrapperType() {
        return WRAPPER_TYPE_RUN;
    }

    public String toString() {
        return "RunWrapper";
    }

    /**
     * @return the parentElement
     */
    public Element getParentElement() {
        return parentElement;
    }

    /**
     * @param parentElement the parentElement to set
     */
    public void setParentElement(Element parentElement) {
        this.parentElement = parentElement;
    }
}
