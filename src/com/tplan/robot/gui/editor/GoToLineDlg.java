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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.util.Utils;
import com.tplan.robot.gui.MainFrame;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;

/**
 * Simple dialog allowing to jump to an editor line of particular number.
 *
 * @product.signature
 */
public class GoToLineDlg extends JDialog implements ActionListener, DocumentListener, ChangeListener {
    JLabel lblComment = new JLabel(" ");
    JLabel lblGo = new JLabel(ApplicationSupport.getString("goToLineDlg.goToLine"));
    JSpinner spinner = new JSpinner();
    JButton btnCancel = new JButton(ApplicationSupport.getString("btnCancel"));
    JButton btnOK = new JButton(ApplicationSupport.getString("btnOk"));

    MainFrame mainFrame;

    public GoToLineDlg(MainFrame mainFrame) {
        super(mainFrame, ApplicationSupport.getString("goToLineDlg.dlgTitle"), true);
        this.mainFrame = mainFrame;
        init();
    }

    private void init() {
        JPanel centralPanel = new JPanel(new GridBagLayout());
        centralPanel.add(lblComment, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        centralPanel.add(lblGo, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        centralPanel.add(spinner, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));

        spinner.addChangeListener(this);
        reset();

        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        pnl.add(centralPanel, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnOK.addActionListener(this);
        btnCancel.addActionListener(this);
        south.add(btnOK);
        south.add(btnCancel);
        getRootPane().setDefaultButton(btnOK);
        pnl.add(south, BorderLayout.SOUTH);
        this.setContentPane(pnl);
        Utils.registerDialogForEscape(this, btnCancel);
    }

    public void actionPerformed(ActionEvent e) {
//        System.out.println("actionEvent");
        if (e.getSource().equals(btnCancel)) {
            dispose();
        } else if (e.getSource().equals(btnOK)) {
//            System.out.println("OK");
            gotoLine();
            dispose();
        } else if (e.getSource() instanceof JFormattedTextField) {
            btnOK.doClick();
        }
    }

    private void gotoLine() {
        JSpinner.DefaultEditor de = (JSpinner.DefaultEditor)spinner.getEditor();
        JFormattedTextField te = (de).getTextField();
        te.setFocusLostBehavior(JFormattedTextField.PERSIST);
        String txt = te.getText();

        Number value = null;
        if (txt != null) {
            try {
                value = new Integer(txt);
            } catch (Exception ex) {
                return;
            }
        }

//        System.out.println("Going to line "+value.intValue());
        if (value != null && value instanceof Number) {
            int line = ((Number)value).intValue() - 1;
            Editor ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel().getEditor();
            Element e = ed.getDocument().getDefaultRootElement().getElement(line);
            ed.setCaretPosition(e.getStartOffset());
            ed.scrollElementRectToVisible(e);
        }
    }

    public void reset() {
        Editor ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel().getEditor();
        int value = 1;
        int max = ed.getLineCount();
        if (spinner.getValue() != null && spinner.getValue() instanceof Number) {
            value = ((Number)spinner.getValue()).intValue();
            if (value < 1 || value > max) {
                value = 1;
            }
        }
        SpinnerNumberModel m = new SpinnerNumberModel(value, 1, max, 1);
        spinner.setModel(m);
        Object args[] = { max };
        lblComment.setText(MessageFormat.format(ApplicationSupport.getString("goToLineDlg.warningMaxLines"), args));

        JSpinner.NumberEditor edit = (JSpinner.NumberEditor) spinner.getEditor();
        edit.getTextField().getDocument().addDocumentListener(this);
        edit.getTextField().addActionListener(this);
        edit.getTextField().setText(spinner.getValue().toString());
        getRootPane().setDefaultButton(btnOK);
    }

    public void stateChanged(ChangeEvent e) {
        JSpinner.DefaultEditor de = (JSpinner.DefaultEditor)spinner.getEditor();
        JFormattedTextField te = (de).getTextField();
        if (e != null) {
            te.setText(spinner.getValue().toString());
            return;
        }
        Editor ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel().getEditor();
        int max = ed.getLineCount();
        boolean enable = false;

        te.setFocusLostBehavior(JFormattedTextField.PERSIST);
        String value = te.getText();

        if (value != null) {
            try {
                int line = ((new Integer(value))).intValue();
                enable = line >= 1 && line <= max;
//                System.out.println("line = "+line+", enable = "+enable);
            } catch (Exception ex) {
            }
        }

        if (/* !"".equals(value) && */ btnOK.isEnabled() != enable) {
//            System.out.println("Setting enabled to "+enable);
            btnOK.setEnabled(enable);
        }
    }

    public void changedUpdate(DocumentEvent e) {
        stateChanged(null);
    }

    public void insertUpdate(DocumentEvent e) {
//        System.out.println("insert");
        stateChanged(null);
    }

    public void removeUpdate(DocumentEvent e) {
//        System.out.println("remove");
        stateChanged(null);
    }
}
