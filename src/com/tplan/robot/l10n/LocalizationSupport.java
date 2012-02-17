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
package com.tplan.robot.l10n;

import com.tplan.robot.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * A framework for plugging, discovery and loading of localized resource bundles.
 * @product.signature
 */
public class LocalizationSupport {

    private static boolean debug = System.getProperty("loc") != null;
    private static Locale lastLoadedLocale = null;

    /** Creates a new instance of LocalizationSupport */
    public LocalizationSupport() {
    }

    public static ResourceBundle translate(String providerCode, Locale source, Locale target, Class cl, String baseName) {
        throw new UnsupportedOperationException("Not yet implemented - reserved for future use.");
//        TranslationProvider provider = TranslationProviderFactory.getInstance().getTranslationProvider(providerCode);
//        if (provider == null) {
//            return null;
//        }
//        ResourceBundle res = provider.translate(getResourceBundle(source, cl, baseName), source, target);
//        FileOutputStream fout = null;
//        try {
//            File f = new File(Utils.getInstallPath() + File.separator + baseName + "_" + target.getLanguage() + ".properties");
//            fout = new FileOutputStream(f);
//            Properties props = new Properties();
//            for (String key : res.keySet()) {
//                props.put(key, res.getString(key));
//            }
//            props.store(fout, baseName);
////            System.out.println("Saved "+f.getAbsolutePath());
//            return res;
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        } finally {
//            try {
//                if (fout != null) {
//                    fout.close();
//                }
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//        }
//        return res;
    }

    public static CustomPropertyResourceBundle getResourceBundle(Locale locale, Class cl, String baseName) {
        CustomPropertyResourceBundle resourceBundle = new CustomPropertyResourceBundle();

        // If the loading class is null, default to this one
        if (cl == null) {
            cl = LocalizationSupport.class;
        }

        // The following block of code parses the path of the application JAR file
        // and tries to find any external resource bundle in the JAR file directory
        // which matches the current locale. Note that this piece of code works
        // only when we are running from the JAR file.
        try {

            // Allways load the default resource so that we can eventually merge
            // it with the custom one. This is necessary because the default
            // implementation of ResourceBundle throws an exception every time a
            // key is not found and it would break the tool by users who customize it
            try {
                String name = baseName + "_" + locale.getLanguage();
                if (locale.getCountry() != null && !"".equals(locale.getCountry())) {
                    name += "_" + locale.getCountry() + ".properties";
                    InputStream inStream = cl.getResourceAsStream(name);
                    resourceBundle.load(inStream);
                } else {
                    name += ".properties";
                    InputStream inStream = cl.getResourceAsStream(name);
                    resourceBundle.load(inStream);
                }
                lastLoadedLocale = locale;
            } catch (Exception ex) {
                if (debug) {
                    System.out.println("Bundle for locale '" + locale + "' not found in the JAR file, loading English one");
                }
                InputStream inStream = cl.getResourceAsStream(baseName + "_en.properties");
                resourceBundle.load(inStream);
                lastLoadedLocale = Locale.ENGLISH;
            }

            String path = getBundleDirectory();
            if (path != null) {
                File parent = new File(path);
                File f = new File(parent, baseName + "_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant() + ".properties");
                if (!f.exists() || !f.canRead()) {
                    f = new File(parent, baseName + "_" + locale.getLanguage() +
                            "_" + locale.getCountry() + ".properties");
                }
                if (!f.exists() || !f.canRead()) {
                    f = new File(parent, baseName + "_" + locale.getLanguage() + ".properties");
                }
                if (f.exists() || f.canRead()) {
                    // Then load the custom bundle..
                    // Those messages which are not present in the custom bundle
                    // will remain intact.
                    if (debug) {
                        System.out.println("External bundle found: " + f.getAbsolutePath());
                    }
                    InputStream inStream = new FileInputStream(f);
                    resourceBundle.load(inStream);
                    lastLoadedLocale = locale;
                } else {
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return resourceBundle;
    }

    public static List<Locale> getAvailableResourceBundles(String baseName, Class cl) {

        // First look outside of the JAR file
        File jar = Utils.getJarFile();

        // We put all bundles found into a map so that external ones
        // automatically rewrite the internal ones
        Map t = new HashMap();
        URL url = getClassURL(cl);

        // Now look inside the JAR file.
        if (jar != null) {

            try {
                ZipFile jarFile = new ZipFile(jar);
                Enumeration e = jarFile.entries();
                ZipEntry entry;
                Locale loc;
                while (e.hasMoreElements()) {
                    entry = (ZipEntry) e.nextElement();
                    loc = parseLocaleFromFileName(entry.getName(), baseName);
                    if (loc != null) {
                        String id = loc.getLanguage() + (loc.getCountry() == null ? "" : loc.getCountry());
                        t.put(id, loc);
                    }
                }

            } catch (ZipException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                File f = new File(url.toURI());
                File[] list = f.getParentFile().listFiles();
//                System.out.println(url + "\n" + f.getParentFile() + "\n" + list);
                String name;
                Locale loc;
                for (int i = 0; i < list.length; i++) {
                    name = list[i].getName();
                    loc = parseLocaleFromFileName(name, baseName);
                    if (loc != null) {
                        String id = loc.getLanguage() + (loc.getCountry() == null ? "" : loc.getCountry());
                        t.put(id, loc);
                    }
                }
            } catch (URISyntaxException ex) {
                ex.printStackTrace();
            }
        }

        // Now look outside of the JAR file
        String path = Utils.getInstallPath();
        File f = new File(path);
        File list[] = f.listFiles();
        String name;
        Locale loc;
        for (int i = 0; i < list.length; i++) {
            name = list[i].getName();
            loc = parseLocaleFromFileName(name, baseName);
            if (loc != null) {
                String id = loc.getLanguage() + (loc.getCountry() == null ? "" : loc.getCountry());
                t.put(id, loc);
            }
        }

        // Sort the locales
        String array[] = new String[t.size()];
        Iterator e = t.keySet().iterator();
        int i = 0;
        while (e.hasNext()) {
            array[i++] = e.next().toString();
        }
        Arrays.sort(array);

        List v = new ArrayList();
        for (i = 0; i < array.length; i++) {
            v.add(t.get(array[i]));
        }
        return v;
    }

    private static URL getClassURL(Class cl) {
        String cn = cl.getName().replace(".", "/") + ".class";
        return cl.getClassLoader().getResource(cn);
    }

    private static String getBundleDirectory() {
        File f = Utils.getJarFile();
        if (f != null) {
            return f.getParent();
        }
        return System.getProperty("user.dir");
    }

    private static Locale parseLocaleFromFileName(String fileName, String baseName) {
        int l = fileName.length();
        int l2 = ".properties".length();
        if (fileName.matches(".*" + baseName + "_[a-z][a-z].properties")) {
            String lang = fileName.substring(l - l2 - 2, l - l2);
            if (debug) {
                System.out.println("Found bundle " + fileName + ", locale " + lang);
            }
            return new Locale(lang);
        } else if (fileName.matches(".*" + baseName + "_[a-z][a-z]_[A-Z][A-Z].properties")) {
            String lang = fileName.substring(l - l2 - 5, l - l2 - 3);
            String country = fileName.substring(l - l2 - 2, l - l2);
            if (debug) {
                System.out.println("Found bundle " + fileName + ", locale " + lang + "_" + country);
            }
            return new Locale(lang, country);
        }

        return null;
    }

    private static Locale getLoadedLocale() {
        return getLastLoadedLocale();
    }

    public static Locale getLastLoadedLocale() {
        return lastLoadedLocale;
    }
}
