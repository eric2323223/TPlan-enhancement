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
package com.tplan.robot.scripting.interpret.java;

import com.tplan.robot.scripting.interpret.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.tools.SimpleJavaFileObject;

/**
 * Java source/byte code container for compilation purposes.
 * @product.signature
 */
class MemoryJavaSourceObject extends SimpleJavaFileObject {

    private TestScriptInterpret testScript;
    private ByteArrayOutputStream compiledCode;

    MemoryJavaSourceObject(String name, TestScriptInterpret testScript) {
        super(URI.create(name + Kind.SOURCE.extension), Kind.SOURCE);
        this.testScript = testScript;
    }

    MemoryJavaSourceObject(final String name, final Kind kind) {
        super(URI.create(name), kind);
    }

    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream(getCompiledCode().toByteArray());
    }

    @Override
    public OutputStream openOutputStream() {
        compiledCode = new ByteArrayOutputStream();
        return getCompiledCode();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        try {
            Document doc = testScript.getDocument();
            String text = doc.getText(0, doc.getLength());
            return text;
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * Get the compiled code.
     * @return the compiledCode
     */
    public ByteArrayOutputStream getCompiledCode() {
        return compiledCode;
    }
}
