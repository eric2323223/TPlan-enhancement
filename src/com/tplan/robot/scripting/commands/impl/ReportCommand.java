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
package com.tplan.robot.scripting.commands.impl;

import com.tplan.robot.preferences.Preference;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.GUIConstants;
import com.tplan.robot.gui.components.FileExtensionFilter;
import com.tplan.robot.plugin.PluginInfo;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.scripting.*;

import com.tplan.robot.scripting.commands.AdvancedCommandHandler;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.report.ReportProvider;
import com.tplan.robot.scripting.report.ReportProviderFactory;
import com.tplan.robot.util.DocumentUtils;
import com.tplan.robot.util.Utils;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.text.Element;

/**
 * Handler implementing functionality of the {@doc.cmd Report} command.
 * @product.signature
 */
public class ReportCommand extends AbstractCommandHandler implements AdvancedCommandHandler {

    public static final String PARAM_DESC = "desc";
    public static final String PARAM_SCOPE = "scope";
    public static final String PARAM_ZIP = "zip";
    public final static String PARAM_FILENAME = "filename";
    public static final String PARAM_PROVIDER = "provider";
    public static final String SCOPE_FILE = "file";
    public static final String SCOPE_COMBINED = "combined";
    public static final String SCOPE_ALL = "all";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static Map contextAttributes;

    // Enhancement in 2.0.1. Cache the created reports and offer them in the context menu.
    private static Map<Object, Action> reportCache = new HashMap();

    // Enhancement in 2.0.1. If the provider is not specified and the format
    // is not supported by the default one, look for other providers.
    // This map maintains mappings of supported formats (lower case file extensions) to provider codes.
    private static Map<String, List<ReportProvider>> formatMap;

    /**
     * Get a map with context attributes.
     *
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    @Override
    public Map getContextAttributes() {
        if (contextAttributes == null) {
            contextAttributes = new HashMap();
            ResourceBundle res = ApplicationSupport.getResourceBundle();
            contextAttributes.put(PARAM_DESC, res.getString("report.param.desc"));
            contextAttributes.put(PARAM_SCOPE, SCOPE_ALL + "|" + SCOPE_FILE);
            String s = "";
            List<PluginInfo> providers = ReportProviderFactory.getInstance().getAvailableProviders();
            if (providers != null) {
                for (PluginInfo o : providers) {
                    s += o.getCode() + "|";
                }
            }
            if (s.endsWith("|")) {
                s = s.substring(0, s.length() - 1);
            }
            contextAttributes.put(PARAM_PROVIDER, s);
        }
        return contextAttributes;
    }

    @Override
    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as this command has a mandatory argument,
     * which is an report file name.
     *
     * @return description of the mandatory argument.
     */
    @Override
    public String getContextArgument() {
        return ApplicationSupport.getString("report.argument");
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext ctx) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        String parName;
        String value;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        if (args.size() < 1) {
            throw new SyntaxErrorException(res.getString("report.syntaxErr.generic"));
        }

        Object out = args.get(0);
        if (out instanceof String || out instanceof File) {
            File f = null;
            try {
                // Test if we can create/delete the file
                if (out instanceof String) {
                    String fname = ctx.getScriptManager().assembleFileName(out.toString(), ctx, "_REPORT_DIR");
                    f = new File(fname);
                } else {
                    f = (File) out;
                    if (!f.isAbsolute()) {
                        f = new File(ctx.getScriptManager().assembleFileName(f.getName(), ctx, "_REPORT_DIR"));
                    }
                }
                if (!Utils.canCreateNewFile(f)) {
                    throw new Exception("Failed to create file " + f.getAbsolutePath());
                }
            } catch (Exception ex) {
                String s = res.getString("report.syntaxErr.cannotCreateFile");
                throw new SyntaxErrorException(MessageFormat.format(s, out));
            }
            vt.put(PARAM_FILENAME, f.getAbsolutePath());
        } else {
            throw new SyntaxErrorException(res.getString("report.syntaxErr.invalidFileParam"));
        }

