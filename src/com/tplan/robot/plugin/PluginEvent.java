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
package com.tplan.robot.plugin;

/**
 * Plugin event is fired by the {@link PluginManager} to {@link PluginListener}
 * instances and indicates that a plugin was installed, uninstalled, enabled or disabled.
 * @product.signature
 */
public class PluginEvent {
    /**
     * Event code {@value} indicates that a plugin was installed.
     */
    public static final int PLUGIN_INSTALLED = 1;
    /**
     * Event code {@value} indicates that a plugin was uninstalled.
     */
    public static final int PLUGIN_UNINSTALLED = 2;
    /**
     * Event code {@value} indicates that a plugin was updated (upgraded to higher version).
     */
    public static final int PLUGIN_UPDATED = 3;
    /**
     * Event code {@value} indicates that a plugin was enabled.
     */
    public static final int PLUGIN_ENABLED = 4;
    /**
     * Event code {@value} indicates that a plugin was disabled.
     */
    public static final int PLUGIN_DISABLED = 5;

    private PluginInfo plugin;
    private Object source;
    private int eventType;

    /**
     * Event constructor.
     * @param source plugin source (most time a {@link PluginManager} instance).
     * @param eventType event type code.
     * @param plugin a plugin.
     */
    public PluginEvent(Object source, int eventType, PluginInfo plugin) {
        this.source = source;
        this.plugin = plugin;
        this.eventType = eventType;
    }
    
    /**
     * Get plugin associated with this event.
     * @return the plugin.
     */
    public PluginInfo getPluginInfo() {
        return plugin;
    }

    /**
     * Get source of the event (most time a {@link PluginManager} instance).
     * @return event source.
     */
    public Object getSource() {
        return source;
    }

    /**
     * Get the event type.
     * @return event type.
     */
    public int getEventType() {
        return eventType;
    }


}
