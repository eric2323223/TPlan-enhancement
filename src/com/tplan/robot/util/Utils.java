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
package com.tplan.robot.util;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.remoteclient.rfb.RfbConstants;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.MainFrame;

import com.tplan.robot.gui.components.CustomHyperlinkListener;
import com.tplan.robot.images.ImageLoader;
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.TokenParserImpl;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.MinimalHTMLWriter;

/**
 * Static application-wide utility methods.
 * @product.signature
 */
public class Utils implements RfbConstants {

    private final static int SECOND = 1000;
    private final static int MINUTE = 60 * SECOND;
    private final static int HOUR = 60 * MINUTE;
    private final static int DAY = 24 * HOUR;
    public final static NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
    public final static NumberFormat fractionNumberFormat = new DecimalFormat("0.###");
    private static Map keyCodes;
    private static List supportedImageExtensions;
    private final static String[][] longTimeFmt = {
        {ApplicationSupport.getString("utils.milisecond"), ApplicationSupport.getString("utils.miliseconds")},
        {ApplicationSupport.getString("utils.second"), ApplicationSupport.getString("utils.seconds")},
        {ApplicationSupport.getString("utils.minute"), ApplicationSupport.getString("utils.minutes")},
        {ApplicationSupport.getString("utils.hour"), ApplicationSupport.getString("utils.hours")},
        {ApplicationSupport.getString("utils.day"), ApplicationSupport.getString("utils.days")},};
    private final static String[][] shortTimeFmt = {
        {ApplicationSupport.getString("utils.milisecond.short"), ApplicationSupport.getString("utils.miliseconds.short")},
        {ApplicationSupport.getString("utils.second.short"), ApplicationSupport.getString("utils.seconds.short")},
        {ApplicationSupport.getString("utils.minute.short"), ApplicationSupport.getString("utils.minutes.short")},
        {ApplicationSupport.getString("utils.hour.short"), ApplicationSupport.getString("utils.hours.short")},
        {ApplicationSupport.getString("utils.day.short"), ApplicationSupport.getString("utils.days.short")},};
    private static File jarFile = null;
    private static String installPath = null;
    private static int[] version = null;

    private static boolean hasProtocol(String uri) {
        return uri != null && uri.indexOf(":/") > 0;
    }

    public static URI getURI(String uri) throws URISyntaxException {
        if (!hasProtocol(uri)) {
            // Legacy code to support both the old-style VNC server names
            // like "localhost:1" as well as the new URI format like "rfb://localhost:5901".
            // When the protocol prefix is not present, we automatically handle it
            // as VNC protocol and the number after single colon is display number.
            // If the protocol is present, the number after single colon is port.
            uri = "rfb://" + parseServer(uri) + ":" + parseLegacyRfbPort(RFB_PORT_OFFSET, uri);
        } else if (uri.length() > 6 && uri.substring(0, 6).equalsIgnoreCase("file:/")) {
            // Special handling of back slash characters on Windows
            try {
                return new URI(uri);
            } catch (Exception ex) {
            }
            String file = uri.substring("file:/".length() + 1);
            return new File(file).toURI();
        }
        return new URI(uri);
    }

    // Method for old-style parsing of VNC server from '<server>:<display>' or '<server>::<port>'
    private static String parseServer(String str) {
        String s = str;
        if (s != null && s.indexOf(':') > -1) {
            s = str.substring(0, str.indexOf(':'));
        }
        return s;
    }

    // Method for old-style parsing of VNC port from '<server>:<display>' or '<server>::<port>'.
    // If no port is specified, the default VNC port 5900 is returned.
    public static int parseLegacyRfbPort(int portOffset, String str) {
        int port = portOffset;
        if (str != null) {
            int index = str.indexOf("::");
            if (index > -1) {
                port = Integer.parseInt(str.substring(index + 2));
            } else {
                index = str.indexOf(':');
                if (index > -1 && !str.substring(index).startsWith(":/")) {
                    port = portOffset + Integer.parseInt(str.substring(index + 1));
                }
            }
        }
        return port;
    }

    public static String parseParamName(String str) throws StringIndexOutOfBoundsException {
        return str.substring(0, str.indexOf('='));
    }

    public static String parseParamValue(String str) throws StringIndexOutOfBoundsException {
        return str.substring(str.indexOf('=') + 1);
    }

    public static int[] getVersion() {
        if (version != null) {
            return version;
        }
        String s[] = ApplicationSupport.APPLICATION_VERSION.split("\\.");
        version = new int[s.length];
        for (int i = 0; i < version.length; i++) {
            try {
                // A bug fix handling the suffix such as "Beta"
                String v = s[i];
                int index = 0;
                while (index < v.length() && Character.isDigit(v.charAt(index))) {
                    index++;
                }
                v = v.substring(0, index);
                version[i] = Integer.parseInt(v);
            } catch (Exception e) {
            }
        }
        return version;
    }

