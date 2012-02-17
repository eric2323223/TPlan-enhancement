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
package com.tplan.robot.scripting;

import com.tplan.robot.imagecomparison.ImageComparisonModule;
import com.tplan.robot.imagecomparison.search.SearchImageComparisonModule;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.remoteclient.capabilities.ClipboardTransferCapable;
import com.tplan.robot.scripting.commands.OutputObject;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import com.tplan.robot.util.ListenerMap;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Scripting context is a map which serves as a repository of objects and
 * structures related to script execution.
 *
 * @product.signature
 */
public interface ScriptingContext extends Map {

    /**
     * Get remote desktop client used for automation. It may be e.g. an RFB or
     * RDP client.
     *
     * @return remote desktop client.
     */
    RemoteDesktopClient getClient();

    /**
     * Get user configuration (preferences). It is typically a set of various
     * parameters which affect behavior of the program. Configuration is
     * usually loaded from a text or XML file or from a database.
     *
     * @return user configuration.
     */
    UserConfiguration getConfiguration();

    /**
     * Get script handler associated with this context. It is an object which
     * handles execution of scripting language commands and structured
     * blocks.
     *
     * @return script handler.
     */
    ScriptManager getScriptManager();

    /**
     * Get the map of script parameters. They are passed to the program usually
     * through the command line. Typical script parameters are remote host name and port
     * to connect to, password for authentication and name of the script file
     * to be executed. See the CLI Options documentation for complete
     * reference of supported CLI options.
     *
     * @return script parameters.
     */
    Map getScriptParams();


    // GET VALUES OF MAJOR IMPLICIT VARIABLES --------------

    /**
     * <p>Set a context variable.</p>
     *
     * <p>The context stores a map of
     * variables ([variable_name, variable_alue] pairs) under the {@link #CONTEXT_VARIABLE_MAP} key.
     * While the {@doc.cmd var} and {@doc.cmd eval} commands create entries
     * in this map, a preprocessor of the proprietary test script interpret
     * {@link ProprietaryTestScriptInterpret} makes sure that any variable reference
     * in form of <code>{variable_name}</code> in a test script command line
     * gets replaced with the variable value before further processing.</p>
     *
     * <p>The variable map is available through the {@link #getVariables()} method.
     * As it is a {@link ListenerMap} instance, anyone may
     * register for variable change events through the {@link ListenerMap#addPropertyChangeListener(java.beans.PropertyChangeListener)}
     * listener interface.</p>
     *
     * @param variableName a variable name (case sensitive).
     * @param value variable value, usually a String (created through <code>Var</code>)
     * or a Number (typically defined by <code>Eval</code>) instance.
     */
    void setVariable(String variableName, Object value);

    /**
     * <p>Get value of a context variable.</p>
     *
     * <p>The context stores a map of
     * variables ([variable_name, variable_alue] pairs) under the {@link #CONTEXT_VARIABLE_MAP} key.
     * While the {@doc.cmd var} and {@doc.cmd eval} commands create entries
     * in this map, a preprocessor of the proprietary test script interpret
     * {@link ProprietaryTestScriptInterpret} makes sure that any variable reference
     * in form of <code>{variable_name}</code> in a test script command line
     * gets replaced with the variable value before further processing.</p>
     *
     * <p>The variable map is available through the {@link #getVariables()} method.
     * As it is a {@link ListenerMap} instance, anyone may
     * register for variable change events through the {@link ListenerMap#addPropertyChangeListener(java.beans.PropertyChangeListener)}
     * listener interface.</p>
     *
     * @param variableName a case sensitive variable name to retrieve the value of.
     * @return value of the specified variable, usually a String (created through <code>Var</code>)
     * or a Number (typically defined by <code>Eval</code>) instance. The method returns
     * null if the variable doesn't exist.
     */
    Object getVariable(String variableName);

