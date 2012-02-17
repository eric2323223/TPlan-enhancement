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
import com.tplan.robot.AutomatedRunnable;
import com.tplan.robot.scripting.commands.impl.*;
import com.tplan.robot.imagecomparison.ImageComparisonModule;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.capabilities.*;
import com.tplan.robot.scripting.commands.*;
import com.tplan.robot.scripting.commands.impl.WarningCommand.WarningInfo;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import com.tplan.robot.scripting.report.ReportProvider;
import com.tplan.robot.util.Utils;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * <p>Default Java test script. Users wishing to write
 * Java test scripts are recommended to extend this class and
 * reimplement the {@link #test()} method with their own sequence of automated 
 * testing command calls. See the <a href="../../../../docs/javatestscripts.html">Developing Java Test Scripts</a>
 * document for an introduction and overview of the {@product.name} Java Test Script Framework.</p>
 *
 * <p>Let's have a simple test script which types "Hello world" on the remote desktop.
 * Let's suppose that there's already connection to a desktop and all we have to do
 * is to type the text. The script in the proprietary language would then look as follows:</p>
 *
 * <blockquote>
 * <pre>
 * Type "Hello word"
 * </pre>
 * </blockquote>
 *
 * <p>The same script in Java based on this class:</p>
 *
 * <blockquote>
 * <pre>
 * import com.tplan.robot.scripting.DefaultJavaTestScript;
 * import com.tplan.robot.scripting.JavaTestScript;
 * import java.io.IOException;
 *
 * public class MyTest extends DefaultJavaTestScript {
 *
 *    public void test() {
 *       try {
 *          type("Hello world");
 *       } catch (IOException ex) {
 *          ex.printStackTrace();
 *       }
 *    }
 * }
 * </pre>
 * </blockquote>
 *
 * <p>To <b>execute the script</b> either:</p>
 * <ul>
 * <li>Start the {@product.name} GUI, open the <code>.java</code> file through <i>File->Open Test Script</i>
 * and select the <i>Execute</i> too bar button or menu item (manual execution),</li>
 * <li>Specify the script through the <code>-r/--run</code> CLI switch (automated execution).
 * To execute in CLI mode without GUI specify the <code>-n</code> switch as well.</li>
 * </ul>
 *
 * <p>{@product.name} in fact supports execution of Java test scripts the same way as the
 * proprietary language ones. The tool is able to compile the Java code (<code>.java</code>) into byte
 * code (<code>.class</code>) internally and execute it on the fly provided that you run a Java Development Kit (JDK).
 * If you run {@product.name} through a Java Runtime Environment (JRE), the tool will report an
 * error because JRE is a limited version of Java and does not include the necessary Java Compiler binary <code>javac</code>.</p>
 *
 * <p>A Java test script may be controlled the same way as proprietary test scripts - it may be executed,
 * paused or stopped from the GUI or CLI. The only limitation is that the pause or stop requests
 * get noticed and applied only by the methods provided by this class. Though GUI even provides some
 * limited support of Java test script development, such as simple Java editor and reporting of <code>javac</code>
 * compilation errors, it is recommended to use an IDE such as
 * <a href="http://www.netbeans.org/">NetBeans</a> or <a href="http://www.eclipse.org/">Eclipse</a> to develop Java classes.
 * This tool is not designed to support editing of complex Java projects.</p>
 *
 * <p>Java test scripts may be also executed as standalone processes through their
 * own <code>main()</code> method. As test scripts are executed as threads, one
 * simple Java program can create and start any number of test instances. This allows
 * to run multiple automated testing tasks simultaneously or even to use the
 * tool for load testing through simulation of multiple users accessing the
 * tested service or application.</p>
 *
 * <p>Our "Hello world" example enhanced with the <code>main()</code> method would
 * look as follows:</p>
 *
 * <blockquote>
 * <pre>
 * import com.tplan.robot.*;
 * import com.tplan.robot.scripting.DefaultJavaTestScript;
 * import com.tplan.robot.scripting.JavaTestScript;
 * import java.io.IOException;
 *
 * public class MyTest extends DefaultJavaTestScript {
 *
 *    public void test() {
 *       try {
 *          type("Hello world");
 *       } catch (IOException ex) {
 *          ex.printStackTrace();
 *       }
 *    }
 *
 *    public static void main(String args[]) {
 *       MyTest test = new MyTest();
 *       ApplicationSupport robot = new ApplicationSupport();
 *       AutomatedRunnable t = robot.createAutomatedRunnable(test, "javatest", args, System.out, false);
 *       new Thread(t).start();
 *    }
 * }
 * </pre>
 * </blockquote>
 *
 * <p>The point is to let the {@link ApplicationSupport} class
 * to create a test runnable ({@link AutomatedRunnable})
 * from an instance of our Java test class. As the resulting object implements
 * the {@link Runnable} interface, it is possible to execute it through a
 * {@link Thread}. Another important feature to notice is that the
 * {@link ApplicationSupport#createAutomatedRunnable(com.tplan.robot.scripting.JavaTestScript, java.lang.String, java.lang.String[], java.io.PrintStream, boolean) createAutomatedRunnable()}
 * method accepts a String array of input arguments. This makes it possible
 * to customize any single test script runnable with any command line options
 * specified in the {@doc.cli} document.</p>
 *
 * <p>If you for some reason can not extend this class but you need to access
 * its methods, you may create an instance and call its methods instead. Your class
 * however must implement at least the {@link JavaTestScript} interface which is the bare
 * minimum for a test script to be able to execute through the {@product.name} framework. 
 * You must also make sure that the {@link JavaTestScript#setContext(com.tplan.robot.scripting.ScriptingContext)} and
 * {@link JavaTestScript#setInterpret(com.tplan.robot.scripting.interpret.TestScriptInterpret)} methods
 * get called correctly on this class instance. An example follows:</p>
 *
 * <blockquote>
 * <pre>
 * import com.tplan.robot.scripting.*;
 * import com.tplan.robot.scripting.interpret.TestScriptInterpret;
 * import java.io.IOException;
 *
 * public class MyCustomTest implements JavaTestScript {
 *    ScriptingContext context;
 *    TestScriptInterpret interpret;
 *
 *    public void setContext(ScriptingContext context) {
 *       this.context = context;
 *    }
 *
 *    public void setInterpret(TestScriptInterpret interpret) {
 *       this.interpret = interpret;
 *    }
 *
 *    public void test() {
 *       try {
 *          DefaultJavaTestScript defaultScript = new DefaultJavaTestScript();
 *          defaultScript.setContext(getContext());
 *          defaultScript.type("Hello world");
 *       } catch (IOException ex) {
 *          ex.printStackTrace();
 *       }
 *    }
 * }
 * </pre>
 * </blockquote>
 *
 * <p>This way may be also efficiently used to create libraries of test tasks or
 * routines similar to the functionality provided by the <code>Run</code> and
 * <code>Include</code> commands.</p>
 * 
 * <p>As is already indicated by the examples above, this class contains methods
 * which provide almost the same functionality as elements
 * of the {@product.name} proprietary language defined by the {@doc.spec}.
 * The following table summarizes compatibility of this Java class with the
 * specification and the existing limitations. Use this table as a reference of how
 * to implement the same functionality both in the scripting language and Java as well as
 * which limitations apply to conversion of proprietary test scripts into Java.<p>
 *
 * <p>
 * <table style="text-align: left; width: 100%;" border="1" cellpadding="2" cellspacing="2">
 * <tbody>
 *   <tr>
 *     <td style="vertical-align: top; color: rgb(255, 255, 255); background-color: rgb(0, 0, 153);">
 *       <b>Spec Command/Element</b>
 *     </td>
 *     <td style="vertical-align: top; color: rgb(255, 255, 255); background-color: rgb(0, 0, 153);">
 *       <b>Supported By Java</b>
 *     </td>
 *     <td style="vertical-align: top; color: rgb(255, 255, 255); background-color: rgb(0, 0, 153);">
 *       <b>Comments</b>
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd lang_structure Code comments}
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Java supports code comments similar to the scripting language ones.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd lang_structure Labels}
 *     </td>
 *     <td style="vertical-align: middle;">
 *       No
 *     </td>
 *     <td style="vertical-align: top;">
 *       Labels define places in the script code to jump to. As Java doesn't support a "goto" command, labels are not supported. A workaround is to use Java language elements to implement conditional execution blocks, such as methods, <code>if/else</code> and <code>switch</code> statements.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd timevalues Time values}
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Time values are supported as arguments of methods corresponding to the scripting commands. Examples are "1s" (one second) or 5m (5 minutes
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd procedures Procedures}
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Procedures correspond with Java void methods.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd numeric Numeric expressions}
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Numeric expressions specified by the proprietary language are fully supported in Java.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd boolean Boolean expressions}
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Boolean expressions specified by the proprietary language are fully supported in Java.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd if If/else statement}
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       If/else statement specified by the proprietary language is fully supported in Java.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd for For statement}
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       For statement specified by the proprietary language is supported in Java, just the <code>for</code> statement iterating over a set of enumerated values must be
 * implemented indirectly with the help of a String array or Java Collections.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd locvars Local variables}
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Java supports the same concept of local variables as the proprietary language. Global variables may be implemented in Java either as class member variables or stored to the variable list in the context through the {@link ScriptingContext#setVariable(String, Object)} method.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd retval Return values}
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Java methods return exit values of the scripting language commands.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd connect Connect} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the set of {@link #connect(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean) connect()} methods.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd disconnect Disconnect} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the {@link #disconnect()} method.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd press Press} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the set of {@link #press(java.lang.String, int, int, java.lang.String) press()} methods.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd type Type and Typeline} commands
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the set of {@link #type(java.lang.String, int, java.lang.String) type()} and {@link #typeLine(java.lang.String, int, java.lang.String) typeLine()} methods.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd mouse Mouse} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the set of
 *       {@link #mouseEvent(java.lang.String, int, int, java.awt.Point, java.awt.Point, int, java.lang.String) mouseEvent()},
 *       {@link #mouseMove(java.awt.Point, java.lang.String) mouseMove()},
 *       {@link #mouseClick(java.awt.Point, int, int, int, java.lang.String) mouseClick()},
 *       {@link #mouseRightClick(java.awt.Point, int, int, java.lang.String) mouseRightClick()},
 *       {@link #mouseDoubleClick(java.awt.Point, java.lang.String) mouseDoubleClick()},
 *       {@link #mouseDrag(java.awt.Point, java.awt.Point, int, int, java.lang.String) mouseDrag()},
 *       {@link #mouseRightDrag(java.awt.Point, java.awt.Point, int, java.lang.String) mouseRightDrag()},
 *       {@link #mouseWheelUp(java.awt.Point, int, int, java.lang.String) mouseWheelUp()} and
 *       {@link #mouseWheelDown(java.awt.Point, int, int, java.lang.String) mouseWheelDown()} methods.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd var} and {@doc.cmd eval} commands
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes (indirectly)
 *     </td>
 *     <td style="vertical-align: top;">
 *       There is no direct method corresponding to the <code>Var/Eval</code> commands.
 * Script variables populated by the commands or the automated testing framework (so called predefined or
 * {@doc.cmd implicit_vars implicit variables}) can be accessed through the context methods
 * {@link ScriptingContext#getVariable(String)} and {@link ScriptingContext#setVariable(String, Object)}.
 * Other user variables may be implemented either as Java local or member variables or through the context as well.
 * <br/><br/>
 * Dynamically populated variables are present in the context but they will return
 * just dummy values populated at the time of context creation. To get the
 * desired variable value use appropriate Java code as follows:
 * <ul>
 * <li>To get the current time in miliseconds (variable <code>_CURTIME</code>)
 * use <code>System.currentTimeMillis()</code>.</li>
 *
 * <li>To get the formatted current time and date (variable <code>_CURDATE</code>)
 * use <code>new java.util.Date().toString()</code>.
 * If you want to apply the format specified in the user preferences, load it
 * through <code>String format = UserConfiguration.getInstance().getString("scripting.curdateFormat")</code>
 * and format the Date object through <code>new java.text.SimpleDateFormat(format).format(new Date())</code>.</li>
 *
 * <li>To get a random number (variable <code>_RANDOM</code>) use java.util.Random.</li>
 * <li>To get RGB of a pixel on the desktop use <code>((BufferedImage) getContext().getClient().getImage()).getRGB(x,y)</code>.
 * This will yield the RGB value as integer which may be used in constructor of <code>java.awt.Color</code>.
 * Should you want to convert the color to the HTML-style format, call <code>getContext().getParser().colorToString(Color)</code>.</li>
 * </ul>
 * <br/>
 * Variables passed through the <code>-v/--variable</code> CLI option may be accessed through the <code>getContext().getCommandLineVariables()</code>
 * method. The overriding mechanism known from proprietary test script is not applied in case of Java test scripts
 * which are free to decide whether to use or ignore the CLI value.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd run} and {@doc.cmd include} commands
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes (indirectly)
 *     </td>
 *     <td style="vertical-align: top;">
 *       There is no direct method corresponding to the <code>Run/Include</code> commands.
 *       It is recommended to use capabilities of the Java language to create libraries with test script routines.
 *       The <code>Include</code> command corresponds to instantiation of another Java class and accessing its methods and member variables.
 *       The <code>Run</code> command is equivalent to calling of <code>test()</code> method of another Java test script instance.
 *     </td>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd pause} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the set of {@link #pause(java.lang.String) pause()} methods.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd exit} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes (indirectly)
 *     </td>
 *     <td style="vertical-align: top;">
 *       To exit a script execution (<code>scope=process</code>) use the {@link #exit(int)} method. Other exit scopes such as <code>procedure</code>, <code>block</code> and <code>file</code> must be implemented indirectly using Java language capabilities.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd wait} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the {@link #wait(java.lang.String)} method.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd waitfor WaitFor} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the {@link #waitForBell(int, java.lang.String, java.lang.String) waitForBell()},
 *       {@link #waitForUpdate(java.awt.Rectangle, java.lang.String, boolean, java.lang.String, java.lang.String) waitForUpdate()},
 *       {@link #waitForMatch(java.io.File[], float, java.lang.String, java.lang.String, java.lang.String, java.awt.Rectangle, java.lang.String, java.lang.String) waitForMatch()},
 *       {@link #waitForMismatch(java.io.File[], float, java.lang.String, java.lang.String, java.lang.String, java.awt.Rectangle, java.lang.String, java.lang.String) waitForMismatch()} and
 *       {@link #waitForClipboard(java.lang.String, java.lang.String) waitForClipboard()} methods.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd compareto CompareTo} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the {@link #compareTo(java.io.File[], java.lang.String, java.lang.String, float, java.awt.Rectangle) compareTo()} methods.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd exec} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the {@link #exec(java.lang.String, java.io.OutputStream, int, java.lang.String) exec()} methods.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd screenshot} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the {@link #screenshot(java.io.File, java.lang.String, java.awt.Rectangle, java.io.File[], java.lang.String, java.lang.String, float, java.awt.Rectangle) screenshot()} methods.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd report} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes (partially)
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the {@link #report(java.io.File, java.lang.String, java.lang.String, java.lang.String) report()} methods.
 *       There's however one limitation. Though the <code>scope</code> parameter is among the method arguments, it is present just
 *       for the sake of backward compatibility and should be considered as obsolete. Its value will be ignored and the method will
 *       always execute with the scope fixed to <code>all</code> (include all outputs created by the test scripts in the report).
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd warning} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the {@link #warning(java.lang.String, java.io.File) warning()} methods.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: middle;">
 *       {@doc.cmd sendmail} command
 *     </td>
 *     <td style="vertical-align: middle;">
 *       Yes
 *     </td>
 *     <td style="vertical-align: top;">
 *       Provided through the {@link #sendMail(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.io.File[], boolean) sendMail()} methods.
 *     </td>
 *   </tr>
 *   
 * </tbody>
 * </table>
 * </p>
 *
 * <p>Methods representing commands of the proprietary scripting language share
 * the following design principles:</p>
 *
 * <ul>
 * <li>Methods have generally the same names as their corresponding commands. There
 * are a few reasonable exceptions, for example the {@doc.cmd mouse} command is for better
 * clarity represented by methods called <code>mouseMove</code>, <code>mouseClick</code>,
 * <code>mouseDrag</code> etc. Such methods usually represent a combination of a command
 * and one or more its parameters (for example, the <code>mouseDoubleClick</code> method
 * represent a "Mouse click button=left count=2" command).</li>
 * <li>To omit a method argument set its value to null (for objects) or to a
 * negative number (for int, long, float or double arguments). This makes the
 * command behave as if the parameter was not specified and rely on the default
 * value. For example, methods performing image search such as <code>compareTo()</code>
 * automatically look for 100% match if the <code>passRate</code> argument is set
 * to <code>-1f</code>.</li>
 * <li>Each method returns an integer value equal to the command return value
 * described in the specification. A value of 0 (zero) usually indicates success
 * while values other than zero indicate either an error or a negative test result.</li>
 * <li>An {@link IOException} is thrown only on when an unrecognized or unexpected 
 * error happens in the communication between the desktop client and server. Don't be
 * surprised that some methods, for example {@link #connect(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean) connect()},
 * rather return an error code than throw an IOException in standard situations, such as
 * failure to connect to the server because it is down, authentication failure etc.</li>
 * <li>All methods may throw an {@link IllegalArgumentException} when one or more argument
 * values are invalid and/or if the corresponding command requires connection to a desktop
 * and the tool is in disconnected state. As this execption is unchecked,
 * it is not explicitly declared as thrown by the method.</li>
 * </ul>
 *
 * <p>Most methods of this class are annotated. The goal is to provide information
 * allowing to map these methods and their arguments onto commands and parameters
 * of the proprietary scripting language. This data is retrieved through Java Reflection API by
 * the {@link DefaultJavaTestScriptConverter} which provides conversion of proprietary test scripts
 * into Java classes. Should you want to extend
 * both the proprietary language and the Java test script class with a new
 * command/method, you have to annotate the method in a similar way to make the
 * command calls convertible to Java code.
 * </p>
 * @product.signature
 */
public class DefaultJavaTestScript extends AbstractJavaTestScript {

    /**
     * Empty test method implementing the {@link JavaTestScript} interface.
     * Reimplement this method with your own test code to create a Java test
     * script.
     */
    public void test() {
        // Empty method
    }

    public TestWrapper getRootWrapper() {
        return this;
    }

    // Annotations here are used to identify mappings among methods and
    // their corresponding scripting language commands.
    // The retention annotation below causes that the annotations make it into
    // the byte code and can be read at runtime through the Java Reflection API.
    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface Param {

        // Name identifies name of the command parameter as specified in the scripting language spec
        String name();

        // Default value is used when the parameter is optional and no value is provided.
        // Empty string is considered as undefined default value.
        // If the value is defined, an attempt will be made to convert it to the
        // desired parameter type. For example, for a parameter of "float" type
        // the value will be parsed through Float.parseFloat(String).
        String defaultValue() default "";

        // Template may contain snippets of Java code. It allows to specify how to
        // convert a value passed from the scripting language into desired Java type.
        // Any occurence of @value@ will be replaced with the parameter value.
        // A good example are mouse buttons: the scripting language recognizes "left",
        // "middle" and "right" but the corresponding Java method accepts only an
        // integer button identifier specified in the MouseEvent class. See the
        // mouseEvent() method for the code snippet example.
        String template() default "";
    };

    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface Command {
        // Name identifies which command the method covers.

        String name();
    };

    // Code templates (snippets). They are processed in JavaTestScriptMethodMapper.map(List, Map, ScriptingContext, Method)
    private final String DEFAULT_MOUSE_COORDS = "";
    private final String MODIFIER_CONVERSION_SNIPPET = "@modifiers@";//"getContext().getParser().parseModifiers(\"@value@\")";
    private final String MOUSE_BUTTON_CONVERSION_SNIPPET = "@mouseButton@"; //"getContext().getParser().parseMouseButton(\"@value@\")";
    private final String FILE_LIST_SNIPPET = "@fileList@";
    private final String OUTPUT_FILE_STREAM_SNIPPET = "new java.io.FileOutputStream(\"@value@\")";
    private final String KEY_LOCATION_CONVERSION_SNIPPET = "@keyLocation@";

    public DefaultJavaTestScript() {
        // TODO: initialize loggers

        // TODO: load CLI parameters

        // TODO: load variable overrides

        // TODO: allow to pack Java tests together with product classes

        // TODO: allow to call test scripts in the old language from the Java class

        // TODO: find out whether we can validate scripts (through command generation)
    }

    // TODO: remove the console mode from RFB client
    // TODO: error code for internal error to all command handlers
    // TODO: fix the thrown exceptions
    private int connect(String host, String user, String password, String params,
            String paramSeparator, Map<String, Object> paramMap, boolean force) throws IOException {
        List l = new ArrayList();
        Map m = new HashMap();

        if (host != null) {
            l.add(host);
        }

        if (user != null) {
            l.add(ConnectCommand.PARAM_USER);
            m.put(ConnectCommand.PARAM_USER, password);
        }

        if (params != null) {
            l.add(ConnectCommand.PARAM_PARAMS);
            m.put(ConnectCommand.PARAM_PARAMS, params);
        }

        if (paramMap != null) {
            l.add(ConnectCommand.PARAM_PARAMS);
            m.put(ConnectCommand.PARAM_PARAMS, paramMap);
        }

        if (paramSeparator != null) {
            l.add(ConnectCommand.PARAM_PARAM_SEPARATOR);
            m.put(ConnectCommand.PARAM_PARAM_SEPARATOR, paramSeparator);
        }

        if (password != null) {
            l.add(ConnectCommand.PARAM_PASSWORD);
            m.put(ConnectCommand.PARAM_PASSWORD, password);
        }

        l.add(ConnectCommand.PARAM_FORCE);
        m.put(ConnectCommand.PARAM_FORCE, force ? "true" : "false");
        return runScriptCommand("connect", l, m);
    }

    /**
     * <p>Connect to a desktop. This method has the same functionality as the
     * {@link #connect(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)} one
     * save that it allows to specify custom parameters through a map of objects.
     * This is useful for programmatic connections to a desktop whose client
     * requires a custom parameter which has other than String value.</p>
     *
     * @param host host connection URI. It can not be null. See the method
     * description for information about its format.
     * @param user user name for plain text password authentication. It may be null
     * for servers which do not require this authentication scheme.
     * @param password password for plain text password authentication. It may be null
     * for servers which do not require this authentication scheme.
     * @param params custom client parameters in form of a map with [param_name, param_value] pairs.
     * Note that this is a generic mechanism to allow future plugins of clients
     * which use custom ways of authentication and login parameters. The two
     * built in RFB and Java clients do not support at the moment any custom parameters.
     * @param force true will force reconnection even if the tool is already
     * connected to the same server. A value of false will cause the method
     * to check whether a connection to the specified server already exists
     * and connect only in case it is not so. Default value is false.
     * @return command exit code as is specified in the {@doc.cmd connect} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int connect(String host, String user, String password,
            Map<String, Object> params,
            boolean force) throws IOException {
        return connect(host, user, password, null, null, params, force);
    }

    /**
     * <p>Connect to a desktop. The method provides access to functionality of the
     * {@doc.cmd connect} scripting language command. The <code>host</code> argument
     * must be a valid URL in form of <code>&lt;protocol&gt;://&lt;host_or_IP&gt;[:&lt;port&gt;]</code>
     * where the protocol must be equal to one of the installed client plugin codes.
     * If port is not explicitly specified, it defaults to the default protocol-specific port
     * provided by the {@link RemoteDesktopClient#getDefaultPort()} method.
     * For more information on host string format see documentation of particular
     * desktop clients.</p>
     *
     * <p>If protocol is omitted in the URL, the host defaults to the RFB (VNC) protocol to provide
     * backward compatibility with VNCRobot 1.x. Port number is in this case considered
     * to be a display number rather than real port. To get a real port take
     * the display number and add it to 5900 which is the default RFB port. Direct
     * port can be in this mode specified through double colon, for example both "localhost:1"
     * and "localhost::5901" refer to the same local VNC server running on port 5901.
     * To specify the same address in the standard URL form one has to list the
     * real port specifically and the equivalent URL is "rfb://localhost:5901".</p>
     *
     * <p>The connect method supports by default just servers with either none
     * or plain user/password authentication. Both user and password parameters
     * may be also passed through CLI or programatically among parameters
     * of the {@link ApplicationSupport#createAutomatedRunnable(com.tplan.robot.scripting.JavaTestScript, java.lang.String, java.lang.String[], java.io.PrintStream, boolean)}
     * method. Clients which use custom authentication schemes must use a different
     * ways of passing custom connection parameters to the script, such as script
     * variables (specified from CLI using <code>-v/--variable</code> and available
     * during script execution through {@link ScriptingContext#getVariable(java.lang.String) getContext().getVariable()}).
     * An alternative way is to use the framework of {@link UserConfiguration user preferences}
     * together with the <code>-o/--option</code> CLI switch.</p>
     *
     * <p>Note that the method doesn't throw any exception on standard situations,
     * such as failures to connect or authenticate. These cases are reported
     * through the return value which should be checked by the script. If a value
     * other than zero is returned to report failure, the connect exception may
     * be obtained through the {@link ScriptingContext#getConnectError()} method.
     * The IOException declared by this method is thrown only on unrecognized I/O
     * errors which may happen during the initialisation of the connection, for
     * example an intermittent network failure.</p>
     *
     * <p><i><u>Examples:</u></i>
     * <ul>
     * <li><code>connect("localhost", null, null, false)</code> will attempt to connect
     * to the RFB (VNC) server running on the default port 5900 of the local system.
     * As no password is provided, the server is expected to require no authentication.<br>
     * </li>
     *
     * <li><code>connect("rfb://localhost:5901", null, "welcome", false)</code> will attempt to connect
     * to the RFB (VNC) server running on the port 5901 of the local system. This is a typical setup
     * on Linux/Unix where the default port of 5900 is occupied by the system X-server.
     * The host string can be in this case also specified as "localhost:1" or "localhost::5901".
     * As a password is provided, the client will use it to authenticate if the server requests it. User
     * name may be null because RFB requires justa password.<br>
     * </li>
     *
     * <li><code>connect("java://192.168.1.1:1099", null, null, false)</code> will connect
     * to the display of the host with IP address 192.168.1.1 through the Java
     * native client using RMI. The host machine must have Java installed and it
     * must be executing {@product.name} in the Java server mode. See the Java
     * client documentation for more.<br>
     * </li>
     *
     * <li><code>connect("java://localhost", null, null, false)</code> will connect
     * to the local system display (meaning the very same desktop you see on your
     * screen or other display device) through the Java native client.<br>
     * </li>
     *
     * <li><code>connect("rdp://192.168.1.1:1234", "Administrator", "welcome", true)</code> will connect
     * to an RDP server running on port 1234 of the system with IP address 192.168.1.1.
     * If {@product.name} is already connected to the given system, the connection is
     * terminated and the application reconnects because the <code>force</code>
     * is set to true. The server is expected to require a user name and plain password
     * authentication. Note that this example is just illustrative because RDP is not
     * yet directly supoported..<br>
     * </li>
     * </ul>
     *
     * @param host host connection URI. It can not be null. See the method
     * description for information about its format.
     * @param user user name for plain text password authentication. It may be null
     * for servers which do not require this authentication scheme.
     * @param password password for plain text password authentication. It may be null
     * for servers which do not require this authentication scheme.
     * @param params custom client parameters. Through the client framework supports
     * by default just URI (protocol+host+port), user name and password parameters,
     * this parameter allows to pass any custom parameter to a custom client.
     * The list may contain any number of parameter name and value pairs separated
     * by comma (',') or a custom separator specified by the <code>paramSeparator</code>
     * argument. For example, to specify two parameters <code>PARAM_A=value_A</code> and
     * <code>PARAM_B=valueB</code> the argument should look like "PARAM_A,valueA,PARAM_B,valueB".
     * Note that this is a generic mechanism to allow future plugins of clients
     * which use custom ways of authentication and login parameters. The two
     * built in RFB and Java clients do not support at the moment any custom parameters.
     * @param paramSeparator optional separator for the list of parameter names and values
     * specified by the <code>params</code> argument. If it is null, it defaults to comma (",").
     * @param force true will force reconnection even if the tool is already
     * connected to the same server. A value of false will cause the method
     * to check whether a connection to the specified server already exists
     * and connect only in case it is not so. Default value is false.
     * @return command exit code as is specified in the {@doc.cmd connect} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "connect")
    public int connect(
            @Param(name = "argument") String host,
            @Param(name = "user") String user,
            @Param(name = "password") String password,
            @Param(name = "params") String params,
            @Param(name = "paramseparator") String paramSeparator,
            @Param(name = "force", defaultValue = "false") boolean force) throws IOException {
        return connect(host, user, password, params, paramSeparator, null, force);
    }

    /**
     * <p>Connect to a server using the specified user and password. As this is
     * just a convenience method calling
     * the {@link #connect(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)}  one,
     * see its documentation for a complete description of the method behavior.</p>
     *
     * @param host host connection URI. It can not be null. See the
     * {@link #connect(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)}
     * method description for information about its format.
     * @param user user name for plain text password authentication. It may be null
     * for servers which do not require this authentication scheme.
     * @param password password for plain text password authentication. It may be null
     * for servers which do not require this authentication scheme.
     * @return command exit code as is specified in the {@doc.cmd connect} specification.
     * @param force true will force reconnection even if the tool is already
     * connected to the same server. A value of false will cause the method
     * to check whether a connection to the specified server already exists
     * and connect only in case it is not so. Default value is false.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "connect")
    public int connect(
            @Param(name = "argument") String host,
            @Param(name = "user") String user,
            @Param(name = "password") String password,
            @Param(name = "force", defaultValue = "false") boolean force) throws IOException {
        return connect(host, user, password, null, null, force);
    }

    /**
     * <p>Connect to a server which may or may not require a password. This
     * is typical for RFB (VNC) servers which support none or just password (without
     * user name) authentication schemes. If the connection already exists,
     * do not reconnect (<code>force=false</code>).</p>
     *
     * <p>As this is just a convenience method calling
     * the {@link #connect(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)}  one,
     * see its documentation for a complete description of the method behavior.</p>
     *
     * @param host host connection URI. It can not be null. See the 
     * {@link #connect(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)}
     * method description for information about its format.
     * @param password password for plain text password authentication. It may be null
     * for servers which do not require this authentication scheme.
     * @return command exit code as is specified in the {@doc.cmd connect} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "connect")
    public int connect(
            @Param(name = "argument") String host,
            @Param(name = "password") String password) throws IOException {
        return connect(host, null, password, false);
    }

    /**
     * <p>Connect to a server which requires no authentication (for example a Java
     * client or an RFB/VNC server which is configured this way).</p>
     *
     * <p>As this is just a convenience method calling
     * the {@link #connect(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)}  one,
     * see its documentation for a complete description of the method behavior.</p>
     *
     * @param host host connection URI. It can not be null. See the
     * {@link #connect(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)}
     * method description for information about its format.
     * @param force true will force reconnection even if the tool is already
     * connected to the same server. A value of false will cause the method
     * to check whether a connection to the specified server already exists
     * and connect only in case it is not so. Default value is false.
     * @return command exit code as is specified in the {@doc.cmd connect} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "connect")
    public int connect(@Param(name = "argument") String host, @Param(name = "force", defaultValue = "false") boolean force) throws IOException {
        return connect(host, null, null, force);
    }

    /**
     * <p>Connect to a server which requires no authentication (for example a Java
     * client or an RFB/VNC server which is configured this way). If the connection already exists,
     * do not reconnect (<code>force=false</code>).</p>
     *
     * <p>As this is just a convenience method calling
     * the {@link #connect(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)}  one,
     * see its documentation for a complete description of the method behavior.</p>
     *
     * @param host host connection URI. It can not be null. See the
     * {@link #connect(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)}
     * method description for information about its format.
     * @return command exit code as is specified in the {@doc.cmd connect} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "connect")
    public int connect(@Param(name = "argument") String host) throws IOException {
        return connect(host, null, null, false);
    }

    /**
     * <p>Disconnect from a desktop. The method provides access to functionality of the
     * {@doc.cmd disconnect} scripting language command.
     * If there's a connection to a desktop, it gets
     * closed through {@link RemoteDesktopClient#close()}.
     * If the tool is not connected to any desktop the method does nothing.</p>
     *
     * @return command exit code as is specified in the {@doc.cmd disconnect} specification.
     * @throws java.io.IOException if an I/O error happens during the connection shutdown.
     */
    @Command(name = "disconnect")
    public int disconnect() throws IOException {
        return runScriptCommand("disconnect", null, null);
    }

    /**
     * <p>Perform a generic mouse action on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param action mouse action string as is defined in the {@doc.cmd mouse}
     * command specification. Supported values are {@code move}, {@code click},
     * {@code drag}, {@code press}, {@code release}, {@code wheelup} and {@code wheeldown}.
     * The action codes are not case sensitive.
     *
     * @param modifiers modifier mask as specified in the {@link java.awt.event.InputEvent}.
     * For example, to specify Ctrl+Alt use {@code InputEvent.CTRL_MASK | InputEvent.ALT_MASK}
     * @param button mouse button ID as specified in {@link java.awt.event.MouseEvent}.
     * Supported values are {@code MouseEvent.BUTTON1} (left button),
     * {@code MouseEvent.BUTTON2} (middle button) and {@code MouseEvent.BUTTON3} (right button).
     * If any other value is used, the argument defaults to the left button. This argument
     * applies only to mouse events which involve buttons, such as mouse click, press,
     * release and drag.
     * @param from start point of a mouse move or drag event. If the argument is null,
     * it defaults to the current desktop mouse pointer position. The value is
     * ignored by other actions.
     * @param to location or target point of the mouse event. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param count how many times to repeat the event. It applies only to mouse
     * clicks and wheel events and it is ignored by other ones. Default value is 1 (one).
     * Delays among multiple clicks are defined by a value in the Mouse command user preferences.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server 
     * communication or if no desktop is connected.
     */
    @Command(name = "mouse")
    public int mouseEvent(
            @Param(name = "argument") String action,
            @Param(name = "modifiers", template = MODIFIER_CONVERSION_SNIPPET, defaultValue = "0") int modifiers,
            @Param(name = "button", template = MOUSE_BUTTON_CONVERSION_SNIPPET) int button,
            @Param(name = "from") Point from,
            @Param(name = "to") Point to,
            @Param(name = "count", defaultValue = "1") int count,
            @Param(name = "wait") String wait) throws IOException {
        List l = new ArrayList();
        Map m = new HashMap();
        TokenParser parser = getContext().getParser();

        String s = parser.modifiersToString(modifiers);
        if (s.length() > 0) {
            l.add(s + "+" + action);
        } else {
            l.add(action);
        }

        if (button > 0) {
            l.add("button");
            m.put("button", button);
        }
        if (to != null) {
            l.add("to");
            m.put("to", to);
        }
        if (from != null) {
            l.add("from");
            m.put("from", from);
        }
        if (count > 0) {
            l.add(AbstractCommandHandler.PARAM_COUNT);
            m.put(AbstractCommandHandler.PARAM_COUNT, Integer.toString(count));
        }
        if (wait != null) {
            l.add(AbstractCommandHandler.PARAM_WAIT);
            m.put(AbstractCommandHandler.PARAM_WAIT, wait);
        }
        return runScriptCommand("mouse", l, m);
    }

    /**
     * <p>Perform a mouse move on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse move} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param to location or target point of the mouse event. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second),
     * "1m" (a minute), "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse move")
    public int mouseMove(@Param(name = "to") Point to, @Param(name = "wait") String wait) throws IOException {
        return mouseEvent("move", 0, MouseEvent.NOBUTTON, null, to, 0, wait);
    }

    /**
     * <p>Perform a mouse move on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse move} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param to location or target point of the mouse event. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse move")
    public int mouseMove(@Param(name = "to") Point to) throws IOException {
        return mouseEvent("move", 0, MouseEvent.NOBUTTON, null, to, 0, null);
    }

    /**
     * <p>Perform a mouse move on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse move} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param x X-coordinate of location or target point of the mouse event. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of location or target point of the mouse event. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseMove(int x, int y, String wait) throws IOException {
        return mouseEvent("move", 0, MouseEvent.NOBUTTON, null, new Point(x, y), 0, wait);
    }

    /**
     * <p>Perform a mouse move on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse move} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param x X-coordinate of location or target point of the mouse event. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of location or target point of the mouse event. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseMove(int x, int y) throws IOException {
        return mouseEvent("move", 0, MouseEvent.NOBUTTON, null, new Point(x, y), 0, null);
    }

    /**
     * <p>Perform left mouse click(s) on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>

     * @param to mouse click point. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param modifiers modifier mask as specified in the {@link java.awt.event.InputEvent}.
     * For example, to specify Ctrl+Alt use {@code InputEvent.CTRL_MASK | InputEvent.ALT_MASK}
     * @param button mouse button ID as specified in {@link java.awt.event.MouseEvent}.
     * Supported values are {@code MouseEvent.BUTTON1} (left button),
     * {@code MouseEvent.BUTTON2} (middle button) and {@code MouseEvent.BUTTON3} (right button).
     * If any other value is used, the argument defaults to the left button. This argument
     * applies only to mouse events which involve buttons, such as mouse click, press,
     * release and drag.
     * @param count how many times to click. Default value is 1 (one click).
     * Delays among multiple clicks are defined by a value in the Mouse command user preferences.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse click")
    public int mouseClick(
            @Param(name = "to") Point to,
            @Param(name = "modifiers", template = MODIFIER_CONVERSION_SNIPPET, defaultValue = "0") int modifiers,
            @Param(name = "btn", template = MOUSE_BUTTON_CONVERSION_SNIPPET) int button,
            @Param(name = "count") int count,
            @Param(name = "wait") String wait) throws IOException {
        return mouseEvent("click", modifiers, button, null, to, count, wait);
    }

    /**
     * <p>Perform left mouse click(s) on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param to location or target point of the mouse event. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param count how many times to click. Default value is 1 (one click).
     * Delays among multiple clicks are defined by a value in the Mouse command user preferences.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse click")
    public int mouseClick(@Param(name = "to") Point to, @Param(name = "count") int count) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON1, null, to, count, null);
    }

    /**
     * <p>Perform a single left mouse click on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param to location or target point of the mouse event. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse click")
    public int mouseClick(@Param(name = "to") Point to, @Param(name = "wait") String wait) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON1, null, to, -1, wait);
    }

    /**
     * <p>Perform a single left mouse click on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param to location or target point of the mouse event. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse click")
    public int mouseClick(@Param(name = "to") Point to) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON1, null, to, -1, null);
    }

    /**
     * <p>Perform left mouse click(s) on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param x X-coordinate of location or target point of the mouse event. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of location or target point of the mouse event. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @param count how many times to click. Default value is 1 (one click).
     * Delays among multiple clicks are defined by a value in the Mouse command user preferences.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseClick(int x, int y, int count, String wait) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON1, null, new Point(x, y), count, wait);
    }

    /**
     * <p>Perform left mouse click(s) on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param x X-coordinate of location or target point of the mouse event. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of location or target point of the mouse event. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @param count how many times to click. Default value is 1 (one click).
     * Delays among multiple clicks are defined by a value in the Mouse command user preferences.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseClick(int x, int y, int count) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON1, null, new Point(x, y), count, null);
    }

    /**
     * <p>Perform a single left mouse click on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param x X-coordinate of location or target point of the mouse event. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of location or target point of the mouse event. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseClick(int x, int y) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON1, null, new Point(x, y), 1, null);
    }

    /**
     * <p>Perform right mouse click(s) on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click btn=right} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param to mouse click point. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param modifiers modifier mask as specified in the {@link java.awt.event.InputEvent}.
     * For example, to specify Ctrl+Alt use {@code InputEvent.CTRL_MASK | InputEvent.ALT_MASK}
     * @param count how many times to click. Default value is 1 (one click).
     * Delays among multiple clicks are defined by a value in the Mouse command user preferences.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse click btn=right")
    public int mouseRightClick(
            @Param(name = "to") Point to,
            @Param(name = "modifiers", template = MODIFIER_CONVERSION_SNIPPET, defaultValue = "0") int modifiers,
            @Param(name = "count") int count,
            @Param(name = "wait") String wait) throws IOException {
        return mouseEvent("click", modifiers, MouseEvent.BUTTON3, null, to, count, wait);
    }

    /**
     * <p>Perform right mouse click(s) on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click btn=right} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param to mouse click point. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param count how many times to click. Default value is 1 (one click).
     * Delays among multiple clicks are defined by a value in the Mouse command user preferences.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse click btn=right")
    public int mouseRightClick(@Param(name = "to", defaultValue = DEFAULT_MOUSE_COORDS) Point to, @Param(name = "count") int count, @Param(name = "wait") String wait) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON3, null, to, count, wait);
    }

    /**
     * <p>Perform right mouse click(s) on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click btn=right} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param to mouse click point. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param count how many times to click. Default value is 1 (one click).
     * Delays among multiple clicks are defined by a value in the Mouse command user preferences.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse click btn=right")
    public int mouseRightClick(@Param(name = "to", defaultValue = DEFAULT_MOUSE_COORDS) Point to, @Param(name = "count") int count) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON3, null, to, count, null);
    }

    /**
     * <p>Perform a single right mouse click on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click btn=right} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param to mouse click point. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse click btn=right")
    public int mouseRightClick(@Param(name = "to", defaultValue = DEFAULT_MOUSE_COORDS) Point to) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON3, null, to, 1, null);
    }

    /**
     * <p>Perform right mouse click(s) on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click btn=right} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param x X-coordinate of the click point. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of the click point. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @param count how many times to click. Default value is 1 (one click).
     * Delays among multiple clicks are defined by a value in the Mouse command user preferences.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseRightClick(int x, int y, int count, String wait) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON3, null, new Point(x, y), count, wait);
    }

    /**
     * <p>Perform right mouse click(s) on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click btn=right} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param x X-coordinate of the click point. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of the click point. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @param count how many times to click. Default value is 1 (one click).
     * Delays among multiple clicks are defined by a value in the Mouse command user preferences.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseRightClick(int x, int y, int count) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON3, null, new Point(x, y), count, null);
    }

    /**
     * <p>Perform a single right mouse click on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click btn=right} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param x X-coordinate of the click point. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of the click point. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseRightClick(int x, int y) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON3, null, new Point(x, y), 1, null);
    }

    /**
     * Perform a double left mouse click on the current desktop client. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left count=2} scripting language command.
     * <p>Perform a double left mouse click on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left count=2} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param to mouse click point. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse click count=2")
    public int mouseDoubleClick(@Param(name = "to", defaultValue = DEFAULT_MOUSE_COORDS) Point to, @Param(name = "wait") String wait) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON1, null, to, 2, wait);
    }

    /**
     * Perform a double left mouse click on the current desktop client. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left count=2} scripting language command.
     * <p>Perform a double left mouse click on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left count=2} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param to mouse click point. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse click count=2")
    public int mouseDoubleClick(@Param(name = "to", defaultValue = DEFAULT_MOUSE_COORDS) Point to) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON1, null, to, 2, null);
    }

    /**
     * <p>Perform a double left mouse click on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left count=2} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param x X-coordinate of the click point. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of the click point. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseDoubleClick(int x, int y, String wait) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON1, null, new Point(x, y), 2, wait);
    }

    /**
     * <p>Perform a double left mouse click on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse click button=left count=2} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param x X-coordinate of the click point. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of the click point. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseDoubleClick(int x, int y) throws IOException {
        return mouseEvent("click", 0, MouseEvent.BUTTON1, null, new Point(x, y), 2, null);
    }

    /**
     * <p>Perform a left button mouse drag on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse drag button=left} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param modifiers modifier mask as specified in the {@link java.awt.event.InputEvent}.
     * For example, to specify Ctrl+Alt use {@code InputEvent.CTRL_MASK | InputEvent.ALT_MASK}
     * @param button mouse button ID as specified in {@link java.awt.event.MouseEvent}.
     * Supported values are {@code MouseEvent.BUTTON1} (left button),
     * {@code MouseEvent.BUTTON2} (middle button) and {@code MouseEvent.BUTTON3} (right button).
     * If any other value is used, the argument defaults to the left button. This argument
     * applies only to mouse events which involve buttons, such as mouse click, press,
     * release and drag.
     * @param from start point of the drag. If the argument is null,
     * it defaults to the current desktop mouse pointer position. The value is
     * ignored by other actions.
     * @param to target drag point (drop location). If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse drag")
    public int mouseDrag(
            @Param(name = "from", defaultValue = DEFAULT_MOUSE_COORDS) Point from,
            @Param(name = "to") Point to,
            @Param(name = "modifiers", template = MODIFIER_CONVERSION_SNIPPET, defaultValue = "0") int modifiers,
            @Param(name = "btn", template = MOUSE_BUTTON_CONVERSION_SNIPPET) int button,
            @Param(name = "wait") String wait) throws IOException {
        return mouseEvent("drag", modifiers, button, from, to, 1, wait);
    }

    /**
     * <p>Perform a right button mouse drag on the currently connected desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse drag btn=right} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param modifiers modifier mask as specified in the {@link java.awt.event.InputEvent}.
     * For example, to specify Ctrl+Alt use {@code InputEvent.CTRL_MASK | InputEvent.ALT_MASK}
     * @param from start point of the drag. If the argument is null,
     * it defaults to the current desktop mouse pointer position. The value is
     * ignored by other actions.
     * @param to target drag point (drop location). If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse drag btn=right")
    public int mouseRightDrag(
            @Param(name = "from", defaultValue = DEFAULT_MOUSE_COORDS) Point from,
            @Param(name = "to") Point to,
            @Param(name = "modifiers", template = MODIFIER_CONVERSION_SNIPPET, defaultValue = "0") int modifiers,
            @Param(name = "wait") String wait) throws IOException {
        return mouseEvent("drag", modifiers, MouseEvent.BUTTON3, from, to, 1, wait);
    }

    /**
     * <p>Scroll up the mouse wheel on the current desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse wheelup} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param modifiers modifier mask as specified in the {@link java.awt.event.InputEvent}.
     * For example, to specify Ctrl+Alt use {@code InputEvent.CTRL_MASK | InputEvent.ALT_MASK}
     * @param to location to move the mouse pointer to before performing the wheel scroll. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param count how many wheel steps to scroll up. Default value is 1 (one mouse wheel step).
     * Delays among multiple steps are defined by a value in the Mouse command user preferences.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse wheelup")
    public int mouseWheelUp(
            @Param(name = "to") Point to,
            @Param(name = "modifiers", template = MODIFIER_CONVERSION_SNIPPET, defaultValue = "0") int modifiers,
            @Param(name = "count") int count,
            @Param(name = "wait") String wait) throws IOException {
        return mouseEvent("wheelup", modifiers, MouseEvent.BUTTON2, null, to, count, wait);
    }

    /**
     * <p>Scroll up the mouse wheel on the current desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse wheelup} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param modifiers modifier mask as specified in the {@link java.awt.event.InputEvent}.
     * For example, to specify Ctrl+Alt use {@code InputEvent.CTRL_MASK | InputEvent.ALT_MASK}
     * @param x X-coordinate of the mouse pointer to move to before scrolling the wheel. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of the mouse pointer to move to before scrolling the wheel. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @param count how many wheel steps to scroll up. Default value is 1 (one mouse wheel step).
     * Delays among multiple steps are defined by a value in the Mouse command user preferences.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseWheelUp(int x, int y, int modifiers, int count, String wait) throws IOException {
        return mouseEvent("wheelup", modifiers, MouseEvent.BUTTON2, null, new Point(x, y), count, wait);
    }

    /**
     * <p>Scroll down the mouse wheel on the current desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse wheeldown} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param modifiers modifier mask as specified in the {@link java.awt.event.InputEvent}.
     * For example, to specify Ctrl+Alt use {@code InputEvent.CTRL_MASK | InputEvent.ALT_MASK}
     * @param to location to move the mouse pointer to before performing the wheel scroll. If the argument is null,
     * it defaults to the current desktop mouse pointer position.
     * @param count how many wheel steps to scroll up. Default value is 1 (one mouse wheel step).
     * Delays among multiple steps are defined by a value in the Mouse command user preferences.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute), "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "mouse wheeldown")
    public int mouseWheelDown(
            @Param(name = "to") Point to,
            @Param(name = "modifiers", template = MODIFIER_CONVERSION_SNIPPET, defaultValue = "0") int modifiers,
            @Param(name = "count") int count,
            @Param(name = "wait") String wait) throws IOException {
        return mouseEvent("wheeldown", modifiers, MouseEvent.BUTTON2, null, to, count, wait);
    }

    /**
     * <p>Scroll down the mouse wheel on the current desktop. The method provides
     * access to functionality of the {@doc.cmd mouse Mouse wheeldown} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of pointer event transfer (its client doesn't
     * implement the {@link PointerTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param modifiers modifier mask as specified in the {@link java.awt.event.InputEvent}.
     * For example, to specify Ctrl+Alt use {@code InputEvent.CTRL_MASK | InputEvent.ALT_MASK}
     * @param x X-coordinate of the mouse pointer to move to before scrolling the wheel. If the argument is negative,
     * it defaults to the current desktop mouse pointer X-coordinate value.
     * @param y Y-coordinate of the mouse pointer to move to before scrolling the wheel. If the argument is negative,
     * it defaults to the current desktop mouse pointer Y-coordinate value.
     * @param count how many wheel steps to scroll up. Default value is 1 (one mouse wheel step).
     * Delays among multiple steps are defined by a value in the Mouse command user preferences.
     * @param wait how long to wait after the event is sent to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd mouse} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int mouseWheelDown(int x, int y, int modifiers, int count, String wait) throws IOException {
        return mouseEvent("wheeldown", modifiers, MouseEvent.BUTTON2, null, new Point(x, y), count, wait);
    }

    // ================== TYPE Command =========================================
    /**
     * <p>Type a text on the current desktop. The method provides
     * access to functionality of the {@doc.cmd type} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of text transfer (its client doesn't
     * implement the {@link KeyTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param text a text to type. Interpretation of the text is fully dependent
     * on the desktop client and it is subject to limitations applied by the client
     * protocol. For example, the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On the other hand
     * the native Java client can transfer any characters which can be typed on
     * the local keyboard.
     * @param count how many times to repeat typing of the text. Default value is 1
     * (type the text once). Delays among multiple typings are defined by a value
     * in the Type command user preferences.
     * @param location key location as is defined in the <code>java.awt.event.KeyEvent</code> class.
     * Acceptable values are <code>KeyEvent.KEY_LOCATION_STANDARD</code>,
     * <code>KeyEvent.KEY_LOCATION_LEFT</code>, <code>KeyEvent.KEY_LOCATION_RIGHT</code> and
     * <code>KeyEvent.KEY_LOCATION_NUMPAD</code>. If the argument is null, the
     * method defaults to <code>KeyEvent.KEY_LOCATION_STANDARD</code>.
     * Note that the method doesn't verify whether the [key,location] pair makes sense.
     * For example, alphabet characters are present on most keyboard just once and the only
     * logically valid location is the default standard one. Most clients however use the
     * location only as a hint and ignore it by keys where it is not applicable.
     * @param wait how long to wait after the text is typed to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd type} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "type")
    public int type(
            @Param(name = "argument") String text,
            @Param(name = "count")int count,
            @Param(name = "location", template = KEY_LOCATION_CONVERSION_SNIPPET) int location,
            @Param(name = "wait") String wait) throws IOException {
        List l = new ArrayList();
        l.add(text);
        Map m = new HashMap();
        if (count > 0) {
            l.add(AbstractCommandHandler.PARAM_COUNT);
            m.put(AbstractCommandHandler.PARAM_COUNT, Integer.toString(count));
        }
        if (wait != null) {
            l.add(AbstractCommandHandler.PARAM_WAIT);
            m.put(AbstractCommandHandler.PARAM_WAIT, wait);
        }
        if (location > KeyEvent.KEY_LOCATION_UNKNOWN) {
            l.add(PressCommand.PARAM_LOCATION);
            m.put(PressCommand.PARAM_LOCATION, location);
        }
        return runScriptCommand("type", l, m);
    }

    /**
     * <p>Type a text on the current desktop. The method provides
     * access to functionality of the {@doc.cmd type} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of text transfer (its client doesn't
     * implement the {@link KeyTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param text a text to type. Interpretation of the text is fully dependent
     * on the desktop client and it is subject to limitations applied by the client
     * protocol. For example, the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On the other hand
     * the native Java client can transfer any characters which can be typed on
     * the local keyboard.
     * @param count how many times to repeat typing of the text. Default value is 1
     * (type the text once). Delays among multiple typings are defined by a value
     * in the Type command user preferences.
     * @param wait how long to wait after the text is typed to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd type} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "type")
    public int type(
            @Param(name = "argument") String text,
            @Param(name = "count")int count,
            @Param(name = "wait") String wait) throws IOException {
        return type(text, count, -1, wait);
    }

    /**
     * <p>Type a text on the current desktop. The method provides
     * access to functionality of the {@doc.cmd type} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of text transfer (its client doesn't
     * implement the {@link KeyTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param text a text to type. Interpretation of the text is fully dependent
     * on the desktop client and it is subject to limitations applied by the client
     * protocol. For example, the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On the other hand
     * the native Java client can transfer any characters which can be typed on
     * the local keyboard.
     * @param wait how long to wait after the text is typed to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd type} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "type")
    public int type(@Param(name = "argument") String text, @Param(name = "wait") String wait) throws IOException {
        return type(text, 0, -1, wait);
    }

    /**
     * <p>Type a text on the current desktop. The method provides
     * access to functionality of the {@doc.cmd type} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of text transfer (its client doesn't
     * implement the {@link KeyTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param text a text to type. Interpretation of the text is fully dependent
     * on the desktop client and it is subject to limitations applied by the client
     * protocol. For example, the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On the other hand
     * the native Java client can transfer any characters which can be typed on
     * the local keyboard.
     * @return command exit code as is specified in the {@doc.cmd type} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "type")
    public int type(@Param(name = "argument") String text) throws IOException {
        return type(text, 0, -1, null);
    }

    // ================== TYPELINE Command =====================================
    /**
     * <p>Type a text and press an Enter key on the current desktop once or more times. It is equivalent
     * to a {@doc.cmd type} command followed by a {@doc.cmd press Press Enter}. The method provides
     * access to functionality of the {@doc.cmd typeline} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of text transfer (its client doesn't
     * implement the {@link KeyTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param text a text to type. Interpretation of the text is fully dependent
     * on the desktop client and it is subject to limitations applied by the client
     * protocol. For example, the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On the other hand
     * the native Java client can transfer any characters which can be typed on
     * the local keyboard.
     * @param count how many times to repeat typing of the text. Default value is 1
     * (type the text once). Delays among multiple typings are defined by a value
     * in the Type command user preferences.
     * @param location key location as is defined in the <code>java.awt.event.KeyEvent</code> class.
     * Acceptable values are <code>KeyEvent.KEY_LOCATION_STANDARD</code>,
     * <code>KeyEvent.KEY_LOCATION_LEFT</code>, <code>KeyEvent.KEY_LOCATION_RIGHT</code> and
     * <code>KeyEvent.KEY_LOCATION_NUMPAD</code>. If the argument is null, the
     * method defaults to <code>KeyEvent.KEY_LOCATION_STANDARD</code>.
     * Note that the method doesn't verify whether the [key,location] pair makes sense.
     * For example, alphabet characters are present on most keyboard just once and the only
     * logically valid location is the default standard one. Most clients however use the
     * location only as a hint and ignore it by keys where it is not applicable.
     * @param wait how long to wait after the text is typed to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd typeline} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "typeline")
    public int typeLine(
            @Param(name = "argument") String text,
            @Param(name = "count") int count,
            @Param(name = "location", template = KEY_LOCATION_CONVERSION_SNIPPET) int location,
            @Param(name = "wait") String wait) throws IOException {
        List l = new ArrayList();
        l.add(text);
        Map m = new HashMap();
        if (count > 0) {
            l.add(AbstractCommandHandler.PARAM_COUNT);
            m.put(AbstractCommandHandler.PARAM_COUNT, Integer.toString(count));
        }
        if (wait != null) {
            l.add(AbstractCommandHandler.PARAM_WAIT);
            m.put(AbstractCommandHandler.PARAM_WAIT, wait);
        }
        if (location > KeyEvent.KEY_LOCATION_UNKNOWN) {
            l.add(PressCommand.PARAM_LOCATION);
            m.put(PressCommand.PARAM_LOCATION, location);
        }
        return runScriptCommand("typeLine", l, m);
    }

    /**
     * <p>Type a text and press an Enter key on the current desktop once or more times. It is equivalent
     * to a {@doc.cmd type} command followed by a {@doc.cmd press Press Enter}. The method provides
     * access to functionality of the {@doc.cmd typeline} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of text transfer (its client doesn't
     * implement the {@link KeyTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param text a text to type. Interpretation of the text is fully dependent
     * on the desktop client and it is subject to limitations applied by the client
     * protocol. For example, the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On the other hand
     * the native Java client can transfer any characters which can be typed on
     * the local keyboard.
     * @param count how many times to repeat typing of the text. Default value is 1
     * (type the text once). Delays among multiple typings are defined by a value
     * in the Type command user preferences.
     * @param wait how long to wait after the text is typed to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd typeline} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "typeline")
    public int typeLine(
            @Param(name = "argument") String text,
            @Param(name = "count") int count,
            @Param(name = "wait") String wait) throws IOException {
        return typeLine(text, count, -1, wait);
    }
    
    /**
     * <p>Type a text and press an Enter key on the current desktop once or more times. It is equivalent
     * to a {@doc.cmd type} command followed by a {@doc.cmd press Press Enter}. The method provides
     * access to functionality of the {@doc.cmd typeline} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of text transfer (its client doesn't
     * implement the {@link KeyTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param text a text to type. Interpretation of the text is fully dependent
     * on the desktop client and it is subject to limitations applied by the client
     * protocol. For example, the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On the other hand
     * the native Java client can transfer any characters which can be typed on
     * the local keyboard.
     * @param wait how long to wait after the text is typed to the desktop. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd typeline} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "typeline")
    public int typeLine(@Param(name = "argument") String text, @Param(name = "wait") String wait) throws IOException {
        return typeLine(text, 0, -1, wait);
    }

    /**
     * <p>Type a text and press an Enter key on the current desktop once or more times. It is equivalent
     * to a {@doc.cmd type} command followed by a {@doc.cmd press Press Enter}. The method provides
     * access to functionality of the {@doc.cmd typeline} scripting language command.</p>
     *
     * <p>If no desktop is connected, the method throws an <code>IOException</code>. If the connected
     * desktop is not capable of text transfer (its client doesn't
     * implement the {@link KeyTransferCapable} interface), the method throws an
     * <code>IllegalArgumentException</code>.</p>
     *
     * @param text a text to type. Interpretation of the text is fully dependent
     * on the desktop client and it is subject to limitations applied by the client
     * protocol. For example, the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On the other hand
     * the native Java client can transfer any characters which can be typed on
     * the local keyboard.
     * @return command exit code as is specified in the {@doc.cmd typeline} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "typeline")
    public int typeLine(@Param(name = "argument") String text) throws IOException {
        return typeLine(text, 0, -1, null);
    }

    // ============= PRESS Command =============================================
    /**
     * <p>Press a key with optional modifiers on the current desktop once or more times. 
     * The method provides access to functionality of the {@doc.cmd press}
     * scripting language command.</p>
     *
     * <p>The method supports any key identifier derived from the <code>VK_</code>
     * key code constants declared in the <code>java.awt.event.KeyEvent</code> class
     * where the identifier itself is the string which follows the <code>VK_</code> prefix.
     * For example, as there is a <code>VK_ENTER</code> constant, the method supports
     * any key like "ENTER", "Enter" or "enter".</p>
     *
     * <p>As the key identifiers are in fact extracted from the <code>KeyEvent</code> class
     * at runtime using Java Reflection API, the range of supported keys may differ
     * depending on the version of Java used to execute this program. A complete map of the
     * supported key identifiers may be obtained through the {@link Utils#getKeyCodeTable()}
     * method.</p>
     * 
     * <p>Transferable keys and key combinations are further on subject to limitations applied
     * by the desktop client. For example the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On contrary
     * the native Java client can transfer only characters which can be generated on
     * the local keyboard regardless of the character set they belong to.</p>
     *
     * @param key a key to press. The argument must consist of a key identifier
     * which may be optionally prefixed with one or more supported modifiers
     * ("Ctrl", "Alt", "Shift" and "Meta") separated by
     * the plus character '+', for example "Ctrl+C" or "Alt+Ctrl+Delete". The method
     * also accepts arguments consisting only of modifiers. None of the components
     * is case sensitive and both the keys and modifiers may be specified in any
     * character case.
     *
     * @param location key location as is defined in the <code>java.awt.event.KeyEvent</code> class.
     * Acceptable values are <code>KeyEvent.KEY_LOCATION_STANDARD</code>,
     * <code>KeyEvent.KEY_LOCATION_LEFT</code>, <code>KeyEvent.KEY_LOCATION_RIGHT</code> and
     * <code>KeyEvent.KEY_LOCATION_NUMPAD</code>. If the argument is null, the
     * method defaults to <code>KeyEvent.KEY_LOCATION_STANDARD</code>.
     * Note that the method doesn't verify whether the [key,location] pair makes sense.
     * For example, alphabet characters are present on most keyboard just once and the only
     * logically valid location is the default standard one. Most clients however use the
     * location only as a hint and ignore it by keys where it is not applicable.
     *
     * @param count how many times to repeat the key press. Default value is 1
     * (press the key once). Delays among multiple press actions are defined by a value
     * in the Press Command user preferences.
     *
     * @param delay how long to wait between the key press and release. This
     * value is by default loaded from the Press command configuration. This parameter
     * allows to customize it for a single command call. It is handy for example
     * on mobile devices where certain functionality gets activated through a longer
     * key press. Value of this parameter must be a valid time value specified by
     * the {@doc.cmd timevalues Time Values} chapter of the language specification.
     * Use null to apply the default value.
     * @param wait how long to wait after all the key press events are sent to the desktop.
     * The argument must be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     *
     * @return command exit code as is specified in the {@doc.cmd press} specification.
     *
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "press")
    public int press(
            @Param(name = "argument") String key,
            @Param(name = "location", template = KEY_LOCATION_CONVERSION_SNIPPET) int location,
            @Param(name = "count") int count,
            @Param(name = "delay") String delay,
            @Param(name = "wait") String wait) throws IOException {
        List l = new ArrayList();
        l.add(key);
        Map m = new HashMap();
        if (count > 0) {
            l.add(PressCommand.PARAM_COUNT);
            m.put(PressCommand.PARAM_COUNT, Integer.toString(count));
        }
        if (location > 0) {
            l.add(PressCommand.PARAM_LOCATION);
            m.put(PressCommand.PARAM_LOCATION, location);
        }
        if (wait != null) {
            l.add(PressCommand.PARAM_WAIT);
            m.put(PressCommand.PARAM_WAIT, wait);
        }
        if (delay != null) {
            l.add(PressCommand.PARAM_DELAY);
            m.put(PressCommand.PARAM_DELAY, delay);
        }
        return runScriptCommand("press", l, m);
    }

    /**
     * <p>Press a key with optional modifiers on the current desktop once or more times.
     * The method provides access to functionality of the {@doc.cmd press}
     * scripting language command.</p>
     *
     * <p>The method supports any key identifier derived from the <code>VK_</code>
     * key code constants declared in the <code>java.awt.event.KeyEvent</code> class
     * where the identifier itself is the string which follows the <code>VK_</code> prefix.
     * For example, as there is a <code>VK_ENTER</code> constant, the method supports
     * any key like "ENTER", "Enter" or "enter".</p>
     *
     * <p>As the key identifiers are in fact extracted from the <code>KeyEvent</code> class
     * at runtime using Java Reflection API, the range of supported keys may differ
     * depending on the version of Java used to execute this program. A complete map of the
     * supported key identifiers may be obtained through the {@link Utils#getKeyCodeTable()}
     * method.</p>
     *
     * <p>Transferable keys and key combinations are further on subject to limitations applied
     * by the desktop client. For example the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On contrary
     * the native Java client can transfer only characters which can be generated on
     * the local keyboard regardless of the character set they belong to.</p>
     *
     * @param key a key to press. The argument must consist of a key identifier
     * which may be optionally prefixed with one or more supported modifiers
     * ("Ctrl", "Alt", "Shift" and "Meta") separated by
     * the plus character '+', for example "Ctrl+C" or "Alt+Ctrl+Delete". The method
     * also accepts arguments consisting only of modifiers. None of the components
     * is case sensitive and both the keys and modifiers may be specified in any
     * character case.
     *
     * @param location key location as is defined in the <code>java.awt.event.KeyEvent</code> class.
     * Acceptable values are <code>KeyEvent.KEY_LOCATION_STANDARD</code>,
     * <code>KeyEvent.KEY_LOCATION_LEFT</code>, <code>KeyEvent.KEY_LOCATION_RIGHT</code> and
     * <code>KeyEvent.KEY_LOCATION_NUMPAD</code>. If the argument is null, the
     * method defaults to <code>KeyEvent.KEY_LOCATION_STANDARD</code>.
     * Note that the method doesn't verify whether the [key,location] pair makes sense.
     * For example, alphabet characters are present on most keyboard just once and the only
     * logically valid location is the default standard one. Most clients however use the
     * location only as a hint and ignore it by keys where it is not applicable.
     *
     * @param count how many times to repeat the key press. Default value is 1
     * (press the key once). Delays among multiple press actions are defined by a value
     * in the Press Command user preferences.
     *
     * @param wait how long to wait after all the key press events are sent to the desktop.
     * The argument must be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     *
     * @return command exit code as is specified in the {@doc.cmd press} specification.
     *
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "press")
    public int press(
            @Param(name = "argument") String key,
            @Param(name = "location", template = KEY_LOCATION_CONVERSION_SNIPPET) int location,
            @Param(name = "count") int count,
            @Param(name = "wait") String wait) throws IOException {
        return press(key, location, count, null, wait);
    }

    // Not annotated - won't be used by the converter
    /**
     * <p>Press a character with optional modifiers on the current desktop once or more times.
     * The method provides
     * access to functionality of the {@doc.cmd press} scripting language command.</p>
     *
     * <p>Transferable characters and key combinations are further on subject to limitations applied
     * by the desktop client. For example the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On contrary
     * the native Java client can transfer only characters which can be generated on
     * the local keyboard regardless of the character set they belong to.</p>
     *
     * @param c a character to press. This method doesn't support modifiers.
     *
     * @param location key location as is defined in the <code>java.awt.event.KeyEvent</code> class.
     * Acceptable values are <code>KeyEvent.KEY_LOCATION_STANDARD</code>,
     * <code>KeyEvent.KEY_LOCATION_LEFT</code>, <code>KeyEvent.KEY_LOCATION_RIGHT</code> and
     * <code>KeyEvent.KEY_LOCATION_NUMPAD</code>. If the argument is null, the
     * method defaults to <code>KeyEvent.KEY_LOCATION_STANDARD</code>.
     * Note that the method doesn't verify whether the [key,location] pair makes sense.
     * For example, alphabet characters are present on most keyboard just once and the only
     * logically valid location is the default standard one. Most clients however use the
     * location only as a hint and ignore it by keys where it is not applicable.
     *
     * @param count how many times to repeat the character press. Default value is 1
     * (press the key once). Delays among multiple press actions are defined by a value
     * in the Press Command user preferences.
     *
     * @param delay how long to wait between the key press and release. This
     * value is by default loaded from the Press command configuration. This parameter
     * allows to customize it for a single command call. It is handy for example
     * on mobile devices where certain functionality gets activated through a longer
     * key press. Value of this parameter must be a valid time value specified by
     * the {@doc.cmd timevalues Time Values} chapter of the language specification.
     * Use null to apply the default value.
     * @param wait how long to wait after all the key press events are sent to the desktop.
     * The argument must be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     *
     * @return command exit code as is specified in the {@doc.cmd press} specification.
     *
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int press(char c, int location, int count, String delay, String wait) throws IOException {
        return press(Character.toString(c), location, count, delay, wait);
    }

    // Not annotated - won't be used by the converter
    /**
     * <p>Press a character with optional modifiers on the current desktop once or more times.
     * The method provides
     * access to functionality of the {@doc.cmd press} scripting language command.</p>
     *
     * <p>Transferable characters and key combinations are further on subject to limitations applied
     * by the desktop client. For example the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On contrary
     * the native Java client can transfer only characters which can be generated on
     * the local keyboard regardless of the character set they belong to.</p>
     *
     * @param c a character to press. This method doesn't support modifiers.
     *
     * @param location key location as is defined in the <code>java.awt.event.KeyEvent</code> class.
     * Acceptable values are <code>KeyEvent.KEY_LOCATION_STANDARD</code>,
     * <code>KeyEvent.KEY_LOCATION_LEFT</code>, <code>KeyEvent.KEY_LOCATION_RIGHT</code> and
     * <code>KeyEvent.KEY_LOCATION_NUMPAD</code>. If the argument is null, the
     * method defaults to <code>KeyEvent.KEY_LOCATION_STANDARD</code>.
     * Note that the method doesn't verify whether the [key,location] pair makes sense.
     * For example, alphabet characters are present on most keyboard just once and the only
     * logically valid location is the default standard one. Most clients however use the
     * location only as a hint and ignore it by keys where it is not applicable.
     *
     * @param count how many times to repeat the character press. Default value is 1
     * (press the key once). Delays among multiple press actions are defined by a value
     * in the Press Command user preferences.
     *
     * @param wait how long to wait after all the key press events are sent to the desktop.
     * The argument must be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     *
     * @return command exit code as is specified in the {@doc.cmd press} specification.
     *
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int press(char c, int location, int count, String wait) throws IOException {
        return press(Character.toString(c), location, count, null, wait);
    }

    /**
     * <p>Press a key with optional modifiers on the current desktop.
     * The method provides access to functionality of the {@doc.cmd press}
     * scripting language command.</p>
     *
     * <p>The method supports any key identifier derived from the <code>VK_</code>
     * key code constants declared in the <code>java.awt.event.KeyEvent</code> class
     * where the identifier itself is the string which follows the <code>VK_</code> prefix.
     * For example, as there is a <code>VK_ENTER</code> constant, the method supports
     * any key like "ENTER", "Enter" or "enter".</p>
     *
     * <p>As the key identifiers are in fact extracted from the <code>KeyEvent</code> class
     * at runtime using Java Reflection API, the range of supported keys may differ
     * depending on the version of Java used to execute this program. A complete map of the
     * supported key identifiers may be obtained through the {@link Utils#getKeyCodeTable()}
     * method.</p>
     *
     * <p>Transferable keys and key combinations are further on subject to limitations applied
     * by the desktop client. For example the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On contrary
     * the native Java client can transfer only characters which can be generated on
     * the local keyboard regardless of the character set they belong to.</p>
     *
     * @param key a key to press. The argument must consist of a key identifier
     * which may be optionally prefixed with one or more supported modifiers
     * ("Ctrl", "Alt", "Shift" and "Meta") separated by
     * the plus character '+', for example "Ctrl+C" or "Alt+Ctrl+Delete". The method
     * also accepts arguments consisting only of modifiers. None of the components
     * is case sensitive and both the keys and modifiers may be specified in any
     * character case.
     *
     * @param location key location as is defined in the <code>java.awt.event.KeyEvent</code> class.
     * Acceptable values are <code>KeyEvent.KEY_LOCATION_STANDARD</code>,
     * <code>KeyEvent.KEY_LOCATION_LEFT</code>, <code>KeyEvent.KEY_LOCATION_RIGHT</code> and
     * <code>KeyEvent.KEY_LOCATION_NUMPAD</code>. If the argument is null, the
     * method defaults to <code>KeyEvent.KEY_LOCATION_STANDARD</code>.
     * Note that the method doesn't verify whether the [key,location] pair makes sense.
     * For example, alphabet characters are present on most keyboard just once and the only
     * logically valid location is the default standard one. Most clients however use the
     * location only as a hint and ignore it by keys where it is not applicable.
     *
     * @param wait how long to wait after all the key press events are sent to the desktop.
     * The argument must be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     *
     * @return command exit code as is specified in the {@doc.cmd press} specification.
     *
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "press")
    public int press(@Param(name = "argument") String key, @Param(name = "location", template = KEY_LOCATION_CONVERSION_SNIPPET) int location, @Param(name = "wait") String wait) throws IOException {
        return press(key, location, 0, null, wait);
    }

    /**
     * <p>Press a key with optional modifiers on the current desktop.
     * The method provides access to functionality of the {@doc.cmd press}
     * scripting language command.</p>
     *
     * <p>The method supports any key identifier derived from the <code>VK_</code>
     * key code constants declared in the <code>java.awt.event.KeyEvent</code> class
     * where the identifier itself is the string which follows the <code>VK_</code> prefix.
     * For example, as there is a <code>VK_ENTER</code> constant, the method supports
     * any key like "ENTER", "Enter" or "enter".</p>
     *
     * <p>As the key identifiers are in fact extracted from the <code>KeyEvent</code> class
     * at runtime using Java Reflection API, the range of supported keys may differ
     * depending on the version of Java used to execute this program. A complete map of the
     * supported key identifiers may be obtained through the {@link Utils#getKeyCodeTable()}
     * method.</p>
     *
     * <p>Transferable keys and key combinations are further on subject to limitations applied
     * by the desktop client. For example the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On contrary
     * the native Java client can transfer only characters which can be generated on
     * the local keyboard regardless of the character set they belong to.</p>
     *
     * @param key a key to press. The argument must consist of a key identifier
     * which may be optionally prefixed with one or more supported modifiers
     * ("Ctrl", "Alt", "Shift" and "Meta") separated by
     * the plus character '+', for example "Ctrl+C" or "Alt+Ctrl+Delete". The method
     * also accepts arguments consisting only of modifiers. None of the components
     * is case sensitive and both the keys and modifiers may be specified in any
     * character case.
     *
     * @param location key location as is defined in the <code>java.awt.event.KeyEvent</code> class.
     * Acceptable values are <code>KeyEvent.KEY_LOCATION_STANDARD</code>,
     * <code>KeyEvent.KEY_LOCATION_LEFT</code>, <code>KeyEvent.KEY_LOCATION_RIGHT</code> and
     * <code>KeyEvent.KEY_LOCATION_NUMPAD</code>. If the argument is null, the
     * method defaults to <code>KeyEvent.KEY_LOCATION_STANDARD</code>.
     * Note that the method doesn't verify whether the [key,location] pair makes sense.
     * For example, alphabet characters are present on most keyboard just once and the only
     * logically valid location is the default standard one. Most clients however use the
     * location only as a hint and ignore it by keys where it is not applicable.
     *
     * @return command exit code as is specified in the {@doc.cmd press} specification.
     *
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "press")
    public int press(@Param(name = "argument") String key, @Param(name = "location", template = KEY_LOCATION_CONVERSION_SNIPPET) int location) throws IOException {
        return press(key, location, 0, null, null);
    }

    /**
     * <p>Press a key with optional modifiers on the current desktop.
     * The method provides access to functionality of the {@doc.cmd press}
     * scripting language command.</p>
     *
     * <p>The method supports any key identifier derived from the <code>VK_</code>
     * key code constants declared in the <code>java.awt.event.KeyEvent</code> class
     * where the identifier itself is the string which follows the <code>VK_</code> prefix.
     * For example, as there is a <code>VK_ENTER</code> constant, the method supports
     * any key like "ENTER", "Enter" or "enter".</p>
     *
     * <p>As the key identifiers are in fact extracted from the <code>KeyEvent</code> class
     * at runtime using Java Reflection API, the range of supported keys may differ
     * depending on the version of Java used to execute this program. A complete map of the
     * supported key identifiers may be obtained through the {@link Utils#getKeyCodeTable()}
     * method.</p>
     *
     * <p>Transferable keys and key combinations are further on subject to limitations applied
     * by the desktop client. For example the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On contrary
     * the native Java client can transfer only characters which can be generated on
     * the local keyboard regardless of the character set they belong to.</p>
     *
     * @param key a key to press. The argument must consist of a key identifier
     * which may be optionally prefixed with one or more supported modifiers
     * ("Ctrl", "Alt", "Shift" and "Meta") separated by
     * the plus character '+', for example "Ctrl+C" or "Alt+Ctrl+Delete". The method
     * also accepts arguments consisting only of modifiers. None of the components
     * is case sensitive and both the keys and modifiers may be specified in any
     * character case.
     *
     * @param wait how long to wait after all the key press events are sent to the desktop.
     * The argument must be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     *
     * @return command exit code as is specified in the {@doc.cmd press} specification.
     *
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "press")
    public int press(@Param(name = "argument") String key, @Param(name = "wait") String wait) throws IOException {
        return press(key, KeyEvent.KEY_LOCATION_STANDARD, 0, null, wait);
    }

    /**
     * <p>Press a key with optional modifiers on the current desktop.
     * The method provides access to functionality of the {@doc.cmd press}
     * scripting language command.</p>
     *
     * <p>The method supports any key identifier derived from the <code>VK_</code>
     * key code constants declared in the <code>java.awt.event.KeyEvent</code> class
     * where the identifier itself is the string which follows the <code>VK_</code> prefix.
     * For example, as there is a <code>VK_ENTER</code> constant, the method supports
     * any key like "ENTER", "Enter" or "enter".</p>
     *
     * <p>As the key identifiers are in fact extracted from the <code>KeyEvent</code> class
     * at runtime using Java Reflection API, the range of supported keys may differ
     * depending on the version of Java used to execute this program. A complete map of the
     * supported key identifiers may be obtained through the {@link Utils#getKeyCodeTable()}
     * method.</p>
     *
     * <p>Transferable keys and key combinations are further on subject to limitations applied
     * by the desktop client. For example the RFB (VNC) protocol cannot transfer characters
     * outside of the Latin-1 (ISO 8859-1) character set. On contrary
     * the native Java client can transfer only characters which can be generated on
     * the local keyboard regardless of the character set they belong to.</p>
     *
     * @param key a key to press. The argument must consist of a key identifier
     * which may be optionally prefixed with one or more supported modifiers
     * ("Ctrl", "Alt", "Shift" and "Meta") separated by
     * the plus character '+', for example "Ctrl+C" or "Alt+Ctrl+Delete". The method
     * also accepts arguments consisting only of modifiers. None of the components
     * is case sensitive and both the keys and modifiers may be specified in any
     * character case.
     *
     * @return command exit code as is specified in the {@doc.cmd press} specification.
     *
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "press")
    public int press(@Param(name = "argument") String key) throws IOException {
        return press(key, KeyEvent.KEY_LOCATION_STANDARD, 0, null, null);
    }

    // ================  WAIT Command  =========================================
    /**
     * Postpone execution of the test script for the specified time period.
     * The method provides access to functionality of the {@doc.cmd wait}
     * scripting language command.</p>
     *
     * @param time how long to wait. The argument must be in format specified
     * by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. If the argument is null or zero, there will be no delay and
     * the test script proceeds immediately to the next command.
     *
     * @return command exit code as is specified in the {@doc.cmd wait} specification.
     */
    @Command(name = "wait")
    public int wait(@Param(name = "argument") String time) {
        List l = new ArrayList();
        l.add(time);
        try {
            return runScriptCommand("wait", l, new HashMap());
        } catch (IOException ex) {
            // The Wait command should never throw the IOException because it
            // doesn't participate in the client/server communication.
            return 1;
        }
    }

    // ================  SCREENSHOT Command  =========================================
    private int screenshot(boolean dummyParam,  // Just to differentiate the method from other ones
            @Param(name = "argument") File file,
            @Param(name = "desc") String description,
            @Param(name = "area") Rectangle area,
            @Param(name = "template") Object templates,
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "passrate") float passRate,
            @Param(name = "cmparea") Rectangle cmpArea) throws IOException {
        List l = new ArrayList();
        Map m = new HashMap();
        if (file != null) {
            l.add(file);
        }
        if (description != null) {
            l.add(ScreenshotCommand.PARAM_DESC);
            m.put(ScreenshotCommand.PARAM_DESC, description);
        }
        if (area != null) {
            l.add(ScreenshotCommand.PARAM_AREA);
            m.put(ScreenshotCommand.PARAM_AREA, area);
        }

        // Comparison arguments
        if (templates != null) {
            if (templates instanceof File[]) {
                List<File> lf = Arrays.asList((File[])templates);
                l.add(ScreenshotCommand.PARAM_TEMPLATE);
                m.put(ScreenshotCommand.PARAM_TEMPLATE, lf);
            } else {
                l.add(ScreenshotCommand.PARAM_TEMPLATE);
                m.put(ScreenshotCommand.PARAM_TEMPLATE, templates);
            }
        }
        if (method != null) {
            l.add(CompareToCommand.PARAM_METHOD);
            m.put(CompareToCommand.PARAM_METHOD, method);
        }
        if (methodParams != null) {
            l.add(CompareToCommand.PARAM_METHODPARAMS);
            m.put(CompareToCommand.PARAM_METHODPARAMS, methodParams);
        }
        if (passRate >= 0) {
            l.add(CompareToCommand.PARAM_PASSRATE);
            m.put(CompareToCommand.PARAM_PASSRATE, passRate);
        }
        if (cmpArea != null) {
            l.add(CompareToCommand.PARAM_CMPAREA);
            m.put(CompareToCommand.PARAM_CMPAREA, cmpArea);
        }
        return runScriptCommand("screenshot", l, m);
    }

    /**
     * <p>Take a screenshot of the remote desktop or its part, save it to a file and
     * eventually perform image comparison with the given template.
     * The method provides access to functionality of the {@doc.cmd screenshot}
     * scripting language command.</p>
     *
     * <p>The method requires a desktop connection and throws an
     * {@link IllegalArgumentException} if not connected. The target
     * format is guessed from the argument file extension and the method throws
     * an {@link IllegalArgumentException} if it is not supported. Images can
     * be saved in any of the format supported by the current Java.
     * Java 6 Standard Edition supports at least JPG/JPEG, BMP, WBMP, PNG and
     * GIF formats. To get a complete list of formats supported by your Java
     * use {@link ImageIO#getWriterFileSuffixes()}.</p>
     *
     * <p>If at least one of the image comparison parameters is specified (not null
     * or positive in case of pass rate), such as the template, method, methodParams,
     * passRate or cmpArea, the command performs image comparison the same way
     * a corresponding {@doc.cmd compareto} command would do. Even if none of the
     * parameters is specified, the command searches the template directory for an
     * image of the same name as the screenshot one and performs image comparison
     * with default parameter values if such an image is found. This "auto comparison"
     * may be switched off in the Screenshot preferences. Default parameter values
     * may be configured there as well.</p>
     *
     * <p>Each successful call of this method inserts a data structure 
     * ({@link ScreenshotCommand#ScreenshotInfo} instance) with the screenshot name,
     * description and eventual image comparison parameters
     * to a list in the context (key {@link ScriptingContext#CONTEXT_OUTPUT_OBJECTS}).
     * It will also internally fire a specific event which notifies registered
     * listeners of the new screenshot (see {@link CommandEvent#OUTPUT_CHANGED_EVENT}).
     * This event may be picked up by any running report provider which may
     * follow up and refresh the report file.</p>
     *
     * @param file a file to save the image to. The file can be either relative 
     * or absolute and it must have extension of one of the image formats supported by Java. 
     * Files with a relative path will be saved to the configured 
     * output directory. This path is by default set to the user home directory 
     * and its default location can be configured in the preferences. It may be 
     * also overriden for the current 
     * script through the {@link ScriptingContext#setOutputDir(java.io.File)} method.
     * If the specified file exists, it gets overwritten. If the user has not 
     * sufficient permissions to create this file, the method throws 
     * an {@link IllegalArgumentException}.
     * @param description screenshot description. It doesn't get written to the 
     * image file. It is saved only to the internal data structure where it may 
     * be picked up by report providers.
     * @param area a rectangle to cut from the desktop image. If the argument 
     * is null, the method will save the full size remote desktop image.
     * @param templates list of template images to compare the remote desktop 
     * image to. 
     * @param method image comparison method. The value must correspond to one of 
     * the plugin codes ({@link Plugin#getCode()} of the installed image comparison 
     * module plugins. {@product.name} provides by default two built-in methods, "default" 
     * and "search". See the "{@doc.comparison}" document for more information.
     * @param methodParams method parameters. This is a legacy helper argument intended 
     * to give third parties an option to specify custom parameters to the 
     * custom image comparison plugins, for example in a form of comma separated values. 
     * This argument is not used by any of the built-in {@product.name} image 
     * comparison methods and it should be specified as null.
     * @param passRate image comparison pass rate specified as percentage. It 
     * indicates how much the images must match to conside the comparison as "pass". 
     * Interpretation of this value is up to individual plugin implementations. Both 
     * built-in {@product.name} image comparison methods calculate how many different 
     * pixels the two compared images contain and compare this figure in relative 
     * form to the pass rate. If the specified pass rate is negative, it is 
     * interpreted as if it is not specified and default pass rate from the 
     * user preferences is used instead.
     * @param cmpArea a rectangle of the desktop image to restrict the image 
     * comparison to. If the argument is null, the comparison will be performed on 
     * the full desktop image. Image comparison area should be used just where 
     * it makes sense with regard to the method algoritm, for example with the "search" 
     * method. See the "{@doc.comparison}" document for more information.
     * @return command exit code as is specified in the {@doc.cmd screenshot} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "screenshot")
    public int screenshot(
            @Param(name = "argument") File file,
            @Param(name = "desc") String description,
            @Param(name = "area") Rectangle area,
            @Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "passrate") float passRate,
            @Param(name = "cmparea") Rectangle cmpArea) throws IOException {
        return screenshot(false, file, description, area, templates, method, methodParams, passRate, cmpArea);
    }

    /**
     * <p>Take a screenshot of the remote desktop or its part, save it to a file and
     * eventually perform image comparison with the given templates.
     * The method provides access to functionality of the {@doc.cmd screenshot}
     * scripting language command.</p>
     *
     * <p>The method requires a desktop connection and throws an
     * {@link IllegalArgumentException} if not connected. The target
     * format is guessed from the argument file extension and the method throws
     * an {@link IllegalArgumentException} if it is not supported. Images can
     * be saved in any of the format supported by the current Java.
     * Java 6 Standard Edition supports at least JPG/JPEG, BMP, WBMP, PNG and
     * GIF formats. To get a complete list of formats supported by your Java
     * use {@link ImageIO#getWriterFileSuffixes()}.</p>
     *
     * <p>If at least one of the image comparison parameters is specified (not null
     * or positive in case of pass rate), such as the template, method, methodParams,
     * passRate or cmpArea, the command performs image comparison the same way
     * a corresponding {@doc.cmd compareto} command would do. Even if none of the
     * parameters is specified, the command searches the template directory for an
     * image of the same name as the screenshot one and performs image comparison
     * with default parameter values if such an image is found. This "auto comparison"
     * may be switched off in the Screenshot preferences. Default parameter values
     * may be configured there as well.</p>
     *
     * <p>Each successful call of this method inserts a data structure
     * ({@link ScreenshotCommand#ScreenshotInfo} instance) with the screenshot name,
     * description and eventual image comparison parameters
     * to a list in the context (key {@link ScriptingContext#CONTEXT_OUTPUT_OBJECTS}).
     * It will also internally fire a specific event which notifies registered
     * listeners of the new screenshot (see {@link CommandEvent#OUTPUT_CHANGED_EVENT}).
     * This event may be picked up by any running report provider which may
     * follow up and refresh the report file.</p>
     *
     * @param file a file to save the image to. The file can be either relative
     * or absolute and it must have extension of one of the image formats supported by Java.
     * Files with a relative path will be saved to the configured
     * output directory. This path is by default set to the user home directory
     * and its default location can be configured in the preferences. It may be
     * also overriden for the current
     * script through the {@link ScriptingContext#setOutputDir(java.io.File)} method.
     * If the specified file exists, it gets overwritten. If the user has not
     * sufficient permissions to create this file, the method throws
     * an {@link IllegalArgumentException}.
     * @param description screenshot description. It doesn't get written to the
     * image file. It is saved only to the internal data structure where it may
     * be picked up by report providers.
     * @param area a rectangle to cut from the desktop image. If the argument
     * is null, the method will save the full size remote desktop image.
     * @param templates list of template images to compare the remote desktop
     * image to.
     * @param method image comparison method. The value must correspond to one of
     * the plugin codes ({@link Plugin#getCode()} of the installed image comparison
     * module plugins. {@product.name} provides by default two built-in methods, "default"
     * and "search". See the "{@doc.comparison}" document for more information.
     * @param methodParams method parameters. This is a legacy helper argument intended
     * to give third parties an option to specify custom parameters to the
     * custom image comparison plugins, for example in a form of comma separated values.
     * This argument is not used by any of the built-in {@product.name} image
     * comparison methods and it should be specified as null.
     * @param passRate image comparison pass rate specified as percentage. It
     * indicates how much the images must match to conside the comparison as "pass".
     * Interpretation of this value is up to individual plugin implementations. Both
     * built-in {@product.name} image comparison methods calculate how many different
     * pixels the two compared images contain and compare this figure in relative
     * form to the pass rate. If the specified pass rate is negative, it is
     * interpreted as if it is not specified and default pass rate from the
     * user preferences is used instead.
     * @param cmpArea a rectangle of the desktop image to restrict the image
     * comparison to. If the argument is null, the comparison will be performed on
     * the full desktop image. Image comparison area should be used just where
     * it makes sense with regard to the method algoritm, for example with the "search"
     * method. See the "{@doc.comparison}" document for more information.
     * @return command exit code as is specified in the {@doc.cmd screenshot} specification.
     * @throws IOException when an I/O error occurs.
     */
    public int screenshot(
            @Param(name = "argument") File file,
            @Param(name = "desc") String description,
            @Param(name = "area") Rectangle area,
            @Param(name = "template") Image templates[],
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "passrate") float passRate,
            @Param(name = "cmparea") Rectangle cmpArea) throws IOException {
        return screenshot(false, file, description, area, templates, method, methodParams, passRate, cmpArea);
    }

    /**
     * <p>Take a screenshot of the remote desktop or its part, save it to a file and
     * eventually perform image comparison with the given template.
     * The method provides access to functionality of the {@doc.cmd screenshot}
     * scripting language command. As this is just a convenience method calling
     * the {@link #screenshot(java.io.File, java.lang.String, java.awt.Rectangle, java.io.File[], java.lang.String, java.lang.String, float, java.awt.Rectangle)} one,
     * see its documentation for a complete description.</p>
     *
     * @param file a file to save the image to. The file can be either relative
     * or absolute and it must have extension of one of the image formats supported by Java.
     * Files with a relative path will be saved to the configured
     * output directory. This path is by default set to the user home directory
     * and its default location can be configured in the preferences. It may be
     * also overriden for the current
     * script through the {@link ScriptingContext#setOutputDir(java.io.File)} method.
     * If the specified file exists, it gets overwritten. If the user has not
     * sufficient permissions to create this file, the method throws
     * an {@link IllegalArgumentException}.
     * @param description screenshot description. It doesn't get written to the
     * image file. It is saved only to the internal data structure where it may
     * be picked up by report providers.
     * @param area a rectangle to cut from the desktop image. If the argument
     * is null, the method will save the full size remote desktop image.
     * @param templates list of template images to compare the remote desktop
     * image to.
     * @param method image comparison method. The value must correspond to one of
     * the plugin codes ({@link Plugin#getCode()} of the installed image comparison
     * module plugins. {@product.name} provides by default two built-in methods, "default"
     * and "search". See the "{@doc.comparison}" document for more information.
     * @param passRate image comparison pass rate specified as percentage. It
     * indicates how much the images must match to conside the comparison as "pass".
     * Interpretation of this value is up to individual plugin implementations. Both
     * built-in {@product.name} image comparison methods calculate how many different
     * pixels the two compared images contain and compare this figure in relative
     * form to the pass rate. If the specified pass rate is negative, it is
     * interpreted as if it is not specified and default pass rate from the
     * user preferences is used instead.
     * @param cmpArea a rectangle of the desktop image to restrict the image
     * comparison to. If the argument is null, the comparison will be performed on
     * the full desktop image. Image comparison area should be used just where
     * it makes sense with regard to the method algoritm, for example with the "search"
     * method. See the "{@doc.comparison}" document for more information.
     * @return command exit code as is specified in the {@doc.cmd screenshot} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "screenshot")
    public int screenshot(
            @Param(name = "argument") File file,
            @Param(name = "desc") String description,
            @Param(name = "area") Rectangle area,
            @Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "method") String method,
            @Param(name = "passrate") float passRate,
            @Param(name = "cmparea") Rectangle cmpArea) throws IOException {
        return screenshot(false, file, description, area, templates, method, null, passRate, cmpArea);
    }

    /**
     * <p>Take a screenshot of the remote desktop or its part, save it to a file and
     * eventually perform image comparison with the given template.
     * The method provides access to functionality of the {@doc.cmd screenshot}
     * scripting language command. As this is just a convenience method calling
     * the {@link #screenshot(java.io.File, java.lang.String, java.awt.Rectangle, java.io.File[], java.lang.String, java.lang.String, float, java.awt.Rectangle)} one,
     * see its documentation for a complete description.</p>
     *
     * @param file a file to save the image to. The file can be either relative
     * or absolute and it must have extension of one of the image formats supported by Java.
     * Files with a relative path will be saved to the configured
     * output directory. This path is by default set to the user home directory
     * and its default location can be configured in the preferences. It may be
     * also overriden for the current
     * script through the {@link ScriptingContext#setOutputDir(java.io.File)} method.
     * If the specified file exists, it gets overwritten. If the user has not
     * sufficient permissions to create this file, the method throws
     * an {@link IllegalArgumentException}.
     * @param description screenshot description. It doesn't get written to the
     * image file. It is saved only to the internal data structure where it may
     * be picked up by report providers.
     * @param area a rectangle to cut from the desktop image. If the argument
     * is null, the method will save the full size remote desktop image.
     * @param templates list of template images to compare the remote desktop
     * image to.
     * @param method image comparison method. The value must correspond to one of
     * the plugin codes ({@link Plugin#getCode()} of the installed image comparison
     * module plugins. {@product.name} provides by default two built-in methods, "default"
     * and "search". See the "{@doc.comparison}" document for more information.
     * @param passRate image comparison pass rate specified as percentage. It
     * indicates how much the images must match to conside the comparison as "pass".
     * Interpretation of this value is up to individual plugin implementations. Both
     * built-in {@product.name} image comparison methods calculate how many different
     * pixels the two compared images contain and compare this figure in relative
     * form to the pass rate. If the specified pass rate is negative, it is
     * interpreted as if it is not specified and default pass rate from the
     * user preferences is used instead.
     * @return command exit code as is specified in the {@doc.cmd screenshot} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "screenshot")
    public int screenshot(
            @Param(name = "argument") File file,
            @Param(name = "desc") String description,
            @Param(name = "area") Rectangle area,
            @Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "method") String method,
            @Param(name = "passrate") float passRate) throws IOException {
        return screenshot(false, file, description, area, templates, method, null, passRate, null);
    }

    /**
     * <p>Take a screenshot of the remote desktop or its part and save it to a file.
     * The method provides access to functionality of the {@doc.cmd screenshot}
     * scripting language command. As this is just a convenience method calling
     * the {@link #screenshot(java.io.File, java.lang.String, java.awt.Rectangle, java.io.File[], java.lang.String, java.lang.String, float, java.awt.Rectangle)} one,
     * see its documentation for a complete description.</p>
     *
     * @param file a file to save the image to. The file can be either relative
     * or absolute and it must have extension of one of the image formats supported by Java.
     * Files with a relative path will be saved to the configured
     * output directory. This path is by default set to the user home directory
     * and its default location can be configured in the preferences. It may be
     * also overriden for the current
     * script through the {@link ScriptingContext#setOutputDir(java.io.File)} method.
     * If the specified file exists, it gets overwritten. If the user has not
     * sufficient permissions to create this file, the method throws
     * an {@link IllegalArgumentException}.
     * @param description screenshot description. It doesn't get written to the
     * image file. It is saved only to the internal data structure where it may
     * be picked up by report providers.
     * @param area a rectangle to cut from the desktop image. If the argument
     * is null, the method will save the full size remote desktop image.
     * @return command exit code as is specified in the {@doc.cmd screenshot} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "screenshot")
    public int screenshot(
            @Param(name = "argument") File file,
            @Param(name = "desc") String description,
            @Param(name = "area") Rectangle area) throws IOException {
        return screenshot(false, file, description, area, null, null, null, -1f, null);
    }

    /**
     * <p>Take a screenshot of the remote desktop and save it to a file.
     * The method provides access to functionality of the {@doc.cmd screenshot}
     * scripting language command. As this is just a convenience method calling
     * the {@link #screenshot(java.io.File, java.lang.String, java.awt.Rectangle, java.io.File[], java.lang.String, java.lang.String, float, java.awt.Rectangle)} one,
     * see its documentation for a complete description.</p>
     *
     * @param file a file to save the image to. The file can be either relative
     * or absolute and it must have extension of one of the image formats supported by Java.
     * Files with a relative path will be saved to the configured
     * output directory. This path is by default set to the user home directory
     * and its default location can be configured in the preferences. It may be
     * also overriden for the current
     * script through the {@link ScriptingContext#setOutputDir(java.io.File)} method.
     * If the specified file exists, it gets overwritten. If the user has not
     * sufficient permissions to create this file, the method throws
     * an {@link IllegalArgumentException}.
     * @param description screenshot description. It doesn't get written to the
     * image file. It is saved only to the internal data structure where it may
     * be picked up by report providers.
     * @return command exit code as is specified in the {@doc.cmd screenshot} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "screenshot")
    public int screenshot(@Param(name = "argument") File file, @Param(name = "desc") String description) throws IOException {
        return screenshot(false, file, description, null, null, null, null, -1f, null);
    }

    /**
     * <p>Take a screenshot of the remote desktop or its part and save it to a file.
     * The method provides access to functionality of the {@doc.cmd screenshot}
     * scripting language command. As this is just a convenience method calling
     * the {@link #screenshot(java.io.File, java.lang.String, java.awt.Rectangle, java.io.File[], java.lang.String, java.lang.String, float, java.awt.Rectangle)} one,
     * see its documentation for a complete description.</p>
     *
     * @param file a file to save the image to. The file can be either relative
     * or absolute and it must have extension of one of the image formats supported by Java.
     * Files with a relative path will be saved to the configured
     * output directory. This path is by default set to the user home directory
     * and its default location can be configured in the preferences. It may be
     * also overriden for the current
     * script through the {@link ScriptingContext#setOutputDir(java.io.File)} method.
     * If the specified file exists, it gets overwritten. If the user has not
     * sufficient permissions to create this file, the method throws
     * an {@link IllegalArgumentException}.
     * @param area a rectangle to cut from the desktop image. If the argument
     * is null, the method will save the full size remote desktop image.
     * @return command exit code as is specified in the {@doc.cmd screenshot} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "screenshot")
    public int screenshot(@Param(name = "argument") File file, @Param(name = "area") Rectangle area) throws IOException {
        return screenshot(false, file, null, area, null, null, null, -1f, null);
    }

    /**
     * <p>Take a screenshot of the remote desktop and save it to a file.
     * The method provides access to functionality of the {@doc.cmd screenshot}
     * scripting language command. As this is just a convenience method calling
     * the {@link #screenshot(java.io.File, java.lang.String, java.awt.Rectangle, java.io.File[], java.lang.String, java.lang.String, float, java.awt.Rectangle)} one,
     * see its documentation for a complete description.</p>
     *
     * @param file a file to save the image to. The file can be either relative
     * or absolute and it must have extension of one of the image formats supported by Java.
     * Files with a relative path will be saved to the configured
     * output directory. This path is by default set to the user home directory
     * and its default location can be configured in the preferences. It may be
     * also overriden for the current
     * script through the {@link ScriptingContext#setOutputDir(java.io.File)} method.
     * If the specified file exists, it gets overwritten. If the user has not
     * sufficient permissions to create this file, the method throws
     * an {@link IllegalArgumentException}.
     * @return command exit code as is specified in the {@doc.cmd screenshot} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "screenshot")
    public int screenshot(@Param(name = "argument") File file) throws IOException {
        return screenshot(false, file, null, null, null, null, null, -1f, null);
    }

    // ================  SENDMAIL Command  =====================================
    /**
     * <p>Send an E-mail of the specified subject, text and optional file attachments
     * to one or more recipients through an SMTP server. The method provides
     * access to functionality of the {@doc.cmd sendmail} scripting language command.</p>
     *
     * <p>The method and the underlying command handler take advantage of the
     * Java Mail API. It is provided as a separate library, typically in form
     * of a file called <code>mail.jar</code> together with its dependency
     * Java Activation Framework (<code>activation.jar</code>). Should you
     * experience any {@link ClassNotFoundException} problems, check whether these
     * two libraries are correctly included in your Java class path.</p>
     *
     * <p>As some of the arguments such as server name and port, sender and
     * recipient addresses and SMTP user name are often
     * static for a particular user, it is possible to define their default values
     * in the user preferences. These arguments may be then omitted (meaning specified as null)
     * in the sendmail method calls. Note that all this data gets saved to a
     * plain text configuration file in the user home directory and it shouldn't
     * contain anything considered sensitive. For this reason there's no configuration
     * parameter for SMTP password which must be specified in each method call
     * (provided that the SMTP server requires authentication).</p>
     * 
     * @param server address and eventually port of the SMTP server. The host
     * may be specified both by a name or numeric IP address, with an optional
     * port number specified after a colon (for example, <code>"mysmtp.mydomain.com:1234"</code>).
     * If no port is specified, the method defaults to the well known SMTP port of 25.
     * @param user optional user name for authentication to the SMTP server.
     * @param password optional password for authentication to the SMTP server.
     * @param from sender address, for example "john.doe@mydomain.com". The method
     * itself doesn't validate the address. If it is however invalid, the underlying
     * JavaMail API may refuse it.
     * @param to one or more recipient address(es) separated by comma.
     * @param subject E-mail subject. It should be just plain text without any new line characters.
     * @param text message body. To insert a new line character use "\n". For example,
     * a text of "test\ntest" will resolve in two lines containing word "test".
     * If the text starts with "<html>" (not case sensitive), the content type is
     * set to "text/html". Otherwise the mail body is sent as plain text ("text/plain").
     * @param attachments optional file attachments.
     * @param debug true switches on verbose mode provided by the JavaMail API
     * framework. The method then prints out detailed messages of the SMTP server
     * connection, authentication and message transfer. Use this mode to debug
     * why the command fails.
     * @return command exit code as is specified in the {@doc.cmd sendmail} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "sendmail")
    public int sendMail(
            @Param(name = "server") String server,
            @Param(name = "user") String user,
            @Param(name = "passwd") String password,
            @Param(name = "from") String from,
            @Param(name = "to") String to,
            @Param(name = "subject") String subject,
            @Param(name = "text") String text,
            @Param(name = "attach", template = FILE_LIST_SNIPPET) File attachments[],
            @Param(name = "debug", defaultValue = "false") boolean debug) throws IOException {
        List l = new ArrayList();
        Map m = new HashMap();
        if (from != null) {
            l.add(SendMailCommand.PARAM_FROM);
            m.put(SendMailCommand.PARAM_FROM, from);
        }
        if (to != null) {
            l.add(SendMailCommand.PARAM_TO);
            m.put(SendMailCommand.PARAM_TO, to);
        }
        if (server != null) {
            l.add(SendMailCommand.PARAM_SERVER);
            m.put(SendMailCommand.PARAM_SERVER, server);
        }
        if (user != null) {
            l.add(SendMailCommand.PARAM_USER);
            m.put(SendMailCommand.PARAM_USER, user);
        }
        if (subject != null) {
            l.add(SendMailCommand.PARAM_SUBJECT);
            m.put(SendMailCommand.PARAM_SUBJECT, subject);
        }
        if (text != null) {
            l.add(SendMailCommand.PARAM_TEXT);
            m.put(SendMailCommand.PARAM_TEXT, text);
        }
        if (attachments != null && attachments.length > 0) {
            String s = "";
            for (File a : attachments) {
                s += Utils.getFullPath(a) + TokenParser.FILE_PATH_SEPARATOR;
            }
            // Remove the separator from the end of the list
            s = s.substring(0, Math.max(0, s.length() - TokenParser.FILE_PATH_SEPARATOR.length()));
            l.add(SendMailCommand.PARAM_ATTACH);
            m.put(SendMailCommand.PARAM_ATTACH, s);
        }
        if (password != null) {
            l.add(SendMailCommand.PARAM_PASSWD);
            m.put(SendMailCommand.PARAM_PASSWD, password);
        }
        l.add(SendMailCommand.PARAM_DEBUG);
        m.put(SendMailCommand.PARAM_DEBUG, debug ? "true" : "false");
        return runScriptCommand("sendMail", l, m);
    }

    /**
     * <p>Send an E-mail of the specified subject, text and optional file attachments
     * to one or more recipients through an SMTP server. The method provides
     * access to functionality of the {@doc.cmd sendmail} scripting language command.
     * The method works correctly only if there are valid default values of
     * the SMTP server, port and user, sender and recipient address(es) in the
     * user preferences. If any of these values is missing, the method throws an
     * {@link IllegalArgumentException}.</p>
     *
     * <p>As this is just a convenience method calling
     * the {@link #sendMail(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.io.File[], boolean)} one,
     * see its documentation for a complete description.</p>
     *
     * @param password optional password for authentication to the SMTP server.
     * @param subject E-mail subject. It should be just plain text without any new line characters.
     * @param text message body. To insert a new line character use "\n". For example,
     * a text of "test\ntest" will resolve in two lines containing word "test".
     * If the text starts with "<html>" (not case sensitive), the content type is
     * set to "text/html". Otherwise the mail body is sent as plain text ("text/plain").
     * @param attachments optional file attachments.
     * @param debug true switches on verbose mode provided by the JavaMail API
     * framework. The method then prints out detailed messages of the SMTP server
     * connection, authentication and message transfer. Use this mode to debug
     * why the command fails.
     * @return command exit code as is specified in the {@doc.cmd sendmail} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "sendmail")
    public int sendMail(
            @Param(name = "passwd") String password,
            @Param(name = "subject") String subject,
            @Param(name = "text") String text,
            @Param(name = "attach", template = FILE_LIST_SNIPPET) File attachments[],
            @Param(name = "debug", defaultValue = "false") boolean debug) throws IOException {
        return sendMail(null, null, password, null, null, subject, text, attachments, debug);
    }

    /**
     * <p>Send an E-mail of the specified subject and text to one or
     * more recipients through an SMTP server. The method provides
     * access to functionality of the {@doc.cmd sendmail} scripting language command.
     * The method works correctly only if there are valid default values of
     * the SMTP server, port and user, sender and recipient address(es) in the
     * user preferences. If any of these values is missing, the method throws an
     * {@link IllegalArgumentException}.</p>
     *
     * <p>As this is just a convenience method calling
     * the {@link #sendMail(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.io.File[], boolean)} one,
     * see its documentation for a complete description.</p>
     *
     * @param password optional password for authentication to the SMTP server.
     * @param subject E-mail subject. It should be just plain text without any new line characters.
     * @param text message body. To insert a new line character use "\n". For example,
     * a text of "test\ntest" will resolve in two lines containing word "test".
     * If the text starts with "<html>" (not case sensitive), the content type is
     * set to "text/html". Otherwise the mail body is sent as plain text ("text/plain").
     * @return command exit code as is specified in the {@doc.cmd sendmail} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "sendmail")
    public int sendMail(
            @Param(name = "passwd") String password,
            @Param(name = "subject") String subject,
            @Param(name = "text") String text) throws IOException {
        return sendMail(null, null, password, null, null, subject, text, null, false);
    }

    /**
     * <p>Send an E-mail of the specified subject, text and optional file
     * attachments to one or more recipients through an SMTP server
     * which requires no authentication. The method provides
     * access to functionality of the {@doc.cmd sendmail} scripting language command.
     * It works correctly only if there are valid default values of
     * the SMTP server, port and user, sender and recipient address(es) in the
     * user preferences. If any of these values is missing, the method throws an
     * {@link IllegalArgumentException}.</p>
     *
     * <p>As this is just a convenience method calling
     * the {@link #sendMail(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.io.File[], boolean)} one,
     * see its documentation for a complete description.</p>
     *
     * @param subject E-mail subject. It should be just plain text without any new line characters.
     * @param text message body. To insert a new line character use "\n". For example,
     * a text of "test\ntest" will resolve in two lines containing word "test".
     * If the text starts with "<html>" (not case sensitive), the content type is
     * set to "text/html". Otherwise the mail body is sent as plain text ("text/plain").
     * @param attachments optional file attachments.
     * @return command exit code as is specified in the {@doc.cmd sendmail} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "sendmail")
    public int sendMail(
            @Param(name = "subject") String subject,
            @Param(name = "text") String text,
            @Param(name = "attach", template = FILE_LIST_SNIPPET) File attachments[]) throws IOException {
        return sendMail(null, null, null, null, null, subject, text, attachments, false);
    }

    /**
     * <p>Send an E-mail of the specified subject and text
     * to one or more recipients through an SMTP server
     * which requires no authentication. The method provides
     * access to functionality of the {@doc.cmd sendmail} scripting language command.
     * It works correctly only if there are valid default values of
     * the SMTP server, port and user, sender and recipient address(es) in the
     * user preferences. If any of these values is missing, the method throws an
     * {@link IllegalArgumentException}.</p>
     *
     * <p>As this is just a convenience method calling
     * the {@link #sendMail(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.io.File[], boolean)} one,
     * see its documentation for a complete description.</p>
     *
     * @param subject E-mail subject. It should be just plain text without any new line characters.
     * @param text message body. To insert a new line character use "\n". For example,
     * a text of "test\ntest" will resolve in two lines containing word "test".
     * If the text starts with "<html>" (not case sensitive), the content type is
     * set to "text/html". Otherwise the mail body is sent as plain text ("text/plain").
     * @return command exit code as is specified in the {@doc.cmd sendmail} specification.
     * @throws IOException when an I/O error occurs.
     */
    @Command(name = "sendmail")
    public int sendMail(@Param(name = "subject") String subject, @Param(name = "text") String text) throws IOException {
        return sendMail(null, null, null, null, null, subject, text, null, false);
    }

    // ================  EXEC Command  =========================================
    // Not commented, not annotated - private method
    private int exec(String command, Object out, int count, String wait) throws IOException {
        List l = new ArrayList();
        Map m = new HashMap();
        if (command != null) {
            l.add(command);
        }
        if (out != null) {
            l.add(ExecCommand.PARAM_OUTPUT_STREAM);
            m.put(ExecCommand.PARAM_OUTPUT_STREAM, out);
        }
        if (count >= 0) {
            l.add(ExecCommand.PARAM_COUNT);
            m.put(ExecCommand.PARAM_COUNT, count);
        }
        if (wait != null) {
            l.add(ExecCommand.PARAM_WAIT);
            m.put(ExecCommand.PARAM_WAIT, wait);
        }
        return runScriptCommand("exec", l, m);
    }

    /**
     * Execute a command of the local operating system. The method provides access
     * to functionality of the {@doc.cmd exec} scripting language command.
     *
     * @param command a command to be executed. On most systems the commands
     * have a few limitations, such as no redirections and pipes are allowed. See Java
     * documentation on {@code Runtime.exec()} for more information.
     * @param out an output stream to write the command output to. If the argument
     * is null, the output is thrown away.
     * @param count how many times to execute the command. Default value is 1;
     * @param wait how long to wait after the command finishes. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd exec} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "exec")
    public int exec(
            @Param(name = "argument") String command,
            @Param(name = "outfile", template = OUTPUT_FILE_STREAM_SNIPPET) OutputStream out,
            @Param(name = "count") int count,
            @Param(name = "wait") String wait) throws IOException {
        return exec(command, (Object) out, count, wait);
    }

    /**
     * Convenience method employing the {@link #exec(java.lang.String, java.io.OutputStream, int, java.lang.String)} method
     * to execute a command once (<code>count=1</code>) with no waiting period specified (<code>wait=null</code>).
     *
     * @param command a command to be executed. On Unix/Linux systems the commands
     * have a few limitations. No redirections and pipes are allowed. See Java
     * documentation on {@code Runtime.exec()} for more information.
     * @param out an output stream to write the command output to. If the argument
     * is null, the output is thrown away.
     * @return command exit code as is specified in the {@doc.cmd exec} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "exec")
    public int exec(@Param(name = "argument") String command, @Param(name = "outfile", template = OUTPUT_FILE_STREAM_SNIPPET) OutputStream out) throws IOException {
        return exec(command, (Object) out, 1, null);
    }

    /**
     * Convenience method employing the {@link #exec(java.lang.String, java.io.OutputStream, int, java.lang.String)} method
     * with the command output redirected to a <code>Writer</code> rather than
     * to an <code>OutputStream</code>.
     *
     * @param command a command to be executed. On Unix/Linux systems the commands
     * have a few limitations. No redirections and pipes are allowed. See Java
     * documentation on {@code Runtime.exec()} for more information.
     * @param out a writer to write the command console output to. If the argument
     * is null, the output is thrown away.
     * @param count how many times to execute the command. Default value is 1;
     * @param wait how long to wait after the command finishes. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd exec} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int exec(String command, Writer out, int count, String wait) throws IOException {
        return exec(command, (Object) out, count, wait);
    }

    /**
     * Convenience method employing the {@link #exec(java.lang.String, java.io.Writer, int, java.lang.String)} method
     * to execute a command once (<code>count=1</code>) with no waiting period specified (<code>wait=null</code>).
     *
     * @param command a command to be executed. On Unix/Linux systems the commands
     * have a few limitations. No redirections and pipes are allowed. See Java
     * documentation on {@code Runtime.exec()} for more information.
     * @param out a writer to write the command console output to. If the argument
     * is null, the output is thrown away.
     * @return command exit code as is specified in the {@doc.cmd exec} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    public int exec(String command, Writer out) throws IOException {
        return exec(command, (Object) out, 1, null);
    }

    /**
     * Convenience method employing the {@link #exec(java.lang.String, java.io.OutputStream, int, java.lang.String)} method
     * to execute a command once (<code>count=1</code>) with no output stream (<code>out=null</code>)
     * and no waiting period (<code>wait=null</code>).
     *
     * @param command a command to be executed. On Unix/Linux systems the commands
     * have a few limitations. No redirections and pipes are allowed. See Java
     * documentation on {@code Runtime.exec()} for more information.
     * @param wait how long to wait after the command finishes. The argument must
     * be in format specified by the {@doc.cmd timevalues Time Values} chapter of the
     * language specification. Examples are "1" (a milisecond), "1s" (a second), "1m" (a minute),
     * "1h" (an hour) and "1d" (a day). If the argument is null, there will be no delay and
     * the test script proceeds immediately to the next command.
     * @return command exit code as is specified in the {@doc.cmd exec} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "exec")
    public int exec(@Param(name = "argument") String command, @Param(name = "wait") String wait) throws IOException {
        return exec(command, (Object) null, 1, wait);
    }

    /**
     * Convenience method employing the {@link #exec(java.lang.String, java.io.OutputStream, int, java.lang.String)}
     * method to execute a command without any further options.
     *
     * @param command a command to be executed. On Unix/Linux systems the commands
     * have a few limitations. No redirections and pipes are allowed. See Java
     * documentation on {@code Runtime.exec()} for more information.
     * @return command exit code as is specified in the {@doc.cmd exec} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "exec")
    public int exec(@Param(name = "argument") String command) throws IOException {
        return exec(command, (Object) null, 1, null);
    }

    /**
     * <p>A convenience method to open a web browser on the local operating system.
     * The method makes several attempts to locate and most often used browsers
     * and starts the first one found. The code is optimized to open the default system
     * browser on Windows, Mac OS and Ubuntu. On other systems the method tries to run typical
     * start comands of the most common web browsers according to a hardcoded list
     * and finishes when one of the commands reports success or the list is exhausted. </p>
     *
     * <p>Note that the method does not use any facilities provided by the Exec command
     * and its behavior can not be modified by any settings applicable to Exec.</p>
     * @param url a document URL to open.
     * @return 0 (zero) on success. Any other value indicates that the method failed
     * to open the browser.
     */
    public int execBrowser(String url) {
        return Utils.execOpenURL(url);
    }


    // ================  REPORT Command  =======================================
    // This is likely to be enhanced in the future

    // TODO: Other outputs, e.g.to PDF, XML, Word, Excel
    private int report(Object out, String provider, String description, String scope) throws IOException {
        List l = new ArrayList();
        Map m = new HashMap();
        if (out != null) {
            l.add(out);
        }
        if (provider != null) {
            l.add(ReportCommand.PARAM_PROVIDER);
            m.put(ReportCommand.PARAM_PROVIDER, provider);
        }
        if (description != null) {
            l.add(ReportCommand.PARAM_DESC);
            m.put(ReportCommand.PARAM_DESC, description);
        }
        if (scope != null) {
            l.add(ReportCommand.PARAM_SCOPE);
            m.put(ReportCommand.PARAM_SCOPE, scope);
        }
        return runScriptCommand("report", l, m);
    }

    /**
     * <p>Start the specified report provider to generate a report on test script
     * execution. The method provides access
     * to functionality of the {@doc.cmd report} scripting language command.</p>
     *
     * <p>Report providers are independent threads which typically register with 
     * the script manager to receive script execution events. Their role is usually 
     * to create and maintain a report with the outputs, such as remote desktop 
     * screenshots, image comparison results, warnings and execution logs. The report 
     * format is completely up to the particular provider
     * implementation. As the report provider interface {@link ReportProvider} is
     * exposed through the plugin framework, anyone may write a custom provider
     * and plug it into the tool.</p>
     *
     * <p>Note that report scope is obsolete and will be removed
     * in the future. It is provided here only for backward compatibility with
     * test scripts prior to v2.0.</p>
     *
     * @param out a file to save the report to.
     * @param provider a report provider code. It must be a valid code returned
     * by the {@link Plugin#getCode()} method of one of the installed report
     * provider plugins or null to use the default report provider
     * (configurable through preferences). {@product.name} provides by default
     * just one simple HTML report provider with the "default" code.
     *
     * @param description report description.
     * @param scope report scope as is defined in the {@doc.cmd report} command specification.
     * @return command exit code as is specified in the {@doc.cmd report} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "report")
    public int report(
            @Param(name = "argument") File out,
            @Param(name = "provider") String provider,
            @Param(name = "desc") String description,
            @Param(name = "scope") String scope) throws IOException {
        return report((Object) out, provider, description, scope);
    }

    /**
     * <p>Start the specified report provider to generate a report on test script
     * execution. The method provides access
     * to functionality of the {@doc.cmd report} scripting language command.</p>
     *
     * <p>Report providers are independent threads which typically register with
     * the script manager to receive script execution events. Their role is usually
     * to create and maintain a report with the outputs, such as remote desktop
     * screenshots, image comparison results, warnings and execution logs. The report
     * format is completely up to the particular provider
     * implementation. As the report provider interface {@link ReportProvider} is
     * exposed through the plugin framework, anyone may write a custom provider
     * and plug it into the tool.</p>
     *
     * @param out a file to save the report to.
     * @param provider a report provider code. It must be a valid code returned
     * by the {@link Plugin#getCode()} method of one of the installed report
     * provider plugins or null to use the default report provider
     * (configurable through preferences). {@product.name} provides by default
     * just one simple HTML report provider with the "default" code.
     *
     * @param description report description.
     * @return command exit code as is specified in the {@doc.cmd report} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "report")
    public int report(@Param(name = "argument") File out,
            @Param(name = "provider") String provider,
            @Param(name = "desc") String description) throws IOException {
        return report((Object) out, provider, description, null);
    }

    /**
     * <p>Start the default report provider to generate a report on test script
     * execution. The method provides access
     * to functionality of the {@doc.cmd report} scripting language command.</p>
     *
     * <p>Report providers are independent threads which typically register with
     * the script manager to receive script execution events. Their role is usually
     * to create and maintain a report with the outputs, such as remote desktop
     * screenshots, image comparison results, warnings and execution logs. The report
     * format is completely up to the particular provider
     * implementation. As the report provider interface {@link ReportProvider} is
     * exposed through the plugin framework, anyone may write a custom provider
     * and plug it into the tool.</p>
     *
     * <p>One of the installed report providers is always set as the default one.
     * Unless user changes the default provider in preferences, this method
     * starts the only built-in HTML report provider with the "default" code.</p>
     *
     * @param out a file to save the report to.
     *
     * @param description report description.
     * @return command exit code as is specified in the {@doc.cmd report} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "report")
    public int report(@Param(name = "argument") File out, @Param(name = "desc") String description) throws IOException {
        return report((Object) out, null, description, null);
    }

    /**
     * <p>Start the default report provider to generate a report on test script
     * execution. The method provides access
     * to functionality of the {@doc.cmd report} scripting language command.</p>
     *
     * <p>Report providers are independent threads which typically register with
     * the script manager to receive script execution events. Their role is usually
     * to create and maintain a report with the outputs, such as remote desktop
     * screenshots, image comparison results, warnings and execution logs. The report
     * format is completely up to the particular provider
     * implementation. As the report provider interface {@link ReportProvider} is
     * exposed through the plugin framework, anyone may write a custom provider
     * and plug it into the tool.</p>
     *
     * <p>One of the installed report providers is always set as the default one.
     * Unless user changes the default provider in preferences, this method
     * starts the only built-in HTML report provider with the "default" code.</p>
     *
     * @param out a file to save the report to.
     *
     * @return command exit code as is specified in the {@doc.cmd report} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "report")
    public int report(@Param(name = "argument") File out) throws IOException {
        return report((Object) out, null, null, null);
    }

    // ================  CompareTo Command  =======================================
    private int compareTo(Object templates, Object module, String methodParams, Float passRate, Rectangle cmpArea) throws IOException {
        List l = new ArrayList();
        Map m = new HashMap();
        if (templates != null) {
            if (templates instanceof File[]) {
                List<File> lf = new ArrayList();
                for (File f : (File[]) templates) {
                    lf.add(f);
                }
                l.add(lf);
            } else {
                l.add(templates);
            }
        }
        if (module != null) {
            l.add(CompareToCommand.PARAM_METHOD);
            m.put(CompareToCommand.PARAM_METHOD, module);
        }
        if (methodParams != null) {
            l.add(CompareToCommand.PARAM_METHODPARAMS);
            m.put(CompareToCommand.PARAM_METHODPARAMS, methodParams);
        }
        if (passRate != null && passRate >= 0) {
            l.add(CompareToCommand.PARAM_PASSRATE);
            m.put(CompareToCommand.PARAM_PASSRATE, passRate);
        }
        if (cmpArea != null) {
            l.add(CompareToCommand.PARAM_CMPAREA);
            m.put(CompareToCommand.PARAM_CMPAREA, cmpArea);
        }
        return runScriptCommand("compareTo", l, m);
    }

    /**
     * <p>Compare the current remote desktop image to one or more template images
     * using the specified image comparison module instance, numeric pass rate
     * and an optional comparison rectangle. The method provides access
     * to functionality of the {@doc.cmd compareto} scripting language command.</p>
     *
     * <p>Result of image comparison is typically indicated by the return value.
     * where 0 usually means success, 1 (one) indicates negative result
     * and higher values indicate an error (for example, image file not found etc.).
     * See the <code>Compareto</code> command specification for the list of error
     * codes.</p>
     *
     * <p>Image comparison modules (methods) may save additional results to
     * the context. Refer to the CompareTo command specification for details.
     * For example, the "search" module stores coordinates of
     * the match areas both to the variable table as well to a list stored
     * in the context. To retrieve them use either of the following ways:</p>
     * <blockquote>
     * <pre>
     *   // 1. Getting x, y from variables
     *   Integer x = (Integer)getContext().getVariable("_SEARCH_X");
     *   Integer y = (Integer)getContext().getVariable("_SEARCH_Y");
     *
     *   // 2. Getting x, y from context's coordinate list
     *   Point p = getContext().getSearchHits().get(0);
     *   x = p.x;
     *   y = p.y;
     * </pre>
     * </blockquote>
     *
     * @param templates list of template images (Image instances).
     * @param module image comparison method (module) name (either "default", "search"
     * or third party image comparison plugin code).
     * @param passRate pass rate between 0 and 1. The value of 1.0 indicates that
     * exact match is required (100% matching)..
     * @param cmpArea comparison area (sub rectangle) in the target image to apply
     * the comparison to. If it is null the full image will be processed.
     * @return command exit code as is specified in the {@doc.cmd compareto} specification.
     * @throws java.io.IOException on an I/O error (can't read file and/or
     * can't get image of the remote desktop).
     */
    public int compareTo(Image templates[], ImageComparisonModule module, float passRate, Rectangle cmpArea) throws IOException {
        return compareTo(templates, module, null, passRate, cmpArea);
    }

    @Command(name = "compareto")
    public int compareTo(
            @Param(name = "argument", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "method") String method,
            @Param(name = "methodparams") String moduleParams,
            @Param(name = "passrate") float passRate,
            @Param(name = "cmparea") Rectangle cmpArea) throws IOException {
        return compareTo(templates, method, moduleParams, passRate, cmpArea);
    }

    @Command(name = "compareto")
    public int compareTo(@Param(name = "argument", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "method") String method,
            @Param(name = "passrate") float passRate,
            @Param(name = "cmparea") Rectangle cmpArea) throws IOException {
        return compareTo((Object) templates, (Object) method, null, passRate, cmpArea);
    }

    @Command(name = "compareto")
    public int compareTo(@Param(name = "argument", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "method") String method,
            @Param(name = "passrate") float passRate) throws IOException {
        return compareTo((Object) templates, (Object) method, null, passRate, null);
    }

    @Command(name = "compareto")
    public int compareTo(@Param(name = "argument", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate) throws IOException {
        return compareTo((Object) templates, (Object) null, null, passRate, null);
    }

    @Command(name = "compareto")
    public int compareTo(@Param(name = "argument", template = FILE_LIST_SNIPPET) File template[],
            @Param(name = "method") String method) throws IOException {
        return compareTo((Object) template, (Object) method, null, null, null);
    }

    @Command(name = "compareto")
    public int compareTo(@Param(name = "argument", template = FILE_LIST_SNIPPET) File templates[]) throws IOException {
        return compareTo((Object) templates, (Object) null, null, null, null);
    }

    // TODO: offline commands should not throw IOException - change methods

    // ================  Pause Command  =======================================
    /**
     * <p>Pause execution of the test script until the user manually resumes it with 
     * an optional description for report providers. The method provides access
     * to functionality of the {@doc.cmd pause} scripting language command.</p>
     *
     * <p>The pausing functionality should not be confused with the {@doc.cmd wait}
     * command and its corresponding Java method {@link #wait(java.lang.String)}.
     * While the Wait command takes the time specified to complete and delays the
     * script execution this way, the Pause one sets the pause flag of the test
     * script interpret and lets the scripting framework handle it.</p>
     * 
     * <p>When the method gets called, it fires an internal event ({@link ScriptEvent#SCRIPT_EXECUTION_PAUSED})
     * to all registered listeners. The GUI (if present) typically updates status
     * of the associated controls ("pause" menu item and tool bar button) and
     * highlights the pause command line in the script editor. If the script is
     * being executed in CLI mode, the tool prints out a pause warning message
     * into the console. In both cases the user has to resume the script execution
     * manually either through deselection of the GUI controls or a key press in CLI.
     * To resume programatically call the {@link TestScriptInterpret#setPause(java.lang.Object, boolean, java.lang.String)}
     * method with the pause argument set to <code>false</code>. This functionality
     * is however not visible to the proprietary test scripts.</p>
     *
     * @param reason an optional description of why the script was paused. It may be picked
     * up by report providers to display it in the report.
     * @return command exit code as is specified in the {@doc.cmd pause} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "pause")
    public int pause(@Param(name = "argument") String reason) throws IOException {
        List l = new ArrayList();
        Map m = new HashMap();
        if (reason != null) {
            l.add(reason);
        }
        return runScriptCommand("pause", l, m);
    }

    /**
     * <p>Pause execution of the test script until the user manually resumes it.
     * The method provides access to functionality of the {@doc.cmd pause} 
     * scripting language command.</p>
     *
     * <p>The pausing functionality should not be confused with the {@doc.cmd wait}
     * command and its corresponding Java method {@link #wait(java.lang.String)}.
     * While the Wait command takes the time specified to complete and delays the
     * script execution this way, the Pause one sets the pause flag of the test
     * script interpret and lets the scripting framework handle it.</p>
     *
     * <p>When the method gets called, it fires an internal event ({@link ScriptEvent#SCRIPT_EXECUTION_PAUSED})
     * to all registered listeners. The GUI (if present) typically updates status
     * of the associated controls ("pause" menu item and tool bar button) and
     * highlights the pause command line in the script editor. If the script is
     * being executed in CLI mode, the tool prints out a pause warning message
     * into the console. In both cases the user has to resume the script execution
     * manually either through deselection of the GUI controls or a key press in CLI.
     * To resume programatically call the {@link TestScriptInterpret#setPause(java.lang.Object, boolean, java.lang.String)}
     * method with the pause argument set to <code>false</code>. This functionality
     * is however not visible to the proprietary test scripts.</p>
     *
     * @return command exit code as is specified in the {@doc.cmd pause} specification.
     * @throws java.io.IOException if an I/O error happens during client-server communication.
     */
    @Command(name = "pause")
    public int pause() throws IOException {
        return pause(null);
    }

    // ================  Exit Command  =======================================
    /**
     * Exit the test script.
     * @param exitCode numeric exit code to be returned to the underlying
     * operating system.
     * @return exit code.
     * @throws java.io.IOException
     */

    // TODO: verify functionality
    @Command(name = "exit")
    public int exit(@Param(name = "argument") int exitCode) throws IOException {
        List l = new ArrayList();
        Map m = new HashMap();
        l.add(exitCode);
        return runScriptCommand("exit", l, m);
    }
    // ================  Warning Command  =====================================

    /**
     * <p>Add a warning to be picked up by a report provider.
     * The method provides access to functionality of the {@doc.cmd warning}
     * scripting language command.</p>
     *
     * <p>Like the {@link #screenshot(java.io.File, java.lang.String, java.awt.Rectangle, java.io.File[], java.lang.String, java.lang.String, float, java.awt.Rectangle) screenshot()}
     * method, each call of this method inserts a data structure
     * ({@link WarningCommand.WarningInfo} instance) with the warning text, date and
     * optional associated screenshot to a list in the context
     * (key {@link ScriptingContext#CONTEXT_OUTPUT_OBJECTS}).
     * It will also internally fire a specific event which notifies registered
     * listeners of the new screenshot (see {@link CommandEvent#OUTPUT_CHANGED_EVENT}).
     * This event may be picked up by any running report provider which may
     * follow up and refresh the report file.</p>
     *
     * @param text warning text.
     * @param image optional screenshot file name to associate with this warning.
     * The point is to give the report
     * provider a hint to display the warning close to the previously taken screenshot
     * so that a human may review the reported problem in the image. This argument should correspond
     * to file name argument of a previously executed {@link #screenshot(java.io.File, java.lang.String, java.awt.Rectangle, java.io.File[], java.lang.String, java.lang.String, float, java.awt.Rectangle) screenshot()}
     * method.
     * @return command exit code as is specified in the {@doc.cmd warning} specification.
     */
    @Command(name = "warning")
    public int warning(@Param(name = "argument") String text, @Param(name = "image") File image) {
        List l = new ArrayList();
        Map m = new HashMap();
        if (text != null) {
            l.add(text);
        }
        if (image != null) {
            l.add(WarningCommand.PARAM_IMAGE);
            m.put(WarningCommand.PARAM_IMAGE, Utils.getFullPath(image));
        }
        try {
            return runScriptCommand("warning", l, m);
        } catch (IOException ex) {
            // The Warning command should never throw the IOException because it
            // doesn't participate in the client/server communication.
            // This return statement should be never reached.
            return 1;
        }
    }

    /**
     * <p>Add a warning to be picked up by a report provider.
     * The method provides access to functionality of the {@doc.cmd warning}
     * scripting language command.</p>
     *
     * <p>Like the {@link #screenshot(java.io.File, java.lang.String, java.awt.Rectangle, java.io.File[], java.lang.String, java.lang.String, float, java.awt.Rectangle) screenshot()}
     * method, each call of this method inserts a data structure
     * ({@link WarningCommand.WarningInfo} instance) with the warning text, date and
     * optional associated screenshot to a list in the context
     * (key {@link ScriptingContext#CONTEXT_OUTPUT_OBJECTS}).
     * It will also internally fire a specific event which notifies registered
     * listeners of the new screenshot (see {@link CommandEvent#OUTPUT_CHANGED_EVENT}).
     * This event may be picked up by any running report provider which may
     * follow up and refresh the report file.</p>
     *
     * @param text warning text.
     * @return command exit code as is specified in the {@doc.cmd warning} specification.
     */
    @Command(name = "warning")
    public int warning(@Param(name = "argument") String text) throws IOException {
        return warning(text, null);
    }

    // ================  WAITFOR Command  =======================================
    private int waitFor(String event, Rectangle area, String extent, Boolean cumulative,
            Object templates, float passRate, String interval, String method, String methodParams,
            Rectangle cmpArea, int count, String timeout, String wait) throws IOException {
        List l = new ArrayList();
        Map m = new HashMap();
        if (event != null) {
            l.add(event);
        }
        if (count > 0) {
            l.add(AbstractCommandHandler.PARAM_COUNT);
            m.put(AbstractCommandHandler.PARAM_COUNT, count);
        }
        if (wait != null) {
            l.add(AbstractCommandHandler.PARAM_WAIT);
            m.put(AbstractCommandHandler.PARAM_WAIT, wait);
        }
        if (timeout != null) {
            l.add(WaitforCommand.PARAM_TIMEOUT);
            m.put(WaitforCommand.PARAM_TIMEOUT, timeout);
        }
        if (area != null) {
            l.add(WaitforCommand.PARAM_AREA);
            m.put(WaitforCommand.PARAM_AREA, area);
        }
        if (extent != null) {
            l.add(WaitforCommand.PARAM_EXTENT);
            m.put(WaitforCommand.PARAM_EXTENT, extent);
        }
        if (cumulative != null) {
            l.add(WaitforCommand.PARAM_CUMULATIVE);
            m.put(WaitforCommand.PARAM_CUMULATIVE, cumulative);
        }
        if (templates != null) {
            if (templates instanceof File[]) {
                List<File> lf = Arrays.asList((File[])templates);
                l.add(WaitforCommand.PARAM_TEMPLATE);
                m.put(WaitforCommand.PARAM_TEMPLATE, lf);
            } else {
                l.add(WaitforCommand.PARAM_TEMPLATE);
                m.put(WaitforCommand.PARAM_TEMPLATE, templates);
            }
        }
        if (passRate >= 0) {
            l.add(CompareToCommand.PARAM_PASSRATE);
            m.put(CompareToCommand.PARAM_PASSRATE, passRate);
        }
        if (interval != null) {
            l.add(WaitforCommand.PARAM_INTERVAL);
            m.put(WaitforCommand.PARAM_INTERVAL, interval);
        }
        if (method != null) {
            l.add(CompareToCommand.PARAM_METHOD);
            m.put(CompareToCommand.PARAM_METHOD, method);
        }
        if (methodParams != null) {
            l.add(CompareToCommand.PARAM_METHODPARAMS);
            m.put(CompareToCommand.PARAM_METHODPARAMS, methodParams);
        }
        if (cmpArea != null) {
            l.add(CompareToCommand.PARAM_CMPAREA);
            m.put(CompareToCommand.PARAM_CMPAREA, cmpArea);
        }
        return runScriptCommand("waitFor", l, m);
    }

    // ====== WAITFOR BELL
    @Command(name = "waitfor bell")
    public int waitForBell(
            @Param(name = "count") int count,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitFor(WaitforCommand.EVENT_BELL, null, null, null, null, -1f, null, null, null, null, count, timeout, wait);
    }

    @Command(name = "waitfor bell")
    public int waitForBell(
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitFor(WaitforCommand.EVENT_BELL, null, null, null, null, -1f, null, null, null, null, 1, timeout, wait);
    }

    @Command(name = "waitfor bell")
    public int waitForBell(@Param(name = "timeout") String timeout) throws IOException {
        return waitFor(WaitforCommand.EVENT_BELL, null, null, null, null, -1f, null, null, null, null, 1, timeout, null);
    }

    // ====== WAITFOR UPDATE
    @Command(name = "waitfor update")
    public int waitForUpdate(@Param(name = "area") Rectangle area,
            @Param(name = "extent") String extent,
            @Param(name = "cumulative", defaultValue = "false") boolean cumulative,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitFor(WaitforCommand.EVENT_UPDATE, area, extent, cumulative, null, -1f, null, null, null, null, -1, timeout, wait);
    }

    @Command(name = "waitfor update")
    public int waitForUpdate(@Param(name = "area") Rectangle area, @Param(name = "extent") String extent, @Param(name = "timeout") String timeout, @Param(name = "wait") String wait) throws IOException {
        return waitFor(WaitforCommand.EVENT_UPDATE, area, extent, null, null, -1f, null, null, null, null, -1, timeout, wait);
    }

    @Command(name = "waitfor update")
    public int waitForUpdate(@Param(name = "area") Rectangle area, @Param(name = "extent") String extent, @Param(name = "timeout") String timeout) throws IOException {
        return waitFor(WaitforCommand.EVENT_UPDATE, area, extent, null, null, -1f, null, null, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor update")
    public int waitForUpdate(@Param(name = "extent") String extent, @Param(name = "timeout") String timeout) throws IOException {
        return waitFor(WaitforCommand.EVENT_UPDATE, null, extent, null, null, -1f, null, null, null, null, -1, timeout, null);
    }

    // ====== WAITFOR MATCH
    public int waitForMatch(Image templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "interval") String interval,
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitFor(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, interval, method, methodParams, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor match")
    public int waitForMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "interval") String interval,
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitFor(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, interval, method, methodParams, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor match")
    public int waitForMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "method") String method,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitFor(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, null, method, null, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor match")
    public int waitForMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "method") String method,
            @Param(name = "timeout") String timeout) throws IOException {
        return waitFor(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, null, method, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor match")
    public int waitForMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "method") String method,
            @Param(name = "timeout") String timeout) throws IOException {
        return waitFor(WaitforCommand.EVENT_MATCH, null, null, null, templates, -1f, null, method, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor match")
    public int waitForMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "timeout") String timeout) throws IOException {
        return waitFor(WaitforCommand.EVENT_MATCH, null, null, null, templates, -1f, null, null, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor match")
    public int waitForMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[]) throws IOException {
        return waitFor(WaitforCommand.EVENT_MATCH, null, null, null, templates, -1f, null, null, null, null, -1, null, null);
    }

    // ====== WAITFOR MISMATCH
    public int waitForMismatch(Image templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "interval") String interval,
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitFor(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, passRate, interval, method, methodParams, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "interval") String interval,
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitFor(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, passRate, interval, method, methodParams, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "method") String method,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitFor(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, passRate, null, method, null, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "method") String method,
            @Param(name = "timeout") String timeout) throws IOException {
        return waitFor(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, passRate, null, method, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "method") String method,
            @Param(name = "timeout") String timeout) throws IOException {
        return waitFor(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, -1f, null, method, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "timeout") String timeout) throws IOException {
        return waitFor(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, -1f, null, null, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[]) throws IOException {
        return waitFor(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, -1f, null, null, null, null, -1, null, null);
    }

    // ====== WAITFOR CLIPBOARD
    @Command(name = "waitfor clipboard")
    public int waitForClipboard(@Param(name = "timeout") String timeout, @Param(name = "wait") String wait) throws IOException {
        return waitFor(WaitforCommand.EVENT_CLIPBOARD, null, null, null, null, -1f, null, null, null, null, -1, timeout, wait);
    }

    @Command(name = "waitfor clipboard")
    public int waitForClipboard(@Param(name = "timeout") String timeout) throws IOException {
        return waitFor(WaitforCommand.EVENT_CLIPBOARD, null, null, null, null, -1f, null, null, null, null, -1, timeout, null);
    }
}
