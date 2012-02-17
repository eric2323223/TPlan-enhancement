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
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.DefaultStyledDocument;

/**
 * Wrapper of a block of text commands. It is used to execute a single command
 * or a sequence of commands passed in form of a String.
 * @product.signature
 */
public class TextBlockWrapper extends GenericWrapper {

    boolean canPause = false;
    private String name;
    Element masterElement;

    public TextBlockWrapper(String commands, boolean canPause) {
        super();
        this.canPause = canPause;
        this.document = new DefaultStyledDocument();
        try {
            document.insertString(0, commands, null);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }

        // Setting of the local variable hash table to null will make sure that this wrapper will use
        // variable repository of its parent
        localVariables = null;
    }

    public TextBlockWrapper(DocumentWrapper parent, Element masterElement, String commands, boolean canPause) {
        super();
        this.canPause = canPause;
        this.masterElement = masterElement;
        this.parentWrapper = parent;
        this.document = new DefaultStyledDocument();
        try {
            document.insertString(0, commands, null);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }

        // Setting of the local variable hash table to null will make sure that this wrapper will use
        // variable repository of its parent
        localVariables = null;
    }

    public boolean isWithinSelection(Element e, boolean validationMode, ScriptingContext repository) {
//        return !selectionMode;
        return true;
    }

    public int getWrapperType() {
        return WRAPPER_TYPE_BLOCK;
    }

    public boolean canPause() {
        return canPause;
    }

    public String toString() {
        return "TextBlockWrapper";
    }

    public Element getMasterElement() {
        return masterElement;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
}
