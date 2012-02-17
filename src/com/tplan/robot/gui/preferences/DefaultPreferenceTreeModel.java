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
package com.tplan.robot.gui.preferences;

import com.tplan.robot.preferences.Preference;
import com.tplan.robot.preferences.ConfigurationKeys;
import com.tplan.robot.gui.preferences.styles.StylePanel;
import com.tplan.robot.gui.MainFrame;
import com.tplan.robot.remoteclient.rfb.RfbConstants;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.ApplicationSupport;

import com.tplan.robot.preferences.Configurable;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.plugin.PluginInfo;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.scripting.JavaTestScriptConverterFactory;
import java.awt.Frame;
import java.text.MessageFormat;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.*;

/**
 * <p>Tree model for the user preferences tree displayed by the {@link PreferenceDialog Preferences} window.
 * Here is defined which nodes the tree will contain and which panel will be displayed when such a node gets selected.
 * @product.signature
 */
public class DefaultPreferenceTreeModel extends DefaultTreeModel implements ConfigurationKeys, RfbConstants {

    public DefaultPreferenceTreeModel(ScriptManager scriptHandler, MainFrame mainFrame) {
        super(new PreferenceTreeNodeImpl(null, ApplicationSupport.getResourceBundle().getString("com.tplan.robot.gui.options.rootPreferencesNode")));
        init(mainFrame);
    }

