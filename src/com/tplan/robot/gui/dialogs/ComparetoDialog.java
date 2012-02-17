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
import com.tplan.robot.gui.components.ComparisonPnl;
import com.tplan.robot.scripting.commands.impl.ScreenshotCommand;
import com.tplan.robot.gui.editor.EditorPnl;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.gui.components.FilteredDocument;
import com.tplan.robot.gui.components.UserInputEvent;
import com.tplan.robot.gui.components.UserInputListener;
import com.tplan.robot.scripting.RecordingModule;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.commands.impl.CompareToCommand;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.util.DocumentUtils;
import com.tplan.robot.util.Utils;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Window allowing to construct a {@doc.cmd Compareto} command.
 * @product.signature
 */
public class ComparetoDialog extends JDialog
        implements ActionListener, UserInputListener, PropertyChangeListener, WindowListener {

    JButton btnOK = new JButton(ApplicationSupport.getString("btnOk"));
    JButton btnCancel = new JButton(ApplicationSupport.getString("btnCancel"));
    JButton btnHelp = new JButton(ApplicationSupport.getString("btnHelp"));
    public boolean canceled = false;
    FilteredDocument doc1;
    String templatePath = Utils.getDefaultTemplatePath();
    String outputPath = Utils.getDefaultOutputPath();
    MainFrame mainFrame;
    boolean editMode = false;
    ComparisonPnl pnlTemplate;
    ScreenshotCommand cmd = new ScreenshotCommand();
    Map entryParams;

    public ComparetoDialog(MainFrame owner, String title, boolean modal) {
        super(owner, title, modal);
        this.mainFrame = owner;
        initDlg();
        addWindowListener(this);
    }

    private void initDlg() {
        setTitle(ApplicationSupport.getString("comparetoDlg.title"));

        pnlTemplate = new ComparisonPnl(mainFrame, this);
        pnlTemplate.setEnableCheckBoxVisible(false, true);
        pnlTemplate.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), ApplicationSupport.getString("comparetoDlg.templatePropsTitledBorder")));
        pnlTemplate.addPropertyChangeListener(this);

        cmd.enableImageComparisons = false;
//        doc1 = new CustomFilteredDocument(pnlTemplate.templateList, this, mainFrame.getScriptHandler(), cmd);
//        doc1.addUserInputListener(this);

