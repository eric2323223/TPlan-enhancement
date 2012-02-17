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
import com.tplan.robot.scripting.commands.CommandHandler;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.TokenParserImpl;
import com.tplan.robot.scripting.commands.AdvancedCommandHandler;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.util.DocumentUtils;

import javax.swing.*;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.Arrays;
import java.util.List;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Pop up menu with list of commands and command parameters supported by the {@doc.spec}
 * and a link to the {@link SnippetWizard snippet menu}.</p>
 *
 * <p>When the menu is displayed for an empty line in the active editor, it displays a list
 * of all supported commands and a link to the Snippet Menu. When an item gets selected,
 * the menu inserts the associated command template into the active editor. The menu
 * also supporpts filtering by a prefix and shows just those commands that start with the given string. For example
 * when user types 't', the list shows just those commands thats start with 't'. Note that the prefix is extracted automatically
 * from the curently edited element in the document of the active editor. There's no set- method for it.</p>
 *
 * <p>When the menu is displayed for an editor line containing a valid command name,
 * it displays a list of supported parameters of that command. The menu doesn't
 * display parameters which are already used in the command. If the menu contains just one
 * single parameter, it doesn't display at all and inserts the parameter immediately
 * into the editor.</p>
 *
 * @product.signature
 */
public class CommandWizard extends FilteredPopupMenuImpl implements ActionListener, FilteredPopupMenu {

    Object items[];
    Map menuItems;
    String event;
    Element element;
    JMenuItem miSnippetMenu;
    SnippetWizard snippetMenu;
    TokenParser parser = new TokenParserImpl();
    Component owner;
    Editor editor;
    String parameter;

    /**
     * Constructor.
     * @param scriptManager script manager instance.
     */
    public CommandWizard(Editor editor) {
        this.editor = editor;
        setForeground(Color.black);
        setBackground(Color.white);
        snippetMenu = new SnippetWizard();
        initItems();
    }

