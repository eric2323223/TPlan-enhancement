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
 *g
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.tplan.robot.gui.components;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.util.Utils;
import java.awt.Component;
import java.awt.Cursor;
import java.io.File;
import java.net.URL;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

/**
 * <p>Hyperlink listener for HTML documents customized for {@product.name}. It
 * has the following link behavior:</p>
 *
 * <ul>
 * <li>If the URL is like <i>"http://preferences/nodenamekey"</i>, it opens the
 * Preferences window. The <i>nodenamekey</i> serves as a resource bundle key
 * to identify the node to select.</li>
 * <li>If the URL is like <i>"http://localdoc/path"</i>, it gets resolved
 * against the current installation path and the URL is changed to point to
 * the local document in the product JAR file or class path.</li>
 * </ul>
 *
 * <p>Valid HTTP links are then opened in an external browser. The listener also
 * makes sure that when mouse pointer enters the link, the cursor changes
 * appropriately and the {@product.name} status bar displays the link URL (if visible).
 * </p>
 *
 *
 *
 * @product.signature
 */
public class CustomHyperlinkListener implements HyperlinkListener {

    private MainFrame mainFrame;
    private Cursor defaultCursor;

    /**
     * Constructor.
     * @param mainFrame main frame window. May be null though it will limit the
     * functionality of preference links and cursors.
     */
    public CustomHyperlinkListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        if (mainFrame != null) {
            this.defaultCursor = mainFrame.getCursor();
        }
    }

    private String convertUrl(URL url) {
        String s = "";
        if (url != null) {
            s = url.toString();
            String host = url.getHost();
            // Links like "http://preferences/node should open the Preferences window
            if ("preferences".equalsIgnoreCase(host)) {
                s = host + ":/" + url.getFile();

                // Local documents must be resolved against the install path.
                // This functionality allows to load the docs from the JAR file or class path.
            } else if ("localdoc".equalsIgnoreCase(host)) {
                // We get the help set file path from the help broker
                if (MainFrame.getHelpBroker() != null) {
                    File f = Utils.getHelpSetFile();
                    if (f != null) {
                        s = "file://" + f.getParent() + url.getFile();
                    } else { // Fail over to the online documentation set
                        s = url.getFile();
                        if (!s.startsWith("/")) {
                            s = "/" + s;
                        }
                        s = ApplicationSupport.APPLICATION_HOME_PAGE + "/docs/" + ApplicationSupport.APPLICATION_DOC_DIR_NAME + s;
                    }
                } else {
                    // Fallback to the English help file
                    s = "file://" + Utils.getInstallPath() + File.separator + "help" + File.separator + "en" + url.getFile();
                }
                if (url.getRef() != null) {
                    s += "#" + url.getRef();
                }
            }
        }
        return s;
    }

    /**
     * HyperlinkListener implementation which follows up HTTP links selected in the
     * panel.
     * @param e a hyperlink event.
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            URL url = e.getURL();
            if (e instanceof HTMLFrameHyperlinkEvent) {
            } else if (url != null) {
                try {
                    if ("preferences".equalsIgnoreCase(url.getHost())) {
                        if (mainFrame == null) {
                            mainFrame = (MainFrame) SwingUtilities.getAncestorOfClass(MainFrame.class, (Component) e.getSource());
                        }
                        if (mainFrame != null) {
                            String key = url.getFile();
                            if (key.startsWith("/")) {
                                key = key.substring(1);
                            }
                            mainFrame.showOptionsDialog(ApplicationSupport.getString(key), null);
                        }
                    } else if ("menuaction".equalsIgnoreCase(url.getHost())) {
                        // New to 2.0.6 - ability to invoke menu actions referred to
                        // by action command or resource bundle key to the menu item text
                        String action = url.getFile();
                        if (mainFrame != null && action != null) {
                            if (action.startsWith("/")) {
                                action = action.substring(1);
                            }
                            String msg = ApplicationSupport.getString(action);
                            JMenuBar mb = mainFrame.getJMenuBar();
                            JMenuItem temp, mi = null;
                            for (int i = 0; i < mb.getMenuCount(); i++) {
                                for (Component c : mb.getMenu(i).getMenuComponents()) {
                                    if (c instanceof JMenuItem) {
                                        temp = (JMenuItem) c;
                                        if (action.equals(temp.getActionCommand()) || (msg != null && msg.equals(temp.getText()))) {
                                            mi = temp;
                                            break;
                                        }
                                    }
                                }
                                if (mi != null) {
                                    break;
                                }
                            }
                            if (mi != null && mi.isEnabled()) {
                                mi.doClick();
                            }
                        }

                    } else if (Utils.execOpenURL(convertUrl(url)) != 0) {
//                        ((JEditorPane) e.getSource()).setPage(e.getURL());
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            if (mainFrame != null) {
                mainFrame.setCursor(defaultCursor);
                if (mainFrame.getStatusBar() != null) {
                    mainFrame.getStatusBar().getFieldLeft().setText("");
                }
            }
        } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            if (mainFrame != null) {
                mainFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
                if (mainFrame.getStatusBar() != null) {
                    mainFrame.getStatusBar().getFieldLeft().setText(convertUrl(e.getURL()));
                }
            }
        }
    }
}
