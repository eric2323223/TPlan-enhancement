/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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

import com.tplan.robot.gui.MainFrame;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLDocument;

/**
 * Welcome panel with product logo and links to the online documentation.
 * @product.signature
 */
public class WelcomePanel extends JPanel implements MouseListener {

    /**
     * Name of the HTML template with the Welcome Panel content.
     */
    public static final String WELCOME_TEMPLATE_FILE = "welcome.html";
    MainFrame mainFrame;
    JEditorPane txt = new JEditorPane();
    Cursor defaultCursor;

    /**
     * Constructor.
     * @param mainFrame the main {@product.name} frame.
     */
    public WelcomePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize the GUI
     * @throws java.lang.Exception if an error is thrown by any GUI method.
     */
    private void init() throws Exception {
        setLayout(new BorderLayout());

        LogoPanel logo = new LogoPanel();
        logo.setLeftCoef(new Point2D.Float(2, 0));
        logo.setRightCoef(new Point2D.Float(2, 1));
        add(logo, BorderLayout.NORTH);
        defaultCursor = mainFrame.getCursor();

        txt.setEditable(false);
        txt.setContentType("txt/html");
        txt.setOpaque(true);
        txt.setBackground(Color.white);
        txt.setPage(getClass().getResource(WELCOME_TEMPLATE_FILE));
        txt.addHyperlinkListener(new CustomHyperlinkListener(mainFrame));
        txt.addMouseListener(this);

        // Set the JEditorPane default font to the JLabel one
        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument) txt.getDocument()).getStyleSheet().addRule(bodyRule);
        
        JScrollPane scroll = new JScrollPane(txt);
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Unused empty method of MouseListener.
     * @param e a mouse event.
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * Unused empty method of MouseListener.
     * @param e a mouse event.
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * Unused empty method of MouseListener.
     * @param e a mouse event.
     */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Unused empty method of MouseListener.
     * @param e a mouse event.
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * The method resets cursor shape when the mouse pointer exits the panel.
     * @param e a mouse event.
     */
    public void mouseExited(MouseEvent e) {
        mainFrame.setCursor(defaultCursor);
    }
}
