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

import com.tplan.robot.plugin.PluginFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for pluggable GUI components.
 *
 * @product.signature
 */
public class GuiComponentFactory extends PluginFactory {

    private static GuiComponentFactory instance;

    private GuiComponentFactory() {
    }

    /**
     * Get shared instance of this factory.
     * @return shared instance of this factory.
     */
    public static GuiComponentFactory getInstance() {
        if (instance == null) {
            instance = new GuiComponentFactory();
        }
        return instance;
    }

    /**
     * Get a GUI component by name.
     *
     * @param name component name (code).
     * @return component with the given name, or null if not found.
     */
    public GuiComponent getComponent(String name) {
        return (GuiComponent) getPluginByCode(name, GuiComponent.class);
    }

    /**
     * Get a list of available pluggable GUI components.
     *
     * @return list of available pluggable GUI components.
     */
    public List<String> getAvailableComponents() {
        return getAvailablePluginCodes(GuiComponent.class);
    }

    /**
     * Get a list of all available GUI component instances.
     *
     * @return list of available pluggable GUI component instances.
     */
    public List<GuiComponent> getComponentInstances() {
        List<GuiComponent> l = new ArrayList();
        for (String s : getAvailablePluginCodes(GuiComponent.class)) {
            try {
                l.add(getComponent(s));
            } catch (Exception e) {
                System.out.println("Failed to install GUI component with plugin code \""+s+"\":");
                e.printStackTrace();
            }
        }
        return l;
    }
}
