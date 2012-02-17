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

import com.tplan.robot.gui.components.EventTableModel;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.scripting.RecordingModule;
import com.tplan.robot.util.Utils;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.editor.EditorPnl;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.List;
import javax.swing.event.ChangeListener;

/**
 * Panel with a table of remote desktop bell/update events and controls
 * allowing to generate a {@doc.cmd Waitfor} command for the selected event(s).
 * @product.signature
 */
public class RecordingPanel extends JPanel implements PropertyChangeListener, ListSelectionListener, ActionListener, ChangeListener {

    ResourceBundle res = ApplicationSupport.getResourceBundle();
    JTable table = new JTable();
    EventTableModel model;
    List events = null;
    RecordingModule recordingModule;
    JButton btnInsert = new JButton();
    JButton btnInsert2 = new JButton();
    JButton btnPreferences = new JButton();
    JButton btnScreenshot = new JButton();
    PathPanel pnlPaths;
    ToolsPanel pnlTools;
    VariableBrowserPanel pnlVars;
    private boolean visible = false;
    MainFrame mainFrame;
    private JTabbedPane tabbedPane = new JTabbedPane();

    public RecordingPanel(MainFrame fr) {
        mainFrame = fr;
        fr.getDocumentTabbedPane().addChangeListener(this);
        model = new EventTableModel(fr.getClient());
        init(fr);
    }

    public RecordingModule getRecordingModule() {
        return recordingModule;
    }

    public void setClient(RemoteDesktopClient client) {
        model.setClient(client);
    }

