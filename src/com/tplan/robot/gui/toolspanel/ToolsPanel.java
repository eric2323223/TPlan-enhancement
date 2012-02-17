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

import com.tplan.robot.gui.*;
import com.tplan.robot.scripting.RecordingModule;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.editor.EditorPnl;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.util.Utils;
import com.tplan.robot.scripting.commands.impl.PressCommand;

import java.io.IOException;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.ScriptingContextImpl;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;

/**
 * Panel capable of generating a key press using key identificators selectable
 * in its components.
 * @product.signature
 */
public class ToolsPanel extends JPanel implements ActionListener {
    ResourceBundle res = ApplicationSupport.getResourceBundle();
    JLabel lblPress = new JLabel();
    JComboBox cbPress = new JComboBox();
    JButton btnPress = new JButton();
    JLabel lblPressGeneric = new JLabel();
    JLabel lblGenericDesc = new JLabel();
    JCheckBox chbCtrl = new JCheckBox();
    JCheckBox chbAlt = new JCheckBox();
    JCheckBox chbShift = new JCheckBox();
    JCheckBox chbWindows = new JCheckBox();
    JComboBox cbKeys = new JComboBox();
    JButton btnPressGeneric = new JButton();

    PressCommand cmdHandler;
    RecordingModule recordingModule;

    MainFrame mainFrame;

    ToolsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        init();
    }

    public RecordingModule getRecordingModule() {
        return recordingModule;
    }

    public void setRecordingModule(RecordingModule recordingModule) {
        this.recordingModule = recordingModule;
    }

    void enableControls() {
        EditorPnl ed = mainFrame.getDocumentTabbedPane().getActiveEditorPanel();
        boolean isEditor = ed != null;

        boolean isProprietary = false;
        if (isEditor && ed.getTestScript() != null) {
            isProprietary = ed.getTestScript().getType() == TestScriptInterpret.TYPE_PROPRIETARY;
        }

        if (isProprietary) {

        } else {

        }
        chbAlt.setEnabled(isProprietary);
        chbCtrl.setEnabled(isProprietary);
        chbShift.setEnabled(isProprietary);
        chbWindows.setEnabled(isProprietary);
        btnPress.setEnabled(isProprietary);
        btnPressGeneric.setEnabled(isProprietary);
        cbKeys.setEnabled(isProprietary);
        cbPress.setEnabled(isProprietary);
    }

    private void init() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        lblPress.setText(res.getString("toolsPanel.keysPanel.Press"));
        btnPress.setText(res.getString("toolsPanel.keysPanel.Apply"));
        btnPress.setMargin(new Insets(2,2,2,2));
        btnPress.addActionListener(this);

        List qv = mainFrame.getUserConfiguration().getListOfStrings("recording.quickKeys");
        List q = Utils.getListOfStrings(ApplicationSupport.getResourceBundle().getString("recording.quickKeys"));
        if (q != null) {
            String s;
            for (int i = 0; i < q.size(); i++) {
                s = q.get(i).toString();
                if (!qv.contains(s)) {
                    qv.add(s);
                }
            }
        }
        cbPress.setModel(new DefaultComboBoxModel(new Vector(qv)));

        JPanel pnlQuick = new JPanel();
        pnlQuick.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), res.getString("toolsPanel.keysPanel.QuickPress")));
        pnlQuick.setLayout(new GridBagLayout());
        pnlQuick.add(lblPress, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 3, 0, 3), 0, 0));
        pnlQuick.add(cbPress, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        pnlQuick.add(btnPress, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 3, 0, 3), 0, 0));
        this.add(pnlQuick);

        lblGenericDesc.setText(res.getString("toolsPanel.keysPanel.CreatePressCommand:"));
        lblPressGeneric.setText(res.getString("toolsPanel.keysPanel.lblPress"));
        lblPressGeneric.setVisible(false);
        chbAlt.setText("Alt");
        chbCtrl.setText("Ctrl");
        chbShift.setText("Shift");
        chbWindows.setText("Windows");
        btnPressGeneric.setText(res.getString("toolsPanel.keysPanel.btnApply"));
        btnPressGeneric.addActionListener(this);
        btnPressGeneric.setMargin(new Insets(2,2,2,2));

        Map t = Utils.getKeyCodeTable();
        List v = new ArrayList(t.keySet());
        Object keys[] = v.toArray();
        Arrays.sort(keys);
        cbKeys.setModel(new DefaultComboBoxModel(keys));

        JPanel pnlGeneric = new JPanel();
        pnlGeneric.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), res.getString("toolsPanel.keysPanel.CreatePress")));
        pnlGeneric.setLayout(new GridBagLayout());
        pnlGeneric.add(lblGenericDesc, new GridBagConstraints(0, 0, 4, 1, 1.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 3, 0, 3), 0, 0));
        pnlGeneric.add(lblPressGeneric, new GridBagConstraints(0, 1, 1, 3, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 3, 0, 3), 0, 0));
        pnlGeneric.add(chbAlt, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        pnlGeneric.add(chbCtrl, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        pnlGeneric.add(chbShift, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        pnlGeneric.add(chbWindows, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        pnlGeneric.add(cbKeys, new GridBagConstraints(2, 1, 1, 4, 1.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 3, 0, 3), 0, 0));
        pnlGeneric.add(btnPressGeneric, new GridBagConstraints(3, 1, 1, 4, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 3, 0, 3), 0, 0));
        this.add(pnlGeneric);
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = null;
        if (e.getSource().equals(btnPress)) {
            cmd = ""+cbPress.getSelectedItem();
        } else if (e.getSource().equals(btnPressGeneric)) {
            cmd = "";
            boolean addPlus = false;
            if (chbAlt.isSelected()) {
                cmd += (addPlus ? "+" : "") + "Alt";
                addPlus = true;
            }
            if (chbCtrl.isSelected()) {
                cmd += (addPlus ? "+" : "") + "Ctrl";
                addPlus = true;
            }
            if (chbShift.isSelected()) {
                cmd += (addPlus ? "+" : "") + "Shift";
                addPlus = true;
            }
            if (chbWindows.isSelected()) {
                cmd += (addPlus ? "+" : "") + "Windows";
                addPlus = true;
            }
            if (addPlus) {
                cmd += "+";
            }
            cmd += cbKeys.getSelectedItem();
        }
        if (cmd != null) {
            if (recordingModule != null) {
                if (recordingModule.isEnabled()) {
                    recordingModule.insertLine("Press "+cmd, false, true, false);
                } else {
                    boolean isEditor = mainFrame.getDocumentTabbedPane().getActiveEditorPanel() != null;
                    if (isEditor) {
                        int option = 0;
                        Integer warningOption =
                                mainFrame.getUserConfiguration().getInteger("warning.insertKeyIntoEditor");
                        if (warningOption == null || warningOption.intValue() < 0) {
                            boolean isConnected = mainFrame.getClient().isConnected();
                            String text = isConnected
                                    ? res.getString("toolsPanel.keysPanel.warning.sendAndInsert")
                                    : res.getString("toolsPanel.keysPanel.warning.insert");
                            text += res.getString("toolsPanel.keysPanel.warning.insertNote");
                            Object options[] = new String[] {
                                res.getString("toolsPanel.keysPanel.warning.insertYes"), 
                                res.getString("toolsPanel.keysPanel.warning.insertNo")
                            };
                            option = Utils.showConfigurableMessageDialog(mainFrame,
                                    res.getString("toolsPanel.keysPanel.warning.insertTitle"),
                                    text,
                                    res.getString("toolsPanel.keysPanel.warning.insertOption"),
                                    "warning.insertKeyIntoEditor",
                                    options,
                                    0);
                        } else {
                            option = warningOption.intValue();
                        }

                        if (option == 0) {
                            recordingModule.insertLine("Press "+cmd, false, false, false);
                        }
                    }
                }
            }
            RemoteDesktopClient rfb = mainFrame.getClient();
            if (rfb != null && rfb.isConnected()) {

                try {
                    ScriptingContext t = new ScriptingContextImpl();
                    t.putAll(mainFrame.getScriptHandler().createDefaultContext());
                    if (cmdHandler == null) {
                        cmdHandler = new PressCommand();
                        cmdHandler.addCommandListener(mainFrame.getDesktopPnl());
                    }
                    List l = new ArrayList(2);
                    l.add(cmd);
                    cmdHandler.execute(l, null, t);
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (SyntaxErrorException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
