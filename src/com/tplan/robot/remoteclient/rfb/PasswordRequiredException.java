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
package com.tplan.robot.remoteclient.rfb;

/**
 * A custom exception to be thrown when an RFB client attempts to authenticate 
 * to an RFB server and the server requests a password.
 * @product.signature
 */
public class PasswordRequiredException extends RfbException {

    /** This identifies which authentication scheme caused this exception. */
    private int securityType = -1;

    /**
     * Constructs a new exception with <code>null</code> as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public PasswordRequiredException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     * @param securityType security type required by the server (see the RFB protocol, message Security).
     */
    public PasswordRequiredException(String message, int securityType) {
        super(message);
        this.securityType = securityType;
    }

    /**
     * Get the security type (authentication type, authentication scheme) associated with this exception.
     * @return security type as defined by the Security message of the RFB protocol.
     */
    public int getSecurityType() {
        return securityType;
    }
}
