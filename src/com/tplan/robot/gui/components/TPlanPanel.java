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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * A panel with the T-Plan black log on image (a person sitting behind a table).
 * The image is split into the top and bottom parts to allow the panel to grow.
 * The black area in the panel center contains help buttons added through the
 * {@link #addHelpButton(java.lang.String, java.awt.event.ActionListener)} method.
 * The buttons are designed to look like HTML links.
 * 
 * @product.signature
 */
public class TPlanPanel extends JPanel implements MouseListener{

    ImagePanel pnlTop = new ImagePanel();
    ImagePanel pnlBottom = new ImagePanel();
    JPanel pnlCenter = new JPanel();
    JPanel pnlFill = new JPanel();

    final Color buttonTextColor = Color.white; //new Color(0xf8, 0x4e, 0x10);
    final Color buttonHoverTextColor = new Color(0xf8, 0x4e, 0x10);

    public TPlanPanel() {
        ImageIcon img = ApplicationSupport.getImageIcon("logon_top.png");
        pnlTop.setImage(img.getImage());
        pnlTop.setBackground(Color.black);

        ImageIcon img2 = ApplicationSupport.getImageIcon("logon_bottom.png");
        pnlBottom.setImage(img2.getImage());
        pnlTop.setBackground(Color.black);

        pnlCenter.setLayout(new GridBagLayout());
        pnlCenter.setBackground(Color.black);
        pnlFill.setOpaque(false);
        pnlCenter.add(pnlFill, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        this.setBackground(Color.black);
        this.setLayout(new BorderLayout());
        this.add(pnlTop, BorderLayout.NORTH);
        this.add(pnlCenter, BorderLayout.CENTER);
        this.add(pnlBottom, BorderLayout.SOUTH);
    }

    /**
     * Add a bullet style help button to the central panel.
     *
     * @param resourceKey a resource bundle key to the button's text. The same string
     * will be set as the button's action command and may be used later on to identify
     * the button. If the string is not found in the application resource bundle,
     * the string itself is used as label to support hard coded text.
     *
     * @param a an action listener to bind the button to.
     * @return the button instance which was created, bound and added to the picture container.
     */
    public JButton addHelpButton(String resourceKey, ActionListener a) {
        ResourceBundle r = ApplicationSupport.getResourceBundle();
        String label = resourceKey;
        if (r.containsKey(resourceKey)) {
            label = r.getString(resourceKey);
        }
        JButton button = new JButton(label, ApplicationSupport.getImageIcon("orangeBullet.png"));

        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setForeground(buttonTextColor);
        button.setActionCommand(resourceKey);
        button.addActionListener(a);
        button.addMouseListener(this);

        // Set the button font to underline to resemble an HTTP link
        Map<TextAttribute, Object> map = new HashMap<TextAttribute, Object>();
        map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        button.setFont(button.getFont().deriveFont(map));

        Insets in = button.getMargin();
        in.left = 7;
        button.setMargin(in);

        pnlCenter.remove(pnlFill);
        int cnt = pnlCenter.getComponentCount();
        pnlCenter.add(button, new GridBagConstraints(0, cnt++, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        pnlCenter.add(pnlFill, new GridBagConstraints(0, cnt, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        return button;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        if (e.getSource() instanceof JButton) {
            ((JButton)e.getSource()).setForeground(buttonHoverTextColor);
        }
    }

    public void mouseExited(MouseEvent e) {
        if (e.getSource() instanceof JButton) {
            ((JButton)e.getSource()).setForeground(buttonTextColor);
        }
    }
}
