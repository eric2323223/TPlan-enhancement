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
package com.tplan.robot.preferences;

/**
 * A subset of often used configuration key constants.
 *
 * @product.signature
 */
public interface ConfigurationKeys {
    public static final String IO_RECENT_SERVERS = "io.recentServers";
    public final String IO_RECENT_SCRIPTS = "io.recentScripts";
    public final String IO_OPEN_FILE_LIST = "io.openFileList";
    public final String IO_ACTIVE_FILE = "io.activeFile";

    public final String EDITOR_EXECUTED_LINE_COLOR = "ui.editor.executedLineColor";
    public final String EDITOR_BREAKPOINT_COLOR = "ui.editor.breakPointColor";
    public final String EDITOR_SYNTAX_ERROR_COLOR = "ui.editor.syntaxErrorColor";

    public final String REFRESH_DAEMON_ENABLE = "rfb.RefreshDaemon.enable";
    public final String REFRESH_DAEMON_MAX_IDLE_TIME = "rfb.RefreshDaemon.maxIdleTimeInSec";

    public final String SCRIPT_HANDLER_OPEN_INCLUDED_FILES = "scripting.ScriptHandlerImpl.openIncludedFiles";
    public final String SCRIPT_HANDLER_CHECK_SYNTAX_BEFORE_EXECUTION = "scripting.ScriptHandlerImpl.checkSyntaxBeforeExecution";

    public final String STATUS_BAR_MINIMUM_DISPLAYED_UPDATE_SIZE = "gui.StatusBar.updateFilterPercentage";
    public final String TOOLBAR_LOCATION = "ui.mainframe.toolbarLocation";
    
    public final String COMPARETO_MAX_LOADED_PIXEL_ROWS = "CompareToCommand.maxLoadedPixelRows";
    public final String COMPARETO_MAX_SEARCH_HITS = "CompareToCommand.maxSearchHits";
    
    public final String MODULE_LIST_PARAM_NAME = "CompareToCommand.customComparisonClasses";
    public final String DEFAULT_MODULE_PARAM_NAME = "CompareToCommand.defaultComparisonModule";

    public static final int MAX_DYNAMIC_MENU_ITEMS = 10;

}
