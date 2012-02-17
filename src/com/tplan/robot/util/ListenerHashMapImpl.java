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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;
import java.util.Map;

/**
 * Hash map extended to fire a property event whenever its data gets modified.
 *
 * @product.signature
 */
public class ListenerHashMapImpl<K, V> extends Hashtable<K, V> implements ListenerMap<K, V> {

    private PropertyChangeSupport l = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        l.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        l.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListeners() {
        for (PropertyChangeListener listener : l.getPropertyChangeListeners()) {
            l.removePropertyChangeListener(listener);
        }
    }

    private void firePropertyChange(PropertyChangeEvent e) {
        l.firePropertyChange(e);
    }

    public synchronized V put(K key, V value) {
        V v = get(key);
        if (value != null) {
            V retVal = super.put(key, value);
            firePropertyChange(new PropertyChangeEvent(this, key.toString(), v, value));
            return retVal;
        }
        return null;
    }

    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        super.putAll(m);
        firePropertyChange(new PropertyChangeEvent(this, "", null, m));
    }

    public synchronized V remove(Object key) {
        V v = super.remove(key);
        firePropertyChange(new PropertyChangeEvent(this, key.toString(), v, null));
        return v;
    }

    public synchronized void clear() {
        super.clear();
        firePropertyChange(new PropertyChangeEvent(this, "", this, null));
    }
}
