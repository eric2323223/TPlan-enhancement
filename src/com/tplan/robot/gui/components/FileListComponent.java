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

import com.tplan.robot.preferences.Preference;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * A GUI component allowing to build and display a list of files. It is used for
 * preferences of type {@link Preference#TYPE_FILE}.
 *
 * @product.signature
 */
public class FileListComponent extends ItemListPanel {

    // TODO: Duplicate files allowed - is it OK?
    // TODO: Bulk removal (multiple selection)
    // TODO: Check whether the file name contains a semicolon

    private JFileChooser fileChooser;

    public FileListComponent() {
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
    }

    public FileFilter addFileExtensionFilter(String extensions[], String description) {
        FileFilter filter = new FileExtensionFilter(extensions, description);
        fileChooser.addChoosableFileFilter(filter);
        return filter;
    }

    @Override
    public Object[] addItem() {

        // If an item is selected, update the file fileChooser to use it
        Object o = getSelectedItem();
        if (o != null && o instanceof File) {
            File f = (File)o;
            f = f.isFile() ? f.getParentFile() : f;
            if (f.exists()) {
                fileChooser.setCurrentDirectory(f);
            }
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f[] = fileChooser.getSelectedFiles();
            Object l[] = new Object[2*f.length];
            for (int i=0; i<f.length; i++) {
                l[2*i] = f[i];
                l[2*i+1] = f[i].getAbsolutePath();
            }
            return l;
        }
        return null;
    }

    /**
     * @return the fileChooser
     */
    public JFileChooser getFileChooser() {
        return fileChooser;
    }
}
