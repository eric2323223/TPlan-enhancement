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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.remoteclient.rfb.RfbConstants;
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.remoteclient.RemoteDesktopServerListener;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.preferences.ConfigurationChangeEvent;
import com.tplan.robot.preferences.ConfigurationChangeListener;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.scripting.ScriptManagerImpl;
import com.tplan.robot.imagecomparison.search.AbstractImagePattern;
//import com.tplan.robot.scripting.imagecomparison.PatternHandler;

import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.capabilities.KeyTransferCapable;
import com.tplan.robot.remoteclient.capabilities.PointerTransferCapable;
import com.tplan.robot.scripting.ScriptListener;
import com.tplan.robot.scripting.commands.CommandEvent;
import com.tplan.robot.scripting.commands.CommandListener;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.util.Utils;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Remote desktop viewer component. It displays image of the remote desktop.
 *
 * @product.signature
 */
public class DesktopViewer extends JPanel
        implements KeyListener, RfbConstants, ConfigurationChangeListener, MouseInputListener, MouseWheelListener,
        RemoteDesktopServerListener, ScriptListener, CommandListener, Action {

    private RemoteDesktopClient client;
    private boolean readOnly;   // True if we process keyboard and mouse events.
    private AbstractImagePattern imagePattern;
//    private PatternHandler ph;
    private boolean enableMouseStamps = "true".equals(System.getProperty("vncrobot.experimental"));
    private boolean debugThreads = System.getProperty("vncrobot.thread.debugThreads") != null;
    private boolean debugKeys = System.getProperty("vncrobot.thread.debugKeys") != null;
    private int zoomFactor = 100;
    private Image zoomedImage = null;//    private int scalingMethod = Image.SCALE_FAST;
    private boolean recordingMode = false;
    int idelay = ToolTipManager.sharedInstance().getInitialDelay();
    int rdelay = ToolTipManager.sharedInstance().getReshowDelay();
    int ddelay = ToolTipManager.sharedInstance().getDismissDelay();
    ScriptManager scriptHandler;
    UserConfiguration cfg;
    Window window;
    KeyStroke readOnlyKeyStroke;
    Map<String, Object> actionProperties = new HashMap();

    public DesktopViewer(RemoteDesktopClient client, ScriptManager scriptHandler, UserConfiguration cfg) {
        this.scriptHandler = scriptHandler;
        this.scriptHandler.addScriptListener(this);
        this.client = client;
        this.cfg = cfg;
        readOnly = cfg.getBoolean("rfb.readOnly").booleanValue();

        // Enhancement in 2.0.4/2.1.1 - shortcut key for Read Only mode
        String s = cfg.getString("viewer.readOnlyKeyStroke");
        if (s != null && !s.isEmpty()) {
            readOnlyKeyStroke = Utils.getKeyStroke(s);
        }
        if (readOnlyKeyStroke == null) {
            readOnlyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F10, KeyEvent.CTRL_MASK);
        }
        getInputMap().put(readOnlyKeyStroke, "viewer.readOnlyKeyStroke");
        getActionMap().put("viewer.readOnlyKeyStroke", this);

        if (client != null) {
            client.addServerListener(this);
        }

        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        cfg.addConfigurationListener(this);

        // Disable focus traversal keys mechanism which consumes keys like Tab, Alt+Tab etc.
        setFocusTraversalKeysEnabled(false);
//        ph = new PatternHandler(this);
//        ph.addPropertyChangeListener();

    }

    public void setClient(RemoteDesktopClient client) {
//        if (this.client != null) {
//            this.client
//        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (client != null && client.isConnected() && client.getDesktopWidth() > 0 && client.getDesktopHeight() > 0 && !client.isLocalDisplay()) {
            return new Dimension((int) (client.getDesktopWidth() * zoomFactor / 100), (int) (client.getDesktopHeight() * zoomFactor / 100));
        }
        return new Dimension((int) ((640.0f * zoomFactor) / 100), (int) ((480.0f * zoomFactor) / 100));
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension d = getPreferredSize();

        // Leave some vertical space for the error message panel
        return new Dimension(d.width, d.height - 120);
    }

