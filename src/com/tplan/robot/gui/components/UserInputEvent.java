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
package com.tplan.robot.gui.components;

/**
 * Custom input event to be used together with the {@link FilteredDocument} and {@link UserInputListener} classes.
 * @product.signature
 */

public class UserInputEvent extends java.util.EventObject {

	/**
	 * Value represented by the text in the component (a number, date, etc.).
	 */
	private Object value = null;

	/**
	 * Correctness flag.
	 */
	private boolean correct;

	/**
	 * Constructor.
	 *
	 * @param source the source object (usually a FilteredDocument instance).
	 * @param value a value represented by the text in the document.
	 * @param correct whether the value is correct or not.
	 */
	public UserInputEvent(Object source, Object value, boolean correct) {
		super(source);
		this.value = value;
		this.correct = correct;
	}

	/**
	 * Get the document value.
     * @return document value.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Get the correctness flag.
     * @return true if the document value is considered to be correct, false otherwise.
	 */
	public boolean isCorrect() {
		return correct;
	}
}
