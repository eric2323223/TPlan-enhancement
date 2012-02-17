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

import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.commands.CommandHandler;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.gui.dialogs.ScreenshotDialog;
import com.tplan.robot.scripting.ScriptingContext;

import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * A document for JTextField or an editable JComboBox capable of validation of
 * a {@product.name} scripting language command.
 * @product.signature
 */
public class CustomFilteredDocument extends FilteredDocument {
    CommandHandler cmd;
    Object owner;
    private ScriptingContext context;

    public CustomFilteredDocument(JTextField field, Object owner, ScriptManager handler, CommandHandler command) {
        super(field);
        this.owner = owner;
        this.cmd = command;
        context = handler.createDefaultContext();
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
     *         otherwise.
     */
    public boolean isContentsCorrect(String text) {
        if (field.isEnabled() && owner != null) {
            String imgName = "";
            if (owner instanceof ScreenshotDialog) {
                imgName = ((ScreenshotDialog) owner).getImageName();
            } else {
                imgName = field.getText();
            }
            String s = cmd.getCommandNames()[0] + " \"" + imgName + "\"";
//            System.out.println("command: "+s);
            boolean ok = true;
            try {
                TokenParser parser = context.getParser();
                List v = new ArrayList(10);
                Map t = parser.parseParameters(s, v);
                cmd.validate(v, t, new HashMap(), context);
            } catch (SyntaxErrorException ex) {
                setErrorToolTipText(ex.getMessage());
                ok = false;
            }
            return !valueCorrectVeto && ok;
        }
        return true;
    }

    /**
     * @param context the context to set
     */
    public void setContext(ScriptingContext context) {
        this.context = context;
    }
}