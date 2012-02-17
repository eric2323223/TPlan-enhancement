/*
 * T-Plan Robot, automated testing tool based on remote desktop technologies.
 * Copyright (C) 2009-2011 T-Plan Limited (http://www.t-plan.co.uk),
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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import javax.swing.text.StyledDocument;

/**
 * Interface for second generation converter of proprietary scripting language to
 * Java code.
 *
 * @product.signature
 */
public interface FileBasedJavaConverter {
    /**
     * Convert content of the styled document or of the specified file into Java
     * code and write it into the writer.
     * @param scriptFile the script file. It may be null but the text
     * to be converted must be then provided in the <code>doc</code> parameter.
     * @param doc optional document containing the text to be converted to Java.
     * If this argument is not null, the converter must read the script
     * code from it rather than from the specified file. This mechanism is intended
     * to support conversion of code which has been changed in the script editor
     * but the updates haven't been saved to the file yet. The first file parameter
     * should be in such a case specified as well if it is known because it is
     * used to resolve eventual references to other script files through the Run
     * and Include commands.
     * @param className desired Java class name. If the parameter is null, the converter
     * must create a default name or a name derived from the script file name.
     * @param packageName desired Java package name.
     * @param wr target writer to write the code to. This is just a back up target
     * and the converter may ignore it and write to a file instead.
     * @throws IOException when a file is not readable.
     * @throws StopRequestException when the process is canceled by user.
     */
    File[] convert(File scriptFile, StyledDocument doc, String className, String packageName, Writer wr)
            throws IOException, StopRequestException;

    void convertFile(File scriptFile, StyledDocument doc, Writer wr,
            String className, String packageName, Map<String, String> referenceMap) throws IOException;
    // TODO: other than full class conversions
    // TODO: access to import, exception and interface maps

}
