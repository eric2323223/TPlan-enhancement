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

import com.tplan.robot.scripting.DefaultJavaTestScript.Command;
import com.tplan.robot.scripting.DefaultJavaTestScript.Param;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Mapper of scripting language commands onto calls
 * of Java methods provided by the DefaultJavaTestScript class. It is used by the
 * Java converter.</p>
 *
 * <p>Note that conversion is not based on Java Reflection API
 * because Java doesn't save method argument names to the byte code. Any use
 * of reflection would also limit flexibility of how tests can be written.
 * The method parameters are rather annotated and the code is compiled
 * to save annotations to the bytecode.</p>
 *
 * @product.signature
 */
public class JavaTestScriptMethodMapper extends NumericEvaluator {

    // map = Map(command, commandMap)
    // commandMap = Map(paramMap, methodName)
    // paramMap = Map(argument, typeClass)
    private Map<String, Map<Method, Map<String, CharSequence>>> map = new HashMap();
    //          ^command     ^method  ^annotated_arguments
    private ConversionEvaluator gen;
    private TokenParser parser;

    public JavaTestScriptMethodMapper(ConversionEvaluator gen, TokenParser parser) {
        this.gen = gen;
        this.parser = parser;
        addClassName(DefaultJavaTestScript.class, parser);
    }

    public boolean supports(String command) {
        return map.containsKey(command.toLowerCase());
    }

    public String convert(List args, Map values, ScriptingContext ctx, Map<Class, String> exceptionMap, Map<String, String> importMap) {
        args = new ArrayList(args);
        String commandName = args.remove(0).toString().toLowerCase();

        Map<Method, Map<String, CharSequence>> cm = map.get(commandName);
        if (cm == null) {
            return null;
        }

        Method m = selectBestMethod(args, values, cm);
        if (m != null && exceptionMap != null) {
            // If the method throws an exception, add it to the converter
            Class[] cl = m.getExceptionTypes();
            for (Class c : cl) {
                exceptionMap.put(c, m.getName());
            }
            return map(args, values, ctx, m, importMap);
        }
        return null;
    }

    private CharSequence convertFileList(String value, ScriptingContext ctx, Map<String, String> importMap) {
        String s = "new File[] { ";
        importMap.put(File.class.getName(), "");
        if (value != null) {
            String t[] = value.split(TokenParser.FILE_PATH_SEPARATOR);
            List<CharSequence> l;
            for (int i = 0; i < t.length; i++) {
                l = gen.convertToExpressionElements(t[i], ctx);
                s += "new File(" + convertExpressionToType(t[i], l, String.class, ctx, importMap) + ")";
                if (i < t.length - 1) {
                    s += ", ";
                } else {
                    s += " ";
                }
            }
        }
        s += "}";
        return s;
    }

    private CharSequence convertKeyLocation(String value, Map<String, String> importMap) {
        try {
            int loc = parser.parseKeyLocation(value);
            importMap.put(KeyEvent.class.getName(), "");
            switch (loc) {
                case KeyEvent.KEY_LOCATION_STANDARD:
                    return "KeyEvent.KEY_LOCATION_STANDARD";
                case KeyEvent.KEY_LOCATION_LEFT:
                    return "KeyEvent.KEY_LOCATION_LEFT";
                case KeyEvent.KEY_LOCATION_RIGHT:
                    return "KeyEvent.KEY_LOCATION_RIGHT";
                case KeyEvent.KEY_LOCATION_NUMPAD:
                    return "KeyEvent.KEY_LOCATION_NUMPAD";
            }
        } catch (SyntaxErrorException ex) {
            ex.printStackTrace();
        }
        return "getContext().getParser().parseKeyLocation(\"" + value + "\")";
    }

    private CharSequence convertModifiers(String value, Map<String, String> importMap) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        String[] keys = value.split("\\+");
        String token;
        String s = "";
        importMap.put(InputEvent.class.getName(), "");

