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
package com.tplan.robot.gui.components;

import com.tplan.robot.ApplicationSupport;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Graphical component allowing to define and maintain a list of objects.
 *
 * @product.signature
 */
public abstract class ItemListPanel extends JPanel implements ActionListener, ListSelectionListener {

    protected Map descriptions = new HashMap();
    protected Map reversedDescs = new HashMap();
    JList list = new JList();
    JScrollPane scrollLeft = new JScrollPane(list);
    JLabel labelLeft;
    ResourceBundle res = ApplicationSupport.getResourceBundle();
    JButton btnUp = new JButton(ApplicationSupport.getImageIcon("up9.gif"));
    JButton btnDown = new JButton(ApplicationSupport.getImageIcon("down9.gif"));
    JButton btnAdd = new JButton(ApplicationSupport.getImageIcon("plus9.gif"));
    JButton btnRemove = new JButton(ApplicationSupport.getImageIcon("minus9.gif"));
    JPanel pnlButtons = new JPanel(new GridBagLayout());
    /**
     * This is property name of the vetoable change event that this component fires before
     * adding an item to the list. Objects wishing to filter the item list may
     * listen for these events through the {@link VetoableChangeListener}
     * interface and throw a {@link PropertyVetoException} to prevent the item
     * from being added to the list.
     */
    public static final String VETOABLE_ITEM_ADD_EVENT = "itemWillBeAdded";

    public ItemListPanel() {
        init();
    }

    @Override
    public void setOpaque(boolean opaque) {
        super.setOpaque(opaque);
        if (pnlButtons != null) {
            pnlButtons.setOpaque(opaque);
        }
        if (scrollLeft != null) {
            scrollLeft.setOpaque(opaque);
        }
        if (list != null) {
            list.setOpaque(opaque);
        }
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        list.setToolTipText(text);
    }

    public void setVisibleRowCount(int lineCount) {
        list.setVisibleRowCount(lineCount);
    }

    public JList getList() {
        return list;
    }

    public JButton getAddButton() {
        return btnAdd;
    }

    public void addCustomButton(Component component, int gridX, int gridY) {
        pnlButtons.add(component, new GridBagConstraints(gridX, gridY, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1));
    }

    public JButton getRemoveButton() {
        return btnRemove;
    }

    public Object getSelectedItem() {
        int index = list.getSelectedIndex();
        if (index >= 0) {
            return reversedDescs.get(list.getSelectedValue());
        }
        return null;
    }

    private void init() {
        this.setLayout(new GridBagLayout());

        labelLeft = new JLabel();
        GridBagConstraints c;
        c = new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 1, 1);
        this.add(labelLeft, c);

        btnUp.setMargin(new Insets(0, 0, 0, 0));
        btnUp.setBorderPainted(false);
        btnUp.setToolTipText(ApplicationSupport.getString("itemListPanel.btnUp"));
        btnDown.setMargin(new Insets(0, 0, 0, 0));
        btnDown.setBorderPainted(false);
        btnDown.setToolTipText(ApplicationSupport.getString("itemListPanel.btnDown"));
        btnAdd.setMargin(new Insets(0, 0, 0, 0));
        btnAdd.setBorderPainted(false);
        btnAdd.setToolTipText(ApplicationSupport.getString("itemListPanel.btnAdd"));
        btnRemove.setMargin(new Insets(0, 0, 0, 0));
        btnRemove.setBorderPainted(false);
        btnRemove.setToolTipText(ApplicationSupport.getString("itemListPanel.btnRemove"));

        list.getSelectionModel().addListSelectionListener(this);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setModel(new DefaultListModel());

