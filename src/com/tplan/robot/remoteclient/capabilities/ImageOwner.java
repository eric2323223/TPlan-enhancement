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

/**
 * This interface declares client capability to own and maintain image of the
 * connected test environment (such as live desktop, static image, ...).
 *
 * @product.signature
 */
public interface ImageOwner extends Capability {
    /**
     * <p>Indicate whether the client is actively or passively updating the image.
     * This flag is used for optimization of the test framework.</p>
     *
     * <p>An active client typically updates the image at regular intervals or on a schedule
     * basis because the server doesn't notify the client of the image updates.
     * An example of an active client is the Java one.</p>
     *
     * <p>A passive client receives image updates asynchronously from the server
     * and it is safe to presume that the image is up to date with a small delay.
     * Examples of passive clients are RFB (VNC) and RDP.</p>
     *
     * @return true for actively updating clients, false for passive ones.
     */
    boolean isActivelyUpdating();

    /**
     * Indicate whether the image is a static one or a live one (dynamically updated).
     * This flag is used for optimization of the test framework.
     *
     * @return true for static image clients (such as the static image one), false
     * for dynamic ones (such as RFB/VNC or RDP).
     */
    boolean isStatic();
}
