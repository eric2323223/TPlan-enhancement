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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.List;

/**
 * Parser and evaluator of numeric and boolean expressions as is defined
 * in the {@doc.cmd numeric Numeric Expressions} and {@doc.cmd boolean Boolean Expressions}
 * chapters of the {@product.name} language specification.
 *
 * @product.signature
 */
public class NumericEvaluator {

    // TODO: unary operators
    // TODO: check if priority is handled correctly
    static final String OPERATOR_LEFT_PARENTHESIS = "(";
    static final String OPERATOR_RIGHT_PARENTHESIS = ")";
    static final String OPERATOR_PLUS = "+";
    static final String OPERATOR_MINUS = "-";
    static final String OPERATOR_MULTIPLY = "*";
    static final String OPERATOR_MODULO = "%";
    static final String OPERATOR_DIVIDE = "/";
    static final String OPERATOR_EQUALS = "==";
    static final String OPERATOR_EQUALS_NOT = "!=";
    static final String OPERATOR_AND = "&&";
    static final String OPERATOR_OR = "||";
    static final String OPERATOR_GREATER_THAN = ">";
    static final String OPERATOR_LOWER_THAN = "<";

    // NOTE: The current limitation is that any operator can't be a prefix of another one.
    // Make sure that you meet this rule before adding new operators! You can't e.g. have operators '=' and '==' !
    // PRIORITY: the lower index, the higher priority
    static final String NUMERIC_OPERATORS[] = {
        OPERATOR_DIVIDE,
        OPERATOR_MULTIPLY,
        OPERATOR_MODULO,
        OPERATOR_MINUS,
        OPERATOR_PLUS
    };
    static final String BOOLEAN_OPERATORS[] = {
        OPERATOR_EQUALS,
        OPERATOR_EQUALS_NOT,
        OPERATOR_GREATER_THAN,
        OPERATOR_LOWER_THAN,
        OPERATOR_AND,
        OPERATOR_OR
    };
    static final String UNARY_OPERATORS[] = {
        OPERATOR_MINUS,
        OPERATOR_PLUS
    };
    protected boolean debug = "true".equals(System.getProperty("vncrobot.eval.debug"));
    protected Map operators;
    protected Map numericOperators;
    protected Map booleanOperators;
    protected Map unaryOperators;

    public NumericEvaluator() {
        String ops[] = NUMERIC_OPERATORS;
        operators = new HashMap();
        numericOperators = new HashMap();
        for (int i = 0; i < ops.length; i++) {
            numericOperators.put(ops[i], new Integer(i + 2));
            operators.put(ops[i], new Integer(i + 2));
        }

        // Boolean operators
        ops = BOOLEAN_OPERATORS;
        booleanOperators = new HashMap();
        for (int i = 0; i < ops.length; i++) {
            booleanOperators.put(ops[i], new Integer(i));
            operators.put(ops[i], new Integer(50));
        }

        ops = UNARY_OPERATORS;
        unaryOperators = new HashMap();
        for (int i = 0; i < ops.length; i++) {
            unaryOperators.put("unary" + ops[i], new Integer(i));
            operators.put("unary" + ops[i], new Integer(i));
        }

        operators.put(OPERATOR_OR, new Integer(60));
        operators.put(OPERATOR_AND, new Integer(59));

        operators.put(OPERATOR_LEFT_PARENTHESIS, new Integer(100));
        operators.put(OPERATOR_RIGHT_PARENTHESIS, new Integer(100));

//        System.out.println("1>2 && 2>1 || 1==1 "+(1>2 && 2>1 || 1==1));
//        System.out.println("1==1 || 1>2 && 2>1 "+(1==1 || 1>2 && 2>1));
    }

    public Object evaluateNumericExpression(String expression, Class resultType) {
        return evaluateNumericExpression(expression, resultType, null);
    }

