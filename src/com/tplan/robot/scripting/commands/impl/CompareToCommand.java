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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.gui.components.ImageFileFilter;
import com.tplan.robot.gui.editor.ImageFileChooser;
import com.tplan.robot.scripting.commands.ExtendedParamsObject;
import com.tplan.robot.imagecomparison.ImageComparisonModule;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.scripting.wrappers.TextBlockWrapper;
import com.tplan.robot.imagecomparison.ImageComparisonModuleFactory;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.commands.AdvancedCommandHandler;
import com.tplan.robot.scripting.commands.CommandEditAction;
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import com.tplan.robot.util.CaseTolerantHashMap;
import com.tplan.robot.util.Utils;
import java.io.IOException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import static com.tplan.robot.scripting.ScriptingContext.*;

/**
 * Handler implementing functionality of the {@doc.cmd Compareto} command.
 * @product.signature
 */
public class CompareToCommand extends AbstractCommandHandler implements ConfigurationKeys, AdvancedCommandHandler {

    public static final String PARAM_PASSRATE = "passrate";
    public static final String PARAM_TEMPLATE = "template";
    public static final String PARAM_CMPAREA = "cmparea";
    public static final String PARAM_METHOD = "method";
    public static final String PARAM_METHODPARAMS = "methodparams";
    public static final String ACTION_EDIT_COMPARETO = "comparetoProperties";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static Map contextAttributes;
    public boolean enableMissingTemplates = false;

    /**
     * Get a map with context attributes.
     *
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes() {
        if (contextAttributes == null) {
            contextAttributes = new HashMap();
            ResourceBundle res = ApplicationSupport.getResourceBundle();
            contextAttributes.put(PARAM_ONFAIL, res.getString("command.param.onpass"));
            contextAttributes.put(PARAM_PASSRATE, res.getString("compareto.param.passrate"));
            contextAttributes.put(PARAM_ONPASS, res.getString("command.param.onpass"));
            String methods = "";
            for (Object s : ImageComparisonModuleFactory.getInstance().getAvailableModules()) {
                methods += s.toString() + "|";
            }
            if (methods.endsWith("|")) {
                methods = methods.substring(0, methods.length() - 1);
            }
            contextAttributes.put(PARAM_METHOD, methods);
            contextAttributes.put(PARAM_METHODPARAMS, res.getString("compareto.param.methodparams"));
            contextAttributes.put(PARAM_CMPAREA, res.getString("compareto.param.area"));
        }
        return contextAttributes;
    }

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as this command has a mandatory argument,
     * which is an image file name.
     *
     * @return description of the mandatory argument.
     */
    public String getContextArgument() {
        return ApplicationSupport.getString("compareto.argument");
    }

    /**
     * <p>Validate if the command has correct syntax.</p>
     *
     * <p>Command argument (the very
     * first item in <code>args</code>) is mandatory for image comparison modules
     * which require a template (their {@link ImageComparisonModule#isSecondImageRequired()} method returns true).
     * The argument value can be either a String with file name (absolute or
     * relative), a String containing list of files separated by the system path
     * separator (':' on Linux/Unix, ';' on Windows), a single <code>java.awt.Image</code> instance
     * or a list (<code>java.util.List</code> instance) with <code>java.awt.Image</code> instances.</p>
     *
     * <p>The method argument (key {@link #PARAM_METHOD} may be either a String with the
     * image comparison plugin code or directly an instance of {@link ImageComparisonModule}.</p>
     *
     * <p>Other parameters must have String values complying with the {@doc.cmd Compareto}
     * command specification.</p>
     */
    public void validate(List args, Map values, Map variableContainer, ScriptingContext ctx) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        Object parName;
        Object value;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        ImageComparisonModule mod;
        Object m = values.get(PARAM_METHOD);
        if (m instanceof ImageComparisonModule) {
            mod = (ImageComparisonModule) m;
        } else {
            try {
                mod = ImageComparisonModuleFactory.getInstance().getModule((String) m);
                if (mod == null) {
                    String s = res.getString("compareto.syntaxErr.invalidMethod");
                    throw new SyntaxErrorException(MessageFormat.format(s, m));
                }
            } catch (IllegalArgumentException ex) {
                throw new SyntaxErrorException(res.getString("compareto.syntaxErr.methodEmpty"));
            }
        }
        vt.put(PARAM_METHOD, mod);
        List<String> modParams = new ArrayList();
        if (mod instanceof ExtendedParamsObject) {
            modParams.addAll(((ExtendedParamsObject) mod).getParameters());
        }
        Map<String, String> modParamMap = new HashMap();

