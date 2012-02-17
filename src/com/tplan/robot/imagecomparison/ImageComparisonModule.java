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

import com.tplan.robot.scripting.ScriptingContext;
import java.awt.*;

/**
 * <p>This class declares methods of image comparison used by the {@doc.cmd CompareTo}, {@doc.cmd Screenshot}
 * and {@doc.cmd WaitFor} commands of the {@product.name} scripting language. You may implement this interface
 * to create your own image comparison algorithm and plug it into the application. The interface also allows to create
 * a method of analysis of the desktop image.</p>
 *
 * <p>The following example implements a dummy comparison method. It also
 * implements the {@link com.tplan.robot.plugin.Plugin} interface so that it can be
 * plugged in to {@product.name} through {@link com.tplan.robot.plugin.PluginManager}.
 *
 * <blockquote>
 * <pre>
import {@product.package}.imagecomparison.ImageComparisonModule;
import {@product.package}.plugin.*;
import {@product.package}.scripting.ScriptingContext;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DummyComparisonModule implements ImageComparisonModule, Plugin {

     // Base image. Not really used in this dummy module.
     private Image baseImage;

     // Name of the method. It will be used as an identificator in the scripting language.
     public String getMethodName() {
         return "dummy";
     }

     // Method description
     public String getMethodDescription() {
         return "This is a dummy module which doesn't perform any comparison and always returns 100% match.";
     }

     public float compare(Image image, Rectangle rectangle, Image image1, String methodParams, ScriptingContext repository, float passRate) {
         // This is just a dummy module which always returns 1, i.e. 100% match.
         // Implement your own image comparison code here.
         return 1.0f;
     }

     // Set the base image. It is just a convenience method used for multiple comparisons against one image.
     public void setBaseImage(Image img) {
         baseImage = img;
     }

     // Compare an image with the base image
     public float compareToBaseImage(Image image, Rectangle area, String methodParams, ScriptingContext repository, float passRate) {
         return compare(baseImage, area, image, methodParams, repository, passRate);
     }

     // We are doing image comparison, not just desktop analysis
     public boolean isSecondImageRequired() {
         return true;
     }

     // No custom method params are supported
     public boolean isMethodParamsSupported() {
         return false;
     }

     // Plugin method - get plugin code (identifier).
    public String getCode() {
        return "dummy";
    }

     // Plugin method - get plugin display name.
    public String getDisplayName() {
        return "Dummy image comparison module";
    }

     // Plugin method - get detailed plugin description.
    public String getDescription() {
        return "This is my dummy image comparison module. It does nothing and always returns 1.";
    }

    // Plugin method - get name of the vendor who provides this plugin.
    public String getVendorName() {
        return "John Doe";
    }

    // Plugin method - get vendor home page URL.
    public String getVendorHomePage() {
        return null;
    }

    // Plugin method - get vendor support contact.
    public String getSupportContact() {
        return null;
    }

    // Plugin method - get plugin version. We return 1.0
    public int[] getVersion() {
        return new int[] { 1, 0};
    }

    // Plugin method - get unique name which identifies one particular plugin accross all versions and releases.
    public String getUniqueId() {
        return "dummy image comparison method by John Doe";
    }

    // Plugin method - get release date. We return January 1, 2009.
    public Date getDate() {
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2009, 1, 1);
        return cal.getTime();
    }

    // Plugin method - get the implemented inteface. We return ImageComparisonModule.class
    public Class getImplementedInterface() {
        return ImageComparisonModule.class;
    }

    // Plugin method - does the application have to be restarted after plugin installation?
    public boolean requiresRestart() {
        return false;
    }

    // Plugin method - get lowest supported version of this framework. We return 2.0.
    public int[] getLowestSupportedVersion() {
        return new int[] { 2 };
    }

    // Plugin method - message to be displayed before plugin installation.
    public String getMessageBeforeInstall() {
        return null;
    }

    // Plugin method - message to be displayed after plugin installation.
    public String getMessageAfterInstall() {
        return null;
    }

    // Plugin method - check dependencies (none in this case).
    public void checkDependencies(PluginManager manager) throws DependencyMissingException {
    }
}
 * </pre>
 * </blockquote>
 *
 * <strong>NOTE:</strong> To compile this dummy module you need to place the application JAR file
 * to your Java class path.
 *
 * <p>Once you compile your comparison module to the Java <code>.class</code> format,
 * take advantage of the {@link com.tplan.robot.gui.plugin.PluginDialog Plugin Manager Dialog} to
 * plug the new module to the application. To plug in the code from a Java program
 * get instance of the {@link com.tplan.robot.plugin.PluginManager Plugin Manager} and
 * add call the {@link com.tplan.robot.plugin.PluginManager#installPlugin(java.lang.String, java.lang.String, boolean, boolean)}
 * method. Another option is to add your new module to the plugin list XML file manually.
 * See the {@link com.tplan.robot.plugin.PluginManager Plugin Manager} documentation
 * for details.</p>
 *
 * <p>The configuration is now complete. You can start the tool and verify availability of your module.
 * If you set your module as the default one, all calls of the <code>CompareTo</code>, <code>Screenshot</code>
 * and <code>WaitFor update</code> commands will by default perform image comparisons using your module. If you didn't
 * modify the default method setting, you can still explicitly specify your custom method using the <code>method</code>
 * parameter. To use your dummy comparison module create commands like
 * <code>CompareTo &lt;image file&gt; method=dummy</code>, <code>Screenshot &lt;image file&gt; method=dummy</code>,
 *  <code>Waitfor match method=dummy</code> or <code>Waitfor mismatch method=dummy</code>.
 * @product.signature
 */