    /**
     * <p>Get map of variables associated with this context.</p>
     *
     * <p>The context stores a map of
     * variables ([variable_name, variable_alue] pairs) under the {@link #CONTEXT_VARIABLE_MAP} key.
     * While the {@doc.cmd var} and {@doc.cmd eval} commands create entries
     * in this map, a preprocessor of the proprietary test script interpret
     * {@link ProprietaryTestScriptInterpret} makes sure that any variable reference
     * in form of <code>{variable_name}</code> in a test script command line
     * gets replaced with the variable value before further processing.</p>
     *
     * <p>The variable map is available through the {@link #getVariables()} method.
     * As it is a {@link ListenerMap} instance, anyone may
     * register for variable change events through the {@link ListenerMap#addPropertyChangeListener(java.beans.PropertyChangeListener)}
     * listener interface.</p>
     *
     * @return variable map. If the map doesn't exist, the method creates a new one and
     * inserts it into the context map. It never returns null.
     */
    ListenerMap<String, Object> getVariables();

    /**
     * Get exit code of the last executed command.
     * @return exit code of the last executed command.
     */
    int getExitCode();

    /**
     * <p>Get the output directory. It is a folder where output objects such as
     * screenshots, files and reports should be saved to unless the
     * test script code specifies otherwise (for example through specifying
     * an output object with an absolute path pointing somewhere else).</p>
     *
     * <p>Default value returned by the method is the user home folder obtained from the JVM system
     * properties as <code>System.getProperty("user.dir")</code>. This default
     * value may be overriden through the <code>scripting.defaultOutputPath</code>
     * user preference. See the {@link UserConfiguration} class for more.</p>
     *
     * <p>The output path may be customized for a particular test script programatically
     * through the {@link #setOutputDir(java.io.File)}
     * method or through setting of the <code>_REPORT_DIR</code> context variable in a test script.
     * The new path is however not applied to the objects which have been created
     * before the change. To make sure that all output goes to one custom directory set the
     * output path at the very beginning of your test script.</p>
     *
     * <p>As the method is just a convenience method which returns value of the
     * <code>_REPORT_PATH</code> context variable, objects interested in receiving
     * events about the path update may take advantage of the {@link ListenerMap}
     * interface of the {@link #getVariables() map of context variables} to register
     * for the variable changes.
     *
     * @return path to save test script execution output objects to.
     */
    File getOutputDir();

    /**
     * <p>Set the output directory. It is a folder where output objects such as
     * screenshots, files and reports should be saved to unless the
     * test script code specifies otherwise (for example through specifying
     * an output object with an absolute path pointing somewhere else).</p>
     *
     * <p>This is just a convenience method which sets the <code>_REPORT_DIR</code>
     * context variable through the {@link #setVariable(java.lang.String, java.lang.Object)} method.
     * A null value will reset the path to the default value described in the {@link #getOutputDir()}
     * method.</p>
     *
     * <p>Be aware that the new path is not applied to the objects which have been created
     * before the change. To make sure that all output goes to one custom directory set the
     * output path at the very beginning of your test script.</p>
     *
     * @param directory a new output path or null to reset the directory to the default value.
     */
    void setOutputDir(File directory);

    /**
     * <p>Get the template directory. It is a folder where commands performing
     * image comparison will look for template images unless the command parameter specifies
     * the template with a full path.</p>
     *
     * <p>Default value returned by the method is the user home folder obtained from the JVM system
     * properties as <code>System.getProperty("user.dir")</code>. This default
     * value may be overriden through the <code>scripting.defaultTemplatePath</code>
     * user preference. See the {@link UserConfiguration} class.</p>
     *
     * <p>The template path may be customized for a particular test script programatically
     * through the {@link #setTemplateDir(java.io.File)}
     * method or through setting of the <code>_TEMPLATE_DIR</code> context variable
     * in the test script.</p>
     *
     * <p>As the method is just a convenience method which returns value of the
     * <code>_TEMPLATE_PATH</code> context variable, objects interested in receiving
     * events about the path update may take advantage of the {@link ListenerMap}
     * interface of the {@link #getVariables() map of context variables} to register
     * for the variable changes.
     *
     * @return directory to read template images from.
     */
    File getTemplateDir();

