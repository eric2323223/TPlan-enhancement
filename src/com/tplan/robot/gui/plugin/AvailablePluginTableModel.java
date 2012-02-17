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
package com.tplan.robot.gui.plugin;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.plugin.PluginInfo;
import java.util.List;
import javax.swing.JTable;

/**
 * Table model for table displaying a list of available plugins ({@link PluginInfo} instances).
 * @product.signature
 */
class AvailablePluginTableModel extends AbstractPluginTableModel {

    AvailablePluginTableModel(JTable table) {
        super(table, new String[] {
            ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.tableHeader.name"),
            ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.tableHeader.version"),
            ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.tableHeader.group"),
            ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.tableHeader.status")
        });
    }

    /**
     * @return the availablePlugins
     */
    public List<PluginInfo> getPluginList() {
        return plugins;
    }

    /**
     * @param availablePlugins the availablePlugins to set
     */
    public void setAvailablePlugins(List<PluginInfo> availablePlugins) {
        this.plugins = availablePlugins;
        refresh();
    }

    protected String getStatus(Plugin p) {
        return PluginManager.getInstance().isInstalled(p)
                ? ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.statusInstalled")
                : ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.statusNotInstalled");
    }

    /**
     * @param availablePlugins the availablePlugins to set
     */
    public void addAvailablePlugins(List<PluginInfo> availablePlugins) {
        for (PluginInfo p : availablePlugins) {
            boolean add = true;
            for (Plugin pp : plugins) {
                if (p.equals(pp)) {
//                    add = false;
                }
            }
            if (add) {
                plugins.add(p);
            }
        }
        refresh();
    }

    protected Comparable getValueAt(List<PluginInfo> l, int rowIndex, int columnIndex) {
        if (rowIndex < getRowCount()) {
            PluginInfo p = l.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return p.getDisplayName();
                case 1:
                    return p.getVersionString();
                case 2:
                    String s = p.getGroupName();
                    return s == null ? ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.unknownGroup") : s;
                case 3:
                    return getStatus(p);
            }
        }
        return null;
    }
}