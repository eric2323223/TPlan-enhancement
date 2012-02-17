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
package com.tplan.robot.gui.components;

import com.tplan.robot.ApplicationSupport;
import java.text.MessageFormat;

import javax.imageio.ImageIO;

/**
 * Image file filter to be used with JFileChooser instances intended
 * to select supported image files from the file system. It takes advantage
 * of the {@link javax.imageio.ImageIO#getWriterFormatNames()} method to check
 * whether an image format is supported or not.
 * @product.signature
 */
public class ImageFileFilter extends FileExtensionFilter {

    /**
     * Default constructor.
     */
    public ImageFileFilter() {
        super(ImageIO.getWriterFormatNames(),
                MessageFormat.format(ApplicationSupport.getString("imageFileFilter.filterName"), System.getProperty("java.version")));
    }
}