    /**
     * <p>Set the template directory. It is a folder where commands performing
     * image comparison will look for template images unless the command parameter specifies
     * the template with a full path.</p>
     *
     * <p>This is just a convenience method which sets the <code>_TEMPLATE_DIR</code>
     * context variable through the {@link #setVariable(java.lang.String, java.lang.Object)} method.
     * A null value will reset the path to the default value described in the {@link #getTemplateDir()}
     * method.</p>
     *
     * @param directory a new path to read template images from.
     */
    void setTemplateDir(File directory);

    /**
     * <p>Get the latest clipboard text received from the desktop server. This is
     * just a convenience method which returns value of the <code>_SERVER_CLIPBOARD_CONTENT</code>
     * context variable and it is equivalent to <code>(String)getVariable("_SERVER_CLIPBOARD_CONTENT")</code>.</p>
     *
     * <p>The method returns a non null method only if (1) a desktop is connected,
     * (2) both the desktop server and client support clipboard content transfer and
     * (3) the client has received which the user has cut or copied text on the desktop.
     * To find out whether the current client ({@link #getClient()}) supports clipboard
     * transfers at all check whether it implements the {@link ClipboardTransferCapable} capability.</p>
     *
     * <p>For example, an RFB (VNC) server does support clipboard transfer provided
     * that either the <code>vncconfig</code> or other clipboard
     * handling utility (<a href="http://www.nongnu.org/autocutsel">autocutsel</a>, ...) is
     * running on the server. When user copies/cuts a text on the desktop using
     * Ctrl+C or Ctrl+X, it gets transferred to the client which is expected to update the
     * <code>_SERVER_CLIPBOARD_CONTENT</code> context variable.</p>
     *
     * <p>To wait for a clipboard change in a test script use the {@doc.cmd waitfor_clipboard} command.
     * To register programatically as a listener for clipboard changes either use the
     * {@link RemoteDesktopClient#addServerListener(com.tplan.robot.remoteclient.RemoteDesktopServerListener)}
     * interface and check for the {@link RemoteDesktopServerEvent#SERVER_CLIPBOARD_EVENT} event type, or
     * register as a listener to the variable map (see {@link #getVariables()} description).
     * The difference is that while one test script uses the same context during
     * the whole script execution, the client may change as with the connect and
     * disconnect command calls of the test script code.</p>
     *
     * @return desktop server clipboard content or null if either the clipboard is
     * empty or the protocol used doesn't support clipboard transfers.
     */
    String getServerClipboardContent();

    /**
     * <p>Get coordinates of template image occurences resulting from image comparison
     * performed through the <code>"search"</code> method. This list is populated by
     * the built-in image search plugin {@link SearchImageComparisonModule} every
     * time its {@link ImageComparisonModule#compare(java.awt.Image, java.awt.Rectangle, java.awt.Image, java.lang.String, com.tplan.robot.scripting.ScriptingContext, float) compare()} or
     * {@link ImageComparisonModule#compareToBaseImage(java.awt.Image, java.awt.Rectangle, java.lang.String, com.tplan.robot.scripting.ScriptingContext, float) compareToBaseImage()} method
     * gets invoked.</p>
     *
     * <p>This method belongs to convenience methods allowing to access image
     * search results on the Java API level. Image search is usually triggered
     * by one of the test script commands (methods) such as compareTo(),
     * screenshot(), waitForMatch() or waitForMismatch() from the {@link com.tplan.robot.scripting.DefaultJavaTestScript}
     * class. The algorithm accepts one or more template images on input and
     * searches desktop of the currently connected client (see {@link #getClient()})
     * for matching areas. The input images are processed one by one
     * in the order they were specified at and the search is stopped when
     * at least one match is found or the end of the image list is achieved.
     * Result of image search is primarily expressed by the calling method's
     * numeric exit code and additional information (such as match coordinates and
     * matching template details) are stored to the context in form of variables
     * (see the {@link #getVariable(java.lang.String)} method). To avoid casting
     * and conversion of variables to numeric values the context provides the
     * following set of convenience methods:
     * <ul>
     * <li>{@link #getComparisonResult()} returns in case of image search either
     * 0 (meaning "no matches found") or 100 (meaning "at least one match was found").</li>
     * <li>{@link #getSearchHits()} returns the list of match locations (left upper
     * corner of the match area on the desktop).</li>
     * <li>{@link #getSearchHitTemplateIndex()} returns index of the matching
     * template in the input list.</li>
     * <li>{@link #getSearchHitTemplateSize()} returns size (width and height)
     * of the matching template.</li>
     * </ul>
     * </p>
     *
     * @return list of coordinates representing match locations found by the last
     * executed image search. If no image search has been performed yet the method
     * returns null. If the image search result was negative and no matches were
     * produced, the method returns an empty list.
     */
    List<Point> getSearchHits();

