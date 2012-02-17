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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.EventObject;

/**
 * Desktop client event describes an event on the client side, such as a local
 * mouse and keyboard event or a change of the clipboard content on the local system.
 *
 * @product.signature
 */
public class RemoteDesktopClientEvent extends EventObject implements RfbConstants {

    public static final int CLIENT_POINTER_EVENT = 1;
    public static final int CLIENT_KEY_EVENT = 2;
    public static final int CLIENT_CLIPBOARD_EVENT = 3;

    private int messageType = -1;

    private long when = System.currentTimeMillis();
    
    private MouseEvent mouseEvent;
    
    private KeyEvent keyEvent;

    private String clipboardText;

    public RemoteDesktopClientEvent(RemoteDesktopClient source, MouseEvent e) {
        super(new WeakReference(source));
        this.mouseEvent = e;
        this.messageType = CLIENT_POINTER_EVENT;
    }

    public RemoteDesktopClientEvent(RemoteDesktopClient source, KeyEvent e) {
        super(new WeakReference(source));
        this.keyEvent = e;
        this.messageType = CLIENT_KEY_EVENT;
    }

    public RemoteDesktopClientEvent(RemoteDesktopClient source, String clipboardText) {
        super(new WeakReference(source));
        this.clipboardText = clipboardText;
        this.messageType = CLIENT_CLIPBOARD_EVENT;
    }

    public int getMessageType() {
        return messageType;
    }

    public long getWhen() {
        return when;
    }

    public void setWhen(long when) {
        this.when = when;
    }

    public MouseEvent getMouseEvent() {
        return mouseEvent;
    }

    public KeyEvent getKeyEvent() {
        return keyEvent;
    }

    @Override
    public Object getSource() {
        Object o = super.getSource();
        if (o instanceof WeakReference) {
            return ((WeakReference)o).get();
        }
        return o;
    }
    
    public RemoteDesktopClient getClient() {
        return (RemoteDesktopClient)getSource();
    }

    /**
     * @return the clipboardText
     */
    public String getClipboardText() {
        return clipboardText;
    }
}
