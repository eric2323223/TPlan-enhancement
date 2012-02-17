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
package com.tplan.robot.scripting;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.scripting.commands.impl.PressCommand;
import com.tplan.robot.util.Utils;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Token parser implementation.
 *
 * @product.signature
 */
public class TokenParserImpl extends NumericEvaluator implements TokenParser {

    public static Character PARAM_VALUE_SEPARATOR = new Character('=');
    ResourceBundle res = ApplicationSupport.getResourceBundle();

    public Map parse(String params) {
        return parse(params, null);
    }

    /**
     * This will read a string in quotes or just a single string.
     * Reading stops when the final quote is reached (quoted string) or
     * when a space or equal sign is reached (unquoted string).
     * <p/>
     * <p>Examples:
     * <li>for string 'test test' the method returns 'test'
     * <li>for string '"test test" test' the method returns '"test test"'
     * <li>for string 'test=blabla' the method return 'test'
     * <li>for string '"test \"test\""=5' the method returns '"test \"test\""'
     *
     * @return the first token or null if the end of string is reached
     */
    public String readToken(char ca[], int beg) {
        int len = ca.length;
        int end = beg;
        boolean inQuote = false;

        if (beg >= len) {
            return null;
        }
        while (end < len) {
            char c = ca[end];

            // Unescaped double quote sign found
            if (c == '\"' && (end == 0 || ca[end - 1] != '\\')) {

                if (inQuote) { // End of a quoted string
                    end++;
                    break;

                } else {  // Beginning of a quoted string
                    end++;
                    inQuote = true;
                }

            // Equal sign indicating end of a non-quoted string found
            } else if (c == '=' && !inQuote) {
                break;

            } else if (c == ' ' && !inQuote) {
                break;
            } else {
                end++;
            }
        }

        return new String(ca, beg, end - beg);
    }

    public Map<String, String> parse(String params, List v) {
        return parse(params, v, null);
    }

    public Map<String, String> parse(String params, List<String> v, List<int[]> l) {
        char ca[] = params.toCharArray();
        int len = ca.length;
        int end = 0, beg = 0;
        String key = null, value = null;

        while (end < len && Character.isWhitespace(ca[end])) {
            end++;
        }
        Map<String, String> t = new HashMap();

        do {
            beg = end;
            key = null;
            value = null;

            key = readToken(ca, beg);
            if (key == null) {
                break;
            }
            end += key.length();

            if (end < len) {
                // If the key is followed by a space or end of string, it is a single key without a value.
                if (ca[end] == ' ') {
                    if (l != null) {
                        l.add(new int[]{beg, end});
                    }
                    put(v, t, key, null);
                    do {
                        end++;
                    } while (end < len && (ca[end] == ' '));

                // Else if the key is followed by the equal sign, parse the value
                } else if (ca[end] == '=') {
                    int a[] = new int[]{beg, end, 0, 0};
                    end++;
                    value = readToken(ca, end);
                    if (value == null) {
                        value = "";
                    }
                    a[2] = end;
                    end += value.length();
                    a[3] = end;
                    if (l != null) {
                        l.add(a);
                    }
                    put(v, t, key, value);
                    do {
                        end++;
                    } while (end < len && (ca[end] == ' '));
                } else {
                    end++;
                }
            } else if (key != null) {
                if (l != null) {
                    l.add(new int[]{beg, end});
                }
                put(v, t, key, null);
            }
        } while (end < len);

        return t;
    }

    private void put(List v, Map t, String key, String value) {
        if (key != null) {
            key = fixQuotes(key);
            if (v != null) {
                v.add(key);
            }
            if (value != null) {
                value = fixQuotes(value);
                t.put(key, value == null ? "" : value);
            }
//            System.out.println("Put '" + key + "'='" + value + "'");
        }
    }

    /**
     * This method will:
     * <p>
     * <ul>
     * 1. Remove double quotes from the beginning and end of a string<br>
     * 2. Replace all occurencies of escaped double quote '\"' with double quote '"'
     * </ul>
     * @param text a text to be processed.
     * @return fixed text.
     */
    private String fixQuotes(String text) {
        if (text != null) {
            if (text.startsWith("\"") && text.startsWith("\"") && text.length() > 1) {
                text = text.substring(1, text.length() - 1);
            }
            // Now replace all occurencies of \" with " in the string
            text = text.replaceAll("\\\\\"", "\"");
        }
        return text;
    }

