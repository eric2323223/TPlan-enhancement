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
package com.tplan.robot.util;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;

/**
 * Custom event queue allowing to consume key events by a single exclusive
 * key listener.
 * @product.signature
 */
public class CustomEventQueue extends EventQueue {

    private AWTEventListener exclusiveListener;

    @Override
    public void dispatchEvent(AWTEvent theEvent) {
        if (exclusiveListener != null && theEvent instanceof KeyEvent) {
            exclusiveListener.eventDispatched(theEvent);
        } else {
            super.dispatchEvent(theEvent);
        }
    }

    /**
     * @return the exclusiveListener
     */
    public AWTEventListener getExclusiveListener() {
        return exclusiveListener;
    }

    /**
     * @param exclusiveListener the exclusiveListener to set
     */
    public void setExclusiveListener(AWTEventListener exclusiveListener) {
        this.exclusiveListener = exclusiveListener;
    }


}
