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

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * <p>Component displaying preview of a template image. It also shows a window with
 * full size image and eventual selected rectangle if it gets clicked on.</p>
 *
 * @product.signature
 */
public class TemplatePreviewComponent extends ImagePreviewComponent {

    public static final String PROPERTY_EVENT_GOING_TO_OPEN_DIALOG = "imageDialogOpening";
    public static final String PROPERTY_EVENT_GOING_TO_CLOSE_DIALOG = "imageDialogClosing";
    private ImageDialog dlg;
    private Rectangle rectangle;
    private JDialog owner;
    private boolean changed = false;

    /**
     * Constructor.
     * @param imageFile an image file.
     * @param frame the main frame. Used to center the underlying dialogs.
     * @param owner owner of the window (a JDialog instance).
     */
    public TemplatePreviewComponent(File imageFile, Frame frame, JDialog owner) {
        super(imageFile, frame);
        this.owner = owner;
    }

    /**
     * Display the template image in full size in a new window.
     */
    @Override
    public void displayFullSizeDialog() {
        changed = false;
        getImageDialog().setRectangle(rectangle);
        dlg.setImage(fullSizeImage);
        dlg.setLocationRelativeTo(dlg);
        firePropertyChange(PROPERTY_EVENT_GOING_TO_OPEN_DIALOG, this, dlg);
        dlg.setVisible(true);
        if (!dlg.isCanceled()) {
            changed = true;
            setRectangle(dlg.getRectangle());
        }
        firePropertyChange(PROPERTY_EVENT_GOING_TO_CLOSE_DIALOG, this, rectangle);
    }

    /**
     * Get the image dialog.
     * @return image dialog.
     */
    public ImageDialog getImageDialog() {
        if (dlg == null) {
            dlg = new ImageDialog(owner, ApplicationSupport.getResourceBundle().getString("templatePreview.windowTitle"), true);
        }
        return dlg;
    }

    /**
     * Get the rectangle of interest selected by user through mouse drags.
     * @return rectangle selected by user. If no rectangle is selected the method returns null.
     */
    public Rectangle getRectangle() {
        return rectangle;
    }

    /**
     * Set the selected rectangle. It will be displayed in a red frame on top
     * of the template image.
     * @param rectangle selected rectangle or null if no rect is selected.
     */
    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
        Image img = fullSizeImage;
        if (rectangle != null) {
            if (img instanceof BufferedImage) {
                img = ((BufferedImage) fullSizeImage).getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            } else {
                img = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_INT_RGB);
                img.getGraphics().drawImage(fullSizeImage, 0, 0, this);
            }
        }
        createThumbnail(img);
    }

    @Override
    public void setImage(Image img) {
        fullSizeImage = img;
        rectangle = null;
        createThumbnail(img);
    }

    @Override
    public void setImageFile(File imageFile) {
        rectangle = null;
        super.setImageFile(imageFile);
    }

    public Image getCutImage() {
        Image img = fullSizeImage;
        if (rectangle != null) {
            if (img instanceof BufferedImage) {
                img = ((BufferedImage) fullSizeImage).getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            } else {
                img = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_INT_RGB);
                img.getGraphics().drawImage(fullSizeImage, 0, 0, this);
            }
        }
        return img;
    }
    /**
     * Find out if there's a user change in the selected rectangle.
     * @return true if user has changed the selected rectangle, false otherwise.
     */
    public boolean isChanged() {
        return changed;
    }
}