        for (int i = 1; i < args.size(); i++) {
            parName = args.get(i).toString();
            value = (String) values.get(parName);
            value = value == null ? "" : value;

            try {
                if (parName.equals(PARAM_ZIP)) {
                    vt.put(PARAM_ZIP, ctx.getParser().parseBoolean(value, parName));
                } else if (parName.equals(PARAM_DESC)) {
                    value = value == null ? "" : value;
                    vt.put(PARAM_DESC, value);
                } else if (parName.equals(PARAM_PROVIDER)) {
                    value = value == null ? "" : value;
                    boolean found = false;
                    for (PluginInfo o : ReportProviderFactory.getInstance().getAvailableProviders()) {
                        if (o.getCode().equals(value)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        String s = res.getString("report.syntaxErr.unknownProvider");
                        throw new SyntaxErrorException(MessageFormat.format(s, value, getContextAttributes().get(PARAM_PROVIDER)));
                    }
                    vt.put(PARAM_PROVIDER, value);
                } else if (parName.equals(PARAM_SCOPE)) {
                    if (value != null && (value.equalsIgnoreCase(SCOPE_ALL) || value.equalsIgnoreCase(SCOPE_COMBINED) || value.equalsIgnoreCase(SCOPE_FILE))) {
                        // Bugfix - don't save the 'combined' scope value because it is not implemented
                        if (!value.equalsIgnoreCase(SCOPE_COMBINED)) {
                            vt.put(PARAM_SCOPE, value);
                        }
                    } else {
                        String s = res.getString("command.syntaxErr.oneOf");
                        throw new SyntaxErrorException(MessageFormat.format(s, parName, SCOPE_ALL + ", " + SCOPE_COMBINED + ", " + SCOPE_FILE));
                    }
                } else {
                    String s = res.getString("command.syntaxErr.unknownParam");
                    throw new SyntaxErrorException(MessageFormat.format(s, parName));
                }
            } catch (Exception ex) {
                if (ex instanceof SyntaxErrorException) {
                    throw (SyntaxErrorException) ex;
                }
                String s = res.getString("command.syntaxErr.unknownParam");
                throw new SyntaxErrorException(MessageFormat.format(s, parName));
            }
        }
        Map vars = ctx.getVariables();
        vars.put(ScriptingContext.REPORT_REPORT_FILE, vt.get(PARAM_FILENAME));
        String fName = new File(vt.get(PARAM_FILENAME).toString()).getName();
        Element elem = (Element) ctx.get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT);
        vars.put(ScriptingContext.REPORT_REPORT_FILENAME, fName);
        ctx.put(ScriptingContext.CONTEXT_REPORT_ELEMENT, elem);

        // Element map introduced in 2.2.1/2.0.6 to address PRB-3416.
        // It handles correctly multiple Report calls within a single script
        // as well as those in subscripts executed through the Run command
        LinkedHashMap<Element, Object[]> el = (LinkedHashMap) ctx.get(ScriptingContext.CONTEXT_REPORT_ELEMENT_LIST);
        if (el == null) {
            el = new LinkedHashMap();
            ctx.put(ScriptingContext.CONTEXT_REPORT_ELEMENT_LIST, el);
        } else {
            // Verify whether the report file has been already taken or not
            TestScriptInterpret ti;
            File f, currentFile = new File(vt.get(PARAM_FILENAME).toString());
            Object[] o;
            for (Element e : el.keySet()) {
                o = el.get(e);
                ti = (TestScriptInterpret) o[0];
                f = new File(o[1].toString());
                if (f.equals(currentFile)) {
                    throw new SyntaxErrorException("Report file '"+f.getAbsolutePath()+"' is already reserved by "+ti.getURI());
                }
            }
        }
        el.put(elem, new Object[] {ctx.getInterpret(), vt.get(PARAM_FILENAME)});


        if (vt.containsKey(PARAM_DESC)) {
            vars.put(ScriptingContext.REPORT_REPORT_DESC, vt.get(PARAM_DESC));
        }

        // Check if the format given by the file extension is supported by the provider
//        ReportProvider rp = ReportProviderFactory.getInstance().getReportProvider((String) vt.get(PARAM_PROVIDER));
//        if (rp == null) {
//            // This happens when user omits the provider in the Report command,
//            // sets default provider to a one installed in a plugin and then
//            // uninstalls the plugin.
//            String s = ApplicationSupport.getString("report.syntaxErr.invalidDefaultProvider");
//            throw new SyntaxErrorException(MessageFormat.format(s, ctx.getConfiguration().getString("ReportCommand.defaultProvider")));
//        }

