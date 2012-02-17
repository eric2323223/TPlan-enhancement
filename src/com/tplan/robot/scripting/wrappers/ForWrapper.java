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
package com.tplan.robot.scripting.wrappers;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.scripting.commands.CommandHandler;
import com.tplan.robot.scripting.*;
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import com.tplan.robot.util.DocumentUtils;

import java.io.IOException;
import java.util.ArrayList;
import javax.swing.text.Element;
import java.util.List;
import java.util.Map;
import javax.swing.text.StyledDocument;

/**
 * Wrapper implementing functionality of the {@doc.cmd for} looping structure.
 * @product.signature
 */
public class ForWrapper extends StructuredBlockWrapper {

    private int min = 0;
    private int max = 0;
    private String variable = "_FOR_VARIABLE";
    private List values = null;
    private ScriptManager scriptHandler;
    private String condition;
    private boolean conditionMet = true;
    private String loopExpression;
    private String initialExpression = null;
    private boolean conditionMode = false;
    private boolean breakFor = false;

    /**
     * Default constructor.
     */
    public ForWrapper() {
    }

    /**
     * Constructor.
     */
    public ForWrapper(StyledDocument document, DocumentWrapper parentWrapper, Element startElement, ScriptingContext repository, boolean validationMode) {
        super(document, parentWrapper, startElement, repository, validationMode);
//        System.out.println("Starting a new FOR wrapper " + hashCode() + " at line #" + (getElementIndex(startElement) + 1));
        setParentWrapper(parentWrapper);
        setScriptFile(parentWrapper.getScriptFile());
        String line = DocumentUtils.getElementText(startElement);
        scriptHandler = repository.getScriptManager();

        // Bug fix #
        if (isExecutionAllowed() || validationMode) {
            validateForStatement(line, repository);
            if (!conditionMode) {
                updateForVariable(repository);
            }
        }
        selectionStartElement = getNextElement(startElement, validationMode, repository);
    }

    /**
     * Find out if an element is within execution selection, i.e. if it is to be executed.
     * The method runs in two modes:
     * <li>We run the entire script - the method returns <code>true</code> for all elements.
     * <li>We run just selected commands - the method returns <code>true</code> if the argument element
     * is within selection and false if not.
     *
     * @param e a document element. Each element represents a single line in the document.
     * @return true if the element is to be executed, false if not.
     */
    public boolean isWithinSelection(Element e, boolean validationMode, ScriptingContext repository) {
        boolean b = getParentWrapper().isWithinSelection(e, validationMode, repository);
//        System.out.println("FOR wrapper " + hashCode() + ": parent isWithinSelection = "+b);
        boolean inRange = isConditionMode() ? conditionMet : min <= max;
        return !isExit() && b && !repository.containsKey(ScriptingContext.CONTEXT_PROCEDURE_DECLARATION_FLAG)
                && super.isWithinSelection(e, validationMode, repository) && inRange && isExecutionAllowed();
    }

    /**
     * Get an element which follows the argument one in the DOM (Document Object Model). It in fact returns the next
     * line of the document.
     *
     * @param element a document element.
     * @return next element defined in DOM or null if end of the document is reached.
     */
    public Element getNextElement(Element element, boolean validationMode, ScriptingContext repository) {
        Element e = null;
        if (validationMode && isConditionMode()) {  // If we are not in condition mode, validate all elements
            return super.getNextElement(element, validationMode, repository);
        }

        // This means that we reached end of the 'for' block
        if (element == null || element == selectionEndElement) {
            min++;

            // If we are still in the defined range, run the loop again by returning the first block element
            boolean inRange = isConditionMode() ? conditionMet : min <= max;
            if (inRange) {
                e = selectionStartElement;
                if (!isConditionMode()) {
                    updateForVariable(repository);
                }
            } else {
                executionAllowed = false;
            }
        }
        if (e == null) {
            e = super.getNextElement(element, validationMode, repository);
        }

        return e;
    }

    /**
     * Get the loop number the wrapper is currently at, starting from zero.
     * If the wrapper operates on a predefined set of values ('for x in xxx'),
     * the number returned is actually index of the value.
     * @return current loop number.
     */
    public int getLoopNumber() {
        return min;
    }

