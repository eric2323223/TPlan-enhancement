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
package com.tplan.robot.scripting.wrappers;

import com.tplan.robot.plugin.PluginFactory;
import com.tplan.robot.plugin.PluginInfo;
import java.util.List;

/**
 * Pluggable factory of nested block interprets.
 * @product.signature
 */
public class NestedBlockInterpretFactory extends PluginFactory {

    // Shared factory instance
    private static NestedBlockInterpretFactory instance;

    private NestedBlockInterpretFactory() {}

    /**
     * Get the shared factory instance.
     * @return the shared instance.
     */
    public static NestedBlockInterpretFactory getInstance() {
        if (instance == null) {
            instance = new NestedBlockInterpretFactory();
        }
        return instance;
    }

    public NestedBlockInterpret getInterpret(String code) {
        return (NestedBlockInterpret)getPluginByCode(code, NestedBlockInterpret.class);
    }

    public List<String> getAvailableCodes() {
        return getAvailablePluginCodes(NestedBlockInterpret.class);
    }

    public List<PluginInfo> getAvailablePlugins() {
        return getAvailablePluginInfos(NestedBlockInterpret.class);
    }
}