    /**
     * Initialize the tree structure, add all necessary tree nodes and
     * dependent components (panels).
     */
    private void init(MainFrame mainFrame) {
        DefaultMutableTreeNode preferenceRoot = (DefaultMutableTreeNode) getRoot();
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        PreferenceTreeNodeImpl node;
        PreferenceTreeNodeImpl secondRoot;
        PreferenceTreeNodeImpl thirdRoot;
        DefaultPreferencePanel component;
        Preference o;

        secondRoot = new PreferenceTreeNodeImpl(null, res.getString("com.tplan.robot.gui.options.appearanceNode"));
        preferenceRoot.add(secondRoot);

// -----------------------------
// Menu config panel
// -----------------------------
        component = new DefaultPreferencePanel();
        component.createContainer(res.getString("options.menuToolbarNode.containerFileMenuShortcutKeys"));

        o = new Preference("menu.NewShortcut", Preference.TYPE_KEYSTROKE,
                res.getString("options.menuToolbarNode.newTestScript"),
                res.getString("options.menuToolbarNode.newTestScriptDesc"));
        component.addPreference(o, 0);
        o = new Preference("menu.OpenShortcut", Preference.TYPE_KEYSTROKE, res.getString("options.menuToolbarNode.OpenTestScript"), null);
        component.addPreference(o, 0);
        o = new Preference("menu.SaveShortcut", Preference.TYPE_KEYSTROKE, res.getString("options.menuToolbarNode.SaveTestScript"), null);
        component.addPreference(o, 0);
        o = new Preference("menu.CloseShortcut", Preference.TYPE_KEYSTROKE, res.getString("options.menuToolbarNode.CloseTestScript"), null);
        component.addPreference(o, 0);

        component.createContainer(res.getString("options.menuToolbarNode.containerScriptBuildingShortcutKeys"));

        o = new Preference("menu.RecordShortcut", Preference.TYPE_KEYSTROKE, res.getString("options.menuToolbarNode.record"), null);
        component.addPreference(o, 1);
        o = new Preference("menu.ScreenshotShortcut", Preference.TYPE_KEYSTROKE, res.getString("options.menuToolbarNode.ScreenshotCommand"), null);
        component.addPreference(o, 1);
        o = new Preference("menu.ComparetoShortcut", Preference.TYPE_KEYSTROKE, res.getString("options.menuToolbarNode.CompareToCommand"), null);
        component.addPreference(o, 1);
        o = new Preference("menu.WaitforShortcut", Preference.TYPE_KEYSTROKE, res.getString("options.menuToolbarNode.WaitforCommand"), null);
        component.addPreference(o, 1);

        o = new Preference("menu.enableRecordingKeysInDesktop", Preference.TYPE_BOOLEAN,
                res.getString("options.menuToolbarNode.enableKeysInVNC"),
                res.getString("options.menuToolbarNode.enableKeysInVNCDesc"));
        component.addPreference(o, 1);

        component.createContainer(res.getString("options.menuToolbarNode.ScriptExecutionShortcutKeys"));

        o = new Preference("menu.RunShortcut", Preference.TYPE_KEYSTROKE, res.getString("options.menuToolbarNode.Run"), null);
        component.addPreference(o, 2);
        o = new Preference("menu.RunSelectionShortcut", Preference.TYPE_KEYSTROKE, res.getString("options.menuToolbarNode.RunSelection"), null);
        component.addPreference(o, 2);
        o = new Preference("menu.PauseShortcut", Preference.TYPE_KEYSTROKE, res.getString("options.menuToolbarNode.Pause"), null);
        component.addPreference(o, 2);
        o = new Preference("menu.StopShortcut", Preference.TYPE_KEYSTROKE, res.getString("options.menuToolbarNode.Stop"), null);
        component.addPreference(o, 2);

        component.createContainer(res.getString("options.appearanceNode.ToolBarLocation"));

        o = new Preference("ui.mainframe.toolbarLocation", Preference.TYPE_STRING, res.getString("options.appearanceNode.containerToolBarLocation"),
                res.getString("options.appearanceNode.ToolBarLocationDesc"));
        o.setSelectOnly(true);
        String values[] = {
            res.getString("options.appearanceNode.toolbarLocationNorth"),
            res.getString("options.appearanceNode.toolbarLocationSouth"),
            res.getString("options.appearanceNode.toolbarLocationEast"),
            res.getString("options.appearanceNode.toolbarLocationWest")
        };
        String displayValues[] = {
            res.getString("options.appearanceNode.toolbarLocationTop"),
            res.getString("options.appearanceNode.toolbarLocationBottom"),
            res.getString("options.appearanceNode.toolbarLocationLeft"),
            res.getString("options.appearanceNode.toolbarLocationRight")
        };
        o.setDisplayValues(new Vector(Arrays.asList(displayValues)));
        o.setValues(new Vector(Arrays.asList(values)));
        component.addPreference(o, 3);

        component.init();

        node = new PreferenceTreeNodeImpl(component, res.getString("options.menuToolbarNode.name"));
        secondRoot.add(node);

// -----------------------------
// Editor config panel
// -----------------------------
        component = new DefaultPreferencePanel();
        component.createContainer(res.getString("options.editor.editorColors"));

        o = new Preference("ui.editor.executedLineColor", Preference.TYPE_COLOR,
                res.getString("options.editor.executedLineColor"),
                null);
        component.addPreference(o, 0);
        o = new Preference("ui.editor.breakPointColor", Preference.TYPE_COLOR,
                res.getString("options.editor.breakpointLineColor"),
                null);
        component.addPreference(o, 0);
        o = new Preference("ui.editor.syntaxErrorColor", Preference.TYPE_COLOR,
                res.getString("options.editor.syntaxErrorLineColor"),
                null);
        component.addPreference(o, 0);

        component.createContainer(res.getString("options.editor.editorStyles"));
        Vector v = new Vector();
        v.add(res.getString("options.editor.comments"));
        v.add(res.getString("options.editor.commandNames"));
        v.add(res.getString("options.editor.commandArguments"));
        v.add(res.getString("options.editor.parameterNames"));
        v.add(res.getString("options.editor.parameterValues"));
        v.add(res.getString("options.editor.keywords"));

        AbstractPreferencePanel stylesComponent = new StylePanel(v, "ui.editor.style", mainFrame);
        component.addComponent(stylesComponent, 1);

        component.createContainer(res.getString("options.editor.editorShortcutKeys"));
        o = new Preference("ui.editor.contextMenuShortCut", Preference.TYPE_KEYSTROKE,
                res.getString("options.editor.editorContextMenuShortcutKey"),
                res.getString("options.editor.editorContextMenuShortcutKeyText"));
        component.addPreference(o, 2);
        o = new Preference("ui.editor.commandListShortCut", Preference.TYPE_KEYSTROKE,
                res.getString("options.editor.commandWizardShortcutKey"),
                null);
        component.addPreference(o, 2);
        o = new Preference("ui.editor.snippetShortCut", Preference.TYPE_KEYSTROKE,
                res.getString("options.editor.templateWizardShortcutKey"),
                null);
        component.addPreference(o, 2);

        o = new Preference("ui.editor.autoConvertToUpperCase", Preference.TYPE_BOOLEAN,
                res.getString("options.editor.autoConvertToUpperCase"),
                null);
        component.addPreference(o, 2);

        component.init();

        node = new PreferenceTreeNodeImpl(component, res.getString("options.editor.scriptEditor"));
        secondRoot.add(node);

// -----------------------------
// Desktop Viewer panel (former Refresh Daemon)
// -----------------------------
        component = new DefaultPreferencePanel();
        component.createContainer(res.getString("options.rfb.refreshDaemonConfiguration"));

        o = new Preference("rfb.RefreshDaemon.enable",
                Preference.TYPE_BOOLEAN,
                res.getString("options.rfb.enableRefreshDaemon"),
                res.getString("options.rfb.enableRefreshDaemonDesc"));
        component.addPreference(o, 0);

        o = new Preference("rfb.RefreshDaemon.maxIdleTimeInSec", Preference.TYPE_INT,
                res.getString("options.rfb.maxIdleTimeInSec"), null);
        o.setMinValue(0);
        o.setDependentOption("rfb.RefreshDaemon.enable");
        component.addPreference(o, 0);

        o = new Preference("rfb.RefreshDaemon.enableDuringExecution", Preference.TYPE_BOOLEAN,
                res.getString("options.rfb.enableDuringExecution"), null);
        o.setDependentOption("rfb.RefreshDaemon.maxIdleTimeInSec");
        component.addPreference(o, 0);

        component.createContainer(res.getString("options.viewer.readOnlyModeKeyStrokeTitle"));
        o = new Preference("viewer.readOnlyKeyStroke", Preference.TYPE_KEYSTROKE,
                res.getString("options.viewer.readOnlyModeKeyStroke"),
                res.getString("options.viewer.readOnlyModeKeyStrokeDesc"));
        component.addPreference(o, 1);

        component.init();

        node = new PreferenceTreeNodeImpl(component, res.getString("options.rfb.desktopViewer"));
        secondRoot.add(node);

// -----------------------------
// Tool Panel config panel
// -----------------------------
        component = new DefaultPreferencePanel();

        component.createContainer(res.getString("options.appearanceNode.containerTools"));
        o = new Preference("ui.mainframe.displayToolPanel",
                Preference.TYPE_BOOLEAN,
                res.getString("options.appearanceNode.optionDisplayToolPanel"),
                res.getString("options.appearanceNode.optionDisplayToolPanelDesc"));
        component.addPreference(o, 0);

        component.createContainer(res.getString("options.toolPanel.eventListPreferences"));

        o = new Preference("recording.minEventSize", Preference.TYPE_FLOAT,
                res.getString("options.toolPanel.eventListPreferencesDesc"), null);
        o.setMaxValue(100);
        o.setMinValue(0);
        component.addPreference(o, 1);
        component.init();

        node = new PreferenceTreeNodeImpl(component, res.getString("options.toolPanel.toolPanel"));
        secondRoot.add(node);

// -----------------------------
// Status Bar config panel
// -----------------------------

        component = new DefaultPreferencePanel();
        component.createContainer(res.getString("options.statusBar.updateCoordinatesField"));

        o = new Preference("gui.StatusBar.updateFilterPercentage", Preference.TYPE_FLOAT,
                res.getString("options.statusBar.updateFilterPercentageText"),
                res.getString("options.statusBar.updateFilterPercentageDesc"));
        o.setMaxValue(100);
        o.setMinValue(0);
        component.addPreference(o, 0);

        component.createContainer(res.getString("options.statusBar.mouseCoordinatesField"));

        o = new Preference("gui.StatusBar.displayRelativeMouseCoordinates", Preference.TYPE_BOOLEAN,
                res.getString("options.statusBar.displayRelativeMouseCoordinatesText"),
                res.getString("options.statusBar.displayRelativeMouseCoordinatesDesc"));
        component.addPreference(o, 1);
        component.init();

        node = new PreferenceTreeNodeImpl(component, ApplicationSupport.getResourceBundle().getString("com.tplan.robot.gui.options.statusBarNode"));
        secondRoot.add(node);

// -----------------------------
// Web Browser config panel
// -----------------------------

        component = new DefaultPreferencePanel();
        component.createContainer(res.getString("options.webbrowser.browser"));

        o = new Preference("webbrowser.custom", Preference.TYPE_BOOLEAN,
                MessageFormat.format(res.getString("options.webbrowser.manual"), ApplicationSupport.APPLICATION_NAME),
                MessageFormat.format(res.getString("options.webbrowser.desc"), ApplicationSupport.APPLICATION_NAME));
        component.addPreference(o, 0);
        o = new Preference("webbrowser.path", Preference.TYPE_STRING,
                res.getString("options.webbrowser.executable"),
                null);
        o.setDependentOption("webbrowser.custom");
        component.addPreference(o, 0);
        component.init();

        node = new PreferenceTreeNodeImpl(component, com.tplan.robot.ApplicationSupport.getResourceBundle().getString("options.webbrowser.browser"));
        secondRoot.add(node);

// -----------------------------
// Messages config panel
// -----------------------------

        component = new DefaultPreferencePanel();
        component.createContainer(res.getString("options.warning.warningErrorMessages"));

        o = new Preference("warning.displayWinLocalWarning", Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN,
                MessageFormat.format(res.getString("options.warning.displayWinLocalWarningText"), ApplicationSupport.APPLICATION_NAME),
                MessageFormat.format(res.getString("options.warning.displayWinLocalWarningDesc"), ApplicationSupport.APPLICATION_NAME));
        component.addPreference(o, 0);

        o = new Preference("warning.closeWhenExecutionRunning", Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN,
                res.getString("options.warning.closeWhenExecutionRunning"),
                null);
        component.addPreference(o, 0);

        o = new Preference("warning.rfbConnectionError", Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN,
                res.getString("options.warning.rfbConnectionError"),
                null);
        component.addPreference(o, 0);

        component.createContainer(res.getString("options.warning.questionMessages"));
        o = new Preference("warning.adjustWindowSize", Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN,
                res.getString("options.warning.adjustWindowSize"),
                null);
        component.addPreference(o, 1);

        o = new Preference("warning.executeWhenScriptContainsErrors", Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN,
                res.getString("options.warning.executeWhenScriptContainsErrors"),
                null);
        component.addPreference(o, 1);

        o = new Preference("warning.bellOrUpdatePreference", Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN,
                res.getString("options.warning.bellOrUpdatePreference"),
                null);
        component.addPreference(o, 1);

        o = new Preference("warning.insertKeyIntoEditor", Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN,
                res.getString("options.warning.insertKeyIntoEditor"),
                null);
        component.addPreference(o, 1);

        o = new Preference("warning.useWebBrowserForHelp", Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN,
                res.getString("options.warning.openHelpInWebBrowser"),
                null);
        component.addPreference(o, 1);

        component.createContainer(res.getString("options.warning.informationMessages"));
        o = new Preference("warning.debugImageComparisonInfo", Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN,
                res.getString("options.warning.debugImageComparisonInfo"),
                null);
        component.addPreference(o, 2);

        component.init();

        node = new PreferenceTreeNodeImpl(component, res.getString("options.warning.warningMessages"));
        secondRoot.add(node);

// Scripting ----------------------------------------------------------------------

// -----------------------------
// Scripting panel
// -----------------------------
        secondRoot = new PreferenceTreeNodeImpl(null, res.getString("options.scripting.scripting"));
        preferenceRoot.add(secondRoot);

        // Language panel
        component = new DefaultPreferencePanel();

        component.createContainer(res.getString("options.scripting.defaultPaths"));
        o = new Preference("scripting.defaultOutputPath", Preference.TYPE_STRING,
                res.getString("options.scripting.defaultOutputPath"),
                res.getString("options.scripting.defaultOutputPathDesc"));
        component.addPreference(o, 0);

        o = new Preference("scripting.defaultTemplatePath", Preference.TYPE_STRING,
                res.getString("options.scripting.defaultTemplatePath"),
                null);
        component.addPreference(o, 0);

        component.createContainer(res.getString("options.scripting.dateAndTimeFormats"));
        o = new Preference("scripting.dateFormat", Preference.TYPE_STRING,
                res.getString("options.scripting.dateFormat"),
                res.getString("options.scripting.dateAndTimeFormatsDesc"));
        component.addPreference(o, 1);

        o = new Preference("scripting.timeFormat", Preference.TYPE_STRING,
                res.getString("options.scripting.timeFormat"),
                null);
        component.addPreference(o, 1);

        o = new Preference("scripting.curdateFormat", Preference.TYPE_STRING,
                res.getString("options.scripting.curdateFormat"),
                res.getString("options.scripting.curdateFormatDesc"));
        component.addPreference(o, 1);

        component.createContainer(res.getString("options.scripting.compatibility"));
        o = new Preference("scripting.replaceVariablesCompatMode", Preference.TYPE_BOOLEAN,
                res.getString("options.scripting.replaceVariablesCompatMode"),
                null);
        component.addPreference(o, 2);

        o = new Preference("scripting.globalVariablesCompatMode", Preference.TYPE_BOOLEAN,
                res.getString("options.scripting.globalVariablesCompatMode"),
                null);
        component.addPreference(o, 2);

        o = new Preference("scripting.disableNestedVariables", Preference.TYPE_BOOLEAN,
                res.getString("options.scripting.disableNestedVariables"),
                null);
        component.addPreference(o, 2);

        component.init();

        thirdRoot = new PreferenceTreeNodeImpl(component, res.getString("options.scripting.language"));
        secondRoot.add(thirdRoot);

        // Execution panel
        component = new DefaultPreferencePanel();
        component.createContainer(res.getString("options.scripting.automaticStartDelays"));

        o = new Preference("scripting.delayBeforeAutomaticExecutionSeconds", Preference.TYPE_INT,
                res.getString("options.scripting.delayBeforeAutomaticExecutionSeconds"),
                MessageFormat.format(res.getString("options.scripting.delayBeforeAutomaticExecutionSecondsDesc"), ApplicationSupport.APPLICATION_NAME));
        o.setMinValue(0);
        component.addPreference(o, 0);
        o = new Preference("scripting.delayAfterAutomaticExecutionSeconds", Preference.TYPE_INT,
                res.getString("options.scripting.delayAfterAutomaticExecutionSeconds"), null);
        o.setMinValue(0);
        component.addPreference(o, 0);

        component.createContainer(res.getString("options.scripting.stepIntoScripts"));

        o = new Preference("scripting.ScriptHandlerImpl.openIncludedFiles", Preference.TYPE_BOOLEAN,
                res.getString("options.scripting.openIncludedFiles"),
                MessageFormat.format(res.getString("options.scripting.openIncludedFilesDesc"), ApplicationSupport.APPLICATION_NAME));
        component.addPreference(o, 1);

        component.createContainer(res.getString("options.scripting.automaticScriptValidation"));

        o = new Preference("ui.editor.enableContinuousValidation", Preference.TYPE_BOOLEAN,
                res.getString("options.scripting.enableContinuousValidation"),
                res.getString("options.scripting.enableContinuousValidation"));
        component.addPreference(o, 2);

        o = new Preference("ui.editor.continuousValidationTimeout", Preference.TYPE_INT,
                res.getString("options.scripting.continuousValidationTimeout"), null);
        o.setMinValue(0);
        o.setDependentOption("ui.editor.enableContinuousValidation");
        component.addPreference(o, 2);

        o = new Preference(SCRIPT_HANDLER_CHECK_SYNTAX_BEFORE_EXECUTION, Preference.TYPE_BOOLEAN,
                res.getString("options.scripting.checkSyntaxBeforeExecution"),
                res.getString("options.scripting.checkSyntaxBeforeExecutionDesc"));
        component.addPreference(o, 2);

        component.createContainer(res.getString("options.scripting.executionOptionsTitle"));
        o = new Preference("rfb.executeReadOnly", Preference.TYPE_BOOLEAN,
                res.getString("options.scripting.executeReadOnly"),
                null);
        component.addPreference(o, 3);
        o = new Preference("scripting.minimizeForLocalDesktop", Preference.TYPE_INT_DISPLAYED_AS_BOOLEAN,
                res.getString("options.scripting.minimizeForLocalDesktop"),
                null);
        component.addPreference(o, 3);

        component.init();
        thirdRoot = new PreferenceTreeNodeImpl(component, res.getString("options.scripting.execute"));
        secondRoot.add(thirdRoot);

        // Java conversion panel
        component = new DefaultPreferencePanel();

        component.createContainer(res.getString("options.scripting.javaConverter"));

        o = new Preference("javaconverter.preferredPluginCode", Preference.TYPE_STRING,
                ApplicationSupport.getString("options.javaConverter.pluginCode"),
                null);
        List<PluginInfo> providers = JavaTestScriptConverterFactory.getInstance().getAvailablePlugins();
        List displayValues2 = new ArrayList();
        List values2 = new ArrayList();
        for (PluginInfo pi : providers) {
            displayValues2.add(pi.getDisplayName() + " (\"" + pi.getCode() + "\")");
            values2.add(pi.getCode());
        }
        o.setDisplayValues(displayValues2);
        o.setValues(values2);
        o.setSelectOnly(true);
        component.addPreference(o, 0);

        o = new Preference("javaconverter.displayPreferences", Preference.TYPE_BOOLEAN,
                ApplicationSupport.getString("options.javaConverter.displayPreferences"),
                null);
        component.addPreference(o, 0);

        component.init();

        thirdRoot = new PreferenceTreeNodeImpl(component, res.getString("options.scripting.javaConverter"));
        secondRoot.add(thirdRoot);

// -----------------------------
// Scripting node tree branch
// -----------------------------

        // Recording module - general options

        thirdRoot = new PreferenceTreeNodeImpl(null, res.getString("options.scriptingCommands.recordingModule"));
        secondRoot.add(thirdRoot);

        // Mouse node
        component = new DefaultPreferencePanel();

        component.createContainer(res.getString("options.scriptingCommands.mouseMoves"));
        o = new Preference("recording.enableMouseMoves", Preference.TYPE_BOOLEAN,
                res.getString("options.scriptingCommands.recordMouseMoves"), null);
        component.addPreference(o, 0);

        o = new Preference("recording.mouse.moveDelay",
                Preference.TYPE_INT,
                res.getString("options.scriptingCommands.moveDelay"),
                res.getString("options.scriptingCommands.moveDelayDesc"));
        o.setDependentOption("recording.enableMouseMoves");
        o.setMinValue(0);
        component.addPreference(o, 0);

        o = new Preference("recording.mouse.moveInsertPrevious",
                Preference.TYPE_INT,
                res.getString("options.scriptingCommands.moveInsertPrevious"),
                res.getString("options.scriptingCommands.moveInsertPreviousDesc"));
        o.setDependentOption("recording.mouse.moveDelay");
        o.setMinValue(0);
        component.addPreference(o, 0);

        component.createContainer(res.getString("options.scriptingCommands.mouseClicks"));
        o = new Preference("recording.enableMouseClicks", Preference.TYPE_BOOLEAN,
                res.getString("options.scriptingCommands.recordMouseClicks"), null);
        component.addPreference(o, 1);

        o = new Preference("recording.mouse.multiClickDelay",
                Preference.TYPE_INT,
                res.getString("options.scriptingCommands.multiClickDelay"),
                res.getString("options.scriptingCommands.multiClickDelayDesc"));
        o.setDependentOption("recording.enableMouseClicks");
        o.setMinValue(0);
        component.addPreference(o, 1);

        component.createContainer(res.getString("options.scriptingCommands.mouseDrags"));
        o = new Preference("recording.enableMouseDrags", Preference.TYPE_BOOLEAN,
                res.getString("options.scriptingCommands.recordMouseDrags"), null);
        component.addPreference(o, 2);

        component.init();

        node = new PreferenceTreeNodeImpl(component, res.getString("options.scriptingCommands.mouse"));
        thirdRoot.add(node);

        // Keyboard node
        component = new DefaultPreferencePanel();
        component.createContainer(res.getString("options.scriptingCommands.enableKeyboard"));

        o = new Preference("recording.enableKeyboard", Preference.TYPE_BOOLEAN,
                res.getString("options.scriptingCommands.recordKeyboardEvents"), null);
        component.addPreference(o, 0);

        component.createContainer(res.getString("options.scriptingCommands.keyboardRecordingPreferences"));

        o = new Preference("recording.keyboard.multiKeyDelay",
                Preference.TYPE_INT,
                res.getString("options.scriptingCommands.multiKeyDelay"),
                null);
        o.setDependentOption("recording.enableKeyboard");
        o.setMinValue(0);
        component.addPreference(o, 1);

        o = new Preference("recording.keyboard.enableTypeline", Preference.TYPE_BOOLEAN,
                res.getString("options.scriptingCommands.enableTypeline"),
                res.getString("options.scriptingCommands.enableTypelineDesc"));
        o.setDependentOption("recording.keyboard.multiKeyDelay");
        component.addPreference(o, 1);

        o = new Preference("recording.keyboard.typelineDelay", Preference.TYPE_INT,
                res.getString("options.scriptingCommands.typelineDelay"),
                res.getString("options.scriptingCommands.typelineDelayDesc"));
        o.setMinValue(0);
        o.setDependentOption("recording.enableTypeline");
        component.addPreference(o, 1);

        component.init();

        node = new PreferenceTreeNodeImpl(component, res.getString("options.scriptingCommands.keyboard"));
        thirdRoot.add(node);

        component = createWaitForUpdatePanel(true, res);
        component.init();

        node = new PreferenceTreeNodeImpl(component, res.getString("options.scriptingCommands.updateEvents"));
        thirdRoot.add(node);

        component = createWaitForBellPanel(res);
        component.init();

        node = new PreferenceTreeNodeImpl(component, res.getString("options.scriptingCommands.bellEvents"));
        thirdRoot.add(node);

// Plugin Preferences --------------------------------------------------------------------
        DefaultMutableTreeNode plugins = new DefaultMutableTreeNode(res.getString("options.plugins"));
        preferenceRoot.add(plugins);
        addPluginPanels(plugins);
    }

