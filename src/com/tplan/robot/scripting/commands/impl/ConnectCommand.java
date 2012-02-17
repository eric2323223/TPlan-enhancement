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
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.remoteclient.RemoteDesktopServerListener;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.remoteclient.rfb.*;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.RemoteDesktopClientFactory;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.interpret.proprietary.ProprietaryTestScriptInterpret;
import com.tplan.robot.scripting.wrappers.TextBlockWrapper;
import com.tplan.robot.util.Utils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 * Handler implementing functionality of the {@doc.cmd Connect} command.
 * @product.signature
 */
public class ConnectCommand extends AbstractCommandHandler implements RfbConstants {

    public static final String PARAM_USER = "user";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_FORCE = "force";
    public static final String PARAM_SERVER = "server";
    public static final String PARAM_PARAMS = "params";
    public static final String PARAM_PARAM_SEPARATOR = "paramseparator";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static Map contextAttributes;

    /**
     * Get a map with context attributes.
     *
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes() {
        if (contextAttributes == null) {
            contextAttributes = new HashMap();
            ResourceBundle res = ApplicationSupport.getResourceBundle();
            contextAttributes.put(PARAM_PASSWORD, res.getString("connect.param.password"));
            contextAttributes.put(PARAM_FORCE, res.getString("connect.param.force"));
            contextAttributes.put(PARAM_ONPASS, res.getString("command.param.onpass"));
            contextAttributes.put(PARAM_ONFAIL, res.getString("command.param.onpass"));
            contextAttributes.put(PARAM_USER, res.getString("connect.param.user"));
            contextAttributes.put(PARAM_PARAMS, res.getString("connect.param.param"));
            contextAttributes.put(PARAM_PARAM_SEPARATOR, res.getString("connect.param.paramseparator"));
        }
        return contextAttributes;
    }

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as 
     * this command has a mandatory argument, which is a numeric exit code.
     *
     * @return description of the mandatory argument.
     */
    public String getContextArgument() {
        return ApplicationSupport.getString("connect.argument");
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext ctx) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        Object parName;
        Object value;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        //First token must be a command
        if (args != null && args.size() > 0) {
            try {
                parName = args.get(0);
                URI uri;
                if (parName instanceof URI) {
                    uri = (URI) parName;
                } else {
                    uri = Utils.getURI(parName.toString());
                }

                // Check if the protocol is supported
                String protocol = uri.getScheme().toUpperCase();
                if (!RemoteDesktopClientFactory.getInstance().getSupportedProtocols().contains(protocol)) {
                    String s = res.getString("connect.syntaxErr.invalidProtocol");
                    throw new SyntaxErrorException(MessageFormat.format(s, protocol));
                }
                vt.put(PARAM_SERVER, uri);

                String paramsToParse = null;
                for (int i = 1; i < args.size(); i++) {
                    parName = args.get(i).toString().toLowerCase();
                    value = values.get(parName);
                    value = value == null ? "" : value;

                    if (parName.equals(PARAM_PASSWORD)) {
                        vt.put(PARAM_PASSWORD, value);
                    } else if (parName.equals(PARAM_USER)) {
                        vt.put(PARAM_USER, value);
                    } else if (parName.equals(PARAM_ONFAIL)) {
                        vt.put(PARAM_ONFAIL, value);
                    } else if (parName.equals(PARAM_ONPASS)) {
                        vt.put(PARAM_ONPASS, value);
                    } else if (parName.equals(PARAM_PARAM_SEPARATOR)) {
                        vt.put(PARAM_PARAM_SEPARATOR, value);
                    } else if (parName.equals(PARAM_PARAMS)) {
                        Map<String, Object> map = (Map<String, Object>) vt.get(PARAM_PARAMS);
                        if (map == null) {
                            map = new HashMap();
                        }
                        if (value instanceof Map) {
                            map.putAll((Map<String, Object>) value);
                        } else {
                            paramsToParse = value.toString();
                        }
                        vt.put(PARAM_PARAMS, map);
                    } else if (parName.equals(PARAM_FORCE)) {
                        TokenParser parser = ctx.getParser();
                        vt.put(PARAM_FORCE, parser.parseBoolean(value, PARAM_FORCE));
                    } else {
                        String s = res.getString("command.syntaxErr.unknownParam");
                        throw new SyntaxErrorException(MessageFormat.format(s, parName));
                    }
                }

                if (paramsToParse != null) {
                    String separator = ",";  // Comma is the default separator
                    if (vt.containsKey(PARAM_PARAM_SEPARATOR)) {
                        separator = vt.get(PARAM_PARAM_SEPARATOR).toString();
                    }
                    Map<String, Object> map = (Map<String, Object>) vt.get(PARAM_PARAMS);

                    // We deliberately use the old good StringTokenizer because
                    // the separator provided by the user may contain a character
                    // which is special to regular expressions used by String.split()
                    StringTokenizer st = new StringTokenizer(paramsToParse, separator, false);
                    String p, v;
                    while (st.hasMoreTokens()) {
                        p = st.nextToken();
                        if (st.hasMoreTokens()) {
                            v = st.nextToken();
                            map.put(p, v);
                        } else {
                            throw new SyntaxErrorException(MessageFormat.format(res.getString("connect.syntaxErr.paramError"), p));
                        }
                    }
                }
            } catch (Exception ex) {
                String s = res.getString("command.syntaxErr.parseError");
                throw new SyntaxErrorException(MessageFormat.format(s, ex.getMessage()));
            }
        } else {
            throw new SyntaxErrorException(res.getString("connect.syntaxErr.syntaxError"));
        }

