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
package com.tplan.robot.imagecomparison;

import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.plugin.PluginFactory;

import java.util.List;

/**
 * <p>Image comparison module factory allows to access image comparison
 * algorithms provided by this application. As the factory is built on top of
 * the plugin framework, it is possible to plug in additional modules to the
 * application through the {@link com.tplan.robot.plugin.Plugin} interface.</p>
 *
 * <p>An image comparison module is a Java class or set of classes implementing
 * the {@link com.tplan.robot.imagecomparison.ImageComparisonModule ImageComparisonModule}.
 * The module allows to compare two images or eventually analyze one single base image.
 * The first image argument is typically called <i>base image</i>. The second
 * image is often called <i>template image</i> or <i>pattern image</i>.</p>
 *
 * <p>To get an instance of image comparison module factory use static method
 * {@link #getInstance()}. Then take advantage of the {@link #getModule(java.lang.String)} method
 * to obtan a module by its name.</p>
 *
 * <p>{@product.name} provides by default two image comparison modules named <i>"default"</i> (constant {@link #MODULE_DEFAULT})
 * and <i>"search"</i> (constant {@link #MODULE_SEARCH}). The following paragraphs provide just a general
 * description. See the {@doc.comparison} document for details on the built-in comparison
 * algorithms.</p>
 *
 * <p><strong><u>Image Comparison Module "default"</u></strong><br>
 * The <i>default</i> module is based on comparison of color histograms. It returns
 * a float number between 0 and 1 representing a ratio of how much two images
 * match in terms of having the same amounts of
 * pixels of each color. This module is typically used in {@product.name} to verify
 * larger remote screen changes, e.g. making sure that an application window
 * displayed on the screen correctly or that a web browser running on the remote
 * desktop displayed a correct static web page.</p>
 *
 * <p><strong><u>Image Comparison Module "search"</u></strong><br>
 * The <i>search</i> module performs pixel by pixel search of a smaller image
 * pattern, typically an icon or image of a component. It is either possible to
 * search for an exact 100% match or specify a pass rate between 0 and 1 which
 * defines ratio of allowed different pixels. </p>
 *
 * <p>The <i>search</i> module is able to handle template images with transparency.
 * As transparent pixels are not
 * compared and they are always counted as matching, it allows users to add
 * transparency to their template images to implement more robust search of
 * patterns in changing environments, e.g. where a background color change is expected.</p>
 *
 * <p>The <i>search</i> module returns either 1 when at least one match is found or
 * 0 (zero) otherwise. List of match coordinates ({@link java.awt.Point}
 * instances) is stored to the <code>repository</code> argument table with key
 * defined by the {@link ImageComparisonModule#SEARCH_COORD_LIST}
 * constant.</code>
 *
 * <p><strong><u>Example</u></strong><br>
 * The following example illustrates a typical usage of the image comparison
 * API:
 *
 * <blockquote>
 * <pre>
import com.tplan.robot.imagecomparison.ImageComparisonModule;
import com.tplan.robot.imagecomparison.ImageComparisonModuleFactory;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.ScriptingContextImpl;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.Point;

 public class ImageComparisonExample {

     public static void main(String[] args) throws IOException {
         ImageComparisonModuleFactory factory =
                 ImageComparisonModuleFactory.getInstance();
         ScriptingContext context = new ScriptingContextImpl();

         // --------------------------------------------------------------------
         //  EXAMPLE #1:  Image comparison using the "default" module.
         //  We will compare two images called "desktop1.png" and "desktop2.png".
         // --------------------------------------------------------------------
         ImageComparisonModule module1 = factory.getModule("default");
         float comparisonResult1 = module1.compare(
                 ImageIO.read(new File("desktop1.png")), // Remote desktop image (base image)
                 null,                                   // Rectangle of the remote desktop image to search
                 ImageIO.read(new File("desktop2.png")), // Pattern image (template image)
                 null,                                   // Method params (null if no custom params are needed)
                 context,                                // Context map for storing of test results and statistics
                 1.0f                                    // Required pass rate (1.0f == 100%); not used in "default" module
            );
         System.out.println("Image comparison using default histogram method finished; result == "+comparisonResult1);

         // --------------------------------------------------------------------
         //  EXAMPLE #2:  Image comparison using the "search" module.
         //  We will look for image "icon.png" in base image called "desktop1.png".
         // --------------------------------------------------------------------
         ImageComparisonModule module2 = factory.getModule("search");
         float comparisonResult2 = module2.compare(
                 ImageIO.read(new File("desktop1.png")), // Remote desktop image (base image)
                 null,                                   // Rectangle of the remote desktop image to search
                 ImageIO.read(new File("icon.png")),     // Pattern image (template image)
                 null,                                   // Method params (null if no custom params are needed)
                 context,                                // Context map for storing of test results and statistics
                 1.0f                                    // Required image comparison pass rate; 1.0f == 100%
            );
         System.out.println("Image search finished; result == "+comparisonResult2);

         // Coordinates of image search are saved as a map of numbers
         List<Point> coords = context.getSearchHits();

         // Iterate through the list of coordinates and print them out
         if (coords != null) {
             int i = 1;
             for (Point p : coords) {
                 System.out.println("Match #"+(i++)+": ["+p.x+","+p.y+"]");
             }
         }
     }
 }
 * </pre>
 * </blockquote>
 * @product.signature
 */
public class ImageComparisonModuleFactory extends PluginFactory
        implements ConfigurationKeys {  //ConfigurationChangeListener,

    private static ImageComparisonModuleFactory instance;

    /**
     * Name of the built-in "default" image comparison module.
     */
    public static final String MODULE_DEFAULT = "default";
    /**
     * Name of the built-in "search" image comparison module.
     */
    public static final String MODULE_SEARCH = "search";

    private ImageComparisonModuleFactory() {
    }

    /**
     * Get shared instance of this factory.
     * @return shared instance of this factory.
     */
    public static ImageComparisonModuleFactory getInstance() {
        if (instance == null) {
            instance = new ImageComparisonModuleFactory();
        }
        return instance;
    }

    /**
     * Get the default image comparison module, typically the one called "default".
     * Do not however rely on it as it may depend on user preferences. To load
     * the "default" module rather use <code>getModule("default")</code>.
     *
     * @return default image comparison module.
     */
    public ImageComparisonModule getDefaultModule() {
        String defaultModule = UserConfiguration.getInstance().getString("CompareToCommand.defaultComparisonModule");
        return (ImageComparisonModule) getPluginByCode(defaultModule, ImageComparisonModule.class);
    }

    /**
     * Get a module by name. As {@product.name} supports just two modules, the
     * argument can be either "default" or "search". Additional modules may be
     * available where custom modules were implemented through the
     * {@link com.tplan.robot.imagecomparison.ImageComparisonModule}
     * interface and plugged into the application.
     *
     * @param moduleName module name.
     * @return image comparison module with the given name, or null if not found.
     */
    public ImageComparisonModule getModule(String moduleName) {
        if (moduleName == null || moduleName.length() == 0) {
            return getDefaultModule();
        }
        return (ImageComparisonModule) getPluginByCode(moduleName, ImageComparisonModule.class);
    }

    /**
     * Get a list of available image comparison module names.
     *
     * @return map of available image comparison module names.
     */
    public List<String> getAvailableModules() {
        return getAvailablePluginCodes(ImageComparisonModule.class);
    }
}
