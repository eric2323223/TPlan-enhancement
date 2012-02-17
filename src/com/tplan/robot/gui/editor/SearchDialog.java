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
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.Vector;

/**
 * Dialog allowing to search and replace a text in the given editor
 *
 * @product.signature
 */
public class SearchDialog extends JDialog implements ActionListener, FocusListener, DocumentListener, ItemListener, WindowListener {
    JLabel lblFind = new JLabel(ApplicationSupport.getString("searchDialog.searchFor"));
    JLabel lblReplace = new JLabel(ApplicationSupport.getString("searchDialog.replaceWith"));
    JComboBox cmbFind = new JComboBox();
    JComboBox cmbReplace = new JComboBox();

    JCheckBox chbCaseSensitive = new JCheckBox(ApplicationSupport.getString("searchDialog.matchCase"), false);
    JCheckBox chbBackwards = new JCheckBox(ApplicationSupport.getString("searchDialog.searchBackwards"), false);

    final JButton btnCancel = new JButton(ApplicationSupport.getString("btnCancel"));
    JButton btnFind = new JButton(ApplicationSupport.getString("searchDialog.btnFind"));
    JButton btnReplaceAndFind = new JButton(ApplicationSupport.getString("searchDialog.btnReplaceAndFindNext"));
    JButton btnReplace = new JButton(ApplicationSupport.getString("searchDialog.btnReplace"));
    JButton btnReplaceAll = new JButton(ApplicationSupport.getString("searchDialog.btnReplaceAll"));

    JLabel lblMsg = new JLabel(" ");

    private Vector findElements = null;
    private Point currentSelection = null;

    DefaultComboBoxModel findModel = new DefaultComboBoxModel();
    DefaultComboBoxModel replaceModel = new DefaultComboBoxModel();

