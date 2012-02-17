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
package com.tplan.robot.gui;

/**
 * Various GUI constants.
 * @product.signature
 */
public interface GUIConstants {

    public static final String DEFAULT_TPLAN_ROBOT_FILE_EXTENSION = "tpr";

    public static final String EVENT_DOCUMENT_CHANGED = "documentChanged";
    public static final String EVENT_SELECTION_CHANGED = "documentSelectionChanged";
    public static final String EVENT_STATUSBAR_MSG = "EVENT_STATUSBAR_MSG";
    public static final String EVENT_MOUSE_MOVED = "EVENT_MOUSE_MOVED";
    public static final String EVENT_DISPLAY_PREFERENCES = "EVENT_DISPLAY_PREFERENCES";
    public static final String EVENT_DISPLAY_HASHTABLE = "EVENT_DISPLAY_HASHTABLE";
//    public static final String EVENT_DISPLAY_VARIABLES = "EVENT_DISPLAY_VARIABLES";

    public static final String EVENT_ADD_CUSTOM_ACTION_MSG = "EVENT_ADD_CUSTOM_ACTION_MSG";

    public static final String EVENT_REMOVE_CUSTOM_ACTION_MSG = "EVENT_REMOVE_CUSTOM_ACTION_MSG"; 

    public static final String EVENT_ADD_STABLE_ACTION_MSG = "EVENT_ADD_STABLE_ACTION_MSG";

    public static final String EVENT_REMOVE_STABLE_ACTION_MSG = "EVENT_REMOVE_STABLE_ACTION_MSG";
}
