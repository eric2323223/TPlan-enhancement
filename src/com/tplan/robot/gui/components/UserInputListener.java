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

/**
 * This interface defines a custom listener which filters changes of a {@link FilteredDocument}. If the listener
 * doesn't like the change, it may throw a java.beans.PropertyVetoException. This will cause that the document
 * instance will consider its contents incorrect. See the {@link FilteredDocument} class for more info.
 *
 * @product.signature
 */
public interface UserInputListener extends java.util.EventListener {
    
    /**
     * Method will be called upon any edit action in the given component. If
     * you throw a PropertyVetoException, the value passed in the event will be
     * considered to be incorrect and refused by the component (which turns red
     * and displays its tool tip message)
     *
     * @param e a {@link UserInputEvent} event instance.
     * @throws java.beans.PropertyVetoException When the class implementing this
     * interface decides that the value is not correct, it may throw a
     * java.beans.PropertyVetoException.
     * @see UserInputEvent
     * @see java.beans.PropertyVetoException
     */
    public void vetoableChange(UserInputEvent e)
    throws java.beans.PropertyVetoException;
}
