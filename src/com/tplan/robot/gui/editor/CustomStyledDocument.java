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

import com.tplan.robot.preferences.ConfigurationChangeEvent;
import com.tplan.robot.preferences.ConfigurationChangeListener;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.scripting.commands.CommandHandler;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.TokenParserImpl;
import com.tplan.robot.util.DocumentUtils;

import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Styled document capable of highlighting syntax of the product scripting language.
 * @product.signature
 */
public class CustomStyledDocument extends DefaultStyledDocument implements ConfigurationChangeListener, Runnable {

    boolean convertFirstLetter = true;
    static String STYLENAME_COMMENT = "comment";
    static String STYLENAME_COMMAND = "command";
    static String STYLENAME_ARGUMENT = "argument";
    static String STYLENAME_PARAMETER = "parameter";
    static String STYLENAME_PARAMETER_VALUE = "parameter value";
    static String STYLENAME_KEYWORD = "procedure";
    ScriptManager scriptManager;
    UserConfiguration cfg;
    final private List<Element[]> unformatted = new ArrayList();
    public static final String[] STYLES = {
        STYLENAME_COMMENT,
        STYLENAME_COMMAND,
        STYLENAME_ARGUMENT,
        STYLENAME_PARAMETER,
        STYLENAME_PARAMETER_VALUE,
        STYLENAME_KEYWORD
    };
    static TokenParser parser = new TokenParserImpl();

    /**
     * Constructor.
     * @param scriptManager script manager.
     * @param cfg user configuration.
     */
    public CustomStyledDocument(ScriptManager scriptManager, UserConfiguration cfg) {
        this.scriptManager = scriptManager;
        this.cfg = cfg;
        addStylesToDocument();
        cfg.addConfigurationListener(this);
        updateAutoConvertFlag();
    }

    /**
     * Add styles to the document.
     */
    protected void addStylesToDocument() {

        // Remove all existing styles first
        Enumeration e = getStyleNames();
        while (e != null && e.hasMoreElements()) {
            removeStyle((String) e.nextElement());
        }

        // Initialize a set of default styles.
        // We will change individual style settings later on.
        Style def = StyleContext.getDefaultStyleContext().
                getStyle(StyleContext.DEFAULT_STYLE);

        addStyle(STYLENAME_COMMAND, def);
        addStyle(STYLENAME_ARGUMENT, def);
        addStyle(STYLENAME_PARAMETER, def);
        addStyle(STYLENAME_PARAMETER_VALUE, def);
        addStyle(STYLENAME_COMMENT, def);
        addStyle(STYLENAME_KEYWORD, def);

        String key = null;
        for (int i = 0; i < STYLES.length; i++) {
            for (int j = 0; j <= 6; j++) {
                key = "ui.editor.style." + i + "." + j;
                updateStyle(key, false);
            }
        }
        updateStyle(key, true);
    }

    /**
     * Removes some content from the document.
     * Removing content causes a write lock to be held while the
     * actual changes are taking place.  Observers are notified
     * of the change on the thread that called this method.
     * <p/>
     * This method is thread safe, although most Swing methods
     * are not. Please see
     * <A HREF="http://java.sun.com/products/jfc/swingdoc-archive/threads.html">Threads
     * and Swing</A> for more information.
     *
     * @param offs the starting offset >= 0
     * @param len  the number of characters to remove >= 0
     * @throws BadLocationException the given remove position is not a valid
     *                              position within the document
     * @see Document#remove
     */
    @Override
    public void remove(int offs, int len) throws BadLocationException {
        super.remove(offs, len);
        writeLock();
        try {
            resetStyles(offs, len);
        } finally {
            writeUnlock();
        }
    }

