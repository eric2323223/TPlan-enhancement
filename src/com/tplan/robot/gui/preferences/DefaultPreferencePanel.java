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
package com.tplan.robot.gui.preferences;

import com.tplan.robot.gui.components.FileComponent;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.gui.components.CustomHyperlinkListener;
import com.tplan.robot.gui.components.FileListComponent;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.gui.editor.KeyTextField;
import com.tplan.robot.gui.preferences.color.ColorPanel;
import com.tplan.robot.gui.preferences.color.HTMLColorPanel;
import com.tplan.robot.gui.preferences.list.ListPanel;
import com.tplan.robot.util.Utils;

import java.awt.event.KeyEvent;
import java.net.URISyntaxException;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;

/**
 * <p>Default implementation of a preference panel. A preference panel is a container which displays
 * a set of user preferences. Instances of this class are added as user objects to nodes of
 * the preference tree (see {@link PreferenceTreeNode} and {@link DefaultPreferenceTreeModel}) and
 * each panel gets displayed in the right part of the {@link PreferenceDialog} when its associated tree node gets
 * selected.</p>
 *
 * <p>The class accepts preferences in form of {@link Preference} instances and creates
 * an appropriate graphical component for each such a preference based on its type. For example if the preference
 * is of type {@link Preference#TYPE_BOOLEAN}, it is displayed in the panel as a check box.</p>
 *
 * <p>Preferences may be grouped into titled containers (frames). To create a titled container use
 * the {@link #createContainer(java.lang.String)} method. Number returned by the method
 * should be passed to the {@link #addPreference(com.tplan.robot.preferences.Preference, int)} method
 * to indicate that a preference should be placed into this particular container.</p>
 *
 * <p>Another way of creation of titled containers is to set preferred container name
 * directly in the <code>Preference</code> instance through
 * the {@link Preference#setPreferredContainerName(java.lang.String)} method.
 * The panel then searches the list of existing panels for the given name and creates a new one
 * if it has not yet been created.  To apply this approach the preference must be added through
 * the {@link #addPreference(com.tplan.robot.preferences.Preference)} method because
 * container index in the {@link #addPreference(com.tplan.robot.preferences.Preference, int)}
 * method has higher priority.</p>
 *
 * <p>The preference panel may also contain other {@link PreferencePanel} instances.
 * To add such an instance use the {@link #addComponent(com.tplan.robot.gui.preferences.PreferencePanel, int)} method.
 * The panel then makes sure that all relevant {@link PreferencePanel} method calls
 * are invoked also on the encapsulated preference panel instances.</p>
 *
 * </p>To initialize the panel content, the {@link #init()} method must be called
 * after all preferences are added through the {@link #addPreference(com.tplan.robot.preferences.Preference)}
 * and {@link #addPreference(com.tplan.robot.preferences.Preference, int)} methods. A typical code sequence
 * example follows:</p>
 *
 * @product.signature
 */
