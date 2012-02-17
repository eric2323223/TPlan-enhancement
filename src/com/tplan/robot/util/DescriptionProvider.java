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
package com.tplan.robot.util;

/**
 * Declares method of an object able to generate description. This is generally
 * used in conjunction with the {@link Measurable} interface to show description
 * of the progress in the GUI.
 *
 * @product.signature
 */
public interface DescriptionProvider {

    /**
     * Get description. As consumers of the description are expected to call
     * this method multiple times over a time period, the description may change
     * depending on the internal state of the implementing object, for example
     * to describe the progress or the currently performed task.
     * @return description.
     */
    String getDescription();
}
