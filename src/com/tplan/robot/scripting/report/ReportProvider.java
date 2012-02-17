/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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

import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.scripting.ScriptListener;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.commands.CommandEvent;
import java.util.Map;

/**
 * Exposed functional interface of report providers. A report provider is an
 * object which typically registers to script ({@link ScriptListener} and command {@link CommandEvent}
 * events and generates a report on the script execution. Report providers
 * are instantiated through {@link ReportProviderFactory}, usually by a call
 * of the {@doc.cmd report} command.
 *
 * @product.signature
 */
public interface ReportProvider extends Plugin {
    /**
     * Get report provider code. It serves as identifier in the scripting
     * language. For example, the default report provider implementation returns
     * "default".
     *
     * @return report provider code.
     */
    String getCode();

    /**
     * <p>Get formats supported by this particular provider. The formats are not 
     * case sensitive and they must correspond to the supported file extensions.
     * For example, a provider able to generate XML, HTML and Excel formats should 
     * return <code>new String[] {"xml", "html", "htm", "xls"}</code>.</p>
     * 
     * <p>The method is used by the calling {@doc.cmd report} instance to validate 
     * file name provided by the script command. If the file doesn't have one of 
     * the extensions supported by the selected provider, a syntax error is 
     * reported.
     * 
     * @return array of supported formats (file extensions).
     */
    String[] getSupportedFormats();

    /**
     * <p>Create a report using the scripting context. This method is called just once
     * right after an instance of this provider is created through the Report
     * command or it's associated Java test script method. It is up to the
     * implementing class whether it prefers to generate one-time report and
     * finish or register as ScriptListener and/or CommandListener and stay alive
     * refreshing the report with updates until the script execution finishes.</p>
     *
     * <p>The context provides access to all necessary objects associated with the
     * script and execution.</p>
     *
     * @param context scripting context of the currently executed script.
     * @param params Report command parameters parsed from the command. They typically
     * contain at least the output file name together with an optional description and scope.
     * See the {@doc.cmd report} command specification for more information.
     * @return exit code to be returned by the Report command - 0 means success,
     * other value means failure.
     */
    int create(ScriptingContext context, Map params);
}
