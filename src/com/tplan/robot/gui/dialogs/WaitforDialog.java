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
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.gui.components.EventTableModel;
import com.tplan.robot.scripting.commands.impl.CompareToCommand;
import com.tplan.robot.scripting.commands.impl.ScreenshotCommand;
import com.tplan.robot.scripting.commands.impl.WaitforCommand;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.editor.EditorPnl;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.gui.preferences.DefaultPreferenceTreeModel;
import com.tplan.robot.gui.preferences.DefaultPreferencePanel;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.gui.components.ComparisonPnl;
import com.tplan.robot.gui.components.FilteredDocument;
import com.tplan.robot.gui.components.UserInputEvent;
import com.tplan.robot.gui.components.UserInputListener;
import com.tplan.robot.scripting.RecordingModule;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import com.tplan.robot.util.Utils;
import com.tplan.robot.util.DocumentUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
import java.util.ResourceBundle;
import java.util.List;
import java.util.Map;

/**
 * Window allowing to construct a {@doc.cmd Waitfor} command.
 * @product.signature
 */
public class WaitforDialog extends JDialog implements ActionListener, FocusListener,
        ListSelectionListener, PropertyChangeListener, ChangeListener, WindowListener,
        UserInputListener, ItemListener {

    ResourceBundle res = ApplicationSupport.getResourceBundle();
    JTabbedPane tabPane = new JTabbedPane();
    JButton btnOK = new JButton(ApplicationSupport.getString("btnOk"));
    JButton btnCancel = new JButton(ApplicationSupport.getString("btnCancel"));
    JButton btnHelp = new JButton(ApplicationSupport.getString("btnHelp"));
    JButton btnComparison = new JButton(ApplicationSupport.getString("waitforDialog.howToUse"));
    JRadioButton rbMatch = new JRadioButton("match");
    JRadioButton rbMismatch = new JRadioButton("mismatch");
    DefaultPreferencePanel pnlUpdateParams = DefaultPreferenceTreeModel.createWaitForUpdatePanel(false, res);
    JTable updateEventTable = new JTable();
    EventTableModel updateEventModel;
    DefaultPreferencePanel pnlBellParams = DefaultPreferenceTreeModel.createWaitForBellPanel(res);
    JTable bellEventTable = new JTable();
    EventTableModel bellEventModel;
    DefaultPreferencePanel pnlMatchParams;
    ComparisonPnl pnlTemplate;
    JTextField txtCmd = new JTextField();
    UserConfiguration cfg;
    RecordingModule recordingModule;
    CompareToCommand cmdCompare;
    FilteredDocument doc1;
    public boolean canceled = true;
    private File templatePath = null;
    private String command;
    private Point lastLocation;
    boolean resetPrecedingWait;
    MainFrame mainFrame;
    boolean editMode = false;
    Map entryParams;

    public WaitforDialog(MainFrame owner, String title, boolean modal) {
        super(owner, title, modal);
        this.mainFrame = owner;
        pnlMatchParams = createMatchPanel();
        pnlTemplate = new ComparisonPnl(owner, this);
//        chooser = new ImageFileChooser(owner);
        updateEventModel = new EventTableModel(mainFrame.getClient());
        bellEventModel = new EventTableModel(mainFrame.getClient());
        cfg = UserConfiguration.getCopy();
        initDlg();
    }

    public void reset() {
    }

    public void setVisible(Element line) {
        editMode = false;

        EditorPnl ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
        TestScriptInterpret ti = ed.getTestScript();

        // Set the selection to the command only. We are interested just in compiled
        // variables which are compiled event when they're out of selection.
        ti.setSelection(line.getStartOffset(), line.getEndOffset() - 1);
        ti.compile(null);
        ti.resetSelection();
        ScriptingContext repository = ti.getCompilationContext();
        Map vars = repository.getVariables();
        String tp = (String) vars.get(ScriptingContext.IMPLICIT_VARIABLE_TEMPLATE_DIR);
        templatePath = tp == null ? null : new File(tp);
        String template = null;

        if (line != null) {
            List v = new ArrayList();
            Map t = DocumentUtils.getTokens(line, v);
            entryParams = new HashMap(t);
            editMode = v.size() > 0 && v.get(0).toString().equalsIgnoreCase("waitfor");
            if (editMode && v.size() > 1) {
                String cmd = (String) v.get(1);
                if (cmd.equalsIgnoreCase(WaitforCommand.EVENT_UPDATE)) {
                    tabPane.setSelectedIndex(0);
                    updateValue("extent", "recording.waitfor.update.defaultExtent", "recording.waitfor.update.insertExtent", t);
                    updateValue("timeout", "recording.waitfor.update.timeoutRatio", "recording.waitfor.update.insertTimeout", t);
                    updateValue("timeout", "recording.waitfor.update.minTimeout", "recording.waitfor.update.useMinTimeout", t);
                    updateValue("area", "area", "recording.waitfor.update.insertArea", t);
                    pnlUpdateParams.loadPreferences(cfg);
                } else if (cmd.equalsIgnoreCase(WaitforCommand.EVENT_BELL)) {
                    tabPane.setSelectedIndex(1);
                } else if (cmd.equalsIgnoreCase(WaitforCommand.EVENT_MATCH) || cmd.equalsIgnoreCase(WaitforCommand.EVENT_MISMATCH)) {
                    tabPane.setSelectedIndex(2);
                    updateValue("interval", "WaitUntilCommand.defaultInterval", "match.useInterval", t);
                    updateValue("timeout", "match.timeout", "match.useTimeout", t);
                    template = (String) t.get(ScreenshotCommand.PARAM_TEMPLATE);
//                    pnlTemplate.loadPreferences(t, mainFrame, template);
                    pnlMatchParams.loadPreferences(cfg);
                    rbMatch.setSelected(false);
                    rbMismatch.setSelected(false);
                    rbMatch.setSelected(cmd.equalsIgnoreCase(WaitforCommand.EVENT_MATCH));
                    rbMismatch.setSelected(cmd.equalsIgnoreCase(WaitforCommand.EVENT_MISMATCH));
                }
            } else {
                rbMatch.setSelected(false);
                rbMismatch.setSelected(false);
                rbMatch.setSelected(true);
            }
            t.put(CompareToCommand.PARAM_TEMPLATE, template);
            pnlTemplate.setValues(t, repository);
        }
        setVisible(true);
    }

    private void updateValue(String paramKey, String cfgKey, String cfgFlag, Map t) {
        if (t.containsKey(paramKey)) {
            cfg.setString(cfgKey, t.get(paramKey).toString());
            if (cfgFlag != null) {
                cfg.setBoolean(cfgFlag, new Boolean(true));
            }
        }
    }

    private void initDlg() {

        addWindowListener(this);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        // Update event table
        JPanel pnlUpdate = new JPanel(new BorderLayout());
        JScrollPane scroll = new JScrollPane(updateEventTable);
        scroll.setPreferredSize(new Dimension(200, 100));
        updateEventTable.setModel(updateEventModel);
        updateEventTable.getSelectionModel().addListSelectionListener(this);
        updateEventModel.setMessageTypeFilter(RemoteDesktopServerEvent.SERVER_UPDATE_EVENT);
        pnlUpdate.add(scroll, BorderLayout.CENTER);

        pnlUpdateParams.setVerticalInset(0);
        pnlUpdateParams.init();
        pnlUpdateParams.loadPreferences(cfg);
        pnlUpdateParams.addPropertyChangeListener(this);
        pnlUpdate.add(pnlUpdateParams, BorderLayout.SOUTH);

        // Bell event table
        JPanel pnlBell = new JPanel(new BorderLayout());
        JScrollPane scroll2 = new JScrollPane(bellEventTable);
        scroll2.setPreferredSize(new Dimension(200, 100));
        bellEventTable.setModel(bellEventModel);
        bellEventTable.getSelectionModel().addListSelectionListener(this);
        bellEventModel.setMessageTypeFilter(RemoteDesktopServerEvent.SERVER_BELL_EVENT);
        pnlBell.add(scroll2, BorderLayout.CENTER);

        pnlBellParams.setVerticalInset(0);
        pnlBellParams.init();
        pnlBellParams.loadPreferences(cfg);
        pnlBellParams.addPropertyChangeListener(this);
        pnlBell.add(pnlBellParams, BorderLayout.SOUTH);

        // Match panel
        JPanel pnlMatch = new JPanel(new BorderLayout());

        // Set the match values
        cfg.setBoolean("match.useInterval", new Boolean(true));
        cfg.setBoolean("match.useTimeout", new Boolean(true));
        cfg.setInteger("match.timeout", new Integer(15));

        pnlMatchParams.loadPreferences(cfg);
        pnlMatchParams.addPropertyChangeListener(this);
        pnlMatch.add(pnlMatchParams, BorderLayout.SOUTH);
        pnlTemplate.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(134, 134, 134)), res.getString("waitfor.border.templateProps")));
        pnlTemplate.setEnableCheckBoxVisible(false, true);
        pnlTemplate.addPropertyChangeListener(this);
        cmdCompare = new CompareToCommand();
        cmdCompare.enableMissingTemplates = true;
