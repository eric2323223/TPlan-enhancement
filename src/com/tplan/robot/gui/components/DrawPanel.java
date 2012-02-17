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

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * A transparent component allowing to define a rectangle using mouse drags.
 * @product.signature
 */
public class DrawPanel extends JPanel implements MouseListener, MouseMotionListener {

    private final int LABEL_TEXT_MARGIN_PTS = 2;
    private final int LABEL_FROM_BORDER_PTS = 3;
    private final int LEFT_UPPER_CORNER = 1;
    private final int RIGHT_UPPER_CORNER = 2;
    private final int RIGHT_LOWER_CORNER = 3;
    private final int LEFT_LOWER_CORNER = 4;
    private final int LEFT_SIDE = 11;
    private final int UPPER_SIDE = 12;
    private final int RIGHT_SIDE = 13;
    private final int LOWER_SIDE = 14;
    private final int DRAG_TOLERANCE = 3;
    private final int BUTTON_SIZE = 16;
    private final int POINT_CROSS_SIZE = 3;
    private boolean dragInProgress = false;
    private Point startPoint = null;
    private Point endPoint = null;
    private boolean enableDragRect = false;
    private boolean enablePoints = true;

    // TODO: scaling (doesn't work with rectangle resizing)
    private int zoomFactor = 1;
    private boolean rectangleDefined = false;
    private int dragPoint = -1;
    private Rectangle btnOkRect = null;
    private Rectangle btnCancelRect = null;
    private boolean okBtnVisible = true;
    private boolean cancelBtnVisible = true;
    static ImageIcon okIcon;
    static ImageIcon cancelIcon;


