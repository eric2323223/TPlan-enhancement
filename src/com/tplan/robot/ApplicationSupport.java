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
package com.tplan.robot;

import com.tplan.robot.preferences.ConfigurationChangeEvent;
import com.tplan.robot.preferences.ConfigurationChangeListener;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.images.ImageLoader;
import com.tplan.robot.remoteclient.rfb.RfbConstants;
import static com.tplan.robot.remoteclient.RemoteDesktopClient.*;
import com.tplan.robot.util.CustomEventQueue;
import com.tplan.robot.scripting.JavaTestScript;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.l10n.CustomPropertyResourceBundle;
import com.tplan.robot.l10n.LocalizationSupport;
import com.tplan.robot.util.Utils;

import java.awt.Toolkit;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.*;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * <p>Main class and entry point of the application. It is responsible
 * for initialization of global objects, localization and multithreading support, 
 * parsing of the input CLI options and startup of the program.</p>
 *
 * <p>When an
 * instance of this class is created, the following tasks are carried out:</p>
 *
 * <ul>
 * <li>Locale of the application is set to Locale.ENGLISH to prevent incorrect
 * handling of numbers and strings in non-English environments. The original locale
 * is however saved for loading of software messages later on.</li>
 *
 * <li>Resource bundle is loaded. It contains text messages used by the program. 
 * See this documentation further on for information about customization of 
 * software messages and localization support.</li>
 *
 * <li>An instance of user configuration is created (see {@link com.tplan.robot.preferences.UserConfiguration}).
 * It contains configurable program parameters. If user's home folder contains a
 * configuration file with name specified by the <code>APPLICATION_CONFIG_FILE</code>,
 * the user configuration object is loaded with the parameters from the file.</li>
 *
 * <li>An instance of image loader is created (see {@link com.tplan.robot.images.ImageLoader}).
 * It is a dummy class used to load icons and logo images bundled together with the product code.</li>
 *
 * <li>An instance of log handler (logger) is created to generate log files.</li>
 *
 * <li>The class then invokes the {@link #createAutomatedRunnable} method
 * which parses and verifies the CLI options and creates the default automation
 * thread (a {@link com.tplan.robot.AutomatedRunnableImpl} instance). The thread is then executed
 * and it starts the GUI or executes just in CLI depending on the execution mode flag passed
 * through the CLI options (default mode is GUI).</li>
 * </ul>
 *
 * <p>The class is also owner of global objects and services which are used widely across
 * all project classes. They are:
 * <ul>
 * <li>Resource bundle holding most software messages. It may be accessed from
 * other classes through the {@link #getResourceBundle()} static method.</li>
 *
 * <li>Image repository. There's a number of images (mostly icons) bundled with
 * the code and this class serves as a central point for their loading. Classes
 * wishing to load icons and logo images from the default repository should
 * use either the {@link #getImageIcon(String)} method to get an ImageIcon instance directly
 * or the {@link #getImageAsStream(String)} method to get the image file as an <code>InputStream</code>.</li>
 *
 * <li>Log service which may be accessed through static methods {@link #logSevere(String)},
 * {@link #logInfo(String)} and {@link #logFine(String)}.</li>
 * </ul>
 *
 * @product.signature
 */
public class ApplicationSupport implements ConfigurationChangeListener {

    /**
     * Product name (value: {@value}).
     */
    public static final String APPLICATION_NAME = "T-Plan Robot";
    /**
     * <p>Version identificator (value: {@value}). See the
     * {@doc.roadmap} for information on builds, releases and versioning.</p>
     *
     * <p>The variable is manually updated
     * before each release. Please avoid changing the variable name or moving
     * it to another class because it's value is parsed by the building scripts.</p>
     */
    public static final String APPLICATION_VERSION = "2.0.6";
    /**
     * <p>Build identificator (value: {@value}). See the
     * {@doc.roadmap}
     * for information on builds, releases and versioning.</p>
     *
     * <p>The variable is manually updated
     * before each release. Please avoid changing the variable name or moving
     * it to another class because it's value is parsed by the building scripts.</p>
     */
    public static final String APPLICATION_BUILD = "2.0.6-20110418.1";
    /**
     * Application home page URL (value: {@value}). It gets displayed in several places throughout
     * the application. It is also used as home page link by all internal plugins.
     */
    public static final String APPLICATION_HOME_PAGE = "http://www.vncrobot.com";
    /**
     * Application support page URL (value: {@value}). It gets displayed in several places throughout
     * the application. It is also used as support contact by all internal plugins.
     */
    public static final String APPLICATION_SUPPORT_CONTACT = "http://www.vncrobot.com/docs/contacts.html";
    /**
     * Application document path prefix (value: {@value}). It is used for construction of
     * online document URLs.
     */
    public static final String APPLICATION_DOC_DIR_NAME = "v2.0";
    /**
     * Application license file name. It must be bundled with the code in the same folder as
     * this class.
     */
    public static final String APPLICATION_LICENSE_FILE = "LICENSE.txt";
    /**
     * Prefix of resource bundle names (property files with software messages, value: {@value}). Default value is
     * "Messages". When the application starts, it looks for all property files
     * starting with this prefix, for example Messages_en.properties or Messages_de_DE.properties.
     * A valid name of resource bundle is [prefix]_[lang]_[country].properties or
     * [prefix]_[lang].properties.
     */
    public static final String APPLICATION_RESOURCE_BUNDLE_PREFIX = "Messages";
    /**
     * Configuration file path. The application looks for the file upon startup. When it exists,
     * all valid configuration values defined in this file will override the default ones.
     */
    public static final String APPLICATION_CONFIG_FILE = System.getProperty("user.home") + File.separator + "tplanrobot.cfg";
    /**
     * Legacy v1.x configuration file path. We parse it if it exists and
     * map int onto the new configuration keys.
     */
    public static final String APPLICATION_LEGACY_CONFIG_FILE = System.getProperty("user.home") + File.separator + "config.properties";
    /**
     * Log file name prefix (value: {@value}).
     */
    public static final String APPLICATION_LOG_FILE_PREFIX = "tplan_robot";
    /**
     * Base Java package name ({@value}). This variable is parsed by the build scripts
     * and used as a path prefix. If the code gets refactored and the base
     * package name is changed, update value of this variable and rebuild
     * the documentation to update the content.
     */
    public static final String APPLICATION_BASE_PACKAGE = "com.tplan.robot";
    /**
     * Name of the application JavaHelp help set file (*.hs).
     */
    public static final String APPLICATION_HELP_SET_FILE = "HelpSet.hs";

    /**
     * Root folder of the installed help files.
     */
    public static final String APPLICATION_HELP_SET_DIR = "help";
    /**
     * Log handler.
     */
    private static Logger logger = null; //Logger.getLogger("vncrobot");
    /**
     * File handler used to save log files.
     */
    private FileHandler handler = null;
    /**
     * Input CLI arguments.
     */
    private static String[] inputArguments;
    /**
     * <p>Array of application CLI options. These are the public options described in the
     * documentation.
     *
     * <p>These options are processed by the private method <code>parseParameters()</code> of this class.</p>
     * <p> Apart from the official options there's a smaller list of debug flags which can be passed through
     * CLI using the <code>-D</code> parameter of the <code>java</code> command. Each such a flag switches
     * on one hidden feature, typically a debugging output of certain module. To obtain a list of these hidden
     * flags run the application with the <code>--hidden</code> CLI option.
     */
    public static final String APPLICATION_CLI_OPTIONS[][] = new String[][]{
        {"h", "help", "cli.option.helpFormat", "cli.option.helpDesc"},
        {"c", "connect", "cli.option.connectFormat", "cli.option.connectDesc"},
        {"p", "password", "cli.option.passwordFormat", "cli.option.passwordDesc"},
        {"r", "run", "cli.option.runFormat", "cli.option.runDesc"},
        {"n", "nodisplay", "cli.option.nodisplayFormat", "cli.option.nodisplayDesc"},
        {"v", "variable", "cli.option.variableFormat", "cli.option.variableDesc"},
        {"o", "option", "cli.option.optionFormat", "cli.option.optionDesc"},
        {"l", "listen", "cli.option.listenFormat", "cli.option.listenDesc"},
        {"u", "user", "cli.option.userFormat", "cli.option.userDesc"},
        {"e", "edit", "cli.option.editFormat", "cli.option.editDesc"},
        {"", "clientparam", "cli.option.clientParamFormat", "cli.option.clientParamDesc"},
        {"", "createscript", "cli.option.createscriptFormat", "cli.option.createscriptDesc"},
        {"", "outputdir", "cli.option.outputdirFormat", "cli.option.outputdirDesc"},
        {"", "templatedir", "cli.option.templatedirFormat", "cli.option.templatedirDesc"},
        {"", "autosave", "cli.option.autosaveFormat", "cli.option.autosaveDesc"},
        {"", "fromlabel", "cli.option.fromlabelFormat", "cli.option.fromlabelDesc"},
        {"", "tolabel", "cli.option.tolabelFormat", "cli.option.tolabelDesc"},
        {"", "nooutput", "cli.option.nooutputFormat", "cli.option.nooutputDesc"},
        {"", "locale", "cli.option.localeFormat", "cli.option.localeDesc"},
        {"", "license", "cli.option.licenseFormat", "cli.option.licenseDesc"},
        {"", "nologin", "cli.option.nologinFormat", "cli.option.nologinDesc"}
    };
    /**
     * Resource bundle containing all software messages. The content is loaded
     * from file specified by the <code>VNC_GUI_PROPERTIES</code> variable. This
     * class is exclusive owner of the bundle and all other classes
     * are supposed to access it through the static method
     * <code>getResourceBundle()</code> method.
     */
    private static CustomPropertyResourceBundle resourceBundle;
    /**
     * Image loader. It is a central point for loading images bundled with
     * the product code (icons, logos etc.).
     */
    private static ImageLoader imageLoader;
    /**
     * Flag which switches on/off logging of the whole RFB communication.
     */
    private static boolean debugRfb = "true".equals(System.getProperty("vncrobot.rfb.debug"));
    /**
     * <p>Locale. As the locale is set to Locale.ENGLISH during initialization 
     * of this class, this variable saves the original locale value. It is later 
     * on used for loading of locale specific resource bundle.</p>
     */
    private final static Locale locale = Locale.getDefault();
    /**
     * Splash image.
     */
    private final static String SPLASH_IMAGE_NAME = "splash.png";

    /**
     * Constructor.
     */
    public ApplicationSupport() {
        init();
    }

    /**
     * Main application method. It creates an instance of this class, an
     * automation thread (a <code>AutomatedRunnable</code> instance) and starts it.
     *
     * @param argv array of application parameters
     * @see com.tplan.robot.AutomatedRunnable
     */
    public static void main(String[] argv) {
        try {
            showSplashIfNeeded(argv);
            inputArguments = argv;
            ApplicationSupport app = new ApplicationSupport();
            printPreamble();
            AutomatedRunnable process = app.createAutomatedRunnable("default", argv, System.out, true);
            process.run();
        } catch (MissingResourceException e) {
            e.printStackTrace();
        }
    }

    /**
     * Display the splash screen image if applicable.
     *
     * @param argv input CLI arguments. If they contain the "-n" CLI mode switch,
     * the splash screen will not be displayed.
     */
    private static void showSplashIfNeeded(String[] argv) {
        try {
            if (SPLASH_IMAGE_NAME != null) {

                // Find out whether the "-n" option is specified
                boolean cli = false;
                for (String s : argv) {
                    if (s.equals("-n")) {
                        cli = true;
                    }
                }
                if (!cli) {
                    InputStream is = getImageAsStream(SPLASH_IMAGE_NAME);
                    if (is != null) {
                        Splash.show(ImageIO.read(is));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get the locale that this program was originally started in.
     * @return program locale.
     */
    public static Locale getLocale() {
        return locale;
    }

    /**
     * Get the input CLI arguments.
     * @return CLI arguments.
     */
    public static String[] getInputArguments() {
        return inputArguments;
    }

    /**
     * Initialize and start the application.
     */
    private void init() throws MissingResourceException {

        // Initialize the resource bundle
        getResourceBundle();

        // Set the default locale to English to make sure that numbers and
        // strings are parsed and handled in English formats
        Locale.setDefault(Locale.ENGLISH);

        // Load configuration
        loadConfiguration();

        // Put the version and build numbers into the configuration file
        UserConfiguration.getInstance().setString("version", APPLICATION_VERSION);
        UserConfiguration.getInstance().setString("build", APPLICATION_BUILD);

        // Set the custom event queue
        CustomEventQueue q = new CustomEventQueue();
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(q);

//        initLogger();
    }

    /**
     * <p>Create an automated runnable prepared to execute a compiled Java test script.
     * The method can be also used to display the product GUI.</p>
     *
     * <p>Only one runnable can be executed at one moment in GUI mode. There's no limitation
     * in CLI mode and this method can be used in a Java program to create a larger
     * number of independent automated testing threads. See the {@link com.tplan.robot.AutomatedRunnable}
     * interface documentation for an example.</p>
     *
     * @param testScript a Java test script to execute in the runnable <code>run()</code> method. The argument may be null provided that
     * the thread is set to run in GUI mode (i.e. the argv[] array doesn't contain the -n/--nodisplay
     * parameter). When such a thread is started it just displays the product GUI.
     *
     * @param threadId a unique string (name) identifying the process, e.g. <code>"my automated task #1"</code>.
     * The name is currently not used in any way except to name the thread. Users may take advantage
     * of the name to identify their threads in custom Java programs through methods of
     * <code>java.lang.Thread</code> and <code>java.lang.ThreadGroup</code>.
     *
     * @param argv a string array with CLI options. The parameters are provided the same way as if the
     * product is started from command line. To obtain a complete list of available options
     * see the {@doc.cli} document and {@doc.home}
     * or run the application with the <code>--help</code> option. Options followed by a value should be specified
     * as two strings in the array, for example <code>new String[] { "--connect", "localhost:1", "--password", "welcome" }</code>.
     *
     * @param logStream a stream for logs from the test script execution. All messages printed
     * out into console in CLI mode will be written to the <code>logStream</code>. To print the logs into the console window use
     * <code>System.out</code> as value. A <code>null</code> value of this argument will suppress all log messages.
     *
     * @param exitOnFinish indicates whether the process should terminate the Java process using
     * <code>System.exit(exitCode)</code> after the automated task gets finished. Third party applications should
     * use the value of <code>false</code> to prevent termination of their Java VM.
     *
     * @return a new automated testing thread ready to be started.
     */
    public AutomatedRunnable createAutomatedRunnable(JavaTestScript testScript, String threadId, String argv[], PrintStream logStream, boolean exitOnFinish) {
        Map rfbParams = new HashMap();
        Map scriptVariables = new HashMap();
        Map options = new HashMap();
        Map guiParams = new HashMap();
        Map scriptingParams = new HashMap();

        // Parse the CLI arguments
        boolean consoleMode = parseParameters(argv, rfbParams, scriptVariables, options, guiParams, scriptingParams, testScript == null);

        AutomatedRunnable process = new AutomatedRunnableImpl(threadId, consoleMode,
                exitOnFinish, logStream, rfbParams, scriptVariables, options, guiParams, scriptingParams, testScript);
        return process;
    }

    /**
     * <p>Create an automated testing thread. If a test script in the proprietary
     * scripting language is passed among the <code>argv</code> parameters through the <code>-r/--run</code> option,
     * it will be executed.</p>
     *
     * <p>Only one thread can be created in GUI mode. There's no limitation
     * in CLI mode and this method can be used in a Java program to create a larger
     * number of independent automated testing threads. See the {@link com.tplan.robot.AutomatedRunnable}
     * interface documentation for an example.</p>
     *
     * @param threadId a unique string (name) identifying the process, e.g. <code>"my automated task #1"</code>.
     * The name is currently not used in any way except to name the thread. Users may take advantage
     * of the name to identify their threads in custom Java programs through methods of
     * <code>java.lang.Thread</code> and <code>java.lang.ThreadGroup</code>.
     *
     * @param argv a string array with CLI options. The parameters are provided the same way as if the
     * product is started from command line. To obtain a complete list of available options
     * see the <a href=http://www.vncrobot.com/docs/v1.3/gui/clioptions.html>CLI Options</a> document
     * or run the application with the <code>--help</code> option. Options followed by a value should be specified
     * as two strings in the array, for example <code>new String[] { "--connect", "localhost:1", "--password", "welcome" }</code>.
     *
     * @param logStream a stream for logs from the test script execution. All messages printed
     * out into console in CLI mode will be written to the <code>logStream</code>. To print the logs into the console window use
     * <code>System.out</code> as value. A <code>null</code> value of this argument will suppress all log messages.
     *
     * @param exitOnFinish indicates whether the process should terminate the Java process using
     * <code>System.exit(exitCode)</code> after the automated task gets finished. Third party applications should
     * use the value of <code>false</code> to prevent termination of their Java VM.
     *
     * @return a new automated testing thread ready to be started.
     */
    public AutomatedRunnable createAutomatedRunnable(String threadId, String argv[], PrintStream logStream, boolean exitOnFinish) {
        return createAutomatedRunnable(null, threadId, argv, logStream, exitOnFinish);
    }

    /**
     * Log a message into the log stream, level "SEVERE".
     * @param msg a log message.
     */
    public static void logSevere(String msg) {
        log(Level.SEVERE, msg);
    }

    /**
     * Log a message into the log stream, level "INFO".
     * @param msg a log message.
     */
    public static void logInfo(String msg) {
        log(Level.INFO, msg);
    }

    /**
     * Log a message into the log stream, level "FINE".
     * @param msg a log message.
     */
    public static void logFine(String msg) {
        log(Level.FINE, msg);
    }

    /**
     * Log a message into the log stream.
     * @param level log message level indicating severity/importance of the message.
     * @param msg a log message.
     */
    private static void log(Level level, String msg) {
        if (logger != null) {
            logger.log(level, msg);
        } else if (debugRfb) {
            System.out.println(msg);
        }
    }

    /**
     * Initialize application logger.
     */
    private void initLogger() {
        try {
            boolean enableLog = UserConfiguration.getInstance().getBoolean("logging.enableLogging").booleanValue();
            if (enableLog && handler == null && logger != null) {
                Handler handlers[] = logger.getHandlers();
                for (int i = 0; i < handlers.length; i++) {
                    logger.removeHandler(handlers[i]);
                }
                handler = new FileHandler(APPLICATION_LOG_FILE_PREFIX + "%g.log");
                handler.setFormatter(new SimpleFormatter());
                logger.addHandler(handler);
                logger.setUseParentHandlers(false);

                logger.setFilter(new LogFilter());
                String str = resourceBundle.getString("cli.loggingStartedOn");
                Object arg[] = {new Date()};
                logger.info(MessageFormat.format(str, arg));
            }
        } catch (IOException e) {
            System.out.println(resourceBundle.getString("cli.cannotCreateLogFile"));
            e.printStackTrace();
        }
    }

    /**
     * Parse CLI options which were used to start the application.
     *
     * @param argv a string array with CLI options. To obtain a complete list of available options
     * see the <a href=http://www.vncrobot.com/docs/v1.3/gui/clioptions.html>CLI Options</a> document
     * or run the application with the <code>--help</code> option.
     *
     * @param clientParams a table to store client specific parameters resulting
     * from parsing of the options, e.g. host name, port, password etc.
     *
     * @param scriptVariables a table to store script variables passed through
     * the <code>-v</code> CLI option.
     *
     * @param options a table to store overriden user options passed through
     * the <code>-o</code> CLI option.
     *
     * @param guiParams a table to store GUI specific parameters resulting
     * from parsing of the options.
     *
     * @param scriptingParams a table to store scripting specific parameters resulting
     * from parsing of the options, e.g. disabling of output (option <code>--nooutput</code>).
     */
    private boolean parseParameters(String argv[], Map clientParams, Map scriptVariables,
            Map options, Map guiParams, Map scriptingParams, boolean postValidate) {
        if (argv == null) {
            return false;
        }

        List v = new ArrayList(Arrays.asList(argv));
        String arg = null, val = null;
        int errCode = 0;
        boolean consoleMode = false;
        boolean isHost = false;
        try {
            while (v.size() > 0) {
                arg = v.get(0).toString().trim();
                if ("".equals(arg)) {
                    v.remove(0);
                    continue;
                }

                // Help required
                if (arg.equals("-h") || arg.equals("--help")) {
                    displayHelp(null, 0);

                // Connect to a desktop server
                } else if (arg.equals("-c") || arg.equals("--connect")) {
                    errCode = 1;
                    val = v.get(1).toString().trim();
                    clientParams.put(LOGIN_PARAM_URI, val);
                    isHost = true;
                    v.remove(0);
                    v.remove(0);

                // Set the user name
                } else if (arg.equals("-u") || arg.equals("--user")) {
                    errCode = 2;
                    val = v.get(1).toString().trim();
                    clientParams.put(LOGIN_PARAM_USER, val);
                    v.remove(0);
                    v.remove(0);

                // Set the password
                } else if (arg.equals("-p") || arg.equals("--password")) {
                    errCode = 2;
                    val = v.get(1).toString().trim();
                    clientParams.put(LOGIN_PARAM_PASSWORD, val);
                    v.remove(0);
                    v.remove(0);

                // Set the script name
                } else if (arg.equals("-r") || arg.equals("--run")) {
                    errCode = 3;
                    val = v.get(1).toString().trim();
                    scriptingParams.put("run", val);
                    v.remove(0);
                    v.remove(0);

                // Set the nodisplay flag
                } else if (arg.equals("-n") || arg.equals("--nodisplay")) {
                    errCode = 4;
                    consoleMode = true;
                    System.setProperty("java.awt.headless", "true");
                    v.remove(0);

                // Show license
                } else if (arg.equals("--license")) {
                    try {
                        InputStream is = ApplicationSupport.class.getResourceAsStream(ApplicationSupport.APPLICATION_LICENSE_FILE);
                        int c;
                        while ((c = is.read()) >= 0) {
                            System.out.print((char) c);
                        }
                    } catch (Exception ex) {
                        System.out.println(MessageFormat.format(getString("help.About.licenseError"),
                                APPLICATION_NAME, APPLICATION_HOME_PAGE));
                    }
                    v.remove(0);
                    System.exit(0);

                // Set the script variables
                } else if (arg.equals("-v") || arg.equals("--var")) {
                    errCode = 5;
                    val = v.get(1).toString().trim();
                    scriptVariables.put(Utils.parseParamName(val), Utils.parseParamValue(val));
                    v.remove(0);
                    v.remove(0);

                // Set the script variables
                } else if (arg.equals("--clientparam")) {
                    errCode = 30;
                    val = v.get(1).toString().trim();
                    clientParams.put(Utils.parseParamName(val), Utils.parseParamValue(val));
                    v.remove(0);
                    v.remove(0);

                // Set the options
                } else if (arg.equals("-o") || arg.equals("--opt")) {
                    errCode = 6;
                    val = v.get(1).toString().trim();
                    options.put(Utils.parseParamName(val), Utils.parseParamValue(val));
                    v.remove(0);
                    v.remove(0);

                // Set the listen mode
                } else if (arg.equals("-l") || arg.equals("--listen")) {
                    errCode = 31;
                    val = v.get(1).toString().trim();
                    int port = Utils.parseLegacyRfbPort(RfbConstants.RFB_LISTEN_PORT_OFFSET, val);
                    clientParams.put(RfbConstants.VAR_LISTEN, port);
                    clientParams.put(LOGIN_PARAM_URI, "rfb://localhost:" + port);
                    v.remove(0);
                    v.remove(0);

                } else if (arg.equals("--hidden")) {
                    errCode = 0;
                    v.remove(0);
                    System.out.println("Hidden Java params:" +
                            "\n-Ddebug.waitfor=true\n  - Switch on debug messages of the Waitfor command" +
                            "\n-Dvncrobot.rfb.debug=true\n  - Switch on debug messages of RFB communication" +
                            "\n-Dvncrobot.thread.debug=true\n  - Switch on debug messages of threads" +
                            "\n-Dvncrobot.eval.debug=true\n  - Switch on debug messages of expression evaluator" +
                            "\n-Dvncrobot.execution.debug=true\n  - Switch on debug messages of script execution" +
                            "\n-Dvncrobot.record.debug=true\n  - Recording debug mode" +
                            "\n-Dvncrobot.thread.debugKeys=true\n  - Debugging of keys pressed on the keyboard" +
                            "\n-Dvncrobot.javarobot.debug=true\n  - Debugging of Java native client" +
                            "\n-Dvncrobot.delay=<time_in_ms>\n  - Wait a specified amount of time between executing of two lines (for demo purposes)");

                    System.exit(0);

                } else if (arg.equals("-e") || arg.equals("--edit")) {
                    errCode = 26;
                    val = v.get(1).toString().trim();
                    guiParams.put("edit", val);
                    v.remove(0);
                    v.remove(0);

                } else if (arg.equals("--createscript")) {
                    errCode = 11;
                    val = v.get(1).toString().trim();
                    guiParams.put("createscript", val);
                    v.remove(0);
                    v.remove(0);

                } else if (arg.equals("--outputdir")) {
                    errCode = 12;
                    val = v.get(1).toString().trim();
                    guiParams.put("outputdir", val);
                    v.remove(0);
                    v.remove(0);

                } else if (arg.equals("--templatedir")) {
                    errCode = 13;
                    val = v.get(1).toString().trim();
                    guiParams.put("templatedir", val);
                    v.remove(0);
                    v.remove(0);

                } else if (arg.equals("--autosave")) {
                    errCode = 14;
                    guiParams.put("autosave", "true");
                    v.remove(0);

                } else if (arg.equals("--fromlabel")) {
                    errCode = 21;
                    val = v.get(1).toString().trim();
                    scriptingParams.put("fromlabel", val);
                    v.remove(0);
                    v.remove(0);

                } else if (arg.equals("--tolabel")) {
                    errCode = 22;
                    val = v.get(1).toString().trim();
                    scriptingParams.put("tolabel", val);
                    v.remove(0);
                    v.remove(0);

                } else if (arg.equals("--nooutput")) {
                    errCode = 23;
                    scriptingParams.put("nooutput", "true");
                    System.setProperty(ScriptManager.OUTPUT_DISABLED_FLAG, "true");
                    v.remove(0);

                } else if (arg.equals("--nologin")) {
                    errCode = 25;
                    guiParams.put("nologin", "true");
                    v.remove(0);

                } else if (arg.equals("--locale")) {
                    errCode = 24;
                    val = v.get(1).toString().trim();
                    String pars[] = val.split("_");
                    Locale loc;
                    if (pars.length == 3) {
                        loc = new Locale(pars[0], pars[1], pars[2]);
                    } else if (pars.length == 2) {
                        loc = new Locale(pars[0], pars[1]);
                    } else if (pars.length == 1) {
                        loc = new Locale(pars[0]);
                    } else {
                        throw new IllegalArgumentException("Locale argument is invalid.");
                    }
                    CustomPropertyResourceBundle r = LocalizationSupport.getResourceBundle(loc, ApplicationSupport.class, APPLICATION_RESOURCE_BUNDLE_PREFIX);
                    if (r != null) {
                        resourceBundle = r;
                    }
                    v.remove(0);
                    v.remove(0);

                } else {
                    errCode = 100;
                    String str = resourceBundle.getString("cli.unknownOption");
                    Object args[] = {arg};
                    displayHelp(MessageFormat.format(str, args), errCode);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            String str = resourceBundle.getString("cli.optionSyntaxError");
            Object args[] = {arg};
            displayHelp(MessageFormat.format(str, args), errCode);
        }

        // Post-parsing validation - some of the parameters require other params to be present
        if (consoleMode) {
            if (postValidate && !scriptingParams.containsKey("run")) {
                displayHelp(MessageFormat.format(resourceBundle.getString("cli.cliModeRequiresScript"), APPLICATION_NAME), 3);
            }
        }
        if (clientParams.containsKey(RfbConstants.VAR_LISTEN) && isHost) {
            displayHelp(MessageFormat.format(resourceBundle.getString("cli.cliListenAndConnectConflict"), APPLICATION_NAME), 31);
        }
        return consoleMode;
    }

    /**
     * Print an error message and help on CLI options to console and exit
     * the application with the specified exit code. This
     * method is typically used when the application is invoked with an invalid
     * CLI option.
     *
     * @param errorMsg text of the error message. It is typically provided by the
     * <code>parseParameters()</code> method which parses the CLI arguments. If
     * the argument is null, the method prints out just the help text.
     *
     * @param exitCode exit code to be returned to the system upon the application exit.
     */
    private void displayHelp(String errorMsg, int exitCode) {
        if (errorMsg != null) {
            System.out.println(errorMsg + "\n");
        }
        String str = resourceBundle.getString("cli.versionAndCopyright");
        Object args[] = {Utils.getProductNameAndVersion(), Utils.getCopyright()};
        System.out.println(MessageFormat.format(str, args));

        String[] param;
        for (int i = 0; i < APPLICATION_CLI_OPTIONS.length; i++) {
            param = APPLICATION_CLI_OPTIONS[i];
            System.out.println(resourceBundle.getString(param[2]) + "\n  " + resourceBundle.getString(param[3]) + "\n");
        }
        System.exit(exitCode);
    }

    /**
     * Implementation of the ConfigurationChangeListener interface. Whenever
     * the user configuration changes, the method reloads preferences used
     * by this class.
     *
     * @param evt a ConfigurationChangeEvent which describes the change of
     * user configuration.
     */
    public void configurationChanged(ConfigurationChangeEvent evt) {
        if (evt.getPropertyName().equals("logging.enableLogging")) {
            boolean b = ((Boolean) evt.getNewValue()).booleanValue();
            if (b) {
                initLogger();
            }
        }
    }

    public static void loadConfiguration() throws MissingResourceException {

        // Load the default configuration first
        try {
            UserConfiguration.loadDefaults(ApplicationSupport.class.getResourceAsStream("DefaultConfiguration.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new MissingResourceException(getString("cli.missingDefaultConfigFile"), "", "");
        }

        // Bug 2845420 - map old config file onto the new values
        File fnew = new File(APPLICATION_CONFIG_FILE);

        if (!fnew.exists()) {     // New config file doesn't exist yet -> look for the old one
            File fold = new File(APPLICATION_LEGACY_CONFIG_FILE);

            if (fold.exists()) {  // A legacy v1.x or 2.0Beta config file found
                try {
                    Properties p = new Properties();
                    p.load(new FileInputStream(fold));
                    String key, value;
                    UserConfiguration cfg = UserConfiguration.getInstance();
                    for (Object o : p.keySet()) {
                        key = o.toString();
                        value = p.getProperty(key);

                        // "com.vncrobot" prefix was removed from config keys
                        if (key.startsWith("com.vncrobot.")) {
                            key = key.substring("com.vncrobot.".length());
                        } else if (key.startsWith("com.vcrobot.scripting.commands.")) {  // There was a 'vcrobot' typo in one key set
                            key = key.substring("com.vcrobot.scripting.commands.".length());
                        } else if (key.startsWith("com.tplan.robot.")) {  // This is to map values saved with early Beta versions
                            key = key.substring("com.tplan.robot.".length());
                        }
                        cfg.setString(key, value);
                    }
                    UserConfiguration.saveConfiguration();
                } catch (IOException ex) {
                    System.out.println("Failed to import the old file " + fold.getAbsolutePath());
                    ex.printStackTrace();
                }
            }
        } else {
            // Load the user configuration if it exists
            try {
                UserConfiguration.load(new FileInputStream(APPLICATION_CONFIG_FILE));
            } catch (Exception e) {
                System.out.println("Failed to read config file " + fnew.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }

    /**
     * <p>Get the resource bundle. It is an object with all software messages.
     * This class is exclusive owner of the bundle and all other classes 
     * are supposed to access it through this static method.</p>
     *
     * <p>The method is null-safe meaning that it loads the resource bundle on
     * the first call.</p>
     * @return application resource bundle with all software messages.
     */
    public static CustomPropertyResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            resourceBundle = LocalizationSupport.getResourceBundle(getLocale(), ApplicationSupport.class, APPLICATION_RESOURCE_BUNDLE_PREFIX);
        }
        return resourceBundle;
    }

    /**
     * Get a string from the resource bundle. This is just a convenience
     * method calling <code>getResourceBundle().getString(key)</code>.
     * @param key a message key.
     * @return message from the resource bundle associated with the given key.
     */
    public static String getString(String key) {
        return getResourceBundle().getString(key);
    }

    /**
     * Set the resource bundle. It is an object with all software messages.
     * This class is exclusive owner of the bundle and all other classes 
     * are supposed to access it through the <code>getResourceBundle()</code> static method.
     * @param r a new resource bundle.
     */
    public static void setResourceBundle(CustomPropertyResourceBundle r) {
        resourceBundle = r;
    }

//    /**
//     * Indicates whether the application was started in CLI mode
//     * @return
//     */
//    public static boolean isCliMode() {
//        return System.getProperty(CLI_MODE_FLAG) != null;
//    }
    /**
     * Get an icon from the image repository.
     *
     * @param name image file name, for example "ok15.gif".
     * @return a new image icon.
     */
    public static ImageIcon getImageIcon(String name) {
        if (imageLoader == null) {
            imageLoader = new ImageLoader();
        }
        return new ImageIcon(imageLoader.getClass().getResource(name));
    }

    /**
     * Get input stream of an image from the image repository.
     *
     * @param name image file name, for example "ok15.gif".
     * @return input file with the image data.
     */
    public static InputStream getImageAsStream(String name) {
        if (imageLoader == null) {
            imageLoader = new ImageLoader();
        }
        return imageLoader.getClass().getResourceAsStream(name);
    }

    private static void printPreamble() {
        System.out.println(getString("cli.notice"));
    }

    /**
     * Custom log filter. It allows to skip log messages with level lower than
     * the one specified in user configuration (i.e. it allows to set the level of logging).
     */
    private class LogFilter implements Filter {

        public boolean isLoggable(LogRecord record) {
            boolean enableLog = UserConfiguration.getInstance().getBoolean("logging.enableLogging").booleanValue();
            if (enableLog) {
                String levelValue = (String) UserConfiguration.getInstance().getString("logging.logLevel");
                Level level = Level.parse(levelValue);
                return level != null && record.getLevel().intValue() >= level.intValue();
            }
            return false;
        }
    }
}
