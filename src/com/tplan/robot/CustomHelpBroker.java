/*
 * T-Plan Robot, automated testing tool based on remote desktop technologies.
 * Copyright (C) 2009-2011 T-Plan Limited (http://www.t-plan.co.uk),
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
package com.tplan.robot;

import com.tplan.robot.gui.*;

import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.util.Utils;
import javax.help.*;
import javax.help.UnsupportedOperationException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Enumeration;

/**
 * <p>Customized help broker which encapsulates both the help set and
 * Online Help (OLH) window.
 * The class extends the <code>HelpBroker</code> interface of the
 * JavaHelp package to provide a special functionality of the
 * help window and to fix the incorrect behavior of the default help broker when
 * the help dialog switches owners. This is necessary because the help window
 * is first owned by the {@link com.tplan.robot.gui.dialogs.LoginDialog Login Dialog} and later on the ownership
 * gets transferred to the application main frame (see {@link com.tplan.robot.gui.MainFrame}).</p>
 *
 * <p>The help set conforms to the requirements of the JavaHelp 1.1.3 API.
 * All data files are located in the <code>com.tplan.robot.helpset</code> package.
 * The help structure is described by three files:
 * <ul>
 * <li><code>HelpSet_en.hs</code> is a top level file defining how the help window
 * looks like and where the help IDs map and help tree files are located,</li>
 * <li><code>Map.jhm</code> contains the map of help IDs and their corresponding URLs,</li>
 * <li><code>TOC.xml</code> defines the tree of the help topics and
 * mapping of the tree nodes onto help IDs.</li>
 * </ul>
 * @product.signature
 */
public final class CustomHelpBroker implements HelpBroker, HelpDispatcher {

    private HelpSet helpSet;
    private JHelp help;
    private JDialog helpFrame;
    private ActionListener helpListener;
    private MainFrame mainFrame;
    private File helpSetFile;

    /**
     * Constructor. It loads the OLH files and initializes this
     * help broker.
     *
     * @param loader a class loader to be used to load the help set. We have to
     * load it through a loader because the help files are zipped inside
     * the application JAR file.
     */
    public CustomHelpBroker(ClassLoader loader) throws Exception {
        File hsFile = Utils.getHelpSetFile();
        if (hsFile != null) {
            helpSetFile = hsFile;
            HelpSet hs = new HelpSet(loader, hsFile.toURI().toURL());
            setHelpSet(hs);
            helpListener = new CSH.DisplayHelpAfterTracking(this);
        } else {
            throw new FileNotFoundException("Default English help content not found at "+
                    Utils.getInstallPath() + File.separator + ApplicationSupport.APPLICATION_HELP_SET_DIR +
                    File.separator + "en" + File.separator + ApplicationSupport.APPLICATION_HELP_SET_FILE +
                    "!\nThe Help will display online web documentation instead.\n");
        }
    }

