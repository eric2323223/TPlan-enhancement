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
import com.tplan.robot.scripting.*;
import com.tplan.robot.util.DocumentUtils;
import com.tplan.robot.scripting.DocumentWrapper;

import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import java.util.List;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

/**
 * Wrapper implementing functionality of the {@doc.cmd if} looping structure.
 * @product.signature
 */
public class IfWrapper extends StructuredBlockWrapper {

    boolean condition = false;
    boolean branchAlreadyExecuted = false;
    static NumericEvaluator evaluator = new NumericEvaluator();
    private String conditionExpression;
    private String conditionUnmodified = null;
    private boolean elsePresent = false;

    /**
     * Constructor.
     */
    public IfWrapper(StyledDocument document, DocumentWrapper parentWrapper, Element startElement, ScriptingContext repository, boolean validationMode) {
        super(document, parentWrapper, startElement, repository, validationMode);
//        System.out.println("Starting a new IF wrapper " + hashCode() + " at line #" + (getElementIndex(startElement) + 1));
        setParentWrapper(parentWrapper);
        setScriptFile(parentWrapper.getScriptFile());
        String line = DocumentUtils.getElementText(startElement);
        try {
            conditionUnmodified = validateElseIf(line, repository);
        } catch (Exception e) {
        }
        line = ((ProprietaryTestScriptInterpret) repository.getInterpret()).applyVariablesToCommand(line, repository);
        shouldExitWrapperOnThisElement(startElement, line, repository, validationMode, true);
        executionAllowed = condition;
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
    @Override
    public boolean isWithinSelection(Element e, boolean validationMode, ScriptingContext repository) {
        if (validationMode) {
            return true;
        }
//        boolean b =
//        System.out.println("IF wrapper " + hashCode() + ": inProcedure="+repository.containsKey(CONTEXT_PROCEDURE_DECLARATION_FLAG)
//                +", parent method = "+getParentWrapper().isWithinSelection(e, validationMode, repository)+", condition="+condition);
        return !isExit() && condition && !repository.containsKey(ScriptingContext.CONTEXT_PROCEDURE_DECLARATION_FLAG) && isExecutionAllowed() && getParentWrapper().isWithinSelection(e, validationMode, repository);
    }

    /**
     * Will be called when an end element starting with a '}' is reached.
     * We must then evaluate this command line whether it contains 'else' or 'else if'.
     *
     * @param e              currently executed element
     * @param text           element text.
     * @param repository     execution context (repository).
     * @param validationMode whether we are in the validation mode or in the execution one.
     * @return true if execution of the wrapper should exit, false if not.
     */
    @Override
    public boolean shouldExitWrapperOnThisElement(Element e, String text, ScriptingContext repository, boolean validationMode, boolean isWithinSelection) {

        // If the 'condition' is true, it means that we just finished execution of a block.
        if (condition) {
            executionAllowed = false;
            branchAlreadyExecuted = true;
        }

        boolean isJustEnd = text.trim().equals("}");

        // Exit if it is just a '}'
        if (isJustEnd) {
            setSelectionEndElement(e);
            return true;
        }

        // If we got until here, the command must contain an 'else' or 'else if' command.
        ScriptManager handler = repository.getScriptManager();

        Boolean fix = UserConfiguration.getInstance().getBoolean("scripting.commandFixingMode").booleanValue();
        final String line = DocumentUtils.getElementText(e);
        String cond = validateElseIf(line, repository);
        elsePresent = false;

        if (fix == null || fix) {
            if (cond != null) {
                List l = evaluator.parseExpression(cond);
                conditionExpression = "";
                for (Object o : l) {
                    if (evaluator.isOperator(o.toString())) {
                        conditionExpression += o;
                    } else {
                        conditionExpression += "\"" + o + "\"";
                    }
                }
                conditionExpression = ((ProprietaryTestScriptInterpret) repository.getInterpret()).applyVariablesToCommand(conditionExpression, repository);
            } else {
                validateElseIf(text, repository); // It must be here to detect duplicated 'else' branch
                conditionExpression = null;
            }
        } else {
            conditionExpression = validateElseIf(((ProprietaryTestScriptInterpret) repository.getInterpret()).applyVariablesToCommand(text, repository), repository);
        }

        // This branch corresponds to 'else if'
        if (conditionExpression != null) {
            conditionUnmodified = cond;
//            System.out.println("-- Evaluating \""+conditionExpression+"\" (original: \""+conditionUnmodified+"\"");
            try {
                Object o = evaluator.evaluateNumericExpression(conditionExpression, null, repository);
                condition = (o instanceof Boolean ? ((Boolean) o).booleanValue() : isExecutionAllowed()) && !branchAlreadyExecuted;
            } catch (IllegalArgumentException ex) {
                // Fix in 2.3/2.0.6 - do not throw the error if we are in a procedure declaration
                Boolean b = (Boolean) repository.get(ScriptingContext.CONTEXT_PROCEDURE_DECLARATION_FLAG);
                if (b == null || !b) {
                    throw ex;
                }
            }
        } else { // This branch corresponds to 'else'
            condition = !branchAlreadyExecuted;
            conditionUnmodified = null;
        }
        if (!text.trim().toLowerCase().startsWith("if")) {
//            System.out.println("Firing...");
            handler.fireScriptEvent(new ScriptDebuggingEvent(this, null, repository, ScriptDebuggingEvent.SCRIPT_DEBUG_STRUCTURE_WRAPPER_CONTINUED));
        }
        executionAllowed = condition;

//        System.out.println("shouldExitOnThisElement(): text="+text+", condition = "+condition+", executionAllowed = "+isExecutionAllowed());

        return false;
    }

    /**
     * This method validates 'if', 'else' and 'else if' commands.
     * It returns null if it is just a plain 'else'.
     * It returns the condition part of the command if it is 'if' or 'else if'.
     * It throws an IllegalArgumentException if the syntax is invalid.
     *
     * @param command
     * @return
     */
    private String validateElseIf(String command, ScriptingContext ctx) {
        String cond = null;
        try {

            // Bug 2901990, issue #1: trim all leading and trailing spaces
            command = command.trim();

            // Skip the beginning '}'
            if (command.startsWith("}")) {
                command = command.substring(1);
            }
            command = command.trim();

            // Bug 2901990, issue #2: allow more than one space between tokens (changed "\\s" to "\\s+")
            String tokens[] = command.split("\\s+");
            if (!command.endsWith("{")) {
                throw new IllegalArgumentException(ApplicationSupport.getString("ifWrapper.syntaxErr.braceExpected"));
            }
            if (tokens[0].equalsIgnoreCase("if")) {
                cond = command.substring(command.toLowerCase().indexOf("if") + 2, command.length() - 1).trim();
//                System.out.println("-- parsing if: '"+command+"', cond = "+cond);
            } else if (tokens[0].equalsIgnoreCase("else")) {
                if (tokens[1].equalsIgnoreCase("if")) {
                    cond = command.substring(command.toLowerCase().indexOf("if") + 2, command.length() - 1).trim();
//                    System.out.println("-- parsing if else: '"+command+"', cond = "+cond);
                } else {
                    if (tokens.length > 2 || !tokens[1].equals("{")) {
                        throw new IllegalArgumentException(ApplicationSupport.getString("ifWrapper.syntaxErr.errorInElse"));
                    }
                    // Bug 2901990, issue #3: check for duplicate 'else' statement
                    if (elsePresent) {
                        throw new IllegalArgumentException(ApplicationSupport.getString("ifWrapper.syntaxErr.duplicatedElse"));
                    }
                    elsePresent = true;
//                    System.out.println("-- parsing else");
                }
            } else {
                throw new IllegalArgumentException(ApplicationSupport.getString("ifWrapper.syntaxErr.generic"));
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ApplicationSupport.getString("ifWrapper.syntaxErr.generic"));
        }
        return cond;
    }

    public String toString() {
        return "IfWrapper";
    }

    /**
     * @return the conditionExpression
     */
    public String getConditionExpression() {
        return conditionUnmodified;
    }
}