    public Object evaluateNumericExpression(String expression, Class resultType, ScriptingContext context) {
        try {
            expression = expression.trim();
            if (debug) {
                System.out.println("Evaluating numeric expression: '" + expression + "'");
            }

            List v = parseExpression(expression);
            if (debug) {
                System.out.println(" -- Parsed elements: " + v);
            }

            Stack varStack = new Stack();
            Stack opStack = new Stack();

            Object s, op;
            Object var, var2;

            // These counters are caused to detect unary operators.
            int opCnt = 0, varCnt = 0;

            for (int i = 0; i < v.size(); i++) {
                s = v.get(i);

                if (s.equals(OPERATOR_RIGHT_PARENTHESIS)) {
                    opCnt = 0;
                    varCnt = 1;
                    op = opStack.pop();

                    while (!op.equals(OPERATOR_LEFT_PARENTHESIS)) {
                        var = varStack.pop();
                        var2 = varStack.size() > 0 && !isUnary(op) ? varStack.pop() : null;
                        varStack.push(evaluate(var2, var, op));
                        op = opStack.pop();
                        debug(varStack, opStack, s);
                    }
                    if (opStack.size() > 0 && isUnary((String) opStack.peek())) {
                        op = opStack.pop();
                        var = varStack.pop();
                        varStack.push(evaluate(null, var, op));
                        debug(varStack, opStack, s);
                    }
                } else if (s.equals(OPERATOR_LEFT_PARENTHESIS)) {
                    opCnt = varCnt = 0;
                    opStack.push(s);
                    debug(varStack, opStack, s);
                } // The token is an operator -> process it
                else if (operators.containsKey(s)) {
                    opCnt++;
                    boolean unary = opCnt > varCnt;
                    if (unary) {
                        opCnt = varCnt = 0;
                        s = "unary" + s;
                    }
                    int priority = ((Number) operators.get(s)).intValue();
                    while (opStack.size() > 0 && !opStack.peek().equals(OPERATOR_LEFT_PARENTHESIS)) {
                        op = opStack.peek();
                        if (varStack.size() > 1 && ((Number) operators.get(op)).intValue() < priority) {
                            opStack.pop();
                            var = varStack.pop();
                            var2 = varStack.size() > 0 && !isUnary(op) ? varStack.pop() : null;
                            varStack.push(evaluate(var2, var, op));
                            debug(varStack, opStack, s);
                        } else {
                            break;
                        }
                    }
                    opStack.push(s);
                    debug(varStack, opStack, s);
                } // Anything else can be just a string constant
                else {
                    varCnt++;
                    varStack.push(s);
                    debug(varStack, opStack, s);
                }
            }
            while (opStack.size() > 0 && !opStack.peek().equals(OPERATOR_LEFT_PARENTHESIS)) {
                op = opStack.pop();
                if (varStack.size() > 0) {
                    var = varStack.pop();
                    var2 = varStack.size() > 0 && !isUnary(op) ? varStack.pop() : null;
                    varStack.push(evaluate(var2, var, op));
                    debug(varStack, opStack, "");
                }
            }

            if (varStack.size() != 1 || opStack.size() > 0) {
                throw new IllegalArgumentException("Can't evaluate expression '" + expression + "'.");
            }

            Object o = convertResult(varStack.pop(), resultType, v.size() == 1);
            return o;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Can't evaluate expression '" + expression + "'.", ex);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Can't evaluate expression '" + expression + "'.", ex);
        }
    }

    private boolean isUnary(Object op) {
        return op.toString().startsWith("unary");
    }

    private void debug(Stack vars, Stack ops, Object token) {
        if (debug) {
            System.out.println(" " + ops.toString() + "  " + vars.toString() + "  " + token);
        }
    }

    protected Object convertResult(Object lastStackItem, Class resultType, boolean isSingleOperandExpression) {
        if (lastStackItem instanceof String) {
            if (resultType != null) {
                if (resultType.equals(Integer.class)) {
                    return new Integer(Integer.parseInt((String) lastStackItem));
                }
            }
            return new Double(Double.parseDouble((String) lastStackItem));
        }
        return lastStackItem;
    }

