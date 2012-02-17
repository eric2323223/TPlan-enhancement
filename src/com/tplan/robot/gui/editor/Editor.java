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
package com.tplan.robot.gui.editor;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.scripting.commands.CommandEvent;
import com.tplan.robot.scripting.commands.CommandHandler;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.preferences.ConfigurationChangeEvent;
import com.tplan.robot.preferences.ConfigurationChangeListener;
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.*;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.preferences.Configurable;
import com.tplan.robot.scripting.PauseRequestException;
import com.tplan.robot.scripting.ScriptListener;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.commands.AdvancedCommandHandler;
import com.tplan.robot.scripting.commands.CommandEditAction;
import com.tplan.robot.scripting.commands.CommandListener;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import com.tplan.robot.scripting.wrappers.NestedBlockInterpret;
import com.tplan.robot.scripting.wrappers.NestedBlockInterpretFactory;
import com.tplan.robot.util.DocumentUtils;
import com.tplan.robot.util.Utils;
import java.awt.datatransfer.Transferable;
import java.text.MessageFormat;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Test script editor providing {@link CustomStyledDocument syntax higlighting} and validation, breakpoints,
 * tracking of executed element and {@link CommandWizard Command} and {@link SnippetWizard Snippet} wizards.
 * @product.signature
 */