    /**
     * Initialize the help dialog. The argument <code>mainFrame</code> will
     * serve as the dialog owner. The dialog will be also centered with regards
     * to the main frame.
     *
     * @param mainFrame application main frame (see {@link com.tplan.robot.gui.MainFrame}).
     */
    public void initHelpWindow(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        help = new JHelp(helpSet);
        helpFrame = new JDialog(mainFrame, MessageFormat.format(ApplicationSupport.getResourceBundle().getString("help.OLH.title"), ApplicationSupport.APPLICATION_NAME), false);
        JPanel pnlHelp = new JPanel(new BorderLayout());
        pnlHelp.add(help, BorderLayout.CENTER);
        helpFrame.setContentPane(pnlHelp);
        helpFrame.pack();
        Dimension d = helpFrame.getSize();
        helpFrame.setSize(d.width + 200, d.height);
        helpFrame.setLocationRelativeTo(mainFrame);

        // Close the window on escape
        String actionKey = "escapeclose";
        helpFrame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);
        helpFrame.getRootPane().getActionMap().put(actionKey, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                helpFrame.dispose();
            }
        });

        // Remove the default help search navigators - we are not showing the search tab
        Enumeration e = help.getHelpNavigators();
        Object o;
        while (e.hasMoreElements()) {
            o = e.nextElement();
            if (o instanceof JHelpSearchNavigator) {
                help.removeHelpNavigator((JHelpSearchNavigator) o);
            }
        }
    }

    /**
     * Set modality of the help dialog.
     * @param modal true switches modality of the window on, false off.
     */
    public void setModal(boolean modal) {
        helpFrame.setModal(modal);
    }

    /**
     * Get the help set which this broker manages.
     * @return help set owned by this broker.
     */
    public HelpSet getHelpSet() {
        return helpSet;
    }

    /**
     * Set the help set which this help broker manages.
     * @param helpSet a help set.
     */
    public void setHelpSet(HelpSet helpSet) {
        this.helpSet = helpSet;
    }

    /**
     * Get the help locale. This implementation always returns null.
     * @return always returns null.
     */
    public Locale getLocale() {
        return null;
    }

    /**
     * Set the help locale. This implementation is void and doesn't perform anything.
     * It is here only because it is defined in the <code>HelpBroker</code> interface.
     * @param locale a locale (will not be used since the method defines no functionality).
     */
    public void setLocale(Locale locale) {
    }

    /**
     * Get the help dialog font.
     * @return help dialog font or null if the dialog hasn't been created yet..
     */
    public Font getFont() {
        return helpFrame == null ? null : helpFrame.getFont();
    }

    /**
     * Set the help dialog font.
     * @param font a font for the help dialog.
     */
    public void setFont(Font font) {
        if (helpFrame != null) {
            helpFrame.setFont(font);
        }
    }

    /**
     * Set the current view. This implementation is void and doesn't perform anything.
     * It is here only because it is defined in the <code>HelpBroker</code> interface.
     * @param s a new view to be displayed (will not be used since the method defines no functionality).
     */
    public void setCurrentView(String s) {
    }

    /**
     * Set the current view. This implementation always returns null.
     * @return always returns null.
     */
    public String getCurrentView() {
        return null;
    }

    /**
     * Initialize the help presentation. This implementation is void and doesn't perform anything.
     * It is here only because it is defined in the <code>HelpBroker</code> interface.
     */
    public void initPresentation() {
    }

    /**
     * Display or hide the help dialog.
     * @param b true displays the dialog, false hides it.
     */
    public void setDisplayed(boolean b) throws javax.help.UnsupportedOperationException {
        int option = 0;
        Integer warningOption =
                UserConfiguration.getInstance().getInteger("warning.useWebBrowserForHelp");
        if (warningOption == null || warningOption.intValue() < 0) {
            Object options[] = new String[]{
                ApplicationSupport.getString("help.preferredBrowser.option.webBrowser"),
                ApplicationSupport.getString("help.preferredBrowser.option.javaWindow")
            };
            option = Utils.showConfigurableMessageDialog(mainFrame,
                    ApplicationSupport.getString("help.preferredBrowser.title"),
                    ApplicationSupport.getString("help.preferredBrowser.text"),
                    ApplicationSupport.getString("help.preferredBrowser.dontAskAnymore"),
                    "warning.useWebBrowserForHelp",
                    options,
                    0);
        } else {
            option = warningOption.intValue();
        }

        if (option == 0) {  // Web browser
            Utils.execOpenURL(getCurrentURL().toString());
        } else if (option == 1) {
            if (helpFrame == null) {  // Java Help window
                initHelpWindow(mainFrame);
            }
            helpFrame.setVisible(true);
        }
    }

    /**
     * Find out whether the help window is displayed or not.
     * @return true if the help dialog is visible, false if not or if the dialog hasn't been created yet.
     */
    public boolean isDisplayed() {
        return helpFrame == null ? false : helpFrame.isVisible();
    }

    /**
     * Set location of the help dialog on the screen.
     * @param point new location for the dialog.
     */
    public void setLocation(Point point) throws UnsupportedOperationException {
        if (helpFrame != null) {
            helpFrame.setLocation(point);
        }
    }

    /**
     * Get the preferred location of the help dialog (it doesn't have to be displayed).
     * @return location of the dialog or null if the dialog hasn't been created yet.
     */
    public Point getLocation() throws UnsupportedOperationException {
        return helpFrame == null ? null : helpFrame.getLocation();
    }

    /**
     * Set size of the help dialog.
     * @param dimension new dialog size.
     */
    public void setSize(Dimension dimension) throws UnsupportedOperationException {
        if (helpFrame != null) {
            helpFrame.setSize(dimension);
        }
    }

    /**
     * Set size of the help dialog.
     * @return dialog size or null if the dialog hasn't been created yet.
     */
    public Dimension getSize() throws UnsupportedOperationException {
        return helpFrame == null ? null : helpFrame.getSize();
    }

    /**
     * Display a view. This implementation is void and doesn't perform anything.
     * It is here only because it is defined in the <code>HelpBroker</code> interface.
     * @param b a boolean (will not be used since the method defines no functionality).
     */
    public void setViewDisplayed(boolean b) {
    }

    /**
     * Find out whether the view is displayed. This implementation is void and
     * always returns false.
     * @return always returns false.
     */
    public boolean isViewDisplayed() {
        return false;
    }

    /**
     * Set the currently displayed ID instance. Each ID corresponds to one help page
     * so this method actually switches the currently displayed help topic.
     *
     * @param id a help ID (<code>Map.ID</code> instance). Help IDs are defined
     * in the <code>Map.jhm</code> file located in the <code>com.tplan.robot.helpset</code>
     * package.
     */
    public void setCurrentID(Map.ID id) throws InvalidHelpSetContextException {
        help.setCurrentID(id);
    }

    /**
     * Set the currently displayed ID identified by name. Each ID corresponds
     * to one help page so this method actually switches the currently displayed help topic.
     *
     * @param s a help ID (String instance). Help IDs are defined
     * in the <code>Map.jhm</code> file located in the <code>com.tplan.robot.helpset</code>
     * package.
     */
    public void setCurrentID(String s) throws BadIDException {
        help.setCurrentID(s);
    }

    /**
     * Get the currently displayed ID instance.
     *
     * @return ID of the currently displayed help topic (a <code>Map.ID</code> instance). To get the list
     * of help IDs see the <code>Map.jhm</code> file located
     * in the <code>com.tplan.robot.helpset</code> package.
     */
    public Map.ID getCurrentID() {
        return help.getModel().getCurrentID();
    }

    /**
     * Set the currently displayed URL.
     * @param url an URL to be displayed in the help dialog.
     */
    public void setCurrentURL(URL url) {
        help.setCurrentURL(url);
    }

    /**
     * Get the currently displayed URL.
     * @return current URL displayed in the help dialog.
     */
    public URL getCurrentURL() {
        return help.getModel().getCurrentURL();
    }

    /**
     * Bind main frame components with their help topic keys/IDs.
     * @param mainFrame the main application frame.
     */
    public void bindMainFrameComponents(MainFrame mainFrame) {

        // Now register the components with the help system
        CSH.setHelpIDString(mainFrame.getDocumentTabbedPane(), "gui.editor");
        CSH.setHelpIDString(mainFrame.getStatusBar(), "gui.statusbar");
        CSH.setHelpIDString(mainFrame.getStatusBar().getFieldRight(), "gui.statusbar_mousecoords");
        CSH.setHelpIDString(mainFrame.getStatusBar().getUpdateField(), "gui.statusbar_updatecoords");
        CSH.setHelpIDString(mainFrame.getDesktopPnl(), "gui.viewer");
        CSH.setHelpIDString(mainFrame.getToolBar(), "gui.menu");
        CSH.setHelpIDString(mainFrame.getJMenuBar(), "gui.menu");
    }

    /**
     * <p>Enable help on a component. It should register the component <code>component</code>
     * for the help topic identified by the ID represented by argument <code>s</code>.
     * This mapping is used for context help where one switches on the context help button
     * on the application toolbar and clicks onto the component to open it's help directly.</p>
     *
     * <p>Note that this implementation is void and doesn't perform anything.
     * The functionality described above is rather performed by one single method
     * {@link #setHelpIDString(javax.swing.JComponent, java.lang.String)}.
     *
     * @param component an AWT/Swing component (will not be used since the method defines no functionality).
     * @param s a help topic ID (will not be used since the method defines no functionality).
     * @param helpSet a help set instance (will not be used since the method defines no functionality).
     */
    public void enableHelpKey(Component component, String s, HelpSet helpSet) {
    }

    /**
     * <p>Enable help on a component. It should register the component <code>component</code>
     * for the help topic identified by the ID represented by argument <code>s</code>.
     * This mapping is used for context help where one switches on the context help button
     * on the application toolbar and clicks onto the component to open it's help directly.</p>
     *
     * <p>Note that this implementation is void and doesn't perform anything.
     * The functionality described above is rather performed by one single method
     * {@link #setHelpIDString(javax.swing.JComponent, java.lang.String)}.
     *
     * @param component an AWT/Swing component (will not be used since the method defines no functionality).
     * @param s a help topic ID (will not be used since the method defines no functionality).
     * @param helpSet a help set instance (will not be used since the method defines no functionality).
     */
    public void enableHelp(Component component, String s, HelpSet helpSet) {
    }

    /**
     * <p>Enable help on a menu item. It should register the menu item <code>menuItem</code>
     * for the help topic identified by the ID represented by argument <code>s</code>.
     * This mapping is used for context help where one switches on the context help button
     * on the application toolbar and clicks onto the menu item to open it's help directly.</p>
     *
     * <p>Note that this implementation is void and doesn't perform anything.
     * The functionality described above is rather performed by one single method
     * {@link #setHelpIDString(javax.swing.JComponent, java.lang.String)}.
     *
     * @param menuItem a menu item (will not be used since the method defines no functionality).
     * @param s a help topic ID (will not be used since the method defines no functionality).
     * @param helpSet a help set instance (will not be used since the method defines no functionality).
     */
    public void enableHelp(MenuItem menuItem, String s, HelpSet helpSet) {
    }

    /**
     * <p>Enable help on a component. It should register the component <code>component</code>
     * for the help topic identified by the ID represented by argument <code>s</code>.
     * This mapping is used for context help where one switches on the context help button
     * on the application toolbar and clicks onto the component to open it's help directly.</p>
     *
     * <p>Note that this implementation is void and doesn't perform anything.
     * The functionality described above is rather performed by one single method
     * {@link #setHelpIDString(javax.swing.JComponent, java.lang.String)}.
     *
     * @param component an AWT/Swing component (will not be used since the method defines no functionality).
     * @param s a help topic ID (will not be used since the method defines no functionality).
     * @param helpSet a help set instance (will not be used since the method defines no functionality).
     */
    public void enableHelpOnButton(Component component, String s, HelpSet helpSet) throws IllegalArgumentException {
    }

    /**
     * <p>Enable help on a menu item. It should register the menu item <code>menuItem</code>
     * for the help topic identified by the ID represented by argument <code>s</code>.
     * This mapping is used for context help where one switches on the context help button
     * on the application toolbar and clicks onto the menu item to open it's help directly.</p>
     *
     * <p>Note that this implementation is void and doesn't perform anything.
     * The functionality described above is rather performed by one single method
     * {@link #setHelpIDString(javax.swing.JComponent, java.lang.String)}.
     *
     * @param menuItem a menu item (will not be used since the method defines no functionality).
     * @param s a help topic ID (will not be used since the method defines no functionality).
     * @param helpSet a help set instance (will not be used since the method defines no functionality).
     */
    public void enableHelpOnButton(MenuItem menuItem, String s, HelpSet helpSet) {
    }

    /**
     * Show the help window and display the help topic identified by the <code>helpID</code>.
     *
     * @param helpID a help topic ID. To get the list
     * of help IDs see the <code>Map.jhm</code> file located
     * in the <code>com.tplan.robot.helpset</code> package.
     *
     * @param modal whether the window should be modal or not. Null argument is equal to <code>false</code>.
     */
    public void show(String helpID, Component owner, Boolean modal) {
        setCurrentID(helpID);
        setModal(modal == null ? false : modal.booleanValue());
        setDisplayed(true);
    }

    /**
     * Show the help window and display the help topic identified
     * by the <code>e</code> ActionEvent. This method is used by the contextual
     * help feature and the ActionEvent contains information about the component
     * which was clicked. This way the underlying handler can identify whether
     * there's a help ID associated with the component and display it.
     *
     * @param e an action event.
     */
    public void contextShow(ActionEvent e) {
        setModal(false);
        helpListener.actionPerformed(e);
    }

    /**
     * <p>Enable help on a Swing component. The method registers the component <code>component</code>
     * for the help topic identified by the ID specified by argument <code>helpId</code>.
     * This mapping is used for context help where one switches on the context help button
     * on the application toolbar and clicks onto the component to open it's help directly.</p>
     *
     * @param component any Swing component which is part of the application GUI (panel, button, toolbar, menu,...).
     * @param helpId a help topic ID. To get the list
     * of help IDs see the <code>Map.jhm</code> file located
     * in the <code>com.tplan.robot.helpset</code> package.
     */
    public void setHelpId(JComponent component, String helpId) {
        CSH.setHelpIDString(component, helpId);
    }

    /**
     * @return the helpSetFile
     */
    public File getHelpSetFile() {
        return helpSetFile;
    }

    public boolean isContextualHelpSupported() {
        return true;
    }

    public boolean isHelpAvailable() {
        return true;
    }

    public void init(Component owner) {
        initHelpWindow(MainFrame.getInstance());
    }

    public void initComponentHelpIds() {
        bindMainFrameComponents(MainFrame.getInstance());
    }
}
