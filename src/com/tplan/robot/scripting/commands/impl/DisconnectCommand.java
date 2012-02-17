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
package com.tplan.robot.scripting.commands.impl;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.remoteclient.rfb.RfbConstants;
import com.tplan.robot.remoteclient.rfb.RfbClient;
import com.tplan.robot.scripting.SyntaxErrorException;

import com.tplan.robot.scripting.ScriptingContext;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.List;

/**
 * Handler implementing functionality of the {@doc.cmd Disconnect} command.
 * @product.signature
 */
public class DisconnectCommand extends AbstractCommandHandler implements RfbConstants {

    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext repository) throws SyntaxErrorException {
        if (args != null && args.size() > 0) {
            throw new SyntaxErrorException(ApplicationSupport.getResourceBundle().getString("disconnect.syntaxErr.noParamsAccepted"));
        }
    }

    public String[] getCommandNames() {
        return new String[]{"disconnect"};  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int execute(List args, Map values, ScriptingContext repository) throws SyntaxErrorException {

        // Validate
        validate(args, values, null, repository);

        return handleDisconnectEvent(repository);
    }

    public int handleDisconnectEvent(ScriptingContext repository) {
        try {
            RemoteDesktopClient client = repository.getClient();

            if (client != null && client.isConnected()) {
                client.close();
            }
            return 0;

        } catch (Exception ex) {
            ex.printStackTrace();
            return 1;
        }
    }

    public List getStablePopupMenuItems() {
        return null;
    }

    /**
     * This method should return true if it can be executed even when the tool is not connected to a desktop.
     *
     * @return this implementation always returns true as no desktop connection is needed for the connect command.
     */
    public boolean canRunWithoutConnection() {
        return true;
    }
}
