/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
import com.tplan.robot.plugin.PluginListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;

/**
 * Table model for table displaying a list of installed plugins ({@link PluginInfo} instances).
 * @product.signature
 */
class InstalledPluginTableModel extends AbstractPluginTableModel implements PluginListener, MouseListener {

    private boolean includeInternals = false;

    InstalledPluginTableModel(JTable table) {
        super(table, new String[] {
            ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.tableHeader.name"),
            ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.tableHeader.version"),
            ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.tableHeader.group"),
            ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.tableHeader.status")
        });
    }

    public List<PluginInfo> getPluginList() {
        PluginManager pm = PluginManager.getInstance();
        List<PluginInfo> l = pm.getPlugins();
        if (!isIncludeInternals()) {
            List<PluginInfo> l2 = new ArrayList();
            for (PluginInfo pi : l) {
                if (!pi.isBuiltIn()) {
                    l2.add(pi);
                }
            }
            l = l2;
        }
        return l;
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

    protected String getStatus(Plugin p) {
        return PluginManager.getInstance().isEnabled(p)
                ? ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.statusEnabled")
                : ApplicationSupport.getResourceBundle().getString("com.tplan.robot.pluginGui.statusDisabled");
    }

    /**
     * @return the includeInternals
     */
    public boolean isIncludeInternals() {
        return includeInternals;
    }

    /**
     * @param includeInternals the includeInternals to set
     */
    public void setIncludeInternals(boolean includeInternals) {
        if (this.includeInternals != includeInternals) {
            this.includeInternals = includeInternals;
            refresh();
        }
    }
}
