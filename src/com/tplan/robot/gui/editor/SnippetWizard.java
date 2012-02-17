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
import com.tplan.robot.l10n.CustomPropertyResourceBundle;
import com.tplan.robot.util.DocumentUtils;

import javax.swing.*;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * <p>Pop up menu with a list of code snippets templates which can be
 * inserted into the active editor.</p>
 *
 * <p>Code templates are defined in the resource bundle
 * under keys with the prefix defined by the {@link #RESOURCE_BUNDLE_KEY_PREFIX} constant.
 * Each snippet is defined by a set of four keys which start with <code>[prefix].[snippet_number].</code>.
 * An example of a snippet follows:</p>
 *
 * <blockquote>
 * <pre>
 * # Snippet key which user can type to select the snippet automatically
 * com.tplan.robot.snippet.5.key=vsy
 *
 * # Snippet description to be showm in the menu
 * com.tplan.robot.snippet.5.desc={_SEARCH_Y} variable from 'search' image comparison
 *
 * # Snippet code template. The ampersand character indicates where to set
 * # position of the cursor after the code gets inserted into the editor.
 * com.tplan.robot.snippet.5.code={_SEARCH_Y}&
 *
 * # A flag showing whether to replace the whole command line with the template
 * # or whether just insert the snippet into the current cursor position.
 * com.tplan.robot.snippet.5.replace=false
 * </pre>
 * </blockquote>
 *
 * <p>Note that this class doesn't insert the code snippet into the document on its own.
 * When a valid snippet is selected, it fires a property change event with well known key
 * to the editor which inserts the associated code into the document.</p>
 * @product.signature
 */
public class SnippetWizard extends FilteredPopupMenuImpl implements ActionListener, FilteredPopupMenu {

    Map items = new HashMap();

    Vector menuItems = new Vector();

    Element element = null;

    /**
     * Prefix of snippet keys in the resource bundle.
     */
    public static final String RESOURCE_BUNDLE_KEY_PREFIX = "com.tplan.robot.snippet.";

    public SnippetWizard() {
        setForeground(Color.black);
        setBackground(Color.white);
        load();
        rebuildMenu();
        addAllMenuItems();
        setFocusable(true);
    }

    /**
     * Display the menu. The owner is typically a script editor but it can be
     * in general any component. Content of the menu will be filtered depending on the
     * text contained in the argument document element. The element will be also
     * used to insert the generated code into the document.
     * @param owner menu owner (invoking component).
     * @param e a document element representing the active editor line of text.
     * @param p a point to display the menu at.
     */
    public void display(Component owner, Element e, Point p) {
        if (isVisible()) {
            setVisible(false);
        }

        this.element = e;

        // Filter the list of visible commands by the textTyped text
        removeAll();

        // Try to parse the command if it exists. If there's no command, list of commands is to be displayed.
        // If there's already a valid command, display a list of parameters.
        String item;
        String typedText = DocumentUtils.getElementText(e).trim();
        String tokens[] = typedText.split("\\s");
        String cmd = null;

        if (tokens.length > 0) {
            cmd = tokens[0];
        }

        if (cmd != null && !cmd.equals("")) {
            typedText = typedText.trim();
            for (int i = 0; i < menuItems.size(); i++) {
                item = ((JMenuItem) menuItems.get(i)).getActionCommand();
                if (item.length() >= typedText.length() && item.substring(0, typedText.length()).equalsIgnoreCase(typedText)) {
                    add((JMenuItem) menuItems.get(i));
                }
            }

            if (getComponentCount() == 0) {
                Snippet s;
                JMenuItem mi;
                for (int i = 0; i < menuItems.size(); i++) {
                    mi = (JMenuItem) menuItems.get(i);
                    s = (Snippet) items.get(mi.getActionCommand());
                    if (s != null && !s.isReplace()) {
                        add((JMenuItem) menuItems.get(i));
                    }
                }
            }
        } else {
            addAllMenuItems();
        }

        // If there's just one menu item, act as if it was selected
        if (getComponentCount() == 1) {
            Snippet s = (Snippet) items.get(((JMenuItem)getComponent(0)).getActionCommand());
            if (s != null) {
                firePropertyChange("snippetSelected", getInvoker(), s);
                return;
            }
        }

        // Display the menu otherwise; calculate it's coordinates so that it displays within the frame
        if (getComponentCount() > 0) {

            // Calculate whether it is better to display the menu downwards or upwards
            int menuHeight = (int) this.getPreferredSize().getHeight();
            p = p.y + menuHeight > owner.getBounds().getY() + owner.getBounds().height
                    ? new Point(p.x, p.y - menuHeight - 10)
                    : p;
            show(owner, (int) p.getX(), (int) p.getY());
        }
    }

    private void load() {
        items.clear();

        int index = 1;
        String key, desc, code, replace;
        Snippet s;
        CustomPropertyResourceBundle res = ApplicationSupport.getResourceBundle();

        try {
            String resKey = RESOURCE_BUNDLE_KEY_PREFIX + index + ".key";
            while (res.contains(resKey)) {
                key = res.getString(resKey);
                desc = res.getString(RESOURCE_BUNDLE_KEY_PREFIX + index + ".desc");
                code = res.getString(RESOURCE_BUNDLE_KEY_PREFIX + index + ".code");
                replace = res.getString(RESOURCE_BUNDLE_KEY_PREFIX + index + ".replace");
                s = new Snippet(key, desc, code, replace == null ? false : replace.equals("true"));
                items.put(key, s);
                index++;
                resKey = RESOURCE_BUNDLE_KEY_PREFIX + index + ".key";
            }
        } catch (MissingResourceException e) {
            // This is expected
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void rebuildMenu() {
        Object objects[] = (new Vector(items.keySet())).toArray();
        Arrays.sort(objects);

        removeAll();
        String key;
        Snippet s;
        JMenuItem mi;

        for (int i = 0; i < objects.length; i++) {
            key = (String) objects[i];
            s = (Snippet) items.get(key);
            mi = new JMenuItem(key + " - " + s.description);
            mi.addActionListener(this);
            mi.setActionCommand(key);
            menuItems.add(mi);
        }
    }

    private void addAllMenuItems() {
        for (int i = 0; i < menuItems.size(); i++) {
            add((JMenuItem) menuItems.elementAt(i));
        }
    }

    /**
     * Action listener handling events from the selected menu items.
     * @param e an action event identifying the selected menu item .
     */
    public void actionPerformed(ActionEvent e) {
        Snippet s = (Snippet) items.get(e.getActionCommand());
        if (s != null) {
            firePropertyChange("snippetSelected", getInvoker(), s);
        }
    }
}
