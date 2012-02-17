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
package com.tplan.robot.scripting;

//import com.tplan.robot.scripting.imagecomparison.PatternHandler;
import com.tplan.robot.scripting.commands.CommandHandler;
import com.tplan.robot.preferences.ConfigurationChangeEvent;
import com.tplan.robot.preferences.ConfigurationChangeListener;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.editor.EditorPnl;
import com.tplan.robot.gui.DesktopViewer;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.gui.editor.Editor;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.rfb.RfbClient;
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.remoteclient.RemoteDesktopServerListener;
import com.tplan.robot.scripting.commands.impl.MouseCommand;
import com.tplan.robot.scripting.commands.impl.PressCommand;
import com.tplan.robot.scripting.commands.impl.ScreenshotCommand;
import com.tplan.robot.scripting.commands.impl.WaitforCommand;
import com.tplan.robot.util.DocumentUtils;
import com.tplan.robot.util.Utils;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.text.Document;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

/**
 * Test script recorder for the Record&Replay feature. It generates commands of
 * the proprietary scripting language from user interaction with the remote desktop.
 *
 * @product.signature
 */
public class RecordingModule implements MouseInputListener, MouseWheelListener, KeyListener,
        RemoteDesktopServerListener, ConfigurationChangeListener, ActionListener, ScriptListener {

    private EditorPnl editorPnl;
    private boolean enabled = true;
    private Element lastElement;
    private List events = new ArrayList();
    private List rfbEvents = new ArrayList();
    private long lastInsertTime = -1;
    private long lastInteractionTime = -1;
    private long lastMouseExitTime = -1;
    private long lastEventListUpdateTime = -1;
    private Map keyCodes = new HashMap();
    private int EVENT_LIST_MAX_SIZE = 100;
    private int RFB_SIZE_LIMIT;
    private RemoteDesktopClient client;
    private SwingPropertyChangeSupport changeSupport;
    private Timer timer;
    private MouseEvent lastMouseMoveEvent;
    private List mouseMovesList = new ArrayList(EVENT_LIST_MAX_SIZE + 2);
    boolean enableMouseMoves;
    boolean enableMouseDrags;
    boolean enableMouseClicks;
    boolean enableMouseWheel;
    boolean enableMouseStamps = "true".equals(System.getProperty("vncrobot.experimental"));
    boolean enableKeyboard;
    boolean debug = System.getProperty("vncrobot.record.debug", "false").equals("true");
    int keyMutiDelay;
    boolean useTypeline;
    int typelineDelay;
    int mouseMultiDelay;
    int mouseMoveDelay;
    int mouseMoveInsertPrevious;
    boolean insertUpdateArea;
    boolean insertUpdateExtent;
    float defaultUpdateExtent;
    boolean insertUpdateTimeout;
    float timeoutUpdateRatio;
    boolean useMinUpdateTimeout;
    long minUpdateTimeout;
    boolean useUpdateWait;
    boolean useMinUpdateWait;
    float waitUpdateRatio;
    long minUpdateWait;
    boolean resetUpdateWait;
    boolean useBellCount;
    boolean insertBellTimeout;
    float timeoutBellRatio;
    boolean useMinBellTimeout;
    long minBellTimeout;
    boolean resetBellWait;
    // False if we process keyboard and mouse events.
    private boolean readOnly;
    private boolean firstRecord = true;
//    private PatternHandler ph;
    private ScriptManager scriptManager;
    DesktopViewer fb;
    UserConfiguration cfg;
    TokenParser parser = new TokenParserImpl();
    private Rectangle lastUpdatedRectangle = null;

//    private boolean mouseMovePending = false;
//
//    private boolean enableMouseMovesBasedOnUpdates = true;
    /**
     * Constructor.
     *
     * @param eventSource source of the mouse and key events which will be translated into scripting language commands.
     *                    Such a typical source is e.g. the VNC viewer panel.
     */
    public RecordingModule(MainFrame frame, Component eventSource, ScriptManager scriptManager, UserConfiguration cfg) {
        this.cfg = cfg;
        this.scriptManager = scriptManager;
        readOnly = cfg.getBoolean("rfb.readOnly").booleanValue();

        fb = (DesktopViewer) eventSource;
        fb.removeMouseListener(fb);
        eventSource.addMouseListener(this);
        fb.addMouseListener(fb);
        eventSource.addMouseMotionListener(this);
        eventSource.addMouseWheelListener(this);
        eventSource.addKeyListener(this);

        client = scriptManager.getClient();
        if (client != null) {
            client.addServerListener(this);
        }

//        scriptManager.addMouseInputListener(this);
//        scriptManager.addKeyListener(this);

        // Number of archived events
//        events.setSize(EVENT_VECTOR_SIZE);

        // Populate the reversed keycode->keyname Map
        Map t = Utils.getKeyCodeTable();
        Iterator e = t.keySet().iterator();
        Object o;
        while (e.hasNext()) {
            o = e.next();
            keyCodes.put(t.get(o), o);
        }
        cfg.addConfigurationListener(this);
        scriptManager.addScriptListener(this);
        configurationChanged(null);
    }

    public synchronized Element insertLine(String command, boolean replaceLast, boolean insertPrecedingWait, boolean removePrecedingWait) {
//        System.out.println("insertLine(\"" + command + "\")");
        EditorPnl editor = getEditorPnl();
        if (editor != null) {
            try {
                Document doc = editor.getEditor().getDocument();
                int caretPos = editor.getEditor().getCaretPosition();
//                System.out.println("  -->Initial caret position: " + caretPos);
                int elemIndex = doc.getDefaultRootElement().getElementIndex(caretPos);
                boolean insertNewLine = true;

                Element ce = doc.getDefaultRootElement().getElement(elemIndex);
                String txt = DocumentUtils.getElementText(ce).trim();
                if (!replaceLast && caretPos == 0 && elemIndex == 0 && !"".equals(txt)) {
                    doc.insertString(0, "\n", null);
                    txt = "";
                    insertNewLine = false;
                }

                // The following code finds out if we are in an empty line or not.
                // If the current line contains some text, a new line is inserted
                // and the caret is moved there.
                if (!txt.equals("")) {
                    Element next = doc.getDefaultRootElement().getElement(elemIndex + 1);
                    if (next == null || !DocumentUtils.getElementText(next).trim().equals("")) {
//                        DocumentUtils.analyzeEditorDocument(editor.getEditor(), false);
//                        System.out.println("Inserting an empty line to offset " + ce.getEndOffset());
                        doc.insertString(ce.getEndOffset() - 1, "\n", null);
//                        DocumentUtils.analyzeEditorDocument(editor.getEditor(), false);
                        next = doc.getDefaultRootElement().getElement(doc.getDefaultRootElement().getElementIndex(ce.getEndOffset() + 1));
                    }
//                    caretPos = next.getEndOffset()-1;
                    caretPos = ce.getEndOffset();
//                    System.out.println("Setting caret position to "+caretPos);
//                    System.out.println("  -->1. Setting caret position to: " + caretPos);
                    editor.getEditor().setCaretPosition(caretPos);
                }

                Element e = DocumentUtils.getCommandElementPriorTo(doc, caretPos);
//                System.out.println(" --> Element prior: " + DocumentUtils.getElementText(e));

                // First look if we should insert a Wait command
                if (!replaceLast && (insertPrecedingWait || removePrecedingWait) && (!firstRecord && !removePrecedingWait) && e != null) {
                    String prevCmd = DocumentUtils.getElementText(e);
                    int pos = prevCmd.indexOf(" ");
                    String cmd = pos >= 0 ? prevCmd.substring(0, pos) : prevCmd;
                    String time = convertTime(System.currentTimeMillis() - lastInsertTime);
                    CommandHandler h = (CommandHandler) scriptManager.getCommandHandlers().get(cmd.toUpperCase());

                    if (!"waitfor".equalsIgnoreCase(cmd) && !"screenshot".equalsIgnoreCase(cmd)) {

                        if (h != null && h.getContextAttributes() != null && h.getContextAttributes().containsKey("wait")) {
                            doc.remove(e.getStartOffset(), prevCmd.length());
                            String replaceStr = insertPrecedingWait ? " wait=" + time : "";
                            String s = prevCmd.replaceAll("\\n", "").replaceAll("\\swait=[\"]*[0-9]*[sSmMhHdD]*[\"]*", "") + replaceStr;
                            doc.insertString(e.getStartOffset(), s, null);
                            caretPos = e.getEndOffset();
//                            System.out.println("  -->2. Setting caret position to: " + caretPos);
                        } else {
                            String waitCmd = "Wait " + time + '\n';
                            doc.insertString(caretPos, waitCmd, null);
                            caretPos += waitCmd.length();
//                            System.out.println("  -->3. Setting caret position to: " + caretPos);
                        }
                    }
                }

                firstRecord = false;

                if (replaceLast && e != null) {
                    int length = e.getEndOffset() - e.getStartOffset();
                    doc.remove(e.getStartOffset(), length);
                    caretPos = e.getStartOffset();
                }
                command = insertNewLine && !replaceLast ? command + "\n" : command;
                doc.insertString(caretPos, command, null);
                editor.getEditor().setCaretPosition(caretPos + 1);

//                System.out.println(" --> Inserted, caretPos = " + editor.getEditor().getCaretPosition());

//                DocumentUtils.analyzeEditorDocument(editor.getEditor(), true);
                lastElement = doc.getDefaultRootElement().getElement(doc.getDefaultRootElement().getElementIndex(caretPos));
                lastInsertTime = System.currentTimeMillis();
                filterRfbEvents();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return lastElement;
    }

    public void removeElement(Element e) {
        try {
            Document doc = getEditorPnl().getEditor().getDocument();
            doc.remove(e.getStartOffset(), e.getEndOffset() - e.getStartOffset());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String convertTime(long time) {
        boolean round = true;
        if (round) {
            time = 100 * ((int) (time / 100) + 1);
        }
        return "" + time;
    }

    private void insertEvent(Object event) {
        events.add(0, event);
        if (events.size() > EVENT_LIST_MAX_SIZE) {
            events.remove(EVENT_LIST_MAX_SIZE);
        }
    }

    /**
     * This method gets called when user performs a mouse click.
     * It decodes whether it is a single click or part of a multiple click (double click, triple click etc.)
     * and inserts appropriate command to the current editor.
     *
     * @param e a MouseEvent describing the mouse click.
     */
    public void mouseClicked(MouseEvent e) {
        if (enabled && !readOnly) {
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            if (enableMouseClicks) {
                int count = 1;
                MouseEvent e2;

                long lastEventTime = e.getWhen();

                // This cycle is to determine multiple clicks like double click, triple click etc.
                // We go through the vector of events and check whether there are events corresponding to multiple clicks.
                for (int i = 0; i < events.size() && events.get(i) instanceof MouseEvent; i++) {
                    e2 = (MouseEvent) events.get(i);

                    // The events are considered to be a multiple click when:
                    // 1. Coordinates are equal
                    // 2. Modifiers are equal
                    // 3. Button is equal
                    // 4. Delay between two subsequent clicks is lower than given number of miliseconds
                    if (e2.getX() == e.getX() && e2.getY() == e.getY() && e2.getModifiers() == e.getModifiers() && (lastEventTime - e2.getWhen() < mouseMultiDelay) && e.getButton() == e2.getButton() && e2.getID() == e.getID()) {
                        count++;
                        lastEventTime = e2.getWhen();
                    } else {
                        break;
                    }
                }

                // Generate the command string
                String s = "Mouse " + MouseCommand.MOUSE_CLICK;

                // Insert the button identifier if other than left button was pressed
                if (e.getButton() != MouseEvent.BUTTON1) {
                    s += " " + MouseCommand.PARAM_BUTTON_SHORT + "=" + parser.buttonToString(e.getButton());
                }

                // Insert modifiers if there are any
                String modifiers = parser.modifiersToString(e.getModifiers());
                if (modifiers.length() > 0) {
                    s += " " + MouseCommand.PARAM_MODIFIER + "=" + modifiers;
                }

                // Generate the count parameter
                if (count > 1) {
                    s += " " + MouseCommand.PARAM_COUNT + "=" + count;
                }

                // This will determine whether this click is preceded by a mouse
                // move command with the same coordinates.
                // It will be replaced if yes.
                boolean replaceLastMove = false;
//                if (enableMouseMoves) {
//                    if (events.size() > 0 && events.get(events.size() - 1) instanceof MouseEvent) {
//                        MouseEvent me = (MouseEvent) events.get(events.size() - 1);
//                        if (me.getID() == MouseEvent.MOUSE_MOVED && e.getX() == me.getX() && e.getY() == me.getY()) {
//                            replaceLastMove = true;
//                        }
//                    }
//                }

                // Generate coordinates
                s += " " + MouseCommand.PARAM_TO + "=" + parser.pointToString(e.getPoint());

                // Insert the command to the current editor
                insertLine(s, count > 1 || replaceLastMove, true, false);
                dragInProgress = false;
            }
            insertEvent(e);
        }
    }

    public void mouseEntered(MouseEvent e) {
        if (!readOnly) {
            if (lastMouseExitTime > 0 && lastInsertTime > 0) {
                long waitTime = Math.abs(lastMouseExitTime - lastInsertTime);
                lastInsertTime = System.currentTimeMillis() - waitTime;
                lastMouseExitTime = -1;
            } else {
                lastInsertTime = System.currentTimeMillis();
            }
        }
    }

    public void mouseExited(MouseEvent e) {
        if (!readOnly) {
            lastMouseExitTime = System.currentTimeMillis();
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        lastInteractionTime = System.currentTimeMillis();
        lastPressEvent = e;
    }

    public void mouseReleased(MouseEvent e) {
        lastInteractionTime = System.currentTimeMillis();
        if (enabled && !readOnly && lastPressEvent != null && dragInProgress) {

            if (enableMouseDrags && !e.getPoint().equals(lastPressEvent.getPoint())) {

                dragInProgress = false;

                // Generate the command string
                String s = "Mouse " + MouseCommand.MOUSE_DRAG;

                // Insert the button identifier if other than left button was pressed
                if (e.getButton() != MouseEvent.BUTTON1) {
                    s += " " + MouseCommand.PARAM_BUTTON_SHORT + "=" + parser.buttonToString(e.getButton());
                }

                // Insert modifiers if there are any
                String modifiers = parser.modifiersToString(e.getModifiers());
                if (modifiers.length() > 0) {
                    s += " " + MouseCommand.PARAM_MODIFIER + "=" + modifiers;
                }

                // Generate coordinates
                s += " " + MouseCommand.PARAM_FROM + "=" + parser.pointToString(lastPressEvent.getPoint());
                s += " " + MouseCommand.PARAM_TO + "=" + parser.pointToString(e.getPoint());

                // Insert the command to the current editor
                insertLine(s, false, true, false);
                insertEvent(e);
            }
        }
    }
    private MouseEvent lastPressEvent = null;
    private boolean dragInProgress = false;

    public void mouseDragged(MouseEvent e) {
        if (enabled && !readOnly && enableMouseDrags) {
            dragInProgress = true;
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (enabled && enableMouseMoves && !readOnly) {
            if (timer == null) {
                timer = new Timer(mouseMoveDelay, this);
                timer.setRepeats(false);
                timer.start();
            } else {
                timer.stop();
                timer.start();
            }
            lastMouseMoveEvent = e;
            mouseMovesList.add(0, e);
            if (mouseMovesList.size() > mouseMoveInsertPrevious) {
                mouseMovesList.remove(mouseMovesList.size() - 1);
            }
        }
    }

    /**
     * Implementation of the ActionListener interface. It is only called by 
     * a timer which is associated with mouse moves (see mouseMoved method).
     *
     * @param e an ActionEvent instance.
     */
    public void actionPerformed(ActionEvent e) {
        timer.stop();
        if (enabled && !readOnly && lastMouseMoveEvent != null) {
            String s = "Mouse " + MouseCommand.MOUSE_MOVE + " " + MouseCommand.PARAM_TO + "=" + parser.pointToString(lastMouseMoveEvent.getPoint());
            insertLine(s, false, true, false);
            insertEvent(lastMouseMoveEvent);
            mouseMovesList.clear();
        }
    }

    private boolean isKeyReserved(KeyEvent e) {
        return fb.getActionForKeyStroke(KeyStroke.getKeyStrokeForEvent(e)) != null;
    }
    private KeyEvent lastKeyPressEvent = null;

    public void keyPressed(KeyEvent e) {

        if (debug) {
            System.out.println("--- RecordingModule: key pressed = " + e + "\n  > Key char->int = " + (int) e.getKeyChar());
        }

        // Here we process just action keys because they do not generate KEY_TYPED events.
        // Other key events are handled by the keyTyped method.
        lastInteractionTime = System.currentTimeMillis();

        if (isKeyReserved(e)) {
            return;
        }

        if (enabled && !readOnly) {
//        System.out.println("keyPressed (e.isActionKey()=)"+e.isActionKey()+": "+e.toString());
            // TODO: implement text corrections in type like Delete, Backspace

            if (e.isActionKey()) {

                if (enableKeyboard) {
                    int count = 1;
                    KeyEvent e2;

                    long lastEventTime = e.getWhen();

                    // We go through the vector of events and check whether there are events corresponding to a typed text.
                    for (int i = 0; i < events.size() && events.get(i) instanceof KeyEvent; i++) {
                        e2 = (KeyEvent) events.get(i);
                        if (e.getID() == e2.getID() && e.getKeyChar() == e2.getKeyChar() && e.getKeyCode() == e2.getKeyCode() && e.getModifiers() == e2.getModifiers() && (lastEventTime - e2.getWhen() < keyMutiDelay)) {
                            count++;
                            lastEventTime = e2.getWhen();
                        } else {
                            break;
                        }
                    }

                    String text = "Press ";
//                String modifiers = KeyEvent.getKeyModifiersText(e.getModifiers());
                    String modifiers = parser.modifiersToString(e.getModifiers());
                    if (!"".equals(modifiers)) {
                        text += modifiers + "+";
                    }
                    String charText = (String) keyCodes.get(new Integer(e.getKeyCode()));
                    if (charText == null) {
                        charText = "<unknown>";
                    }
                    text += charText;
                    if (count > 1) {
                        text += " " + PressCommand.PARAM_COUNT + "=" + count;
                    }
//                text += '\n';

                    if (debug) {
                        System.out.println("--- RecordingModule: Inserting '" + text + "'");
                    }

                    // Insert the command to the current editor
                    insertLine(text, count > 1, true, false);
                }
                insertEvent(e);
            }
            lastKeyPressEvent = e;
        }
    }

    public void keyReleased(KeyEvent e) {
//        System.out.println("keyReleased: "+e);
//        insertEvent(e);
    }

    public void keyTyped(KeyEvent e) {
        if (debug) {
            System.out.println("--- RecordingModule: key typed = " + e + "\n  > Key char->int = " + (int) e.getKeyChar());
            System.out.println(" -- isActionKey() = " + e.isActionKey());
            System.out.println(" -- isISOControl() = " + Character.isISOControl(e.getKeyChar()));
            System.out.println(" -- isWhitespace() = " + Character.isWhitespace(e.getKeyChar()));
        }

        if (isKeyReserved(e)) {
            return;
        }

        if (enabled && !readOnly && lastKeyPressEvent != null) {

            if (enableKeyboard) {
                boolean replace = false;
                String text = "";
                if (isControl(e)) {
                    if (lastKeyPressEvent.getKeyCode() == KeyEvent.VK_ENTER) {

                        // Change the Type cmd prior to Typeline if the delay from the last type key is less than 1 sec
                        if (useTypeline && e.getModifiers() == 0 && lastElement != null) {
                            String s = DocumentUtils.getElementText(lastElement);
                            if (s.startsWith("Type ") &&
                                    (System.currentTimeMillis() - lastInsertTime) < typelineDelay) {
                                replace = true;
                                text = s.replaceFirst("Type", "Typeline");
                            }
                        }
                    }

                    if ("".equals(text)) {
                        int count = 1;
                        KeyEvent e2;

                        long lastEventTime = e.getWhen();

                        // We go through the vector of events and check whether there are events corresponding to a typed text.
                        for (int i = 0; i < events.size() && events.get(i) instanceof KeyEvent; i++) {
                            e2 = (KeyEvent) events.get(i);
                            if (e.getID() == e2.getID() && e.getKeyChar() == e2.getKeyChar() && e.getKeyCode() == e2.getKeyCode() && e.getModifiers() == e2.getModifiers() && (lastEventTime - e2.getWhen() < keyMutiDelay)) {
                                count++;
                                replace = true;
                                lastEventTime = e2.getWhen();
                            } else {
                                break;
                            }
                        }

                        text = "Press ";
//                    String modifiers = KeyEvent.getKeyModifiersText(e.getModifiers());
                        String modifiers = parser.modifiersToString(e.getModifiers());
                        if (!"".equals(modifiers)) {
                            text += modifiers + "+";
                        }
                        String charText = KeyEvent.getKeyText(lastKeyPressEvent.getKeyCode());
                        if (charText == null) {
                            charText = "<unknown>";
                        }
                        text += charText;
                        if (count > 1) {
                            text += " " + PressCommand.PARAM_COUNT + "=" + count;
                        }

                        if (debug) {
                            System.out.println("--- RecordingModule: Inserting '" + text + "'");
                        }
                    }

                } else {
                    text = "" + e.getKeyChar();
                    KeyEvent e2;

                    // We go through the vector of events and check whether there are events corresponding to a typed text.
                    for (int i = 0; i < events.size() && events.get(i) instanceof KeyEvent; i++) {
                        e2 = (KeyEvent) events.get(i);
                        if (!isControl(e2) && !e2.isActionKey()) {
                            text = e2.getKeyChar() + text;
                            replace = true;
                        } else {
                            break;
                        }
                    }

                    text = "Type \"" + Utils.escapeUnescapedDoubleQuotes(text) + "\"";
                }

                // Insert the command to the current editor
                insertLine(text, replace, true, false);
            }
            insertEvent(e);
        }
    }

    private boolean isControl(KeyEvent e) {
        return Character.isISOControl(e.getKeyChar()) || (e.isAltDown() || e.isAltGraphDown() || e.isControlDown() || e.isMetaDown());
    }

    public EditorPnl getEditorPnl() {
        return editorPnl;
    }

    public void setEditorPnl(EditorPnl editorPnl) {
        if (editorPnl == null || !editorPnl.equals(this.editorPnl)) {
//            System.out.println("Selecting "+editorPnl);
            this.editorPnl = editorPnl;
            firePropertyChange("recordingEditorChanged", null, editorPnl);
        }
    }

    public void serverMessageReceived(RemoteDesktopServerEvent e) {
//        if (enabled) {
        if (e.getMessageType() == RemoteDesktopServerEvent.SERVER_UPDATE_EVENT) {
            RemoteDesktopClient rfb = e.getClient();
            if (rfb.isConnected() && rfb.getDesktopWidth() > 0 && rfb.getDesktopHeight() > 0) {
                Rectangle r = e.getUpdateRect();

                int cw = rfb.getDesktopWidth();
                int ch = rfb.getDesktopHeight();
                float ratio = 100 * (r.width * r.height) / (cw * ch);
                if (ratio >= RFB_SIZE_LIMIT) {
                    synchronized (rfbEvents) {
                        rfbEvents.add(0, e);
                        filterRfbEvents();
                    }
                }
            }
        } else if (e.getMessageType() == RemoteDesktopServerEvent.SERVER_BELL_EVENT) {
            synchronized (rfbEvents) {
                rfbEvents.add(0, e);
                filterRfbEvents();
            }
        }
    }

    /**
     * Remove all events that are older than the last user interaction
     */
    private void filterRfbEvents() {
        RemoteDesktopServerEvent event;
        List v = new ArrayList();
        for (int i = 0; i < rfbEvents.size(); i++) {
            event = (RemoteDesktopServerEvent) rfbEvents.get(i);
            if (lastInteractionTime > 0 && event != null && event.getWhen() >= lastInteractionTime) {
                v.add(event);
            }
        }
        rfbEvents = v;
        lastEventListUpdateTime = lastInteractionTime;
        firePropertyChange("rfbEventList", new Long(lastInteractionTime), new ArrayList(rfbEvents));
    }

    public long getLastEventListUpdateTime() {
        return lastEventListUpdateTime;
    }

    public List getRfbEvents() {
        return new ArrayList(rfbEvents);
    }

    public void insertWaitFor(List selectedEvents, List events, boolean preferBellEvents) {
        if (preferBellEvents) {
            RemoteDesktopServerEvent e;
            for (int i = 0; i < selectedEvents.size(); i++) {
                e = (RemoteDesktopServerEvent) selectedEvents.get(i);
                if (e.getMessageType() == RemoteDesktopServerEvent.SERVER_BELL_EVENT) {
                    insertLine(createWaitForBell(e, events, null), false, false,
                            cfg.getBoolean("recording.waitfor.bell.resetBellWait").booleanValue());
                    break;
                }
            }
        } else {
            insertLine(createWaitForUpdate(createUpdateEvent(selectedEvents), events, null), false, false,
                    cfg.getBoolean("recording.waitfor.update.resetUpdateWait").booleanValue());
        }
    }

    private RemoteDesktopServerEvent createUpdateEvent(List selectedEvents) {
        Rectangle r = null;
        RemoteDesktopServerEvent e = null;
        long time = 0;
        int p1, p2;

        for (int i = 0; selectedEvents != null && i < selectedEvents.size(); i++) {
            e = (RemoteDesktopServerEvent) selectedEvents.get(i);
            if (e.getMessageType() == RemoteDesktopServerEvent.SERVER_UPDATE_EVENT) {
                time = Math.max(time, e.getWhen());
                if (r == null) {
                    r = new Rectangle(e.getUpdateRect());
                } else {
                    r.x = Math.min(r.x, e.getUpdateRect().x);
                    r.y = Math.min(r.y, e.getUpdateRect().y);
                    p1 = r.x + r.width;
                    p2 = e.getUpdateRect().x + e.getUpdateRect().width;
                    if (p2 > p1) {
                        r.width = p2 - r.x;
                    }
                    p1 = r.y + r.height;
                    p2 = e.getUpdateRect().y + e.getUpdateRect().height;
                    if (p2 > p1) {
                        r.height = p2 - r.y;
                    }
                }
            }
        }
        if (r == null) {
            return null;
        }
//            System.out.println("Resulting rect: "+r);
        e = new RemoteDesktopServerEvent(e.getClient(), r);
        e.setWhen(time);
        return e;
    }

    public String createWaitForUpdate(RemoteDesktopServerEvent e, List events, UserConfiguration cfg) {

        boolean insertUpdateArea = this.insertUpdateArea;
        boolean insertUpdateExtent = this.insertUpdateExtent;
        float defaultUpdateExtent = this.defaultUpdateExtent;
        boolean insertUpdateTimeout = this.insertUpdateTimeout;
        float timeoutUpdateRatio = this.timeoutUpdateRatio;
        boolean useMinUpdateTimeout = this.useMinUpdateTimeout;
        long minUpdateTimeout = this.minUpdateTimeout;
        boolean useUpdateWait = this.useUpdateWait;
        boolean useMinUpdateWait = this.useMinUpdateWait;
        float waitUpdateRatio = this.waitUpdateRatio;
        long minUpdateWait = this.minUpdateWait;

        if (cfg != null) {
            insertUpdateArea = cfg.getBoolean("recording.waitfor.update.insertArea").booleanValue();
            insertUpdateExtent = cfg.getBoolean("recording.waitfor.update.insertExtent").booleanValue();
            defaultUpdateExtent = cfg.getInteger("recording.waitfor.update.defaultExtent").intValue();
            insertUpdateTimeout = cfg.getBoolean("recording.waitfor.update.insertTimeout").booleanValue();
            timeoutUpdateRatio = cfg.getDouble("recording.waitfor.update.timeoutRatio").floatValue();
            useMinUpdateTimeout = cfg.getBoolean("recording.waitfor.update.useMinTimeout").booleanValue();
            minUpdateTimeout = cfg.getInteger("recording.waitfor.update.minTimeout").intValue();
            useUpdateWait = cfg.getBoolean("recording.waitfor.update.useWait").booleanValue();
            useMinUpdateWait = cfg.getBoolean("recording.waitfor.update.useMinWait").booleanValue();
            waitUpdateRatio = cfg.getDouble("recording.waitfor.update.waitRatio").floatValue();
            minUpdateWait = cfg.getInteger("recording.waitfor.update.minWait").intValue();
        }

        String s = "Waitfor " + WaitforCommand.EVENT_UPDATE;
        if (insertUpdateArea) {
            s += " " + WaitforCommand.PARAM_AREA + "=" + parser.rectToString(e.getUpdateRect());
        }

        if (insertUpdateExtent) {
            float extent = defaultUpdateExtent;

            // If the 'area' param is not included, we must calculate a relative update compared to the whole screen
            if (!insertUpdateArea) {
                RfbClient rfb = (RfbClient) e.getSource();
                extent = defaultUpdateExtent * ((float) (e.getUpdateRect().width * e.getUpdateRect().height) / (rfb.getDesktopWidth() * rfb.getDesktopHeight()));
            }
            s += " " + WaitforCommand.PARAM_EXTENT + "=" + extent + "%";
        }

        long time = e.getWhen();
//        System.out.println("base event, time="+e.getWhen());

        RemoteDesktopServerEvent evt;
        int count = 1;
        for (int i = 0; i < events.size(); i++) {
            evt = (RemoteDesktopServerEvent) events.get(i);
            if (!e.equals(evt) && isUpdateApplicable(e, evt, insertUpdateExtent ? defaultUpdateExtent : 100)) {
                count++;
                time = Math.max(time, evt.getWhen());
//                System.out.println("applicable event #"+i+" found, time="+evt.getWhen());
            }
        }

//        System.out.println("final time="+time);
        if (count > 1) {
            if (!useUpdateWait) {
                s += " " + WaitforCommand.PARAM_COUNT + "=" + count;
            } else {
                long wait = (long) ((e.getWhen() - lastEventListUpdateTime) * waitUpdateRatio);
                if (useMinUpdateWait) {
                    wait = wait > minUpdateWait ? wait : minUpdateWait;
                }
                s += " " + WaitforCommand.PARAM_WAIT + "=" + wait;
            }
        }

        if (insertUpdateTimeout) {
            time = (long) ((time - lastEventListUpdateTime) * timeoutUpdateRatio);
            if (useMinUpdateTimeout) {
                time = time > minUpdateTimeout ? time : minUpdateTimeout;
            }
            s += " " + WaitforCommand.PARAM_TIMEOUT + "=" + time;
        }
        return s;
    }

    private boolean isUpdateApplicable(RemoteDesktopServerEvent e, RemoteDesktopServerEvent evt, float passRatio) {
        if (e.getUpdateRect() != null && evt.getUpdateRect() != null) {
            Rectangle intersect = e.getUpdateRect().intersection(evt.getUpdateRect());
            if (intersect != null && !intersect.isEmpty()) {
                float ratio = 100 * (intersect.width * intersect.height) / (e.getUpdateRect().width * e.getUpdateRect().height);
                return ratio >= passRatio;
            }
        }
        return false;
    }

    /**
     * Create a bell event corresponding to an RFB Bell event. The vector of events passed as argument of this method
     * is searched for similar events and used for definition of the <code>count</code> parameter.
     *
     * @param e
     * @return a <code>'Waitfor bell'</code> command created with the specified parameters
     */
    public String createWaitForBell(RemoteDesktopServerEvent e, List events, UserConfiguration cfg) {

        boolean useBellCount = this.useBellCount;
        boolean insertBellTimeout = this.insertBellTimeout;
        float timeoutBellRatio = this.timeoutBellRatio;
        boolean useMinBellTimeout = this.useMinBellTimeout;
        long minBellTimeout = this.minBellTimeout;

        if (cfg != null) {
            useBellCount = cfg.getBoolean("recording.waitfor.bell.useCount").booleanValue();
            insertBellTimeout = cfg.getBoolean("recording.waitfor.bell.insertTimeout").booleanValue();
            timeoutBellRatio = cfg.getDouble("recording.waitfor.bell.timeoutRatio").floatValue();
            useMinBellTimeout = cfg.getBoolean("recording.waitfor.bell.useMinTimeout").booleanValue();
            minBellTimeout = cfg.getInteger("recording.waitfor.bell.minTimeout").intValue();
        }

        String s = "Waitfor " + WaitforCommand.EVENT_BELL;

        long time = e.getWhen();

        if (useBellCount) {
            RemoteDesktopServerEvent evt;
            int count = 0;
            for (int i = 0; i < events.size(); i++) {
                evt = (RemoteDesktopServerEvent) events.get(i);
                if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_BELL_EVENT) {
                    count++;
                    time = Math.max(time, evt.getWhen());
                }
            }
            if (count > 1) {
                s += " " + WaitforCommand.PARAM_COUNT + "=" + count;
            }
        }
        if (insertBellTimeout) {
            time = (long) ((time - lastEventListUpdateTime) * timeoutBellRatio);
            if (useMinBellTimeout) {
                time = time > minBellTimeout ? time : minBellTimeout;
            }
            s += " " + WaitforCommand.PARAM_TIMEOUT + "=" + time;
        }
        return s;
    }

    public void insertScreenshot(boolean replace, List v, Map t) {
        String cmd = DocumentUtils.getCommandForValueMap(v, t);
        insertLine(cmd, replace, isEnabled(), false);
    }

    public void insertScreenshot(boolean replace, String fileName, String desc, String attachments, String template) {
        String s = "Screenshot " + fileName;
        if (desc != null && !desc.trim().equals("")) {
            s += " " + ScreenshotCommand.PARAM_DESC + "=\"" + Utils.escapeUnescapedDoubleQuotes(desc) + "\"";
        }
        if (attachments != null && !attachments.trim().equals("")) {
            s += " " + ScreenshotCommand.PARAM_ATTACH + "=\"" + Utils.escapeUnescapedDoubleQuotes(attachments) + "\"";
        }
        if (template != null && !template.trim().equals("")) {
            s += " " + ScreenshotCommand.PARAM_TEMPLATE + "=\"" + Utils.escapeUnescapedDoubleQuotes(template) + "\"";
        }
        insertLine(s, replace, isEnabled(), false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (!enabled && this.enabled && lastElement != null) {
            // Insert a timeout to the last generated command
            Editor ed = editorPnl.getEditor();
            if (lastElement.getDocument().equals(ed.getDocument())) {
                Element e = ed.getCurrentElement();
                int offset = ed.getCaretPosition() - e.getStartOffset();
                ed.setCaretPosition(lastElement.getStartOffset());
                String text = DocumentUtils.getElementText(lastElement);
                int pos = text.indexOf(" ");
                String cmd = pos >= 0 ? text.substring(0, pos) : text;
                String time = convertTime(System.currentTimeMillis() - lastInsertTime);
                CommandHandler h = (CommandHandler) scriptManager.getCommandHandlers().get(cmd.toUpperCase());

                if (h != null && h.getContextAttributes() != null && h.getContextAttributes().containsKey("wait")) {
                    text += " wait=" + time;
                } else {
                    text += "\nWait " + time;
                }
                insertLine(text, true, false, false);
            }
        }
        this.enabled = enabled;
        fb.setRecordingMode(enabled);
        if (enabled) {
            firstRecord = true;
            lastElement = null;
            lastInsertTime = -1;
            lastMouseMoveEvent = null;
            events.clear();
            synchronized (rfbEvents) {
                rfbEvents.clear();
            }
        }
    }

    public void resetTime() {
//        System.out.println("Resetting time");
        lastInsertTime = System.currentTimeMillis();
        lastInteractionTime = -1;
        lastMouseExitTime = -1;
    }

    /**
     * Adds a <code>PropertyChangeListener</code> to the listener list.
     * The listener is registered for all properties.
     * <p/>
     * A <code>PropertyChangeEvent</code> will get fired in response
     * to setting a bound property, such as <code>setFont</code>,
     * <code>setBackground</code>, or <code>setForeground</code>.
     * <p/>
     * Note that if the current component is inheriting its foreground,
     * background, or font from its container, then no event will be
     * fired in response to a change in the inherited property.
     *
     * @param listener the <code>PropertyChangeListener</code> to be added
     */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.removePropertyChangeListener(listener);
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * This class fires events mainly during scenario replay to inform
     * other objects that a scenario is being replayed or that it has been
     * paused, resumed or stopped. Such an event is identified by the
     * <code>propertyName</code> field of the PropertyChangeEvent instance.
     *
     * @param propertyName property name describing the action.
     * @param oldValue     the old value of the property (as an Object)
     * @param newValue     the new value of the property (as an Object)
     * @see java.beans.PropertyChangeSupport
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changeSupport != null) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Removes a <code>PropertyChangeListener</code> from the listener list.
     * This removes a <code>PropertyChangeListener</code> that was registered
     * for all properties.
     *
     * @param listener the <code>PropertyChangeListener</code> to be removed
     */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    public void configurationChanged(ConfigurationChangeEvent evt) {
        if (evt == null || evt.getPropertyName().startsWith("recording")) {
            enableMouseMoves = cfg.getBoolean("recording.enableMouseMoves").booleanValue();
            enableMouseDrags = cfg.getBoolean("recording.enableMouseDrags").booleanValue();
            enableMouseClicks = cfg.getBoolean("recording.enableMouseClicks").booleanValue();
            enableKeyboard = cfg.getBoolean("recording.enableKeyboard").booleanValue();
            enableMouseWheel = cfg.getBoolean("recording.enableMouseWheel").booleanValue();

            RFB_SIZE_LIMIT = cfg.getInteger("recording.minEventSize").intValue();

            // This delay will be used to decide whether two subsequent clicks define a double click or
            // two separate clicks
            mouseMultiDelay = cfg.getInteger("recording.mouse.multiClickDelay").intValue();
            mouseMoveDelay = cfg.getInteger("recording.mouse.moveDelay").intValue();
            mouseMoveInsertPrevious = cfg.getInteger("recording.mouse.moveInsertPrevious").intValue();
            if (mouseMovesList.size() > mouseMoveInsertPrevious) {
                mouseMovesList.remove(mouseMovesList.size() - 1);
            }

            keyMutiDelay = cfg.getInteger("recording.keyboard.multiKeyDelay").intValue();
            useTypeline = cfg.getBoolean("recording.keyboard.enableTypeline").booleanValue();
            typelineDelay = cfg.getInteger("recording.keyboard.typelineDelay").intValue();

            insertUpdateArea = cfg.getBoolean("recording.waitfor.update.insertArea").booleanValue();
            insertUpdateExtent = cfg.getBoolean("recording.waitfor.update.insertExtent").booleanValue();
            defaultUpdateExtent = cfg.getDouble("recording.waitfor.update.defaultExtent").floatValue();
            insertUpdateTimeout = cfg.getBoolean("recording.waitfor.update.insertTimeout").booleanValue();
            timeoutUpdateRatio = cfg.getDouble("recording.waitfor.update.timeoutRatio").floatValue();
            useMinUpdateTimeout = cfg.getBoolean("recording.waitfor.update.useMinTimeout").booleanValue();
            minUpdateTimeout = cfg.getInteger("recording.waitfor.update.minTimeout").intValue();

            useUpdateWait = cfg.getBoolean("recording.waitfor.update.useWait").booleanValue();
            useMinUpdateWait = cfg.getBoolean("recording.waitfor.update.useMinWait").booleanValue();
            waitUpdateRatio = cfg.getDouble("recording.waitfor.update.waitRatio").floatValue();
            minUpdateWait = cfg.getInteger("recording.waitfor.update.minWait").intValue();
            resetUpdateWait = cfg.getBoolean("recording.waitfor.update.resetUpdateWait").booleanValue();

            useBellCount = cfg.getBoolean("recording.waitfor.bell.useCount").booleanValue();
            insertBellTimeout = cfg.getBoolean("recording.waitfor.bell.insertTimeout").booleanValue();
            timeoutBellRatio = cfg.getDouble("recording.waitfor.bell.timeoutRatio").floatValue();
            useMinBellTimeout = cfg.getBoolean("recording.waitfor.bell.useMinTimeout").booleanValue();
            minBellTimeout = cfg.getInteger("recording.waitfor.bell.minTimeout").intValue();
            resetBellWait = cfg.getBoolean("recording.waitfor.bell.resetBellWait").booleanValue();
        } else if (evt.getPropertyName().startsWith("rfb.readOnly")) {
            Boolean b = cfg.getBoolean("rfb.readOnly");
            readOnly = b == null ? false : b.booleanValue();
            if (readOnly) {
                resetTime();
            }
        }
    }

    public void scriptEvent(ScriptEvent event) {
        if (event.getType() == ScriptEvent.SCRIPT_EXECUTED_LINE_CHANGED) {
            synchronized (rfbEvents) {
                rfbEvents.clear();
            }
            firePropertyChange("rfbEventList", new Long(lastInteractionTime), new ArrayList(rfbEvents));
        } else if (event.getType() == ScriptEvent.SCRIPT_CLIENT_CREATED) {
            if (client != null) {
                client.removeServerListener(this);
            }
            client = event.getContext().getClient();
            client.addServerListener(this);
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if (enabled && !readOnly) {
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            if (enableMouseWheel) {
                int count = 1;
                MouseWheelEvent e2;

                long lastEventTime = e.getWhen();
                int steps = 0;
                int r;

                // This cycle is to determine multiple clicks like double wheel, triple wheel event etc.
                // We go through the vector of events and check whether there are events corresponding to multiple events.
                for (int i = 0; i < events.size() && events.get(i) instanceof MouseWheelEvent; i++) {
                    e2 = (MouseWheelEvent) events.get(i);

                    // The events are considered to be a multiple click when:
                    // 1. Coordinates are equal
                    // 2. Modifiers are equal
                    // 3. Both events are wheel up or down
                    // 4. Delay between two subsequent clicks is lower than given number of miliseconds
                    r = e2.getWheelRotation();
                    if (e2.getX() == e.getX() && e2.getY() == e.getY() && e2.getModifiers() == e.getModifiers() && (lastEventTime - e2.getWhen() < mouseMultiDelay) && e2.getID() == e.getID() && ((r > 0 && e.getWheelRotation() > 0) || (r < 0 && e.getWheelRotation() < 0))) {
                        count++;
                        lastEventTime = e2.getWhen();
                        steps += Math.abs(r);
                    } else {
                        break;
                    }
                }

                steps += Math.abs(e.getWheelRotation());

                // Generate the command string
                String s = "Mouse " + (e.getWheelRotation() > 0 ? MouseCommand.MOUSE_WHEEL_DOWN : MouseCommand.MOUSE_WHEEL_UP);

                // Insert modifiers if there are any
                String modifiers = parser.modifiersToString(e.getModifiers());
                if (modifiers.length() > 0) {
                    s += " " + MouseCommand.PARAM_MODIFIER + "=" + modifiers;
                }

                // Generate the count parameter
                if (count > 1) {
                    s += " " + MouseCommand.PARAM_COUNT + "=" + Math.abs(steps);
                }

                // This will determine whether this event is preceded by a mouse move command with the same coordinates.
                // It will be replaced if yes.
                boolean replaceLastMove = false;
                if (enableMouseMoves) {
                    if (events.size() > 0 && events.get(events.size() - 1) instanceof MouseEvent) {
                        MouseEvent me = (MouseEvent) events.get(events.size() - 1);
                        if (me.getID() == MouseEvent.MOUSE_MOVED && e.getX() == me.getX() && e.getY() == me.getY()) {
                            replaceLastMove = true;
                        }
                    }
                }

                // Generate coordinates
                s += " " + MouseCommand.PARAM_TO + "=" + parser.pointToString(e.getPoint());

                // Insert the command to the current editor
                insertLine(s, count > 1 || replaceLastMove, true, false);
                dragInProgress = false;
            }
            insertEvent(e);
        }
    }
}
