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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Vector;

/**
 * A transparent panel which allows to draw rectangles. It is used on top of a
 * desktop viewer to show image update areas.
 *
 * @product.signature
 */
public class DesktopGlassPanel extends JPanel implements PropertyChangeListener, ActionListener, GUIConstants {
//        MouseListener, MouseMotionListener {

    private Vector frameUpdates = new Vector();
    private Vector pixelUpdates = new Vector();
    private Rectangle imagePattern = null;
    private Color pixelUpdateColor = Color.GREEN;
    private Color frameColor = Color.RED;
    private int zoomFactor = 1;

    public DesktopGlassPanel() {
        this.setOpaque(false);
    }

    public void setFrameUpdates(Vector frameUpdates) {
        this.frameUpdates = frameUpdates;
        repaint();
    }

    public void paint(Graphics g) {
        Rectangle r;
        Point p;
        Object o;
        synchronized (frameUpdates) {
            for (int i = 0; i < frameUpdates.size(); i++) {
                o = frameUpdates.elementAt(i);
                r = null;
                if (o instanceof Rectangle) {
                    r = (Rectangle) o;
                } else if (o instanceof RemoteDesktopServerEvent) {
                    r = ((RemoteDesktopServerEvent) o).getUpdateRect();
                } else if (o instanceof Point) {
                    g.setColor(pixelUpdateColor);
                    p = (Point) o;
                    g.fillRect(zoomFactor * p.x, zoomFactor * p.y, zoomFactor, zoomFactor);
                }
                if (r != null) {
                    g.setColor(frameColor);
                    g.drawRect(zoomFactor * r.x, zoomFactor * r.y, zoomFactor * (r.width - 1), zoomFactor * (r.height - 1));
                }
            }
        }
        synchronized (pixelUpdates) {
            for (int i = 0; i < pixelUpdates.size(); i++) {
                o = pixelUpdates.elementAt(i);
                if (o instanceof Point) {
                    g.setColor(pixelUpdateColor);
                    p = (Point) o;
                    g.fillRect(zoomFactor * p.x, zoomFactor * p.y, zoomFactor, zoomFactor);
                }
            }
        }
        if (imagePattern != null) {
            g.setColor(Color.RED);
            g.drawRect(imagePattern.x, imagePattern.y, imagePattern.width - 1, imagePattern.height - 1);
        }

        super.paint(g);
    }

    /**
     * This is necessary to enable key events for this JPanel.
     * Override this method to return true, so we can get keyTyped
     * events.
     *
     * @return true for this class
     */
    public boolean isFocusable() {
        return true;
    }

    public void updateEventSelectionChanged(Object e) {
        frameUpdates.removeAllElements();
        if (e != null) {
            if (e instanceof RemoteDesktopServerEvent && ((RemoteDesktopServerEvent) e).getMessageType() == RemoteDesktopServerEvent.SERVER_UPDATE_EVENT) {
                frameUpdates.add(e);
            } else if (e instanceof List) {
                List v = (List) e;
                for (Object o : v) {
                    if (o instanceof RemoteDesktopServerEvent && ((RemoteDesktopServerEvent) o).getMessageType() == RemoteDesktopServerEvent.SERVER_UPDATE_EVENT) {
                        frameUpdates.add(o);
                    }
                }
            }
        }
        repaint(1);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("updateEventSelected".equals(evt.getPropertyName())) {
            updateEventSelectionChanged(evt.getNewValue());
        } else if ("imagePattern".equals(evt.getPropertyName())) {
            imagePattern = (Rectangle) evt.getOldValue();
            Timer t = new Timer(1000, this);
            t.setRepeats(false);
            t.start();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof Timer && imagePattern != null) {
            Rectangle r = imagePattern;
            imagePattern =
                    null;
            repaint(r);
        }
    }

    public Vector getPixelUpdates() {
        return pixelUpdates;
    }

    public void setPixelUpdates(Vector pixelUpdates) {
        this.pixelUpdates = pixelUpdates;
        repaint();
    }

    public Color getPixelUpdateColor() {
        return pixelUpdateColor;
    }

    public void setPixelUpdateColor(Color pixelUpdateColor) {
        if (this.pixelUpdateColor != pixelUpdateColor) {
            this.pixelUpdateColor = pixelUpdateColor;
            repaint();
        }
    }

    public Color getFrameColor() {
        return frameColor;
    }

    public void setFrameColor(Color frameColor) {
        if (this.frameColor != frameColor) {
            this.frameColor = frameColor;
            repaint();
        }
    }

    public int getZoomFactor() {
        return zoomFactor;
    }

    public void setZoomFactor(int zoomFactor) {
        if (this.zoomFactor != zoomFactor) {
            this.zoomFactor = zoomFactor;
            repaint();
        }
    }
}
