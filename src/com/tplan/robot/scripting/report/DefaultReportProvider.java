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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.plugin.DependencyMissingException;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.preferences.*;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.plugin.PluginDependency;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.*;
import com.tplan.robot.scripting.TestWrapper;
import com.tplan.robot.scripting.commands.CommandEvent;
import com.tplan.robot.scripting.commands.CommandFactory;
import com.tplan.robot.scripting.commands.CommandHandler;
import com.tplan.robot.scripting.commands.CommandListener;
import com.tplan.robot.scripting.commands.impl.ReportCommand;
import com.tplan.robot.scripting.commands.impl.ScreenshotCommand;
import com.tplan.robot.scripting.commands.impl.WarningCommand;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.util.Utils;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.ImageIcon;
import javax.swing.Timer;
import static com.tplan.robot.scripting.ScriptingContext.*;

/**
 * Default report provider generates an HTML report from script execution. It is
 * compatible with the functionality provided by previous product versions.
 * @product.signature
 */
public class DefaultReportProvider implements ActionListener, ConfigurationChangeListener,
        Configurable, ScriptListener, CommandListener, Plugin, ReportProvider {

    static final String VAR_VNCROBOT_VERSION = "VAR_VNCROBOT_VERSION";
    static final String VAR_SERVER_NAME = "VAR_SERVER_NAME";
    static final String VAR_SCRIPT_NAME = "VAR_SCRIPT_NAME";
    static final String VAR_SCRIPT_LINE = "VAR_SCRIPT_LINE";
    static final String VAR_STATUS = "VAR_STATUS";
    static final String VAR_DATE = "VAR_DATE";
    static final String VAR_REPORT_DESCRIPTION = "VAR_REPORT_DESCRIPTION";
    static final String VAR_IMAGE_COMPARISON_TABLE = "VAR_IMAGE_COMPARISON_TABLE";
    static final String VAR_IMAGE_NUMBER = "VAR_IMAGE_NUMBER";
    static final String VAR_IMAGE_NAME = "VAR_IMAGE_NAME";
    static final String VAR_IMAGE_DESCRIPTION = "VAR_IMAGE_DESCRIPTION";
    static final String VAR_IMAGE_HEIGHT = "VAR_IMAGE_HEIGHT";
    static final String VAR_IMAGE_WIDTH = "VAR_IMAGE_WIDTH";
    static final String VAR_IMAGE_END = "VAR_IMAGE_END";
    static final String VAR_IMAGE_START = "VAR_IMAGE_START";
    static final String VAR_IMAGE_TAG = "VAR_IMAGE_TAG";
    static final String VAR_IMAGE_SCRIPT_NAME = "VAR_IMAGE_SCRIPT_NAME";
    static final String VAR_IMAGE_ATTACHMENTS = "VAR_IMAGE_ATTACHMENTS";
    static final String VAR_IMAGE_COMPARISON_RATE = "VAR_IMAGE_COMPARISON_RATE";
    static final String VAR_IMAGE_WARNINGS = "VAR_IMAGE_WARNINGS";
    static final String VAR_IMAGE_WARNING_TABLE = "VAR_IMAGE_WARNING_TABLE";
    static final String VAR_LOGO_WIDTH = "VAR_LOGO_WIDTH";
    static final String VAR_LOGO_HEIGHT = "VAR_LOGO_HEIGHT";
    private final int BUF_SIZE = 2048;
    UserConfiguration cfg;
    int STATUS_REFRESH_PERIOD = 10;
    final String STATUS_IMAGE_NAME = "currentstate.jpg";
    boolean ATTACH_SCRIPTS = true;        // Image comparison preferences
    boolean ATTACH_TEMPLATES = true;
    boolean createComparisonTable = true;
    boolean failedOnly = false;        // Warnings preferences
    boolean createWarningTable = true;
    boolean displayWarnings = true;
    ScriptingContext repository;
    Map params;
    boolean isRunning = true;
    int exitCode = 0;
    boolean ignoreScreenshot = false;
    boolean isStopped = false;
    boolean isPaused = false;
    String pauseReason = null;
    Timer statusUpdateTimer = null;
    ScreenshotCommand screenshotCommand = new ScreenshotCommand();
    ScriptManager handler;
    TestScriptInterpret interpret;
    Map scriptCache = new HashMap();
    File reportFile;
    int failedComparisonCounter = 0;

    public DefaultReportProvider() {
    }

    public int create(ScriptingContext repository, Map params) {
        this.repository = repository;
        this.params = params;
        isRunning = true;
        handler = repository.getScriptManager();
        interpret = repository.getInterpret();
        handler.addCommandListener(this);
        handler.addScriptListener(this);

        cfg = (UserConfiguration) repository.get(CONTEXT_USER_CONFIGURATION);
        try {
            STATUS_REFRESH_PERIOD = cfg.getInteger("ReportCommand.statusScreenshotDelayInSec").intValue() * 1000;
            ATTACH_SCRIPTS = cfg.getBoolean("ReportCommand.attachScripts").booleanValue();

            // Image comparison preferences
            ATTACH_TEMPLATES = cfg.getBoolean("ReportCommand.attachTemplates").booleanValue();
            createComparisonTable = cfg.getBoolean("ReportCommand.createComparisonTable").booleanValue();
            failedOnly = cfg.getBoolean("ReportCommand.failedComparisonsOnly").booleanValue();

            // Warnings preferences
            createWarningTable = cfg.getBoolean("ReportCommand.createWarningTable").booleanValue();
            displayWarnings = cfg.getBoolean("ReportCommand.displayWarnings").booleanValue();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String name = handler.assembleFileName((String) params.get(ReportCommand.PARAM_FILENAME), repository, IMPLICIT_VARIABLE_REPORT_DIR);
        reportFile = new File(name);
        if (reportFile.getParentFile() != null) {
            reportFile.getParentFile().mkdirs();
        }

        // This will disable automatic comparisons
        screenshotCommand.enableImageComparisons = false;

        statusUpdateTimer = new Timer(STATUS_REFRESH_PERIOD, this);
        statusUpdateTimer.setRepeats(true);
        statusUpdateTimer.setInitialDelay(10);
        statusUpdateTimer.start();

        cfg.addConfigurationListener(this);

        return generateReport();
    }

    /**
     * Create the report. If it already exists, it is overwritten.
     */
    protected int generateReport() {

        try {
            String outParam = System.getProperty(ScriptManager.OUTPUT_DISABLED_FLAG);
            boolean outputDisabled = outParam != null && outParam.equals("true");
            if (outputDisabled) {
                return 0;
            }
            PrintWriter wr = new PrintWriter(new FileOutputStream(reportFile));
            RemoteDesktopClient client = repository.getClient();
            ResourceBundle res = ApplicationSupport.getResourceBundle();

            BufferedReader reader;

            // Load the list of output objects (screenshots and warnings) and associate screenshots with warnings
            List v = (List) repository.get(CONTEXT_OUTPUT_OBJECTS);
            v = associateWarningsWithScreenshots(v);

            String line;

            TestWrapper wrapper = repository.getMasterWrapper();
            File f = wrapper == null ? null : wrapper.getScriptFile();
            if (f == null) {
                URI uri = repository.getInterpret().getURI();
                if (uri != null) {
                    f = new File(uri.getPath());
                }
            }
            String scriptName;
            if (f == null) {
               if (wrapper.getWrapperType() == TestWrapper.WRAPPER_TYPE_JAVA) {
                    scriptName = wrapper.getTestSource();
                } else {
                    scriptName = ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.unnamedDocument");
                }
            } else {
                scriptName = Utils.getFullPath(f);
            }

            final String host = client == null ? "Disconnected" : client.getConnectString();

            Date currentDate = new Date();
            Date startDate = (Date) repository.get(CONTEXT_EXECUTION_START_DATE);
            final String date = startDate + " - " + currentDate + " (" + Utils.getTimePeriodForDisplay(currentDate.getTime() - startDate.getTime(), true) + ")";
            final String reportDesc = params.containsKey(ReportCommand.PARAM_DESC)
                    ? (String) params.get(ReportCommand.PARAM_DESC)
                    : res.getString("reportProvider.default.defaultReportDesc");
            final String reportScope = params.containsKey(ReportCommand.PARAM_SCOPE)
                    ? (String) params.get(ReportCommand.PARAM_SCOPE)
                    : ReportCommand.SCOPE_ALL;

            String status = getStatus(isRunning, exitCode);

            reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("report_template.html")));
            String imageFragment = "";
            boolean isInImageFragment = false;
            ScreenshotCommand.ScreenshotInfo si;
            int imageCounter = 0;
            int warningsCounter = 0;
            failedComparisonCounter = 0;

            String comparisonTable = createComparisonTable ? createComparisonTable(repository, failedOnly) : "";
            String warningTable = createWarningTable ? createWarningTable(repository) : "";
            String desc = reportDesc.replaceAll("\\\\", "\\\\\\\\");
            String version = Utils.getProductNameAndVersion();

            Dimension ld = copyLogo(reportFile);

            Object o;

            while ((line = reader.readLine()) != null) {
                line = line.replaceAll(VAR_SCRIPT_NAME, scriptName.replaceAll("\\\\", "\\\\\\\\"));
                line = line.replaceAll(VAR_SERVER_NAME, host);
                line = line.replaceAll(VAR_STATUS, status);
                line = line.replaceAll(VAR_DATE, date);
                line = line.replaceAll(VAR_VNCROBOT_VERSION, version);
                line = line.replaceAll(VAR_REPORT_DESCRIPTION, desc);
                line = line.replaceAll(VAR_IMAGE_COMPARISON_TABLE, comparisonTable);
                line = line.replaceAll(VAR_IMAGE_WARNING_TABLE, warningTable);

                if (ld != null) {
                    line = line.replaceAll(VAR_LOGO_WIDTH, "" + ld.getWidth());
                    line = line.replaceAll(VAR_LOGO_HEIGHT, "" + ld.getHeight());
                }

                // Indices of the <body> and </body> tags
                int index1 = line.indexOf(VAR_IMAGE_START);
                int index2 = line.indexOf(VAR_IMAGE_END);

                if (index1 >= 0) {
                    imageFragment = line.substring(index1 + VAR_IMAGE_START.length()) + "\n";
                    wr.print(line.substring(0, index1));
                    isInImageFragment = true;
                    if (index2 < 0) {
                        continue;
                    }
                }
                if (index2 >= 0) {
                    imageFragment += line.substring(0, index2) + "\n";
                    line = line.substring(index2 + VAR_IMAGE_END.length()) + "\n";
                    isInImageFragment = false;

                    String imageInstance, imageScript, imageDesc;

                    // Now generate the image section
                    for (int i = 0; v != null && i < v.size(); i++) {
                        o = v.get(i);
                        if (o instanceof WarningCommand.WarningInfo) {
                            List w = new ArrayList();
                            w.add(o);
                            wr.print(createWarningHtml(w));
                            warningsCounter++;

                        } else if (o instanceof ScreenshotCommand.ScreenshotInfo) {
                            si = (ScreenshotCommand.ScreenshotInfo) v.get(i);
                            if (!isWithinReportScope(si, reportScope, scriptName)) {
                                continue;
                            }
                            imageCounter++;
                            imageInstance = imageFragment.replaceAll(VAR_IMAGE_HEIGHT, "" + si.height);
                            imageInstance = imageInstance.replaceAll(VAR_IMAGE_WIDTH, "" + si.width);
                            imageInstance = imageInstance.replaceAll(VAR_IMAGE_NUMBER, "" + imageCounter);
                            imageInstance = imageInstance.replaceAll(VAR_IMAGE_NAME, si.imageFile.getName());
                            imageInstance = imageInstance.replaceAll(
                                    VAR_IMAGE_DESCRIPTION, ("" + si.description).replaceAll("\\\\", "\\\\\\\\"));
                            imageInstance = imageInstance.replaceAll(VAR_SCRIPT_LINE, "" + (si.scriptLine));


                            if (wrapper instanceof DocumentWrapper) {
                                if (ATTACH_SCRIPTS) {
                                    imageScript = includeScript(new File(si.scriptName), repository, (si.scriptLine));
                                } else {
                                    imageScript = Utils.getFullPath(new File(si.scriptName));
                                }
                            } else {
                                imageScript = wrapper.getTestSource();
                            }
                            imageInstance = imageInstance.replaceAll(VAR_IMAGE_SCRIPT_NAME, "" + imageScript);
                            imageInstance = imageInstance.replaceAll(
                                    VAR_IMAGE_ATTACHMENTS, ("" + getAttachments(si.attachments)).replaceAll("\\\\", "\\\\\\\\"));

                            String compRate = "";
                            if (si.comparisonResult >= 0) {
                                boolean failed = si.comparisonPassRate > si.comparisonResult;
                                failedComparisonCounter += failed ? 1 : 0;
                                String s = failed
                                        ? res.getString("reportProvider.default.comparisonFailedFragment")
                                        : res.getString("reportProvider.default.comparisonPassedFragment");
                                compRate = MessageFormat.format(s, si.comparisonResult, si.comparisonPassRate);
                                if (ATTACH_TEMPLATES && si.comparisonTemplate != null && si.comparisonTemplate.size() > 0) {
                                    String ls = res.getString("reportProvider.default.templateImageLinkFragment");
                                    final int len = si.comparisonTemplate.size();
                                    File tf;
                                    for (int j = 0; j < len; j++) {
                                        tf = si.comparisonTemplate.get(j);
                                        String name = tf.getName();
                                        String extension = Utils.getExtension(name);
                                        name = name.substring(0, name.indexOf('.')) + "_template." + extension;

                                        // Add a template link to the end of the message
                                        ls += "<a href=\"" + name + "\">" + name + "</a>";
                                        if (j < len - 1) {
                                            ls += TokenParser.FILE_PATH_SEPARATOR;
                                        }
                                        Utils.copyFile(tf, new File(reportFile.getParentFile(), name));
                                    }
                                    compRate += ls;
                                }
                            }
                            imageInstance = imageInstance.replaceAll(VAR_IMAGE_COMPARISON_RATE, compRate);

                            // -- Bug fix 10019
                            imageDesc = si.description == null ? "" : si.description.replaceAll("<", "&lt;");
                            imageDesc = imageDesc.replaceAll(">", "&gt;");
                            // -- End of bug fix 10019

                            // Generate warnings if any
                            if (si.warnings != null && si.warnings.size() > 0) {
                                warningsCounter += si.warnings.size();
                                String ws = createWarningHtml(si.warnings).replaceAll("\\\\", "\\\\\\\\");
                                imageInstance = imageInstance.replaceAll(VAR_IMAGE_WARNINGS, ws);
                            } else {
                                imageInstance = imageInstance.replaceAll(VAR_IMAGE_WARNINGS, "");
                            }

                            // Generate the HTML image tag
                            imageInstance = imageInstance.replaceAll(VAR_IMAGE_TAG,
                                    "<img src=\"" + si.imageFile.getName() + "\"" + " alt=\"" + imageDesc + "\"" + " title=\"" + imageDesc + "\"" + " width=\"" + si.width + "\" height=\"" + si.height + "\"" + ">");
                            wr.println(imageInstance);

                            // Now if the image is not available in the same folder as the report file, copy it there
                            if (reportFile.getParent() != null && !reportFile.getParent().equals(si.imageFile.getParent())) {
                                Utils.copyFile(si.imageFile, new File(reportFile.getParentFile(), si.imageFile.getName()));
                            }
                        }
                    }
                    if (v == null || v.size() == 0) {
                        wr.println(res.getString("reportProvider.default.noScreenshots"));
                    }

                }

                if (isInImageFragment) {
                    imageFragment += line;
                } else {
                    wr.println(line);
                }
            }

            // Write the invisible values. They are generated into HTML comments and they can be parsed by
            // other tools which may want to process this report.
            wr.println("<!-- NOTE: these invisible values are intended to provide info about this report to external applications. -->");
            wr.println("<!-- version=" + ApplicationSupport.APPLICATION_BUILD + " -->");
            wr.println("<!-- running=" + isRunning + " -->");
            final boolean stopped = repository.containsKey(ScriptingContext.CONTEXT_STOP_REASON);
            wr.println("<!-- stopped=" + stopped + " -->");
            wr.println("<!-- paused=" + isPaused + " -->");
            wr.println("<!-- exitCode=" + exitCode + " -->");
            wr.println("<!-- imageCount=" + imageCounter + " -->");
            wr.println("<!-- failedComparisons=" + failedComparisonCounter + " -->");
            wr.println("<!-- warningCount=" + warningsCounter + " -->");
            wr.println("<!-- executionTimeInSec=" + (long) ((currentDate.getTime() - startDate.getTime()) / 1000) + " -->");

            wr.flush();
            wr.close();
            reader.close();

            Map variables = repository.getVariables();

            Boolean b = (Boolean) params.get(ReportCommand.PARAM_ZIP);
            if (b != null && b.booleanValue()) {
                v.add(reportFile);
                File zip = zipReport(reportFile, v, repository);
                variables.put(REPORT_REPORT_FILE, Utils.getFullPath(zip));
                variables.put(REPORT_REPORT_FILENAME, zip.getName());
            } else {
                variables.put(REPORT_REPORT_FILE, Utils.getFullPath(reportFile));
                variables.put(REPORT_REPORT_FILENAME, reportFile.getName());
            }
            repository.getScriptManager().fireScriptEvent(new ScriptEvent(this, null, repository, ScriptEvent.SCRIPT_VARIABLES_UPDATED));

        } catch (Exception ex) {
            ex.printStackTrace();
            return 1;
        }
        return 0;
    }

    private String createWarningHtml(List warnings) {
        if (warnings == null || warnings.size() == 0) {
            return "";
        }
        String s = "";
        WarningCommand.WarningInfo wi;

        if (displayWarnings) {
            String td = "<td style=\"vertical-align: top; background-color: rgb(255, 255, 255); bgcolor=\"white\">";
            s = "<table>\n";

            for (int i = 0; i < warnings.size(); i++) {
                wi = (WarningCommand.WarningInfo) warnings.get(i);
                s += "<tr>\n" + td + "<a name=" + wi.hashCode() + "><b>" + ApplicationSupport.getString("reportProvider.default.warningLabelFragment") + "</td>\n";
                s += td + wi.description + " <i>(" + wi.date + ")</i></td>\n</tr>";
            }
            s += "\n</table>\n";

        } else {
            // When the Display Warnings feature is disabled, we have to add at least the warning anchor names
            for (int i = 0; i < warnings.size(); i++) {
                wi = (WarningCommand.WarningInfo) warnings.get(i);
                s += "\n<a name=" + wi.hashCode() + ">";
            }
            s += "\n";
        }
        return s;
    }

    private String createWarningTable(Map repository) {
        List v = (List) repository.get(CONTEXT_OUTPUT_OBJECTS);

        if (v == null || v.size() == 0) {
            return "";
        }

        String td = null, s = null;
        WarningCommand.WarningInfo wi;
        Object o;
        int counter = 0;
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        for (int i = 0; v != null && i < v.size(); i++) {
            o = v.get(i);
            if (o instanceof WarningCommand.WarningInfo) {
                wi = (WarningCommand.WarningInfo) o;
                counter++;
                if (s == null) {
                    td = "<td style=\"vertical-align: top; color: rgb(255, 255, 255); background-color: rgb(153, 153, 153); bgcolor=\"white\">";
                    s = res.getString("reportProvider.default.warningTable.titleFragment") +
                            "<table cellpadding=\"2\" cellspacing=\"1\" border=\"0\" bgcolor=\"black\" style=\"text-align: left;\">" +
                            "<tbody><tr>" +
                            td + " " + res.getString("reportProvider.default.warningTable.numberColumn") + " </td>" +
                            td + " " + res.getString("reportProvider.default.warningTable.imageColumn") + " </td>" +
                            td + " " + res.getString("reportProvider.default.warningTable.descColumn") + " </td>" +
                            "</tr>";
                    td = "<td bgcolor=\"white\">";
                }
                s += "<tr>\n" +
                        td + "<center><a href=#" + wi.hashCode() + ">" + counter + ".</a></center></td>";
                if (wi.associatedImage != null) {
                    String name = new File(wi.associatedImage).getName();
                    s += td + "<a href=#" + name + ">" + name + "</a></td>";
                } else {
                    s += td + "</td>";
                }
                s += td + wi.description + "</td>\n</tr>";
            }
        }

        return (s == null ? "" : s + "</tbody></table><br>");
    }

    private String createComparisonTable(Map repository, boolean onlyFailed) {
        List v = (List) repository.get(CONTEXT_OUTPUT_OBJECTS);
        String td = null, s = null, res, template, extension, copyName;
        ScreenshotCommand.ScreenshotInfo si;
        Object o;
        int imageCounter = 0;
        ResourceBundle r = ApplicationSupport.getResourceBundle();
        for (int i = 0; v != null && i < v.size(); i++) {
            o = v.get(i);
            if (o instanceof ScreenshotCommand.ScreenshotInfo) {
                si = (ScreenshotCommand.ScreenshotInfo) o;
                imageCounter++;

                // Positive result indicates that comparison has been performed
                if (si.comparisonResult >= 0) {
                    boolean failed = si.comparisonPassRate > si.comparisonResult;

                    if (!onlyFailed || failed) {
                        if (s == null) {
                            String title = onlyFailed
                                    ? r.getString("reportProvider.default.comparisonTableFailedOnly")
                                    : r.getString("reportProvider.default.comparisonTableAll");
                            td = "<td style=\"vertical-align: top; color: rgb(255, 255, 255); background-color: rgb(153, 153, 153); bgcolor=\"white\">";
                            s = title +
                                    "\n<table cellpadding=\"2\" cellspacing=\"1\" border=\"0\" bgcolor=\"black\" style=\"text-align: left;\">\n" +
                                    "<tbody>\n<tr>\n" +
                                    td + " " + r.getString("reportProvider.default.comparisonTableNumber") + " </td>\n" +
                                    td + " " + r.getString("reportProvider.default.comparisonTableResult") + " </td>\n" +
                                    td + " " + r.getString("reportProvider.default.comparisonTableImage") + " </td>\n" +
                                    td + " " + r.getString("reportProvider.default.comparisonTableTemplate") + " </td>\n" +
                                    "</tr>";
                            td = "<td bgcolor=\"white\">";
                        }
                        String t = failed
                                ? r.getString("reportProvider.default.comparisonFailedResult")
                                : r.getString("reportProvider.default.comparisonPassedResult");
                        res = MessageFormat.format(t, si.comparisonResult, si.comparisonPassRate);

                        if (si.comparisonTemplate != null) {
                            template = "";
                            if (ATTACH_TEMPLATES) {
                                String nm;
                                File tf;
                                int length = si.comparisonTemplate.size();
                                for (int j = 0; j < length; j++) {
                                    tf = si.comparisonTemplate.get(j);
                                    extension = Utils.getExtension(tf);
                                    nm = tf.getName();
                                    copyName = nm.substring(0, nm.lastIndexOf('.')) + "_template." + extension;

                                    // Add a template link to the end of the message
                                    template += "<a href=" + copyName + ">" + nm + "</a>";
                                    if (j < length - 1) {
                                        template += TokenParser.FILE_PATH_SEPARATOR;
                                    }
                                }
                            }
                        } else {
                            template = r.getString("reportProvider.default.comparisonTableNotFound");
                        }
                        s += "<tr>\n" +
                                td + "<center><a href=#" + si.imageFile.getName() + ">" + imageCounter + ".</a></center></td>\n" +
                                td + res + "</td>\n" +
                                td + "<a href=" + si.imageFile.getName() + ">" + si.imageFile.getName() + "</a></td>\n" +
                                td + template + "</td\n>" +
                                "</tr>\n";
                    }
                }
            }
        }
        return (s == null ? "" : s + "\n</tbody></table><br>");
    }

    private boolean isWithinReportScope(ScreenshotCommand.ScreenshotInfo si, String reportScope, String scriptName) {
        // If the scope is 'all', we include all images
        if (reportScope.equalsIgnoreCase(ReportCommand.SCOPE_ALL)) {
            return true;
        } else if (reportScope.equalsIgnoreCase(ReportCommand.SCOPE_FILE)) {
//                System.out.println("Image "+si.imageFile+", master file="+si.masterScript);
            return scriptName.equals(si.masterScript);
        }
        return false;
    }

    /**
     * Create the status string to be reported.
     *
     * @param isRunning a flag showing whether the script is running or if the execution has been finished.
     * @param exitCode  exit code (used only if the script has finished to report the execution result).
     * @return a string describing the execution status (or result) to be elaborated into the report.
     */
    private String getStatus(boolean isRunning, int exitCode) {
        String status;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        if (isRunning) {
            String s = isPaused ? res.getString("reportProvider.default.statusPaused") : res.getString("reportProvider.default.statusRunning");
            if (isPaused && pauseReason != null) {
                s += " (" + pauseReason + ")";
            }
            String t = res.getString("reportProvider.default.statusFragment");
            status = MessageFormat.format(t, s, STATUS_IMAGE_NAME, Utils.getTimePeriodForDisplay(STATUS_REFRESH_PERIOD, true));
        } else {
            if (isStopped) {
                String t = res.getString("reportProvider.default.statusStoppedFragment");
                status = MessageFormat.format(t, STATUS_IMAGE_NAME);
            } else {
                String t = res.getString("reportProvider.default.statusFinished");
                String result = exitCode == 0
                        ? res.getString("reportProvider.default.statusCompleted")
                        : res.getString("reportProvider.default.statusFailed");
                status = MessageFormat.format(t, result, exitCode, STATUS_IMAGE_NAME);
            }
        }
        return status;
    }

    private String includeScript(File scriptFile, ScriptingContext repository, int lineIndex) {
        String link = ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.unnamedDocument");
        if (scriptFile != null) {

            if (scriptCache.containsKey(scriptFile)) {
                link = (String) scriptCache.get(scriptFile);
                link = link.replaceAll("__line_index", "line" + lineIndex);
                return link;
            }

            String scriptName = scriptFile.getName() + ".html";
            ScriptManager handler = repository.getScriptManager();
            File outFile = new File(handler.assembleFileName(scriptName, repository, IMPLICIT_VARIABLE_REPORT_DIR));
            String scriptPath = Utils.getFullPath(scriptFile);

            try {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(scriptFile));
                } catch (FileNotFoundException ex) {
                    return ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.unnamedDocument");
                }

                PrintWriter wr = new PrintWriter(new FileOutputStream(outFile));
                wr.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" + "<html>\n<head>\n<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\">\n" + "\n<title>" + MessageFormat.format(ApplicationSupport.getString("reportProvider.default.reportTitle"), ApplicationSupport.APPLICATION_NAME, scriptPath) + "\n</title>\n</head>\n<body>");

                String line;
                int counter = 1;
                String spaces = "";
                while ((line = reader.readLine()) != null) {
                    spaces = "";
                    while (line.startsWith(" ")) {
                        line = line.substring(1);
                        spaces += "&nbsp;";
                    }

                    // -- Bug fix 10019
                    line = line.replaceAll("<", "&lt;");
                    line = line.replaceAll(">", "&gt;");
                    // -- End of bug fix 10019

                    line = "<a name=line" + counter + "><font size=-1>" + counter + "</font><code>   " + spaces + line.trim() + "</code><br>";
                    counter++;
                    wr.println(line);
                }

                wr.println("\n</body>\n</html>");
                wr.flush();
                wr.close();
                reader.close();

                link = "<a href=\"" + scriptName + "#__line_index\">" + scriptPath + "</a>";
                scriptCache.put(scriptFile, link);
                link = link.replaceAll("__line_index", "line" + lineIndex);

            } catch (Exception e) {
                e.printStackTrace();
                link = scriptPath;
                scriptCache.put(scriptFile, link);
            }
        }
        return link;
    }

    private String getAttachments(String attachments) {
        String text = "";
        if (attachments != null && !attachments.trim().equals("")) {
            text += ApplicationSupport.getString("reportProvider.default.attachments") + " ";
            String tokens[] = attachments.split(";");
            for (int i = 0; i < tokens.length; i++) {
                text += "<a href=\"" + tokens[i] + "\">" + tokens[i] + "</a>";
                if (i < tokens.length - 1) {
                    text += ", ";
                }
            }
        }
        return text;
    }

    private Dimension copyLogo(File reportFile) throws IOException {
        final String logoFileName = reportFile.getParent() + File.separator + "vncrlogo.png";
        File fout = new File(logoFileName);
        if (!fout.exists()) {
            try {
                InputStream is = ApplicationSupport.getImageAsStream("logo_small.png");
                Utils.copy(is, fout);
            } catch (IOException e) {
                return null;
            }
        }

        ImageIcon ic = ApplicationSupport.getImageIcon("logo_small.png");
        return new Dimension(ic.getIconWidth(), ic.getIconHeight());
    }

    private File zipReport(File reportFile, List imageList, Map repository) throws IOException {
        String name = reportFile.getAbsolutePath();
        String ext = Utils.getExtension(name);
        if (ext != null) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        name += ".zip";
        File zipFile = new File(name);
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

        Map variables = (Map) repository.get(CONTEXT_VARIABLE_MAP);
        zos.setComment("Script = " + variables.get(IMPLICIT_VARIABLE_FILE_NAME) + ", execution date & time = " + variables.get(IMPLICIT_VARIABLE_DATESTAMP) + variables.get(IMPLICIT_VARIABLE_TIMESTAMP));
        File f = null;
        ZipEntry entry;
        byte buf[] = new byte[BUF_SIZE];

        for (int i = 0; i < imageList.size(); i++) {
            if (imageList.get(i) instanceof File) {
                f = (File) imageList.get(i);
            } else if (imageList.get(i) instanceof ScreenshotCommand.ScreenshotInfo) {
                ScreenshotCommand.ScreenshotInfo si = (ScreenshotCommand.ScreenshotInfo) imageList.get(i);
                f = si.imageFile;
            }

            if (f != null) {
                entry = new ZipEntry(f.getName());
                zos.putNextEntry(entry);
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f), BUF_SIZE);
                // Read data from the source file and write it out to the zip file
                int count;
                while ((count = bis.read(buf, 0, BUF_SIZE)) != -1) {
                    zos.write(buf, 0, count);
                }
                zos.closeEntry();
                zos.flush();
                bis.close();
            }
        }
        zos.flush();
        zos.finish();
        zos.close();
        return zipFile;
    }

    public void commandEvent(CommandEvent e) {
        String name = e.getActionCode();

        // If a screenshot gets generated, create a new report
        if (name != null && name.equals(CommandEvent.OUTPUT_CHANGED_EVENT)) {
            generateReport();
        }
    }

    public void scriptEvent(ScriptEvent event) {
        switch (event.getType()) {
            case ScriptEvent.SCRIPT_EXECUTION_STOPPED:
                isStopped = true;
                isPaused = false;
                // We do not have to regenerate the report because
                // we will also receive the execution finished event
                break;
            case ScriptEvent.SCRIPT_EXECUTION_FINISHED:
                exitCode = event.getContext().getExitCode();
                isRunning = false;
                isPaused = false;
                if (statusUpdateTimer != null && statusUpdateTimer.isRunning()) {
                    statusUpdateTimer.stop();
                }
                takeStatusScreenshot(STATUS_IMAGE_NAME, reportFile);
                generateReport();
                if (handler.isConsoleMode()) {
                    String outParam = System.getProperty(ScriptManager.OUTPUT_DISABLED_FLAG);
                    boolean outputDisabled = outParam != null && outParam.equals("true");
                    if (!outputDisabled) {
                        System.out.println(MessageFormat.format(ApplicationSupport.getString("reportProvider.default.cliMsgReportAvailable"), Utils.getFullPath(reportFile)));
                    } else {
                        System.out.println(ApplicationSupport.getString("reportProvider.default.cliMsgReportOff"));
                    }
                }

                handler.removeScriptListener(this);
                break;
            case ScriptEvent.SCRIPT_EXECUTION_PAUSED:
            case ScriptEvent.SCRIPT_EXECUTION_RESUMED:
                isPaused = event.getType() == ScriptEvent.SCRIPT_EXECUTION_PAUSED;
                pauseReason = (String) event.getCustomObject();
                generateReport();
                break;
            case ScriptEvent.SCRIPT_PAUSE_FLAG_CHANGED:
                isPaused = event.getContext().getInterpret().isPause();
                pauseReason = null;
                generateReport();
                break;
        }
    }

    /**
     * <p>Implementation of the ActionListener interface.
     * <p/>
     * <p>This method is at present used just by the state update timer which invokes this method at regular
     * intervals. The method then creates a screenshot of the RFB buffer and saves it into a so called state
     * image. A link to this image is then elaborated into the report as a view of the current state.
     * <p/>
     * <p>This class uses its own instance of the ScreenshotCommand class because we don't want
     * the default ScreenshotCommand class to fire all the listeners which would make the report command to
     * re-create the HTML report and even include the state image into the report body.
     *
     * @param e an action event.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(statusUpdateTimer)) {
            takeStatusScreenshot(STATUS_IMAGE_NAME, reportFile);
        }
    }

    /**
     * <p>The method creates a screenshot of the RFB buffer and saves it into a so called state
     * image. A link to this image is then elaborated into the report as a view of the current state.
     * <p/>
     * <p>This class uses its own instance of the ScreenshotCommand class because we don't want
     * the default ScreenshotCommand class to fire all the listeners which would make the report command to
     * re-create the HTML report and even include the state image into the report body.
     *
     * @param imageName
     */
    private void takeStatusScreenshot(String imageName, File reportFile) {
        if (repository.getClient() != null && repository.getClient().isConnected()) {
            // Create a dummy repository from the current one and remove the screenshot list from there
            ScriptingContext r = new ScriptingContextImpl();
            r.putAll(this.repository);
            r.remove(CONTEXT_OUTPUT_OBJECTS);

            String imgFile = reportFile.getParent() + File.separator + imageName;

            // Take the state screenshot using the private instance of the ScreenshotCommand class
            try {
                List args = new ArrayList(3);
//                args.add("screenshot");
                args.add(imgFile);

                screenshotCommand.execute(args, new HashMap(), r);
            } catch (Exception ex) {
                String s = ApplicationSupport.getString("reportProvider.default.statusImgError");
                System.out.println(MessageFormat.format(s, imgFile, ex.getMessage()));
            }
        }
    }

    private List associateWarningsWithScreenshots(List screenshots) {
        if (screenshots == null) {
            return null;
        }
        Object o;
        ScreenshotCommand.ScreenshotInfo si;
        WarningCommand.WarningInfo wi;
        List out = new ArrayList();
        Map toRemove = new HashMap();

        for (int i = 0; i < screenshots.size(); i++) {
            o = screenshots.get(i);
            if (o instanceof ScreenshotCommand.ScreenshotInfo) {
                si = (ScreenshotCommand.ScreenshotInfo) o;
                si.warnings = new ArrayList();
                for (int j = 0; j < screenshots.size(); j++) {
                    if (screenshots.get(j) instanceof WarningCommand.WarningInfo) {
                        wi = (WarningCommand.WarningInfo) screenshots.get(j);
                        if (wi.associatedImage != null && wi.associatedImage.equals(si.shortName)) {
//                                System.out.println("associating warning [" + wi.description + "] with screenshot " + si.shortName);
                            si.warnings.add(wi);
                            toRemove.put(wi, wi);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < screenshots.size(); i++) {
            o = screenshots.get(i);
            if (!toRemove.containsKey(o)) {
                out.add(o);
//                } else {
//                    System.out.println("removing warning [" + o + "]");
            }
        }
        return out;
    }

    public void configurationChanged(ConfigurationChangeEvent evt) {
        String name = evt.getPropertyName();
        boolean refreshReport = false;
        if (name.equals("ReportCommand.statusScreenshotDelayInSec")) {
            STATUS_REFRESH_PERIOD = cfg.getInteger("ReportCommand.statusScreenshotDelayInSec").intValue() * 1000;
            refreshReport = true;
        } else if (name.equals("ReportCommand.attachScripts")) {
            ATTACH_SCRIPTS = cfg.getBoolean("ReportCommand.attachScripts").booleanValue();
            refreshReport = true;
        } else if (name.equals("ReportCommand.attachTemplates")) {
            ATTACH_TEMPLATES = cfg.getBoolean("ReportCommand.attachTemplates").booleanValue();
            refreshReport = true;
        } else if (name.equals("ReportCommand.createComparisonTable")) {
            createComparisonTable = cfg.getBoolean("ReportCommand.createComparisonTable").booleanValue();
            refreshReport = true;
        } else if (name.equals("ReportCommand.failedComparisonsOnly")) {
            failedOnly = cfg.getBoolean("ReportCommand.failedComparisonsOnly").booleanValue();
            refreshReport = true;
        } else if (name.equals("ReportCommand.createWarningTable")) {
            createWarningTable = cfg.getBoolean("ReportCommand.createWarningTable").booleanValue();
            refreshReport = true;
        } else if (name.equals("ReportCommand.displayWarnings")) {
            displayWarnings = cfg.getBoolean("ReportCommand.displayWarnings").booleanValue();
            refreshReport = true;
        }

        if (refreshReport && interpret.isExecuting()) {
            generateReport();
        }
    }

    public void setConfiguration(UserConfiguration cfg) {
    }

    public List<Preference> getPreferences() {
        List v = new ArrayList();
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        Preference o = new Preference("ReportCommand.attachScripts",
                Preference.TYPE_BOOLEAN,
                res.getString("options.defaultReportProvider.linkScripts.name"),
                res.getString("options.defaultReportProvider.linkScripts.desc"));
        o.setPreferredContainerName(res.getString("options.defaultReportProvider.group.options"));
        v.add(o);

        o = new Preference("ReportCommand.statusScreenshotDelayInSec",
                Preference.TYPE_INT,
                res.getString("options.defaultReportProvider.statusDelay.name"),
                res.getString("options.defaultReportProvider.statusDelay.desc"));
        o.setPreferredContainerName(res.getString("options.defaultReportProvider.group.status"));
        o.setMinValue(0);
        v.add(o);

        o = new Preference("ReportCommand.createWarningTable",
                Preference.TYPE_BOOLEAN,
                res.getString("options.defaultReportProvider.createWarningTable.name"),
                res.getString("options.defaultReportProvider.createWarningTable.desc"));
        o.setPreferredContainerName(res.getString("options.defaultReportProvider.group.warnings"));
        v.add(o);

        o = new Preference("ReportCommand.displayWarnings",
                Preference.TYPE_BOOLEAN,
                res.getString("options.defaultReportProvider.displayWarnings.name"),
                null);
        o.setPreferredContainerName(res.getString("options.defaultReportProvider.group.warnings"));
        v.add(o);

        o = new Preference("ReportCommand.attachTemplates",
                Preference.TYPE_BOOLEAN,
                res.getString("options.defaultReportProvider.linkTemplates.name"),
                res.getString("options.defaultReportProvider.linkTemplates.desc"));
        o.setPreferredContainerName(res.getString("options.defaultReportProvider.group.comparisons"));
        v.add(o);

        o = new Preference("ReportCommand.createComparisonTable",
                Preference.TYPE_BOOLEAN,
                res.getString("options.defaultReportProvider.createComparisonTable.name"),
                res.getString("options.defaultReportProvider.createComparisonTable.desc"));
        o.setPreferredContainerName(res.getString("options.defaultReportProvider.group.comparisons"));
        v.add(o);

        o = new Preference("ReportCommand.failedComparisonsOnly",
                Preference.TYPE_BOOLEAN,
                res.getString("options.defaultReportProvider.failedComparisonsOnly.name"),
                null);
        o.setPreferredContainerName(res.getString("options.defaultReportProvider.group.comparisons"));
        o.setDependentOption("ReportCommand.createComparisonTable");
        v.add(o);

        return v;
    }

    public String getCode() {
        return "default";
    }

    public String getDisplayName() {
        return ApplicationSupport.getString("reportProvider.default.pluginName");
    }

    public String getDescription() {
        return ApplicationSupport.getString("reportProvider.default.pluginDesc");
    }

    public String getUniqueId() {
        return "native_report_provider_default_legacy_v1.x_compatible";
    }

    public String getVendorName() {
        return ApplicationSupport.APPLICATION_NAME;
    }

    public String getSupportContact() {
        return ApplicationSupport.APPLICATION_SUPPORT_CONTACT;
    }

    public int[] getVersion() {
        return Utils.getVersion();
    }

    public Class getImplementedInterface() {
        return ReportProvider.class;
    }

    public boolean requiresRestart() {
        return true;
    }

    public String getVendorHomePage() {
        return ApplicationSupport.APPLICATION_HOME_PAGE;
    }

    public java.util.Date getDate() {
        return Utils.getReleaseDate();
    }

    public int[] getLowestSupportedVersion() {
        return Utils.getVersion();
    }

    public String getMessageBeforeInstall() {
        return null;
    }

    public String getMessageAfterInstall() {
        return null;
    }

    /**
     * Check whether all dependencies are installed. As report providers are
     * closely integrated with the Report command, the method throws a DependencyMissingException
     * if the command handler is not installed.
     *
     * @param manager plugin manager instance.
     * @throws com.tplan.robot.plugin.DependencyMissingException when one or more required dependencies is not installed.
     */
    public void checkDependencies(PluginManager manager) throws DependencyMissingException {
        if (!CommandFactory.getInstance().getAvailableCommandNames().contains("report")) {
            List<PluginDependency> l = new ArrayList();
            PluginDependency d = new PluginDependency(this, "report", CommandHandler.class, null, null);
            l.add(d);
            throw new DependencyMissingException(this, l);
        }
    }

    public String[] getSupportedFormats() {
        return new String[]{"htm", "html"};
    }
}
