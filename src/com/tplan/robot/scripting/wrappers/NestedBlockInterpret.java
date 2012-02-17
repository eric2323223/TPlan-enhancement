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

import com.tplan.robot.scripting.DocumentWrapper;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.SyntaxErrorException;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

/**
 * Interface of a nested script wrapper allowing to include blocks of commands
 * in another language (for example Java) into proprietary scripts.
 * @product.signature
 */
public interface NestedBlockInterpret {

    /**
     * Initialize the wrapper. Will be called by the script interpret when the
     * block header is reached. The wrapper must in this method search the
     * document for the specific block end construction and consume the text
     * between the block header and end. The located block end element must be
     * after the call of this method made available through the <code>getEndElement()</code>
     * method because the interpret will query it to know where to jump.
     *
     * If the <code>compileOnly</code> flag is true, the wrapper is supposed just
     * to compile the block code (check its syntax). If the flag is false the wrapper
     * is supposed to execute the block code (and eventually recompile it first
     * if needed).
     * @param document a document containing the script DOM.
     * @param parentWrapper parent wrapper calling this wrapper.
     * @param startElement block start element (the element which was identified
     * to start with the reserved name of this wrapper).
     * @param ctx context with variables and all necessary framework object references..
     * @param compileOnly true when the script is being just compiled, false if it
     * is being executed.
     * @throws com.tplan.robot.scripting.SyntaxErrorException the method is supposed
     * to throw this exception when the block header or end statement has incorrect
     * syntax or if the code inside the block is invalid.
     */
    void init(StyledDocument document, DocumentWrapper parentWrapper,
            Element startElement, ScriptingContext ctx, boolean compileOnly) throws SyntaxErrorException;

    /**
     * Get block name (not case sensitive) which identifies and starts the block. The name must not
     * conflict with another existing command or block name. For example,
     * let's have a block of Perl commands in the script as follows:
     *
     * <blockquote>
     * <pre>
     * perl {
     *   <Perl command 1>
     *   <Perl command 2>
     * } endperl
     * </pre>
     * <blockquote>
     * The method in such a case should return "perl" to make the test interpret
     * create this wrapper whenever it finds a line starting with "perl".
     * @return block name.
     */
    String getCode();

    /**
     * Get the block end element in the document. The wrapper is supposed to search
     * the document for the end element in the <code>init()</code> method and make it
     * available through this method. The test script interpret picks up the element
     * right after the init phase and jumps there to avoid interpreting of the block
     * code as standard script commands.
     *
     * @return block end element.
     */
    Element getEndElement();

    /**
     * Convert to the code of the given type (TestScriptInterpret.TYPE_JAVA or other).
     * It is called by code converters. If the code can't be converted, the
     * method may return null.
     * @param targetType type of test script interpret (see the TYPE_ constants 
     * in the TestScriptInterpret interface).
     * @return code converted to the target type or null.
     */
    String convertContent(int targetType);
}
