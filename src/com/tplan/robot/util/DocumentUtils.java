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
package com.tplan.robot.util;

import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.gui.editor.Editor;

import com.tplan.robot.scripting.TokenParserImpl;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import javax.swing.text.StyledDocument;

/**
 * Static utility methods related to styled documents.
 * @product.signature
 */
public class DocumentUtils {

    private static TokenParser parser = new TokenParserImpl();

    public static boolean isComment(Element e) {
        return isComment(getElementText(e));
    }

    public static boolean isComment(String text) {
        return text.trim().startsWith("#");
    }

    public static String getElementText(Element e) {
        String text = "";
        try {
            if (e != null) {
                text = e.getDocument().getText(e.getStartOffset(), e.getEndOffset() - e.getStartOffset());
                text = text.replaceAll("\n", "");
            }
        } catch (BadLocationException ex) {
        }
        return text;
    }

    public static Element getElementPriorTo(Document doc, int offset) {
        Element e = null;
        int index = doc.getDefaultRootElement().getElementIndex(offset) - 1;
        if (index >= 0 && index < doc.getDefaultRootElement().getElementCount()) {
            e = doc.getDefaultRootElement().getElement(index);
        }
        return e;
    }

    public static Element getElementAfter(Element el) {
        int i = el.getDocument().getDefaultRootElement().getEndOffset() + 1;
        Element e = (i >= 0 && i < el.getDocument().getLength()) ? el.getDocument().getDefaultRootElement().getElement(i) : null;
        return e;
    }

    public static Element getCommandElementPriorTo(Document doc, int offset) {
        Element e = null;
        int index = doc.getDefaultRootElement().getElementIndex(offset) - 1;
        String s;
        for (int i = index; i >= 0 && index < doc.getDefaultRootElement().getElementCount(); i--) {
            e = doc.getDefaultRootElement().getElement(i);
            s = getElementText(e);
            if (s != null) {
                s = s.trim();
                if (s.length() > 0 && s.charAt(0) != '#') {
                    return e;
                }
            }
        }
        return null;
    }
    private static int indent = 0;

    public static void analyzeEditorDocument(Editor ed, boolean showFullStructure) {
        if (ed != null) {
            Document doc = ed.getDocument();
            System.out.println("Document length: " + doc.getLength() + "\nCaret position: " + ed.getCaretPosition() + "\n");
            analyzeElementTree(doc.getDefaultRootElement(), showFullStructure);
        }
    }

    private static void analyzeElementTree(Element e, boolean showFullStructure) {
        if (e.equals(e.getDocument().getDefaultRootElement())) {
            indent = 0;
        } else {
            indent++;
        }
        if (showFullStructure || e.getName().equals("content")) {
            String s = "";
            s = e.getName() + " [" + e.getStartOffset() + "," + e.getEndOffset() + "]";
            for (int i = s.length(); i < 20; i++) {
                s += " ";
            }
            for (int i = 0; i < indent; i++) {
                s = " " + s;
            }
            if (e.getName().equals("content")) {
                s += getElementText(e);
            }
            System.out.println(s);
        }
        for (int i = 0; i < e.getElementCount(); i++) {
            analyzeElementTree(e.getElement(i), showFullStructure);
        }
        indent--;
    }

    public static Map getTokens(Element e, List v) {
        String text = DocumentUtils.getElementText(e).trim();
        String cmd = parser.parseCommandName(text);
        Map t = parser.parseParameters(text, v);
        v.add(0, cmd);
        return t;
    }

    public static String getCommandForValueMap(List v, Map t) {
        String text = (String) v.get(0);
        String s;
        Object o;
        for (int i = 1; i < v.size(); i++) {
            s = (String) v.get(i);
            o = t.get(s);
            text += " " + (o == null ? "\"" + s + "\"" : s);
            if (o != null) {
                text += "=";
                if (o instanceof String) {
                    text += "\"" + Utils.escapeUnescapedDoubleQuotes(o.toString()) + "\"";
                } else if (o instanceof Rectangle) {
                    Rectangle r = (Rectangle) o;
                    text += parser.rectToString(r);
                }
            // TODO: other object types - Number, Point, etc.
            }
        }
        return text;
    }

    public static void updateCommand(Element e, Map valueMap) throws BadLocationException {
        List v = new ArrayList();
        Map t = getTokens(e, v);
        t.putAll(valueMap);
        v.addAll(valueMap.keySet());
        String text = getCommandForValueMap(v, t);
//        System.out.println("text: "+text);

        text += '\n';
        int length = e.getEndOffset() - e.getStartOffset() - 1;
        Document doc = e.getDocument();
        doc.remove(e.getStartOffset(), length);
        doc.insertString(e.getStartOffset(), text, null);
    }

    public static List findElements(StyledDocument doc, String pattern) {
        List v = new ArrayList();
        Element e = doc.getDefaultRootElement();
        findElement(e, pattern, v);
        return v;
    }

    private static void findElement(Element e, String pattern, List v) {
        if (e.getName().equals("paragraph")) {
            if (complies(e, pattern)) {
                v.add(e);
            }
        } else {
            for (int i = 0; i < e.getElementCount(); i++) {
                findElement(e.getElement(i), pattern, v);
            }
        }
    }

    private static boolean complies(Element e, String pattern) {
        String s = DocumentUtils.getElementText(e);
        return s.matches(pattern);
    }

    public static int getLineForOffset(StyledDocument doc, int offset) {
        return doc.getDefaultRootElement().getElementIndex(offset);
    }

    public static int getOffsetForLine(StyledDocument doc, int line) {
        Element e = getElementForLine(doc, line, doc.getDefaultRootElement());
        if (e != null) {
            return e.getStartOffset();
        }
        return -1;
    }

    public static Element getElementForLine(StyledDocument doc, int lineNumber, Element e) {
        Element ret = null;
        try {
            if (lineNumber >= 0 && e.getElementCount() >= lineNumber &&
                    e.getElement(lineNumber).getName().equals("paragraph")) {
                ret = e.getElement(lineNumber);
            } else {
                for (int i = 0; i < e.getElementCount(); i++) {
                    ret = getElementForLine(doc, lineNumber, e.getElement(i));
                    if (ret != null) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
        }
        return ret;
    }

    public static Element getElementForOffset(StyledDocument doc, int offset) {
        return doc.getDefaultRootElement().getElement(doc.getDefaultRootElement().getElementIndex(offset));
    }
}
