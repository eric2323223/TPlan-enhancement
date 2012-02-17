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
package com.tplan.robot.gui.preferences.styles;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.preferences.AbstractPreferencePanel;
import com.tplan.robot.gui.preferences.color.ColorPanel;
import com.tplan.robot.gui.editor.CustomStyledDocument;
import com.tplan.robot.gui.MainFrame;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Preference panel with editor styles. It defines how
 * {@link com.tplan.robot.gui.editor.Editor} instances highlight syntax of
 * individual commands and elements of the {@doc.spec}.
 * @product.signature
 */
public class StylePanel extends AbstractPreferencePanel implements ActionListener, ListSelectionListener, ItemListener {

    ResourceBundle res = ApplicationSupport.getResourceBundle();
    JList list = new JList();
    JScrollPane scroll;
    JPanel pnlEffects;
    String paramBase;

    JLabel lblFontType = new JLabel(res.getString("options.style.fontType"));
    JCheckBox chbBold = new JCheckBox(res.getString("options.style.fontBold"));
    JCheckBox chbItalic = new JCheckBox(res.getString("options.style.fontItalic"));

    JCheckBox chbForeground = new JCheckBox(res.getString("options.style.foreground"));
    JCheckBox chbBackground = new JCheckBox(res.getString("options.style.background"));

    ColorPanel cpnForeground;
    ColorPanel cpnBackground;

    JLabel lblEffect = new JLabel(res.getString("options.style.effect"));
    JComboBox cbEffect = new JComboBox();

    Map params = new HashMap();
    Map<JComponent, String> components = new HashMap();

    JTextPane txpSample = new JTextPane();

    boolean firstInit = true;

    MainFrame frame;

    UserConfiguration cfgCopy;

    public StylePanel(List listItems, String paramBase, Frame frame) {
        this.paramBase = paramBase;
        cpnForeground = new ColorPanel();
        cpnBackground = new ColorPanel();
        this.frame = (MainFrame) frame;
        cfgCopy = this.frame.getUserConfiguration().getCopy();
        init(listItems);
    }

    private void init(List listItems) {
        this.setLayout(new GridBagLayout());

        GridBagConstraints c;

        // Item list initialization
        list.getSelectionModel().addListSelectionListener(this);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultListModel m = new DefaultListModel();
        String enc;
        for (int i = 0; listItems != null && i < listItems.size(); i++) {
            enc = (String) listItems.get(i);
            m.addElement(enc);
        }
        list.setModel(m);

        // Add the list to the panel
        scroll = new JScrollPane(list);
        c = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 1, 1);
        this.add(scroll, c);


        // Initialize the effect panel
        pnlEffects = new JPanel(new GridBagLayout());
        GridBagConstraints ec;

