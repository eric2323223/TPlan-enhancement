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
import com.tplan.robot.scripting.commands.impl.ScreenshotCommand;
import com.tplan.robot.gui.editor.EditorPnl;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.gui.components.*;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.*;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.util.DocumentUtils;
import com.tplan.robot.util.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Vector;

/**
 * Window allowing to construct a {@doc.cmd Screenshot} command.
 * @product.signature
 */
public class ScreenshotDialog extends JDialog
        implements ActionListener, FocusListener, UserInputListener, PropertyChangeListener, WindowListener {

    JButton btnOK = new JButton(ApplicationSupport.getString("btnOk"));
    JButton btnCancel = new JButton(ApplicationSupport.getString("btnCancel"));
    JButton btnHelp = new JButton(ApplicationSupport.getString("btnHelp"));
    JLabel lblName = new JLabel(ApplicationSupport.getString("screenshotDlg.imgFileName"));
    JLabel lblDesc = new JLabel(ApplicationSupport.getString("screenshotDlg.desc"));
    JLabel lblAttachments = new JLabel(ApplicationSupport.getString("screenshotDlg.attachments"));
    JLabel lblArea = new JLabel(ApplicationSupport.getString("screenshotDlg.area"));
    JTextField txtName = new JTextField();
    JTextField txtDesc = new JTextField();
    JTextField txtAttachments = new JTextField();
    JTextField txtArea = new JTextField();
    JButton btnDefineArea = new JButton(ApplicationSupport.getString("screenshotDlg.btnDefine"));
    JComboBox cmbExtensions;
    List extensions = new ArrayList();
    public boolean canceled = false;
    CustomFilteredDocument doc1;
    String templatePath = Utils.getDefaultTemplatePath();
    String outputPath = Utils.getDefaultOutputPath();
    MainFrame mainFrame;
    Image imageCopy;
    ImageDialog dlgImage;
    boolean editMode = false;
    ComparisonPnl pnlTemplate;
    ScreenshotCommand cmd = new ScreenshotCommand();
    Map entryParams;
    TokenParser parser = new TokenParserImpl();
    boolean canDrawResults = false;
    JCheckBox chbDrawResults = new JCheckBox(ApplicationSupport.getString("screenshotDlg.drawResults"));

    public ScreenshotDialog(MainFrame owner, String title, boolean modal) {
        super(owner, title, modal);
        this.mainFrame = owner;
        addWindowListener(this);
        canDrawResults = cmd.getContextAttributes().containsKey("drawresults");
        initDlg();
    }

    private void initDlg() {
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                ApplicationSupport.getString("screenshotDlg.argsTitledBorder")));
        txtName.setColumns(30);
        txtName.addFocusListener(this);
        txtDesc.setColumns(30);
        txtDesc.addFocusListener(this);
        txtAttachments.setColumns(30);
        txtAttachments.addFocusListener(this);

        String[] supportedFormats = ImageIO.getWriterFormatNames();
        for (int i = 0; i < supportedFormats.length; i++) {
            supportedFormats[i] = supportedFormats[i].toLowerCase();
        }
        Arrays.sort(supportedFormats);
        for (int i = 0; i < supportedFormats.length; i++) {
            if (!extensions.contains(supportedFormats[i])) {
                extensions.add(supportedFormats[i]);
            }
        }
        cmbExtensions = new JComboBox(new Vector(extensions));
        cmbExtensions.setSelectedIndex(0);

        cmd.enableImageComparisons = false;
        doc1 = new CustomFilteredDocument(txtName, this, mainFrame.getScriptHandler(), cmd);
        doc1.addUserInputListener(this);

        paramsPanel.add(lblName, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        paramsPanel.add(txtName, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        paramsPanel.add(cmbExtensions, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        paramsPanel.add(lblDesc, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        paramsPanel.add(txtDesc, new GridBagConstraints(1, 4, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        paramsPanel.add(lblAttachments, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        paramsPanel.add(txtAttachments, new GridBagConstraints(1, 5, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        paramsPanel.add(lblArea, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        paramsPanel.add(txtArea, new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        paramsPanel.add(btnDefineArea, new GridBagConstraints(2, 6, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        if (canDrawResults) {
            paramsPanel.add(chbDrawResults, new GridBagConstraints(0, 7, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 2, 2, 2), 0, 0));
        }
        chbDrawResults.setEnabled(false);

        pnlTemplate = new ComparisonPnl(mainFrame, this);
        pnlTemplate.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), ApplicationSupport.getString("screenshotDlg.comparisonTitledBorder")));
        pnlTemplate.setEnableCheckBoxVisible(true, false);
        pnlTemplate.addPropertyChangeListener(this);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnOK.addActionListener(this);
        btnOK.setEnabled(false);
        btnCancel.addActionListener(this);
        btnHelp.addActionListener(this);
        btnHelp.setEnabled(mainFrame.isHelpAvailable());
        btnDefineArea.addActionListener(this);

        south.add(btnOK);
        south.add(btnCancel);
        south.add(btnHelp);
        getRootPane().setDefaultButton(btnOK);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(paramsPanel, BorderLayout.NORTH);
        contentPane.add(pnlTemplate, BorderLayout.CENTER);
        contentPane.add(south, BorderLayout.SOUTH);
        this.setContentPane(contentPane);

        Utils.registerDialogForEscape(this, btnCancel);
    }

    public void setVisible(Element line) {
        RemoteDesktopClient rfb = mainFrame.getClient();
        canceled = false;
        imageCopy = null;
        if (rfb != null && rfb.isConnected()) {
            imageCopy = new BufferedImage(rfb.getDesktopWidth(), rfb.getDesktopHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = imageCopy.getGraphics();
            g.drawImage(rfb.getImage(), 0, 0, mainFrame);
        }
        btnDefineArea.setEnabled(rfb != null && rfb.isConnected());
        EditorPnl pnl = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
        TestScriptInterpret ti = pnl.getTestScript();

        // Set the selection to the command only. We are interested just in compiled
        // variables which are compiled event when they're out of selection.
        ti.setSelection(line.getStartOffset(), line.getEndOffset() - 1);
        ti.compile(null);
        ti.resetSelection();
        ScriptingContext context = pnl.getTestScript().getCompilationContext();
        setTemplatePath((String) context.getVariable(ScriptingContext.IMPLICIT_VARIABLE_TEMPLATE_DIR));
        setOutputPath((String) context.getVariable(ScriptingContext.IMPLICIT_VARIABLE_REPORT_DIR));
        doc1.setContext(context);

        if (line != null) {
            List v = new ArrayList();
            Map t = DocumentUtils.getTokens(line, v);
            editMode = v.size() > 0 && v.get(0).toString().equalsIgnoreCase("screenshot");
            if (!editMode) {
                v.clear();
                t.clear();
            } else {
                // Set all params to lower case (implements tolerance to letter case)
                Map newT = new HashMap();
                Iterator e = t.keySet().iterator();
                Object o;
                while (e.hasNext()) {
                    o = e.next();
                    newT.put(o.toString().toLowerCase(), t.get(o));
                }
                t = newT;
            }
            entryParams = t;
            updateFields(t, v);

            pnlTemplate.setValues(t, context);
        }

        setVisible(true);
    }

    public void updateImage(Image img) {
        imageCopy = img;
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

    private void updateFields(Map validationParams, List v) {
        String s = v.size() > 1 ? (String) v.get(1) : null;
        if (s != null) {
            String ext = Utils.getExtension(s);
            if (ext != null) {
                s = s.substring(0, s.length() - ext.length() - 1);
                cmbExtensions.setSelectedItem(ext.toLowerCase());
            }
        }
        txtName.setText(s == null ? "" : s);

        s = (String) validationParams.get(ScreenshotCommand.PARAM_DESC);
        txtDesc.setText(s == null ? "" : s);

        s = (String) validationParams.get(ScreenshotCommand.PARAM_ATTACH);
        txtAttachments.setText(s == null ? "" : s);

        Object o = validationParams.get(ScreenshotCommand.PARAM_AREA);
        if (o == null) {
            s = "";
        } else if (o instanceof String) {
            s = (String) o;
        } else if (o instanceof Rectangle) {
            Rectangle r = (Rectangle) o;
            s = parser.rectToString(r);
        }
        txtArea.setText(s);
        if (canDrawResults && validationParams.containsKey("drawresults")) {
            boolean draw = Boolean.parseBoolean(validationParams.get("drawresults").toString());
            chbDrawResults.setSelected(draw);
        }
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath == null ? Utils.getDefaultOutputPath() : outputPath;
    }

    public String getImageName() {
        return txtName.getText() + "." + cmbExtensions.getSelectedItem();
    }

    public String getImageDescription() {
        // Return description. Escape all unescaped double quotes.
        return Utils.escapeUnescapedDoubleQuotes(txtDesc.getText());
    }

    public String getImageAttachments() {
        // Return attachments. Escape all unescaped double quotes.
        return Utils.escapeUnescapedDoubleQuotes(txtAttachments.getText());
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnCancel)) {
            canceled = true;
            imageCopy = null;
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
            imageCopy = null;
            dispose();
        } else if (e.getSource().equals(btnDefineArea)) {
            if (dlgImage == null) {
                dlgImage = new ImageDialog(this, ApplicationSupport.getString("screenshotDlg.defineAreaTitle"), true);
            }
            dlgImage.setImage(imageCopy);
            try {
                TokenParser parser = new TokenParserImpl();
                RemoteDesktopClient client = MainFrame.getInstance().getClient();
                Rectangle defaults = null;
                if (client != null && client.isConnected()) {
                    defaults = new Rectangle(0, 0, client.getDesktopWidth(), client.getDesktopHeight());
                }
                Rectangle r = parser.parseRectangle(txtArea.getText(), defaults, ScreenshotCommand.PARAM_AREA);
                dlgImage.setRectangle(r);
            } catch (Exception ex) {
            }
            dlgImage.setVisible(true);
            if (!dlgImage.isCanceled()) {
                if (dlgImage.getRectangle() != null) {
                    txtArea.setText(parser.rectToString(dlgImage.getRectangle()));
                    dlgImage.setRectangle(null);
                } else {
                    txtArea.setText("");
                }
            }
        } else if (e.getSource().equals(btnHelp)) {
            mainFrame.showHelpDialog("gui.screenshot", this);
        }
    }

    private boolean doOk() throws PropertyVetoException {
        RecordingModule rec = mainFrame.getRecordingModule();
        if (!canceled) {

            if (!pnlTemplate.doOk()) {
                return false;
            }

            List v = new ArrayList();
            Map t = new HashMap();
            v.add("Screenshot");

            String file = getImageName();
            v.add(file);

            String s = getImageDescription();
            if (s != null && s.trim().length() > 0) {
                v.add(ScreenshotCommand.PARAM_DESC);
                t.put(ScreenshotCommand.PARAM_DESC, s);
            }

            s = getImageAttachments();
            if (s != null && s.trim().length() > 0) {
                v.add(ScreenshotCommand.PARAM_ATTACH);
                t.put(ScreenshotCommand.PARAM_ATTACH, s);
            }

            s = txtArea.getText();
            if (s != null && s.trim().length() > 0) {
                v.add(ScreenshotCommand.PARAM_AREA);
                t.put(ScreenshotCommand.PARAM_AREA, s);
            }

            pnlTemplate.getValues(v, t);

            // Validate whether the template name is not the same as screenshot name
            if (t.containsKey("template")) {
                try {
                    String template = t.get("template").toString();
                    if (template.trim().equals("")) {
                        t.remove("template");
                        v.remove("template");
                    } else {
                        File sf = new File(file);
                        File tf = new File(template);
                        if (tf.equals(sf) || sf.getCanonicalPath().equals(tf.getCanonicalPath())) {
                            JOptionPane.showMessageDialog(this,
                                    ApplicationSupport.getString("screenshotDlg.warningIdenticalScreenshotAndTemplate"));
                            return false;
                        }
                    }
                } catch (Exception ex) {
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
            if (canDrawResults && chbDrawResults.isEnabled() && chbDrawResults.isSelected()) {
                t.put("drawresults", "true");
                v.add("drawresults");
            }
            rec.insertScreenshot(editMode, v, t);
        }
        rec.resetTime();
        return true;
    }

    public void focusGained(FocusEvent e) {
        ((JTextField) e.getSource()).selectAll();
        getRootPane().setDefaultButton(btnOK);
    }

    public void focusLost(FocusEvent e) {
        if (e.getSource().equals(txtName) && !"".equals(txtName.getText())) {
//            pnlTemplate.imageNameUpdated(txtName.getText());
        }
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
        btnOK.setEnabled(doc1.isValueCorrect() && pnlTemplate.isContentCorrect());
    }

    public void setTemplatePath(String tp) {
        templatePath = tp == null ? Utils.getDefaultTemplatePath() : tp;
        pnlTemplate.setTemplatePath(tp);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if (name.equals("contentsCorrect") || name.equals(ComparisonPnl.PROPERTY_PARAMS_CHANGED)) {
            btnOK.setEnabled(doc1.isValueCorrect() && pnlTemplate.isContentCorrect());
            if (canDrawResults) {
                chbDrawResults.setEnabled(pnlTemplate.chbEnable.isSelected());
            }
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
    }
}
