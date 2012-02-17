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
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.gui.*;
import com.tplan.robot.scripting.ScriptListener;
import com.tplan.robot.util.Utils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Dialog showing the list of variables defined in the execution context.
 *
 * REPLACED BY ANOTHER COMPONENT IN 2.0 - PENDING REMOVAL.
 * 
 * @product.signature
 */
public class VariableBrowserDialog extends JDialog
        implements ActionListener, ListSelectionListener, ScriptListener {

    JTable table = new JTable();
    JButton btnClose = new JButton(ApplicationSupport.getString("variableBrowserDialog.close"));
    JButton btnCopy = new JButton(ApplicationSupport.getString("variableBrowserDialog.copyNameAndClose"));
    JButton btnCopyValue = new JButton(ApplicationSupport.getString("variableBrowserDialog.copyValueAndClose"));

    String lastSelectedKey = null;

    MapTableModel tm;

    boolean columnWidthManuallyUpdated = false;

    public VariableBrowserDialog(MainFrame owner, String title, boolean modal) {
        super(owner, title, modal);
        initDlg();
        owner.getScriptHandler().addScriptListener(this);
    }

    private void initDlg() {
        Utils.registerDialogForEscape(this, btnClose);
        JPanel pnlContent = new JPanel();
        pnlContent.setLayout(new BorderLayout());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(500, 300));
        pnlContent.add(scroll, BorderLayout.CENTER);
        this.setContentPane(pnlContent);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnClose.addActionListener(this);
        south.add(btnCopy);
        btnCopy.addActionListener(this);
        south.add(btnCopyValue);
        btnCopyValue.addActionListener(this);
        south.add(btnClose);
        getRootPane().setDefaultButton(btnClose);
        pnlContent.add(south, BorderLayout.SOUTH);
        table.getSelectionModel().addListSelectionListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final String columns[] = {
            ApplicationSupport.getString("variableBrowserDialog.columnVariableName"),
            ApplicationSupport.getString("variableBrowserDialog.columnValue")
        };
        tm = new MapTableModel(getVarMap(), columns, true);
        table.setModel(tm);

        valueChanged(null);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);
    }

    public Map getVarMap() {
//        Map vars = new HashMap();
//        TestWrapper wr = repository.getMasterWrapper();
//        while (wr != null && wr instanceof DocumentWrapper) {
//            DocumentWrapper w = (DocumentWrapper)wr;
//            if (w.getLocalVariables() != null) {
//                vars.putAll(w.getLocalVariables());
//            }
//            wr = wr.getParentWrapper();
//        }
//        vars.putAll(repository.getVariables());
//
//        // Add the variables passed from CLI and mark them as "fixed"
//        Map cli = ((MainFrame) getOwner()).getScriptHandler().getCliVariables();
//        Iterator e = cli.keySet().iterator();
//        Object key;
//        while (e.hasNext()) {
//            key = e.next();
//            vars.remove(key);
//            vars.put(MessageFormat.format(ApplicationSupport.getString("variableBrowserDialog.fixedVariable"), key), cli.get(key));
//        }
        return null;
    }

    public void refresh() {
        // Create a new model
//        String selectedVar = table.getSelectedRow() >= 0 ? (String)table.getValueAt(table.getSelectedRow(), 0) : null;
        int selectedIndex = tm.getRowForVariable(lastSelectedKey);
        tm.refresh(getVarMap());
//        System.out.println("selected key = "+selectedVar+", index = "+selectedIndex);
        if (selectedIndex >= 0) {
            table.changeSelection(selectedIndex, 0, true, false);
        }
        updateColumnWidth();
    }

    private synchronized void updateColumnWidth() {
        if (tm == null || tm.getColumnCount() < 1 || columnWidthManuallyUpdated) {
            return;
        }
        TableColumn tc = table.getColumnModel().getColumn(0);
        Object o;
        int width = 0;
        TableCellRenderer r = table.getCellRenderer(0, 0);
        if (r != null && r instanceof Component) {
            Component c = (Component) r;
            FontMetrics fm = c.getFontMetrics(c.getFont());
            for (int i = 0; i < tm.getRowCount(); i++) {
                o = tm.getValueAt(i, 0);
                if (o != null) {
                    width = Math.max(width, fm.stringWidth(o.toString()));
                }
            }
            width += 6;
            if (width > tc.getWidth()) {
                tc.setMinWidth(width);
            }
            tc.setPreferredWidth(width);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnCopy)) {
            int x = table.getSelectedRow();
            String value = "{" + table.getValueAt(x, 0).toString() + "}";
            JTextField f = new JTextField(value);
            f.selectAll();
            f.copy();
            dispose();
        } else if (e.getSource().equals(btnCopyValue)) {
            int x = table.getSelectedRow();
            String value = table.getValueAt(x, 1).toString();
            JTextField f = new JTextField(value);
            f.selectAll();
            f.copy();
            dispose();
        } else if (e.getSource().equals(btnClose)) {
            dispose();
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        if (table.getSelectedRow() >= 0) {
            btnCopy.setEnabled(true);
            btnCopyValue.setEnabled(true);
            lastSelectedKey = table.getSelectedRow() >= 0 ? (String) table.getValueAt(table.getSelectedRow(), 0) : null;
        } else {
            btnCopy.setEnabled(false);
            btnCopyValue.setEnabled(false);
        }
    }

    public void scriptEvent(ScriptEvent event) {
        if (isShowing() && event.getType() == ScriptEvent.SCRIPT_VARIABLES_UPDATED) {
            refresh();
        }
    }

    private class MapTableModel extends AbstractTableModel {
        Map t;
        private Object keys[];
        String columnNames[];
        boolean sort;

        MapTableModel(Map t, String columnNames[], boolean sort) {
            this.t = t;
            this.columnNames = columnNames;
            this.sort = sort;
            if (t != null) {
                this.keys = (new ArrayList(t.keySet())).toArray();
                if (sort) {
                    Arrays.sort(keys);
                }
            }
        }

        public synchronized void refresh(Map t) {
            this.t = t;
            if (t != null) {
                this.keys = (new ArrayList(t.keySet())).toArray();
                if (sort) {
                    Arrays.sort(keys);
                }
            }
            fireTableDataChanged();
        }

        public String getColumnName(int column) {
            return columnNames[column].toString();
        }

        public synchronized int getRowCount() {
            return t == null ? 0 : t.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public synchronized Object getValueAt(int row, int col) {
            if (t != null && keys != null && row < keys.length) {
                return col == 0 ? keys[row] : t.get(keys[row]);
            }
            return null;
        }

        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public void setValueAt(Object value, int row, int col) {
        }

        public synchronized int getRowForVariable(String variable) {
            if (keys != null && keys.length > 0 && variable != null) {
                for (int i = 0; i < keys.length; i++) {
                    if (variable.equals(keys[i])) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }

}