        ec = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 1, 1);
        pnlEffects.add(lblFontType, ec);

        chbBold.addItemListener(this);
        ec = new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 1, 1);
        pnlEffects.add(chbBold, ec);

        chbItalic.addItemListener(this);
        ec = new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 1, 1);
        pnlEffects.add(chbItalic, ec);

        chbForeground.addItemListener(this);
        ec = new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(10, 1, 1, 1), 1, 1);
        pnlEffects.add(chbForeground, ec);

        cpnForeground.cmbColors.addActionListener(this);
        ec = new GridBagConstraints(1, 1, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 1, 1, 1), 1, 1);
        pnlEffects.add(cpnForeground, ec);

        chbBackground.addItemListener(this);
        ec = new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(10, 1, 1, 1), 1, 1);
        pnlEffects.add(chbBackground, ec);

        cpnBackground.cmbColors.addActionListener(this);
        ec = new GridBagConstraints(1, 2, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 1, 1, 1), 1, 1);
        pnlEffects.add(cpnBackground, ec);

        // Add the sample text pane
        CustomStyledDocument doc = new CustomStyledDocument(frame.getScriptHandler(), cfgCopy);
        txpSample.setDocument(doc);
        txpSample.setEditable(false);
        txpSample.setText("# This is a comment\nprocedure type_argument {\n   Type \"argument\" count=2 wait=2s\n}");
        JPanel pnlPreview = new JPanel(new BorderLayout());
        pnlPreview.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), res.getString("options.style.stylePreview")));
        pnlPreview.add(txpSample, BorderLayout.CENTER);

        ec = new GridBagConstraints(0, 3, 3, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 1, 1, 1), 1, 1);
        pnlEffects.add(pnlPreview, ec);

        // Add the effect panel to the panel
        c = new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 1, 1);
        this.add(pnlEffects, c);

        c = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 1, 1);
    }

    /**
     * This method gets invoked when list selection changes. It is then necessary to set all the properties of
     * the style in the right panel.
     *
     * @param e list selection event
     */
    public void valueChanged(ListSelectionEvent e) {
        int index = list.getSelectedIndex();
        String keyBase = paramBase + "." + index + ".";

        setCheckBox(chbBold, keyBase + "0");
        setCheckBox(chbItalic, keyBase + "1");
        setColorBox(chbForeground, cpnForeground, keyBase + "2", keyBase + "3");
        setColorBox(chbBackground, cpnBackground, keyBase + "4", keyBase + "5");

    }

    private void setCheckBox(JCheckBox box, String key) {
        Object o = params.get(key);
        if (o != null && o instanceof Boolean) {
            box.setSelected(((Boolean) o).booleanValue());
        } else {
            box.setSelected(false);
        }
    }

    private void setColorBox(JCheckBox box, ColorPanel clrPnl, String boxKey, String colorKey) {
        setCheckBox(box, boxKey);

        Object o = params.get(colorKey);
        if (o != null && o instanceof Color) {
            clrPnl.setSelectedColor((Color) o);
        } else {
            clrPnl.cmbColors.setSelectedIndex(0);
        }
    }

    /**
     * Set the values from the valuesObject in the panel components. When you
     * override the method, please call the superclass method IN THE END
     * of the new method. It ensures that after the values are set,
     * the flag <code>changed</code> will be correctly set to false.
     *
     * @param valuesObject object with values for the dialog components (usually
     *                     representation of user configuration).
     */
    public void loadPreferences(Object valuesObject) {
        super.loadPreferences(valuesObject);

        UserConfiguration cfg = (UserConfiguration) valuesObject;
        params = new HashMap();

        String key;
        Object value = null;
        for (int i = 0; i < list.getModel().getSize(); i++) {
            for (int j = 0; j <= 6; j++) {
                key = paramBase + "." + i + "." + j;
                switch (j) {
                    case 0:
                    case 1:
                    case 2:
                    case 4:
                        value = cfg.getBoolean(key);
                        break;
                    case 3:
                    case 5:
                        value = cfg.getColor(key);
                        break;
                    case 6:
                        value = cfg.getString(key);
                        break;
                    default:
                        value = null;
                        break;
                }
                if (value != null) {
                    params.put(key, value);
//                    System.out.println("loadPreferences(): read key " + key + ", value " + value);
                }
            }
        }
        if (firstInit) {
            list.setSelectedIndex(0);
            firstInit = false;
        }
    }

    /**
     * Get the values from the panel components and save them into
     * the <code>valuesObject</code>. Here does nothing.
     *
     * @param valuesObject object with values for the dialog components (usually
     *                     representation of user configuration).
     */
    public void savePreferences(Object valuesObject) {
        UserConfiguration cfg = (UserConfiguration) valuesObject;
        if (changed) {
            Iterator e = params.keySet().iterator();
            String key;
            Object value;
            while (e.hasNext()) {
                key = (String) e.next();
                value = params.get(key);
                if (value instanceof Boolean) {
                    cfg.setBoolean(key, (Boolean)value);
                } else if (value instanceof Color) {
                    cfg.setColors(key, new Color[] {(Color)value});
                } else if (value instanceof String) {
                    cfg.setString(key, (String)value);
                }
            }
        }
    }

    /**
     * Gets invoked when a check box is selected or deselected. We have to update the corresponding configuration flag.
     *
     * @param e an item event describing the change
     */
    public void itemStateChanged(ItemEvent e) {
        parameterUpdated(e.getSource());
    }

    public void actionPerformed(ActionEvent e) {
        parameterUpdated(e.getSource());
    }

    private void parameterUpdated(Object src) {
        int index = list.getSelectedIndex();
        if (index >= 0) {
            changed = true;
            String keyBase = paramBase + "." + index + ".";
            String key = null;

            if (src instanceof JCheckBox) {
                if (src.equals(chbBold)) {
                    key = keyBase + "0";
                } else if (src.equals(chbItalic)) {
                    key = keyBase + "1";
                } else if (src.equals(chbForeground)) {
                    key = keyBase + "2";
                    if (chbForeground.isSelected()) {
//                        parameterUpdated(cpnForeground);
                    }
                } else if (src.equals(chbBackground)) {
                    key = keyBase + "4";
                    if (chbBackground.isSelected()) {
//                        parameterUpdated(cpnBackground);
                    }
                }
                if (key != null) {
                    params.put(key, new Boolean(((JCheckBox) src).isSelected()));
//                    System.out.println("saving flag " + key + ", value " + params.get(key));
                }
            } else if (src.equals(cbEffect)) {
                params.put(keyBase+"6", ((JComboBox) src).getSelectedItem());
//                System.out.println("saving string " + key + ", value " + params.get(key));
            } else if (src instanceof JComboBox) {
                if (src.equals(cpnForeground.cmbColors)) {
                    key = keyBase + "3";
                } else if (src.equals(cpnBackground.cmbColors)) {
                    key = keyBase + "5";
                }
                if (key != null) {
                    Object o = ((JComboBox)src).getSelectedItem();
                    params.put(key, (Color)o);
//                    System.out.println("saving color " + key + ", value " + params.get(key));
                }
            }
        }

        updatePreview();
    }

    private void updatePreview() {
        savePreferences(cfgCopy);
//        txpSample.setText("# This is a comment\nType \"argument\" count=2 wait=2s");
    }

    @Override
    public void setShowKeysInToolTips(boolean show) {
        final String m = ApplicationSupport.getString("preference.tooltip");
        for (JComponent c : components.keySet()) {
            c.setToolTipText(show ? MessageFormat.format(m, components.get(c)) : null);
        }
    }
}