public class Editor extends JTextPane
        implements ActionListener, ConfigurationKeys, ConfigurationChangeListener,
        PropertyChangeListener, MouseInputListener, GUIConstants, CaretListener, ScriptListener,
        CommandListener, ClipboardOwner {

    private final List<Element> execdElements = new ArrayList();
    private Color execLineColor;
    private Color breakPointColor;
    private Color syntaxErrorColor;
    Map breakPointTable = new HashMap();
    private boolean pauseScript = false;
    private EditorGutter sidePanel;
    private EditorPnl editorPnl;
    private JPopupMenu popUpMenu = new JPopupMenu();
    private Map popupActions = new HashMap();
    private Highlighter highlighter;
    private List errors;
    private static CommandWizard cl;// = new CommandWizard();
    private Action preferencesAction = new ConfigureEditorAction();
    private Action commandWizardAction = new CmdListAction();
    private Action snippetAction = new SnippetAction();
    private Action compileAction; // Initialized in constructor
    private Action convertAction; // Initialized in constructor
    private Element popUpMenuElement;
    UserConfiguration cfg;
    private static int indent = 2;
    private String typedInMenu = null;
    private String temp = null;
    private List<Element[]> nestedRanges = new ArrayList();
    public static final String ACTION_VALUE_CMDTEXT = "CMDTEXT";

    /**
     * Constructor.
     * @param pnl editor panel which will contain and manage this editor.
     * @param cfg user configuration instance.
     */
    public Editor(EditorPnl pnl, UserConfiguration cfg) {
        this.cfg = cfg;
        this.editorPnl = pnl;
        compileAction = new CompileAction(pnl);
        convertAction = new ConvertAction(pnl);
        cfg.addConfigurationListener(this);
        reloadConfiguration();
        addMouseListener(this);
        addMouseMotionListener(this);
        addCaretListener(this);

        if (!isConsoleMode()) {
            setOpaque(false);
            highlighter = new DefaultHighlighter();
            this.setHighlighter(highlighter);

            if (cl == null) {
                cl = new CommandWizard(this);
            }
            cl.addPropertyChangeListener(this);

            // Register the Command List feature
            CmdListAction cla = new CmdListAction();
            getActionMap().put("command-list", cla);
            String key = cfg.getString("ui.editor.commandListShortCut");
            getInputMap().put(Utils.getKeyStroke(key), "command-list");

            // Register the Command List feature
            SnippetAction sa = new SnippetAction();
            getActionMap().put("snippet", sa);
            key = cfg.getString("ui.editor.snippetShortCut");
            getInputMap().put(Utils.getKeyStroke(key), "snippet");

            // Register the Command List feature
            ContextMenuAction cma = new ContextMenuAction();
            getActionMap().put("context-menu", cma);
            KeyStroke ks = Utils.getKeyStroke(cfg.getString("ui.editor.contextMenuShortCut"));
            getInputMap().put(ks, "context-menu");

            registerCommandShortcuts();
        }
    }

    private boolean isConsoleMode() {
        return editorPnl == null;
    }

    /**
     * Create and bind command shortcut actions. A shortcut action is created for every command which implements
     * method getContextShortcut() to return a KeyStroke. This key is then registered by editor and can be used to type
     * the command by its shortcut.
     * <p/>
     * For example, method getContextShortcut() of the Press command returns the key corresponding to Ctrl+Shift+P.
     * If you press this key in the editor, "Press " string gets generated in the edited line.
     */
    private void registerCommandShortcuts() {
        Map t = editorPnl.pnlMain.getScriptHandler().getCommandHandlers();
        Iterator e = t.keySet().iterator();
        CommandHandler c;
        String name;
        KeyStroke k;

        while (e.hasNext()) {
            name = (String) e.next();
            c = (CommandHandler) t.get(name);
            k = c.getContextShortcut();
            if (k != null) {
                name = name.substring(0, 1).toUpperCase() + name.substring(1, name.length()).toLowerCase();
                CmdFromShortcutAction action = new CmdFromShortcutAction(name);
                getActionMap().put("command-" + name, action);
                getInputMap().put(k, "command-" + name);
            }
        }
    }

    public void addPopUpMenuAction(Action action, Element element) {
        List<Action> v = (List) popupActions.get(element);
        if (v == null) {
            v = new ArrayList();
            popupActions.put(element, v);
        } else {
            // Fix in 2.3/2.0.6 - prevent duplicated items.
            // WaitFor triggered from Java code is known to fail to identify
            // the elements and remove them properly.
            Object name = action.getValue(Action.NAME);
            if (name != null) {
                for (Action a : v) {
                    if (name.equals(a.getValue(Action.NAME))) {
                        v.remove(a);
                    }
                }
            }
        }
        v.add(action);
    }

    public void removePopUpMenuAction(Action action, Element element) {
        List v = (List) popupActions.get(element);
        if (v != null && v.contains(action)) {
            v.remove(action);
        }
    }

    private JPopupMenu createPopUpMenuForElement(Element element) {
        String text = DocumentUtils.getElementText(element);
        popUpMenu.removeAll();
        List v = (List) popupActions.get(element);
        Action a;
        for (int i = 0; v != null && i < v.size(); i++) {
            a = (Action) v.get(i);
            a.putValue(ACTION_VALUE_CMDTEXT, text);
            popUpMenu.add(new JMenuItem(a));
        }

        boolean hasCustomItems = v != null && v.size() > 0;

        int cnt = 0;

        // If the test script is in proprietary language, check if the command
        // handler provides any custom menu actions and add them
        final boolean isProprietary = editorPnl.getTestScript().getType() == TestScriptInterpret.TYPE_PROPRIETARY;
        if (isProprietary) {
            CommandHandler ch = ((ProprietaryTestScriptInterpret) editorPnl.getTestScript()).getCommandHandlerForElement(element);
            if (ch != null) {
                v = ch.getStablePopupMenuItems();
                if (v != null && v.size() > 0) {
                    if (hasCustomItems) {
                        popUpMenu.addSeparator();
                        cnt = 0;
                    }
                    for (int i = 0; i < v.size(); i++) {
                        a = (Action) v.get(i);
                        popUpMenu.add(a);
                        cnt++;
                        if (a instanceof CommandEditAction) {
                            ((CommandEditAction) a).setElement(element);
                        }
                    }
                }
                if (cnt > 0) {
                    popUpMenu.addSeparator();
                    cnt = 0;
                }
                if (MainFrame.getHelpBroker() != null) {
                    popUpMenu.add(new JMenuItem(new CommandHelpAction(ch.getCommandNames()[0])));
                    cnt++;
                }
            } else {
                text = text.trim().toLowerCase();
                int space = text.indexOf(" ");
                if (space > 0) {
                    String word = text.substring(0, space);
                    NestedBlockInterpret ni = NestedBlockInterpretFactory.getInstance().getInterpret(word);
                    if (ni != null) {
                        if (ni instanceof Plugin) {
                            if (ni instanceof Configurable) {
                                popUpMenu.add(new JMenuItem(new ConfigureAction(word, ((Plugin) ni).getDisplayName())));
                                cnt++;
                            }
                            popUpMenu.add(new JMenuItem(new CommandHelpAction(word)));
                            cnt++;
                        }
                    }
                }
            }
        }

        // Finally add stable menu items like Editor Preferences etc.
        if (cnt > 0) {
            popUpMenu.addSeparator();
            cnt = 0;
        }

        // Add the Wizard and Snippet items only if on an empty line
        text = DocumentUtils.getElementText(element);
        if (text != null && text.length() == 0) {
            popUpMenu.add(new JMenuItem(commandWizardAction));
            popUpMenu.add(new JMenuItem(snippetAction));
        }
        popUpMenu.add(new JMenuItem(compileAction));
        popUpMenu.add(new JMenuItem(convertAction));
        popUpMenu.addSeparator();
        popUpMenu.add(new JMenuItem(preferencesAction));
        if (System.getProperty("editor.html") != null) {
            popUpMenu.add(new JMenuItem(new ToHtmlAction()));
            popUpMenu.add(new JMenuItem(new ToBBCodeAction()));
        }
        return popUpMenu;
    }

    /**
     * Reload objects that are subject to user configuration, i.e. break point color,
     * color of the executed line indicator etc. It is usually executed once when an instance
     * gets created and then when any such a value gets changed by user.
     */
    private void reloadConfiguration() {
        execLineColor = cfg.getColor(EDITOR_EXECUTED_LINE_COLOR);
        execLineColor = execLineColor == null ? Color.YELLOW : execLineColor;

        breakPointColor = cfg.getColor(EDITOR_BREAKPOINT_COLOR);
        breakPointColor = breakPointColor == null ? Color.RED : breakPointColor;

        syntaxErrorColor = cfg.getColor(EDITOR_SYNTAX_ERROR_COLOR);
        syntaxErrorColor = syntaxErrorColor == null ? Color.RED : syntaxErrorColor;
    }

    public EditorPnl getEditorPnl() {
        return editorPnl;
    }

    public void validate(List errors) {
        this.errors = errors;
        if (!isConsoleMode()) {
            SyntaxErrorException e;
            Highlighter.Highlight[] hl = getHighlighter().getHighlights();
            int length = hl.length - 1;
            for (int i = length; i >= 0; i--) {
                Highlighter.Highlight highlight = hl[i];
                if (highlight.getPainter() instanceof ErrorHighlightPainter) {
                    getHighlighter().removeHighlight(highlight);
                }

            }

            for (int i = 0; i < errors.size(); i++) {
                if (errors.get(i) instanceof SyntaxErrorException) {
                    e = (SyntaxErrorException) errors.get(i);
                    String thisFile = this.getEditorPnl().getFile() == null ? "" : Utils.getFullPath(this.getEditorPnl().getFile());
                    if ((e.getScriptFile() != null && Utils.getFullPath(e.getScriptFile()).equals(thisFile)) || (e.getElement() != null && e.getElement().getDocument().equals(this.getDocument()))) {
                        if (e.getElement() != null) {
                            try {
                                getHighlighter().addHighlight(e.getElement().getStartOffset(), e.getElement().getEndOffset(),
                                        new ErrorHighlightPainter(syntaxErrorColor));
                            } catch (BadLocationException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        MainFrame.getInstance().getDocumentTabbedPane().updateTabTitles();
    }

    /**
     * Overriden method to ensure painting of executed line indicator and breakpoints.
     *
     * @param g graphic context.
     */
    public void paint(Graphics g) {

        try {
            if (!isConsoleMode()) {
                synchronized (execdElements) {
                    // This piece of code will paint currently executed line in given color
                    if (execdElements != null) {
                        Element elem;
                        for (int i = 0; i < execdElements.size(); i++) {
                            if (execdElements.get(i) instanceof Element) {
                                elem = (Element) execdElements.get(i);

                                try {
                                    Rectangle r = getRectangleForLine(DocumentUtils.getLineForOffset(getStyledDocument(), elem.getStartOffset()));
                                    if (r != null) {
                                        g.setColor(execLineColor);
                                        g.fillRect(r.x, r.y, this.getWidth(), r.height);
                                    }
                                } catch (BadLocationException ex) {
                                }
                            }
                        }
                    }
                    // The following will paint breakpoints.
                    Rectangle r;

                    Iterator en = breakPointTable.keySet().iterator();
                    Element elem;
                    while (en.hasNext()) {
                        elem = (Element) en.next();
                        if (execdElements == null || !execdElements.contains(elem)) {
                            try {
                                r = getRectangleForLine(DocumentUtils.getLineForOffset(getStyledDocument(), elem.getStartOffset()));
                                if (r != null) {
                                    g.setColor(breakPointColor);
                                    g.fillRect(r.x, r.y, this.getWidth(), r.height);
                                }
                            } catch (BadLocationException e) {
                                continue; // Don't paint incorrect breakpoints
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Catch all exceptions not to make the tool stuck
        }
        try {
            super.paint(g);
        } catch (Exception ex) {
        }
    }

    /**
     * Implementation of the ActionListener interface. This component is
     * interested in the execute module events so that it can show
     * the actually executed command in the script.
     *
     * @param e an ActionEvent
     */
    public void actionPerformed(ActionEvent e) {

        if ("breakPointRemoved".equals(e.getActionCommand())
                || "breakPointAdded".equals(e.getActionCommand())) {
            // Optimized - repaint just the area that has changed
            Rectangle r = null;
            try {
                r = getRectangleForLine(e.getModifiers());
                r.width = this.getWidth();
                repaint(r);
            } catch (BadLocationException e1) {
                repaint();
            }
        } else if ("breakPointsCleared".equals(e.getActionCommand())) {
            repaint();
        }
    }

    public void scrollLineRectToVisible(Rectangle rect) {
        if (!isConsoleMode()) {
            Rectangle visible = getVisibleRect();

            int maxRectY = rect.y + rect.height;
            int maxVisibleY = visible.y + visible.height;

            // If the text line is visible, don't do anything
            if (rect.y > visible.y && (maxRectY) < (maxVisibleY)) {
                return;
            }

            if (getGutter() != null) {
                Rectangle r = new Rectangle(rect);

                // The rectangle is below the currently visible one
                if (maxRectY > maxVisibleY) {
                    r.y = Math.min(r.y - r.height + visible.height, this.getY() + this.getHeight());
                }

                // Also allow a few points reserve from the top.
                r.y -= 2;
                r.x = 0;
                getGutter().scrollRectToVisible(r);
            } else {
                scrollRectToVisible(rect);
            }
        }
    }

    public void scrollElementToVisible(Element e) {
        if (e == null || !e.getDocument().equals(this.getDocument())) {
            return;
        }
        try {
            Rectangle r = getUI().modelToView(this, e.getStartOffset());
            scrollRectToVisible(r);
        } catch (Exception e1) {
            // This can happen because the document has been modified
        }

    }

    public int getLineCount() {
        StyledDocument doc = (StyledDocument) getDocument();
        return doc.getDefaultRootElement().getElementCount();
    }

    public Rectangle getRectangleForLine(int line) throws BadLocationException {
        int offset = DocumentUtils.getOffsetForLine(getStyledDocument(), line);
        return offset < 0 || offset > getDocument().getLength() ? null : getUI().modelToView(this, offset);
    }

//    public int getDisplayedLine() {
//        return 0;
//    }
    public Element getCurrentElement() {
        return DocumentUtils.getElementForOffset(getStyledDocument(), getCaretPosition());
    }

    int getLineForPoint(Point point) {
        // Get the offset belonging to the point
        int offset = getUI().viewToModel(this, point);
        return DocumentUtils.getLineForOffset(getStyledDocument(), offset);
    }

    boolean canAddBreakpoint(Point p) {
        try {
            Rectangle rect = getUI().modelToView(this, getDocument().getLength());
            return rect != null && rect.getY() + rect.getHeight() > p.y;
        } catch (BadLocationException e) {
        }
        return false;
    }

    /**
     * Inmplementation of the ConfigurationChangeListener which is called
     * whenever a value in configuration gets changed. We need to find out
     * if it affects this class and aplly it.
     *
     * @param evt a ConfigurationChangeEvent instance identifying the value that has changed.
     */
    public void configurationChanged(ConfigurationChangeEvent evt) {
        if (evt.getPropertyName().equals(EDITOR_EXECUTED_LINE_COLOR)
                || evt.getPropertyName().equals(EDITOR_BREAKPOINT_COLOR)
                || evt.getPropertyName().equals(EDITOR_SYNTAX_ERROR_COLOR)) {
            reloadConfiguration();
            repaint();
        }
    }

    public EditorGutter getGutter() {
        return sidePanel;
    }

    void setGutter(EditorGutter sidePanel) {
        this.sidePanel = sidePanel;
    }

    public void scriptEvent(ScriptEvent event) {
        int type = event.getType();
        switch (type) {
            case ScriptEvent.SCRIPT_EXECUTION_STARTED:
                pauseScript = false;
                break;
            case ScriptEvent.SCRIPT_EXECUTION_FINISHED:
                synchronized (execdElements) {
                    execdElements.clear();
                }
                pauseScript = false;
                repaint();
                break;
            case ScriptEvent.SCRIPT_EXECUTION_PAUSED:
            case ScriptEvent.SCRIPT_EXECUTION_RESUMED:
                pauseScript = false; //!((Boolean)evt.getNewValue()).booleanValue();
                break;
            case ScriptEvent.SCRIPT_INTERPRET_DESTROYED:
                return;
            case ScriptEvent.SCRIPT_PROCEDURE_FINISHED:
                // Handler of this event type is duplicated in this method because
                // if the called procedure is in a library (== in another file),
                // we need both editors to process it and remove the execution mark.
                synchronized (execdElements) {
                    Element el = (Element) event.getCustomObject();
                    if (el != null && getDocument().equals(el.getDocument()) && execdElements.contains(el)) {
                        execdElements.remove(el);
                        return;
                    }
                }
        }

        Element el = (Element) event.getContext().get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT);
        boolean isJava = event.getInterpret() != null && event.getInterpret().getType() == TestScriptInterpret.TYPE_JAVA;

        // Filter out events which are not associated with this editor
        if (isJava) {
            if (!getDocument().equals(event.getInterpret().getDocument()) && (el == null || !el.getDocument().equals(getDocument()))) {
                return;
            }
        } else if (el == null || !getDocument().equals(el.getDocument())) {
            return;
        }

        boolean debug = System.getProperty("debug.editor") != null;

        switch (event.getType()) {
            case ScriptEvent.SCRIPT_PROCEDURE_STARTED:
                synchronized (execdElements) {
                    // Bug 2923339 fix: changed "set" to "add"
                    el = (Element) event.getCustomObject();
//                    if (!execdElements.contains(el)) {
                    execdElements.add(0, el);
//                    }
                    if (debug) {
                        System.out.println("\nEditor of " + getEditorPnl().getTestScript().getURI() + ": SCRIPT_PROCEDURE_STARTED: added '" + DocumentUtils.getElementText(el) + "'\n  " + execdElements);
                    }
                }
                repaint();
                break;
            case ScriptEvent.SCRIPT_PROCEDURE_FINISHED:
                synchronized (execdElements) {
                    if (debug) {
                        System.out.println("\nEditor of " + getEditorPnl().getTestScript().getURI() + ": SCRIPT_PROCEDURE_FINISHED: " + execdElements);
                    }
                    if (execdElements.size() > 0) { // && evt.getNewValue() != null && !evt.getNewValue().toString().equals("")) {
                        el = execdElements.remove(0);
//                        if (debug) {
//                            System.out.println("\nEditor of " + getEditorPnl().getTestScript().getURI() + ": SCRIPT_PROCEDURE_FINISHED: removed '" + DocumentUtils.getElementText(el) + "'\n  " + execdElements);
//                        }
                    }
                }
                repaint();
                break;
            case ScriptEvent.SCRIPT_EXECUTED_LINE_CHANGED:
                synchronized (execdElements) {
                    if (execdElements.size() > 0) {
                        Element eel = execdElements.remove(0);
                        if (debug) {
                            System.out.println("\nEditor of " + getEditorPnl().getTestScript().getURI() + ": SCRIPT_EXECUTED_LINE_CHANGED: removed '" + DocumentUtils.getElementText(eel) + "'\n  " + execdElements);
                        }
                    }
                    execdElements.add(0, el);
                    if (debug) {
                        System.out.println("  added '" + DocumentUtils.getElementText(el) + "'\n  " + execdElements);
                    }
                }
                MainFrame fr = editorPnl == null ? null : editorPnl.pnlMain;
                if (fr != null && fr.isFollowExecTrace()) {
                    scrollElementToVisible(el);
                }
                repaint();
                break;
            case ScriptEvent.SCRIPT_COMPILATION_STARTED:
                nestedRanges.clear();
                break;
            case ScriptEvent.SCRIPT_NESTED_INTERPRET_CREATED:
                if (getDocument() instanceof CustomStyledDocument) {
                    Element ee[] = new Element[]{el, ((NestedBlockInterpret) event.getSource()).getEndElement()};
                    nestedRanges.add(ee);
                }
                break;
            case ScriptEvent.SCRIPT_COMPILATION_FINISHED:
                if (getDocument() instanceof CustomStyledDocument) {
                    ((CustomStyledDocument) getDocument()).addUnformattedElementRanges(nestedRanges);
                }

                validate(event.getContext().getCompilationErrors());
                // This CaretEvent is sent to make the status bar to redisplay the error message.
                // TODO: reimplement to a cleaner solution
                fireCaretUpdate(new CaretEvent(this) {

                    @Override
                    public int getDot() {
                        return getCaret().getDot();
                    }

                    @Override
                    public int getMark() {
                        return getCaret().getMark();
                    }
                });
                repaint();
                break;

            case ScriptEvent.SCRIPT_GOING_TO_RUN_LINE:
                Element elem = (Element) event.getContext().get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT);
                if (breakPointTable.containsKey(elem) || pauseScript) {
                    // We are on a breakpoint => fire a PropertyVetoException
                    pauseScript = false;
                    throw new PauseRequestException(this, ApplicationSupport.getString("editor.pauseReasonBreakpoint"));
                }
                break;
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {

        // As the old value we may expect an editor instance or the document element.
        // If the editor is not equal to this OR the element does not belong to this editor's document,
        // do not process the event.
        if (evt.getOldValue() instanceof Editor && !this.equals(evt.getOldValue())) {
            return;
        } else if (evt.getOldValue() instanceof Element) {
            if (!getDocument().equals(((Element) evt.getOldValue()).getDocument())) {
                return;
            }
        }

        if ("commandSelected".equals(evt.getPropertyName())) {
            if (this.equals(evt.getOldValue())) {
                autoInsertCommand((String) evt.getNewValue());
            }
        } else if ("paramSelected".equals(evt.getPropertyName())) {
            if (this.equals(evt.getOldValue())) {
                autoInsertParameter((String) evt.getNewValue(), false);
            }
        } else if ("elementSelected".equals(evt.getPropertyName())) {
            if (this.equals(evt.getOldValue())) {
                autoInsertParameter((String) evt.getNewValue(), true);
            }
        } else if ("snippetSelected".equals(evt.getPropertyName())) {
            if (this.equals(evt.getOldValue())) {
                autoInsertSnippet((Snippet) evt.getNewValue());
            }
        } else if ("characterTyped".equals(evt.getPropertyName())) {
            if (evt.getOldValue() instanceof FilteredPopupMenu) {
                JPopupMenu m = (JPopupMenu) evt.getOldValue();
                if (this.equals(m.getInvoker())) {
                    // This event comes from the command list menu.
                    MenuKeyEvent e = (MenuKeyEvent) evt.getNewValue();
                    String c = Character.toString(e.getKeyChar());
                    try {
                        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                            if (typedInMenu != null && typedInMenu.length() > 0) {
                                typedInMenu = typedInMenu.substring(0, typedInMenu.length() - 1);
//                                System.out.println("backspace, text: "+typedInMenu);
                            }
                            getDocument().remove(getCaretPosition() - 1, 1);
                        } else {
                            typedInMenu = typedInMenu == null ? c : typedInMenu + c;
                            getDocument().insertString(getCaretPosition(), c, null);
                        }
//                        System.out.println("typed: '"+typedInMenu+"', caret is at "+getCaretPosition());
                        displayFilteredMenu((FilteredPopupMenu) m);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public void commandEvent(CommandEvent e) {
        String code = e.getActionCode();
        if (EVENT_ADD_CUSTOM_ACTION_MSG.equals(code)) {
            if (e.getCustomObject() instanceof Action) {
                addPopUpMenuAction((Action) e.getCustomObject(), (Element) e.getContext().get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT));
            }
        } else if (EVENT_REMOVE_CUSTOM_ACTION_MSG.equals(code)) {
            if (e.getCustomObject() instanceof Action) {
                removePopUpMenuAction((Action) e.getCustomObject(), (Element) e.getContext().get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT));
            }
        }
    }

    private void displayFilteredMenu(FilteredPopupMenu filteredPopupMenu) {
        try {
            Rectangle r = getUI().modelToView(this, getCaretPosition());
            Point p = new Point(r.x + r.width, r.y + r.height);
            Element e = DocumentUtils.getElementForOffset(getStyledDocument(), getCaretPosition());
            filteredPopupMenu.display(this, e, p);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    protected void autoInsertSnippet(Snippet snippet) {
        try {
            Element e = DocumentUtils.getElementForOffset(getStyledDocument(), getCaretPosition());

            // Preserve the spaces at the beginning of the line up to the cursor position
            String text = DocumentUtils.getElementText(e);
            int spaceCount = 0;
            if (text != null) {
                while (spaceCount < text.length() && (e.getStartOffset() + spaceCount < getCaretPosition()) && Character.isWhitespace(text.charAt(spaceCount))) {
                    spaceCount++;
                }
            }
            if (spaceCount > 0) {
                this.indent = spaceCount;
            }

            int insertPos;

            if (snippet.isReplace()) {
                insertPos = e.getStartOffset();

                // Remove the typed text if necessary
                int removeLenth = e.getEndOffset() - insertPos - 1;
                if (removeLenth > 0) {
                    getDocument().remove(insertPos, removeLenth);
                }
            } else {
//                        System.out.println("typed: '"+typedInMenu+"', caret is at "+getCaretPosition());
                // Remove the typed text if necessary
                if (typedInMenu != null && typedInMenu.length() > 0) {
                    insertPos = getCaretPosition() - typedInMenu.length();
                    getDocument().remove(insertPos, typedInMenu.length());
                    insertPos = getCaretPosition();
                    typedInMenu = null;
                }
                insertPos = getCaretPosition();
            }

            String s = addIndent(snippet.getCode(), spaceCount);
            int caretPos = snippet.getCaretPosition(s);
            if (caretPos >= 0) {
                s = s.substring(0, caretPos) + s.substring(caretPos + 1, s.length());
            }
            setFocusable(true);
            requestFocus();
            getDocument().insertString(insertPos, s, null);

            if (caretPos > 0) {
                setCaretPosition(insertPos + caretPos);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private String addIndent(String s, int spaceCount) {
        String tokens[] = s.split("\\n");
        String spaces = "";
        for (int i = 0; i < spaceCount; i++) {
            spaces += " ";
        }
        s = "";
        for (int i = 0; i < tokens.length; i++) {
            s += spaces + tokens[i] + (i == tokens.length - 1 ? "" : "\n");
        }

        int pos;
        spaces = "";
        for (int i = 0; i < this.indent; i++) {
            spaces += " ";
        }

        while ((pos = s.indexOf('^')) >= 0) {
            s = s.substring(0, pos) + spaces + s.substring(pos + 1, s.length());
        }
        return s;
    }

    protected void autoInsertCommand(String command) {
        try {
            Element e = DocumentUtils.getElementForOffset(getStyledDocument(), getCaretPosition());

            // Preserve the spaces at the beginning of the line up to the cursor position
            String text = DocumentUtils.getElementText(e);
            int spaceCount = 0;
            if (text != null) {
                while (spaceCount < text.length() && (e.getStartOffset() + spaceCount < getCaretPosition()) && Character.isWhitespace(text.charAt(spaceCount))) {
                    spaceCount++;
                }
            }
            if (spaceCount > 0) {
                this.indent = spaceCount;
            }

            int insertPos = e.getStartOffset() + spaceCount;

            // Remove the typed text if necessary
            int removeLenth = e.getEndOffset() - insertPos - 1;
            if (removeLenth > 0) {
                getDocument().remove(insertPos, removeLenth);
            }
            CommandHandler ch = (CommandHandler) editorPnl.pnlMain.getScriptHandler().getCommandHandlers().get(command.toUpperCase());

            // Enhancement in 2.0.3 - better GUI support of parameter and argument list
            String arg = null;
            if (!(ch instanceof AdvancedCommandHandler)) {
                arg = ch == null ? null : ch.getContextArgument();
            }

            if (arg == null) {
                getDocument().insertString(insertPos, command + " ", null);
            } else {
                int start = insertPos + command.length() + 2;
                if (!arg.startsWith("\"")) {
                    arg = "\"<" + arg + ">\" ";
                }
                command += " " + arg;
                getDocument().insertString(insertPos, command, null);
                setSelectionStart(start);
                setSelectionEnd(start + arg.length() - 3);
            }

            // Enhancement in 2.0.3 - better GUI support of parameter and argument list.
            // This will open a pop up menu with available arguments.
            if (ch instanceof AdvancedCommandHandler && ch.getContextArgument() != null) {
                displayFilteredMenu(cl);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    protected void autoInsertParameter(String param, boolean isArgument) {
        try {
            if (editorPnl.getTestScript().getType() == TestScriptInterpret.TYPE_PROPRIETARY) {
                Element e = DocumentUtils.getElementForOffset(getStyledDocument(), getCaretPosition());
                CommandHandler ch = ((ProprietaryTestScriptInterpret) editorPnl.getTestScript()).getCommandHandlerForElement(e);
                String text = DocumentUtils.getElementText(e);
                String buf = "";
                if (isArgument) {
                    buf += param;
                    getDocument().insertString(e.getEndOffset() - 1, buf, null);
                } else {
                    if (text == null || !text.endsWith(" ")) {
                        buf += " ";
                    }
                    buf += param + "=";

                    boolean isValueMenu = false;
                    if (ch instanceof AdvancedCommandHandler) {
                        List l = ((AdvancedCommandHandler) ch).getParameterValues(param, text, editorPnl.getTestScript().getCompilationContext());
                        if (l != null) {
                            isValueMenu = true;
                        }
                    }
                    if (isValueMenu) {
                        int pos = e.getEndOffset() - 1;
                        getDocument().insertString(pos, buf, null);
                        setCaretPosition(e.getEndOffset() - 1);
                        displayFilteredMenu(cl);
                    } else {
                        String desc = null;
                        if (ch.getContextAttributes() != null) {
                            desc = (String) ch.getContextAttributes().get(param);
                        }
                        int start = e.getEndOffset() + buf.length();
                        if (desc != null) {
                            buf += "\"<" + desc + ">\"";
                        }
                        getDocument().insertString(e.getEndOffset() - 1, buf, null);
                        if (desc != null) {
                            setSelectionStart(start);
                            setSelectionEnd(start + desc.length() + 2);
                        }

                    }
                }
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    public void scrollElementRectToVisible(Element e) {
        try {
            Rectangle r = getRectangleForLine(DocumentUtils.getLineForOffset(getStyledDocument(), e.getStartOffset()));
            if (r != null) {
                r.width = this.getWidth();
                scrollLineRectToVisible(r);
//                repaint();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void scrollOffsetRectToVisible(Point p) {
        try {
            Rectangle r = getUI().modelToView(this, p.x);
            Rectangle r1 = getUI().modelToView(this, p.x + p.y);
            if (r != null) {
                r.width = r1.x + r1.width - r.x;
                scrollLineRectToVisible(r);
//                repaint();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void displayContextMenu(Point p) {
        Element el = DocumentUtils.getElementForOffset(getStyledDocument(), getUI().viewToModel(this, p));
        JPopupMenu menu = createPopUpMenuForElement(el);
        if (menu.getComponentCount() > 0) { // && canAddBreakpoint(p)) {
            popUpMenuElement = el;

            // Bug fix in 1.3.17 - when caret is somewhere else and user
            // clicks on a command, it doesn't display properties of that
            // command. We have to move the caret to the command to make it work.
            setCaretPosition(el.getStartOffset());

            menu.show(this, (int) p.getX(), (int) p.getY());
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        requestFocus();
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            displayContextMenu(e.getPoint());
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        setToolTipText(null);
        Point p = e.getPoint();
        if (getVisibleRect().contains(p)) {
            Element el = DocumentUtils.getElementForOffset(getStyledDocument(), getUI().viewToModel(this, e.getPoint()));
            SyntaxErrorException ex;
            for (int i = 0; el != null && getErrors() != null && i < getErrors().size(); i++) {
                ex = (SyntaxErrorException) getErrors().get(i);
                if (ex.getElement() != null && ex.getElement().equals(el)) {
                    setToolTipText(ex.getMessage());
                }
            }
        }
    }

    public void caretUpdate(CaretEvent e) {
        if (editorPnl != null && editorPnl.pnlMain != null) {
            editorPnl.pnlMain.propertyChange(new PropertyChangeEvent(this, "caretUpdate", this, e));
        }
    }

    public Element getPopUpMenuElement() {
        return popUpMenuElement;
    }

    /**
     * @return the errors
     */
    public List getErrorList() {
        return getErrors();
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // Do nothing
    }

    /**
     * @return the errors
     */
    public List getErrors() {
        return errors;
    }

    /**
     * Simple highlight painter that fills a highlighted area with
     * a solid color.
     */
    public class ErrorHighlightPainter extends LayeredHighlighter.LayerPainter {

        /**
         * Constructs a new highlight painter. If <code>c</code> is null,
         * the JTextComponent will be queried for its selection color.
         *
         * @param c the color for the highlight
         */
        public ErrorHighlightPainter(Color c) {
            color = c;
        }

        /**
         * Returns the color of the highlight.
         *
         * @return the color
         */
        public Color getColor() {
            return color;
        }

        // --- HighlightPainter methods ---------------------------------------
        /**
         * Paints a highlight.
         *
         * @param g      the graphics context
         * @param offs0  the starting model offset >= 0
         * @param offs1  the ending model offset >= offs1
         * @param bounds the bounding box for the highlight
         * @param c      the editor
         */
        public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
            Rectangle alloc = bounds.getBounds();
            try {
                // --- determine locations ---
                TextUI mapper = c.getUI();
                Rectangle p0 = mapper.modelToView(c, offs0);
                Rectangle p1 = mapper.modelToView(c, offs1);

                // --- render ---
                Color color = getColor();

                if (color == null) {
                    g.setColor(c.getSelectionColor());
                } else {
                    g.setColor(color);
                }
                if (p0.y == p1.y) {
                    // same line, render a rectangle
                    Rectangle r = p0.union(p1);
                    g.fillRect(r.x, r.y + r.height - 2, r.width, r.height);
                } else {
                    // different lines
                    int p0ToMarginWidth = alloc.x + alloc.width - p0.x;
                    g.fillRect(p0.x, p0.y, p0ToMarginWidth, p0.height);
                    if ((p0.y + p0.height) != p1.y) {
                        g.fillRect(alloc.x, p0.y + p0.height, alloc.width,
                                p1.y - (p0.y + p0.height));
                    }
                    g.fillRect(alloc.x, p1.y, (p1.x - alloc.x), p1.height);
                }
            } catch (BadLocationException e) {
                // can't render
            }
        }

        // --- LayerPainter methods ----------------------------
        /**
         * Paints a portion of a highlight.
         *
         * @param g      the graphics context
         * @param offs0  the starting model offset >= 0
         * @param offs1  the ending model offset >= offs1
         * @param bounds the bounding box of the view, which is not
         *               necessarily the region to paint.
         * @param c      the editor
         * @param view   View painting for
         * @return region drawing occured in
         */
        public Shape paintLayer(Graphics g, int offs0, int offs1,
                Shape bounds, JTextComponent c, View view) {
            Color color = getColor();

            if (color == null) {
                g.setColor(c.getSelectionColor());
            } else {
                g.setColor(color);
            }
            if (offs0 == view.getStartOffset()
                    && offs1 == view.getEndOffset()) {
                // Contained in view, can just use bounds.
                Rectangle alloc;
                if (bounds instanceof Rectangle) {
                    alloc = (Rectangle) bounds;
                } else {
                    alloc = bounds.getBounds();
                }
//                g.fillRect(alloc.x, alloc.y, alloc.width, alloc.height);
                g.fillRect(alloc.x, alloc.y + alloc.height - 2, alloc.width, 2);
                return alloc;
            } else {
                // Should only render part of View.
                try {
                    // --- determine locations ---
                    Shape shape = view.modelToView(offs0, Position.Bias.Forward,
                            offs1, Position.Bias.Backward,
                            bounds);
                    Rectangle r = (shape instanceof Rectangle) ? (Rectangle) shape : shape.getBounds();
//                    g.fillRect(r.x, r.y, r.width, r.height);
                    g.fillRect(r.x, r.y + r.height - 2, r.width, 2);
                    return r;
                } catch (BadLocationException e) {
                    // can't render
                }
            }
            // Only if exception
            return null;
        }
        private Color color;
    }

    class CommandHelpAction extends AbstractAction {

        private String helpId;

        CommandHelpAction(String commandName) {
            if (commandName != null) {
                commandName = commandName.toLowerCase();
                this.helpId = "scripting.commref_" + commandName.toLowerCase();
                Object args[] = {commandName.substring(0, 1).toUpperCase() + commandName.substring(1)};
                putValue(Action.NAME, MessageFormat.format(ApplicationSupport.getString("editor.helpOn"), args));
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (editorPnl != null && editorPnl.pnlMain != null) {
                editorPnl.pnlMain.showHelpDialog(helpId, editorPnl.pnlMain);
            }
        }
    }

    class CmdListAction extends AbstractAction {

        CmdListAction() {
            putValue(Action.NAME, ApplicationSupport.getString("editor.commandList"));
        }

        public void actionPerformed(ActionEvent e) {
            typedInMenu = null;
            displayFilteredMenu(cl);
        }
    }

    class SnippetAction extends AbstractAction {

        SnippetAction() {
            putValue(Action.NAME, ApplicationSupport.getString("editor.insertTemplate"));
        }

        public void actionPerformed(ActionEvent e) {
            typedInMenu = null;
            displayFilteredMenu(cl.getSnippetMenu());
        }
    }

    class CompileAction extends AbstractAction {

        EditorPnl pnl;

        CompileAction(EditorPnl pnl) {
            putValue(Action.NAME, ApplicationSupport.getString("editor.compile"));
            this.pnl = pnl;
        }

        public void actionPerformed(ActionEvent e) {
            pnl.compile();
        }
    }

    class ConvertAction extends AbstractAction {

        EditorPnl pnl;

        ConvertAction(EditorPnl pnl) {
            putValue(Action.NAME, ApplicationSupport.getString("editor.convert"));
            this.pnl = pnl;
        }

        public void actionPerformed(ActionEvent e) {
            MainFrame.getInstance().exportToJava(pnl, null);
        }
    }

    class ContextMenuAction extends AbstractAction {

        ContextMenuAction() {
            putValue(Action.NAME, ApplicationSupport.getString("editor.contextMenu"));
        }

        public void actionPerformed(ActionEvent e) {
            try {
                Rectangle r = modelToView(getCaretPosition());
                displayContextMenu(new Point(r.x, r.y));
            } catch (BadLocationException ex) {
            }
        }
    }

    class ArgumentAction extends AbstractAction {

        String text;

        ArgumentAction(String cmdName, String argument) {
            text = cmdName + " " + argument;
            putValue(Action.NAME, text);
        }

        public void actionPerformed(ActionEvent e) {
            autoInsertCommand((String) text);
        }
    }

    class CmdFromShortcutAction extends AbstractAction {

        CmdFromShortcutAction(String cmdName) {
            putValue(Action.NAME, cmdName);
        }

        public void actionPerformed(ActionEvent e) {
            autoInsertCommand((String) getValue(Action.NAME));
        }
    }

    class ConfigureEditorAction extends AbstractAction {

        final String path = ApplicationSupport.getString("com.tplan.robot.gui.options.editorNode");

        public ConfigureEditorAction() {
            super(ApplicationSupport.getString("editor.configureEditor"));
        }

        public void actionPerformed(ActionEvent e) {
            if (editorPnl != null && editorPnl.pnlMain != null) {
                editorPnl.pnlMain.showOptionsDialog(path, null);
            }
        }
    }

    /*
     * Position the caret to the beginning of the line.
     * @see DefaultEditorKit#beginLineAction
     * @see DefaultEditorKit#selectBeginLineAction
     * @see DefaultEditorKit#getActions
     */
//    class BeginLineAction extends TextAction {
//
//        /**
//         * Create this action with the appropriate identifier.
//         * @param nm  the name of the action, Action.NAME.
//         * @param select whether to extend the selection when
//         *  changing the caret position.
//         */
//        BeginLineAction(String nm, boolean select) {
//            super(nm);
//            this.select = select;
//        }
//
//        /** The operation to perform when this action is triggered. */
//        public void actionPerformed(ActionEvent e) {
//            JTextComponent target = getTextComponent(e);
//            if (target != null) {
//                try {
//                    int offs = target.getCaretPosition();
//                    int begOffs = Utilities.getRowStart(target, offs);
//                    if (select) {
//                        target.moveCaretPosition(begOffs);
//                    } else {
//                        target.setCaretPosition(begOffs);
//                    }
//
//                } catch (BadLocationException bl) {
//		    UIManager.getLookAndFeel().provideErrorFeedback(target);
//                }
//            }
//        }
//
//        private boolean select;
//    }
    public boolean isPauseScript() {
        return pauseScript;
    }

    public void setPauseScript(boolean pauseScript) {
        this.pauseScript = pauseScript;
    }

    class ToHtmlAction extends AbstractAction {

        ToHtmlAction() {
            super("Export to HTML");
        }

        public void actionPerformed(ActionEvent e) {
            StringWriter wr = new StringWriter();
            try {
                Utils.exportScriptToHtml(wr, (StyledDocument) getDocument(), false);
                StringSelection stringSelection = new StringSelection(wr.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, Editor.this);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    class ToBBCodeAction extends AbstractAction {

        ToBBCodeAction() {
            super("Export to BBCode");
        }

        public void actionPerformed(ActionEvent e) {
            StringWriter wr = new StringWriter();
            try {
                Utils.exportScriptToBBCode(wr, (StyledDocument) getDocument());
                StringSelection stringSelection = new StringSelection(wr.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, Editor.this);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected class ConfigureAction extends AbstractAction {

        String path;

        public ConfigureAction(String label, String path) {
            this.path = path;
            String s = ApplicationSupport.getResourceBundle().getString("commandHandler.configureActionDesc");
            label = MessageFormat.format(s, label.substring(0, 1).toUpperCase() + label.substring(1, label.length()));
            putValue(SHORT_DESCRIPTION, label);
            putValue(NAME, label);
        }

        public void actionPerformed(ActionEvent e) {
            MainFrame.getInstance().commandEvent(new CommandEvent(this, null, EVENT_DISPLAY_PREFERENCES, path));
        }
    }
}
