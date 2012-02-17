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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Case tolerant hash map. If the key is a String, the map is able to deliver
 * the value even if the key is specified in an incorrect character case (for
 * example "Key" instead of "key"). As this is a low performance map it should be
 * used only for maps of small size.
 *
 * @product.signature
 */
public class CaseTolerantHashMap<K,V> extends HashMap<K,V> {

    public CaseTolerantHashMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    public CaseTolerantHashMap() {
        super();
    }

    public V get(Object key) {
        V v = super.get(key);
        if (v == null && key instanceof String) {
            v = super.get(getKey(key));
        }
        return v;
    }

    public V remove(Object key) {
        V v = super.remove(key);
        if (v == null && key instanceof String) {
            v = super.remove(getKey(key));
        }
        return v;
    }

     public boolean containsKey(Object key) {
        boolean b = super.containsKey(key);
        if (!b && key instanceof String) {
            Set set = keySet();
            String ks = (String)key;
            for (Object k : set) {
                if (k instanceof String && ks.toUpperCase().equals((String)k)) {
                    return true;
                }
            }
        }
        return b;
     }

     private Object getKey(Object key) {
         if (key instanceof String) {
            Set set = keySet();
            String ks = (String)key;
            for (Object k : set) {
                if (k instanceof String && ks.toUpperCase().equals((String)k)) {
                    return k;
                }
            }
         }
         return key;
     }
}