    AbstractAction closeAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            btnCancel.doClick();
        }
    };

    MainFrame mainFrame;

    public SearchDialog(MainFrame mainFrame) {
        super(mainFrame, ApplicationSupport.getString("searchDialog.dlgTitle"), false);
        this.mainFrame = mainFrame;
        init();
    }

    private void init() {
        this.addWindowListener(this);

        getRootPane().getActionMap().put("exitdlg", closeAction);
        getRootPane().getInputMap().put(Utils.getKeyStroke("Esc"), "exitdlg");

        JPanel centralPanel = new JPanel(new GridBagLayout());
        centralPanel.add(lblFind, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        centralPanel.add(cmbFind, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        centralPanel.add(lblReplace, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        centralPanel.add(cmbReplace, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        centralPanel.add(chbCaseSensitive, new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        centralPanel.add(chbBackwards, new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        centralPanel.add(lblMsg, new GridBagConstraints(0, 4, 2, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 10, 2, 2), 0, 0));

//        lblMsg.setHorizontalTextPosition();

        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        pnl.add(centralPanel, BorderLayout.CENTER);

        cmbFind.setModel(findModel);
        cmbFind.setEditable(true);
        cmbFind.addFocusListener(this);
        cmbFind.addActionListener(this);
//        cmbFind.getEditor().addActionListener(this);
//        ((JTextField)cmbFind.getEditor().getEditorComponent()).addActionListener(this);

        cmbReplace.setModel(replaceModel);
        cmbReplace.setEditable(true);
        cmbReplace.addFocusListener(this);
        cmbReplace.addActionListener(this);

        ((JTextField) cmbFind.getEditor().getEditorComponent()).getDocument().addDocumentListener(this);
        ((JTextField) cmbReplace.getEditor().getEditorComponent()).getDocument().addDocumentListener(this);

        chbCaseSensitive.addItemListener(this);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnCancel.addActionListener(this);
        btnReplace.addActionListener(this);
        btnReplaceAll.addActionListener(this);
        btnFind.addActionListener(this);
        btnReplaceAndFind.addActionListener(this);
        south.add(btnFind);
        south.add(btnReplaceAndFind);
        south.add(btnReplace);
        south.add(btnReplaceAll);
        south.add(btnCancel);
        getRootPane().setDefaultButton(btnFind);
        Utils.registerDialogForEscape(this, btnCancel);
        pnl.add(south, BorderLayout.SOUTH);
        this.setContentPane(pnl);

//        cmbFind.requestFocus();
//        cmbFind.grabFocus();
//        ((JTextField) cmbFind.getEditor().getEditorComponent()).requestFocus();
//        ((JTextField) cmbFind.getEditor().getEditorComponent()).grabFocus();
//        cmbFind.getEditor().selectAll();
//        btnCancel.grabFocus();
//        btnCancel.requestFocus();
//        btnController();
//        FocusManager fm = FocusManager.getCurrentManager();
//        fm.focusNextComponent(btnCancel);
//        System.out.println(""+fm.getFocusOwner());
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnCancel)) {
            dispose();
        } else if (e.getSource().equals(btnFind)) {
            search(cmbFind.getSelectedItem());
//            search(((JTextField)cmbFind.getEditor().getEditorComponent()).getText());
            updateCmb();
        } else if (e.getSource().equals(btnReplace)) {
            replace(false);
            updateCmb();
        } else if (e.getSource().equals(btnReplaceAndFind)) {
            replace(true);
            updateCmb();
        } else if (e.getSource().equals(btnReplaceAll)) {
            replaceAll(cmbFind.getSelectedItem());
            updateCmb();
        } else if (e.getSource().equals(cmbFind)) {
            changedUpdate(null);
//            updateCmb();
        } else if (e.getSource().equals(cmbReplace)) {
            btnController();
//            updateCmb();
        } else if (e.getSource().equals(cmbFind.getEditor().getEditorComponent())) {
            e.setSource("");
            btnFind.doClick();
        }
    }

    private void updateCmb() {
        String pattern = cmbFind.getSelectedItem() == null ? null : cmbFind.getSelectedItem().toString();
        if (pattern != null && !pattern.equals("")) {
            DefaultComboBoxModel m = (DefaultComboBoxModel)cmbFind.getModel();
            m.removeElement(pattern);
            m.insertElementAt(pattern, 0);
            m.setSelectedItem(pattern);
        }
        String replacement = cmbReplace.getSelectedItem() == null ? null : cmbReplace.getSelectedItem().toString();

        if (replacement != null && !replacement.equals("")) {
            DefaultComboBoxModel m = (DefaultComboBoxModel)cmbReplace.getModel();
            m.removeElement(replacement);
            m.insertElementAt(replacement, 0);
            m.setSelectedItem(replacement);
        }
    }

    private void replaceAll(Object selectedItem) {
        Editor ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel().getEditor();
        boolean caseSensitive = chbCaseSensitive.isSelected();
        Vector v = find(selectedItem.toString(), ed, caseSensitive);
        String replacement = cmbReplace.getSelectedItem() == null ? "" : cmbReplace.getSelectedItem().toString();
        Document doc = ed.getDocument();

        Point p;
        try {
            for (int i = v.size() - 1; i >= 0; i--) {
                p = (Point) v.elementAt(i);
                doc.remove(p.x, p.y);
                doc.insertString(p.x, replacement, null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (v.size() > 0) { 
            currentSelection = null;
            Object args[] = { v.size() };
            lblMsg.setText(MessageFormat.format(ApplicationSupport.getString("searchDialog.replaced"), args));
        } else {
            lblMsg.setText(ApplicationSupport.getString("searchDialog.noMatch"));
        }

    }

    private void replace(boolean findNext) {
        EditorPnl pnl = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
//        String replace = ((JTextField)cmbReplace.getEditor().getEditorComponent()).getText();
        if (pnl != null && currentSelection != null) {
            Document doc = pnl.getEditor().getDocument();
            try {
                String replacement = cmbReplace.getSelectedItem() == null ? "" : cmbReplace.getSelectedItem().toString();
                doc.remove(currentSelection.x, currentSelection.y);
                doc.insertString(currentSelection.x, replacement, null);
                pnl.getEditor().setCaretPosition(currentSelection.x + replacement.length());
                currentSelection = null;

                if (findNext) {
                    search(cmbFind.getSelectedItem());
                } else {
                    lblMsg.setText(ApplicationSupport.getString("searchDialog.oneReplaced"));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void btnController() {
        String pattern = ((JTextField) cmbFind.getEditor().getEditorComponent()).getText();
//        String replace = ((JTextField)cmbReplace.getEditor().getEditorComponent()).getText();
        boolean isPattern = pattern != null && pattern.length() > 0;
//        boolean isReplace = replace != null && replace.length() > 0;

        btnFind.setEnabled(isPattern);
        btnReplace.setEnabled(isPattern && currentSelection != null);
        btnReplaceAndFind.setEnabled(btnReplace.isEnabled());
        btnReplaceAll.setEnabled(isPattern);
    }

    private void search(Object selectedItem) {
        EditorPnl pnl = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
        Editor ed = pnl.getEditor();
        boolean caseSensitive = chbCaseSensitive.isSelected();
        findElements = find(selectedItem.toString(), ed, caseSensitive);
        Point next = getNext(ed, findElements);
//        System.out.println("found "+findElements.size()+" elements, next elem = "+next);
        if (next != null) {
//            Element e = ed.getElementForOffset(next.x);
            ed.scrollOffsetRectToVisible(next);
            ed.setCaretPosition(next.x);
            ed.select(next.x, next.x + next.y);
            currentSelection = next;
            int i = findElements.indexOf(currentSelection) + 1;
            Object args[] = { i, findElements.size() };
            lblMsg.setText(MessageFormat.format(ApplicationSupport.getString("searchDialog.displayedOccurence"), args));
        } else {
            // TODO: better messaging, 'no more ... found' etc.
            if (findElements.size() > 0) {
                if (chbBackwards.isSelected()) {
                    lblMsg.setText(ApplicationSupport.getString("searchDialog.beginningReached"));
                } else {
                    lblMsg.setText(ApplicationSupport.getString("searchDialog.endReached"));
                }
            } else {
                lblMsg.setText(ApplicationSupport.getString("searchDialog.noMatch"));
            }
        }
        btnController();
    }

    /**
     * Specify which next found element
     *
     * @param editor
     * @param v
     * @return
     */
    private Point getNext(Editor editor, Vector v) {
        boolean searchBackwards = chbBackwards.isSelected();

        int caretPos = editor.getCaretPosition();
        if (currentSelection != null) {
            caretPos = searchBackwards ? currentSelection.x : currentSelection.x + currentSelection.y;
        }

        Point point;
        Point next = null;
        for (int i = 0; i < v.size(); i++) {
            point = (Point) v.elementAt(i);
            if (point.x >= caretPos) {
                if (!searchBackwards) {
                    next = point;
                }
                break;
            }
            if (searchBackwards && point.x + point.y < caretPos) {
                next = point;
            }
        }

        return next;
    }

    private Vector find(String pattern, Editor ed, boolean caseSensitive) {
        Vector v = new Vector();

        try {
            int start = 0;
            int end = ed.getDocument().getLength();
            String txt = ed.getDocument().getText(start, end);
            if (!caseSensitive) {
                txt = txt.toLowerCase();
                pattern = pattern.toLowerCase();
            }
            int pos = 0, i = -1;
            do {
                i = txt.indexOf(pattern, pos);
                if (i >= 0) {
                    Point p = new Point(i + start, pattern.length());
                    v.add(p);
                    pos = i + pattern.length();
                }
            } while (i >= 0);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return v;
    }

    public void focusGained(FocusEvent e) {
        if (e.getSource() instanceof JTextField) {
            ((JTextField) e.getSource()).selectAll();
        }
    }

    public void focusLost(FocusEvent e) {
    }

    public void changedUpdate(DocumentEvent e) {
        btnController();
    }

    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public void itemStateChanged(ItemEvent e) {
        changedUpdate(null);
    }

    public void windowActivated(WindowEvent e) {
        getRootPane().setDefaultButton(btnFind);
    }

    public void windowClosed(WindowEvent e) {
        currentSelection = null;
        lblMsg.setText(" ");
    }

    public void windowClosing(WindowEvent e) {

    }

    public void windowDeactivated(WindowEvent e) {

    }

    public void windowDeiconified(WindowEvent e) {

    }

    public void windowIconified(WindowEvent e) {

    }

    public void windowOpened(WindowEvent e) {
        currentSelection = null;
        lblMsg.setText(" ");
        getRootPane().setDefaultButton(btnFind);
        cmbFind.requestFocus();
        cmbFind.getEditor().getEditorComponent().requestFocus();
    }

    public void setCurrentSelection() {
        Editor ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel().getEditor();
        String pattern = ed.getSelectedText();
        if (pattern != null && !pattern.equals("")) {
            DefaultComboBoxModel m = (DefaultComboBoxModel)cmbFind.getModel();
            m.removeElement(pattern);
            m.insertElementAt(pattern, 0);
            m.setSelectedItem(pattern);
            this.currentSelection = new Point(ed.getSelectionStart(), ed.getSelectionEnd()-ed.getSelectionStart());
            ed.scrollOffsetRectToVisible(currentSelection);
        }
    }

}
