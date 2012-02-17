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

import com.tplan.robot.scripting.TokenParser;

import com.tplan.robot.scripting.TokenParserImpl;
import javax.swing.*;
import java.awt.*;

/**
 * <p>Custom document for a JTextField or editable JComboBox allowing to display
 * and validate rectangle coordinates in the form used by the {@doc.spec},
 * especially in parameters <code>area</code> and <code>cmparea</code>
 * of the {@doc.cmd Screenshot}, {@doc.cmd Compareto} and {@doc.cmd Waitfor}
 * commands.</p>
 *
 * @product.signature
 */
public class RectangleDocument extends FilteredDocument {

    String paramName;
    static TokenParser parser = new TokenParserImpl();

    /**
     * Constructor for JTextField.
     * @param field a JTextField instance.
     * @param paramName rectangle parameter name as is defined in the {@doc.spec}.
     */
     public RectangleDocument(JTextField field, String paramName) {
        super(field);
        this.paramName = paramName;
    }

    /**
     * Constructor for JTextField.
     * @param field a JTextField instance.
     * @param toolTipText tool tip message for the field.
     * @param errorToolTipText tool tip message to be displayed when the
     * field contains an invalid value.
     * @param paramName rectangle parameter name as is defined in the {@doc.spec}.
     */
     public RectangleDocument(JTextField field, String toolTipText, String errorToolTipText, String paramName) {
        super(field, toolTipText, errorToolTipText);
        this.paramName = paramName;
    }

    /**
     * Constructor for JComboBox.
     * @param comboBox a JComboBox instance.
     * @param paramName rectangle parameter name as is defined in the {@doc.spec}.
     */
    public RectangleDocument(JComboBox comboBox, String paramName) {
        super(comboBox);
        this.paramName = paramName;
    }

    /**
     * Constructor for JComboBox.
     * @param comboBox a JComboBox instance.
     * @param toolTipText tool tip message for the field.
     * @param errorToolTipText tool tip message to be displayed when the
     * @param paramName rectangle parameter name as is defined in the {@doc.spec}.
     */
    public RectangleDocument(JComboBox comboBox, String toolTipText, String errorToolTipText, String paramName) {
        super(comboBox, toolTipText, errorToolTipText);
        this.paramName = paramName;
    }

    /**
     * Validate content of the document.
     * @param text document text to validate.
     * @return true if the text represents a valid rectangle, false otherwise.
     */
    public boolean isContentsCorrect(String text) {
        boolean ok = true;
        try {
            Rectangle r = parser.parseRectangle(text, paramName);
        } catch (Exception e) {
            setErrorToolTipText(e.getMessage());
            ok = false;
        }
        return !valueCorrectVeto && ok;
    }
}
