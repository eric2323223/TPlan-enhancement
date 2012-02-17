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

/**
 * Snippet class. It is just a container for snippet key, description, code template 
 * and caret position. See the {@link SnippetWizard} class for information on how
 * snippets are populated.
 * 
 * @product.signature
 */
public class Snippet {
    String key;
    String description;
    String code;
    boolean replace;

    /**
     * Constructor.
     * @param key Snippet key which user can type to select the snippet automatically
     * @param description Snippet description to be showm in the menu
     * @param code Snippet code template. It may contain an ampersand character indicating where to set
     * position of the cursor after the code gets inserted into the editor.
     * @param replace A flag showing whether to replace the whole command line with the template
     * or whether just insert the snippet into the current cursor position.
     */
    public Snippet(String key, String description, String code, boolean replace) {
        this.key = key;
        this.description = description;
        this.code = code;
        this.replace = replace;
    }

    public Snippet() {
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    /**
     * Get the desired caret position after this code snippet is inserted into
     * an editor.
     * @param s a final code snippet to be inserted into editor.
     * @return position measured from the beginning of the argument string to insert
     * the caret to. If the snippet doesn't define a caret position, the method returns -1.
     */
    public int getCaretPosition(String s) {
        return s.indexOf('&');
    }

    public boolean isReplace() {
        return replace;
    }
}
