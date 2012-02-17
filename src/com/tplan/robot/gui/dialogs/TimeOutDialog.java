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
import com.tplan.robot.gui.*;
import com.tplan.robot.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;

/**
 * Timeout dialog used to count down the execuion start up and shut down timeouts.
 * @product.signature
 */
public class TimeOutDialog extends JDialog implements ActionListener, WindowListener {

    private boolean canceled = true;
    private String file;
    private int timeout;
    JPanel contentPane = new JPanel();
    final JLabel lblMessage = new JLabel();
    JButton btnRun = new JButton();
    JButton btnCancel = new JButton();
    JButton btnConfigure;
    Timer updateTimer;
    Timer timeoutTimer;
    String countDownPattern;
    String preferencePanelName;
    String preferenceParentPanelName;

    public TimeOutDialog(JFrame frame, String file, int timeoutMs, boolean shutDownMode, String preferencePanelName, String preferenceParentPanelName) {
        super(frame, "", true);
        this.file = file;
        this.timeout = timeoutMs;
        this.preferencePanelName = preferencePanelName;
        this.preferenceParentPanelName = preferenceParentPanelName;
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        initLabels(shutDownMode);
        init();
        pack();
        MainFrame.centerDlg(frame, this);
    }

    private void initLabels(boolean isShutdown) {
        if (isShutdown) {
            setTitle(ApplicationSupport.getString("com.tplan.robot.gui.TimeOutDlg.shutdownTitle"));
            btnRun.setText(ApplicationSupport.getString("com.tplan.robot.gui.TimeOutDlg.btnShutdownNow"));
            btnCancel.setText(ApplicationSupport.getString("com.tplan.robot.gui.TimeOutDlg.btnCancelShutdown"));
            countDownPattern = ApplicationSupport.getString("com.tplan.robot.gui.TimeOutDlg.shutdownLabel");
        } else {
            setTitle(ApplicationSupport.getString("com.tplan.robot.gui.TimeOutDlg.startupTitle"));
            btnRun.setText(ApplicationSupport.getString("com.tplan.robot.gui.TimeOutDlg.btnRunNow"));
            btnCancel.setText(ApplicationSupport.getString("com.tplan.robot.gui.TimeOutDlg.btnCancelStartup"));
            countDownPattern = ApplicationSupport.getString("com.tplan.robot.gui.TimeOutDlg.startupMessage");
        }
    }

    private void init() {
        addWindowListener(this);
        contentPane.setLayout(new GridBagLayout());
        this.setContentPane(contentPane);
        btnRun.addActionListener(this);
        btnCancel.addActionListener(this);
        getRootPane().setDefaultButton(btnRun);

        setTimeoutMessage(timeout);

        GridBagConstraints c;
        int span = 2;
        c = new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.EAST,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0);
        contentPane.add(btnRun, c);

        c = new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0);
        contentPane.add(btnCancel, c);

        if (preferencePanelName != null) {
            btnConfigure = new JButton(ApplicationSupport.getString("com.tplan.robot.gui.TimeOutDlg.btnCancelAndConfigure"));
            c = new GridBagConstraints(2, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
                    GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0);
            btnConfigure.addActionListener(this);
            contentPane.add(btnConfigure, c);
            span = 3;
        }

        c = new GridBagConstraints(0, 0, span, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(20, 20, 20, 20), 10, 10);
        contentPane.add(lblMessage, c);

        // The update timer will update update the count down in the dialog message
        updateTimer = new Timer(1000, this);
        updateTimer.setRepeats(true);

        // Timeout timer will be invoked just once when the time is out. It will close the dialog.
        timeoutTimer = new Timer(timeout, this);
        timeoutTimer.setRepeats(false);
        Utils.registerDialogForEscape(this, btnCancel);
    }

    private void setTimeoutMessage(int timeInMilis) {
        Object params[] = {file, new Integer(timeInMilis / 1000), ApplicationSupport.APPLICATION_NAME};
        lblMessage.setText(MessageFormat.format(countDownPattern, params));
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src.equals(btnCancel)) {  // Cancel button clicked - stop the count down and dispose
            windowClosing(null);
        } else if (src.equals(updateTimer)) {  // Update timer - update displayed seconds
            timeout -= 1000;
            setTimeoutMessage(timeout);
        } else if (src.equals(timeoutTimer)) {  // Timed out -> run the script
            canceled = false;
            windowClosing(null);
        } else if (src.equals(btnRun)) {  // User clicked the Run Now button -> run the script
            canceled = false;
            windowClosing(null);
        } else if (btnConfigure != null && src.equals(btnConfigure)) {  // User clicked the Cancel & Configure
            windowClosing(null);
            MainFrame.getInstance().showOptionsDialog(preferencePanelName, preferenceParentPanelName);
        }
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        dispose();
        timeoutTimer.stop();
        updateTimer.stop();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
        updateTimer.start();
        timeoutTimer.start();
    }
}