    private void updateForVariable(ScriptingContext context) {
        if (isConditionMode()) {
            if (loopExpression != null) {
                CommandHandler c = (CommandHandler) scriptHandler.getCommandHandlers().get("EVAL");
                List args = new ArrayList(3);
                try {
                    // Bug 2935527 fix - The 'for' statement crashes on numeric expression
                    String command = "EVAL " + getLoopExpression();
                    if (context.getInterpret() instanceof ProprietaryTestScriptInterpret) {
                        command = ((ProprietaryTestScriptInterpret) context.getInterpret()).applyVariablesToCommand(command, context);
                    }
                    Map m = context.getParser().parseParameters(command, args);
                    c.execute(args, m, context);
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (SyntaxErrorException e) {
                    // Improvement in 2.3 - reprocess the command to enclose all parameter values to double quotes properly
                    Boolean fix = UserConfiguration.getInstance().getBoolean("scripting.commandFixingMode").booleanValue();
                    if (fix == null || fix) {
                        int pos = loopExpression.indexOf('=');
                        String s = loopExpression.substring(pos + 1);
                        if (s.startsWith("\"") && s.endsWith("\"")) {
                            s = s.substring(1, s.length() - 1);
                        }
                        String varName = loopExpression.substring(0, pos);
                        List l = IfWrapper.evaluator.parseExpression(s);
                        s = "";
                        for (Object o : l) {
                            if (IfWrapper.evaluator.isOperator(o.toString())) {
                                s += o;
                            } else if (varName.equals(o) || context.getVariable(o.toString()) != null) {
                                s += "{" + o + "}";
                            } else {
                                s += o;
                            }
                        }
                        String expr = loopExpression.substring(0, loopExpression.indexOf('=')) + "=\"" + s + "\"";
                        String command = "EVAL " + expr;
                        if (context.getInterpret() instanceof ProprietaryTestScriptInterpret) {
                            command = ((ProprietaryTestScriptInterpret) context.getInterpret()).applyVariablesToCommand(command, context);
                        }
                        Map m = context.getParser().parseParameters(command, args);
                        try {
                            c.execute(args, m, context);
                            loopExpression = expr;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            if (getValues() != null && getValues().size() > min) {
                // Put the index into the variable table
                Map t = context.getVariables();
                Object o = getValues() == null ? new Integer(min) : getValues().get(min);
                t.put(getVariable(), o);
            }
//        System.out.println("For: Putting variable "+variable+"="+t.get(variable));
        }
    }

    private boolean isLoopConditionMet(ScriptingContext repository) {
        try {
//            ScriptManager handler = repository.getScriptManager();
            String expr = ((ProprietaryTestScriptInterpret) repository.getInterpret()).applyVariablesToCommand(getCondition(), repository);
            TokenParser parser = repository.getParser();
            boolean b = ((Boolean) parser.evaluateNumericExpression(expr, Boolean.class, repository)).booleanValue();
            return b;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean shouldExitWrapperOnThisElement(Element e, String text, ScriptingContext repository, boolean validationMode, boolean isWithinSelection) {
        setSelectionEndElement(e);
        boolean isJustEnd = text.trim().equals("}");
        if (!isJustEnd) {
            throw new IllegalArgumentException(ApplicationSupport.getString("forWrapper.syntaxErr.missingBrace") + "\n  " + text);
        }
        if (breakFor || !isWithinSelection) {
            return true;
        }
        if (validationMode && isConditionMode()) {
            return true;
        } else {
            if (isConditionMode()) {
                updateForVariable(repository);
                conditionMet = isLoopConditionMet(repository);
                return !conditionMet;
            } else {
                return min >= max;
            }
        }
    }

    private void validateForStatement(String line, ScriptingContext repository) {
        String temp = line.trim().substring("FOR".length()).trim();
        if (!line.endsWith("{")) {
            throw new IllegalArgumentException(ApplicationSupport.getString("forWrapper.syntaxErr.braceExpected"));
        }
        temp = temp.substring(0, temp.length() - 1);
        TokenParser parser = repository.getParser();

        // First variant - 'for () {'
        if (temp.startsWith("(")) {
            // Get rid of the trailing '{'
            temp = temp.substring(0, temp.length() - 1).trim();
            if (!temp.endsWith(")")) {
                throw new IllegalArgumentException(ApplicationSupport.getString("forWrapper.syntaxErr.missingBracket"));
            }
            // Get rid of the leading '(' and trailing ')'
            temp = temp.substring(1, temp.length() - 1).trim();
            String tokens[] = temp.split("[,;]");  // Need a better way - what if comparing a string constant containing a space or a semicolon?
            if (tokens.length > 3 || tokens.length < 2) {
                throw new IllegalArgumentException(ApplicationSupport.getString("forWrapper.syntaxErr.generic"));
            }
            conditionMode = true;

            CommandHandler cm = (CommandHandler) scriptHandler.getCommandHandlers().get("EVAL");
            Boolean b = (Boolean) repository.get(ScriptingContext.CONTEXT_PROCEDURE_DECLARATION_FLAG);
            final boolean inProcedureDeclaration = b != null && b;

            // If the first token is not empty, process it
            String token = tokens[0].trim();
            if (!"".equals(token)) {
                try {
                    token = ((ProprietaryTestScriptInterpret) repository.getInterpret()).applyVariablesToCommand(token, repository);
                    token = addDoubleQuotes(token);
                    initialExpression = token;
                    List args = new ArrayList(3);
                    Map map = repository.getParser().parse(token, args);
                    try {
                        cm.execute(args, map, repository);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                } catch (SyntaxErrorException e) {
                    // If we are in a procedure declaration, ignore the error
                    // 2.3 bug fix, 12 Jan 2011
                    if (!inProcedureDeclaration) {
                        throw new IllegalArgumentException(e.getMessage());
                    }
                }
            }

            token = tokens[1].trim();
            if ("".equals(token)) {
                throw new IllegalArgumentException(ApplicationSupport.getString("forWrapper.syntaxErr.emptyCondition"));
            }
            condition = token;
            token = ((ProprietaryTestScriptInterpret) repository.getInterpret()).applyVariablesToCommand(token, repository);

            // If the condition syntax is invalid, this will throw an exception
            String expr = ((ProprietaryTestScriptInterpret) repository.getInterpret()).applyVariablesToCommand(token, repository);
            try {
                parser.evaluateNumericExpression(expr, Boolean.class, repository);
            } catch (IllegalArgumentException e) {

                // If we are in a procedure declaration, ignore the error
                // 2.3 bug fix, 12 Jan 2011
                if (inProcedureDeclaration) {
                    return;
                }
                // Improvement in 2.3 - reprocess the command to enclose all parameter values to double quotes properly
                Boolean fix = UserConfiguration.getInstance().getBoolean("scripting.commandFixingMode").booleanValue();
                if (fix == null || fix) {
                    List l = IfWrapper.evaluator.parseExpression(expr);
                    String s = "";
                    for (Object o : l) {
                        if (IfWrapper.evaluator.isOperator(o.toString())) {
                            s += o;
                        } else if (repository.getVariable(o.toString()) != null) {
                            s += "{" + o + "}";
                        } else {
                            s += o;
                        }
                    }
                    expr = ((ProprietaryTestScriptInterpret) repository.getInterpret()).applyVariablesToCommand(s, repository);
                    parser.evaluateNumericExpression(expr, Boolean.class, repository);
                    token = s;
                    condition = s;
                } else {
                    throw e;
                }
            }

            token = tokens.length > 2 ? tokens[2].trim() : "";
            if (!"".equals(token)) {
                loopExpression = addDoubleQuotes(token);
            }
        } // Second variant - 'for x in ... {'
        else {
            line = ((ProprietaryTestScriptInterpret) repository.getInterpret()).applyVariablesToCommand(line, repository);
            List v = new ArrayList();
            parser.parseParameters(line, v);
            if (v.size() < 2 || !v.get(1).toString().equalsIgnoreCase("IN")) {
                throw new IllegalArgumentException(ApplicationSupport.getString("forWrapper.syntaxErr.genericEnumeratedType"));
            }

            if (v.size() == 3) {
                executionAllowed = false;
            }
            variable = (String) v.remove(0);  // Remove the variable name
            v.remove(0);                      // Remove the 'IN' keyword
            if (v.get(v.size() - 1).equals("{")) {
                v.remove(v.size() - 1);
            }
            setValues(v);
            conditionMode = false;
        }
    }

    private String addDoubleQuotes(String expr) {
        int index = expr.indexOf('=');
        if (index > 0 && index < expr.length()) {
            String prefix = expr.substring(0, index + 1) + "\"" + expr.substring(index + 1) + "\"";
//            System.out.println("addDoubleQuotes(): expr="+expr+", result="+prefix);
            return prefix;
        }
        return expr;
    }

    private void setValues(List values) {
        this.values = values;
        if (values != null) {
            min = 0;
            max = values.size() - 1;
        }
    }

    public String toString() {
        return "ForWrapper";
    }

    public void breakFor() {
        executionAllowed = false;
        breakFor = true;
    }

    /**
     * @return the conditionMode
     */
    public boolean isConditionMode() {
        return conditionMode;
    }

    /**
     * @return the loopExpression
     */
    public String getLoopExpression() {
        return loopExpression;
    }

    /**
     * @return the condition
     */
    public String getCondition() {
        return condition;
    }

    /**
     * @return the values
     */
    public List getValues() {
        return values;
    }

    /**
     * @return the initialExpression
     */
    public String getInitialExpression() {
        return initialExpression;
    }

    /**
     * @return the variable
     */
    public String getVariable() {
        return variable;
    }
}