        String fileName = out instanceof String ? (String) out : ((File) out).getName();
        String extension = Utils.getExtension(fileName);
        String provider = getProviderCodeForFormat(extension, (String) vt.get(PARAM_PROVIDER));
        vt.put(PARAM_PROVIDER, provider);

//        boolean supported = false;
//        for (String format : rp.getSupportedFormats()) {
//            if (format.equalsIgnoreCase(extension)) {
//                supported = true;
//                break;
//            }
//        }
//        if (!supported) {
//            throw new SyntaxErrorException(MessageFormat.format(
//                    res.getString("report.syntaxErr.unsupportedFormat"), rp.getCode(), extension == null ? "null" : extension.toUpperCase()));
//        }
    }

    public String[] getCommandNames() {
        return new String[]{"report"};
    }

    public int execute(List args, Map values, ScriptingContext ctx) throws SyntaxErrorException {

        Map t = new HashMap();

        // Validate
        validate(args, values, t, ctx);

        // Handle the event
        return handleReportEvent(ctx, t);
    }

    /**
     * Execute the report command. Please refer to the ReportProvider documentation as most functionality is
     * implemented there.
     *
     * @param ctx repository with the execution context.
     * @param params     command parameters which were parsed from the command text.
     * @return exit code - 0 on success or error code otherwise.
     */
    protected int handleReportEvent(ScriptingContext ctx, Map params) {
        String code = null;
        if (params.containsKey(PARAM_PROVIDER)) {
            code = params.get(PARAM_PROVIDER).toString();
        }
        ReportProvider ra = ReportProviderFactory.getInstance().getReportProvider(code);
        int retval = ra.create(ctx, params);
        if (retval == 0) {
            File f = new File((String) params.get(PARAM_FILENAME));
            if (f.exists() && f.canRead()) {
                Object key = ctx.get(ScriptingContext.CONTEXT_CURRENT_DOCUMENT_ELEMENT); //Utils.getFullPath(f);
                if (reportCache.containsKey(key)) {
                    fireCommandEvent(this, ctx, GUIConstants.EVENT_REMOVE_CUSTOM_ACTION_MSG, reportCache.get(key));
                }
                Action a = new ReportAction(f);
                reportCache.put(key, a);
                fireCommandEvent(this, ctx, GUIConstants.EVENT_ADD_CUSTOM_ACTION_MSG, a);
            }
        }
        return retval;
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns false.
     */
    @Override
    public boolean canRunWithoutConnection() {
        return true;
    }

    @Override
    public List getStablePopupMenuItems() {
        Action action = new ConfigureAction(this);
        List v = new ArrayList();
        v.add(action);
        return v;
    }

    @Override
    public List<Preference> getPreferences() {
        List v = new ArrayList();
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        Preference o = new Preference("ReportCommand.defaultProvider",
                Preference.TYPE_STRING,
                res.getString("options.report.defaultProvider.name"),
                res.getString("options.report.defaultProvider.desc"));
        o.setPreferredContainerName(res.getString("options.report.group.providers"));
        List<PluginInfo> providers = ReportProviderFactory.getInstance().getAvailableProviders();
        List displayValues = new ArrayList();
        List values = new ArrayList();
        for (PluginInfo pi : providers) {
            displayValues.add(pi.getDisplayName() + " (\"" + pi.getCode() + "\")");
            values.add(pi.getCode());
        }
        o.setDisplayValues(displayValues);
        o.setSelectOnly(true);
        o.setValues(values);
        v.add(o);

        return v;
    }

    private boolean supports(ReportProvider rp, String format) {
        String[] fts = rp.getSupportedFormats();
        if (fts != null) {
            for (String ft : fts) {
                if (ft.equalsIgnoreCase(format)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getProviderCodeForFormat(String extension, String explicitProvider) throws SyntaxErrorException {

        // If the provider is explicitly specified, check if it supports the specified
        // format and throw a SyntaxErrorException if not.
        if (explicitProvider != null) {
            ReportProvider rp = ReportProviderFactory.getInstance().getReportProvider(explicitProvider);
            if (supports(rp, extension)) {
                return explicitProvider;
            }
            throw new SyntaxErrorException("Provider \"" + explicitProvider + "\" doesn't support " + extension.toUpperCase() + " format!");
        }

        // Provider is not explicitly specified => check the default one first
        String defaultProvider = UserConfiguration.getInstance().getString("ReportCommand.defaultProvider");
        ReportProvider rp = ReportProviderFactory.getInstance().getReportProvider(defaultProvider);
        if (rp != null && supports(rp, extension)) {
            return defaultProvider;
        }

        // This code applies when the default provider doesn't support the given format.
        // We go through the list of providers in such a case and find the first
        // provider which supports it. If no provider is find we throw a SyntaxErrorException.
        // We maintain a map of formats and provider codes which gets populated below.
        if (extension != null && getFormatToProviderMap().containsKey(extension.toLowerCase())) {
            List<ReportProvider> l = formatMap.get(extension.toLowerCase());
            if (l.size() > 0) {
                return l.get(l.size() - 1).getCode();
            }
        }

        String msg;
        if (extension != null) {
            msg = "Format " + extension.toUpperCase() + " is not supported by any provider.";
        } else {
            msg = "Argument is not a valid report file.";
        }
        throw new SyntaxErrorException(msg);
    }

    private Map<String, List<ReportProvider>> getFormatToProviderMap() {
        if (formatMap == null) {
            formatMap = new HashMap();
            List<PluginInfo> providers = ReportProviderFactory.getInstance().getAvailableProviders();
            if (providers != null) {
                ReportProvider rp;
                for (PluginInfo o : providers) {
                    rp = ReportProviderFactory.getInstance().getReportProvider(o.getCode());
                    String[] fts = rp.getSupportedFormats();
                    if (fts != null) {
                        for (String ft : fts) {
                            ft = ft.toLowerCase();
                            List<ReportProvider> l = formatMap.get(ft);
                            if (l == null) {
                                l = new ArrayList();
                                formatMap.put(ft, l);
                            }
                            l.add(rp);
                        }
                    }
                }
            }
        }
        return formatMap;
    }

    public List getArguments(String command, ScriptingContext context) {
        List l = new ArrayList();
        List params = new ArrayList();
        context.getParser().parseParameters(command, params);
        List objectList = new ArrayList();
        objectList.add(getReportFileChooser(params.size() > 0 ? (String) params.get(0) : null, context, context.getOutputDir()));
        try {
            File dummy = context.getOutputDir().getCanonicalFile();
            objectList.add(dummy);
        } catch (IOException e) {
        }
        objectList.add(new Boolean(true));  // Do force relative resolution
        l.add(objectList);
        return l;
    }

    public static JFileChooser getReportFileChooser(String file, ScriptingContext context, File folder) {
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
        chooser.addChoosableFileFilter(new FileExtensionFilter(new String[]{"htm", "html"}, "HTML files"));
        chooser.addChoosableFileFilter(new FileExtensionFilter(new String[]{"xml"}, "XML files"));
        if (file != null && file.trim().length() > 0) {
            File f[] = context.getParser().parseFileList(file, folder); //
            chooser.setSelectedFiles(f);
        } else {
            chooser.setCurrentDirectory(folder);
        }
        return chooser;
    }

    public List getParameters(String command, ScriptingContext context) {
        return Arrays.asList(new String[]{PARAM_DESC, PARAM_PROVIDER, PARAM_SCOPE});
    }

    public List getParameterValues(String paramName, String command, ScriptingContext context) {
        if (PARAM_PROVIDER.equalsIgnoreCase(paramName)) {
            List params = new ArrayList();
            context.getParser().parseParameters(command, params);
            String ext = null;
            if (params.size() > 0) {
                ext = Utils.getExtension(params.get(0).toString());
            }
            if (ext != null) {  // There's a report file with extension specified
                List<String> l = new ArrayList();
                List<ReportProvider> rp = getFormatToProviderMap().get(ext.toLowerCase());
                if (rp != null && rp.size() > 0) {
                    for (ReportProvider r : rp) {
                        l.add(r.getCode());
                    }
                } else {  // No report provider for the given extension
                    l.add("default");
                }
                return l;
            }
        } else if (PARAM_SCOPE.equalsIgnoreCase(paramName)) {
            return Arrays.asList(new String[]{SCOPE_ALL, SCOPE_FILE});
        }
        return null;
    }

    protected class ReportAction extends AbstractAction {

        File f;

        ReportAction(File reportFile) {
            f = reportFile;
            String label = ApplicationSupport.getString("report.openAction");
            putValue(NAME, label);
            label = MessageFormat.format(ApplicationSupport.getString("report.openActionTT"), f.getName());
            putValue(SHORT_DESCRIPTION, label);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                Utils.execOpenURL(f.toURI().toURL().toString());
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
        }
    }
}