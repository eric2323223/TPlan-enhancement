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
package com.tplan.robot.gui;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

/**
 * <p>This class allows to define menu and tool bar of an application through
 * a Java property file. It is very useful because it allows to update the menu
 * and tool bar contents without changing the code.</p>
 *
 * <p>To define a menu and a toolbar you need two resource bundles, a structure
 * bundle and a label one. Though you can put everything into one file, it is
 * not recommended. The following code shows an example of how
 * to write the property files.
 *
 * <p><u>Structure bundle <code>Structure.properties</code>:</u>
 *
 * <blockquote>
 * <pre>
 * # Root menu definition - this will define how many menus will be on the menu bar.
 * # In this case we will create just two menus, menu 'File' and 'Help'.
 * # We will also create a tool bar with two buttons, 'Open' and 'Show Name'.
 * menu.SampleApp=menu.File menu.Help
 *
 * # Space separated list of menu items under the 'File' menu.
 * # Item menuitem.Open is a regular menu item.
 * # Item menuitem.Reopen is a submenu (ends with '->').
 * # A minus '-' will insert a menu separator.
 * # Item menuitem.ShowName will be a toggle (selectable) menu item (ends with '*'.
 *
 * menu.File=menuitem.Open menuitem.Reopen-> - menuitem.ShowName*
 *
 * # Definition of action names of the File menu items.
 * # These names will act as identifiers of the menu item and eventually corresponding toolbar button.
 * # To define an action name for an item, take the item property key and add 'Action'.
 *
 * menuitem.OpenAction=open
 * menuitem.ReopenAction=reopen
 * menuitem.ShowNameAction=showName
 *
 * # If you need to define a shortcut key for an item, add a trailing 'Shortcut'.
 * menuitem.OpenShortcut=Ctrl+O
 *
 * # List of menu items under the 'Help' menu. We'll define just one 'Help' menu item.
 * menu.Help=menuitem.Help
 * menuitem.HelpAction=help
 * menuitem.HelpShortcut=F10
 *
 * # Toolbar definition
 * # Note that you will not have to redefine the shortcut for the 'Open' button. It will be defined automatically
 * # because the action names of the menu item and toolbar buttons are the same.
 * toolbar.SampleApp=btn.Open btn.ShowName*
 * btn.OpenAction=open
 * btn.ShowNameAction=showName
 *
 * # Load icons for the specified buttons
 * btn.OpenIcon=open16.gif
 * btn.ShowNameIcon=showName16.gif
 *
 * </pre></blockquote>
 *
 * <u>Label bundle <code>Labels.properties</code>:</u>
 *
 * <blockquote>
 * <pre>
 * # This file defines texts of the menu and toolbar objects - labels and tool tips.
 * # Note that the comma and letter at the and will not be displayed and the letter will serve as menu accelerator
 * (Alt+&lt;letter&gt;).
 *
 * # File menu labels & tool tips
 * menu.FileLabel=File
 * menuitem.OpenLabel=Open,O
 * menuitem.OpenTip=Open a file
 * menuitem.ReopenLabel=Reopen,R
 * menuitem.ReopenTip=Reopen a file
 * menuitem.ShowNameLabel=Show Name,S
 * menuitem.ShowNameTip=Select to display the file name, deselect to hide it
 *
 * # Help menu labels
 * menu.Help=Help
 * menuitem.HelpLabel=Help
 *
 * # Toolbar labels (just tool tips in this case because we prefer icons)
 * btn.OpenLabel=Open
 * btn.ShowNameLabel=Select to display the file name, deselect to hide it
 * </pre></blockquote>
 *
 * <p>
 * Now you can use the following code to create the menu and toolbar button and handle the menu/tool bar actions:
 *
 * <blockquote>
 * <pre>
 * class MyMenuHandler implements ActionListener {
 *    public MyMenuHandler() {
 *       ActionManager am;
 *       try {
 *         am = new ActionManager(
 *             new PropertyResourceBundle(getClass().getResourceAsStream("Structure.properties")),
 *             new PropertyResourceBundle(getClass().getResourceAsStream("Labels.properties"))
 *         );
 *       } catch (IOException ex) {
 *         System.out.println("One of the resource bundles was not found.");
 *       }
 *
 *       // Create the menu and add it to your application
 *       JMenuBar mbar = createMenubar("menu.SampleApp");
 *       am.addActionListener(this);
 *       ...
 *
 *       // Create the tool bar and add it to your application
 *       JToolBar tbar = createToolbar("toolbar.SampleApp", this);
 *       ...
 *    }
 *
 *    public void actionPerformed(ActionEvent e) {
 *      if (e.getActionCommand().equals("open") {
 *         // Implement the functionality of the 'Open' menu item and tool bar here
 *         ...
 *      } else if (e.getActionCommand().equals("reopen") {
 *         // Implement the functionality of the 'Open' menu item and tool bar here
 *         ...
 *      }
 *    }
 * }
 * </pre></blockquote>
 * @product.signature
 */
