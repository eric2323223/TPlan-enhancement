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
package com.tplan.robot.imagecomparison.search;

import com.sybase.robot.scripting.commands.impl.ImageUtil;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.util.Measurable;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.util.Stoppable;
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.util.Utils;
import com.tplan.robot.preferences.UserConfiguration;

import com.tplan.robot.imagecomparison.ImageComparisonModule;
import com.tplan.robot.imagecomparison.histogram.ImageHistogram;
import com.tplan.robot.plugin.DependencyMissingException;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptingContext;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Search image comparison module.
 * @product.signature
 */
public class SearchImageComparisonModule extends AbstractImagePattern
        implements ImageComparisonModule, ExtendedSearchCapabilities, ConfigurationKeys, Stoppable, Measurable, Plugin {

    /* An image consumer used to figure out image size. */
    private JLabel imgConsumer = new JLabel();
    protected int MAX_LOADED_PIXEL_ROWS = 200;
    /** Maximum number of occurences. */
    protected int MAX_HITS = 100;
    float offsetCount;
    final int histogramDelayInSearchCycles = 2000;
    private boolean enableHeuristics = true;
    /**
     * List of pixels which didn't match during the last search. When the
     * variable is null, tracking of different pixels gets switched off.
     */
    private List<Point> failedPixels;
    /** Pixels to be searched first (heuristics). */
    private int[] heurPixels;
    public long startTime;
    public long histTime;
    public long heurTime;
    public long endTime;
    private List<Rectangle> hits;

    public String getMethodName() {
        return "search";
    }

    public float getProgress() {
        return offsetCount > 0 ? (float) pxcnt / offsetCount : 0;
    }

    public String getMethodDescription() {
        return "Search for a template in an image using pixel comparison.";
    }

    public float compare(Image desktopImage, Rectangle area, Image image, String methodParams, ScriptingContext repository, float passRate) {
        startTime = System.nanoTime();

        // Load the template image pixels
//        long time = System.currentTimeMillis();
        setPixelsFromImage(image, new Rectangle(0, 0, image.getWidth(imgConsumer), image.getHeight(imgConsumer)));

//        System.out.println("setPixelsFromImage(): "+(System.currentTimeMillis()-time)+"ms");
//        time = System.currentTimeMillis();
        if (isEnableHeuristics()) {
            rebuildHeuristics(passRate);
        } else {
            resetHeuristics();
        }
        heurTime = System.nanoTime();

//        System.out.println("rebuildHeuristics(): "+(System.currentTimeMillis()-time)+"ms");
//        time = System.currentTimeMillis();
        float result = compareToBaseImage(desktopImage, area, methodParams, repository, passRate);
//        System.out.println("compareToBaseImage(): "+(System.currentTimeMillis()-time)+"ms");
        // This is a necessary cleanup
        pixels = null;
        resetHeuristics();
        endTime = System.nanoTime();

        return result;
    }

    public void setBaseImage(Image img) {
        // Load the template image pixels
        setPixelsFromImage(img, new Rectangle(0, 0, img.getWidth(imgConsumer), img.getHeight(imgConsumer)));
    }

    public float compareToBaseImage(Image desktopImage, Rectangle area, String methodParams, ScriptingContext repository, float passRate) {

        stop = false;
        pxcnt = (int) (histogramDelayInSearchCycles * passRate * passRate);
        Rectangle fullRect = new Rectangle(0, 0, desktopImage.getWidth(imgConsumer), desktopImage.getHeight(imgConsumer));
        if (area == null) {
            area = fullRect;
        } else {
            area = area.intersection(fullRect);
        }

        // Number of pixel rectangles which will be searched plus a couple of more
        // to reflect the time spent by creation of histogram. Used as 100% value for progress measuring.
        offsetCount = (area.width - r.width) * (area.height - r.height) + pxcnt;

        UserConfiguration cfg = (UserConfiguration) repository.get(ScriptingContext.CONTEXT_USER_CONFIGURATION);
        Integer max = cfg == null ? null : cfg.getInteger(COMPARETO_MAX_SEARCH_HITS);
        if (max != null) {
            MAX_HITS = max.intValue();
        }
        max = cfg == null ? null : cfg.getInteger(COMPARETO_MAX_LOADED_PIXEL_ROWS);
        if (max != null) {
            MAX_LOADED_PIXEL_ROWS = max.intValue();
        }

        int imgHeight = area.height;

        // Load the remote desktop image pixels
        Rectangle r = new Rectangle(area.x, area.y, area.width, Math.min(area.height, this.r.height + MAX_LOADED_PIXEL_ROWS));
        int pixels[] = null;
        List<Point> pts = new ArrayList<Point>();
        List tmp;
        Number index;
        Point pt;

        while (r.y <= (imgHeight + area.y) && r.height >= this.r.height && (MAX_HITS <= 0 || pts.size() < MAX_HITS)) {
            pixels = Utils.getPixels(desktopImage, r);
            tmp = findPattern(pixels, r, MAX_HITS, passRate);
            for (int i = 0; i < tmp.size() && (MAX_HITS <= 0 || pts.size() < MAX_HITS); i++) {
                index = (Number) tmp.get(i);
//                System.out.println("Match found at "+index);
                pt = getPointFromPixelIndex(index.intValue(), area.width, r.getLocation());
                if (!pts.contains(pt)) {
                    pts.add(pt);
                    //Eric: for verification
//                    ImageUtil.drawRectangle((BufferedImage)desktopImage, pt, this.r, new File("c:\\match_result.png"));
                    
                }
            }
            r.y += MAX_LOADED_PIXEL_ROWS;
            r.height = Math.min(imgHeight + area.y - r.y, r.height);
        }


        List matchPoints = pts;
        hits = new ArrayList();

        float result = 0.0f;

        repository.put(ScriptingContext.CONTEXT_IMAGE_SEARCH_POINT_LIST, pts);

        Map variables = repository.getVariables();
        Point p;
        String suffix;
        if (matchPoints.size() > 0) {
            for (int i = 0; i < matchPoints.size(); i++) {
                p = (Point) matchPoints.get(i);
                suffix = "_" + (i + 1);
                variables.put("_SEARCH_X" + suffix, new Integer(p.x));
                variables.put("_SEARCH_Y" + suffix, new Integer(p.y));
                hits.add(new Rectangle(p.x,p.y, r.width, r.height));
            }
            variables.put("_SEARCH_X", variables.get("_SEARCH_X_1"));
            variables.put("_SEARCH_Y", variables.get("_SEARCH_Y_1"));
            result = 1.0f;
        } else {
            variables.put("_SEARCH_X", new Integer(-1));
            variables.put("_SEARCH_Y", new Integer(-1));
        }
        variables.put("_SEARCH_MATCH_COUNT", new Integer(matchPoints.size()));

        ScriptManager sh = repository.getScriptManager();
        if (sh != null) {
            sh.fireScriptEvent(new ScriptEvent(this, null, repository, ScriptEvent.SCRIPT_VARIABLES_UPDATED));
        }
        return result;
    }

    public boolean isSecondImageRequired() {
        return true;
    }

    public boolean isMethodParamsSupported() {
        return false;
    }

    public boolean isEnableHeuristics() {
        return enableHeuristics;
    }

    public void setEnableHeuristics(boolean enableHeuristics) {
        this.enableHeuristics = enableHeuristics;
    }

    public String getCode() {
        return "search";
    }

    public String getDisplayName() {
        return "Image Search";
    }

    public String getDescription() {
        return "Search image comparison algorithm optimized for typical remote desktop appearance.";
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
        return ImageComparisonModule.class;
    }

    public boolean requiresRestart() {
        return false;
    }

    public String getVendorHomePage() {
        return ApplicationSupport.APPLICATION_HOME_PAGE;
    }

    public java.util.Date getDate() {
        return Utils.getReleaseDate();
    }

    public String getUniqueId() {
        return "VNCRobot_native_Search_image_comparison_module";
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
     * Get the type (name) of the image comparison method. It describes and
     * uniquely identifies the search method used, e.g. "Exact match",
     * "Shape match" etc.
     * @return This implementation always returns "Exact match".
     */
    public String getType() {
        return "Exact match";
    }

    private void rebuildHeuristics(float passRate) {

        // Get the template image histogram
//        long time = System.currentTimeMillis();
        ImageHistogram templateHistogram = new ImageHistogram();
        templateHistogram.load(pixels);
        histTime = System.nanoTime();
//        System.out.println("  histogram generated: "+(System.currentTimeMillis()-time)+"ms\n    histogram: "+templateHistogram);
//        time = System.currentTimeMillis();
//        templateHistogram.load(pixels);
//        System.out.println("  histogram2 generated: "+(System.currentTimeMillis()-time)+"ms\n    histogram: "+templateHistogram);
//        time = System.currentTimeMillis();

        // Calculate the max number of different pixels allowed by the pass rate
        final int neededPixelCount = Math.min((int) (pixels.length - pixels.length * passRate) + 1, (int) templateHistogram.getLength());

        // Analyze the heuristics and find colors with the
        // lowest count (the most unique colors in the image)
        int[] l = templateHistogram.getSortedListOfColors(neededPixelCount);
//        System.out.println("  sorted: "+(System.currentTimeMillis()-time)+"ms");
//        time = System.currentTimeMillis();
        Map t = templateHistogram.getPointers();

        // Create a map where key=color, value=how many pixels, and number
        // of pixels is equal to the number of allowed failed pixels plus one
        Map heurColors = new HashMap();
        Number color;
        int[] colorCnt;
        int pixelCnt = 0;
        int toAdd;
        for (int i = 0; i < l.length && pixelCnt < neededPixelCount; i++) {
            color = new Integer(l[i]);
            colorCnt = (int[]) t.get(color);
            if (colorCnt != null) {
                toAdd = Math.min(colorCnt[0], neededPixelCount - pixelCnt);
                if (toAdd > neededPixelCount && l.length > 1) {
                    // Never use a heuristics consisting of a single color if there are more than one.
                    // This makes sure that we'll be looking for pixels of at least two different colors.
                    toAdd--;
                }
                heurColors.put(color, new Integer(toAdd));
                pixelCnt += toAdd;
            }
        }
//        System.out.println("  map: "+(System.currentTimeMillis()-time)+"ms");
//        time = System.currentTimeMillis();

        // Go through the template image pixels, locate pixels of the selected colors
        // and save the pixel indices into the array.
        heurPixels = new int[neededPixelCount];

        int index = 0;
        Number pixel, counter;
        for (int i = 0; i < pixels.length && index < neededPixelCount; i++) {
            pixel = new Integer(pixels[i]);
            counter = (Number) heurColors.get(pixel);
            if (counter != null) {
                heurPixels[index] = i;
                index++;
                if (counter.intValue() > 1) {
                    heurColors.put(pixel, new Integer(counter.intValue() - 1));
                } else {
                    heurColors.remove(pixel);
                }
            }
        }
    }

    /**
     * Find out whether pixel in the source image starting from the given
     * offset match to the pixels of this pattern.
     * @param source array of source image pixels.
     * @param offset position from which to start the search the source image
     * pixel array. The value of zero will start the search from the beginning.
     * @param sourceRec source image geometry, i.e. width and height of the
     * rectangle represented by the <code>source[]</code> array of pixels.
     */
    public boolean matches(int source[], int offset, Rectangle sourceRec, float passRate) {
        int sourceIndex, patternIndex;
        final int sourceLength = source.length;
        final int srcWidth = sourceRec.width;
        final int rw = r.width,  rh = r.height;
        int failedPixelCount = 0;

        // Calculate the number of different pixels allowed by the pass rate
        final int allowedFailedPixelCount =
                (int) (pixels.length - passRate * pixels.length);
//                (int)((float)getNumberOfNonAlphaPixels()*(1-passRate));
        pxcnt++;

        if (failedPixels != null) {
            failedPixels.clear();
        }

        int y, x, ppx, spx;

        // First process all the heuristics pixels
        if (heurPixels != null) {
            int heurLength = heurPixels.length;
            for (int i = 0; i < heurLength; i++) {
                // Calculate the pattern pixel index
                patternIndex = heurPixels[i];

                // Calculate the corresponding index in the source pixel array
                sourceIndex = ((int) (patternIndex / rw)) * srcWidth + offset + (patternIndex % rw);

                // Compare the pattern and source pixel colors
                // Ignore the transparent pattern pixels with value Integer.MAX_VALUE
                ppx = pixels[patternIndex];
                spx = source[sourceIndex];
                if (sourceIndex >= sourceLength || (((ppx >> 24) & 0xFF) == 0xFF && spx != ppx)) {

                    failedPixelCount++;

                    // If failed pixel tracking is enabled, save the point
                    if (failedPixels != null) {
                        x = patternIndex % rw;
                        y = (int) (patternIndex / rw);
                        failedPixels.add(new Point(x, y));
                    }

                    // If the number of non-matching pixels exceeds the limit specified by the pass rate,
                    // return false (no match)
                    if (failedPixelCount > allowedFailedPixelCount) {
                        return false;
                    }
                }
            }
        }

        failedPixelCount = 0;
        if (failedPixels != null) {
            failedPixels.clear();
        }

        // Process all pixels of pattern specified by [x,y] within pattern rectangle r
        for (y = 0; y < rh; y++) {
            for (x = 0; x < rw; x++) {
                // Calculate the pattern pixel index
                patternIndex = y * rw + x;

                // Calculate the corresponding index in the source pixel array
                sourceIndex = y * srcWidth + offset + x;

                // Compare the pattern and source pixel colors.
                // Ignore the transparent pattern pixels with value Integer.MAX_VALUE
                if (sourceIndex >= sourceLength || (((pixels[patternIndex] >> 24) & 0xFF) == 0xFF && source[sourceIndex] != pixels[patternIndex])) {

                    failedPixelCount++;

                    // If failed pixel tracking is enabled, save the point
                    if (failedPixels != null) {
                        failedPixels.add(new Point(x, y));
                    }

                    // If the number of non-matching pixels exceeds the limit specified by the pass rate,
                    // return false (no match)
                    if (failedPixelCount > allowedFailedPixelCount) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public List<Point> getFailedPixels() {
        return failedPixels;
    }

    public void setTrackingOfFailedPixelsEnabled(boolean enabled) {
        failedPixels = enabled ? new ArrayList() : null;
        enableHeuristics = !enabled;
    }

    private void resetHeuristics() {
        heurPixels = null;
    }

    public void checkDependencies(PluginManager manager) throws DependencyMissingException {
    }

    public List<Rectangle> getHits() {
        return hits;
    }

    public boolean isTrackingOfFailedPixelsSupported() {
        return true;
    }
}
