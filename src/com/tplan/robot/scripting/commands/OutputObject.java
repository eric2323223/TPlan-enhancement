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
package com.tplan.robot.scripting.commands;

import java.util.Date;
import java.util.Map;

/**
 * <p>Output object interface. An output object is an object produced by test
 * script execution, such as a screenshot, image comparison result, error message
 * or a log. Each object type is uniquely identified through a type number.
 * If you implement your own output object, do
 * not use numbers between 1 and 100 which are reserved for internal T-Plan
 * Robot objects.</p>
 * 
 * @product.signature
 */
public interface OutputObject {

    /**
     * Unknown output object type numeric code.
     */
    public final int TYPE_UNKNOWN = 0;
    /**
     * Screenshot output object type numeric code.
     */
    public final int TYPE_SCREENSHOT = 1;
    /**
     * Code of a generic log output object.
     */
    public final int TYPE_LOG = 11;
    /**
     * Code of a warning message.
     */
    public final int TYPE_WARNING = 12;
    /**
     * Code of an error message.
     */
    public final int TYPE_ERROR = 13;

    /**
     * Get the output object type. If you implement your own output object, do
     * not use numbers between 1 and 100 which are reserved for internal T-Plan
     * Robot objects.
     *
     * @return output object type.
     */
    int getType();

    /**
     * Get code of the output object. It is typically used as a tag
     * in the output XML.
     * @return output object code.
     */
    String getCode();

    /**
     * Get the output object description.
     * @return output object description.
     */
    String getDescription();

    /**
     * Get date and time of when the output object was created.
     * @return output object date.
     */
    Date getDate();

    /**
     * Return a map of valid properties of this output object. This is used for
     * generic output without knowing the actual type of the output object and
     * its properties.
     * @return map of the object properties.
     */
    Map<String, Object> toMap();
}