    /**
     * Get index of the matching template image in the last image search. See
     * the {@link #getSearchHits()} method documentation for more.
     * @return index of matching template image or -1 if no image search has
     * been performed or no matches were found.
     */
    int getSearchHitTemplateIndex();

    /**
     * Get size of the matching template image in the last image search. It can
     * be used together with the list of match points to construct matching areas
     * (rectangles) on the connected desktop image. See
     * the {@link #getSearchHits()} method documentation for more.
     * @return size of matching template image or null if no image search has
     * been performed or no matches were found.
     */
    Dimension getSearchHitTemplateSize();

    /**
     * <p>Get a number representing result of the last performed image comparison
     * performed through the CompareTo or Screenshot commands. It is a number
     * between 0 and 100 representing percentage of how much the two compared
     * images matched. The value is subject to the image comparison method used;
     * image search for example returns either 0 (meaning "no matches found")
     * or 100 (meaning "at least one match was found") while the "default"
     * histogram based method returns any number between 0 and 100 reflecting
     * the percentage of matching pixels.</p>
     *
     * @return a number between 0 and 100 representing image comparison result
     * as a percentage expressing how much the two compared images match.
     */
    Number getComparisonResult();

    /**
     * Get an instance of the text parser. It is an object providing methods
     * able to parse command names, parameters and their values, and convert
     * textual representation of particular objects/values (for example, rectangles)
     * to/from Java object instances.
     *
     * @return scripting language parser.
     */
    TokenParser getParser();

    /**
     * Get a dummy event source. As Java events require a {@link java.awt.Component java.awt.Component}
     * event source in the constructor, the component returned by this method is
     * used as event source by some framework classes which use the Java event
     * system but are not Component instances.
     * @return
     */
    Component getEventSource();

    /**
     * Get the master test wrapper. It is a top level envelope around a compiled
     * or executed test script and maintains basic information about the test
     * script source and owner.
     * @return top level master wrapper which is usually either a
     * {@link com.tplan.robot.scripting.wrappers.ScriptWrapper} instance (when the
     * test script is a proprietary one) or {@link com.tplan.robot.scripting.DefaultJavaTestScript}
     * when the test script is a Java one.
     */
    TestWrapper getMasterWrapper();

    /**
     * Get the map of override variables. They are typically defined through the
     * <code>-v/--variable</code> command line parameters. They override values
     * of variables specified in scripts. See the CLI Options documentation on
     * how to override script variables.
     *
     * @return command line variable map.
     */
    Map<String, String> getCommandLineVariables();

    /**
     * Get the test script interpret associated with this context.
     * @return test script interpret.
     */
    TestScriptInterpret getInterpret();

    /**
     * Get the list of compilation errors in form of {@link SyntaxErrorException}
     * instances.
     * @return list of compilation errors.
     */
    List<SyntaxErrorException> getCompilationErrors();

    /**
     * Get the list of output objects.
     * @return list of output objects such as screenshots, warnings and logs.
     */
    List<OutputObject> getOutputObjects();

    /**
     * Get the most recent throwable resulting from an attempt to connect to
     * a desktop. If a {@doc.cmd connect} command gets executed, it saves any
     * eventual connection error to the context.
     *
     * @return connection error (exception).
     */
    Throwable getConnectError();

