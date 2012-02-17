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
import java.io.File;
import java.lang.ref.WeakReference;
import javax.swing.text.StyledDocument;

/**
 * Top level wrapper which encapsulates a test script document.
 * @product.signature
 */
public class ScriptWrapper extends GenericWrapper {

    private WeakReference context;

    /**
     * Default constructor.
     */
    public ScriptWrapper() {
    }

    /**
     * Constructor.
     * @param component a text component. It is needed because it holds a documentand contains information about text
     * selection.
     * @param selectionMode <code>false</code> indicates that we execute the entire test script, <code>true</code>
     * just some selected commands. If true is used the constructor automatically initializes start and end elements
     * encapsulated by this class.
     */
    private ScriptWrapper(StyledDocument document, int selectionStart, int selectionEnd, File scriptFile, boolean selectionMode) {
        super(document, selectionStart, selectionEnd, selectionMode);
        setScriptFile(scriptFile);
    }

    public ScriptWrapper(StyledDocument document, File scriptFile) {
        this(document, -1, -1, scriptFile, false);
    }

    public ScriptWrapper(StyledDocument document, int selectionStart, int selectionEnd, File scriptFile) {
        this(document, selectionStart, selectionEnd, scriptFile, true);
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
        if (e != null) {
            return e.getStartOffset() >= selectionStartElement.getStartOffset() && e.getEndOffset() <= selectionEndElement.getEndOffset();
        }
        return true;
    }

    public int getWrapperType() {
        return WRAPPER_TYPE_SCRIPT;
    }

    public String toString() {
        return "ScriptWrapper";
    }

    /**
     * @return the context
     */
    public ScriptingContext getContext() {
        return (ScriptingContext)context.get();
    }

    /**
     * @param context the context to set
     */
    public void setContext(ScriptingContext context) {
        this.context = new WeakReference(context);
    }
}
