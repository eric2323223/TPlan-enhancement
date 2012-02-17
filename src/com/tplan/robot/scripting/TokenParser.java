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

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Token parser is responsible for breaking down individual test commands into
 * elements representing the command name, arguments and parameters.
 *
 * @product.signature
 */
public interface TokenParser {

    public static final String FILE_PATH_SEPARATOR = ";";

    Map<String, String> parse(String params);

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
    String readToken(char ca[], int beg);

    Map<String, String> parse(String params, List<String> v);

    Map<String, String> parse(String params, List<String> v, List<int[]> l);

    Number parseNumber(Object obj, String paramName) throws SyntaxErrorException;

    Number parseInteger(Object obj, String paramName) throws SyntaxErrorException;

    Boolean parseBoolean(Object obj, String paramName) throws SyntaxErrorException;

    Number parsePercentage(Object obj, String paramName) throws SyntaxErrorException;

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
     * @param obj an object containing time (it may be a String or Number)
     * @param paramName parameter name (to be mentioned in the eventually thrown exception)
     * @return time in miliseconds
     * @throws NumberFormatException when the string doesn't comply with the format.
     */
    Number parseTime(Object obj, String paramName) throws SyntaxErrorException;

    Map<String, String> parsePointParams(String s, String paramName) throws SyntaxErrorException;

    RelativePoint parsePoint(Object obj, String paramName) throws SyntaxErrorException;

    Map<String, String> parseRectangleParams(String s, String paramName) throws SyntaxErrorException;

    Rectangle parseRectangle(Object obj, Rectangle defaults, String paramName) throws SyntaxErrorException;

    Rectangle parseRectangle(Object obj, String paramName) throws SyntaxErrorException;

    int parseMouseButton(String button) throws SyntaxErrorException;

    int parseModifiers(String modifiers) throws SyntaxErrorException;

    String rectToString(Rectangle r);

    String pointToString(Point p);

    String parseCommandName(String command);

    String parseParameterString(String command);

    Map<String, String> parseParameters(String command);

    Map<String, String> parseParameters(String command, List v);

    Object evaluateNumericExpression(String expression, Class resultType, ScriptingContext context);

    Object evaluateNumericExpression(String expression, Class resultType);

    String modifiersToString(int modifiers);

    int parseKeyLocation(String location) throws SyntaxErrorException;

    String buttonToString(int button);

    String fileListToString(File files[], File defaultPath, boolean forceRelative);

    String fileListToString(File files[], File defaultPath);

    String fileListToString(File files[]);

    File[] parseFileList(String fileList);

    File[] parseFileList(String fileList, File defaultPath);

    /**
     * Convert color to HTML-style RGB string. The returned value will be 6
     * characters long, with R, G, B color components encoded in this order as
     * lower case hexadecimal values (2 characters each).
     * @param c a color.
     * @return HTML-style RGB string or empty string when the arhument color is null.
     */
    String colorToString(Color c);

    /**
     * Parse an HTML-style RGB color.
     *
     * @param s HTM-style color argument. Each of the R, G, B components must be
     * specified as 2-character hexadecimal number in this specific order. The
     * string may be optionally prefixed with the hash character '#'. Examples
     * of valid colors are "ffffff" or "#ffffff" (white), "ff0000" or "#ff0000"
     * (red), "000000" or "#000000" (black) etc. If the argument doesn't meet
     * the required format, the method throws an IllegalArgumentException.
     * @return parsed Color instance.
     */
    Color parseColor(String s);
}
