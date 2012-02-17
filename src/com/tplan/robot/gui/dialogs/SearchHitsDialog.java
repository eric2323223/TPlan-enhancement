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
package com.tplan.robot.gui.dialogs;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.gui.DesktopGlassPanel;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.gui.components.ImageDrawPanel;
import com.tplan.robot.gui.preferences.color.ColorPanel;
import com.tplan.robot.imagecomparison.ImageComparisonModule;
import com.tplan.robot.imagecomparison.ImageComparisonModuleFactory;
import com.tplan.robot.imagecomparison.search.ExtendedSearchCapabilities;
import com.tplan.robot.imagecomparison.search.SearchImageComparisonModule;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.util.Utils;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.MessageFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Vector;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * <p>Dialog which displays image search matches found by comparisons performed through the
 * Compareto, Screenshot of Waitfor match windows. The window displays a copy of
 * the remote desktop image and allows to display areas where the template image was found
 * and what the eventual differencies were (if pass rate lower than 100% was used).</p>
 * @product.signature
 */
public class SearchHitsDialog extends JDialog
        implements ActionListener, ItemListener, ChangeListener, MouseMotionListener {

    /** The draw panel which displays the remote desktop image. */
    private ImageDrawPanel pnl;
    /** A copy of the template image. */
    private Image templateImage;
    /**
     * Glass panel. It is placed on top of the image panel and it displays the
     * match rectangles and pixels which are different between the template and
     * remote desktop image.
     */
    private DesktopGlassPanel glassPanel = new DesktopGlassPanel();
    /** A reference to the main frame. */
    private MainFrame mainFrame;
    /** Drop down with the matches. */
    private JComboBox cmbHits = new JComboBox();
    /** Check box allowing to switch on/off showing of the match rectangles. */
    private JCheckBox chbFrame = new JCheckBox(ApplicationSupport.getString("searchHitsDlg.chbMatchRect"));
    /** Check box allowing to switch on/off showing of the different pixels. */
    private JCheckBox chbDiff = new JCheckBox(ApplicationSupport.getString("searchHitsDlg.chbDiff"));
    /** Label displaying coordinates and color of the pixel under the mouse pointer. */
    private JLabel lblCoords = new JLabel("");
    /** Color drop down allowing to select color of the different pixels. */
    private ColorPanel pnlDiffColor;
    /** Color drop down allowing to select color of the match rectangles. */
    private ColorPanel pnlFrameColor;
    /** Vector of search matches in form of <code>Rectangle</code> instances. */
    private Vector hits;
    /** Pass rate for the image comparison. */
    private float passRate;
    private ExtendedSearchCapabilities module;

    /**
     * Constructor.
     * @param parent a parent for this dialog.
     * @param mainFrame a reference to the main frame.
     */
    public SearchHitsDialog(JDialog parent, MainFrame mainFrame) {
        super(parent, ApplicationSupport.getString("searchHitsDlg.title"), true);
        this.mainFrame = mainFrame;

        pnlDiffColor = new ColorPanel();
        pnlDiffColor.setSelectedColor(Color.GREEN);
        pnlDiffColor.addChangeListener(this);
        pnlDiffColor.cmbColors.addActionListener(this);

        pnlFrameColor = new ColorPanel();
        pnlFrameColor.setSelectedColor(Color.RED);
        pnlFrameColor.addChangeListener(this);
        pnlFrameColor.cmbColors.addActionListener(this);

        pnl = new ImageDrawPanel();
        pnl.addMouseMotionListener(this);
        getContentPane().setLayout(new BorderLayout());
        pnl.setLayout(new GridBagLayout());
        pnl.add(glassPanel, new GridBagConstraints(0, 0, 0, 0, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.add(pnl, new GridBagConstraints(0, 0, 0, 0, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        JPanel pnlWrap = new JPanel(new BorderLayout(0, 0));
        JScrollPane scroll = new JScrollPane(contentPanel);
        pnlWrap.add(scroll, BorderLayout.CENTER);
        getContentPane().add(pnlWrap, BorderLayout.CENTER);

        JPanel pnlNorth = new JPanel();
        pnlNorth.setLayout(new GridBagLayout());
        pnlNorth.add(new JLabel(ApplicationSupport.getString("searchHitsDlg.labelMatchesDisplayed")),
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 5), 0, 0));
        pnlNorth.add(lblCoords, new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 5), 0, 0));

        pnlNorth.add(cmbHits, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        pnlNorth.add(new JLabel(ApplicationSupport.getString("searchHitsDlg.labelDisplay")),
                new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 10, 2, 2), 0, 0));
        pnlNorth.add(chbFrame, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        pnlNorth.add(pnlFrameColor, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        pnlNorth.add(chbDiff, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        pnlNorth.add(pnlDiffColor, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));

        pnlNorth.add(new JPanel(), new GridBagConstraints(5, 0, 1, 2, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        cmbHits.addActionListener(this);
        chbDiff.setSelected(true);
        chbFrame.setSelected(true);
        chbDiff.addItemListener(this);
        chbFrame.addItemListener(this);
        getContentPane().add(pnlNorth, BorderLayout.NORTH);
    }

    /**
     * Set the remote desktop and template images used by this dialog.
     * @param remoteDesktopImage a copy of remote desktop image.
     * @param templateImage a copy of the template image.
     */
    public void setImages(Image remoteDesktopImage, Image templateImage) {
        pnl.setImage(remoteDesktopImage);
        this.templateImage = templateImage;
        pack();
        setLocationRelativeTo(getParent());
    }

    /**
     * Set the pass rate used in the previous image search. We need it here
     * because when a match rectangle is selected in the drop down, we use
     * a special feature of the 'search' image comparison module to search
     * the rectangle for pixels which are different.
     * @param passRate pass rate between 0 (representing 0%) and 1.0 (representing 100%).
     */
    public void setPassRate(float passRate) {
        this.passRate = passRate;
    }

    public void setModule(ExtendedSearchCapabilities module) {
        this.module = module;
    }

    /**
     * Implementation of the <code>ActionListener</code> interface. It is used
     * to process change events from the three dialog drop downs (match drop down
     * and two color ones).
     * @param e an <code>ActionEvent</code> which identifies source of the event.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(cmbHits)) {
            float res[] = resetDrawing();
            int diff = (int) res[0];
            float ratio = 100 * ((float) diff / res[1]);
            Object args[] = {diff, ratio};
            String s;
            if (diff < 0) {
                s = ApplicationSupport.getString("searchHitsDlg.chbDiff");
            } else {
                s = ApplicationSupport.getString("searchHitsDlg.chbDiffExtended");
            }
            chbDiff.setText(MessageFormat.format(s, args));
        } else if (e.getSource().equals(pnlDiffColor.cmbColors)) {
            glassPanel.setPixelUpdateColor(pnlDiffColor.getSelectedColor());
        } else if (e.getSource().equals(pnlFrameColor.cmbColors)) {
            glassPanel.setFrameColor(pnlFrameColor.getSelectedColor());
        }
    }

    /**
     * Reset the glass panel drawings. The method should be called when
     * the selected match rectangle gets changed (i.e. when user selects
     * something in the match drop down). It performs a search for
     * the different pixels and resets the data in the glass pane which
     * redisplays the selected rectangle and pixel differencies.
     *
     * @return number of pixels which are different between the template and
     * selected rectangle of the remote desktop image.
     */
    private float[] resetDrawing() {
        int index = cmbHits.getSelectedIndex();
        if (index == 0) {
            glassPanel.setFrameUpdates(chbFrame.isSelected() ? hits : new Vector());
            return new float[]{-1.0f, -1.0f};
        } else {
            if (module.isTrackingOfFailedPixelsSupported()) {
                Rectangle r = (Rectangle) hits.get(index - 1);
                Vector v = new Vector();
                if (chbFrame.isSelected()) {
                    v.add(r);
                }
                glassPanel.setFrameUpdates(v);
                Vector details = new Vector();
                ExtendedSearchCapabilities mod = getDetails(r, passRate, details);
                Point p;
                for (int i = 0; i < details.size(); i++) {
                    p = (Point) details.get(i);
                    p.x += r.x;
                    p.y += r.y;
                }
                glassPanel.setPixelUpdates(chbDiff.isSelected() ? details : new Vector());
                return new float[]{details.size(), r.width * r.height}; // mod.getNumberOfNonAlphaPixels()
            } else {
                Vector v = new Vector();
                v.add((Rectangle) hits.get(index - 1));
                glassPanel.setFrameUpdates(v);
                return new float[]{-1.0f, -1.0f};
            }
        }
    }

    /**
     * Implementation of the <code>ItemListener</code>. It gets fired every time
     * one of the two the dialog check boxes gets selected or deselected. It then
     * calls the <code>resetDrawing()</code> method to reset what is shown by the
     * glass panel.
     * @param e an <code>ItemEvent</code> which identifies source of the event.
     */
    public void itemStateChanged(ItemEvent e) {
        Object src = e.getSource();
        if (src.equals(chbFrame) || src.equals(chbDiff)) {
            resetDrawing();
        }
    }

    /**
     * Search the selected rectangle for differencies between the remote desktop
     * image (or whichever image displayed by this dialog) and template image.
     * The method takes advantage of a special mode of the 'search' image
     * comparison module which is able to run in a 'tracking' mode and deliver a
     * list (Vector instance) of the different pixels. This of course makes sense
     * only when the pass rate used for the search is lower than 100% (1.0).
     *
     * @param r rectangle of the remote desktop image to be compared to the template.
     * @param passRate a pass rate for the search.
     * @return a <code>Vector</code> instance containing coordinates
     * (<code>Point</code> instances) of pixels which are different between
     * the given rectangle of the remote desktop image and template.
     */
    private ExtendedSearchCapabilities getDetails(Rectangle r, float passRate, Vector v) {
        module.setTrackingOfFailedPixelsEnabled(true);

        ScriptingContext repository = mainFrame.getScriptHandler().createDefaultContext();

        float f = ((ImageComparisonModule) module).compare(pnl.getImage(), r, templateImage, null, repository, passRate);

        List<Rectangle> hits = module.getHits();
        if (hits != null && hits.size() == 1) {
            v.addAll(module.getFailedPixels());
        }
        return module;
    }

    /**
     * Get the vector of match rectangles.
     *
     * @return a <code>Vector</code> instance containing rectangles of the remote
     * desktop image (<code>Rectangle</code> instances) where matches with the
     * template image were identified.
     */
    public Vector getHits() {
        return hits;
    }

    /**
     * Set the vector of match rectangles resulting from a 'search' image comparison.
     *
     * @param hits a <code>Vector</code> instance which must contain rectangles of the remote
     * desktop image (<code>Rectangle</code> instances) where matches with the
     * template image were identified.
     */
    public void setHits(Vector hits) {
        this.hits = hits;
        Vector v = new Vector();
        Rectangle r;
        for (int i = 0; i < hits.size(); i++) {
            r = (Rectangle) hits.get(i);
            v.add("#" + (i + 1) + ": x:" + r.x + ",y:" + r.y + ",w:" + r.width + ",h:" + r.height);
        }
        v.add(0, ApplicationSupport.getString("searchHitsDlg.matchListAll"));
        cmbHits.setModel(new DefaultComboBoxModel(v));
        glassPanel.setFrameUpdates(hits);

        // If there are just 2 items it means that there's exactly one match.
        // Select it automatically instead of the firts combo item which
        // represents all matches.
        if (v.size() == 2) {
            cmbHits.setSelectedIndex(1);
        }
    }

    /**
     * Set the vector of match rectangles resulting from a 'search' image comparison.
     *
     * @param hits a <code>Vector</code> instance which must contain rectangles of the remote
     * desktop image (<code>Rectangle</code> instances) where matches with the
     * template image were identified.
     */
    public void setRectHits(List<Rectangle> hits) {
        this.hits = new Vector(hits);
        Vector v = new Vector();
        Rectangle r;
        for (int i = 0; i < hits.size(); i++) {
            r = (Rectangle) hits.get(i);
            v.add("#" + (i + 1) + ": x:" + r.x + ",y:" + r.y + ",w:" + r.width + ",h:" + r.height);
        }
        v.add(0, ApplicationSupport.getString("searchHitsDlg.matchListAll"));
        cmbHits.setModel(new DefaultComboBoxModel(v));
        glassPanel.setFrameUpdates(this.hits);

        // If there are just 2 items it means that there's exactly one match.
        // Select it automatically instead of the firts combo item which
        // represents all matches.
        if (v.size() == 2) {
            cmbHits.setSelectedIndex(1);
        }
    }

    /**
     * Implementation of the <code>ChangeListener</code>. It gets invoked when
     * a custom color gets defined in one of the color drop downs. The method
     * passes the new color to the glass panel which then repaints the window.
     * @param e a <code>ChangeEvent</code> which identifies source of the event.
     */
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() != null && e.getSource().equals(pnlDiffColor)) {
            glassPanel.setPixelUpdateColor(pnlDiffColor.getSelectedColor());
        } else if (e.getSource() != null && e.getSource().equals(pnlFrameColor)) {
            glassPanel.setFrameColor(pnlFrameColor.getSelectedColor());
        }

    }

    /**
     * Implementation of the <code>MouseListener</code>. It gets invoked when
     * mouse is dragged in the area of the displayed image. The method does
     * nothing because we're not interested in mouse drags.
     *
     * @param e a <code>MouseEvent</code> which identifies mouse pointer
     * coordinates.
     */
    public void mouseDragged(MouseEvent e) {
    }

    /**
     * Implementation of the <code>MouseListener</code>. It gets invoked when
     * mouse pointer is moved in the area of the displayed image. The method
     * then updates a label which displays coordinates of the mouse pointer
     * together with the color of the underlying pixel. When the mouse pointer
     * is withing a single match rectangle, corresponding template image
     * coordinates and pixel color are displayed too. This allows to identify
     * differencies among pixels of the selected rectangle
     * of the remote desktop and corresponding ones of the template image.
     *
     * @param e a <code>MouseEvent</code> which identifies mouse pointer
     * coordinates.
     */
    public void mouseMoved(MouseEvent e) {
        Point p = e.getPoint();
        String s = Utils.getMouseCoordinateText(
                p,
                (BufferedImage) pnl.getImage(),
                true,
                true,
                false);
        // If the point is within the currently displayed match rectangle,
        // display also the template point & color
        int index = cmbHits.getSelectedIndex() - 1;
        if (index >= 0) {
            Rectangle r = (Rectangle) hits.get(index);
            if (r.contains(p)) {
                p.x -= r.x;
                p.y -= r.y;
                s += " x " + Utils.getMouseCoordinateText(
                        p,
                        (BufferedImage) templateImage,
                        true,
                        true,
                        false);
            }
        }
        lblCoords.setText(s);
    }
}
