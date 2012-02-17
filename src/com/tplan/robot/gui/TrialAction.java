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
 */package com.tplan.robot.gui;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.util.Utils;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

/**
 * Help action available for the open source Robot product, such as getting of
 * an Enterprise trial etc.
 * 
 * @product.signature
 */
class TrialAction extends AbstractAction implements Runnable, WindowListener {

    TrialAction() {
        putValue(AbstractAction.NAME, "Get T-Plan Robot Enterprise");
        putValue(AbstractAction.ACTION_COMMAND_KEY, "trial");
        putValue(AbstractAction.SMALL_ICON, ApplicationSupport.getImageIcon("app_icon.png"));
    }

    public static void install(MainFrame frame) {
        // Locate the Tools menu
        JMenu m, helpMenu = null;
        JMenuBar mb = frame.getJMenuBar();
        String name = ApplicationSupport.getString("menu.HelpText");
        if (name.contains(ActionManager.HOT_KEY_SEPARATOR)) {
            name = name.substring(0, name.indexOf(ActionManager.HOT_KEY_SEPARATOR));
        }

        for (int i = 0; i < mb.getMenuCount(); i++) {
            m = mb.getMenu(i);
            if (m.getText().equals(name)) {
                helpMenu = m;
                break;
            }
        }

        if (helpMenu == null) {
            helpMenu = mb.getMenu(0);
        }

        helpMenu.add(new OnlineDocumentAction("T-Plan Robot Tutorial (web)", "http://www.t-plan.com/robot/docs/tutorials/v2.0/index.html"));
        helpMenu.addSeparator();

        TrialAction ta = new TrialAction();
        helpMenu.add(ta);
        Integer tmsg = UserConfiguration.getInstance().getInteger("os.showWelcomeMsg");
//        System.out.println("tmsg="+UserConfiguration.getInstance().getInteger("os.showWelcomeMsg"));
        if (tmsg == null || tmsg == -1) {
            frame.addWindowListener(ta);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String msg = ApplicationSupport.getString("os.trialText");
        Object options[] = new Object[]{ApplicationSupport.getString("os.trialContinue"), ApplicationSupport.getString("btnCancel")};
        int option = Utils.showConfigurableMessageDialog(
                MainFrame.getInstance(),
                ApplicationSupport.getString("os.trialTitle"),
                msg,
                null,
                null,
                options,
                0, false);
        if (option == 0) {
            Utils.execOpenURL("http://www.t-plan.com/contacts_robottrial.php");
        }
    }

    // Display the Enterprise message
    @Override
    public void run() {
        String msg = ApplicationSupport.getString("os.welcomeText");
        msg = MessageFormat.format(msg, ApplicationSupport.APPLICATION_VERSION);
        Object options[] = new Object[]{ApplicationSupport.getString("btnOk")};
        Utils.showConfigurableMessageDialog(
                MainFrame.getInstance(),
                ApplicationSupport.getString("os.welcomeTitle"),
                msg,
                ApplicationSupport.getString("os.welcomeCheckBox"),
                "os.showWelcomeMsg",
                options,
                0, false);
    }

    @Override
    public void windowOpened(WindowEvent e) {
        MainFrame.getInstance().removeWindowListener(this);
        ScriptManager sm = MainFrame.getInstance().getScriptHandler();
        // Only display the message if there's no executing script
        if (sm.getScriptToRun() == null && sm.getExecutingTestScripts() != null && sm.getExecutingTestScripts().size() == 0) {
            Integer tmsg = UserConfiguration.getInstance().getInteger("os.showWelcomeMsg");
            if (tmsg == null) {
                try {
                    UserConfiguration.getInstance().setInteger("os.showWelcomeMsg", 0);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            SwingUtilities.invokeLater(this);
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }
}

class OnlineDocumentAction extends AbstractAction {

    private String url;

    OnlineDocumentAction(String name, String url) {
        putValue(NAME, name);
        this.url = url;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Utils.execOpenURL(url);
    }
}
