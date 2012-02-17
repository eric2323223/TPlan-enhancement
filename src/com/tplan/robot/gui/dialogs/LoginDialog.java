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

import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.remoteclient.RemoteDesktopServerListener;
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.gui.*;
import com.tplan.robot.remoteclient.rfb.*;
import com.tplan.robot.l10n.LocalizationSupport;
import com.tplan.robot.util.Utils;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.Splash;
import com.tplan.robot.gui.components.CustomHyperlinkListener;
import com.tplan.robot.gui.components.LanguageButton;
import com.tplan.robot.gui.components.TPlanPanel;
import com.tplan.robot.gui.preferences.DefaultPreferencePanel;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.RemoteDesktopClientFactory;
import com.tplan.robot.l10n.CustomPropertyResourceBundle;

import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.preferences.Preference;
import java.net.URISyntaxException;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Map;

/**
 * Login Dialog which allows to login to a desktop.
 * @product.signature
 */
public class LoginDialog extends JDialog
        implements ActionListener, WindowListener, KeyListener,
        DocumentListener, RfbConstants, RemoteDesktopServerListener, Runnable, ConfigurationKeys, ChangeListener {

    ResourceBundle res = ApplicationSupport.getResourceBundle();
    JPanel pnlLeft = new JPanel();
    JPanel pnlParams = new JPanel();
    JButton btnConnect = new JButton();
    JButton btnCancel = new JButton();
    Border border1;
    JPanel jPanel1 = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    FlowLayout flowLayout1 = new FlowLayout();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel lblTitle = new JLabel();
    JLabel lblTitle2 = new JLabel();
    JCheckBox chbReadOnly = new JCheckBox();
    JComboBox cmbProtocols = new JComboBox();
    TPlanPanel pnlImage = new TPlanPanel();
    DefaultPreferencePanel pnlClientParams;
    LanguageButton btnLang = new LanguageButton();
    private MainFrame mainFrame;
    private List<String> protocols;
    List<JButton> helpButtons = new ArrayList();
    RemoteDesktopClient temporaryClient;
    JTextPane txpError = new JTextPane();
    JScrollPane scrollError = new JScrollPane(txpError);
    Window window;
    Container contentPane;

    public LoginDialog(MainFrame frame) {
        super(frame, true);
        try {
            this.mainFrame = (MainFrame) frame;
            this.protocols = RemoteDesktopClientFactory.getInstance().getSupportedProtocols();
            this.contentPane = getContentPane();
            this.window = this;

            init();
            enableOkBtn();
            setLocationRelativeTo(getOwner());
            pack();
            Splash.pushToBack();
            window = this;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setWindow(Window window) {
        if (this.window != null) {
            this.window.removeWindowListener(this);
        }
        if (window == null) {
            this.window = this;
            window = this;
        } else {
            this.window = window;
        }
        window.addWindowListener(this);
        if (!equals(window)) {
            if (window instanceof JDialog) {
                JDialog dlg = (JDialog)window;
                Utils.registerDialogForEscape(dlg, btnCancel);
                dlg.getRootPane().setDefaultButton(btnConnect);
                dlg.setContentPane(contentPane);
            } else if (window instanceof JFrame) {
                JFrame frame = (JFrame) window;
                Utils.registerDialogForEscape(frame, btnCancel);
                frame.getRootPane().setDefaultButton(btnConnect);
                frame.setContentPane(contentPane);
            }
        } else {
            setContentPane(contentPane);
        }
    }

    public void clearErrors() {
        txpError.setText("\n\n");
    }

    /**
     * Implementation of the ActionListener interface.
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src.equals(btnConnect)) {

            btnConnect.setEnabled(false);
            Thread t = new Thread(this, res.getString("com.tplan.robot.gui.LoginDlg.windowTitle"));
            t.start();
        } else if (src.equals(btnCancel)) {

            // If the dialog gets canceled, clear the host and password fields
            RemoteDesktopClient client = mainFrame.getClient();
            if (client != null && client.isConnecting()) {
                try {
                    client.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
            window.dispose();

        } else if (e.getActionCommand().equals("com.tplan.robot.gui.LoginDlg.buttonHelp")) {
            mainFrame.showHelpDialog("gui.viewer_login", window);

        } else if (e.getActionCommand().equals("com.tplan.robot.gui.LoginDlg.buttonInstall")) {
            mainFrame.showHelpDialog("rn", window);

        } else if (e.getActionCommand().equals("com.tplan.robot.gui.LoginDlg.buttonTutorials")) {
            Utils.execOpenURL(ApplicationSupport.APPLICATION_HOME_PAGE + "/docs/tutorials/v2.0/");

        } else if (src.equals(cmbProtocols)) {
            protocolChanged();
        } else if (src.equals(btnLang)) {
            Locale l = btnLang.getSelectedLocale();
            ResourceBundle res = LocalizationSupport.getResourceBundle(l, ApplicationSupport.class, ApplicationSupport.APPLICATION_RESOURCE_BUNDLE_PREFIX);
            ApplicationSupport.setResourceBundle((CustomPropertyResourceBundle) res);
            if (mainFrame != null) {
                mainFrame.setResourceBundle(res);
            }
            resetLabels(res);
        }
    }

    public void setSelectedProtocol(String proto) {
        int index = Utils.indexOfIgnoreCase(protocols, proto);
        if (index >= 0) {
            cmbProtocols.setSelectedIndex(index);
        }
    }

    // Get the client login params from the GUI components
    private Map<String, Object> getClientParams() {
        Map<String, Object> params = new HashMap();

        CustomConfig cfg = new CustomConfig();
        pnlClientParams.savePreferences(cfg);
        for (Object key : cfg.getMap().keySet()) {
            params.put(key.toString(), cfg.getMap().get(key));
        }
        String proto = protocols.get(cmbProtocols.getSelectedIndex());
        String uri = params.containsKey(RemoteDesktopClient.LOGIN_PARAM_URI) ? params.get(RemoteDesktopClient.LOGIN_PARAM_URI).toString() : null;
        if (uri.indexOf(":/") < 0) {
            uri = proto + "://" + uri;
        }
        params.put(RemoteDesktopClient.LOGIN_PARAM_URI, uri);
        return params;
    }

    /**
     * Set selected URI. The method selects the appropriate protocol in the drop
     * down and prefills the host and port name (if supported by the client) from
     * the given URI. The method is used (1) preset the dialog to the last
     * connected server (a config option) and (2) to set up login params
     * when connecting to one of the "Recent Servers" (menu File->Reconnect).
     * @param uri an URI to connect to, for example "rfb://localhost:5901"
     * or "java://localhost".
     */
    public void setSelectedUri(String uri) {
        try {
            URI u = Utils.getURI(uri);
            int index = -1;
            String proto = u.getScheme();
            for (int i = 0; i < protocols.size(); i++) {
                if (proto.equalsIgnoreCase(protocols.get(i))) {
                    index = i;
                    break;
                }
            }

            if (index >= 0) {
                cmbProtocols.setSelectedIndex(index);
                CustomConfig cfg = new CustomConfig();
                int i = uri.indexOf("://");
                if (i > 0) {
                    uri = uri.substring(i + 3);
                }
                cfg.setString(RemoteDesktopClient.LOGIN_PARAM_URI, uri);
                pnlClientParams.loadPreferences(cfg);
                pnlClientParams.revalidate();
                window.repaint();
            } else {
                protocolChanged();
            }
        } catch (URISyntaxException ex) {
            protocolChanged();
        }
    }

    public void run() {
        clearErrors();
        RemoteDesktopClient client = mainFrame.getClient();

        // TODO: select the server in the drop down
        if (client != null && client.isConnected()) {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Rewrite the host, port and password only if the login dlg is showing.
        // If it is not showing, it is being invoked as a part of application start.
        try {
            if (window.isShowing()) {
                Map<String, Object> clientParams = getClientParams();
                URI uri = Utils.getURI(clientParams.get(RemoteDesktopClient.LOGIN_PARAM_URI).toString());
                client = RemoteDesktopClientFactory.getInstance().getClient(uri.getScheme());
                client.setLoginParams(clientParams);
                mainFrame.getScriptHandler().setClient(client);
                client.addServerListener(this);
                mainFrame.getUserConfiguration().updateListOfRecents(uri.toString(), IO_RECENT_SERVERS, MainFrame.MAX_DYNAMIC_MENU_ITEMS, false);
                UserConfiguration.saveConfiguration();
            }

            if (client.getProtocol().equalsIgnoreCase("rfb")) {
                Integer configValue = mainFrame.getUserConfiguration().getInteger("warning.displayWinLocalWarning");
                if ((configValue == null || configValue.intValue() < 0)) {
                    String svr = client.getHost();
                    int port = client.getPort();
                    boolean isWinLocalWarningNeeded = Utils.isWindowsLocalhost(svr, port);
                    if (isWinLocalWarningNeeded) {
                        String firstButton = res.getString("com.tplan.robot.gui.LoginDlg.btnReturnToLogin");
                        if (!window.isShowing()) {
                            firstButton = res.getString("com.tplan.robot.gui.LoginDlg.btnDoNotConnectAndDisplay");
                        }
                        Object options[] = {
                            firstButton,
                            res.getString("com.tplan.robot.gui.LoginDlg.btnConnectAnyway"),
                            res.getString("com.tplan.robot.gui.LoginDlg.btnExit")
                        };

                        int option = Utils.showConfigurableMessageDialog(window,
                                res.getString("com.tplan.robot.gui.LoginDlg.warningTitle"),
                                MessageFormat.format(res.getString("com.tplan.robot.gui.LoginDlg.localWindowsWarning"), ApplicationSupport.APPLICATION_NAME),
                                res.getString("com.tplan.robot.gui.LoginDlg.warningOption"),
                                "warning.displayWinLocalWarning",
                                options,
                                1);

                        switch (option) {
                            case 0:
                                btnConnect.setEnabled(true);
                                return;
                            case 1:
                                break;
                            case 2:
                                UserConfiguration.saveConfiguration();
                                System.exit(0);
                        }
                    }
                }
            }

            try {
                client.connect();
            } catch (PasswordRequiredException ex) {
                handleConnectError(ex);
            }
        } catch (Exception ex) {
            handleConnectError(ex);
        }
    }

    private void handleConnectError(Throwable ex) {
        RemoteDesktopClient client = mainFrame.getClient();

        Map<String, Object> map = getClientParams();
        String cs = client.getConnectString();
        if (cs != null) {
            String cs2 = (String) map.get(RemoteDesktopClient.LOGIN_PARAM_URI);
            if (cs2 == null || !cs.equalsIgnoreCase(cs2)) {
                setSelectedUri(cs);
            }
        }

        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (client == null || !client.isConsoleMode()) {
            String text;
            if (ex instanceof RemoteDesktopClient) {
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                text = sw.toString();
            } else {
                text = ex.getMessage();
            }
            txpError.setText(text);

            if (!window.isShowing()) {
                JFrame owner = (JFrame) window.getOwner();
                if (owner == null || owner.isShowing()) {
                    MainFrame.centerDlg(owner, this);
                } else {
                    Utils.centerOnScreen(this);
                }
                enableOkBtn();
                window.setVisible(true);
            } else {
                windowActivated(null);
            }
            reset();
        } else {
            System.out.println(ex.getMessage());
        }
    }

//    private String askForPassword(PasswordRequiredException ex) {
//        if (mainFrame.getClient().isConsoleMode()) {
//            System.out.println(ex.getMessage());
//            return null;
//        }
//        do {
//            PasswordDlg dlg;
//            if (!window.isShowing() && mainFrame != null && mainFrame.isShowing()) {
//                dlg = new PasswordDlg(mainFrame);
//            } else {
//                if (window instanceof JDialog) {
//                    dlg = new PasswordDlg(window);
//                }
//            }
//
//            dlg.setVisible(true);
//
//            if (dlg.canceled) {
//                return null;
//            } else {
//                String passwd = new String(dlg.txtPasswd.getPassword());
//                if (!"".equals(passwd)) {
//                    return passwd;
//                }
//            }
//        } while (true);
//    }
    /**
     * Enable or disable the OK button. It is only enabled when
     */
    private void enableOkBtn() {
        btnConnect.setEnabled(true);
        this.getRootPane().setDefaultButton(btnConnect);
    }

    public void reset() {
        // Set the default button
        getRootPane().setDefaultButton(btnConnect);

        // Reset the login field
        windowActivated(null);

        this.enableOkBtn();
        chbReadOnly.setSelected(mainFrame.getUserConfiguration().getBoolean("rfb.readOnly").booleanValue());
    }

    /**
     * Invoked when the user attempts to close the window
     * from the window's system menu.  If the program does not
     * explicitly hide or dispose the window while processing
     * this event, the window close operation will be cancelled.
     */
    public void windowClosing(WindowEvent e) {
        if (mainFrame == null || !mainFrame.isShowing()) {
            System.exit(0);
        } else {
            window.dispose();
        }
    }

    public void windowOpened(WindowEvent e) {
//        btnHelp.setEnabled(mainFrame != null && mainFrame.isHelpAvailable());
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
        if (!Utils.isMac()) {
            window.toFront();
        }
        btnLang.setEnabled(!mainFrame.isVisible());
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER && getRootPane().getDefaultButton() != null && getRootPane().getDefaultButton().isEnabled()) {
//            JRootPane rp = window instanceof JDialog ? ((JDialog)window).getRootPane() : ((JFrame)window).getRootPane();
//            rp.getDefaultButton().doClick();
            btnConnect.doClick();
        }
    }

    /**
     * Implementation of the DocumentListener interface. It is bound to the
     * editor of the editable drop down. If the text changes in any way, we need
     * to evaluate whether the Connect button can be enabled.
     *
     * @param e a DocumentEvent
     */
    public void changedUpdate(DocumentEvent e) {
        enableOkBtn();
    }

    /**
     * Implementation of the DocumentListener interface. It is bound to the
     * editor of the editable drop down. If the text changes in any way, we need
     * to evaluate whether the Connect button can be enabled.
     *
     * @param e a DocumentEvent
     */
    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    /**
     * Implementation of the DocumentListener interface. It is bound to the
     * editor of the editable drop down. If the text changes in any way, we need
     * to evaluate whether the Connect button can be enabled.
     *
     * @param e a DocumentEvent
     */
    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    /**
     * Set label of the Cancel button to "Cancel" (if the main frame is already visible)
     * or to "Work Offline" (initial startup when the main frame is not yet displayed).
     * @param isMainFrameVisible use true if the button label should be "Cancel", false if
     * "Work Offline" is expected.
     */
    public void setCancelLabel(boolean isMainFrameVisible) {
        String key = isMainFrameVisible ? "com.tplan.robot.gui.LoginDlg.buttonCancel" : "com.tplan.robot.gui.LoginDlg.buttonOffline";
        btnCancel.setText(res.getString(key));
    }

    /**
     * Handle a protocol change. The method will recreate the GUI controls for
     * the logon parameters required by the client.
     */
    private void protocolChanged() {

        if (cmbProtocols.getSelectedIndex() < 0) {
            return;
        }
        // Clear up the error text pane
        clearErrors();

        // Instantiate the selected client and get its list of logon preferences
        String proto = protocols.get(cmbProtocols.getSelectedIndex());
        temporaryClient = RemoteDesktopClientFactory.getInstance().getClient(proto);
        List<Preference> l = temporaryClient.getLoginParamsSpecification();

        // Create a new preference panel and initialize it
        pnlClientParams = new DefaultPreferencePanel();
        pnlClientParams.setUseHTML(true);
        pnlClientParams.setCreateBorders(false);
        pnlClientParams.setOpaque(false);
        pnlClientParams.setHyperLinkListener(new CustomHyperlinkListener((mainFrame)));
        for (Preference p : l) {
            pnlClientParams.addPreference(p);
        }
        pnlClientParams.init();

        // Alignment constants
        final int LEFT = 20;
        final int RIGHT = 20;
        final int GAP = 10;

        int y = 0;

        pnlParams.removeAll();

        // Remove all components
        pnlParams.add(lblTitle, new GridBagConstraints(0, y++, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, LEFT, 0, GAP), 0, 0));
        pnlParams.add(lblTitle2, new GridBagConstraints(0, y++, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 2 * LEFT, 20, GAP), 0, 0));
        pnlParams.add(new JLabel(ApplicationSupport.getString("com.tplan.robot.gui.LoginDlg.labelClient")), new GridBagConstraints(0, y, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, LEFT, 0, GAP), 0, 0));
        pnlParams.add(cmbProtocols, new GridBagConstraints(1, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, RIGHT), 0, 0));

        List<Component[]> cl = pnlClientParams.getComponentGridList();
        for (Component[] ca : cl) {
            if (ca.length == 1) {
                pnlParams.add(ca[0], new GridBagConstraints(0, y++, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 2 * LEFT, 0, RIGHT), 0, 0));
            } else if (ca.length == 2) {
                pnlParams.add(ca[0], new GridBagConstraints(0, y, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, LEFT, 0, GAP), 0, 0));
                pnlParams.add(ca[1], new GridBagConstraints(1, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, RIGHT), 0, 0));
                ca[1].addKeyListener(this);
            }
        }
        pnlParams.add(chbReadOnly, new GridBagConstraints(0, y++, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 2 * LEFT, 0, RIGHT), 0, 0));
        pnlParams.add(scrollError, new GridBagConstraints(0, y++, 2, 1, 1.0, 0.2, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 2 * LEFT, 0, RIGHT), 0, 25));
        pnlParams.add(jPanel1, new GridBagConstraints(0, y++, 2, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 25, RIGHT), 0, 0));
        pnlParams.revalidate();
        pnlParams.repaint();
        window.pack();
    }

    /**
     * Intitialize the GUI components.
     *
     * @throws Exception when anything gets wrong.
     */
    private void init() throws Exception {
        resetLabels(this.res);
        getRootPane().setDefaultButton(btnConnect);

        // Add help buttons to the image
        helpButtons.add(pnlImage.addHelpButton("com.tplan.robot.gui.LoginDlg.buttonHelp", this));
        helpButtons.add(pnlImage.addHelpButton("com.tplan.robot.gui.LoginDlg.buttonInstall", this));
        helpButtons.add(pnlImage.addHelpButton("com.tplan.robot.gui.LoginDlg.buttonTutorials", this));

        getContentPane().add(pnlImage, BorderLayout.EAST);
        // ========= End of image panel ========================================

        // Initialize protocols
        RemoteDesktopClientFactory fct = RemoteDesktopClientFactory.getInstance();
        RemoteDesktopClient cl;
        List<String> pd = new ArrayList();
        String defaultClient = "RFB";
        int defaultClientIndex = 0;
        for (String proto : protocols) {
            cl = fct.getClient(proto);
            if (cl instanceof Plugin) {
                pd.add(((Plugin) cl).getDisplayName());
                if (((Plugin) cl).getCode().equalsIgnoreCase(defaultClient)) {
                    defaultClientIndex = protocols.indexOf(proto);
                }
            } else {
                pd.add(proto);
            }
        }
        cmbProtocols.setModel(new DefaultComboBoxModel(pd.toArray()));
        cmbProtocols.setSelectedIndex(defaultClientIndex);

        jPanel1.setLayout(flowLayout1);
        pnlParams.setLayout(new GridBagLayout());
        btnConnect.addActionListener(this);
        btnConnect.addKeyListener(this);
        btnCancel.addActionListener(this);
        btnCancel.addKeyListener(this);
        pnlLeft.setLayout(borderLayout1);
        this.setModal(true);
        chbReadOnly.setOpaque(false);
        chbReadOnly.addChangeListener(this);

        Font f = lblTitle.getFont();
        lblTitle.setFont(new Font(f.getName(), Font.BOLD, f.getSize() + 2));
        lblTitle2.setForeground(new Color(0xf8, 0x4e, 0x10));
        lblTitle2.setFont(new Font(f.getName(), Font.BOLD, f.getSize() + 4));

        cmbProtocols.setEditable(false);
        cmbProtocols.addActionListener(this);

        jPanel1.setOpaque(false);
        btnLang.addActionListener(this);
        jPanel1.add(btnLang);
        jPanel1.add(btnConnect);
        jPanel1.add(btnCancel);

        txpError.setForeground(Color.red);
        txpError.setOpaque(true);
        txpError.setEditable(false);
        clearErrors();
        scrollError.setOpaque(false);
        scrollError.setBorder(null);
//        txpError.setMinimumSize(new Dimension(100, 40));
//        txpError.setPreferredSize(new Dimension(100, 40));

        pnlParams.setOpaque(false);

        pnlLeft.add(pnlParams, BorderLayout.SOUTH);
        pnlLeft.setOpaque(true);
        pnlLeft.setBackground(Color.WHITE);

        getContentPane().add(pnlLeft, BorderLayout.CENTER);
        addWindowListener(this);
        List<String> v = mainFrame.getUserConfiguration().getListOfStrings(IO_RECENT_SERVERS);
        if (v.size() > 0) {
            setSelectedUri(v.get(0));
        } else {
            protocolChanged();
        }
        Utils.registerDialogForEscape(this, btnCancel);
    }

    public void resetLabels(ResourceBundle res) {
        this.res = res;
        this.setTitle(MessageFormat.format(res.getString("com.tplan.robot.gui.LoginDlg.title"), ApplicationSupport.APPLICATION_NAME));
        lblTitle.setText(MessageFormat.format(
                res.getString("com.tplan.robot.gui.LoginDlg.logonTitle"),
                ApplicationSupport.APPLICATION_NAME, ApplicationSupport.APPLICATION_VERSION));
        lblTitle2.setText(res.getString("com.tplan.robot.gui.LoginDlg.logonTitle2"));
        btnConnect.setText(res.getString("com.tplan.robot.gui.LoginDlg.buttonOk"));
        btnCancel.setText(res.getString("com.tplan.robot.gui.LoginDlg.buttonOffline"));
        chbReadOnly.setText(res.getString("com.tplan.robot.gui.LoginDlg.checkReadOnly"));

        for (JButton btn : helpButtons) {
            btn.setText(res.getString(btn.getActionCommand()));
        }
        protocolChanged();
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource().equals(chbReadOnly)) {
            mainFrame.getUserConfiguration().setBoolean("rfb.readOnly", new Boolean(chbReadOnly.isSelected()));
        }
    }

    public void serverMessageReceived(RemoteDesktopServerEvent evt) {
        int type = evt.getMessageType();
        if (type == RemoteDesktopServerEvent.SERVER_CONNECTED_EVENT) {
            // The RFB module successfuly connected, dispose this window
            if (window.isShowing()) {
                window.dispose();
            }
        } else if (type == RemoteDesktopServerEvent.SERVER_DISCONNECTED_EVENT) {
        }
    }

    private class PasswordDlg extends JDialog implements ActionListener {

        JButton btnOk = new JButton(ApplicationSupport.getResourceBundle().getString("btnOk"));
        JButton btnCancel = new JButton(ApplicationSupport.getResourceBundle().getString("btnCancel"));
        JPasswordField txtPasswd = new JPasswordField();
        boolean canceled = true;

        PasswordDlg(Frame owner) {
            super(owner);
            init();
        }

        PasswordDlg(Dialog owner) {
            super(owner);
            init();
        }

        private void init() {
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            setModal(true);
            getRootPane().setDefaultButton(btnOk);
            Utils.registerDialogForEscape(this, btnCancel);
            getContentPane().setLayout(new BorderLayout());
            btnOk.addActionListener(this);
            btnCancel.addActionListener(this);
            JPanel pnl = new JPanel(new GridBagLayout());
            JTextPane txa = new JTextPane();
            Font f = txa.getFont();
            txa.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
            txa.setText(ApplicationSupport.getResourceBundle().getString("com.tplan.robot.gui.LoginDlg.labelPasswdRequired"));
            txa.setEditable(false);
            txa.setOpaque(false);
            pnl.add(txa, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
            pnl.add(txtPasswd, new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
            getContentPane().add(pnl, BorderLayout.CENTER);
            JPanel pnlSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));
            pnlSouth.add(btnOk);
            pnlSouth.add(btnCancel);
            getContentPane().add(pnlSouth, BorderLayout.SOUTH);
            pack();
            setLocationRelativeTo(getOwner());
            txtPasswd.requestFocus();
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(btnOk)) {
                canceled = false;
            }
            dispose();
        }
    }

    private class CustomConfig extends UserConfiguration {

        CustomConfig() {
            super();
        }

        Map getMap() {
            return configuration;
        }
    }
}
