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

import com.tplan.robot.preferences.ConfigurationChangeEvent;
import com.tplan.robot.preferences.ConfigurationChangeListener;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.*;

import com.tplan.robot.scripting.ExecOrCompileThread;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Vector;

/**
 * Top level editor component integrating a {@link Editor script editor} and {@link EditorGutter editor gutter}.
 * @product.signature
 */
public class EditorPnl extends JPanel
        implements DocumentListener, CaretListener, GUIConstants,
        UndoableEditListener, ActionListener, ConfigurationChangeListener {

    JPanel pnl = new JPanel();
    Editor editor; //= new Editor(this);
    JScrollPane scrollPane = new JScrollPane();
    EditorGutter pnlSide;
    MainFrame pnlMain;
    JSplitPane splitPane;
    protected UndoAction undoAction = new UndoAction();
    protected RedoAction redoAction = new RedoAction();
    protected UndoManager undo = new UndoManager();
    protected Timer updateTimer;
    protected Thread validationRunnable;
    /**
     * List of action listeners which want to receive events from this class.
     */
    private final Vector actionListeners = new Vector();
    /**
     * Flag showing if there are unsaved changes
     */
    private boolean documentModified = false;
    /**
     * Last file modification time
     */
    private long lastModified = -1;
    boolean changesToCompile = false;
    UserConfiguration cfg;
    private TestScriptInterpret testScript;
    private boolean timerEnabled;
    private int timeout;

    /**
     * Constructor.
     *
     * @param testScript test script interpret
     * @param pnlMain main frame.
     * @param cfg user configuration instance.
     */
    public EditorPnl(TestScriptInterpret testScript, MainFrame pnlMain, UserConfiguration cfg) {
        this.pnlMain = pnlMain;
        this.cfg = cfg;
        this.timerEnabled = cfg.getBoolean("ui.editor.enableContinuousValidation").booleanValue();
        this.timeout = cfg.getInteger("ui.editor.continuousValidationTimeout").intValue();
        editor = new Editor(this, cfg);
        this.testScript = testScript;
        init();
    }

    /**
     * Init method of this component. It initializes all GUI components,
     * binds all necessary listeners and performs layout.
     */
    private void init() {
        editor.setContentType("text/plain");
        setDocument(getTestScript().getDocument(), false);
        editor.addCaretListener(this);
        editor.getKeymap().addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK), undoAction);
        editor.getKeymap().addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK), redoAction);
        pnlSide = new EditorGutter(editor);
        pnlSide.addActionListener(editor);
        pnl.setLayout(new BorderLayout());
        pnl.add(BorderLayout.CENTER, editor);
        pnl.add(BorderLayout.WEST, pnlSide);
        editor.setGutter(pnlSide);

        // Set opaque to true and bg color to white because the editor is not opaque
        pnl.setBackground(Color.white);
        pnl.setOpaque(true);
        scrollPane.getViewport().add(pnl);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(10);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        this.setPreferredSize(new Dimension(220, 100));
        this.setLayout(new BorderLayout());
        this.add(BorderLayout.CENTER, scrollPane);

        // Initialize listeners
        addActionListener(pnlMain);
        addActionListener(pnlSide);
        ScriptManager sh = pnlMain.getScriptHandler();
        sh.addCommandListener(editor);
        sh.addScriptListener(editor);

        cfg.addConfigurationListener(this);
    }

    public File getFile() {
        if (testScript != null && testScript.getURI() != null) {
            return new File(testScript.getURI().getPath());
        }
        return null;
    }

    private void setDocument(StyledDocument doc, boolean copyContent) {
        Document docOld = editor.getDocument();
        docOld.removeDocumentListener(this);
        docOld.removeUndoableEditListener(this);
        undo = new UndoManager();
        try {
            editor.setDocument(doc);
            if (copyContent) {
                String text = docOld.getText(0, docOld.getLength());
                editor.setText(text);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        editor.breakPointTable.clear();
        doc.addDocumentListener(this);
        doc.addUndoableEditListener(this);
    }

    public void close() {
        actionListeners.removeAllElements();
        ScriptManager sh = pnlMain.getScriptHandler();
        sh.removeCommandListener(editor);
        sh.removeScriptListener(editor);
        sh.removeScriptListener(pnlSide);
    }

    private void resetTimer() {
        if (timerEnabled) {
            if (updateTimer == null) {
                updateTimer = new Timer(timeout, this);
                updateTimer.setRepeats(false);
            } else {
                updateTimer.stop();
            }
            updateTimer.setDelay(timeout);
            updateTimer.start();
        } else if (updateTimer != null && updateTimer.isRunning()) {
            updateTimer.stop();
        }
    }

    /**
     * Set the document content from an URL.
     *
     * @param url an URL to load the document from.
     */
//    public void setPage(URL url) {
//        try {
//            Document docOld = editor.getDocument();
//
//            File file = new File(url.getFile());
//            if (!file.exists() || !file.canRead() || !file.isFile()) {
//                return;
//            }
//
//            testScript.setURI(url.toURI());

//            // Resetting of the editor kit is necessary because if we open an HTML document and then
//            // just plain text, the editor throws an exception
//            editor.setEditorKit(new StyledEditorKit());
//            editor.setPage(url);
//            Document defaultDoc = editor.getDocument();
//            StyledDocument cdoc = testScript.getDocument();
//            try {
//                editor.setDocument(cdoc);
//                String text = defaultDoc.getText(0, defaultDoc.getLength());
////                System.out.print("Opening "+url.toString()+"... ");
//                editor.setText(text);
//            } catch (BadLocationException e) {
//                editor.setDocument(defaultDoc);
//                e.printStackTrace();
//            }
//            editor.breakPointTable.clear();
//            lastModified = file.lastModified();
//            fireActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, EVENT_DOCUMENT_CHANGED));
//
//            docOld.removeDocumentListener(this);
//            docOld.removeUndoableEditListener(this);
//            undo = new UndoManager();
//
//            Document doc = editor.getDocument();
//            doc.addDocumentListener(this);
//            doc.addUndoableEditListener(this);

//            fireActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "filenameChanged"));
//            System.out.println("done.");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    /**
     * Fire an ActionEvent to all registered listeners.
     * This class fires events mainly to inform that the document or text
     * selection has changed. Such an event is identified by the
     * <code>actionCommand</code> field of the ActionEvent instance.
     * All action commands used by this class are defined in GUIConstants interface.
     *
     * @param evt an event describing the action.
     */
    protected void fireActionEvent(ActionEvent evt) {
        for (int i = 0; i < actionListeners.size(); i++) {
            ActionListener actionListener = (ActionListener) actionListeners.elementAt(i);
            actionListener.actionPerformed(evt);
        }
    }

    /**
     * Add an action listener which wants to receive events from this class.
     * All listeners are stored in a Vector instance. If an event gets fired, it goes
     * to all listeners in the order they registered in.
     * <p/>
     * It is not possible to add the same instance twice. If <code>listener</code>
     * is already in the list of listeners, the method does nothing.
     *
     * @param listener an instance of a class that implements the ActionListener interface.
     */
    public void addActionListener(ActionListener listener) {
        if (!actionListeners.contains(listener)) {
            actionListeners.add(listener);
        }
    }

    /**
     * Remove an action listener from the list of action listeners.
     * If <code>listener</code> is not in the list of listeners, the method does nothing.
     *
     * @param listener an instance of a class that implements the ActionListener interface.
     */
    public void removeActionListener(ActionListener listener) {
        if (actionListeners.contains(listener)) {
            actionListeners.remove(listener);
        }
    }

    /**
     * Implementation of the DocumentListener interface.
     *
     * @param e a DocumentEvent describing the change in the document.
     */
    public void changedUpdate(DocumentEvent e) {
        fireActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, EVENT_DOCUMENT_CHANGED));
//        System.out.println("doc event fired, type = "+e.getType()+" (CHANGE="+DocumentEvent.EventType.CHANGE+")");

        // Don't set the modification flag to true if the event type is CHANGE because it just means
        // that the style was updated
        if (!e.getType().equals(DocumentEvent.EventType.CHANGE)) {
//            System.out.println("modified=true");
            documentModified = true;
            changesToCompile = true;
        }
    }

    /**
     * Implementation of the DocumentListener interface.
     *
     * @param e a DocumentEvent describing the change in the document.
     */
    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    /**
     * Implementation of the DocumentListener interface.
     *
     * @param e a DocumentEvent describing the change in the document.
     */
    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public void caretUpdate(CaretEvent e) {
        fireActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, EVENT_SELECTION_CHANGED));
        resetTimer();
    }

    /**
     * Find out whether the current document is empty or not. A document containing just
     * white characters (space, new line/CR etc.) is considered to be empty.
     *
     * @return true if the document is empty, false if not.
     */
    public boolean isDocumentEmpty() {
        return "".equals(editor.getText().trim());
    }

    /**
     * Find out whether the current text selection is empty or not.
     *
     * @return true if no text is selected, false if there's a selection.
     */
    public boolean isSelectionEmpty() {
        return editor.getSelectionStart() <= editor.getSelectionEnd();
    }

    /**
     * Get the value of the <code>documentChanged</code> property. It indicates
     * if the current document has been changed by user or not.
     *
     * @return true if the document has been modified by user, false if not
     */
    public boolean isDocumentChanged() {
        return undo.canUndo() && documentModified;
    }

    /**
     * Get the value of the <code>documentChanged</code> property. It indicates
     * if the current document has been changed by user or not.
     *
     * @param documentModified true if the document has been modified by user, false if not
     */
    public void setDocumentModified(boolean documentModified) {
        this.documentModified = documentModified;
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        if (e.getEdit() instanceof AbstractDocument.DefaultDocumentEvent) {
            AbstractDocument.DefaultDocumentEvent evt = (AbstractDocument.DefaultDocumentEvent) e.getEdit();
            if (evt.getType().equals(DocumentEvent.EventType.CHANGE)) {
                return;
            }
        }
        resetTimer();
        undo.addEdit(e.getEdit());
        undoAction.updateUndoState();
        redoAction.updateRedoState();
        pnlMain.menuEnabler();
    }

    public UndoAction getUndoAction() {
        return undoAction;
    }

    public RedoAction getRedoAction() {
        return redoAction;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(updateTimer)) {
            if (!getTestScript().isExecuting() && changesToCompile) {
                validationRunnable = new Thread(new ValidationRunnable(pnlMain, this));
                validationRunnable.start();
            }
        }
    }

    public void configurationChanged(ConfigurationChangeEvent evt) {
        String propName = evt.getPropertyName();
        if (propName.equals("ui.editor.enableContinuousValidation") || propName.equals("ui.editor.continuousValidationTimeout")) {
            if (updateTimer != null) {
                updateTimer.stop();
                updateTimer.removeActionListener(this);
                updateTimer = null;
            }
            timerEnabled = cfg.getBoolean("ui.editor.enableContinuousValidation").booleanValue();
            timeout = cfg.getInteger("ui.editor.continuousValidationTimeout").intValue();
            if (timerEnabled) {
                updateTimer = new Timer(timeout, this);
                updateTimer.setRepeats(false);
                updateTimer.setDelay(timeout);
                updateTimer.start();
            }
        }
    }

    public Editor getEditor() {
        return editor;
    }

    public void compile() {
        Thread thread = new ExecOrCompileThread(getTestScript(), false, pnlMain);
        thread.run();  // Do not start the thread, just compile
        pnlMain.getMsgPanel().setMessageVector(getTestScript().getCompilationContext().getCompilationErrors(), false);
    }
    
