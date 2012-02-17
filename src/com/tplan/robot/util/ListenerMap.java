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

import java.beans.PropertyChangeListener;
import java.util.Map;

/**
 * <p>Interface of a map able to fire a property event whenever its data gets modified.
 * The old/new value fields of the event must be filled according to the following system:</p>
 *
 * <ul>
 * <li>When a new key-value pair is inserted into the map through the {@link Map#put(java.lang.Object, java.lang.Object)}
 * method, the property name field must be <code>key.toString()</code>, the old value field must be set to null and the new value field to the new value.</li>
 * <li>When an existing key-value pair is being overwritten through the {@link Map#put(java.lang.Object, java.lang.Object)}
 * method, the property name field must be <code>key.toString()</code> and the old/new value fileds should reflect the old and new value.</li>
 * <li>When a map of key-value pairs is inserted using {@link Map#putAll(java.util.Map)}, the property name
 * must be an empty string, old value must be null and new value should be the map of inserted values.</li>
 * <li>When the map gets cleared through the {@link Map#clear()} method, the property name
 * must be an empty string, old value must be a reference to this map and the new value must be set to null.</li>
 * <ul>
 * @product.signature
 */
public interface ListenerMap<K,V> extends Map<K,V> {

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListeners();
}