public class DefaultPreferencePanel extends AbstractPreferencePanel
        implements ActionListener, ItemListener, ChangeListener, KeyListener, Runnable {

    private List options = new ArrayList();
    private JPanel contentPanel = new JPanel();
    private Map components = new HashMap();
    private List<JPanel> containers = new ArrayList();
    private int verticalInset = 1;
    private boolean createBorders = true;
    private List<Component[]> componentGridList;
    private HyperlinkListener hyperLinkListener;
    private boolean useHTML = false;
    private Map<String, ButtonGroup> buttonGroups = new HashMap();

    public DefaultPreferencePanel() {
        contentPanel.setOpaque(false);
    }

    /**
     * Add a preference to a titled container.
     * @param preference a preference.
     * @param containerIndex titled container index to add to. The container must be created
     * before it is used through the {@link #createContainer(java.lang.String)} method.
     */
    public void addPreference(Preference preference, int containerIndex) {
        options.add(preference);
        components.put(preference, containers.get(containerIndex));
    }

    /**
     * Add a preference to the default container.
     * @param preference a preference.
     */
    public void addPreference(Preference preference) {
        options.add(preference);

        JPanel currentContainer = null;
        if (preference.getPreferredContainerName() != null) {
            if (components.get(preference.getPreferredContainerName()) instanceof JPanel) {
                currentContainer = (JPanel) components.get(preference.getPreferredContainerName());
            } else {
                currentContainer = containers.get(createContainer(preference.getPreferredContainerName()));
            }
        } else {
            // Create a default container
            currentContainer = containers.get(createContainer(null));
        }
        components.put(preference, currentContainer);
    }

    /**
     * Add a preference panel component to a titled container.
     * @param component a preference panel implementation.
     * @param containerIndex titled container index to add to. The container must be created
     * before it is used through the {@link #createContainer(java.lang.String)} method.
     */
    public void addComponent(PreferencePanel component, int containerIndex) {
        options.add(component);
        components.put(component, containers.get(containerIndex));
    }

    public JComponent getComponent(String configurationKey) {
        return (JComponent) components.get(configurationKey);
    }

    /**
     * Create a titled container.
     * @param title container title.
     * @return index of the container.
     */
    public int createContainer(String title) {
        JPanel container = new JPanel();
        container.setOpaque(this.isOpaque());
        if (createBorders) {
            Border border = BorderFactory.createEtchedBorder(Color.white, new Color(134, 134, 134));
            if (title != null) {
                border = new TitledBorder(border, title);
            }
            container.setBorder(border);
        }
        container.setLayout(new GridBagLayout());
        containers.add(container);
        if (title != null) {
            components.put(title, container);
        }
        return containers.indexOf(container);
    }

    /**
     * Set the values from the configuration in the panel components.
     * @param configuration a configuration instance.
     */
    @Override
    public void loadPreferences(Object configuration) {
        if (configuration instanceof UserConfiguration) {
            UserConfiguration cfg = (UserConfiguration) configuration;

            JComponent comp;
            Object obj;
            Preference o;
            int type;
            for (int i = 0; i < options.size(); i++) {
                obj = options.get(i);
                if (obj instanceof PreferencePanel) {
                    ((PreferencePanel) obj).loadPreferences(configuration);
                } else if (obj instanceof Preference) {
                    o = (Preference) obj;
                    comp = (JComponent) components.get(o.getConfigurationKey());
                    type = o.getType();
                    if (type == Preference.TYPE_BOOLEAN || type == Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN) {
                        Boolean b = null;
                        if (type == Preference.TYPE_BOOLEAN) {
                            b = cfg.getBoolean(o.getConfigurationKey());
                        } else {
                            Integer bi = cfg.getInteger(o.getConfigurationKey());
                            if (bi != null) {
                                b = new Boolean(bi.intValue() < 0);
                            }
                        }
                        if (b == null) {
                            if (o.getDefaultValue() != null && o.getDefaultValue() instanceof Boolean) {
                                ((AbstractButton) comp).setSelected((Boolean) o.getDefaultValue());

                            } else {
                                ((AbstractButton) comp).setSelected(false);
                            }
                        } else {
                            ((AbstractButton) comp).setSelected(b.booleanValue());
                        }
                    } else if (type == Preference.TYPE_INT || type == Preference.TYPE_FLOAT) {
                        Number value = cfg.getDouble(o.getConfigurationKey());
                        if (value == null) {
                            if (o.getDefaultValue() != null && o.getDefaultValue() instanceof Number) {
                                value = ((Number) o.getDefaultValue()).doubleValue();
                            } else {
                                value = new Double(0);
                            }
                        }
                        ((JSpinner) comp).setValue(value);
                    } else if (type == Preference.TYPE_STRING || type == Preference.TYPE_PASSWORD) {
                        String cfgValue = cfg.getString(o.getConfigurationKey());
                        if (cfgValue == null && o.getDefaultValue() != null) {
                            cfgValue = o.getDefaultValue().toString();
                        }
                        if (o.getDisplayValues() != null && comp instanceof JComboBox) {
                            int index = o.getValues().indexOf(cfgValue);
                            JComboBox box = (JComboBox) comp;
                            if (index >= 0 && index < o.getValues().size()) {
                                box.setSelectedIndex(index);
                            } else if (box.isEditable()) {
                                DefaultComboBoxModel model = (DefaultComboBoxModel) box.getModel();
                                model.addElement(cfgValue);
                                box.setModel(model);
                                box.setSelectedItem(cfgValue);
                            }
                        } else {
                            ((JTextField) comp).setText(cfgValue);
                        }
                    } else if (type == Preference.TYPE_TEXT) {
                        String text = cfg.getString(o.getConfigurationKey());
                        text = Utils.convertStringToMultiline(text);
                        JTextArea area = (JTextArea) ((JScrollPane) comp).getViewport().getView();
                        area.setText(text);
                    } else if (type == Preference.TYPE_COLOR) {
                        ((ColorPanel) comp).setSelectedColor(cfg.getColor(o.getConfigurationKey()));
                    } else if (type == Preference.TYPE_COLOR_FROM_IMAGE) {
                        ((HTMLColorPanel) comp).setSelectedColor(cfg.getColor(o.getConfigurationKey()));
                    } else if (type == Preference.TYPE_KEYSTROKE) {
                        ((KeyTextField) comp).setText(cfg.getString(o.getConfigurationKey()));
                    } else if (type == Preference.TYPE_STRINGLIST) {
                        ((ListPanel) comp).setValues(cfg.getListOfStrings(o.getConfigurationKey()));
                    } else if (o.getType() == Preference.TYPE_FILE || o.getType() == Preference.TYPE_DIRECTORY) {
                        String file = cfg.getString(o.getConfigurationKey());
                        if (file == null && o.getDefaultValue() != null) {
                            file = o.getDefaultValue().toString();
                        }
                        ((FileComponent) comp).setText(file);
                    } else if (o.getType() == Preference.TYPE_FILELIST) {
                        List<String> values = cfg.getListOfStrings(o.getConfigurationKey());

                        // The file list requires the table of descriptions so let's create one.
                        Map<File, String> descs = new HashMap();
                        List<File> lf = new ArrayList();
                        File f;
                        if (values != null) {
                            for (String s : values) {
                                f = new File(s);
                                lf.add(f);
                                descs.put(f, f.getAbsolutePath());
                            }
                        }
                        ((FileListComponent) comp).setDisplayValuesTable(descs);
                        ((FileListComponent) comp).setValues(lf);
                    } else if (o.getType() == Preference.TYPE_DUMMY) {
                        // Nothing to do (dummy preference)
                    }
                }
            }
        }
        super.loadPreferences(configuration);
    }

    /**
     * Save the values from the panel components into the configuration.
     * @param configuration a configuration instance.
     */
    @Override
    public void savePreferences(Object configuration) {
        if (configuration instanceof UserConfiguration) {
            UserConfiguration cfg = (UserConfiguration) configuration;

            JComponent comp;
            Object obj;
            Preference o;
            int type;
            for (int i = 0; i < options.size(); i++) {
                obj = options.get(i);
                if (obj instanceof PreferencePanel) {
                    ((PreferencePanel) obj).savePreferences(configuration);
                } else if (obj instanceof Preference) {
                    o = (Preference) options.get(i);
                    comp = (JComponent) components.get(o.getConfigurationKey());
                    type = o.getType();

                    if (type == Preference.TYPE_BOOLEAN || type == Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN) {
                        boolean b = ((AbstractButton) comp).isSelected();
                        if (type == Preference.TYPE_BOOLEAN) {
                            if (o.getDefaultValue() != null && o.getDefaultValue() instanceof Boolean) {
                                if (b != (Boolean) o.getDefaultValue()) {
                                    cfg.setBoolean(o.getConfigurationKey(), new Boolean(b));
                                }
                            } else {
                                cfg.setBoolean(o.getConfigurationKey(), new Boolean(b));
                            }
                        } else {
                            cfg.setInteger(o.getConfigurationKey(), new Integer(b ? -1 : 0));
                        }
                    } else if (type == Preference.TYPE_INT) {
                        Number n = (Number) ((JSpinner) comp).getValue();
                        if (n != null && o.getDefaultValue() instanceof Number
                                && n.intValue() == ((Number) o.getDefaultValue()).intValue()) {
                            cfg.remove(o.getConfigurationKey());
                        } else {
                            cfg.setInteger(o.getConfigurationKey(), n);
                        }
                    } else if (type == Preference.TYPE_FLOAT) {
                        cfg.setDouble(o.getConfigurationKey(), (Number) ((JSpinner) comp).getValue());
                    } else if (type == Preference.TYPE_STRING || type == Preference.TYPE_PASSWORD) {
                        if (comp instanceof JComboBox) {
                            JComboBox box = (JComboBox) comp;
                            Object value = null;
                            if (box.isEditable()) {
                                value = box.getSelectedItem();
                            } else if (o.getValues() != null) {
                                int index = ((JComboBox) comp).getSelectedIndex();
                                index = index < 0 ? 0 : index;
                                if (index < o.getValues().size()) {
                                    value = o.getValues().get(index);
                                } else {
                                    value = box.getSelectedItem();
                                }
                            }
                            cfg.setString(o.getConfigurationKey(), value.toString());
                        } else if (comp instanceof JTextField) {
                            JTextField f = (JTextField) comp;
                            // 2.0.3 enhancement - allow to skip empty values if configured
                            String text = ((JTextField) comp).getText();
                            if (o.isAcceptEmptyValue() || (text != null && text.length() > 0)) {
                                cfg.setString(o.getConfigurationKey(), text);
                            }
                        }
                    } else if (type == Preference.TYPE_TEXT) {
                        JTextArea area = (JTextArea) ((JScrollPane) comp).getViewport().getView();
                        cfg.setString(o.getConfigurationKey(), Utils.convertMultilineToString(area.getText()));
                    } else if (type == Preference.TYPE_COLOR) {
                        ColorPanel pnl = (ColorPanel) comp;
                        cfg.setColors(o.getConfigurationKey(), new Color[]{pnl.getSelectedColor()});
                    } else if (type == Preference.TYPE_COLOR_FROM_IMAGE) {
                        HTMLColorPanel pnl = (HTMLColorPanel) comp;
                        cfg.setColors(o.getConfigurationKey(), new Color[]{pnl.getSelectedColor()});
                    } else if (type == Preference.TYPE_KEYSTROKE) {
                        cfg.setString(o.getConfigurationKey(), ((KeyTextField) comp).getText());
                    } else if (type == Preference.TYPE_STRINGLIST) {
                        cfg.setListOfObjects(o.getConfigurationKey(), ((ListPanel) comp).getValues());
                    } else if (o.getType() == Preference.TYPE_FILE || o.getType() == Preference.TYPE_DIRECTORY) {
                        String text = ((FileComponent) comp).getText();
                        cfg.setString(o.getConfigurationKey(), text);
                        if (o.isAcceptFileAsUri()) {
                            try {
                                URI uri = new URI(text);
                                cfg.setString(o.getConfigurationKey(), uri.toString());
                            } catch (URISyntaxException ex) {  // Not an URI, looks like a file
                                File f = new File(text);
                                if (f.canRead()) {
                                    cfg.setString(o.getConfigurationKey(), f.toURI().toString());
                                }
                            }
                        }
                    } else if (o.getType() == Preference.TYPE_FILELIST) {
                        cfg.setListOfObjects(o.getConfigurationKey(), ((FileListComponent) comp).getValues());
                    } else if (o.getType() == Preference.TYPE_DUMMY) {
                        // Nothing to do (dummy preference)
                    }
                }
            }
        }
        super.savePreferences(configuration);
    }

    public void init() {
        setLayout(new BorderLayout());
        contentPanel.setLayout(new GridBagLayout());
        add(contentPanel, BorderLayout.NORTH);
        componentGridList = new ArrayList();
        Component ca[];

        Preference o;
        Object obj;
        GridBagConstraints c;
        int layoutLine = 0;
        JPanel currentContainer = null;
        Map depsCache = new HashMap();

        for (int j = 0; j < containers.size(); j++) {
            currentContainer = (JPanel) containers.get(j);
            c = new GridBagConstraints(0, j, 1, 1, 1.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(verticalInset, 1, verticalInset, 1), 0, 0);
            contentPanel.add(currentContainer, c);
            layoutLine = 0;

            for (int i = 0; i < options.size(); i++) {

                obj = options.get(i);

                // Test if the preference belongs to the current container
                if (!currentContainer.equals(components.get(obj))) {
                    continue;
                }

                if (obj instanceof PreferencePanel) {
                    if (obj instanceof JComponent) {
                        currentContainer.add((JComponent) obj, c);
                    }
                } else if (obj instanceof Preference) {
                    o = (Preference) options.get(i);

                    String desc = o.getDescription();
                    if (desc != null && desc.trim().length() > 0) {
                        c = new GridBagConstraints(0, layoutLine, 2, 1, 1.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(verticalInset, 1, verticalInset, 1), 0, 0);

                        desc = desc.trim();
                        boolean containsHTML = desc.length() > 5 && desc.substring(0, 6).equalsIgnoreCase("<html>");
                        if (useHTML || containsHTML) {
                            JEditorPane jta = new JEditorPane();
                            jta.setContentType("text/html");
                            if (hyperLinkListener != null) {
                                jta.addHyperlinkListener(hyperLinkListener);
                            } else {
                                jta.addHyperlinkListener(new CustomHyperlinkListener((MainFrame) SwingUtilities.getAncestorOfClass(MainFrame.class, jta)));
                            }

                            // Set the JEditorPane default font to the JLabel one
                            Font font = UIManager.getFont("Label.font");
                            String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
                            ((HTMLDocument) jta.getDocument()).getStyleSheet().addRule(bodyRule);


                            jta.setText(o.getDescription());
                            jta.setEditable(false);
                            jta.setOpaque(false);
                            currentContainer.add(jta, c);
                            getComponentGridList().add(new Component[]{jta});
                        } else {
                            JTextArea jta = new JTextArea(o.getDescription());
                            jta.setEditable(false);
                            jta.setOpaque(false);
                            jta.setWrapStyleWord(true);
                            currentContainer.add(jta, c);
                            getComponentGridList().add(new Component[]{jta});
                        }
                        layoutLine++;
                    }

                    JComponent comp = null;
                    boolean useLabel = true;

                    if (o.getType() == Preference.TYPE_BOOLEAN || o.getType() == Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN) {
                        AbstractButton btn;
                        if (o.getButtonGroup() != null) {
                            btn = new JRadioButton(o.getLabel());
                            ButtonGroup bg = buttonGroups.get(o.getButtonGroup());
                            if (bg == null) {
                                bg = new ButtonGroup();
                                buttonGroups.put(o.getButtonGroup(), bg);
                            }
                            bg.add(btn);
                        } else {
                            btn = new JCheckBox(o.getLabel());
                        }
                        btn.setOpaque(this.isOpaque());
                        if (o.getDefaultValue() != null && o.getDefaultValue() instanceof Boolean) {
                            btn.setSelected(((Boolean) o.getDefaultValue()));
                        }
                        btn.addItemListener(this);
                        comp = btn;
                        useLabel = false;
                    } else if (o.getType() == Preference.TYPE_STRING) {
                        if (o.getDisplayValues() != null) {
                            DefaultComboBoxModel model = new DefaultComboBoxModel(o.getDisplayValues().toArray());
                            comp = new JComboBox(model);
                            ((JComboBox) comp).setEditable(!o.isSelectOnly());
                            if (o.getDefaultValue() != null) {
                                ((JComboBox) comp).setSelectedItem(o.getDefaultValue());
                            }
                            ((JComboBox) comp).addActionListener(this);
                        } else {
                            comp = new JTextField();
                            if (o.getDefaultValue() != null) {
                                ((JTextField) comp).setText(o.getDefaultValue().toString());
                            }
                            ((JTextField) comp).addActionListener(this);
                            ((JTextField) comp).addKeyListener(this);
                        }
                    } else if (o.getType() == Preference.TYPE_TEXT) {
                        JTextArea area = new JTextArea();
                        int rows = o.getTextRowsToDisplay();
                        rows = rows < 0 ? 10 : rows;
                        area.setRows(rows);
                        comp = new JScrollPane(area);
                        Object text = o.getDefaultValue();
                        if (text != null) {
                            area.setText(text.toString());
                        }
                        useLabel = false;
                    } else if (o.getType() == Preference.TYPE_PASSWORD) {
                        comp = new JPasswordField();
                        ((JTextField) comp).addActionListener(this);
                    } else if (o.isNumber()) {
                        JSpinner sp = new JSpinner();
                        if (o.getType() == Preference.TYPE_FLOAT) {
                            sp.setModel(new SpinnerNumberModel((double) o.getMinValue(), (double) o.getMinValue(), (double) o.getMaxValue(), 1.0d));
                        } else {
                            sp.setModel(new SpinnerNumberModel(o.getMinValue(), o.getMinValue(), o.getMaxValue(), 1));
                        }
                        comp = sp;
                        if (o.getDefaultValue() != null && o.getDefaultValue() instanceof Number) {
                            sp.setValue(o.getDefaultValue());
                        }
                        sp.addChangeListener(this);
                    } else if (o.getType() == Preference.TYPE_FILE || o.getType() == Preference.TYPE_DIRECTORY) {
                        FileComponent fc = new FileComponent(o.getType() == Preference.TYPE_DIRECTORY);
                        comp = fc;
                        if (o.getAcceptedFileExtensions() != null) {
                            fc.addFileExtensionFilter(o.getAcceptedFileExtensions(), o.getAcceptedFileExtensionsDesc());
                        }
                        JComponent cac = o.getFileAccessory();
                        if (cac != null) {
                            JFileChooser chooser = fc.getFileChooser();
                            chooser.setAccessory(cac);
                            if (cac instanceof PropertyChangeListener) {
                                chooser.removePropertyChangeListener((PropertyChangeListener) cac);
                                chooser.addPropertyChangeListener((PropertyChangeListener) cac);
                            }
                        }
                        if (o.getDirFileToSearchFor() != null) {
                            fc.setFileToSearchFor(o.getDirFileToSearchFor(), o.getDirDefaultSearchPath(),
                                    o.isDirSearchUseRegularExpressions(), o.isDirSearchIgnoreCase(), o.isDirSearchForceValueToFound());
                        }
                    } else if (o.getType() == Preference.TYPE_FILELIST) {
                        comp = new FileListComponent();
                        if (o.getAcceptedFileExtensions() != null) {
                            ((FileListComponent) comp).addFileExtensionFilter(o.getAcceptedFileExtensions(), o.getAcceptedFileExtensionsDesc());
                        }
                        JComponent cac = o.getFileAccessory();
                        if (cac != null) {
                            JFileChooser chooser = ((FileListComponent) comp).getFileChooser();
                            chooser.setAccessory(cac);
                            if (cac instanceof PropertyChangeListener) {
                                chooser.removePropertyChangeListener((PropertyChangeListener) cac);
                                chooser.addPropertyChangeListener((PropertyChangeListener) cac);
                            }
                        }
                    } else if (o.getType() == Preference.TYPE_COLOR) {
                        comp = new ColorPanel();
                    } else if (o.getType() == Preference.TYPE_COLOR_FROM_IMAGE) {
                        HTMLColorPanel cp = new HTMLColorPanel();
                        cp.setImageProvider(o.getImageProvider());
                        cp.addChangeListener(this);
                        comp = cp;

                    } else if (o.getType() == Preference.TYPE_KEYSTROKE) {
                        KeyTextField field = new KeyTextField();
                        field.setValues(Utils.getKeyCodeTable());
                        comp = field;
                    } else if (o.getType() == Preference.TYPE_STRINGLIST) {
                        ListPanel pnl = new ListPanel();
                        pnl.setDisplayValuesTable(o.getDisplayValuesTable());
                        pnl.setLabelLeft(o.getLabel());
                        if (o.getDescriptionOfAvailableValues() != null) {
                            pnl.setLabelRight(o.getDescriptionOfAvailableValues());
                        }
                        useLabel = false;
                        comp = pnl;
                    } else if (o.getType() == Preference.TYPE_DUMMY) {
                        comp = null;
                        useLabel = false;
                    }

                    if (comp != null) {
                        components.put(o.getConfigurationKey(), comp);
                        if (o.getDependentOption() != null) {
                            depsCache.put(o.getDependentOption(), comp);
                        }
                        JLabel label = null;
                        if (useLabel && o.getLabel() != null) {
                            label = new JLabel(o.getLabel());
                            c = new GridBagConstraints(0, layoutLine, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(verticalInset, 1, verticalInset, 5), 0, 0);
                            currentContainer.add(label, c);
                            c = new GridBagConstraints(1, layoutLine, 1, 1, 1.0, 0.0,
                                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(verticalInset, 1, verticalInset, 1), 0, 0);
                            label.setLabelFor(comp);
                            final JLabel l = label;
                            comp.addPropertyChangeListener(new PropertyChangeListener() {

                                public void propertyChange(PropertyChangeEvent evt) {
                                    l.setEnabled(l.getLabelFor().isEnabled());
                                }
                            });
                        } else {
                            c = new GridBagConstraints(0, layoutLine, 2, 1, 1.0, 0.0,
                                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(verticalInset, 1, verticalInset, 1), 0, 0);
                        }
                        if (o.getPreferredContainerName() != null) {
                            if (components.get(o.getPreferredContainerName()) instanceof JPanel) {
                                currentContainer = (JPanel) components.get(o.getPreferredContainerName());
                            } else {
                                currentContainer = containers.get(createContainer(o.getPreferredContainerName()));
                            }
                        } else if (currentContainer == null) {
                        }
                        if (label != null) {
                            getComponentGridList().add(new Component[]{label, comp});
                        } else {
                            getComponentGridList().add(new Component[]{comp});
                        }
                        currentContainer.add(comp, c);
                        layoutLine++;
                    }
                }
            }

        }
        Iterator en = depsCache.keySet().iterator();
        while (en.hasNext()) {
            String key = (String) en.next();
            obj = components.get(key);
            if (obj != null && obj instanceof JComponent) {
                addItemListenerToCheckbox((Component) depsCache.get(key), (JComponent) obj);
            }
        }
    }

    public void setVerticalInset(int verticalInset) {
        this.verticalInset = verticalInset;
    }

    private void addItemListenerToCheckbox(Component comp, JComponent box) {
        final Component component = comp;
        final JComponent control = box;
        if (box instanceof AbstractButton) {
            final AbstractButton checkBox = (AbstractButton) box;
            checkBox.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    AbstractButton box = (AbstractButton) e.getSource();
                    component.setEnabled(box.isSelected() && box.isEnabled());
                    firePropertyChange("valueUpdated", null, box);
                }
            });
        }
        box.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName() != null && evt.getPropertyName().equals("enabled")) {
                    boolean enable = control.isEnabled();
                    if (control instanceof AbstractButton) {
                        enable = enable && ((AbstractButton) control).isSelected();
                    }
                    component.setEnabled(enable);
                }
            }
        });
        boolean enable = control.isEnabled();
        if (control instanceof AbstractButton) {
            enable = enable && ((AbstractButton) control).isSelected();
        }
        comp.setEnabled(enable);
    }

    public void actionPerformed(ActionEvent e) {
        firePropertyChange("valueUpdated", null, e.getSource());
    }

    public void itemStateChanged(ItemEvent e) {
        firePropertyChange("valueUpdated", null, e.getSource());
    }

    public void stateChanged(ChangeEvent e) {
        firePropertyChange("valueUpdated", null, e.getSource());
    }

    /**
     * @return the createBorders
     */
    public boolean isCreateBorders() {
        return createBorders;
    }

    /**
     * @param createBorders the createBorders to set
     */
    public void setCreateBorders(boolean createBorders) {
        this.createBorders = createBorders;
    }

    /**
     * @return the componentGridList
     */
    public List<Component[]> getComponentGridList() {
        return componentGridList;
    }

    /**
     * @param hyperLinkListener the hyperLinkListener to set
     */
    public void setHyperLinkListener(HyperlinkListener hyperLinkListener) {
        this.hyperLinkListener = hyperLinkListener;
    }

    /**
     * @return the useHTML
     */
    public boolean isUseHTML() {
        return useHTML;
    }

    /**
     * @param useHTML the useHTML to set
     */
    public void setUseHTML(boolean useHTML) {
        this.useHTML = useHTML;
    }

    public void setShowKeysInToolTips(boolean show) {
        JComponent comp;
        Object obj;
        Preference o;
        final ToolTipManager tm = ToolTipManager.sharedInstance();
        for (int i = 0; i < options.size(); i++) {
            obj = options.get(i);
            if (obj instanceof DefaultPreferencePanel) {
                ((DefaultPreferencePanel) obj).setShowKeysInToolTips(show);
            } else if (obj instanceof Preference) {
                o = (Preference) options.get(i);
                comp = (JComponent) components.get(o.getConfigurationKey());
                String s = show ? o.getSummary() : null;
                comp.setToolTipText(s);
                Object label = comp.getClientProperty("labeledBy");
                if (label instanceof JComponent) {
                    ((JComponent) label).setToolTipText(s);
                }
                if (comp instanceof Container) {
                    setShowKeysInToolTips((Container) comp, s);
                }
            }
        }
    }

    private void setShowKeysInToolTips(Container container, String text) {
        for (Component c : container.getComponents()) {
            if (c instanceof Container) {
                setShowKeysInToolTips((Container) c, text);
            } else if (c instanceof JComponent) {
                ((JComponent) c).setToolTipText(text);
            }
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Object o : components.values()) {
            if (o instanceof Component) {
                ((Component) o).setEnabled(enabled);
            }
        }
    }

    public void keyTyped(KeyEvent e) {
        SwingUtilities.invokeLater(this);
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void run() {
        firePropertyChange("valueUpdated", null, this);
    }
}
