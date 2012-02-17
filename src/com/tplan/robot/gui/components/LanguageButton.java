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
import com.tplan.robot.l10n.LocalizationSupport;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Customized button with language popup menu. Unlike standard buttons this one
 * displays a pop up menu when selected and fires an ActionEvent only if a
 * different locale gets selected in the menu. To get the selected locale use the
 * {@link #getSelectedLocale()} method.
 * 
 * @product.signature
 */
public class LanguageButton extends JButton implements ActionListener {

    List<Locale> langs;
    private Locale selectedLocale;
    JPopupMenu menu;

    public LanguageButton() {
        setBorderPainted(false);
        setBackground(new Color(0, 0, 90));  // Default BG color - dark blue
        setForeground(Color.white);
        setRolloverEnabled(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        init();
        setSelectedLocale(LocalizationSupport.getLastLoadedLocale());
    }

    private void setSelectedLocale(Locale loc) {
        if (loc != null && langs.contains(loc)) {
            selectedLocale = loc;
        } else {
            selectedLocale = Locale.ENGLISH;
        }
        String s = getSelectedLocale().getLanguage().toUpperCase();
        String c = getSelectedLocale().getCountry();
        if (c != null && c.length() > 0) {
            s += "_" + c.toUpperCase();
        }
        setText(s);
    }

    private void init() {
        langs = LocalizationSupport.getAvailableResourceBundles(ApplicationSupport.APPLICATION_RESOURCE_BUNDLE_PREFIX, ApplicationSupport.class);
        menu = new JPopupMenu();
        JMenuItem item;
        for (Locale l : langs) {
            item = new JMenuItem(getLocaleDesc(l));
            item.addActionListener(this);
            menu.add(item);
        }
        menu.pack();
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(isEnabled() ? getBackground() : Color.darkGray);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    /**
     * Create a locale description.
     * @param loc
     * @return
     */
    private String getLocaleDesc(Locale loc) {
        String lang = loc.getDisplayLanguage(loc);
        String country = loc.getDisplayCountry(loc);
        if (lang == null || lang.equals("")) {
            lang = loc.getLanguage();
            return (country == null || country.equals("")) ? lang : lang + "_" + loc.getCountry();
        }
        return (country == null || country.equals("")) ? lang : lang + " (" + country + ")";
    }

    @Override
    public void fireActionPerformed(ActionEvent e) {

        // Only fire the action event when a locale gets selected
        if ("locale".equals(e.getActionCommand())) {
            super.fireActionPerformed(e);
            return;
        }

        // Otherwise display the menu
        if (!menu.isVisible()) {
            int height = menu.getHeight();
            if (height == 0) {
                height = (int) menu.getPreferredSize().getHeight();
            }
            Point loc = this.getLocation();
            menu.show((this), loc.x, loc.y - height);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JMenuItem) {
            int index = menu.getComponentIndex((Component) e.getSource());
            menu.setVisible(false);
            if (index < langs.size()) {
                Locale l = langs.get(index);
                if (selectedLocale != l) {
                    setSelectedLocale(l);
                    fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "locale"));
                }
            }
        }
    }

    /**
     * Get the selected locale.
     * @return the selectedLocale currently selected locale (language).
     */
    public Locale getSelectedLocale() {
        return selectedLocale;
    }
}
