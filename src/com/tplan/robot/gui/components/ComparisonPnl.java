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
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.commands.impl.ScreenshotCommand;
import com.tplan.robot.scripting.commands.impl.CompareToCommand;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.gui.dialogs.SearchHitsDialog;
import com.tplan.robot.imagecomparison.ImageComparisonModuleFactory;
import com.tplan.robot.util.Measurable;
import com.tplan.robot.util.Stoppable;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.editor.ImageFileChooser;
import com.tplan.robot.imagecomparison.ImageComparisonModule;
import com.tplan.robot.imagecomparison.search.ExtendedSearchCapabilities;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.preferences.ConfigurationChangeEvent;
import com.tplan.robot.preferences.ConfigurationChangeListener;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.TokenParserImpl;
import com.tplan.robot.scripting.commands.CommandHandler;
import com.tplan.robot.util.Utils;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.List;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A panel with components allowing to edit image comparison parameters used
 * by commands of the {@product.name} scripting language.
 * 
 * @product.signature
 */
public class ComparisonPnl extends JPanel implements WindowListener,
        ItemListener, ActionListener, DocumentListener, ConfigurationChangeListener,
        ListSelectionListener, PropertyChangeListener, VetoableChangeListener {

    TemplatePreviewComponent preview;
    ImageDialog dlgImage;
    BufferedImage remoteDesktopImage;
    BufferedImage backupOfRemoteDesktopImage;
    JPanel pnlSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));
    JPanel pnlCompare = new JPanel(new GridBagLayout());
    ResourceBundle res = ApplicationSupport.getResourceBundle();
    JButton btnDeleteTemplate = new JButton();
    JButton btnEditTemplate = new JButton();
    JButton btnCompare = new JButton();
    JButton btnPreferences = new JButton();
    JButton btnComparisonHelp = new JButton();
    JProgressBar progressBar = new JProgressBar();
    JLabel lblProgress = new JLabel();
    final String cmpBtnText = res.getString("comparisonPnl.compare");
    final String stpBtnText = res.getString("comparisonPnl.stop");
    final String debugBtnText = res.getString("comparisonPnl.debugImage");
    final String resetBtnText = res.getString("comparisonPnl.resetImage");
    JButton btnDebugImage = new JButton();
    JDialog owner;
    boolean comparisonRunning = false;
    CompareThread cpThread;
    SearchHitsDialog dlg;
    File debuggedRemoteDesktopImage = null;
    File lastDebugImage = null;
    TokenParser parser = new TokenParserImpl();
    public static final String PROPERTY_PARAMS_CHANGED = "parametersChanged";

    // Option radio buttons
    public final JCheckBox chbEnable = new JCheckBox(res.getString("comparisonPnl.enableComparison"));
//    public final JRadioButton rbUseExisting = new JRadioButton(res.getString("comparisonPnl.useAnExistingTemplate"));
//    public final JRadioButton rbCreateNew = new JRadioButton(res.getString("comparisonPnl.createNewTemplate"));

    // Template file name controls
    JLabel lblTemplateName = new JLabel(res.getString("comparisonPnl.templateFileName"));
