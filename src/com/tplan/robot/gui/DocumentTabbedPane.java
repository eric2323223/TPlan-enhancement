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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.gui.editor.EditorPnl;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.interpret.TestScriptInterpretFactory;
import com.tplan.robot.util.Utils;
import java.awt.event.MouseListener;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.io.File;
import java.io.IOException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tabbed pane containing instances of script editor.
 *
 * @product.signature
 */
public class DocumentTabbedPane extends JTabbedPane
        implements ChangeListener, PropertyChangeListener, ConfigurationKeys,
        MouseListener, ActionListener {

    MainFrame mainFrame;
    boolean ignoreChangeEvents = false;
    JPopupMenu menu;
    int popUpMenuTabIndex = -1;
    private final String UNTITLED = ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.unnamedDocument");
    Map<Component, JLabel> tabRenderers = new HashMap();

    DocumentTabbedPane(MainFrame frame) {
        this.mainFrame = frame;
        this.addChangeListener(this);
//        this.addMouseListener(this);
    }

    public EditorPnl getActiveEditorPanel() {
        return (EditorPnl) getSelectedComponent();
    }

    public EditorPnl getEditorForFile(File f) throws IOException {
        return getEditorForFile(f, true, true);
    }

    public EditorPnl getEditorForDocument(Document doc) {
        EditorPnl pnl;
        for (int i = 0; doc != null && i < getTabCount(); i++) {
            pnl = (EditorPnl) getComponentAt(i);
            if (pnl.getEditor().getDocument().equals(doc)) {
                return pnl;
            }
        }
        return null;
    }

    public boolean isFileOpen(File f) {
        EditorPnl pnl;
        for (int i = 0; i < getTabCount(); i++) {
            pnl = (EditorPnl) getComponentAt(i);
            if (pnl.getFile() != null && pnl.getFile().getAbsoluteFile().equals(f.getAbsoluteFile())) {
                return true;
            }
        }
        return false;
    }

    public EditorPnl getEditorForFile(File f, boolean add, boolean select) throws IOException {
        EditorPnl pnl;
        URI uri;
        try {
            uri = f.getCanonicalFile().toURI();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        for (int i = 0; i < getTabCount(); i++) {
            pnl = (EditorPnl) getComponentAt(i);
            if (uri.equals(pnl.getTestScript().getURI())) {
                if (select) {
                    setSelectedComponent(pnl);
                    mainFrame.menuEnabler();
                }
                return pnl;
            }
        }
        // Bug 2923342 fix in 2.0.1 - go through active scripts before creating a new interpret
        TestScriptInterpret testScript = null;
        for (TestScriptInterpret ti : mainFrame.getScriptHandler().getActiveTestScripts()) {
            if (uri.equals(ti.getURI())) {
                testScript = ti;
            }
        }
        if (testScript == null) {
            testScript = TestScriptInterpretFactory.getInstance().createByExtension(uri);
            testScript.setScriptManager(mainFrame.getScriptHandler());
            testScript.setURI(uri, true);
        }

        pnl = new EditorPnl(testScript, mainFrame, mainFrame.getUserConfiguration());
        pnl.removePropertyChangeListener(this);
        pnl.addPropertyChangeListener(this);
        if (add) {
            addEditorPnl(pnl, select);
            testScript.compile(null);
        }
        return pnl;
    }

    // Bug 2923342 fix in 2.0.1 - opening of scripts linked through Run or Include
    public EditorPnl getEditorForDocument(Document doc, ScriptManager sm, boolean add, boolean select) {
        EditorPnl pnl;
        for (int i = 0; doc != null && i < getTabCount(); i++) {
            pnl = (EditorPnl) getComponentAt(i);
            if (pnl.getEditor().getDocument().equals(doc)) {
                if (select) {
                    setSelectedTab(pnl);
                }
                return pnl;
            }
        }

        for (TestScriptInterpret ti : sm.getActiveTestScripts()) {
            if (doc.equals(ti.getDocument())) {
                return getEditorForTestScript(ti, add, select);
            }
        }
        return null;
    }

    // Bug 2923342 fix in 2.0.1 - opening of scripts linked through Run or Include
    public EditorPnl getEditorForTestScript(TestScriptInterpret ti, boolean add, boolean select) {
        EditorPnl pnl = null;
        for (int i = 0; i < getTabCount(); i++) {
            pnl = (EditorPnl) getComponentAt(i);
            if (pnl.getEditor().getDocument().equals(ti.getDocument())) {
                return pnl;
            }
        }
        pnl = new EditorPnl(ti, mainFrame, mainFrame.getUserConfiguration());
        pnl.removePropertyChangeListener(this);
        pnl.addPropertyChangeListener(this);
        if (add) {
            addEditorPnl(pnl, select);
        }
        return pnl;
    }

    public void addEditorPnl(EditorPnl pnl, boolean select) {
        String text = pnl.getFile() == null
                ? ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.unnamedDocument")
                : pnl.getFile().getName();

        addTab(null, pnl);
        int index = indexOfComponent(pnl);
        setTitleAt(index, text);

        pnl.removePropertyChangeListener(this);
        pnl.addPropertyChangeListener(this);
        try {
            setToolTipTextAt(index, pnl.getFile() == null ? text : pnl.getFile().getCanonicalPath());
        } catch (IOException e) {
            setToolTipTextAt(index, pnl.getFile().getAbsolutePath());
        }
        if (select) {
            setSelectedTab(pnl);
        }
        mainFrame.menuEnabler();
    }

    public EditorPnl createEmptyEditor(int scriptType) {
        TestScriptInterpret testScript = TestScriptInterpretFactory.getInstance().createByType(scriptType);
        testScript.setScriptManager(mainFrame.getScriptHandler());
        EditorPnl pnl = new EditorPnl(testScript, mainFrame, mainFrame.getUserConfiguration());
        addTab(null, pnl);
        setTitleAt(indexOfComponent(pnl), ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.unnamedDocument"));
        setSelectedComponent(pnl);
        mainFrame.menuEnabler();
        return pnl;
    }

//    public void addTab(String title, Component component) {
//        super.addTab(title, component);
//        setTabComponentAt(getTabCount() - 1, new JLabel(title + "blala"));
//        System.out.println("tabComponentAt(" + (getTabCount() - 1) + ")=" + getTabComponentAt(getTabCount() - 1));
//    }
    public void closeEditor(EditorPnl pnl) {
        if (pnl != null) {
            pnl.close();
            remove(pnl);
            tabRenderers.remove(pnl);
            pnl.getTestScript().destroy();
            mainFrame.menuEnabler();
        }
    }

    public void closeActiveEditor() {
        closeEditor((EditorPnl) getSelectedComponent());
    }

    public void stateChanged(ChangeEvent e) {
        if (mainFrame.rec != null && getActiveEditorPanel() != null) {
            mainFrame.rec.setEditorPnl(getActiveEditorPanel());
        }
        if (!ignoreChangeEvents) {
            mainFrame.enableFollowExecTrace(false);
            mainFrame.menuEnabler();

            EditorPnl pnl = getActiveEditorPanel();
            if (pnl != null) {
                TestScriptInterpret testScript = pnl.getTestScript();
                if (testScript.getType() == TestScriptInterpret.TYPE_PROPRIETARY) {
                    testScript.compile(null);
                } else if (testScript.getType() == TestScriptInterpret.TYPE_JAVA) {
                    // Do not compile Java code the editor is activated, it is too time consuming
                }
            }
        }
    }

    public void setSelectedTab(Component c) {
        ignoreChangeEvents = true;
        if (c != null) {
            setSelectedComponent(c);
        }
        ignoreChangeEvents = false;
    }

    public int getEditorIndex(EditorPnl editor) {
        for (int i = 0; i < getTabCount(); i++) {
            if (getComponentAt(i).equals(editor)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * This method returns true if just default empty editor is opened.
     * It is used to decide whether to open an existing file in a new editor or in the default one.
     * @return true if only default empty 'Untitled' editor is opened, false otherwise.
     */
    private boolean isJustDefaultEditorOpened() {
        return getTabCount() == 1 && !getActiveEditorPanel().isDocumentChanged() && getActiveEditorPanel().getFile() == null;
    }

    /**
     * Save the list of opened files and name of the currently selected tab to user configuration.
     * These values will be used after app restart to reopen the files.
     * @see #reopenListOfFiles
     */
    public void saveListOfOpenedFiles() {
        List v = new ArrayList();
        EditorPnl pnl;
        for (int i = 0; i < getTabCount(); i++) {
            pnl = (EditorPnl) getComponentAt(i);
            if (pnl.getFile() != null) {
                v.add(Utils.getFullPath(pnl.getFile()));
            }
        }
        mainFrame.getUserConfiguration().setListOfObjects(IO_OPEN_FILE_LIST, v);
        if (getTabCount() > 0 && getActiveEditorPanel() != null && getActiveEditorPanel().getFile() != null) {
            mainFrame.getUserConfiguration().setString(IO_ACTIVE_FILE, Utils.getFullPath(getActiveEditorPanel().getFile()));
        }
    }

    /**
     * Reopen editors based on the list of opened files in user configuration. This list is being saved when
     * the application exits. When it is restarted it is supposed to reopen the files and select the default one
     * exactly as it was when user finished last time.
     *
     * <p>If the list is empty, this tabbed pane opens one default untitled document in the default editor.
     */
    public List<IOException> reopenListOfFiles() {
        List v = mainFrame.getUserConfiguration().getListOfStrings(IO_OPEN_FILE_LIST);
        List<IOException> l = new ArrayList();
        if (v != null && v.size() > 0) {
            File file;
            for (int i = 0; i < v.size(); i++) {
                file = new File((String) v.get(i));
                try {
                    getEditorForFile(file);
                } catch (IOException ex) {
                    l.add(ex);
                }
            }
            String f = mainFrame.getUserConfiguration().getString(IO_ACTIVE_FILE);
            if (f != null && !f.trim().equals("")) {
                try {
                    setSelectedComponent(getEditorForFile(new File(f)));
                } catch (IOException ex) {
                }
            }
            if (l.size() > 0) {
                return l;
            }
        }
        return null;
    }

    public boolean isAnyDocumentModified() {
        for (int i = 0; i < getTabCount(); i++) {
            if (((EditorPnl) getComponentAt(i)).isDocumentChanged()) {
                return true;
            }
        }
        return false;
    }

    public void propertyChange(PropertyChangeEvent evt) {
    }

    @Override
    public void setTitleAt(int i, String text) {
        JLabel l = getRenderer(i);
        l.setText(text);
    }

    @Override
    public String getTitleAt(int i) {
        JLabel l = getRenderer(i);
        String title = l != null ? l.getText() : super.getTitleAt(i);
        if (title != null && title.endsWith("*")) {
            title = title.substring(0, title.length() - 1);
        }
        return title;
    }

    // Create & save a custom JLabel tab renderer
    private JLabel getRenderer(int i) {
        Component c = getComponentAt(i);
        JLabel l = tabRenderers.get(c);
        if (l == null) {
            l = new JLabel();
            setTabComponentAt(i, l);
            l.addMouseListener(this);
            tabRenderers.put(c, l);
        }
        return l;
    }

    @Override
    public void setToolTipTextAt(int i, String text) {
        getRenderer(i).setToolTipText(text);
    }

    public void updateTabTitles() {
        EditorPnl pnl;
//        System.out.println("updateTabTitles()");

        final String s = ApplicationSupport.getString("com.tplan.robot.gui.tabbedpane.modifiedFile");
        String name, tooltip;

        for (int i = 0; i < getTabCount(); i++) {
            pnl = (EditorPnl) getComponentAt(i);
            if (pnl.getFile() != null) {
                name = pnl.getFile().getName();
                tooltip = pnl.getFile().getAbsolutePath();
            } else {
                name = UNTITLED;
                tooltip = UNTITLED;
            }
            if (pnl.isDocumentChanged()) {
                name += "*";
                tooltip = MessageFormat.format(s, tooltip);
            }

            List l = pnl.getEditor().getErrors();
            JLabel lbl = getRenderer(i);
            if (l != null && l.size() > 0) { // there are some errors
                lbl.setForeground(Color.RED);
            } else {
                lbl.setForeground(Color.BLACK);
            }
            lbl.setText(name);
            lbl.setToolTipText(tooltip);
        }
    }

    // THE FOLLOWING CODE IMPLEMENTS THE POPUP MENU CAPABLE OF CLOSING FILES
    // Extension added on Oct 13, 2007
    private void initMenu() {
        menu = new JPopupMenu();
        createPopupMenuItem("close", menu);
        createPopupMenuItem("closeother", menu);
        createPopupMenuItem("closeall", menu);
        menu.addSeparator();
        createPopupMenuItem("save", menu);
        createPopupMenuItem("saveas", menu);
        createPopupMenuItem("saveall", menu);
        menu.addSeparator();
        createPopupMenuItem("compile", menu);
        createPopupMenuItem("export", menu);
    }

    private JMenuItem createPopupMenuItem(String actionKey, JPopupMenu menu) {
        Object o = mainFrame.getActionManager().getMenuItem(actionKey);
        if (o instanceof JMenuItem) {
            JMenuItem globalMenuItem = (JMenuItem) o;
            JMenuItem popupMenuItem = new JMenuItem(globalMenuItem.getText(), globalMenuItem.getIcon());
            popupMenuItem.setActionCommand(actionKey);
            popupMenuItem.addActionListener(this);
            menu.add(popupMenuItem);
            return popupMenuItem;
        }
        return null;
    }

    private void enablePopupMenuItems() {
        Object o;
        JMenuItem item;
        String actionKey;

        for (int i = 0; i < menu.getComponentCount(); i++) {
            o = menu.getComponent(i);
            if (o instanceof JMenuItem) {
                item = (JMenuItem) o;
                actionKey = item.getActionCommand();
                o = mainFrame.getActionManager().getMenuItem(actionKey);
                if (o instanceof JComponent) {
                    item.setEnabled(((JComponent) o).isEnabled());
                } else if (o instanceof Component) {
                    item.setEnabled(((Component) o).isEnabled());
                } else {
                    item.setEnabled(false);
                }
            }
        }
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
        Object o = mainFrame.getActionManager().getMenuItem(e.getActionCommand());
        if (o instanceof JMenuItem) {
            JMenuItem globalMenuItem = (JMenuItem) o;
            if (globalMenuItem.isEnabled()) {
                globalMenuItem.doClick();
            }
        }
    }

    public int indexAtLocation(int x, int y) {
        JLabel l;
        System.out.println("x,y=" + x + "," + y);
        for (Component c : tabRenderers.keySet()) {
            l = tabRenderers.get(c);
            System.out.println("l=" + l.getText() + ", bounds=" + l.getBounds());
            if (l != null && l.getBounds().contains(x, y)) {
                System.out.println("Contains, index=" + indexOfTab(l.getText()));
//                int i = indexOfTabComponent(c);
//                if (i >= 0) {
//                    return i;
//                }
            }
        }
        return super.indexAtLocation(x, y);
    }

    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     */
    public void mouseClicked(MouseEvent e) {
        // React just on left click
        if (e.getButton() == MouseEvent.BUTTON3) {
            Component invoker = this;

            if (tabRenderers.values().contains(e.getSource())) {
                invoker = (JLabel) e.getSource();
                popUpMenuTabIndex = indexOfTabComponent(invoker);
                setSelectedIndex(popUpMenuTabIndex);
            } else {
                popUpMenuTabIndex = indexAtLocation(e.getX(), e.getY());
            }
            // Get the index of the tab at the click location
            if (popUpMenuTabIndex >= 0 && popUpMenuTabIndex < getTabCount()) {

                // If the menu hasn't been created, create it
                if (menu == null) {
                    initMenu();
                }

                // Enable or disable menu items based on what their state in the
                // main menu is
                enablePopupMenuItems();
                menu.show(invoker, e.getX(), e.getY());
            }
        } else {
            for (Component c : tabRenderers.keySet()) {
                if (e.getSource().equals(tabRenderers.get(c))) {
                    setSelectedComponent(c);
                    break;
                }
            }
        }

    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Invoked when the mouse enters a component.
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Invoked when the mouse exits a component.
     */
    public void mouseExited(MouseEvent e) {
    }
}