    public static Date getReleaseDate() {
        try {
            String s = ApplicationSupport.APPLICATION_BUILD;
            int i = s.indexOf('-') + 1;
            if (i > 0) {
                s = s.substring(i);
            }
            i = s.indexOf('.');
            if (i >= 0) {
                s = s.substring(0, i);
            }
            if (s.length() == 8) {
                int year = Integer.parseInt(s.substring(0, 4));
                int month = Integer.parseInt(s.substring(4, 6));
                int date = Integer.parseInt(s.substring(6, 8));
                Date d = new GregorianCalendar(year, month - 1, date).getTime();
                return d;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static String getProductNameAndVersion() {
        Object params[] = {
            ApplicationSupport.APPLICATION_NAME,
            ApplicationSupport.APPLICATION_VERSION,
            ApplicationSupport.APPLICATION_BUILD
        };
        String text = ApplicationSupport.getString("com.tplan.robot.product");
        if (text != null && text.length() > 0) {
            return MessageFormat.format(text, params);
        } else {
            return params[0] + " v" + params[1] + " (Build No. " + params[2] + ")";
        }
    }

    public static String getCopyright() {
        return ApplicationSupport.getString("com.tplan.robot.copyright");
    }

    public static String getLicenseText() {
        return ApplicationSupport.getString("com.tplan.robot.license");
    }

    public static String getTimePeriodForDisplay(long miliseconds, boolean longFormat) {
        return getTimePeriodForDisplay(miliseconds, longFormat, false);
    }

    public static String getTimePeriodForDisplay(long miliseconds, boolean longFormat, boolean showMiliseconds) {
        return getTimePeriodForDisplay(miliseconds, longFormat, showMiliseconds, false);
    }

    public static String getTimePeriodForDisplay(long miliseconds, boolean longFormat, boolean showMiliseconds, boolean showMillisAsSecondFraction) {
        String text;
        String words[][] = longFormat ? longTimeFmt : shortTimeFmt;

        if (showMiliseconds && miliseconds < SECOND) {
            text = (int) (miliseconds) == 1 ? words[0][0] : words[0][1];
            return numberFormat.format((long) (miliseconds)) + " " + text;
        } else if (miliseconds < MINUTE) {
            int seconds = (int) (miliseconds / SECOND);
            text = (int) (miliseconds / SECOND) == 1 ? words[1][0] : words[1][1];
            if (showMillisAsSecondFraction) {
                Double d = (double) miliseconds / SECOND;
                return fractionNumberFormat.format(d) + " " + text;
            } else {
                return numberFormat.format((long) (miliseconds / SECOND)) + " " + text
                        + (showMiliseconds ? " " + getTimePeriodForDisplay(miliseconds - seconds * SECOND, longFormat, showMiliseconds) : "");
            }
        } else if (miliseconds < HOUR) {
            int minutes = (int) (miliseconds / MINUTE);
            text = minutes == 1 ? words[2][0] : words[2][1];
            return numberFormat.format(minutes) + " " + text + " " + getTimePeriodForDisplay(miliseconds - minutes * MINUTE, longFormat, showMiliseconds);
        } else if (miliseconds < DAY) {
            int hours = (int) (miliseconds / HOUR);
            text = hours == 1 ? words[3][0] : words[3][1];
            return numberFormat.format(hours) + " " + text + " " + getTimePeriodForDisplay(miliseconds - hours * HOUR, longFormat, showMiliseconds);
        }
        int days = (int) (miliseconds / DAY);
        text = days == 1 ? words[4][0] : words[4][1];
        return numberFormat.format(days) + " " + text + " " + getTimePeriodForDisplay(miliseconds - days * DAY, longFormat, showMiliseconds);
    }

    public static String getDateStamp(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(UserConfiguration.getInstance().getString("scripting.dateFormat"));
        return format.format(date);
    }

    public static String getTimeStamp(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(UserConfiguration.getInstance().getString("scripting.timeFormat"));
        return format.format(date);
    }

    /**
     * Get the extension of a file.
     *
     * @param f a file.
     * @return file extension in lowercase or null if the file has no extension.
     */
    public static String getExtension(java.io.File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
     * Get the extension of a file.
     *
     * @param f a file.
     * @return file extension null if the file has no extension.
     */
    public static String getExtension(String f) {
        String ext = null;
        int i = f.lastIndexOf('.');

        if (i > 0 && i < f.length() - 1) {
            ext = f.substring(i + 1);
        }
        return ext;
    }

    public static void copyFile(File in, File out) throws Exception {
        FileChannel sourceChannel = new FileInputStream(in).getChannel();
        FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }

    public static void copy(InputStream in, File out) throws IOException {
        FileOutputStream destination = null;
        byte[] buffer;
        int bytes_read;

        try {
            destination = new FileOutputStream(out);
            buffer = new byte[1024];
            while (true) {
                bytes_read = in.read(buffer);
                if (bytes_read == -1) {
                    break;
                }
                destination.write(buffer, 0, bytes_read);
            }
        } // No matter what happens, always close any streams we've opened.
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    ;
                }
            }
            if (destination != null) {
                try {
                    destination.close();
                } catch (IOException e) {
                    ;
                }
            }
        }
    }

    public static String getFullPath(File f) {
        if (f != null) {
            String path;
            try {
                path = f.getCanonicalPath();
            } catch (IOException e) {
                path = f.getAbsolutePath();
            }
            return path;
        }
        return null;
    }

    public static Map<String, Integer> getKeyCodeTable() {
        if (keyCodes == null) {
            keyCodes = new HashMap();

            Field field;
            String name;
            KeyEvent event = new KeyEvent(new JLabel(), KeyEvent.KEY_PRESSED, 0, 0, 0, ' ');
            Field fields[] = KeyEvent.class.getFields();

            // The following keys represent the Windows and Properties (Context Menu) keys.
            // Since these were introduced in Java 1.5, we explicitly insert them here to support Java 1.4.
            keyCodes.put("CONTEXT_MENU", new Integer(0x020D));

            // Now use the Java Reflection API to retrieve constants defined in the KeyEvent class
            for (int i = 0; i < fields.length; i++) {
                field = fields[i];
                if (field.getName().startsWith("VK_")) {
                    name = field.getName().replaceAll("VK_", "");
                    name = name.replaceAll("_", "");
                    try {
                        keyCodes.put(name, new Integer(field.getInt(event)));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

            // These are just shortcuts of already existing key codes
            keyCodes.put("ESC", new Integer(KeyEvent.VK_ESCAPE));
            keyCodes.put("DEL", new Integer(KeyEvent.VK_DELETE));
            keyCodes.put("PRTSCR", new Integer(KeyEvent.VK_PRINTSCREEN));
            keyCodes.put("PGUP", new Integer(KeyEvent.VK_PAGE_UP));
            keyCodes.put("PGDOWN", new Integer(KeyEvent.VK_PAGE_DOWN));
            keyCodes.put("INS", new Integer(KeyEvent.VK_INSERT));
            keyCodes.put("CTRL", new Integer(KeyEvent.VK_CONTROL));

            // Fix in 2.0.1 - this code makes a single Windows key press work fine
            keyCodes.put("WINDOWS", new Integer(0xFFEB));

            // Fix in 2.0.2 - these are numeric values which are not handled well
            // by Java

//          case VK_MULTIPLY: return Toolkit.getProperty("AWT.multiply", "NumPad *");
//          case VK_ADD: return Toolkit.getProperty("AWT.add", "NumPad +");
//          case VK_SEPARATOR: return Toolkit.getProperty("AWT.separator", "NumPad ,");
//          case VK_SUBTRACT: return Toolkit.getProperty("AWT.subtract", "NumPad -");
//          case VK_DECIMAL: return Toolkit.getProperty("AWT.decimal", "NumPad .");
//          case VK_DIVIDE: return Toolkit.getProperty("AWT.divide", "NumPad /");
//          case VK_DELETE: return Toolkit.getProperty("AWT.delete", "Delete");
//          case VK_NUM_LOCK: return Toolkit.getProperty("AWT.numLock", "Num Lock");
//          case VK_SCROLL_LOCK: return Toolkit.getProperty("AWT.scrollLock", "Scroll Lock");
        }
        return keyCodes;
    }

    // Added in 2.0.1 for low level debugging purposes.
    public static String getKeyName(int keyCode) {
        if ((keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) || (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) || (keyCode >= 'a' && keyCode <= 'z')) {
            return "" + new Character((char) keyCode);
        }
        Map<String, Integer> k = getKeyCodeTable();
        for (String key : k.keySet()) {
            if (k.get(key).intValue() == keyCode) {
                return key;
            }
        }
        // Modifiers have special codes
        switch (keyCode) {
            case 0xFFE3:
                return "CTRL";
            case 0xFFE9:
                return "ALT";
            case 0xFFE1:
                return "SHIFT";
            case 0xFFEB:
                return "META";

        }
        return KeyStroke.getKeyStroke(keyCode, 0).toString();
    }

    public static int getKeyCode(String codeText) {
        // Fix in 2.0.1 - letters and digits return directly the ASCII value.
        // This fixes incorrect mapping of lower case characters (which are
        // missing in the KeyEvent.VK_XXX constant list) to other keys of the same code
        if (codeText.length() == 1) {
            char c = codeText.charAt(0);
            if (Character.isLetterOrDigit(c)) {
                return c;
            }
        }
        if (getKeyCodeTable().containsKey(codeText.toUpperCase())) {
            return ((Number) keyCodes.get(codeText.toUpperCase())).intValue();
        } else if (codeText.length() == 1) {
            char c = codeText.charAt(0);
            return c;
//            KeyStroke k = KeyStroke.getKeyStroke(c);
//            if (k.getKeyCode() != KeyEvent.VK_UNDEFINED) {
//                return k.getKeyCode();
//            }
        }
        return KeyEvent.VK_UNDEFINED;
    }

    public static KeyStroke getKeyStroke(String text) {
        if (text == null || text.trim().equals("")) {
            return null;
        }
        String keys[];
        int keyCode;
        try {
            keys = text.trim().split("\\+");

            // Process the last token -> it must be the key like F1 to F12, A, B, ...
            String value = keys[keys.length - 1];

            keyCode = getKeyCode(value);
            if (keyCode == KeyEvent.VK_UNDEFINED && value.length() > 1) {
                return null;
            }
        } catch (Exception ex) {
            return null;
        }

        String token;
        int modifiers = 0;

        // Check all the modifiers
        for (int i = 0; i < keys.length - 1; i++) {
            token = keys[i].toUpperCase();
            if (token.equals("SHIFT")) {
                modifiers = modifiers | KeyEvent.SHIFT_MASK;
            } else if (token.equals("ALT")) {
                modifiers = modifiers | KeyEvent.ALT_MASK;
            } else if (token.equals("CTRL")) {
                modifiers = modifiers | KeyEvent.CTRL_MASK;
            } else {
                return null;
            }
        }
        return KeyStroke.getKeyStroke(keyCode, modifiers);
//        return KeyStroke.getKeyStroke(text.replaceAll("\\+", " ").toLowerCase());
    }

    public static String escapeUnescapedDoubleQuotes(String s) {
        if (s != null && !s.equals("")) {
            return s.replaceAll("\\\\\"", "\\\"").replaceAll("\\\"", "\\\\\"");
        }
        return s;
    }

    /**
     * Get pixels of an image rectangle.
     *
     * @param img an image.
     * @param r   rectangle to grab the pixels from.
     * @return integer array of pixels.
     */
    public static int[] getPixels(Image img, Rectangle r) {
        int width = r.width;
        int height = r.height;
        int ai[] = null;

        if (img instanceof BufferedImage) {
            try {
                BufferedImage bi = (BufferedImage) img;
                width = Math.max(0, Math.min(width, bi.getWidth() - r.x));
                height = Math.max(0, Math.min(height, bi.getHeight() - r.y));
                ai = new int[width * height];
                ai = ((BufferedImage) img).getRGB(r.x, r.y, width, height, ai, 0, width);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            ai = new int[width * height];
            PixelGrabber pixelgrabber =
                    new PixelGrabber(img, r.x, r.y, width, height, ai, 0, width);
            try {
                pixelgrabber.grabPixels();
            } catch (InterruptedException interruptedexception) {
                interruptedexception.printStackTrace();
            }
        }
        return ai;
    }

    public static boolean isWindowsLocalhost(String host, int port) {
        boolean isLocal = false;
        if (host != null) {
            host = host.toLowerCase().trim();
            if (host.startsWith("localhost") || host.startsWith("127.0.0")) {
                isLocal = true;
            }
        }

        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");

        return isLocal && osName.indexOf("Windows") >= 0;
    }

    public static boolean isHostEqual(String host, int port, RemoteDesktopClient client) {
        if (client.isConnected() || client.isConnecting()) {
            try {
                InetAddress a1 = InetAddress.getByName(client.getHost());
                InetAddress a2 = InetAddress.getByName(host);
                int port2 = client.getPort();
                if (port2 <= 0) {
                    port2 = client.getDefaultPort();
                }
                return a1.equals(a2) && (port == port2);
            } catch (UnknownHostException e) {
            }
        }
        return false;
    }

    /**
     * Show an error window. This method avoids JOptionPane and creates its own
     * dialog. This resolves the issue that JOptionPane doesn't wrap long messages
     * and displays wide window running out of the screen bounds. If an exception
     * is passed as an argument, its stack trace is displayed in a separate text
     * area inserted into a scroll pane.
     * @param parentComponent parent component (owner). It should be either a
     * JFrame or JDialog instance or null.
     * @param title message title. A default one will be provided if the argument is null.
     * @param message error message text. A default one will be provided if the argument is null.
     * @param ex an exception to display. May be null.
     */
    public static void showErrorDialog(Component parentComponent, String title, String message, Throwable ex) {
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        if (title == null) {
            title = res.getString("errDlgTitle");
        }
        if (message == null) {
            message = res.getString("errDlgMessage");
        }

        JScrollPane scroll = null;
        if (ex != null) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            JTextArea txa = new JTextArea(sw.toString());
            txa.setEditable(false);
            scroll = new JScrollPane(txa);
            scroll.setPreferredSize(new Dimension(500, 280));
        }

        Component txtNorth;
        if (message.startsWith("<html>")) {
            JEditorPane jta = new JEditorPane("text/html", message);
            jta.addHyperlinkListener(new CustomHyperlinkListener((MainFrame) SwingUtilities.getAncestorOfClass(MainFrame.class, jta)));

            // Set the JEditorPane default font to the JLabel one
            Font font = UIManager.getFont("Label.font");
            String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
            ((HTMLDocument) jta.getDocument()).getStyleSheet().addRule(bodyRule);

            jta.setEditable(false);
            jta.setOpaque(false);
//            jta.setPreferredSize(new Dimension(500, 80));
            JScrollPane scr = new JScrollPane(jta);
            txtNorth = scr;
        } else {
            JTextArea txaNorth = new JTextArea(message);
            txaNorth.setLineWrap(true);
            txaNorth.setOpaque(false);
            txaNorth.setEditable(false);
            if (scroll == null) {
                txaNorth.setPreferredSize(new Dimension(500, 80));
                JScrollPane scr = new JScrollPane(txaNorth);
                txtNorth = scr;
            } else {
                txtNorth = txaNorth;
            }
        }

        JDialog dlg;
        if (parentComponent instanceof JFrame) {
            dlg = new JDialog((JFrame) parentComponent, title, true);
        } else if (parentComponent instanceof JDialog) {
            dlg = new JDialog((JDialog) parentComponent, title, true);
        } else {
            dlg = new JDialog(SwingUtilities.getWindowAncestor(parentComponent), title, Dialog.ModalityType.APPLICATION_MODAL);
        }
        JPanel content = new JPanel(new BorderLayout(10, 10));
        if (scroll != null) {
            content.add(txtNorth, BorderLayout.NORTH);
            content.add(scroll, BorderLayout.CENTER);
        } else {
            content.add(txtNorth, BorderLayout.CENTER);
        }
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        final JDialog d = dlg;
        ActionListener la = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                d.dispose();
            }
        };

        JButton defaultBtn = new JButton(res.getString("btnOk"));
        buttons.add(defaultBtn);
        defaultBtn.addActionListener(la);
        content.add(buttons, BorderLayout.SOUTH);
        dlg.setContentPane(content);
        dlg.pack();
        dlg.setLocationRelativeTo(dlg.getOwner());
        if (defaultBtn != null) {
            dlg.getRootPane().setDefaultButton(defaultBtn);
        }
        dlg.setVisible(true);
    }

    /**
     * Display a warning message which contains a check box like 'Do not display this message again'.
     * It is a replacement of the JOptionPane messages.
     *
     * @param parentComponent  parent component (must be a JFrame or JDialog)
     * @param title            message window title
     * @param message          message to be displayed
     * @param checkboxLabel    text of the check box
     * @param configurationKey configuration key of the check box option (should be an int option because
     *                         the method saves button index chosen by the user rather than a plain boolean)
     * @param options          an array of labels for buttons
     * @param defaultBtnNo     default button index
     * @return selected option (i.e. selected button index), or -1 if the dialog is not closed through the buttons
     */
    public static int showConfigurableMessageDialog(Component parentComponent, String title,
            String message, String checkboxLabel, String configurationKey, Object[] options, int defaultBtnNo) {
        return showConfigurableMessageDialog(parentComponent, title, message, checkboxLabel, configurationKey, options, defaultBtnNo, true);

    }

    public static int showConfigurableMessageDialog(Component parentComponent, String title,
            String message, String checkboxLabel, String configurationKey, Object[] options, int defaultBtnNo, boolean showResetHint) {

        Integer configValue = null;

        if (configurationKey != null) {
            configValue = UserConfiguration.getInstance().getInteger(configurationKey);
            if (configValue == null) {
                configValue = new Integer(-1);
            }
        }

        JPanel pnl = new JPanel(new BorderLayout(10, 10));
//        JTextArea txa = new JTextArea(message);
//        pnl.add(txa, BorderLayout.CENTER);
//        txa.setEditable(false);
//        txa.setOpaque(false);

        Component txtNorth;
        if (message.startsWith("<html>")) {
            JEditorPane jta = new JEditorPane("text/html", message);
            jta.addHyperlinkListener(new CustomHyperlinkListener(MainFrame.getInstance()));

            // Set the JEditorPane default font to the JLabel one
            Font font = UIManager.getFont("Label.font");
            String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
            ((HTMLDocument) jta.getDocument()).getStyleSheet().addRule(bodyRule);

            jta.setEditable(false);
            jta.setOpaque(false);
//            jta.setPreferredSize(new Dimension(500, 80));
            JScrollPane scr = new JScrollPane(jta);
            txtNorth = scr;
        } else {
            JTextArea txaNorth = new JTextArea(message);
            txaNorth.setLineWrap(true);
            txaNorth.setOpaque(false);
            txaNorth.setEditable(false);
            txaNorth.setPreferredSize(new Dimension(500, 80));
            JScrollPane scr = new JScrollPane(txaNorth);
            txtNorth = scr;
        }
        pnl.add(txtNorth, BorderLayout.CENTER);

        JCheckBox box = null;
        if (configurationKey != null) {
            JPanel boxPanel = new JPanel(new BorderLayout());
            box = new JCheckBox(checkboxLabel);
            box.setSelected(configValue.intValue() >= 0);
            boxPanel.add(box, BorderLayout.NORTH);
            if (showResetHint) {
                JLabel hint = new JLabel(ApplicationSupport.getString("warningMessages.hint"));
                Font f = hint.getFont();
                hint.setFont(new Font(f.getName(), f.getStyle(), f.getSize() - 2));
                boxPanel.add(hint, BorderLayout.SOUTH);
            }
            pnl.add(boxPanel, BorderLayout.SOUTH);
        }

        JDialog dlg;
        if (parentComponent instanceof JFrame) {
            dlg = new JDialog((JFrame) parentComponent, title, true);
        } else {
            dlg = new JDialog((JDialog) parentComponent, title, true);
        }
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.add(pnl, BorderLayout.CENTER);
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        final Map t = new HashMap();
        final JDialog d = dlg;
        ActionListener la = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Integer i = (Integer) t.get(e.getSource());
                if (i != null) {
                    t.put("result", i);
                }
                d.dispose();
            }
        };

        JButton defaultBtn = null;
        for (int i = 0; i < options.length; i++) {
            JButton btn = new JButton(options[i].toString());
            t.put(btn, new Integer(i));
            buttons.add(btn);
            if (i == defaultBtnNo) {
                defaultBtn = btn;
            }
            btn.addActionListener(la);
        }
        content.add(buttons, BorderLayout.SOUTH);
        dlg.setContentPane(content);

        dlg.pack();
        dlg.setLocationRelativeTo(parentComponent);
        if (defaultBtn != null) {
            dlg.getRootPane().setDefaultButton(defaultBtn);
        }
        dlg.setVisible(true);

        Integer result = (Integer) t.get("result");
        if (result == null) {
            result = new Integer(-1);
        }

        if (box != null) {
            if (box.isSelected()) {
                UserConfiguration.getInstance().setInteger(configurationKey, result);
            } else {
                UserConfiguration.getInstance().setInteger(configurationKey, new Integer(-1));
            }
        }
//        System.out.println("result: "+result.intValue());
        return result.intValue();
    }

