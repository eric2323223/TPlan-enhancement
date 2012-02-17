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
package com.tplan.robot.gui;

import com.tplan.robot.scripting.ExecOrCompileThread;
import com.tplan.robot.remoteclient.rfb.RfbConstants;
import com.tplan.robot.remoteclient.RemoteDesktopServerListener;
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.AutomatedRunnableImpl;
import com.tplan.robot.HelpDispatcher;
import com.tplan.robot.HelpManager;
import com.tplan.robot.Splash;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.gui.dialogs.*;
import com.tplan.robot.gui.toolspanel.RecordingPanel;
import com.tplan.robot.scripting.commands.CommandEvent;
import com.tplan.robot.scripting.commands.impl.PressCommand;
import com.tplan.robot.scripting.commands.impl.ScreenshotCommand;
import com.tplan.robot.preferences.*;
import com.tplan.robot.gui.editor.*;
import com.tplan.robot.gui.preferences.PreferenceDialog;
import com.tplan.robot.gui.components.DrawPanel;
import com.tplan.robot.gui.components.FileExtensionFilter;
import com.tplan.robot.gui.components.ImageDialog;
import com.tplan.robot.gui.components.PreferencePanelDialog;
import com.tplan.robot.gui.components.WelcomePanel;
import com.tplan.robot.gui.plugin.PluginDialog;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.RemoteDesktopClientFactory;
import com.tplan.robot.remoteclient.rfb.ShutdownHook;
import com.tplan.robot.scripting.FileBasedJavaConverter;
import com.tplan.robot.scripting.PauseRequestException;
import com.tplan.robot.scripting.RecordingModule;
import com.tplan.robot.scripting.ScriptListener;
import com.tplan.robot.scripting.TestWrapper;
import com.tplan.robot.scripting.TokenParserImpl;
import com.tplan.robot.scripting.commands.CommandListener;
import com.tplan.robot.scripting.JavaTestScriptConverter;
import com.tplan.robot.scripting.JavaTestScriptConverterFactory;
import com.tplan.robot.scripting.JavaTestScript;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.StopRequestException;
import com.tplan.robot.scripting.commands.impl.CompareToCommand;
import com.tplan.robot.scripting.commands.impl.WaitforCommand;
import com.tplan.robot.scripting.interpret.java.JavaTestScriptInterpret;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.util.DocumentUtils;
import com.tplan.robot.util.Utils;

import java.net.URISyntaxException;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Top level window representing {@product.name} GUI.
 * @product.signature
 */