    protected Object evaluate(Object var1, Object var2, Object operand) {

        // Return value (will be a Number or Boolean instance)
        Object o = null;

        // Second argument is null -> must be an unary operator
        if (var1 == null) {
            if (unaryOperators.containsKey(operand.toString())) {
                double arg1;
                try {
                    arg1 = var2 instanceof Number ? ((Number) var2).doubleValue() : Double.parseDouble(var2.toString());
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Expression evaluation error: Unary operator '" + operand + "' requires one numeric argument.");
                }
                if (operand.equals("unary" + OPERATOR_PLUS)) {
                    o = new Float(arg1);
                } else if (operand.equals("unary" + OPERATOR_MINUS)) {
                    o = new Float(-arg1);
                }
            }

        } // Numeric operators - plus, minus, multiply, divide
        else if (numericOperators.containsKey(operand)) {
            double arg1, arg2;
            try {
                arg1 = var1 instanceof Number ? ((Number) var1).doubleValue() : Double.parseDouble(var1.toString());
                arg2 = var2 instanceof Number ? ((Number) var2).doubleValue() : Double.parseDouble(var2.toString());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Expression evaluation error: Operator '" + operand + "' requires two numeric arguments.");
            }
            if (operand.equals(OPERATOR_PLUS)) {
                o = new Float(arg1 + arg2);
            } else if (operand.equals(OPERATOR_MINUS)) {
                o = new Float(arg1 - arg2);
            } else if (operand.equals(OPERATOR_MODULO)) {
                o = new Float((long)arg1 % (long)arg2);
            } else if (operand.equals(OPERATOR_MULTIPLY)) {
                o = new Float(arg1 * arg2);
            } else if (operand.equals(OPERATOR_DIVIDE)) {
                o = new Float(arg1 / arg2);
            }
        } // Boolean operators - equals, not equals, greater than, less than, AND, OR
        else if (booleanOperators.containsKey(operand)) {
            if (operand.equals(OPERATOR_OR)) {
                // Operator OR always requires two boolean arguments
                if (var1 instanceof Boolean && var2 instanceof Boolean) {
                    o = new Boolean(((Boolean) var1).booleanValue() || ((Boolean) var2).booleanValue());
                } else {
                    throw new IllegalArgumentException("Expression evaluation error: Operator '" + operand + "' requires two boolean arguments.");
                }
            } else if (operand.equals(OPERATOR_AND)) {
                // Operator AND always requires two boolean arguments
                if (var1 instanceof Boolean && var2 instanceof Boolean) {
                    o = new Boolean(((Boolean) var1).booleanValue() && ((Boolean) var2).booleanValue());
                } else {
                    throw new IllegalArgumentException("Expression evaluation error: Operator '" + operand + "' requires two boolean arguments.");
                }
            } else {
                // Other boolean operators allow comparison between numbers/strings
                Number arg1 = null, arg2 = null;
                try {
                    arg1 = var1 instanceof Number ? (Number) var1 : new Double(Double.parseDouble(var1.toString()));
                    arg2 = var2 instanceof Number ? (Number) var2 : new Double(Double.parseDouble(var2.toString()));
                } catch (Exception ex) {
                    // If both arguments are not numbers, we'll perform String comparison
                }
                if (operand.equals(OPERATOR_EQUALS) || operand.equals(OPERATOR_EQUALS_NOT)) {
                    if (arg1 != null && arg2 != null) {
                        o = new Boolean(arg1.doubleValue() == arg2.doubleValue());
                    } else {
                        o = new Boolean(var1.toString().equals(var2.toString()));
                    }
                    if (operand.equals(OPERATOR_EQUALS_NOT)) {
                        o = new Boolean(!((Boolean) o).booleanValue());
                    }
                } else if (operand.equals(OPERATOR_GREATER_THAN)) {
                    if (arg1 != null && arg2 != null) {
                        o = new Boolean(arg1.doubleValue() > arg2.doubleValue());
                    } else {
                        throw new IllegalArgumentException("Expression evaluation error: Operator '" + operand + "' requires two numeric arguments.");
                    }
                } else if (operand.equals(OPERATOR_LOWER_THAN)) {
                    if (arg1 != null && arg2 != null) {
                        o = new Boolean(arg1.doubleValue() < arg2.doubleValue());
                    } else {
                        throw new IllegalArgumentException("Expression evaluation error: Operator '" + operand + "' requires two numeric arguments.");
                    }
                }
            }
        }
        if (o == null) {
            throw new IllegalArgumentException("Illegal operator '" + operand + "'.");
        }
        return o;
    }

    public String[] getBinaryOperators() {
        return NUMERIC_OPERATORS;
    }

    protected String endsWithOperator(String s) {
        if (s != null && s.length() > 0) {
            Iterator e = operators.keySet().iterator();
            String op;
            while (e.hasNext()) {
                op = (String) e.next();
                if (s.endsWith(op)) {
                    return op;
                }
            }
        }
        return null;
    }

