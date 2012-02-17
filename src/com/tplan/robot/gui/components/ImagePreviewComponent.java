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
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

/**
 * A component which can display thumbnail of an image and show a window with
 * full size image if it gets clicked on. It can be used both with a generic
 * dialog or a JFileChooser instance to provide a preview of selected image files.
 * @product.signature
 */
public class ImagePreviewComponent extends JComponent
        implements PropertyChangeListener, MouseListener {

    protected ImageIcon thumbnail = null;
    File file = null;
    Dimension fullSize;
    Frame frame;
    protected Image fullSizeImage;
    static final String NO_IMAGE_LABEL = ApplicationSupport.getString("comparisonPnl.noImageSelected");
    ChessboardBackgroundPanel bg;

    /**
     * Constructor to be used with a JFileChooser.
     * @param fc a JFileChooser instance.
     * @param frame owner of the component.
     */
    public ImagePreviewComponent(JFileChooser fc, Frame frame) {
        init(frame);
        fc.addPropertyChangeListener(this);
    }

    /**
     * Generic constructor used to display an image from a file.
     * @param imageFile an image file.
     * @param frame owner of the component.
     */
    public ImagePreviewComponent(File imageFile, Frame frame) {
        init(frame);
        setImageFile(imageFile);
    }

    private void init(Frame frame) {
        this.frame = frame;
        Font f = frame.getFont();
        if (f == null) {
            f = UIManager.getFont("Label.font");
        }
        setFont(new Font(f.getName(), f.getStyle(), f.getSize() - 1));
        addMouseListener(this);
        setPreferredSize(new Dimension(130, 90));
        setMinimumSize(new Dimension(130, 90));
        bg = new ChessboardBackgroundPanel();
    }

    /**
     * Load and display an image from a file.
     * @param imageFile an image file.
     */
    public void setImageFile(File imageFile) {
        this.file = imageFile;
        loadImage();
    }

    /**
     * Load the image from the specified file and scale it to the thumbnail size.
     */
    private void loadImage() {
        fullSize = null;
        setToolTipText(null);
        if (file == null) {
            thumbnail = null;
            return;
        }
        try {
            Image tmpIcon = ImageIO.read(file);
            setImage(tmpIcon);
        } catch (IOException e) {
            thumbnail = null;
        }
    }

    /**
     * Load and display image from an Image instance.
     * @param img an image instance.
     */
    public void setImage(Image img) {
        fullSizeImage = img;
        createThumbnail(img);
    }

    protected void createThumbnail(Image img) {
        if (img != null) {
            fullSize = new Dimension(img.getWidth(this), img.getHeight(this));
            if (fullSize.width > 110) {
                thumbnail = new ImageIcon(img.getScaledInstance(110, -1,
                        Image.SCALE_SMOOTH));
                setToolTipText(ApplicationSupport.getString("imagePreview.displayFullSizeToolTip"));
            } else if (fullSize.height > 80) {
                thumbnail = new ImageIcon(img.getScaledInstance(-1, 80,
                        Image.SCALE_SMOOTH));
                setToolTipText(ApplicationSupport.getString("imagePreview.displayFullSizeToolTip"));
            } else {
                thumbnail = new ImageIcon(img);
            }
            setPreferredSize(new Dimension(130, 80));
            setMinimumSize(new Dimension(130, 80));
        } else {
            thumbnail = null;
            fullSize = null;
            file = null;
        }
        revalidate();
        repaint();
    }

    /**
     * Get the full size image displayed by this component.
     * @return image displayed by this component or null if none displayed.
     */
    public Image getFullSizeImage() {
        return fullSizeImage;
    }

    /**
     * Implementation of the PropertyChangeListener interface. It listens to
     * events fired by the file chooser and updates the displayed image depending
     * on the file selection.
     * @param e a property event fired by the JFileChooser.
     */
    public void propertyChange(PropertyChangeEvent e) {
        boolean update = false;
        String prop = e.getPropertyName();

        // If the directory changed, don't show an image.
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
            file = null;
            update = true;
            setToolTipText(null);

        // If a file became selected, find out which one.
        } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
            file = (File) e.getNewValue();
            update = true;
        }

        //Update the preview accordingly.
        if (update) {
            thumbnail = null;
            if (isShowing()) {
                loadImage();
                repaint();
            }
        }
    }

    /**
     * Paint the scaled image.
     * @param g a graphics context.
     */
    protected void paintComponent(Graphics g) {
        if (thumbnail == null) {
            loadImage();
        }
        if (thumbnail != null) {
            String str = fullSize.width + "x" + fullSize.height;
            Rectangle2D r = getFontMetrics(getFont()).getStringBounds(str, g);
            int x = getWidth() / 2 - thumbnail.getIconWidth() / 2;
            int y = getHeight() / 2 - (thumbnail.getIconHeight() + (int) r.getHeight()) / 2;

            if (y < 0) {
                y = 0;
            }

            if (x < 5) {
                x = 5;
            }
            bg.paint(g, new Rectangle(x, y, thumbnail.getIconWidth(), thumbnail.getIconHeight()));
            thumbnail.paintIcon(this, g, x, y);
            x = getWidth() / 2 - (int) r.getWidth() / 2;
            y += thumbnail.getIconHeight() + r.getHeight();
            g.drawString(str, x, y);
        } else {
            Rectangle2D r = getFontMetrics(getFont()).getStringBounds(NO_IMAGE_LABEL, g);
            int x = getWidth() / 2 - (int)(r.getWidth() / 2);
            int y = getHeight() / 2 - (int)(r.getHeight() / 2);
            g.drawString(NO_IMAGE_LABEL, x, y);
        }
    }

    /**
     * Display the image in full size.
     */
    public void displayFullSizeDialog() {
        if (fullSizeImage != null) {
            new ImageDlg(frame, file == null
                    ? ApplicationSupport.getString("imagePreview.imageViewDlgTitle")
                    : file.getAbsolutePath(),
                    true, fullSizeImage);
        }
    }

    /**
     * This method of the MouseListener interface displays a window with full size
     * image if user clicks on the thumbnail preview.
     * @param e a mouse event.
     */
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && thumbnail != null) {
            displayFullSizeDialog();
        }
    }

    /**
     * This MouseListener interface method is void and doesn't do anything.
     * @param e a mouse event.
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * This MouseListener interface method is void and doesn't do anything.
     * @param e a mouse event.
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * This MouseListener interface method is void and doesn't do anything.
     * @param e a mouse event.
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * This MouseListener interface method is void and doesn't do anything.
     * @param e a mouse event.
     */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Find out whether the component is displaying an image.
     * @return true if an image is displayed, false if not.
     */
    public boolean isImage() {
        return thumbnail != null && thumbnail.getIconWidth() > 0;
    }

    /**
     * A simple dialog which displays a full size image.
     */
    private class ImageDlg extends JDialog implements MouseListener {

        JLabel lbl;

        ImageDlg(Frame parent, String title, boolean modal, Image img) {
            super(parent, title, modal);
            lbl = new JLabel(new ImageIcon(img));
            lbl.addMouseListener(this);
            lbl.setToolTipText(ApplicationSupport.getString("imagePreview.clickToCloseToolTip"));
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(lbl, BorderLayout.CENTER);
            String actionKey = "escapeclose";
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);
            getRootPane().getActionMap().put(actionKey, new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    mouseClicked(null);
                }
            });
            pack();
            setLocationRelativeTo(frame);
            setVisible(true);
        }

        public void mouseClicked(MouseEvent e) {
            dispose();
            lbl.removeMouseListener(this);
            getContentPane().remove(lbl);
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }
    }
}
