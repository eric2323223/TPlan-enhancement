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
package com.tplan.robot.gui.editor;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Text field component which displays textual representation of pressed keys
 * in the format accepted by the {@doc.cmd Press} command. For example when user
 * presses the Control key together with 'T', the component displays 'Ctrl+T'.
 *
 * @product.signature
 */
public class KeyTextField extends JTextField implements KeyListener, FocusListener {

    Map keyCache = new HashMap();
    Map keys;
    Map reversedKeys;

    String keyString;
    boolean deleteContent = false;

    public KeyTextField() {
        addKeyListener(this);
        setDocument(new KeyDocument());
        addFocusListener(this);
    }

    public void setValues(Map t) {
        keys = t;

        reversedKeys = new HashMap();
        Iterator e = keys.keySet().iterator();
        Object o;
        while (e.hasNext()) {
            o = e.next();
            reversedKeys.put(keys.get(o), o);
        }
    }

    public void keyPressed(KeyEvent e) {
        updateKeyField(e);
    }

    public void keyReleased(KeyEvent e) {
        updateKeyField(e);
    }

    public void keyTyped(KeyEvent e) {
    }

    private void updateKeyField(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (deleteContent) {
                keyCache.clear();
                deleteContent = false;
            }
            firePropertyChange("keyPressed", null, reversedKeys.get(new Integer(e.getKeyCode())));
            keyCache.put(new Integer(e.getKeyCode()), e);
        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
            firePropertyChange("keyGenerated", "", keyString);
            keyCache.remove(new Integer(e.getKeyCode()));
            deleteContent = true;
            return;
        }
        Iterator en = keyCache.keySet().iterator();
        String text = "";
        String modifiers = "";
        Number code;
        while (en.hasNext()) {
            code = (Number)en.next();
            if (code.intValue() == KeyEvent.VK_ALT) {
                modifiers += "Alt+";
            } else if (code.intValue() == KeyEvent.VK_SHIFT) {
                modifiers += "Shift+";
            } else if (code.intValue() == KeyEvent.VK_CONTROL) {
                modifiers += "Ctrl+";
            } else {
                text += reversedKeys.get(code);
            }
        }
        if (modifiers.endsWith("+") && text.equals("")) {
            modifiers = modifiers.substring(0, modifiers.length()-1);
        }
        keyString = modifiers+text;
        setText(keyString);
    }

    public String getKeyString() {
        return keyString;
    }

    public void focusGained(FocusEvent e) {
        setText("");
        keyCache.clear();
    }

    public void focusLost(FocusEvent e) {
    }

    private class KeyDocument extends PlainDocument {
        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if (keyString == null || keyString.equals(str)) {
                super.insertString(offs, str, a);
            }
        }
    }
}
