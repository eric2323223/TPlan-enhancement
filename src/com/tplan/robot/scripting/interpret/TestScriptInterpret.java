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
package com.tplan.robot.scripting.interpret;

import com.tplan.robot.scripting.*;
import java.io.IOException;
import java.net.URI;
import javax.swing.text.StyledDocument;

/**
 * Generic test script interface. It describes methods of a test script
 * written in a particular language.
 *
 * @product.signature
 */
public interface TestScriptInterpret {

    final int TYPE_UNKNOWN = 0;
    final int TYPE_PROPRIETARY = 1;
    final int TYPE_JAVA = 2;

    /**
     * Get the test script type. Reserved values are 0 (unknown language), 1 (test script in
     * the {@product.name} proprietary scripting language) and 2 (Java test scripts).
     *
     * @return test script type.
     */
    int getType();

    /**
     * Get typical file extensions that should be handled by a particular 
     * implementation of a test script. Test script interpret factory ({@link TestScriptInterpretFactory}) 
     * takes advantage of this method to create interpret instances for particular 
     * test script files.
     *
     * @return list of specific file extensions which should be handled by this
     * interpret. For example, a Java interpret would return <code>new String[] { "java" }</code>.
     * If the method returns null, it means that there is no specific extension
     * for the script type handled by this class. The latter case applies for example
     * for the proprietary test script format which may be saved with any extension.
     */
    String[] getSupportedFileExtensions();

    /**
     * Get test script source code URI.
     * @return test script source code URI
     */
    URI getURI();

    /**
     * Set the test script URI. If the {@code load} argument is true, the interpret
     * should load content from the URI to the document. A value of {@code false} is
     * usually used in "Save As" operations where the URI changes for the
     * current document content. Saving of the file is however not performed by
     * this method.
     *
     * @param uri test script URI.
     * @param load whether to open the URI in the current document (see {@link #getDocument()}).
     * @throws IOException if the load parameter is true and loading of tets script fails.
     */
    void setURI(URI uri, boolean load) throws IOException;

    /**
     * Set the script manager associated with this test script interpret.
     * @param sm
     */
    void setScriptManager(ScriptManager sm);

    /**
     * Get the script manager associated with this test interpret instance.
     * @return script manager associated with this test interpret
     */
    ScriptManager getScriptManager();

    /**
     * Compile source code of the test script. If the script is in a scripting language,
     * the method should check the code syntax. If the script is in a compilable
     * language (for example Java test scripts), the method should compile the code
     * and make it ready for execution.
     *
     * @param customContext customized context for this validation. If the argument
     * is null, the interpret is supposed to obtain a preinitialized context instance
     * from the script manager through {@link ScriptManager#createDefaultContext()}.
     * @return true if the script compiled fine, false if there were errors.
     * Compilation errors are to be saved as a list of {@link SyntaxErrorException}
     * instances to the compilation context. The interpret is also supposed to fire
     * appropriate compilation script events through the associated script manager.
     * The context should be made available after compilation through
     * the {@link #getCompilationContext()} method.
     *
     * @throws InterpretErrorException should be thrown on an internal error or
     * illegal interpret state. The exception message gets typically displayed
     * or otherwise reported to the user.
     */
    boolean compile(ScriptingContext customContext) throws InterpretErrorException;

    /**
     * Execute the test script.
     *
     * // TODO: document particular events
     *
     * @param customContext customized context for this execution. If the argument
     * is null, the interpret is supposed to obtain a preinitialized context instance
     * from the script manager through {@link ScriptManager#createDefaultContext()}.
     * The context should be made available after execution through
     * the {@link #getExecutionContext()} method.
     *
     * @return execution result (exit code).
     * 
     * @throws InterpretErrorException should be thrown on an internal error or
     * illegal interpret state. The exception message gets typically displayed
     * or otherwise reported to the user.
     */
    int execute(ScriptingContext customContext) throws InterpretErrorException;

    /**
     * Get context from the last execution. If the script hasn't been executed
     * through {@link #execute(ScriptingContext)}, the method should return null.
     * @return context from the last execution.
     */
    ScriptingContext getExecutionContext();

    /**
     * Get context from the last compilation. If the script hasn't been compiled
     * through {@link #compile(ScriptingContext)}, the method should return null.
     * @return context from the last compilation.
     */
    ScriptingContext getCompilationContext();

    /**
     * <p>Indicate whether it is possible to execute just a part of the test script. 
     * This is typically possible with scripting languages, such as the proprietary
     * {@product.name} one, where user can select part of the code in the editor and 
     * execute it. It is on the other hand not possible with test scripts written 
     * in Java.</p>
     * 
     * <p>Interprets returning <code>true</code> should expect the calls of the 
     * {@link #setSelection(int, int)} method. Interprets which do not support 
     * this feature and return <code>false</code> should throw an exception when 
     * the method is called.
     * @return true if partial execution is supported or false otherwise.
     */
    boolean isPartialExecutionAllowed();

    /**
     * Set the start and end offset for partial execution of a test script.
     * The offsets refer to positions in the document returned by the {@link #getDocument()}
     * method. Interprets which do not support partial executions (i.e. their
     * {@link #isPartialExecutionAllowed()} method returns false) should throw
     * an {@code IllegalStateException} exception if this method gets called.
     * @param startOffset start offset indicating position in the test script
     * document to start the execution from.
     * @param endOffset end offset in the test script where execution should be finished.
     * @throws IllegalStateException if the method gets called even though the {@link #isPartialExecutionAllowed()}
     * method returns false to indicate that this mode is not supported.
     */
    void setSelection(int startOffset, int endOffset) throws IllegalStateException;

    /**
     * Reset selection for partial execution purposes. Interprets which do not support partial executions (i.e. their
     * {@link #isPartialExecutionAllowed()} method returns false) should throw
     * an {@code IllegalStateException} exception if this method gets called.
     * @throws IllegalStateException if the method gets called even though the {@link #isPartialExecutionAllowed()}
     * method returns false to indicate that this mode is not supported.
     */
    void resetSelection() throws IllegalStateException;

    /**
     * Get the styled document holding source code of the test script. The reason
     * we hold it in a document instance is easy access to document elements.
     * The method should never return null.
     *
     * @return document holding source code of the test script or ready to be
     * used as a container for test code.
     */
    StyledDocument getDocument();

    boolean isExecuting();

    boolean isStop();

    boolean isManualStop();

    /**
     * <p>Request to stop the script execution. The method is not supposed to stop the
     * automated task immediately. It is rather expected to set an internal
     * "stop" flag. The interpret should periodically test the flag, for example
     * between individual command calls, and stop the execution as soon as
     * possible.</p>
     *
     * <p>The method should not fire a stop event through the {@link ScriptListener}
     * interface. It should be rather fired when the execution is really stopped,
     * not when the request is made.</p>
     * @param source request source.
     * @param stop true stops (false doesn't really make sense).
     * @param isManual this is additional flag allowing to specify whether the
     * stop is manual, i.e. requested by the user either through GUI controls or
     * by pressing Ctrl+C in CLI. This information is saved to the context for
     * the benefit of report providers and doesn't affect the way the stop action
     * is carried out.
     * @param manualStopJustification optional string describing why or how the
     * script was stopped. It is used just for reporting purposes.
     */
    void setStop(Object source, boolean stop, boolean isManual, String manualStopJustification);

    boolean isPause();

    void setPause(Object source, boolean pause, String reason);

    /**
     * Clean up method. A call of this method indicates that the interpret will
     * not be used any more. Interprets are expected to perform an
     * internal cleanup on a call of this method and get removed from listener
     * lists and caches.
     */
    void destroy();
}
