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
package com.tplan.robot.gui.preferences;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.util.*;
import java.util.List;

/**
 * Top level component integrating a tree on the left and a display panel
 * on the right into a split pane. An instance of this component is displayed
 * by the {@link PreferenceDialog Preferences window}.
 * @product.signature
 */
public class PreferenceTreePane extends JSplitPane
        implements ChangeListener, TreeSelectionListener, PreferencePanel {

    /**
     * Tree of preferences and preference categories.
     */
    JTree tree = new JTree();
    /**
     * Preference tree model.
     */
    protected TreeModel model;
    /**
     * Validity of all preference data displayed by this dialog.
     */
    protected boolean valid;
    /**
     * Change status flag.
     */
    protected boolean changed = false;
    /**
     * User configuration, i.e. a container (usually a Map) with key-value pairs.
     */
    protected Object configuration;
    /**
     * Dialog width and height.
     */
    protected int width,  height;
    /** 
     * Vector of change listeners.
     */
    private transient Vector changeListeners;

    // Graphical components
    private GridBagLayout gbLayout = new GridBagLayout();
    JScrollPane scrollPaneLeft = new JScrollPane();
    JScrollPane scrollPaneRight = new JScrollPane();
    JPanel rightPanel = new JPanel();
    JPanel fillPanel = new JPanel();

    /**
     * Default and only constructor. It initializes only GUI components. The
     * tree model as well as the configuration object should be set through the
     * appropriate methods.
     */
    public PreferenceTreePane() {
        try {
            init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Initialize the components.
     */
    private void init() {

        // Init the left panel with the tree
        scrollPaneLeft.getViewport().add(tree, null);
        this.add(scrollPaneLeft, JSplitPane.LEFT);
        tree.addTreeSelectionListener(this);

        // Init the right display panel
        rightPanel.setLayout(gbLayout);
        rightPanel.add(fillPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        this.add(scrollPaneRight, JSplitPane.RIGHT);
        scrollPaneRight.getViewport().add(rightPanel, null);
    }

    /**
     * Sets the right panel of the dialog to a given panel and repaints it
     * @param panel a panel to be shown in the right part of the dialog
     */
    protected void setRightPanel(JComponent panel) {
        rightPanel.removeAll();
        if (panel != null) {
            rightPanel.add(panel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        }
        revalidate();
        repaint();
    }

    /**
     * Set the tree model for the left tree
     * @param model a tree model for the right tree
     */
    public void setTreeModel(TreeModel model) {
        this.model = model;
        tree.setModel(this.model);
        if (this.model.getRoot() != null && this.model.getRoot() instanceof TreeNode) {
            bindListeners((TreeNode) this.model.getRoot());
        }
        if (configuration != null) {
            loadPreferences(configuration);
        }
    }

    /**
     * Expand the tree to display all nodes.
     */
    public void expandTree() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * Load values of preferences displayed by this component from configuration.
     * It will recursively run through all tree nodes and call their
     * <code>loadPreferences()</code> methods.
     *
     * @param cfg a user configuration.
     */
    public void loadPreferences(Object cfg) {
        if (model != null) {
            try {
                TreeNode root = (TreeNode) model.getRoot();

                loadPreferences(root, cfg);
                tree.setMinimumSize(tree.getPreferredSize());
                rightPanel.setPreferredSize(new Dimension(width, height));
                setDividerLocation((int) tree.getPreferredSize().getWidth() + getDividerSize() + 5);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected synchronized void loadPreferences(TreeNode root, Object cfg) {
        if (root instanceof PreferenceTreeNode) {
            PreferenceTreeNode node = (PreferenceTreeNode) root;

            if (node.getDisplayComponent() instanceof PreferencePanel) {
                PreferencePanel panel = (PreferencePanel) node.getDisplayComponent();

                panel.loadPreferences(cfg);

                // Find out the biggest panel dimensions
                if (panel instanceof JComponent) {
                    Dimension dim = ((JComponent) panel).getPreferredSize();
                    height = Math.max(height, (int) dim.getHeight());
                    width = Math.max(width, (int) dim.getWidth());
                }
            }
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            loadPreferences(root.getChildAt(i), cfg);
        }
    }

    /**
     * Get the preference values from the all components and save them into
     * the configuration. It will recursively run through all tree nodes and call their
     * <code>savePreferences()</code> methods.
     *
     * @return configuration object the preferences were saved to.
     */
    public Object savePreferences() {
        if (model != null && configuration != null) {
            try {
                TreeNode root = (TreeNode) model.getRoot();

                if (root != null) {
                    savePreferences(root, configuration);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return configuration;
    }

    protected synchronized void savePreferences(TreeNode root, Object Values) {
        if (root instanceof PreferenceTreeNode) {
            PreferenceTreeNode myNode = (PreferenceTreeNode) root;

            if (myNode.getDisplayComponent() != null && myNode.getDisplayComponent() instanceof PreferencePanel) {
                PreferencePanel panel = (PreferencePanel) myNode.getDisplayComponent();

                try {
                    panel.savePreferences(Values);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            savePreferences(root.getChildAt(i), Values);
        }
    }

    public void savePreferences(Object values) {
        savePreferences((TreeNode) model.getRoot(), values);
    }

    /**
     * Register this component as listener of all events fired by the preference
     * tree components.
     *
     * @param root a tree root.
     */
    protected synchronized void bindListeners(TreeNode root) {
        if (root instanceof PreferenceTreeNode) {
            PreferenceTreeNode myNode = (PreferenceTreeNode) root;
            JComponent panel = myNode.getDisplayComponent();

            if (panel != null && panel instanceof PreferencePanel) {
                ((PreferencePanel) panel).addChangeListener(this);
            }
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            bindListeners(root.getChildAt(i));
        }
    }

    /**
     * Run recursively through the tree and find out if any of the panels is in
     * incorrect state.
     *
     * @return true if everything is ok, false if any of the panels is incorrect
     * @param root a tree node to start the recursive operation with.
     */
    protected boolean isValid(TreeNode root) {
        if (root instanceof PreferenceTreeNode) {
            valid = valid && ((PreferenceTreeNode) root).isContentValid();
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            isValid(root.getChildAt(i));
        }
        return valid;
    }

    /**
     * Run recursively through the tree and find out if any of the panels is in
     * incorrect state.
     *
     * @param root a tree node to start the recursive operation with.
     * @return true if everything is ok, false if any of the panels is incorrect
     */
    protected boolean isChanged(TreeNode root) {
        if (root instanceof PreferenceTreeNode) {
            Object panel = ((PreferenceTreeNode) root).getDisplayComponent();

            if (panel instanceof PreferencePanel) {
                changed = changed || ((PreferencePanel) panel).isChanged();
            }
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            isChanged(root.getChildAt(i));
        }
        return changed;
    }

    /**
     * Run recursively through the tree and call clean up.
     */
    public void destroy() {
        destroy((TreeNode) model.getRoot());
        tree.removeTreeSelectionListener(this);
        scrollPaneLeft.getViewport().remove(tree);
        changeListeners = new Vector();
        model = null;
    }

    /**
     * Run recursively through the tree and call clean up.
     * @param root a tree node to start the recursive operation with.
     */
    protected void destroy(TreeNode root) {
        if (root instanceof PreferenceTreeNode) {
            Object panel = ((PreferenceTreeNode) root).getDisplayComponent();

            if (panel instanceof PreferencePanel) {
                ((PreferencePanel) panel).destroy();
            }
            ((PreferenceTreeNode) root).setDisplayComponent(null);
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            destroy(root.getChildAt(i));
        }
    }

    /**
     * Implementation of the ChangeListener.
     * Invoked when the target of the listener changes its state.
     * @param e  a ChangeEvent object
     */
    public void stateChanged(ChangeEvent e) {
        changed = true;
        valid = true;
        valid = isValid((TreeNode) model.getRoot());
        tree.repaint();
        fireStateChanged(new ChangeEvent(this));
    }

    /**
     * Set the object with dialog values. If the argument is not null, the method
     * setDisplayValues() is fired.
     * @param cfg object with configuration values for the dialog components.
     */
    public void setConfiguration(Object cfg) {
        configuration = cfg;
        if (cfg != null) {
            loadPreferences(cfg);
        }
    }

    /**
     * Implementation of the TreeSelectionListener. Called whenever
     * the selection changes. This method switches content of the right panel.
     * @param e the event that characterizes the change.
     */
    public void valueChanged(TreeSelectionEvent e) {
        if (e.getPath() == null) {
            setRightPanel(null);
            return;
        }
        TreeNode node = (TreeNode) e.getPath().getLastPathComponent();
        JComponent panel = null;

        if (node != null && node instanceof PreferenceTreeNode) {
            panel = ((PreferenceTreeNode) node).getDisplayComponent();
        }
        setRightPanel(panel);
    }

    /**
     * Get the state of the dialog.
     * @return true if there's been any change in the dialog, false otherwise
     */
    public boolean isChanged() {
        return changed || isChanged((TreeNode) model.getRoot());
    }

    /**
     * Returns true if all panels referenced by the nodes of the option tree
     * contain just correct values.
     * @return true if all panels referenced by the nodes of the option tree
     * contain just correct values, false otherwise.
     */
    public boolean isContentValid() {
        return valid;
    }

    public synchronized void removeChangeListener(ChangeListener l) {
        if (changeListeners != null && changeListeners.contains(l)) {
            Vector v = (Vector) changeListeners.clone();

            v.removeElement(l);
            changeListeners = v;
        }
    }

    public synchronized void addChangeListener(ChangeListener l) {
        Vector v = changeListeners == null ? new Vector(2) : (Vector) changeListeners.clone();

        if (!v.contains(l)) {
            v.addElement(l);
            changeListeners = v;
        }
    }

    protected void fireStateChanged(ChangeEvent e) {
        if (changeListeners != null) {
            Vector listeners = changeListeners;
            int count = listeners.size();

            for (int i = 0; i < count; i++) {
                ((ChangeListener) listeners.elementAt(i)).stateChanged(e);
            }
        }
    }

    /**
     * Search the tree for the first node with a particular label and select it.
     * The search is not case sensitive.
     *
     * @param nodeLabel node label (the string displayed by the node in GUI).
     * @param rootLabel node label to use as a root of the search. If this argument
     * is null, the whole tree will be searched. This argument is supposed to
     * allow to narrow down the search for the case that there are more nodes
     * with the same label in the tree.
     * @return true if the tree node was found and selected and false otherwise.
     */
    public boolean selectNodeByLabel(String nodeLabel, String rootLabel) {
        TreeNode root = (TreeNode) model.getRoot();
        if (rootLabel != null) {
            root = findNode(rootLabel, root, false);
        }

        if (root != null) {
            TreeNode node = findNode(nodeLabel, root, false);
            if (node != null) {

                // Construct the TreePath object
                List v = new ArrayList();

                v.add(0, node);
                while (node.getParent() != null) {
                    node = node.getParent();
                    v.add(0, node);
                }

                // Set the selection path
                tree.setSelectionPath(new TreePath(v.toArray()));
                return true;
            }
        }
        return false;
    }

    /**
     * Find a node with the given name in the subtree defined by the root node.
     * Local use only.
     * @param root a tree node to start the recursive operation with.
     * @param nodeName name of the node to be found.
     * @param caseSensitive whether the search should be case sensitive or not.
     * @return the node of the specified name or null if not found.
     */
    protected TreeNode findNode(String nodeName, TreeNode root, boolean caseSensitive) {
        if (root == null || (caseSensitive && root.toString().equals(nodeName)) || (!caseSensitive && root.toString().equalsIgnoreCase(nodeName))) {
            return root;
        } else {
            for (int i = 0; i < root.getChildCount(); i++) {
                TreeNode child = root.getChildAt(i);
                TreeNode node = findNode(nodeName, child, caseSensitive);

                if (node != null) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Set on or off displaying of preference keys in tool tips. Added in 2.0.1.
     * @param show true sets on, false off.
     */
    public void setShowKeysInToolTips(boolean show) {
        setShowKeysInToolTips(tree.getModel().getRoot(), show);
    }

    protected void setShowKeysInToolTips(Object root, boolean show) {
        TreeNode n = (TreeNode) root;
        if (root instanceof PreferenceTreeNode) {
            JComponent c = ((PreferenceTreeNode) n).getDisplayComponent();
            if (c instanceof DefaultPreferencePanel) {
                ((DefaultPreferencePanel) c).setShowKeysInToolTips(show);
            }
        }
        for (int i = 0; i < n.getChildCount(); i++) {
            setShowKeysInToolTips(n.getChildAt(i), show);
        }
    }

    /**
     * Void implementation to satisfy the PreferencePanel interface
     * @param configuration
     */
    public void resetToDefaults(Object configuration) {
    }
}