    /**
     * Inserts some content into the document.
     * Inserting content causes a write lock to be held while the
     * actual changes are taking place, followed by notification
     * to the observers on the thread that grabbed the write lock.
     * <p/>
     * This method is thread safe, although most Swing methods
     * are not. Please see
     * <A HREF="http://java.sun.com/products/jfc/swingdoc-archive/threads.html">Threads
     * and Swing</A> for more information.
     *
     * @param offs the starting offset >= 0
     * @param str  the string to insert; does nothing with null/empty strings
     * @param a    the attributes for the inserted content
     * @throws BadLocationException the given insert position is not a valid
     *                              position within the document
     * @see Document#insertString
     */
    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if ((str == null) || (str.length() == 0)) {
            return;
        }
        super.insertString(offs, str, a);
        try {
            resetStyles(offs, str.length());
        } finally {
        }
    }

    /**
     * Deletes the region of text from <code>offset</code> to
     * <code>offset + length</code>, and replaces it with <code>text</code>.
     * It is up to the implementation as to how this is implemented, some
     * implementations may treat this as two distinct operations: a remove
     * followed by an insert, others may treat the replace as one atomic
     * operation.
     *
     * @param offset index of child element
     * @param length length of text to delete, may be 0 indicating don't
     *               delete anything
     * @param text   text to insert, <code>null</code> indicates no text to insert
     * @param attrs  AttributeSet indicating attributes of inserted text,
     *               <code>null</code>
     *               is legal, and typically treated as an empty attributeset,
     *               but exact interpretation is left to the subclass
     * @throws BadLocationException the given position is not a valid
     *                              position within the document
     * @since 1.4
     */
    @Override
    public void replace(int offset, int length, String text,
            AttributeSet attrs) throws BadLocationException {
        super.replace(offset, length, text, attrs);
        try {
            resetStyles(offset, length);
        } finally {
        }
    }

    /**
     * Reset styles on an updated chunk of text. If it affects more than one line (i.e. one element),
     * the method will call the <code>resetElementStyle()</code> method for each modified element.
     *
     * @param offs text offset
     * @param len  text length
     */
    public synchronized void resetStyles(int offs, int len) {
        int end = offs + len;
        Element elem = getParagraphElement(offs);
        Element oldElem = null;

        while (offs < end && elem != null && oldElem != elem) {
            oldElem = elem;

            if (elem != null) {
                resetElementStyle(elem);
                offs = elem.getEndOffset() + 1;
            }
            elem = getParagraphElement(offs);
        }
    }

    private boolean isFormatted(Element e) {
        for (Element[] ee : unformatted) {
            if (e.getStartOffset() >= ee[0].getStartOffset() && e.getEndOffset() <= ee[1].getEndOffset()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluate content of an element and update all styles.
     * This method should be called after any document update.
     *
     * @param e a document element
     */
    private void resetElementStyle(Element e) {
        String cmd = DocumentUtils.getElementText(e);
//        CommandHandler ch = scriptManager.getCommandHandlerForElement(e);
        int offs = e.getStartOffset();

        // Reset all element styles
        Style def = StyleContext.getDefaultStyleContext().
                getStyle(StyleContext.DEFAULT_STYLE);
        setCharacterAttributes(offs, e.getEndOffset() - offs, def, true);

        // Fix in 2.0.5 to support unformatted nested code
        if (!isFormatted(e) || cmd.length() < 1) {
            return;
        }

        if (isComment(cmd)) {
            // Set style of the command name
            Style style = getStyle(STYLENAME_COMMENT);
            if (style != null) {
                setCharacterAttributes(offs, e.getEndOffset() - offs, style, true);
            }
            return;
        }

        // Find how many leading spaces there are and how long the command name is (there might be
        // more names of one command)
        int cmdLen = cmd.length();
        int leadingSpaces = 0;
        while (leadingSpaces < cmdLen && Character.isWhitespace(cmd.charAt(leadingSpaces))) {
            leadingSpaces++;
        }

        // Length of the command name (the very first word)
        int length = cmd.indexOf(' ', leadingSpaces + 1) - leadingSpaces;

        // This happens when the line contains just a command name
        if (length < 0) {
            length = cmd.length() - leadingSpaces;
        }

        // We have a command handler -> we can parse parameters etc.
        String cmdName = cmd.substring(leadingSpaces, leadingSpaces + length);
        CommandHandler ch = scriptManager.getCommandHandlers().get(cmdName.toUpperCase());
        if (ch != null) {

            boolean isProcedure = false;
            if (ch.getCommandNames()[0].equalsIgnoreCase("procedure")) {
                isProcedure = true;
            }

            // Set style of the command name
            Style style = isProcedure ? getStyle(STYLENAME_KEYWORD) : getStyle(STYLENAME_COMMAND);

            if (style != null) {
                setCharacterAttributes(offs + leadingSpaces, length, style, true);
            }

            // Feature: convert the first command letter to upper case automatically
            if (convertFirstLetter && Character.isLowerCase(cmd.charAt(leadingSpaces))) {
                try {
                    if (!cmd.toLowerCase().startsWith("procedure", leadingSpaces)) {
                        replace(offs + leadingSpaces, 1, "" + Character.toUpperCase(cmd.charAt(leadingSpaces)), style);
                    }
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }

            List v = new ArrayList();
            Map t = parser.parseParameters(cmd, v);

            if (v != null && v.size() > 0) {
                int vectorIndex = 0;
                int position = length + 1 + leadingSpaces;
                String s = null;

                if (ch.getContextArgument() != null) {
                    s = (String) v.get(0);
                    position = setTokenStyle(cmd, s, position, e, STYLENAME_ARGUMENT);
                    vectorIndex = 1;
                }

                for (int i = vectorIndex; i < v.size(); i++) {
                    s = (String) v.get(i);

                    position = setTokenStyle(cmd, s, position, e, STYLENAME_PARAMETER);

                    s = (String) t.get(s);
                    if (s != null && s.length() > 0) {
                        position = setTokenStyle(cmd, s, position, e, STYLENAME_PARAMETER_VALUE);
                    }
                }
            }
        } else {

            if (leadingSpaces < cmdLen && cmd.charAt(leadingSpaces) == '}') {
                leadingSpaces++;
            }

            String cmd1 = "", cmd2 = "";
            while (leadingSpaces < cmdLen && cmd.charAt(leadingSpaces) == ' ') {
                leadingSpaces++;
            }
            char c;
            int start1 = leadingSpaces;
            while (leadingSpaces < cmdLen) {
                c = cmd.charAt(leadingSpaces);
                if (!Character.isLetter(c)) {
                    break;
                }
                cmd1 += c;
                leadingSpaces++;
            }
            int end1 = leadingSpaces;

            while (leadingSpaces < cmdLen && cmd.charAt(leadingSpaces) == ' ') {
                leadingSpaces++;
            }
            while (leadingSpaces < cmdLen) {
                c = cmd.charAt(leadingSpaces);
                if (!Character.isLetter(c)) {
                    break;
                }
                cmd2 += c;
                leadingSpaces++;
            }
            int end2 = leadingSpaces;

            if (cmd1.equalsIgnoreCase("if") || cmd1.equalsIgnoreCase("else")) {
                if (cmd2.equalsIgnoreCase("if")) {
                    String token = cmd.substring(start1, end2);
                    setTokenStyle(cmd, token, start1, e, STYLENAME_KEYWORD);
                } else {
                    String token = cmd.substring(start1, end1);
                    setTokenStyle(cmd, token, start1, e, STYLENAME_KEYWORD);
                }
            } else if (cmd1.equalsIgnoreCase("for") || cmd1.equalsIgnoreCase("procedure") || cmd1.equalsIgnoreCase("break")) {
                String token = cmd.substring(start1, end1);
                setTokenStyle(cmd, token, start1, e, STYLENAME_KEYWORD);
            }
        }
    }

    /**
     * Set token style to a given named style.
     * <p/>
     * <p>The method parses the line of text (argument <code>cmd</code> from given position (argument
     * <code>position</code>) and looks for a token (argument <code>token</code>). Such a token is typically
     * a command argument, parameter name or parameter value defined by the scripting language.
     * <p/>
     * <p>When the token is found, it's style is updated using the named style (argument <code>styleName</code>.
     * If the token is enclosed in double quotes ("<token>"), their style is also updated. The named style must be
     * present in the document. If it is not there, no update takes place.
     *
     * @param cmd       text of a command (i.e. one line of text of a script)
     * @param token     a word or piece of text we want to style
     * @param position  from which index in the text to look for the token
     * @param e         element corresponding to the line of text
     * @param styleName name of a style. A predefined set of styles is added to the document when it's instantiated.
     *                  The styles are configurable in the Preferences window.
     * @return new search position for the next update. It actually indicates end of the updated element.
     */
    private int setTokenStyle(String cmd, String token, int position, Element e, String styleName) {
        int tokenStart = 0, tokenEnd = 0;
        int offset = e.getStartOffset();
        token = token.replaceAll("\"", "\\\\\"");
        tokenStart = cmd.indexOf(token, position);
        if ("".equals(token) && cmd.length() > position + 1 && cmd.charAt(position + 1) == '"') {
            tokenStart++;
        }

        if (tokenStart >= 0) {
            tokenEnd = tokenStart + token.length();

            // If one char BEFORE the token is a double quote ("), include it by decreasing the start position
            if (tokenStart > 0 && cmd.charAt(tokenStart - 1) == '"') {
                tokenStart--;
                tokenEnd++;
            }
            // If one char AFTER the token is a double quote ("), include it by increasing the end position
            if (tokenEnd > 0 && tokenEnd < cmd.length() - 1 && cmd.charAt(tokenEnd + 1) == '"') {
                tokenEnd++;
            }

            // Set token style
            Style style = getStyle(styleName);
            if (style != null && tokenEnd > tokenStart) {
                setCharacterAttributes(offset + tokenStart, tokenEnd - tokenStart, style, true);
            }
        }
        return tokenEnd;
    }

    /**
     * Find out if the text is a comment. Comments always start with '#' which can be preceded by any number of spaces.
     *
     * @param text a line of text from the srcipt
     * @return true if the text is a comment, false otherwise
     */
    private boolean isComment(String text) {
        if (text != null) {
            text = text.trim();
            if (text.length() > 0) {
                return text.charAt(0) == '#' || text.startsWith("//");
            }
        }
        return false;
    }

    public void configurationChanged(ConfigurationChangeEvent evt) {
        if (evt.getPropertyName().startsWith("ui.editor.style")) {
            updateStyle(evt.getPropertyName(), true);
        } else if (evt.getPropertyName().equals("ui.editor.autoConvertToUpperCase")) {
            updateAutoConvertFlag();
        }
    }

    private void updateAutoConvertFlag() {
        Boolean b = cfg.getBoolean("ui.editor.autoConvertToUpperCase");
        if (b != null) {
            convertFirstLetter = b.booleanValue();
        }
    }

    private void updateStyle(String parameter, boolean resetStyles) {
        try {
            String tokens[] = parameter.split("\\.");
            int styleIndex = Integer.parseInt(tokens[tokens.length - 2]);
            if (styleIndex < 0 || styleIndex >= STYLES.length) {
                return;
            }
            Style style = getStyle(STYLES[styleIndex]);

            int attrIndex = Integer.parseInt(tokens[tokens.length - 1]);
            Boolean b;
            String parBase = parameter.substring(0, parameter.length() - 1);

            switch (attrIndex) {
                case 0: // Bold flag
                    b = cfg.getBoolean(parameter);
                    b = b == null ? false : b;
                    StyleConstants.setBold(style, b.booleanValue());
                    break;
                case 1: // Italic flag
                    b = cfg.getBoolean(parameter);
                    b = b == null ? false : b;
                    StyleConstants.setItalic(style, b.booleanValue());
                    break;
                case 2: // Foreground flag & color
                case 3:
                    b = cfg.getBoolean(parBase + "2");
                    if (b != null && b.booleanValue()) {
                        Color c = cfg.getColor(parBase + "3");
                        if (c != null) {
                            StyleConstants.setForeground(style, c);
                        }
                    } else {
                        style.removeAttribute(StyleConstants.Foreground);
                    }
                    break;
                case 4: // Foreground flag & color
                case 5:
                    b = cfg.getBoolean(parBase + "4");
                    if (b != null && b.booleanValue()) {
                        Color c = cfg.getColor(parBase + "5");
                        if (c != null) {
                            StyleConstants.setBackground(style, c);
                        }
                    } else {
                        style.removeAttribute(StyleConstants.Background);
                    }
                    break;
            }

            // Now reset styles in the whole document
            // TODO: run a thread and wait for more updates (would be more efficient)
            if (resetStyles) {
                resetStyles(0, getLength());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Set the list of element ranges which should be left unformatted. The
     * list items must be arrays of length equal to 2 which contains
     * the start and end element of the range.
     * @param elementRanges ranges of unformatted elements.
     */
    public void addUnformattedElementRanges(List<Element[]> elementRanges) {
        if (elementRanges == null) {
            if (unformatted.size() == 0) {
                return;
            }
            unformatted.clear();
            new Thread(this).start();
            return;
        } else {
            if (elementRanges.size() == 0 && unformatted.size() == 0) {
                return;
            }
        }
        unformatted.clear();
        unformatted.addAll(elementRanges);
        new Thread(this).start();
    }

    public void run() {
        resetStyles(0, getLength());
    }
}