//        pnlTemplate.templateList.setDocument(doc1);
//        pnlTemplate.chbTemplateName.setSelected(true);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnOK.addActionListener(this);
        btnOK.setEnabled(false);
        btnCancel.addActionListener(this);
        btnHelp.addActionListener(this);
        btnHelp.setEnabled(mainFrame.isHelpAvailable());

        south.add(btnOK);
        south.add(btnCancel);
        south.add(btnHelp);
        getRootPane().setDefaultButton(btnOK);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(pnlTemplate, BorderLayout.CENTER);
        contentPane.add(south, BorderLayout.SOUTH);
        this.setContentPane(contentPane);

        Utils.registerDialogForEscape(this, btnCancel);
    }

    public void setVisible(Element line) {
        EditorPnl ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
        TestScriptInterpret ti = ed.getTestScript();
        ti.compile(null);

        ScriptingContext repository = ti.getCompilationContext();
        setTemplatePath((String) repository.getVariable(ScriptingContext.IMPLICIT_VARIABLE_TEMPLATE_DIR));
        setOutputPath((String) repository.getVariable(ScriptingContext.IMPLICIT_VARIABLE_REPORT_DIR));

        if (line != null) {
            List v = new ArrayList();
            Map t = DocumentUtils.getTokens(line, v);
            editMode = v.size() > 0 && v.get(0).toString().equalsIgnoreCase("compareto");
            if (v.size() > 1) {
                String s = v.get(1).toString();
                v.set(1, "template");
                t.put("template", s);
            }
            if (!editMode) {
                v.clear();
                t.clear();
            }

            entryParams = t;

            File f = null;
            String template = (String) t.get(ScreenshotCommand.PARAM_TEMPLATE);
            if (template != null) {
                // Removed in 2.0.5
//                if (template.indexOf(File.separator) >= 0) {
//                    f = new File(template);
//                } else {
//                    f = new File(templatePath + File.separator + template);
//                }
            } else {
                String fileName = (String) t.get(ScreenshotCommand.PARAM_FILENAME);
                if (fileName != null) {
                    ScreenshotCommand cmd = new ScreenshotCommand();
                    cmd.enableImageComparisons = false;
                    f = lookForTemplateImage(fileName, t, cmd);
                    if (!f.exists() || !f.isFile() || !f.canRead()) {
                        f = null;
                    }
                }
            }
            if (f != null) {
                t.put(CompareToCommand.PARAM_TEMPLATE, f.getAbsolutePath());
            }
            pnlTemplate.setValues(t, repository);
        }
        btnOK.setEnabled(pnlTemplate.isContentCorrect());

        setVisible(true);
    }

    private File lookForTemplateImage(String fileName, Map validationParams, ScreenshotCommand cmd) {
        File f = null;
        if (validationParams.containsKey(ScreenshotCommand.PARAM_TEMPLATE)) {
            f = new File((String) validationParams.get(ScreenshotCommand.PARAM_TEMPLATE));
        } else {
            f = new File(fileName);  // Screenshot image file
            f = new File(templatePath + File.separator + f.getName());
//            System.out.println("file argument: " + f.getAbsolutePath());
            String found = cmd.getTemplate(f.getAbsolutePath(), mainFrame.getUserConfiguration());
//            System.out.println("found: " + found);
            f = found == null ? null : new File(found);
        }
        return f;
    }

    private String rectToString(Rectangle r) {
        return "x:" + r.x + ",y:" + r.y + ",w:" + r.width + ",h:" + r.height;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath == null ? Utils.getDefaultOutputPath() : outputPath;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnCancel)) {
            canceled = true;
            mainFrame.getRecordingModule().resetTime();
            dispose();
        } else if (e.getSource().equals(btnOK)) {

            // A PropertyVetoException indicates that a warning was shown and user selected not to close the dialog.
            try {
                if (!doOk()) {
                    return;
                }
            } catch (PropertyVetoException ex) {
                return;
            }
            canceled = false;
            dispose();
        } else if (e.getSource().equals(btnHelp)) {
            mainFrame.showHelpDialog("gui.compareto", this);
        }
    }

    public void reset() {
        canceled = false;
//        System.out.println("\n---- Compareto window opened ----");
    }

    private boolean doOk() throws PropertyVetoException {
        RecordingModule rec = mainFrame.getRecordingModule();
//        System.out.println("doOk()\n - canceled = "+canceled);
        if (!canceled) {

            if (!pnlTemplate.doOk()) {
                return false;
            }

            List v = new ArrayList();
            Map t = new HashMap();
            v.add("Compareto");

            pnlTemplate.getValues(v, t);

            String s;
            for (int i = 0; i < v.size(); i++) {
                s = v.get(i).toString();
                if (s.toLowerCase().equals("template")) {
                    v.remove(s);
                    s = (String) t.remove(s);
                    v.add(1, s);
                }
            }
            if (entryParams != null) {
                if (entryParams.containsKey("onpass")) {
                    t.put("onpass", entryParams.get("onpass"));
                    v.add("onpass");
                }
                if (entryParams.containsKey("onfail")) {
                    t.put("onfail", entryParams.get("onfail"));
                    v.add("onfail");
                }
            }
            rec.insertScreenshot(editMode, v, t);

        }
        rec.resetTime();
        return true;
    }

    public void vetoableChange(UserInputEvent e) throws PropertyVetoException {
        // TODO: validation of other params (area, ...) - neet to add a param to SyntaxErrorException
        if (e.getSource().equals(doc1)) {
            try {
                String name = doc1.getText(0, doc1.getLength());
                pnlTemplate.setImageName(name);
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }
        btnOK.setEnabled(doc1.isValueCorrect());// && pnlTemplate.areValuesCorrect());
    }

    public void setTemplatePath(String tp) {
        templatePath = tp == null ? Utils.getDefaultTemplatePath() : tp;
        pnlTemplate.setTemplatePath(tp);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ComparisonPnl.PROPERTY_PARAMS_CHANGED)) {
            btnOK.setEnabled(pnlTemplate.isContentCorrect());
        }
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
        pnlTemplate.cleanup();
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
//        pnlTemplate.templateList.requestFocus();
    }
}