    public Number parseNumber(Object obj, String paramName) throws SyntaxErrorException {
        try {
            if (obj instanceof Number) {
                return (Number) obj;
            } else {
                return (Number) evaluateNumericExpression(obj.toString(), Double.class);
            }
        } catch (Exception ex) {
            String s = res.getString("parser.syntaxErrorGeneric");
            throw new SyntaxErrorException(MessageFormat.format(s, paramName, ex.getMessage()));
        }
    }

    public Number parseInteger(Object obj, String paramName) throws SyntaxErrorException {
        Number n = parseNumber(obj, paramName);
        if (n.intValue() != n.floatValue()) {
            String s = res.getString("parser.intExpectedFloatFound");
            throw new SyntaxErrorException(MessageFormat.format(s, paramName));
        }
        return n;
    }

    public Boolean parseBoolean(Object obj, String paramName) throws SyntaxErrorException {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        String p = obj.toString().toLowerCase();
        if ("true".equals(p) || "false".equals(p)) {
            return new Boolean(p);
        } else {
            String s = res.getString("parser.booleanExpected");
            throw new SyntaxErrorException(MessageFormat.format(s, paramName));
        }
    }

    public Number parsePercentage(Object obj, String paramName) throws SyntaxErrorException {
        Number n;
        if (obj instanceof Number) {
            n = (Number) obj;
        } else {
            String s = obj.toString();
            if (s.endsWith("%")) {
                s = s.substring(0, s.indexOf("%"));
            }
            n = parseNumber(s, paramName);
        }
        if (n.floatValue() > 100) {
            String s = res.getString("parser.percentageTooHigh");
            throw new SyntaxErrorException(MessageFormat.format(s, paramName));
        } else if (n.floatValue() < 0) {
            String s = res.getString("parser.percentageTooLow");
            throw new SyntaxErrorException(MessageFormat.format(s, paramName));
        }
        return n;
    }

    /**
     * Parse time from a string. The method always returns the time value in miliseconds.
     * <p/>
     * Supported formats are:
     * - '5' = 5 ms
     * - '5s' = 5 seconds = 5000 ms
     * - '5m' = 5 minutes = 5*60*1000 ms
     * - '5h' = 5 hours = 5*60*60*1000 ms
     * - '5d' = 5 days = 5*24*60*60*1000 ms
     * <p/>
     * The formats are case sensitive.
     *
     * @param obj a String or a Number containing time
     * @param paramName parameter name (to be mentioned in the eventually thrown exception)
     * @return time in miliseconds
     * @throws NumberFormatException when the string doesn't comply with the format.
     */
    public Number parseTime(Object obj, String paramName) throws SyntaxErrorException {
        if (obj instanceof Number) {
            return (Number) obj;
        }
        String s = obj.toString();
        int coef = 1;
        if (s != null && s.length() > 0) {
            char c = s.charAt(s.length() - 1);
            if (!Character.isDigit(c)) {
                switch (c) {
                    case 's':
                        coef = 1000;
                        break;
                    case 'm':
                        coef = 60000;
                        break;
                    case 'h':
                        coef = 3600000;
                        break;
                    case 'd':
                        coef = 86400000;
                        break;
                    default:
                        String msg = res.getString("parser.unknownTimeFormat");
                        throw new SyntaxErrorException(MessageFormat.format(msg, paramName, s));
                }
            }
        }
        s = coef > 1 ? s.substring(0, s.length() - 1) : s;
        Number n = parseNumber(s, paramName);
        return new Double(n.floatValue() * coef);
    }

    public RelativePoint parsePoint(Object obj, String paramName) throws SyntaxErrorException {
        if (obj instanceof Point) {
            return new RelativePoint((Point) obj);
        }
        Map<String, String> map = parseRectangleParams(obj.toString(), paramName);
        try {
            RelativePoint r = new RelativePoint();
            String value;
            Number n;
            if (map.containsKey("x")) {
                value = map.get("x");
                boolean percentage = false;
                if (value.endsWith("%")) {
                    value = value.substring(0, value.indexOf("%"));
                    percentage = true;
                }
                n = parseNumber(value, paramName);
                if (percentage) {
                    r.setXPercentage(n.floatValue());
                } else {
                    r.x = n.intValue();
                }
            }
            if (map.containsKey("y")) {
                value = map.get("y");
                boolean percentage = false;
                if (value.endsWith("%")) {
                    value = value.substring(0, value.indexOf("%"));
                    percentage = true;
                }
                n = parseNumber(value, paramName);
                if (percentage) {
                    r.setYPercentage(n.floatValue());
                } else {
                    r.y = n.intValue();
                }
            }
            return r;
        } catch (SyntaxErrorException ex) {
            String s = res.getString("parser.numericValueExpected");
            throw new SyntaxErrorException(MessageFormat.format(s, paramName));
        }
    }

