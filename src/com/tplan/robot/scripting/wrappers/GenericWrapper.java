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
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import java.io.File;
import java.util.Hashtable;
import java.util.Map;

/**
 * Superclass of all scripting language wrappers.
 * @product.signature
 */
public abstract class GenericWrapper implements DocumentWrapper {

    /** A StyledDocument which represents one test script (one text file). */
//    protected JTextComponent editor;
    /** First text element of the current text selection. It is always null if selectionMode is false. */
    protected Element selectionStartElement;
    /** Last text element of the current text selection. It is always null if selectionMode is false. */
    protected Element selectionEndElement;
    /** Selection mode flag. It indicates whether we execute the entire test script or just some selected commands. */
    protected boolean selectionMode = false;
    /** Script file name. It is null for untitled documents. */
    protected File scriptFile;
    /** Exit flag. If set to true, no further commands should be executed. */
    protected boolean exit = false;
    protected DocumentWrapper parentWrapper;
    protected Map localVariables = new Hashtable();

    protected StyledDocument document;

    /**
     * Default constructor.
     */
    public GenericWrapper() {
    }

    /**
     * Constructor.
     * @param selectionMode <code>false</code> indicates that we execute the entire test script, <code>true</code>
     * just some selected commands. If true is used the constructor automatically initializes start and end elements
     * encapsulated by this class.
     */
    public GenericWrapper(StyledDocument document, int selectionStart, int selectionEnd, boolean selectionMode) {
//        this.editor = component;
        this.document = document;
        if (selectionMode && document != null) {
//            StyledDocument document = (StyledDocument) editor.getDocument();
            selectionStartElement = document.getParagraphElement(selectionStart);
            selectionEndElement = document.getParagraphElement(selectionEnd);
        }
        this.selectionMode = selectionMode;
    }

    /**
     * Get the document associated with this wrapper.
     * @return a StyledDocument instance. Never returns null.
     */
    public StyledDocument getDocument() {
        return document;
    }

    /**
     * Get the document associated with this wrapper.
     */
    public void setDocument(StyledDocument document) {
        this.document = document;
    }

    /**
     * Get value of the <code>selectionMode</code> flag.
     * @return value of the <code>selectionMode</code> flag.
     */
    public boolean isSelectionMode() {
        return selectionMode;
    }

    /**
     * Get the the first document element to be executed. An element of a StyledDocument represents one line of a text.
     * As we always parse all commands from the script beginning this method always returns the very first document element.
     *
     * @return the very first document element because we always start processing the script from the beginning.
     */
    public Element getStartElement() {
        return getDocument().getParagraphElement(0);
    }

    /**
     * Get an element which follows the argument one in the DOM (Document Object Model). It in fact returns the next
     * line of the document.
     *
     * @param element a document element.
     * @return next element defined in DOM or null if end of the document is reached.
     */
    public Element getNextElement(Element element, boolean validationMode, ScriptingContext repository) {
        Element e = null;
        int index = element.getDocument().getDefaultRootElement().getElementIndex(element.getStartOffset()) + 1;
        if (element.getDocument().getDefaultRootElement().getElementCount() > index) {
            e = element.getDocument().getDefaultRootElement().getElement(index);
        }
        return e;
    }

    /**
     * Get index of an element in the DOM (Document Object Model). Each StyledDocument instance has a root element
     * which contains an array of Element instances. One Element instance represents a line of text. In other words
     * this method actually returns index of a line of text in the document.
     *
     * @param e a document element.
     * @return index of the element in the DOM or -1 if the element is not found.
     */
    public int getElementIndex(Element e) {
        if (e != null) {
            Element root = getDocument().getDefaultRootElement();
            for (int i = 0; i < root.getElementCount(); i++) {
                if (root.getElement(i).equals(e)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Get the first element of the selected text. If we are executing the entire test script, i.e.
     * the <code>selectionMode</code> flag is set to false and the <code>selectionStartElement</code> variable is null,
     * we return the very first element of the document.
     *
     * @return starting element of the text selection.
     */
    public Element getSelectionStartElement() {
        return selectionStartElement == null ? getDocument().getParagraphElement(0) : selectionStartElement;
    }

    public Element getSelectionEndElement() {
        return selectionEndElement;
    }

    public void setSelectionStartElement(Element selectionStartElement) {
        this.selectionStartElement = selectionStartElement;
    }

    public void setSelectionEndElement(Element selectionEndElement) {
        this.selectionEndElement = selectionEndElement;
    }

    public File getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(File scriptFile) {
        this.scriptFile = scriptFile;
    }

    public int getWrapperType() {
        return WRAPPER_TYPE_UNKNOWN;
    }

    public DocumentWrapper getParentWrapper() {
        return parentWrapper;
    }

    public void setParentWrapper(DocumentWrapper parentWrapper) {
        this.parentWrapper = parentWrapper;
    }

    public void setVariable(ScriptingContext ctx, String name, Object value) {
        if (localVariables == null || (parentWrapper != null && parentWrapper.getVariable(name) != null)) {
            parentWrapper.setVariable(ctx, name, value);
        } else {
            localVariables.put(name, value);
        }
    }

    public Object getVariable(String name) {
        Object o;
        if (parentWrapper != null && (o = parentWrapper.getVariable(name)) != null) {
            return o;
        }
        return localVariables == null ? null : localVariables.get(name);
    }

    public Map getLocalVariables() {
        return localVariables;
    }

    public void setLocalVariables(Map table) {
        localVariables = table;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
    }

    public boolean isExecutionAllowed() {
        return true;
    }

    public boolean canPause() {
        return true;
    }

    public void exit() {
        exit = true;
    }

    public boolean isExit() {
        DocumentWrapper w = getParentWrapper();
        if (w != null && !w.equals(this)) {
            return exit || w.isExit();
        }
        return exit;
    }

    public String getTestSource() {
        return getScriptFile() == null ? null : getScriptFile().getAbsolutePath();
    }

    public int getLineNumber(ScriptingContext ctx) {
        Element e = (Element) ctx.get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT);
        if (e != null) {
            return getElementIndex(e);
        }
        return 0;
    }

}