public class ActionManager implements ItemListener {

    /**
     * Resource bundle with the menu and toolbar structure.
     */
    private ResourceBundle structBundle = null;
    /**
     * Resource bundle with the menu and button texts for display.
     */
    private ResourceBundle displayBundle = null;
    /**
     * Hash table with available actions.
     */
    private Map<String, Action> actionMap = new HashMap();
    /**
     * Hash table with menus and menu items.
     */
    private Map<String, JMenuItem> menuItemMap = new HashMap();
    /**
     * Hash table with toolbar buttons.
     */
    private Map<String, AbstractButton> toolbarBtnMap = new HashMap();
    /**
     * Help provider to register the help keys to.
     */
    private HelpProvider helpProvider;
    /**
     * User configuration
     */
    private UserConfiguration cfg;
    public static String HOT_KEY_SEPARATOR = ",";
    public String SUFFIX_TEXT = "Text";
    public String SUFFIX_TOOLTIP = "Tip";
    public String SUFFIX_ACTION = "Action";
    public String SUFFIX_SUBMENU = "->";
    public String SUFFIX_ACCELERATOR = "Acc";
    public String SUFFIX_ICON = "Icon";
    public String SUFFIX_TOGGLE_MENU_ITEM = "*";
    public String MENU_SEPARATOR = "-";

    /**
     * Constructor.
     *
     * @param helpProvider a help provider allows to map the actions onto a
     * particular help topic.
     * @param structBundle resource bundle with the menu and toolbar structure.
     * @param displayBundle resource bundle with the menu and button texts for display.
     * @param cfg user configuration.
     */
    public ActionManager(HelpProvider helpProvider,
            ResourceBundle structBundle, ResourceBundle displayBundle, UserConfiguration cfg) {
        this.helpProvider = helpProvider;
        this.structBundle = structBundle;
        this.displayBundle = displayBundle;
        this.cfg = cfg;
    }

    /**
     * Add the [action name, action] pairs to the action map. If you use this
     * method to pass predefined actions before you call the menu/tool bar
     * creation methods, the actions will be then mapped onto the menu items
     * and/or toolbar buttons with the same action name. This is used mainly
     * for dynamically created menus like the list of recently opened files
     * or connected servers.
     *
     * @param actions an array of actions.
     */
    public void addActions(Action[] actions) {
        // Fill the table with menu actions
        if (actions != null) {

            Action a;
            for (int i = 0; i < actions.length; i++) {
                a = actions[i];
                actionMap.put(a.getValue(Action.NAME).toString(), a);
            }
        }
    }