        if (mod.isSecondImageRequired()) {
            if (args.size() < 1) {
                throw new SyntaxErrorException(res.getString("compareto.syntaxErr.generic"));
            }

            parName = args.get(0);
            boolean ignoreMissingTemplate = ctx.getConfiguration().getBoolean("CompareToCommand.ignoreMissingTemplates").booleanValue();
            vt.put(PARAM_TEMPLATE, parseAndValidateTemplates(parName, ctx, !ignoreMissingTemplate && !enableMissingTemplates));
        }

        TokenParser parser = ctx.getParser();

        // Now proceed to other arguments
        for (int j = (mod.isSecondImageRequired() ? 1 : 0); j < args.size(); j++) {
            parName = args.get(j).toString().toLowerCase();
            value = values.get(parName);
            value = value == null ? "" : value;

            if (parName.equals(PARAM_ONFAIL)) {
                vt.put(PARAM_ONFAIL, value);
            } else if (parName.equals(PARAM_ONPASS)) {
                vt.put(PARAM_ONPASS, value);
            } else if (parName.equals(PARAM_METHODPARAMS)) {
                vt.put(PARAM_METHODPARAMS, value);
            } else if (parName.equals(PARAM_METHOD)) {
                // Already saved above
            } else if (parName.equals(PARAM_PASSRATE)) {
                vt.put(PARAM_PASSRATE, value instanceof Number ? value : parser.parsePercentage(value, PARAM_PASSRATE));
            } else if (parName.equals(PARAM_CMPAREA)) {
                vt.put(PARAM_CMPAREA, value instanceof Rectangle ? value : parser.parseRectangle(value, PARAM_CMPAREA));
            } else if (Utils.containsIgnoreCase(modParams, parName)) {
                modParamMap.put((String) parName, value.toString());
                try {
                    ((ExtendedParamsObject) mod).setParameters(modParamMap);
                } catch (Exception e) {
                    throw new SyntaxErrorException(e.getMessage());
                }
            } else {
                String s = res.getString("command.syntaxErr.unknownParam");
                throw new SyntaxErrorException(MessageFormat.format(s, parName));
            }
        }

        if (mod.isSecondImageRequired() && !vt.containsKey(PARAM_TEMPLATE)) {
            String s = res.getString("command.syntaxErr.mandatoryParam");
            throw new SyntaxErrorException(MessageFormat.format(s, PARAM_TEMPLATE));
        }

