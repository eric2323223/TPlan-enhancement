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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.Configurable;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.util.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * <p>Plugin manager provides framework for installation, uninstallation
 * and loading of plugins and serves as a central point of plugin instantiation for
 * all plugin factories.</p>
 *
 * <p>This class implements the singleton pattern. As it has a private constructor,
 * direct instantiation is not possible and other classes must use the {@link #getInstance()}
 * method to get a shared plugin manager instance.</p>
 *
 * <p>The shared instance gets created when the {@link #getInstance()} method is called
 * for the first time. The manager first loads the default map of plugins
 * from the internal XML specified by the {@link #INTERNAL_PLUGIN_XML} variable.
 * If there's a custom plugin map found at the path specified by {@link #EXTERNAL_PLUGIN_XML},
 * it is also loaded and applied to the default plugin map. Any changes caused during runtime
 * by plugin installation, uninstallation, enabling and disabling can be programatically saved
 * to the external XML through the {@link #savePluginMap()} method.</p>
 *
 * <p>Plugins create hierarchy which is reflected in the plugin map structure. Each
 * plugin must implement one of the public functional interfaces which were made
 * loadable through the plugin mechanism. Such interfaces are called
 * <b>exposed functional interfaces</b> or just <b>functional interfaces</b>.</p>
 *
 * <p>Plugins implementing the same functional interface are said to belong to the same
 * <b>plugin group</b>. There's an optional XML attribute allowing to assign
 * name to such a group. As the name is displayed by the GUI, it should shortly describe functionality
 * provided by the group (interface), for example <i>"Desktop Clients"</i>, <i>"Scripting Commands"</i> etc.</p>
 *
 * <p>Many objects in the {@product.name} code base such as desktop clients,
 * image comparison modules, script command handlers and report providers are
 * already implemented as plugins. These are called <b>default</b> or <b>built-in plugins</b> and they
 * are listed in the default plugin map <a href=PluginMap.xml>PluginMap.xml</a>.
 * Plugins which are delivered in separate JAR/ZIP files or class paths are often called
 * <b>external plugins</b>.</p>
 *
 * <p>As built-in plugins form integral part of the product code, they cannot be uninstalled.
 * They can be however disabled and/or replaced (<b>reimplemented</b>) by external plugins.
 * To reimplement an internal plugin, a new one must be developed where its {@link Plugin#getCode() getCode()}
 * method must return the same value as the internal one.</p>
 *
 * <p>Each plugin must implement the {@link Plugin} interface as well as one of
 * the exposed functional interfaces. Its class must be publicly instantiable which
 * means it can not be abstract or private. It also must have a default parameterless
 * constructor which initializes the instance correctly.</p>
 *
 * <p>Plugins can be delivered in form of directories or JAR/ZIP files with
 * compiled Java classes. Installation of plugins can be done in several ways:
 * <ul>
 * <li>Use the <b>Plugin Manager Window</b> available in the {@product.name} GUI.</li>
 * <li>Advanced users may modify one of the XML maps (either the internal one or
 * the external user-specific one) and add an entry for the new plugin.</li>
 * <li>Plugins can be also installed and uninstalled from Java programs including Java test scripts.
 * See the {@link #getAvailablePlugins(java.io.File)}, {@link #installPlugin(java.lang.String, java.lang.String, boolean, boolean)} and
 * {@link #uninstallPlugin(com.tplan.robot.plugin.Plugin)} methods form more information.</li>
 * </ul>
 * </p>
 *
 * <a name=xml>
 * <p>Format of the XML map is visible in the default
 * <a href=PluginMap.xml>PluginMap.xml</a>. Meaning of individual tags/attributes are:
 * <table border=1>
 * <tr>
 * <td style="vertical-align: top; color: rgb(255, 255, 255); background-color: rgb(0, 0, 153);"><b>Tag/Attribute</b></td>
 * <td style="vertical-align: top; color: rgb(255, 255, 255); background-color: rgb(0, 0, 153);"><b>Description</b></td>
 * </tr>
 * <tr>
 * <td>{@value #XML_APPLICATION}</td>
 * <td>Top level tag enclosing the {@product.name} plugin configuration.</td>
 * </tr>
 * <tr>
 * <td>{@value #XML_PLUGINGROUP}</td>
 * <td>This tag encloses group of plugins which implement the same functional interface.</td>
 * </tr>
 * <tr>
 * <td>{@value #XML_INTERFACE}</td>
 * <td>Defines class of an exposed functional interface in Java format, for example "mypackage.MyFunctionalInterface".</td>
 * </tr>
 * <tr>
 * <td>{@value #XML_GROUP_NAME}</td>
 * <td>Specifies displayable name of a plugin group. It in fact serves as short description of functionality provided by the associated functional interface.</td>
 * </tr>
 * <tr>
 * <td>{@value #XML_PLUGIN}</td>
 * <td>This tag encloses a particular plugin. Its value must be plugin class in Java format, for example "mypackage.MyPlugin"
 * External plugins are required to specify path to the library using the {@value #XML_SOURCE} attribute.</td>
 * </tr>
 * <tr>
 * <td>{@value #XML_SOURCE}</td>
 * <td>Attribute of the {@value #XML_PLUGIN} tag. It specifies path of the library (either a JAR/ZIP file or a single Java class path) which contains the compiled plugin class, for example "file:///usr/lib/myplugins.jar".
 * It is not mandatory for built-in plugins but other external plugins should always define it to make sure the classes are made visible to the JVM class loader. Failure to include the library path for an external plugin is likely to result in a <code>ClassNotFoundException</code> thrown at an attempt to install, enable or instantiate the plugin. </td>
 * </tr>
 * <tr>
 * <td>{@value #XML_ENABLED}</td>
 * <td>Optional attribute of the {@value #XML_PLUGIN} tag specifying whether the plugin is enabled or disabled. It's value may be either "true" or "false". It the attribute is not present, the attribute defaults to "true" meaning that the plugin is considered to be enabled.</td>
 * </tr>
 * </table>
 * </p>
 */
public class PluginManager implements FilenameFilter {

    /**
     * Shared instance of plugin manager.
     */
    private static PluginManager manager;
    /** List of plugin listeners. */
    private final List<PluginListener> pluginListeners = new ArrayList();

    // Plugin map: key=interface class, value=map of plugins implementing the interface
    // Inner map:  key=plugin getCode(), value=list of plugins (in fact PluginInfo instances)
    private Map<Class, Map<String, List<PluginInfo>>> plugins;

    // Library map: key=library URL, value=list of plugins
    private Map<String, List<PluginInfo>> libraryMap = new HashMap();

    // Interface map: key=interface class, value=group name
    private Map<Class, String> interfaceMap = new HashMap();
    private Map<Class, String> groupKeyMap = new HashMap();
    /**
     * Absolute path to external plugin map XML file. Just like user preferences,
     * the file is used to save any user modifications to the default plugin map such as installation,
     * uninstallation, activation or deactivation of plugins. The file
     * is typically stored in the user home folder and has the same name is the internal XML.
     */
    public static final String EXTERNAL_PLUGIN_XML = System.getProperty("user.home") + File.separator + "PluginMap.xml";
    /**
     * File name of the default plugin map XML. It is expected to be bundled
     * with the source code in the same package as this class. It's current value is {@value}.
     */
    public static final String INTERNAL_PLUGIN_XML = "PluginMap.xml";
    /**
     * Plugin group XML tag ({@value}).
     */
    public static final String XML_PLUGINGROUP = "plugingroup";
    /**
     * Plugin XML tag ({@value}).
     */
    public static final String XML_PLUGIN = "plugin";
    /**
     * Application XML tag ({@value}).
     */
    public static final String XML_APPLICATION = "application";
    /**
     * Plugin group name XML tag attribute ({@value}).
     */
    public static final String XML_GROUP_NAME = "name";
    /**
     * Resource bundle key of the plugin group name ({@value}).
     */
    public static final String XML_GROUP_NAME_KEY = "key";
    /**
     * Plugin group functional interface XML tag attribute ({@value}).
     */
    public static final String XML_INTERFACE = "interface";
    /**
     * Plugin status XML tag attribute ({@value}).
     */
    public static final String XML_ENABLED = "enabled";
    /**
     * Plugin library path XML tag attribute ({@value}).
     */
    public static final String XML_SOURCE = "source";
    private static boolean debug = System.getProperty("debug.pluginManager") != null;

    // Singleton pattern
    private PluginManager() {
        super();
    }

    /**
     * Get shared instance of Plugin Manager.
     * @return shared instance of Plugin Manager. Never returns null.
     */
    public static PluginManager getInstance() {
        if (manager == null) {
            manager = new PluginManager();
            try {
                manager.readDefaultPluginMap();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new Error("Failed to read internal plugin map!");
            }
        }
        return manager;
    }

    /**
     * <p>Uninstall an external plugin. This is not applicable to internal built-in
     * plugins which cannot be uninstalled. The method removes the plugin from the map of plugins
     * in the memory. If the plugin is loaded from a library which is not needed
     * any more (i.e. there's no other plugin installed from the library), it is
     * removed from the internal list of libraries and also from the application
     * Java class path. Successful uninstallation of a plugin fires a plugin event
     * with the {@link PluginEvent#PLUGIN_UNINSTALLED} code to all registered listeners.</p>
     *
     * <p>After the plugin is uninstalled, the method checks the default
     * plugin map for any built-in plugin providing the same functionality (with
     * the same code returned by {@link Plugin#getCode() getCode()}) and enables
     * it automatically if it exists. This logic is not applied to external plugins
     * and enabling of an already installed external plugin must be done
     * programatically.</p>
     *
     * <p>The method doesn't save plugin map changes to the file system.
     * Users calling this method programatically have to call the {@link #savePluginMap()}
     * method afterwards.</p>
     *
     * @param plugin a plugin. If the argument is null or it represents an internal
     * built-in plugin, the method does nothing and returns false.
     * @return true if the plugin was successfully uninstalled, false otherwise.
     */
    public boolean uninstallPlugin(Plugin plugin) {
        if (plugin != null) {
            PluginInfo pp = findPluginInfo(plugin);
            if (pp != null && pp.isBuiltIn()) {
                return false;
            }
            Collection<Map<String, List<PluginInfo>>> col = plugins.values();
            for (Map<String, List<PluginInfo>> map : col) {
                for (List<PluginInfo> ll : map.values()) {
                    for (PluginInfo pi : ll) {
                        if (pi.equals(plugin)) {
                            ll.remove(pi);
                            for (Object s : libraryMap.keySet().toArray()) {
                                List<PluginInfo> l = libraryMap.get(s);
                                if (l.contains(pi)) {
                                    l.remove(pi);
                                }
                                if (l.size() == 0) {
                                    libraryMap.remove(s);
                                }
                            }
                            PluginEvent e = new PluginEvent(this, PluginEvent.PLUGIN_UNINSTALLED, pi);
                            firePluginEvent(e);

                            // If there's another plugin deactivated by installation
                            // of this one, reenable it
                            List<PluginInfo> p = getPlugins(pi.getCode(), pi.getImplementedInterface(), true);
                            if (p != null && p.size() > 0) {
                                // Get the last built-in plugin
                                PluginInfo temp;
                                for (int i = p.size() - 1; i >= 0; i--) {
                                    temp = p.get(i);
                                    if (temp.isBuiltIn()) {
                                        temp.setEnabled(true);
                                        break;
                                    }
                                }
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Get list of all installed plugins which are present in the current plugin map.
     * @return list of all installed plugins ({@link PluginInfo} instances).
     */
    public List<PluginInfo> getPlugins() {
        List<PluginInfo> l = new ArrayList();
        Collection<Map<String, List<PluginInfo>>> col = plugins.values();
        for (Map<String, List<PluginInfo>> map : col) {
            for (List<PluginInfo> ll : map.values()) {
                l.addAll(ll);
            }
        }
        return l;
    }

    private void addURLToLibPath(URL url) {
        String s = System.getProperty(JavaLibrary.PLUGIN_LIBRARY_PATH_KEY);
        if (s != null && !s.isEmpty()) {
            try {
                StringTokenizer st = new StringTokenizer(s, File.pathSeparator);
                String t;
                File f = new File(url.getPath());
                while (st.hasMoreTokens()) {
                    t = st.nextToken();
                    if (new File(t).equals(f)) {
                        return;
                    }
                }
            } catch (Exception ex) {
                System.out.println("Broken Java plugin library path: " + s);
                ex.printStackTrace();
                return;
            }
        }
        if (s == null || s.isEmpty()) {
            s = url.getPath();
        } else {
            s = s + File.pathSeparator + url.getPath();
        }
        System.setProperty(JavaLibrary.PLUGIN_LIBRARY_PATH_KEY, s);
    }

    /**
     * Search the plugin map for a plugin info associated with the argument plugin.
     * @param p a plugin (Plugin instance).
     * @return plugin info associated with the plugin or null if the plugin is not installed.
     */
    private PluginInfo findPluginInfo(Plugin p) {
        if (p instanceof PluginInfo) {
            return (PluginInfo) p;
        }
        Collection<Map<String, List<PluginInfo>>> col = plugins.values();
        for (Map<String, List<PluginInfo>> map : col) {
            for (List<PluginInfo> ll : map.values()) {
                for (PluginInfo pi : ll) {
                    if (pi.equals(p)) {
                        return (PluginInfo) pi;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find out whether a plugin is already installed. Two plugins are considered
     * to be equal when they return the same unique code and version.
     * @param p a plugin.
     * @return true if the plugin is installed, false if not.
     */
    public boolean isInstalled(Plugin p) {
        Collection<Map<String, List<PluginInfo>>> col = plugins.values();
        for (Map<String, List<PluginInfo>> map : col) {
            for (List<PluginInfo> ll : map.values()) {
                for (PluginInfo pi : ll) {
                    if (pi.equals((Plugin) p)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find out whether a plugin is disabled or enabled. See
     * the {@link #setEnabled(com.tplan.robot.plugin.Plugin, boolean)}
     * method documentation for more information.
     * @param p a plugin.
     * @return true if the plugin is enabled, false if not or if the plugin
     * is not installed.
     */
    public boolean isEnabled(Plugin p) {
        PluginInfo pi = findPluginInfo(p);
        return pi != null && pi.isEnabled();
    }

    /**
     * Find out whether a plugin is a default (built-in) one. All plugins defined
     * by the default XML plugin map bundled with the code are supposed to be built-in.
     * @param plugin a plugin.
     * @return true if the plugin is a built-in plugin, false if not or if the plugin
     * is not installed.
     */
    public boolean isBuiltIn(Plugin plugin) {
        PluginInfo pi = findPluginInfo(plugin);
        return pi != null && pi.isBuiltIn();
    }

    /**
     * <p>Enable or disable a plugin. A disabled plugin is not visible to other classes
     * (through plugin factories) but remains installed and can be reenabled any time.
     * If the method is called to enable an already enabled plugin or disable an
     * already disabled one, it does nothing. Any change of the plugin status
     * fires a plugin event with the {@link PluginEvent#PLUGIN_ENABLED} or
     * {@link PluginEvent#PLUGIN_DISABLED} code to all registered listeners.</p>
     *
     * <p>Disabling of plugins is used in case of plugin reimplementations, which means
     * installation of a plugin which replaces functionality of a built-in
     * plugin or an already installed external one. The method can be
     * however also used to disable plugins programatically at a runtime.</p>
     *
     * <p>If an external plugin is being disabled, the method checks the default
     * plugin map for a built-in plugin providing the same functionality (with
     * the same code returned by {@link Plugin#getCode() getCode()}) and enables
     * it automatically if it exists. This logic is not applied to external plugins
     * and enabling of an already installed external plugins must be done
     * programatically.</p>
     *
     * @param plugin a plugin ({@link Plugin} instance).
     * @param enabled true enables functionality of the plugin, false disables.
     * @throws com.tplan.robot.plugin.CodeConflictException
     */
    public void setEnabled(Plugin plugin, boolean enabled) throws CodeConflictException {
        PluginEvent e = null;
        PluginInfo pi = findPluginInfo(plugin);

        // Check whether the status change is really needed
        if (pi.isEnabled() == enabled) {
            return;
        }

        // If activating, check if there's no other conflicting plugin
        if (enabled) {
            List<PluginInfo> l = getPlugins(plugin.getCode(), plugin.getImplementedInterface(), false);
            if (l != null && l.size() > 0) {
                throw new CodeConflictException(pi, l.get(0));
            }
        } else {
            // If deactivating, check whether there's a built-in plugin which was
            // disabled because of installation of this one
            List<PluginInfo> p = getPlugins(pi.getCode(), pi.getImplementedInterface(), true);
            if (p != null && p.size() > 0) {
                // Get the last built-in plugin
                PluginInfo temp;
                for (int i = p.size() - 1; i >= 0; i--) {
                    temp = p.get(i);
                    if (temp.isBuiltIn()) {
                        temp.setEnabled(true);
                        break;
                    }
                }
            }
        }
        if (pi != null) {
            if (enabled) {
                if (!pi.isEnabled()) {
                    pi.setEnabled(true);
                    e = new PluginEvent(this, PluginEvent.PLUGIN_ENABLED, pi);
                }
            } else {
                if (pi.isEnabled()) {
                    pi.setEnabled(false);
                    e = new PluginEvent(this, PluginEvent.PLUGIN_DISABLED, pi);
                }
            }
        }
        if (e != null) {
            firePluginEvent(e);
        }
    }

    /**
     * Get list of plugin info instances for all plugins implementing a particular
     * functional interface.
     *
     * @param implementedInterface class of the exposed functional interface
     * implemented by the plugin.
     *
     * @param includeDisabled false returns just the enabled plugin, true lists
     * all plugins regardless whether they are enabled or disabled.
     *
     * @return list of plugin info instances for all installed plugins
     * implementing the specified functional interface.
     */
    public List<PluginInfo> getPlugins(Class implementedInterface, boolean includeDisabled) {
        return getPlugins(plugins, implementedInterface, includeDisabled);
    }

    /**
     * Get list of plugin info instances for all plugins implementing a particular
     * functional interface.
     *
     * @param pluginMap a plugin map to search.
     *
     * @param implementedInterface class of the exposed functional interface
     * implemented by the plugin.
     *
     * @param includeDisabled false returns just the enabled plugin, true lists
     * all plugins regardless whether they are enabled or disabled.
     *
     * @return list of plugin info instances for all installed plugins
     * implementing the specified functional interface.
     */
    private List<PluginInfo> getPlugins(Map<Class, Map<String, List<PluginInfo>>> pluginMap, Class implementedInterface, boolean includeDisabled) {
        List<PluginInfo> l = new ArrayList();
        Map<String, List<PluginInfo>> map = pluginMap.get(implementedInterface);
        if (map != null) {
            Collection<List<PluginInfo>> col = map.values();
            for (List<PluginInfo> ll : col) {
                for (PluginInfo cl : ll) {
                    if (includeDisabled || isEnabled(cl)) {
                        l.add(cl);
                    }
                }
            }
        }
        return l;
    }

    List<String> getPluginCodes(Class implementedInterface) {
        List<String> l = new ArrayList();
        Map<String, List<PluginInfo>> map = plugins.get(implementedInterface);
        List<Class> pl;
        if (map != null) {
            String[] keys = new String[map.size()];
            keys = map.keySet().toArray(keys);
            boolean add;
            for (String key : keys) {
                add = false;
                for (Plugin cl : map.get(key)) {
                    if (isEnabled(cl)) {
                        add = true;
                    }
                }
                if (add) {
                    l.add(key);
                }
            }
        }
        return l;
    }

    /**
     * Get list of all installed plugins of the specific code and implemented interface
     * which are present in the current plugin map.
     * @param code plugin code.
     * @param implementedInterface exposed functional interface implemented by the plugin.
     * @param includeDisabled true will include disabled plugins, false lists just enabled ones.
     * @return list of all installed plugins ({@link PluginInfo} instances) which return
     * the specified plugin code and implement the specified interface.
     */
    public List<PluginInfo> getPlugins(Object code, Class implementedInterface, boolean includeDisabled) {
        Map<String, List<PluginInfo>> map = plugins.get(implementedInterface);
        if (map != null && map.containsKey(code)) {
            if (includeDisabled) {
                return new ArrayList(map.get(code));
            } else {
                List<PluginInfo> l = new ArrayList();
                for (PluginInfo p : map.get(code)) {
                    if (isEnabled(p)) {
                        l.add(p);
                    }
                }
                return l;
            }
        }
        return null;
    }

    Plugin getPluginInstance(Object code, Class implementedInterface) {
        Plugin p = null;
        List<PluginInfo> l = getPlugins(code, implementedInterface, false);
        if (l != null && l.size() > 0) {
            p = l.get(0);
            if (p instanceof PluginInfo) {
                try {
                    p = (Plugin) ((PluginInfo) p).getPluginClass().newInstance();
                    if (p instanceof Configurable) {
                        ((Configurable) p).setConfiguration(UserConfiguration.getInstance());
                    }
                } catch (Exception ex) {
                    p = null;
                    ex.printStackTrace();
                }
            }
        }
        return p;
    }

    private void loadAutoPlugins() {
        File autoDir = null;
        try {
            String path = Utils.getInstallPath();
            autoDir = new File(path, "plugins");
            if (autoDir.canRead() && autoDir.isDirectory()) {
                File files[] = autoDir.listFiles(this);
                if (files != null) {
                    List<PluginInfo> l;
                    for (File f : files) {
                        try {
                            l = getAvailablePlugins(f);
                            if (l != null && l.size() > 0) {
                                for (PluginInfo pi : l) {
                                    if (!isInstalled(pi)) {
                                        addPlugin(pi.pluginClass.getName(), f.toURI().toURL().toString(), false, true, true);
                                    }
                                }
                            }
                        } catch (Throwable ex) {
                            System.out.println("Error when reading plugin JAR file " + f.getAbsolutePath() + "\nNo plugins from this file will be installed.");
                        }
                    }
                }
            }
        } catch (Throwable t) {
            System.out.println("Error when loading auto plug ins from " + (autoDir != null ? autoDir.getAbsolutePath() : "?"));
            t.printStackTrace();
        }
    }

    /**
     * Read the default plugin map from the XML file specified
     * by the {@link #INTERNAL_PLUGIN_XML} variable.
     *
     * @throws javax.xml.parsers.ParserConfigurationException when there's a parser configuration error.
     * @throws org.xml.sax.SAXException when the XML format is incorrect.
     * @throws java.io.IOException when the input stream is not readable due to an I/O error.
     * @throws java.lang.ClassNotFoundException when class of a plugin specified in the map is not found.
     * It typically means that the plugin class or any of its dependencies are not present in the
     * class path of the current Java runtime.
     * @throws java.lang.InstantiationException when the plugin can't be instantiated.
     * Each plugin must have a default parameterless constructor and must be instantiable.
     * @throws java.lang.IllegalAccessException An IllegalAccessException is thrown when plugin manager tries
     * to create a plugin instance or invoke its method through the Java Reflection API, but the currently
     * executing method does not have access to the definition of
     * the specified plugin class, method or constructor.
     */
    private void readDefaultPluginMap() throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        plugins = new HashMap();
        libraryMap = new HashMap();

        // First load defaults
        InputStream stream = getClass().getResourceAsStream(INTERNAL_PLUGIN_XML);
        readMap(stream, true);

        // If there's an external file, overwrite the list with it
        try {
            String file = EXTERNAL_PLUGIN_XML;
            readMap(new FileInputStream(file), false);
        } catch (IOException ex) {
            // Nothing to do, an I/O exception means the user has no extra plugins
        }

        loadAutoPlugins();
    }

    /**
     * Get path of the plugin library (JAR/ZIP file or directory
     * of compiled Java classes).
     *
     * @param plugin a plugin.
     * @return path of the library (JAR/ZIP file or directory
     * with compiled Java classes) which contains the specified plugin.
     */
    private String getPluginSource(Plugin plugin) {
        for (String url : libraryMap.keySet()) {
            for (Plugin p : libraryMap.get(url)) {
                if (p.equals(plugin)) {
                    return url;
                }
            }
        }
        return null;
    }

    /**
     * Get map of exposed functional interfaces.
     * @return map of exposed functional interfaces where key is the interface
     * class and value is the plugin group name assigned to the interface by the
     * {@value #XML_GROUP_NAME} attribute in the XML map.
     */
    public Map<Class, String> getInterfaceMap() {
        return new HashMap<Class, String>(interfaceMap);
    }

    /**
     * <p>Save the current plugin map from the memory to a file specified by the
     * {@link #EXTERNAL_PLUGIN_XML} variable. Format of this XML file is specified
     * in description of this class.</p>
     *
     * <p>Note that saving of the plugin map is not performed automatically and
     * users who modify the plugin map from custom programs must call this method
     * to save their changes.</p>
     *
     * @throws java.io.FileNotFoundException when the file cannot be opened for
     * writing.
     */
    public void savePluginMap() throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(EXTERNAL_PLUGIN_XML);
        pw.println("<?xml version=\"1.0\"?>");
        pw.println("<" + XML_APPLICATION + ">");

        // Plugins are organized by interfaces.
        // The top level XML plugin group tag must have attributes "interface" and "code"
        String groupKey;
        for (Class cl : plugins.keySet()) {
            groupKey = "";
            if (groupKeyMap.containsKey(cl)) {
                groupKey = " " + XML_GROUP_NAME_KEY + "=\"" + groupKeyMap.get(cl) + "\"";
            }
            pw.println("   <" + XML_PLUGINGROUP + " " + XML_GROUP_NAME + "=\"" +
                    interfaceMap.get(cl) + "\" " + XML_INTERFACE + "=\"" + cl.getCanonicalName() + "\"" + groupKey + ">");

            Map<String, List<PluginInfo>> map = plugins.get(cl);
            for (List<PluginInfo> l : map.values()) {
                for (PluginInfo p : l) {
                    if (!isBuiltIn(p)) {
                        String s = "      <" + XML_PLUGIN + " " + XML_ENABLED + "=\"" + isEnabled(p) + "\"";
                        String src = getPluginSource(p);
                        if (src != null) {
                            s += " " + XML_SOURCE + "=\"" + src + "\"";
                        }
                        s += ">" + findPluginInfo(p).getPluginClass().getCanonicalName() + "</" + XML_PLUGIN + ">";
                        pw.println(s);
                    }
                }
            }
            pw.println("   </" + XML_PLUGINGROUP + ">");
        }
        pw.println("</" + XML_APPLICATION + ">");
        pw.flush();
        pw.close();
    }

    /**
     * Read the plugin map from the specified input stream. The map is expected to be
     * in the XML format specified in description of this class.
     *
     * @param in input stream with the XML data.
     * @param isDefault true indicates that it is a default plugin map
     * loaded from the internal XML bundled with the code (see {@link #INTERNAL_PLUGIN_XML}).
     * Plugins flagged as internal have slightly different behavior from
     * the externally delivered ones. For example, if an external plugin gets
     * uninstalled or disabled and there's an internal one providing the same functionality,
     * it gets automatically reenabled. Internal plugins also cannot be uninstalled.
     *
     * @throws javax.xml.parsers.ParserConfigurationException when there's a parser configuration error.
     * @throws org.xml.sax.SAXException when the XML format is incorrect.
     * @throws java.io.IOException when the input stream is not readable due to an I/O error.
     * @throws java.lang.ClassNotFoundException when class of a plugin specified in the map is not found.
     * It typically means that the plugin class or any of its dependencies are not present in the
     * class path of the current Java runtime.
     * @throws java.lang.InstantiationException when the plugin can't be instantiated.
     * Each plugin must have a default parameterless constructor and must be instantiable.
     * @throws java.lang.IllegalAccessException An IllegalAccessException is thrown when plugin manager tries
     * to create a plugin instance or invoke its method through the Java Reflection API, but the currently
     * executing method does not have access to the definition of
     * the specified plugin class, method or constructor.
     */
    private void readMap(InputStream in, boolean isDefault) throws ParserConfigurationException,
            SAXException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(in);

        doc.getDocumentElement().normalize();

        Node n, top;
        String name, implInterface = null, groupName, groupKey;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        NodeList nodeList = doc.getElementsByTagName(XML_PLUGINGROUP);
        for (int i = 0; i < nodeList.getLength(); i++) {
            top = nodeList.item(i);
            NodeList pluginList = top.getChildNodes();
            String source = null;
            if (top.getNodeType() == Node.ELEMENT_NODE) {
                source = ((Element) top).getAttribute(XML_SOURCE);
                implInterface = ((Element) top).getAttribute(XML_INTERFACE);
                groupKey = ((Element) top).getAttribute(XML_GROUP_NAME_KEY);
                groupName = ((Element) top).getAttribute(XML_GROUP_NAME);
                if (groupKey != null && groupKey.length() > 0 && res.containsKey(groupKey)) {
                    groupName = res.getString(groupKey);
                }

                if (implInterface != null && implInterface.length() > 0) {
                    Class cl = Class.forName(implInterface);
                    interfaceMap.put(cl, groupName == null ? "" : groupName);
                    if (groupKey != null) {
                        groupKeyMap.put(cl, groupKey);
                    }
                }
            }
            for (int j = 0; j < pluginList.getLength(); j++) {
                n = pluginList.item(j);
                name = n.getNodeName();
                if (name.equalsIgnoreCase(XML_PLUGIN) && n.getNodeType() == Node.ELEMENT_NODE) {
                    String active = ((Element) n).getAttribute(XML_ENABLED);
                    boolean isActive = true;
                    if (active != null && active.equalsIgnoreCase("false")) {
                        isActive = false;
                    }
                    if (source == null || source.length() == 0) {
                        source = ((Element) n).getAttribute(XML_SOURCE);
                    }

                    n = n.getChildNodes().item(0);
                    try {
                        // Read the plugin class
                        addPlugin(n.getNodeValue(), source, true, isActive, isDefault);
                    } catch (ClassNotFoundException ex) {
                        System.out.println("WARNING: Plugin class " + n.getNodeValue() + " was not found. The plugin will be disabled.");
                    } catch (NoClassDefFoundError ex) {
                        System.out.println("WARNING: Plugin class " + n.getNodeValue() + " failed to load because it is not compatible\nwith this Robot version. The plugin will be disabled.");
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * <p>Install and eventually enable an external plugin. The method adds the plugin to the
     * internal plugin map and if the <code>enable</code> flag is set to true, it
     * makes it enabled and thus immediatelly visible to all plugin
     * factories. If the plugin library is not present on the Java class path,
     * it is dynamically added and all its classes are made available to the default
     * JVM class loader. Successful installation of a plugin fires a plugin event
     * with the {@link PluginEvent#PLUGIN_INSTALLED} code to all registered listeners.</p>
     *
     * <p>When value of the <code>force</code> argument is false, the method
     * supports the following standard error scenarios:
     *
     * <ul>
     * <li>An attempt to install and enable plugin while there's already another enabled one
     * with the same functionality (i.e. both return the same code through {@link Plugin#getCode()}) will
     * throw a {@link CodeConflictException}. To override it recall the method with
     * the <code>force</code> set to true. The method then disables the other plugin
     * and installs and enables the new one. For more information on plugin disabling
     * see the {@link #setEnabled(com.tplan.robot.plugin.Plugin, boolean)} method.</li>
     *
     * <li>If a higher version of the same plugin is already installed, the method
     * throws a {@link HigherVersionInstalledException}. For this purpose two plugins
     * are considered to be the same if they have the same {@link Plugin#getUniqueId() unique ID}.
     * This cannot be overriden by the
     * <code>force</code> flag. To proceed with the downgrade one has to uninstall
     * the plugin first using {@link #uninstallPlugin(com.tplan.robot.plugin.Plugin)}
     * and then install the new one. Internal built-in plugins
     * cannot be downgraded.</li>
     *
     * <li>If the plugin requires higher {@product.name} version than is currently
     * installed, an {@link UnsupportedVersionException} is thrown. This cannot
     * be overriden by the <code>force</code> flag and {@product.name} upgrade
     * to the required version must be performed before the plugin can be successfully
     * installed.</li>
     *
     * <li>If one or more plugin dependencies are not installed, a {@link DependencyMissingException}
     * exception is thrown. This error can
     * be overriden by the <code>force</code> flag. The plugin is however unlikely to work correctly
     * and installation of the missing plugins is highly recommended.</li>
     * </ul>
     *
     * <p>Non-standard errors are reported through other exceptions declared by this method
     * such as <code>InstantiationException</code>, <code>IllegalAccessException</code>,
     * <code>IOException</code> and <code>ClassNotFoundException</code>. They
     * typically indicate an internal or misconfiguration error where the plugin
     * class (library) is missing, it cannot be instantiated or added to the plugin map.</p>
     *
     * <p>The method doesn't save plugin map changes to the file system.
     * Users calling this method programatically have to call the {@link #savePluginMap()}
     * method afterwards.</p>
     *
     * @param pluginClassName full plugin class name, for example "mypackage.MyPlugin".
     * @param libraryUrl URL of the library which contains the plugin class.
     * @param force true overrides some standard error situations. See the method description.
     * @param enable true enables the plugin after installation, false just installs
     * and leaves the plugin disabled.
     *
     * @throws java.io.IOException when the map is not writable or the specified library is not available/readable due to an I/O error.
     * @throws java.lang.ClassNotFoundException when class of a plugin specified in the map is not found.
     * It typically means that the plugin class or any of its dependencies are not present in the
     * class path of the current Java runtime.
     * @throws java.lang.InstantiationException when the plugin can't be instantiated.
     * Each plugin must have a default parameterless constructor and must be instantiable.
     * @throws java.lang.IllegalAccessException An IllegalAccessException is thrown when plugin manager tries
     * to create a plugin instance or invoke its method through the Java Reflection API, but the currently
     * executing method does not have access to the definition of
     * the specified plugin class, method or constructor.
     * @throws com.tplan.robot.plugin.UnsupportedVersionException thrown when the plugin
     * requires higher {@product.name} version than the current one.
     *
     * @throws com.tplan.robot.plugin.HigherVersionInstalledException thrown when a higher
     * version of the same plugin is already installed.
     *
     * @throws com.tplan.robot.plugin.CodeConflictException thrown when another
     * plugin providing the same functionality is already installed and enabled.
     * @throws DependencyMissingException when one or more dependencies (other plugins)
     * required by the plugin is not installed.
     */
    public void installPlugin(String pluginClassName, String libraryUrl, boolean force, boolean enable) throws InstantiationException, IllegalAccessException, IOException, ClassNotFoundException, UnsupportedVersionException, HigherVersionInstalledException, CodeConflictException, DependencyMissingException {
        addPlugin(pluginClassName, libraryUrl, force, enable, false);
    }

    /**
     * Private install method allowing to indicate that the added plugin
     * is a built-in one. This method is used internally to add plugins to the map
     * when the internal XML is being parsed. See {@link #installPlugin(java.lang.String, java.lang.String, boolean, boolean)
     * for information on method functionality and arguments.
     */
    private void addPlugin(String pluginClassName, String libraryUrl, boolean force, boolean enable, boolean isDefault)
            throws InstantiationException, IllegalAccessException, IOException, ClassNotFoundException, UnsupportedVersionException, HigherVersionInstalledException, CodeConflictException, DependencyMissingException {

        List<PluginInfo> l = null;
        if (libraryUrl != null && libraryUrl.trim().length() > 0) {

            // Create a list of plugins under the given libraryUrl
            // if it doesn't exist
            l = libraryMap.get(libraryUrl);
            if (l == null) {
                l = new ArrayList();
                libraryMap.put(libraryUrl, l);

                // Add the library to the class path. We have to do it
                // because we will instantiate the plugin later on.
                addURL(new java.net.URL(libraryUrl));
            }
        }

        PluginInfo pi = new PluginInfo(pluginClassName, libraryUrl, isDefault, enable);
        Plugin p = pi.getPluginInstance();

        if (p instanceof JavaLibrary && ((JavaLibrary) p).isLibrary()) {
            addURLToLibPath(new java.net.URL(libraryUrl));
        }
        Class pluginInterface = p.getImplementedInterface();
        String pluginCode = p.getCode();

        // Perform checks ------------------------------
        // Is application version higher or equal to the requested one?
        if (!pi.isSupported()) {
            throw new UnsupportedVersionException(pi, Utils.getVersion());
        }

        // Are all plugin dependencies satisfied?
        // Do not check default plugins because it may cause an infinite loop.
        // Also do not check if the force mode is on.
        if (!isDefault && !force) {
            p.checkDependencies(this);
        }

        // Set the group code which is saved in the interface map
        pi.setGroupName(interfaceMap.get(p.getImplementedInterface()));

        // Is there already a plugin map for the implemented interface?
        // If not, create a new one
        Map<String, List<PluginInfo>> m = plugins.get(pluginInterface);
        if (m == null) {
            m = new HashMap();
            plugins.put(pluginInterface, m);
        }

        // Is this plugin already installed?
        List<PluginInfo> lpi = m.get(pluginCode);
        PluginInfo piForUpdate = null;

        if (lpi != null) {
            for (PluginInfo pinst : lpi) {
                // Is it the same plugin (i.e. has the same unique code)?
                if (pinst.getUniqueId().equals(pi.getUniqueId())) {

                    // If version of already installed plugin is higher,
                    // throw a HigherVersionInstalledException
                    if (pinst.compareTo(pi) > 0) {
                        throw new HigherVersionInstalledException(pi, pinst);
                    } else {  // Update -> remove the older plugin
                        piForUpdate = pinst;
                    }

                } else if (pinst.isEnabled() && enable) {

                    // There's another plugin which has the same code but returns
                    // other unique code. It may be a feature reimplementation
                    // from another vendor or a customization.If the force mode is off, throw a
                    // CodeConflictException to notify user that the other plugin
                    // needs to be deactivated or uninstalled.
                    if (force) {
                        pinst.setEnabled(false);
                    } else {
                        throw new CodeConflictException(pi, pinst);
                    }
                }
            }

        }

        // If there's no list (i.e. this plugin is the first one for the given interface), create it
        if (lpi == null) {
            lpi = new ArrayList();
            m.put(pluginCode, lpi);
        }

        // Save the plugin info instance to the plugins, key is its code
        lpi.add(pi);

        // If we are updating, remove the old plugin
        // Bug fix in 2.0.3: if the plug in is a built in one, disable it instead.
        if (piForUpdate != null) {
            if (isBuiltIn(piForUpdate)) {
                piForUpdate.setEnabled(false);
            } else {
                uninstallPlugin(piForUpdate);
            }
        }

        // If the plugin is loaded from an external library,
        // save its URL to the library plugins as well
        if (l != null) {
            l.add(pi);
        }

        if (debug) {
            System.out.println("Installed plugin " + pluginClassName + "\n  source='" + libraryUrl + "'\n  code=" + pluginCode + "\n  implemented interface=" + pluginInterface.getCanonicalName());
        }

        PluginEvent event = new PluginEvent(this, piForUpdate != null ? PluginEvent.PLUGIN_UPDATED : PluginEvent.PLUGIN_INSTALLED, pi);
        firePluginEvent(event);
    }

    /**
     * Find out whether a class implements a particular interface. Unlike the
     * <code>Class.getImplementedInterfaces()</code> the method also checks all
     * superclasses. It provides the same functionality as the "instanceof"
     * operator save that it avoids any class instantiation.
     *
     * @param cl a class to be checked for the implemented interface.
     * @param interf an interface class.
     * @return true if the class or any of its superclasses implements the
     * specified interface or false otherwise.
     */
    public static boolean implementsInterface(Class cl, Class interf) {
        boolean flag = false;
        for (Class intf : cl.getInterfaces()) {
            if (intf.getCanonicalName().equals(interf.getCanonicalName())) {
                return true;
            }
            Class[] si = intf.getInterfaces();
            if (si != null && si.length > 0) {
                flag |= implementsInterface(intf, interf);
            }
        }
        if (!flag && cl.getSuperclass() != null) {
            flag |= implementsInterface(cl.getSuperclass(), interf);
        }
        return flag;
    }

    /**
     * Get plugins available in a JAR or ZIP file or Java class path. The method
     * searches the specified resource and finds all classes that implement the
     * {@link Plugin} interface.
     *
     * @param file a JAR/ZIP file or a directory with compiled Java  classes.
     * @return list of {@link PluginInfo} instances describing available plugins.
     *
     * @throws java.io.IOException when the input stream is not readable due to an I/O error.
     * @throws java.lang.ClassNotFoundException when class of a plugin specified in the map is not found.
     * It typically means that the plugin class or any of its dependencies are not present in the
     * class path of the current Java runtime.
     * @throws java.lang.IllegalAccessException An IllegalAccessException is thrown when plugin manager tries
     * to create a plugin instance or invoke its method through the Java Reflection API, but the currently
     * executing method does not have access to the definition of
     * the specified plugin class, method or constructor.
     */
    public List<PluginInfo> getAvailablePlugins(File file) throws IOException, ClassNotFoundException, IllegalAccessException {
        List<PluginInfo> l = new ArrayList();
        List<String> classes = getResources(file.getCanonicalPath(), Pattern.compile(".*class$"));

        // Add the JAR file to the class loader path
        addURL(file.toURI().toURL());

        for (String cl : classes) {
            // Windows hack: the path may contain both slash and backslash
            cl = cl.replace(File.separator, ".");
            cl = cl.replace("/", ".");

            if (cl.endsWith(".class")) {
                cl = cl.substring(0, cl.length() - ".class".length());
            }
            Class o = Class.forName(cl);

            if (!o.isInterface() && implementsInterface(o, Plugin.class) && !o.equals(PluginInfo.class) && (o.getModifiers() & Modifier.ABSTRACT) == 0 && (o.getModifiers() & Modifier.PRIVATE) == 0) {
                PluginInfo pi;
                try {
                    pi = new PluginInfo(cl, file.toURI().toURL().toString(), false, false);
                    pi.setGroupName(interfaceMap.get(pi.getImplementedInterface()));
                    l.add(pi);
                    if (debug) {
                        System.out.println(cl + " is a plugin");
                    }
                } catch (MalformedURLException ex) {
                    ex.printStackTrace();
                } catch (InstantiationException ex) {
                    ex.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return l;
    }

    public static List<String> getResources(Pattern pattern) throws IOException {
        ArrayList<String> retval = new ArrayList<String>();
        String classPath = System.getProperty("java.class.path", ".");
        String[] classPathElements = classPath.split(File.pathSeparator);
        for (String element : classPathElements) {
            retval.addAll(getResources(element, pattern));
        }
        return retval;
    }

    public static List<String> getResources(String element, Pattern pattern) throws IOException {
        ArrayList<String> retval = new ArrayList<String>();
        File file = new File(element);
        if (file.isDirectory()) {
            retval.addAll(getResourcesFromDirectory(file, file.getCanonicalPath(), pattern));
        } else {
            retval.addAll(getResourcesFromJarFile(file, pattern));
        }
        return retval;
    }

    public static List<String> getResourcesFromDirectory(File directory, String directoryCanonicalPath, Pattern pattern) throws IOException {
        ArrayList<String> retval = new ArrayList<String>();
        File[] fileList = directory.listFiles();
        for (File file : fileList) {
            if (file.isDirectory()) {
                retval.addAll(getResourcesFromDirectory(file, directoryCanonicalPath, pattern));
            } else {
                String fileName = file.getCanonicalPath();
                boolean accept = pattern.matcher(fileName).matches();
                if (accept) {
                    if (file.isAbsolute()) {
                        fileName = fileName.substring(directoryCanonicalPath.length() + File.separator.length());
                    }
                    retval.add(fileName);
                }
            }
        }
        return retval;
    }

    /**
     * List resources from a JAR or ZIP file. This is used to get all Java file names
     * contained in an archive.
     *
     * @param file a JAR or ZIP file.
     * @param pattern a pattern to apply to the resource list.
     * @return collection of resources available in the argument archive.
     */
    public static Collection<String> getResourcesFromJarFile(File file, Pattern pattern) {
        ArrayList<String> retval = new ArrayList<String>();
        ZipFile zf;
        try {
            zf = new ZipFile(file);
        } catch (ZipException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
        Enumeration e = zf.entries();
        while (e.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) e.nextElement();
            String fileName = ze.getName();
            boolean accept = pattern.matcher(fileName).matches();
            if (accept) {
                retval.add(fileName);
            }
        }
        try {
            zf.close();
        } catch (IOException e1) {
            throw new Error(e1);
        }
        return retval;
    }

    /**
     * Add a URL dynamically to the class path. This is used to load plug in JAR
     * files or classes.
     *
     * @param u a JAR file or class path URL.
     * @throws java.io.IOException
     */
    public static void addURL(URL u) throws IOException {

        URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        URL urls[] = sysLoader.getURLs();
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].toString().equals(u.toString())) {
                return;
            }
        }

        Class sysclass = URLClassLoader.class;

        try {
            if (debug) {
                System.out.println("Adding library to the class path: " + u);
            }
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(sysLoader, new Object[]{u});
//            String classPath = System.getProperty("java.class.path", ".");
//            classPath += File.separator+u.getFile();
//            System.setProperty("java.class.path", classPath);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }

    /**
     * Fire a plugin event to all registered listeners.
     * @param e a plugin event.
     */
    private void firePluginEvent(PluginEvent e) {
        synchronized (pluginListeners) {
            for (PluginListener l : pluginListeners) {
                try {
                    l.pluginEvent(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Add a plugin listener. Plugin listeners are notified of plugin map changes
     * such as plugin installation, uninstallation, activation or deactivation.
     * @param listener a listener to be registered for plugin map changes.
     */
    public void addPluginListener(PluginListener listener) {
        synchronized (pluginListeners) {
            if (!pluginListeners.contains(listener)) {
                pluginListeners.add(listener);
            }
        }
    }

    /**
     * Remove an object from the list of plugin listeners. If the specified
     * object is not present in the listener list, the method does nothing. See
     * the {@link #addPluginListener(com.tplan.robot.plugin.PluginListener)} method
     * for more information.
     *
     * @param listener a listener to be registered for plugin events.
     */
    public void removePluginListener(PluginListener listener) {
        synchronized (pluginListeners) {
            if (pluginListeners.contains(listener)) {
                pluginListeners.remove(listener);
            }
        }
    }

    public boolean accept(File dir, String name) {
        return new File(dir, name).isFile() && "jar".equalsIgnoreCase(Utils.getExtension(name));
    }
}