    public void setRecordingModule(RecordingModule recordingModule) {
        if (this.recordingModule != null) {
            this.recordingModule.removePropertyChangeListener(this);
        }
        recordingModule.addPropertyChangeListener(this);
        pnlPaths.setRecordingModule(recordingModule);
        pnlTools.setRecordingModule(recordingModule);
        this.recordingModule = recordingModule;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("dividerLocation")) {
            if (evt.getSource() instanceof JSplitPane) {
                JSplitPane sp = (JSplitPane) evt.getSource();
                setVisible(sp.getDividerLocation() > 1);
//                System.out.println("dividerLocation="+sp.getDividerLocation()+" min height="+this.getMinimumSize().height);
            }
        }
        if (isVisible()) {
            if ("rfbEventList".equals(evt.getPropertyName())) {
                events = (List) evt.getNewValue();
                model.refresh(events, ((Long) evt.getOldValue()).longValue());
                firePropertyChange("updateEventSelected", this, null);
            }
            pnlPaths.propertyChange(evt);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            if (!visible) {
                dispose();
            } else {
                pnlPaths.editorChanged();
            }
            this.visible = visible;
        }
    }

    private void enableToolsControls() {
        pnlPaths.enableControls();
        pnlTools.enableControls();
        pnlVars.enableControls();
        enableControls();
    }

    private void enableControls() {
        EditorPnl ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
        boolean isEditor = ed != null;

        boolean isProprietary = false;
        if (isEditor && ed.getTestScript() != null) {
            isProprietary = ed.getTestScript().getType() == TestScriptInterpret.TYPE_PROPRIETARY;
        }

        boolean selectionChanged = table.getSelectedRow() >= 0 && table.getSelectedRow() < events.size();
        if (selectionChanged) {
            List v = model.getListOfSelectedEvents(table);
            mainFrame.getGlassPanel().updateEventSelectionChanged(v);
        } else {
            mainFrame.getGlassPanel().updateEventSelectionChanged(null);
        }

        boolean enable = isProprietary && selectionChanged;
        btnInsert.setEnabled(enable);
        btnInsert2.setEnabled(enable);
    }

    private void init(MainFrame fr) {
//        fr.getScriptHandler().addPropertyChangeListener(this);

        this.setLayout(new GridBagLayout());

        // Initialize the event table tab
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(200, 200));
        table.setModel(model);
        table.getSelectionModel().addListSelectionListener(this);
        JPanel pnlEvents = new JPanel(new BorderLayout());
        pnlEvents.add(scroll, BorderLayout.CENTER);
        JPanel pnlEventsSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnInsert.setToolTipText(res.getString("toolsPanel.recordingPanel.generateWaitfor"));
        btnInsert.setMargin(new Insets(0, 0, 0, 0));
        btnInsert.addActionListener(this);
        btnInsert.setEnabled(false);
        try {
            btnInsert.setIcon(ApplicationSupport.getImageIcon("down16.gif"));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        pnlEventsSouth.add(btnInsert);

        btnInsert2.setToolTipText(res.getString("toolsPanel.recordingPanel.openWaitfor"));
        btnInsert2.setMargin(new Insets(0, 0, 0, 0));
        btnInsert2.addActionListener(this);
        btnInsert2.setEnabled(false);
        try {
            btnInsert2.setIcon(com.tplan.robot.ApplicationSupport.getImageIcon("waitfor16.gif"));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        pnlEventsSouth.add(btnInsert2);

        btnPreferences.setToolTipText(res.getString("toolsPanel.recordingPanel.recordingPreferences"));
        btnPreferences.setMargin(new Insets(0, 0, 0, 0));
        btnPreferences.addActionListener(this);
        try {
            btnPreferences.setIcon(com.tplan.robot.ApplicationSupport.getImageIcon("preferences16.gif"));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        pnlEventsSouth.add(btnPreferences);

        pnlEvents.add(pnlEventsSouth, BorderLayout.SOUTH);

        // Initialize the tabbed pan
        add(getTabbedPane(), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        firePropertyChange("updateEventSelected", this, null);
//        JLabel btm = new JLabel();
//        btm.setPreferredSize(new Dimension(0, 0));
//        add(btm, new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        pnlPaths = new PathPanel(fr);
        getTabbedPane().addTab(res.getString("toolsPanel.recordingPanel.settings"), pnlPaths);

        getTabbedPane().addTab(res.getString("toolsPanel.recordingPanel.events"), pnlEvents);

        pnlTools = new ToolsPanel(fr);
        getTabbedPane().addTab(res.getString("toolsPanel.recordingPanel.keys"), pnlTools);

        pnlVars = new VariableBrowserPanel(fr);
        getTabbedPane().addTab(res.getString("toolsPanel.recordingPanel.vars"), pnlVars);

//        JPanel pnlSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));

        btnScreenshot.setToolTipText(res.getString("toolsPanel.recordingPanel.generateScreenshot"));
        btnScreenshot.setMargin(new Insets(0, 0, 0, 0));
        btnScreenshot.addActionListener(this);
        try {
            btnScreenshot.setIcon(com.tplan.robot.ApplicationSupport.getImageIcon("screenshot16.gif"));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
//        pnlSouth.add(btnScreenshot);

        // Set the help ids
        fr.setHelpId(this, "gui.toolpanel");
        fr.setHelpId(pnlPaths, "gui.toolpanel_settings");
        fr.setHelpId(pnlEvents, "gui.toolpanel_events");
        fr.setHelpId(pnlTools, "gui.toolpanel_keys");

        enableToolsControls();
    }

    public void valueChanged(ListSelectionEvent e) {
        enableControls();
    }

    private void dispose() {
        firePropertyChange("updateEventSelected", this, null);
    }

    public void actionPerformed(ActionEvent e) {
        List v = model.getListOfSelectedEvents(table);

        if (e.getSource().equals(btnInsert) || e.getActionCommand().equals("waitfor")) {
            boolean preferBell = evaluateEvents(v);
            recordingModule.insertWaitFor(v, new ArrayList(events), preferBell);
        } else if (e.getSource().equals(btnInsert2)) {
            mainFrame.showWaitforDialog(v, null);
        } else if (e.getSource().equals(btnPreferences)) {
            String key = ApplicationSupport.getString("options.scriptingCommands.updateEvents");
            if (v != null && v.size() > 0) {
                if (((RemoteDesktopServerEvent) v.get(0)).getMessageType() == RemoteDesktopServerEvent.SERVER_BELL_EVENT) {
                    key = ApplicationSupport.getString("options.scriptingCommands.bellEvents");
                }
            } else {
                key = ApplicationSupport.getString("options.toolPanel.toolPanel");
            }
            mainFrame.showOptionsDialog(key, null);
        }
    }

    private boolean evaluateEvents(List v) {
        if (v.size() > 0) {
            RemoteDesktopServerEvent e;
            int type;
            type = ((RemoteDesktopServerEvent) v.get(0)).getMessageType();
            for (int i = 0; i < v.size(); i++) {
                e = (RemoteDesktopServerEvent) v.get(i);
                if (e.getMessageType() != type) {
                    // There are both BELL and UPDATE events
                    int option = 0;
                    Integer warningOption =
                            mainFrame.getUserConfiguration().getInteger("warning.bellOrUpdatePreference");
                    if (warningOption == null || warningOption.intValue() < 0) {
                        Object options[] = new String[]{"Bell", "Update"};
                        option = Utils.showConfigurableMessageDialog(mainFrame,
                                res.getString("toolsPanel.recordingPanel.Question"),
                                res.getString("toolsPanel.recordingPanel.QuestionText"),
                                res.getString("toolsPanel.recordingPanel.questionOption"),
                                "warning.bellOrUpdatePreference",
                                options,
                                0);
                    } else {
                        option = warningOption.intValue();
                    }
                    if (option == 0) {
                        return true;
                    } else if (option == 1) {
                        return false;
                    }
                }
            }
            return type == RemoteDesktopServerEvent.SERVER_BELL_EVENT;
        }
        return false;
    }

    public void stateChanged(ChangeEvent e) {
        enableToolsControls();
        pnlVars.editorChanged();
    }

    /**
     * @return the tabbedPane
     */
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
}