//        doc1 = new CustomFilteredDocument(pnlTemplate.templateList, this, mainFrame.getScriptHandler(), cmdCompare);
//        doc1.addUserInputListener(this);
        pnlTemplate.addPropertyChangeListener(this);
//        pnlTemplate.rbCreateNew.setSelected(true);
        pnlMatch.add(pnlTemplate, BorderLayout.CENTER);

        JPanel pnlNorth = new JPanel(new GridBagLayout());
        pnlNorth.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(134, 134, 134)), res.getString("waitfor.border.matchMismatch")));
        ButtonGroup group = new ButtonGroup();
        group.add(rbMatch);
        group.add(rbMismatch);
        rbMatch.addItemListener(this);
        rbMismatch.addItemListener(this);
        pnlNorth.add(new JLabel(ApplicationSupport.getString("waitforDialog.createWaitfor")), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 10), 0, 0));
        pnlNorth.add(rbMatch, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        pnlNorth.add(rbMismatch, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        pnlMatch.add(pnlNorth, BorderLayout.NORTH);

        btnHelp.addActionListener(this);
        btnHelp.setEnabled(mainFrame.isHelpAvailable());

        // Init the tabbed pane content and the command text field
        JPanel pnlCenter = new JPanel(new BorderLayout());
        tabPane.addTab("Update", pnlUpdate);
        tabPane.addTab("Bell", pnlBell);
        tabPane.addTab("Match", pnlMatch);
        tabPane.addChangeListener(this);
        pnlCenter.add(tabPane, BorderLayout.CENTER);

        JPanel pnlCmd = new JPanel(new BorderLayout(2, 2));
        txtCmd.setColumns(50);
        txtCmd.addFocusListener(this);
        pnlCmd.add(txtCmd, BorderLayout.CENTER);
        Border border = BorderFactory.createEtchedBorder(Color.white, new Color(134, 134, 134));
        border = new TitledBorder(border, ApplicationSupport.getString("waitforDialog.resultingCommand"));
        pnlCmd.setBorder(border);
        pnlCenter.add(pnlCmd, BorderLayout.SOUTH);
        getContentPane().add(pnlCenter, BorderLayout.CENTER);

        // Init the bottom panel with buttons OK and Cancel
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnOK.addActionListener(this);
        btnOK.setEnabled(false);
        btnCancel.addActionListener(this);
        btnComparison.addActionListener(this);

        south.add(btnOK);
        south.add(btnCancel);
        south.add(btnHelp);
        getRootPane().setDefaultButton(btnOK);
        getContentPane().add(south, BorderLayout.SOUTH);
        Utils.registerDialogForEscape(this, btnCancel);
    }

    public void setSelectedEvents(List v) {
        // Update event tab selected
//        if (tabPane.getSelectedIndex() == 0) {
        int tabToSelect = -1;
        updateEventTable.clearSelection();
        List e = updateEventModel.getEvents();
        for (int i = 0; v != null && i < v.size(); i++) {
            if (e.contains(v.get(i))) {
                int index = e.indexOf(v.get(i));
                updateEventTable.addRowSelectionInterval(index, index);
                tabToSelect = 0;
            }
        }
//        } else if (tabPane.getSelectedIndex() == 1) {
        bellEventTable.clearSelection();
        e = bellEventModel.getEvents();
        for (int i = 0; v != null && i < v.size(); i++) {
            if (e.contains(v.get(i))) {
                int index = e.indexOf(v.get(i));
                bellEventTable.addRowSelectionInterval(index, index);
                tabToSelect = 1;
            }
        }
        if (tabToSelect >= 0) {
            tabPane.setSelectedIndex(tabToSelect);
        }
//        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnCancel)) {
            canceled = true;
            dispose();
        } else if (e.getSource().equals(btnOK)) {
            canceled = false;
            command = txtCmd.getText();
            switch (tabPane.getSelectedIndex()) {
                case 0:
                    resetPrecedingWait = cfg.getBoolean("recording.waitfor.update.resetUpdateWait").booleanValue();
                    break;
                case 1:
                    resetPrecedingWait = cfg.getBoolean("recording.waitfor.bell.resetBellWait").booleanValue();
                    break;
                case 2:
                    resetPrecedingWait = cfg.getBoolean("match.resetWait").booleanValue();
                    break;
            }
            if (tabPane.getSelectedIndex() == 2) {
                try {
                    if (!pnlTemplate.doOk()) {
                        return;
                    }

                    // Bug fix in 2.1.1 - we have to reread the command because
                    // the comparison panel might have changed it
                    command = txtCmd.getText();

                    // This line must be here because warnings can be displayed using
                    // a JOptionPane and it calls the windowActivated() method
                    canceled = false;
                } catch (Exception ex) {
                    return;
                }
            }
            dispose();
        } else if (e.getSource().equals(btnHelp)) {
            String key = "gui.waitfor";
            switch (tabPane.getSelectedIndex()) {
                case 0:
                    key = "gui.waitfor_update";
                    break;
                case 1:
                    key = "gui.waitfor_bell";
                    break;
                case 2:
                    key = "gui.waitfor_match";
                    break;
            }
            mainFrame.showHelpDialog(key, this);
        }
    }

    public String getCommand() {
        return command;
    }

    public boolean isResetPrecedingWait() {
        return resetPrecedingWait;
    }

    public void focusGained(FocusEvent e) {
        getRootPane().setDefaultButton(btnOK);
    }

    public void focusLost(FocusEvent e) {
    }

    public void valueChanged(ListSelectionEvent e) {

        // When the source of the change is the update event table, we need to display the selected events
        // as red frames in the desktop.
        if (tabPane.getSelectedIndex() == 0) {
            JTable table = updateEventTable;
            if (table.getSelectedRow() >= 0 && table.getSelectedRow() < updateEventModel.getEvents().size()) {
                List v = updateEventModel.getListOfSelectedEvents(table);
                firePropertyChange("updateEventSelected", this, v);
            } else {
                firePropertyChange("updateEventSelected", this, null);
            }
        }

        updateCommand();
    }

    private void updateTemplatePath() {
        ScriptingContext ctx;
        EditorPnl ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
        if (ed.getTestScript().getType() == TestScriptInterpret.TYPE_PROPRIETARY) {
            Map customCmdTable = new HashMap();
            Map cmdOrig = mainFrame.getScriptHandler().getCommandHandlers();
            customCmdTable.put("VAR", cmdOrig.get("VAR"));
            ctx = ((ProprietaryTestScriptInterpret) ed.getTestScript()).compileCustom(null, customCmdTable, false, false);
        } else {
            ctx = ed.getTestScript().getCompilationContext();
        }
        Map t = ctx.getVariables();
        if (t != null) {
            setTemplatePath(new File((String) t.get("_TEMPLATE_DIR")));
        }
    }

    private void updateCommand() {

        String suffix = "";
        if (entryParams != null) {
            if (entryParams.containsKey(WaitforCommand.PARAM_ONPASS)) {
                String onpass = Utils.escapeUnescapedDoubleQuotes(entryParams.get(WaitforCommand.PARAM_ONPASS).toString());
                suffix += " " + WaitforCommand.PARAM_ONPASS + "=\"" + onpass + '"';
            }
            if (entryParams.containsKey(WaitforCommand.PARAM_ONTIMEOUT)) {
                String onfail = Utils.escapeUnescapedDoubleQuotes(entryParams.get(WaitforCommand.PARAM_ONTIMEOUT).toString());
                suffix += " " + WaitforCommand.PARAM_ONTIMEOUT + "=\"" + onfail + '"';
            }
        }

        // Update event tab selected
        if (tabPane.getSelectedIndex() == 0) {
            List v = updateEventModel.getListOfSelectedEvents(updateEventTable);
            String text = null;
            if (v != null && v.size() > 0) {
                pnlUpdateParams.savePreferences(cfg);
                text = recordingModule.createWaitForUpdate((RemoteDesktopServerEvent) v.get(0), v, cfg) + suffix;
            }
            txtCmd.setText(text);

        } else if (tabPane.getSelectedIndex() == 1) {
            // Bell event tab selected
            List v = bellEventModel.getListOfSelectedEvents(bellEventTable);
            String text = null;
            if (v != null && v.size() > 0) {
                pnlBellParams.savePreferences(cfg);
                text = recordingModule.createWaitForBell((RemoteDesktopServerEvent) v.get(0), v, cfg) + suffix;
            }
            txtCmd.setText(text);
        } else {
            // Match event tab selected
            pnlMatchParams.savePreferences(cfg);
            List v = new ArrayList();
            Map t = new HashMap();
            pnlTemplate.getValues(v, t);
            String cmd = createWaitForMatch(t) + suffix;
            txtCmd.setText(cmd);
        }
        enableOk();
    }

    private String createWaitForMatch(Map pt) {
        pt = new HashMap(pt);
        String cmd = null;
        String arg = rbMatch.isSelected() ? "match" : "mismatch";
        cmd = "Waitfor " + arg;

        Object img = pt.get(ScreenshotCommand.PARAM_TEMPLATE);
        if (img != null) {

            img = Utils.escapeUnescapedDoubleQuotes(img.toString());
            cmd += " template=\"" + img + "\"";
            pt.remove(ScreenshotCommand.PARAM_TEMPLATE);
        }

        if (pt.containsKey(ScreenshotCommand.PARAM_PASSRATE)) {
            cmd += " " + ScreenshotCommand.PARAM_PASSRATE + "=" + pt.get(ScreenshotCommand.PARAM_PASSRATE) + "%";
            pt.remove(ScreenshotCommand.PARAM_PASSRATE);
        }

        if (pt.containsKey(CompareToCommand.PARAM_CMPAREA)) {
            cmd += " " + CompareToCommand.PARAM_CMPAREA + "=" + pt.get(CompareToCommand.PARAM_CMPAREA);
            pt.remove(CompareToCommand.PARAM_CMPAREA);
        }

        boolean useInterval = cfg.getBoolean("match.useInterval").booleanValue();
        if (useInterval) {
            cmd += " interval=" + cfg.getInteger("WaitUntilCommand.defaultInterval") + "s";
        }

        String defaultMethod = mainFrame.getUserConfiguration().getString("CompareToCommand.defaultComparisonModule").toLowerCase();
        defaultMethod = defaultMethod == null || defaultMethod.equals("") ? "default" : defaultMethod;
        String selectedMethod = (String) pt.get(ScreenshotCommand.PARAM_METHOD);
        if (selectedMethod != null && !defaultMethod.equals(selectedMethod)) {
            cmd += " method=\"" + selectedMethod + "\"";
            pt.remove(CompareToCommand.PARAM_METHOD);
        }

        boolean useTimeout = cfg.getBoolean("match.useTimeout").booleanValue();
        if (useTimeout) {
            cmd += " timeout=" + cfg.getInteger("match.timeout") + "s";
        }

        // Fix in 2.1.2/2.2.1/2.3 - handle method specific params
        for (Object key : pt.keySet()) {
            cmd += " "+key+"=\""+pt.get(key)+"\"";
        }
        return cmd;
    }

    private void enableOk() {
        boolean enable = txtCmd.getText() != null && !txtCmd.getText().equals("");
        btnOK.setEnabled(enable);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("rfbEventList".equals(evt.getPropertyName())) {
            List events = (List) evt.getNewValue();
            long time = ((Long) evt.getOldValue()).longValue();
            updateEventModel.refresh(events, time);
            bellEventModel.refresh(events, time);
        } else if (evt.getSource() instanceof DefaultPreferencePanel || "valueUpdated".equals(evt.getPropertyName())) {
            updateCommand();
        } else if ("parametersChanged".equals(evt.getPropertyName())) {
            updateCommand();
        }
    }

    public void setRecordingModule(RecordingModule recordingModule) {
        recordingModule.removePropertyChangeListener(this);
        recordingModule.addPropertyChangeListener(this);
        List events = recordingModule.getRfbEvents();
        long time = recordingModule.getLastEventListUpdateTime();
        updateEventModel.refresh(events, time);
        bellEventModel.refresh(events, time);
        this.recordingModule = recordingModule;
    }

    public void stateChanged(ChangeEvent e) {
        if (tabPane.getSelectedIndex() == 2 && templatePath == null) {
            updateTemplatePath();
        }
        updateCommand();
    }

    private DefaultPreferencePanel createMatchPanel() {
        DefaultPreferencePanel component = new DefaultPreferencePanel();

        component.createContainer(ApplicationSupport.getString("waitforDialog.timingPreferencesTitledBorder"));
        Preference o = new Preference("match.useInterval", Preference.TYPE_BOOLEAN,
                ApplicationSupport.getString("waitforDialog.customComparisonInterval"), null);
        component.addPreference(o, 0);
        o = new Preference("WaitUntilCommand.defaultInterval", Preference.TYPE_INT,
                ApplicationSupport.getString("waitforDialog.comparisonIntervalLabel"),
                null);
        o.setMinValue(1);
        o.setDependentOption("match.useInterval");
        component.addPreference(o, 0);

        o = new Preference("match.useTimeout", Preference.TYPE_BOOLEAN,
                ApplicationSupport.getString("waitforDialog.defineTimeout"), null);
        component.addPreference(o, 0);

        o = new Preference("match.timeout", Preference.TYPE_INT,
                ApplicationSupport.getString("waitforDialog.timeoutLabel"), null);
        o.setMinValue(0);
        o.setDependentOption("match.useTimeout");
        component.addPreference(o, 0);

        o = new Preference("match.resetWait", Preference.TYPE_BOOLEAN,
                ApplicationSupport.getString("waitforDialog.resetWait"), null);
        component.addPreference(o, 0);

        component.init();

        return component;
    }

    public File getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(File templatePath) {
        if (templatePath == null || !templatePath.equals(this.templatePath)) {
            this.templatePath = templatePath;
        }
    }

    public void windowActivated(WindowEvent e) {
        canceled = true;
    }

    public void windowClosed(WindowEvent e) {
        updateEventTable.clearSelection();
        pnlTemplate.cleanup();
    }

    public Point getLastLocation() {
        return lastLocation;
    }

    public void windowClosing(WindowEvent e) {
        lastLocation = getLocation();
        dispose();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
        templatePath = null;
        canceled = true;
        stateChanged(null);
    }

    public void vetoableChange(UserInputEvent e) throws PropertyVetoException {
        if (e.getSource().equals(doc1)) {
            try {
                String name = doc1.getText(0, doc1.getLength());
                pnlTemplate.setImageName(name);
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }
        updateCommand();
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void itemStateChanged(ItemEvent e) {
        if (tabPane.getSelectedIndex() == 2) {
            updateCommand();
        }
    }
}
