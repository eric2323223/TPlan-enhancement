/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tplan.robot.preferences;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 *
 * @author robert
 */
public interface ColorChooserImageProvider {

    BufferedImage[] getImages(List<String> descriptions);

    boolean supportColorChooser();

}
