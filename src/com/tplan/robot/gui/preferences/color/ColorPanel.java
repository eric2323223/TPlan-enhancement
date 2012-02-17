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
package com.tplan.robot.gui.preferences.color;

import com.tplan.robot.ApplicationSupport;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.colorchooser.AbstractColorChooserPanel;

/**
 * Graphical component allowing to select a predefined color from a drop down
 * or to define a custom color through pop up dialog with a color chooser.
 * @product.signature
 */
public class ColorPanel extends JPanel implements ActionListener, ChangeListener {

    /**
     * Array of default colors which will be inserted into the drop down.
     */
    static Color defaultColors[] = {
        new NamedColor(Color.green, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlGreen")),
        new NamedColor(Color.red, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlRed")),
        new NamedColor(Color.orange, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlOrange")),
        new NamedColor(Color.pink, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlPink")),
        new NamedColor(Color.yellow, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlYellow")),
        new NamedColor(Color.cyan, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlCyan")),
        new NamedColor(Color.magenta, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlMagenta")),
        new NamedColor(new Color(153, 0, 204), ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlPurple")),
        new NamedColor(new Color(153, 153, 255), ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlLightBlue")),
        new NamedColor(Color.blue, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlBlue")),
        new NamedColor(new Color(51, 0, 153), ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlDarkBlue")),
        new NamedColor(Color.white, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlWhite")),
        new NamedColor(Color.lightGray, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlLightGray")),
        new NamedColor(Color.gray, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlGray")),
        new NamedColor(Color.darkGray, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlDarkGray")),
        new NamedColor(Color.black, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlBlack"))
    };
    /**
     * Vector of default colors. This vector is used to prefill each instance
     * of this class.
     */
    static Vector colors = new Vector();
    /**
     * Vector of change listeners. When a new custom color is added to
     * an instance of this class, all other instances get a message about it
     * and they reload the combo box model. This way we ensure, that when user
     * defines a custom color, it will be immediately provided by all component
     * instances
     */
    private static Vector changeListeners;
    /**
     * A flag showing if there are new custom colors defined by user
     */
    static public boolean newCustomColors = false;
    static JDialog dlg;
    static JColorChooser chooser;
    /**
     * Custom renderer for the combo box
     */
    ColorListCellRenderer renderer = new ColorListCellRenderer();
    /**
     * Component layout manager
     */
    BorderLayout borderLayout1 = new BorderLayout();
    /**
     * Component combo box
     */
    public JComboBox cmbColors = new JComboBox();
    /**
     * Component button
     */
    JButton btnMoreColors = new JButton();
    /**
     * Model for the color combo box
     */
    DefaultComboBoxModel model = createModel();
    /**
     * Static initializer. It puts all default colors from
     * the <code>defaultColors</code> array into the <code>colors</code> vector.
     */


    static {
        for (int i = 0; i < defaultColors.length; i++) {
            colors.add(defaultColors[i]);
        }
    }
    boolean changed = false;

    /**
     * Overriden to set the tool tip message to the components instead of the panel.
     * @param text toool tip message text (null sets off tool tips).
     */
    @Override
    public void setToolTipText(String text) {
        cmbColors.setToolTipText(text);
    }

    /**
     * Default constructor
     */
    public ColorPanel() {
        try {
            jbInit();
            addChangeListener(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Implementation of the ChangeListener interface. When a new custom color
     * is added to an instance of this class, all other instances get a message
     * about it and they reload the combo box model. This way we ensure, that
     * when user defines a custom color, it will be immediately provided
     * by all component instances
     *
     * @param e a ChangeEvent
     */
    public void stateChanged(ChangeEvent e) {
        if (!e.getSource().equals(this)) {
            Object selectedColor = cmbColors.getSelectedItem();

            cmbColors.setModel(createModel());
            cmbColors.setSelectedItem(selectedColor);
        }
    }

    /**
     * Implementation of the ActionListener interface. Invoked when user clicks
     * the component button. It opens a new dialog with a JFileChooser
     * and lets user define a custom color. If the dialog is properly ended, the
     * selected color will be added to the static list of available colors and
     * all instances of this class will be notified about the change through
     * the ChangeListener mechanism.
     *
     * @param e an ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnMoreColors)) {
            Window win = SwingUtilities.getWindowAncestor(this);
            if (dlg == null) {
                dlg = new JDialog(win, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlCustomDlgTitle"), Dialog.ModalityType.APPLICATION_MODAL);
                chooser = new JColorChooser();
                AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
                for (AbstractColorChooserPanel pnl : panels) {
                    chooser.removeChooserPanel(pnl);
                }
                for (int i=panels.length-1; i >= 0; i--) {
                    chooser.addChooserPanel(panels[i]);
                }
                JPanel pnl = new JPanel(new BorderLayout());
                dlg.setContentPane(pnl);

                pnl.add(chooser, BorderLayout.CENTER);
                JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton btnOK = new JButton(ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlCustomDlgOK"));
                south.add(btnOK);
                dlg.getRootPane().setDefaultButton(btnOK);
                btnOK.addActionListener(new ActionListener() {

                    /**
                     * Invoked when an action occurs.
                     */
                    public void actionPerformed(ActionEvent e) {
                        Color c = chooser.getColor();
                        if (c != null) {
                            setSelectedColor(c);
                        }
                        dlg.dispose();
                    }
                });

                JButton btnCancel = new JButton(ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlCustomDlgCancel"));
                south.add(btnCancel);
                btnCancel.addActionListener(new ActionListener() {

                    /**
                     * Invoked when an action occurs.
                     */
                    public void actionPerformed(ActionEvent e) {
                        dlg.dispose();
                    }
                });
                pnl.add(south, BorderLayout.SOUTH);
                dlg.pack();
            }

            final Color selected = (Color) cmbColors.getSelectedItem();

            if (selected != null) {
                chooser.setColor(selected);
            }

            dlg.setLocationRelativeTo(win);
            dlg.setVisible(true);
        }
    }

    /**
     * Add a new color to the static list of colors. If the color is already
     * in the list, the operation gets canceled. When successful,
     * the method fires a ChangeEvent to force all the class instances to reload
     * their combo box models
     *
     * @param c            a new color
     * @param sourceObject source component to be set as source in the change event
     */
    public static void addColor(Color c, Object sourceObject) {
        Color color = new NamedColor(c,
                ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlCustomColorName"));

        if (colors.indexOf(color) >= 0) {   // Do not add the color if it is there
            return;
        }
        colors.insertElementAt(color, 0);
        fireStateChanged(new ChangeEvent(sourceObject));
        newCustomColors = true;
    }

    /**
     * Set selected color in the combo box. If the combo doesn't contain
     * the color, it will be added by the method addColor(Color, Object).
     *
     * @param c a color to be selected or added and selected
     */
    public void setSelectedColor(Color c) {
        if (c != null) {
            int i = model.getIndexOf(c);

            if (i < 0) {
                addColor(c, this);
                setSelectedColor(c);
            } else {
                cmbColors.setSelectedIndex(i);
            }
        } else if (cmbColors.getItemCount() > 0) {
            cmbColors.setSelectedIndex(0);
        }
    }

    /**
     * Get an array of first ten custom colors that have been added to the color
     * combo box. The colors are usually saved to the config file and loaded upon
     * the application start.
     *
     * @return array of first ten custom colors that have been added to the color
     *         combo box
     */
    public static Color[] getCustomColorRGBValues() {
        int size = colors.size() - defaultColors.length;

        size = Math.min(10, size);

        Color col[] = new Color[size];

        for (int i = 0; i < size; i++) {
            Color c = (Color) colors.get(i);

            col[i] = c;
        }
        return col;
    }

    /**
     * Create a new combo box model and fill it with the available colors
     *
     * @return a new combo box model with available colors
     */
    protected DefaultComboBoxModel createModel() {
        return new DefaultComboBoxModel(colors);
    }

    /**
     * Initialize the components. Generated by the JBuilder Designer.
     *
     * @throws Exception when initialization of a component fails
     */
    void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        btnMoreColors.setText(ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlCustomButton"));
        btnMoreColors.setToolTipText(ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlCustomButtonToolTip"));
        btnMoreColors.setMargin(new Insets(0, 2, 0, 2));
        borderLayout1.setHgap(5);
        this.add(cmbColors, BorderLayout.CENTER);
        this.add(btnMoreColors, BorderLayout.EAST);
        btnMoreColors.addActionListener(this);
        cmbColors.setEditable(false);
        cmbColors.setModel(createModel());
        cmbColors.setRenderer(renderer);
        setOpaque(false);
    }

    /**
     * Remove a ChangeListener from the list of listeners. A ChangeEvent gets
     * fired when user adds a new color to the color combo box.
     *
     * @param l a ChangeListener
     */
    public static synchronized void removeChangeListener(ChangeListener l) {
        if (changeListeners != null && changeListeners.contains(l)) {
            Vector v = (Vector) changeListeners.clone();

            v.removeElement(l);
            changeListeners = v;
        }
    }

    /**
     * Remove a ChangeListener from the list of listeners. A ChangeEvent gets
     * fired when user adds a new color to the color combo box.
     *
     * @param l a ChangeListener
     */
    public static synchronized void addChangeListener(ChangeListener l) {
        Vector v = changeListeners == null ? new Vector(2) : (Vector) changeListeners.clone();

        if (!v.contains(l)) {
            v.addElement(l);
            changeListeners = v;
        }
    }

    /**
     * Fire a ChangeEvent. Used when user adds a new color to the
     * color combo box.
     *
     * @param e a ChangeEvent
     */
    protected static void fireStateChanged(ChangeEvent e) {
        if (changeListeners != null) {
            Vector listeners = changeListeners;
            int count = listeners.size();

            for (int i = 0; i < count; i++) {
                ((ChangeListener) listeners.elementAt(i)).stateChanged(e);
            }
        }
    }

    public Color getSelectedColor() {
        return (Color) cmbColors.getSelectedItem();
    }

    public JButton getButton() {
        return btnMoreColors;
    }
}
