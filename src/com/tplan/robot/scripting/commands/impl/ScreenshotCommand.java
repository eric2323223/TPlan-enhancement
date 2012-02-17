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

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.imagecomparison.ImageComparisonModule;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.scripting.wrappers.GenericWrapper;
import com.tplan.robot.imagecomparison.ImageComparisonModuleFactory;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.TestWrapper;
import com.tplan.robot.scripting.commands.AdvancedCommandHandler;
import com.tplan.robot.scripting.commands.CommandEditAction;
import com.tplan.robot.scripting.commands.CommandEvent;
import com.tplan.robot.scripting.commands.ExtendedParamsObject;
import com.tplan.robot.scripting.commands.OutputObject;
import com.tplan.robot.util.CaseTolerantHashMap;
import com.tplan.robot.util.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Handler implementing functionality of the {@doc.cmd Screenshot} command.
 * @product.signature
 */
public class ScreenshotCommand extends AbstractCommandHandler implements ImageObserver, AdvancedCommandHandler {

    public static final String PARAM_FORMAT = "format";
    public static final String PARAM_FILENAME = "filename";
    public static final String PARAM_DESC = "desc";
    public static final String PARAM_ATTACH = "attach";
    public static final String PARAM_PASSRATE = "passrate";
    public static final String PARAM_TEMPLATE = "template";
    public static final String PARAM_METHOD = "method";
    public static final String PARAM_METHODPARAMS = "methodparams";
    public static final String PARAM_AREA = "area";
    public static final String ACTION_EDIT_SCREENSHOT = "screenshotProperties";
    public static final String ACTION_DEFINE_SCREENSHOT_AREA = "defineScreenshotArea";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static Map contextAttributes;
    public boolean enableImageComparisons = true;

