/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
package com.tplan.robot.remoteclient.rfb;

import com.tplan.robot.remoteclient.rfb.encoding.*;
import com.tplan.robot.plugin.PluginFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Pluggable factory producing RFB image encodings.
 * @product.signature
 */
public class RfbEncodingFactory extends PluginFactory {
    private static RfbEncodingFactory instance;

    /**
     * @return the instance
     */
    public static RfbEncodingFactory getInstance() {
        if (instance == null) {
            instance = new RfbEncodingFactory();
        }
        return instance;
    }

    public Encoding getEncoding(int code) {
        return (Encoding) getPluginByCode(Integer.toString(code), Encoding.class);
    }

    public List<Number> getSupportedEncodingCodes() {
        List<Number> l = new ArrayList();
        Integer code;
        Encoding enc;
        for (Object o : getAvailablePluginCodes(Encoding.class)) {
            try {
                if (o instanceof Integer) {
                    code = (Integer) o;
                } else {
                    code = Integer.parseInt(o.toString());
                }
                l.add(code);
            } catch (Exception ex) {
                System.out.println("Failed to load encoding \"" + o + "\", nested exception:");
                ex.printStackTrace();
            }
        }
        return l;
    }
}
