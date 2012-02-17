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

import com.tplan.robot.preferences.Preference;
import com.tplan.robot.util.Utils;
import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

/**
 * A component allowing to select and display a file name. It is used for
 * preferences of type {@link Preference#TYPE_FILE Preference.TYPE_FILE}.
 * The component consists of an editable text field and a button which opens
 * a file chooser.
 *
 * @product.signature
 */
public class FileComponent extends JPanel implements ActionListener {

    private JButton btnBrowse = new JButton();
    private JButton btnCancelSearch = new JButton("Cancel");
    private JTextField txtFile = new JTextField();
    private JFileChooser chooser = new JFileChooser();
    private boolean acceptDirectoryOnly = false;
    private String fileToSearchFor,  defaultSearchPath;
    private boolean useRegularExpressions,  ignoreCase,  forceValueToFound;
    private boolean stop[] = new boolean[]{false};
    private String[] currentDir = new String[1];
    private JTextField lblCurrentDir = new JTextField();
    final static int DIR_FIELD_COLUMNS = 60;

    public FileComponent(boolean acceptDirectoryOnly) {
        this.acceptDirectoryOnly = acceptDirectoryOnly;
        init();
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        txtFile.setToolTipText(text);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        txtFile.setEnabled(enabled);
        btnBrowse.setEnabled(enabled);
    }

    public FileFilter addFileExtensionFilter(String extensions[], String description) {
        FileFilter filter = new FileExtensionFilter(extensions, description);
        chooser.addChoosableFileFilter(filter);
        return filter;
    }

    private void init() {
        setLayout(new GridBagLayout());
        setOpaque(false);
        btnBrowse.setText("...");
        btnBrowse.addActionListener(this);
        btnBrowse.setMargin(new Insets(0, 2, 0, 2));
        btnCancelSearch.addActionListener(this);
        lblCurrentDir.setColumns(DIR_FIELD_COLUMNS);
        lblCurrentDir.setOpaque(false);
        lblCurrentDir.setBorder(null);
        lblCurrentDir.setEditable(false);
        txtFile.setColumns(16);
        add(txtFile, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        add(btnBrowse, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        // File chooser set up
        chooser.setMultiSelectionEnabled(false);
        if (isAcceptDirectoryOnly()) {
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.addChoosableFileFilter(new DirectoryFileFilter());
        } else {
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnBrowse)) {
            String t = txtFile.getText();
            if (t != null && t.length() > 0) {
                File f = new File(t);
                if (!f.isAbsolute()) {
                    f = new File(Utils.getInstallPath(), f.getAbsolutePath());
                }
                if (f.exists()) {
                    if (f.isDirectory()) {
                        chooser.setCurrentDirectory(f);
                    } else {
                        chooser.setCurrentDirectory(f.getParentFile());
                    }
                }
            }
            Window win = SwingUtilities.getWindowAncestor(this);
            int option = chooser.showOpenDialog(win);
            switch (option) {
                case JFileChooser.APPROVE_OPTION:
                    final File f = chooser.getSelectedFile();
                    File ff = f;
                    if (fileToSearchFor != null) {
                        stop[0] = false;
                        FileSearchDlg dlg = new FileSearchDlg(win);
                        dlg.start(f);
                        List<File> l = dlg.getHits();
                        stop[0] = true;

                        String err = null;
                        if (l == null || l.size() < 1) {
                            err = "The path does not seem to contain the required file called '" + fileToSearchFor + "'.";
                        } else if (l.size() > 1) {
                            err = "The path contains multiple instances of the required file called '" + fileToSearchFor + "'.";
                        }
                        if (err != null) {
                            err += "\nThe selected path will be saved but the object using it may not work properly.";
                            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(win), err, "Invalid directory", JOptionPane.ERROR_MESSAGE);
                        } else if (forceValueToFound) {
                            if (acceptDirectoryOnly) {
                                ff = l.get(0).getParentFile();
                            } else {
                                ff = l.get(0);
                            }
                        }
                    }
                    txtFile.setText(ff.toString());
                    break;
                case JFileChooser.CANCEL_OPTION:
                    break;
            }

        } else if (e.getSource().equals(btnCancelSearch)) {
            stop[0] = true;
        }
    }

    public void setText(String text) {
        txtFile.setText(text);
        if (text != null) {
            txtFile.setCaretPosition(text.length());
        }
    }

    public String getText() {
        return txtFile.getText();
    }

