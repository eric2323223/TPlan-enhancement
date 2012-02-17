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

import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.preferences.ConfigurationChangeEvent;
import com.tplan.robot.preferences.ConfigurationChangeListener;
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.editor.Editor;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.TokenParserImpl;
import com.tplan.robot.scripting.commands.impl.WaitforCommand;
import com.tplan.robot.util.DocumentUtils;
import com.tplan.robot.util.Utils;

import java.util.List;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;

/**
 * Status bar GUI component.
 * @product.signature
 */
public class StatusBar extends JPanel implements MouseListener,
        ConfigurationKeys, ConfigurationChangeListener, PropertyChangeListener, GUIConstants {

    private final JLabel logField = new JLabel();
    private final JLabel mouseCoordinates = new JLabel();
    private final JTextField updateField = new JTextField();
    private JPopupMenu menu1;
    private JPopupMenu menu2;
    private JPopupMenu menu3;
    private RemoteDesktopServerEvent event;
    private Action copyAction = null;
    private Action configureAction = null;
    private boolean displayRelativeCoordinates;
    private MainFrame frame;

    public StatusBar(MainFrame frame) {
        super();
        this.frame = frame;
        displayRelativeCoordinates =
                UserConfiguration.getInstance().getBoolean("gui.StatusBar.displayRelativeMouseCoordinates").booleanValue();

        frame.getUserConfiguration().addConfigurationListener(this);
        init();
    }

    private void init() {
        setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        setLayout(new GridBagLayout());

        updateField.setEditable(false);
        updateField.setBorder(null);

        updateField.addMouseListener(this);
        logField.addMouseListener(this);
        mouseCoordinates.addMouseListener(this);

        GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 1, 1), 0, 0);
        add(logField, c);
        c = new GridBagConstraints(1, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(1, 1, 1, 1), 0, 0);
        add(new JSeparator(JSeparator.VERTICAL), c);
        c = new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(1, 1, 1, 5), 0, 0);
        add(updateField, c);
        c = new GridBagConstraints(3, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(1, 1, 1, 1), 0, 0);
        add(new JSeparator(JSeparator.VERTICAL), c);
        c = new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(1, 5, 1, 5), 0, 0);
        add(mouseCoordinates, c);

        updateField.setFont(mouseCoordinates.getFont());
    }

    void computeFieldSizes(RemoteDesktopClient rfb) {
        String msg = ApplicationSupport.getString("statusbar.updateCoordinates");
        Object params[] = {
            rfb.getDesktopWidth() + "", rfb.getDesktopHeight() + "", rfb.getDesktopWidth() + "", rfb.getDesktopHeight() + "", "100"
        };
        msg = MessageFormat.format(msg, params);
        Dimension d = new Dimension(updateField.getPreferredSize());
        d.width = updateField.getFontMetrics(updateField.getFont()).stringWidth(msg);
        updateField.setPreferredSize(d);

        msg = ApplicationSupport.getString("statusbar.mouseCoordinates");
        msg = Utils.getMouseCoordinateText(new Point(rfb.getDesktopWidth(), rfb.getDesktopHeight()), (BufferedImage) rfb.getImage(), false, true, displayRelativeCoordinates);
        d = new Dimension(mouseCoordinates.getPreferredSize());
        d.width = mouseCoordinates.getFontMetrics(mouseCoordinates.getFont()).stringWidth(msg);
        mouseCoordinates.setPreferredSize(d);
        mouseCoordinates.setMinimumSize(d);
    }

    void updateRectangleChanged(RemoteDesktopServerEvent evt, float percentage) {
        Integer minimumSize = UserConfiguration.getInstance().getInteger("gui.StatusBar.updateFilterPercentage");
        if (percentage >= (minimumSize != null ? minimumSize.intValue() : 10) && percentage >= 0 && percentage <= 100) {
            event = evt;
            String msg = ApplicationSupport.getString("statusbar.updateCoordinates");

            Object params[] = {
                "" + evt.getUpdateRect().x,
                "" + evt.getUpdateRect().y,
                "" + evt.getUpdateRect().width,
                "" + evt.getUpdateRect().height,
                "" + (int) percentage
            };
            msg = MessageFormat.format(msg, params);
            updateField.setText(msg);
        }
    }

    void updateEditorPosition(Editor ed) {
        int off = ed.getCaretPosition();
        Element e = DocumentUtils.getElementForOffset(ed.getStyledDocument(), off);
        int line = DocumentUtils.getLineForOffset(ed.getStyledDocument(), off) + 1;
        off = off - e.getStartOffset() + 1;
        mouseCoordinates.setText(line + ":" + off);
        SyntaxErrorException ex;
        List errors = ed.getErrorList();
        boolean isError = false;
        for (int i = 0; e != null && errors != null && i < errors.size(); i++) {
            ex = (SyntaxErrorException) errors.get(i);
            if (ex.getElement() != null && ex.getElement().equals(e)) {
                logField.setText(ex.getMessage());
                isError = true;
                break;
            }
        }
        if (!isError) {
            logField.setText("");
        }
    }

    private JPopupMenu getPopUpMenu2() {
        if (menu2 == null) {
            menu2 = new JPopupMenu();

            copyAction = new CopyCoordinatesAction();
            JMenuItem mi = new JMenuItem(copyAction);
            menu2.add(mi);

            if (configureAction == null) {
                configureAction = new ConfigureSizeLimitAction();
            }
            mi = new JMenuItem(configureAction);
            menu2.add(mi);
        }
        copyAction.setEnabled(event != null);
        configureAction.setEnabled(true);
        return menu2;
    }

    private JPopupMenu getPopUpMenu1() {
        if (menu1 == null) {
            menu1 = new JPopupMenu();

            if (configureAction == null) {
                configureAction = new ConfigureSizeLimitAction();
            }
            JMenuItem mi = new JMenuItem(configureAction);
            menu1.add(mi);
        }
        configureAction.setEnabled(true);
        return menu1;
    }

    private JPopupMenu getPopUpMenu3() {
        if (menu3 == null) {
            menu3 = new JPopupMenu();

            if (configureAction == null) {
                configureAction = new ConfigureSizeLimitAction();
            }
            JMenuItem mi = new JMenuItem(configureAction);
            menu3.add(mi);
        }
        configureAction.setEnabled(true);
        return menu3;
    }

    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            if (e.getSource().equals(logField)) {
                getPopUpMenu1().show(logField, e.getX(), e.getY() - (menu1.getHeight() > 0 ? menu1.getHeight() : 30));
            } else if (e.getSource().equals(updateField)) {
                getPopUpMenu2().show(updateField, e.getX(), e.getY() - (menu2.getHeight() > 0 ? menu2.getHeight() : 30));
            } else if (e.getSource().equals(mouseCoordinates)) {
                getPopUpMenu3().show(mouseCoordinates, e.getX(), e.getY() - (menu3.getHeight() > 0 ? menu3.getHeight() : 30));
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void configurationChanged(ConfigurationChangeEvent evt) {
        if (evt.getPropertyName().equals("gui.StatusBar.displayRelativeMouseCoordinates")) {
            displayRelativeCoordinates =
                    UserConfiguration.getInstance().getBoolean("gui.StatusBar.displayRelativeMouseCoordinates").booleanValue();
            computeFieldSizes(frame.getClient());
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(EVENT_MOUSE_MOVED)) {
            if (evt.getNewValue() instanceof Point && frame.getClient() != null && frame.getClient().isConnected()) {
                final boolean isReadOnly = ((JToggleButton) frame.getActionManager().getToolbarButton("readonly")).isSelected();
                BufferedImage bi = (BufferedImage) frame.getClient().getImage();
                String text = "";
                if (bi != null) {
                    text = Utils.getMouseCoordinateText((Point) evt.getNewValue(),
                            bi, true, isReadOnly, displayRelativeCoordinates);
                }
                mouseCoordinates.setText(text);
            }
        }
    }

    /**
     * @return the logField
     */
    public JLabel getFieldLeft() {
        return logField;
    }

    /**
     * @return the mouseCoordinates
     */
    public JLabel getFieldRight() {
        return mouseCoordinates;
    }

    /**
     * @return the updateField
     */
    public JTextField getUpdateField() {
        return updateField;
    }

    class CopyCoordinatesAction extends AbstractAction {

        TokenParser parser = new TokenParserImpl();

        public CopyCoordinatesAction() {
            super(ApplicationSupport.getResourceBundle().getString("statusbar.menuItem.copyCoordinates"));
        }

        public void actionPerformed(ActionEvent e) {
            if (event != null) {
                Rectangle r = event.getUpdateRect();
                String text = WaitforCommand.PARAM_AREA + "=" + parser.rectToString(r);
                JTextField f = new JTextField(text);
                f.selectAll();
                f.copy();
            }
        }
    }

    class ConfigureSizeLimitAction extends AbstractAction {

        final String path = ApplicationSupport.getString("com.tplan.robot.gui.options.statusBarNode");

        public ConfigureSizeLimitAction() {
            super(ApplicationSupport.getResourceBundle().getString("statusbar.menuItem.configure"));
        }

        public void actionPerformed(ActionEvent e) {
            frame.showOptionsDialog(path, null);
        }
    }
}
