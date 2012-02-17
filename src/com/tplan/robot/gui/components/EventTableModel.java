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
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;

import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.TokenParserImpl;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Data model for tables showing events received from a desktop.
 * @product.signature
 */
public class EventTableModel extends DefaultTableModel {
    List v, t, events;
    String columnNames[] = {
            ApplicationSupport.getString("eventTableModel.columnServerEvent"),
            ApplicationSupport.getString("eventTableModel.columnTime")
        };
    int messageTypeFilter = -1;
    private RemoteDesktopClient client;
    TokenParser parser = new TokenParserImpl();

    public EventTableModel(RemoteDesktopClient rfbModule) {
        this.client = rfbModule;
    }

    public String getColumnName(int column) {
        return columnNames[column].toString();
    }

    public int getRowCount() {
        return v == null ? 0 : v.size();
    }

    public int getColumnCount() {
        return 2;
    }

    public synchronized Object getValueAt(int row, int col) {
        if (col == 0 && row < v.size()) {
            return v.get(row);
        } else if (col == 1 && row < t.size()) {
            return t.get(row);
        }
        return null;
    }

    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void setValueAt(Object value, int row, int col) {
    }

    synchronized public List getListOfSelectedEvents(JTable table) {
        List v = new ArrayList();
        if (events != null) {
            if (table.getSelectedRow() >= 0 && table.getSelectedRow() < events.size()) {
                int[] rows = table.getSelectedRows();
                for (int i = 0; i < rows.length; i++) {
                    v.add(events.get(rows[i]));
                }
            }
        }
        return v;
    }

    synchronized public List getEvents() {
        return new ArrayList(events);
    }

    public synchronized void refresh(List events, long baseTime) {
        this.events = new ArrayList();
        v = new ArrayList();
        t = new ArrayList();
        RemoteDesktopServerEvent e;
        long time;

        for (int i = 0; i < events.size(); i++) {
            e = (RemoteDesktopServerEvent) events.get(i);
            if (e != null) {
                time = e.getWhen() - baseTime;
                if (messageTypeFilter < 0 || e.getMessageType() == messageTypeFilter) {
                    if (e.getMessageType() == RemoteDesktopServerEvent.SERVER_UPDATE_EVENT) {
                        t.add(time + "ms");
                        double ratio = e.getUpdateRect().getWidth() * e.getUpdateRect().getHeight();
                        ratio = 100 * ratio / (getClient().getDesktopHeight() * getClient().getDesktopWidth());
                        String s = parser.rectToString(e.getUpdateRect()) + " (" + (int) ratio + "%)";
                        v.add(s);
                    } else if (e.getMessageType() == RemoteDesktopServerEvent.SERVER_BELL_EVENT) {
                        t.add(time + "ms");
                        v.add("BELL");
                    }
                    this.events.add(e);
                }
            }
        }
        fireTableDataChanged();
    }

    public void setMessageTypeFilter(int messageTypeFilter) {
        this.messageTypeFilter = messageTypeFilter;
    }

    /**
     * @return the client
     */
    public RemoteDesktopClient getClient() {
        return client;
    }

    /**
     * @param client the client to set
     */
    public void setClient(RemoteDesktopClient client) {
        this.client = client;
    }
}