        for (int j = 0; j < keys.length; j++) {
            token = keys[j];
            if (token.equalsIgnoreCase("shift")) {
                s += "InputEvent.SHIFT_MASK";
            } else if (token.equalsIgnoreCase("alt")) {
                s += "InputEvent.ALT_MASK";
            } else if (token.equalsIgnoreCase("ctrl")) {
                s += "InputEvent.CTRL_MASK";
            } else if (token.equalsIgnoreCase("meta")) {
                s += "InputEvent.META_MASK";
            } else if (!token.equals("")) {
                return "getContext().getParser().parseModifiers(\"" + value + "\")";
            }
            if (j < keys.length - 1) {
                s += " | ";
            }
        }
        return s;
    }

    private CharSequence convertMouseButton(String value, Map<String, String> importMap) {
        try {
            int loc = parser.parseMouseButton(value);
            importMap.put(MouseEvent.class.getName(), "");
            switch (loc) {
                case MouseEvent.BUTTON1:
                    return "MouseEvent.BUTTON1";
                case MouseEvent.BUTTON2:
                    return "MouseEvent.BUTTON2";
                case MouseEvent.BUTTON3:
                    return "MouseEvent.BUTTON3";
            }
        } catch (SyntaxErrorException ex) {
            ex.printStackTrace();
        }
        return "getContext().getParser().parseMouseButton(\"" + value + "\")";
    }

    private String map(List args, Map values, ScriptingContext ctx, Method m, Map<String, String> importMap) {
        String s = m.getName() + "(";
//        System.out.println("Selected method " + m.getName() + "(" + m.getParameterTypes() + ")");

        // First process the list of arguments and remove all which are already provided
        // by the method annotation.
        args = new ArrayList(args);
        Command ac = m.getAnnotation(Command.class);
        List<String> paramNames = new ArrayList();
        Map<String, String> pm = ctx.getParser().parseParameters(ac.name(), paramNames);

        for (String param : paramNames) {
            if (pm.containsKey(param)) {
                param = param.toLowerCase();
            } else {
                param = "argument";
            }
            args.remove(param);
        }

        Class types[] = m.getParameterTypes();
        Class type;
        String value, defaultValue;
        List<CharSequence> l;
        String temp;

        // Now iterate over the list of method parameters and generate a value
        // or Java expression for each of them based on the values passed in the command call
        if (types.length > 0) {
            Param p;
            for (int i = 0; i < types.length; i++) {
                type = types[i];

                // Get parameter annotation
                p = getParamAnnotation(m, i);
                if (p != null) {

                    // If the command parameter has a value, convert it to the
                    // required type or a Java expression
                    value = (String) values.get(p.name());
                    if (value != null) {

                        // If the param annotation defines a code template, use it.
                        // Otherwise make an attempt to convert the value to a
                        // Java value or expression.
                        if (p.template().length() > 0) {
                            temp = p.template().replace("@value@", value);

                            // This checks for other known annotated flags
                            // which indicates
                            if (temp.contains("@keyLocation@")) {
                                temp = temp.replace("@keyLocation@", convertKeyLocation(value, importMap));
                            } else if (temp.contains("@fileList@")) {
                                temp = temp.replace("@fileList@", convertFileList(value, ctx, importMap));
                            } else if (temp.contains("@mouseButton@")) {
                                temp = temp.replace("@mouseButton@", convertMouseButton(value, importMap));
                            } else if (temp.contains("@modifiers@")) {
                                temp = temp.replace("@modifiers@", convertModifiers(value, importMap));
                            }
                            s += temp;
                        } else { // Convert to Java value or expression
                            l = gen.convertToExpressionElements(value, ctx);
                            s += convertExpressionToType(value, l, type, ctx, importMap);
                        }

                    } else {  // There's no value, use one of the default values
                        defaultValue = p.defaultValue();

                        // A valid annotated default value must be a non-empty string
                        if (defaultValue.length() > 0) {

                            // Annotated default value starting with "java:" is a Java code snippet
                            if (defaultValue.startsWith("java:")) {
                                s += defaultValue.substring("java:".length());
                            } else {  // Convert the annotated value to the desired Java type
                                l = new ArrayList();
                                l.add(defaultValue);
                                s += convertExpressionToType(defaultValue, l, type, ctx, importMap);
                            }
                        } else {  // Empty string -> use a superdefault value instead
                            s += getSuperDefaultValue(type);
                        }
                    }
                } else {  // Unannotated parameter, TODO
                    s += getSuperDefaultValue(type); //"<unannotated_param>";
                }
                if (i < types.length - 1) {
                    s += ", ";
                }
            }
            // TODO: add parameters (expressions)
        }
        s += ");\n";
        return s;
    }

    private String getSuperDefaultValue(Class type) {
        if (type.equals(Float.class) || type.equals(float.class)) {
            return "-1f";
        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            return "-1";
        } else if (type.equals(Long.class) || type.equals(long.class)) {
            return "-1";
        } else if (isSubclass(type, Number.class)) {
            return "-1";
        }
        return "null";
    }

    private boolean isSubclass(Class cl, Class superclass) {
        if (cl.equals(superclass)) {
            return true;
        }
        Class curr = cl;
        while ((curr = curr.getSuperclass()) != null) {
            if (curr.equals(superclass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convert an expression in the proprietary scripting language to a Java
     * expression of particular type.
     * @param l a sequence of expression elements.
     * @param type class of the requested type, for example String.class, Integer.class, Rectangle.class etc.
     * @param ctx execution context.
     * @return Java expression of given type.
     */
    String convertExpressionToType(String expression, List<CharSequence> l, Class type, ScriptingContext ctx, Map<String, String> importMap) {

        // Find out if the expression contains at least one string.
        // If it does, the whole expression is automatically a string.
        // Otherwise it may contain one or more variables.
        boolean containsString = false;
        for (CharSequence s : l) {
            if (s instanceof String) {
                containsString = true;
                break;
            }
        }

        // This flag indicates whether there's just one single string.
        // which usually represents a value
        boolean isSingleString = containsString && l.size() == 1;

        // If a String instance is requested, convert the expression to a string
        if (type.equals(String.class) || type.equals(Object.class)) {
            String expr = gen.convertToExpression(l);
            if (!containsString) { // If there are just variables, add empty string "" to convert it to a String
                expr = "\"\"+" + expr;
            }
            return expr;
        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            if (isSingleString) {
                Object value = gen.guessVariableType(l.get(0), ctx);
                if (value instanceof Number) {
                    return "" + ((Number) value).intValue();
                } else {
                    return value.toString().replace("{", "").replace("}", "");
                }
            }
            return "Integer.parseInt(" + convertExpressionToType(expression, l, String.class, ctx, importMap) + ")";
        } else if (type.equals(Long.class) || type.equals(long.class)) {
            if (isSingleString) {
                Object value = gen.guessVariableType(l.get(0), ctx);
                if (value instanceof Number) {
                    return "" + ((Number) value).longValue();
                } else {
                    return value.toString().replace("{", "").replace("}", "");
                }
            }
            return "Long.parseLong(" + convertExpressionToType(expression, l, String.class, ctx, importMap) + ")";
        } else if (type.equals(Float.class) || type.equals(float.class)) {
            if (isSingleString) {
                Object value = gen.guessVariableType(l.get(0), ctx);
                if (value instanceof Number) {
                    return "" + ((Number) value).floatValue();
                } else {
                    return value.toString().replace("{", "").replace("}", "");
                }
            }
            return "Float.parseFloat(" + convertExpressionToType(expression, l, String.class, ctx, importMap) + ")";
        } else if (type.equals(File.class)) {
            importMap.put(File.class.getName(), "");
            return "new File(" + convertExpressionToType(expression, l, String.class, ctx, importMap) + ")";
//              return "new File(" + gen.evaluateNumericExpression(expression, String.class) + ")";
        } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            if (isSingleString) {
                String s = (String) l.get(0);
                if (s.equals("\"true\"") || s.equals("true")) {
                    return "true";
                } else if (s.equals("\"false\"") || s.equals("false")) {
                    return "false";
                } else {
                    return s.replace("{", "").replace("}", "");
                }
            }
            return "new Boolean(" + convertExpressionToType(expression, l, String.class, ctx, importMap) + ")";
        } else if (type.equals(Point.class)) {
            // Shortcuts
            if (expression.equals("x:{_COMPARETO_CLICK_X},y:{_COMPARETO_CLICK_Y}")) {
                return "getContext().getSearchHitClickPoint()";
            } else if (expression.equals("x:{_SEARCH_X},y:{_SEARCH_Y}")) {
                return "getContext().getSearchHits().get(0)";
            }

            importMap.put(Point.class.getName(), "");
            try {
                Map<String, String> params = parser.parsePointParams(expression, "javagen");
                String x = "-1";  // Negative value will cause the command to use current pointer coordinate
                if (params.containsKey("x")) {
                    x = gen.evaluateNumericExpression(params.get("x"), Integer.class, ctx).toString();
                }
                String y = "-1";  // Negative value will cause the command to use current pointer coordinate
                if (params.containsKey("y")) {
                    y = gen.evaluateNumericExpression(params.get("y"), Integer.class, ctx).toString();
                }
                return "new Point(" + x + ", " + y + ")";

            } catch (SyntaxErrorException ex) {
                return "<invalid point syntax: " + expression + ">";
            }

        } else if (type.equals(Rectangle.class)) {
            importMap.put(Rectangle.class.getName(), "");
            try {
                Map<String, String> params = parser.parseRectangleParams(expression, "javagen");
                String x = "-1";  // Negative value will cause the command to use current pointer coordinate
                if (params.containsKey("x")) {
                    x = gen.evaluateNumericExpression(params.get("x"), Integer.class, ctx).toString();
                }
                String y = "-1";  // Negative value will cause the command to use current pointer coordinate
                if (params.containsKey("y")) {
                    y = gen.evaluateNumericExpression(params.get("y"), Integer.class, ctx).toString();
                }
                String w = "-1";  // Negative value will cause the command to use current pointer coordinate
                if (params.containsKey("w")) {
                    w = gen.evaluateNumericExpression(params.get("w"), Integer.class, ctx).toString();
                }
                String h = "-1";  // Negative value will cause the command to use current pointer coordinate
                if (params.containsKey("h")) {
                    h = gen.evaluateNumericExpression(params.get("h"), Integer.class, ctx).toString();
                }
                return "new Rectangle(" + x + ", " + y + ", " + w + ", " + h + ")";

            } catch (SyntaxErrorException ex) {
                return "<invalid rect syntax: " + expression + ">";
            }
        }
        return "<unimplemented-class " + type.getName() + ">";
    }

    private Param getParamAnnotation(Method m, int index) {
        Annotation pa[][] = m.getParameterAnnotations();
        Annotation paa[] = pa[index];
        for (Annotation a : paa) {
            if (a instanceof Param) {
                return ((Param) a);
            }
        }
        return null;
    }

    private Method selectBestMethod(List args, Map values, Map<Method, Map<String, CharSequence>> cm) {
        Map<String, CharSequence> l;
        boolean supportsAllParams;
        Method selected = null;
        Object param;
        CharSequence value;

        for (Method m : cm.keySet()) {
            l = cm.get(m);
            supportsAllParams = true;

            for (int i = 0; i < args.size(); i++) {
                param = args.get(i);
                if (!l.containsKey(param)) {
                    supportsAllParams = false;
                    break;
                } else { // Supports param but also check if there's a default value
                    value = l.get(param);

                    // If the value is a String instance, it is a parameter which
                    // is defined in the method annotation and it has a fixed
                    // value. That's why we have to check whether the values match
                    // and skip the method if they don't.
                    // An exception is when the parameter has no value at all
                    // because it may be an argument.
                    Object v = values.get(param);
                    if (value instanceof String && v != null && !value.toString().equals(v)) {
                        supportsAllParams = false;
                        break;
                    }
                }
            }

            // Now iterate over the annotated method parameters and make sure that if they
            // define a fixed value, the command contains it
            if (supportsAllParams) {
                for (String s : l.keySet()) {
                    value = l.get(s);
                    if (value instanceof String && value.length() > 0) {  // Fixed parameter value
                        if (((String) value).equalsIgnoreCase(s)) {  // param=value -> it is an additional argument
                            if (!args.contains(s)) {
                                supportsAllParams = false;
                                break;
                            }
                        } else if (!values.containsKey(s) || !values.get(s).toString().equalsIgnoreCase(value.toString())) {
                            supportsAllParams = false;
                            break;
                        }
                    }
                }
            }
            if (supportsAllParams) {
                // If a method got previously selected, choose the one with less
                // parameters
                if (selected != null) {
                    if (m.getParameterTypes().length < selected.getParameterTypes().length) {
                        selected = m;
                    }
                } else {
                    selected = m;
                }
            }
        }
        return selected;
    }

    public void addClassName(Class cl, TokenParser parser) {

        // Iterate over all declared methods
        for (Method m : cl.getDeclaredMethods()) {

            // Check if the method is annotated as a command call
            Annotation a = m.getAnnotation(Command.class);
            if (a == null) {
                continue;
            }

            Command c = (Command) a;
            mapMethod(c.name(), m, parser);
        }
    }

    /**
     * Create a map for a single method.
     *
     * @param name annotated command name. It will serve as a key to the map.
     * @param m associated method.
     */
    private void mapMethod(String annotatedName, Method m, TokenParser parser) {

        Map<String, CharSequence> argList = new HashMap();

        // Parse the prefix. It can contain just command name or a name
        // with any combination of argument and/or parameters.
        // Example of valid annotations:
        //    @Command (name="mouse")
        //    @Command (name="mouse click")
        //    @Command (name="mouse click btn=right")
        List<String> l = new ArrayList();
        Map<String, String> am = parser.parse(annotatedName, l);

        if (l.size() < 1) {
            throw new IllegalArgumentException("Invalid annotation of method " + m);
        }

        // The first token must be the command name. Pop it from the list.
        String name = (String) l.remove(0);

        // Other tokens should be saved as default values to the map.
        // Arguments will be saved with parameter name "argument".
        for (String s : l) {

            if (am.containsKey(s)) { // Regular 'param=value' expression
                argList.put(s, am.get(s));
            } else {   // If there's no name, it's an argument
                argList.put("argument", s);
            }
        }

        // Either get or create a command map
        Map<Method, Map<String, CharSequence>> commandMap = map.get(name);
        if (commandMap == null) {
            commandMap = new HashMap();
            map.put(name, commandMap);
        }

        Annotation pa[][] = m.getParameterAnnotations();
        Annotation paa[];
        Class types[] = m.getParameterTypes();
        Class type;  // Parameter type
        Param p;     // Parameter annotation

        for (int i = 0; i < types.length; i++) {
            type = types[i];
            paa = pa[i];
            p = null;

            for (Annotation a : paa) {
                if (a instanceof Param) {
                    p = ((Param) a);
                    break;
                }
            }

            // If the parameter is annotated, add id to the map. We use intentionally
            // an empty StringBuffer as a value because it indicates that it is a
            // method parameter without any predefined value. It allows to diferentiate
            // these from fixed params defined through the method annotation whose
            // values are always String instances.
            if (p != null) {
                argList.put(p.name(), new StringBuffer());
            }
        }
        commandMap.put(m, argList);
    }
}
