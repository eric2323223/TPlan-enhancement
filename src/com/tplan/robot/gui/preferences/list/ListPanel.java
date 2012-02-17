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
package com.tplan.robot.gui.preferences.list;

import com.tplan.robot.ApplicationSupport;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Graphical component allowing to select a subset from a list of predefined
 * values and define their order of preference. It consists of two list components
 * and several controls (buttons) allowing to move the items between the lists and
 * to change their order. List items can be also moved and reordered through mouse
 * drags thanks to the functionality implemented in {@link DragAndDropList}.
 *
 * @product.signature
 */
public class ListPanel extends JPanel implements ActionListener, ListSelectionListener {

    private List values;
    private Map descriptions = new HashMap();
    private Map reversedDescs;
    JList listLeft;
    JScrollPane scrollLeft;
    JLabel labelLeft;
    JList listRight;
    JScrollPane scrollRight;
    JLabel labelRight;
    ResourceBundle res = ApplicationSupport.getResourceBundle();
//    JButton btnUp = new JButton(res.getString("options.dragList.btnUp"));
//    JButton btnDown = new JButton(res.getString("options.dragList.btnDown"));
//    JButton btnRight = new JButton(res.getString("options.dragList.btnRemove"));
//    JButton btnLeft = new JButton(res.getString("options.dragList.btnAdd"));
    JButton btnUp = new JButton(ApplicationSupport.getImageIcon("up9.gif"));
    JButton btnDown = new JButton(ApplicationSupport.getImageIcon("down9.gif"));
    JButton btnRight = new JButton(ApplicationSupport.getImageIcon("right9.gif"));
    JButton btnLeft = new JButton(ApplicationSupport.getImageIcon("left9.gif"));
    JPanel pnlButtons;

    public ListPanel() {
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
        if (scrollRight != null) {
            scrollRight.setOpaque(opaque);
        }
        if (listLeft != null) {
            listLeft.setOpaque(opaque);
        }
        if (scrollRight != null) {
            listRight.setOpaque(opaque);
        }
    }

    /**
     * Overriden to set the tool tip message to the components instead of the panel.
     * @param text toool tip message text (null sets off tool tips).
     */
    @Override
    public void setToolTipText(String text) {
        listLeft.setToolTipText(text);
        listRight.setToolTipText(text);
    }

    public void setVisibleRowCount(int rowCount) {
        listLeft.setVisibleRowCount(rowCount);
        listRight.setVisibleRowCount(rowCount);
    }

    private void init() {
        this.setLayout(new GridBagLayout());

        labelLeft = new JLabel();
        GridBagConstraints c;
        c = new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 1, 1);
        this.add(labelLeft, c);

        listLeft = new JList();
        listLeft.getSelectionModel().addListSelectionListener(this);
        listLeft.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollLeft = new JScrollPane(listLeft);

        labelRight = new JLabel(res.getString("options.dragList.available"));
        c = new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 1, 1);
        this.add(labelRight, c);

        listRight = new JList();
        listRight.getSelectionModel().addListSelectionListener(this);
        listRight.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollRight = new JScrollPane(listRight);

        c = new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 1, 1);
        this.add(scrollLeft, c);

        c = new GridBagConstraints(2, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 1, 1);
        this.add(scrollRight, c);

        pnlButtons = new JPanel(new GridBagLayout());
        c = new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1);
        this.add(pnlButtons, c);

        c = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1);
        pnlButtons.add(btnUp, c);
        Insets in = new Insets(0, 0, 0, 0);
        btnUp.addActionListener(this);
        btnUp.setMargin(in);
        btnUp.setBorderPainted(false);
        btnUp.setEnabled(false);

        c = new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1);
        pnlButtons.add(btnDown, c);
        btnDown.addActionListener(this);
        btnDown.setMargin(in);
        btnDown.setBorderPainted(false);
        btnDown.setEnabled(false);

        c = new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1);
        pnlButtons.add(btnRight, c);
        btnRight.addActionListener(this);
        btnRight.setMargin(in);
        btnRight.setBorderPainted(false);
        btnRight.setEnabled(false);

        c = new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1);
        pnlButtons.add(btnLeft, c);
        btnLeft.addActionListener(this);
        btnLeft.setMargin(in);
        btnLeft.setBorderPainted(false);
        btnLeft.setEnabled(false);
    }

    public List getValues() {
        List v = new ArrayList();
        DefaultListModel m = (DefaultListModel) listLeft.getModel();
        for (int i = 0; i < m.size(); i++) {
            v.add(reversedDescs.get(m.get(i)));
        }
        return v;
    }

    public void setValues(List values) {
        if (values == null) {
            this.values = null;
        } else {
            this.values = new ArrayList(values);
        }
        List excluded = new ArrayList(descriptions.keySet());
        DefaultListModel m = new DefaultListModel();
        Object enc;
        for (int i = 0; values != null && i < values.size(); i++) {
            enc = values.get(i);
            if (descriptions.containsKey(enc)) {
                m.addElement(descriptions.get(enc));
                excluded.remove(enc);
            }
        }
        listLeft.setModel(m);

        DefaultListModel m2 = new DefaultListModel();
        for (Object o : excluded) {
            m2.addElement(descriptions.get(o));
        }
        listRight.setModel(m2);

    }

    /**
     * Map with [value, displayName] pairs. If the table is populated, the lists
     * will show the display names instead of values.
     *
     * @param descriptions
     */
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

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        DefaultListModel m = (DefaultListModel) listLeft.getModel();
        DefaultListModel m2 = (DefaultListModel) listRight.getModel();
        if (src.equals(btnUp) || src.equals(btnDown)) {
            int index = listLeft.getSelectedIndex();
            Object o = m.remove(index);

            if (e.getSource().equals(btnUp)) {
                index--;
            } else if (e.getSource().equals(btnDown)) {
                index++;
            }
            m.add(index, o);
            listLeft.setSelectedIndex(index);
        } else if (src.equals(btnRight)) {
            int index = listLeft.getSelectedIndex();
            Object o = m.remove(index);
            m2.addElement(o);
            listRight.setSelectedValue(o, true);
        } else if (src.equals(btnLeft)) {
            int index = listRight.getSelectedIndex();
            Object o = m2.remove(index);
            m.addElement(o);
            listLeft.setSelectedValue(o, true);
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        int indexLeft = listLeft.getSelectedIndex();
        int indexRight = listRight.getSelectedIndex();
        if (e.getSource().equals(listLeft.getSelectionModel())) {
            btnUp.setEnabled(indexLeft > 0);
            btnDown.setEnabled(indexLeft >= 0 && indexLeft < (listLeft.getModel().getSize() - 1));
            btnRight.setEnabled(indexLeft >= 0);
        } else if (e.getSource().equals(listRight.getSelectionModel())) {
            btnLeft.setEnabled(indexRight >= 0);
        }
//
    }

    public Object getSelectedValueLeft() {
        Object o = listLeft.getSelectedValue();
        if (o != null) {
            if (reversedDescs != null && reversedDescs.containsKey(o)) {
                o = reversedDescs.get(o);
            }
            return o;
        }
        return null;
    }

    public Object getSelectedValueRight() {
        Object o = listRight.getSelectedValue();
        if (o != null) {
            if (reversedDescs != null && reversedDescs.containsKey(o)) {
                return reversedDescs.get(o);
            }
            return o;
        }
        return null;
    }

    public void setLabelLeft(String text) {
        labelLeft.setText(text);
    }

    public void setLabelRight(String text) {
        labelRight.setText(text);
    }

    public JList getListLeft() {
        return listLeft;
    }

    public JList getListRight() {
        return listRight;
    }
}

