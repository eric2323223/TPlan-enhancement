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
package com.tplan.robot.remoteclient;

import com.tplan.robot.gui.dialogs.LoginDialog;
import com.tplan.robot.remoteclient.rfb.PasswordRequiredException;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.remoteclient.capabilities.KeyTransferCapable;
import com.tplan.robot.scripting.DefaultJavaTestScript;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * <p>Remote desktop client is a high level interface describing common capabilities
 * of remote desktop technologies. The point is to provide a common API to most
 * existing remote desktop protocols such as RFB (aka VNC), RDP and others. The
 * term "remote" is not exact here because the client can in fact be implemented
 * to connect to the local desktop, such as for example the Java client or
 * VNC client and server running on the same Windows instance.</p>
 *
 * <p>Particular remote desktop capabilities are described through
 * interfaces in the {@link com.tplan.robot.remoteclient.capabilities} package.
 * They declare what the client can do with regard to its protocol.
 * For example, if the client is able to transfer key strokes typed on the local
 * keyboard to the remote desktop, it should declare this capability through
 * implementation of the {@link KeyTransferCapable} interface.</p>
 *
 * <p>Clients are typically implemented as plugins. They are instantiated
 * indirectly through the {@link RemoteDesktopClientFactory} class by name
 * of the protocol they implement. For example, an RFB client instance can be
 * obtained through {@link RemoteDesktopClientFactory#getClient(java.lang.String) RemoteDesktopClientFactory.getInstance().getClient("rfb")}.
 * The factory can also parse a connect URI and create a suitable client, for
 * example {@link RemoteDesktopClientFactory#getClientForConnectionString(java.lang.String) RemoteDesktopClientFactory.getInstance().getClientForConnectionString("rfb://localhost:5901")}.
 * While these methods may be used in a custom program, a typical {@product.name} user
 * creating just test scripts is more likely to call the {@doc.cmd connect} command
 * or its corresponding {@link DefaultJavaTestScript#connect(java.lang.String, java.lang.String, java.lang.String, boolean)} Java method.</p>
 *
 * <p>An instantiated client should be first populated with login parameters
 * such as connect URI (containing protocol, host name and optional port number),
 * user name and password. This should be done through the {@link #setLoginParams(java.util.Map)} method.
 * The parameter set depends on the protocol (RFB may need just host name and port)
 * and configuration of particular server (for example, an RFB server may be
 * configured to require password). Client parameter sets can be easily customized
 * through the {@link #getLoginParamsSpecification()} method. Particular parameter
 * values may be then specified through the Login Dialog, through the <code>--clientparam</code>
 * CLI option or through the <code>connect()</code> method of {@link DefaultJavaTestScript Java Test Script API}.</p>
 *
 * <p>Once the login parameters are passed to the client, an attempt to connect
 * to the desktop through the {@link #connect()} method may be invoked. The client
 * is expected to fire expected {@link RemoteDesktopServerEvent server events} to all
 * registered {@link RemoteDesktopServerListener} listeners. It should also fire
 * {@link RemoteDesktopClientEvent client events} to all registered {@link RemoteDesktopClientListener}
 * instances. Correct implementation of the event system is essential for the
 * framework to work correctly.</p>
 *
 * @product.signature
 */
public interface RemoteDesktopClient extends Plugin {

    /**
     * Constant for server URI. Used as key of the port value in the map
     * of CLI arguments.
     */
    public static final String LOGIN_PARAM_URI = "URI";

    /**
     * Constant for password. Used as key of the password value in the map
     * of CLI arguments.
     */
    public static final String LOGIN_PARAM_PASSWORD = "PASSWORD";

    /**
     * Constant for user name. Used as key of the user name value in the map
     * of CLI arguments.
     */
    public static final String LOGIN_PARAM_USER = "USER";

    /**
     * <p>Find out whether the client is connected to the local display. By
     * "local display" we mean the very same desktop displayed on the local display
     * device (such as users's computer screen). An example of a local display is VNC
     * server running on Windows or Java client connected directly to the
     * system desktop buffer. On contrary, VNC servers running on ports 5901
     * and higher on a local Linux/Unix machine (localhost:1, localhost:2,...)
     * must not be considered local by this method.</p>
     *
     * <p>Value returned by this method affects behavior of the {@product.name} GUI.
     * If the display is local, the viewer doesn't display the desktop image because
     * it would lead to recursive image (infinite mirroring effect). When a test
     * script is executed on the local display, the GUI minimizes in order to hide
     * from eventual screenshots.</p>
     *
     * @return true if the client is connected to a local display, false if not.
     */
    boolean isLocalDisplay();

    //------- GET AND SET METHODS
    /**
     * Get the protocol and eventually version supported by the client.
     *
     * @return Protocol version in the format defined by the protocol.
     */
    String getProtocol();

    /**
     * Get the server host name. If the name hasn't been set through
     * <code>setHost()</code>, the method returns null.
     *
     * @return Server host name (can be both name or IP address).
     */
    String getHost();

    /**
     * Get the port of the target RFB server. Note that it is the real port, not the display number.
     * If you are connecting e.g. to a server called myserver.mydomain.com:2, the port is 5902.
     *
     * @return RFB server port number.
     */
    int getPort();

    /**
     * Get the default server port. For RFB (VNC) it is 5900 while RDP uses 3389.
     * @return default port number.
     */
    int getDefaultPort();

    /**
     * Get the password. If the password hasn't been set through
     * <code>setPassword()</code>, the method returns null.
     *
     * @return Password for authentication to the RFB server.
     */
    String getPassword();

    /**
     * Get the user. If the user name hasn't been set through
     * {@link #setUser(java.lang.String)}, the method returns null.
     *
     * @return user name for authentication to the RFB server.
     */
    String getUser();

    /**
     * Get the remote desktop width. If the client is not connected to a
     * server, the method should return zero.
     *
     * @return Remote desktop width or zero if not connected.
     */
    int getDesktopWidth();

    /**
     * Get the remote desktop height. If the client is not connected to any RFB
     * server, the method should return zero.
     *
     * @return Remote desktop width or zero if not connected.
     */
    int getDesktopHeight();

    /**
     * Get the remote desktop name. If the client is not connected to any RFB
     * server, the method should return null or an empty string.
     *
     * @return Remote desktop width or null or empty string if not connected.
     */
    String getDesktopName();

    /**
     * Indicates if the client runs in the console or GUI mode. In the console
     * mode all log messages should be printed out to the standard output.
     * <p>
     * In GUI mode the client should rather fire the messages to all registered
     * RfbServerListener instances. The GUI components should then report the
     * messages in an appropriate way.
     *
     * @return true if the client runs in CLI mode, false if in GUI mode.
     */
    boolean isConsoleMode();

    /**
     * Set the console mode flag.
     * @param consoleMode false indicates that the application runs in GUI mode,
     * true indicates CLI one.
     */
    void setConsoleMode(boolean consoleMode);

    /**
     * This is a convenience method allowing to track the mouse pointer coordinates.
     * The client is expected to cache the last mouse event sent by the
     * <code>sendMouseEvent()</code> and return it through this method.
     *
     * @return The last mouse event sent to the server.
     */
    MouseEvent getLastMouseEvent();

    /**
     * Get the remote desktop image. The client should maintain an off screen
     * image (e.g. a BufferedImage instance) and synchronize it with the updates
     * received in the FrameBufferUpdate server messages.
     *
     * @return Remote desktop image.
     */
    Image getImage();

    //------- STATE METHODS
    /**
     * Should indicate whether the client is connected to an RFB server, i.e. if
     * there's an active connection which has already passed the authentication
     * and init phase (after ServerInit message is received from the server).
     *
     * @return true if there's an active connection, false if not.
     */
    boolean isConnected();

    /**
     * Should indicate whether the client is currently connecting to an RFB
     * server but the communication hasn't passed the authentication and init
     * phases, i.e. it is in the process of initial handshake, authentication
     * or exchange of session parameters.
     */
    boolean isConnecting();

    //------- CONNECT/DISCONNECT METHODS
    /**
     * Connect the client to the specified host.
     * @throws java.lang.Exception
     * @throws com.tplan.robot.remoteclient.rfb.PasswordRequiredException
     */
    Thread connect() throws Exception, PasswordRequiredException;


    /**
     * Indicate whether the client has sufficient connect information or not.
     * If false is returned, the tool will display the Login dialog (in GUI mode)
     * or reports an error (in CLI mode).
     * @return true if the client has sufficient data to establish a connection
     * to the desktop, false if not.
     */
    boolean hasSufficientConnectInfo();

    /**
     * <p>Close the connection to the RFB server. If there's no connection, the
     * method should do nothing.</p>
     */
    Thread close() throws IOException;

    //------- RFB CLIENT TO SERVER MESSAGES

    /**
     * Implementation of the ClientCutText client-to-server RFB v3.3 message. The method
     * is supposed to send the update of the local clipboard to the server.
     *
     * @param text content of the local clipboard.
     */
    void sendClientCutText(String text) throws IOException;


    //------- LISTENER INTERFACES
    /**
     * Add an RFB server listener to the client. Each registered listener will
     * receive an event whenever a RFB server messages is received, such as
     * FrameBufferUpdate, Bell or ServerCutText.
     *
     * @param listener an object implementing the <code>RfbServerListener</code>
     * interface.
     */
    void addServerListener(RemoteDesktopServerListener listener);

    /**
     * Remove an object from the list of server listeners. If the argument
     * object is not registered in the list, the method should do nothing.
     *
     * @param listener an object implementing the <code>RfbServerListener</code>
     * interface.
     */
    void removeServerListener(RemoteDesktopServerListener listener);

    /**
     * Add a client listener. Each registered listener will
     * receive an event whenever the client sends one of the selected
     * messages to the server, such as PointerEvent, KeyEvent etc.
     *
     * @param listener an object implementing the <code>RemoteDesktopClientListener</code>
     * interface.
     */
    void addClientListener(RemoteDesktopClientListener listener);

    /**
     * Remove an object from the list of client listeners. If the argument
     * object is not registered in the list, the method should do nothing.
     *
     * @param listener an object implementing the <code>RemoteDesktopClientListener</code>
     * interface.
     */
    void removeClientListener(RemoteDesktopClientListener listener);

    /**
     * Get the list of login parameters (parameters). This is a generic mechanism allowing
     * the client to declare a custom list of input arguments required to connect to
     * the server, such as server name, user name, password, certificate file
     * name etc. This data is used by the Login dialog to display corresponding
     * GUI controls.
     *
     * @return list of required login arguments.
     */
    List<Preference> getLoginParamsSpecification();

    /**
     * <p>Set the client login parameter values. This is the entry point to specify
     * connection URI (parameter name {@link #LOGIN_PARAM_URI}), user name ({@link #LOGIN_PARAM_USER})
     * and password ({@link #LOGIN_PARAM_PASSWORD}). The client is expected to parse
     * the URI for the host name and port number to be returned by the {@link #getHost()} and
     * {@link #getPort()} methods. The point is to let the client to provide default
     * values when the URI doesn't contain them.</p>
     *
     * <p>Save for these standard parameters listed above
     * the table can be populated with any other protocol specific parameters
     * understood by the client. Any such parameter should be declared through
     * the {@link #getLoginParamsSpecification()} method to make the
     * {@link LoginDialog Login Dialog} to display an appropriate GUI component
     * and allow user to enter a parameter value.</p>
     *
     * <p>Custom parameters can be also passed through the <code>--clientparam</code>
     * CLI option. Any such instance will be parsed and passed to the client
     * through this method.</p>
     *
     * @param params map of the [parameter name, parameter value] pairs.
     */
    void setLoginParams(Map<String, Object> params);

    /**
     * Destroy the client. The method will be called after the script using the
     * client terminates. The method is supposed to perform a complete clean up
     * to release references to internal objects to get ready for garbage collection.
     */
    void destroy();

    /**
     * Get the connect string (URL). It typically contains protocol, host name
     * and optional port. The connect string must uniquely identify the connected
     * desktop (or desktop this client is initialized to connect to)..
     * @return
     */
    String getConnectString();

    /**
     * Test whether this client is connected to a desktop identified by a
     * particular URL (connect string). The method should for example check
     * whether the referred host and port is the same (if applicable). For
     * example, if the client is a VNC one and is connected to "localhost:1",
     * the method should return true when the argument is "rfb://127.0.0.1:5901"
     * because the URL refers to the same host and port.
     * @param connectString an URL (connect string).
     * @return true if the client is connected to the connect string
     */
    boolean isConnectedTo(String connectString);
}
