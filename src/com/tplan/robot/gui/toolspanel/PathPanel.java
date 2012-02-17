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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.editor.Editor;
import com.tplan.robot.gui.editor.EditorPnl;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.plugin.PluginInfo;
import com.tplan.robot.scripting.RecordingModule;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptListener;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.ScriptingContextImpl;
import com.tplan.robot.scripting.commands.impl.ReportCommand;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import com.tplan.robot.scripting.report.ReportProvider;
import com.tplan.robot.scripting.report.ReportProviderFactory;
import com.tplan.robot.util.DocumentUtils;
import com.tplan.robot.util.Utils;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.swing.border.TitledBorder;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;

/**
 * Panel with output and template directory paths and a Report checkbox.
 * @product.signature
 */
public class PathPanel extends JPanel implements ActionListener, ItemListener,
        DocumentListener, PropertyChangeListener, ScriptListener {

    ResourceBundle res = ApplicationSupport.getResourceBundle();
    JLabel lblReportPath = new JLabel();
    JLabel lblReportFile = new JLabel();
    JLabel lblTemplatePath = new JLabel();
    JLabel lblReportDesc = new JLabel();
    JCheckBox chbReport = new JCheckBox();
    JTextField txtReportPath = new JTextField();
    JTextField txtReportFile = new JTextField();
    JTextField txtReportDesc = new JTextField();
    JTextField txtTemplatePath = new JTextField();
    JButton btnReportPath = new JButton();
    JButton btnTemplatePath = new JButton();
    RecordingModule recordingModule;
    ScriptingContext context;
    final String REPORT_DIR_PATTERN = "^[ ]*[Vv][Aa][Rr][ ].*_REPORT_DIR=.*";
    final String TEMPLATE_DIR_PATTERN = "^[ ]*[Vv][Aa][Rr][ ].*_TEMPLATE_DIR=.*";
    final String REPORT_PATTERN = "^[ ]*[Rr][Ee][Pp][Oo][Rr][Tt] .*";
    private boolean editorUpdateEnabled = true;
    MainFrame mainFrame;
    String defaultReportFileName;

    PathPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        defaultReportFileName = "index.html";
        List<PluginInfo> lp = ReportProviderFactory.getInstance().getAvailableProviders();
        String fts[];

        // Search the available report providers if XML format is supported.
        for (PluginInfo pi : lp) {
            try {
                fts = ((ReportProvider) pi.getPluginInstance()).getSupportedFormats();
                for (String s : fts) {
                    if (s.equalsIgnoreCase("xml")) {
                        defaultReportFileName = "index.xml";
                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        mainFrame.getScriptHandler().addScriptListener(this);
        init();
    }

    public RecordingModule getRecordingModule() {
        return recordingModule;
    }

    public void setRecordingModule(RecordingModule rm) {
        if (recordingModule != null) {
            recordingModule.removePropertyChangeListener(this);
        }
        this.recordingModule = rm;
        recordingModule.addPropertyChangeListener(this);
        editorChanged();
    }

    public void editorChanged() {
        context = new ScriptingContextImpl();
        Map m = getContext(recordingModule.getEditorPnl());
        if (m != null) {
            context.putAll(m);
        }
        enableControls();
        updateFields(context, false);
    }

    void enableControls() {
        EditorPnl ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
        boolean isEditor = ed != null;

        boolean isProprietary = false;
        if (isEditor && ed.getTestScript() != null) {
            isProprietary = ed.getTestScript().getType() == TestScriptInterpret.TYPE_PROPRIETARY;
        }

        chbReport.setEnabled(isProprietary);
        txtReportPath.setEnabled(isProprietary);
        txtTemplatePath.setEnabled(isProprietary);
        btnReportPath.setEnabled(isProprietary);
        btnTemplatePath.setEnabled(isProprietary);

        if (!isProprietary) {
            bindListeners(false);
            chbReport.setSelected(false);
            txtReportDesc.setText("");
            txtReportFile.setText("");
            txtReportPath.setText("");
            txtTemplatePath.setText("");
            bindListeners(true);
        }
    }

    private void scriptValidated(ScriptingContext repository, boolean currentDocumentCompiled) {
        this.context = repository;
        updateFields(repository, !currentDocumentCompiled);
    }

    /**
     * Retrieve value of a parameter from a document element containing a command.
     * @param e an element.
     * @param paramName parameter name. A value of null retrieves command argument.
     * @param ctx context.
     * @return parameter value.
     */
    private String getParamValueFromElement(Element e, String paramName, ScriptingContext ctx) {
        String text = DocumentUtils.getElementText(e);
        List<String> l = new ArrayList();
        Map<String, String> tt = ctx.getParser().parseParameters(text, l);
        if (paramName == null) {
            if (l.size() > 0 && !tt.containsKey(l.get(0))) {
                return l.get(0);
            }
            return null;
        }
        return tt.get(paramName);
    }

    private void updateFields(ScriptingContext ctx, boolean skipReportOnes) {
        if (ctx != null) {
            bindListeners(false);
            Map t = (Map) ctx.getVariables();
            if (t != null && !skipReportOnes) {
                String rd = Utils.getDefaultOutputPath();
//                if (ctx.containsKey(ScriptingContext.CONTEXT_OUTPUT_PATH_ELEMENT)) {
//                    Element e = (Element) ctx.get(ScriptingContext.CONTEXT_OUTPUT_PATH_ELEMENT);
//                    rd = getParamValueFromElement(e, ScriptingContext.IMPLICIT_VARIABLE_REPORT_DIR, ctx);
//                }
                // Population of output path changed to a map to address PRB-3416.
                // It handles correctly multiple variable definitions within a single script
                // as well as those in subscripts executed through the Run command
                if (ctx.containsKey(ScriptingContext.CONTEXT_OUTPUT_PATH_ELEMENT)) {
                    LinkedHashMap<Element, Object> l = (LinkedHashMap) ctx.get(ScriptingContext.CONTEXT_OUTPUT_PATH_ELEMENT);
                    Document doc = recordingModule.getEditorPnl().getEditor().getDocument();
                    for (Element e : l.keySet()) {
                        if (e.getDocument().equals(doc)) {
                            rd = getParamValueFromElement(e, ScriptingContext.IMPLICIT_VARIABLE_REPORT_DIR, ctx);
                            break;
                        }
                    }
                }
                String temp = txtReportPath.getText();
                if (temp == null || !temp.equals(rd)) {
                    txtReportPath.setText(rd);
                }


                String td = Utils.getDefaultTemplatePath();

                // Population of output path changed to a map to address PRB-3416.
                // It handles correctly multiple variable definitions within a single script
                // as well as those in subscripts executed through the Run command
                if (ctx.containsKey(ScriptingContext.CONTEXT_TEMPLATE_PATH_ELEMENT)) {
                    LinkedHashMap<Element, Object> l = (LinkedHashMap) ctx.get(ScriptingContext.CONTEXT_TEMPLATE_PATH_ELEMENT);
                    Document doc = recordingModule.getEditorPnl().getEditor().getDocument();
                    for (Element e : l.keySet()) {
                        if (e.getDocument().equals(doc)) {
                            td = getParamValueFromElement(e, ScriptingContext.IMPLICIT_VARIABLE_TEMPLATE_DIR, ctx);
                            break;
                        }
                    }
                }
//                if (ctx.containsKey(ScriptingContext.CONTEXT_TEMPLATE_PATH_ELEMENT)) {
//                    Element e = (Element) ctx.get(ScriptingContext.CONTEXT_TEMPLATE_PATH_ELEMENT);
//                    td = getParamValueFromElement(e, ScriptingContext.IMPLICIT_VARIABLE_TEMPLATE_DIR, ctx);
//                }
                temp = txtTemplatePath.getText();
                if (temp == null || !temp.equals(td)) {
                    txtTemplatePath.setText(td);
                }

                String report = t == null ? null : (String) t.get(ScriptingContext.REPORT_REPORT_FILENAME);

                if (report != null) {
//                    String desc = (String) t.get(ScriptingContext.REPORT_REPORT_DESC);
                    report = null;
                    String desc = null;

                    // Population of report changed to a map to address PRB-3416.
                    // It handles correctly multiple Report calls within a single script
                    // as well as those in subscripts executed through the Run command
                    if (ctx.containsKey(ScriptingContext.CONTEXT_REPORT_ELEMENT_LIST)) {
                        LinkedHashMap<Element, Object> l = (LinkedHashMap) ctx.get(ScriptingContext.CONTEXT_REPORT_ELEMENT_LIST);
                        Document doc = recordingModule.getEditorPnl().getEditor().getDocument();
                        for (Element e : l.keySet()) {
                            if (e.getDocument().equals(doc)) {
                                report = getParamValueFromElement(e, null, ctx);
                                desc = getParamValueFromElement(e, ReportCommand.PARAM_DESC, ctx);
                                break;
                            }
                        }
                    }
                    temp = txtReportFile.getText();
                    if (temp != null && !temp.equals(report)) {
                        txtReportFile.setText(report);
                    }
                    temp = txtReportDesc.getText();
                    if (temp != null && !temp.equals(desc)) {
                        txtReportDesc.setText(desc);
                    }
                    chbReport.setSelected(report != null);
                    enableReportFields(report != null);
                } else {
                    chbReport.setSelected(false);
                    // Clear the fields (added in 2.0.6/2.2.1)
                    txtReportDesc.setText("");
                    txtReportFile.setText("");
                    enableReportFields(false);
                }
            }
            bindListeners(true);
        }
    }

    //TODO: tooltip when text larger than width
    //TODO: display description of the fields
    private void bindListeners(boolean bind) {
        txtReportFile.getDocument().removeDocumentListener(this);
        txtReportDesc.getDocument().removeDocumentListener(this);
        txtReportPath.getDocument().removeDocumentListener(this);
        txtTemplatePath.getDocument().removeDocumentListener(this);
        chbReport.removeItemListener(this);
        if (bind) {
            txtReportFile.getDocument().addDocumentListener(this);
            txtReportDesc.getDocument().addDocumentListener(this);
            txtReportPath.getDocument().addDocumentListener(this);
            txtTemplatePath.getDocument().addDocumentListener(this);
            chbReport.addItemListener(this);
        }
    }

    private void init() {
        lblReportPath.setLabelFor(txtReportPath);
        lblReportPath.setText(res.getString("toolsPanel.pathPanel.outputPath"));
        lblTemplatePath.setLabelFor(txtTemplatePath);
        lblTemplatePath.setText(res.getString("toolsPanel.pathPanel.templatePath"));
        lblReportFile.setLabelFor(txtReportFile);
        lblReportFile.setText(res.getString("toolsPanel.pathPanel.reportFile"));
        lblReportDesc.setText(res.getString("toolsPanel.pathPanel.description"));
        chbReport.setText(res.getString("toolsPanel.pathPanel.createHTML"));
        enableReportFields(false);

        btnReportPath.setMargin(new Insets(0, 2, 0, 2));
        btnReportPath.setText(res.getString("toolsPanel.pathPanel.threeDotsBtn"));
        btnReportPath.addActionListener(this);
        btnTemplatePath.setMargin(new Insets(0, 2, 0, 2));
        btnTemplatePath.setText(res.getString("toolsPanel.pathPanel.threeDotsBtn2"));
        btnTemplatePath.addActionListener(this);

        bindListeners(true);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel pnlPaths = new JPanel(new GridBagLayout());
        TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), res.getString("toolsPanel.pathPanel.directories"));
        pnlPaths.setBorder(tb);
        pnlPaths.add(lblReportPath, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 3), 0, 0));
        pnlPaths.add(txtReportPath, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0));
        pnlPaths.add(btnReportPath, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 2, 0, 3), 0, 0));
        pnlPaths.add(lblTemplatePath, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 3), 0, 0));
        pnlPaths.add(txtTemplatePath, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0));
        pnlPaths.add(btnTemplatePath, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 2, 0, 3), 0, 0));
        this.add(pnlPaths);

        JPanel pnlReport = new JPanel(new GridBagLayout());
        pnlReport.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), res.getString("toolsPanel.pathPanel.reportSettings")));
        pnlReport.add(chbReport, new GridBagConstraints(0, 2, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0));
        pnlReport.add(lblReportFile, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 3), 0, 0));
        pnlReport.add(txtReportFile, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0));
        pnlReport.add(lblReportDesc, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 3), 0, 0));
        pnlReport.add(txtReportDesc, new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0));
        this.add(pnlReport);
    }

    private ScriptingContext getContext(EditorPnl ed) {
        if (ed.getTestScript().getType() == TestScriptInterpret.TYPE_PROPRIETARY) {
            Map customCmdTable = new HashMap();
            Map cmdOrig = mainFrame.getScriptHandler().getCommandHandlers();
            customCmdTable.put("VAR", cmdOrig.get("VAR"));
            customCmdTable.put("REPORT", cmdOrig.get("REPORT"));
            return ((ProprietaryTestScriptInterpret) ed.getTestScript()).compileCustom(null, customCmdTable, true, false);
        }
        return ed.getTestScript().getCompilationContext();
    }

    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);

        JTextField tf = null;
        if (e.getSource().equals(btnReportPath)) {
            tf = txtReportPath;
        } else {
            tf = txtTemplatePath;
        }
        File f = new File(tf.getText());
        if (f.exists()) {
            chooser.setCurrentDirectory(f);
        }
        if (chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            try {
                tf.setText(chooser.getSelectedFile().getCanonicalPath());
            } catch (IOException ex) {
                tf.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    private void updateCommand(String pattern, String precedingPatterns[], String newCommand) {
        Editor ed = recordingModule.getEditorPnl().getEditor();
        StyledDocument doc = (StyledDocument) ed.getDocument();
        List v = DocumentUtils.findElements(doc, pattern);
        int elemIndex = ed.getDocument().getDefaultRootElement().getElementIndex(ed.getCaretPosition());
        Element ce = ed.getDocument().getDefaultRootElement().getElement(elemIndex);
        Element e = null;
        boolean replace = false;
        List p = new ArrayList();

        if (v.size() > 0) {
            e = (Element) v.get(v.size() - 1);
            replace = true;
        } else {
            if (precedingPatterns != null) {

                // Find all lines of code complying with the patterns in the array
                for (int i = 0; i < precedingPatterns.length; i++) {
                    p.addAll(DocumentUtils.findElements(doc, precedingPatterns[i]));
                }

                // Find the element with the highest offset
                Element pe;
                for (int i = 0; i < p.size(); i++) {
                    pe = (Element) p.get(i);
                    if (e == null || e.getStartOffset() < pe.getStartOffset()) {
                        e = pe;
                    }
                }
            }
            if (e == null) {
                e = ed.getDocument().getDefaultRootElement().getElement(0);
            }
        }

        // Set the caret position to the position of the last element that complies with the pattern
        int startPos = e.getStartOffset();
        if (startPos == 0 && p.size() > 0) {
            startPos = 1;
        }
        ed.setCaretPosition(startPos);

        Element insElem = recordingModule.insertLine(newCommand, replace, false, false);

        // Set the caret position back to the original element only if it is below the inserted element
        if (ce.getStartOffset() > insElem.getEndOffset()) {
            ed.setCaretPosition(ce.getStartOffset());
        }
    }

    public void itemStateChanged(ItemEvent e) {
        if (e == null || e.getSource().equals(chbReport)) {
            boolean selected = chbReport.isSelected();
            enableReportFields(selected);
            if (selected && txtReportFile.getText() == null || txtReportFile.getText().trim().equals("")) {
                txtReportFile.setText(defaultReportFileName);
            }
            updateReportCommandInEditor();
        }
    }

    private void enableReportFields(boolean enable) {
        enable = enable && chbReport.isEnabled();
        lblReportFile.setEnabled(enable);
        txtReportFile.setEnabled(enable);
        lblReportDesc.setEnabled(enable);
        txtReportDesc.setEnabled(enable);
    }

    private void updateReportCommandInEditor() {
        boolean delete = !chbReport.isSelected();
        Editor ed = recordingModule.getEditorPnl().getEditor();
        StyledDocument doc = (StyledDocument) ed.getDocument();
        List v = DocumentUtils.findElements(doc, REPORT_PATTERN);
        Element e;

        if (delete) {  // Delete the report command if it exists in the document
            if (v.size() > 0) {
                try {
                    e = (Element) v.get(v.size() - 1);
                    int start = e.getStartOffset();
                    int offset = e.getEndOffset() - start;
                    offset = Math.min(offset, ed.getDocument().getLength() - start);
                    ed.getDocument().remove(start, offset);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        } else {  // Rewrite the report command
            String cmd = "Report \"" + Utils.escapeUnescapedDoubleQuotes(txtReportFile.getText()) + "\"";
            if (!txtReportDesc.getText().equals("")) {
                cmd += " desc=\"" + Utils.escapeUnescapedDoubleQuotes(txtReportDesc.getText()) + "\"";
            }
            updateCommand(REPORT_PATTERN, new String[]{REPORT_DIR_PATTERN, TEMPLATE_DIR_PATTERN}, cmd);
        }
    }

    public void updateOutputDirInEditor() {
        String cmd = "Var _REPORT_DIR=\"" + Utils.escapeUnescapedDoubleQuotes(txtReportPath.getText()) + "\"";
        updateCommand(REPORT_DIR_PATTERN, null, cmd);
        updateReportCommandInEditor();
    }

    public void updateTemplateDirInEditor() {
        String cmd = "Var _TEMPLATE_DIR=\"" + Utils.escapeUnescapedDoubleQuotes(txtTemplatePath.getText()) + "\"";
        updateCommand(TEMPLATE_DIR_PATTERN, new String[]{REPORT_DIR_PATTERN}, cmd);
    }

    public void changedUpdate(DocumentEvent e) {
        if (editorUpdateEnabled) {
            if (e.getDocument().equals(txtReportDesc.getDocument()) || e.getDocument().equals(txtReportFile.getDocument())) {
                updateReportCommandInEditor();
            } else if (e.getDocument().equals(txtReportPath.getDocument())) {
                updateOutputDirInEditor();
            } else if (e.getDocument().equals(txtTemplatePath.getDocument())) {
                updateTemplateDirInEditor();
            }
        }
    }

    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public boolean isEditorUpdateEnabled() {
        return editorUpdateEnabled;
    }

    public void setEditorUpdateEnabled(boolean editorUpdateEnabled) {
        this.editorUpdateEnabled = editorUpdateEnabled;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("recordingEditorChanged")) {
            editorChanged();
        }
    }

    public void scriptEvent(ScriptEvent event) {
        if (event.getType() == ScriptEvent.SCRIPT_COMPILATION_FINISHED) {
            boolean currentDocument = recordingModule.getEditorPnl().getEditor().getDocument().equals(event.getInterpret().getDocument());
            scriptValidated(event.getContext(), currentDocument);
        }
    }
}
