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
 * This interface declares client capability to receive updates of the
 * server system clipboard. Clients implementing this interface declare that they may
 * fire events of clipboard changes through the {@link RemoteDesktopServerListener}
 * listener mechanism.
 *
 * @product.signature
 */
public interface ClipboardTransferCapable extends Capability {

    /**
     * Allows to switch dynamically support of clipboard transfer.
     * @return true if the desktop supports clipboard events, false if not.
     */
    boolean isClipboardTransferSupported();
}
