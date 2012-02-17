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

import com.tplan.robot.preferences.Configurable;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.util.Utils;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper around {@link Plugin} attributes which is often used instead of real plugins to avoid
 * unnecessary plugin instantiation. It also defines a few extra methods which
 * deliver some functionality of the plugin framework.
 * @product.signature
 */
public class PluginInfo implements Plugin, Comparable<PluginInfo> {

    // Plugin properties
    Class pluginClass;
    Class implementedInterface;
    String name;
    String description;
    String displayName;
    String vendorName;
    String vendorHomePage;
    String uniqueId;
    int[] version;
    int[] lowestSupportedVersion;
    String messageBeforeInstall;
    String messageAfterInstall;
    String supportContact;
    Date date;
    boolean restartRequired;
    private boolean configurable;

    // Meta data
    boolean enabled;
    String groupName;
    boolean builtIn;
    String libraryUrl;

    // Class loader map
    static Map<String, ClassLoader> classLoaderMap = new HashMap();

    /**
     * Constructor with package permissions.
     */
    PluginInfo() {
    }

    /**
     * Constructor with package permissions.
     * @param pluginClassName plugin class name
     * @param libraryUrl library URL
     * @param builtIn indicates whether the plugin is built-in or external
     * @param enabled indicates whether the plugin is enabled or disabled
     * @throws java.net.MalformedURLException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     */
    PluginInfo(String pluginClassName, String libraryUrl, boolean builtIn, boolean enabled) throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (libraryUrl != null && libraryUrl.length() > 0) {
            ClassLoader cl = getClassLoader(libraryUrl);
            this.pluginClass = Class.forName(pluginClassName, true, cl);
        } else {
            this.pluginClass = Class.forName(pluginClassName);
        }

        Object instance = pluginClass.newInstance();
        setSampleInstance((Plugin) instance);
        this.builtIn = builtIn;
        this.enabled = enabled;
        this.libraryUrl = libraryUrl;
    }

    private ClassLoader getClassLoader(String libraryUrl) throws MalformedURLException {
        return ClassLoader.getSystemClassLoader();

// Removed - no custom class loaders will be supported
//        ClassLoader cl = classLoaderMap.get(libraryUrl);
//        if (cl == null) {
//            URL[] searchPath = new URL[]{new URL(libraryUrl)};
//            cl = new URLClassLoader(searchPath);
//            classLoaderMap.put(libraryUrl, cl);
//        }
//        return cl;
    }

    /**
     * @return the pluginClass
     */
    public Class getPluginClass() {
        return pluginClass;
    }

    /**
     * @param pluginClass the pluginClass to set
     */
    void setPluginClass(Class pluginClass) {
        this.pluginClass = pluginClass;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    void setEnabled(boolean active) {
        this.enabled = active;
    }

    /**
     * @return the groupName
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @param groupName the groupName to set
     */
    void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * @param sampleInstance the sampleInstance to set
     */
    private void setSampleInstance(Plugin sampleInstance) {
        pluginClass = sampleInstance.getClass();
        implementedInterface = sampleInstance.getImplementedInterface();
        name = sampleInstance.getCode();
        description = sampleInstance.getDescription();
        displayName = sampleInstance.getDisplayName();
        vendorName = sampleInstance.getVendorName();
        vendorHomePage = sampleInstance.getVendorHomePage();
        version = sampleInstance.getVersion();
        supportContact = sampleInstance.getSupportContact();
        date = sampleInstance.getDate();
        restartRequired = sampleInstance.requiresRestart();
        uniqueId = sampleInstance.getUniqueId();
        lowestSupportedVersion = sampleInstance.getLowestSupportedVersion();
        messageAfterInstall = sampleInstance.getMessageAfterInstall();
        messageBeforeInstall = sampleInstance.getMessageBeforeInstall();
        configurable = sampleInstance instanceof Configurable;
    }

    /**
     * @return the builtIn
     */
    public boolean isBuiltIn() {
        return builtIn;
    }

    /**
     * @param builtIn the builtIn to set
     */
    void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    /**
     * Returns 1 if version of this plugin is higher, -1 if lower
     * and 0 if the versions are equal.
     * @param o another plugin info instance to compare to
     */
    public int compareTo(PluginInfo o) {
        int thisVersion[] = o.getVersion();
        return compare(version, thisVersion);
    }

    public boolean isSupported() {
        return compare(Utils.getVersion(), getLowestSupportedVersion()) >= 0;
    }

    /**
     * Returns 1 if version1 is higher than version2, 0 if they are equal or
     * -1 if version1 is lower than version2.
     *
     * @param version1
     * @param version2
     * @return
     */
    private int compare(int version1[], int version2[]) {
        int max = Math.min(version1.length, version2.length);
        for (int i = 0; i < max; i++) {
            if (version1[i] != version2[i]) {
                return version1[i] > version2[i] ? 1 : -1;
            }
        }
        if (version1.length == version2.length) {
            return 0;
        }
        return version1.length > version2.length ? 1 : -1;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PluginInfo) {
            PluginInfo p = (PluginInfo) o;
            return p.getUniqueId().equals(uniqueId) && compareTo(p) == 0;
        }
        return super.equals(o);
    }

    /**
     * @return the libraryUrl
     */
    public String getLibraryUrl() {
        return libraryUrl;
    }

    public Plugin getPluginInstance() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Plugin p = (Plugin) pluginClass.newInstance();
        if (p instanceof Configurable) {
            ((Configurable) p).setConfiguration(UserConfiguration.getInstance());
        }
        return p;
    }

    /**
     * @return the implementedInterface
     */
    public Class getImplementedInterface() {
        return implementedInterface;
    }

    /**
     * @return the name
     */
    public String getCode() {
        return name;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return the vendorName
     */
    public String getVendorName() {
        return vendorName;
    }

    /**
     * @return the vendorHomePage
     */
    public String getVendorHomePage() {
        return vendorHomePage;
    }

    /**
     * @return the version
     */
    public int[] getVersion() {
        return version;
    }

    /**
     * @return the supportContact
     */
    public String getSupportContact() {
        return supportContact;
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @return the restartRequired
     */
    public boolean isRestartRequired() {
        return restartRequired;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresRestart() {
        return restartRequired;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public int[] getLowestSupportedVersion() {
        return lowestSupportedVersion;
    }

    public String getMessageBeforeInstall() {
        return messageBeforeInstall;
    }

    public String getMessageAfterInstall() {
        return messageAfterInstall;
    }

    public String getVersionString() {
        return getVersionString(version);
    }

    public static String getVersionString(int version[]) {
        String s = "";
        if (version != null) {
            for (int v : version) {
                s += v + ".";
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        return s;
    }

    /**
     * @return the configurable
     */
    public boolean isConfigurable() {
        return configurable;
    }

    public void checkDependencies(PluginManager manager) throws DependencyMissingException {
    }

}
