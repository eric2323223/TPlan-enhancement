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
import com.tplan.robot.gui.preferences.DefaultPreferencePanel;
import com.tplan.robot.preferences.Configurable;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.scripting.commands.ExtendedParamsObject;
import com.tplan.robot.util.Utils;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Lightweight preferences dialog allowing to display preferences of a {@link Configurable}
 * object in the a single panel.
 *
 * @product.signature
 */
public class PreferencePanelDialog extends JDialog implements ActionListener {

    boolean canceled = true;
    protected JButton btnOK = new JButton(ApplicationSupport.getString("btnOk"));
    protected JButton btnCancel = new JButton(ApplicationSupport.getString("btnCancel"));
    protected DefaultPreferencePanel component;
    protected JScrollPane scroll;
    protected UserConfiguration parameterContainer;

    /**
     * Constructor.
     * @param configurable a Configurable instance to display the preferences for.
     * @param parent dialog parent (a JDialog instance).
     * @param title window title.
     * @param modal modality flag.
     */
    public PreferencePanelDialog(Object configurable, Window parent, String title, boolean modal) {
        super(parent, title, modal ? Dialog.ModalityType.APPLICATION_MODAL : Dialog.ModalityType.MODELESS);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Utils.registerDialogForEscape(this, btnCancel);
        JPanel pnlSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnOK.addActionListener(this);
        pnlSouth.add(btnOK);
        btnCancel.addActionListener(this);
        pnlSouth.add(btnCancel);
        getContentPane().add(pnlSouth, BorderLayout.SOUTH);

        if (configurable != null) {
            setConfigurableObject(configurable);
        }
    }

    /**
     * Set the instance of {@link Configurable}. This method is called from the constructor
     * if a valid Configurable object was passed as argument or it can be called
     * at any time later on.
     *
     * @param configurable a configurable object.
     */
    public void setConfigurable(Configurable configurable) {
        setConfigurableObject(configurable);
    }

    /**
     * Set the instance of {@link ExtendedParamsObject}. This method is called from the constructor
     * if a valid Configurable object was passed as argument or it can be called
     * at any time later on.
     *
     * @param configurable a configurable object.
     */
    public void setExtendedParamsObject(ExtendedParamsObject configurable) {
        setConfigurableObject(configurable);
    }

    private void setConfigurableObject(Object configurable) {
        java.util.List<Preference> l = null;
        if (configurable instanceof Configurable) {
            l = ((Configurable) configurable).getPreferences();
        } else if (configurable instanceof ExtendedParamsObject) {
            l = ((ExtendedParamsObject) configurable).getVisualParameters();
        } else {
            throw new IllegalArgumentException("The configurable object must implement either the Configurable or ExtendedParamsObject interface.");
        }
        if (scroll != null) {
            getContentPane().remove(scroll);
        }
        component = new DefaultPreferencePanel();
        for (Preference p : l) {
            component.addPreference(p);
        }
        component.init();
        UserConfiguration cfg = parameterContainer == null
                ? UserConfiguration.getInstance() : parameterContainer;
        component.loadPreferences(cfg);

        scroll = new JScrollPane(component);
        getContentPane().add(scroll, BorderLayout.CENTER);
    }

    /**
     * Handler of action events from the dialog buttons.
     * @param e an action event fired by one of the buttons.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnCancel)) {
            dispose();
        } else if (e.getSource().equals(btnOK)) {
            // Save and continue
            canceled = false;
            if (parameterContainer == null) {
                component.savePreferences(UserConfiguration.getInstance());
                UserConfiguration.saveConfiguration();
            } else {
                component.savePreferences(parameterContainer);
            }
            dispose();
        }
    }

    /**
     * Find out whether the dialog was canceled by user.
     * @return true if the dialog was canceled, false if not.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Get the user configuration instance.
     * @return user configuration instance.
     */
    public UserConfiguration getParameterContainer() {
        return parameterContainer;
    }

    /**
     * Set the user configuration instance.
     * @param configurationContainer user configuration instance.
     */
    public void setParameterContainer(UserConfiguration configurationContainer) {
        this.parameterContainer = configurationContainer;
    }
}