    public JTextField getTextField() {
        return txtFile;
    }

    public void addActionListener(ActionListener a) {
        txtFile.addActionListener(a);
    }

    public void removeActionListener(ActionListener a) {
        txtFile.removeActionListener(a);
    }

    /**
     * @return the chooser
     */
    public JFileChooser getFileChooser() {
        return chooser;
    }

    /**
     * @return the acceptDirectoryOnly
     */
    public boolean isAcceptDirectoryOnly() {
        return acceptDirectoryOnly;
    }

    /**
     * This method allows to set verification options for TYPE_FILE and
     * TYPE_DIRECTORY preferences. The <code>fileToSearchFor</code> parameter may
     * specify the required file name or a regular expression (pattern) the selected
     * file must comply with. If the preference is of the TYPE_DIRECTORY type,
     * the underlying GUI component is expected to search the selected directory
     * recursively and verify whether it contains a file complying with the
     * specified name or pattern.
     * @param fileToSearchFor the fileToSearchFor to set
     * @param defaultSearchPath default directory. It may be used for resolution
     * of relative paths.
     * @param useRegularExpressions true matches the specified file name using
     * regular expressions, false sets regular string comparison.
     * @param ignoreCase whether the regular comparison should be done in a case
     * sensitive way or not. If the value is true, the file names will be
     * compared with the <code>fileToSearchFor</code> parameter using {@link String#equalsIgnoreCase(java.lang.String)}
     * instead of standard {@link String#equals(java.lang.Object)}. If regular
     * expressions are on (the <code>useRegularExpressions</code> parameter is true),
     * this parameter is ignored.
     * @param forceValueToFound Indicates whether the preference value should
     * be forced to the located file (TYPE_FILE) or its parent folder (TYPE_DIRECTORY).
     * The value of true forces the file search mechanism to set the value to the file found,
     * false leaves the value as it was selected by the user.
     */
    public void setFileToSearchFor(String fileToSearchFor, String defaultSearchPath, boolean useRegularExpressions,
            boolean ignoreCase, boolean forceValueToFound) {
        this.fileToSearchFor = fileToSearchFor;
        this.useRegularExpressions = useRegularExpressions;
        this.ignoreCase = ignoreCase;
        this.defaultSearchPath = defaultSearchPath;
        this.forceValueToFound = forceValueToFound;
    }

    private class FileSearchDlg extends JDialog implements Runnable, WindowListener {

        File file;
        List<File> found = new ArrayList();

        FileSearchDlg(Window parent) {
            super(parent, ModalityType.APPLICATION_MODAL);
            setAlwaysOnTop(true);
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            JPanel pnl = new JPanel(new GridBagLayout());
            pnl.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Search progress"));
            pnl.add(lblCurrentDir, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));
            pnl.add(btnCancelSearch, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER,
                    GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 0, 0));
            setContentPane(pnl);
            pack();
            Dimension d = getPreferredSize();
            d.width = Math.min(d.width, 400);
            setPreferredSize(d);
            setLocationRelativeTo(parent);
        }

        public void start(File f) {
            this.file = f;
            setTitle("Searching "+f.getAbsolutePath());
            new Thread(this).start();
            new UpdateThread().start();
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
            }
            if (!stop[0]) {
                setVisible(true);
            }
            stop[0] = true;
        }

        public void stop() {
            stop[0] = true;
            setVisible(false);
            dispose();
        }

        public void run() {
            Utils.findFile(file, fileToSearchFor, found, useRegularExpressions, ignoreCase, stop, currentDir);
            stop();
        }

        public List<File> getHits() {
            return found;
        }

        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
            stop();
        }

        public void windowClosed(WindowEvent e) {
        }

        public void windowIconified(WindowEvent e) {
        }

        public void windowDeiconified(WindowEvent e) {
        }

        public void windowActivated(WindowEvent e) {
        }

        public void windowDeactivated(WindowEvent e) {
        }
    }

    private class UpdateThread extends Thread {

        public void run() {
            String s;
            int space = DIR_FIELD_COLUMNS / 2 - 2;
            while (!stop[0]) {
                s = currentDir[0];
                if (s.length() > DIR_FIELD_COLUMNS) {
                    s = s.substring(0, space) + "...." + s.substring(space+4);
                }
                lblCurrentDir.setText(s);
                paintImmediately(lblCurrentDir.getBounds());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }
        }
    }
}