    public boolean isOperator(String s) {
        return operators.containsKey(s);
    }

    public List parseExpression(String expression) {
        List v = new ArrayList();
        boolean inQuote = false;

        int index = 0;
        String s = "", op = null;
        char c, charBefore = 0;
        do {
            c = expression.charAt(index++);
            op = endsWithOperator(s + c);

            if (c == '"' && (charBefore > 0 && charBefore != '\\')) {
                if (inQuote) {
                    v.add(s);
                    s = "";
                    inQuote = false;
                } else {
                    inQuote = true;
                    s = "";
                }
                continue;
            }

//            if ((!Character.isWhitespace(c) || inQuote) && op == null) {
            if (inQuote || (!Character.isWhitespace(c) && op == null)) {
                s += c;
            }

            if (!inQuote && op != null) {
                if (s.length() > op.length() - 1) {
                    v.add(s.substring(0, s.length() - op.length() + 1));
                }
                v.add(op);
                s = "";
            }
            charBefore = c;
        } while (index < expression.length());
        if (s != null && s.length() > 0 && op == null) {
            v.add(s);
        }
        return v;
    }

//    public static void main(String args[]) {
//        NumericEvaluator ne = new NumericEvaluator();
//        ne.test();
//    }
    public void test() {
        boolean result = true;
        result = result && testExpr("", null, true);
        result = result && testExpr("1+1>1", new Boolean(true), false);
        result = result && testExpr("1+1<1", new Boolean(false), false);
        result = result && testExpr("(1+1>1)&&(\"test\"==test)", new Boolean(true), false);
        result = result && testExpr("(1+1<1)&&(\"test\"==test)", new Boolean(false), false);
        result = result && testExpr("(1+1<1)||(\"test\"==test)", new Boolean(true), false);
        result = result && testExpr("(1<1+1)||(\"test\"==test)", new Boolean(true), false);

        result = result && testExpr("10*20/2", new Double(100), false);
        result = result && testExpr("10*(20/2)", new Double(100), false);
        result = result && testExpr("10/10*2", new Double(2), false);
        result = result && testExpr("(10*2)/(10*2)", new Double(1), false);

        System.out.println(">>> Overall result: " + (result ? "PASS" : "FALSE"));
    }

    private boolean testExpr(String expr, Object expectedResult, boolean errorExpected) {
        boolean pass = true;
        try {
            Object result = this.evaluateNumericExpression(expr, Double.class);
            System.out.print("TEST: Evaluating '" + expr + "'");
            if (result instanceof Boolean && expectedResult instanceof Boolean) {
                if (((Boolean) result).booleanValue() == ((Boolean) expectedResult).booleanValue()) {
                    System.out.println(" - PASS: result=" + result + ", expected=" + expectedResult);
                } else {
                    boolean debugValue = debug;
                    debug = true;
                    this.evaluateNumericExpression(expr, Double.class);
                    debug = debugValue;
                    System.out.println(" - FAIL: result=" + result + ", expected=" + expectedResult);
                    pass = false;
                }
            } else if (result instanceof Number && expectedResult instanceof Number) {
                if (((Number) result).doubleValue() == ((Double) expectedResult).doubleValue()) {
                    System.out.println(" - PASS: result=" + result + ", expected=" + expectedResult);
                } else {
                    boolean debugValue = debug;
                    debug = true;
                    this.evaluateNumericExpression(expr, Double.class);
                    debug = debugValue;
                    System.out.println(" - FAIL: result=" + result + ", expected=" + expectedResult);
                    pass = false;
                }
            } else {
                boolean debugValue = debug;
                debug = true;
                this.evaluateNumericExpression(expr, Double.class);
                debug = debugValue;
                System.out.println(" - FAIL: result=" + result + ", expected=" + expectedResult);
                pass = false;
            }
        } catch (IllegalArgumentException ex) {
            if (errorExpected) {
                System.out.println("TEST: Evaluating '" + expr + "' - PASS: expected error was detected: " + ex.getMessage());
            } else {
                ex.printStackTrace();
                System.out.println("TEST: Evaluating '" + expr + "' - FAIL: unexpected error was detected: " + ex.getMessage());
                pass = false;
            }
        }
        return pass;
    }
}