    /**
     * Overriden method to add the listener also to the snippet menu.
     * @param listener a property change listener.
     */
    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        snippetMenu.addPropertyChangeListener(listener);
    }

    /**
     * Load menu content.
     */
    private void initItems() {
        // Load all supported command handlers and sort them by name
        Map commandMap;
        if (editor.getEditorPnl().getTestScript() != null) {
            commandMap = editor.getEditorPnl().getTestScript().getScriptManager().getCommandHandlers();
        } else {
            commandMap = MainFrame.getInstance().getScriptHandler().getCommandHandlers();
        }
        items = (new ArrayList(commandMap.keySet())).toArray();
        Arrays.sort(items);

        menuItems = new HashMap();
        String item, attr;
        JMenuItem mi;
        CommandHandler ch;
        Map t;
        Object attrs[];

        for (int i = 0; i < items.length; i++) {
            item = (String) items[i];
            ch = (CommandHandler) commandMap.get(item);

            // Set the first letter upper case and other chars to lower case
            item = item.substring(0, 1).toUpperCase() + item.substring(1, item.length()).toLowerCase();
            items[i] = item;
            mi = new JMenuItem(item);
            mi.addActionListener(this);

            // If the handler defines a shortcut key, add it
            if (ch != null && ch.getContextShortcut() != null) {
                mi.setAccelerator(ch.getContextShortcut());
            }
            menuItems.put(items[i], mi);

            // Create menu items for attributes of the current command
            t = ch == null ? null : ch.getContextAttributes();
            if (t != null && !t.isEmpty()) {
                attrs = (new ArrayList(t.keySet())).toArray();
                Arrays.sort(attrs);
                menuItems.put(item.toLowerCase() + "_attribs", attrs);
                for (int j = 0; j < attrs.length; j++) {
                    attr = (String) attrs[j];
                    mi = new JMenuItem(attr);
                    mi.addActionListener(this);
                    menuItems.put(item.toLowerCase() + "_" + attr, mi);
                }
            }
        }

        // Add the Snippet Menu
        miSnippetMenu = new JMenuItem(ApplicationSupport.getString("commandList.insertTemplate"));
        miSnippetMenu.setForeground(Color.black);
        miSnippetMenu.setBackground(Color.white);
        miSnippetMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
        miSnippetMenu.addActionListener(this);
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
        this.owner = owner;
        if (owner instanceof Editor) {
            this.editor = (Editor) owner;
        }
        if (isVisible()) {
            setVisible(false);
        }

        this.element = e;

        // Filter the list of visible commands by the typed text
        removeAll();

        // Try to parse the command if it exists. If there's no command,
        // list of commands is to be displayed.
        // If there's already a valid command, display a list of parameters.
        String item;
        String typedText = DocumentUtils.getElementText(e);
        List v = new ArrayList(10);
        List<int[]> coords = new ArrayList();
        Map params = parser.parse(typedText, v, coords);
        String cmd = v.size() > 0 ? v.get(0).toString().toUpperCase() : null;
//        for (int i = 0; i < v.size(); i++) {
//            System.out.print("Element '" + v.get(i) + "', positions=");
//            for (int j : coords.get(i)) {
//                System.out.print(j + ": ");
//                int a[] = coords.get(i);
//                if (a.length == 2) {
//                    System.out.print("'" + typedText.substring(a[0], a[1]) + "'");
//                } else if (a.length == 4) {
//                    System.out.print("'" + typedText.substring(a[0], a[1]) + "' ");
//                    System.out.print("'" + typedText.substring(a[2], a[3]) + "' ");
//                }
//            }
//            System.out.println();
//        }

        TestScriptInterpret ti = editor.getEditorPnl().getTestScript();
        ScriptManager sm = ti.getScriptManager();
        int caret = editor.getCaretPosition();
        int editValue = editValue(coords, caret - e.getStartOffset());
        int editArgument = argumentValue(coords, caret - e.getStartOffset());
//        System.out.println("\nCaret position=" + caret + "\nEdit index=" + editValue);

        // Get the associated command handler
        CommandHandler ch = null;
        if (cmd == null || !sm.getCommandHandlers().containsKey(cmd)) {
            cmd = null;
        } else {
            ch = sm.getCommandHandlers().get(cmd);
        }

        typedText = typedText.trim();

        boolean isAdvanced = ch instanceof AdvancedCommandHandler && ti.getCompilationContext() != null;

        // This flag shows whether the command is an advanced one and expects an argument
        boolean requiresArgument = isAdvanced && ch.getContextArgument() != null;

        // This flag indicates whether to show the argument menu.
        boolean needsArgument = requiresArgument && isAdvanced && (v.size() <= params.size() || v.size() <= 1);
//                && ((AdvancedCommandHandler)ch).getArguments(cmd, ti.getCompilationContext()) != null;

        boolean doNotHandleSingleItemMenu = false;

        if (cmd == null) {
            event = "commandSelected";

            if (typedText.equals("")) {
                add(miSnippetMenu);
            }

            for (int i = 0; i < items.length; i++) {
                item = (String) items[i];
                if (typedText == null || (item.length() >= typedText.length() && item.substring(0, typedText.length()).equalsIgnoreCase(typedText))) {
                    add((JMenuItem) menuItems.get(item));
                }
            }
        } else if (needsArgument || (requiresArgument && editArgument >= 0)) {  // Show list of arguments (if available)
            event = "commandSelected";
            List l = ((AdvancedCommandHandler) ch).getArguments(typedText, ti.getCompilationContext());
            List ln = new ArrayList(v);
            for (Object o : l) {
                if (o instanceof String) {
                    String c = Character.toUpperCase(cmd.charAt(0)) + cmd.substring(1).toLowerCase() + " " + o;
                    if (ln.size() > 1) {
                        ln.set(1, o.toString());
                    } else {
                        ln.add(o.toString());
                    }
                    addItem(new ElementUpdateAction(c, composeCommand(ln, params), owner));
                } else {
                    removeAll();
                    break;
                }
            }
        } else if (isAdvanced && editValue >= 0) { // Show list of parameter values
            event = "commandSelected";
            String par = (String) v.get(editValue);
            List pl = pl = ((AdvancedCommandHandler) ch).getParameterValues(par, typedText, ti.getCompilationContext());
            if (pl != null) {
                Map m = new HashMap(params);
                for (Object o : pl) {
                    if (o instanceof String) {
                        String c = (String) o;
                        m.put(par, o);
                        addItem(new ElementUpdateAction(c, composeCommand(v, m), owner));
                    } else {
                        removeAll();
                        m.put(par, ch.getContextAttributes().get(par));
                        addItem(new ElementUpdateAction(typedText, composeCommand(v, m), owner));
                    }
                }
            } else {
                Map m = new HashMap(params);
                Object o = "<" + ch.getContextAttributes().get(par) + ">";
                if (o != null && !o.toString().equals(m.get(par))) {
                    m.put(par, o);
                    firePropertyChange(event, owner, composeCommand(v, m));
                }
            }

        } else { // Show list of parameters
            event = "paramSelected";

            Object[] en = params.keySet().toArray();
            String s;
            for (Object o : en) {
                s = (String) o;
                params.put(s.toUpperCase(), params.get(s));
            }

            List<String> l = null;
            if (ch != null && ch instanceof AdvancedCommandHandler) {
                l = ((AdvancedCommandHandler) ch).getParameters(typedText, ti.getCompilationContext());
            }

            Object array[] = (Object[]) menuItems.get(cmd.toLowerCase() + "_attribs");
            int pos = typedText.lastIndexOf(' ');
            typedText = pos >= 0 ? typedText.substring(pos + 1) : "";

            for (int i = 0; array != null && i < array.length; i++) {
                item = (String) array[i];
                if (!params.containsKey(item.toUpperCase()) && (l == null || l.contains(item))) {
                    add((JMenuItem) menuItems.get(cmd.toLowerCase() + "_" + item));
                }
            }
        }

        // If there's just one menu item, act as if it was selected
        if (getComponentCount() == 1) { // && !doNotHandleSingleItemMenu) {
            JMenuItem it = (JMenuItem) getComponent(0);
            it.doClick();
//            firePropertyChange(event, owner, it).getText());
            return;
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

    private void addItem(Action action) {
        JMenuItem mi = new JMenuItem(action);
        mi.setForeground(Color.black);
        mi.setBackground(Color.white);
        add(mi);
    }

    private void addItem(String text) {
        JMenuItem mi = new JMenuItem(text);
        mi.setForeground(Color.black);
        mi.setBackground(Color.white);
        add(mi);
    }

    private int editValue(List<int[]> l, int position) {
        int[] a;
        for (int i = 0; i < l.size(); i++) {
            a = l.get(i);
            if (a.length == 4 && position >= a[2] && position <= a[3]) {
                return i;
            }
        }
        return -1;
    }

    private int argumentValue(List<int[]> l, int position) {
        if (l.size() > 1) {
            int[] a = l.get(1);
            if (a.length == 2 && position >= a[0] && position <= a[1]) {
                return 0;
            }
        }
        return -1;
    }

    /**
     * Action listener handling events from the selected menu items.
     * @param e an action event identifying the selected menu item .
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(miSnippetMenu)) {
            Point p = miSnippetMenu.getLocation();
            setVisible(false);
            snippetMenu.display(getInvoker(), element, p);
        } else {
            firePropertyChange(event, getInvoker(), ((JMenuItem) e.getSource()).getText());
        }
        getInvoker().requestFocus();
        firePopupMenuCanceled();
    }

    /**
     * Get the {@link SnippetWizard Snippet Menu} instance.
     * @return snippet menu instance.
     */
    public SnippetWizard getSnippetMenu() {
        return snippetMenu;
    }

    /**
     * Overriden method to resolve lost editor focus when the menu is canceled.
     */
    @Override
    protected void firePopupMenuCanceled() {
        super.firePopupMenuCanceled();

        // This Runnable resolves lost editor focus when the menu is canceled.
        // We have to give some time to Swing to dispose the menu properly
        // and we get the owner to ask for the focus later on.
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                }
                owner.requestFocus();
            }
        });
    }

    private String composeCommand(List pars, Map parValue) {
        String s = "";
        String p;
        int len = pars.size();
        for (int i = 0; i < len; i++) {
            p = pars.get(i).toString();
            if (parValue.containsKey(p)) {
                s += p + "=\"" + parValue.get(p) + "\"";
            } else {
                if (p.contains(" ")) {
                    s += "\"" + p + "\"";
                } else {
                    s += p;
                }
            }
            if (i < len - 1) {
                s += " ";
            }
        }
        return s;
    }

    class ElementUpdateAction extends AbstractAction {

        String value;
        Component invoker;

        ElementUpdateAction(String displayText, String value, Component invoker) {
            this.invoker = invoker;
            putValue(NAME, displayText);
            this.value = value;
        }

        public void actionPerformed(ActionEvent e) {
            CommandWizard.this.firePropertyChange("commandSelected", invoker, value);
            firePopupMenuCanceled();
        }
    }

    class ElementAction extends AbstractAction {

        String value;
        Component invoker;

        ElementAction(String displayText, String value, Component invoker) {
            this.invoker = invoker;
            putValue(NAME, displayText);
            this.value = value;
        }

        public void actionPerformed(ActionEvent e) {
            CommandWizard.this.firePropertyChange("elementSelected", invoker, value + " ");
            firePopupMenuCanceled();
        }
    }
}