        c = new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 1, 1);
        this.add(scrollLeft, c);

        c = new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1);
        this.add(pnlButtons, c);

        c = new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1);
        pnlButtons.add(btnUp, c);
        btnUp.addActionListener(this);
        btnUp.setEnabled(false);

        c = new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1);
        pnlButtons.add(btnDown, c);
        btnDown.addActionListener(this);
        btnDown.setEnabled(false);

        c = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1);
        pnlButtons.add(btnAdd, c);
        btnAdd.addActionListener(this);
        btnAdd.setEnabled(true);

        c = new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1);
        pnlButtons.add(btnRemove, c);

        btnRemove.addActionListener(this);
        btnRemove.setEnabled(false);
        list.setVisibleRowCount(3);
    }

    public List getValues() {
        List v = new ArrayList();
        ListModel m = (ListModel) list.getModel();
        for (int i = 0; i < list.getModel().getSize(); i++) {
            v.add(reversedDescs.get(m.getElementAt(i)));
        }
        return v;
    }

    public void setValues(List values) {
        if (values != null) {
            List excluded = new ArrayList(descriptions.keySet());
            DefaultListModel m = new DefaultListModel();
            Object enc;
            for (int i = 0; i < values.size(); i++) {
                enc = values.get(i);
                if (descriptions.containsKey(enc)) {
                    m.addElement(descriptions.get(enc));
                    excluded.remove(enc);
                }
            }
            list.setModel(m);
        }
    }

    public void setDisplayValuesTable(Map descriptions) {
        this.descriptions = descriptions;
        if (descriptions != null) {
            reversedDescs = new HashMap();
            Iterator e = descriptions.keySet().iterator();
            Object key;
            while (e.hasNext()) {
                key = e.next();
                reversedDescs.put(descriptions.get(key), key);
            }
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        list.setEnabled(enabled);
        updateButtonState();
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        DefaultListModel m = (DefaultListModel) list.getModel();
        if (src.equals(btnUp) || src.equals(btnDown)) {
            int index = list.getSelectedIndex();
            Object o = m.remove(index);

            if (e.getSource().equals(btnUp)) {
                index--;
            } else if (e.getSource().equals(btnDown)) {
                index++;
            }
            m.add(index, o);
            list.setSelectedIndex(index);
            firePropertyChange("parametersChanged", this, list);
        } else if (src.equals(btnAdd)) {
            Object o[] = addItem();
            if (o != null) {
                for (int i = 0; i < o.length; i = i + 2) {

                    // Fire a vetoable change event to allow item filtering by external objects
                    try {
                        fireVetoableChange(VETOABLE_ITEM_ADD_EVENT, null, o[i]);
                    } catch (PropertyVetoException ex) {
                        continue;  // Veto fired -> do not add the item and proceed to the next one
                    }
                    descriptions.put(o[i], o[i + 1]);
                    reversedDescs.put(o[i + 1], o[i]);
                    m.add(m.size(), o[i + 1]);
                    list.setSelectedValue(o[i + 1], true);
                }
            }
            firePropertyChange("parametersChanged", this, list);
        } else if (src.equals(btnRemove)) {
            int index = list.getSelectedIndex();
            Object o = m.remove(index);
            if (m.size() > 0) {
                list.setSelectedValue(list.getModel().getElementAt(Math.min(index, m.size() - 1)), true);
            }
            firePropertyChange("parametersChanged", this, list);
        }
    }

    public abstract Object[] addItem();

    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource().equals(list.getSelectionModel())) {
            updateButtonState();
        }
    }

    private void updateButtonState() {
        int indexLeft = list.getSelectedIndex();
        btnUp.setEnabled(this.isEnabled() && indexLeft > 0);
        btnDown.setEnabled(this.isEnabled() && indexLeft >= 0 && indexLeft < (list.getModel().getSize() - 1));
        btnRemove.setEnabled(this.isEnabled() && indexLeft >= 0);
        btnAdd.setEnabled(this.isEnabled());
    }

    public void setListDescription(String text) {
        if (text == null) {
            labelLeft.setVisible(false);
        } else {
            labelLeft.setText(text);
            labelLeft.setVisible(true);
        }
    }
}