//    public final JCheckBox chbTemplateName = new JCheckBox(res.getString("comparisonPnl.customizeFileName"));
    final FileListPanel templateList = new FileListPanel();
    JPanel pnlCenter;
    JCheckBox chbPassrate = new JCheckBox(res.getString("comparisonPnl.customPassRate"));
    JCheckBox chbMethod = new JCheckBox(res.getString("comparisonPnl.comparisonMethod"));
    JCheckBox chbArea = new JCheckBox(res.getString("comparisonPnl.comparisonArea"));
    JTextField txtPassrate = new JTextField();
    JComboBox cmbMethods = new JComboBox();
    JTextField txtArea = new JTextField();
    JButton btnArea = new JButton("...");
    JButton btnReplace = new JButton();
    RectangleDocument rdoc;
    PercentageDocument pdoc;
    MainFrame mainFrame;
    List modules;
    String templatePath;
    String imageFileName = "";
    Map<File, Rectangle> templateUpdates = new HashMap();
    Map<File, File> rewrittenTemplates = new HashMap();
    Map inputParams;
    ScriptingContext context;

    public ComparisonPnl(MainFrame mainFrame, JDialog dlg) {
        this.mainFrame = mainFrame;
        this.owner = dlg;
        mainFrame.getUserConfiguration().addConfigurationListener(this);
        dlg.addWindowListener(this);
        init(dlg);
    }

    /**
     * Save values from the panel fields to a List and Map pair compatible with 
     * the format accepted by command handlers.
     * @param v a list to save the argument and parameter names to.
     * @param t a map to save the [parameter_name, parameter_value] pairs to.
     */
    public void getValues(List v, Map t) {
        if (chbEnable.isSelected()) {
            if (chbPassrate.isSelected()) {
                v.add(ScreenshotCommand.PARAM_PASSRATE);
                t.put(ScreenshotCommand.PARAM_PASSRATE, txtPassrate.getText());
            }
            if (chbMethod.isSelected()) {
                v.add(ScreenshotCommand.PARAM_METHOD);
                t.put(ScreenshotCommand.PARAM_METHOD, cmbMethods.getSelectedItem());
            }
            if (chbArea.isSelected() && !"".equals(txtArea.getText().trim())) {
                v.add(CompareToCommand.PARAM_CMPAREA);
                t.put(CompareToCommand.PARAM_CMPAREA, txtArea.getText());
            }

            // Find out if each template is in the template dir. Otherwise we
            // need to insert it into the command with the full path.
            List values = templateList.getValues();
            String template = "";
//            Boolean b = mainFrame.getUserConfiguration().getBoolean("ScreenshotCommand.autoComparison");
            Object o;

            for (int i = 0; i < values.size(); i++) {
                File f = (File) values.get(i);
                try {
                    if (templatePath != null) { // && b != null && b.booleanValue()) {
                        File tf = new File(templatePath);
                        String parent = f.getParent();
                        if (parent != null && parent.equals(tf.getCanonicalPath())) {
                            template += f.getName();
                        } else {
                            template += f.getAbsolutePath();
                        }
                    } else {
                        template += f.getAbsolutePath();
                    }
                    if (i < values.size() - 1) {
                        template += TokenParser.FILE_PATH_SEPARATOR;
                    }
                } catch (Exception ex) {
                }
            }
            v.add(ScreenshotCommand.PARAM_TEMPLATE);
            t.put(ScreenshotCommand.PARAM_TEMPLATE, template);
        }
    }

    /**
     * Fill the fields of this components with parameters parsed from an
     * image comparison capable command, such as Screenshot, Compareto or Waitfor
     * match/mismatch.
     *
     * @param params image comparison params as defined in the Compareto, Screenshot
     * or Waitfor match/mismatch commands.
     * @param ctx scripting context resulting from compilation of the underlying script.
     */
    public void setValues(Map params, ScriptingContext ctx) {
        // Reset the tables
        templateUpdates.clear();
        rewrittenTemplates.clear();
        inputParams = params;
        templateList.getList().setModel(new DefaultListModel());
        templatePath = ctx.getTemplateDir().getAbsolutePath();
        this.context = ctx;
        String template = (String) params.get(CompareToCommand.PARAM_TEMPLATE);

        boolean isImageComparison = template != null ||
                params.containsKey(ScreenshotCommand.PARAM_ONFAIL) ||
                params.containsKey(ScreenshotCommand.PARAM_ONPASS) ||
                params.containsKey(ScreenshotCommand.PARAM_METHOD) ||
                params.containsKey(ScreenshotCommand.PARAM_METHODPARAMS) ||
                params.containsKey(ScreenshotCommand.PARAM_TEMPLATE) ||
                params.containsKey(ScreenshotCommand.PARAM_PASSRATE);

        if (chbEnable.isVisible()) {
            chbEnable.setSelected(isImageComparison);
        }
        if (isImageComparison) {
            try {
                List<File> l = CompareToCommand.validateTemplateFileList(template, ctx, false);
                Map<File, String> descs = new HashMap();
                for (File f : l) {
                    descs.put(f, f.getAbsolutePath());
                }
                templateList.setDisplayValuesTable(descs);
                templateList.setValues(l);
            } catch (SyntaxErrorException ex) {
                // We should never get an exception because we call the validation
                // method with error reporting switched off
                ex.printStackTrace();
            }
        } else {
            if (chbEnable.isVisible()) {
                chbEnable.setSelected(false);
            }
            templateList.getList().setModel(new DefaultListModel());
        }

        String s;
        if (params.containsKey(ScreenshotCommand.PARAM_PASSRATE)) {
            Object o = params.get(ScreenshotCommand.PARAM_PASSRATE);
            if (o instanceof Number) {
                s = "" + ((Number) o).doubleValue();
            } else {
                s = o.toString();
                if (s.endsWith("%")) {
                    s = s.substring(0, s.length() - 1);
                }
            }
            txtPassrate.setEnabled(true);
            chbPassrate.setSelected(true);
        } else {
            s = "" + mainFrame.getUserConfiguration().getDouble("CompareToCommand.defaultPassRate").doubleValue();
            txtPassrate.setEnabled(false);
            chbPassrate.setSelected(false);
        }
        txtPassrate.setText(s);
        updateDefaultPassRate();

        String defaultMethod = mainFrame.getUserConfiguration().getString("CompareToCommand.defaultComparisonModule");
        if (modules.contains(defaultMethod) && modules.indexOf(defaultMethod) != 0) {
            modules.remove(defaultMethod);
            modules.add(0, defaultMethod);
            cmbMethods.setModel(new DefaultComboBoxModel(new Vector(modules)));
        }
        if (params.containsKey(ScreenshotCommand.PARAM_METHOD)) {
            cmbMethods.setSelectedItem(params.get(ScreenshotCommand.PARAM_METHOD));
            cmbMethods.setEnabled(true);
            chbMethod.setSelected(true);
        } else {
            if (isImageComparison) {
                cmbMethods.setSelectedIndex(0);
                cmbMethods.setEnabled(false);
                chbMethod.setSelected(false);
            }
        }

        Object o = params.get(CompareToCommand.PARAM_CMPAREA);
        if (o == null) {
            s = "";
        } else if (o instanceof String) {
            s = (String) o;
        } else if (o instanceof Rectangle) {
            Rectangle r = (Rectangle) o;
            s = parser.rectToString(r);
        }
        chbArea.setSelected(o != null);
        txtArea.setText(s);

        if (preview != null) {
            RemoteDesktopClient client = mainFrame.getClient();
            remoteDesktopImage = null;
            if (client != null && client.isConnected()) {
                remoteDesktopImage = new BufferedImage(client.getDesktopWidth(), client.getDesktopHeight(), BufferedImage.TYPE_INT_RGB);
                backupOfRemoteDesktopImage = remoteDesktopImage;
                Graphics g = remoteDesktopImage.getGraphics();
                g.drawImage(client.getImage(), 0, 0, mainFrame);
            }
            preview.setImage(null);
        }

        // If the list of templates contains at least one item, select it.
        // It will also populate the preview.
        // Otherwise set the preview image to null.
        if (templateList.getList().getModel().getSize() > 0) {
            templateList.getList().setSelectedIndex(0);
        }

        updateButtonState();
    }

    /**
     * Init method which constructs layout of the panel.
     */
    private void init(JDialog dlg) {
        templatePath = Utils.getDefaultTemplatePath();
        setLayout(new BorderLayout());

        // North panel with the radio buttons
        JPanel pnlNorth = new JPanel(new GridBagLayout());
        pnlNorth.add(chbEnable, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
        chbEnable.setSelected(true);
        chbEnable.setVisible(false);

        this.add(pnlNorth, BorderLayout.NORTH);

        // Center panel - template parameters
        pnlCenter = new JPanel(new GridBagLayout());
        pnlCenter.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), res.getString("comparisonPnl.templateParamsBorderTitle")));
        pnlCenter.add(chbPassrate, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        pnlCenter.add(txtPassrate, new GridBagConstraints(1, 1, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        pnlCenter.add(chbMethod, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        pnlCenter.add(cmbMethods, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        pnlCenter.add(chbArea, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        pnlCenter.add(txtArea, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        pnlCenter.add(btnArea, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));

        List<String> l = ImageComparisonModuleFactory.getInstance().getAvailableModules();
        modules = l;
        cmbMethods.setModel(new DefaultComboBoxModel(modules.toArray()));

        templateList.addPropertyChangeListener(this);
        templateList.getList().addListSelectionListener(this);
        templateList.getList().setVisibleRowCount(3);
        templateList.setListDescription(null);
        templateList.getAddButton().setToolTipText(res.getString("comparisonPnl.btnAddTemplateToolTip"));
        templateList.getRemoveButton().setToolTipText(res.getString("comparisonPnl.btnRemoveTemplateToolTip"));
        templateList.addVetoableChangeListener(this);

        btnReplace.setMargin(new Insets(0, 0, 0, 0));
        btnReplace.setBorderPainted(false);
        btnReplace.setIcon(ApplicationSupport.getImageIcon("replace9.gif"));
        btnReplace.addActionListener(this);
        btnReplace.setToolTipText(ApplicationSupport.getString("comparisonPnl.btnRewriteTemplateToolTip"));
        templateList.addCustomButton(btnReplace, 0, 2);

        rdoc = new RectangleDocument(txtArea, CompareToCommand.PARAM_CMPAREA);
        rdoc.addDocumentListener(this);
        pdoc = new PercentageDocument(txtPassrate,
                res.getString("comparisonPnl.passRateFieldCorrect"),
                res.getString("comparisonPnl.passRateFieldIncorrect"));
        pdoc.addDocumentListener(this);

        cmbMethods.addActionListener(this);
        btnArea.addActionListener(this);
        btnArea.setMargin(new Insets(2, 3, 2, 3));

        chbPassrate.addItemListener(this);
        chbMethod.addItemListener(this);
        chbArea.addItemListener(this);

        cmbMethods.addActionListener(this);

        this.add(pnlCenter, BorderLayout.CENTER);

        // Initialize the status
        chbEnable.addItemListener(this);
        chbEnable.setSelected(false);

        preview = new TemplatePreviewComponent(null, mainFrame, dlg);
        preview.addPropertyChangeListener(this);
        dlgImage = new ImageDialog(dlg, res.getString("comparisonPnl.areaDlgTitle"), true);
        dlgImage.pnl.pnlDraw.setEnablePoints(false);

        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), res.getString("comparisonPnl.previewBorderTitle")));
        pnl.add(preview, BorderLayout.CENTER);

        pnlSouth.add(btnEditTemplate);
        pnlSouth.add(btnDeleteTemplate);
        pnlSouth.add(btnDebugImage);
        pnlSouth.add(btnPreferences);
        pnlSouth.add(btnComparisonHelp);

        // Init the progress bar
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);

        // Init the progress label (situated below the progress bar)
        Font f = lblProgress.getFont();
        lblProgress.setFont(new Font(f.getName(), f.getStyle(), f.getSize() - 1));
        lblProgress.setHorizontalAlignment(JLabel.CENTER);
        lblProgress.setAlignmentY(JLabel.TOP_ALIGNMENT);

        // Set component texts
        btnDeleteTemplate.setText(res.getString("comparisonPnl.delete"));
        btnEditTemplate.setText(res.getString("comparisonPnl.editTemplate"));
        btnComparisonHelp.setText(res.getString("comparisonPnl.comparisonHelp"));
        btnPreferences.setText(res.getString("comparisonPnl.preferences"));
        btnDebugImage.setText(res.getString("comparisonPnl.debugImage"));
        btnCompare.setText(res.getString("comparisonPnl.compare"));
        lblProgress.setText(res.getString("comparisonPnl.initialProgressLabel"));

        btnDeleteTemplate.setMargin(new Insets(1, 1, 1, 1));
        btnEditTemplate.setMargin(new Insets(1, 1, 1, 1));
        btnComparisonHelp.setMargin(new Insets(1, 1, 1, 1));
        btnPreferences.setMargin(new Insets(1, 1, 1, 1));
        btnDebugImage.setMargin(new Insets(1, 1, 1, 1));
        btnCompare.setMargin(new Insets(1, 1, 1, 1));

        // Init the 'Compare' button
        FontMetrics fm = btnCompare.getFontMetrics(btnCompare.getFont());
        int width = Math.max(fm.stringWidth(cmpBtnText), fm.stringWidth(stpBtnText));
        Dimension d = btnCompare.getPreferredSize();
        d.width = width + btnCompare.getInsets().left + btnCompare.getInsets().right;
        btnCompare.setPreferredSize(d);
        btnCompare.setMinimumSize(d);
        btnCompare.setMargin(new Insets(1, 1, 1, 1));
        btnCompare.setEnabled(true);
        btnCompare.addActionListener(this);

        // Init the 'Load RD Image' button
        fm = btnDebugImage.getFontMetrics(btnDebugImage.getFont());
        width = Math.max(fm.stringWidth(debugBtnText), fm.stringWidth(resetBtnText));
        d = btnDebugImage.getPreferredSize();
        d.width = width + btnDebugImage.getInsets().left + btnDebugImage.getInsets().right;
        btnDebugImage.setPreferredSize(d);
        btnDebugImage.setMinimumSize(d);

        // Add the 'Compare' button and the progress components into a panel
        pnlCompare.add(btnCompare, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
        pnlCompare.add(progressBar, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        pnlCompare.add(lblProgress, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        btnEditTemplate.setEnabled(true);
        btnComparisonHelp.setEnabled(mainFrame.isHelpAvailable());

        btnDeleteTemplate.addActionListener(this);
        btnEditTemplate.addActionListener(this);
        btnComparisonHelp.addActionListener(this);
        btnPreferences.addActionListener(this);
        btnDebugImage.addActionListener(this);

        pnlCenter.add(lblTemplateName, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        pnlCenter.add(templateList, new GridBagConstraints(1, 0, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
        pnlCenter.add(pnlCompare, new GridBagConstraints(0, 4, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        pnlCenter.add(pnlSouth, new GridBagConstraints(0, 5, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));

        this.add(pnl, BorderLayout.EAST);
    }

    public void cleanup() {
        dlgImage.setImage(null);
        remoteDesktopImage = null;
        backupOfRemoteDesktopImage = null;
        btnDebugImage.setText(debugBtnText);
        lblProgress.setText(ApplicationSupport.getString("comparisonPnl.initialProgressLabel"));
        if (cpThread != null) {
            cpThread.setShowResults(false);
            stopComparisonThread();
            cpThread = null;
            debuggedRemoteDesktopImage = null;
        }
    }

    public void updateTemplateFileName(String name) {
        updateButtonState();
    }

    /**
     * <p>Save the data in this component. Any class using this component is
     * requested to call this method when user decides to finish and save
     * the changes.</p>
     *
     * <p>The method validates the values and displays optional JOptionPane
     * messages in cases where a user confimation is needed, for example rewriting
     * of existing template images or possible incorrect use of some particular
     * parameter combinations. If user selects to cancel in any of these message
     * windows, the method returns false and the class owning this component is
     * supposed to cancel the finish action.</p>
     * 
     * @return true if the data was saved and it is safe to continue, false if
     * the finish&save action is canceled.
     */
    public boolean doOk() {
        stopComparisonThread();

        if (!chbEnable.isSelected()) {
            return true;
        }

        boolean shouldDispose = true;

        for (Object template : templateList.getValues()) {
            File f = (File) template;
            boolean templateExists = (f != null) && f.exists() && f.isFile();

            // Warn if user selected to rewrite the image with the remote desktop
            if (templateExists && rewrittenTemplates.containsKey(f)) {
                int option = JOptionPane.showConfirmDialog(this,
                        MessageFormat.format(res.getString("comparisonPnl.warningOverwriteTemplate"), f.getName()));
                if (option == JOptionPane.CANCEL_OPTION) {
                    shouldDispose = false;
                    break;
                }
                // If user selects "No", remove
                if (option == JOptionPane.NO_OPTION) {
                    rewrittenTemplates.remove(f);
                }
            }

            // Load the image. TODO: cache the previously loaded images
            BufferedImage img = null;
            try {
                // Bug 2859836 fix: check if the image is in the map of rewritten
                // templates and use it otherwise.
                if (rewrittenTemplates.containsKey(f)) {
                    img = remoteDesktopImage;
                } else {
                    if (f.exists()) {
                        img = ImageIO.read(f);
                    } else {
                        img = remoteDesktopImage;
                    }
                }
                // If user performed any image cutting, apply it to the image
                Rectangle rect = templateUpdates.get(f);
                if (rect != null) {
                    img = img.getSubimage(rect.x, rect.y, rect.width, rect.height);
                }
            } catch (Exception e) {
                e.printStackTrace();
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String str = res.getString("comparisonPnl.couldNotSave");
                Object arg[] = {sw.toString()};
                JOptionPane.showMessageDialog(this, MessageFormat.format(str, arg));
                shouldDispose = false;
                break;
            }

            if (img != null) {
                int imagew = img.getWidth();
                int imageh = img.getHeight();
                boolean isSearch = "search".equals(cmbMethods.getSelectedItem());

                // If remote desktop image is not null, check whether an image 
                // fragment is used together with the 'default' method and show
                // a warning if so
                if (remoteDesktopImage != null) {
                    int clientw = remoteDesktopImage.getWidth();
                    int clienth = remoteDesktopImage.getHeight();

                    // Warn if image fragment used together with the 'default' comparison method
                    if (!isSearch && (imagew != clientw || imageh != clienth)) {
                        Object options[] = new String[]{
                            res.getString("btnYes"),
                            res.getString("btnNo"),
                            res.getString("btnCancel"),
                            res.getString("btnHelp"),};
                        int option = JOptionPane.showOptionDialog(this,
                                MessageFormat.format(res.getString("comparisonPnl.warningShouldUseSearch"), f.getName()),
                                res.getString("comparisonPnl.warningWindowTitle"),
                                JOptionPane.WARNING_MESSAGE,
                                JOptionPane.OK_OPTION,
                                null,
                                options,
                                options[2]);
                        switch (option) {
                            case 0:
                                chbMethod.setSelected(true);
                                cmbMethods.setSelectedItem("search");
                                break;
                            case 2:
                                shouldDispose = false;
                                break;
                            case 3:
                                btnComparisonHelp.doClick();
                                shouldDispose = false;
                                break;
                        }
                    }
                }

                Rectangle r = null;
                try {
                    parser = new TokenParserImpl();
                    r = parser.parseRectangle(txtArea.getText(), CompareToCommand.PARAM_CMPAREA);
                } catch (Exception e) {
                }

                // Warn if cmparea is used together with the 'default' comparison method
                if (r != null && !isSearch && (r.width != imagew || r.height != imageh)) {
                    int option = JOptionPane.showConfirmDialog(this,
                            MessageFormat.format(res.getString("comparisonPnl.warningShouldUseSearch2"), f.getName()),
                            res.getString("comparisonPnl.warningWindowTitle"), JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.NO_OPTION) {
                        shouldDispose = false;
                        break;
                    }
                }

                if (!templateExists || (templateExists && templateUpdates.containsKey(f))) {
                    try {

                        // Create the template image
                        String ext = Utils.getExtension(f.getName());
                        if (ext != null) {
                            if (!f.getParentFile().exists() && !f.mkdirs()) {
                                JOptionPane.showMessageDialog(this, MessageFormat.format(res.getString("comparisonPnl.canNotCreateFolder"), f.getParent()));
                                shouldDispose = false;
                            }
                            ImageIO.write(img, ext, f);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        String str = res.getString("comparisonPnl.couldNotSave");
                        Object arg[] = {sw.toString()};
                        JOptionPane.showMessageDialog(this, MessageFormat.format(str, arg));
                        shouldDispose = false;
                    }
                }
            }
        }
        return shouldDispose;
    }

    /**
     * This method should be called any time the radio button selection changes.
     * It enables/disables the template components.
     *
     * @param b enable/disable template components
     */
    protected void setTemplateParamsEnabled(boolean b) {
        templateList.setEnabled(b);
        lblTemplateName.setEnabled(b);
        chbPassrate.setEnabled(b);
        txtPassrate.setEnabled(b && chbPassrate.isSelected());

        // Bug fix: the button should be disabled if there is no connection
        btnReplace.setEnabled(b && remoteDesktopImage != null && templateList.getList().getSelectedIndex() >= 0);

        chbMethod.setEnabled(b);
        cmbMethods.setEnabled(b && chbMethod.isSelected());

        chbArea.setEnabled(b);
        txtArea.setEnabled(b && chbArea.isSelected());
        btnArea.setEnabled(txtArea.isEnabled());

        if (btnDeleteTemplate != null) {
            if (b) {
                updateButtonState();
            } else {
                btnDeleteTemplate.setEnabled(b);
                btnCompare.setEnabled(b);
                btnEditTemplate.setEnabled(b);
            }
        }
    }

    /**
     * Implementation of the ActionListener interface handling events of the
     * controls of this component.
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(cmbMethods)) {
            updateDefaultPassRate();
            firePropertyChange(PROPERTY_PARAMS_CHANGED, this, e.getSource());
        } else if (e.getSource().equals(btnArea)) {
            try {
                Rectangle r = parser.parseRectangle(txtArea.getText(), ScreenshotCommand.PARAM_AREA);
                dlgImage.setRectangle(r);
            } catch (Exception ex) {
                dlgImage.setRectangle(null);
            }
            dlgImage.setImage(remoteDesktopImage);
            dlgImage.setVisible(true);
            if (!dlgImage.isCanceled()) {
                if (dlgImage.getRectangle() != null) {
                    txtArea.setText(parser.rectToString(dlgImage.getRectangle()));
                    dlgImage.setRectangle(null);
                } else {
                    txtArea.setText("");
                }
            }
            firePropertyChange(PROPERTY_PARAMS_CHANGED, this, btnArea);
        } else if (e.getSource().equals(btnEditTemplate)) {
            preview.displayFullSizeDialog();
        } else if (e.getSource().equals(btnDeleteTemplate)) {
            try {
                File imageFile = (File) templateList.getSelectedItem();
                if (imageFile != null && imageFile.exists()) {
                    imageFile.delete();
                    templateList.getRemoveButton().doClick();
                    updateButtonState();
                    preview.setImage(null);
                    preview.setImageFile(null);
                    preview.repaint();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        // No need to fire the event, the list will do it on removal
//            firePropertyChange("parametersChanged", this, btnDeleteTemplate);
        } else if (e.getSource().equals(btnCompare)) {
            if (!stopComparisonThread()) {
                doCompare(true);
            }
        } else if (e.getSource().equals(btnReplace)) {
            File f = (File) templateList.getSelectedItem();
            if (f != null && f.exists()) {
                rewrittenTemplates.put(f, f);
                valueChanged(null);
            }
        } else if (e.getSource().equals(btnComparisonHelp)) {
            mainFrame.showHelpDialog("gui.comparison", owner);
        } else if (e.getSource().equals(btnPreferences)) {
            CommandHandler h = context.getScriptManager().getCommandHandlers().get("COMPARETO");
            String pnl = null;
            if (h != null && h instanceof Plugin) {
                pnl = ((Plugin) h).getDisplayName();
            }
            mainFrame.showOptionsDialog(pnl, null);
        } else if (e.getSource().equals(btnDebugImage)) {
            try {
                // There's already a loaded image => remove it, reset the button label
                // and reload the remote desktop image
                if (debuggedRemoteDesktopImage != null) {
                    debuggedRemoteDesktopImage = null;
                    remoteDesktopImage = backupOfRemoteDesktopImage;
                    btnDebugImage.setText(debugBtnText);
                } else {
                    Integer pref = UserConfiguration.getInstance().getInteger("warning.debugImageComparisonInfo");
                    int option = 0;

                    if (pref == null || pref.intValue() < 0) {
                        // Show the info message
                        option = Utils.showConfigurableMessageDialog(
                                mainFrame,
                                res.getString("comparisonPnl.loadRDImageInfoTitle"),
                                res.getString("comparisonPnl.loadRDImageInfoText"),
                                res.getString("comparisonPnl.loadRDImageInfoOption"),
                                "warning.debugImageComparisonInfo",
                                new Object[]{
                                    res.getString("comparisonPnl.loadRDImageInfoOk"),
                                    res.getString("comparisonPnl.loadRDImageInfoCancel")
                                },
                                0);
                    }
                    if (option == 0) {
                        JFileChooser chooser = new ImageFileChooser(mainFrame);
                        chooser.setAcceptAllFileFilterUsed(true);
                        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        chooser.setMultiSelectionEnabled(false);
                        if (lastDebugImage != null && lastDebugImage.exists()) {
                            chooser.setSelectedFile(lastDebugImage);
                        }
                        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                            File f = chooser.getSelectedFile();
                            ImageIcon ic = new ImageIcon(f.getPath());
                            remoteDesktopImage = new BufferedImage(ic.getIconWidth(), ic.getIconHeight(), BufferedImage.TYPE_INT_RGB);
                            Graphics g = remoteDesktopImage.getGraphics();
                            g.drawImage(ic.getImage(), 0, 0, this);
                            lastDebugImage = f;
                            debuggedRemoteDesktopImage = f;
                            btnDebugImage.setText(resetBtnText);
                        }
                    }
                }
                btnCompare.setEnabled(remoteDesktopImage != null && preview.getFullSizeImage() != null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private boolean stopComparisonThread() {
        if (cpThread != null && cpThread.isAlive() && cpThread.mod instanceof Stoppable) {
            ((Stoppable) cpThread.mod).stop();
            btnCompare.setText(cmpBtnText);
            return true;
        }
        return false;
    }

    /**
     * Implementation of the ItemListener interface. It defines what should be done when one of the
     * selectable components changes its state.
     *
     * @param e an ItemEvent
     */
    public void itemStateChanged(ItemEvent e) {
        Object src = e.getSource();
        if (src instanceof JCheckBox) {
            if (src.equals(chbEnable)) {
                setTemplateParamsEnabled(chbEnable.isSelected());
                if (!chbEnable.isSelected()) {
                    if (preview != null) {
                        preview.setImageFile(null);
                    }
                }
            } else if (src.equals(chbPassrate)) {
                txtPassrate.setEnabled(chbPassrate.isEnabled() && chbPassrate.isSelected());
                updateDefaultPassRate();
            } else if (src.equals(chbMethod)) {
                cmbMethods.setEnabled(chbMethod.isEnabled() && chbMethod.isSelected());
                updateDefaultPassRate();
            } else if (src.equals(chbArea)) {
                txtArea.setEnabled(chbArea.isEnabled() && chbArea.isSelected());
                btnArea.setEnabled(txtArea.isEnabled());
            }
        }

        firePropertyChange(PROPERTY_PARAMS_CHANGED, this, e.getSource());
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e == null || e.getSource().equals(templateList.getList())) {
            File f = (File) templateList.getSelectedItem();
            if (f != null && f.exists() && !rewrittenTemplates.containsKey(f)) {
                preview.setRectangle(null);
                preview.setImageFile(f);
                preview.setRectangle(templateUpdates.get(f));
            } else {
                preview.setImage(remoteDesktopImage);

                // Reset the rectangle only if the list of files is greater than 
                // one. This will preserve the cut actions performed before 
                // the file name for a new template was entered.
                if (templateList.getList().getModel().getSize() > 1) {
                    preview.setRectangle(templateUpdates.get(f));
                } else {
                    templateUpdates.put(f, preview.getRectangle());
                }
            }
            updateButtonState();
        }
        firePropertyChange(PROPERTY_PARAMS_CHANGED, this, e);
    }

    public boolean isContentCorrect() {
        if (chbEnable.isVisible() && !chbEnable.isSelected()) {
            return true;  // Image comparison is intentionally disabled
        }
        return templateList.getList().getModel().getSize() > 0;
    }

    private void doCompare(boolean showProgress) {
        try {
            String moduleName = (String) cmbMethods.getSelectedItem();
            ImageComparisonModule mod = ImageComparisonModuleFactory.getInstance().getModule(moduleName);
            ScriptingContext repository = mainFrame.getScriptHandler().createDefaultContext();
            Rectangle r = null;
            try {
                if (chbArea.isSelected()) {
                    r = parser.parseRectangle(txtArea.getText(), "temp");
                }
            } catch (Exception ex) {
            }

            // Read the pass rate. Default value is defined in user configuration.
            float passRate = mainFrame.getUserConfiguration().getDouble(
                    "CompareToCommand.defaultPassRate").floatValue();

            try {
                passRate = Float.parseFloat(txtPassrate.getText());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            cpThread = new CompareThread(mod, remoteDesktopImage,
                    preview.getCutImage(), r, (String) null, repository, passRate / 100, this, showProgress);
            cpThread.start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateButtonState() {
        boolean enable = chbEnable.isSelected();
        boolean exists = false;
        File template = (File) templateList.getSelectedItem();
        if (template != null) {
            File f = template;
            exists = f.exists() && f.isFile() && f.canRead();
        }

        btnReplace.setEnabled(remoteDesktopImage != null && enable && exists);

        if (btnDeleteTemplate != null) {
            btnDeleteTemplate.setEnabled(enable && exists);
            btnCompare.setEnabled(enable && remoteDesktopImage != null && preview.getFullSizeImage() != null);
            btnEditTemplate.setEnabled(enable && preview.getFullSizeImage() != null);
        }
    }

    public void changedUpdate(DocumentEvent e) {
        firePropertyChange("parametersChanged", this, e);
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        cleanup();
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void setEnableCheckBoxVisible(boolean visible, boolean selected) {
        chbEnable.setSelected(selected);
        chbEnable.setVisible(visible);
    }

    public void setImageName(String imageName) {
        imageFileName = imageName;
        updateTemplateFileName(imageName);
    }

    public void setTemplatePath(String tp) {
        templatePath = tp == null ? Utils.getDefaultTemplatePath() : tp;
        updateTemplateFileName(imageFileName);
        Object arg[] = {templatePath};
//        rbCreateNew.setText(MessageFormat.format(res.getString("comparisonPnl.createNewTemplateInDir"), arg));
    }

    protected String getPath(String name) {
        if (name != null && name.indexOf(File.separator) < 0) {
            name = templatePath + File.separator + name;
        }
        return name;
    }

    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public void configurationChanged(ConfigurationChangeEvent evt) {
        String s = evt.getPropertyName();
        if (s.equals("CompareToCommand.defaultPassRate") ||
                s.equals("CompareToCommand.defaultSearchPassRate")) {
            updateDefaultPassRate();
        }
    }

    /**
     * Update the default pass rate to the value specified in the Preferences.
     */
    private void updateDefaultPassRate() {
        Object o;
        if (chbMethod.isSelected()) {
            o = cmbMethods.getSelectedItem();
        } else {
            o = UserConfiguration.getInstance().getString(
                    "CompareToCommand.defaultComparisonModule");
        }
        if (o != null && !chbPassrate.isSelected()) {
            if (o.equals("default")) {
                txtPassrate.setText("" + mainFrame.getUserConfiguration().getDouble(
                        "CompareToCommand.defaultPassRate").doubleValue());
            } else if (o.equals("search")) {
                txtPassrate.setText("" + mainFrame.getUserConfiguration().getDouble(
                        "CompareToCommand.defaultSearchPassRate").doubleValue());
            }
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(TemplatePreviewComponent.PROPERTY_EVENT_GOING_TO_CLOSE_DIALOG)) {
            Rectangle r = preview.getRectangle();
            File f = (File) templateList.getSelectedItem();
            if (r == null) {
                templateUpdates.remove(f);
            } else {
                templateUpdates.put(f, r);
            }
        }
        firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
    }

    /**
     * Vetoable change listener method to allow filtering of image files that are
     * being added to the list. If we are not connected to a desktop
     * and the remote desktop image is null, the method throws a PropertyVetoException
     * on any attempt to add a new image based on the remote desktop.
     * @param evt
     * @throws java.beans.PropertyVetoException
     */
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {

        // This means that an item is going to be added to the list.
        // If we are not connected to a desktop and the remote desktop image
        // is null, do not allow to create new images.
        if (ItemListPanel.VETOABLE_ITEM_ADD_EVENT.equals(evt.getPropertyName())) {
            if (remoteDesktopImage == null && evt.getNewValue() instanceof File) {
                File f = (File) evt.getNewValue();
                if (!f.exists()) {
                    JOptionPane.showMessageDialog(dlg, ApplicationSupport.getString("comparisonPnl.cannotCreateNewWithoutConnection"));
                    throw new PropertyVetoException("Can't create new image files without desktop connection", evt);
                }
            }
        }
    }

    private class ProgressBarUpdate extends Thread implements ActionListener {

        ImageComparisonModule mod;
        JProgressBar bar;
        boolean stop = false;

        ProgressBarUpdate(ImageComparisonModule mod, JProgressBar bar) {
            this.mod = mod;
            this.bar = bar;
        }

        @Override
        public void run() {
            do {
                if (mod instanceof Measurable) {
                    float progress = ((Measurable) mod).getProgress();
                    bar.setValue((int) (100 * progress));
                    String text;
                    if (progress > 0.05 && cpThread != null) {
                        long execTime = System.currentTimeMillis() - cpThread.startTime;
                        long estimate = (long) (execTime / progress);
                        Object arg[] = {(int) (100 * progress), Utils.getTimePeriodForDisplay(estimate, false, true)};
                        text = MessageFormat.format(res.getString("comparisonPnl.progressLabelPercentAndEstimate"), arg);
                    } else {
                        Object arg[] = {(int) (100 * progress)};
                        text = MessageFormat.format(res.getString("comparisonPnl.progressLabelPercent"), arg);
                    }
                    lblProgress.setText(text);
                    bar.repaint();
                }
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } while (!stop);

        }

        public void stopTimer() {
            stop = true;
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    private class CompareThread extends Thread {

        BufferedImage remoteDesktopImage;
        Image templateImage;
        Rectangle r;
        String methodParams;
        ScriptingContext repository;
        float passRate;
        ImageComparisonModule mod;
        float result;
        ComparisonPnl pnl;
        boolean showProgress;
        long startTime;

        public CompareThread(ImageComparisonModule mod, BufferedImage remoteDesktopImage, Image templateImage,
                Rectangle r, String methodParams, ScriptingContext repository, float passRate, ComparisonPnl pnl, boolean showProgress) {
            this.remoteDesktopImage = remoteDesktopImage;
            this.templateImage = templateImage;
            this.r = r;
            this.methodParams = methodParams;
            this.repository = repository;
            this.passRate = passRate;
            this.mod = mod;
            this.pnl = pnl;
            this.showProgress = showProgress;
        }

        @Override
        public void run() {
            ProgressBarUpdate pbu = null;
            try {
                result = 0;
                long time = System.currentTimeMillis();
                startTime = time;
                if (showProgress) {
                    pbu = new ProgressBarUpdate(mod, pnl.progressBar);
                    pnl.comparisonRunning = true;
                    pnl.btnCompare.setText(stpBtnText);
                    pnl.progressBar.setValue(0);
                    pbu.start();
                }
                result = mod.compare(remoteDesktopImage, r, templateImage, null, repository, passRate);
                pnl.btnCompare.setText(cmpBtnText);
                pnl.comparisonRunning = false;
                if (pbu != null) {
                    pbu.stopTimer();
                }
                pnl.progressBar.setValue(100);
                time = System.currentTimeMillis() - time;
                String displayTime = Utils.getTimePeriodForDisplay(time, false, true);
                int maxHitsLimit = UserConfiguration.getInstance().getInteger("CompareToCommand.maxSearchHits");
                Object arg[] = {displayTime, maxHitsLimit};
                boolean stopped = false;
                boolean hitsReached = false;
                if (mod instanceof Stoppable) {
                    stopped = ((Stoppable) mod).isStopped();
                }
                if (mod instanceof Measurable && mod.getMethodName().equalsIgnoreCase("search")) {
                    Map vars = repository.getVariables();
                    if (vars != null) {
                        Number cnt = (Number) vars.get("_SEARCH_MATCH_COUNT");
                        if (cnt != null && cnt.intValue() >= maxHitsLimit && ((Measurable) mod).getProgress() < 1.0f) {
                            hitsReached = true;
                        }
                    }
                }

                // Set the progress label correctly
                if (stopped) {
                    lblProgress.setText(MessageFormat.format(res.getString("comparisonPnl.comparisonStopped"), arg));
                } else if (hitsReached) {
                    lblProgress.setText(MessageFormat.format(res.getString("comparisonPnl.comparisonReachedMaxHits"), arg));
                } else {
                    lblProgress.setText(MessageFormat.format(res.getString("comparisonPnl.comparisonTime"), arg));
                }

                if (showProgress) {
                    String rect = r == null ? "" : parser.rectToString(r);
                    Object args[] = {mod.getMethodName(), 100 * passRate, rect, 100 * result};
                    String str;
                    if (r == null) {
                        str = res.getString("comparisonPnl.comparisonResultsWithoutArea");
                    } else {
                        str = res.getString("comparisonPnl.comparisonResultsWithArea");
                    }
                    String s = MessageFormat.format(str, args);

                    // Add the text from the progress label
                    s += lblProgress.getText();

                    if (mod.getMethodName().startsWith("search")) {
                        Map vars = repository.getVariables();
                        if (vars != null) {
                            Number cnt = (Number) vars.get("_SEARCH_MATCH_COUNT");
                            if (cnt != null && cnt.intValue() > 0) {
                                Vector hits = new Vector();
                                int w = templateImage.getWidth(pnl);
                                int h = templateImage.getHeight(pnl);

                                String x, y;
                                String list = "";
                                for (int i = 0; i < cnt.intValue(); i++) {
                                    x = vars.get("_SEARCH_X_" + (i + 1)).toString();
                                    y = vars.get("_SEARCH_Y_" + (i + 1)).toString();
                                    if (i < 10) {
                                        list += "\n  #" + (i + 1) + ": [" + x + "," + y + "]";
                                    }
                                    hits.add(new Rectangle(Integer.parseInt(x), Integer.parseInt(y), w, h));
                                }

                                Object args2[] = {cnt, list};
                                s += MessageFormat.format(res.getString("comparisonPnl.comparisonResultsSearch"), args2);
                                if (cnt.intValue() > 10) {
                                    Object args3[] = {cnt.intValue() - 10};
                                    s += MessageFormat.format(res.getString("comparisonPnl.comparisonResultsSearchMore"), args3);
                                }
                                String options[] = {
                                    res.getString("comparisonPnl.comparisonResultsOk"),
                                    res.getString("comparisonPnl.comparisonResultsShow")
                                };
                                int option = JOptionPane.showOptionDialog(
                                        ComparisonPnl.this, s, res.getString("comparisonPnl.comparisonResultsShowTitle"),
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                                if (option == 1) {
                                    if (dlg == null) {
                                        dlg = new SearchHitsDialog(owner, mainFrame);
                                    }
                                    dlg.setModule((ExtendedSearchCapabilities) mod);
                                    dlg.setPassRate(passRate / 100);
                                    dlg.setImages(remoteDesktopImage, preview.getFullSizeImage());
                                    dlg.setHits(hits);
                                    dlg.setVisible(true);
                                }
                            } else {
                                int option;
                                if (mainFrame.isHelpAvailable()) {
                                    String options[] = {
                                        res.getString("comparisonPnl.comparisonResultsOk"),
                                        res.getString("comparisonPnl.comparisonResultsTroubleshoot")
                                    };
                                    option = JOptionPane.showOptionDialog(
                                            ComparisonPnl.this, s,
                                            res.getString("comparisonPnl.comparisonResultsNoMatch"),
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                                    if (option == 1) {
                                        mainFrame.showHelpDialog("gui.comparison_troubleshooting", owner);
                                    }
                                } else {
                                    String options[] = {
                                        res.getString("comparisonPnl.comparisonResultsOk"),};
                                    option = JOptionPane.showOptionDialog(
                                            ComparisonPnl.this, s,
                                            res.getString("comparisonPnl.comparisonResultsNoMatch"),
                                            JOptionPane.OK_OPTION,
                                            JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                                }
//                                JOptionPane.showMessageDialog(pnl, s, res.getString("comparisonPnl.comparisonResultsNoMatch"), JOptionPane.OK_OPTION);
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(pnl, s);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if (pbu != null) {
                    pbu.stopTimer();
                }

            }
        }

        void setShowResults(boolean on) {
            showProgress = on;
        }
    }

    private class FileListPanel extends ItemListPanel {

        JFileChooser chooser;

        @Override
        public Object[] addItem() {
            if (chooser == null) {
                chooser = new ImageFileChooser(mainFrame);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            }

            chooser.setCurrentDirectory(new File(templatePath));

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                String ext = Utils.getExtension(f);
                if (ext == null || ext.length() == 0) {
                    // Default to the PNG format
                    f = new File(f.getAbsolutePath() + ".png");
                }
                return new Object[]{f, f.getAbsolutePath()};
            }
            return null;
        }
    }
}
