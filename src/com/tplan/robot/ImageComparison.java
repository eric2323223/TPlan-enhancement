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

import com.tplan.robot.imagecomparison.ImageComparisonModule;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.scripting.TokenParserImpl;
import com.tplan.robot.imagecomparison.ImageComparisonModuleFactory;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.ScriptingContextImpl;
import com.tplan.robot.l10n.LocalizationSupport;
import com.tplan.robot.util.Utils;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;

/**
 * This class defines a CLI allowing to perform image comparison using algorithms
 * embedded in the {@product.name} code. See {@doc.clicmp} for more information.
 *
 * @product.signature
 */
public class ImageComparison implements ImageObserver {
    
    /**
     * Default constructor.
     */
    public ImageComparison() {
    }

    /**
     * Entry point for CLI image comparison interface. The method parses CLI options and
     * compares the specified images.
     * @param args entry command line arguments.
     */
    public static void main(String[] args) {
        ImageObserver imgObserver = new ImageComparison();
        
        ResourceBundle resourceBundle = LocalizationSupport.getResourceBundle(
                Locale.getDefault(), ApplicationSupport.class, ApplicationSupport.APPLICATION_RESOURCE_BUNDLE_PREFIX);
        
        if (args.length < 2 || args[0].length() == 0 || args[1].length() == 0) {
            System.out.println(MessageFormat.format(resourceBundle.getString("com.tplan.robot.comparisonCli.help"), ApplicationSupport.APPLICATION_NAME));
            return;
        }
        
        Locale.setDefault(Locale.ENGLISH);
        
        try {
            boolean silent = false;
            boolean concise = false;
            String method = null;
            float passRate = -1;
            int maxHits = -1;
            
            // We have two images, a bigger 'source' and a smaller 'template'.
            // Source image
            Image source = null;
            try {
                source = ImageIO.read(new File(args[0]));
            } catch (IOException ioe) {
                Object temp[] = { (new File(args[0])).getAbsolutePath() };
                System.out.println(MessageFormat.format(
                        resourceBundle.getString("com.tplan.robot.comparisonCli.errCannotReadSourceImg"), temp));
                System.exit(2);
            }
            Rectangle cmpArea = new Rectangle(source.getWidth(imgObserver), source.getHeight(imgObserver));
            
            // Pattern source
            Image pattern = null; 
            try {
                pattern = ImageIO.read(new File(args[1]));
            } catch (IOException ioe) {
                Object temp[] = { (new File(args[1])).getAbsolutePath() };
                System.out.println(MessageFormat.format(
                        resourceBundle.getString("com.tplan.robot.comparisonCli.errCannotReadTemplateImg"), temp));
                System.exit(3);
            }
            Rectangle patternRect = new Rectangle(pattern.getWidth(imgObserver), pattern.getHeight(imgObserver));
            
            // Process the optional CLI parameters.
            // Allowed params are:
            //   -m <method>    ... Module name - 'default', 'search' or name of a custom module
            //   -h <max_hits>  ... Maximum number of hits (default is 100)
            //   -p <pass_rate> ... Pass rate (default is 1)
            //   -r <rectangle> ... Search area (default value is the whole image)
            List opts = new ArrayList(Arrays.asList(args));
            String arg = null, val = null, errMessage = "";
            try {
                opts.remove(0);   // Remove the source image option
                opts.remove(0);   // Remove the pattern image option
                
                // Process optional params
                while (opts.size() > 0) {
                    arg = opts.remove(0).toString().trim();
                    
                    if (arg.equals("-h")) {   // Max hits specified
                        errMessage = resourceBundle.getString("com.tplan.robot.comparisonCli.errInvalidSearchHits");
                        maxHits = Integer.parseInt(opts.remove(0).toString());
                        if (maxHits <= 0) {
                            throw new IllegalArgumentException();
                        }
                    } else if (arg.equals("-m")) {   // Module name specified
                        errMessage = resourceBundle.getString("com.tplan.robot.comparisonCli.errInvalidModule");
                        method = opts.remove(0).toString();
                    } else if (arg.equals("-p")) {   // Pass rate specified
                        errMessage = resourceBundle.getString("com.tplan.robot.comparisonCli.errInvalidPassRate");
                        passRate = Float.parseFloat(opts.remove(0).toString());
                        if (passRate < 0 || passRate > 100) {
                            throw new IllegalArgumentException();
                        }
                    } else if (arg.equals("-r")) {   // Search area specified
                        errMessage = resourceBundle.getString("com.tplan.robot.comparisonCli.errInvalidRectangle");
                        try {
                            TokenParser parser = new TokenParserImpl();
                            Rectangle r = parser.parseRectangle(opts.remove(0).toString(), "-r");
                            if (r.width <= 0) {
                                r.width = cmpArea.width - r.x;
                            }
                            if (r.height <= 0) {
                                r.height = cmpArea.height - r.y;
                            }
                            cmpArea = r;
                        } catch (SyntaxErrorException e) {
                            throw new IllegalArgumentException();
                        }
                    } else if (arg.equals("-s")) {   // Verbose mode off
                        silent = true;
                    } else if (arg.equals("-c")) {   // Concise mode on
                        concise = true;
                    } else if (arg.equals("")) {   // Skip empty args
                        //                       opts.remove(0);
                    } else {
                        Object temp[] = { arg };
                        errMessage = MessageFormat.format(resourceBundle.getString("com.tplan.robot.comparisonCli.errUnknownOption"), temp);
                        throw new IllegalArgumentException();
                    }
                }
            } catch (Exception ex) {
                Object temp[] = { errMessage };
                System.out.println(MessageFormat.format(resourceBundle.getString("com.tplan.robot.comparisonCli.errSyntaxError"), temp));
                System.out.println(MessageFormat.format(resourceBundle.getString("com.tplan.robot.comparisonCli.help"), ApplicationSupport.APPLICATION_NAME));
                System.exit(4);
            }

            ApplicationSupport.loadConfiguration();
            UserConfiguration cfg = UserConfiguration.getInstance();
            
            // Load the default pass rate if it is not specified through -p
            if (passRate < 0) {
                if ("search".equalsIgnoreCase(method)) {
                    passRate = cfg.getDouble("CompareToCommand.defaultSearchPassRate").floatValue();
                } else {
                    passRate = cfg.getDouble("CompareToCommand.defaultPassRate").floatValue();
                }
            }
            
            // Custom maximum of hits is defined
            if (maxHits >= 1) {
                cfg.setInteger(ConfigurationKeys.COMPARETO_MAX_SEARCH_HITS, new Integer(maxHits));
            } else {
                maxHits = cfg.getInteger(ConfigurationKeys.COMPARETO_MAX_SEARCH_HITS).intValue();
            }
            
            // Load the image comparison module
            ImageComparisonModule module = null;
            try {
                module = ImageComparisonModuleFactory.getInstance().getModule(method);
            } catch (Exception ex) {
               List<String> l = ImageComparisonModuleFactory.getInstance().getAvailableModules();
                String list = "";
                for (String name : l) {
                    module = ImageComparisonModuleFactory.getInstance().getModule(name);
                    list += name + " - " + module.getMethodDescription() + "\n";
                }
                System.out.println(resourceBundle.getString("com.tplan.robot.comparisonCli.errInvalidModule")+list);
                System.exit(4);
            }
            
            if (module.getMethodName().equalsIgnoreCase("search") &&
                    (patternRect.width > source.getWidth(imgObserver) || patternRect.height > source.getHeight(imgObserver))) {
                System.out.println(resourceBundle.getString("com.tplan.robot.comparisonCli.warningIncorrectImageOrder"));
            }
            
            boolean isSearch = module.getMethodName().equalsIgnoreCase("search");
            
            // Report the comparison params if in silent mode
            if (!silent && !concise) {
                Object temp[] = { 
                    args[0], 
                    new Integer(source.getWidth(imgObserver)), 
                    new Integer(source.getHeight(imgObserver)),
                    args[1],
                    new Integer(patternRect.width),
                    new Integer(patternRect.height),
                    module.getMethodName(),
                    module.getMethodDescription(),
                    cmpArea,
                    new Float(passRate)
                };
                System.out.println(
                        MessageFormat.format(resourceBundle.getString("com.tplan.robot.comparisonCli.verboseParamsGeneric"), temp));
                if (isSearch) {
                    temp = new Object[] { new Integer(maxHits) };
                    System.out.println(MessageFormat.format(resourceBundle.getString("com.tplan.robot.comparisonCli.verboseParamsSearch"), temp));
                }
            }
            
            // Execute the comparison
            ScriptingContext repository = new ScriptingContextImpl();
            long time = System.currentTimeMillis();
            float result = module.compare(source, cmpArea, pattern, null, repository, passRate/100);
            time = System.currentTimeMillis() - time;
            
            // Report the results if in silent mode
            final boolean pass = (100*result >= passRate);
            
            // Print out result details only if the silent mode is on.
            if (!silent) {
                if (!concise) {
                    Object temp[] = { 
                        new Float(100*result),
                        pass ? resourceBundle.getString("com.tplan.robot.comparisonCli.pass") 
                          : resourceBundle.getString("com.tplan.robot.comparisonCli.fail"),
                        Utils.getTimePeriodForDisplay(time, false, true)
                    };
                    System.out.println(MessageFormat.format(resourceBundle.getString("com.tplan.robot.comparisonCli.result"), temp));
                }
                if (pass && isSearch) {
                    Map vars = repository.getVariables();
                    if (vars != null) {
                        Number cnt = (Number) vars.get("_SEARCH_MATCH_COUNT");
                        if (cnt != null) {
                            if (!concise) {
                                Object temp[] = { cnt };
                                System.out.println(MessageFormat.format(resourceBundle.getString("com.tplan.robot.comparisonCli.searchHitsCnt"), temp));
                            }
                            String x, y;
                            for (int i = 1; i <= cnt.intValue(); i++) {
                                x = vars.get("_SEARCH_X_" + i).toString();
                                y = vars.get("_SEARCH_Y_" + i).toString();
                                if (concise) {
                                    System.out.println("["+x+","+y+"]");
                                } else {
                                    Object temp[] = { new Integer(i), new Integer(x), new Integer(y) };
                                    System.out.println(MessageFormat.format(resourceBundle.getString("com.tplan.robot.comparisonCli.singleMatch"), temp));
                                }
                            }
                            if (!concise) {
                                System.out.println();
                            }
                        }
                    }
                }
            }
            
            // Return 0 if pass, 1 if fail
            System.exit(pass ? 0 : 1);
            
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(5);
        }
    }

    /**
     * Dummy implementation of the ImageObserver interface. The method always returns false.
     * @param img an image
     * @param infoflags flags
     * @param x x coordinate
     * @param y y coordinate
     * @param width width
     * @param height height
     * @return always returns false.
     */
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        return false;
    }
}
