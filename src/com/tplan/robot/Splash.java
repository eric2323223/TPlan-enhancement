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
package com.tplan.robot;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import javax.swing.JWindow;

/**
 * Splash manager..
 * @product.signature
 */
public class Splash extends JWindow {

    private BufferedImage image;
    static Splash instance;
    private boolean paintCalled = false;

    private Splash(Frame parent, BufferedImage image) {
        super(parent);
        this.image = image;

        MediaTracker mt = new MediaTracker(this);
        mt.addImage(image, 0);
        try {
            mt.waitForID(0);
        } catch (InterruptedException ie) {
        }
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(image, 0, 0, this);

        if (!paintCalled) {
            paintCalled = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public static boolean isDisplayed() {
        return instance != null && instance.isVisible();
    }

    public static void close() {
        if (instance != null) {
            instance.setVisible(false);
            instance.dispose();
            instance.image = null;
            instance = null;
        }
    }

    public static void pushToBack() {
        if (isDisplayed()) {
            instance.toBack();
        }
    }

    static void show(BufferedImage image) {
        if (instance == null && image != null) {
            Frame f = new Frame();
            instance = new Splash(f, image);
            int w = image.getWidth();
            int h = image.getHeight();
            instance.setBackground(Color.WHITE);
            instance.setSize(w, h);
            instance.setLocationRelativeTo(null);
            instance.setVisible(true);

            if (!EventQueue.isDispatchThread() && Runtime.getRuntime().availableProcessors() == 1) {
                synchronized (instance) {
                    while (!instance.paintCalled) {
                        try {
                            instance.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
    }
}
