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
import com.tplan.robot.gui.GUIConstants;
import com.tplan.robot.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * A simple dialog which displays an image and allows user to define a rectangle of
 * interest using mouse drags. It has a simple toolbar which contains a label showing
 * coordinates and a Close button.
 * @product.signature
 */
public class ImageDialog extends JDialog implements PropertyChangeListener, ActionListener {

    ImageDrawPanel pnl;
    Rectangle rectangle;
    JToolBar toolBar = new JToolBar();
    JButton btnClose = new JButton(ApplicationSupport.getString("imageDialog.buttonSaveAndClose"));
    JLabel lblCoords = new JLabel("[0,0]");
    JLabel hint;
    boolean canceled = true;
    JPanel contentPanel;
    boolean enableSaveForNoUpdates = false;

    /**
     * Constructor.
     * @param parent dialog parent (a JDialog instance).
     * @param title window title.
     * @param modal modality flag.
     */
    public ImageDialog(Window parent, String title, boolean modal) {
        super(parent, title, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
        pnl = new ImageDrawPanel();
        pnl.pnlDraw.addPropertyChangeListener(this);
        getContentPane().setLayout(new BorderLayout());

        contentPanel = new ChessboardBackgroundPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.add(pnl, new GridBagConstraints(0, 0, 0, 0, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        hint = new JLabel(ApplicationSupport.getString("imageDialog.hintMessage"));
        Font f = hint.getFont();
        f = new Font(f.getName(), f.getStyle(), f.getSize() - 1);
        hint.setFont(f);
        getContentPane().add(hint, BorderLayout.SOUTH);

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setOpaque(false);
        getContentPane().add(scroll, BorderLayout.CENTER);

        toolBar.add(btnClose);
        toolBar.addSeparator(new Dimension(20, 10));
        toolBar.add(lblCoords);
        btnClose.addActionListener(this);
        toolBar.setFloatable(false);
        getContentPane().add(toolBar, BorderLayout.NORTH);
    }

    public void enableSaveForNoUpdates(boolean enable) {
        this.enableSaveForNoUpdates = enable;
    }

    /**
     * Set the toolbar visible/invisible
     * @param visible true displays the toolbar, false hides it.
     */
    public void setToolBarVisible(boolean visible) {
        toolBar.setVisible(visible);
    }

    /**
     * Show or hide the dialog.
     * @param visible true displays the dialog, false disposes it.
     */
    public void setVisible(boolean visible) {
        if (visible) {
            if (pnl.pnlDraw.isEnablePoints() && pnl.pnlDraw.isEnableDragRect()) {
                hint.setText(ApplicationSupport.getString("imageDialog.hintMessagePointAndRect"));
            } else if (pnl.pnlDraw.isEnablePoints()) {
                hint.setText(ApplicationSupport.getString("imageDialog.hintMessagePoint"));
            } else if (pnl.pnlDraw.isEnableDragRect()) {
                hint.setText(ApplicationSupport.getString("imageDialog.hintMessage"));
            } else {
                hint.setText(null);
            }
            btnClose.setEnabled(enableSaveForNoUpdates);
            canceled = true;
            lblCoords.setText("[0,0]");
        }
        super.setVisible(visible);
    }

    /**
     * Get the transparent draw panel which allows to draw a rectangle using mouse drags.
     * @return draw panel.
     */
    public DrawPanel getDrawPanel() {
        return pnl.pnlDraw;
    }

    /**
     * Set the image displayed by the dialog.
     * @param image an image to display. A null value will display an empty window.
     */
    public void setImage(Image image) {
        pnl.setImage(image);
        pack();
        setLocationRelativeTo(getParent());
    }

    /**
     * Implementation of the PropertyChangeListener. It processes events from
     * the draw panel which fires property change events when a rectangle is defined
     * or any of its controls are selected.
     *
     * @param evt a property change event from the draw panel.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("okPressed")) {
            rectangle = (Rectangle) evt.getNewValue();
            canceled = false;
            dispose();
        } else if (evt.getPropertyName().equals("dragRectDefined")) {
            rectangle = (Rectangle) evt.getNewValue();
            btnClose.setEnabled(true);
        } else if (evt.getPropertyName().equals("cancelPressed")) {
            rectangle = null;
            btnClose.setEnabled(true);
        } else if (evt.getPropertyName().equals(GUIConstants.EVENT_MOUSE_MOVED)) {
            if (evt.getNewValue() instanceof Point && pnl.image instanceof BufferedImage) {
                String s = Utils.getMouseCoordinateText(
                        (Point) evt.getNewValue(),
                        (BufferedImage) pnl.image,
                        true,
                        true,
                        false);
                lblCoords.setText(s);
            }
        }
    }

    /**
     * Get the rectangle defined by user through mouse drags. If no rectangle was
     * selected or user removed the previously selected rectangle, the method returns null.
     * @return the rectangle of interest selected in the dialog or null if no rectangle is defined.
     */
    public Rectangle getRectangle() {
        return rectangle;
    }

    /**
     * Set and display the rectange selected in the dialog.
     * @param rectangle a rectangle to be selected and displayed. A null value
     * clears selection.
     */
    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
        pnl.pnlDraw.setDragRect(rectangle);
    }

    /**
     * Handler of action events from the Close button.
     * @param e an action event fired by the Close button.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnClose)) {
            rectangle = getDrawPanel().getDragRect();
            canceled = false;
            getDrawPanel().clickOk();
        }
    }

    /**
     * Find out whether the dialog was canceled by user.
     * @return true if the dialog was canceled, false if not.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Dummy method to keep compatibility with the Enterprise version.
     * @param enabled doesn't matter because the method does nothing.
     */
        public void setClickPointEnabled(boolean enabled) {
        getBtnClickPt().setVisible(enabled);
        pnl.pnlDraw.setEnableClickPoint(enabled);
    }

    /**
     * Dummy method to keep compatibility with the Enterprise version.
     * Always returns null.
     * @return always returns null.
     */
    public JButton getBtnClickPt() {
        return null;
    }

    /**
     * Dummy method to keep compatibility with the Enterprise version.
     * @return always returns false.
     */
    public boolean isCloseOnCancel() {
        return false;
    }

    /**
     * Dummy method to keep compatibility with the Enterprise version.
     * @param closeOnCancel doesn't matter because the method does nothing.
     */
    public void setCloseOnCancel(boolean closeOnCancel) {
    }

    /**
     * Dummy method to keep compatibility with the Enterprise version.
     * @param enableTransparency doesn't matter because the method does nothing.
     * @param enableMinAlpha doesn't matter because the method does nothing.
     */
    public void setEnableTransparencyFeatures(boolean enableTransparency, boolean enableMinAlpha) {
    }

    /**
     * Dummy method to keep compatibility with the Enterprise version.
     * @param visible doesn't matter because the method does nothing.
     */
    public void setPointSelectionControlsVisible(boolean visible) {
    }
}