    public Map<String, String> parsePointParams(String s, String paramName) throws SyntaxErrorException {
        String coords[] = s.split("[,;]");
        String c;
        Map<String, String> map = new HashMap();
        for (int i = 0; i < coords.length; i++) {
            s = coords[i];
            try {
                c = s.substring(s.indexOf(":") + 1);
                if (s.startsWith("x:") || s.startsWith("X:")) {
                    if (map.containsKey("x")) {
                        String msg = res.getString("parser.duplicateCoordinate");
                        throw new SyntaxErrorException(MessageFormat.format(msg, paramName, "x"));
                    }
                    map.put("x", c);
                } else if (s.startsWith("y:") || s.startsWith("Y:")) {
                    if (map.containsKey("y")) {
                        String msg = res.getString("parser.duplicateCoordinate");
                        throw new SyntaxErrorException(MessageFormat.format(msg, paramName, "y"));
                    }
                    map.put("y", c);
                } else {
                    String msg = res.getString("parser.pointFormatError");
                    throw new SyntaxErrorException(MessageFormat.format(msg, paramName));
                }
            } catch (SyntaxErrorException ex) {
                throw ex;
            } catch (Exception ex) {
                String msg = res.getString("parser.pointFormatError");
                throw new SyntaxErrorException(MessageFormat.format(msg, paramName));
            }
        }
        return map;
    }

    public Map<String, String> parseRectangleParams(String s, String paramName) throws SyntaxErrorException {
        String coords[] = s.split("[,;]");
        String c;
        Map<String, String> map = new HashMap();
        for (int i = 0; i < coords.length; i++) {
            s = coords[i];
            try {
                c = s.substring(s.indexOf(":") + 1);
                if (s.startsWith("x:") || s.startsWith("X:")) {
                    if (map.containsKey("x")) {
                        String msg = res.getString("parser.duplicateCoordinate");
                        throw new SyntaxErrorException(MessageFormat.format(msg, paramName, "x"));
                    }
                    map.put("x", c);
                } else if (s.startsWith("y:") || s.startsWith("Y:")) {
                    if (map.containsKey("y")) {
                        String msg = res.getString("parser.duplicateCoordinate");
                        throw new SyntaxErrorException(MessageFormat.format(msg, paramName, "y"));
                    }
                    map.put("y", c);
                } else if (s.toLowerCase().startsWith("w:") || s.toLowerCase().startsWith("width:")) {
                    if (map.containsKey("w")) {
                        String msg = res.getString("parser.duplicateCoordinate");
                        throw new SyntaxErrorException(MessageFormat.format(msg, paramName, "w"));
                    }
                    map.put("w", c);
                } else if (s.toLowerCase().startsWith("h:") || s.toLowerCase().startsWith("height:")) {
                    if (map.containsKey("h")) {
                        String msg = res.getString("parser.duplicateCoordinate");
                        throw new SyntaxErrorException(MessageFormat.format(msg, paramName, "h"));
                    }
                    map.put("h", c);
                } else {
                    String msg = res.getString("parser.rectangleFormatError");
                    throw new SyntaxErrorException(MessageFormat.format(msg, paramName));
                }
            } catch (SyntaxErrorException ex) {
                throw ex;
            } catch (Exception ex) {
                String msg = res.getString("parser.rectangleFormatError");
                throw new SyntaxErrorException(MessageFormat.format(msg, paramName));
            }
        }
        return map;
    }

    public Rectangle parseRectangle(Object obj, String paramName) throws SyntaxErrorException {
        return parseRectangle(obj, null, paramName);
    }

    public Rectangle parseRectangle(Object obj, Rectangle defaults, String paramName) throws SyntaxErrorException {
        if (obj instanceof Rectangle) {
            return (Rectangle) obj;
        }
        Map<String, String> map = parseRectangleParams(obj.toString(), paramName);
        try {
            Rectangle r = defaults != null ? new Rectangle(defaults) : new Rectangle();
            if (map.containsKey("x")) {
                r.x = parseNumber(map.get("x"), paramName).intValue();
            }
            if (map.containsKey("y")) {
                r.y = parseNumber(map.get("y"), paramName).intValue();
            }
            if (map.containsKey("w")) {
                r.width = parseNumber(map.get("w"), paramName).intValue();
            }
            if (map.containsKey("h")) {
                r.height = parseNumber(map.get("h"), paramName).intValue();
            }
            return r;
        } catch (SyntaxErrorException ex) {
            String msg = res.getString("parser.numericValueExpected");
            throw new SyntaxErrorException(MessageFormat.format(msg, paramName));
        }

    }

