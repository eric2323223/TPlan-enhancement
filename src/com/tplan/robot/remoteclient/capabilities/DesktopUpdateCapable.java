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
package com.tplan.robot.remoteclient.capabilities;

import com.tplan.robot.remoteclient.RemoteDesktopServerListener;

/**
 * <p>This interface declares client capability to receive updates of the
 * remote desktop image. There's usually a mechanism allowing the server to notify
 * the client of any desktop change and clients are able maintain an up to date
 * copy of the server desktop image. There are no methods because clients
 * implementing this interface declare that they may fire events of desktop
 * update through the {@link RemoteDesktopServerListener}
 * listener interface. Typical examples of active protocols are RDP and RFB.</p>
 *
 * <p>The other sort of clients is sometimes called passive. There's usually no
 * mechanism to receive asynchonnous events about desktop updates. Such clients
 * typically refresh their local copy of desktop image at scheduled intervals
 * or according to a plan or strategy. The image they maintain may or may not be
 * up to date. Typical examples of such behavior are local desktop drivers, for
 * example the Java native client.</p>
 *
 * <p>This interface has impact on behavior of the <code>Waitfor update</code>
 * scripting command. If the client implements it, the command attaches to
 * the screen update message flow from the server and waits for the given
 * rectangle (or scope) to refresh. If the method returns false, the command
 * rather actively refreshes the image at scheduled intervals and attempts
 * to detect differences between the original and refreshed image. This may
 * lead to late desktop change detection. For this reason clients which
 * do not implement this interface are not recommended for load or performance
 * testing where it is necessary to measure exact time from a user action to
 * the corresponding desktop update (for example an application start).</p>
 *
 * @product.signature
 */
public interface DesktopUpdateCapable extends Capability {
    /**
     * Allows to switch dynamically support of desktop updates.
     * @return true if the desktop supports update events, false if not.
     */
    boolean isKeyTransferSupported();
}