    /**
     * Get a map with context attributes.
     *
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes() {
        if (contextAttributes == null) {
            contextAttributes = new HashMap();
            ResourceBundle res = ApplicationSupport.getResourceBundle();
            contextAttributes.put(PARAM_DESC, res.getString("screenshot.param.desc"));
            contextAttributes.put(PARAM_ATTACH, res.getString("screenshot.param.attach"));
            contextAttributes.put(PARAM_ONFAIL, res.getString("command.param.onpass"));
            contextAttributes.put(PARAM_PASSRATE, res.getString("compareto.param.passrate"));
            contextAttributes.put(PARAM_TEMPLATE, res.getString("screenshot.param.template"));
            contextAttributes.put(PARAM_ONPASS, res.getString("command.param.onpass"));

            String methods = "";
            for (Object s : ImageComparisonModuleFactory.getInstance().getAvailableModules()) {
                methods += s.toString() + "|";
            }
            if (methods.endsWith("|")) {
                methods = methods.substring(0, methods.length() - 1);
            }

            contextAttributes.put(PARAM_METHODPARAMS, res.getString("compareto.param.methodparams"));
            contextAttributes.put(PARAM_AREA, res.getString("screenshot.param.area"));
            contextAttributes.put(CompareToCommand.PARAM_CMPAREA, res.getString("screenshot.param.cmparea"));
        }
        return contextAttributes;
    }

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as this command has a mandatory argument,
     * which is an event identifier.
     *
     * @return description of the mandatory argument.
     */
    public String getContextArgument() {
        return ApplicationSupport.getString("screenshot.argument");
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext repository) throws SyntaxErrorException {
        values = new CaseTolerantHashMap(values);  // Make the parsed map case tolerant
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        String parName;
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        Object value;

        if (args.size() < 1) {
            throw new SyntaxErrorException(res.getString("screenshot.syntaxErr.generic"));
        }

        parName = args.get(0).toString();
        boolean failed = false;
        Exception exc = null;
        String fname = repository.getScriptManager().assembleFileName(parName, repository, "_REPORT_DIR");
        try {
            File f = new File(fname);
            if (f.getParentFile() != null) {
                f.getParentFile().mkdirs();
            }
            if (f.createNewFile()) {
                f.delete();
            }
        } catch (Exception ex) {
            exc = ex;
            failed = true;
        }
        if (failed) {
            String s = res.getString("report.syntaxErr.cannotCreateFile");
            String s2 = res.getString("screenshot.syntaxErr.errorMessage");
            throw new SyntaxErrorException(MessageFormat.format(s, fname) + (exc == null ? "" : MessageFormat.format(s2, exc.getMessage())));
        }
        vt.put(PARAM_FILENAME, parName);

        String format = Utils.getExtension(parName);
        boolean supported = false;
        String[] supportedFormats = ImageIO.getWriterFormatNames();
        for (int j = 0; format != null && j < supportedFormats.length; j++) {
            if (supportedFormats[j].equals(format)) {
                supported = true;
                break;
            }
        }

        // Unsupported format required - create a message which dynamically lists formats supported by ImageIO
        if (!supported) {
            String fs = "";
            for (int j = 0; j < supportedFormats.length; j++) {
                fs = fs + supportedFormats[j];
                if (j < supportedFormats.length - 1) {
                    fs += ", ";
                }
            }
            String msg = MessageFormat.format(res.getString("screenshot.syntaxErr.unsupportedImageFormat"), format, fs);
            throw new SyntaxErrorException(msg);
        }
        vt.put(PARAM_FORMAT, format);
        TokenParser parser = repository.getParser();

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
        List<String> modParams = new ArrayList();
        if (mod instanceof ExtendedParamsObject) {
            modParams.addAll(((ExtendedParamsObject) mod).getParameters());
        }
        Map<String, String> modParamMap = new HashMap();

        for (int i = 1; i < args.size(); i++) {
            parName = args.get(i).toString().toLowerCase();
            value = values.get(parName);
//            value = value == null ? "" : value;

            if (parName.equals(PARAM_DESC)) {
                vt.put(PARAM_DESC, value);
            } else if (parName.equals(PARAM_ATTACH)) {
                vt.put(PARAM_ATTACH, value);
            } else if (parName.equals(PARAM_PASSRATE)) {
                vt.put(PARAM_PASSRATE, value);
            } else if (parName.equals(PARAM_ONFAIL)) {
                vt.put(PARAM_ONFAIL, value);
            } else if (parName.equals(PARAM_ONPASS)) {
                vt.put(PARAM_ONPASS, value);
            } else if (parName.equals(PARAM_TEMPLATE)) {
                vt.put(PARAM_TEMPLATE, value);
            } else if (parName.equals(CompareToCommand.PARAM_CMPAREA)) {
                vt.put(CompareToCommand.PARAM_CMPAREA, parser.parseRectangle(value, CompareToCommand.PARAM_CMPAREA));
            } else if (parName.equals(PARAM_METHOD)) {
                vt.put(PARAM_METHOD, mod);
            } else if (parName.equals(PARAM_METHODPARAMS)) {
                vt.put(PARAM_METHODPARAMS, value);
            } else if (parName.equals(PARAM_AREA)) {
                Rectangle rectangle;
                if (value instanceof Rectangle) {
                    rectangle = (Rectangle) value;
                } else {
                    rectangle = parser.parseRectangle(value, PARAM_AREA);
                }
                vt.put(PARAM_AREA, rectangle);

                // Validate the rectangle
                RemoteDesktopClient rfb = repository.getClient();
                if (rfb != null && rfb.isConnected()) {
                    if (rectangle.x + rectangle.width > rfb.getDesktopWidth()) {
                        String s = res.getString("screenshot.syntaxErr.widthExceeded");
                        throw new SyntaxErrorException(MessageFormat.format(s, parName, rfb.getDesktopWidth()));
                    }
                    if (rectangle.y + rectangle.height > rfb.getDesktopHeight()) {
                        String s = res.getString("screenshot.syntaxErr.heightExceeded");
                        throw new SyntaxErrorException(MessageFormat.format(s, parName, rfb.getDesktopHeight()));
                    }
                }
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

        validateOnPassAndOnFail(repository, vt);
    }

    public String[] getCommandNames() {
        return new String[]{"screenshot"};
    }

    private boolean isComparison(Map params) {
        return params.containsKey(PARAM_ONFAIL) || params.containsKey(PARAM_ONPASS) ||
                params.containsKey(PARAM_TEMPLATE) || params.containsKey(PARAM_PASSRATE);
    }

    public int execute(List args, Map values, ScriptingContext repository) throws SyntaxErrorException {

        Map t = new HashMap();

        // Validate
        validate(args, values, t, repository);

        return handleScreenshotEvent(repository, t);
    }

    /**
     * Execute the Screenshot command.
     * @param context execution context.
     * @param params command parameters.
     * @return returns 2 when no screenshot gets created (e.g. because of I/O error), 1 if the screenshot was created
     * and comparison to a template failed, 0 if everything was OK.
     */
    private int handleScreenshotEvent(ScriptingContext context, Map params) {
        int returnValue = 0;
        try {
            if (context.getClient() == null || !context.getClient().isConnected()) {
                throw new SyntaxErrorException(MessageFormat.format(ApplicationSupport.getString("scriptHandler.syntaxError.commandRequiresConnection"), getCommandNames()[0].toUpperCase()));
            }
            ScriptManager handler = context.getScriptManager();
            String outParam = System.getProperty(ScriptManager.OUTPUT_DISABLED_FLAG);
            boolean outputDisabled = outParam != null && outParam.equals("true");
            if (outputDisabled) {
                return 0;
            }

            String name = handler.assembleFileName((String) params.get(PARAM_FILENAME), context, ScriptingContext.IMPLICIT_VARIABLE_REPORT_DIR);
//            System.out.println("Assembled file name = "+name);

            if (name == null) {
                return 2;
            }

            String description = (String) params.get(PARAM_DESC);
            if (description == null || description.equals("")) {
                description = ApplicationSupport.getString("command.Screenshot.noDesc");
            }

            String format = (String) params.get(PARAM_FORMAT);
            RemoteDesktopClient rfb = context.getClient();

            List<OutputObject> v = context.getOutputObjects();

            File f = new File(name);
            if (f.getParentFile() != null) {
                f.getParentFile().mkdirs();
            }

//            Font font = repository.getEventSource().getFont();
            int fbWidth = rfb.getDesktopWidth();
            int fbHeight = rfb.getDesktopHeight();

            if (fbWidth <= 0 || fbHeight <= 0) {
                return 2;
            }

            Image rfbImg = rfb.getImage();
            boolean isArea = params.containsKey(PARAM_AREA);

            // Full or custom rectangle
            Rectangle r = isArea ? (Rectangle) params.get(PARAM_AREA)
                    : new Rectangle(rfb.getDesktopWidth(), rfb.getDesktopHeight());

            BufferedImage img = null;

            if (rfbImg instanceof BufferedImage) {  // Optimized code for BufferedImage instance
                if (isArea) {
                    img = ((BufferedImage) rfbImg).getSubimage(r.x, r.y, r.width, r.height);
                } else {
                    img = (BufferedImage) rfbImg;
                }
            } else {
                img = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);

                Image img2;
                synchronized (rfbImg) {
                    img2 = ((BufferedImage) rfbImg).getSubimage(r.x, r.y, r.width, r.height);
                }
                Graphics2D g = (Graphics2D) img.getGraphics();
                g.drawImage(img2, 0, 0, context.getEventSource());
                g.dispose();
            }

            UserConfiguration cfg = context.getConfiguration();
            saveImage(img, format, f, cfg);

            ScreenshotInfo scrInfo = new ScreenshotInfo();
            scrInfo.date = new Date(System.currentTimeMillis());
            scrInfo.imageFile = f;
            scrInfo.width = img.getWidth();
            scrInfo.height = img.getHeight();
            scrInfo.shortName = (String) params.get(PARAM_FILENAME);

            scrInfo.description = description;
            if (params.containsKey(PARAM_ATTACH)) {
                scrInfo.attachments = (String) params.get(PARAM_ATTACH);
            }

            TestWrapper wrapper = context.getMasterWrapper();
            if (wrapper != null) {
                scrInfo.scriptName = wrapper.getScriptFile() != null
                        ? wrapper.getScriptFile().getAbsolutePath()
                        : ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.unnamedDocument");
                scrInfo.scriptLine = wrapper.getLineNumber(context);
                scrInfo.wrapperType = wrapper.getWrapperType();
                scrInfo.masterScript = getOriginScript(wrapper);
            }

            v.add(scrInfo);

            List<File> templateList = null;

            // If any of the 'compareto' command parameters are present, perform image comparison.
            boolean performComparison = isComparison(params);

            boolean templateSpecified = params.containsKey(PARAM_TEMPLATE);

            // Look if the Auto Comparison feature is on and if a template exists
            boolean autoComparison = cfg.getBoolean("ScreenshotCommand.autoComparison").booleanValue();
            boolean ignoreMissingTemplate = context.getConfiguration().getBoolean("CompareToCommand.ignoreMissingTemplates").booleanValue();

            if (templateSpecified) {   // A template is explicitly specified
                templateList = CompareToCommand.parseAndValidateTemplates(params.get(PARAM_TEMPLATE), context, !ignoreMissingTemplate);

            } else if (performComparison || autoComparison) {     // No template explicitly specified -> look for the default one
                String template = handler.assembleFileName(f.getName(), context, ScriptingContext.IMPLICIT_VARIABLE_TEMPLATE_DIR);

                // Look if other image types are allowed and if another template exists
                template = getTemplate(template, cfg);
                File tmpFile = new File(template);
                boolean templateExists = tmpFile.exists() && tmpFile.isFile() && !tmpFile.equals(f);

                // One of the comparison parameters was specified but no template
                // was neither specified nor found
                if (!templateExists && performComparison && !ignoreMissingTemplate) {
                    throw new SyntaxErrorException("No suitable image template found!");
                }

                if (templateExists) {
                    templateList = new ArrayList();
                    templateList.add(tmpFile);
                }
            }

            if (templateList != null) {   // Do not compare if the image files are identical
                CompareToCommand cmd = new CompareToCommand();
                params.put(PARAM_TEMPLATE, CompareToCommand.convertToImageList(templateList, context));

                returnValue = cmd.handleCompareToEvent(context, params);
                Map vars = context.getVariables();

                try {
                    scrInfo.comparisonResult = Float.parseFloat((String) vars.get(ScriptingContext.COMPARETO_RESULT));
                    scrInfo.comparisonPassRate = Float.parseFloat((String) vars.get(ScriptingContext.COMPARETO_PASS_RATE));
                    scrInfo.comparisonTime = Long.parseLong((String) vars.get(ScriptingContext.COMPARETO_TIME_IN_MS));
                    scrInfo.comparisonTemplate = templateList;
                    Object o = vars.get(ScriptingContext.COMPARETO_TEMPLATE_INDEX);
                    if (o != null) {
                        if (o instanceof Number) {
                            scrInfo.comparisonTemplateIndex = ((Number) o).intValue() + 1;
                        } else {
                            scrInfo.comparisonTemplateIndex = Integer.parseInt(o.toString()) + 1;
                        }
                    }
                } catch (Exception ex) {

                    // If an error happens during the Compareto command execution, the result and passrate are missing
                    // -> behave as if failed and print out a warning message to the console
                    scrInfo.comparisonResult = 0;
                    scrInfo.comparisonPassRate = 100;

                    System.out.println(MessageFormat.format(ApplicationSupport.getString("screenshot.syntaxErr.cannotReadTemplate"), templateList));
//                    ex.printStackTrace();
                }

            }


            fireCommandEvent(this, context, CommandEvent.OUTPUT_CHANGED_EVENT, scrInfo);

        } catch (Exception ex) {
            ex.printStackTrace();
            return 2;
        }
        return returnValue;
    }

    public static void saveImage(BufferedImage img, String format, File f, UserConfiguration cfg) throws IOException {
        if (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg")) {
            saveJPEG(img, f, cfg);
        } else {
            ImageIO.write(img, format, f);
        }
    }

    private static void saveJPEG(BufferedImage bimg, File file, UserConfiguration cfg) {

        // Encode as a JPEG
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        boolean success = false;

        // First try the internal Sun Java class to be able to set the quality.
        // If the code throws any exception or error, default to the standard
        // ImageIO class. This resolves compatibility with OpenJDK and
        // eventually other Java clones.
        try {
            JPEGImageEncoder jpeg = JPEGCodec.createJPEGEncoder(fos);
            JPEGEncodeParam param = jpeg.getDefaultJPEGEncodeParam(bimg);
            Number quality = cfg.getInteger("ScreenshotCommand.jpegQuality");
            float q = quality == null ? .7f : quality.floatValue() / 100;
            param.setQuality(q, false);
            jpeg.setJPEGEncodeParam(param);
            jpeg.encode(bimg);
            success = true;
        } catch (Throwable e) {
        }

        try {
            if (fos != null) {
                fos.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Failed -> use ImageIO
        if (!success) {
            try {
                ImageIO.write(bimg, "jpg", file);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public String getTemplate(String imageName, UserConfiguration cfg) {
        boolean lookForOtherImages = cfg.getBoolean("ScreenshotCommand.lookForOtherImageTypes").booleanValue();
        if (lookForOtherImages) {
            List v = cfg.getListOfStrings("ScreenshotCommand.templateFormats");
            File f = new File(imageName);
            if (f.getParentFile() != null) {
                File fs[] = f.getParentFile().listFiles();
                String name = f.getName().substring(0, f.getName().lastIndexOf('.'));
                String file, ext;
                int priority = v.size();
                for (int i = 0; fs != null && i < fs.length; i++) {
                    file = fs[i].getName();
                    ext = Utils.getExtension(file);
                    if (ext != null) {
                        file = file.substring(0, file.lastIndexOf("."));
                        if (file.equals(name)) {
                            ext = ext.toLowerCase();
                            if (v.contains(ext) && v.indexOf(ext) < priority) {
                                imageName = fs[i].getAbsolutePath();
                                priority = v.indexOf(ext);
                                if (priority == 0) {  // Highest priority image format found => return the template
                                    return imageName;
                                }
                            }
                        }
                    }
                }
            }
        }
        return imageName;
    }

    private String getOriginScript(TestWrapper wrapper) {
        if (wrapper != null) {
//            System.out.println("START: wrapper "+wrapper.getScriptFile()+", parent wrapper "+wrapper.getParentWrapper());
            while (wrapper.getParentWrapper() != null && wrapper.getWrapperType() != GenericWrapper.WRAPPER_TYPE_SCRIPT && wrapper.getWrapperType() != GenericWrapper.WRAPPER_TYPE_RUN) {
//                System.out.println("wrapper "+wrapper.getScriptFile()+", parent wrapper "+wrapper.getParentWrapper().getScriptFile());
                wrapper = wrapper.getParentWrapper();
            }
        }

        String script;
        File f = wrapper.getScriptFile();
        script = f == null ? ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.unnamedDocument")
                : f.getAbsolutePath();
        return script;
    }

    public List getStablePopupMenuItems() {
        List v = new ArrayList();
        Action action = new CommandEditAction(this, ACTION_EDIT_SCREENSHOT);
        v.add(action);
//        action = new CommandEditAction(this, ACTION_DEFINE_SCREENSHOT_AREA, ApplicationSupport.getString("screenshot.action.defineArea"));
//        v.add(action);
        action = new ConfigureAction(this);
        v.add(action);
        return v;
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns true as no VNC connection is needed for the screenshot command.
     *         If there's no connection, the screenshot image will contain the default orange-white image with product logo.
     */
    @Override
    public boolean canRunWithoutConnection() {
        return true;
    }

    @Override
    public List<Preference> getPreferences() {
        List v = new ArrayList();
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        Preference o = new Preference("ScreenshotCommand.jpegQuality",
                Preference.TYPE_INT,
                res.getString("options.screenshot.jpegQuality.name"),
                null);
        o.setPreferredContainerName(res.getString("options.screenshot.group.encoding"));
        o.setMinValue(1);
        o.setMaxValue(100);
        v.add(o);

        o = new Preference("ScreenshotCommand.autoComparison",
                Preference.TYPE_BOOLEAN,
                res.getString("options.screenshot.autoComparison.name"),
                res.getString("options.screenshot.autoComparison.desc"));
        o.setPreferredContainerName(res.getString("options.screenshot.group.comparison"));
        v.add(o);

        o = new Preference("ScreenshotCommand.lookForOtherImageTypes",
                Preference.TYPE_BOOLEAN,
                res.getString("options.screenshot.lookForOtherImgTypes.name"),
                MessageFormat.format(res.getString("options.screenshot.lookForOtherImgTypes.desc"), ApplicationSupport.APPLICATION_NAME));
        o.setPreferredContainerName(res.getString("options.screenshot.group.comparison"));
        v.add(o);

        return v;
    }

    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        return true;
    }

    public List getArguments(String command, ScriptingContext context) {
        List l = new ArrayList();
        List params = new ArrayList();
        context.getParser().parseParameters(command, params);
        List objectList = new ArrayList();
        JFileChooser ch = CompareToCommand.getTemplateFileChooser(params.size() > 0 ? (String) params.get(0) : null, context, context.getOutputDir());
        ch.setMultiSelectionEnabled(false);
        objectList.add(ch);
        try {
            File dummy = context.getOutputDir().getCanonicalFile();
            objectList.add(dummy);
        } catch (IOException e) {
        }
        objectList.add(new Boolean(true));  // Do force relative resolution
        l.add(objectList);
        return l;
    }

    public List getParameters(String command, ScriptingContext context) {
        List params = new ArrayList();
        Map<String, String> t = new CaseTolerantHashMap(context.getParser().parseParameters(command, params));
        List l = new ArrayList(Arrays.asList(new String[]{CompareToCommand.PARAM_CMPAREA,
                    PARAM_METHOD, PARAM_ONFAIL, PARAM_ONPASS, PARAM_PASSRATE, PARAM_AREA, PARAM_ATTACH, PARAM_DESC, PARAM_TEMPLATE}));
        if (isComparison(t)) {
            ImageComparisonModule m = ImageComparisonModuleFactory.getInstance().getModule(t.get(PARAM_METHOD));
            if (m != null) {
                if (m.isMethodParamsSupported()) {
                    l.add(PARAM_METHODPARAMS);
                }
                if (m instanceof ExtendedParamsObject) {
                    l.addAll(((ExtendedParamsObject) m).getParameters());
                }
            }
        }
        return l;
    }

    public List getParameterValues(String paramName, String command, ScriptingContext context) {
        if (paramName != null) {
            if (paramName.equalsIgnoreCase(CompareToCommand.PARAM_CMPAREA) || paramName.equalsIgnoreCase(PARAM_AREA)) {
                List l = new ArrayList();
                l.add(new Rectangle());
                return l;
            } else if (paramName.equalsIgnoreCase(PARAM_METHOD)) {
                return ImageComparisonModuleFactory.getInstance().getAvailableModules();
            } else if (!getContextAttributes().containsKey(paramName.toLowerCase())) {
                Map<String, String> t = new CaseTolerantHashMap(context.getParser().parseParameters(command));
                if (isComparison(t)) {
                    ImageComparisonModule m = ImageComparisonModuleFactory.getInstance().getModule(t.get(PARAM_METHOD));
                    if (m instanceof ExtendedParamsObject) {
                        return ((ExtendedParamsObject) m).getParameterValues(paramName);
                    }
                }
            }
        }
        return null;
    }

    public class ScreenshotInfo implements OutputObject {

        public String shortName;
        public File imageFile;
        public Date date;
        public String scriptName;
        public int scriptLine = -1;
        public String description;
        public String attachments;
        public int wrapperType = GenericWrapper.WRAPPER_TYPE_UNKNOWN;
        public String masterScript;
        public float comparisonResult = -1f;
        public float comparisonPassRate = -1f;
        public List<File> comparisonTemplate;
        public int comparisonTemplateIndex = -1;
        public long comparisonTime = -1;
        public int width;
        public int height;
        public List warnings;

        public int getType() {
            return TYPE_SCREENSHOT;
        }

        public String getDescription() {
            return description;
        }

        public Date getDate() {
            return date;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap();
            put(m, "name", shortName);
            put(m, "file", imageFile.getAbsolutePath());
            put(m, "date", date);
            if (scriptName != null) {
                put(m, "script", scriptName);
                put(m, "scriptname", new File(scriptName).getName());
            }
            if (scriptLine >= 0) {
                m.put("line", scriptLine);
            }
            put(m, "desc", description);
            if (attachments != null) {
                List<File> l = new ArrayList();
                String f[] = attachments.split(";");
                for (String s : f) {
                    l.add(new File(s));
                }
                m.put("attach", l);
            }
            if (comparisonTemplateIndex >= 0) {
                put(m, "cmpindex", comparisonTemplateIndex);
            }
            if (comparisonResult >= 0) {
                put(m, "cmpresult", comparisonResult);
                put(m, "cmppassed", comparisonResult >= comparisonPassRate);
            }
            if (comparisonPassRate >= 0) {
                put(m, "cmppassrate", comparisonPassRate);
            }
            if (comparisonTime >= 0) {
                put(m, "cmptimems", comparisonTime);
                put(m, "cmptimestring", Utils.getTimePeriodForDisplay(comparisonTime, false, true));
            }
            put(m, "width", width);
            put(m, "height", height);
            put(m, "type", getType());
            put(m, "template", comparisonTemplate);
            return m;
        }

        private void put(Map<String, Object> m, String s, Object o) {
            if (o != null) {
                m.put(s, o);
            }
        }

        public String getCode() {
            return "screenshot";
        }
    }

//    class AreaAction extends AbstractAction {
//
//        AbstractCommandHandler h;
//
//        public AreaAction(AbstractCommandHandler h) {
//            this.h = h;
//            String label = ApplicationSupport.getString("screenshot.action.defineArea");
//            putValue(SHORT_DESCRIPTION, label);
//            putValue(NAME, label);
//        }
//
//        public void actionPerformed(ActionEvent e) {
//            fireCommandEvent(h, null, "defineScreenshotArea", this);
//        }
//    }
}
