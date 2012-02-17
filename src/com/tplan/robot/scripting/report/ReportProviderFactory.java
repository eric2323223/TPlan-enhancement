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
package com.tplan.robot.scripting.report;

import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.plugin.PluginFactory;
import com.tplan.robot.plugin.PluginInfo;
import java.util.List;

/**
 * Report provider factory allowing to deliver report provider implementations
 * as plugins.
 *
 * @product.signature
 */
public class ReportProviderFactory extends PluginFactory {

    // This class implements the singleton pattern
    private static ReportProviderFactory instance;

    private ReportProviderFactory() {}

    /**
     * Get shared instance of this factory.
     * @return shared factory instance.
     */
    public static ReportProviderFactory getInstance() {
        if (instance == null) {
            instance = new ReportProviderFactory();
        }
        return instance;
    }

    /**
     * Get a provider associated with the given code.
     * @param provider provider code returned by the {@link ReportProvider#getCode()} method.
     * If the argument is null, the code defaults to "default" and returns the built-in
     * HTML report provider.
     *
     * @return an instance of the requested report provider or null if there's
     * no plugin associated with the code.
     */
    public ReportProvider getReportProvider(String provider) {
        if (provider == null) {
            provider = UserConfiguration.getInstance().getString("ReportCommand.defaultProvider");
            if (provider == null) {
                provider = "default";
            }
            ReportProvider instance = (ReportProvider)getPluginByCode(provider, ReportProvider.class);

            // This happens when user installs a provider plugin, sets it as the
            // default one and then uninstalls it -> falll back to the default one
            if (instance == null) {
                instance = (ReportProvider)getPluginByCode("default", ReportProvider.class);
            }
            return instance;
        }
        return (ReportProvider)getPluginByCode(provider, ReportProvider.class);
    }

    /**
     * Get available report provider plugins.
     * @return list of installed and enabled report provider plugins.
     */
    public List<PluginInfo> getAvailableProviders() {
        return getAvailablePluginInfos(ReportProvider.class);
    }

}
