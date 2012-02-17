/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
import com.tplan.robot.util.DescriptionProvider;
import com.tplan.robot.util.Measurable;
import com.tplan.robot.util.Utils;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * <p>Component showing progress of a {@link com.tplan.robot.util.Measurable Measurable}.
 * A <code>Measurable</code> is an object performing a task which is capable to report
 * progress in form of a numeric value. This component periodically retrieves
 * the value and displays a progress bar showing how much was done.</p>
 *
 * @product.signature
 */
public class ProgressPanel extends JPanel implements Runnable {

    JProgressBar progressBar = new JProgressBar();
    JLabel lblProgress = new JLabel();
    JLabel lblDesc = new JLabel();
    long startTime;
    private Measurable measurable;
    private boolean stop = false;
    private String msg1 = "{0}%";
    private String msg2 = "{0}% (estimated time: {1})";

    /**
     * Default constructor.
     */
    public ProgressPanel() {
        init();
    }

    /**
     * Display a custom description.
     * @param desc text to display below the progress bar.
     */
    public void setDescription(String desc) {
        lblDesc.setText(desc);
    }

    /**
     * Get the {@link com.tplan.robot.util.Measurable Measurable} instance that this
     * component displays progress for.
     * @return the measurable
     */
    public Measurable getMeasurable() {
        return measurable;
    }

    /**
     * Set the {@link com.tplan.robot.util.Measurable Measurable} instance. Note that to start
     * to display progress the {@link #startMeasuring()} method must be called.
     * @param measurable the measurable to set.
     */
    public void setMeasurable(Measurable measurable) {
        this.measurable = measurable;
        if (measurable == null) {
            stop = true;
        }
    }

    /**
     * Start reporting the progress.
     */
    public void startMeasuring() {
        startTime = System.currentTimeMillis();
        stop = false;
        new Thread(this).start();
    }

    /**
     * Stop reporting the progress.
     */
    public void stopMeasuring() {
        stop = true;
    }

    private void init() {

        setLayout(new GridBagLayout());

        // Init the progress bar
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);

        // Init the progress label (situated below the progress bar)
        Font f = lblProgress.getFont();
        lblProgress.setFont(new Font(f.getName(), f.getStyle(), f.getSize() - 1));
        lblProgress.setHorizontalAlignment(JLabel.CENTER);
        lblProgress.setAlignmentY(JLabel.TOP_ALIGNMENT);
        lblProgress.setText(" ");

        add(lblDesc, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));
        add(progressBar, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 10, 2, 10), 0, 0));
        add(lblProgress, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    }

    /**
     * Implementation of the {@link java.lang.Runnable} interface. The method periodically retrieves
     * the progress value of the measurable and updates the UI.
     */
    public void run() {
        if (measurable != null) {

            float progress = 0f;
            int value;
            ResourceBundle res = ApplicationSupport.getResourceBundle();
            if (res != null) {
                msg1 = res.getString("comparisonPnl.progressLabelPercent");
                msg2 = res.getString("comparisonPnl.progressLabelPercentAndEstimate");
            }
            do {
                if (measurable instanceof DescriptionProvider) {
                    String desc = ((DescriptionProvider)measurable).getDescription();
                    if (desc == null || !desc.equals(lblDesc.getText())) {
                        lblDesc.setText(desc);
                    }
                }
                progress = measurable.getProgress();
                value = Math.min(100, (int) (100 * progress));
                progressBar.setValue(value);
                String text;
                if (progress > 0.05) {
                    long execTime = System.currentTimeMillis() - startTime;
                    long estimate = (long) (execTime / progress);
                    long left = Math.max(0, Math.round((long)(0.5 + estimate * (1-progress))));
                    Object arg[] = {value, Utils.getTimePeriodForDisplay(left, false, estimate < 3000)};
                    text = MessageFormat.format(msg2, arg);
                } else {
                    Object arg[] = {value};
                    text = MessageFormat.format(msg1, arg);
                }
                lblProgress.setText(text);
                progressBar.repaint();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } while (!stop && progress < 1f);
            firePropertyChange("processFinished", false, true);
        }
    }
}