    /**
     * Add an action listener to all menu items and corresponding toolbar buttons
     * which are found in the menu and toolbar button maps. Use this method to
     * bind the menu and toolbar with an object providing functionality of the particular
     * actions.
     *
     * @param a class implementing the ActionListener interface.
     */
    public void addActionListener(ActionListener a) {

        // Add the action listener to all menu items
        for (AbstractButton o : menuItemMap.values()) {
            o.removeActionListener(a);
            o.addActionListener(a);
        }

        // Add the action listener to all toolbar buttons
        for (AbstractButton o : toolbarBtnMap.values()) {
            o.removeActionListener(a);
            o.addActionListener(a);
        }
    }

    /**
     * Get a menu item from the menu map associated with the given action name.
     *
     * @param action menu item action name.
     * @return menu item stored in the menu map under the argument action or
     * null if it is not there.
     */
    public JMenuItem getMenuItem(String action) {
        return menuItemMap.get(action);
    }

    /**
     * Get a toolbar button from the button table. The key is typically the name of the toolbar button action.
     *
     * @param key a key from the structure file which identifies the toolbar button.
     * @return an object (menu item) stored in the button table under the argument key or null if it is not there.
     */
    public Object getToolbarButton(String key) {
        return toolbarBtnMap.get(key);
    }