    public String rectToString(Rectangle r) {
        return "x:" + r.x + ",y:" + r.y + ",w:" + r.width + ",h:" + r.height;
    }

    public String pointToString(Point p) {
        return "x:" + p.x + ",y:" + p.y;
    }

    public int parseMouseButton(String button) throws SyntaxErrorException {
        if ("left".equalsIgnoreCase(button)) {
            return MouseEvent.BUTTON1;
        } else if ("middle".equalsIgnoreCase(button)) {
            return MouseEvent.BUTTON2;
        } else if ("right".equalsIgnoreCase(button)) {
            return MouseEvent.BUTTON3;
        }
        String s = res.getString("parser.unknownMouseButton");
        throw new SyntaxErrorException(MessageFormat.format(s, button));
    }

    public int parseModifiers(String modifiers) throws SyntaxErrorException {
        String[] keys = modifiers.toString().split("\\+");
        String token;
        int mask = 0;
        for (int j = 0; j < keys.length; j++) {
            token = keys[j];
            if (token.equalsIgnoreCase("shift")) {
                mask = mask | InputEvent.SHIFT_MASK;
            } else if (token.equalsIgnoreCase("alt")) {
                mask = mask | InputEvent.ALT_MASK;
            } else if (token.equalsIgnoreCase("ctrl")) {
                mask = mask | InputEvent.CTRL_MASK;
            } else if (!token.equals("")) {
                String s = res.getString("parser.unknownModifier");
                throw new SyntaxErrorException(MessageFormat.format(s, token));
            }
        }
        return mask;
    }

    public int parseKeyLocation(String location) throws SyntaxErrorException {
        int loc = KeyEvent.KEY_LOCATION_LEFT;
        if (location != null) {
            if (PressCommand.PARAM_LOCATION_LEFT.equalsIgnoreCase(location)) {
                loc = KeyEvent.KEY_LOCATION_LEFT;
            } else if (PressCommand.PARAM_LOCATION_RIGHT.equalsIgnoreCase(location)) {
                loc = KeyEvent.KEY_LOCATION_RIGHT;
            } else if (PressCommand.PARAM_LOCATION_STANDARD.equalsIgnoreCase(location)) {
                loc = KeyEvent.KEY_LOCATION_STANDARD;
            } else if (PressCommand.PARAM_LOCATION_NUMPAD.equalsIgnoreCase(location)) {
                loc = KeyEvent.KEY_LOCATION_NUMPAD;
            } else {
                String s = res.getString("command.syntaxErr.oneOf");
                throw new SyntaxErrorException(MessageFormat.format(s, "location", PressCommand.PARAM_LOCATION_STANDARD + ", " +
                        PressCommand.PARAM_LOCATION_NUMPAD + ", " + PressCommand.PARAM_LOCATION_LEFT + ", " + PressCommand.PARAM_LOCATION_RIGHT));
            }
        }
        return loc;
    }

    public String fileListToString(File files[]) {
        return fileListToString(files, null, false);
    }

    public String fileListToString(File files[], File defaultPath) {
        return fileListToString(files, defaultPath, false);
    }

    public String fileListToString(File files[], File defaultPath, boolean forceRelative) {
        String s = "";
        if (files != null) {
            File f;
            for (int i = 0; i < files.length; i++) {
                f = files[i];
                if (defaultPath != null && f.getParentFile().equals(defaultPath)) {
                    s += f.getName();
                } else {
                    try {
                        if (forceRelative) {
                            String rel = Utils.getRelativePath(f, defaultPath);
                            s += rel != null ? rel : f.getCanonicalPath();
                        } else {
                            s += f.getCanonicalPath();
                        }
                    } catch (Exception ex) {
                        s += f.getAbsolutePath();
                    }
                }
                if (i < files.length - 1) {
                    s += FILE_PATH_SEPARATOR;
                }
            }
        }
        return s;
    }

    public String parseCommandName(String command) {
        // Split by spaces and return the first token
        String tokens[] = command.trim().split("\\s");
        return tokens[0];
    }

