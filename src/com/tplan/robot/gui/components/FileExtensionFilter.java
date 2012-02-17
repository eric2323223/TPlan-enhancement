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

import com.tplan.robot.util.Utils;

import javax.swing.filechooser.FileFilter;
import java.util.Map;
import java.io.File;
import java.util.HashMap;

/**
 * Generic file filter to be used with JFileChooser instances intended
 * to select files having one of the given extensions.
 * @product.signature
 */
public class FileExtensionFilter extends FileFilter {
    Map extensionMap;
    String desc;

    /**
     * Default constructor.
     * @param extensions array of file extensions to be supported by this filter
     * @param description
     */
    public FileExtensionFilter(String extensions[], String description) {
        extensionMap = new HashMap();
        for (String s : extensions) {
            extensionMap.put(s.toLowerCase(), "");
        }
        desc = description;
    }

    /**
     * Determine whether a file is supported by this filter.
     * @param f a file.
     * @return true if the file is supported (meaning it has one of the specified extensions), false if not.
     */
    public boolean accept(File f) {
        if (f != null) {
            String ext = Utils.getExtension(f.getName());
            if (ext != null && extensionMap != null && extensionMap.containsKey(ext.toLowerCase())) {
                return true;
            }
            if (f.isDirectory()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get file filter description.
     * @return file filter description.
     */
    public String getDescription() {
        return desc;
    }
}