    public static void registerDialogForEscape(JDialog dlg, JButton closeButton) {
        // Close the window on escape
        String actionKey = "escapeclose";
        final JButton btnClose = closeButton;
        dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);
        dlg.getRootPane().getActionMap().put(actionKey, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                btnClose.doClick();
            }
        });
    }

    public static void registerDialogForEscape(JFrame frame, JButton closeButton) {
        // Close the window on escape
        String actionKey = "escapeclose";
        final JButton btnClose = closeButton;
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);
        frame.getRootPane().getActionMap().put(actionKey, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                btnClose.doClick();
            }
        });
    }

    public static void registerComponentForEscape(JComponent comp, JButton closeButton) {
        // Close the window on escape
        String actionKey = "escapeclose";
        final JButton btnClose = closeButton;
        comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);
        comp.getActionMap().put(actionKey, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                btnClose.doClick();
            }
        });
    }

    public static void writeLog(PrintStream writer, String msg) {
        if (writer != null) {
            writer.print(msg);
        }
    }

    /**
     * Get lower case list of image file extensions supported by this version
     * of Java.
     *
     * @return list of supported file extensions in lower case, for example
     * ["png", "jpg", "jpeg", "bmp"].
     */
    public static List<String> getSupportedImageExtensions() {
        if (supportedImageExtensions == null) {
            supportedImageExtensions = new ArrayList();
            String[] supportedFormats = ImageIO.getWriterFormatNames();
            for (int i = 0; i < supportedFormats.length; i++) {
                supportedFormats[i] = supportedFormats[i].toLowerCase();
            }
            Arrays.sort(supportedFormats);
            for (int i = 0; i < supportedFormats.length; i++) {
                if (!supportedImageExtensions.contains(supportedFormats[i])) {
                    supportedImageExtensions.add(supportedFormats[i].toLowerCase());
                }
            }
        }

        return supportedImageExtensions;
    }

    /**
     * Parse semicolon separated parameters as a list of String values.
     *
     * @return a List which contains String instances.
     */
    public static List getListOfStrings(String value) {
        if (value == null) {
            return null;
        }

        List v = new ArrayList();

        if (value.toString().trim().equals("")) {
            return v;
        }
        StringTokenizer tokenizer =
                new StringTokenizer(value, ";");

        while (tokenizer.hasMoreTokens()) {
            v.add(tokenizer.nextToken().toString());
        }

        return v;
    }

    public static String getMouseCoordinateText(Point p, BufferedImage image,
            boolean shortVersion, boolean isReadOnly, boolean displayRelativeCoordinates) {
        int[] c = getColorAt(p.x, p.y, image);
        String msg = !shortVersion || (isReadOnly && c != null)
                ? ApplicationSupport.getString("statusbar.mouseCoordinatesWithPixelColor")
                : ApplicationSupport.getString("statusbar.mouseCoordinates");
        if (p != null) {
            String cs = "";
            if (!shortVersion) {
                cs = "------";
            }
            if (c != null) {
                String t = c[0] > 15 ? Integer.toHexString(c[0]) : "0" + Integer.toHexString(c[0]);
                cs += t;
                t = c[1] > 15 ? Integer.toHexString(c[1]) : "0" + Integer.toHexString(c[1]);
                cs += t;
                t = c[2] > 15 ? Integer.toHexString(c[2]) : "0" + Integer.toHexString(c[2]);
                cs += t;
                Raster alphaRaster = image.getAlphaRaster();
                if (alphaRaster != null) {
                    int color[] = new int[alphaRaster.getNumBands()];
                    c = alphaRaster.getPixel(p.x, p.y, c);
                    if (c[0] < 0xFF) {
                        cs += " " + ApplicationSupport.getString("statusbar.transparentColorLabel");
                    }
                }
            }
            if (displayRelativeCoordinates) {
                Object params[] = {(int) (100 * p.getX() / image.getWidth()) + "%", (int) (100 * p.getY() / image.getHeight()) + "%", cs};
                msg = MessageFormat.format(msg, params);
            } else {
                Object params[] = {(int) p.getX() + "", (int) p.getY() + "", cs};
                msg = MessageFormat.format(msg, params);
            }
        } else {
            msg = " ";
        }
        return msg;
    }

    public static int[] getColorAt(int x, int y, BufferedImage image) {
        try {
            int ai[] = new int[4];
            image.getRaster().getPixels(x, y, 1, 1, ai);
            return ai;
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
        return null;
    }

    /**
     * Print out content of an integer array for debug purposes.
     * @param array an integer array
     * @param desc description
     */
    public static void debugArray(int[] array, String desc) {
        System.out.print(desc + "(length:" + array.length + ")=[");
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i] + (i == array.length - 1 ? "" : ","));
        }
        System.out.println("]");
    }

    /**
     * Print out content of an integer array for debug purposes.
     * @param array an integer array
     * @param desc description
     */
    public static void debugArray(byte[] array, int length, String desc) {
        String s = desc + "(length:" + length + ")=[";
        String index = addSpaces("", s.length());
        for (int i = 0; i < length; i++) {
            if (i % 10 == 0) {
                index = addSpaces(index, s.length() - index.length()) + i;
            }
            s += (Integer.toString(array[i] & 0xff, 16).toUpperCase() + (i == length - 1 ? "" : ", "));
        }
        s += "]";
        System.out.println(s + "\n" + index);
        System.out.flush();
    }

    private static String addSpaces(String s, int n) {
        for (; n > 0; n--) {
            s += " ";
        }
        return s;
    }

    /**
     * Print out content of an Object array for debug purposes.
     * @param array an Object array
     * @param desc description
     */
    public static void debugArray(Object[] array, String desc) {
        System.out.print(desc + "(length:" + array.length + ")=[");
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i] + (i == array.length - 1 ? "" : ","));
        }
        System.out.println("]");
    }

    /**
     * Paint default image into a BufferedImage. It currently consists of a
     * gradient white-to-orange background with the product logo and custom text
     * in the center.
     */
    public static void paintDefaultImage(Image image, Image logoIcon, String text) {
        JPanel pnl = new JPanel();
        int width = image == null ? 800 : image.getWidth(pnl);
        int height = image == null ? 600 : image.getHeight(pnl);

        Graphics2D g = (Graphics2D) image.getGraphics();
        GradientPaint paint = new GradientPaint(0, height / 4, Color.orange, width / 2, height / 2, Color.WHITE);
        g.setPaint(paint);
        g.fillRect(0, 0, width, height);

        FontMetrics fm = g.getFontMetrics(g.getFont());

        int logoWidth = logoIcon == null ? 0 : logoIcon.getWidth(pnl);
        int logoHeight = logoIcon == null ? 0 : logoIcon.getHeight(pnl);

        int x = (int) ((width - logoWidth) / 2);
        int y = (int) ((height - logoHeight - fm.getHeight()) / 2);
        if (logoIcon != null) {
            g.drawImage(logoIcon, x, y, pnl);
        }

        x = (int) ((width - fm.stringWidth(text)) / 2);
        y = y + logoHeight + fm.getHeight() + 2;
        g.setColor(Color.BLACK);
        g.drawString(text, x, y);
    }
    /**
     * Open a URL in web browser.
     * @param url a URL to open.
     * @return browser exit code. A value of 0 (zero) usually means success while
     * non-zero exit code indicates an error.
     */
    private static String lastBrowser = null;

    public static int execOpenURL(String url) {

        Boolean b = UserConfiguration.getInstance().getBoolean("webbrowser.custom");
        String customBrowser = UserConfiguration.getInstance().getString("webbrowser.path");

        if (b != null && b.booleanValue() && customBrowser != null && customBrowser.length() > 0) {
            if (customBrowser.indexOf("$url$") >= 0) {
                customBrowser = customBrowser.replace("$url$", "\"" + url + "\"");
            } else {
                customBrowser += " \"" + url + "\"";
            }
            try {
                Runtime.getRuntime().exec(customBrowser);
                return 0;
            } catch (Exception ex) {
                ex.printStackTrace();
                return 1;
            }
        } else {

            // JDK 1.6 - take advantage of Desktop (if supported)
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                    return 0;
                } catch (Exception ex) {
//                    ex.printStackTrace();
                }
            }

            // The old traditional way - try out all various browsers
            String osName = System.getProperty("os.name");
            try {

                if (osName.startsWith("Mac OS")) {
                    Class fileMgr = Class.forName("com.apple.eio.FileManager");
                    Method openURL = fileMgr.getDeclaredMethod("openURL",
                            new Class[]{String.class});
                    openURL.invoke(null, new Object[]{url});
                    return 0;
                }

                // Use the last successful browser
                if (lastBrowser != null) {
                    if (Runtime.getRuntime().exec(lastBrowser + " " + url).waitFor() == 0) {
                        return 0;
                    }
                }

                // Try the most known browsers
                String browser = null;
                String[] browsers = {
                    "firefox", "iexplore", "opera", "konqueror", "epiphany", "mozilla", "netscape", "chrome"};

                // On Windows try the default way of File Protocol Handler.
                // If it fails try the hardcoded list of browsers.
                if (osName.startsWith("Windows")) {
                    // For Windows use the "explorer" first
                    browsers = new String[]{
                                "explorer", "C:\\Program Files\\Internet Explorer\\iexplore.exe",
                                "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape", "chrome"
                            };
                    for (int count = 0; count < browsers.length && browser == null; count++) {
                        try {
                            String command = browsers[count] + " \"" + url + "\"";
                            Runtime.getRuntime().exec(command);
                            browser = browsers[count];
                            lastBrowser = browsers[count];
                            break;

                        } catch (Exception ex) {
                        }
                    }
                    if (browser != null) {
                        return 0;
                    }
                }

                // Try the most known browsers on Linux/Unix. Check their availability
                // through the 'which' command
                for (int count = 0; count < browsers.length; count++) {
                    if (Runtime.getRuntime().exec(
                            new String[]{"which", browsers[count]}).waitFor() == 0) {
                        browser = browsers[count];
                        lastBrowser = browser;
                        break;
                    }
                }
                if (browser == null) {
                    return 1;
                } else {
                    Runtime.getRuntime().exec(new String[]{browser, url});
                    return 0;
                }
            } catch (Exception e) {
                return 1;
            }
        }
    }

    /**
     * Set visibility of all created toolbar buttons.
     * @param visible
     */
    public static void setToolbarButtonsVisible(JToolBar toolBar, String action[], boolean visible) {
        String a;
        for (Component b : toolBar.getComponents()) {
            if (b instanceof AbstractButton) {
                if (action != null) {
                    for (String s : action) {
                        a = ((AbstractButton) b).getActionCommand();
                        if (s.equals(((AbstractButton) b).getActionCommand())) {
                            b.setVisible(visible);
                        }
                    }
                } else {
                    b.setVisible(visible);
                }
            } else if (!visible || action == null) {  // This applies to spacers
                b.setVisible(visible);
            }
        }
    }

    public static void setVisibilityOfComponents(Container c, Object exclude[], boolean visible) {
        boolean exc = false;
        for (Component co : c.getComponents()) {
            exc = false;
            if (exclude != null) {
                for (Object o : exclude) {
                    if (o.equals(co)) {
                        exc = true;
                        break;
                    }
                }
            }
            if (!exc) {
                co.setVisible(visible);
            }
        }
    }

    public static void centerOnScreen(Window win) {
        GraphicsEnvironment gd = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle gcbounds = gd.getMaximumWindowBounds();
        int x = gcbounds.x + (gcbounds.width - win.getWidth()) / 2;
        int y = gcbounds.y + (gcbounds.height - win.getHeight()) / 2;
        win.setLocation(x, y);
    }

    public static String getInstallPath() {
        return getInstallPath(Utils.class);
    }

    public static String getInstallPath(Class clazz) {
        if (installPath != null && (clazz == null || clazz.equals(Utils.class))) {
            return installPath;
        }
        if (clazz == null) {
            clazz = Utils.class;
        }
        File f = getJarFile(clazz);
        if (f != null) {
            return f.getParent();
        }
        String thisPath = clazz.getName().replace(".", "/") + ".class";
        URI url;

        try {
            url = clazz.getClassLoader().getResource(thisPath).toURI();

            String s = url.getPath();
            if (s.startsWith("file:")) {
                s = s.substring(s.indexOf("file:") + "file:".length());
            }
            if (s.endsWith(thisPath)) {
                s = s.substring(0, s.length() - thisPath.length());
            }

            // Fix for Windows - paths start with "/"
            File fl = new File(s);
            if (clazz.equals(Utils.class)) {
                installPath = getFullPath(fl);
            }
            return s;
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static File getJarFile() {
        return getJarFile(Utils.class);
    }

    public static File getJarFile(Class clazz) {
        if (jarFile != null && (clazz == null || Utils.class.equals(clazz))) {
            return jarFile;
        }
        if (clazz == null) {
            clazz = Utils.class;
        }
        String s = null;
        String thisPath = clazz.getName().replace(".", "/") + ".class";
        try {
            URI url = clazz.getClassLoader().getResource(thisPath).toURI();
            String protocol = url.getScheme();
            if (protocol != null && protocol.equals("jar")) {  // We are running from JAR file
//                System.out.println(url+"\nscheme="+url.getScheme()+" userInfo="+url.getUserInfo()+" host="+ url.getHost()
//                        +" port="+ url.getPort()+" path=" +url.getRawPath()+" query="+ url.getQuery()+" fragment="+ url.getRawFragment()
//                        +" authority="+url.getAuthority()+" SchemeSpecificPart="+url.getSchemeSpecificPart()+" "+url.getRawSchemeSpecificPart());
                String path = url.getRawSchemeSpecificPart();
                s = path.substring(0, path.indexOf("!"));
                URI uri = new URL(s).toURI();
                File f = new File(uri);
                if (clazz.equals(Utils.class)) {
                    jarFile = f;
                }
                return f;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * An exception safe variant of the {@link Thread#sleep(long)} method.
     * @param millis number of milliseconds to sleep for.
     */
    public static void sleep(long millis) {
        final long endTime = System.currentTimeMillis() + millis;
        long time;
        while ((time = System.currentTimeMillis()) < endTime) {
            try {
                Thread.sleep(Math.max(0, endTime - time));
            } catch (InterruptedException e) {
            }
        }
    }

    public static List<String> getStartCommand(String args[]) {
        List<String> cmd = new ArrayList();
        String jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        cmd.add(jvm);
        cmd.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(ApplicationSupport.class.getName());
        String appArgs[] = ApplicationSupport.getInputArguments();
        if (appArgs != null) {
            cmd.addAll(Arrays.asList(appArgs));
        }
        return cmd;
    }

    public static void restart(MainFrame frame, String args[]) {
        List<String> cmd = getStartCommand(args);
        final List<String> c = cmd;
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                ProcessBuilder pb = new ProcessBuilder(c);
                try {
                    pb.start();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        if (frame != null) {
            frame.exit();
        } else {
            System.exit(0);
        }
    }

    public static String getDefaultTemplatePath() {
        return getConfigurablePathOrHomeDir("scripting.defaultTemplatePath");
    }

    public static String getDefaultOutputPath() {
        return getConfigurablePathOrHomeDir("scripting.defaultOutputPath");
    }

    private static String getConfigurablePathOrHomeDir(String configurationKey) {
        String path = UserConfiguration.getInstance().getString(configurationKey);
        if (path == null || path.trim().length() == 0) {
            path = System.getProperty("user.home");
        }
        return path;
    }

    public static List<String> getRecentServersByProtocol(String protocol) {
        List<String> v = UserConfiguration.getInstance().getListOfStrings(ConfigurationKeys.IO_RECENT_SERVERS);
        List<String> displayValues = new ArrayList();
        URI uri;
        String name;
        for (String s : v) {
            try {
                uri = Utils.getURI(s);
                if (uri.getScheme() != null && uri.getScheme().equalsIgnoreCase(protocol)) {
                    name = uri.getHost() + (uri.getPort() >= 0 ? ":" + uri.getPort() : "");
                    if (!displayValues.contains(name)) {
                        displayValues.add(name);
                    }
                }
            } catch (URISyntaxException ex) {
            }
        }
        return displayValues;
    }

    /**
     * Export a test script as is displayed in the editor (with styles) to HTML.
     *
     * @param writer an output writer to write to.
     * @param doc a styled document containing a test script with styles.
     * @param addLineNumbers true turns on line numbering.
     * @throws java.io.IOException
     * @throws javax.swing.text.BadLocationException
     */
    public static void exportScriptToHtml(Writer writer, StyledDocument doc, final boolean addLineNumbers) throws IOException, BadLocationException {
        // First write to a string
        StringWriter sw = new StringWriter();
        MinimalHTMLWriter wr = new MinimalHTMLWriter(sw, doc) {

            private String indent;
            private Element parentElement;
            private int fontMask;
            private static final int BOLD = 0x01;
            private static final int ITALIC = 0x02;
            private static final int UNDERLINE = 0x04;

            protected void writeStyles() throws IOException {
                // No styles supported
            }
            // Overriden to insert line numbers

            @Override
            protected void writeStartParagraph(Element elem) throws IOException {
                if (addLineNumbers) {
                    try {
                        int i = DocumentUtils.getLineForOffset((StyledDocument) elem.getDocument(), elem.getStartOffset()) + 1;
                        write("<font size=\"-1\"><a name=\"line" + i + "\">" + i + ".</font>&nbsp;&nbsp;&nbsp;");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                super.writeStartParagraph(elem);
            }

            @Override
            protected void writeContent(Element elem, boolean needsIndenting)
                    throws IOException, BadLocationException {
                AttributeSet attr = elem.getAttributes();
                writeNonHTMLAttributes(attr);
                if (needsIndenting) {
                    indent();
                }
                writeHTMLTags(attr);
                text(elem);
                writeHTMLTags(new SimpleAttributeSet());
            }

            /**
             * Generates
             * bold &lt;b&gt;, italic &lt;i&gt;, and &lt;u&gt; tags for the
             * text based on its attribute settings.
             *
             * @exception IOException on any I/O error
             */
            protected void writeHTMLTags(AttributeSet attr) throws IOException {

                int oldMask = fontMask;
                setFontMask(attr);

                int endMask = 0;
                int startMask = 0;
                if ((oldMask & BOLD) != 0) {
                    if ((fontMask & BOLD) == 0) {
                        endMask |= BOLD;
                    }
                } else if ((fontMask & BOLD) != 0) {
                    startMask |= BOLD;
                }

                if ((oldMask & ITALIC) != 0) {
                    if ((fontMask & ITALIC) == 0) {
                        endMask |= ITALIC;
                    }
                } else if ((fontMask & ITALIC) != 0) {
                    startMask |= ITALIC;
                }

                if ((oldMask & UNDERLINE) != 0) {
                    if ((fontMask & UNDERLINE) == 0) {
                        endMask |= UNDERLINE;
                    }
                } else if ((fontMask & UNDERLINE) != 0) {
                    startMask |= UNDERLINE;
                }
                writeEndMask(endMask);
                writeStartMask(startMask);
            }

            /**
             * Writes out start tags &lt;u&gt;, &lt;i&gt;, and &lt;b&gt; based on
             * the mask settings.
             *
             * @exception IOException on any I/O error
             */
            private void writeStartMask(int mask) throws IOException {
                if (mask != 0) {
                    if ((mask & UNDERLINE) != 0) {
                        write("<u>");
                    }
                    if ((mask & ITALIC) != 0) {
                        write("<i>");
                    }
                    if ((mask & BOLD) != 0) {
                        write("<b>");
                    }
                }
            }

            /**
             * Writes out end tags for &lt;u&gt;, &lt;i&gt;, and &lt;b&gt; based on
             * the mask settings.
             *
             * @exception IOException on any I/O error
             */
            private void writeEndMask(int mask) throws IOException {
                if (mask != 0) {
                    if ((mask & BOLD) != 0) {
                        write("</b>");
                    }
                    if ((mask & ITALIC) != 0) {
                        write("</i>");
                    }
                    if ((mask & UNDERLINE) != 0) {
                        write("</u>");
                    }
                }
            }

            /**
             * Tweaks the appropriate bits of fontMask
             * to reflect whether the text is to be displayed in
             * bold, italic, and/or with an underline.
             *
             */
            private void setFontMask(AttributeSet attr) {
                fontMask = 0;
                if (StyleConstants.isBold(attr)) {
                    fontMask |= BOLD;
                }

                if (StyleConstants.isItalic(attr)) {
                    fontMask |= ITALIC;
                }

                if (StyleConstants.isUnderline(attr)) {
                    fontMask |= UNDERLINE;
                }
            }
            // Overriden to preserve code indent

            @Override
            protected void text(Element elem) throws IOException, BadLocationException {
                String contentStr = getText(elem);
                if ((contentStr.length() > 0)
                        && (contentStr.charAt(contentStr.length() - 1) == NEWLINE)) {
                    contentStr = contentStr.substring(0, contentStr.length() - 1) + "<br>";
                }

                if (contentStr.length() > 0) {
                    // Convert all leading spaces to &nbsp; entities.
                    // Do this however just for the first non-empty leaf element
                    // of the branch element representing the whole code line.
                    if (!elem.getParentElement().equals(parentElement)) {
                        parentElement = elem.getParentElement();
                        String text = DocumentUtils.getElementText(elem);
                        indent = "";

                        // Define how many leading spaces the whole line has
                        // and generate an indent string of &nbsp; entities
                        for (int i = 0; i < text.length() && Character.isWhitespace(text.charAt(i)); i++) {
                            indent += "&nbsp;";
                        }
                    }

                    // Insert the indent for the first non-empty element.
                    // Set it to null then to insert it just once for each line
                    String text = contentStr.trim();
                    if (indent != null && text.length() > 0) {
                        contentStr = indent + contentStr.substring(contentStr.indexOf(text));
                        indent = null;
                    }
                    write(contentStr);
                }
            }
        };
        wr.write();

        // The minimal HTML writer is quite messy and most
        // of its internals are private. That's why we both
        // customize it as well as filter out the output.

        // The most annoying thing is that the writer encloses
        // each document element (== script line) with the <p> tag.
        // We replace it with <br>
        String text = sw.toString().replace("<p>", "");
        text = text.replaceAll("<p class=[a-zA-Z]*>", "");
        text = text.replaceAll("</p>", "");

        // Insert the <code> element
        text = text.replace("<body>", "<body>\n<code>");
        text = text.replace("</body>", "</code>\n</body>");

        // Write the output to the file
        writer.write(text);
        writer.flush();
        writer.close();
    }

    public static void exportScriptToBBCode(Writer writer, StyledDocument doc) throws IOException, BadLocationException {
        ElementIterator iterator = new ElementIterator(doc);
        Element element;
        AttributeSet attributes;
        Object a;
        String text, code = "";
        TokenParser parser = new TokenParserImpl();
        String defaultColor = parser.colorToString(Color.BLACK);
        Color fg;

        while ((element = iterator.next()) != null) {
            if (element.getElementCount() == 0) {  // Process just leaf elements
                attributes = element.getAttributes();
                text = doc.getText(element.getStartOffset(), element.getEndOffset() - element.getStartOffset());

                if (text.trim().isEmpty()) {
                    code += text;
                    continue;
                }
                fg = (Color) attributes.getAttribute(StyleConstants.Foreground);
                if (fg == null) {
                    text = "[color=#" + defaultColor + "]" + text + "[/color]";
                } else {
                    text = "[color=#" + parser.colorToString(fg) + "]" + text + "[/color]";
                }

                a = attributes.getAttribute(StyleConstants.Bold);
                if (a != null && ((Boolean) a).booleanValue()) {
                    text = "[b]" + text + "[/b]";
                }

                a = attributes.getAttribute(StyleConstants.Italic);
                if (a != null && ((Boolean) a).booleanValue()) {
                    text = "[i]" + text + "[/i]";
                }
                code += text;
            }
        }
        System.out.println(code);
        writer.write(code);
    }

    /**
     * Get relative path to a file resolved to another file or directory.
     * @param file file to get the relative path for.
     * @param relativeTo file or directory to resolve against.
     * @return relative file path as String (for example, "../../test/myfile.txt").
     * The method may return null where relative referencing is not possible, for
     * example where the files are on different drives (disks) on Windows.
     * @throws java.io.IOException when the File.getCanonicalPath() method throws an I/O exception.
     */
    public static String getRelativePath(File file, File relativeTo) throws IOException {

        // Bug fix for 2.0.6/2.1.1: The JFileChooser on Windows returns some
        // kind of Windows-specific internal class instead of java.io.File.
        // It behaves a bit differently and going up the path it gets to C:\,
        // then to Computer and then to Desktop. That's why we recreate the files below.
        file = new File(Utils.getFullPath(file));
        relativeTo = new File(Utils.getFullPath(relativeTo));

        file = file.getCanonicalFile();
        relativeTo = relativeTo.getCanonicalFile();
        if (relativeTo.isDirectory()) {
            relativeTo = new File(relativeTo, "dummy");
        }

        File f1 = file;
        File f2 = relativeTo;
        List<File> l1 = new ArrayList();
        while ((f1 = f1.getParentFile()) != null) {
            l1.add(0, f1);
        }

        List<File> l2 = new ArrayList();
        while ((f2 = f2.getParentFile()) != null) {
            l2.add(0, f2);
        }

        // Find out how many levels from the root down are equal
        int cnt = 0;
        for (; cnt < l1.size() && cnt < l2.size(); cnt++) {
            f1 = l1.get(cnt);
            f2 = l2.get(cnt);
            if (!f1.equals(f2)) {
                break;
            }
        }

        if (cnt > 0) {  // At least the root dir must be same
            String s = "";
            for (int i = cnt; i < l2.size(); i++) {
                s += ".." + File.separator;
            }
            for (int i = cnt; i < l1.size(); i++) {
                s += l1.get(i).getName() + File.separator;
            }
            s += file.getName();
            return s;
        }

        return null;
    }

    /**
     * Letter case tolerant contains() method for lists.
     * @param l a list.
     * @param o a value to search for. If the value is a String, the list is
     * searched using String.equalsIgnoreCase().
     * @return true if the list contains the value, false if not.
     */
    public static boolean containsIgnoreCase(List l, Object o) {
        if (o instanceof String) {
            String s = (String) o;
            for (Object oo : l) {
                if (oo instanceof String && s.equalsIgnoreCase((String) oo)) {
                    return true;
                }
            }
        } else {
            return l.contains(o);
        }
        return false;
    }

    /**
     * Letter case tolerant indexOf() method for lists.
     * @param l a list.
     * @param o a value to search for. If the value is a String, the list is
     * searched using String.equalsIgnoreCase().
     * @return value index or -1 if not found.
     */
    public static int indexOfIgnoreCase(List l, Object o) {
        if (o instanceof String) {
            String s = (String) o;
            Object oo;
            int len = l.size();
            for (int i = 0; i < len; i++) {
                oo = l.get(i);
                if (oo instanceof String && s.equalsIgnoreCase((String) oo)) {
                    return i;
                }
            }
        } else {
            return l.indexOf(o);
        }
        return -1;
    }

    public static String convertStringToMultiline(String text) {
        if (text != null && !text.equals("")) {
            String ss[] = text.split("\\\\n");
            text = "";
            for (String s : ss) {
                text += s;
                if (s.endsWith("\\")) {
                    text += "n";
                } else {
                    text += '\n';
                }
            }
        }
        return text;
    }

    public static String convertMultilineToString(String text) {
        if (text != null && !text.equals("")) {
            BufferedReader sr = new BufferedReader(new StringReader(text));
            String line;
            text = "";
            try {
                while ((line = sr.readLine()) != null) {
                    line = line.replace("\\n", "\\\\n");
                    text += line + "\\n";
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return text;
    }

    public static Map putAll(Map target, Map source) {
        boolean ok = false;
        while (!ok && source != null) {
            try {
                target.putAll(source);
                ok = true;
            } catch (ConcurrentModificationException e) {
            }
        }
        return target;
    }
    public static Map<File, Boolean> canCreateFileMap = new HashMap();

    /**
     * <p>Test whether the argument file can be created for writing. If the file
     * already exists, the method returns result of the {@link File#canWrite()} method.
     * If the file doesn't exist, the method makes an attempt to create it together
     * with any necessary directories to test whether it is possible or not. Any
     * newly created objects are deleted in the end of the test.</p>
     *
     * <p>Note that for performance reasons the files tested by this method are
     * cached together with the result. If the method is called repeatedly with
     * the same file, the file is not really tested in the file system and the
     * cached result is returned instead. Should you need to clear the cache, see
     * the {@link #canCreateFileMap} map. To disable caching simply set the map
     * to null; to reenable it assign it a new hash map.
     *
     * @param f a file.
     * @return true if it already exists and is writable or if it can be created
     * for writing.
     * @throws IOException if any of the directory or file create operations throw
     * an IO exception, it is rethrown.
     */
    public static boolean canCreateNewFile(File f) throws IOException {
        Boolean b = canCreateFileMap != null ? canCreateFileMap.get(f) : null;
        if (b != null) {
            return b;
        }
        boolean canCreate = false;
        List<File> l = null;
        IOException ex = null;
        try {
            if (f.exists()) {
                return f.canWrite();
            } else {
                File parent = f.getParentFile();
                if (parent != null && !parent.exists()) {
                    File temp = parent;

                    l = new ArrayList();
                    l.add(temp);
                    while ((temp = temp.getParentFile()) != null && !temp.exists()) {
                        l.add(temp);
                    }
                    parent.mkdirs();
                }
                if (f.createNewFile()) {
                    canCreate = true;
                    f.delete();
                }
            }
        } catch (IOException e) {
            canCreate = false;
            ex = e;
        } finally {
            if (l != null) {
                try {
                    for (File dir : l) {
                        dir.delete();
                    }
                } catch (Exception e) {
                    ex.printStackTrace();
                }
            }
        }

        if (canCreateFileMap != null) {
            canCreateFileMap.put(f, canCreate);
        }
        if (ex != null) {
            throw ex;
        }
        return canCreate;
    }
//    public static void main(String[] args) {
//        String text = "line1\nline2 with \\n\nline3";
//        System.out.println("1.\n"+text);
//        text = convertMultilineToString(text);
//        System.out.println("2.\n"+text);
//        text = convertStringToMultiline(text);
//        System.out.println("3.\n"+text);
//    }

    public static BufferedImage addAlpha(BufferedImage img) {
        if (img.getColorModel().hasAlpha()) {  // Has alpha already
            return img;
        }
        BufferedImage newImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImg.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return newImg;
    }

    /**
     * Search a file path recursively for a file whose name matches a particular string or a
     * {@link Pattern} compliant regular expression.
     *
     * @param dir root directory to start the search from.
     * @param name the name or regular expression to match.
     * @param outList output list for the located files. A new one will be
     * created and returned when it is null.
     * @param useRegularExpression true will handle the <code>name</code> as a
     * regular expression. If the argument is false, a regular string comparison
     * will be done.
     * @param ignoreCase only used when <code>useRegularExpression=false</code>.
     * If this flag is true, the file name will be compared to the string specified
     * by <code>name</code> using {@link String#equalsIgnoreCase(java.lang.String)}
     * rather than {@link String#equals(java.lang.Object)}.
     * @param stop if a non-null boolean array with the length>=1 is
     * specified, the value of <code>stop[0]</code> will be periodically checked
     * and when it becomes "true", the file search will stop immediately. This
     * functionality may be used for example for a "Stop search" button in the GUI.
     * @param currentDir if a non-null String array with the length>=1 is
     * specified, the file search will populate <code>currentDir[0]</code> with
     * the absolute directory path whenever a new directory is entered. This
     * functionality may be used for example for a GUI component which shows the
     * currently processed folder..
     * @return list of files matching the entry criteria. If no file is found, the
     * method returns an empty list.
     */
    public static List<File> findFile(File dir, String name, List<File> outList,
            boolean useRegularExpression, boolean ignoreCase, boolean stop[], String currentDir[]) {
        return findFile(dir, name, outList, useRegularExpression, ignoreCase, stop, currentDir, null);
    }

    private static List<File> findFile(File dir, String name, List<File> outList,
            boolean useRegularExpression, boolean ignoreCase, boolean stop[], String currentDir[], List<File> scanned) {
        if (outList == null) {
            outList = new ArrayList();
        }
        if (scanned == null) {
            scanned = new ArrayList();
        }
        if (stop != null && stop[0]) {
            return outList;
        }
        if (dir.isFile()) {
            String n = dir.getName();
            if (useRegularExpression) {
                if (n.matches(name)) {
                    outList.add(dir);
                }
            } else {
                if (ignoreCase) {
                    if (n.equalsIgnoreCase(name)) {
                        outList.add(dir);
                    }
                } else {
                    if (n.equals(name)) {
                        outList.add(dir);
                    }
                }
            }
        } else if (dir.isDirectory() && (stop == null || !stop[0])) {
            try {
                File cdir = dir.getCanonicalFile();
                if (scanned.contains(cdir)) {
                    return outList;
                } else {
                    scanned.add(cdir);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                return outList;
            }
            if (currentDir != null && currentDir.length > 0) {
                currentDir[0] = dir.getAbsolutePath();
            }
            File files[] = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (stop != null && stop[0]) {
                        return outList;
                    }
                    outList = findFile(f, name, outList, useRegularExpression, ignoreCase, stop, currentDir, scanned);
                    if (stop != null && stop[0]) {
                        return outList;
                    }
                }
            }
        }
        return outList;
    }

    public static boolean isLink(final File file) {
        try {
            if (file == null || !file.exists()) {
                return false;
            }
            File canonicalDir = file.getParentFile().getCanonicalFile();
            File fileInCanonicalDir = new File(canonicalDir, file.getName());
            return fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.indexOf("mac") >= 0;
    }

    public static List<Image> getRobotIcons() {
        List<Image> l = new ArrayList(4);
        Class loader = ImageLoader.class;
        try {
            l.add(new ImageIcon(loader.getResource("robot64.png")).getImage());
        } catch (Exception e) {
        }
        try {
            l.add(new ImageIcon(loader.getResource("robot56.png")).getImage());
        } catch (Exception e) {
        }
        try {
            l.add(new ImageIcon(loader.getResource("robot32.png")).getImage());
        } catch (Exception e) {
        }
        try {
            l.add(new ImageIcon(loader.getResource("app_icon.png")).getImage());
        } catch (Exception e) {
        }
        return l;
    }

    public static File getHelpSetFile() {
        // First look if there's a help set for the locale the app was started on
        Locale loc = ApplicationSupport.getLocale();
        File f = null;
        String locDir;

        // First check the lang_country_variant folder existence
        if (loc.getVariant() != null & loc.getVariant().length() > 0) {
            locDir = loc.getLanguage() + "_" + loc.getCountry() + "_" + loc.getVariant();
            f = checkHelpSetFileExists(locDir);
        }

        // Second check the lang_country folder existence
        if (f == null && loc.getCountry() != null & loc.getCountry().length() > 0) {
            locDir = loc.getLanguage() + "_" + loc.getCountry();
            f = checkHelpSetFileExists(locDir);
        }

        // Third check the lang folder existence
        if (f == null) {
            locDir = loc.getLanguage();
            f = checkHelpSetFileExists(locDir);
        }

        // Fall back to English which must always be there
        if (f == null) {
            locDir = "en";
            f = checkHelpSetFileExists(locDir);
        }

        if (f != null) {
            return f;
        }
        return null;
    }

    private static File checkHelpSetFileExists(String localeString) {
        String installPath = Utils.getInstallPath();
        File f = new File(installPath + File.separator
                + ApplicationSupport.APPLICATION_HELP_SET_DIR + File.separator
                + localeString + File.separator
                + ApplicationSupport.APPLICATION_HELP_SET_FILE);
        if (f.exists() && f.isFile() && f.canRead()) {
            return f;
        }
        return null;
    }

    /**
     * Find out whether a class implements a particular interface. Unlike the
     * <code>Class.getImplementedInterfaces()</code> the method also checks all
     * superclasses. It provides the same functionality as the "instanceof"
     * operator save that it avoids any class instantiation.
     *
     * @param cl a class to be checked for the implemented interface.
     * @param interf an interface class.
     * @return true if the class or any of its superclasses implements the
     * specified interface or false otherwise.
     */
    public static boolean implementsInterface(Class cl, String interf) {
        boolean flag = false;
        for (Class intf : cl.getInterfaces()) {
            if (intf.getCanonicalName().equals(interf)) {
                return true;
            }
            Class[] si = intf.getInterfaces();
            if (si != null && si.length > 0) {
                flag |= implementsInterface(intf, interf);
            }
        }
        if (!flag && cl.getSuperclass() != null) {
            flag |= implementsInterface(cl.getSuperclass(), interf);
        }
        return flag;
    }
}
