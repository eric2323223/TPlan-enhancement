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

import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.plugin.PluginEvent;
import com.tplan.robot.plugin.PluginInfo;
import com.tplan.robot.plugin.PluginListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Abstract table model for GUI tables displaying a list of plugins ({@link PluginInfo} instances).
 * @product.signature
 */
abstract class AbstractPluginTableModel extends DefaultTableModel implements PluginListener, MouseListener {

    String columns[];

    List<PluginInfo> plugins = new ArrayList();
    private boolean ascendingOrder = true;
    private int sortColumn = -1;
    private JTable table;

    AbstractPluginTableModel(JTable table, String[] columnHeaders) {
        this.columns = columnHeaders;
        this.table = table;
        refresh();
        PluginManager.getInstance().addPluginListener(this);
        table.getTableHeader().addMouseListener(this);
    }

    public PluginInfo getPlugin(int index) {
        if (plugins != null && index < plugins.size()) {
            return plugins.get(index);
        }
        return null;
    }

    protected void sort(boolean ascending, int column) {
        if (plugins.size() > 1) {
            List<PluginInfo> l = new ArrayList(plugins);

            // Primitive bubble sort is sufficient for our purposes
            PluginInfo temp;
            Comparable c1, c2;
            int r;
            boolean change = false;
            int size = l.size();
            do {
                change = false;
                for (int i = 0; i < size - 1; i++) {
                    c1 = getValueAt(l, i, column);
                    c2 = getValueAt(l, i + 1, column);
                    r = c2.compareTo(c1);
                    if (r > 0) {
                        if (!ascending) {
                            temp = l.get(i);
                            l.remove(i);
                            l.add(i + 1, temp);
                            change = true;
                        }
                    } else if (r < 0) {
                        if (ascending) {
                            temp = l.get(i);
                            l.remove(i);
                            l.add(i + 1, temp);
                            change = true;
                        }
                    }
                }
            } while (change);
            plugins = l;
        }
    }

    public int getIndex(Plugin p) {
        Plugin pp;
        for (int i = 0; i < plugins.size(); i++) {
            pp = plugins.get(i);
            if (pp.equals(p)) {
                return i;
            }
        }
        return -1;
    }

    public abstract List<PluginInfo> getPluginList();

    protected void refresh() {

        // Save reference to the currently selected plugin
        Plugin selected = null;
        int index = table.getSelectedRow();
        if (index >= 0 && plugins != null && index < plugins.size()) {
            selected = plugins.get(index);
        }

        // Reload the plugin list
        plugins = getPluginList();

        // If sorting is enabled, sort the plugin list
        if (sortColumn >= 0 && sortColumn < getColumnCount()) {
            sort(isAscendingOrder(), sortColumn);
        }

        fireTableDataChanged();

        // Restore selection if the plugin is still there
        if (selected != null) {
            index = getIndex(selected);
            if (index >= 0) {
                table.getSelectionModel().setSelectionInterval(index, index);
            }
        }
    }

    public int getRowCount() {
        return plugins == null ? 0 : plugins.size();
    }

    public int getColumnCount() {
        return columns.length;
    }

    public String getColumnName(int columnIndex) {
        return columnIndex < columns.length ? columns[columnIndex] : null;
    }

    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    abstract protected Comparable getValueAt(List<PluginInfo> l, int rowIndex, int columnIndex);

    abstract protected String getStatus(Plugin p);

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getValueAt(plugins, rowIndex, columnIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // Do nothing
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void pluginEvent(PluginEvent e) {
        refresh();
    }

    /**
     * @return the ascendingOrder
     */
    public boolean isAscendingOrder() {
        return ascendingOrder;
    }

    /**
     * @param ascendingOrder the ascendingOrder to set
     */
    public void setAscendingOrder(boolean ascendingOrder) {
        if (this.ascendingOrder != ascendingOrder) {
            this.ascendingOrder = ascendingOrder;
            refresh();
        }
    }

    /**
     * @return the sortColumn
     */
    public int getSortColumn() {
        return sortColumn;
    }

    /**
     * @param sortColumn the sortColumn to set
     */
    public void setSortColumn(int sortColumn) {
        if (this.sortColumn != sortColumn) {
            this.sortColumn = sortColumn;
            if (sortColumn >= 0) {
                refresh();
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        int index = table.getTableHeader().columnAtPoint(e.getPoint());
        if (index >= 0 && index < getColumnCount()) {
            setSortColumn(index);
            setAscendingOrder(!isAscendingOrder());
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {

    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }
}
