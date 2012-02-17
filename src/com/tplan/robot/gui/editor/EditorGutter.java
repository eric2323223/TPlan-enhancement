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
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.*;

import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptListener;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.util.DocumentUtils;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.Iterator;
import java.util.Map;

/**
 * Test script editor gutter implementing infrastructure for code breakpoints.
 * @product.signature
 */
public class EditorGutter extends JPanel
        implements MouseListener, ActionListener, ConfigurationKeys, ConfigurationChangeListener,
        PropertyChangeListener, GUIConstants, ScriptListener {
    
    private Editor editor;
    
    private ImageIcon iconBreak;
    
    private Element executedElement = null;
    
    private Color executionColor;
    
    private JPopupMenu popUpMenu;
    
    private AddBreakPointAction addAction = new AddBreakPointAction();
    
    private RemoveBreakPointAction removeAction = new RemoveBreakPointAction();
    
    private RemoveAllBreakPointsAction removeAllAction = new RemoveAllBreakPointsAction();
    
    /**
     * List of action listeners which want to receive events from this class.
     */
    private final Vector actionListeners = new Vector();
    
    private Font lineNumberFont;
    
    private boolean allowLineNumbering = false;
    
    EditorGutter(Editor editor) {
        this.editor = editor;
        Font f = getFont();
        f = new Font(f.getName(), f.getStyle(), f.getSize()-3);
        setLineNumberFont(f);
        setPreferredSize(new Dimension(16, 10));
        addMouseListener(this);
        iconBreak = ApplicationSupport.getImageIcon("breakpoint16.png");
        editor.cfg.addConfigurationListener(this);
        reloadConfiguration();
    }
    
    private JPopupMenu getPopUpMenu() {
        if (popUpMenu == null) {
            popUpMenu = new JPopupMenu();
            
            JMenuItem item = new JMenuItem();
            item.setAction(addAction);
            popUpMenu.add(item);
            
            item = new JMenuItem();
            item.setAction(removeAction);
            popUpMenu.add(item);
            
            popUpMenu.addSeparator();
            
            item = new JMenuItem();
            item.setAction(removeAllAction);
            popUpMenu.add(item);
            
            popUpMenu.setInvoker(this);
        }
        return popUpMenu;
    }
    
    public void paint(Graphics g) {
        super.paint(g);
        
        // The following will paint breakpoints. As breakpoints are defined as
        // a vector of line numbers, we need to find out the offset (start char
        // position) and end char position and then we can use TextUI to compute
        // the line rectangle for us
        Rectangle r;
        
        Map breakPoints = editor.breakPointTable;
        if (executedElement != null && breakPoints.containsKey(executedElement)) {
            try {
                r = editor.getRectangleForLine(DocumentUtils.getLineForOffset(editor.getStyledDocument(), executedElement.getStartOffset()));
                r.x = 0;
                r.width = this.getWidth();
                g.setColor(executionColor);
                g.fillRect(r.x, r.y, r.width, r.height);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        
        Iterator en = breakPoints.keySet().iterator();
        Element elem;
        while (en.hasNext()) {
            elem = (Element) en.next();
            int line = DocumentUtils.getLineForOffset(editor.getStyledDocument(), elem.getStartOffset());
            try {
                r = editor.getRectangleForLine(line);
                r.x = 0;
                r.width = this.getWidth();
                
                // Center the image icon
                Point p = centerIcon(iconBreak, r);
                g.drawImage(iconBreak.getImage(), p.x, p.y, this);
                
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        
        if (allowLineNumbering) {
            r = editor.getVisibleRect();
            int offset = editor.viewToModel(new Point(r.x+this.getWidth()+1, r.y));
            int line = DocumentUtils.getLineForOffset(editor.getStyledDocument(), offset);
            
            try {
                Rectangle lineRect = editor.modelToView(offset);
                lineRect.width = this.getWidth();
                g.setFont(lineNumberFont);
                g.setColor(Color.GRAY);
                while (lineRect.intersects(r) && line < editor.getLineCount()-1) {
                    g.drawString(""+(line+1), lineRect.x, lineRect.y+lineNumberFont.getSize()+1);
                    offset = DocumentUtils.getOffsetForLine(editor.getStyledDocument(), ++line);
                    lineRect = editor.modelToView(offset);
                    lineRect.width = this.getWidth();
                }
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private Point centerIcon(Icon i, Rectangle r) {
        Point p = new Point();
        p.x = r.x + (r.width - i.getIconWidth()) / 2;
        p.y = r.y + (r.height - i.getIconHeight()) / 2;
        return p;
    }
    
    protected void fireActionEvent(ActionEvent evt) {
        for (int i = 0; i < actionListeners.size(); i++) {
            ActionListener actionListener = (ActionListener) actionListeners.elementAt(i);
            actionListener.actionPerformed(evt);
        }
    }
    
    public void addActionListener(ActionListener listener) {
        if (!actionListeners.contains(listener)) {
            actionListeners.add(listener);
        }
    }
    
    public void removeActionListener(ActionListener listener) {
        if (actionListeners.contains(listener)) {
            actionListeners.remove(listener);
        }
    }
    
    private int getLineIndex(Point point) {
        point.x = getWidth() + editor.getInsets().left;
        return editor.getLineForPoint(point);
    }
    
    
    /**
     * Indicates whether a break point can be set on a document element or not.
     * Break points cannot be set on:
     * <li>Empty lines.
     * <li>Comments (lines starting with '#').
     * <li>Procedure headers (lines starting with the 'procedure' keyword).
     * <li>End of procedure (lines starting with '}').
     *
     * @param elem a document element (from the DOM model).
     * @return true if a breakpoint can be set on this element, false if not.
     */
    boolean canSetBreakpoint(Element elem) {
        try {
            String text = elem.getDocument().getText(elem.getStartOffset(), elem.getEndOffset() - elem.getStartOffset()).trim();
            return !text.equals("") && !text.startsWith("#")
            && !text.toUpperCase().startsWith("PROCEDURE") && !text.startsWith("}");
        } catch (BadLocationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return false;
        }
    }
    
    /**
     * Implementation of the MouseListener interface.
     * A click onto the editor side panel creates or removes a breakpoint.
     * Each breakpoint is defined by document, document element and script name.
     *
     * @param e a MouseEvent
     */
    public void mousePressed(MouseEvent e) {
        addAction.setMouseEvent(e);
        removeAction.setMouseEvent(e);
        
        if (e.getButton() == MouseEvent.BUTTON3) {
            removeAllAction.updateAvailability();
            getPopUpMenu().show(this, e.getX(), e.getY());
            return;
        } else {
            if (removeAction.isEnabled()) {
                removeAction.actionPerformed(null);
            } else if (addAction.isEnabled()) {
                addAction.actionPerformed(null);
            }
        }
    }
    
    public void mouseReleased(MouseEvent e) {
    }
    
    public void mouseClicked(MouseEvent e) {
    }
    
    public void mouseEntered(MouseEvent e) {
    }
    
    public void mouseExited(MouseEvent e) {
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(EVENT_DOCUMENT_CHANGED)) {
            repaint();
        }
    }
    
    public void configurationChanged(ConfigurationChangeEvent evt) {
        if (evt.getPropertyName().equals(EDITOR_EXECUTED_LINE_COLOR) ||
                evt.getPropertyName().equals(EDITOR_BREAKPOINT_COLOR)) {
            reloadConfiguration();
            repaint();
        }
    }
    
    private void reloadConfiguration() {
        executionColor = editor.cfg.getColor(EDITOR_EXECUTED_LINE_COLOR);
        executionColor = executionColor == null ? Color.YELLOW : executionColor;
    }
    
    public void scriptEvent(ScriptEvent event) {
        if (event.getType() == ScriptEvent.SCRIPT_EXECUTED_LINE_CHANGED) {
            executedElement = (Element) event.getContext().get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT);
            repaint();
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("linePosition")) {
        } else if ("replayFinished".equals(evt.getPropertyName())) {
            executedElement = null;
            repaint();
        }
    }
    
    private void repaintLine(int lineIndex) {
        Rectangle r = null;
        try {
            r = editor.getRectangleForLine(lineIndex);
            r.x = 0;
            r.width = EditorGutter.this.getWidth();
            repaint(r);
        } catch (BadLocationException e1) {
            repaint();
        }
        
    }

    
    class BreakPoint {
        Element element;
        String scriptName;
    }
    
    class AddBreakPointAction extends AbstractAction {
        
        protected MouseEvent me;
        
        protected int lineIndex;
        
        protected Element elem;
        
        AddBreakPointAction() {
            String label = ApplicationSupport.getString("com.tplan.robot.gui.EditorSidePnl.addBreakPointMenuItem");
            putValue(LONG_DESCRIPTION, label);
            putValue(NAME, label);
        }
        
        public void actionPerformed(ActionEvent e) {
            if (!enabled) {
                return;
            }
            
            Map breakPoints = editor.breakPointTable;
            BreakPoint bp = new BreakPoint();
            bp.element = elem;
            breakPoints.put(elem, bp);
            fireActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "breakPointAdded", lineIndex));
            
            // Optimized - repaint just the area that has changed
            repaintLine(lineIndex);
            
        }
        
        public Element getLineOfCodeForPoint(MouseEvent evt) {
            if (evt == null || (lineIndex = getLineIndex(evt.getPoint())) < 0) {
                return null;
            }
            Element elem = DocumentUtils.getElementForLine(editor.getStyledDocument(), lineIndex, editor.getDocument().getDefaultRootElement());
            if (elem == null || !canSetBreakpoint(elem)) {
                return null;
            }
            return elem;
        }
        
        public void setMouseEvent(MouseEvent me) {
            this.me = me;
            elem = getLineOfCodeForPoint(me);
            setEnabled(elem != null && canPerformAction());
        }
        
        protected boolean canPerformAction() {
            Map breakPoints = editor.breakPointTable;
            Iterator e = breakPoints.keySet().iterator();
            Element el;
            while (e.hasNext()) {
                el =  (Element) e.next();
                if (el.getStartOffset() == elem.getStartOffset()) {
                    return false;
                }
            }
            return !breakPoints.containsKey(elem) && editor.canAddBreakpoint(me.getPoint());
        }
    }
    
    class RemoveBreakPointAction extends AddBreakPointAction {
        RemoveBreakPointAction() {
            String label = ApplicationSupport.getString("com.tplan.robot.gui.EditorSidePnl.removeBreakPointMenuItem");
            putValue(LONG_DESCRIPTION, label);
            putValue(NAME, label);
        }
        
        public void actionPerformed(ActionEvent e) {
            if (!enabled) {
                return;
            }
            
            Map breakPoints = editor.breakPointTable;
            if (breakPoints.containsKey(elem)) {
                breakPoints.remove(elem);
            } else {
                Iterator en = breakPoints.keySet().iterator();
                Element el;
                while (en.hasNext()) {
                    el =  (Element) en.next();
                    if (el.getStartOffset() == elem.getStartOffset()) {
                        breakPoints.remove(el);
                    }
                }
            }
            fireActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "breakPointRemoved", lineIndex));
            
            // Optimized - repaint just the area that has changed
            repaintLine(lineIndex);
        }
        
        protected boolean canPerformAction() {
            Map breakPoints = editor.breakPointTable;
            Iterator e = breakPoints.keySet().iterator();
            Element el;
            while (e.hasNext()) {
                el =  (Element) e.next();
                if (el.getStartOffset() == elem.getStartOffset()) {
                    return true;
                }
            }
            return breakPoints.containsKey(elem) && editor.canAddBreakpoint(me.getPoint());
        }
    }
    
    class RemoveAllBreakPointsAction extends AbstractAction {
        RemoveAllBreakPointsAction() {
            String label = ApplicationSupport.getString("com.tplan.robot.gui.EditorSidePnl.removeAllBreakPointsMenuItem");
            putValue(LONG_DESCRIPTION, label);
            putValue(NAME, label);
        }
        
        public void actionPerformed(ActionEvent e) {
            editor.breakPointTable.clear();
            fireActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "breakPointsCleared", 0));
            repaint();
        }
        
        public void updateAvailability() {
            setEnabled(editor.breakPointTable.size() > 0);
        }
    }
    
    public Font getLineNumberFont() {
        return lineNumberFont;
    }
    
    public void setLineNumberFont(Font lineNumberFont) {
        this.lineNumberFont = lineNumberFont;
    }
}