        validateOnPassAndOnFail(ctx, vt);
    }

    public String[] getCommandNames() {
        return new String[]{"connect"};  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int execute(List args, Map values, ScriptingContext ctx) throws SyntaxErrorException {

        Map t = new HashMap();

        // Validate
        validate(args, values, t, ctx);

        return handleConnectEvent(ctx, t);
    }

    private int handleConnectEvent(ScriptingContext ctx, Map params) {

        // Default exit value is '1' (fail)
        int returnValue = 1;

        try {
            ScriptManager sm = ctx.getScriptManager();
            URI server = (URI) params.get(PARAM_SERVER);
            ctx.remove(ScriptingContext.CONTEXT_CONNECT_THROWABLE);

            boolean force = false;
            if (params.containsKey(PARAM_FORCE)) {
                force = ((Boolean) params.get(PARAM_FORCE)).booleanValue();
            }

            RemoteDesktopClient client = ctx.getClient();

            // If there's an existing connected client and it implements a 
            // different protocol, close it and create a new one
            if (client == null || (client != null && !client.getProtocol().equalsIgnoreCase(server.getScheme()))) {
                if (client != null) {
                    try {
                        client.close();
                    } catch (IOException e) {
                    }
                }
                client = RemoteDesktopClientFactory.getInstance().getClient(server.getScheme());
                sm.setClient(client);
                ctx.put(ScriptingContext.CONTEXT_CLIENT, client);
                force = true;
            }

            if (force || !client.isConnected() || !client.isConnectedTo(server.toString())) {

                Map<String, Object> m = new HashMap();

                if (params.containsKey(PARAM_PARAMS)) {
                    m.putAll((Map<String, Object>) params.get(PARAM_PARAMS));
                }
                m.put(RemoteDesktopClient.LOGIN_PARAM_URI, server.toString());
                m.put(RemoteDesktopClient.LOGIN_PARAM_PASSWORD, params.get(PARAM_PASSWORD));
                m.put(RemoteDesktopClient.LOGIN_PARAM_USER, params.get(PARAM_USER));
                client.setLoginParams(m);

                UserConfiguration cfg = ctx.getConfiguration();
                WaitForFullUpdateListener l = new WaitForFullUpdateListener(client);

                LoginRunnable loginRunnable = new LoginRunnable(client, cfg);
                loginRunnable.run();

                if (loginRunnable.isLoginFailed()) {
                    l.cleanup();
                    ctx.put(ScriptingContext.CONTEXT_CONNECT_THROWABLE, loginRunnable.getConnectThrowable());
                    if (params.containsKey(PARAM_ONFAIL)) {
                        if (ctx.getInterpret() instanceof ProprietaryTestScriptInterpret) {
                            ((ProprietaryTestScriptInterpret) ctx.getInterpret()).runBlock(
                                    new TextBlockWrapper((String) params.get(PARAM_ONFAIL), true), ctx);
                        }
                    }
                } else {

                    // We got connected => wait for the first screen update
                    try {
                        while (!l.updateReceived) {
                            Thread.sleep(5);
                        }
                    } catch (InterruptedException e) {
                        l.cleanup();
                    }
                    returnValue = 0;
                    if (params.containsKey(PARAM_ONPASS)) {
                        String command = (String) params.get(PARAM_ONPASS);
                        if (command != null && !"".equals(command.trim())) {
                            if (ctx.getInterpret() instanceof ProprietaryTestScriptInterpret) {
                                ((ProprietaryTestScriptInterpret) ctx.getInterpret()).runBlock(
                                        new TextBlockWrapper(command, true), ctx);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return 2;
        }
        return returnValue;
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

    private class WaitForFullUpdateListener implements RemoteDesktopServerListener {

        boolean updateReceived = false;
        private RemoteDesktopClient client;

        WaitForFullUpdateListener(RemoteDesktopClient rfb) {
            this.client = rfb;
            rfb.addServerListener(this);
        }

        public void serverMessageReceived(RemoteDesktopServerEvent evt) {
            if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_UPDATE_EVENT) {
                cleanup();

                // The following thread gives the VNC browser time to update the image and display it
                Runnable r = new Runnable() {

                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        updateReceived = true;
                    }
                };
                (new Thread(r, "Connect Command 1 sec Timeout")).start();

            } else if (evt.getMessageType() == RemoteDesktopServerEvent.SERVER_IO_ERROR_EVENT) {
                cleanup();
            }
        }

        public void cleanup() {
            if (client != null) {
                client.removeServerListener(this);
            }
            client = null;
        }
    }
}
