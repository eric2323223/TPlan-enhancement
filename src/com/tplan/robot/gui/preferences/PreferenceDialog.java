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

import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.util.Utils;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.UserConfiguration;

import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * <p> User Preferences window. It is just a simple dialog containing
 * an instance of the {@link PreferenceTreePane} class.
 * @product.signature
 */
public class PreferenceDialog extends JDialog implements ChangeListener, ActionListener, ItemListener {

    /** Option component. */
    PreferenceTreePane optionPane = new PreferenceTreePane();
    JPanel pnlSouth = new JPanel();
    JButton btnOK = new JButton();
    JButton btnCancel = new JButton();
    JButton btnHelp = new JButton();
    JCheckBox cbShowKeys = new JCheckBox();
    MainFrame mainFrame;

    public PreferenceDialog(MainFrame parent, String title, boolean modal) {
        super(parent, title, modal);
        this.mainFrame = parent;
        try {
            init(parent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Implementation of the ChangeListener interface. The method gets called
     * when an option component changes its state, typically when user changes
     * a value in the dialog.
     * @param e a ChangeEvent
     */
    public void stateChanged(ChangeEvent e) {
        btnOK.setEnabled(true);
    }

    /**
     * Get the currently selected node label.
     * @return currently selected node label or null if there's no selection.
     */
    public String getSelectedNodeLabel() {
        if (optionPane.tree.getSelectionPath() != null) {
            return ((DefaultMutableTreeNode) optionPane.tree.getSelectionPath().getLastPathComponent()).getUserObject().toString();
        }
        return null;
    }

    /**
     * Initialize the components. 
     * @throws Exception when initialization of a component fails
     */
    void init(MainFrame parent) throws Exception {
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        // Initialize the option tree and pass the user configuration to the component
        optionPane.setTreeModel(new DefaultPreferenceTreeModel(parent.getScriptHandler(), parent));
        optionPane.setConfiguration(mainFrame.getUserConfiguration());
        optionPane.setDividerSize(4);
        optionPane.expandTree();

        pnlSouth.setLayout(new GridBagLayout());

        int x = 0;
        GridBagConstraints c = new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 5, 1, 1), 0, 0);
        cbShowKeys.setText(res.getString("options.showKeysInToolTipsText"));
        cbShowKeys.setToolTipText(res.getString("options.showKeysInToolTipsTT"));
        cbShowKeys.addItemListener(this);
        pnlSouth.add(cbShowKeys, c);

        JPanel pnlFill = new JPanel();
        c = new GridBagConstraints(x++, 0, 1, 1, 1.0, 1.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(1, 5, 1, 1), 0, 0);
        pnlSouth.add(pnlFill, c);

        btnOK.setText(res.getString("btnOk"));
        btnOK.addActionListener(this);
        c = new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 5, 1, 1), 0, 0);
        pnlSouth.add(btnOK, c);
        getRootPane().setDefaultButton(btnOK);

        btnCancel.setText(res.getString("btnCancel"));
        btnCancel.addActionListener(this);
        c = new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 5, 1, 1), 0, 0);
        pnlSouth.add(btnCancel, c);

        btnHelp.setText(res.getString("btnHelp"));
        btnHelp.addActionListener(this);
        btnHelp.setEnabled(mainFrame.isHelpAvailable());
        c = new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 5, 1, 1), 0, 0);
        pnlSouth.add(btnHelp, c);

        getContentPane().add(optionPane, BorderLayout.CENTER);
        getContentPane().add(pnlSouth, BorderLayout.SOUTH);

        pack();
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Now make the window 2% larger than the computed size
        Dimension dim = getSize();
        final int INSET = 0;
        final float WIDTH_PADDING_PERCENTAGE = 1.02f;
        final int increasedWidth = (int) (WIDTH_PADDING_PERCENTAGE * dim.getWidth());

        setSize(increasedWidth, (int) dim.getHeight() + INSET);
        setLocationRelativeTo(parent);
        Utils.registerDialogForEscape(this, btnCancel);
    }

    /**
     * Search the tree for the first node with a particular label and select it.
     *
     * @param panelName node label (the string displayed by the node in GUI).
     * A value of null will clear up the tree selection.
     * @param parentName node label to use as a root of the search. If this argument
     * is null, the whole tree will be searched. This argument is supposed to
     * allow to narrow down the search for the case that there are more nodes
     * with the same label in the tree.
     */
    public void setSelectedPanel(String panelName, String parentName) {
        if (panelName != null) {
            optionPane.selectNodeByLabel(panelName, parentName);
        } else {
            optionPane.tree.clearSelection();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnCancel)) {
            dispose();
        } else if (e.getSource().equals(btnOK)) {
            optionPane.savePreferences(mainFrame.getUserConfiguration());
            UserConfiguration.saveConfiguration();
            dispose();
        } else if (e.getSource().equals(btnHelp)) {
            mainFrame.showHelpDialog("gui.preferences", this);
        }
    }

    public void resetValues() {
        optionPane.setConfiguration(mainFrame.getUserConfiguration());
        itemStateChanged(null);
    }

    public void itemStateChanged(ItemEvent e) {
        if (e == null || e.getSource().equals(cbShowKeys)) {
            optionPane.setShowKeysInToolTips(cbShowKeys.isSelected());
        }
    }
}