//    /**
//     * @return the javaStub
//     */
//    public JavaTestStub getJavaStub() {
//        return javaStub;
//    }
//
//    /**
//     * @param javaStub the javaStub to set
//     */
//    public void setJavaStub(JavaTestStub javaStub) {
//        this.javaStub = javaStub;
//    }
    /**
     * @return the documentType
     */
//    public int getDocumentType() {
//        return documentType;
//    }
//
//    /**
//     * @param documentType the documentType to set
//     */
//    public void setDocumentType(int documentType) {
//        this.documentType = documentType;
//        if (documentType == TestWrapper.WRAPPER_TYPE_JAVA) {
//            editor.getDocument().removeDocumentListener(this);
//            editor.getDocument().removeUndoableEditListener(this);
//            String text = "";
//            try {
//                // Transfer content of the old document to the new one
//                text = editor.getDocument().getText(0, editor.getDocument().getLength());
//            } catch (BadLocationException ex) {
//                ex.printStackTrace();
//            }
//
//            // Set the document to the default one, we do not validate Java code
//            editor.setDocument(new DefaultStyledDocument());
//            editor.setText(text);
//            editor.getDocument().addDocumentListener(this);
//            editor.getDocument().addUndoableEditListener(this);
//        }
//    }
    /**
     * @return the testScript
     */
    public TestScriptInterpret getTestScript() {
        return testScript;
    }

    public class UndoAction extends AbstractAction {

        public UndoAction() {
            super("undo");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                undo.undo();
                resetTimer();
                changesToCompile = true;
            } catch (CannotUndoException ex) {
            }
            updateUndoState();
            redoAction.updateRedoState();
        }

        protected void updateUndoState() {
            setEnabled(undo.canUndo());
        }
    }

    public class RedoAction extends AbstractAction {

        public RedoAction() {
            super("redo");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                undo.redo();
                if (editor.getDocument() instanceof CustomStyledDocument) {
                    CustomStyledDocument doc = (CustomStyledDocument) editor.getDocument();
                    doc.resetStyles(0, doc.getLength());
                }
                resetTimer();
                changesToCompile = true;
            } catch (CannotRedoException ex) {
            }
            updateRedoState();
            undoAction.updateUndoState();
        }

        protected void updateRedoState() {
            setEnabled(undo.canRedo());
        }
    }

    private class ValidationRunnable implements Runnable {

        MainFrame frame;
        EditorPnl editor;
        boolean running = false;

        ValidationRunnable(MainFrame frame, EditorPnl editor) {
            this.frame = frame;
            this.editor = editor;
        }

        public void run() {
            if (!running) {
                running = true;
                frame.compileScript(editor);
//                ScriptingContext ctx = editor.getTestScript().compile();
//                editor.fireActionEvent(new ActionEvent(editor, ActionEvent.ACTION_PERFORMED, "compiled"));
                editor.changesToCompile = false;
                running = false;
            }
        }
    }
}
