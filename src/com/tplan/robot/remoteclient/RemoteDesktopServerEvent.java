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
package com.tplan.robot.remoteclient;

import com.tplan.robot.remoteclient.rfb.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.EventObject;

/**
 * Desktop client event describes an event on the server side, such as server
 * connect or disconnect, desktop image update, change of the clipboard content
 * on the server side and other.
 *
 * @product.signature
 */
public class RemoteDesktopServerEvent extends EventObject implements RfbConstants {

    public static final int SERVER_CONNECTING_EVENT = 101;
    public static final int SERVER_CONNECTED_EVENT = 102;
    public static final int SERVER_INIT_EVENT = 103;
    public static final int SERVER_DISCONNECTING_EVENT = 104;
    public static final int SERVER_DISCONNECTED_EVENT = 105;
    public static final int SERVER_CONNECTION_POOLED = 121;
    public static final int SERVER_CONNECTION_REUSED = 122;

    public static final int SERVER_UPDATE_EVENT = 111;
    public static final int SERVER_BELL_EVENT = 112;
    public static final int SERVER_CLIPBOARD_EVENT = 113;

    public static final int SERVER_IO_ERROR_EVENT = 200;

    private int messageType = -1;

    private Rectangle updateRect = null;

    private Exception exception;

    private long when = System.currentTimeMillis();

    private String clipboardText = null;

    private Resumable resumable;

    public RemoteDesktopServerEvent(RemoteDesktopClient source, int messageType) {
        super(new WeakReference(source));
        this.messageType = messageType;
    }

    public RemoteDesktopServerEvent(RemoteDesktopClient source, Rectangle updateRect) {
        super(new WeakReference(source));
        this.messageType = SERVER_UPDATE_EVENT;
        this.updateRect = new Rectangle(updateRect);
    }

    public RemoteDesktopServerEvent(RemoteDesktopClient source, String clipboardText) {
        super(new WeakReference(source));
        this.clipboardText = clipboardText;
        this.messageType = SERVER_CLIPBOARD_EVENT;
    }

    public RemoteDesktopServerEvent(RemoteDesktopClient source, Exception ex) {
        super(new WeakReference(source));
        this.exception = ex;
        this.messageType = SERVER_IO_ERROR_EVENT;
    }

    public int getMessageType() {
        return messageType;
    }

    public Rectangle getUpdateRect() {
        return updateRect;
    }

    public Exception getException() {
        return exception;
    }

    public long getWhen() {
        return when;
    }

    public void setWhen(long when) {
        this.when = when;
    }

    public String getClipboardText() {
        return clipboardText;
    }

    public RemoteDesktopClient getClient() {
        return (RemoteDesktopClient)getSource();
    }

    @Override
    public Object getSource() {
        Object o = super.getSource();
        if (o instanceof WeakReference) {
            return ((WeakReference)o).get();
        }
        return o;
    }

    /**
     * @return the resumable
     */
    public Resumable getResumable() {
        return resumable;
    }

    public boolean canContinue() {
        return resumable == null || resumable.canResume();
    }

    public String toString() {
        String s = "RemoteDesktopServerEvent@"+hashCode()+"[type=";
        switch (messageType) {
            case SERVER_CONNECTING_EVENT:
                s += "Connecting to "+getClient().getConnectString();
                break;
            case SERVER_CONNECTED_EVENT:
                s += "Connected to "+getClient().getConnectString();
                break;
            case SERVER_CONNECTION_POOLED:
                s += "Pooled connection to "+getClient().getConnectString();
                break;
            case SERVER_CONNECTION_REUSED:
                s += "Reused connection to "+getClient().getConnectString();
                break;
            case SERVER_BELL_EVENT:
                s += "Bell";
                break;
            case SERVER_CLIPBOARD_EVENT:
                s += "Clipboard received: \""+getClipboardText()+"\"";
                break;
            case SERVER_DISCONNECTING_EVENT:
                s += "Disconnecting from"+getClient().getConnectString();
                break;
            case SERVER_DISCONNECTED_EVENT:
                s += "Disconnected from"+getClient().getConnectString();
                break;
            case SERVER_UPDATE_EVENT:
                s += "Update of ["+updateRect.x+","+updateRect.y+","+updateRect.width+","+updateRect.height+"]";
                break;
            case SERVER_INIT_EVENT:
                s += "Init";
                break;
            default:
                s += "unknown";
                break;
        }
        s += ",when="+getWhen();
        return s+"]";
    }
}
