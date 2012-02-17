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
package com.tplan.robot.gui.dialogs;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.gui.components.CustomHyperlinkListener;
import com.tplan.robot.gui.components.ImagePanel;
import com.tplan.robot.gui.components.MapTableModel;
import com.tplan.robot.util.Utils;

import java.io.IOException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.text.html.HTMLDocument;

/**
 * A simple window providing information about the product, license and environment.
 * Accessible through the Help->About menu item.
 * @product.signature
 */
public class AboutDialog extends JDialog implements ActionListener {

    /**
     * Constructor.
     * @param frame a JFrame instance which will be acting as owner of the window.
     */
    public AboutDialog(JFrame frame) {
        super(frame, MessageFormat.format(ApplicationSupport.getString("help.About.title"), ApplicationSupport.APPLICATION_NAME), true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        init();
        pack();
        MainFrame.centerDlg(frame, this);
    }

    /**
     * Initialize the window components.
     */
    private void init() {

        JTabbedPane tabbedPane = new JTabbedPane();
        if (MainFrame.OPEN_SOURCE) {
            try {
                BufferedImage bi = ImageIO.read(ApplicationSupport.getImageAsStream("splash.png"));
                if (bi != null && bi.getWidth() > 0) {
                    ImagePanel pnl = new ImagePanel();
                    pnl.setImage(bi);
                    JPanel p = new JPanel();
                    p.setOpaque(true);
                    p.setBackground(Color.GRAY);
                    p.add(pnl, BorderLayout.NORTH);
                    tabbedPane.add(ApplicationSupport.getString("help.Aboult.splash"), p);
                }
            } catch (Exception ex) {
            }
        }

        String text = ApplicationSupport.getString("help.About.text");
        Object params[] = {
            Utils.getProductNameAndVersion(),
            ApplicationSupport.getString("com.tplan.robot.copyrightHTML"),
            ApplicationSupport.APPLICATION_HOME_PAGE
        };
        text = MessageFormat.format(text, params);

        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText(text);
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setMargin(new Insets(15, 15, 15, 15));

        // Set the JEditorPane default font to the JLabel one
        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument) textPane.getDocument()).getStyleSheet().addRule(bodyRule);

        getContentPane().add(textPane, BorderLayout.NORTH);
        textPane.addHyperlinkListener(new CustomHyperlinkListener(((MainFrame) getOwner())));

        JTextPane jtp = new JTextPane();
        jtp.setEditable(false);
        try {
            // First try to open the license which should be bundled with the code
            // in the same folder where the ApplicationSupport class resides.
            jtp.setPage(ApplicationSupport.class.getResource(ApplicationSupport.APPLICATION_LICENSE_FILE));
        } catch (IOException ex) {
            try {
                // If the bundled license is not found, try to load the one
                // which is in the installation directory.
                jtp.setPage(new File(Utils.getInstallPath() + File.separator + ApplicationSupport.APPLICATION_LICENSE_FILE).toURI().toURL());
            } catch (Exception ex1) {

                // Could not find the license - display an error text.
                jtp.setContentType("text/html");
                jtp.setText(MessageFormat.format(ApplicationSupport.getString("help.About.licenseError"),
                        ApplicationSupport.APPLICATION_NAME, ApplicationSupport.APPLICATION_HOME_PAGE));
            }
        }

        Map map = new HashMap(System.getProperties());

        JTable table = new JTable();
        String[] cols = new String[]{
            ApplicationSupport.getString("help.About.tableHeader1"),
            ApplicationSupport.getString("help.About.tableHeader2"),};
        table.setModel(new MapTableModel(map, cols, true));

        final JButton btnClose = new JButton(ApplicationSupport.getString("help.About.buttonClose"));
        btnClose.addActionListener(this);

        // Create the Java string
        String javaName = System.getProperty("java.runtime.name");
        if (javaName == null || javaName.length() == 0) {
            javaName = "Unknown Java";
        }
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null || javaVersion.length() == 0) {
            javaVersion = "unknown";
        }
        String javaVendor = System.getProperty("java.vendor");
        if (javaVendor == null || javaVendor.length() == 0) {
            javaVendor = "unknown vendor";
        }
        final String java = MessageFormat.format("{0} version {1} from {2}", javaName, javaVersion, javaVendor);

        // Create the OS string
        String osName = System.getProperty("os.name");
        if (osName == null || osName.length() == 0) {
            osName = "Unknown system";
        }
        String osVersion = System.getProperty("os.version");
        if (osVersion == null || osVersion.length() == 0) {
            osVersion = "unknown";
        }
        String osArch = System.getProperty("os.arch");
        if (osArch == null || osArch.length() == 0) {
            osArch = "unknown architecture";
        }
        final String os = MessageFormat.format("{0} version {1} ({2})", osName, osVersion, osArch);

        // Create list of supported image formats
        String fts[] = ImageIO.getWriterFileSuffixes();
        Map<String, String> m = new HashMap();
        for (String s : fts) {
            m.put(s.toUpperCase(), "");
        }
        String formats = "";
        int i = 0;
        Object fs[] = m.keySet().toArray();
        Arrays.sort(fs);
        for (Object s : fs) {
            formats += s;
            if (i++ < m.size() - 1) {
                formats += ", ";
            }
        }
        JPanel pnlSysInfo = new JPanel(new GridBagLayout());
        pnlSysInfo.add(new JLabel(ApplicationSupport.getString("help.About.txtSystem")), new GridBagConstraints(
                0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        pnlSysInfo.add(new JLabel(ApplicationSupport.getString("help.About.txtJava")), new GridBagConstraints(
                0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        pnlSysInfo.add(new JLabel(ApplicationSupport.getString("help.About.txtImageFormats")), new GridBagConstraints(
                0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        pnlSysInfo.add(new JScrollPane(table), new GridBagConstraints(
                0, 3, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));

        JTextField txtOs = new JTextField(os);
        txtOs.setEditable(false);
        pnlSysInfo.add(txtOs, new GridBagConstraints(
                1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        JTextField txtJava = new JTextField();
        txtJava.setEditable(false);
        txtJava.setText(java);
        pnlSysInfo.add(txtJava, new GridBagConstraints(
                1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        JTextField txtFormats = new JTextField(formats);
        txtFormats.setEditable(false);
        pnlSysInfo.add(txtFormats, new GridBagConstraints(
                1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));

        tabbedPane.add(ApplicationSupport.getString("help.About.tabLicense"), new JScrollPane(jtp));
        tabbedPane.add(ApplicationSupport.getString("help.About.tabSystem"), pnlSysInfo);
        tabbedPane.setPreferredSize(new Dimension(650, 380));
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        JPanel pnlSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnlSouth.add(btnClose);
        getContentPane().add(pnlSouth, BorderLayout.SOUTH);

        Utils.registerDialogForEscape(this, btnClose);
    }

    /**
     * Implementation of the <code>ActionListener</code>. It implements
     * functionality of the "Close" button.
     * @param e an ActionEvent.
     */
    public void actionPerformed(ActionEvent e) {
        dispose();
    }
}
