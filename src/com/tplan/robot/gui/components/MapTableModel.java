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

import com.tplan.robot.util.ListenerMap;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import javax.swing.table.AbstractTableModel;

/**
 * Table model for a java.util.Map data source. If the constructor sort argument is true, 
 * The model gets sorted automatically in ascending order by the first column data.
 * To fill in or refresh the data
 * call the {@link #refresh(java.util.Map)} method. If the argument map implements
 * the {@link ListenerMap} interface, the model registers itself to receive events
 * of the map changes. For such a map it is not necessary to call the refresh method
 * unless the map instance changes.
 * 
 * @product.signature
 */
public class MapTableModel extends AbstractTableModel implements PropertyChangeListener {

    Map t;
    private Object[] keys;
    String[] columnNames;
    boolean sort;

    public MapTableModel(Map t, String[] columnNames, boolean sort) {
        super();
        this.columnNames = columnNames;
        this.sort = sort;
        refresh(t);
    }

    public void refresh(Map t) {
        int size = this.t != null ? this.t.size() : 0;
        if (this.t instanceof ListenerMap) {
            ((ListenerMap) this.t).removePropertyChangeListener(this);
        }
        this.t = t;
        fireTableRowsDeleted(0, size);
        if (t instanceof ListenerMap) {
            ((ListenerMap) t).addPropertyChangeListener(this);
        }
        if (t != null) {
            this.keys = (new ArrayList(t.keySet())).toArray();
            if (sort) {
                Arrays.sort(keys);
            }
        }
        if (t != null) {
            fireTableRowsInserted(0, t.size());
        }
    }

    public String getColumnName(int column) {
        return columnNames[column].toString();
    }

    public synchronized int getRowCount() {
        return t == null ? 0 : t.size();
    }

    public int getColumnCount() {
        return 2;
    }

    public synchronized Object getValueAt(int row, int col) {
        if (t != null && keys != null && row < keys.length) {
            return col == 0 ? keys[row] : t.get(keys[row]);
        }
        return null;
    }

    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void setValueAt(Object value, int row, int col) {
    }

    public synchronized int getRowForVariable(String variable) {
        if (keys != null && keys.length > 0 && variable != null) {
            for (int i = 0; i < keys.length; i++) {
                if (variable.equals(keys[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        fireTableDataChanged();
    }
}