    /**
     * Load a string from the given resource bundle. This is just a convenience
     * method which returns null instead of throwing a MissingResourceException
     * in case that the key is not found.
     *
     * @param bundle a resource bundle.
     * @param key a property key.
     */
    private String getString(ResourceBundle bundle, String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
        }
        return null;
    }

    /**
     * Create the menu bar (JMenuBar instance) from the structure and display
     * bundles which were passed to this class instance in constructor.
     * The method parses the root menu entry and calls method createMenu()
     * for every menu declaration.
     *
     * @param rootKey root key in the structure bundle which defines the menu.
     * @return a new JMenuBar instance created according to the structure bundle.
     */
    public JMenuBar createMenubar(String rootKey) {
        JMenuBar menuBar = new JMenuBar();

        // Parse the root key for tokens separated by a space
        String[] keys = getString(structBundle, rootKey).split("\\s");
        JMenu menu;
        for (String key : keys) {
            menu = createMenu(key);
            if (menu != null) {
                menuBar.add(menu);
            }
        }
        return menuBar;
    }

    /**
     * Create a menu defined under the given key in the structure and label bundles. T
     * he method parses the key value and calls method createMenuItem() for every
     * menu item found in the definition.
     *
     * <p>The method also automatically registers each menu with the help module
     * making it possible to open its context help.
     *
     * @param key a key identifying the menu in the structure bundle.
     * @return a new menu (JMenu instance) created according to the structure bundle.
     */
    private JMenu createMenu(String rootKey) {

        // Get menu structure from the struct bundle
        String menuStruct = getString(structBundle, rootKey);
        if (menuStruct == null) {
            throw new IllegalStateException("ERROR: Key " + rootKey + " defining a menu is missing. The menu structure bundle may be crippled.");
        }

        // Create a new menu, set its text and eventual hot key which may
        // be present as a text suffix separated by the hot key separator
        JMenu menu = new JMenu();
        String text = getString(displayBundle, rootKey + SUFFIX_TEXT);
        setTextAndMnemonic(menu, text);

        // Parse menu tokens separated by a space
        String[] keys = menuStruct.split("\\s");

        // Iterate over menu structure keys and process them.
        for (String key : keys) {
            if (key.endsWith(SUFFIX_SUBMENU)) {  // The key is a menu
                String subMenuKey = key.substring(0, key.length() - SUFFIX_SUBMENU.length());
                JMenu subMenu = createMenu(subMenuKey);

                // Enable the menu only if at least one of its items is enabled
                boolean enable = true;
                for (int i = 0; i < subMenu.getItemCount(); i++) {
                    if (subMenu.getItem(i).isEnabled()) {
                        enable = true;
                        break;
                    }
                }
                subMenu.setEnabled(enable);

                menu.add(subMenu);
            } else if (MENU_SEPARATOR.equals(key)) {  // The key is a menu separator constant
                menu.addSeparator();
            } else {                                  // The key is a regular menu item
                menu.add(createMenuItem(key));
            }
        }

        // Register the menu with the JavaHelp using the help id of the first menu item
        if (menu.getItemCount() > 0) {
            String action = menu.getItem(0).getActionCommand();
            if (action != null && !"".equals(action)) {
                String helpKey = createHelpKeyForAction(action);
                if (helpKey != null && helpProvider != null) {
                    helpProvider.setHelpId(menu, helpKey);
                }
            }
        }

        menuItemMap.put(rootKey, menu);
        return menu;
    }

    private void setTextAndMnemonic(JMenuItem item, String text) {
        if (text != null) {
            int hotKeyIndex = text.indexOf(HOT_KEY_SEPARATOR);

            if (hotKeyIndex >= 0) {
                item.setMnemonic(text.charAt(hotKeyIndex + 1));
                text = text.substring(0, hotKeyIndex);
            }
            item.setText(text);
        } else {
            item.setText("[TEXT NOT FOUND]");
        }
    }

    /**
     * Create a help key referencing a particular JavaHelp topic for the given
     * action name. This method implementation is subject to the help content
     * structure and for {@product.name} it returns <code>"gui.menu_" + action</code>.
     * Should you wish to reuse this class for another application, reimplement
     * this method to meet your help key set structure.
     *
     * @param action action name of a menu item and/or toolbar button.
     * @return help key referencing the help page of the given action, or null if
     * no help for the action is available.
     */
    public String createHelpKeyForAction(String action) {
        return "gui.menu_" + action;
    }

    /**
     * <p>Create a menu item defined under the given key in the structure and display bundles.
     * The following rules apply:
     * <ul>
     * <li>A JMenuItem is created by default. If the key ends with suffix defined
     * by the SUFFIX_TOGGLE_MENU_ITEM (by default an asterisk), e.g. 'menuitem.ShowFlag*',
     * a JCheckBoxMenuItem instance is created instead and the asterisk is removed from the key.
     * <li>The method appends a keyword defined by the SUFFIX_TEXT constant (default: "Label")
     * to the key and uses it to load the menu item text from the label bundle.
     * Example of a key is 'menuitem.ShowFlagLabel'.
     * <li>A keyword 'Action' is appended to the key and the string used to load the action name.
     * <li>A keyword 'Shortcut' is appended to the key and the string used to load the shortcut, e.g. Ctrl+A, F10,
     * Ctrl+Shift+F3 etc. The shortcut is first searched in the user preferences and then in the structure bundle.
     * </ul>A keyword 'Image' is appended to the key and the string used to load the file name of the image icon.
     *
     * @param key a key identifying the menu item in the structure bundle.
     * @return a new menu item initialized with a label, action, shortcut and image according to the menu definition
     * in the structure bundle.
     */
    private JMenuItem createMenuItem(String key) {

        // Create a new menu item
        JMenuItem menuItem;
        if (key.endsWith(SUFFIX_TOGGLE_MENU_ITEM)) {
            key = key.substring(0, key.length() - 1);
            menuItem = new JCheckBoxMenuItem();
            menuItem.addItemListener(this);
        } else {
            menuItem = new JMenuItem();
        }

        // Set the action name
        String actionName = getString(structBundle, key + SUFFIX_ACTION);
        if (actionName == null) {
            actionName = key;
        }
        menuItem.setActionCommand(actionName);

        // Look if there's a predefined action passed by the addActions method
        Action action = (Action) actionMap.get(actionName);
        if (action != null) {
            menuItem.setAction(action);
            menuItem.setEnabled(action.isEnabled());
        } else {  // Disable the menu item if there's no action
            menuItem.setEnabled(false);
        }

        // Set the text and accelerator
        String text = getString(displayBundle, key + SUFFIX_TEXT);
        setTextAndMnemonic(menuItem, text);

        // Set the accelerator which may be in configuration (highest priority)
        // or in the structure bundle (lower priority)
        String shortcut = cfg.getString(key + SUFFIX_ACCELERATOR);
        if (shortcut == null) {
            shortcut = getString(structBundle, key + SUFFIX_ACCELERATOR);
        }
        if (shortcut != null && !"".equals(shortcut)) {
            KeyStroke k = Utils.getKeyStroke(shortcut);

            // Fix hot keys on Mac - map Ctrl to Meta and Alt to Ctrl
            if (Utils.isMac()) {
                int m = k.getModifiers();
                if ((m & InputEvent.CTRL_DOWN_MASK) > 0) {
                    m = (m & (~(InputEvent.CTRL_DOWN_MASK | InputEvent.CTRL_MASK))) | InputEvent.META_DOWN_MASK;
                }
                if ((k.getModifiers() & InputEvent.ALT_DOWN_MASK) > 0) {
                    m = (m & (~(InputEvent.ALT_DOWN_MASK | InputEvent.ALT_MASK))) | InputEvent.CTRL_DOWN_MASK;
                }
                k = KeyStroke.getKeyStroke(k.getKeyCode(), m);
            }
            menuItem.setAccelerator(k);
        }

        // Set the icon
        String iconName = getString(structBundle, key + SUFFIX_ICON);
        if (iconName != null) {
            menuItem.setHorizontalTextPosition(JButton.RIGHT);
            menuItem.setIcon(getIcon(iconName));
        }

        menuItemMap.put(actionName, menuItem);
        return menuItem;
    }

    public ImageIcon getIcon(String icon) {
        return ApplicationSupport.getImageIcon(icon);
    }

    /**
     * Create the tool bar (JToolBar instance) from the structure and label bundles which were passed to this
     * class instance in constructor. The method parses the root menu entry and
     * calls method {@link #createToolbarButton(java.lang.String, java.awt.event.ActionListener)} for every
     * button declaration.
     *
     * @param rootKey root rootKey in the structure bundle which defines the tool bar.
     * @param listener an ActionListener which will handle action calls.
     * @return a new JToolBar instance created according to the structure bundle.
     */
    public JToolBar createToolbar(String rootKey, ActionListener listener) {
        JToolBar toolBar = new JToolBar();
        toolBar.setRollover(true);
        toolBar.setFloatable(true);

        String[] keys = getString(structBundle, rootKey).split("\\s");
        for (String key : keys) {
            try {
                if (MENU_SEPARATOR.equals(key)) {
                    toolBar.addSeparator();
                } else {
                    toolBar.add(createToolbarButton(key, listener));
                }
            } catch (Exception ex) {
                System.out.println("Invalid key " + key);
                ex.printStackTrace();
            }
        }
        return toolBar;
    }

    /**
     * Create a toolbar button defined under the given key in the structure and label bundles. The following rules apply:
     * <ul>
     * <li>A JButton is created by default. If the key ends with an asterisk, e.g. 'toolbar.ShowFlag*',
     * a JToggleButton instance is created instead and the asterisk is removed from the key.</li>
     * <li>The method appends a suffix defined by the SUFFIX_TEXT constant to the
     * key and uses it to load the button text from the label bundle. Example of a key is 'toolbar.ShowFlagText'.</li>
     * <li>A suffix defined by the SUFFIX_ACTION constant is appended to the key and the string used to load the action name.</li>
     * <li>A suffix defined by the SUFFIX_ICON constant is appended to the key and the string used to load the file name of the image icon.</li>
     * </ul>
     *
     * @param key a key identifying the tool bar button in the structure bundle.
     * @param listener an ActionListener which will handle action calls.
     * @return a new toolbar button.
     */
    private AbstractButton createToolbarButton(String key, ActionListener listener) {
        boolean toggle = false;
        if (key.endsWith(SUFFIX_TOGGLE_MENU_ITEM)) {
            key = key.substring(0, key.length() - 1);
            toggle = true;
        }
        final String icon = getString(structBundle, key + SUFFIX_ICON);
        final String text = getString(displayBundle, key + SUFFIX_TEXT);

        // If no icon or text is available, do not create any button
        if (icon == null && text == null) {
            return null;
        }

        AbstractButton button;
        if (toggle) {
            button = new JToggleButton();
            button.addItemListener(this);
        } else {
            button = new JButton();
        }

        String actionName = getString(structBundle, key + SUFFIX_ACTION);
        if (actionName == null) {
            actionName = key;  // Default action name is the key
        }

        Action action = (Action) actionMap.get(actionName);
        if (action != null) {
            button.setActionCommand(actionName);
            button.setAction(action);
            button.setEnabled(action.isEnabled());
            toolbarBtnMap.put(key, button);
        } else {
            button.setActionCommand(actionName);
            button.setEnabled(false);
            button.addActionListener(listener);
            toolbarBtnMap.put(actionName, button);
        }

        // Set the buttons transparent and borderless
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setMargin(new Insets(1, 1, 1, 1));

        // Register the button by the JavaHelp
        String helpKey = createHelpKeyForAction(actionName);
        if (helpKey != null) {
            helpProvider.setHelpId(button, helpKey);
        }

        button.setToolTipText(getString(displayBundle, key + SUFFIX_TOOLTIP));

        if (icon != null) {
            button.setIcon(getIcon(icon));
        }

        return button;
    }

    /**
     * This method allows to bind dynamically generated menus. A typical example is the 'Reopen' menu which contains
     * the list of the last N opened files. This method should be called every time the menu contents changes.
     *
     * @param key key of the menu to which to bind the dynamic menu.
     * @param items vector of the labels of dynamic menu items. The item text (label) also serves as the action name.
     * The actions to be bound to the menu items must be present in the action table before you call this method. See
     * method addActions().
     */
    public void createDynamicMenu(String key, List items) {
        JMenuItem obj = getMenuItem(key);
        if (obj == null || !(obj instanceof JMenu)) {
            System.out.println("Can't create dynamic menu (key " + key + "): " + obj);
        }
        JMenu menu = (JMenu) obj;
        menu.removeAll();

        JMenuItem item;
        String text;
        Action act;
        for (int i = 0; i < items.size(); i++) {
            text = items.get(i).toString();
            item = new JMenuItem(text);
            if ((act = (Action) actionMap.get(text)) != null) {
                item.setAction(act);
            }
            menu.add(item);
        }
        menu.setEnabled(items.size() > 0);
    }

    /**
     * Implementation of the ItemListener interface. Its purpose is to synchronize the state changes of corresponding
     * items. Such an item may be either a JCheckBoxMenuItem or JToggleButton. This method is called when any of the
     * selectable items created by this class changes the state. If a corresponding button or menu item exists
     * for the same action name, this method makes sure that the state of the corresponding items changes too.
     *
     * @param e an ItemEvent.
     */
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() instanceof JToggleButton) {
            JToggleButton b = (JToggleButton) e.getSource();
            JCheckBoxMenuItem it = (JCheckBoxMenuItem) getMenuItem(b.getActionCommand());
            if (it != null && it.isSelected() != b.isSelected()) {
                it.removeItemListener(this);
                it.setSelected(b.isSelected());
                it.addItemListener(this);
            }
        } else if (e.getSource() instanceof JCheckBoxMenuItem) {
            JCheckBoxMenuItem it = (JCheckBoxMenuItem) e.getSource();
            JToggleButton b = (JToggleButton) getToolbarButton(it.getActionCommand());
            if (b != null && it.isSelected() != b.isSelected()) {
                b.removeItemListener(this);
                b.setSelected(it.isSelected());
                b.addItemListener(this);
            }
        }
    }
}