    /**
     * Indicate whether this context as a compilation one or execution one.
     * @return true if it is a compilation context, false if it's and execution one.
     * Default value is true.
     */
    boolean isCompilationContext();

    /**
     * Dispose (destroy) this context. The method should clear up all references which may
     * be difficult for the garbage collector to reclaim, for example variable map
     * listeners. The context should not be used any more after this method gets called.
     */
    void dispose();

    // Constants
    /**
     * Key to a repository object - script variables.
     * Script variables are defined by the Var command.
     */
    public static String CONTEXT_CLIENT = "CONTEXT_CLIENT";

    /**
     * Key to a repository object - a List of coordinates resulted from an image search
     */
    public static String CONTEXT_PARSER = "CONTEXT_PARSER";

    /**
     * Key to a repository object - script variables.
     * Script variables are defined by the Var command.
     */
    public static String CONTEXT_VARIABLE_MAP = "CONTEXT_VARIABLE_MAP";

    /**
     * Key to a repository object - map with procedure arguments.
     */
    public static String CONTEXT_PROCEDURE_ARG_MAP = "CONTEXT_PROCEDURE_ARG_MAP";


    /**
     * Key to a repository object - script handler.
     */
    public static String CONTEXT_SCRIPT_MANAGER = "CONTEXT_SCRIPT_MANAGER";

    /**
     * Key to a repository object - script variables passed via CLI arguments.
     */
    public static String CONTEXT_CLI_VARIABLE_MAP = "CONTEXT_CLI_VARIABLE_MAP";

    /**
     * Key to a repository object - list with output objects, i.e. screenshots and warnings.
     */
    public static String CONTEXT_OUTPUT_OBJECTS = "CONTEXT_OUTPUT_OBJECTS";

    /**
     * Key to a repository object - currently executed document element.
     */
    public static String CONTEXT_CURRENT_DOCUMENT_ELEMENT = "CONTEXT_CURRENT_DOCUMENT_ELEMENT";

    /**
     * Key to a repository object - last executed document element.
     */
    public static String CONTEXT_LAST_EXECUTED_DOCUMENT_ELEMENT = "CONTEXT_LAST_EXECUTED_DOCUMENT_ELEMENT";

    /**
     * Key to a repository object - list with events received from the RFB server. It may contain just RfbServerEvent
     * instances.
     */
    public static String CONTEXT_RFB_EVENT_LIST = "CONTEXT_RFB_EVENT_LIST";

    /**
     * Key to a repository object - execution start date & time.
     */
    public static String CONTEXT_EXECUTION_START_DATE = "CONTEXT_EXECUTION_START_DATE";

    /**
     * Key to a repository object - current script wrapper.
     */
    public static String CONTEXT_CURRENT_SCRIPT_WRAPPER = "CONTEXT_CURRENT_SCRIPT_WRAPPER";

    /**
     * Key to a repository object - master script wrapper.
     */
    public static String CONTEXT_MASTER_SCRIPT_WRAPPER = "CONTEXT_MASTER_SCRIPT_WRAPPER";

    /**
     * Key to a repository object - procedure table.
     */
    public static String CONTEXT_PROCEDURE_MAP = "CONTEXT_PROCEDURE_MAP";

    /**
     * Key to a repository object - a flag showing whether this is a validation repository or an execution one.
     */
    public static String CONTEXT_COMPILATION_FLAG = "CONTEXT_COMPILATION_FLAG";

    /**
     * Key to a repository object - a reason (String) describing why script
     * execution was stopped. A mere presence of this object in the context
     * should be interpreted as a request to stop execution and/or compilation.
     */
    public static String CONTEXT_STOP_REASON = "CONTEXT_STOP_REASON";

    /**
     * Key to a repository object - list of validation errors, i.e. errors in the script syntax.
     */
    public static String CONTEXT_COMPILATION_ERRORS = "CONTEXT_COMPILATION_ERRORS";

    /**
     * Key to a repository object - labels of the GoTo command
     */
    public static String CONTEXT_LABEL_MAP = "CONTEXT_LABEL_MAP";