public interface ImageComparisonModule {
    
    /**
     * This is a key used by the <i>search</i> image comparison module to save
     * list of coordinates to the repository table.
     */
    static final String SEARCH_COORD_LIST = "SEARCH_COORD_LIST";
    
    /**
     * Get the method name. It will be used as an identificator of the image comparison module in commands of the 
     * scripting language (parameter <code>module</code> of the <code>'CompareTo'</code> and <code>'Waitfor update'</code>
     * commands) and in default comparison method configuration (user config parameter
     * <code>CompareToCommand.defaultComparisonModule</code>).
     *
     * <p>Method names are not case sensitive. Be sure not to specify a name which is reserved by
     * the tool or already used by another method. Version 1.3 reserves two names, <code>'default'</code>
     * and <code>search</code>. You can't override these names.
     *
     * @return name of the module (image comparison method).
     */
    public String getMethodName();
    
    /**
     * Get the method description. Though this method is not used in versions 1.2 and lower, future versions may provide a GUI
     * method of adding custom comparison method which will take advantage of the method description.
     *
     * @return Description of the image comparison method (algorithm).
     */
    public String getMethodDescription();
    
    /**
     * Set the base image.
     *
     * <p>This is just a convenience method used by the <code>'Waitfor match'</code> command to perform repeated image
     * comparisons against one image.
     *
     * <p>The default image comparison algorithm is based on color histograms. When you call this method,
     * a histogram is calculated for the base image and cached for future repeated comparisons performed through the
     * <code>compareToBaseImage()</code> method calls. As calculation of a color histograms is quite a time expensive
     * operation, this approach allows to achieve higher performance.
     *
     * <p>See the <code>compareToBaseImage()</code> method doc for more info.
     *
     * @param img
     */
    public void setBaseImage(Image img);
    
    /**
     * Compare the desktop image to another image and return a number indicating how much they match.
     *
     * @param desktopImage desktop image.
     * @param area a rectangle area of the remote desktop to be used for image comparison. If the argument is null,
     * the whole remote desktop image will be used.
     * @param image another image to be compared to the desktop image.
     * @param methodParams method parameters passed by the <code>'methodparams'</code> parameter of
     * the <code>'CompareTo'</code> and <code>'Waitfor update'</code> commands. You can use this to pass custom
     * parameters from the script to this module.
     * @param repository a Map with execution context. Note that most of the objects contain there are not public
     * and the parameter is specified here to allow compatibility among the custom and internal comparsion methods.
     * @param passRate pass rate in form of a float value between 0 and 1. This user input value indicates
     * the lowest result which will be considered as match ("passed"). It is in fact the number specified by the
     * 'passrate' parameter of the 
     * commands <a href=http://vncrobot.com/docs/v1.3/scripting/commref.html#screenshot>Screenshot</a>,
     * <a href=http://vncrobot.com/docs/v1.3/scripting/commref.html#compareto>Compareto</a> and
     * <a href=http://vncrobot.com/docs/v1.3/scripting/commref.html#waitfor>Waitfor match/mismatch</a>.
     * @return a number between 0 and 1. While a value of 0 indicates 0% match (images do not match at all),
     * 1 indicates a 100% match (images are equal).
     */
    public float compare(Image desktopImage, Rectangle area, Image image, String methodParams, ScriptingContext repository, float passRate);
    
