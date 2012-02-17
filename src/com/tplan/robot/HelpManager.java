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
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import javax.swing.JComponent;

/**
 * Front end manager for all Help functionality. It is a facade which provides
 * fail over solution when the JavaHelp library (jh.jar) is not available or when
 * it fails to initialize. The Manager in such a case creates the default help dispatcher
 * ({@link DefaultHelpBroker} instance) which parses the help XML on its own
 * and redirects all the help functionality to a web browser. Even if the help files
 * are physically not present on the environment, the default dispatcher makes an attempt
 * to read copy of the Map.jhm XML from the product JAR file and when successful it maps
 * the help keys onto the documentation exposed on the T-Plan web site.
 * 
 * @product.signature
 */
public class HelpManager implements HelpDispatcher {

    private static HelpManager instance;
    private static HelpDispatcher provider;

    private HelpManager() {
    }

    /**
     * Get the shared help provider (dispatcher). The GUI components should call
     * this method to get the dispatcher and invoke methods of the {@link HelpDispatcher}
     * interface on it. This method will make an attempt to initialize the provider based
     * on the JavaHelp library {@link CustomHelpBroker}). If an error is detected, it
     * fails over to the default system web browser provider ({@link DefaultHelpBroker}).
     *
     * @return a help dispatcher able to display help topics.
     */
    public static HelpDispatcher getProvider() {
        if (provider == null) {
            // First try to instantiate the help provider based on the JavaHelp library
            String errMsg = MessageFormat.format(
                    ApplicationSupport.getString("com.tplan.robot.gui.MainFrame.errHelpUnavailable"),
                    ApplicationSupport.APPLICATION_NAME);
            try {
                ClassLoader loader = ApplicationSupport.class.getClassLoader();
                Class clazz = Class.forName("com.tplan.robot.CustomHelpBroker");
                provider = (HelpDispatcher) clazz.getConstructor(ClassLoader.class).newInstance(loader);

            } catch (InvocationTargetException ite) {
//                Utils.writeLog(System.out, ite.getTargetException().getMessage());
            } catch (NoClassDefFoundError err) {
                Utils.writeLog(System.out, errMsg);
            } catch (Exception e) {
                e.printStackTrace();
                Utils.writeLog(System.out, errMsg);
            }
            if (provider == null) {
                provider = new DefaultHelpBroker();
            }
            instance = new HelpManager();
        }
        return instance;
    }

    @Override
    public void setHelpId(JComponent component, String helpId) {
        provider.setHelpId(component, helpId);
    }

    @Override
    public boolean isContextualHelpSupported() {
        return provider.isContextualHelpSupported();
    }

    @Override
    public void show(String helpID, Component owner, Boolean modal) {
        provider.show(helpID, owner, modal);
    }

    @Override
    public boolean isHelpAvailable() {
        return provider.isHelpAvailable();
    }

    @Override
    public void init(Component owner) {
        try {
            provider.init(owner);
        } catch (Throwable t) {
            provider = new DefaultHelpBroker();
            provider.init(owner);
        }
    }

    @Override
    public void initComponentHelpIds() {
        provider.initComponentHelpIds();
    }

    @Override
    public void contextShow(ActionEvent e) {
        provider.contextShow(e);
    }
}
