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
package com.tplan.robot.gui.editor;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.components.ImageFileFilter;
import com.tplan.robot.gui.components.ImagePreviewComponent;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Image file chooser with an image preview component.
 *
 * @product.signature
 */
public class ImageFileChooser extends JFileChooser {
    private ImagePreviewComponent preview;

    /**
     * Constructor.
     * @param owner chooser window owner.
     */
    public ImageFileChooser(Frame owner) {
        super();
        preview = new ImagePreviewComponent(this, owner);
        JPanel pnl = new JPanel(new BorderLayout());
        Border border = BorderFactory.createEtchedBorder(Color.white, new Color(134, 134, 134));
        border = new TitledBorder(border, ApplicationSupport.getString("imageFileChooser.imagePreviewTitledBorder"));
        pnl.setBorder(border);
        pnl.add(preview, BorderLayout.CENTER);
        setAccessory(pnl);
        setFileFilter(new ImageFileFilter());
    }

    /**
     * Find out whether the file currently selected in the file chooser is a
     * displayable image or not.
     * @return true if the selected file is a supported image, false otherwise.
     */
    public boolean isSelectedFileImage() {
        return preview.isImage();
    }
}
