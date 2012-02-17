/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tplan.robot.gui.preferences.color;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.components.ImageDialog;
import com.tplan.robot.preferences.ColorChooserImageProvider;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.TokenParserImpl;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author robert
 */
public class HTMLColorPanel extends JPanel implements ActionListener,
        KeyListener, Runnable {

    /**
     * A buffered image used to paint the color square
     */
    private BufferedImage image;
    Graphics2D g;
    final int inset = 1;
    JTextField field = new JTextField();
    JLabel label = new JLabel();
    Color color;
    TokenParser parser = new TokenParserImpl();
    JPanel pnl = new JPanel(new BorderLayout(0, 0));
    JButton btnColor = new JButton();
    private ColorChooserImageProvider imageProvider;
    static JDialog dlg;
    static JColorChooser chooser;
    /**
     * Vector of change listeners. When a new custom color is added to
     * an instance of this class, all other instances get a message about it
     * and they reload the combo box model. This way we ensure, that when user
     * defines a custom color, it will be immediately provided by all component
     * instances
     */
    private Vector changeListeners;

    public HTMLColorPanel() {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnColor)) {
            boolean displayChooser = true;
            BufferedImage images[] = null;
            List<String> names = new ArrayList();
            if (imageProvider != null) {
                displayChooser = imageProvider.supportColorChooser();
                images = imageProvider.getImages(names);
            }

            // Display chooser directly
            if (displayChooser && (images == null || images.length < 1)) {
                showChooser();
            } else {
                int listSize = images != null ? images.length : 0;
                if (displayChooser) {
                    listSize++;
                }
                JRadioButton options[] = new JRadioButton[listSize];
                int i = 0;
                if (imageProvider != null) {
                    if (names != null) {
                        for (String s : names) {
                            options[i++] = new JRadioButton(s);
                        }
                    }
                }
                if (displayChooser) {
                    options[i] = new JRadioButton("Choose color from a color chooser");
                }
                if (options.length > 0 && options[0] != null) {
                    options[0].setSelected(true);
                }
                ButtonGroup gr = new ButtonGroup();
                for (JRadioButton btn : options) {
                    gr.add(btn);
                }

                Object message[] = new Object[options.length+1];
                message[0] = "Select the source to choose the color from:";
                System.arraycopy(options, 0, message, 1, options.length);

                Window win = SwingUtilities.getWindowAncestor(this);
                int retval = JOptionPane.showConfirmDialog(win, message, "Choose Color Options",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                switch (retval) {
                    case JOptionPane.OK_OPTION:
                        int index = -1;
                        JRadioButton btn;
                        for (i = 0; i < options.length; i++) {
                            btn = options[i];
                            if (btn.isSelected()) {
                                index = i;
                                break;
                            }
                        }
                        if (index >= 0) {
                            if (images == null || index >= images.length) {
                                showChooser();
                            } else {
                                BufferedImage img = images[index];
                                ImageDialog dlg = new ImageDialog(win, "Choose a color", true);
                                dlg.setEnableTransparencyFeatures(false, false);
                                dlg.setImage(img);
                                dlg.setClickPointEnabled(true);
                                dlg.getDrawPanel().setEnableDragRect(false);
                                dlg.setPointSelectionControlsVisible(true);
                                dlg.getBtnClickPt().setVisible(false);
                                dlg.setCloseOnCancel(true);
                                dlg.setVisible(true);
                                if (!dlg.isCanceled()) {
                                    Point p = dlg.getDrawPanel().getClickPoint();
                                    if (p != null && p.x >= 0 && p.y >= 0 && p.x < img.getWidth() && p.y <= img.getHeight()) {
                                        setSelectedColor(new Color(img.getRGB(p.x, p.y)));
                                    }
                                }
                            }
                        }
                        break;
                }
            }
        }
    }

    private void init() {
        // Remove the field's border
        Border b = field.getBorder();
        field.setBorder(null);
        field.addKeyListener(this);
        label.setOpaque(true);
        label.setBackground(field.getBackground());
        pnl.add(field, BorderLayout.CENTER);
        pnl.add(label, BorderLayout.EAST);
        pnl.setBorder(b);

        setLayout(new BorderLayout(0, 5));
        add(pnl, BorderLayout.CENTER);

        btnColor.setText("...");
        btnColor.setMargin(new Insets(0, 2, 0, 2));
        btnColor.addActionListener(this);
        add(btnColor, BorderLayout.EAST);
        setOpaque(false);
    }

    private void repaintColor() {
        String text = field.getText();
        ImageIcon icon = null;
        try {
            Color c = parser.parseColor(text);
            int size = field.getFont().getSize();
            if (image == null) {
                int rightGap = (pnl.getHeight() - size) / 2;
                image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                g = image.createGraphics();
                g.setColor(field.getBackground());
                g.fillRect(size, 0, rightGap, size);
            }
            g.setColor(c);
            g.fill(new Rectangle(inset,
                    inset,
                    size - inset - 1,
                    size - inset - 1));
            icon = new ImageIcon(image);
        } catch (Exception ex) {
        }
        if (text.length() == 0 || icon != null) {
            label.setIcon(icon);
            label.setText(" ");
        } else {
            label.setIcon(null);
            label.setText("<ERR>");
        }
        fireStateChanged(new ChangeEvent(this));
    }

    public void keyTyped(KeyEvent e) {
        SwingUtilities.invokeLater(this);
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void run() {
        repaintColor();
    }

    /**
     * @return the imageProvider
     */
    public ColorChooserImageProvider getImageProvider() {
        return imageProvider;
    }

    /**
     * @param imageProvider the imageProvider to set
     */
    public void setImageProvider(ColorChooserImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    /**
     * Remove a ChangeListener from the list of listeners. A ChangeEvent gets
     * fired when user adds a new color to the color combo box.
     *
     * @param l a ChangeListener
     */
    public synchronized void removeChangeListener(ChangeListener l) {
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
    public synchronized void addChangeListener(ChangeListener l) {
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
    protected void fireStateChanged(ChangeEvent e) {
        if (changeListeners != null) {
            Vector listeners = changeListeners;
            int count = listeners.size();

            for (int i = 0; i < count; i++) {
                ((ChangeListener) listeners.elementAt(i)).stateChanged(e);
            }
        }
    }

    public Color getSelectedColor() {
        try {
            return parser.parseColor(field.getText());
        } catch (Exception ex) {
        }
        return null;
    }

    public void setSelectedColor(Color c) {
        String text = c == null ? null : parser.colorToString(c);
        field.setText(text);
        repaintColor();
    }

    private void showChooser() {
        Window win = SwingUtilities.getWindowAncestor(this);
        if (dlg == null) {
            dlg = new JDialog(win, ApplicationSupport.getString("com.tplan.robot.gui.options.colorPnlCustomDlgTitle"), Dialog.ModalityType.APPLICATION_MODAL);
            chooser = new JColorChooser();
            AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
            for (AbstractColorChooserPanel pnl : panels) {
                chooser.removeChooserPanel(pnl);
            }
            for (int i = panels.length - 1; i >= 0; i--) {
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

        final Color selected = getSelectedColor();

        if (selected != null) {
            chooser.setColor(selected);
        }

        dlg.setLocationRelativeTo(win);
        dlg.setVisible(true);
    }
}
