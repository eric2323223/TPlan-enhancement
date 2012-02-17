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

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.plugin.DependencyMissingException;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.util.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

/**
 *
 * @author robert
 */
public abstract class AbstractTestScriptInterpret implements TestScriptInterpret, Plugin, DocumentListener {

    protected URI uri = null;
    protected ScriptManager scriptManager = null;
    protected ScriptingContext compilationContext;
    protected ScriptingContext executionContext;
    protected boolean executing = false;
    protected boolean modified = false;
    protected boolean manualStop = false;
    protected String manualStopJustification = null;
    protected boolean doNotFireEvents = false;
    protected final static String PFLAG = "PFLAG";
    protected final static String SFLAG = "SFLAG";

    protected AbstractTestScriptInterpret() {
        getDocument().addDocumentListener(this);
    }

    public URI getURI() {
        return uri;
    }

    public void setURI(URI uri, boolean load) throws IOException {
        if (this.uri == null || !this.uri.equals(uri)) {
            this.uri = uri;
            if (load) {
                File f = new File(uri);
                String s = readFile(f);
                StyledDocument doc = getDocument();
                doc.removeDocumentListener(this);
                doc.addDocumentListener(this);
                setModified(false);
                try {
                    doc.insertString(0, s, null);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                    throw new IOException("Failed to load script text to the document", ex);
                }
            }
        }
    }

    protected void setModified(boolean modified) {
        this.modified = modified;
    }

    private String readFile(File file) throws IOException {
        // Bug 2941550 fix: when reading the file, force the UTF-8 encoding.
        InputStreamReader in = new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));
        BufferedReader reader = new BufferedReader(in);
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line + "\n");
        }
        return stringBuilder.toString();
    }

    public void setScriptManager(ScriptManager sm) {
        this.scriptManager = sm;
    }

    public String getCode() {
        return "" + getType();
    }

    public String getVendorName() {
        return ApplicationSupport.APPLICATION_NAME;
    }

    public String getSupportContact() {
        return ApplicationSupport.APPLICATION_SUPPORT_CONTACT;
    }

    public int[] getVersion() {
        return Utils.getVersion();
    }

    public Class getImplementedInterface() {
        return TestScriptInterpret.class;
    }

    public boolean requiresRestart() {
        return true;
    }

    public String getVendorHomePage() {
        return ApplicationSupport.APPLICATION_HOME_PAGE;
    }

    public java.util.Date getDate() {
        return Utils.getReleaseDate();
    }

    public int[] getLowestSupportedVersion() {
        return Utils.getVersion();
    }

    public String getMessageBeforeInstall() {
        return null;
    }

    public String getMessageAfterInstall() {
        return null;
    }

    /**
     * Check whether all dependencies are installed.
     *
     * @param manager plugin manager instance.
     * @throws com.tplan.robot.plugin.DependencyMissingException when one or more required dependencies is not installed.
     */
    public void checkDependencies(PluginManager manager) throws DependencyMissingException {
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public ScriptingContext getExecutionContext() {
        return executionContext;
    }

    public ScriptingContext getCompilationContext() {
        return compilationContext;
    }

    public boolean isExecuting() {
        return executing;
    }

    public boolean isStop() {
        Boolean b = executionContext != null ? (Boolean) executionContext.get(SFLAG) : null;
        return b != null && b;
    }

    public boolean isManualStop() {
        return manualStop;
    }

    public void setStop(Object source, boolean stop, boolean isManual, String manualStopJustification) {
        if (executionContext != null) {
            Boolean b = (Boolean) executionContext.get(SFLAG);
            boolean old = b != null ? b : false;
            if (old != stop) {
                executionContext.put(SFLAG, stop);
                this.manualStop = isManual;
                setPause(source, false, "Script stopped");
            }
        }
    }

    public boolean isPause() {
        Boolean b = executionContext != null ? (Boolean) executionContext.get(PFLAG) : null;
        return b != null && b;
    }

    public void setPause(Object source, boolean pause, String reason) {
        if (executionContext != null) {
            Boolean b = (Boolean) executionContext.get(PFLAG);
            boolean old = b != null ? b : false;
            if (old != pause) {
                executionContext.put(PFLAG, pause);
                if (reason == null) {
                    executionContext.remove("CONTEXT_PAUSE_REASON");
                } else {
                    executionContext.put("CONTEXT_PAUSE_REASON", reason);
                }
                if (!doNotFireEvents) {
                    int type = pause
                            ? ScriptEvent.SCRIPT_EXECUTION_PAUSED
                            : ScriptEvent.SCRIPT_EXECUTION_RESUMED;
                    Object src = executionContext.getMasterWrapper() == null ? this : executionContext.getMasterWrapper();
                    ScriptEvent evt = new ScriptEvent(src, this, executionContext, type);
                    evt.setCustomObject(reason);
                    scriptManager.fireScriptEvent(evt);
                }
            }
        }
    }

    /**
     * Gives notification that there was an insert into the document.  The
     * range given by the DocumentEvent bounds the freshly inserted region.
     *
     * @param e the document event
     */
    public void insertUpdate(DocumentEvent e) {
        setModified(true);
    }

    /**
     * Gives notification that a portion of the document has been
     * removed.  The range is given in terms of what the view last
     * saw (that is, before updating sticky positions).
     *
     * @param e the document event
     */
    public void removeUpdate(DocumentEvent e) {
        setModified(true);
    }

    /**
     * Gives notification that an attribute or set of attributes changed.
     *
     * @param e the document event
     */
    public void changedUpdate(DocumentEvent e) {
        setModified(true);
    }

    public void destroy() {
        if (scriptManager != null) {
            scriptManager.removeInterpret(this);
            if (!doNotFireEvents) {
                scriptManager.fireScriptEvent(new ScriptEvent(this, null, ScriptEvent.SCRIPT_INTERPRET_DESTROYED));
            }
            scriptManager = null;
        }
        if (executionContext != null) {
            executionContext.dispose();
        }
        if (compilationContext != null) {
            compilationContext.dispose();
        }
    }
}