    static {
        try {
            okIcon = ApplicationSupport.getImageIcon("ok15.gif");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        try {
            cancelIcon = ApplicationSupport.getImageIcon("cancel16.png");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
    Dimension dragBounds = null;

    public DrawPanel() {
        setOpaque(false);
//        if (okIcon == null) {
//        try {
//            ImageIcon okIcon = ApplicationSupport.getImageLoader().getClass().getResource("ok15.gif"));
//        } catch (NullPointerException e) {
//            e.printStackTrace();
//        }
//        }
//        if (cancelIcon == null) {
//        try {
//            ImageIcon cancelIcon = new ImageIcon(ApplicationSupport.getImageLoader().getClass().getResource("cancel16.png"));
//        } catch (NullPointerException e) {
//            e.printStackTrace();
//        }
//        }
    }

    public void setDragRect(Rectangle r) {
        if (r == null) {
            resetDragRect();
        } else {
            startPoint = r.getLocation();
            endPoint = new Point(r.x + r.width, r.y + r.height);
            rectangleDefined = true;
        }
    }

    public void paint(Graphics g) {
        if (isEnableDragRect() || enablePoints) {
            Rectangle dr = getNonScaledDragRect();
            Rectangle sdr = getDragRect(dr);
            if (dr != null) { // && ) {
                boolean isPoint = dr.width == 0 && dr.height == 0;
                dr = new Rectangle(toScaled(dr.x), toScaled(dr.y), toScaled(dr.width), toScaled(dr.height));
                String s;
                if (isPoint) {
                    Color c = g.getColor();
                    g.setColor(Color.RED);
                    g.drawLine(dr.x - POINT_CROSS_SIZE, dr.y, dr.x + POINT_CROSS_SIZE, dr.y);
                    g.drawLine(dr.x, dr.y - POINT_CROSS_SIZE, dr.x, dr.y + POINT_CROSS_SIZE);
                    s = "x:" + sdr.x + ",y:" + sdr.y;
                    g.setColor(c);
                } else {
                    drawRunningRect(g, dr);
                    s = "x:" + sdr.x + ",y:" + sdr.y + ",w:" + sdr.width + ",h:" + sdr.height;
                }
                Rectangle lr = computeLabelRectangle(dr, s);
                if (lr != null) {
                    g.setColor(Color.LIGHT_GRAY);
                    g.fillRect(lr.x, lr.y, lr.width, lr.height);
                    g.setColor(Color.black);
                    int size = getFont().getSize();
                    g.drawString(s, lr.x + LABEL_TEXT_MARGIN_PTS, lr.y + (lr.height - size) / 2 + size);

                    if (btnOkRect != null) {
                        g.setColor(Color.black);
                        g.drawImage(okIcon.getImage(), btnOkRect.x + (btnOkRect.width - okIcon.getIconWidth()), btnOkRect.y + LABEL_TEXT_MARGIN_PTS, this);
                    }
                    if (btnCancelRect != null) {
                        g.setColor(Color.black);
                        g.drawImage(cancelIcon.getImage(), btnCancelRect.x, btnCancelRect.y + LABEL_TEXT_MARGIN_PTS, this);
                    }
                }
            }
        }
        super.paint(g);
    }
    int shift = 0;
    private final int SEGMENT_LENGTH = 4;
    private Color c1 = Color.black;
    private Color c2 = Color.white;

    private void drawRunningLine(Graphics g, Point start, Point end) {
        boolean vertical = start.x == end.x;
        int length = (vertical ? Math.abs(start.y - end.y) : Math.abs(start.x - end.x));
        int cnt = length / SEGMENT_LENGTH;
        int max = (vertical ? Math.max(start.y, end.y) : Math.max(start.x, end.x));
        int pos = 0;
        for (int i = 0; i < cnt; i++) {
            pos = (vertical ? start.y : start.x) + i * SEGMENT_LENGTH + shift;
            g.setColor(i % 2 == 1 ? c2 : c1);

            if (vertical) {
                g.drawLine(start.x, pos, start.x, Math.min(pos + SEGMENT_LENGTH, max));
            } else {
                g.drawLine(pos, start.y, Math.min(pos + SEGMENT_LENGTH, max), start.y);
            }
        }
    }

    private void drawRunningRect(Graphics g, Rectangle r) {
        Point lcorner = new Point(r.x + r.width, r.y + r.height);
        Point p = new Point(lcorner.x, r.y);
        drawRunningLine(g, r.getLocation(), p);
        drawRunningLine(g, p, lcorner);

        p.x = r.x;
        p.y = lcorner.y;
        drawRunningLine(g, p, lcorner);
        drawRunningLine(g, r.getLocation(), p);
        shift = (shift + 1) % (2 * SEGMENT_LENGTH);
        if (shift >= SEGMENT_LENGTH) {
            Color c = c2;
            c2 = c1;
            c1 = c;
        }
        repaint(500, r.x, r.y, r.width + 2, r.height + 2);
    }

    private int toScaled(int coord) {
        return zoomFactor * Math.round((float) coord / zoomFactor);
    }

    private Rectangle computeLabelRectangle(Rectangle dr, String s) {
        int delta = BUTTON_SIZE + 2 * LABEL_TEXT_MARGIN_PTS;
        int textWidth = getFontMetrics(getFont()).stringWidth(s) + 2 * LABEL_TEXT_MARGIN_PTS;
        int width = textWidth + (okBtnVisible ? delta : 0) + (cancelBtnVisible ? delta : 0);
        int height = Math.max(getFont().getSize(), BUTTON_SIZE) + 2 * LABEL_TEXT_MARGIN_PTS;

        int x = dr.x + dr.width - width - LABEL_FROM_BORDER_PTS;
        if (x < 1) {
            x = 1;
        }
        int y = dr.y + dr.height + LABEL_FROM_BORDER_PTS;
        if (y + height > getHeight()) {
            y = dr.y + dr.height - height - LABEL_FROM_BORDER_PTS;
        }

        if (okBtnVisible) {
            btnOkRect = new Rectangle(x + textWidth, y, BUTTON_SIZE, BUTTON_SIZE);
        }
        if (cancelBtnVisible) {
            btnCancelRect = new Rectangle(x + textWidth, y, BUTTON_SIZE, BUTTON_SIZE);
            if (btnOkRect != null) {
                btnCancelRect.x += delta;
            }
        }

        // Don't display if it doesn't fit the screen
        if (width + 10 >= getWidth() || height + 10 >= getHeight()) {
            return null;
        }
        return new Rectangle(x, y, width, height);
    }

    public void mouseDragged(MouseEvent e) {
        if (isEnableDragRect()) {
            if (!dragInProgress) {
                dragInProgress = true;
            } else {
                if (dragPoint >= 0 && startPoint != null && endPoint != null) {
                    switch (dragPoint) {
                        case LEFT_UPPER_CORNER:
                            startPoint = e.getPoint();
                            break;
                        case RIGHT_LOWER_CORNER:
                            endPoint = e.getPoint();
                            break;
                        case LEFT_LOWER_CORNER:
                            startPoint.x = e.getX();
                            endPoint.y = e.getY();
                            break;
                        case RIGHT_UPPER_CORNER:
                            startPoint.y = e.getY();
                            endPoint.x = e.getX();
                            break;
                        case LEFT_SIDE:
                            startPoint.x = e.getX();
                            break;
                        case UPPER_SIDE:
                            startPoint.y = e.getY();
                            break;
                        case RIGHT_SIDE:
                            endPoint.x = e.getX();
                            break;
                        case LOWER_SIDE:
                            endPoint.y = e.getY();
                            break;
                    }
                    firePropertyChange("dragRectDefined", this, getDragRect());
                } else {
                    endPoint = e.getPoint();
                }
            }
        }
        Point p = new Point(e.getPoint());
        if (p.x < 0) {
            p.x = 0;
        }
        if (p.y < 0) {
            p.y = 0;
        }
        firePropertyChange(GUIConstants.EVENT_MOUSE_MOVED, null, p);
        repaint();
    }

    public void mouseMoved(MouseEvent e) {
        if (isEnableDragRect()) {
            if (rectangleDefined) {
                int cType = getDragCorner(e);
                resetCursor(cType);
            }
        }
        firePropertyChange(GUIConstants.EVENT_MOUSE_MOVED, null, e.getPoint());
    }

    public void mouseClicked(MouseEvent e) {
        if (!isEnableDragRect() && !enablePoints) {
            return;
        }
        if (btnOkRect != null && btnOkRect.contains(e.getPoint())) {
            clickOk();
        } else if (btnCancelRect != null && btnCancelRect.contains(e.getPoint())) {
            Rectangle r = getDragRect();
            resetDragRect();
            firePropertyChange("cancelPressed", this, r);
        } else if (enablePoints) {
            startPoint = e.getPoint();
            endPoint = e.getPoint();
            rectangleDefined = true;
            repaint();
            return;
        }
        dragInProgress = false;
    }

    private void resetCursor(int dragType) {
        Window w = SwingUtilities.getWindowAncestor(this);
        switch (dragType) {
            case LEFT_UPPER_CORNER:
                w.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                break;
            case RIGHT_UPPER_CORNER:
                w.setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                break;
            case RIGHT_LOWER_CORNER:
                w.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                break;
            case LEFT_LOWER_CORNER:
                w.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                break;
            case LOWER_SIDE:
            case UPPER_SIDE:
                w.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                break;
            case RIGHT_SIDE:
            case LEFT_SIDE:
                w.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                break;
            default:
                w.setCursor(Cursor.getDefaultCursor());
                break;
        }
    }

    public void clickOk() {
        Rectangle r = getDragRect();
        resetDragRect();
        firePropertyChange("okPressed", this, r);
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        resetCursor(-1);
    }

    public void mousePressed(MouseEvent e) {
        if (!isEnableDragRect() && !enablePoints) {
            return;
        }
        if (rectangleDefined) {
            dragPoint = getDragCorner(e);
        } else {
            startPoint = e.getPoint();
            endPoint = null;
            repaint();
        }
    }

    private int getDragCorner(MouseEvent e) {
        if (startPoint != null && endPoint != null) {
            int x = e.getX();
            int y = e.getY();
            Rectangle r = getNonScaledDragRect();
            int sx = r.x;
            int sy = r.y;
            int ex = r.x + r.width;
            int ey = r.y + r.height;
            if (Math.abs(x - sx) <= DRAG_TOLERANCE) {
                if (Math.abs(y - sy) <= DRAG_TOLERANCE) {
                    return LEFT_UPPER_CORNER;
                } else if (Math.abs(y - ey) <= DRAG_TOLERANCE) {
                    return LEFT_LOWER_CORNER;
                } else if (y > Math.min(sy, ey) && y < Math.max(sy, ey)) {
                    return LEFT_SIDE;
                }
            } else if (Math.abs(x - ex) <= DRAG_TOLERANCE) {
                if (Math.abs(y - sy) <= DRAG_TOLERANCE) {
                    return RIGHT_UPPER_CORNER;
                } else if (Math.abs(y - ey) <= DRAG_TOLERANCE) {
                    return RIGHT_LOWER_CORNER;
                } else if (y > Math.min(sy, ey) && y < Math.max(sy, ey)) {
                    return RIGHT_SIDE;
                }
            } else if (Math.abs(y - sy) <= DRAG_TOLERANCE) {
                if (x > Math.min(sx, ex) && x < Math.max(sx, ex)) {
                    return UPPER_SIDE;
                }
            } else if (Math.abs(y - ey) <= DRAG_TOLERANCE) {
                if (x > Math.min(sx, ex) && x < Math.max(sx, ex)) {
                    return LOWER_SIDE;
                }
            }
        }
        return -1;
    }

    public void mouseReleased(MouseEvent e) {
        if (!isEnableDragRect() && !enablePoints) {
            return;
        }
        if (dragInProgress && !rectangleDefined) {
            endPoint = e.getPoint();
            Rectangle r = getDragRect();
            firePropertyChange("dragRectDefined", this, r);
            rectangleDefined = true;
            repaint();
        }
        dragInProgress = false;
    }

    public void resetDragRect() {
        startPoint = null;
        endPoint = null;
        rectangleDefined = false;
        repaint();
        firePropertyChange("dragRectDefined", this, null);
    }

    private Rectangle getNonScaledDragRect() {
        if (startPoint != null && endPoint != null) {
            int startX = Math.max(startPoint.x, 0);
            int endX = Math.max(endPoint.x, 0);
            int startY = Math.max(startPoint.y, 0);
            int endY = Math.max(endPoint.y, 0);
            int x = Math.min(startX, endX);
            x = Math.max(x, 0);
            int y = Math.min(startY, endY);
            y = Math.max(y, 0);
            int w = Math.abs(startX - endX);
            w = x + w >= getWidth() ? getWidth() - x - 1 : w;
            int h = Math.abs(startY - endY);
            h = y + h >= getHeight() ? getHeight() - y - 1 : h;
            return new Rectangle(x, y, w, h);
        }
        return null;
    }

    public Rectangle getDragRect() {
        Rectangle r = getNonScaledDragRect();
        return getDragRect(r);
    }

    private Rectangle getDragRect(Rectangle r) {
        if (r != null) {
            r = new Rectangle(r);
            r.x = Math.round(r.x / zoomFactor);
            r.y = Math.round(r.y / zoomFactor);
            r.width = Math.round(r.width / zoomFactor);
            r.height = Math.round(r.height / zoomFactor);
        }
        return r;
    }

    public void setEnableDragRect(boolean enableDragRect) {
        this.enableDragRect = enableDragRect;
        removeMouseListener(this);
        removeMouseMotionListener(this);
        if (enableDragRect || enablePoints) {
            enableEvents(MouseEvent.MOUSE_MOVED | MouseEvent.MOUSE_PRESSED);
            addMouseListener(this);
            addMouseMotionListener(this);
        } else {
            disableEvents(MouseEvent.MOUSE_MOVED | MouseEvent.MOUSE_PRESSED);
        }
    }

    /**
     * This is necessary to enable key events for this JPanel.
     * Override this method to return true, so we can get keyTyped
     * events.
     *
     * @return true for this class
     */
//    public boolean isFocusable() {
//        return true;
//    }
    public int getZoomFactor() {
        return zoomFactor;
    }

    public void setZoomFactor(int zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    public boolean isOkBtnVisible() {
        return okBtnVisible;
    }

    public void setOkBtnVisible(boolean okBtnVisible) {
        this.okBtnVisible = okBtnVisible;
        if (!okBtnVisible) {
            btnOkRect = null;
        }
    }

    public boolean isCancelBtnVisible() {
        return cancelBtnVisible;
    }

    public void setCancelBtnVisible(boolean cancelBtnVisible) {
        this.cancelBtnVisible = cancelBtnVisible;
        if (!cancelBtnVisible) {
            btnCancelRect = null;
        }
    }

    // TODO: implement drag bounds
    public Dimension getDragBounds() {
        return dragBounds;
    }

    public void setDragBounds(Dimension dragBounds) {
        this.dragBounds = dragBounds;
    }

    /**
     * @return the enablePoints
     */
    public boolean isEnablePoints() {
        return enablePoints;
    }

    /**
     * @param enablePoints the enablePoints to set
     */
    public void setEnablePoints(boolean enablePoints) {
        this.enablePoints = enablePoints;
        setEnableDragRect(isEnableDragRect());
    }

    /**
     * @return the enableDragRect
     */
    public boolean isEnableDragRect() {
        return enableDragRect;
    }

    /**
     * Dummy method to keep compatibility with the Enterprise version.
     * @param enabled doesn't matter because the method does nothing.
     */
    public void setEnableClickPoint(boolean enableClickPoint) {
    }

    /**
     * Dummy method to keep compatibility with the Enterprise version.
     * @return always returns null.
     */
    public Point getClickPoint() {
        return null;
    }
}
