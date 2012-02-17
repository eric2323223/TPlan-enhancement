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
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.gui.editor.Editor;
import com.tplan.robot.gui.editor.EditorPnl;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.Map;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * GUI component displaying a tree of script errors.
 * @product.signature
 */
public class ValidationMessagePanel extends JPanel implements TreeSelectionListener, MouseListener, ActionListener {

    JTree tree = new JTree();
    JScrollPane scroll = new JScrollPane();
    private List messageVector;
    private Map nodeTable = new HashMap();
    private MainFrame mainFrame;
    private JPopupMenu popupMenu;

    ValidationMessagePanel(MainFrame mainFrame) {
        init();
        this.mainFrame = mainFrame;
    }

    private JPopupMenu getPopupMenu() {
        if (popupMenu == null) {
            popupMenu = new JPopupMenu();
            JMenuItem item = new JMenuItem(ApplicationSupport.getResourceBundle().getString("compileErrorTree.menuItem.hide"));
            item.addActionListener(this);
            popupMenu.add(item);
        }
        return popupMenu;
    }

    public void setMessageVector(List messageVector) {
        setMessageVector(messageVector, true);
    }

    public void setMessageVector(List messageVector, boolean clear) {
        DefaultTreeModel model;
        DefaultMutableTreeNode root;
        if (clear) {
            this.messageVector = messageVector;
            root = new DefaultMutableTreeNode(ApplicationSupport.getResourceBundle().getString("compileErrorTree.rootNode"));
            model = new DefaultTreeModel(root);
            nodeTable.clear();
        } else {
            this.messageVector.addAll(messageVector);
            model = (DefaultTreeModel) tree.getModel();
            root = (DefaultMutableTreeNode) model.getRoot();
        }

        SyntaxErrorException e;
        String file;
        DefaultMutableTreeNode fileNode, msgNode;

        if (messageVector != null && messageVector.size() > 0) {

            for (int i = 0; i < messageVector.size(); i++) {
                if (messageVector.get(i) instanceof SyntaxErrorException) {
                    e = (SyntaxErrorException) messageVector.get(i);
                    file = e.getScriptFile() == null ? ApplicationSupport.getResourceBundle().getString("compileErrorTree.unknownFile") : e.getScriptFile().getAbsolutePath();
                    fileNode = (DefaultMutableTreeNode) nodeTable.get(file);
                    if (fileNode == null) {
                        fileNode = new DefaultMutableTreeNode(file);
                        root.add(fileNode);
                        nodeTable.put(file, fileNode);
                    }
                    String key = file + ":" + e.getLineIndex();
                    if (!nodeTable.containsKey(key)) {
                        msgNode = new MsgTreeNode(MessageFormat.format(ApplicationSupport.getResourceBundle().getString("compileErrorTree.errorTemplate"), e.getLineIndex(), e.getMessage()), e);
                        nodeTable.put(key, msgNode);
                        fileNode.add(msgNode);
                    }
                }
            }
        }
        tree.setModel(model);
        expandTree();
    }

    private void init() {
        this.setLayout(new BorderLayout());
        this.add(scroll, BorderLayout.CENTER);
        scroll.getViewport().add(tree);
        tree.addTreeSelectionListener(this);
        tree.addMouseListener(this);
        setMessageVector(new ArrayList());
        this.addMouseListener(this);
    }

    public Dimension getPreferredSize() {
        return new Dimension(50, 200);
    }

    public Dimension getMinimumSize() {
        return new Dimension(50, 200);
    }

    /**
     * Expand the dialog tree
     */
    public void expandTree() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    public void valueChanged(TreeSelectionEvent e) {
        if (e.getPath().getLastPathComponent() instanceof MsgTreeNode) {
            MsgTreeNode node = (MsgTreeNode) e.getPath().getLastPathComponent();
            SyntaxErrorException ex = node.getMessage();
            if (ex.getScriptFile() != null) {
                boolean editorOpen = mainFrame.getDocumentTabbedPane().isFileOpen(ex.getScriptFile());
                try {
                    EditorPnl ed = mainFrame.getDocumentTabbedPane().getEditorForFile(ex.getScriptFile());
                    Editor editor = ed.getEditor();
                    if (!editorOpen) {
                        editor.validate(messageVector);
                    }
                    editor.scrollElementToVisible(ex.getElement());
                    editor.requestFocus();
                    editor.select(ex.getElement().getStartOffset(), ex.getElement().getEndOffset());
                    editor.setCaretPosition(ex.getElement().getStartOffset());
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu menu = getPopupMenu();
            if (menu.getComponentCount() > 0) {
                menu.show(this, e.getX(), e.getY());
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.setMessagePaneVisible(false);
    }

    private class MsgTreeNode extends DefaultMutableTreeNode {

        private SyntaxErrorException msg;

        MsgTreeNode(Object valueObject, SyntaxErrorException e) {
            super(valueObject);
            msg = e;
        }

        SyntaxErrorException getMessage() {
            return msg;
        }
    }
}