public class MainFrame extends JFrame
        implements WindowListener, ActionListener, RfbConstants, GUIConstants, ConfigurationKeys,
        ConfigurationChangeListener, PropertyChangeListener, RemoteDesktopServerListener,
        HelpProvider, ScriptListener, CommandListener {

    final String MENU_AND_TOOLBAR_PROPERTIES = "MenuAndToolbar.properties";
    private static ResourceBundle resourceBundle;
    private RemoteDesktopClient client;
    private DesktopViewer desktopPnl;
    private JToolBar toolBar;
    private StatusBar statusBar;
    private ActionManager actionManager;
    private LoginDialog loginDlg;
    private DocumentTabbedPane tabbedPane;
    private File currentDirectory;
    private JSplitPane splitPane;
    private JSplitPane globalSplitPane;
    private JSplitPane leftSplitPane;
    private JPanel pnlWrap;
    private ValidationMessagePanel msgPanel;
    private boolean followExecTrace = true;
    private boolean exportInProgress = false;
    private int exitCode = 0;
    private PreferenceDialog optionsDlg;
//    private VariableBrowserDialog variableDlg;
    private PressCommand.DisplaySupportedKeysAction keysAction;//    private boolean manuallyStopped = false;
    private Boolean resetReadOnlyModeAfterExecution = null;
    RecordingModule rec;
    DesktopGlassPanel glassPanel;
    RecordingPanel pnlTools;
    SearchDialog dlgFind;
    GoToLineDlg dlgGoToLine;
    ScreenshotDialog dlgScreenshot;
    ComparetoDialog dlgCompareto;
    WaitforDialog dlgWaitfor;
    JScrollPane desktopScrollPane;
    JButton scrollBtn = new JButton();
    Map guiParams;
    ScriptManager scriptManager;
    private AutomatedRunnableImpl process;
    UserConfiguration cfg;
    private DrawPanel drawPanel;
    Element editedElement;
    private boolean stepMode = false;
    WelcomePanel pnlwelcome = new WelcomePanel(this);
    private boolean minimized = false;
    private int frameState = JFrame.NORMAL;
    private static HelpDispatcher helpBroker = null;
    private Rectangle frameLocation = null;
    private static MainFrame instance;
    private javax.swing.filechooser.FileFilter scriptFileFilter;
    private javax.swing.filechooser.FileFilter javaFileFilter;
    private List<Component> desktopPaints = new ArrayList();
    private List<GuiComponent> guiPlugins;
    public static final boolean OPEN_SOURCE = ApplicationSupport.APPLICATION_NAME.equals("T-Plan Robot");

    public MainFrame(AutomatedRunnableImpl process, Map guiParams, UserConfiguration cfg) {
        instance = this;
        this.process = process;
        this.cfg = cfg;
        this.scriptManager = process.getScriptHandler();
        this.scriptManager.addScriptListener(this);
        setClient(process.getClient());
        this.guiParams = guiParams;
//        resetReadOnlyModeAfterExecution = false; = cfg.getBoolean("rfb.readOnly").booleanValue();

        // If the default T-Plan Robot script file extension is defined,
        // initialize a shared instance of file filter to be used by all file choosers
        if (DEFAULT_TPLAN_ROBOT_FILE_EXTENSION != null) {
            scriptFileFilter = new FileExtensionFilter(
                    new String[]{DEFAULT_TPLAN_ROBOT_FILE_EXTENSION},
                    MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.scriptFileFilterDesc"),
                    ApplicationSupport.APPLICATION_NAME, DEFAULT_TPLAN_ROBOT_FILE_EXTENSION));
        }

        // Initialize the shared *.java file filter
        javaFileFilter = new FileExtensionFilter(
                new String[]{"java"},
                ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.javaFileFilterDesc"));

        helpBroker = HelpManager.getProvider();

        // Init the application icons
        try {
            setIconImages(Utils.getRobotIcons());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Init the help window before login
        helpBroker.init(this);

        if (!guiParams.containsKey("nologin")) {  // "nologin" CLI option skips the login dlg

            // Only run the login routine if (1) there's no script to run passed from
            // CLI or (2) there's a client which already host name prefilled.
            // This condition makes sure that login is not run for scripts
            // which handle connection to server on their own through Connect command.
            Object scriptToRun = scriptManager.getScriptToRun();
            if (scriptToRun == null || (scriptToRun != null && client != null && client.hasSufficientConnectInfo())) {
                login(null, false);
            }
        }
        getContentPane().removeAll();

        resourceBundle = ApplicationSupport.getResourceBundle();

        // Intitialize components
        init();
        if (client != null && client.isConnected()) {
            Dimension d = new Dimension(client.getDesktopWidth(), client.getDesktopHeight());//pnlWrap.getPreferredSize();
            resetDesktopScrollSize(d.width, d.height);
        }

        boolean showTools = cfg.getBoolean("ui.mainframe.displayToolPanel").booleanValue();
        if (showTools) {
            Object obj = actionManager.getMenuItem("toolpanel");
            if (obj != null && obj instanceof JComponent) {
                ((JCheckBoxMenuItem) obj).setSelected(true);
            }
        }
        setToolPanelVisible(showTools);

        // Init bindings of GUI components and help IDs
        helpBroker.initComponentHelpIds();

        // Initialize all pluggable GUI components
        try {
            guiPlugins = GuiComponentFactory.getInstance().getComponentInstances();
            for (GuiComponent c : guiPlugins) {
                try {
                    c.setMainFrame(this);
                    if (c instanceof MenuStateListener) {
                        ((MenuStateListener) c).menuStateChanged(this);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Boolean b = UserConfiguration.getInstance().getBoolean("ui.mainframe.maximize");
        if (b != null && b) {
            this.setExtendedState(this.getExtendedState() | MAXIMIZED_BOTH);
        }

        Utils.centerOnScreen(this);
    }

    /**
     * @return the helpBroker
     */
    public static Object getHelpBroker() {
        // This somewhat clumsy construction returns null if the help broker (dispatcher) is
        // null or it returns that help is not available. This is intended to keep
        // compatibility with previous versions where GUI components enabled the Help
        // buttons when the help broker was not null.
        return helpBroker != null && helpBroker.isHelpAvailable() ? helpBroker : null;
    }

    /**
     * Overridden method to dispose the splash screen if needed.
     * @param visible true shows the main frame, false hides.
     */
    public void setVisible(boolean visible) {
        Splash.close();
        super.setVisible(visible);
    }

    /**
     * Get instance of this frame. If the frame hasn't been created yet or the
     * tool runs in CLI mode, the method returns null.
     *
     * @return the frame instance.
     */
    public static MainFrame getInstance() {
        return instance;
    }

    /**
     * <p>This method invokes the
     * {@link com.tplan.robot.CustomHelpBroker#setHelpIDString(javax.swing.JComponent, java.lang.String) setHelpIDString()}
     * method of the <code>CustomHelpBroker</code> class through the Java Reflection API.
     * Because we invoke the method this way, we can catch any error resulting from missing JavaHelp classes.
     * This makes sure that the tool keeps working even when the JavaHelp library is not
     * present in the JVM class path.
     *
     * @param component any Swing component which is part of application GUI (panel, button, toolbar, menu,...).
     * @param helpId a help topic ID. To get the list
     * of help IDs see the <code>Map.jhm</code> file located
     * in the <code>com.tplan.robot.helpset</code> package.
     */
    public void setHelpId(JComponent component, String helpId) {
        helpBroker.setHelpId(component, helpId);
    }

    /**
     * <p>This method invokes the
     * {@link com.tplan.robot.CustomHelpBroker#show(java.lang.String, java.lang.Boolean) show()}
     * method of the <code>CustomHelpBroker</code> class through the Java Reflection API.
     * Because we invoke the method this way, we can catch any error resulting from missing JavaHelp classes.
     * This makes sure that the tool keeps working even when the JavaHelp library is not
     * present in the JVM class path.
     *
     * @param owner owner of the help window, typically the Login Dialog or the main application frame.
     * @param helpKey a help topic ID. To get the list
     * of help IDs see the <code>Map.jhm</code> file located
     * in the <code>com.tplan.robot.helpset</code> package.
     */
    public void showHelpDialog(String helpKey, Object owner) {
        helpKey = helpKey == null || "".equals(helpKey) ? "doc.collection" : helpKey;
        boolean modal = owner != null && owner instanceof JDialog ? true : false;
        helpBroker.show(helpKey, this, modal);
    }

    /**
     * Overridden method to make sure that the window size doesn't exceed the screen size.
     */
    @Override
    public void pack() {
        super.pack();
        this.setDefaultLookAndFeelDecorated(true);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = ge.getMaximumWindowBounds();
        if (bounds.width < getWidth() || bounds.height < getHeight()) {
            setSize(Math.min(bounds.width, getWidth()), Math.min(bounds.height, getHeight()));
        }
        this.setMaximizedBounds(bounds);
    }

    /**
     * Convert a test script in the active editor to Java.
     * @param src source editor. It must contain a proprietary test script.
     * @param target target editor. If it is null, a new untitled editor will
     * be created. If it is the same as the source editor, the content will be changed.
     * @return the target editor (it may be a new one).
     */
    public EditorPnl exportToJava(EditorPnl src, EditorPnl target) {
        // Get the preferred converter plugin code and instantiate the converter
        exportInProgress = true;
        menuEnabler();
        try {
            String pluginCode = cfg.getString("javaconverter.preferredPluginCode");
            if (pluginCode == null || !JavaTestScriptConverterFactory.getInstance().getAvailableCodes().contains(pluginCode)) {
                pluginCode = "default";
            }
            JavaTestScriptConverter g = JavaTestScriptConverterFactory.getInstance().getConverter(pluginCode);

            // If the 'Display preferences' flag is true, display the converter options
            Boolean displayPrefs = cfg.getBoolean("javaconverter.displayPreferences");
            if (g instanceof Configurable && (displayPrefs == null || displayPrefs)) {
                String title = ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.preferencesPanelTitle");
                PreferencePanelDialog dlg = new PreferencePanelDialog(
                        (Configurable) g,
                        this,
                        MessageFormat.format(title, ((Plugin) g).getDisplayName()),
                        true);
                dlg.pack();
                dlg.setLocationRelativeTo(this);
                dlg.setVisible(true);
                if (dlg.isCanceled()) {
                    exportInProgress = false;
                    menuEnabler();
                    return null;
                }
            }

            // Get the converter preferences
            String packageName = cfg.getString("javaconverter.packageName");
            String className = cfg.getString("javaconverter.className");

            StringWriter wr = new StringWriter();
            try {
                // New approach to enable file based converters (March 2011)
                // If the converter implements the FileBasedJavaConverter interface,
                // use it rather than the legacy one.
                if (g instanceof FileBasedJavaConverter) {
                    try {
                        File[] files = ((FileBasedJavaConverter) g).convert(src.getFile(), src.getEditor().getStyledDocument(), className, packageName, wr);
                        if (files != null && files.length > 0) {
                            File f;
                            for (int i = files.length - 1; i >= 0; i--) {
                                f = files[i];
                                target = openScript(f);
                            }
                            if (target != null) {
                                exportInProgress = false;
                                return target;
                            }
                        }  // otherwise proceed to open the default empty editor
                    } catch (StopRequestException e) {
                        exportInProgress = false;
                        return null;  // This is OK because the user canceled the process
                    }
                } else {
                    g.convert(src.getTestScript(), className, packageName, src.getEditor().getStyledDocument(), wr);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (target == null) {
                target = newScript(TestScriptInterpret.TYPE_JAVA);
            }
            target.getEditor().setText(wr.getBuffer().toString());

            // Bug fix in 2.0.3 - compile the code
            target.compile();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        exportInProgress = false;
        menuEnabler();
        return target;
    }

    private void minimize() {
        if (!minimized && getToolBar() != null) {
            // TODO: change to setExtendedState(); backup the state and restore it after execution
            frameState = getExtendedState();
            frameLocation = new Rectangle(getLocationOnScreen(), getSize());
            setExtendedState(JFrame.NORMAL);
            Utils.setToolbarButtonsVisible(getToolBar(), null, false);
            Utils.setToolbarButtonsVisible(getToolBar(), new String[]{"replay", "replayselection", "step", "pause", "stop", "followexectrace"}, true);
            Utils.setVisibilityOfComponents(getContentPane(), new Object[]{getToolBar()}, false);
            pack();
            minimized = true;
        }
    }

    private void minimizeIfConfigured() {
        int flag = cfg.getInteger("scripting.minimizeForLocalDesktop");
        if (flag < 0) {
            Object options[] = {ApplicationSupport.getString("scripting.minimize.yes"), ApplicationSupport.getString("scripting.minimize.no")};
            int result = Utils.showConfigurableMessageDialog(this,
                    ApplicationSupport.getString("scripting.minimize.title"),
                    ApplicationSupport.getString("scripting.minimize.text"),
                    ApplicationSupport.getString("scripting.minimize.rememberOption"),
                    "scripting.minimizeForLocalDesktop",
                    options,
                    0);
            if (result > 0) {
                return;
            }
            flag = 0;
        }
        if (flag == 0) {
            minimize();
        }
    }

    private void maximizeIfMinimized() {
        if (minimized) {
            Utils.setToolbarButtonsVisible(getToolBar(), null, true);
            Utils.setVisibilityOfComponents(getContentPane(), null, true);
            if (frameLocation != null) {  // Restore the previous size and location
                setSize(frameLocation.getSize());
                setLocation(frameLocation.getLocation());
            } else {
                pack();
            }
            setExtendedState(frameState);
            minimized = false;
        }
    }

    /**
     * Run a script specified among the CLI arguments.
     */
    private void runCliScript() {

        // Look if there is a script to be run automatically
        final Object script = scriptManager.getScriptToRun();
        if (script != null && scriptManager.getExecutingTestScripts().size() == 0) {

            Runnable r = new Runnable() {

                public void run() {
                    Integer param = cfg.getInteger("scripting.delayBeforeAutomaticExecutionSeconds");
                    int timeout = param == null ? 15 : param.intValue();

                    // Bug 2926140: do not display the window at all if the time out is zero
                    TimeOutDialog dlg = null;
                    if (timeout > 0) {
                        dlg = new TimeOutDialog(MainFrame.this, script.toString(), 1000 * timeout, false,
                                ApplicationSupport.getString("options.scripting.execute"), ApplicationSupport.getString("options.scripting.scripting"));
                        Utils.centerOnScreen(dlg);
                        dlg.setVisible(true);
                    }

                    if (dlg == null || (!dlg.isCanceled() && (getActionManager() != null))) {

                        // If a script has been passed via CLI arguments, open it in the editor
                        if (script != null) {
                            String installPath = Utils.getInstallPath();
                            File f = null;
                            if (script instanceof String || script instanceof File) {
                                f = new File(script.toString());
                            } else if (script instanceof TestWrapper) {
                                f = new File(((TestWrapper) script).getTestSource());
                            }
                            if (f != null && !f.isAbsolute() && installPath != null) {
                                String path = installPath;
                                if (!path.endsWith(File.separator)) {
                                    path += File.separator;
                                }
                                f = new File(path + script);
                            }

                            if (script instanceof String) {
                                if (f != null && f.exists() && f.canRead() && f.isFile()) {
                                    OpenAction action = new OpenAction(f.toString(), MainFrame.this);
                                    action.setEditorFile(f.getAbsolutePath());
                                } else {
                                    JOptionPane.showMessageDialog(MainFrame.this, MessageFormat.format(
                                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.autoScriptNotFound"), Utils.getFullPath(f)),
                                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.autoScriptNotFoundTitle"), JOptionPane.ERROR_MESSAGE);
                                    scriptManager.setScriptToRun(null);
                                    return;
                                }
                            } else if (script instanceof JavaTestScript) {
                                final JavaTestScriptInterpret interpret = new JavaTestScriptInterpret();
                                interpret.setScriptManager(scriptManager);
                                interpret.setTestInstance((JavaTestScript) script);
                                Thread thread = new ExecOrCompileThread(interpret, true, MainFrame.this);
                                thread.start();
                                return;
                            }
                        }
                        final JMenuItem item = (JMenuItem) getActionManager().getMenuItem("replay");
                        if (item != null && item.isEnabled()) {
                            item.doClick();
                        }
                    } else {
                        scriptManager.setScriptToRun(null);
                    }
                }
            };
            Thread t = new Thread(r, "MainFrame: runCliScript()");
            t.start();
        }
    }

    private void init() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        JOptionPane.setRootFrame(this);
        setTitle(client);
        addWindowListener(this);
        createInitialContent();
    }

    private LoginDialog getLoginDlg() {
        if (loginDlg == null) {
            loginDlg = new LoginDialog(this);
            loginDlg.reset();
            loginDlg.pack();
        }
        return loginDlg;
    }

    void login(String uri, boolean clearErrors) {
        getLoginDlg(); // Initialize the dialog if it hasn't been created yet
        if (clearErrors) {
            loginDlg.clearErrors();
        }
        if (uri != null) {
            loginDlg.setSelectedUri(uri);
        }
        if (client == null || !client.hasSufficientConnectInfo()) {
            showConnectDialog();
            Utils.centerOnScreen(this);
        } else {
            JFrame frame = null;
            if (!this.isShowing()) {
                frame = new JFrame(loginDlg.getTitle());
                frame.setIconImages(Utils.getRobotIcons());
                loginDlg.setWindow(frame);
                frame.pack();
                frame.setLocationRelativeTo(null);
            }
            Thread t = new Thread(loginDlg, "MainFrame: login()");
            t.run();
            if (frame != null) {
                while (frame.isVisible()) {
                    Utils.sleep(50);
                }
                frame.setContentPane(new JPanel());
                frame.dispose();
                loginDlg.setWindow(null);
            }
        }
    }

    /**
     * Show the login dialog and connect to a VNC server
     */
    private void showConnectDialog() {
        LoginDialog dlg = getLoginDlg();
        dlg.reset();
        dlg.clearErrors();
        dlg.validate();
        if (this.isVisible() && this.isShowing()) {
            dlg.setCancelLabel(true);
            centerDlg(this, dlg);
        } else {
            dlg.setCancelLabel(false);
            Utils.centerOnScreen(dlg);
        }
        dlg.setModal(true);
        if (this.isShowing()) {
            dlg.setVisible(true);
        } else {
            JFrame frame = new JFrame(dlg.getTitle());
            frame.setIconImages(Utils.getRobotIcons());
            dlg.setWindow(frame);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            while (frame.isVisible()) {
                Utils.sleep(50);
            }
            frame.setContentPane(new JPanel());
            frame.dispose();
            dlg.setWindow(null);
        }
    }

    private EditorPnl newScript(int scriptType) {

        if (getDocumentTabbedPane().getActiveEditorPanel() != null && getDocumentTabbedPane().getActiveEditorPanel().getFile() != null) {
            currentDirectory = getDocumentTabbedPane().getActiveEditorPanel().getFile();
        }
        EditorPnl ep = getDocumentTabbedPane().createEmptyEditor(scriptType);
        menuEnabler();
        return ep;
    }

    private void configureScriptFileChooserFilters(JFileChooser c, TestScriptInterpret t) {
        c.setAcceptAllFileFilterUsed(false);
        c.addChoosableFileFilter(c.getAcceptAllFileFilter());
        int type = t == null ? TestScriptInterpret.TYPE_PROPRIETARY : t.getType();
        switch (type) {
            case TestScriptInterpret.TYPE_PROPRIETARY:
                c.addChoosableFileFilter(javaFileFilter);
                if (scriptFileFilter != null) {
                    c.addChoosableFileFilter(scriptFileFilter);
                }
                c.setFileFilter(scriptFileFilter);
                break;
            case TestScriptInterpret.TYPE_JAVA:
                if (scriptFileFilter != null) {
                    c.addChoosableFileFilter(scriptFileFilter);
                }
                c.addChoosableFileFilter(javaFileFilter);
                c.setFileFilter(javaFileFilter);
                break;
        }
    }

    private void openScript() {
        JFileChooser chooser = new JFileChooser();
        configureScriptFileChooserFilters(chooser, null);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        if (getDocumentTabbedPane().getActiveEditorPanel() != null && getDocumentTabbedPane().getActiveEditorPanel().getFile() != null) {
            chooser.setCurrentDirectory(getDocumentTabbedPane().getActiveEditorPanel().getFile());
        } else if (currentDirectory != null) {
            chooser.setCurrentDirectory(currentDirectory);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openScript(chooser.getSelectedFile());
        }
    }

    private EditorPnl openScript(File script) {
        cfg.updateListOfRecents(script.getAbsolutePath(),
                IO_RECENT_SCRIPTS,
                MAX_DYNAMIC_MENU_ITEMS);
        EditorPnl ed = null;
        try {
            ed = getDocumentTabbedPane().getEditorForFile(script);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        menuEnabler();
        return ed;
    }

    private void saveScript() {
        saveScript(getDocumentTabbedPane().getActiveEditorPanel());
    }

    /**
     * Save a script to a file associated with an open editor.
     *
     * @param pnl an editor panel to save the content of.
     */
    public void saveScript(EditorPnl pnl) {
        if (getDocumentTabbedPane().getTabCount() < 1) {
            return;
        }
        File f = pnl.getFile();
        if (f != null) {
            try {
                // Bug 12008 fix: check the last modification time if it matches
                if (f.lastModified() > 0 && pnl.getLastModified() > 0 && f.lastModified() != pnl.getLastModified()) {
                    Object options[] = {
                        ApplicationSupport.getString("btnOk"),
                        ApplicationSupport.getString("btnCancel"),
                        ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileModifiedBtnSaveAs")
                    };
                    Object args[] = {new Date(pnl.getLastModified()), new Date(f.lastModified())};
                    int option = JOptionPane.showOptionDialog(this,
                            MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileModifiedByAnotherUser"), args),
                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileExistsTitle"),
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]);
                    switch (option) {
                        case 0:
                            break;
                        case 1:
                            return;
                        case 2:
                            saveScriptAs(pnl);
                            return;
                    }
                }

                // Bug 2941550 fix: when writing the file, force the UTF-8 encoding.
                PrintWriter writer;
                try {
                    writer = new PrintWriter(f, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Utils.showErrorDialog(this, ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.ioerror"),
                            MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.unsupportedUTF"),
                            f.getName(), Charset.defaultCharset().displayName()), ex);
                    writer = new PrintWriter(f);
                }
                String text = pnl.getEditor().getText();
                writer.print(text);
                writer.flush();
                writer.close();
                pnl.setDocumentModified(false);

                // Wait some time for the underlying OS to refresh the file meta data.
                // Especially Windows are known to cache the changes and not to update
                // the time of file modification immediately.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                }

                // If the file modification time stamp was updated, save it to the panel.
                // If it hasn't been set the panel modification time to zero to prevent
                // display of the "Someone else modified the file" error message next time.
                long modified = f.lastModified();
                if (pnl.getLastModified() == modified) {
                    pnl.setLastModified(0);
                } else {
                    pnl.setLastModified(modified);
                }
                menuEnabler();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            saveScriptAs();
        }
    }

    /**
     * Perform the "Save As" action. A file chooser is opened and user may select an existing
     * file in the file system or enter a new one to save the content of the editor into.
     *
     * @return true if the save operation has been completed successfuly, false if the dialog has been canceled
     */
    private boolean saveScriptAs() {
        return saveScriptAs(getDocumentTabbedPane().getActiveEditorPanel());
    }

    private boolean saveScriptAs(EditorPnl pnl) {
        if (getDocumentTabbedPane().getTabCount() < 1) {
            return true;
        }

        JFileChooser chooser = new JFileChooser();
        configureScriptFileChooserFilters(chooser, pnl.getTestScript());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        Object params[] = {
            getDocumentTabbedPane().getTitleAt(getDocumentTabbedPane().indexOfComponent(pnl))
        };
        chooser.setDialogTitle(MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.saveAsTitle"), params));
        if (pnl != null && pnl.getFile() != null) {
            chooser.setCurrentDirectory(pnl.getFile());
        } else if (currentDirectory != null) {
            chooser.setCurrentDirectory(currentDirectory);
        }

        int result;
        do {
            result = JOptionPane.OK_OPTION;
            int chooserResult = chooser.showSaveDialog(this);
            boolean isJava = false;
            boolean isHTML = false;
            if (chooserResult == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                String ext = Utils.getExtension(f);

                // Depending on which file filter is selected check the file
                // extension and add it automatically if it is not there
                if (javaFileFilter.equals(chooser.getFileFilter())) {  // .java filter selected
                    if (ext == null || !ext.equalsIgnoreCase("java")) {
                        f = new File(f.getParent(), f.getName() + ".java");
                    }
                    isJava = true;

                } else if (scriptFileFilter != null && scriptFileFilter.equals(chooser.getFileFilter())) {  // .tpr filter selected
                    if (ext == null || !ext.equalsIgnoreCase(DEFAULT_TPLAN_ROBOT_FILE_EXTENSION)) {
                        f = new File(f.getParent(), f.getName() + "." + DEFAULT_TPLAN_ROBOT_FILE_EXTENSION);
                    }
                } else {
                    isHTML = ext != null && (ext.equalsIgnoreCase("htm") || ext.equalsIgnoreCase("html"));
                }
                if (f.isFile() && f.exists()) {
                    Object args[] = {
                        f.getName()
                    };
                    result = JOptionPane.showConfirmDialog(this,
                            MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileExistsText"), args),
                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileExistsTitle"),
                            JOptionPane.YES_NO_CANCEL_OPTION);
                }
                switch (result) {
                    case JOptionPane.CANCEL_OPTION:
                        return false;
                    case JOptionPane.NO_OPTION:
                        // Just break to loop and show the file chooser again
                        break;
                    case JOptionPane.OK_OPTION:

                        // Check the content of the source file. If it is a script,
                        // offer conversion to Java.
                        if (isJava && pnl.getTestScript().getType() == TestScriptInterpret.TYPE_PROPRIETARY) {
                            int exportResult = JOptionPane.showConfirmDialog(this,
                                    ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.convertToJavaMsg"),
                                    ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.convertToJavaTitle"),
                                    JOptionPane.YES_NO_CANCEL_OPTION);

                            boolean shouldBreak = false;
                            switch (exportResult) {
                                case JOptionPane.CANCEL_OPTION:
                                    return false;
                                case JOptionPane.NO_OPTION:
                                    // Just break to loop and show the file chooser again
                                    shouldBreak = true;
                                    break;
                                case JOptionPane.OK_OPTION:
                                    EditorPnl newEditor = exportToJava(pnl, null);

                                    // If the new editor is null, user canceled the export to Java.
                                    // Reshow the file chooser in such a case.
                                    if (newEditor == null) {
                                        shouldBreak = true;
                                        break;
                                    }

                                    // Close the old editor
                                    tabbedPane.closeEditor(pnl);
                                    pnl = newEditor;
                                    break;
                            }

                            // If 'No' was selected, continue the loop and reshow the file chooser
                            if (shouldBreak) {
                                result = JOptionPane.NO_OPTION;
                                continue;
                            }
                        }

                        if (isHTML) {
                            // Convert to HTML?
                            int exportOption = JOptionPane.showConfirmDialog(this,
                                    ApplicationSupport.getString("exportToHtml.confirmMsg"),
                                    ApplicationSupport.getString("exportToHtml.confirmTitle"), JOptionPane.YES_NO_CANCEL_OPTION);
                            switch (exportOption) {
                                case JOptionPane.OK_OPTION:
                                    try {
                                        Utils.exportScriptToHtml(new FileWriter(f), pnl.getEditor().getStyledDocument(), false);
                                        Utils.execOpenURL(f.toURI().toURL().toString());
                                    } catch (Exception ex) {
                                        Utils.showErrorDialog(this, ApplicationSupport.getString("exportToHtml.errTitle"),
                                                ApplicationSupport.getString("exportToHtml.errText"), ex);
                                    }
                                    return false;
                                case JOptionPane.CANCEL_OPTION:
                                    return false;
                            }
                        }

                        tabbedPane.setTitleAt(tabbedPane.indexOfComponent(pnl), f.getName());
                        tabbedPane.setToolTipTextAt(tabbedPane.indexOfComponent(pnl), f.getAbsolutePath());
                        try {
                            pnl.getTestScript().setURI(f.toURI(), false);
                        } catch (IOException ex) {
                            // never thrown because the "load" flag is false
                            ex.printStackTrace();
                        }

                        cfg.updateListOfRecents(f.getAbsolutePath(),
                                IO_RECENT_SCRIPTS,
                                MAX_DYNAMIC_MENU_ITEMS);
                        saveScript();

                        // Bug fix: Validate after "Save as" because changed script path
                        // may affect visibility of other scripts linked through
                        // the "Include" and "Run" commands
                        compileScript(pnl);
                        return true;
                }

            } else {  // This means that the file chooser was canceled -> exit the method
                return false;
            }
        } while (result != JOptionPane.OK_OPTION);
        return true;
    }

    /**
     * Take a screenshot window.
     * @param currentDir
     * @param parent
     * @return the screenshot image file.
     */
    private File takeScreenshot(File currentDir, Component parent) {
        if (client == null || !client.isConnected()) {
            JOptionPane.showMessageDialog(this, "You must be connected to a desktop to take a screen shot.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        File imgFile = null;
        try {
            ImageDialog dlg = new ImageDialog(this, ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.takeScreenshotPreviewTitle"), true);

            // Save a copy of the image rather than using the client one.
            Image image = client.getImage();
            BufferedImage imgCopy = new BufferedImage(image.getWidth(this), image.getHeight(this), BufferedImage.TYPE_INT_ARGB);
            imgCopy.createGraphics().drawImage(image, 0, 0, this);
            dlg.setImage(imgCopy);
            dlg.enableSaveForNoUpdates(true);
            dlg.setVisible(true);
            Rectangle r = dlg.isCanceled() ? null : dlg.getRectangle();

            ImageFileChooser chooser = new ImageFileChooser(this);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            chooser.setDialogTitle(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.takeScreenshotWindowTitle"));
            if (currentDir != null) {
                chooser.setCurrentDirectory(currentDir);
            } else if (cfg.getString("takescreenshot.lastDir") != null) {
                chooser.setCurrentDirectory(new File(cfg.getString("takescreenshot.lastDir")));
            } else if (currentDirectory != null) {
                chooser.setCurrentDirectory(currentDirectory);
            }
            int result = JOptionPane.OK_OPTION;

            do {
                result = JOptionPane.OK_OPTION;
                int chooserResult = chooser.showSaveDialog(this);
                File f = chooser.getSelectedFile();
                if (f != null) {
                    cfg.setString("takescreenshot.lastDir", f.getAbsolutePath());
                    String ext = Utils.getExtension(f);
                    if (ext == null) {
                        f = new File(f.getParent(), f.getName() + ".png");
                    }
                }
                if (chooserResult == JFileChooser.APPROVE_OPTION) {
                    if (f.isFile() && f.exists()) {
                        Object args[] = {
                            f.getName()
                        };
                        result = JOptionPane.showConfirmDialog(this,
                                MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileExistsText"), args),
                                ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileExistsTitle"),
                                JOptionPane.YES_NO_CANCEL_OPTION);
                    }
                    switch (result) {
                        case JOptionPane.CANCEL_OPTION:
                            return null;
                        case JOptionPane.NO_OPTION:
                            // Just break to loop and show the file chooser again
                            break;
                        case JOptionPane.OK_OPTION:
                            imgFile = f;

                            try {
                                takeScreenshot(imgCopy, imgFile, r);
                            } catch (Exception e) {
                                Object args[] = {imgFile.getAbsolutePath(), e.getMessage()};
                                JOptionPane.showMessageDialog(chooser,
                                        MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgImgCantBeCreated"), args),
                                        ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgImgCantBeCreatedTitle"),
                                        JOptionPane.WARNING_MESSAGE);
                                result = JOptionPane.NO_OPTION;
                                break;
                            }
                            return imgFile;
                    }

                } else {  // This means that the file chooser was canceled -> exit the method
                    return null;
                }
            } while (result != JOptionPane.OK_OPTION);
        } catch (Exception ex) {
            Utils.showErrorDialog(this, "Error", "Internal error", ex);
        } catch (OutOfMemoryError err) {
            Utils.showErrorDialog(this, "Error", "<html><body>The program ran out of memory. "
                    + "Follow the instructions in the <a href=\"http://localdoc/install/install.html#troubleshooting\">Release Notes</a><br>"
                    + "on how to increase the amount of memory assigned to Java.</body></html>", err);
        }
        return imgFile;
    }

    public void takeScreenshot(BufferedImage img, File f, Rectangle r) throws Exception {
        if (r != null) {
            img = img.getSubimage(r.x, r.y, r.width, r.height);
        }
        ScreenshotCommand.saveImage(img, Utils.getExtension(f), f, UserConfiguration.getInstance());
    }

    /**
     * Save all open documents. This method checks all open documents and invokes the save method for those
     * that are modified.
     */
    public void saveAll() {
        for (int i = 0; i < getDocumentTabbedPane().getTabCount(); i++) {
            saveScript((EditorPnl) getDocumentTabbedPane().getComponentAt(i));
        }
    }

    /**
     * <p>Method validates whether the currently opened file has been modified or not. It should be called
     * before we let user open, reopen or create another file.
     * <p/>
     * <p>If there's an open file and it has been modified, a warning message is displayed. It gives user
     * options to discard changes, save the file or cancel the operation. If "save" gets selected, the method
     * saves the file using the {@link #saveScript saveScript()} method. The method returns true if either discard
     * or save option is selected and false if the message window is canceled.
     * <p/>
     * <p>If there's no open file and user has just modified the editor content, we rather call
     * the {@link #saveScript saveScript()} method if "save" is selected. This will open a file chooser and
     * user has to select/enter a file name to save the changes to.
     *
     * @return true if there are no changes or user selects either "save changes" or "discard changes". False is
     *         returned only if there's a modified file and the warning window is canceled.
     */
    private boolean continueDespiteFileModified() {
        return continueDespiteFileModified(getDocumentTabbedPane().getActiveEditorPanel());
    }

    private boolean continueDespiteFileModified(EditorPnl pnl) {

        // If the document is not modified, we may safely continue
        if (pnl == null || !pnl.isDocumentChanged()) {
            return true;
        }

        // This flag shows whether a file has been opened and modified
        // or if user modified the default editor document
        boolean isOpenedFile = pnl.getFile() != null;

        Object[] options = {
            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileModifiedBtnSave"),
            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileModifiedBtnDiscard"),
            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileModifiedBtnCancel")
        };

        // Load different message text for "save" and "save as" options
        String text;
        if (isOpenedFile) {
            Object params[] = {pnl.getFile().getAbsolutePath()};
            text = MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileModifiedText"), params);
        } else {
            text = ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileUnnamedModifiedText");
        }

        // This flag will be used to break the loop if the "Save As" action gets performed
        boolean breakFlag = false;
        do {
            breakFlag = true;
            int option = JOptionPane.showOptionDialog(this,
                    text + "\n",
                    ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgFileModifiedTitle"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            switch (option) {
                // User chose to discard the changes and continue
                case JOptionPane.NO_OPTION:
                    return true;

                // User chose to save the file and continue
                case JOptionPane.YES_OPTION:
                    // If a file has been opened and modified, save it
                    if (isOpenedFile) {
                        saveScript(pnl);
                        return true;

                        // If the default "no name" file has been modified, perform the "Save As" action.
                        // Don't leave the loop if the "Save As" dialog gets canceled - it will redisplay the
                        // "File modified" message
                    } else {
                        breakFlag = saveScriptAs();
                    }
                    break;
                default:
                    return false;
            }
        } while (!breakFlag);

        // We only get here when the "save as" action is performed
        return true;
    }

    private void createInitialContent() {

        try {
            InputStream is = ApplicationSupport.class.getResourceAsStream(MENU_AND_TOOLBAR_PROPERTIES);
            PropertyResourceBundle props = new PropertyResourceBundle(is);

            // Create a new action handler
            actionManager = new ActionManager(this, props, resourceBundle, cfg);
        } catch (IOException ex) {
            System.out.println("Can't find menu&toolbar definition file: JAR file may be corrupted!");
            ex.printStackTrace();
            System.exit(1);
        }

        // Create a default, empty panel 800x600 pixels with BorderLayout.
        // It will serve as a content rfbScrollPane
        JPanel contentPane = new JPanel(new CustomBorderLayout());
        setContentPane(contentPane);

        JPanel pnlMain = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0);

        // Create the RFB Panel which will display the remote desktop
        c = new GridBagConstraints(1, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0);

        desktopPnl = new DesktopViewer(client, scriptManager, cfg);
        boolean enableShortcuts = cfg.getBoolean("menu.enableRecordingKeysInDesktop").booleanValue();
        if (enableShortcuts) {
            addAction(desktopPnl, "record", "menu.RecordShortcut");
            addAction(desktopPnl, "screenshot", "menu.ScreenshotShortcut");
            addAction(desktopPnl, "waitfor", "menu.WaitforShortcut");
        }

        scriptManager.addCommandListener(this);
        tabbedPane = new DocumentTabbedPane(this);
        tabbedPane.setOpaque(true);

        glassPanel = new DesktopGlassPanel();
        desktopPnl.setLayout(new GridBagLayout());
        c = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
        desktopPnl.add(glassPanel, c);

        desktopPnl.add(pnlwelcome, c);
        desktopPnl.addPropertyChangeListener(this);

        //-----
        drawPanel = new DrawPanel();
        drawPanel.setEnableDragRect(true);
        drawPanel.setOpaque(false);
        drawPanel.addPropertyChangeListener(this);
        //-----

        scriptManager.setDesktopViewer(desktopPnl);

        //JPanel pnlWrap = new JPanel(new GridBagLayout());
        pnlWrap = new JPanel();
        pnlWrap.setLayout(new GridBagLayout());
        pnlWrap.setBorder(BorderFactory.createLoweredBevelBorder());
        c = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

        desktopScrollPane = new JScrollPane(desktopPnl);
        desktopScrollPane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, scrollBtn);
        scrollBtn.addActionListener(this);
        scrollBtn.setBorderPainted(false);
        try {
            ImageIcon icon = com.tplan.robot.ApplicationSupport.getImageIcon("resize16.gif");
            scrollBtn.setIcon(icon);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        pnlWrap.add(desktopScrollPane, c);

        leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplitPane.setResizeWeight(0.0);
        leftSplitPane.resetToPreferredSizes();
        leftSplitPane.setOneTouchExpandable(true);
        leftSplitPane.setBottomComponent(getDocumentTabbedPane());
        leftSplitPane.setMinimumSize(new Dimension(140, 100));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.setLeftComponent(leftSplitPane);
        splitPane.setRightComponent(pnlWrap);
        splitPane.setResizeWeight(1.0);

        msgPanel = new ValidationMessagePanel(this);
        msgPanel.setVisible(false);

        globalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        globalSplitPane.setOneTouchExpandable(false);
        globalSplitPane.setTopComponent(splitPane);
        globalSplitPane.setDividerSize(0);
        globalSplitPane.setResizeWeight(1.0);


        c = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0);
        pnlMain.add(globalSplitPane, c);
        splitPane.resetToPreferredSizes();

        // Create a status bar and add it to the content rfbScrollPane
        c = new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0);
        statusBar = new StatusBar(this);
        statusBar.getFieldLeft().setText(" ");
        desktopPnl.addPropertyChangeListener(statusBar);
        glassPanel.addPropertyChangeListener(statusBar);
        pnlMain.add(statusBar, c);
        this.getContentPane().add(BorderLayout.CENTER, pnlMain);

        // Create menu and toolbar
        this.setJMenuBar(getActionManager().createMenubar("menu.Main"));

        // Update in 2.0.6 - add the Enterprise version links to the Help menu
        if (OPEN_SOURCE) {
            try {
                TrialAction.install(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Position tool bar according to user preferences
        String toolBarLoc = cfg.getString("ui.mainframe.toolbarLocation");
        if (toolBarLoc == null || (!toolBarLoc.equals(BorderLayout.NORTH) && !toolBarLoc.equals(BorderLayout.SOUTH) && !toolBarLoc.equals(BorderLayout.EAST) && !toolBarLoc.equals(BorderLayout.WEST))) {
            toolBarLoc = BorderLayout.NORTH;
        }
        toolBar = getActionManager().createToolbar("toolbar.Main", this);
        this.getContentPane().add(toolBarLoc, getToolBar());
        getActionManager().addActionListener(this);

        cfg.addConfigurationListener(this);

        // Rebuild dynamic menus
        List v = cfg.getListOfStrings(IO_RECENT_SCRIPTS);
        rebuildScriptMenu("menu.Reopen", v);

        v = cfg.getListOfStrings(IO_RECENT_SERVERS);
        rebuildServerMenu("menu.Reconnect", v);

        boolean readOnly = cfg.getBoolean("rfb.readOnly").booleanValue();
        Object obj = getActionManager().getMenuItem("readonly");
        if (obj != null && obj instanceof JComponent) {
            ((JCheckBoxMenuItem) obj).setSelected(readOnly);
        }
        obj = getActionManager().getToolbarButton("readonly");
        if (obj != null && obj instanceof JComponent) {
            ((JToggleButton) obj).setSelected(readOnly);
        }

        obj = getActionManager().getMenuItem("nooutput");
        if (obj != null && obj instanceof JComponent) {
            String outParam = System.getProperty(ScriptManager.OUTPUT_DISABLED_FLAG);
            boolean outputDisabled = outParam != null && outParam.equals("true");
            ((JCheckBoxMenuItem) obj).setSelected(outputDisabled);
        }

        // Handle the CLI option to edit a particular script for editing
        if (guiParams.containsKey("edit")) {
            String name = (String) guiParams.get("edit");
            File f = new File(name);
            try {
                EditorPnl pnl = getDocumentTabbedPane().getEditorForFile(f, true, true);
                getDocumentTabbedPane().setSelectedTab(pnl);
            } catch (IOException e) {
                if (scriptManager.getScriptToRun() == null) {
                    Utils.showErrorDialog(this,
                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.ioerror"),
                            MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.failedToOpenFile"), Utils.getFullPath(f)),
                            e);
                } else {
                    e.printStackTrace();
                }
            }
        } /* -------------------- Functionality requested by D3Concept ---------- */ // --createscript creates a new file and opens it in an empty editor
        else if (guiParams != null && guiParams.containsKey("createscript")) {
            String name = (String) guiParams.get("createscript");
            File f = new File(name);
            if (!f.isAbsolute()) {
                f = new File(Utils.getInstallPath(), name);
            }
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                EditorPnl pnl = getDocumentTabbedPane().getEditorForFile(f, true, true);
                int len = pnl.getEditor().getDocument().getLength();
                if (len > 0) {
                    try {
                        pnl.getEditor().getDocument().remove(0, len);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
                String s = "";
                // --outputdir inserts the _REPORT_DIR to the active editor
                if (guiParams.containsKey("outputdir")) {
                    s += "Var _REPORT_DIR=\"" + guiParams.get("outputdir") + "\"\n";
                }
                // --outputdir inserts the _TEMPLATE_DIR to the active editor
                if (guiParams.containsKey("templatedir")) {
                    s += "Var _TEMPLATE_DIR=\"" + guiParams.get("templatedir") + "\"\n";
                }
                if (s.length() > 0) {
                    try {
                        pnl.getEditor().getDocument().insertString(0, s, null);
                        pnl.getEditor().setCaretPosition(s.length());
                        propertyChange(new PropertyChangeEvent(this, "caretUpdate", pnl.getEditor(), null));
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                if (scriptManager.getScriptToRun() == null) {
                    Utils.showErrorDialog(this,
                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.ioerror"),
                            MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.failedToCreateFile"), Utils.getFullPath(f)),
                            e);
                } else {
                    e.printStackTrace();
                }
            }
        } // -------------------- End of functionality requested by D3Concept ----------
        // By default reopen the list of files
        else {
            getDocumentTabbedPane().reopenListOfFiles();
        }

        // If there's no open document, open a default untitled editor
        if (getDocumentTabbedPane().getTabCount() == 0) {
            getDocumentTabbedPane().createEmptyEditor(TestScriptInterpret.TYPE_PROPRIETARY);
        }

        // Pack and resize the components
        pack();

        Dimension d = pnlWrap.getPreferredSize();
        resetDesktopScrollSize(d.width, d.height);

        menuEnabler();
    }

    private void setDrawPanelVisible(boolean visible) {
        if (visible) {
            GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                    GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
            desktopPnl.add(drawPanel, c);
            desktopPnl.repaint();
        } else {
            desktopPnl.remove(drawPanel);
            desktopPnl.repaint();
        }
    }

    public void addDesktopPaint(Component paintingComponent) {
        desktopPaints.add(paintingComponent);
        if (paintingComponent instanceof PropertyChangeListener) {
            desktopPnl.addPropertyChangeListener((PropertyChangeListener) paintingComponent);
        }
        GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
        desktopPnl.add(paintingComponent, c);
        desktopPnl.revalidate();
        desktopPnl.repaint();
    }

    public void removeDesktopPaint(Component paintingComponent) {
        if (desktopPaints.remove(paintingComponent)) {
            if (paintingComponent instanceof PropertyChangeListener) {
                desktopPnl.removePropertyChangeListener((PropertyChangeListener) paintingComponent);
            }
            desktopPnl.remove(paintingComponent);
            desktopPnl.repaint();
        }
    }

    public List<Component> getDesktopPaints() {
        return Collections.unmodifiableList(desktopPaints);
    }

    private void addAction(JComponent component, String actionName, String configKey) {
        String key = cfg.getString(configKey);
        if (key != null && Utils.getKeyStroke(key) != null) {
            ToolsAction a = new ToolsAction(actionName);
            component.getActionMap().put(actionName, a);
            component.getInputMap().put(Utils.getKeyStroke(key), actionName);
        }
    }

    /**
     * This method is responsible for enabling and disabling of menu items and toolbar buttons.
     */
    public void menuEnabler() {
        if (getDocumentTabbedPane() == null) {
            return;
        }
        // ----------------------------
        // File menu
        // ----------------------------

        // Exit, new, open and reopen buttons are always enabled
        enableMenuItem("exit", true);
        enableMenuItem("new", true);
        enableMenuItem("open", true);

        //TODO: reopen button should be enabled only if there are some submenu items
        enableMenuItem("menu.Reopen", true);

        EditorPnl ed = getDocumentTabbedPane().getActiveEditorPanel();

        // Save button is only enabled if a script is opened and modified
        final boolean isEditor = ed != null;
        final boolean scriptModified = isEditor && ed.isDocumentChanged() && !ed.isDocumentEmpty();
        final boolean isProprietaryEditor = isEditor && ed.getTestScript() != null && ed.getTestScript().getType() == TestScriptInterpret.TYPE_PROPRIETARY;

        enableMenuItem("saveas", true);
        enableMenuItem("save", scriptModified);
        enableMenuItem("saveall", getDocumentTabbedPane().isAnyDocumentModified());
        enableMenuItem("export", isProprietaryEditor);
        getDocumentTabbedPane().updateTabTitles();

        enableMenuItem("find", isEditor);
        enableMenuItem("gotoline", isEditor);
        enableMenuItem("screenshot", isProprietaryEditor);
        enableMenuItem("compareto", isProprietaryEditor);
        enableMenuItem("waitfor", isProprietaryEditor);

        boolean running = scriptManager.getExecutingTestScripts().size() > 0;
        boolean canCloseAll = !running && getDocumentTabbedPane().getTabCount() > 0;
        enableMenuItem("close", canCloseAll);
        enableMenuItem("closeall", canCloseAll);
        enableMenuItem("closeother", !running && getDocumentTabbedPane().getTabCount() > 1);

        // Connect button is always enabled, disconnect only if we are connected
        boolean connected = client != null && client.isConnected();
        enableMenuItem("connect", !running);
        enableMenuItem("disconnect", connected && !running);
        enableMenuItem("zoomin", connected && desktopPnl.getZoomFactor() > 10);
        enableMenuItem("zoomout", connected);

        //TODO: reconnect button should be enabled only if there are some submenu items
        enableMenuItem("menu.Reconnect", !running);

        // -----------------------------
        // Edit menu
        // -----------------------------
        enableMenuItem("undo", isEditor && ed.getUndoAction().isEnabled());
        enableMenuItem("redo", isEditor && ed.getRedoAction().isEnabled());

        final boolean documentsOpen = getDocumentTabbedPane().getTabCount() > 0;
        enableMenuItem("cut-to-clipboard", documentsOpen);
        enableMenuItem("copy-to-clipboard", documentsOpen);
        enableMenuItem("paste-from-clipboard", documentsOpen);
        enableMenuItem("select-all", documentsOpen);
        enableMenuItem("preferences", true);
        enableMenuItem("toolpanel", true);
        enableMenuItem("takescreenshot", connected);
        enableMenuItem("plugins", true);


        // -----------------------------
        // Script menu
        // -----------------------------
        boolean canReplay = false;
        if (isEditor) {
            // TODO: Getting the text is inefficient -> implement a flag directly in editor
            canReplay = !"".equals(ed.getEditor().getText().trim());
            if (desktopPnl != null) {
                canReplay = !running;
            }
            canReplay = canReplay && !exportInProgress;
        }
        enableMenuItem("compile", isEditor);
        enableMenuItem("replay", canReplay);
        boolean selectionEmpty = (isEditor && ed.getEditor().getSelectionStart() == ed.getEditor().getSelectionEnd());
        enableMenuItem("replayselection", canReplay && !selectionEmpty && ed.getTestScript().isPartialExecutionAllowed());
        enableMenuItem("step", true);
        enableMenuItem("nooutput", true);
        enableMenuItem("pause", running);
        enableMenuItem("stop", running);
        enableMenuItem("followexectrace", true);
        enableMenuItem("readonly", true);

        // -----------------------------
        // Tools menu
        // -----------------------------
        enableMenuItem("variablebrowser", isProprietaryEditor);
        enableMenuItem("keybrowser", true);

        // -----------------------------
        // Help menu
        // -----------------------------

        // Help menus are always enabled
        enableMenuItem("helpabout", true);
        enableMenuItem("helptopics", helpBroker != null);
        enableMenuItem("contextualhelp", helpBroker != null && helpBroker.isContextualHelpSupported());
        enableMenuItem("edge", true);
        boolean b = System.getProperty("vncrobot.record.debug", "").equals("true");

        enableMenuItem("record", (b || (client != null && client.isConnected())) && isProprietaryEditor);

        // If there's no proprietary editor and recording is on, switch it off
        JToggleButton s = (JToggleButton) getActionManager().getToolbarButton("record");
        if (!isProprietaryEditor && s != null && s.isSelected()) {
            s.setSelected(false);
        }

        // Update menu items of individual menu plugins
        if (guiPlugins != null) {
            for (GuiComponent c : guiPlugins) {
                if (c instanceof MenuStateListener) {
                    try {
                        ((MenuStateListener) c).menuStateChanged(this);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Enable a menu item and associated toolbar button identified by an action command.
     *
     * @param actionCommand an action command.
     * @param enabled       true enables, false disables.
     */
    private void enableMenuItem(String actionCommand, boolean enabled) {
        Object obj = getActionManager().getMenuItem(actionCommand);
        if (obj != null && obj instanceof JComponent) {
            ((JComponent) obj).setEnabled(enabled);
        }
        obj = getActionManager().getToolbarButton(actionCommand);
        if (obj != null && obj instanceof JComponent) {
            ((JComponent) obj).setEnabled(enabled);
        }
    }

//    public static String ApplicationSupport.getString(String strKey) {
//        return com.tplan.robot.ApplicationSupport.getResourceBundle() == null ? "" : ApplicationSupport.getResourceBundle().ApplicationSupport.getString(strKey);
//    }
    /**
     * Implementation of the WindowListener interface.
     * If the window gets closed, close properly all socket conections and dispose it.
     *
     * @param evt
     */
    public void windowClosing(WindowEvent evt) {
        exit();
    }

    /**
     * Exit the GUI. The method performs all necessary checks like whether
     * there are any unsaved editor changes or running test scripts.
     * A warning message is typically displayed and user is given options to cancel
     * the shutdown process or to continue.
     */
    public void exit() {
        // Check if a script is being executed
        if (scriptManager.getExecutingTestScripts().size() > 0) {
            int flag = cfg.getInteger("warning.closeWhenExecutionRunning");
            if (flag < 0) {
                Object options[] = {
                    ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgExecutingOptionFinish"),
                    ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgExecutingOptionCancel")
                };

                int option = Utils.showConfigurableMessageDialog(this,
                        ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgExecutingTitle"),
                        ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgExecutingText"),
                        ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgExecutingDoNotAsk"),
                        "warning.closeWhenExecutionRunning",
                        options,
                        0);
                switch (option) {
                    case 0:
                        break;
                    case 1:
                        return;
                }
            }
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(scriptManager, client));
        }

        // Save list of opened files
        if (getDocumentTabbedPane() != null) {
            getDocumentTabbedPane().saveListOfOpenedFiles();
        }

        /* ----------------- Functionality requested by D3Concept ---------- */
        // Save the auto created script if requested
        if (guiParams.containsKey("createscript")) {
            String name = (String) guiParams.get("createscript");
            File f = new File(name);
            if (getDocumentTabbedPane().isFileOpen(f)) {
                try {
                    EditorPnl ed = getDocumentTabbedPane().getEditorForFile(f);
                    if (ed.isDocumentChanged()) {
                        saveScript(ed);
                    }
                } catch (IOException e) {
                    Utils.showErrorDialog(this,
                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.ioerror"),
                            MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.failedToSaveFile"), Utils.getFullPath(f)),
                            e);
                }
            }
        }
        /* ------------- End of functionality requested by D3Concept ---------- */


        // First check if there are unsaved file changes
        // False return value means that the action was canceled
        for (int i = 0; i < getDocumentTabbedPane().getTabCount(); i++) {
            if (!continueDespiteFileModified((EditorPnl) tabbedPane.getComponentAt(i))) {
                menuEnabler();
                // False return value means that the action was canceled => cancel the shutdown
                return;
            }
        }

        // Check if user configuration has been modified and save it eventually
        UserConfiguration.saveConfiguration();

        // Close any open RFB connections
        if (client != null && client.isConnected()) {

            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Dispose the main frame and terminate the JVM
        dispose();
        System.exit(exitCode);
    }

    /**
     * Implementation of the WindowListener interface.
     * This method is empty and does nothing in this class.
     *
     * @param evt a WindowEvent describing the window event.
     */
    public void windowActivated(WindowEvent evt) {
    }

    /**
     * Implementation of the WindowListener interface.
     * This method is empty and does nothing in this class.
     *
     * @param evt a WindowEvent describing the window event.
     */
    public void windowDeactivated(WindowEvent evt) {
    }

    /**
     * Implementation of the WindowListener interface.
     * This method is empty and does nothing in this class.
     *
     * @param evt a WindowEvent describing the window event.
     */
    public void windowOpened(WindowEvent evt) {
        // If a script is to be run automatically and no host is available, it probably means that the script
        // contains its own Connect command. We need to start the execution then.
        runCliScript();
    }

    /**
     * Implementation of the WindowListener interface.
     * This method is empty and does nothing in this class.
     *
     * @param evt a WindowEvent describing the window event.
     */
    public void windowClosed(WindowEvent evt) {
    }

    /**
     * Implementation of the WindowListener interface.
     * This method is empty and does nothing in this class.
     *
     * @param evt a WindowEvent describing the window event.
     */
    public void windowIconified(WindowEvent evt) {
    }

    /**
     * Implementation of the WindowListener interface.
     * This method is empty and does nothing in this class.
     *
     * @param evt a WindowEvent describing the window event.
     */
    public void windowDeiconified(WindowEvent evt) {
    }

    private void resetDesktopScrollSize(int w, int h) {
        desktopScrollPane.setPreferredSize(new Dimension(w, h));
        desktopScrollPane.setSize(new Dimension(w, h));
        Dimension d = splitPane.getSize();
        if (d.width <= 0) {
            d = getContentPane().getPreferredSize();
        }
//        int pos = (int) (d.getWidth() - w - splitPane.getDividerSize() - desktopScrollPane.getVerticalScrollBar().getWidth());
        int pos = (int) splitPane.getWidth() - splitPane.getDividerSize() - (int) splitPane.getRightComponent().getPreferredSize().getWidth() - 2 * splitPane.getInsets().left - 2 * splitPane.getInsets().right;
//        System.out.println("splitPane.getWidth()="+splitPane.getWidth()+", pos="+pos+", d.width()="+d.getWidth()+", w="+w+", splitPane.getDividerSize()="+splitPane.getDividerSize());
        if (desktopScrollPane.getVerticalScrollBar().isShowing()) {
            pos = pos - desktopScrollPane.getVerticalScrollBar().getWidth();
        }
//        new Exception("resetDesktopScrollSize()\n  w="+w+",h="+h+",pos="+pos).printStackTrace();
        splitPane.setDividerLocation(pos);
    }

    /**
     * Implementation of the ActionListener interface.
     *
     * @param e an ActionEvent identified the required action.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(scrollBtn)) {
//            if (client != null && client.isConnected()) {
//                resetDesktopScrollSize(client.getDesktopWidth(), client.getDesktopHeight());
//            } else {
            Dimension d = pnlWrap.getPreferredSize();
            resetDesktopScrollSize(d.width, d.height);
//            }
            return;
        }

        String cmd = e.getActionCommand();

        // A null command indicates that the action is handled by an Action instance
        if (cmd == null) {
            return;
        }

        if (e.getSource() != null && e.getSource() instanceof EditorPnl) {
            if (cmd.equals(GUIConstants.EVENT_DOCUMENT_CHANGED)) {
                menuEnabler();
            } else if (cmd.equals(GUIConstants.EVENT_SELECTION_CHANGED)) {
                menuEnabler();
            }
        }

        // The following events originate from menu items or toolbal components
        // Exit button
        if (cmd.equals("exit")) {
            this.windowClosing(null);
        } // New button
        else if (cmd.equals("new")) {
            newScript(TestScriptInterpret.TYPE_PROPRIETARY);
        } // Open button
        else if (cmd.equals("open")) {
            openScript();
        } // Save button
        else if (cmd.equals("save")) {
            saveScript();
        } // Save button
        else if (cmd.equals("saveas")) {
            saveScriptAs();
        } // Save button
        else if (cmd.equals("saveall")) {
            saveAll();
        } // Disconnect button
        else if (cmd.equals("export")) {
            exportToJava(getDocumentTabbedPane().getActiveEditorPanel(), null);
        } else if (cmd.equals("disconnect") && client.isConnected()) {
            try {
                client.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } // Connect button
        else if (cmd.equals("connect")) {
            showConnectDialog();
            Utils.centerOnScreen(this);
        } // Replay button
        else if (cmd.equals("replay")) {
            executeScript();
        } // Compile button
        else if (cmd.equals("compile")) {
            compileScript(tabbedPane.getActiveEditorPanel());
        } // Replay Selection button
        else if (cmd.equals("replayselection")) {
            executeSelection();
        } // Pause Replay
        else if (cmd.equals("pause")) {
            // TODO: reimplement when multiple executions are supported
            List<TestScriptInterpret> l = scriptManager.getExecutingTestScripts();
            if (l.size() > 0) {
                TestScriptInterpret ti = l.get(0);
                ti.setPause(this, !ti.isPause(), resourceBundle.getString("gui.pauseReason"));
                getDocumentTabbedPane().getActiveEditorPanel().getEditor().setPauseScript(false);
            }
        } // Step Execution
        else if (cmd.equals("step")) {
            stepMode = ((JToggleButton) getActionManager().getToolbarButton("step")).isSelected();
        } // Pause Replay
        else if (cmd.equals("nooutput")) {
            boolean flag = ((JCheckBoxMenuItem) getActionManager().getMenuItem("nooutput")).isSelected();
//            scriptManager.setOutputEnabled(!flag);
            System.setProperty(ScriptManager.OUTPUT_DISABLED_FLAG, flag ? "true" : "false");
        } // Stop Replay
        else if (cmd.equals("stop")) {
            // TODO: reimplement when multiple executions are supported
            List<TestScriptInterpret> l = scriptManager.getExecutingTestScripts();
            if (l.size() > 0) {
                TestScriptInterpret interpret = l.get(0);
                boolean pause = interpret.isPause();
                interpret.setStop(this, true, true, resourceBundle.getString("gui.stopReason"));
                if (pause) {
                    ((JToggleButton) getActionManager().getToolbarButton("pause")).setSelected(false);
                }
            }
            scriptManager.setScriptToRun(null);
        } // Perform undo for the active editor
        else if (cmd.equals("undo")) {
            if (getDocumentTabbedPane().getActiveEditorPanel() != null) {
                getDocumentTabbedPane().getActiveEditorPanel().getUndoAction().actionPerformed(e);
                menuEnabler();
            }
        } // Perform undo for the active editor
        else if (cmd.equals("redo")) {
            if (getDocumentTabbedPane().getActiveEditorPanel() != null) {
                getDocumentTabbedPane().getActiveEditorPanel().getRedoAction().actionPerformed(e);
                menuEnabler();
            }
        } // Display the About help
        else if (cmd.equals("helpabout")) {
            AboutDialog about = new AboutDialog(this);
            about.setVisible(true);
        } // Display the help
        else if (cmd.startsWith("helptopics")) {
            int sepIndex = cmd.indexOf("_");
            String helpKey = null;
            if (sepIndex > 0) {
                helpKey = cmd.substring(sepIndex + 1);
            }
            showHelpDialog(helpKey, this);

        } else if (cmd.equals("contextualhelp")) {
            if (helpBroker != null && helpBroker.isContextualHelpSupported()) {
                helpBroker.contextShow(e);
            }
        } // Close the active document
        else if (cmd.equals("close")) {
            // False return value means that the action was canceled
            if (!continueDespiteFileModified()) {
                return;
            }
            getDocumentTabbedPane().closeActiveEditor();
            menuEnabler();
        } else if (cmd.equals("closeall")) {
            // False return value means that the action was canceled
            while (getDocumentTabbedPane().getTabCount() > 0) {
                if (!continueDespiteFileModified()) {
                    menuEnabler();
                    return;
                }
                getDocumentTabbedPane().closeActiveEditor();
            }
            menuEnabler();
        } else if (cmd.equals("closeother")) {
            // New to 2.3.1/2.0.6
            int edCnt = getDocumentTabbedPane().getTabCount();
            if (edCnt > 1) {
                final EditorPnl active = getDocumentTabbedPane().getActiveEditorPanel();

                List<EditorPnl> l = new ArrayList();
                EditorPnl ed;
                for (int i = 0; i < edCnt; i++) {
                    ed = (EditorPnl) getDocumentTabbedPane().getComponentAt(i);
                    if (!active.equals(ed)) {
                        l.add(ed);
                    }
                }

                for (EditorPnl eed : l) {
                    if (!continueDespiteFileModified(eed)) {
                        menuEnabler();
                        return;
                    }
                    getDocumentTabbedPane().closeEditor(eed);
                }
                menuEnabler();
            }
        } else if (cmd.equals("preferences")) {
            showOptionsDialog(null, null);

        } else if (cmd.equals("followexectrace")) {
            followExecTrace = ((JToggleButton) getActionManager().getToolbarButton("followexectrace")).isSelected();
            if (followExecTrace) {
                List<TestScriptInterpret> l = scriptManager.getExecutingTestScripts();
                if (l.size() > 0) {
                    // TODO: reimplement when multiple executions are supported
                    ScriptingContext ctx = l.get(0).getExecutionContext();
                    if (ctx != null && ctx.containsKey(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT)) {
                        Element el = (Element) ctx.get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT);
                        EditorPnl ed = getDocumentTabbedPane().getEditorForDocument(el.getDocument());
                        if (!tabbedPane.getActiveEditorPanel().equals(ed)) {
                            getDocumentTabbedPane().setSelectedTab(ed);
                            ed.getEditor().scrollElementToVisible(el);
                        }
                    }
                }
            }

        } else if (cmd.equals("readonly")) {
            cfg.setBoolean("rfb.readOnly", new Boolean(((AbstractButton) e.getSource()).isSelected()));
//        } else if (cmd.equals("variablebrowser")) {
//            showVariableDialog();
        } else if (cmd.equals("keybrowser")) {
            showKeyDialog();
        } else if (cmd.equals("cut-to-clipboard")) {
            getDocumentTabbedPane().getActiveEditorPanel().getEditor().cut();
        } else if (cmd.equals("copy-to-clipboard")) {
            getDocumentTabbedPane().getActiveEditorPanel().getEditor().copy();
        } else if (cmd.equals("paste-from-clipboard")) {
            getDocumentTabbedPane().getActiveEditorPanel().getEditor().paste();
        } else if (cmd.equals("select-all")) {
            getDocumentTabbedPane().getActiveEditorPanel().getEditor().selectAll();
        } else if (cmd.equals("toolpanel")) {
            AbstractButton btn = (AbstractButton) e.getSource();
            setToolPanelVisible(btn.isSelected());
        } else if (cmd.equals("takescreenshot")) {
            takeScreenshot(null, this);
        } else if (cmd.equals("find")) {
            showFindDialog(false);
        } else if (cmd.equals("gotoline")) {
            showGoToLineDialog();
        } else if (cmd.equals("screenshot")) {
            showScreenshotDialog(null);
        } else if (cmd.equals("compareto")) {
            showComparetoDialog(null);
        } else if (cmd.equals("zoomin")) {
            desktopPnl.setZoomFactor(desktopPnl.getZoomFactor() + 10);
        } else if (cmd.equals("zoomout")) {
            desktopPnl.setZoomFactor(desktopPnl.getZoomFactor() - 10);
        } else if (cmd.equals("waitfor")) {
            showWaitforDialog(null, null);
        } else if (cmd.equals("record")) {
            JToggleButton btn = ((JToggleButton) getActionManager().getToolbarButton("record"));
            boolean record = btn.isSelected();
            getRecordingModule().setEnabled(record);
            btn.setToolTipText(record
                    ? ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.btnRecordTooltipStop")
                    : ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.btnRecordTooltipStart"));
        } else if (cmd.equals("plugins")) {
            PluginDialog dlg = new PluginDialog(this, ApplicationSupport.getString("com.tplan.robot.pluginGui.windowTitle"), true);
            dlg.pack();
            dlg.setLocationRelativeTo(this);
            dlg.setVisible(true);
        }
    }

//    public void setHelpId(JComponent component, String helpId) {
//        process.setHelpId(component, helpId);
//    }
//    public void showHelpDialog(String helpKey, Object owner) {
//        showHelpDialog(helpKey, owner);
//    }
    public boolean isRecordingOn() {
        return rec != null ? rec.isEnabled() : false;
    }

    public RecordingModule getRecordingModule() {
        if (rec == null) {
            rec = new RecordingModule(this, desktopPnl, scriptManager, cfg);
            rec.setEditorPnl(getDocumentTabbedPane().getActiveEditorPanel());
            rec.setEnabled(false);
            rec.addPropertyChangeListener(glassPanel);
        }
        return rec;
    }

    private Element getActiveElement() {
        Editor ed = getDocumentTabbedPane().getActiveEditorPanel().getEditor();
        return DocumentUtils.getElementForOffset(ed.getStyledDocument(), ed.getCaretPosition());
    }

    private void showScreenshotDialog(Element element) {
        if (dlgScreenshot == null) {
            dlgScreenshot = new ScreenshotDialog(this, ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.screenshotCmdWindowTitle"), true);
            dlgScreenshot.pack();
        }
        dlgScreenshot.setLocationRelativeTo(this);

        if (element == null) {
            element = getActiveElement();
        }
        dlgScreenshot.setVisible(element);
    }

    private void showComparetoDialog(Element element) {
        if (dlgCompareto == null) {
            dlgCompareto = new ComparetoDialog(this, ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.comparetoCmdWindowTitle"), true);
            dlgCompareto.pack();
        }
        dlgCompareto.setLocationRelativeTo(this);
        dlgCompareto.reset();

        if (element == null) {
            element = getActiveElement();
        }
        dlgCompareto.setVisible(element);
    }

    public void showWaitforDialog(List selectedEvents, Element element) {
        if (dlgWaitfor == null) {
            dlgWaitfor = new WaitforDialog(this, ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.waitforCmdWindowTitle"), true);
            dlgWaitfor.pack();
            dlgWaitfor.setRecordingModule(getRecordingModule());
            dlgWaitfor.addPropertyChangeListener(glassPanel);
        }
        if (dlgWaitfor.getLastLocation() == null) {
            dlgWaitfor.setLocationRelativeTo(this);
        } else {
            dlgWaitfor.setLocationRelativeTo(null);
            dlgWaitfor.setLocation(dlgWaitfor.getLastLocation());
        }
        dlgWaitfor.setSelectedEvents(selectedEvents);

        if (element == null) {
            element = getActiveElement();
        }

        dlgWaitfor.setVisible(element);
        if (!dlgWaitfor.canceled) {
            getRecordingModule().insertLine(dlgWaitfor.getCommand(), dlgWaitfor.isEditMode(), false, dlgWaitfor.isResetPrecedingWait());
        }
        getRecordingModule().resetTime();
    }

    private void showGoToLineDialog() {
        if (dlgGoToLine == null) {
            dlgGoToLine = new GoToLineDlg(this);
            dlgGoToLine.pack();
        }
        dlgGoToLine.setLocationRelativeTo(this);
        dlgGoToLine.reset();
        dlgGoToLine.setVisible(true);
    }

    private void showFindDialog(boolean replaceMode) {
        if (dlgFind == null) {
            dlgFind = new SearchDialog(this);
            dlgFind.pack();
        }
        dlgFind.setLocationRelativeTo(this);
        dlgFind.setCurrentSelection();
        dlgFind.setVisible(true);
    }

//    public void showVariableDialog() {
//        if (variableDlg == null) {
//            variableDlg = new VariableBrowserDialog(this, ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.varBrowserWindowTitle"), false);
//            variableDlg.pack();
//            variableDlg.setLocationRelativeTo(this);
//        }
//        variableDlg.refresh();
//        variableDlg.setVisible(true);
//    }
//
    public void showKeyDialog() {
        if (keysAction == null) {
            PressCommand cmd = (PressCommand) scriptManager.getCommandHandlers().get("PRESS");
            keysAction = cmd.getKeyBrowserAction();
        }
        keysAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "keyEvent"));
    }

    public void showOptionsDialog(String selectedPanel, String optionalParentNodeName) {
        if (optionsDlg != null && selectedPanel == null) {
            selectedPanel = optionsDlg.getSelectedNodeLabel();
        }
        if (optionsDlg == null) {
            optionsDlg = new PreferenceDialog(this, ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.prefsWindowTitle"), true);
        } else {
            optionsDlg.resetValues();
        }
        optionsDlg.setSelectedPanel(selectedPanel, optionalParentNodeName);
        if (!optionsDlg.isVisible()) {
            optionsDlg.setVisible(true);
        }
    }

    public void enableFollowExecTrace(boolean enable) {
        if (getActionManager() != null) {
            JToggleButton btn = (JToggleButton) getActionManager().getToolbarButton("followexectrace");
            if (btn != null) {
                btn.setSelected(enable);
            }
        }
        followExecTrace = enable;
    }

    public boolean isFollowExecTrace() {
        return followExecTrace;
    }

    private void executeSelection() {
        EditorPnl pnl = getDocumentTabbedPane().getActiveEditorPanel();
        final TestScriptInterpret ts = pnl.getTestScript();

        if (ts.isPartialExecutionAllowed()) {
            String text = pnl.getEditor().getSelectedText();
            if (text != null && !"".equals(text.trim())) {
                Editor ed = getDocumentTabbedPane().getActiveEditorPanel().getEditor();
                ts.setSelection(ed.getSelectionStart(), ed.getSelectionEnd());
                Thread thread = new ExecOrCompileThread(ts, true, this);
                thread.start();
            }
        }
    }

    private void executeScript() {
        EditorPnl pnl = getDocumentTabbedPane().getActiveEditorPanel();
        final TestScriptInterpret ts = pnl.getTestScript();

        // Bug 2861279 - Execute Selection breaks the Execute functionality
        // This is not really necessary because the selection should have been
        // cleared up by the ScriptListener. We just double make sure that the selection
        // positions get reset before executing the whole script.
        if (ts.isPartialExecutionAllowed()) {
            ts.resetSelection();
        }
        // -- End of bug 2861279 fix

        Thread thread = new ExecOrCompileThread(ts, true, this);
        thread.start();
    }

    public void compileScript(EditorPnl ed) {
//        Thread thread = new ExecOrCompileThread(ed.getTestScript(), false, this);
//        thread.run();  // Do not start the thread, just compile
//        msgPanel.setMessageVector(ed.getTestScript().getCompilationContext().getCompilationErrors(), false);
        ed.compile();
    }

    // TODO: check why it is not used
//    private boolean validateScript(GenericWrapper w) {
//        Boolean b = cfg.getBoolean(SCRIPT_HANDLER_CHECK_SYNTAX_BEFORE_EXECUTION);
//        if ((b == null || b.booleanValue()) && w.getWrapperType() != TestWrapper.WRAPPER_TYPE_JAVA) {
//            Map repository = scriptManager.validate(w);
//            if (scriptManager.getValidationMessages().size() > 0) {
//                int option = 0;
//                Integer warningOption =
//                        cfg.getInteger("warning.executeWhenScriptContainsErrors");
//                if (warningOption == null || warningOption.intValue() < 0) {
//                    Object options[] = {
//                        ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgCompileErrorsOptionYes"),
//                        ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgCompileErrorsOptionNo")
//                    };
//                    option = Utils.showConfigurableMessageDialog(this,
//                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgCompileErrorsTitle"),
//                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgCompileErrorsText"),
//                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgCompileErrorsDoNotAsk"),
//                            "warning.executeWhenScriptContainsErrors",
//                            options,
//                            0);
//                } else {
//                    option = warningOption.intValue();
//                }
//                if (option == 1) {
//                    msgPanel.setMessageVector(scriptManager.getValidationMessages());
//                    setMessagePaneVisible(true);
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
    void setMessagePaneVisible(boolean visible) {
        if (visible && globalSplitPane.getBottomComponent() == null) {
            globalSplitPane.setBottomComponent(msgPanel);
        }
        msgPanel.setVisible(visible);
        globalSplitPane.setDividerSize(visible ? 4 : 0);
        if (visible) {
            globalSplitPane.resetToPreferredSizes();
        }
    }

    /**
     * Centers child dialog to the middle of parent window.
     *
     * @param parent reference to the parent window
     * @param dlg    dialog which should be centered
     */
    public static void centerDlg(JFrame parent, JDialog dlg) {
        if (parent == null) {
            Utils.centerOnScreen(dlg);
        } else {
            Dimension dlgSize = dlg.getSize();
            Dimension frameSize = parent.getSize();
            Point frameLoc = parent.getLocation();
            dlg.setLocation((frameSize.width - dlgSize.width) / 2 + frameLoc.x,
                    (frameSize.height - dlgSize.height) / 2 + frameLoc.y);
        }
    }

    public void configurationChanged(ConfigurationChangeEvent evt) {
        // Recent scripts have changed -> rebuild the Recent Scripts menu
        if (evt.getPropertyName().equals(IO_RECENT_SCRIPTS)) {
            List v = cfg.getListOfStrings(IO_RECENT_SCRIPTS);
            rebuildScriptMenu("menu.Reopen", v);
            menuEnabler();
        } // Recent servers have changed -> rebuild the Recent Scripts menu
        else if (evt.getPropertyName().equals(IO_RECENT_SERVERS)) {
            List v = cfg.getListOfStrings(IO_RECENT_SERVERS);
            rebuildServerMenu("menu.Reconnect", v);
            menuEnabler();
        } else if (evt.getPropertyName().equals(TOOLBAR_LOCATION)) {
        }
    }

    private void rebuildScriptMenu(String strKey, List v) {
        if (v.size() > MAX_DYNAMIC_MENU_ITEMS) {
            v = new ArrayList(v.subList(0, MAX_DYNAMIC_MENU_ITEMS - 1));
        }
        Action[] a = new Action[v.size()];
        for (int i = 0; i < v.size(); i++) {
            a[i] = new OpenAction(v.get(i).toString(), this);
        }
        getActionManager().addActions(a);
        getActionManager().createDynamicMenu(strKey, v);
    }

    private void rebuildServerMenu(String strKey, List v) {
        if (v.size() > MAX_DYNAMIC_MENU_ITEMS) {
            v = new ArrayList(v.subList(0, MAX_DYNAMIC_MENU_ITEMS - 1));
        }
        Action[] a = new Action[v.size()];
        for (int i = 0; i < v.size(); i++) {
            a[i] = new ConnectAction(v.get(i).toString(), this);
        }
        getActionManager().addActions(a);
        getActionManager().createDynamicMenu(strKey, v);
    }

    public void scriptEvent(ScriptEvent event) {
        switch (event.getType()) {

            // Executed line has changed. If the editor is not open, open it
            case ScriptEvent.SCRIPT_EXECUTED_LINE_CHANGED:
                Boolean b = cfg.getBoolean(ConfigurationKeys.SCRIPT_HANDLER_OPEN_INCLUDED_FILES);
                boolean open = event.getInterpret().getURI() != null && (b != null && b);
                getDocumentTabbedPane().getEditorForDocument(event.getInterpret().getDocument(), event.getScriptManager(), open, open);
                break;

            // A message is available -> display it in the status bar
            case ScriptEvent.SCRIPT_MESSAGE_AVAILABLE:
                String text = event.getMessage() == null ? " " : event.getMessage();
                if (text.equals("")) {
                    text = " ";
                }
                statusBar.getFieldLeft().setText(text);
                break;

            // Script execution has started -> switch to read only mode (optional)
            // and enable/disable menu items
            case ScriptEvent.SCRIPT_EXECUTION_STARTED:

                // Look if the option of switching to read-only mode is selected
                boolean switchToReadOnly = cfg.getBoolean("rfb.executeReadOnly").booleanValue();
                if (switchToReadOnly && resetReadOnlyModeAfterExecution == null) {
                    AbstractButton btnReadOnly = (AbstractButton) getActionManager().getToolbarButton("readonly");

                    // Bug 2861305 - Read only mode not reset intermittently after execution
                    // Set on the "reset read only after execution" flag only if it is not currently on.
                    resetReadOnlyModeAfterExecution = !btnReadOnly.isSelected();
                    // End of bug 2861305 fix

                    if (!btnReadOnly.isSelected()) {
                        btnReadOnly.doClick();
                    }
                }
                menuEnabler();
                if (client != null && client.isLocalDisplay() && !minimized) {
                    minimizeIfConfigured();
                }
                statusBar.getFieldLeft().setText(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.statusBarMsgRunning"));
                break;

            case ScriptEvent.SCRIPT_EXECUTION_FINISHED:
                this.exitCode = event.getContext().getExitCode();

                // Bug 2861279 - Execute Selection breaks the Execute functionality
                // Reset the script selection pointers.
                if (event.getInterpret().isPartialExecutionAllowed()) {
                    event.getInterpret().resetSelection();
                }
                // -- End of bug 2861279 fix

                // Bug fix: the execution time was not displayed correctly because the CONTEXT_EXECUTION_DURATION
                // variable is not yet populated at this point. That's why we have to calculate the time
                // from the current time and execution start date.
                Date startDate = (Date) event.getContext().get(ScriptingContext.CONTEXT_EXECUTION_START_DATE);
                statusBar.getFieldLeft().setText(MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.statusBarMsgFinished"),
                        Utils.getTimePeriodForDisplay(System.currentTimeMillis() - startDate.getTime(), true)));

                // Bug fix: open the editor only if the interpret has a valid file
                Boolean b2 = cfg.getBoolean(ConfigurationKeys.SCRIPT_HANDLER_OPEN_INCLUDED_FILES);
                boolean open2 = event.getInterpret().getURI() != null && (b2 != null && b2);
                getDocumentTabbedPane().getEditorForDocument(event.getInterpret().getDocument(), event.getScriptManager(), open2, open2);

                // Scroll to the last executed element
                Element el = (Element) event.getContext().get(ScriptingContext.CONTEXT_LAST_EXECUTED_DOCUMENT_ELEMENT);
                if (followExecTrace && el != null) {
                    getDocumentTabbedPane().getActiveEditorPanel().getEditor().scrollElementRectToVisible(el);
                }
                menuEnabler();

                // Check if the "Switch To Read Only Mode On Execution" is on
                switchToReadOnly = cfg.getBoolean("rfb.executeReadOnly").booleanValue();
                if (switchToReadOnly) {

                    // Bug 2861305 - Read only mode not reset intermittently after execution
                    // Only reset if the runtime flag populated on the script start is on
                    if (resetReadOnlyModeAfterExecution != null && resetReadOnlyModeAfterExecution) {
                        AbstractButton btnReadOnly = (AbstractButton) getActionManager().getToolbarButton("readonly");
                        if (btnReadOnly.isSelected()) {
                            btnReadOnly.doClick();
                        }
                    }
                    // End of bug 2861305 fix
                }

                resetReadOnlyModeAfterExecution = null;
                maximizeIfMinimized();

                // Look if there is a script to be run automatically. If yes, display the timeout dialog and shutdown
                // the application.
                final Object script = scriptManager.getScriptToRun();
                if (script != null) {

                    Thread t = new Thread("MainFrame: Timeout after automatic execution") {

                        public void run() {
                            Integer param = cfg.getInteger("scripting.delayAfterAutomaticExecutionSeconds");
                            int timeout = param == null ? 15 : param.intValue();

                            // Bug 2926140: do not display the window at all if the time out is zero
                            TimeOutDialog dlg = null;
                            if (timeout >= 0) {
                                dlg = new TimeOutDialog(MainFrame.this, script.toString(), 1000 * timeout, true,
                                        ApplicationSupport.getString("options.scripting.execute"),
                                        ApplicationSupport.getString("options.scripting.scripting"));
                                dlg.setVisible(true);
                            }

                            if (dlg == null || !dlg.isCanceled()) {
                                MainFrame.this.windowClosing(null);
                            } else {
                                scriptManager.setScriptToRun(null);
                            }
                        }
                    };
                    t.start();
                }
//                }
                break;

            // An Include command is executed -> open it in editor unless it's open
            case ScriptEvent.SCRIPT_EXECUTING_INCLUDE:
                b = cfg.getBoolean(ConfigurationKeys.SCRIPT_HANDLER_OPEN_INCLUDED_FILES);
                if (b != null && b.booleanValue()) {
                    getDocumentTabbedPane().getEditorForTestScript(event.getInterpret(), false, false);
                }
                break;
            case ScriptEvent.SCRIPT_EXECUTION_PAUSED:
            case ScriptEvent.SCRIPT_EXECUTION_RESUMED:
                Object o = getActionManager().getToolbarButton("pause");
                if (o != null && o instanceof JToggleButton) {
                    ((JToggleButton) o).setSelected(event.getType() == ScriptEvent.SCRIPT_EXECUTION_PAUSED);
                }
                break;
            case ScriptEvent.SCRIPT_GOING_TO_RUN_LINE:
                if (stepMode) {
                    b = cfg.getBoolean(ConfigurationKeys.SCRIPT_HANDLER_OPEN_INCLUDED_FILES);
                    Element e = (Element) event.getContext().get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT);
                    boolean isElementVisible = e.getDocument().equals(getDocumentTabbedPane().getActiveEditorPanel().getEditor().getDocument());

                    if (isElementVisible || (isFollowExecTrace() && b.booleanValue())) {
                        throw new PauseRequestException(this, ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.pauseReasonStepMode"));
                    }
                }
                break;
            case ScriptEvent.SCRIPT_CLIENT_CREATED:
                setClient(event.getContext().getClient());
                break;
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
//        System.out.println("MainFrame: received "+evt.getPropertyName());
        if (evt.getPropertyName().equals("caretUpdate")) {
            statusBar.updateEditorPosition((Editor) evt.getOldValue());

        } else if ("okPressed".equals(evt.getPropertyName())) {
//            System.out.println("defined rectangle: "+evt.getNewValue());
            Map v = new HashMap();
            v.put(ScreenshotCommand.PARAM_AREA, evt.getNewValue());
            try {
                DocumentUtils.updateCommand(editedElement, v);
            } catch (BadLocationException e) {
                e.printStackTrace();
                // TODO: error message
            }
            setDrawPanelVisible(false);
            editedElement = null;
        } else if ("cancelPressed".equals(evt.getPropertyName())) {
            setDrawPanelVisible(false);
            editedElement = null;
        }
    }

    private void setTitle(RemoteDesktopClient client) {
        // Change the main window title
        String ver = ApplicationSupport.APPLICATION_NAME + " " + ApplicationSupport.APPLICATION_VERSION;
        if (client != null) {
            int w = client.getDesktopWidth();
            int h = client.getDesktopHeight();
            String port = "";
            if (client.getPort() >= 0) {
                port = ":" + client.getPort();
            } else if (client.getDefaultPort() >= 0) {
                port = ":" + client.getDefaultPort();
            }

            String url = client.getConnectString(); //client.getProtocol() + "://" + client.getHost() + port;

            String pattern = ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.connectedWindowTitle");
            Object params[] = {
                ver,
                url != null ? url.toLowerCase() : "",
                w > 0 ? "" + w : "?",
                h > 0 ? "" + h : "?"
            };
            setTitle(MessageFormat.format(pattern, params));
        } else {
            String s = ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.initialWindowTitle");
            setTitle(MessageFormat.format(s, ver));
        }
    }

    public void serverMessageReceived(RemoteDesktopServerEvent evt) {
        int type = evt.getMessageType();
        if (type == RemoteDesktopServerEvent.SERVER_CONNECTED_EVENT) {

            setTitle(evt.getClient());

            int w = client.getDesktopWidth();
            int h = client.getDesktopHeight();

            // Adjust the status bar field sizes to be able to display
            // the range of coordinates
            if (statusBar != null) {
                statusBar.computeFieldSizes(client);
            }

            // Update the menu status
            if (getActionManager() != null) {
                menuEnabler();
            }

            // If the display is not local, hide the Welcome panel and display
            // the remote desktop in the desktop viewer component.
            if (!evt.getClient().isLocalDisplay()) {
                if (pnlwelcome != null) {
                    pnlwelcome.setVisible(false);
                }

                // If the remote desktop is bigger, ask if to adjust the window size
                // See bug 12007
                boolean adjust = false;
                if (this.isShowing() && desktopPnl.getWidth() < w) {
                    Integer warningOption = cfg.getInteger("warning.adjustWindowSize");
                    if (scriptManager.getExecutingTestScripts().size() == 0) {
                        int result = -1;
                        if (warningOption == null || warningOption.intValue() < 0) {
                            Object options[] = {
                                ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgResizeOptionYes"),
                                ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgResizeOptionNo")};
                            result = Utils.showConfigurableMessageDialog(this,
                                    ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgResizeWindowTitle"),
                                    MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgResizeText"), w, h, desktopPnl.getWidth(), desktopPnl.getHeight()),
                                    ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgResizeRemember"),
                                    "warning.adjustWindowSize",
                                    options,
                                    0);
                        } else {
                            result = warningOption.intValue();
                        }
                        adjust = (result == JOptionPane.YES_OPTION);
                    } else {
                        // Bug fix in 2.0.5 - when connecting from a running test script,
                        // respect the preference (in the past it always resized)
                        adjust = warningOption != null && warningOption == 0;
                    }
                }

                if (adjust) {
                    leftSplitPane.setPreferredSize(leftSplitPane.getSize());
                    resetDesktopScrollSize(w, h);
                    this.pack();
                    resetDesktopScrollSize(w, h);
                    Utils.centerOnScreen(this);
                } else {
//                    if (splitPane != null) {
//                        splitPane.resetToPreferredSizes();
//                        resetDesktopScrollSize(w, h);
//                    }
                }
            } else {  // Local display connected -> minimize if configured

                if (pnlwelcome != null && !pnlwelcome.isVisible()) {
                    pnlwelcome.setVisible(true);
                }
//                minimizeIfConfigured();
            }

            // Look if there is a script to be run automatically
//            runCliScript();

        } else if (type == RemoteDesktopServerEvent.SERVER_DISCONNECTED_EVENT) {
            if (pnlwelcome != null) {
                pnlwelcome.setVisible(true);
            }
            String s = ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.initialWindowTitle");
            setTitle(MessageFormat.format(s, ApplicationSupport.APPLICATION_NAME));
            if (statusBar != null) {
                statusBar.getUpdateField().setText("");
            }
            if (getActionManager() != null && getDocumentTabbedPane() != null) {
                menuEnabler();
            }
            maximizeIfMinimized();

        } else if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_BELL_EVENT) {
            String msg = "BELL";
            if (statusBar.getUpdateField().getText().startsWith(msg)) {
                int count = 1;
                try {
                    String value = statusBar.getUpdateField().getText();
                    value = value.substring(msg.length() + 1);
                    value = value.substring(0, value.length() - 1);
                    count = Integer.parseInt(value) + 1;
                } catch (Exception ex) {
                    count = 2;
                }
                msg = "BELL " + count + "x";
            }
            statusBar.getUpdateField().setText(msg);
        } else if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_UPDATE_EVENT) {
            if (statusBar != null) {
                float percentage = (float) (100 * evt.getUpdateRect().width * evt.getUpdateRect().height) / (client.getDesktopWidth() * client.getDesktopHeight());
                statusBar.updateRectangleChanged(evt, percentage);
            }
        } else if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_IO_ERROR_EVENT) {
            if (scriptManager.getExecutingTestScripts().size() > 0) {
                System.out.println(evt.getException().getMessage());
            } else {
                Integer flag = getUserConfiguration().getInteger("warning.rfbConnectionError");
                if (flag == null || flag.intValue() < 0) {
                    int option = Utils.showConfigurableMessageDialog(this,
                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.errorMessageWindowTitle"),
                            evt.getException().getMessage(),
                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgDoNotShow"),
                            "warning.rfbConnectionError",
                            new Object[]{ApplicationSupport.getString("btnOk"), ApplicationSupport.getString("btnReconnect")},
                            0);
                    if (option == 1) {
                        RemoteDesktopClient cl = evt.getClient();
//                        login(cl.getProtocol() + "://" + cl.getHost() + (cl.getPort() >= 0 ? ":" + cl.getPort() : ""));
                        login(cl.getConnectString(), true);
                    }
                }
            }
        } else if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_CLIPBOARD_EVENT) {
            // Enhancement in 2.0.3 - display the text received through the server clipboard message
            statusBar.getFieldLeft().setText(
                    MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.clipboardReceived"), evt.getClipboardText()));
        }
    }

    public DesktopGlassPanel getGlassPanel() {
        return glassPanel;
    }

    public ScriptManager getScriptHandler() {
        return scriptManager;
    }

    public RemoteDesktopClient getClient() {
        return client;
    }

    public UserConfiguration getUserConfiguration() {
        return cfg;
    }

    private void setToolPanelVisible(boolean visible) {
        if (visible) {
            getRecordingModule();
            if (pnlTools == null) {
                pnlTools = new RecordingPanel(this);
                pnlTools.addPropertyChangeListener(glassPanel);
                pnlTools.setRecordingModule(rec);
                leftSplitPane.addPropertyChangeListener(pnlTools);
            }
            leftSplitPane.setTopComponent(pnlTools);
            leftSplitPane.setDividerSize(splitPane.getDividerSize());
            leftSplitPane.resetToPreferredSizes();

            // The point of the following code is to add a spacing reserve equal
            // to height of the tab. This is necessary because the tabbed pane returns
            // minimum height equal to the tabbed pane displayed with tabs in one row
            // but practically the component gets squeezed a bit and displays
            // the tabs in two rows.
            Component c = pnlTools.getTabbedPane().getTabComponentAt(0);
            int reserve = c != null ? c.getBounds().height : 17;
            leftSplitPane.setDividerLocation(pnlTools.getMinimumSize().height + reserve);
        } else {
            leftSplitPane.setTopComponent(null);
            leftSplitPane.setDividerSize(0);
            leftSplitPane.resetToPreferredSizes();
        }
        if (pnlTools != null) {
            pnlTools.setVisible(visible);
        }
    }

    public RecordingPanel getPnlTools() {
        return pnlTools;
    }

    public boolean isHelpAvailable() {
        return helpBroker != null && helpBroker.isHelpAvailable();
    }

    private void setClient(RemoteDesktopClient client) {
        if (this.client != null) {
            this.client.removeServerListener(this);
        }
        this.client = client;
        if (pnlTools != null) {
            pnlTools.setClient(client);
        }
        if (this.client != null) {
            this.client.addServerListener(this);
        }
        setTitle(client);
    }

    public void commandEvent(CommandEvent e) {
        String code = e.getActionCode();
        if (CompareToCommand.ACTION_EDIT_COMPARETO.equals(code)) {
            showComparetoDialog((Element) e.getCustomObject());
        } else if (EVENT_DISPLAY_PREFERENCES.equals(code)) {
            String selected = e.getCustomObject() != null ? e.getCustomObject().toString() : null;
            showOptionsDialog(selected, null);
        } else if (ScreenshotCommand.ACTION_EDIT_SCREENSHOT.equals(code)) {
            showScreenshotDialog((Element) e.getCustomObject());
        } else if (ScreenshotCommand.ACTION_EDIT_SCREENSHOT.equals(code)) {
            editedElement = getDocumentTabbedPane().getActiveEditorPanel().getEditor().getPopUpMenuElement();
            List v = new ArrayList();
            Map t = DocumentUtils.getTokens(editedElement, v);
            String s;
            for (int i = 0; i < v.size(); i++) {
                s = (String) v.get(i);
                if (s.equalsIgnoreCase(ScreenshotCommand.PARAM_AREA)) {
                    try {
                        TokenParser parser = new TokenParserImpl();
                        RemoteDesktopClient client = e.getContext().getClient();
                        Rectangle defaults = null;
                        if (client != null && client.isConnected()) {
                            defaults = new Rectangle(0, 0, client.getDesktopWidth(), client.getDesktopHeight());
                        }
                        Rectangle r = parser.parseRectangle((String) t.get(s), defaults, ScreenshotCommand.PARAM_AREA);
                        drawPanel.setDragRect(r);
                    } catch (Exception ex) {
                    }
                }
            }

            Object options[] = {ApplicationSupport.getString("btnOk"), ApplicationSupport.getString("btnCancel")};

            int pref = cfg.getInteger("warning.editScreenshotAreaInfo").intValue();
            if (pref < 0) {
                int option = Utils.showConfigurableMessageDialog(this,
                        ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgEditScreenshotAreaTitle"),
                        ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgEditScreenshotAreaInfo"),
                        ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.msgEditScreenshotAreaDoNotShow"),
                        "warning.editScreenshotAreaInfo",
                        options,
                        0);
                switch (option) {
                    case 0:
                        setDrawPanelVisible(true);
                        break;
                    case 1:
                        break;
                }
            } else {
                setDrawPanelVisible(true);
            }

//        } else if (EVENT_DISPLAY_VARIABLES.equals(code)) {
//            showVariableDialog();
        } else if (WaitforCommand.ACTION_EDIT_WAITFOR.equals(code)) {
            showWaitforDialog(null, (Element) e.getCustomObject());
        }
    }

    /**
     * @return the actionManager
     */
    public ActionManager getActionManager() {
        return actionManager;
    }

    /**
     * @return the toolBar
     */
    public JToolBar getToolBar() {
        return toolBar;
    }

    /**
     * @return the tabbedPane
     */
    public DocumentTabbedPane getDocumentTabbedPane() {
        return tabbedPane;
    }

    /**
     * @return the statusBar
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }

    /**
     * @return the desktopPnl
     */
    public DesktopViewer getDesktopPnl() {
        return desktopPnl;
    }

    /**
     * @return the msgPanel
     */
    public ValidationMessagePanel getMsgPanel() {
        return msgPanel;
    }

    /**
     * @return the drawPanel
     */
    public DrawPanel getDrawPanel() {
        return drawPanel;
    }

    class OpenAction extends AbstractAction {

        String file;
        MainFrame frame;

        OpenAction(String file, MainFrame frame) {
            this.file = file;
            this.frame = frame;
            putValue(Action.NAME, file);
        }

        public void actionPerformed(ActionEvent e) {
            setEditorFile(file);
        }

        public void setEditorFile(String file) {
            File f = new File(file);
            cfg.updateListOfRecents(f.getAbsolutePath(),
                    IO_RECENT_SCRIPTS,
                    MAX_DYNAMIC_MENU_ITEMS);
            try {
                getDocumentTabbedPane().getEditorForFile(f);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            frame.menuEnabler();
        }

//        public void setJavaStub(JavaTestStub stub) {
//            try {
//                EditorPnl e = tabbedPane.getEditorForJavaStub(stub);
//                if (e.getFile() != null) {
//                    cfg.updateListOfRecents(e.getFile().getAbsolutePath(),
//                            IO_RECENT_SCRIPTS,
//                            MAX_DYNAMIC_MENU_ITEMS);
//                }
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//            frame.menuEnabler();
//        }
        /**
         * Returns true if the action is enabled.
         *
         * @return true if the action is enabled, false otherwise
         * @see Action#isEnabled
         */
        public boolean isEnabled() {
            return true;
        }
    }

    class CustomBorderLayout extends BorderLayout {

        public void addLayoutComponent(Component comp, Object constraints) {
            if (comp instanceof JToolBar) {
                cfg.removeConfigurationListener(MainFrame.this);
                cfg.setString("ui.mainframe.toolbarLocation", constraints.toString());
                cfg.addConfigurationListener(MainFrame.this);

                // This is a workaround that the tool bar doesn't set its orientation
                // when it is added to a vertical position
                JToolBar tb = (JToolBar) comp;
                if (constraints.equals(WEST) || constraints.equals(EAST)) {
                    tb.setOrientation(JToolBar.VERTICAL);
                } else {
                    tb.setOrientation(JToolBar.HORIZONTAL);
                }
            }
            super.addLayoutComponent(comp, constraints);
        }
    }

    class ConnectAction extends AbstractAction {

        String server;
        MainFrame frame;

        ConnectAction(String server, MainFrame frame) {
            this.server = server;
            this.frame = frame;
            putValue(Action.NAME, server);
        }

        public void actionPerformed(ActionEvent e) {
            if (server != null) {
                // Bug 2923343 fix in 2.0.1 - close the current client if connected
                if (client != null && (client.isConnected() || client.isConnecting())) {
                    try {
                        client.close();
                        client.destroy();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                RemoteDesktopClient cl = RemoteDesktopClientFactory.getInstance().getClientForURI(server);
                frame.getScriptHandler().setClient(cl);
                try {
                    cfg.updateListOfRecents(Utils.getURI(server).toString(), IO_RECENT_SERVERS, MAX_DYNAMIC_MENU_ITEMS, false);
                    UserConfiguration.saveConfiguration();
                } catch (URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }
            frame.login(server, true);
            frame.menuEnabler();
        }

        /**
         * Returns true if the action is enabled.
         *
         * @return true if the action is enabled, false otherwise
         * @see Action#isEnabled
         */
        public boolean isEnabled() {
            return true;
        }
    }

    class ToolsAction extends AbstractAction {

        ToolsAction(String cmdName) {
            putValue(Action.NAME, cmdName);
        }

        public void actionPerformed(ActionEvent e) {
            if (!process.isConsoleMode()) {
                ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), (String) getValue(Action.NAME));
                MainFrame.this.actionPerformed(ae);
            }
        }
    }

    public AutomatedRunnableImpl getProcess() {
        return process;
    }

    public static void setResourceBundle(ResourceBundle aResourceBundle) {
        resourceBundle = aResourceBundle;
    }
}