    public static DefaultPreferencePanel createWaitForBellPanel(ResourceBundle res) {
        DefaultPreferencePanel component = new DefaultPreferencePanel();
        component.createContainer(res.getString("options.waitfor.insertTimeout"));

        Preference o = new Preference("recording.waitfor.bell.insertTimeout", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.generateTimeout"), null);
        component.addPreference(o, 0);

        o = new Preference("recording.waitfor.bell.timeoutRatio", Preference.TYPE_INT,
                res.getString("options.waitfor.timeoutRatio"), null);
        o.setDependentOption("recording.waitfor.bell.insertTimeout");
        component.addPreference(o, 0);

        o = new Preference("recording.waitfor.bell.useMinTimeout", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.useMinTimeout"), null);
        o.setDependentOption("recording.waitfor.bell.timeoutRatio");
        component.addPreference(o, 0);

        o = new Preference("recording.waitfor.bell.minTimeout", Preference.TYPE_INT,
                res.getString("options.waitfor.minTimeout"), null);
        o.setMinValue(0);
        o.setDependentOption("recording.waitfor.bell.useMinTimeout");
        component.addPreference(o, 0);

        o = new Preference("recording.waitfor.bell.resetBellWait", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.resetBellWait"), null);
        component.addPreference(o, 0);

        component.createContainer(res.getString("options.waitfor.multipleBellEvents"));

        o = new Preference("recording.waitfor.bell.useCount", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.useBellCount"), null);
        component.addPreference(o, 1);
        return component;
    }

    public static DefaultPreferencePanel createWaitForUpdatePanel(boolean b, ResourceBundle res) {
        // Update Events node
        DefaultPreferencePanel component = new DefaultPreferencePanel();
        Preference o;
        int index = 0;

        component.createContainer(res.getString("options.waitfor.updateEventOptions"));
        o = new Preference("recording.waitfor.update.insertArea", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.update.insertArea"), null);
        component.addPreference(o, index);

        o = new Preference("recording.waitfor.update.insertExtent", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.update.insertExtent"), null);
        component.addPreference(o, index);

        o = new Preference("recording.waitfor.update.defaultExtent", Preference.TYPE_INT,
                res.getString("options.waitfor.update.defaultExtent"), null);
        o.setMinValue(0);
        o.setMaxValue(100);
        o.setDependentOption("recording.waitfor.update.insertExtent");
        component.addPreference(o, index);

        component.createContainer(res.getString("options.waitfor.update.updateTiming"));
        index++;
        o = new Preference("recording.waitfor.update.insertTimeout", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.update.insertTimeout"), null);
        component.addPreference(o, index);

        o = new Preference("recording.waitfor.update.timeoutRatio", Preference.TYPE_INT,
                res.getString("options.waitfor.update.timeoutRatio"), null);
        o.setDependentOption("recording.waitfor.update.insertTimeout");
        component.addPreference(o, index);

        o = new Preference("recording.waitfor.update.useMinTimeout", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.update.useMinTimeout"), null);
        o.setDependentOption("recording.waitfor.update.timeoutRatio");
        component.addPreference(o, index);

        o = new Preference("recording.waitfor.update.minTimeout", Preference.TYPE_INT,
                res.getString("options.waitfor.update.minTimeout"), null);
        o.setMinValue(0);
        o.setDependentOption("recording.waitfor.update.useMinTimeout");
        component.addPreference(o, index);

        o = new Preference("recording.waitfor.update.resetUpdateWait", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.update.resetUpdateWait"), null);
        component.addPreference(o, index);

        component.createContainer(res.getString("options.waitfor.multipleUpdates"));
        index++;

        o = new Preference("recording.waitfor.update.useWait", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.update.useWait"), null);
        component.addPreference(o, index);

        o = new Preference("recording.waitfor.update.waitRatio", Preference.TYPE_INT,
                res.getString("options.waitfor.update.waitRatio"), null);
        o.setDependentOption("recording.waitfor.update.useWait");
        component.addPreference(o, index);

        o = new Preference("recording.waitfor.update.useMinWait", Preference.TYPE_BOOLEAN,
                res.getString("options.waitfor.update.useMinWait"), null);
        o.setDependentOption("recording.waitfor.update.waitRatio");
        component.addPreference(o, index);

        o = new Preference("recording.waitfor.update.minWait", Preference.TYPE_INT,
                res.getString("options.waitfor.update.minWait"), null);
        o.setMinValue(0);
        o.setDependentOption("recording.waitfor.update.useMinWait");
        component.addPreference(o, index);

        return component;
    }

    private void addPluginPanels(DefaultMutableTreeNode root) {
        PluginManager pm = PluginManager.getInstance();
        Map<Class, String> interfaces = pm.getInterfaceMap();

        String category, pluginName;
        DefaultPreferencePanel component;
        PreferenceTreeNodeImpl categoryNode;
        Plugin plugin;
        List<Preference> prefs;

        List<PreferenceTreeNodeImpl> ctl = new ArrayList();

        for (Class cl : interfaces.keySet()) {

            // For each implemented plugin interface create a new preferenceRoot
            category = interfaces.get(cl);
            categoryNode = new PreferenceTreeNodeImpl(null, category);

            List<PluginInfo> plugins = pm.getPlugins(cl, true);

            if (plugins != null) {
                List<PreferenceTreeNodeImpl> nl = new ArrayList();
                for (PluginInfo pi : plugins) {
                    prefs = null;
                    try {
                        plugin = pi.getPluginInstance();

                        if (plugin instanceof Configurable) {
                            prefs = ((Configurable) plugin).getPreferences();
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    if (prefs != null && prefs.size() > 0) {
                        component = new DefaultPreferencePanel();
                        for (Preference p : prefs) {
                            component.addPreference(p);
                        }
                        component.init();
//                        categoryNode.add(new PreferenceTreeNodeImpl(component, pi.getDisplayName()));
                        nl.add(new PreferenceTreeNodeImpl(component, pi.getDisplayName()));
                    }
                }
                PreferenceTreeNodeImpl na[] = nl.toArray(new PreferenceTreeNodeImpl[0]);
                Arrays.sort(na);
                for (PreferenceTreeNodeImpl n : na) {
                    categoryNode.add(n);
                }
            }
            // Only add the node if it has any children
            if (!categoryNode.isLeaf()) {
//                root.add(categoryNode);
                ctl.add(categoryNode);
            }
        }
        PreferenceTreeNodeImpl ca[] = ctl.toArray(new PreferenceTreeNodeImpl[0]);
        Arrays.sort(ca);
        for (PreferenceTreeNodeImpl n : ca) {
            root.add(n);
        }

    }
}