    /**
     * Compare the desktop image to the base image and return a number indicating how much they match.
     *
     * <p>This is just a convenience method used by the <code>'Waitfor match'</code> command to perform repeated image
     * comparisons against one image.
     *
     * <p>The default image comparison algorithm is based on color histograms. A call of this method performs
     * image comparison against precalculated base image histogram rather that against the base image pixels.
     * As calculation of a color histograms is quite a time expensive operation, this approach
     * allows to achieve higher performance.
     *
     * <p>Any custom implementation of this interface may or may not take advantage of this approach. If you don't want
     * to diferentiate between repeated and one time comparison calls, define the <code>setBaseImage()</code> and
     * <code>compareToBaseImage()</code> methods as follows:
     *
     * <blockquote>
     * <pre>
     *     // Base image
     *     private Image baseImage;
     *
     *     // Set the base image
     *     public void setBaseImage(Image img) {
     *         baseImage = img;
     *     }
     *
     *     // Compare an image with the base image
     *     public float compareToBaseImage(Image desktopImage) {
     *         return compare(desktopImage, baseImage);
     *     }
     * </blockquote>
     * </pre>
     *
     * @param desktopImage an image to be compared to the base image.
     * @param area a rectangle area of the remote desktop to be used for image comparison. If the argument is null,
     * the whole remote desktop image will be used.
     * @param methodParams method parameters passed by the <code>'methodparams'</code> parameter of
     * the <code>'CompareTo'</code> or <code>'Waitfor update'</code> command.
     * @param repository a Map with execution context. Note that most of the objects contain there are not public
     * and the parameter is specified here to allow compatibility among the custom and internal comparsion methods.
     * @param passRate pass rate in form of a float value between 0 and 1. This user input value indicates
     * the lowest result which will be considered as match ("passed"). It is in fact the number specified by the
     * 'passrate' parameter of the commands <a href=http://vncrobot.com/docs/v1.3/scripting/commref.html#screenshot>Screenshot</a>,
     * <a href=http://vncrobot.com/docs/v1.3/scripting/commref.html#compareto>Compareto</a> and
     * <a href=http://vncrobot.com/docs/v1.3/scripting/commref.html#waitfor>Waitfor match/mismatch</a>.
     * @return a number between 0 and 1. While a value of 0 indicates 0% match (images do not match at all),
     * 1 indicates a 100% match (images are equal).
     */
    public float compareToBaseImage(Image desktopImage, Rectangle area, String methodParams, ScriptingContext repository, float passRate);
    
    /**
     * Determine whether we are comparing two images or just analyze the desktop image.
     *
     * <p>If this method returns true, commands <code>CompareTo</code> and <code>WaitFor update</code>
     * of the scripting language will require presence of the <code>'template'</code> parameter which
     * specifies an image located in the filesystem to be compared to the current desktop.
     *
     * <p>If you wish just to analyze the desktop image instead of comparing it to another one, implement this method
     * to return false. The tool will not validate whether an image from the file system is
     * passed correctly. Note that commands will supply null instead of an image in all arguments
     * corresponding to the template image, i.e. expect method calls like <code>compare(desktopImage, null)</code> and
     * <code>setBaseImage(null)</code>.
     *
     * @return true for comparison mode, false for image analysis.
     */
    public boolean isSecondImageRequired();
    
    /**
     * Define whether this module supports some custom parameters which can be passed via the <code>methodparams</code>
     * parameter of the scripting language.
     *
     * <p>When this method returns false, the tool will report a syntax error when this module gets used in a command
     * together with the <code>methodparams</code> parameter.
     *
     * <p>When this method returns true, commands using this module may take advantage of the <code>methodparams</code>
     * parameter to pass custom parameters which are not defined by the scripting language.
     *
     * <p>The default image comparison algorithm doesn't provide any custom parameters.
     *
     * @return true if the module supports customized parameters, false if not.
     */
    public boolean isMethodParamsSupported();
    
}
