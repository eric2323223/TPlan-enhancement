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


import javax.swing.*;

/**
 * <p>Custom document for a JTextField or editable JComboBox allowing to display
 * and validate percentage values.</p>
 *
 * @product.signature
 */
public class PercentageDocument extends FilteredDocument {

    /**
     * Constructor for JTextField.
     * @param field a JTextField instance.
     */
    public PercentageDocument(JTextField field) {
        super(field);
    }

    /**
     * Constructor for JTextField.
     * @param field a JTextField instance.
     * @param toolTipText tool tip message for the field.
     * @param errorToolTipText tool tip message to be displayed when the
     * field contains an invalid value.
     */
    public PercentageDocument(JTextField field, String toolTipText, String errorToolTipText) {
        super(field, toolTipText, errorToolTipText);
    }

    /**
     * Constructor for JComboBox.
     * @param comboBox a JComboBox instance.
     */
    public PercentageDocument(JComboBox comboBox) {
        super(comboBox);
    }

    /**
     * Constructor for JComboBox.
     * @param comboBox a JComboBox instance.
     * @param toolTipText tool tip message for the field.
     * @param errorToolTipText tool tip message to be displayed when the
     * field contains an invalid value.
     */
    public PercentageDocument(JComboBox comboBox, String toolTipText, String errorToolTipText) {
        super(comboBox, toolTipText, errorToolTipText);
    }

    /**
     * Validate content of the document.
     * @param text document text to validate.
     * @return true if the text represents a valid value, false otherwise.
     */
    @Override
    public boolean isContentsCorrect(String text) {
        boolean ok = true;
        try {
            float d = Float.parseFloat(text);
            if (d < 0.0 || d > 100.0) {
                ok = false;
            }
        } catch (Exception e) {
            ok = false;
        }
        return !valueCorrectVeto && ok;
    }
}