    /**
     * Key to a repository object - user configuration
     */
    public static String CONTEXT_USER_CONFIGURATION = "CONTEXT_USER_CONFIGURATION";

    /**
     * Key to a repository object - a flag indicating whether we are in a procedure declaration or not
     */
    public static String CONTEXT_PROCEDURE_DECLARATION_FLAG = "CONTEXT_PROCEDURE_DECLARATION_FLAG";

    /**
     * Key to a repository object - labels of the GoTo command
     */
    public static String CONTEXT_GOTO_TARGET_LABEL = "CONTEXT_GOTO_TARGET_LABEL";

    /**
     * Key to a repository object - a flag indicating whether the current line of code can be executed or not
     */
    public static String CONTEXT_SELECTED_DOCUMENT_ELEMENT = "CONTEXT_SELECTED_DOCUMENT_ELEMENT";

    /**
     * Key to a repository object - a List of coordinates resulted from an image search
     */
    public static String CONTEXT_IMAGE_SEARCH_POINT_LIST = com.tplan.robot.imagecomparison.ImageComparisonModule.SEARCH_COORD_LIST;

    /**
     * Key to a repository object - duration of script execution.
     */
    public static String CONTEXT_EXECUTION_DURATION = "CONTEXT_EXECUTION_DURATION";

    /**
     * Key to a repository object - a Component instance which should be used as
     * MouseEvent and KeyEvent source
     */
    public static String CONTEXT_EVENT_SOURCE = "CONTEXT_EVENT_SOURCE";

    /**
     * Key of a context object - test script interpret.
     */
    public static String CONTEXT_INTERPRET = "CONTEXT_INTERPRET";

    /**
     * Key of a context object - Throwable (exception) from a failed attempt to connect to a desktop.
     */
    public static String CONTEXT_CONNECT_THROWABLE = "CONTEXT_CONNECT_THROWABLE";

    /**
     * Key to a repository object - debug flag. If this object is present in
     * the repository, script execution/validation debugging mechanisms are
     * activated. Save for debugging this mode is also used by the Java script
     * converter.
     */
    public static String CONTEXT_DEBUG_MODE_FLAG = "CONTEXT_DEBUG_MODE_FLAG";

    /**
     * Key of a context object - last script generated client event such as MouseEvent or KeyEvent (since 2.3).
     */
    public static final String CONTEXT_LAST_GENERATED_CLIENT_EVENT = "CONTEXT_LAST_GENERATED_CLIENT_EVENT";

    public static final String CONTEXT_REPORT_ELEMENT = "CONTEXT_REPORT_ELEMENT";
    public static final String CONTEXT_REPORT_ELEMENT_LIST = "CONTEXT_REPORT_ELEMENT_LIST";
    public static final String CONTEXT_TEMPLATE_PATH_ELEMENT = "CONTEXT_TEMPLATE_PATH_ELEMENT";
    public static final String CONTEXT_OUTPUT_PATH_ELEMENT = "CONTEXT_OUTPUT_PATH_ELEMENT";

    public static final String IMPLICIT_VARIABLE_FILE_NAME = "_FILE";
    public static final String IMPLICIT_VARIABLE_FILE_NAME_SHORT = "_FILENAME";
    public static final String IMPLICIT_VARIABLE_TIMESTAMP = "_TIME";
    public static final String IMPLICIT_VARIABLE_DATESTAMP = "_DATE";
    public static final String IMPLICIT_VARIABLE_MACHINE_NAME = "_MACHINE";

    public static final String IMPLICIT_VARIABLE_PORT = "_PORT";
    public static final String IMPLICIT_VARIABLE_PROTOCOL = "_PROTOCOL";
    public static final String IMPLICIT_VARIABLE_URL = "_URL";