        validateOnPassAndOnFail(ctx, vt);
    }

    public static List parseAndValidateTemplates(Object argument, ScriptingContext ctx, boolean reportMissingTemplates) throws SyntaxErrorException {
        List l = new ArrayList();
        if (argument instanceof Image) {
            l.add(argument);
        } else if (argument instanceof Image[]) {
            Image[] ia = (Image[]) argument;
            l.addAll(Arrays.asList(ia));
        } else if (argument instanceof List) {
            List ll = new ArrayList();
            for (Object o : (List) argument) {
                if (o instanceof Image) {
                    ll.add(o);
                } else if (o instanceof File) {
                    File f = (File) o;
                    if (!f.isAbsolute()) {
                        f = new File(ctx.getScriptManager().assembleFileName(f.getPath(), ctx, IMPLICIT_VARIABLE_TEMPLATE_DIR));
                    }
                    if (!f.exists() || !f.canRead() || !f.isFile()) {
                        if (reportMissingTemplates) {
                            String txt = ApplicationSupport.getString("compareto.syntaxErr.cannotReadImage");
                            throw new SyntaxErrorException(MessageFormat.format(txt, f.getAbsolutePath()));
                        }
                    }
                    validateImageFormat(f);
                    ll.add(f);
                } else {
                    throw new SyntaxErrorException(MessageFormat.format(ApplicationSupport.getString("compareto.syntaxErr.imageListOnly"), o.getClass().getName()));
                }
            }
            l.addAll(ll);
        } else if (argument instanceof File) {
            File f = (File) argument;
            if (!f.isAbsolute()) {
                f = new File(ctx.getScriptManager().assembleFileName(f.getPath(), ctx, IMPLICIT_VARIABLE_TEMPLATE_DIR));
            }
            if (!f.exists() || !f.canRead() || !f.isFile()) {
                if (reportMissingTemplates) {
                    String txt = ApplicationSupport.getString("compareto.syntaxErr.cannotReadImage");
                    throw new SyntaxErrorException(MessageFormat.format(txt, f.getAbsolutePath()));
                }
            }
            validateImageFormat(f);
            l.add(f);
        } else if (argument instanceof String) {
            l.addAll(validateTemplateFileList((String) argument, ctx, reportMissingTemplates));
        }
        return l;
    }

    public static List<File> validateTemplateFileList(String s, ScriptingContext ctx, boolean reportMissingTemplates) throws SyntaxErrorException {
        List<File> l = new ArrayList();
        if (s != null) {
            String tokens[] = s.split(TokenParser.FILE_PATH_SEPARATOR);
            File f;
            for (String parName : tokens) {
                f = new File(ctx.getScriptManager().assembleFileName(parName.toString(), ctx, IMPLICIT_VARIABLE_TEMPLATE_DIR));
                if (!f.exists() || !f.canRead() || !f.isFile()) {
                    UserConfiguration cfg = ctx.getConfiguration();
                    if (reportMissingTemplates) {
                        String txt = ApplicationSupport.getString("compareto.syntaxErr.cannotReadImage");
                        throw new SyntaxErrorException(MessageFormat.format(txt, parName));
                    }
                }
                validateImageFormat(f);
                l.add(f);
            }
        }
        return l;
    }

    public static void validateImageFormat(File f) throws SyntaxErrorException {
        String ext = Utils.getExtension(f);
        if (ext == null || !Utils.getSupportedImageExtensions().contains(ext.toLowerCase())) {
            throw new SyntaxErrorException(MessageFormat.format(
                    ApplicationSupport.getString("compareto.syntaxErr.unsupportedFormat"), ext, Utils.getSupportedImageExtensions()));

        }
    }

    public static List<Image> convertToImageList(List l, ScriptingContext ctx) throws SyntaxErrorException {
        List<Image> templates = new ArrayList();
        if (l != null) {
            for (Object o : l) {
                File f;
                if (o instanceof Image) {
                    templates.add((Image) o);
                } else if (o instanceof File) {
                    // Bug 2886394 fix - if the file is relative, resolve the path the same way
                    // we do it for String input args
                    f = (File) o;
                    if (!f.isAbsolute()) {
                        f = new File(ctx.getScriptManager().assembleFileName(f.getPath(), ctx, IMPLICIT_VARIABLE_TEMPLATE_DIR));
                    }

                    try {
                        templates.add(ImageIO.read(f));
                    } catch (IOException e) {
                        return null;
                    }
                } else {
                    throw new SyntaxErrorException(MessageFormat.format(ApplicationSupport.getString("compareto.syntaxErr.imageAndFileListOnly"), o.getClass().getName()));
                }
            }
        }
        return templates;
    }

    public String[] getCommandNames() {
        return new String[]{"compareto"};
    }

    public int execute(List args, Map values, ScriptingContext ctx) throws SyntaxErrorException {

        // Validate
        Map params = new HashMap();
        validate(args, values, params, ctx);

        try {
            return handleCompareToEvent(ctx, params);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return 1;
    }

    int handleCompareToEvent(ScriptingContext context, Map params) throws InterruptedException, SyntaxErrorException {
        List tmplList = (List) params.get(PARAM_TEMPLATE);
        UserConfiguration cfg = context.getConfiguration();
        ScriptManager sh = context.getScriptManager();

        // Load all images specified as File instances
        List<Image> templates = convertToImageList((List) tmplList, context);
        if (templates == null) {
            return 2;
        }

        // Get the image comparison module
        ImageComparisonModule comparisonModule;
        Object m = params.get(PARAM_METHOD);
        if (m instanceof ImageComparisonModule) {
            comparisonModule = (ImageComparisonModule) m;
        } else {
            comparisonModule = ImageComparisonModuleFactory.getInstance().getModule((String) m);
        }

        // Read the pass rate. Default value is defined in user configuration.
        float passRate;
        if (params.containsKey(PARAM_PASSRATE)) {
            passRate = context.getParser().parseNumber(params.get(PARAM_PASSRATE), PARAM_PASSRATE).floatValue(); //((Number) params.get(PARAM_PASSRATE)).floatValue();
        } else {
            if ("search".equals(comparisonModule.getMethodName())) {
                passRate = cfg.getDouble("CompareToCommand.defaultSearchPassRate").floatValue();
            } else {
                passRate = cfg.getDouble("CompareToCommand.defaultPassRate").floatValue();
            }
        }
        long time = System.currentTimeMillis();

        Map vars = context.getVariables();
        RemoteDesktopClient client = context.getClient();
        Image img = client.getImage();
        float rate = 0;
        Image templateImage = null;
        int i = 0;
        Rectangle r;
        for (; i < templates.size(); i++) {
            templateImage = templates.get(i);
            r = params.containsKey(PARAM_CMPAREA)
                    ? context.getParser().parseRectangle(params.get(PARAM_CMPAREA), PARAM_CMPAREA)
                    : null;
            rate = 100 * comparisonModule.compare(img,
                    r,
                    templateImage,
                    (String) params.get(PARAM_METHODPARAMS),
                    context,
                    passRate / 100);
            if (passRate <= rate) {
                break;
            }
        }
        time = System.currentTimeMillis() - time;

        context.getScriptManager().fireScriptEvent(new ScriptEvent(this, null, context, ""));

        // Update the implicit variables
        vars.put(COMPARETO_RESULT, "" + rate);
        vars.put(COMPARETO_PASS_RATE, "" + passRate);
        vars.put(COMPARETO_TIME_IN_MS, "" + time);

        vars.remove(COMPARETO_TEMPLATE);
        vars.remove(COMPARETO_TEMPLATE_INDEX);
        vars.remove(COMPARETO_TEMPLATE_WIDTH);
        vars.remove(COMPARETO_TEMPLATE_HEIGHT);
        
        if (passRate <= rate) {
            vars.put(COMPARETO_TEMPLATE, tmplList.get(i).toString());
            vars.put(COMPARETO_TEMPLATE_INDEX, "" + i);
            if (templateImage != null) {
                vars.put(COMPARETO_TEMPLATE_WIDTH, "" + templateImage.getWidth(context.getEventSource()));
                vars.put(COMPARETO_TEMPLATE_HEIGHT, "" + templateImage.getHeight(context.getEventSource()));
            }
        }

        int returnValue = 0;

        // This means that the comparison didn't pass and the current image is probably different
        if (passRate > rate) {
            if (params.containsKey(PARAM_ONFAIL)) {
                String command = (String) params.get(PARAM_ONFAIL);
                if (command != null && !"".equals(command.trim())) {
                    if (context.getInterpret() instanceof ProprietaryTestScriptInterpret) {
                        ((ProprietaryTestScriptInterpret) context.getInterpret()).runBlock(new TextBlockWrapper(command, true), context);
                    }
                }
            }
            returnValue = 1;
        } else if (params.containsKey(PARAM_ONPASS)) {
            String command = (String) params.get(PARAM_ONPASS);
            if (command != null && !"".equals(command.trim())) {
                if (context.getInterpret() instanceof ProprietaryTestScriptInterpret) {
                    ((ProprietaryTestScriptInterpret) context.getInterpret()).runBlock(new TextBlockWrapper(command, true), context);
                }
            }
        }
        return returnValue;
    }

    @Override
    public List<Preference> getPreferences() {
        List v = new ArrayList();
        Preference o;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        // General options
        o = new Preference("CompareToCommand.maxLoadedPixelRows",
                Preference.TYPE_INT,
                res.getString("options.compareto.pixelRows.name"),
                MessageFormat.format(res.getString("options.compareto.pixelRows.desc"), ApplicationSupport.APPLICATION_NAME));
        o.setMinValue(1);
        o.setPreferredContainerName(res.getString("options.compareto.groupTitle.general"));
        v.add(o);

        o = new Preference("CompareToCommand.ignoreMissingTemplates",
                Preference.TYPE_BOOLEAN,
                res.getString("options.compareto.missingTemplates.name"),
                MessageFormat.format(res.getString("options.compareto.missingTemplates.desc"), ApplicationSupport.APPLICATION_NAME));
        o.setPreferredContainerName(res.getString("options.compareto.groupTitle.general"));
        v.add(o);

        // "default" module options
        o = new Preference("CompareToCommand.defaultPassRate",
                Preference.TYPE_FLOAT,
                res.getString("options.compareto.defaultPassRate.name"),
                null);
        o.setMinValue(0);
        o.setMaxValue(100);
        o.setPreferredContainerName(res.getString("options.compareto.groupTitle.default"));
        v.add(o);

        // "search" module options
        o = new Preference("CompareToCommand.defaultSearchPassRate",
                Preference.TYPE_FLOAT,
                res.getString("options.compareto.searchPassRate.name"),
                null);
        o.setMinValue(0);
        o.setMaxValue(100);
        o.setPreferredContainerName(res.getString("options.compareto.groupTitle.search"));
        v.add(o);

        o = new Preference("CompareToCommand.maxSearchHits",
                Preference.TYPE_INT,
                res.getString("options.compareto.searchHitLimit.name"),
                res.getString("options.compareto.searchHitLimit.desc"));
        o.setMinValue(1);
        o.setPreferredContainerName(res.getString("options.compareto.groupTitle.search"));
        v.add(o);

        return v;
    }

    public List getStablePopupMenuItems() {
        List v = new ArrayList();
        Action action = new CommandEditAction(this, ACTION_EDIT_COMPARETO);
        v.add(action);
        return v;
    }

    public List getArguments(String command, ScriptingContext context) {
        List l = new ArrayList();
        List params = new ArrayList();
        Map<String, String> t = context.getParser().parseParameters(command, params);
        List objectList = new ArrayList();
        objectList.add(getTemplateFileChooser(params.size() > 0 ? (String) params.get(0) : null, context, context.getTemplateDir()));
        try {
            File dummy = context.getTemplateDir().getCanonicalFile();
            objectList.add(dummy);
        } catch (IOException e) {
        }
        objectList.add(new Boolean(true));  // Do force relative resolution
        l.add(objectList);
        return l;
    }

    public static JFileChooser getTemplateFileChooser(String value, ScriptingContext context, File folder) {
        JFileChooser chooser = new ImageFileChooser(MainFrame.getInstance());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.addChoosableFileFilter(new ImageFileFilter());
        if (value != null && value.trim().length() > 0) {
            File f[] = context.getParser().parseFileList(value, folder); //
            chooser.setSelectedFiles(f);
        } else {
            chooser.setCurrentDirectory(folder);
        }
        return chooser;
    }

    public List getParameters(String command, ScriptingContext context) {
        List params = new ArrayList();
        Map<String, String> t = new CaseTolerantHashMap(context.getParser().parseParameters(command, params));
        List l = new ArrayList(Arrays.asList(new String[]{PARAM_CMPAREA, PARAM_METHOD, PARAM_ONFAIL, PARAM_ONPASS, PARAM_PASSRATE}));
        ImageComparisonModule m = ImageComparisonModuleFactory.getInstance().getModule(t.get(PARAM_METHOD));
        if (m != null) {
            if (m.isMethodParamsSupported()) {
                l.add(PARAM_METHODPARAMS);
            }
            if (m instanceof ExtendedParamsObject) {
                l.addAll(((ExtendedParamsObject) m).getParameters());
            }
        }
        return l;
    }

    public List getParameterValues(String paramName, String command, ScriptingContext context) {
        if (paramName != null) {
            if (paramName.equalsIgnoreCase(PARAM_CMPAREA)) {
                List l = new ArrayList();
                l.add(new Rectangle());
                return l;
            } else if (paramName.equalsIgnoreCase(PARAM_METHOD)) {
                return ImageComparisonModuleFactory.getInstance().getAvailableModules();
            } else if (!getContextAttributes().containsKey(paramName.toLowerCase())) {
                Map<String, String> t = new CaseTolerantHashMap(context.getParser().parseParameters(command));
                ImageComparisonModule m = ImageComparisonModuleFactory.getInstance().getModule(t.get(PARAM_METHOD));
                if (m instanceof ExtendedParamsObject) {
                    return ((ExtendedParamsObject) m).getParameterValues(paramName);
                }
            }
        }
        return null;
    }
}