//    @Override
//    public Dimension getMaximumSize() {
//        return getPreferredSize();
//    }
    @Override
    public void paint(Graphics g) {
        if (client != null && !client.isLocalDisplay() && client.getImage() != null) {
            if (debugThreads) {
                System.out.println("FrameBufferPanel.paint(): entering synchronized (rfb.getImage())");
            }
            synchronized (client.getImage()) {
                super.paint(g);
                Image img = client.getImage();
                if (zoomFactor != 100 && zoomedImage != null) {
//                    img = zoomedImage;
                }
//                if (window == null) {
//                    window = SwingUtilities.getWindowAncestor(this);
//                    Point loc = window.getLocationOnScreen();
//                    Rectangle r = getVisibleRect();
//                    Point p = getLocationOnScreen();
//
//                    System.out.println("loc=" + loc + ", visibleRect=" + r+", p="+p);
//                }
//                if (client.isDisplayLocal()) {
//                    Point p = getLocationOnScreen();
//                    Rectangle r = getVisibleRect();
//                    g.drawImage(img, 0, 0, img.getWidth(this), p.y, 0, 0, img.getWidth(this), p.y, this);
//                    g.drawImage(img, 0, p.y+r.height, img.getWidth(this), img.getHeight(this)-p.y-r.height, 0, p.y+r.height, img.getWidth(this), img.getHeight(this)-p.y-r.height, this);
////                    g.drawImage(img, 0, p.y, p.x, img.getHeight(this), 0, r.height, img.getWidth(this), img.getHeight(this), this);
//                } else {
                g.drawImage(img, 0, 0, this);
//                }
            }
            if (debugThreads) {
                System.out.println("FrameBufferPanel.paint(): leaving synchronized (rfb.getImage())");
            }
        } else {
            super.paint(g);
        }
        super.paintChildren(g);
    }

    public void keyPressed(KeyEvent evt) {
        if (debugKeys) {
            System.out.println("[id,modifiers,keyCode,keyChar,keyLocation]: KeyEvent " +
                    evt.getID() + " " + evt.getModifiers() + " " + Integer.toHexString(evt.getKeyCode()) + " " +
                    Integer.toHexString(evt.getKeyChar()) + " " + evt.getKeyLocation());
        }
        processLocalKeyEvent(evt);
    }

    public void keyReleased(KeyEvent evt) {
        keyPressed(evt);
    }

    public void keyTyped(KeyEvent evt) {
        evt.consume();
    }

    public void mousePressed(MouseEvent evt) {
        processLocalMouseEvent(evt, false);
    }

    public void mouseReleased(MouseEvent evt) {
        processLocalMouseEvent(evt, false);
    }

    public void mouseMoved(MouseEvent evt) {
        if (!hasFocus() && !readOnly) {
            this.requestFocus();
        }
//        Rectangle r = getPerimeterLine(evt.getPoint(), STAMPSIZE, true);
//        cache = getPixels(rfb.getImage(), r);
        processLocalMouseEvent(evt, true);
    }

    public void mouseDragged(MouseEvent evt) {
        processLocalMouseEvent(evt, true);
    }

    public void processLocalKeyEvent(KeyEvent evt) {

        // Part of enhancement in 2.0.5/2.1.1 - Read Only mode short cut.
        // When a key is pressed or released, make sure it is not present in the
        // input map. As the map contains mappings just for key presses, we have
        // to create a dummy key press for all other types of events to make sure
        // that neither the key press nor the release are sent to the client.
        KeyEvent pressKeyEvent = evt;
        if (evt.getID() != KeyEvent.KEY_PRESSED) {
            pressKeyEvent = new KeyEvent(evt.getComponent(), KeyEvent.KEY_PRESSED, evt.getWhen(),
                    evt.getModifiersEx(), evt.getKeyCode(), evt.getKeyChar(), evt.getKeyLocation());
        }

        // Proceed only if the input map does not contain the press key event
        // or if a script is being executed.
        // This allows to filter out Robot's GUI reserved shortcut keys from the
        // client-server communication.
        TestScriptInterpret ti = scriptHandler.getClientOwner(client);
        boolean isExecuting = ti != null && ti.isExecuting();
        boolean isKeyReserved = getInputMap().get(KeyStroke.getKeyStrokeForEvent(pressKeyEvent)) != null;

        if (isExecuting || !isKeyReserved) {
            if (client != null && !client.isLocalDisplay() && client.isConnected() && client instanceof KeyTransferCapable && ((KeyTransferCapable) client).isKeyTransferSupported()) {
                if (!readOnly || evt.getSource() instanceof ScriptManagerImpl) {
                    synchronized (client) {
                        try {
                            ((KeyTransferCapable) client).sendKeyEvent(evt);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        client.notify();
                    }
                }
            }
            // Don't ever pass keyboard events to AWT for default processing.
            evt.consume();
        }
    }
    static long time = System.currentTimeMillis();

    public void processLocalMouseEvent(MouseEvent evt, boolean moved) {
        if (client != null && !client.isLocalDisplay() && client.isConnected() && client instanceof PointerTransferCapable && ((PointerTransferCapable) client).isPointerTransferSupported()) {
            int zw = calculateZoomedCoordinate(client.getDesktopWidth());
            int zh = calculateZoomedCoordinate(client.getDesktopHeight());
            if (evt.getX() < zw && evt.getY() < zh) {

                if (zoomFactor != 100) {
//                    System.out.println("==== MOUSE EVENT ====\n"+"Original position="+evt.getPoint());
                    int dx = (int) (100 * evt.getX() / zoomFactor) - evt.getX();
                    int dy = (int) (100 * evt.getY() / zoomFactor) - evt.getY();
                    evt.translatePoint(dx, dy);
//                    System.out.println("recalculating mouse: dx="+dx+", dy="+dy+", new position="+evt.getPoint());
                }

                firePropertyChange(GUIConstants.EVENT_MOUSE_MOVED, null, evt.getPoint());
                if (!readOnly || evt.getSource() instanceof ScriptManagerImpl) {
                    if (moved) {
                        //TODO
//                        softCursorMove(evt.getX(), evt.getY());
                    }
                    /*                    if (enableMouseStamps && evt.getID() == MouseEvent.MOUSE_PRESSED) {
                    if (recordingMode) {
                    imagePattern = ph.getImagePatternForLocation(evt.getPoint(), rfb);
                    }
                    }
                     */
                    synchronized (client) {
                        try {
                            ((PointerTransferCapable) client).sendPointerEvent(evt, false);
                        } catch (IOException ex) {
                            // EOFException indicates that server closed the connection. We need to report it to user.
                            // Reporting depends on whether we are running in the console or GUI mode
                            ApplicationSupport.logSevere(ex.getMessage());
                            if (client.isConsoleMode()) {
                                System.out.println("An I/O error occured: " + ex.getMessage() + "\nExiting...");
                                System.exit(1);
                            } else {
                                if (scriptHandler.getExecutingTestScripts().size() > 0) {
                                    System.out.println("An I/O error occured: " + ex.getMessage() + "\nExiting...");
                                } else {
                                    JOptionPane.showMessageDialog(this,
                                            ex.getMessage(),
                                            ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.errorMessageWindowTitle"),
                                            JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        } catch (Exception ex) {
                            com.tplan.robot.ApplicationSupport.logSevere(ex.getMessage());
                            if (client.isConsoleMode()) {
                                System.out.println("Unknown error occured: " + ex.getMessage() + "\nExiting...");
                                ex.printStackTrace();
                                System.exit(1);
                            } else {
                                ex.printStackTrace();
                            }
                        }
                        client.notify();
                    }
                } else {
                    if (evt.getID() == MouseEvent.MOUSE_PRESSED) {
                        displayReadonlyTooltip(evt);
                    }
                }
            }
        }
    }

//---------------------------------------------------------------
    private void displayReadonlyTooltip(MouseEvent evt) {
        ToolTipManager tm = ToolTipManager.sharedInstance();
        this.setToolTipText(ApplicationSupport.getResourceBundle().getString("com.tplan.robot.gui.viewer.readOnlyToolTip"));
        MouseEvent e = new MouseEvent((Component) evt.getSource(), MouseEvent.MOUSE_MOVED, evt.getWhen(), evt.getModifiers(),
                evt.getX(), evt.getY(), 0, true);
        tm.mouseMoved(e);
        tm.unregisterComponent(this);
    }

    /**
     * This is necessary to enable key events for this JPanel.
     *
     * @return always returns true.
     */
    @Override
    public boolean isFocusable() {
        return true;
    }

    public void mouseClicked(MouseEvent evt) {
    }

    public void mouseEntered(MouseEvent evt) {
        if (readOnly) {
            ToolTipManager tm = ToolTipManager.sharedInstance();
            tm.setInitialDelay(0);
            tm.setReshowDelay(0);
            tm.setDismissDelay(5000);
            tm.unregisterComponent(this);
        }
    }

    public void mouseExited(MouseEvent evt) {
        ToolTipManager tm = ToolTipManager.sharedInstance();
        tm.setInitialDelay(idelay);
        tm.setReshowDelay(rdelay);
        tm.setDismissDelay(ddelay);
        tm.unregisterComponent(this);
    }

    void setCutText(String text) {
        try {
            if (client != null && client.isConnected()) {
                client.sendClientCutText(text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void configurationChanged(ConfigurationChangeEvent evt) {
        if (evt.getPropertyName().startsWith("rfb.readOnly")) {
            Boolean b = cfg.getBoolean("rfb.readOnly");
            readOnly = b == null ? false : b.booleanValue();
        }
    }

    public void serverMessageReceived(RemoteDesktopServerEvent evt) {
        int type = evt.getMessageType();
        if (type == RemoteDesktopServerEvent.SERVER_UPDATE_EVENT) {
            Rectangle r = evt.getUpdateRect();
            if (zoomFactor != 100 && zoomedImage != null) {

//                System.out.println("\nInitial: width="+r.width+", height="+r.height);

                // Increase the updated rectangle because otherwise we get scaling errors
//                int inc = 2 + Math.round(10f*(1/(float)r.getWidth()));
                int inc = 10;
                r = new Rectangle(r.x - inc, r.y - inc, r.width + 2 * inc, r.height + 2 * inc);
                r.x = Math.max(r.x, 0);
                r.y = Math.max(r.y, 0);
                r.width = Math.min(r.width, client.getDesktopWidth() - r.x);
                r.height = Math.min(r.height, client.getDesktopHeight() - r.y);
//                System.out.println("After:   width="+r.width+", height="+r.height);

                // Get the updated image rect
                BufferedImage img = (BufferedImage) client.getImage();
                img = img.getSubimage(r.x, r.y, r.width, r.height);

                // Calculate the scaled rectangle
                r = new Rectangle(calculateZoomedCoordinate(r.x),
                        calculateZoomedCoordinate(r.y),
                        calculateZoomedCoordinate(r.width),
                        calculateZoomedCoordinate(r.height));

                if (r.width > 0 && r.height > 0) {
                    // Scale the updated rect
//                    Image img2 = img.getScaledInstance(r.width, r.height, scalingMethod);
                    Image img2 = scale(img, r.width, r.height);

                    // Update the zoom image with the scaled updated rect
                    zoomedImage.getGraphics().drawImage(img2, r.x, r.y, this);
                }

            }
            repaint(0, r.x, r.y, r.width, r.height);
        } else if (type == RemoteDesktopServerEvent.SERVER_BELL_EVENT) {
            if (!client.isConsoleMode() && cfg.getBoolean("rfb.beepOnBell").booleanValue()) {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    public AbstractImagePattern getImagePattern() {
        return imagePattern;
    }

    public int getZoomFactor() {
        return zoomFactor;
    }

    public void setZoomFactor(int zoomFactor) {
        if (zoomFactor > 0 && this.zoomFactor != zoomFactor) {
            this.zoomFactor = zoomFactor;
//            System.out.println("zoom factor = "+zoomFactor);
            if (zoomFactor == 100) {
                zoomedImage = null;
            } else {
                updateZoomImage();
            }

            revalidate();
            repaint();
        }
    }

    private void updateZoomImage() {
        Image img = client.getImage();
        float factor = zoomFactor / 100.0f;
        int w = (int) (factor * img.getWidth(this));
        int h = (int) (factor * img.getHeight(this));
//        zoomedImage = new BufferedImage(w, h, ((BufferedImage) img).getType());
//        img = img.getScaledInstance(w, h, scalingMethod);
//        zoomedImage.getGraphics().drawImage(img, 0, 0, this);
        zoomedImage = scale((BufferedImage) img, w, h);
    }

    private int calculateZoomedCoordinate(int coordinate) {
        return zoomFactor != 100 ? (int) ((zoomFactor * coordinate) / 100) : coordinate;
    }

    public boolean isRecordingMode() {
        return recordingMode;
    }

    public void setRecordingMode(boolean recordingMode) {
        this.recordingMode = recordingMode;
    }

    private BufferedImage scale(BufferedImage img, int width, int height) {
        Graphics2D gin = img.createGraphics();
        GraphicsConfiguration gc = gin.getDeviceConfiguration();
        gin.dispose();

        BufferedImage out =
                gc.createCompatibleImage(width, height, Transparency.BITMASK);
        Graphics2D gout = out.createGraphics();
        gout.setComposite(AlphaComposite.Src);
        gout.drawImage(img, 0, 0, width, height, null);
        gout.dispose();
        return out;
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        processLocalMouseEvent(e, false);
    }

    public void commandEvent(CommandEvent e) {
        if (e.getActionCode().equals(CommandEvent.KEY_EVENT)) {
//            KeyEvent evt = (KeyEvent) e.getCustomObject();
//            switch (evt.getID()) {
//                case KeyEvent.KEY_PRESSED:
//                    keyPressed(evt);
//                    break;
//                case KeyEvent.KEY_RELEASED:
//                    keyReleased(evt);
//                    break;
//                case KeyEvent.KEY_TYPED:
//                    keyTyped(evt);
//                    break;
//                default:
//                    return;
//            }
        } else if (e.getActionCode().equals(CommandEvent.POINTER_EVENT)) {
//            MouseEvent evt = (MouseEvent) e.getCustomObject();
//            switch (evt.getID()) {
//                case MouseEvent.MOUSE_PRESSED:
//                    mousePressed(evt);
//                    break;
//                case MouseEvent.MOUSE_CLICKED:
//                    mouseClicked(evt);
//                    break;
//                case MouseEvent.MOUSE_RELEASED:
//                    mouseReleased(evt);
//                    break;
//                case MouseEvent.MOUSE_MOVED:
//                    mouseMoved(evt);
//                    break;
//                case MouseEvent.MOUSE_DRAGGED:
//                    mouseDragged(evt);
//                    break;
//            }
            MouseEvent evt = (MouseEvent) e.getCustomObject();
            firePropertyChange(GUIConstants.EVENT_MOUSE_MOVED, null, evt.getPoint());
        }
    }

    public void scriptEvent(ScriptEvent event) {
        if (event.getType() == ScriptEvent.SCRIPT_CLIENT_CREATED) {
            if (client != null) {
                client.removeServerListener(this);
            }
            client = event.getContext().getClient();
            client.addServerListener(this);
            repaint();
        }
    }

    public Object getValue(String key) {
        return actionProperties.get(key);
    }

    public void putValue(String key, Object value) {
        actionProperties.put(key, value);
    }

    public void actionPerformed(ActionEvent e) {
        // Toggle the Read Only mode
        MainFrame frame = MainFrame.getInstance();
        if (frame != null) {
            Object obj = frame.getActionManager().getToolbarButton("readonly");
            if (obj != null && obj instanceof JToggleButton) {
                JToggleButton btn = (JToggleButton)obj;
                btn.doClick();
            }
        }
    }
}
