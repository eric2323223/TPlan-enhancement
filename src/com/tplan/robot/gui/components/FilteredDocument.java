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
package com.tplan.robot.gui.components;

import javax.swing.text.*;
import java.util.*;

import java.awt.event.*;
import java.awt.Color;
import javax.swing.event.*;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.ToolTipManager;
import java.beans.*;

/**
 * <p>Custom document for a JTextField or editable JComboBox allowing to filter 
 * and validate its content. It has the following features:
 *
 * <p><li>It allows you to filter the text that is entered into the component. Each time a change occurs,
 * method <code>isContentsCorrect()</code> is called to check if the text is correct or not; in case of incorrect text
 * the component changes its text and background colors to error colors and if a tooltip text has been passed
 * in the constructor, displays immediately a tooltip window with error tooltip text.
 * </li>
 *
 * <p><li>Enables prompt reaction on any document change through method <code>contentsChanged(boolean)</code>,
 * which is fired on every document change. Its argument indicates if the text is correct or not.
 * This is useful if you have a dialog and you want for example to disable the 'OK' button while
 * a dialog component contains an incorrect value.
 * </li>
 * @product.signature
 */

public abstract class FilteredDocument extends PlainDocument
        implements KeyListener, DocumentListener, FocusListener, ActionListener, PropertyChangeListener {
    
    /**
     * Flag that indicates if the text in the textfield is correct. It is
     * updated upon any document change through the abstract method
     * <I>isContentsCorrect()</I>
     */
    protected boolean valueCorrect = false;
    
    /**
     * A helper flag used to indicate whether a document change was vetoed or not.
     */
    protected boolean valueCorrectVeto = false;
    
    /**
     * Color for the background in case of valid number; it is initialized
     * by the BG color of the passed component
     */
    protected Color bgColorCorrect;
    
    /**
     * Color for the text in case of correct text; it is initialized
     * by the text color of the passed component
     */
    protected Color textColorCorrect;
    
    /**
     * String used for incorrect text tooltip
     */
    protected String errorMessage = null;
    
    /**
     * Color for incorrect text; default value is Color.white
     */
    protected Color textColorIncorrect = Color.white;
    
    /**
     * Variable used to keep the JTextField that uses this document; we need it
     * because we set the tooltips and change the colors
     */
    protected JTextField field = null;
    
    /**
     * Variable used to keep the JCombobox that uses this document; we need it
     * because we set the tooltips and change the colors.
     */
    protected JComboBox box = null;
    
    /**
     * Used only for comboboxes, indicates if a backspace or white characters entered.
     */
    protected boolean key_typed = true;
    
    /**
     * String used for correct text tooltip
     */
    protected String tooltipMessage = null;
    
    /**
     * Constructor used for a JTextField
     * @param field a JTextField instance that uses this document
     */
    public FilteredDocument(JTextField field) {
        this.field = field;
        setTextColorCorrect(field.getForeground());
        setBgColorCorrect(field.getBackground());
        setToolTipText();
        this.addDocumentListener(this);
        //field.addFocusListener(this);
        field.setDocument(this);
        field.addPropertyChangeListener("enabled", this);
    }
    
    /**
     * Constructor used for a JTextField
     * @param field a JTextField instance that uses this document
     * @param toolTipText tooltip text used for correct text
     * @param errorToolTipText tooltip text used for incorrect text
     */
    public FilteredDocument(JTextField field, String toolTipText, String errorToolTipText) {
        this(field);
        tooltipMessage = toolTipText;
        errorMessage = errorToolTipText;
        setToolTipText();
    }
    
    /**
     * Constructor used for an editable JComboBox
     * @param comboBox a JComboBox instance that uses this document
     */
    public FilteredDocument(JComboBox comboBox) {
        this.box = comboBox;
        this.field = (JTextField)box.getEditor().getEditorComponent();
        setTextColorCorrect(field.getForeground());
        setBgColorCorrect(field.getBackground());
        this.field.addKeyListener(this);
        setToolTipText();
        this.addDocumentListener(this);
        this.box.addActionListener(this);
        //field.addFocusListener(this);
        field.setDocument(this);
        box.addPropertyChangeListener("enabled", this);
    }
    
    /**
     * Constructor used for an editable JComboBox
     * @param comboBox a JComboBox instance that uses this document
     * @param toolTipText tooltip text used for correct text
     * @param errorToolTipText tooltip text used for incorrect text
     */
    public FilteredDocument(JComboBox comboBox, String toolTipText, String errorToolTipText) {
        this(comboBox);
        tooltipMessage = toolTipText;
        errorMessage = errorToolTipText;
        setToolTipText();
    }
    
    /**
     * Get the value of the <I>valueCorrect</I> flag.
     * @return true if the text in the textfield is correct, false if not.
     */
    public boolean isValueCorrect() {
        return valueCorrect;
    }
    
    /**
     * Get the color for the correct text.
     * @return color that is used to paint correct text.
     */
    public Color getTextColorCorrect() {
        return textColorCorrect;
    }
    
    /**
     * Set the color for the correct text
     * @param c color that will be used to paint correct text
     */
    public void setTextColorCorrect(Color c) {
        textColorCorrect = c;
    }
    
    /**
     * Get the color for the background for correct text.
     * @return color that is used to paint the background of the JTextField in case of correct text
     */
    public Color getBgColorCorrect() {
        return bgColorCorrect;
    }
    
    /**
     * Set the color for the background for correct text.
     * @param c color that will be used to paint the background of the JTextField in case of correct text.
     */
    public void setBgColorCorrect(Color c) {
        bgColorCorrect = c;
    }
    
    /**
     * Get the color for incorrect text; default value is Color.white.
     * @return color that is used to paint the text of the JTextField in case of incorrect text.
     */
    public Color getTextColorIncorrect() {
        return textColorIncorrect;
    }
    
    /**
     * Set the color for incorrect text; default value is Color.white
     * @param c color that will be used to paint the text of the JTextField
     * in case of incorrect text
     */
    public void setTextColorIncorrect(Color c) {
        textColorIncorrect = c;
    }
    
    /**
     * Color for the background in case of incorrect text, default value
     * is Color.red
     */
    protected Color bgColorIncorrect = Color.red;
    private transient Vector vetoableChangeListeners;
    
    /**
     * Get the color for the background in case of incorrect text;
     * default value is Color.red
     * @return color that is used to paint the background of the JTextField
     * in case of incorrect text
     */
    public Color getBgColorIncorrect() {
        return bgColorIncorrect;
    }
    
    /**
     * Set the color for the background in case of incorrect text,
     * default value is Color.red
     * @param c color that will be used to paint the background of
     * the JTextField in case of incorrect text
     */
    public void setBgColorIncorrect(Color c) {
        bgColorIncorrect = c;
    }
    
    /**
     * Implementation of the DocumentListener interface. The method is empty and
     * has no functionality in this class.
     * @param e a DocumentEvent.
     */
    public void changedUpdate(DocumentEvent e) {
    }
    
    /**
     * Implementation of the DocumentListener interface. The method is called
     * when a piece of text is inserted into the document. In this case we call
     * {@link #contentsChanged contentsChanged()} method.
     * @param e a DocumentEvent.
     */
    public void insertUpdate(DocumentEvent e) {
        contentsChanged();
    }
    
    /**
     * Implementation of the DocumentListener interface. The method is called
     * when some characters are deleted from the document. In this case we call
     * {@link #contentsChanged contentsChanged()} method.
     * @param e a DocumentEvent.
     */
    public void removeUpdate(DocumentEvent e) {
        contentsChanged();
    }
    
    /**
     * Implementation of the KeyListener for this document. It's used for
     * comboboxes only.
     * @param e a KeyEvent.
     */
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == 8      // Accept backspace and other white characters
                || e.getKeyCode() > 40) {
            key_typed = true;
        }
    }
    
    /**
     * Implementation of the KeyListener for this document. The method is empty and
     * has no functionality in this class.
     * @param e a KeyEvent.
     */
    public void keyTyped(KeyEvent e) {
    }
    
    /**
     * Implementation of the KeyListener for this document. The method is empty and
     * has no functionality in this class.
     * @param e a KeyEvent.
     */
    public void keyReleased(KeyEvent e) {
    }
    
    /**
     * Implementation of the ActionListener for this document. Used only for
     * comboboxes which fire this when losing focus or upon 'Enter' pressing.
     * @param e a ActionEvent.
     */
    public void actionPerformed(ActionEvent e) {
        boolean notEmpty = !box.getEditor().getItem().equals("");
        
        valueCorrect = notEmpty;
        contentsChanged(notEmpty, box.getEditor().getItem().toString());
    }
    
    /**
     * Implementation of the FocusListener. This is a patch that solves the
     * problem that when a component using this document gains the focus, the
     * text is sometimes not selected.
     * @param e a FocusEvent.
     */
    public void focusGained(FocusEvent e) {
        field.setSelectionStart(0);
        field.setSelectionEnd(getLength());
    }
    
    /**
     * Implementation of the FocusListener. The method is empty and
     * has no functionality in this class.
     * @param e a FocusEvent.
     */
    public void focusLost(FocusEvent e) {
    }
    
    /**
     * Set the string that is used for correct text tooltip.
     * @param text a new tool tip text to be displayed when the contents of the
     * document is correct.
     */
    public void setToolTipText(String text) {
        tooltipMessage = text;
        setToolTipText();
    }
    
    /**
     * Set the string that is used for incorrect text tooltip.
     * @param text a new tool tip text to be displayed when the contents of the
     * document is incorrect.
     */
    public void setErrorToolTipText(String text) {
        errorMessage = text;
        setErrorToolTipText();
    }
    
    /**
     * Set a tooltip which is displayed in case of correct text; private use only
     */
    protected void setToolTipText() {
        field.setToolTipText(tooltipMessage);
    }
    
    /**
     * Set a tooltip which is displayed in case of incorrect text; private use only
     */
    protected void setErrorToolTipText() {
        field.setToolTipText(errorMessage);
    }
    
    /**
     * This method will be always called when some change in the document occurs.
     * Implement this method to ensure the correct behavior of your dialog.
     *
     * @param contentOK a boolean value which indicates if the entered text is
     * correct or not ({@link #isContentsCorrect isContentsCorrect()} method result).
     * @param obj value of the document (its text).
     */
    protected void contentsChanged(boolean contentOK, Object obj) {
        UserInputEvent e = new UserInputEvent(this, obj, contentOK);
        
        try {
            fireVetoableChange(e);
        } catch (PropertyVetoException ex) {
            valueCorrectVeto = true;
        }
    }
    
    /**
     * This method should return a boolean value that indicates if the entered
     * text is correct or not; if it is not correct, the field changes its color
     * to the error color and displays a tooltip with an error message.
     * Implement this method as your filter what text should be accepted
     * by this document.
     *
     * @param text content of the textfield or combobox.
     * @return the method should return true if the text is correct and false
     * otherwise.
     */
    public boolean isContentsCorrect(String text) {
        return !valueCorrectVeto;
    }
    
    /**
     * Method gets called anytime a change in the document occurs. Checks if
     * the value is a valid number and if it fits given limits, if not,
     * paints the editor text and background in error colors and fires the
     * abstract method contentsChanged(boolean)
     */
    protected void contentsChanged() {
        
        String text = null;
        
        try {
            text = getText(0, getLength());
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        
        valueCorrectVeto = false;
        
        boolean flag = isContentsCorrect(text);
        boolean isOldvalueCorrect = valueCorrect;
        
        if (!flag && text.equals("")) {
            
            // This branch is performed only in case of JComboBox ---------
            if (box != null) {
                if (key_typed) {
                    field.setForeground(textColorIncorrect);
                    field.setBackground(bgColorIncorrect);
                    key_typed = false;
                    valueCorrect = false;
                    contentsChanged(false, text);
                } else {
                    if (field.isEditable()) {
                        field.setForeground(textColorCorrect);
                        field.setBackground(bgColorCorrect);
                    }
                    valueCorrect = true;
                    contentsChanged(true, text);
                }
            } else {
                if (field.isEditable()) {
                    field.setForeground(textColorIncorrect);
                    field.setBackground(bgColorIncorrect);
                }
                valueCorrect = false;
                contentsChanged(false, text);
            }
        } else if (!flag) {
            if (field.isEditable()) {
                field.setForeground(textColorIncorrect);
                field.setBackground(bgColorIncorrect);
            }
            valueCorrect = false;
            contentsChanged(false, text);
        } else {
            if (field.isEditable()) {
                field.setForeground(textColorCorrect);
                field.setBackground(bgColorCorrect);
            }
            key_typed = false;
            valueCorrect = true;
            contentsChanged(true, text);
        }
        
        valueCorrect = valueCorrect && !valueCorrectVeto;
        
        if (field.isEnabled()) {
            if (field.isEditable()) {
                field.setForeground(valueCorrect ? textColorCorrect : textColorIncorrect);
                field.setBackground(valueCorrect ? bgColorCorrect : bgColorIncorrect);
            } 
        } else {
            field.setForeground(textColorCorrect);
            field.setBackground(bgColorCorrect);
        }
        
        if (!valueCorrect) {
            displayToolTip();
        } else if (!isOldvalueCorrect) {
            restoreToolTip();
        }
    }
    
    /**
     * Method sets error tooltip text and forces the ToolTipManager to show it
     * immediately (by sending it false mouse events). Method called when
     * incorrect text is entered
     */
    protected void displayToolTip() {
        setErrorToolTipText();
        
        // Display the tool tip messages only if the field is visible
        if (field.isShowing()) {
            ToolTipManager TM = ToolTipManager.sharedInstance();
            int delay = TM.getInitialDelay();
            int reshow = TM.getReshowDelay();
            
            TM.setInitialDelay(0);
            TM.setReshowDelay(0);
            MouseEvent e = new MouseEvent(field,
                    MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(),
                    0, 0, 0, 0, false);
            
            TM.mouseEntered(e);
            
            e = new MouseEvent(field,
                    MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
                    0, 0, 0, 0, false);
            TM.mouseMoved(e);
            TM.setReshowDelay(reshow);
            TM.setInitialDelay(delay);
        }
    }
    
    /**
     * Method sets regular tooltip text and forces the ToolTipManager to hide it
     * immediately (by sending it false mouse events). Method called when a
     * correct text is entered
     */
    protected void restoreToolTip() {
        setToolTipText();
        
        // Display the tool tip messages only if the field is visible
        if (field.isShowing()) {
            ToolTipManager TM = ToolTipManager.sharedInstance();
            MouseEvent e = new MouseEvent(field,
                    MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                    0, 0, 0, 0, false);
            
            TM.mousePressed(e);
        }
    }
    
    /**
     * Remove a UserInputListener from the list of listeners.
     * @param l a class implementing UserInputListener.
     */
    public synchronized void removeUserInputListener(UserInputListener l) {
        if (vetoableChangeListeners != null && vetoableChangeListeners.contains(l)) {
            Vector v = (Vector)vetoableChangeListeners.clone();
            
            v.removeElement(l);
            vetoableChangeListeners = v;
        }
    }
    
    /**
     * Add a UserInputListener to the list of listeners. All the listeners
     * are notified when a change in the component occurs. If a listener doesn't
     * like the value, it may fire a java.beans.PropertyVetoException to
     * indicate that the value is incorrect.
     *
     * @param l a class implementing UserInputListener.
     */
    public synchronized void addUserInputListener(UserInputListener l) {
        Vector v = vetoableChangeListeners == null
                ? new Vector(2)
                : (Vector)vetoableChangeListeners.clone();
        
        if (!v.contains(l)) {
            v.addElement(l);
            vetoableChangeListeners = v;
        }
    }
    
    /**
     * Fire a new property change event to all registered listeners.
     * If a listener doesn't like the value, it may fire a
     * java.beans.PropertyVetoException to set the correctness flag to false.
     * @param e a UserInputEvent.
     * @throws java.beans.PropertyVetoException when the text is considered to
     * be incorrect by any of the listeners.
     */
    protected void fireVetoableChange(UserInputEvent e)
    throws java.beans.PropertyVetoException {
        if (vetoableChangeListeners != null) {
            Vector listeners = vetoableChangeListeners;
            int count = listeners.size();
            
            for (int i = 0; i < count; i++) {
                ((UserInputListener)listeners.elementAt(i)).vetoableChange(e);
            }
        }
    }
    
    /**
     * This method gets called when a bound property is changed.
     * @param evt A PropertyChangeEvent object describing the event source
     *   	and the property that has changed.
     */
    
    public void propertyChange(PropertyChangeEvent evt) {
        // Bug fix: 13036
        final boolean enabled = box == null ? field.isEnabled() : box.isEnabled();
        if (!enabled) {
            contentsChanged();
        }
    }
}
