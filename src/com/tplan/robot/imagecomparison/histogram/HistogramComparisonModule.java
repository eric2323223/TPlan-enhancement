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
package com.tplan.robot.imagecomparison.histogram;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.imagecomparison.ImageComparisonModule;
import com.tplan.robot.plugin.DependencyMissingException;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.util.Measurable;
import com.tplan.robot.util.Stoppable;
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.preferences.UserConfiguration;

import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.util.Utils;
import java.util.Date;
import javax.swing.*;
import java.awt.*;
import java.awt.image.PixelGrabber;
import java.util.Iterator;
import java.util.Map;

/**
 * Image comparison module based on color histogram.
 * @product.signature
 */
public class HistogramComparisonModule
        implements ImageComparisonModule, ConfigurationKeys, Stoppable, Measurable, Plugin {
    
    /**
     * Maximum number of horizontal pixel rows which will be processed at a time. A higher number will result in faster
     * execution but will consume more memory.
     */
    int MAX_LOADED_PIXEL_ROWS = 200;
    
    /**
     * Histogram of the base image, which is a stable template image against which more image comparisons will be
     * performed. It is used to speed up executions e.g. in 'Waitfor match' where we periodically compare
     * the remote desktop image to a stable template.
     */
    private ImageHistogram baseImageHist;
    
    /**
     * An image observer used to determine the image size.
     */
    private JLabel imageObserver = new JLabel();
    
    
    private boolean stop;
    
    private ImageHistogram remoteDesktopHist;
    
    private int pxcnt = 0;
    
    private int histSize = 0;
    
    public void stop() {
        stop = true;
    }
    
    public boolean isStopped() {
        return stop;
    }

    public float getProgress() {
        int base = histSize + (int)(remoteDesktopHist == null ? 0 : remoteDesktopHist.getLength());
        int histContrib = remoteDesktopHist==null? 0 : (int)(remoteDesktopHist.getProgress()*remoteDesktopHist.getLength());
        return base == 0 ? 0 : (float)(pxcnt+histContrib)/base;
    }

    public float compare(Image img1, Rectangle area, Image img2, String methodParams, ScriptingContext repository, float passRate) {
        stop = false;
        UserConfiguration cfg = repository.getConfiguration();
        Integer max = cfg == null ? null : cfg.getInteger(COMPARETO_MAX_LOADED_PIXEL_ROWS);
        if (max != null) {
            MAX_LOADED_PIXEL_ROWS = max.intValue();
        }
        
        // Now normalize the image.
        Rectangle r = new Rectangle();
        r.width = Math.min(img1.getWidth(imageObserver), img2.getWidth(imageObserver));
        r.height = Math.min(img1.getHeight(imageObserver), img2.getHeight(imageObserver));
        histSize = r.width * r.height;
        
        // If a custom area is defined, update location of the pixel rectangle
        Rectangle rr = new Rectangle(r);
        if (area != null) {
            rr.x = area.x;
            rr.y = area.y;
        }
        
        // Get histogram of the remote desktop
        remoteDesktopHist = getHistogram(img1, rr);
        
        // Perform the comparison
        float ratio = compare(remoteDesktopHist, img2, r);
        return ratio;
    }
    
    public void setBaseImage(Image img) {
//        System.out.println("setting base image to "+img);
        if (img == null) {
            baseImageHist = null;
        } else {
            Rectangle r = new Rectangle(img.getWidth(imageObserver), img.getHeight(imageObserver));
            baseImageHist = getHistogram(img, r);
//            System.out.println("getting base image histogram, rectangle = "+r);
        }
    }
    
    public float compareToBaseImage(Image img2, Rectangle area, String methodParams, ScriptingContext repository, float passRate) {
        Rectangle r = new Rectangle(img2.getWidth(imageObserver), img2.getHeight(imageObserver));
        return compare(baseImageHist, img2, r);
    }
    
    public boolean isSecondImageRequired() {
        return true;
    }
    
    public boolean isMethodParamsSupported() {
        return false;
    }
    
    public String getMethodName() {
        return "default";
    }
    
    public String getMethodDescription() {
        return "Histogram based image comparison module";
    }
    
    
    //---------------------------- NEW METHODS -----------------------------------------
    public ImageHistogram getHistogram(Image img, Rectangle rect) {
        remoteDesktopHist = new ImageHistogram();
        return remoteDesktopHist.load(img, rect);
    }
    
    public ImageHistogram getHistogram(int pixels[], Rectangle histRect, Rectangle sourceRect) {
        remoteDesktopHist = new ImageHistogram();
        return remoteDesktopHist.load(pixels, histRect, sourceRect);
    }
    
    public float compare(ImageHistogram h, Image img, Rectangle rect) {
        pxcnt = 0;
        stop = false;
        Map t = h.getPointers();
        int diff = 0;
        Integer px;
        int colorCnt[];
        
        int pixels[] = null;
        
        int imgWidth = rect == null ? img.getWidth(imageObserver) : rect.width;
        int imgHeight = rect == null ? img.getHeight(imageObserver) : rect.height;
        
        Rectangle r = new Rectangle(0, 0, imgWidth, Math.min(imgHeight, MAX_LOADED_PIXEL_ROWS));
        int plen;
        int index;
        
        while (r.y <= imgHeight && r.height > 0 && !stop) {
            pixels = getPixels(img, r);
            plen = pixels.length;
            
            for (int i = 0; i < plen; i++) {
                pxcnt++;
                px = new Integer(pixels[i]);
                colorCnt = (int[]) t.get(px);
                if (colorCnt != null) {
                    if (colorCnt[0] > 0) {
                        colorCnt[0]--;
                    } else {
                        diff++;
                    }
                } else {
                    diff++;
                }
            }
            r.y += MAX_LOADED_PIXEL_ROWS;
            r.height = Math.min(imgHeight - r.y, MAX_LOADED_PIXEL_ROWS);
        }
        return stop ? 0 : 1 - ((float) diff / (float) h.getLength());
    }
    
//---------------------------- END OF NEW METHODS -----------------------------------------
    
    public int[] getPixels(Image img, Rectangle r) {
        int width = r.width;
        int height = r.height;
        int ai[] = new int[width * height];
        
        PixelGrabber pixelgrabber =
                new PixelGrabber(img, r.x, r.y, width, height, ai, 0, width);
        try {
            pixelgrabber.grabPixels();
        } catch (InterruptedException interruptedexception) {
            interruptedexception.printStackTrace();
        }
        return ai;
    }
    
    public float compareHistograms(ImageHistogram h1, ImageHistogram h2) {
        stop = false;
        Map t1 = h1 == null ? null : h1.getPointers();
        if (h1 == null || t1 == null) {
            return 1;
        }
        Iterator en = t1.keySet().iterator();
        Map t2 = h2.getPointers();
        Object o;
        int cnt, cnt2;
        long d;
        long diff = 0;
        
        while (en.hasNext()) {
            o = en.next();
            cnt = ((int[])t1.get(o))[0];
            if (t2.containsKey(o)) {
                cnt2 = ((int[])t1.get(o))[0];
                if (cnt > cnt2) {
                    diff += cnt - cnt2;
                }
            } else {
                diff += cnt;
            }
        }
        float result = diff == 0 ? 1 : 1 - ((float)diff/(float)h1.getLength());
        return result;
    }

    public String getCode() {
        return "default";
    }

    public String getDisplayName() {
        return "Image Histogram";
    }

    public String getDescription() {
        return "Histogram based image comparison algorithm.";
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

    public Date getDate() {
        return Utils.getReleaseDate();
    }

    public String getUniqueId() {
        return "VNCRobot_native_Histogram_image_comparison_module";
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

    public void checkDependencies(PluginManager manager) throws DependencyMissingException {
    }
}