    public static final String IMPLICIT_VARIABLE_DISPLAY = "_DISPLAY";
    public static final String IMPLICIT_VARIABLE_REPORT_DIR = "_REPORT_DIR";
    public static final String IMPLICIT_VARIABLE_TEMPLATE_DIR = "_TEMPLATE_DIR";
    public static final String IMPLICIT_VARIABLE_SCRIPT_DIR = "_SCRIPT_DIR";
    public static final String IMPLICIT_VARIABLE_EXIT_CODE = "_EXIT_CODE";
    public static final String IMPLICIT_VARIABLE_DESKTOP_WIDTH = "_DESKTOP_WIDTH";
    public static final String IMPLICIT_VARIABLE_DESKTOP_HEIGHT = "_DESKTOP_HEIGHT";
    public static final String IMPLICIT_VARIABLE_WARNING_COUNT = "_WARNING_COUNT";
    public static final String IMPLICIT_VARIABLE_SERVER_CLIPBOARD_CONTENT = "_SERVER_CLIPBOARD_CONTENT";
    public static final String IMPLICIT_VARIABLE_CURTIME = "_CURTIME";
    public static final String IMPLICIT_VARIABLE_CURDATE = "_CURDATE";
    public static final String IMPLICIT_VARIABLE_CURDATE_FORMAT = "_CURDATE_FORMAT";
    public static final String IMPLICIT_VARIABLE_MOUSE_X = "_MOUSE_X";
    public static final String IMPLICIT_VARIABLE_MOUSE_Y = "_MOUSE_Y";

    // New to 2.0.3
    public static final String IMPLICIT_VARIABLE_RANDOM = "_RANDOM";
    public static final String IMPLICIT_VARIABLE_RANDOM_MIN = "_RANDOM_MIN";
    public static final String IMPLICIT_VARIABLE_RANDOM_MAX = "_RANDOM_MAX";
    public static final String IMPLICIT_VARIABLE_RGB = "_RGB";
    public static final String IMPLICIT_VARIABLE_RGB_X = "_RGB_X";
    public static final String IMPLICIT_VARIABLE_RGB_Y = "_RGB_Y";
    public static final String IMPLICIT_VARIABLE_PROCEDURE_ARGUMENT_COUNT = "_PROCEDURE_ARG_COUNT";

    // New to 2.0Beta - generic data regarding the product, version etc.
    public static final String IMPLICIT_VARIABLE_PRODUCT_VERSION_SHORT = "_PRODUCT_VERSION_SHORT";
    public static final String IMPLICIT_VARIABLE_PRODUCT_VERSION_LONG = "_PRODUCT_VERSION_LONG";
    public static final String IMPLICIT_VARIABLE_PRODUCT_NAME = "_PRODUCT_NAME";
    public static final String IMPLICIT_VARIABLE_PRODUCT_HOME_PAGE = "_PRODUCT_HOME_PAGE";
    public static final String IMPLICIT_VARIABLE_PRODUCT_INSTALL_DIR = "_PRODUCT_INSTALL_DIR";

    public static final String WAITUNTIL_X = "_X";
    public static final String WAITUNTIL_Y = "_Y";
    public static final String WAITUNTIL_W = "_W";
    public static final String WAITUNTIL_H = "_H";
    public static final String WAITUNTIL_TIMEOUT = "_TIMEOUT";

    public static final String REPORT_REPORT_FILE = "_REPORT_FILE";
    public static final String REPORT_REPORT_FILENAME = "_REPORT_FILENAME";
    public static final String REPORT_REPORT_DESC = "_REPORT_DESC";
    public static final String REPORT_STATUS_IMAGE_NAME = "_REPORT_STATUS_IMAGE_NAME";

    public static final String COMPARETO_RESULT = "_COMPARETO_RESULT";
    public static final String COMPARETO_PASS_RATE = "_COMPARETO_PASS_RATE";
    public static final String COMPARETO_TEMPLATE = "_COMPARETO_TEMPLATE";
    public static final String COMPARETO_TIME_IN_MS = "_COMPARETO_TIME_IN_MS";
    public static final String COMPARETO_TEMPLATE_INDEX = "_COMPARETO_TEMPLATE_INDEX";
    public static final String COMPARETO_TEMPLATE_WIDTH = "_COMPARETO_TEMPLATE_WIDTH";
    public static final String COMPARETO_TEMPLATE_HEIGHT = "_COMPARETO_TEMPLATE_HEIGHT";
}
