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
package com.tplan.robot.gui.plugin;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.gui.components.FileExtensionFilter;
import com.tplan.robot.plugin.CodeConflictException;
import com.tplan.robot.plugin.DependencyMissingException;
import com.tplan.robot.plugin.HigherVersionInstalledException;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.plugin.PluginInfo;
import com.tplan.robot.plugin.UnsupportedVersionException;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.util.Utils;
import java.awt.event.ItemEvent;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.util.ResourceBundle;

/**
 * Plugin Manager allowing to view, enable, disable, install and uninstall plugins
 * through GUI.
 * @product.signature
 */
public class PluginDialog extends JDialog
        implements ActionListener, ListSelectionListener, HyperlinkListener, ItemListener, TableModelListener {

    JButton btnClose = new JButton(ApplicationSupport.getString("btnClose"));
    JButton btnHelp = new JButton(ApplicationSupport.getString("btnHelp"));
    JButton btnActivate = new JButton(ApplicationSupport.getString("com.tplan.robot.pluginGui.enable"));
    JButton btnClear = new JButton(ApplicationSupport.getString("com.tplan.robot.pluginGui.clear"));
    PluginDetailPanel pnlPlugin1 = new PluginDetailPanel();
    PluginDetailPanel pnlPlugin2 = new PluginDetailPanel();
    JTable tableCurrent = new JTable();
    JTable tableAvailable = new JTable();
    InstalledPluginTableModel modelCurrent = new InstalledPluginTableModel(tableCurrent);
    AvailablePluginTableModel modelAvailable = new AvailablePluginTableModel(tableAvailable);
    JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    JButton btnFromFile = new JButton();
    JFileChooser chooser = new JFileChooser();
    JCheckBox cbDisplayInternals = new JCheckBox();
    private boolean restartWindowShown = false;

    public PluginDialog(MainFrame owner, String title, boolean modal) {
        super(owner, title, modal);
        initDlg();
        modelCurrent.addTableModelListener(this);
        modelAvailable.addTableModelListener(this);
    }

    void resetTabLabels() {
        if (tabPane.getTabCount() > 0) {
            String s = ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.installed");
            s = MessageFormat.format(s, modelCurrent.getRowCount());
            tabPane.setTitleAt(0, s);
        }
        if (tabPane.getTabCount() > 1) {
            String s = ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.available");
            s = MessageFormat.format(s, modelAvailable.getRowCount());
            tabPane.setTitleAt(1, s);
        }
    }

    private void initDlg() {
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        Utils.registerDialogForEscape(this, btnClose);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel pnlContent = new JPanel();
        pnlContent.setLayout(new BorderLayout(5, 5));

        JPanel pnlTab1 = new JPanel(new BorderLayout());
        JSplitPane tab1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JScrollPane scroll1 = new JScrollPane(tableCurrent);
        scroll1.setPreferredSize(new Dimension(400, 400));
        tab1.add(scroll1, JSplitPane.LEFT);
        pnlPlugin1.clean();
        pnlPlugin2.clean();
        tab1.add(pnlPlugin1, JSplitPane.RIGHT);
        pnlTab1.add(tab1, BorderLayout.CENTER);

        JPanel pnlNorth1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlTab1.add(pnlNorth1, BorderLayout.NORTH);
        cbDisplayInternals.setText(res.getString("com.tplan.robot.pluginGui.showInternalPlugins"));
        cbDisplayInternals.setSelected(modelCurrent.isIncludeInternals());
        cbDisplayInternals.addItemListener(this);
        pnlNorth1.add(cbDisplayInternals);

        tabPane.add("", pnlTab1);

        JPanel pnlTab2 = new JPanel(new BorderLayout());
        JSplitPane tab2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JScrollPane scroll2 = new JScrollPane(tableAvailable);
        scroll2.setPreferredSize(new Dimension(400, 400));
        tab2.add(scroll2, JSplitPane.LEFT);
        tab2.add(pnlPlugin2, JSplitPane.RIGHT);
        pnlTab2.add(tab2, BorderLayout.CENTER);

        JPanel pnlNorth2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnFromFile.setText(res.getString("com.tplan.robot.pluginGui.btnAdd"));
        btnFromFile.addActionListener(this);
        pnlNorth2.add(btnFromFile);
        btnClear.addActionListener(this);
        pnlNorth2.add(btnClear);
        pnlTab2.add(pnlNorth2, BorderLayout.NORTH);

        tabPane.add("", pnlTab2);
        resetTabLabels();

        pnlContent.add(tabPane, BorderLayout.CENTER);
        this.setContentPane(pnlContent);

        // South panel
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnClose.addActionListener(this);
        south.add(btnHelp);
        btnHelp.addActionListener(this);
        south.add(btnClose);
        getRootPane().setDefaultButton(btnClose);

        pnlContent.add(south, BorderLayout.SOUTH);

        tableCurrent.setModel(modelCurrent);
        tableCurrent.getSelectionModel().addListSelectionListener(this);
        tableCurrent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableCurrent.getSelectionModel().setSelectionInterval(0, 0);

        pnlPlugin1.getBtnActivate().addActionListener(this);
        pnlPlugin1.getBtnInstall().addActionListener(this);
        pnlPlugin1.getBtnConfig().addActionListener(this);
        pnlPlugin1.getTxpDesc().addHyperlinkListener(this);
        pnlPlugin1.getTxpDesc().setMargin(new Insets(5, 5, 5, 5));
        pnlPlugin1.getBtnInstall().setText(res.getString("com.tplan.robot.pluginGui.btnUninstall"));
        Font f = this.getFont();
        f = new Font(f.getName(), Font.BOLD, f.getSize() + 2);
        pnlPlugin1.getLblName().setFont(f);

        tableAvailable.setModel(modelAvailable);
        tableAvailable.getSelectionModel().addListSelectionListener(this);
        tableAvailable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableAvailable.getSelectionModel().setSelectionInterval(0, 0);

        pnlPlugin2.getBtnActivate().setEnabled(false);
        pnlPlugin2.getBtnActivate().setVisible(false);
        pnlPlugin2.getBtnInstall().addActionListener(this);
        pnlPlugin2.getBtnInstall().setEnabled(false);
        pnlPlugin2.getBtnInstall().setText(res.getString("com.tplan.robot.pluginGui.btnInstall"));
        pnlPlugin2.getBtnConfig().setVisible(false);
        pnlPlugin2.getTxpDesc().addHyperlinkListener(this);
        pnlPlugin2.getTxpDesc().setMargin(new Insets(5, 5, 5, 5));
        pnlPlugin2.getLblName().setFont(f);

        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(new FileExtensionFilter(new String[] { "jar", "zip"},
                ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.fileFilterDesc")));
    }

    private void installPlugin(Plugin p, boolean enableRegression, boolean showPreinstallMsg) {
        if (p != null && p instanceof PluginInfo) {
            PluginManager pm = PluginManager.getInstance();
            PluginInfo pi = (PluginInfo) p;
            try {
                // If the plugin provides a pre-install message, display it
                if (showPreinstallMsg && pi.getMessageBeforeInstall() != null) {
                    String opt[] = {ApplicationSupport.getResourceBundle().getString("btnOk")};
                    JOptionPane.showOptionDialog(this, pi.getMessageBeforeInstall(),
                            ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.preinstallMsgTitle"),
                            JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opt, opt[0]);
                }

                pm.installPlugin(pi.getPluginClass().getCanonicalName(), pi.getLibraryUrl(), false, true);
                pm.savePluginMap();

                // If the plugin provides a post-install message, display it
                if (pi.getMessageAfterInstall() != null) {
                    String opt[] = {ApplicationSupport.getResourceBundle().getString("btnOk")};
                    JOptionPane.showOptionDialog(this, pi.getMessageAfterInstall(),
                            ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.postinstallMsgTitle"),
                            JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opt, opt[0]);
                }

                // The plugin requires restart -> show a message to the user
                if (pi.isRestartRequired()) {
                    if (!restartWindowShown) {
                        ResourceBundle resourceBundle = ApplicationSupport.getResourceBundle();
                        Object options[] = {
                            resourceBundle.getString("com.tplan.robot.pluginGui.restartButton"),
                            resourceBundle.getString("btnCancel"),};
                        int option = JOptionPane.showOptionDialog(this,
                                resourceBundle.getString("com.tplan.robot.pluginGui.restartRequiredMsg"),
                                resourceBundle.getString("com.tplan.robot.pluginGui.restartTitle"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                options,
                                options[0]);
                        switch (option) {
                            case 0:
                                Utils.restart((MainFrame) getOwner(), ApplicationSupport.getInputArguments());
                                break;
                            case 1:  // Restart was canceled -> display an icon in the status bar
                                ImageIcon icon = ApplicationSupport.getImageIcon("restart16.gif");
                                ((MainFrame) getOwner()).getStatusBar().getFieldLeft().setIcon(icon);
                                ((MainFrame) getOwner()).getStatusBar().getFieldLeft().setText(resourceBundle.getString("com.tplan.robot.pluginGui.restartTitle"));
                                return;
                        }
                    }
                }
            } catch (UnsupportedVersionException ex) {
                // This exception means that the plugin doesn't support current application version
                String opt[] = {ApplicationSupport.getResourceBundle().getString("btnOk")};
                JOptionPane.showOptionDialog(this, ex.getMessage(),
                        ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.errMsgTitle"),
                        JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE, null, opt, opt[0]);
            } catch (DependencyMissingException ex) {
                // This exception means that the plugin doesn't support current application version
                String opt[] = {ApplicationSupport.getResourceBundle().getString("btnOk")};
                JOptionPane.showOptionDialog(this, ex.getMessage(),
                        ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.errMsgTitle"),
                        JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE, null, opt, opt[0]);
            } catch (HigherVersionInstalledException ex) {
                // The plugin is already installed and has a higher version.
                // Warn user and ask whether to downgrade.
                String opt[] = {
                    MessageFormat.format(ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.btnDowngrade"), ex.getPluginInfo().getVersionString()),
                    ApplicationSupport.getResourceBundle().getString("btnCancel")
                };
                String msg = MessageFormat.format(
                        ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.higherVersionInstalled"),
                        ex.getInstalledPluginInfo().getVersionString(),
                        ex.getPluginInfo().getVersionString());
                int option = JOptionPane.showOptionDialog(this, msg,
                        ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.errMsgTitle"),
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, opt, opt[0]);
                if (option == 0 && enableRegression) { // Downgrade -> remove the old one and retry
                    pm.uninstallPlugin(ex.getInstalledPluginInfo());
                    installPlugin(p, false, false);
                }
            } catch (CodeConflictException ex) {
                if (enableRegression && handleCodeConflict(ex)) {
                    installPlugin(p, false, false);
                }
            } catch (Exception ex) {
                String s = ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.unknownErrorMsg");
                s = MessageFormat.format(s, pi.getDisplayName(), pi.getVersionString());
                Utils.showErrorDialog(
                        this,
                        ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.unknownErrorTitle"),
                        s,
                        ex);
            }
        }
    }

    private boolean handleCodeConflict(CodeConflictException ex) {
        // There's another plugin installed which returns the same code.
        // Ask user whether to disable it.
        String opt[] = {
            ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.btnDisable"),
            ApplicationSupport.getResourceBundle().getString("btnCancel")
        };
        String msg = MessageFormat.format(
                ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.codeConflict"),
                ex.getInstalledPluginInfo().getDisplayName(),
                ex.getInstalledPluginInfo().getVersionString());
        int option = JOptionPane.showOptionDialog(this, msg,
                ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.errMsgTitle"),
                JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE, null, opt, opt[0]);
        if (option == 0) { // Disable the old one and retry
            try {
                // Disable the old one and retry
                PluginManager.getInstance().setEnabled(ex.getInstalledPluginInfo(), false);
                return true;
            } catch (CodeConflictException ex1) {
                // This should never happen because we are not enabling
                ex.printStackTrace();
            }
        }
        return false;
    }

    public void actionPerformed(ActionEvent e) {
        PluginManager pm = PluginManager.getInstance();

        if (e.getSource().equals(btnClose)) {
            dispose();
        } else if (e.getSource().equals(btnHelp)) {
            ((MainFrame)getOwner()).showHelpDialog("gui.plugins", this);
        } else if (e.getSource().equals(pnlPlugin1.getBtnActivate())) {
            Plugin p = modelCurrent.getPlugin(tableCurrent.getSelectedRow());
            boolean isActive = pm.isEnabled(p);
            try {
                pm.setEnabled(p, !isActive);
            } catch (CodeConflictException ex) {
                if (handleCodeConflict(ex)) {
                    try {
                        pm.setEnabled(p, !isActive);
                    } catch (CodeConflictException ex1) {
                        // This should never happen because we have already disabled the conflicting plugin
                        ex.printStackTrace();
                    }
                }
            }
            try {
                pm.savePluginMap();
            } catch (FileNotFoundException exf) {
                exf.printStackTrace();
            }
        } else if (e.getSource().equals(pnlPlugin2.getBtnInstall())) {
            installPlugin(modelAvailable.getPlugin(tableAvailable.getSelectedRow()), true, true);
        } else if (e.getSource().equals(btnClear)) {
            modelAvailable.setAvailablePlugins(new ArrayList());
        } else if (e.getSource().equals(btnFromFile)) {
            String path = UserConfiguration.getInstance().getString("io.recentPluginDir");
            if (path != null && path.length() > 0) {
                File f = new File(path);
                if (!f.isDirectory()) {
                    f = f.getParentFile();
                }
                if (f.exists()) {
                    chooser.setCurrentDirectory(f);
                }
            }
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try {
                    List<PluginInfo> l = pm.getAvailablePlugins(f);
//                    System.out.println("Plugins: " + l);
                    modelAvailable.addAvailablePlugins(l);
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                path = f.isDirectory() ? f.getAbsolutePath() : f.getParent();
                UserConfiguration.getInstance().setString("io.recentPluginDir", path);
            }
        } else if (e.getSource().equals(pnlPlugin1.getBtnInstall())) {
            int index = tableCurrent.getSelectedRow();
            Plugin p = modelCurrent.getPlugin(index);
            if (p != null) {
                try {
                    pm.uninstallPlugin(p);
                    pm.savePluginMap();

                    // Select another line
                    int size = tableCurrent.getRowCount();
                    if (size > 0) {
                        if (index >= size) {
                            index = size - 1;
                        }
                        tableCurrent.setRowSelectionInterval(index, index);
                    }
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        } else if (e.getSource().equals(pnlPlugin1.getBtnConfig())) {
            PluginInfo p = modelCurrent.getPlugin(tableCurrent.getSelectedRow());
            if (p != null) {
                MainFrame f = (MainFrame) getOwner();
                f.showOptionsDialog(p.getDisplayName(), p.getGroupName());
            }
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource().equals(tableCurrent.getSelectionModel())) {
            if (tableCurrent.getSelectedRow() >= 0) {
                PluginInfo p = modelCurrent.getPlugin(tableCurrent.getSelectedRow());
                if (PluginManager.getInstance().isEnabled(p)) {
                    pnlPlugin1.getBtnActivate().setText(ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.disable"));
                } else {
                    pnlPlugin1.getBtnActivate().setText(ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.enable"));
                }
                boolean isBuiltIn = PluginManager.getInstance().isBuiltIn(p);
                pnlPlugin1.getBtnActivate().setEnabled(!isBuiltIn);
                pnlPlugin1.getBtnInstall().setEnabled(!isBuiltIn);
                pnlPlugin1.getTxpDesc().setContentType("text/html");
                pnlPlugin1.getTxpDesc().setText(getPluginHtmlDesc(p));
                pnlPlugin1.getLblName().setText(p == null ? " " : p.getDisplayName());
                pnlPlugin1.getBtnConfig().setEnabled(p == null ? false : p.isConfigurable());
            } else {
                pnlPlugin1.clean();
            }
        } else if (e.getSource().equals(tableAvailable.getSelectionModel())) {
            pnlPlugin2.getBtnInstall().setEnabled(false);
            if (tableAvailable.getSelectedRow() >= 0) {
                PluginInfo p = modelAvailable.getPlugin(tableAvailable.getSelectedRow());
                if (p != null) {
                    pnlPlugin2.getTxpDesc().setContentType("text/html");
                    pnlPlugin2.getTxpDesc().setText(getPluginHtmlDesc(p));
                    pnlPlugin2.getLblName().setText(p.getDisplayName());
                    pnlPlugin2.getBtnInstall().setEnabled(true);
                    pnlPlugin2.getBtnConfig().setEnabled(p.isConfigurable());
                } else {
                    pnlPlugin2.clean();
                }
            } else {
                pnlPlugin2.clean();
            }
        }
    }

    private String getPluginHtmlDesc(Plugin p) {
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        String s = "";
        if (p != null) {
            s += "<html><body>\n";
            String t;
            int[] v = p.getVersion();
            if (v != null) {
                t = "";
                for (int i : v) {
                    t += i + ".";
                }
                if (t.endsWith(".")) {
                    t = t.substring(0, t.length() - 1);
                }
                s += "\n<b>" + res.getString("com.tplan.robot.pluginGui.htmlVersion") + "</b> " + t + "<br>";
            }
            if (p instanceof PluginInfo) {
                t = ((PluginInfo) p).getLibraryUrl();
                if (t == null || t.length() == 0) {
                    t = res.getString("com.tplan.robot.pluginGui.htmlInternal");
                }
                s += "\n<b>" + res.getString("com.tplan.robot.pluginGui.htmlSourceURL") + "</b> " + t + "<br>";
            }
            t = p.getVendorName();
            if (t == null) {
                t = "&lt;" + res.getString("com.tplan.robot.pluginGui.htmlUnknown") + "&gt;";
            }
            s += "\n<b>" + res.getString("com.tplan.robot.pluginGui.htmlVendor") + "</b> " + t + "<br>";
            Date d = p.getDate();
            if (d != null) {
                DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
                s += "\n<b>" + res.getString("com.tplan.robot.pluginGui.htmlDate") + "</b> " + df.format(d) + "<br>";
            }
            t = p.getVendorHomePage();
            if (t != null) {
                if (t.startsWith("http://")) {
                    t = "<a href=\"" + t + "\">" + t + "</a>";
                }
                s += "\n<b>" + res.getString("com.tplan.robot.pluginGui.htmlHome") + "</b> " + t + "<br>";
            }
            t = p.getSupportContact();
            if (t != null) {
                s += "\n<b>" + res.getString("com.tplan.robot.pluginGui.htmlContact") + "</b> ";
                if (t.startsWith("mailto:")) {
                    s += "<a href=\"" + t + "\">" + t.substring("mailto:".length()) + "</a><br>";
                } else if (t.startsWith("http://")) {
                    s += "<a href=\"" + t + "\">" + t + "</a><br>";
                } else {
                    s += t + "<br>";
                }
            }
            t = p.getDescription();
            if (t != null) {
                s += "<br><b>" + res.getString("com.tplan.robot.pluginGui.htmlDescription") + "</b><br>" + t;
            }
            s += "</body></html>";
        }
        return s;
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JEditorPane pane = (JEditorPane) e.getSource();
            if (e instanceof HTMLFrameHyperlinkEvent) {
//                HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
//                HTMLDocument doc = (HTMLDocument) pane.getDocument();
//                doc.processHTMLFrameHyperlinkEvent(evt);
            } else {
                try {
                    if (Utils.execOpenURL(e.getURL().toString()) != 0) {
                        pane.setPage(e.getURL());
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource().equals(cbDisplayInternals)) {
            modelCurrent.setIncludeInternals(cbDisplayInternals.isSelected());
        }
    }

    public void tableChanged(TableModelEvent e) {
        resetTabLabels();
    }
}
