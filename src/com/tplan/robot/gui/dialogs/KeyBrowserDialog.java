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
package com.tplan.robot.gui.dialogs;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.gui.editor.KeyTextField;
import com.tplan.robot.util.Utils;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Map;

/**
 * Dialog showing key identifiers supported by the current Java versions.
 * @product.signature
 */
public class KeyBrowserDialog extends JDialog implements ActionListener, ListSelectionListener, PropertyChangeListener, WindowListener {

    JTable table = new JTable();
    JButton btnClose = new JButton(ApplicationSupport.getString("keyBrowserDialog.btnClose"));
    JButton btnCopy = new JButton(ApplicationSupport.getString("keyBrowserDialog.btnCopyAndClose"));
    JButton btnHelp = new JButton(ApplicationSupport.getString("btnHelp"));
    KeyTextField txtKey = new KeyTextField();

    public KeyBrowserDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        initDlg();
    }

    private void initDlg() {
        addWindowListener(this);
        JPanel centralPanel = new JPanel(new BorderLayout());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(450, 350));
        centralPanel.add(scroll, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(txtKey, BorderLayout.SOUTH);
        txtKey.addPropertyChangeListener(this);
        JLabel lbl = new JLabel(ApplicationSupport.getString("keyBrowserDialog.fieldLabel"));
        southPanel.add(lbl, BorderLayout.CENTER);
        centralPanel.add(southPanel, BorderLayout.SOUTH);

        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        pnl.add(centralPanel, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnCopy.addActionListener(this);
        south.add(btnCopy);
        btnClose.addActionListener(this);
        south.add(btnClose);
        btnHelp.addActionListener(this);
        south.add(btnHelp);
        getRootPane().setDefaultButton(btnClose);
        pnl.add(south, BorderLayout.SOUTH);
        table.getSelectionModel().addListSelectionListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        valueChanged(null);
        this.setContentPane(pnl);
        Utils.registerDialogForEscape(this, btnClose);
    }

    public void setValues(Map t, String columns[], boolean sort) {
        table.setModel(new MapTableModel(t, columns, sort));
        txtKey.setValues(t);
    }

    public void refresh() {
        if (table.getModel() instanceof MapTableModel) {
            MapTableModel tm = (MapTableModel)table.getModel();
            tm.refresh();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnCopy)) {
            String value;
            if (txtKey.getText() != null && !txtKey.getText().trim().equals("")) {
                value = txtKey.getText();
            } else {
                int x = table.getSelectedRow();
                int y = table.getSelectedColumn();
                value = table.getValueAt(x, y).toString();
            }
            JTextField f = new JTextField(value);
            f.selectAll();
            f.copy();
            dispose();
        } else if (e.getSource().equals(btnClose)) {
            dispose();
        } else if (e.getSource().equals(btnHelp)) {
            MainFrame.getInstance().showHelpDialog("gui.keybrowser", this);
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        if (table.getSelectedRow() >= 0) {
            btnCopy.setEnabled(true);
        } else {
            btnCopy.setEnabled(txtKey.getText() != null && !txtKey.getText().trim().equals(""));
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("keyPressed")) {
            btnCopy.setEnabled(true);
        }
    }

    public void windowActivated(WindowEvent e) {
        txtKey.requestFocus();
    }

    public void windowClosed(WindowEvent e) {

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

    }

    private class MapTableModel extends AbstractTableModel {
        Map t;
        private Object keys[];
        String columnNames[];
        boolean sort;

        MapTableModel(Map t, String columnNames[], boolean sort) {
            this.t = t;
            this.columnNames = columnNames;
            this.keys = (new ArrayList(t.keySet())).toArray();
            this.sort = sort;
            if (sort) {
                Arrays.sort(keys);
            }
        }

        public String getColumnName(int column) {
            return columnNames[column].toString();
        }
        public int getRowCount() {
            return t.size();
        }
        public int getColumnCount() {
            return 2;
        }
        public Object getValueAt(int row, int col) {
            return col == 0 ? keys[row] : t.get(keys[row]);
        }
        public boolean isCellEditable(int row, int column) {
            return false;
        }
        public void setValueAt(Object value, int row, int col) {
        }

        public void refresh() {
            this.keys = (new ArrayList(t.keySet())).toArray();
            fireTableDataChanged();
        }
    }

}