    public String parseParameterString(String command) {
        String commId = parseCommandName(command);
        int position = command.indexOf(commId) + commId.length();
        return command.substring(position).trim();
    }

    public Map<String, String> parseParameters(String command) {
        return parse(parseParameterString(command));
    }

    public Map<String, String> parseParameters(String command, List v) {
        return parse(parseParameterString(command), v);
    }

    public String modifiersToString(int modifiers) {
        String s = "";
        if ((modifiers & InputEvent.CTRL_MASK) > 0) {
            s += "Ctrl+";
        }
        if ((modifiers & InputEvent.ALT_MASK) > 0) {
            s += "Alt+";
        }
        if ((modifiers & InputEvent.SHIFT_MASK) > 0) {
            s += "Shift+";
        }
        if (s.length() > 0) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public String buttonToString(int button) {
        switch (button) {
            case MouseEvent.BUTTON1:
                return "left";
            case MouseEvent.BUTTON2:
                return "middle";
            case MouseEvent.BUTTON3:
                return "right";
        }
        return "left"; // Default button
    }

    public File[] parseFileList(String fileList) {
        return parseFileList(fileList, null);
    }

    public File[] parseFileList(String fileList, File defaultPath) {
        File f[] = null;
        if (fileList != null) {
            String t[] = fileList.split(FILE_PATH_SEPARATOR);
            f = new File[t.length];
            int i = 0;
            File file;
            for (String s : t) {
                file = new File(s);
                if (defaultPath != null && !file.isAbsolute()) {
                    file = new File(defaultPath, file.getName());
                }
                f[i++] = file;
            }
        }
        return f;
    }

    /**
     * Parse an HTML-style RGB color. It may be also in a form of three
     * semicolon separated numbers representing the R, G and B components.
     *
     * @param s HTM-style color argument. Each of the R, G, B components must be
     * specified as 2-character hexadecimal number in this specific order. The
     * string may be optionally prefixed with the hash character '#'. Examples
     * of valid colors are "ffffff" or "#ffffff" (white), "ff0000" or "#ff0000"
     * (red), "000000" or "#000000" (black) etc.<p>The argument may be also in a form of three
     * semicolon separated numbers representing the R, G and B components, for example
     * "255;0;0" is equivalent to "ff0000" and represents red color.</p>
     * <p>If the argument doesn't meet
     * the required format, the method throws an IllegalArgumentException.</p>
     * @return parsed Color instance.
     */
    public Color parseColor(String s) {
        int r = 0, g = 0, b = 0;
        if (s != null && s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s == null || (!s.contains(";") && s.length() != 6)) {
            throw new IllegalArgumentException("Color must be 6 characters long hexa number representing RGB (HTML color style).");
        }
        String frag = "", err = null;
        try {
            frag = s.substring(0, 2);
            r = Integer.parseInt(frag, 16);
        } catch (Exception ex) {
            err = "'" + frag + "' is not valid red color value.";
        }
        if (err == null) {
            try {
                frag = s.substring(2, 4);
                g = Integer.parseInt(frag, 16);
            } catch (Exception ex) {
                err = "'" + frag + "' is not valid green color value.";
            }
        }
        if (err == null) {
            try {
                frag = s.substring(4, 6);
                b = Integer.parseInt(frag, 16);
            } catch (Exception ex) {
                err = "'" + frag + "' is not valid blue color value.";
            }
        }

        // If there's an error, try the other format
        if (err != null) {
            try {
                String sa[] = s.split(";");
                if (sa.length == 3) {
                    r = Integer.parseInt(sa[0]);
                    g = Integer.parseInt(sa[1]);
                    b = Integer.parseInt(sa[2]);
                    err = null;
                }
            } catch (Exception e) {
            }
        }
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
        return new Color(r, g, b);
    }

    /**
     * Convert color to HTML-style RGB string. The returned value will be 6
     * characters long, with R, G, B color components encoded in this order as
     * lower case hexadecimal values (2 characters each).
     * @param c a color.
     * @return HTML-style RGB string or empty string when the arhument color is null.
     */
    public String colorToString(Color c) {
        String s = "", component;
        component = Integer.toHexString(c.getRed());
        if (component.length() < 2) {
            component = "0" + component;
        }
        s += component;
        component = Integer.toHexString(c.getGreen());
        if (component.length() < 2) {
            component = "0" + component;
        }
        s += component;
        component = Integer.toHexString(c.getBlue());
        if (component.length() < 2) {
            component = "0" + component;
        }
        s += component;
        return s;
    }

}
