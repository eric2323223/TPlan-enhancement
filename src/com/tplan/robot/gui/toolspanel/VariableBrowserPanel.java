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
package com.tplan.robot.gui.toolspanel;

import com.tplan.robot.gui.components.MapTableModel;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.scripting.DocumentWrapper;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.gui.*;
import com.tplan.robot.gui.editor.Editor;
import com.tplan.robot.gui.editor.EditorPnl;
import com.tplan.robot.scripting.ScriptListener;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.TestWrapper;

import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.util.Utils;
import java.beans.PropertyChangeEvent;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import javax.swing.text.BadLocationException;

/**
 * Dialog showing the list of variables defined in the execution context.
 * @product.signature
 */
public class VariableBrowserPanel extends JPanel
        implements ActionListener, ListSelectionListener, ScriptListener, PropertyChangeListener {

    JTable table = new JTable();
    JButton btnCopyNameToMem = new JButton(ApplicationSupport.getImageIcon("copyName16.gif"));
    JButton btnCopyValueToMem = new JButton(ApplicationSupport.getImageIcon("copyValue16.gif"));
    JButton btnCopyNameToEd = new JButton(ApplicationSupport.getImageIcon("varName16.gif"));
    JButton btnCopyValueToEd = new JButton(ApplicationSupport.getImageIcon("varValue16.gif"));
    JButton btnHelp = new JButton(ApplicationSupport.getImageIcon("help16.gif"));
    JTextField dummyField = new JTextField();
    String lastSelectedKey = null;
    MapTableModel tm;
    boolean columnWidthManuallyUpdated = false;
    MainFrame mainFrame;
    JPanel defaultPanel = new JPanel();
    JScrollPane scroll;
    JTextArea jta;
    ScriptingContext context;

    public VariableBrowserPanel(MainFrame owner) {
        this.mainFrame = owner;
        init();
        owner.getScriptHandler().addScriptListener(this);
    }

    void enableControls() {
        // At the moment we leave all controls enabled at all times
    }

    private void init() {
        setLayout(new BorderLayout());

        jta = new JTextArea(ApplicationSupport.getString("toolsPanel.varsPanel.noVarsAvailable"));
        jta.setWrapStyleWord(true);
        jta.setEditable(false);
        jta.setOpaque(false);
//        jta.setPreferredSize(new Dimension(10, 10));
        defaultPanel.add(jta, BorderLayout.CENTER);

        scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(500, 300));

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.add(btnCopyNameToMem);
        btnCopyNameToMem.addActionListener(this);
        btnCopyNameToMem.setToolTipText(ApplicationSupport.getString("toolsPanel.varsPanel.copyNameToMem"));
        btnCopyNameToMem.setMargin(new Insets(0, 0, 0, 0));
        south.add(btnCopyValueToMem);
        btnCopyValueToMem.addActionListener(this);
        btnCopyValueToMem.setToolTipText(ApplicationSupport.getString("toolsPanel.varsPanel.copyValueToMem"));
        btnCopyValueToMem.setMargin(new Insets(0, 0, 0, 0));
        south.add(btnCopyNameToEd);
        btnCopyNameToEd.addActionListener(this);
        btnCopyNameToEd.setToolTipText(ApplicationSupport.getString("toolsPanel.varsPanel.copyNameToEd"));
        btnCopyNameToEd.setMargin(new Insets(0, 0, 0, 0));
        south.add(btnCopyValueToEd);
        btnCopyValueToEd.addActionListener(this);
        btnCopyValueToEd.setToolTipText(ApplicationSupport.getString("toolsPanel.varsPanel.copyValueToEd"));
        btnCopyValueToEd.setMargin(new Insets(0, 0, 0, 0));
        south.add(btnHelp);
        btnHelp.addActionListener(this);
        btnHelp.setToolTipText(ApplicationSupport.getString("toolsPanel.varsPanel.help"));
        btnHelp.setMargin(new Insets(0, 0, 0, 0));

        add(south, BorderLayout.SOUTH);
        table.getSelectionModel().addListSelectionListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final String columns[] = {
            ApplicationSupport.getString("variableBrowserDialog.columnVariableName"),
            ApplicationSupport.getString("variableBrowserDialog.columnValue")
        };
        tm = new MapTableModel(getVarMap(true), columns, true);
        table.setModel(tm);

        valueChanged(null);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);

        defaultPanel.setOpaque(true);
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.add(scroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));
        pnl.add(defaultPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));
        add(pnl, BorderLayout.CENTER);
    }

    @Override
    public void setVisible(boolean visible) {
        editorChanged();
        super.setVisible(visible);
    }

    public Map getVarMap(Boolean useCompileContext) {
        Map vars = new Hashtable();

        EditorPnl pnl = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();

        if (useCompileContext == null) { // Decide which context to use. Use compile context only if there's no execution one.
            useCompileContext = pnl == null || pnl.getTestScript() == null || pnl.getTestScript().getExecutionContext() == null;
        }

        if (pnl != null) {
            ScriptingContext ctx = useCompileContext
                    ? pnl.getTestScript().getCompilationContext()
                    : pnl.getTestScript().getExecutionContext();

            if (ctx != null) {
                TestWrapper wr = ctx.getMasterWrapper();
                while (wr != null && wr instanceof DocumentWrapper) {
                    DocumentWrapper w = (DocumentWrapper) wr;
                    if (w.getLocalVariables() != null) {
                        Utils.putAll(vars, w.getLocalVariables());
                    }
                    wr = wr.getParentWrapper();
                }
                Utils.putAll(vars, ctx.getVariables());
            }

            if (context == null || !context.equals(ctx)) {
                if (context != null) {
                    context.getVariables().removePropertyChangeListener(this);
                }
                if (ctx != null) {
                    ctx.getVariables().addPropertyChangeListener(this);
                }
                context = ctx;
            }
        }

        // Add the variables passed from CLI and mark them as "fixed"
        Map cli = mainFrame.getScriptHandler().getCliVariables();
        Iterator e = cli.keySet().iterator();
        Object key;
        while (e.hasNext()) {
            key = e.next();
            vars.remove(key);
            vars.put(MessageFormat.format(ApplicationSupport.getString("variableBrowserDialog.fixedVariable"), key), cli.get(key));
        }
        return vars;
    }

    public void refresh(Boolean useCompileContext) {
        int selectedIndex = tm.getRowForVariable(lastSelectedKey);
        tm.refresh(getVarMap(useCompileContext));
        if (selectedIndex >= 0) {
            table.changeSelection(selectedIndex, 0, true, false);
        }
        if (tm.getRowCount() > 0) {
            scroll.setVisible(true);
            defaultPanel.setVisible(false);
            updateColumnWidth();
        } else {
            defaultPanel.setVisible(true);
            scroll.setVisible(false);
        }
        revalidate();
        repaint();
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
        String value;
        final int x = table.getSelectedRow();
        final EditorPnl ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
        if (e.getSource().equals(btnCopyNameToMem)) {   // Copy variable name to clipboard
            value = table.getValueAt(x, 0).toString();

            // If the test script is a proprietary one, add the enclosing braces
            if (ed != null && ed.getTestScript() != null && ed.getTestScript().getType() == TestScriptInterpret.TYPE_PROPRIETARY) {
                value = "{" + value + "}";
            }

            // Copy the text to the clipboard using an internal JTextField instance
            dummyField.setText(value);
            dummyField.selectAll();
            dummyField.copy();

        } else if (e.getSource().equals(btnCopyValueToMem)) {  // Copy variable value to clipboard
            value = table.getValueAt(x, 1).toString();

            // Copy the text to the clipboard using an internal JTextField instance
            dummyField.setText(value);
            dummyField.selectAll();
            dummyField.copy();
        } else if (e.getSource().equals(btnCopyNameToEd)) {  // Copy variable name to editor
            value = table.getValueAt(x, 0).toString();
            if (ed != null) {
                Editor edt = ed.getEditor();
                int cp = edt.getCaretPosition();
                try {
                    if (cp == 0 || !edt.getText(cp - 1, 1).equals("{")) {
                        value = "{" + value;
                    }
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
                try {
                    if (cp >= edt.getDocument().getLength() || !edt.getText(cp, 1).equals("}")) {
                        value = value + "}";
                    }
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
                try {
                    edt.getDocument().insertString(cp, value, null);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        } else if (e.getSource().equals(btnCopyValueToEd)) {  // Copy variable value to editor
            value = table.getValueAt(x, 1).toString();
            if (ed != null) {
                Editor edt = ed.getEditor();
                try {
                    edt.getDocument().insertString(edt.getCaretPosition(), value, null);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        } else if (e.getSource().equals(btnHelp)) {
            mainFrame.showHelpDialog("scripting.commref_var", mainFrame);
        }
    }

    /**
     * This ListSelectionListener method updates button status when the table
     * selection changes.
     * @param e a list selection event originating from the table selection model.
     */
    public void valueChanged(ListSelectionEvent e) {
        boolean isSelection = table.getSelectedRow() >= 0;
        if (isSelection) {
            lastSelectedKey = table.getSelectedRow() >= 0 ? (String) table.getValueAt(table.getSelectedRow(), 0) : null;
        }
        btnCopyNameToMem.setEnabled(isSelection);
        btnCopyValueToMem.setEnabled(isSelection);
        btnCopyNameToEd.setEnabled(isSelection);
        btnCopyValueToEd.setEnabled(isSelection);
        btnHelp.setEnabled(isSelection && lastSelectedKey != null && lastSelectedKey.startsWith("_") && MainFrame.getHelpBroker() != null);
    }

    void editorChanged() {
        EditorPnl ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
        if (ed != null) {
            jta.setText(MessageFormat.format(
                    ApplicationSupport.getString("toolsPanel.varsPanel.noVarsAvailable"),
                    mainFrame.getDocumentTabbedPane().getTitleAt(mainFrame.getDocumentTabbedPane().getSelectedIndex())));
        } else {
            jta.setText(ApplicationSupport.getString("toolsPanel.varsPanel.noEditorAvailable"));
        }

        refresh(null);
    }

    public void scriptEvent(ScriptEvent event) {
        int type = event.getType();
        if (isVisible()) {
            if (type == ScriptEvent.SCRIPT_COMPILATION_FINISHED || type == ScriptEvent.SCRIPT_EXECUTION_STARTED) {
                EditorPnl ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
                if (ed != null && ed.getTestScript() != null && (event.getContext().equals(ed.getTestScript().getCompilationContext())) || event.getContext().equals(ed.getTestScript().getExecutionContext())) {
                    refresh(null);
                }
            }
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (isVisible()) {
            refresh(null);
        }
    }
}
