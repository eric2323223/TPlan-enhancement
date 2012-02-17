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
import com.tplan.robot.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Panel with orange-to-white gradient background and product logo in the center.
 * It is used by the {@link com.tplan.robot.gui.dialogs.LoginDialog Login Dialog}
 * and the {@link com.tplan.robot.gui.components.WelcomePanel Welcome Panel}.
 * @product.signature
 */
public class LogoPanel extends JPanel {

    private Color colorLeft = Color.orange;
    private Color colorRight = Color.white;
    private JLabel lblLogo;
    private JLabel lblVersion = new JLabel(Utils.getProductNameAndVersion());
    private Point2D leftCoef = new Point2D.Float(0, 4);
    private Point2D rightCoef = new Point2D.Float(2, 2);

    /**
     * Default constructor.
     */
    public LogoPanel() {
        setBackground(colorRight);
        setOpaque(true);
        setLayout(new BorderLayout());
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setOpaque(false);
        lblVersion.setHorizontalTextPosition(JLabel.CENTER);
        lblVersion.setHorizontalAlignment(JLabel.CENTER);
        try {
            lblLogo = new JLabel(ApplicationSupport.getImageIcon("logo_small.png"));
            pnl.add(lblLogo, BorderLayout.CENTER);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        pnl.add(lblVersion, BorderLayout.SOUTH);
        add(pnl, BorderLayout.NORTH);
    }

    /**
     * Overriden method which paints the gradient background. Note that painting
     * of the logo is ensured through a JLabel instance outside of this method.
     * @param g graphics context.
     */
    public void paint(Graphics g) {
        if (g instanceof Graphics2D) {

            Graphics2D g2D = (Graphics2D) g;
            float lx = (float) (getLeftCoef().getX() == 0 ? 0 : getWidth() / getLeftCoef().getX());
            float ly = (float) (getLeftCoef().getY() == 0 ? 0 : getHeight() / getLeftCoef().getY());
            float rx = (float) (getRightCoef().getX() == 0 ? 0 : getWidth() / getRightCoef().getX());
            float ry = (float) (getRightCoef().getY() == 0 ? 0 : getHeight() / getRightCoef().getY());

            GradientPaint paint = new GradientPaint(lx, ly,getColorLeft(), rx, ry, getColorRight());
            g2D.setPaint(paint);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        setOpaque(false);
        super.paint(g);
        setOpaque(true);
    }

    /**
     * Get the left gradient color.
     * @return the colorLeft left color of the gradient.
     */
    public Color getColorLeft() {
        return colorLeft;
    }

    /**
     * Set the left gradient color.
     * @param colorLeft a new color for the gradient.
     */
    public void setColorLeft(Color colorLeft) {
        this.colorLeft = colorLeft;
    }

    /**
     * Get the right gradient color.
     * @return the colorLeft right color of the gradient.
     */
    public Color getColorRight() {
        return colorRight;
    }

    /**
     * Set the right gradient color.
     * @param colorRight a new color for the gradient.
     */
     public void setColorRight(Color colorRight) {
        this.colorRight = colorRight;
    }

    /**
     * Get the left gradient coefficient point. It affects the angle and starting point
     * of the gradient paint.
     * @return the left gradient coefficient point.
     */
    public Point2D getLeftCoef() {
        return leftCoef;
    }

    /**
     * Set the left gradient coefficient point.
     * @param leftCoef new left coefficient point.
     */
    public void setLeftCoef(Point2D leftCoef) {
        this.leftCoef = leftCoef;
    }

    /**
     * Get the right gradient coefficient point. It affects the angle and starting point
     * of the gradient paint.
     * @return the right gradient coefficient point.
     */
    public Point2D getRightCoef() {
        return rightCoef;
    }

    /**
     * Set the right gradient coefficient point.
     * @param rightCoef new right coefficient point.
     */
    public void setRightCoef(Point2D rightCoef) {
        this.rightCoef = rightCoef;
    }
}
