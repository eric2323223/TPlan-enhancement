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
package com.tplan.robot;

import com.tplan.robot.util.Utils;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import static com.tplan.robot.ApplicationSupport.*;

/**
 *
 * @product.signature
 */
public class DefaultHelpBroker implements HelpDispatcher {

    Map<String, String> helpKeyMap = new HashMap();

    public DefaultHelpBroker() {
        try {
            File helpSetFile = Utils.getHelpSetFile();
            if (helpSetFile != null) {
                helpSetFile = new File(helpSetFile.getParent(), "Map.jhm");
            }
            if (helpSetFile == null || !helpSetFile.exists()) {
                System.out.println("Default English help content not found at "
                        + Utils.getInstallPath() + File.separator + ApplicationSupport.APPLICATION_HELP_SET_DIR
                        + File.separator + "en" + File.separator + ApplicationSupport.APPLICATION_HELP_SET_FILE
                        + "!\nPlease check that the Help.hs, Map.jhm and TOC.xml files exist and have sufficient\n"
                        + "read permissions. The Help will display online web documentation instead.\n");
                URL url = new URL(APPLICATION_HOME_PAGE + "/docs/" + APPLICATION_DOC_DIR_NAME);
                readMap(getClass().getResourceAsStream("Map.jhm"), url, helpKeyMap);
            } else {
                readMap(new FileInputStream(helpSetFile), helpSetFile.getParentFile().toURI().toURL(), helpKeyMap);
            }
        } catch (Exception e) {
            System.out.println("Failed to read help structure.");
            e.printStackTrace();
        }
    }

    public boolean isHelpAvailable() {
        return helpKeyMap.size() > 0;
    }

    public void show(String helpID, Component owner, Boolean modal) {
        Utils.execOpenURL(helpKeyMap.get(helpID));
    }

    private void readMap(InputStream in, URL baseURL, Map<String, String> helpKeyMap) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        // Suppress loading of any DTD referenced in the map
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(in);

        doc.getDocumentElement().normalize();

        Node n;
        String key, url;
        String urlBase = baseURL.toString() + "/";

        NodeList nodeList = doc.getElementsByTagName("mapID");
        for (int i = 0; i < nodeList.getLength(); i++) {
            n = nodeList.item(i);
            key = ((Element) n).getAttribute("target");
            url = urlBase + ((Element) n).getAttribute("url");
            helpKeyMap.put(key, url);
//            System.out.println("Storing help key "+key+"="+url);
        }
    }

    public void init(Component owner) {
    }

    public boolean isContextualHelpSupported() {
        return false;
    }

    public void setHelpId(JComponent component, String helpId) {
        // Do nothing - contextual help is not supported
    }

    public void initComponentHelpIds() {
        // Do nothing - contextual help is not supported
    }

    public void contextShow(ActionEvent e) {
        // Do nothing - contextual help is not supported
    }
}
