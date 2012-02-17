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

import com.tplan.robot.plugin.DependencyMissingException;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.remoteclient.RemoteDesktopClientEvent;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.Configurable;
import com.tplan.robot.preferences.ConfigurationChangeEvent;
import com.tplan.robot.preferences.ConfigurationChangeListener;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.plugin.Plugin;
import com.tplan.robot.plugin.PluginEvent;
import com.tplan.robot.plugin.PluginListener;
import com.tplan.robot.remoteclient.AbstractRemoteDesktopClient;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.rfb.encoding.Encoding;

import com.tplan.robot.util.Utils;
import java.util.Date;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.security.Key;
import java.security.spec.KeySpec;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import static java.awt.event.InputEvent.*;

/**
 * <a name="spec"/>
 * <h1 class="headorange">RFB 3.3 Client</h1>
 * <p>This remote desktop client implements <a href="http://www.realvnc.com/docs/rfbproto.pdf">RFB protocol version 3.3</a>
 * and supports connection to any protocol compatible VNC server, such as
 * <a href="http://www.tightvnc.com">TightVNC</a> or <a href="http://www.realvnc.com">RealVNC</a>.
 * See the <a href="../../../../../../install/install.html#vnc">Release Notes</a/> document
 * for further information about tested and reported-to-work VNC products.</p>
 *
 * <p>The client supports the following login parameters:
 * <ul>
 * <li><b>Server and port number</b>. Unlike the legacy VNCRobot product there
 * must be a <b>real port number</b>, not a VNC desktop number.
 * To get a port add 5900 to the desktop number.
 * If not provided the port defaults to 5900.</li>
 * <li><b>Password</b> for connection to a server with the default VNC
 * authentication. This parameter doesn't have to be set if the server is
 * configured to require no authentication.</li>
 * <li><b>Shared access mode.</b> This flag is defined by the RFB protocol.
 * If it is on, the VNC server desktop will be shared
 * and open to multiple connections. If the flag is off, the client runs in
 * so called <i>exclusive access mode</i> and only one VNC client may be
 * connected at a time. Any new attempt to connect to the server usually
 * disconnects the currently connected client. As the flag is sent to the VNC
 * server during connection initialization, the client has to reconnect in order
 * to change the mode.</li>
 * </ul>
 * </p>
 *
 * <p><b>Windows VNC servers</b> typically run on port 5900. As they work directly with
 * the default system desktop, a connection from a VNC viewer to server requires two machines.
 * Starting a viewer on the server machine is not a good
 * idea because it leads to the "infinite mirror effect" where the viewer
 * recursively displays desktop image.</p>
 *
 * <p><b>Unix and Linux systems</b> are on the other hand capable of running multiple
 * desktops in parallel and they may start any number of VNC servers as long as the system
 * resources are not exhausted (such as memory). The client can execute on the
 * same machine provided that its GUI displays on another desktop instance than
 * the one it connects to. As the base port of 5900 is usually taken by the system's
 * default X-Windows server, VNCs typically occupy ports 5901 and higher.
 * Server instances are sometimes referred to by desktop
 * numbers such as :1 or :2 where DesktopNumber=PortNumber-5900 (port 5901 is desktop :1 etc.). </p>
 *
 * <p>A single instance of this client can handle one connection to a VNC server.
 * To connect the client initialize the necessary information such as the host name,
 * port and eventual password and call the {@link #connect()} method. Logon data may be
 * populated either through the standard set- methods such as {@link #setHost(java.lang.String)} or
 * using {@link #setLoginParams(java.util.Map)} (typically used by GUI components like the Login Dialog).
 * As soon as the initial handshake is successfuly completed, the client starts a standalone
 * thread to handle the normal client-server interaction (exchange of events and desktop
 * image updates) until the {@link #close()} method is called or the connection
 * gets terminated from the server side. The client internally maintains a desktop
 * image buffer available through the {@link #getImage()} method.</p>
 *
 * <p>Client capabilities as specified by the RFB protocol:</p>
 *
 * <table border=1>
 * <tr>
 * <td style="vertical-align: top; color: rgb(255, 255, 255); background-color: rgb(0, 0, 153);"><b>RFB Feature/Capability</b></td>
 * <td style="vertical-align: top; color: rgb(255, 255, 255); background-color: rgb(0, 0, 153);"><b>Support Scope</b></td>
 * <td style="vertical-align: top; color: rgb(255, 255, 255); background-color: rgb(0, 0, 153);"><b>Note</b></td>
 * </tr>
 * <tr>
 * <td><b>Protocol Version</b></td>
 * <td>RFB 3.3</td>
 * <td>Compatible with any VNC server supporting RFB 3.3 or higher.</td>
 * </tr>
 * <tr>
 * <td><b>Security Type</b></td>
 * <td>None (code 1)<br>VNC Authentication (code 2)</td>
 * <td>No custom auth schemes are supported at the moment.</td>
 * </tr>
 * <tr>
 * <td><b>Pixel Format</b></td>
 * <td>Any format up to 32-bits.</td>
 * <td>The client can either accept format suggested by the server or request a custom one (configurable through Preferences). <br><br>Pixel format is implemented in a generic way to handle standard 8, 16 and 24 (32) bit formats as well as any custom one.
 * To add a custom format see the <code>com.tplan.robot.rfb.customPixelFormat</code> parameter in the <a href="../../DefaultConfiguration.properties">configuration file</a>.</td>
 * </tr>
 * <tr>
 * <td><b>Encodings</b></td>
 * <td>Raw (code 0)<br>CopyRect (1)<br>RRE (2)<br>CoRRE (4)<br>Hextile (5)<br>Zlib (6)<br>Color maps supported as well</td>
 * <td>The list as well as the encoding order is configurable through Preferences. Encodings are implemented as plugins and it is possible to add new ones in a compatible way.</td>
 * </tr>
 * <tr>
 * <td><b>Key & Pointer Events</b></td>
 * <td>Supported</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td><b>Clipboard Events</b></td>
 * <td>Supported (both on client and server sides)</td>
 * <td>Additional configuration steps are required on the server side. See the <a href="../../../../../../install/install.html#vnc">Release Notes</a/> document for more.</td>
 * </tr>
 * <tr>
 * <td><b>Bell Event</b></td>
 * <td>Supported</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td><b>Listen Mode</b></td>
 * <td>Supported</td>
 * <td>Listen mode allows to initiate reverse connection from server to client. It allows to connect to servers running on devices without IP address, such as mobile phones.
 * Though this functionality is out of scope of RFB protocol, it is supported by most VNC servers and this client supports it as well.</td>
 * </tr>
 * <tr>
 * <td><b>File Transfer</td>
 * <td>Not supported</td>
 * <td>File transfer is not specified by the RFB protocol. It is a proprietary undocumented feature of particular VNC product and as such it is not supported by this client.</td>
 * </tr>
 * </table>
 *
 * @product.signature
 */
public class RfbClientImpl extends AbstractRemoteDesktopClient implements RfbConstants, RfbClient, Runnable,
        ImageObserver, Configurable, ConfigurationChangeListener, PluginListener {

    /**
     * Network socket for the connection to the VNC server.
     */
    private Socket socket;
    /**
     * Input stream for the messages from server.
     */
    private DataInputStream inStream;
    /**
     * Output stream for the messages to server.
     */
    private OutputStream outStream;    // Boolean flags -----------------------------------------------------------
    /**
     * A flag indicating whether the module is connected to an RFB server.
     */
    private boolean connected = false;
    /**
     * A flag indicating whether the module is connecting to an RFB server,
     * i.e. whether it is in the process of initial handshake, authentication
     * or exchange of session parameters.
     */
    private boolean connecting = false;
    /**
     * A flag showing whether the application runs in the CLI or GUI mode.
     */
    private boolean consoleMode = false;
    /**
     * The last mouse event sent to the RFB server.
     */
    private MouseEvent lastMouseEvent;
    /**
     * Current RFB thread. Each connection to an RFB server is started in
     * a separate thread.
     */
    Thread rfbThread = null;
    /**
     * User configuration object. It contains user preferences.
     */
    UserConfiguration cfg;
    /**
     * The highest major version number of RFB protocol supported by the server.
     * This value is received from the RFB server in the ProtocolVersion message
     * and it is stored into this variable for later debugging and logging purposes.
     */
    private int majorVersion;
    /**
     * The highest minor version number of RFB protocol supported by the server.
     * This value is received from the RFB server in the ProtocolVersion message
     * and it is stored into this variable for later debugging and logging purposes.
     */
    private int minorVersion;
    /**
     * Server host name.
     */
    private String host;
    /**
     * User name for authentication to the server. Not used in this implementation
     * because RFB servers don't require user names, just passwords.
     */
    private String user;
    /**
     * Server port.
     */
    private int port = RFB_PORT_OFFSET;
    /**
     * Server password (unencrypted).
     */
    private String password;
    /**
     * Remote desktop name. It is received from the ServerInit message.
     */
    private String desktopName;
    /**
     * Image of the remote desktop. In this implementation it is always
     * a BufferedImage instance.
     */
    private BufferedImage image;
    /**
     * Preferred list of encodings (number codes).
     */
    private int[] encodings = {2, 0, 1, 4, 5, 6};
    /**
     * A flag indicating whether to connect in exclusive (false) or shared mode (true)
     */
    private boolean sharedMode = true;
    /**
     * Pixel format used for the current communication. It stores the same value
     * as <code>serverPixelFormat</code> unless the client uses a custom format
     * through the setPixel
     */
    private PixelFormat pixelFormat;
    /**
     * This variable stores pixel format received from the server in the
     * ServerInit message.
     */
    private PixelFormat serverPixelFormat;
    /**
     * Temporary variable for security type required by the server.
     */
    private int securityType = -1;
    /**
     * Table of mappings among Java and RFB key codes.
     * @see #loadSpecialKeyCodes()
     * @see com.tplan.robot.api.rfb.RfbConstants#SPECIAL_KEY_CODES
     */
    private Map specialKeyCodeMap;
    /**
     * Map of supported encodings. Key is the numeric encoding code as defined in RFB 3.3
     * protocol.
     */
    private Map<Integer, Encoding> encodingMap;
    /**
     * Resource bundle with localizable text messages.
     */
    ResourceBundle res;
    /**
     * Debug flag which switches on/off the debug messages describing the whole
     * RFB communication.
     */
    private boolean debug = System.getProperty("vncrobot.rfb.debug") != null;
    /**
     * A byte buffer used to construct KeyEvent and PointerEvent messages.
     */
    private byte[] outBuf = new byte[128];
    /**
     * Helper variable for holding modifiers of the previous InputEvent
     */
    private int previousEventModifiers = 0;
    private URI uri = null;
    private int listenPort = -1;
    private boolean manualEncodings;

    /**
     * Constructor.
     *
     * @param consoleMode should be true when the application is running in console mode,
     * false if in GUI mode.
     * @param cfg user configuration instance.
     */
    public RfbClientImpl(boolean consoleMode, UserConfiguration cfg) {
        this.cfg = cfg;
        this.consoleMode = consoleMode;
        this.res = ApplicationSupport.getResourceBundle();
        image = createImage(800, 600);
        setConfiguration(cfg);
        encodings = cfg.getArrayOfInts("rfb.imageEncodings");
    }

    /**
     * Constructor.
     *
     */
    public RfbClientImpl() {
        this(false, UserConfiguration.getInstance());
    }

    /**
     * <p>Connects the client to the RFB server specified in module variables.
     * Users of this class are supposed to fill in the variables using the
     * {@link #setHost(java.lang.String)}, {@link #setPort(int)}
     * and {@link #setPassword(java.lang.String)} methods or initialize
     * the values in a custom way.</p>
     *
     * <p>When the argument flag is false, the method should open a network
     * socket and perform the initial handshake communication according to
     * the RFB 3.3 protocol:
     * <ul>
     * <li>Receive the ProtocolVersion message from the server,</li>
     * <li>Send the ProtocolVersion message to the server,</li>
     * <li>Receive the Security message from the version and authenticate if
     * necessary,</li>
     * <li>Send the ClientInit message which specifies whether access to
     * the remote desktop will be shared or exclusive,</li>
     * <li>Receive the ServerInit message which tells the client the width, height
     * and name of the remote desktop as well as the default pixel format,</li>
     * <li>Send the list of encodings supported by the client and their order
     * of preference,</li>
     * <li>The client may also optionally send the SetPixelFormat message
     * to override the pixel format suggested by the server.</li>
     * </ul>
     * </p>
     *
     * <p>The following sample code shows a typical usage to connect to an RFB
     * server running on the local machine on display :1 (port 5901) with password "welcome".</p>
     *
     * <blockquote>
     * <pre>
     * RemoteDesktopClient client = RemoteDesktopClientFactory.getInstance().getClient("rfb");
     * Map<String, Object> m = new HashMap();
     * m.put(RemoteDesktopClient.LOGIN_PARAM_URI, "rfb://localhost:1");
     * m.put(RemoteDesktopClient.LOGIN_PARAM_PASSWORD, "welcome");
     * client.setLoginParams(m);
     * try {
     *    client.connect();
     * } catch (Exception pe) {
     *    pe.printStackTrace();
     * }
     * </pre>
     * </blockquote>
     *
     * <p>Implementations of this interface are not supposed to be thread safe and
     * each RFB client will be associated just with one connection thread
     * (i.e. the connect method will not be called when there's already
     * a connection).</p>
     * @return the connection thread.
     */
    public Thread connect() throws Exception, PasswordRequiredException {
        String errorMsg = null;
        Exception ex = null;
        try {
            connecting = true;
            fireRemoteServerEvent(new RemoteDesktopServerEvent(this, RemoteDesktopServerEvent.SERVER_CONNECTING_EVENT));

            securityType = -1;

            // First create a socket. If the server name or port is incorrect, the method fails.
            createSocket();

            // Once we have a connection, server sends the ProtocolVersion message.
            // The client must send the highest protocol version it supports in return.
            readProtocolVersion(inStream);

            synchronized (outStream) {
                outStream.write(PROTOCOL_VERSION_3_3.getBytes());
            }
            if (debug) {
                System.out.print("C2S/ProtocolVersion: Client requested communication in RFB version " + PROTOCOL_VERSION_3_3);
            }
            com.tplan.robot.ApplicationSupport.logFine(res.getString("com.tplan.robot.rfb.RfbModule.c2s.usingVersion3.3"));

            // Authenticate to the server
            authenticate(securityType);

            // Send and receive the ClientInitialization and ServerInitialization messages
            writeClientInit();
            readServerInit();
            this.encodingMap = initEncodings();
            sendSetEncodings();

            if (cfg != null) {
                PixelFormat pf = getConfigPixelFormat();
                if (pf != null) {
                    setPixelFormat(pf);
                }
            } else if (pixelFormat != serverPixelFormat) {
                setPixelFormat(pixelFormat);
            } else if (debug) {
                System.out.println("C2S/SetPixelFormat: Using server preferred pixel format.");
            }

            connecting = false;
            fireRemoteServerEvent(new RemoteDesktopServerEvent(this, RemoteDesktopServerEvent.SERVER_CONNECTED_EVENT));

            rfbThread = new Thread(this, "RFB Module, host=" + getHost() + ":" + getPort());
            rfbThread.start();

        } catch (NoRouteToHostException e) {
            Object args[] = {getHost()};
            errorMsg = MessageFormat.format(res.getString("com.tplan.robot.rfb.RfbModule.c2s.noRouteToHost"), args);
            ex = e;
        } catch (UnknownHostException e) {
            Object args[] = {getHost()};
            errorMsg = MessageFormat.format(res.getString("com.tplan.robot.rfb.RfbModule.c2s.unknownServerName"), args);
            ex = e;
        } catch (ConnectException e) {
            Object args[] = {getHost(), "" + getPort()};
            errorMsg = MessageFormat.format(res.getString("com.tplan.robot.rfb.RfbModule.c2s.cannotConnect"), args);

            // Improvement in 2.0Beta - People get confused by having to specify the port number
            // instead of VNC display number. We are checking the port number here and add
            // a hint to the error message if the number seems to be a display one.
            String suffix = null;
            if (listenPort >= 0) { // We are in the listen mode
                if (getPort() < RFB_LISTEN_PORT_OFFSET) {
                    suffix = getHost() + ":" + Integer.toString(getPort() + RFB_LISTEN_PORT_OFFSET);
                }
            } else { // Normal connection mode
                if (getPort() < RFB_PORT_OFFSET) {
                    suffix = getHost() + ":" + Integer.toString(getPort() + RFB_PORT_OFFSET);
                }
            }
            if (suffix != null) {
                errorMsg += "\n" + MessageFormat.format(res.getString("com.tplan.robot.rfb.RfbModule.c2s.cannotConnect2"), suffix);
            }
            ex = e;
        } catch (EOFException e) {
            errorMsg = res.getString("com.tplan.robot.rfb.RfbModule.c2s.remoteSideClosedConn");
        } catch (IOException e) {
            if (e.getMessage() == null) {
                errorMsg = res.getString("com.tplan.robot.rfb.RfbModule.c2s.unspecifiedIOError");
            } else {
                errorMsg = res.getString("com.tplan.robot.rfb.RfbModule.c2s.networkError") + " " + e.getMessage();
            }
            ex = e;
        } catch (PasswordRequiredException e) {
            // If PasswordRequiredException is thrown, don't modify it because we'll be able to reuse the connection
            fireRemoteServerEvent(new RemoteDesktopServerEvent(this, e));
            throw e;
        } catch (Exception e) {
            com.tplan.robot.ApplicationSupport.logSevere(res.getString("com.tplan.robot.rfb.RfbModule.RFBCore") + e.getMessage());
            fireRemoteServerEvent(new RemoteDesktopServerEvent(this, e));
            throw e;
        }
        if (errorMsg != null) {
            com.tplan.robot.ApplicationSupport.logSevere(errorMsg);
            Exception e = new RfbException(errorMsg, ex);
            fireRemoteServerEvent(new RemoteDesktopServerEvent(this, e));
            throw e;
        }
        return rfbThread;
    }

    /**
     * Get pixel format from the user configuration.
     *
     * @return pixel format object or null if user set a preference to accept
     * server suggested pixel format.
     */
    private PixelFormat getConfigPixelFormat() {
        PixelFormat pf = null;
        if (cfg != null) {
            Boolean useCustom = cfg.getBoolean("rfb.useCustomPixelFormat");
            if (useCustom != null && useCustom.booleanValue()) {
                List<? extends Number> l = cfg.getListOfNumbers("rfb.customPixelFormat");
                if (l != null && l.size() > 0) {
                    pf = new PixelFormat(getDesktopWidth(), getDesktopHeight(), l);
                }
            }

        }
        return pf;
    }

    /**
     * Indicate whether the client has sufficient data to establish connection
     * to an RFB server. The method returns true if at least host or listen mode
     * port is specified.
     * @return true if at least host or listen mode port is specified, false if not.
     */
    public boolean hasSufficientConnectInfo() {
        return host != null || System.getProperties().containsKey(VAR_LISTEN);
    }

    /**
     * Create a new socket for TCP communication with the server.
     *
     * @throws java.io.IOException if the socket cannot be opened
     */
    private void createSocket() throws IOException {
        if (socket != null && socket.isConnected()) {
            close();
        }

        // Listen port is specified -> act as a server and wait on the specified
        // port for connection from the VNC server
        if (listenPort >= 0) {
            ServerSocket serverSocket = new ServerSocket(listenPort);
            socket = serverSocket.accept();
            serverSocket.close();
            serverSocket = null;
        } else {  // Standard client to server connect
            socket = new Socket(this.host, this.port);
            Boolean ka = UserConfiguration.getInstance().getBoolean("rfb.keepAlive");
            if (ka != null && ka) {
                socket.setKeepAlive(true);
            }
            Integer so = UserConfiguration.getInstance().getInteger("rfb.soTimeout");
            if (so != null && so.intValue() > 0) {
                socket.setSoTimeout(so);
            }
        }
        outStream = socket.getOutputStream();
        inStream = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 16384));
    }

    /**
     * Close the socket connection.
     * This will fire an action event to all registered listeners.
     * @throws IOException when closure of the connection fails.
     */
    public Thread close() throws IOException {
        fireRemoteServerEvent(new RemoteDesktopServerEvent(this, RemoteDesktopServerEvent.SERVER_DISCONNECTING_EVENT));

        connected = false;
        connecting = false;

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        socket = null;
        pixelFormat = null;
        securityType = -1;
        fireRemoteServerEvent(new RemoteDesktopServerEvent(this, RemoteDesktopServerEvent.SERVER_DISCONNECTED_EVENT));

        if (image != null) {
            fireRemoteServerEvent(new RemoteDesktopServerEvent(this, new Rectangle(0, 0, getDesktopWidth() - 1, getDesktopHeight() - 1)));
        }
        if (debug) {
            System.out.println("RfbCore: close(): connection to server " + getHost() + ":" + (port - RFB_PORT_OFFSET) + " closed");
        }
        return rfbThread;
    }

    /**
     * Initialize map of encodings supported by this client.
     *
     * @return map of encodings; key is the encoding numeric code as defined
     * by the RFB 3.3 protocol and value is an encoding handler, i.e. class
     * implementing the <code>Encoding</code> interface.
     */
    private Map<Integer, Encoding> initEncodings() {
        Map map = new HashMap<Integer, Encoding>();
        RfbEncodingFactory fact = RfbEncodingFactory.getInstance();
        Integer code;
        Encoding enc;
        for (Number o : fact.getSupportedEncodingCodes()) {
            try {
                enc = fact.getEncoding(o.intValue());
                map.put(o, enc);
            } catch (Exception ex) {
                System.out.println("Failed to load encoding \"" + o + "\", nested exception:");
                ex.printStackTrace();
            }
        }
        return map;
    }

    /**
     * Read RFB protocol version from the input stream.
     *
     * @param in socket input stream
     * @return protocol version (can be 3.8, 3.3 etc.)
     * @throws java.lang.Exception when protocol version parsing fails
     */
    private String readProtocolVersion(DataInputStream in) throws Exception {

        byte[] b = new byte[12];
        in = in == null ? inStream : in;
        in.readFully(b);
        String msg = new String(b);

        try {
            majorVersion = Integer.parseInt(msg.substring(4, 7));
            minorVersion = Integer.parseInt(msg.substring(8, 11));
            Object args[] = {majorVersion, minorVersion};
            com.tplan.robot.ApplicationSupport.logFine(
                    MessageFormat.format(
                    res.getString("com.tplan.robot.rfb.RfbModule.s2c.serverSupportedVersion"), args));
        } catch (NumberFormatException ex) {
            Object args[] = {getHost(), getPort()};
            String errorMsg = MessageFormat.format(
                    res.getString("com.tplan.robot.rfb.RfbModule.notRFBServer"), args);
            throw new Exception(errorMsg);
        }
        return msg;
    }

    /**
     * Read and decode server security type (i.e. authentication method) from
     * the socket input stream. This client supports just no security and VNC
     * authentication which is password based security with DES encryption.
     *
     * @param in socket input stream.
     * @return security type code.
     * @throws java.lang.Exception when parsing fails or the server requests
     * a security type which is not supported by this client.
     */
    private int readSecurityType(DataInputStream in) throws IOException, UnsupportedAuthenticationException {
        in = in == null ? inStream : in;
        securityType = in.readInt();

        switch (securityType) {
            case SECURITY_NONE:
                if (debug) {
                    System.out.println("S2C/Security: Server requires no security.");
                }
                return securityType;
            case SECURITY_VNC_AUTH:
                if (debug) {
                    System.out.println("S2C/Security: Security requires password (VNC Authentication)");
                }
                return securityType;

            case SECURITY_INVALID:
                byte[] buf = new byte[inStream.readInt()];
                in.readFully(buf);
                String msg = new String(buf);

                if (debug) {
                    System.out.println("S2C/Security: Server returned security type of 0 (failure), reason: '" + msg + "'");
                }

                // This message is undocumented but is pretty standard.
                if (msg != null && msg.equals("Too many authentication failures")) {
                    msg = "S2C: " + ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.tooManyFailures");
                }
                throw new IOException(msg);

            case SECURITY_RA2:
            case SECURITY_RA2NE:
            case SECURITY_TIGHT:
            case SECURITY_ULTRA:
            case SECURITY_TLS:
            default:
                Object params[] = {new Integer(securityType)};
                String pattern = ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.unsupportedSecurityType");
                if (debug) {
                    System.out.println("S2C/Security: Unsupported security type "
                            + securityType + " (" + Integer.toHexString(securityType) + ")");
                }
                throw new UnsupportedAuthenticationException("S2C: " + MessageFormat.format(pattern, params));
        }
    }

    /**
     * <p>Authenticate to the server. The method first reads the authentication type
     * required by the server. This step is skipped if the <parameter>securityType</code>
     * argument is >= 0.
     *
     * @param securityType if the argument is >= 0, the security type will not be read. It is used when
     *                     user tries to connect to a server without password and server requires it.
     * @throws Exception if uthentication fails.
     */
    private int authenticate(int securityType) throws Exception {
        if (securityType < SECURITY_NONE) {
            securityType = readSecurityType(inStream);
        }

        switch (securityType) {

            // No security. Move on to the initialization section
            case SECURITY_NONE:
                com.tplan.robot.ApplicationSupport.logFine(res.getString("com.tplan.robot.rfb.RfbModule.s2c.noSecurityReqd"));
                return SECURITY_RESPONSE_OK;

            // VNC Authentication. It is a password based challenge-response authentication
            case SECURITY_VNC_AUTH:
                byte[] challenge;
                com.tplan.robot.ApplicationSupport.logFine(res.getString("com.tplan.robot.rfb.RfbModule.s2c.passwdReqd"));

                // Paswword is required but is empty
                if (password == null || "".equals(password)) {
                    Object params[] = {host};
                    String text = MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.passwordRequired"), params);
                    if (debug) {
                        if (isConsoleMode()) {
                            System.out.println("RFB Client: Password not available -> fail (CLI mode)");
                        } else {
                            System.out.println("RFB Client: Password not available -> display password window (GUI mode)");
                        }
                    }
                    throw new PasswordRequiredException(text, SECURITY_VNC_AUTH);
                }

                // Zero char means end of string on UNIX. If our password contains a zero byte,
                // truncate it.
                password = password.indexOf(0) > -1 ? password.substring(0, password.indexOf(0)) : password;

                // Truncate the password to 8 characters
                password = password.length() > 8 ? password.substring(0, 8) : password;

                // Read a 16-byte challenge from the server
                challenge = new byte[16];
                inStream.readFully(challenge);

                if (debug) {
                    System.out.println("S2C/Security: Challenge received from server"); //: " + new sun.misc.BASE64Encoder().encode(challenge));
                }

                // Convert first 8 password chars into a byte array
                byte[] passwdBytes = password.getBytes();
                byte key[] = new byte[8];

                // Create a DES key through reverting the order of bits in
                // each single byte of the password. Form more info
                // see e.g. http://www.vidarholen.net/contents/junk/vnc.html
                for (int i = 0; i < key.length && i < passwdBytes.length; i++) {
                    key[i] = revert(passwdBytes[i]);
                }

                // Use Java Cryptography API to encrypt the 16-byte challenge
                // using the password key and DES.
                // Note that the each 8 bytes of the challenge must be encoded
                // separately in a single step.
                KeySpec ks = new DESKeySpec(key);
                Key k = SecretKeyFactory.getInstance("DES").generateSecret(ks);
                Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, k);
                cipher.doFinal(challenge, 0, 8, challenge, 0);
                cipher.doFinal(challenge, 8, 8, challenge, 8);

                synchronized (outStream) {
                    outStream.write(challenge);
                }
                if (debug) {
                    System.out.println("C2S/Security: Encrypted challenge sent to the server");
                }
                com.tplan.robot.ApplicationSupport.logFine(res.getString("com.tplan.robot.rfb.RfbModule.s2c.passwdSent"));

                int authResult = inStream.readInt();

                switch (authResult) {
                    case SECURITY_RESPONSE_OK:
                        com.tplan.robot.ApplicationSupport.logFine(res.getString("com.tplan.robot.rfb.RfbModule.s2c.authSucceeded"));
                        if (debug) {
                            System.out.println("S2C/SecurityResult: VNC Authentication result is OK");
                        }
                        return authResult;
                    case SECURITY_RESPONSE_FAILED:
                        if (debug) {
                            System.out.println("S2C/SecurityResult: VNC Authentication failed (code " + authResult + ")");
                        }
                        throw new Exception(ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.authFailed"));
                    case SECURITY_RESPONSE_TOO_MANY_ATTEMPTS:
                        if (debug) {
                            System.out.println("S2C/SecurityResult: VNC Authentication failed (Too Many Attempts - code " + authResult + ")");
                        }
                        throw new Exception(ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.tooManyFailures"));
                    default:
                        if (debug) {
                            System.out.println("S2C/SecurityResult: VNC Authentication failed (unknown code " + authResult + ")");
                        }
                        Object params[] = {new Integer(securityType)};
                        String pattern = ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.unknownAuthResult");
                        throw new Exception(MessageFormat.format(pattern, params));
                }

            default:
                Object params[] = {new Integer(securityType)};
                String pattern = ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.unsupportedSecurityType");
                throw new UnsupportedAuthenticationException(MessageFormat.format(pattern, params));
        }
    }

    /**
     * Reverse bit order in a byte. For example a value of -128 (binary 1000 000) will
     * be reverted to 1 (binary 0000 0001). This is needed for VNC authentication
     * because VNC servers use reverted bytes of password as DES encryption key.
     *
     * @param b a byte number.
     * @return byte value with reversed bit order.
     */
    private byte revert(byte b) {
        if (b == 0) {
            return b;  // Reverted zero is the same
        }
        byte result = 0;
        if (b < 0) {
            b += 128;
            result += 1;
        }

        if (b >= 64) {
            result += 2;
            b -= 64;
        }
        if (b >= 32) {
            result += 4;
            b -= 32;
        }
        if (b >= 16) {
            result += 8;
            b -= 16;
        }
        if (b >= 8) {
            result += 16;
            b -= 8;
        }
        if (b >= 4) {
            result += 32;
            b -= 4;
        }
        if (b >= 2) {
            result += 64;
            b -= 2;
        }
        if (b >= 1) {
            result -= 128;
        }
        return result;
    }

    /**
     * Write the ClientInit message.
     */
    private void writeClientInit() throws IOException {
        boolean shared = isSharedMode();
        if (shared) {
            synchronized (outStream) {
                outStream.write(CINIT_SHARE_DESKTOP);
            }
            com.tplan.robot.ApplicationSupport.logFine(res.getString("com.tplan.robot.rfb.RfbModule.s2c.sharedModeReqd"));
        } else {
            synchronized (outStream) {
                outStream.write(CINIT_EXCLUSIVE_DESKTOP);
            }
            com.tplan.robot.ApplicationSupport.logFine(res.getString("com.tplan.robot.rfb.RfbModule.s2c.exclusiveModeReqd"));
        }
        if (debug) {
            System.out.println("C2S/ClientInit: Client requested " + (shared ? "shared" : "exclusive")
                    + " desktop.");
        }
    }

    /**
     * Read the ServerInit message.
     */
    private void readServerInit() throws IOException {
        int width = inStream.readUnsignedShort();
        int height = inStream.readUnsignedShort();

        // Right now we don't decode the pixel format encoded in the next 16 bytes.
        // Evaluate if it is needed.
        byte[] temp = new byte[16];
        inStream.readFully(temp);

        byte[] name = new byte[inStream.readInt()];
        inStream.readFully(name);
        desktopName = new String(name);

        serverPixelFormat = new PixelFormat(width, height, temp, 0);

        // Debug code showing server's preferred pixel format
        if (debug) {
            String msg = "S2C/ServerInit: Connected to '" + desktopName + "' (" + getDesktopWidth() + "x" + getDesktopHeight() + ")"
                    + "S2C/ServerInit: Server suggests following pixel format:\n";
            msg += serverPixelFormat.toString();
            System.out.println(msg);
        }

        if (pixelFormat == null) {
            // If the pixel format wasn't preset, set it to the server one
            pixelFormat = serverPixelFormat;
        }
        createNewImage(width, height);
        fireRemoteServerEvent(new RemoteDesktopServerEvent(this, RemoteDesktopServerEvent.SERVER_INIT_EVENT));

        connected = true;
        Object args[] = {desktopName, height, width};
        com.tplan.robot.ApplicationSupport.logFine(
                MessageFormat.format(res.getString("com.tplan.robot.rfb.RfbModule.s2c.connectedTo"), args));
    }

    /**
     * Read the SetColorMapEntries message.
     */
    private void readSetColorMapEntries() throws IOException {

        // Read the 1-byte padding
        inStream.readByte();

        // First color is an offset
        int firstColor = inStream.readUnsignedShort();

        // Number of colors attached to the message, each color is 6 bytes
        int numberOfColors = inStream.readUnsignedShort();

        // Read the byte array with RGB values
        byte[] temp = new byte[6 * numberOfColors];
        int n = inStream.read(temp);

        // Debug code showing server's preferred pixel format
        if (debug) {
            String msg = "S2C/SetColorMapEntries: Server defines color map:";
            msg += "\n   First color: " + firstColor;
            msg += "\n   Number of colors:    " + numberOfColors; //numberOfColors;
            System.out.println(msg);
        }
        pixelFormat.setColorMap(firstColor, temp.length, temp);
    }

    public void setPixelFormat(byte bitsPerPixel,
            byte colorDepth, boolean bigEndian, boolean trueColor,
            int redMax, int greenMax, int blueMax,
            byte redShift, byte greenShift, byte blueShift) {

        PixelFormat pf = new PixelFormat(getDesktopWidth(), getDesktopHeight(),
                bitsPerPixel, colorDepth, bigEndian, trueColor, redMax, greenMax,
                blueMax, redShift, greenShift, blueShift);
        if (!isConnected()) {
            pixelFormat = pf;
        } else {
//            setPixelFormat(pf);
        }
    }

    private void setPixelFormat(PixelFormat pf) throws IOException {
        if (isConnected()) {

            byte buf[] = pf.toMessage();

            synchronized (outStream) {
                outStream.write(buf);
                pixelFormat = pf;
            }

            // Debug code
            if (debug) {
                String msg = "C2S/SetPixelFormat: Client requested pixel format:\n";
                msg += pf.toString();
                System.out.println(msg);
            }
        }
        pixelFormat = pf;
    }

    /**
     * Send current encoding list to the RFB server. Ignores invalid values or
     * unsupported encoding numbers.
     *
     * @param encs array of encoding types (code numbers as defined by the RFB protocol).
     */
    public void setEncodings(int encs[]) throws IOException {
        List<Integer> v = new ArrayList();
        for (int enc : encs) {
            if (encodingMap.containsKey(enc)) {
                v.add(new Integer(enc));
            }
        }

        if (v.size() != encs.length) {
            encs = new int[v.size()];
            int i = 0;
            for (Integer n : v) {
                encs[i++] = n.intValue();
            }
        }

        if (cfg != null && !manualEncodings) {
            cfg.setListOfNumbers("rfb.imageEncodings", v);
        } else {
            encodings = encs;
        }
        sendSetEncodings();
    }

    /**
     * Get an array of supported encoding type codes.
     * @return an array of supported encoding type codes.
     */
    public int[] getEncodings() {
        if (cfg != null && !manualEncodings) {
            int e[] = cfg.getArrayOfInts("rfb.imageEncodings");
            return e;
        }
        return encodings;
    }

    /**
     * <p>Implementation of the SetEncodings client-to-server RFB v3.3 message.
     * The method is supposed to send the encodings specfied by the array of integer
     * numbers where the array members are encoding type numbers defined by the
     * RFB protocol, such as 0 (Raw encoding), 1 (CopyRect) etc. The list
     * of encodings is obtained through the <code>getEncodings()</code> method
     * and can be modified through <code>setEncodings()</code>.</p>
     *
     * <p>The message is constructed and sent to the server as defined
     * in the RFB protocol:
     * <ul>
     * <li> byte[0] - message type (2)</li>
     * <li> byte[1] - padding (0)</li>
     * <li> byte[2]-byte[3] - number of encodings sent within this message</li>
     * <li> byte[4]-byte[n] - encoding codes in the order of preference. Each
     * encoding occupies 4 bytes.</li>
     * </ul>
     *
     * @throws java.io.IOException when an I/O error occurs.
     */
    private void sendSetEncodings() throws IOException {
        try {
            if (isConnected()) {
                if (!manualEncodings) {
                    encodings = cfg.getArrayOfInts("rfb.imageEncodings");
                }
                if (encodings != null && encodings.length > 0) {
                    // Fix in 2.0.6/2.3 - filter out those encodings which are not
                    // supported by the current set of encoding plugins.
                    // This resolves situation when one migrates to a Robot
                    // with a different supprted encoding set.
                    int size = encodings.length;
                    int enc;
                    List<Integer> l = new ArrayList();
                    for (int i = 0; i < size; i++) {
                        enc = encodings[i];
                        if (encodingMap.containsKey(enc)) {
                            l.add(enc);
                        }
                    }
                    size = l.size();
                    if (l.size() != encodings.length) {
                        encodings = new int[size];  // Can't use List.toArray() because it is int[] and not Integer[]
                        for (int i = 0; i < size; i++) {
                            encodings[i] = l.get(i);
                        } // -- End of fix
                    }

                    byte[] b = new byte[4 * (size + 1)];

                    b[0] = (byte) MSG_C2S_SET_ENCODINGS;
                    b[3] = (byte) size;  // We ignore b[2] because there are never more than 256 encodings

                    int position = 4;
                    String debugMsg = "";
                    for (int i = 0; i < size; i++) {
                        enc = encodings[i];
                        if (debug) {
                            Encoding e = encodingMap.get(enc);
                            debugMsg += e == null ? "unknown code " + enc : ((Plugin) e).getDisplayName() + (i == size - 1 ? "" : ",");
                        }
                        b[position] = (byte) ((enc >> 24) & BYTEMASK);
                        b[position + 1] = (byte) ((enc >> 16) & BYTEMASK);
                        b[position + 2] = (byte) ((enc >> 8) & BYTEMASK);
                        b[position + 3] = (byte) (enc & BYTEMASK);
                        position += 4;
                    }
                    if (debug) {
                        System.out.println("C2S/SetEncodings: Client preferred encodings sent: " + debugMsg);
                    }
                    synchronized (outStream) {
                        outStream.write(b);
                    }
                }
                com.tplan.robot.ApplicationSupport.logFine(res.getString("com.tplan.robot.rfb.RfbModule.c2s.encodingsSent"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>Implementation of the ServerCutText server-to-client RFB v3.3 message.</p>
     *
     * @return text of the server clipboard.
     * @throws java.io.IOException when an I/O error occurs.
     */
    private String readServerCutText() throws IOException {
        // Read the 3-byte padding
        byte[] pad = new byte[3];
        inStream.readFully(pad);

        // Read length of the text (int value)
        int len = inStream.readInt();

        // Read the text of specified length
        byte[] text = new byte[len];
        inStream.readFully(text);
        if (debug) {
            System.out.println("S2C/ServerCutText: Text received: '" + text + "'");
        }
        return new String(text);
    }

    /**
     * <p>Implementation of the ClientCutText client-to-server RFB v3.3 message.</p>
     *
     * @param text of the client clipboard.
     * @throws java.io.IOException when an I/O error occurs.
     */
    public void sendClientCutText(String text) throws IOException {
        if (text != null && text.length() > 0) {
            int length = text.length();

            // Buffer size is 8 bytes for control data plus text length
            byte[] buf = new byte[length + 8];

            buf[0] = (byte) MSG_C2S_CLIENT_CUT_TEXT;
            buf[4] = (byte) ((length >> 24) & BYTEMASK);
            buf[5] = (byte) ((length >> 16) & BYTEMASK);
            buf[6] = (byte) ((length >> 8) & BYTEMASK);
            buf[7] = (byte) (length & BYTEMASK);

            System.arraycopy(text.getBytes(), 0, buf, 8, length);
            synchronized (outStream) {
                outStream.write(buf);
            }
        }
    }

    /**
     * <p>Implementation of the FrameBufferUpdateRequest client-to-server
     * RFB v3.3 message. It asks the server for update of the given image
     * rectangle.</p>
     *
     * @param rect image rectangle which the client is interested in.
     * @param incremental true allows the server to send the update in several
     * smaller messages, false indicates that the whole image rectangle data
     * should be sent within one single message.
     *
     * @throws java.io.IOException when an I/O error occurs.
     */
    public void sendFramebufferUpdateRequest(Rectangle rect, boolean incremental) throws IOException {

        // The message size is 10 bytes - message code, the incremental flag
        // and rectangle coordinates (2 bytes each).
        byte[] b = new byte[10];
        b[0] = (byte) MSG_C2S_FRAMEBUFFER_UPDATE_REQUEST;
        b[1] = (byte) (incremental ? 1 : 0);
        b[2] = (byte) ((rect.x >> 8) & BYTEMASK);
        b[3] = (byte) (rect.x & BYTEMASK);
        b[4] = (byte) ((rect.y >> 8) & BYTEMASK);
        b[5] = (byte) (rect.y & BYTEMASK);
        b[6] = (byte) ((rect.width >> 8) & BYTEMASK);
        b[7] = (byte) (rect.width & BYTEMASK);
        b[8] = (byte) ((rect.height >> 8) & BYTEMASK);
        b[9] = (byte) (rect.height & BYTEMASK);

        synchronized (outStream) {
            outStream.write(b);
        }

        // Enable debug messages only for non incremental messages to avoid
        // high volume of print out
        if (debug && !incremental) {
            System.out.println("C2S/FramebufferUpdateRequest: Update of [x:" + rect.x + ",y:" +
                    rect.y + ",w:" + rect.width + ",h:" + rect.height + "] requested, incremental=" + incremental);
        }
    }

    public void sendPointerEvent(MouseEvent evt) throws IOException {
        sendPointerEvent(evt, false);
    }

    /**
     * Send a PointerEvent to the RFB server based on a Java MouseEvent.
     *
     * @param evt a MouseEvent or MouseWheelEvent.
     * @throws java.io.IOException when an I/O error occurs.
     */
    public void sendPointerEvent(MouseEvent evt, boolean sendModifiers) throws IOException {
        this.lastMouseEvent = evt;

        boolean isWheel = false; // If true we'll send both down and up events
        int clicks = 1;  // How many clicks of the mouse wheel
        int outBufOffset = 0;

        final int btn = evt.getButton();

        // Now code the button mask. Each bit of this byte corresponds to
        // a mouse button where a bit value of 0 is up and 1 is down.
        // Bits 0, 1 and 2 correspond on a traditional mouse to the left,
        // middle and right buttons. On a wheel mouse we have to generate
        // two events (down and up) at bit 3 (upward wheel movement)
        // or bit 4 (downward) for each step of the wheel.
        int btnMask = 0;

        switch (evt.getID()) {
            case MouseEvent.MOUSE_PRESSED:
            case MouseEvent.MOUSE_DRAGGED:
                // Default value is left button (1)
                btnMask = 1;
                if (btn == MouseEvent.BUTTON1) {
                    btnMask = 1;  // Bit 0
                } else if (btn == MouseEvent.BUTTON2) {
                    btnMask = 2;  // Bit 1
                } else if (btn == MouseEvent.BUTTON3) {
                    btnMask = 4;  // Bit 2
                }
                break;
            case MouseEvent.MOUSE_WHEEL:
                isWheel = true;
                MouseWheelEvent mwe = (MouseWheelEvent) evt;
                if (mwe.getWheelRotation() < 0) {
                    btnMask = 8;  // Negative value indicates upward wheel click
                } else {
                    btnMask = 16; // Positive value indicates downward wheel click
                }
                // How many clicks to send
                clicks = Math.abs(mwe.getWheelRotation());
                break;
        }

        // Fix in 2.0.1: do not press the modifier keys unless required. This caused
        // Meta (the Windows key) to be sent by right mouse click in normal user interaction.
        // Events generated by the Mouse command however may press it.

        // First write the modifiers
        if (sendModifiers) {
            outBufOffset = bufferModifiers(outBuf, outBufOffset, evt);
        }
        // Now construct the buffer for the pointer event
        final int x = Math.max(evt.getX(), 0);
        final int y = Math.max(evt.getY(), 0);

        if (isWheel) { // A mouse wheel event
            // Send down and up event pair for each click of the mouse wheel
            for (int i = 0; i < clicks; i++) {
                outBufOffset = bufferPointerEvent(outBuf, outBufOffset, btnMask, x, y);
                outBufOffset = bufferPointerEvent(outBuf, outBufOffset, 0, x, y);
            }
        } else {  // A single mouse event
            outBufOffset = bufferPointerEvent(outBuf, outBufOffset, btnMask, x, y);
        }

        if (sendModifiers && previousEventModifiers == 0) { // Release all modifiers
            outBufOffset = bufferModifiers(outBuf, outBufOffset, null);
        }

        synchronized (outStream) {
            outStream.write(outBuf, 0, outBufOffset);
        }
//        if (evt.getID() != MouseEvent.MOUSE_MOVED) {
//        System.out.println("sendPointerEvent2(event " + evt + "): \n  " + Arrays.toString(outBuf));
//        }

        fireRemoteClientEvent(new RemoteDesktopClientEvent(this, evt));
        com.tplan.robot.ApplicationSupport.logFine("C2S: Mouse event sent, [" + evt.paramString() + "]");
    }

    /**
     * Write binary representation of a key event of given key code and status
     * (key press or release) to a byte buffer.
     *
     * @param buffer a byte array to write to.
     * @param offset an offset (starting index) to start writing at.
     * @param pressed true is key press, false is release.
     * @param buttonMask a mask indicating status of muse buttons. See
     * the PointerEvent chapter of the RFB 3.3 specification.
     * @param x
     * @param y
     * @return a new offset incremented by the lenght of written data.
     */
    private int bufferPointerEvent(byte[] buffer, int offset, int buttonMask, int x, int y) {
        buffer[offset] = (byte) MSG_C2S_POINTER_EVENT;
        buffer[offset + 1] = (byte) buttonMask;
        buffer[offset + 2] = (byte) ((x >> 8) & BYTEMASK);
        buffer[offset + 3] = (byte) (x & BYTEMASK);
        buffer[offset + 4] = (byte) ((y >> 8) & BYTEMASK);
        buffer[offset + 5] = (byte) (y & BYTEMASK);
        return offset + 6;
    }

    private boolean isModifier(KeyEvent evt) {
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_CONTROL:
            case KeyEvent.VK_ALT:
            case KeyEvent.VK_META:
                return true;
        }
        return false;
    }

    public void sendKeyEvent(KeyEvent evt) throws IOException {

        Arrays.fill(outBuf, (byte) 0);

        if (System.getProperty("debug.sendKeyEvent") != null) {
            System.out.println("-------------------------------------------------\nKey event: " + evt);
        }
        int key = convertJavaKeyEventToRfbKeyCode(evt);

        // Keys less than zero mean that we should ignore them.
        // An example of such a key is CapsLock.
        if (key < 0) {
            return;
        }

        boolean allowJustModifiers = true;

        final int eventKeyCode = evt.getKeyCode();
        final boolean justModifiers = eventKeyCode == KeyEvent.VK_CONTROL || eventKeyCode == KeyEvent.VK_ALT || eventKeyCode == KeyEvent.VK_SHIFT || eventKeyCode == KeyEvent.VK_META;


        if (justModifiers && !allowJustModifiers) {
            return;
        }

        // Key modifiers (Alt/Shift/Ctrl/Meta)
        // We are using extended modifiers to be in sync with mouse event handling
        int modifiers = evt.getModifiersEx();

        // Bug fix in 2.0.1
        final boolean hasShift = (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0;

        if (modifiers != 0) {
            Boolean b = UserConfiguration.getInstance().getBoolean("rfb.convertKeysWithModifiersToLowerCase");
            if (b == null || b) {

                // If there's a Shift present and the character is a lower case letter,
                // convert it to upper case. This is what most systems expect.
                if (hasShift) {
                    if (key >= 'a' && key <= 'z') {
                        key = Character.toUpperCase(key);
                    }
                } else if (key >= 'A' && key <= 'Z') {  // Otherwise convert it to lower case to make keys like Ctrl+O send Ctrl+o
                    key = Character.toLowerCase(key);
                }
            }
        }

        final boolean isKeyPress = (evt.getID() == KeyEvent.KEY_PRESSED);

        // First write the "down" events for the modifier keys into buffer
        int outBufOffset = bufferModifiers(outBuf, 0, evt);

        // Write the key itself unless it was a single modifier key
        if (!justModifiers) {
            outBufOffset = bufferKeyEvent(outBuf, outBufOffset, key, isKeyPress);
        }

        // If the event is "up", release all modifier keys, i.e. write
        // an "up" event for each pressed modifier
        if (!isKeyPress) {
            outBufOffset = bufferModifiers(outBuf, outBufOffset, null);
        }

        synchronized (outStream) {
            outStream.write(outBuf, 0, outBufOffset);
        }
        if (System.getProperty("debug.sendKeyEvent") != null) {
            System.out.println("-------------------------------------------------\n");
            System.out.flush();
        }
        fireRemoteClientEvent(new RemoteDesktopClientEvent(this, evt));
        com.tplan.robot.ApplicationSupport.logFine("C2S: KeyEvent sent, " + KeyStroke.getKeyStrokeForEvent(evt).toString());
    }

    /**
     * Write binary representation of a key event of given key code and status
     * (key press or release) to a byte buffer.
     *
     * @param buffer a byte array to write to.
     * @param offset an offset (starting index) to start writing at.
     * @param keyCode a key code as specified in the KeyEvent chapter of the
     * RFB 3.3 (3.8) protocol.
     * @param pressed true is key press, false is release.
     * @return a new offset incremented by the lenght of written data.
     */
    private int bufferKeyEvent(byte buffer[], int offset, int keyCode, boolean pressed) {
        if (System.getProperty("debug.sendKeyEvent") != null) {
            System.out.println("  bufferKeyEvent(): key=" + Utils.getKeyName(keyCode) + " (0x" + Integer.toHexString(keyCode) + "), pressed=" + pressed);
        }
        buffer[offset] = (byte) MSG_C2S_KEY_EVENT;
        buffer[offset + 1] = (byte) (pressed ? 1 : 0);
        buffer[offset + 2] = (byte) 0;
        buffer[offset + 3] = (byte) 0;
        buffer[offset + 4] = (byte) ((keyCode >> 24) & BYTEMASK);
        buffer[offset + 5] = (byte) ((keyCode >> 16) & BYTEMASK);
        buffer[offset + 6] = (byte) ((keyCode >> 8) & BYTEMASK);
        buffer[offset + 7] = (byte) (keyCode & BYTEMASK);
        return offset + 8;
    }

    /**
     * Write binary representation of all modifier key events into a buffer.
     * A single key event is written for each modifier like Alt, Ctrl, Shift
     * or Meta.
     *
     * @param buffer a byte array to write to.
     * @param offset an offset (starting index) to start writing at.
     * @param evt an input event holding the modifier flags. It can be a
     * KeyEvent, MouseEvent or MouseWheelEvent.
     * @return a new offset incremented by the lenght of written data.
     */
    private int bufferModifiers(byte buffer[], int offset, InputEvent evt) {
        // We have to use extended modifiers mask because some mouse button
        // masks are in conflict with the Alt and Meta ones
        int m = evt == null ? 0 : evt.getModifiersEx();

        // Only send those masks which have changed from the previous event
        if ((m & CTRL_DOWN_MASK) != (previousEventModifiers & CTRL_DOWN_MASK)) {
            // A constant for Ctrl defined in RFB protocol is 0xFFE3
            offset = bufferKeyEvent(buffer, offset, 0xFFE3, (m & CTRL_DOWN_MASK) != 0);
        }

        if ((m & ALT_DOWN_MASK) != (previousEventModifiers & ALT_DOWN_MASK)) {
            // When the middle mouse button is released, Java 1.5 generates
            // a false Alt modifier. Skip it.
            if (!((m & BUTTON2_DOWN_MASK) != 0 && evt.getID() == MouseEvent.MOUSE_RELEASED)) {
                // A constant for Ctrl defined in RFB protocol is 0xFFE9
                offset = bufferKeyEvent(buffer, offset, 0xFFE9, (m & ALT_DOWN_MASK) != 0);
            }
        }

        if ((m & SHIFT_DOWN_MASK) != (previousEventModifiers & SHIFT_DOWN_MASK)) {
            // A constant for Shift defined in RFB protocol is 0xFFE1
            offset = bufferKeyEvent(buffer, offset, 0xFFE1, (m & SHIFT_DOWN_MASK) != 0);
        }

        if ((m & META_DOWN_MASK) != (previousEventModifiers & META_DOWN_MASK)) {
            // When the right mouse button is released, Java 1.5 generates
            // a false Meta modifier. Skip it.
            if (!((m & BUTTON3_DOWN_MASK) != 0 && evt.getID() == MouseEvent.MOUSE_RELEASED)) {
                // A constant for Meta defined in RFB protocol is 0xFFEB
                offset = bufferKeyEvent(buffer, offset, 0xFFEB, (m & META_DOWN_MASK) != 0);
            }
        }
        previousEventModifiers = m;
        return offset;
    }

    /**
     * VNC protocol uses key mapping defined in /usr/X11R6/include/X11/keysymdef.h.
     * Java provides different codes for certain keys (mainly the control/action ones)
     * and the following method provides a way of mapping among them.
     *
     * The method takes advantage of array of special codes defined in
     * com.tplan.robot.api.rfb.RfbConstants. The codes are loaded in form of
     * a Map where the key code defined by the KeyEvent.getKeyCode() method
     * is a hash key and corresponding code defined in the keysymdef.h is
     * the hash table value.
     *
     * If the method returns a valid key code (>0) for the given event,
     * it should be sent to the RFB server rather than the original Java defined value.
     *
     * If the method returns -1, the key press should be ignored and nothing is
     * to be sent to the RFB server. Such keys typically represent modifiers like
     * Alt, Shift, Controll or Meta or keys we don't want to expose.
     *
     * @param evt a Key Event
     * @return an RFB recognized key code corresponding to the Java KeyEvent.
     * If -1 is returned, the key press should be ignored.
     */
    private int convertJavaKeyEventToRfbKeyCode(KeyEvent evt) {

        // Fix in 2.0.1: if the key code equals to a letter or digit,
        // return it because both Java and RFB identify these chars by the ASCII code.
        int code = evt.getKeyCode();
        if (Character.isLetter(evt.getKeyChar())) {
            return (int) evt.getKeyChar();
        }
        // Update in 2.0.3 - digits go right through only if they are not on numpad
        if (Character.isDigit(evt.getKeyChar())) {
            if (evt.getKeyLocation() != KeyEvent.KEY_LOCATION_NUMPAD) {
                return (int) evt.getKeyChar();
            }
        }

        // Load the map of special key codes if it hasn't yet been loaded
        if (specialKeyCodeMap == null) {
            specialKeyCodeMap = loadSpecialKeyCodes();
        }

        int key = -1;

        // Look for the key in the table of special key codes.
        // If the key is not there, use its normal ASCII value.
        // If the returned code is < 0, return.
        Integer specKeyCode = null;
        Object o = specialKeyCodeMap.get(code);
        if (o != null) {
            if (o instanceof Integer) {
                specKeyCode = (Integer) o;
            } else if (o instanceof Map) {
                specKeyCode = (Integer) ((Map) o).get(evt.getKeyLocation());
                if (specKeyCode == null) {
                    specKeyCode = (Integer) ((Map) o).get(KeyEvent.KEY_LOCATION_STANDARD);
                }
            }
        }
        // Process a non-special key press
        if (specKeyCode != null) {
            key = specKeyCode.intValue();
        } else {
            // Allow just action keys which are properly defined through the
            // table of special key codes.
            if (evt.isActionKey()) {
                key = -1;
            } else {
                // Normal ASCII key, just watch out for pressed Ctrl which shifts
                // the char code for values 0-35 by 0x60
                final int keyChar = evt.getKeyChar() == 0 ? KeyEvent.CHAR_UNDEFINED : evt.getKeyChar();
                if (keyChar != KeyEvent.CHAR_UNDEFINED) {
                    key = keyChar < 0x20 && evt.isControlDown() ? keyChar + 0x60 : keyChar;

                    // Enhancement in 2.0.3 - check the special key code map for
                    // key location specific mapping. This allows to map for example
                    // digits and numeric operator characters onto numpad keys.
                    int location = evt.getKeyLocation();
                    if (location != KeyEvent.KEY_LOCATION_UNKNOWN && location != KeyEvent.KEY_LOCATION_STANDARD) {
                        o = specialKeyCodeMap.get(key);
                        if (o != null) {
                            if (o instanceof Map) {
                                specKeyCode = (Integer) ((Map) o).get(location);
                                if (specKeyCode != null) {
                                    key = specKeyCode;
                                }
                            }
                        }
                    } // End of enhancement

                } else {
                    key = code;
                }
            }
        }
        return key;
    }

    /**
     * Load special key codes from the array defined in
     * com.tplan.robot.api.rfb.RfbConstants. The codes represent a map among key
     * codes used by RFB and Java and they are loaded in form of
     * a Map where the key code defined by the KeyEvent.getKeyCode() method
     * is a hash key and corresponding code defined in the keysymdef.h is
     * the hash table value.
     *
     * @return  a Map with special key code mapping where the key code
     * defined by the KeyEvent.getKeyCode() method is a hash key and
     * corresponding code defined in the keysymdef.h is the value.
     */
    private Map<Integer, Object> loadSpecialKeyCodes() {
        Map<Integer, Object> t = new HashMap();
        int entry[];
        Integer key, code, location;
        for (int i = 0; i < SPECIAL_KEY_CODES.length; i++) {
            entry = SPECIAL_KEY_CODES[i];
            key = new Integer(entry[0]);
            code = new Integer(entry[1]);
            location = entry.length == 3 ? entry[2] : KeyEvent.KEY_LOCATION_STANDARD;
            if (t.containsKey(key) || location != KeyEvent.KEY_LOCATION_STANDARD) {
                Object o = t.get(key);
                if (o instanceof Map) {
                    ((Map) o).put(location, code);
                } else if (o == null || o instanceof Integer) {
                    Map m = new HashMap();
                    m.put(location, code);
                    if (o != null) {
                        m.put(KeyEvent.KEY_LOCATION_STANDARD, o);
                    }
                    t.put(key, m);
                }
            } else {
                t.put(key, code);
            }
        }
        return t;
    }

    /**
     * Implementation of the Runnable interface.
     */
    public void run() {
        try {
            handleS2cMessages();

        } catch (RfbException ex) {
            // EOFException indicates that server closed the connection. We need to report it to user.
            // Reporting depends on whether we are running in the console or GUI mode
            com.tplan.robot.ApplicationSupport.logSevere(ex.getMessage());
            if (isConsoleMode()) {
                System.out.println(MessageFormat.format(res.getString("com.tplan.robot.rfb.RfbModule.cli.exceptionMsg"), ex.getMessage()));
            } else {
                fireRemoteServerEvent(new RemoteDesktopServerEvent(this, ex));
            }

        } catch (IOException e) {
            // I/O error happen when RFB module is getting closed
            // while a message is being processed
            if (isConnected()) {
                com.tplan.robot.ApplicationSupport.logSevere(e.getMessage());
                if (isConsoleMode()) {
                    System.out.println(MessageFormat.format(res.getString("com.tplan.robot.rfb.RfbModule.cli.IOexceptionMsg"), e.getMessage()));
                    e.printStackTrace();
                } else {
                    fireRemoteServerEvent(new RemoteDesktopServerEvent(this, e));
                }
            }
        } catch (Exception ex) {
            com.tplan.robot.ApplicationSupport.logSevere(ex.getMessage());
            if (isConsoleMode()) {
                System.out.println(MessageFormat.format(res.getString("com.tplan.robot.rfb.RfbModule.cli.unknownExceptionMsg"), ex.getMessage()));
                ex.printStackTrace();
            } else {
                fireRemoteServerEvent(new RemoteDesktopServerEvent(this, ex));
            }
        } finally {
            try {
                close();
            } catch (IOException ex) {
            }
        }
    }

    private BufferedImage createNewImage(int fbWidth, int fbHeight) {

        fbWidth = fbWidth == 0 ? 800 : fbWidth;
        fbHeight = fbHeight == 0 ? 600 : fbHeight;

        // Create new off-screen image either if it does not exist, or if
        // its geometry should be changed. It's not necessary to replace
        // existing image if only pixel format should be changed.
        if (image == null) {
            image = createImage(fbWidth, fbHeight);
        } else if (image.getWidth(null) != fbWidth
                || image.getHeight(null) != fbHeight) {
            synchronized (image) {
                image = createImage(fbWidth, fbHeight);
            }
        }
        fireRemoteServerEvent(new RemoteDesktopServerEvent(this, new Rectangle(0, 0, fbWidth, fbHeight)));
        return image;
    }

    private BufferedImage createImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * <p>Process server-to-client messages. Once the handshake and authentication
     * stage is over, server keeps sending messages about buffer updates to the client.
     * The very first byte of the message determines the message type. This method
     * reads the type and then dispatches the message to an appropriate method
     * which will process the graphical data and update the desktop image.
     * <p/>
     * <p>This method is also a central point of RFB I/O error handling. If an expected
     * error happens during the communication with server, the method loads
     * a user understandable message text from the app resource bundle and encapsulates
     * it into a RfbException instance. GUI will then catch this exception and report
     * it to the user.
     * <p/>
     * <p>Any unexpected exception is thrown without any changes. Such an error should be
     * considered an internal error and reported to user in an appropriate way.
     *
     * @throws Exception A RfbException is thrown when a known error happens,
     *                   e.g. the server closes connection either the standard way (socket gets closed)
     *                   or unexpectedly (server crashes or gets restarted - EOF is reached in socket
     *                   communication). Text of any RfbException instance is loaded from the
     *                   property file and may be localized or customized.
     *                   <p/>
     *                   <p>Any other exception thrown by this method indicates an internal
     *                   error and should be reported this way.
     */
    private void handleS2cMessages() throws Exception {

        try {
            // First ask for a full (non-incremental) update
            sendFramebufferUpdateRequest(new Rectangle(getDesktopWidth(), getDesktopHeight()), false);
            com.tplan.robot.ApplicationSupport.logFine("C2S: requesting initial full image of the remote desktop");

            // These variables are used to cache the coordinates and encoding of updated rectangles
            int rx, ry, rw, rh, enc;

            // Helper variables used for message type code and number of
            // rectangles received within the FrameBufferUpdate message.
            int msgType, rectCount;

            // Read & process messages while the RFB connection is open
            while (isConnected() && !Thread.interrupted()) {

                // Read message type from the server.
                msgType = inStream.readUnsignedByte();

                // Process the message depending on its type.
                switch (msgType) {
                    case MSG_S2C_FRAMEBUFFER_UPDATE:

                        // Skip the first byte and then read a short
                        inStream.readByte();
                        rectCount = inStream.readUnsignedShort();

                        com.tplan.robot.ApplicationSupport.logFine("S2C: FramebufferUpdate event received, rectangle count is " + rectCount + ":");

                        for (int i = 0; i < rectCount; i++) {

                            // Read the coordinates and encoding of each tile (subrect) received
                            rx = inStream.readUnsignedShort();
                            ry = inStream.readUnsignedShort();
                            rw = inStream.readUnsignedShort();
                            rh = inStream.readUnsignedShort();
                            enc = inStream.readInt();

                            // Get the encoding handler based on the code
                            Encoding encodingHandler = encodingMap.get(enc);
                            if (isConnected()) {
                                if (encodingHandler != null) {
                                    encodingHandler.updateImage(image, inStream, pixelFormat, rx, ry, rw, rh);
                                    com.tplan.robot.ApplicationSupport.logFine("  Update rect #" + i + ", encoding=" + ((Plugin) encodingHandler).getDisplayName() + ", [x,y,w,h]=[" + rx + "," + ry + "," + rw + "," + rh + "]");

                                } else {
                                    // This should never happen because the server
                                    // agreed to communicate in our protocol version.
                                    Object params[] = {
                                        getHost(),
                                        enc,
                                        majorVersion + "." + minorVersion
                                    };
                                    String msg = MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.unknownRectangleEncoding"), params);
                                    throw new RfbException(msg);
                                }
                                fireRemoteServerEvent(new RemoteDesktopServerEvent(this, new Rectangle(rx, ry, rw, rh)));
                            }
                        }

                        sendFramebufferUpdateRequest(new Rectangle(getDesktopWidth(), getDesktopHeight()), true);
                        break;

                    case MSG_S2C_SET_COLOR_MAP_ENTRIES:
                        // Color maps implemented in VNCRobot 1.3.12
                        readSetColorMapEntries();
                        sendFramebufferUpdateRequest(new Rectangle(getDesktopWidth(), getDesktopHeight()), false);
                        break;

                    case MSG_S2C_BELL:
                        receiveBell();
                        break;

                    case MSG_S2C_SERVER_CUT_TEXT:
                        String s = readServerCutText();
                        //System.out.println("MSG_S2C_SERVER_CUT_TEXT"+s);
                        fireRemoteServerEvent(new RemoteDesktopServerEvent(this, s));
                        com.tplan.robot.ApplicationSupport.logFine("S2C: ServerCutText event received: " + s);
                        break;

                    default:
                        // This means that unknown message type was received.
                        // This should never happen because the server
                        // agreed to communicate in our protocol version.
                        Object pars[] = {new Integer(msgType), host};
                        String m = MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.unknownRfbMsgType"), pars);
                        com.tplan.robot.ApplicationSupport.logSevere(m);
                        close();
                        throw new Exception(m);
                }
            }
        } catch (SocketException ex) {
            // This error happens when the connection gets closed.
            // If it was closed by purpose, rfbModule.isConnected() must return false.
            // Otherwise it is an error and must be reported.
            if (isConnected()) {
                close();
                Object params[] = {host};
                String msg = MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.socketClosed"), params);
                throw new RfbException(msg);
            } else {
                return;
            }
        } catch (SocketTimeoutException ex) {
            // This error happens when the connection gets closed.
            // If it was closed by purpose, rfbModule.isConnected() must return false.
            // Otherwise it is an error and must be reported.
            if (isConnected()) {
                close();
                Object params[] = {host};
                String msg = MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.socketClosed"), params);
                throw new RfbException(msg);
            } else {
                return;
            }
        } catch (EOFException ex) {
            // EOFException indicates that server closed the connection.
            // Close the RFB server and report the problem to user.
            close();
            Object params[] = {host};
            String msg = MessageFormat.format(ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.serverClosedConnection"), params);
            throw new RfbException(msg);

        } catch (IOException ex) {
            if (isConnected()) {
                throw ex;
            }

        } catch (Exception ex) {
            if (isConnected()) {
                throw ex;
            }
        }
    }

    /**
     * Dummy implementation of the ImageObserver interface. The method does
     * nothing and returns just true.
     *
     * @param img not used.
     * @param infoflags not used.
     * @param x not used.
     * @param y not used.
     * @param width not used.
     * @param height not used.
     * @return always returns true.
     */
    public boolean imageUpdate(Image img, int infoflags,
            int x, int y, int width, int height) {
        return true;
    }

    public void configurationChanged(ConfigurationChangeEvent evt) {
        if (evt.getPropertyName().equals("rfb.imageEncodings")) {
            try {
                if (isConnected() && !manualEncodings) {
                    sendSetEncodings();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else if (evt.getPropertyName().equals("rfb.useCustomPixelFormat")
                || evt.getPropertyName().equals("rfb.customPixelFormat")) {
            try {
                if (isConnected()) {
                    PixelFormat pf = getConfigPixelFormat();
                    if (pf == null) {
                        // If custom format is null, use the server one
                        pf = serverPixelFormat;
                    }
                    // Send the new pixel format
                    setPixelFormat(pf);

                    // Refresh the remote desktop
                    sendFramebufferUpdateRequest(new Rectangle(0, 0, getDesktopWidth(), getDesktopHeight()), false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get the last mouse event sent to the RFB server. This is used by some components to find out the probable mouse
     * pointer coordinates.
     * <p>Note that the coordinates determined this way might be wrong. Itf there are more clients (VNC viewers)
     * connected to the RFB server in a shared mode, this module doesn't get the information about mouse events fired
     * by other clients.
     *
     * @return last mouse event sent to the RFB server.
     */
    public MouseEvent getLastMouseEvent() {
        return lastMouseEvent;
    }

    /**
     * Get protocol implemented by this client ("RFB").
     * <p/>
     *
     * @return the highest supported version of the RFB protocol. This instance always returns "RFB 003.003\n"
     */
    public String getProtocol() {
        return "RFB";
    }

    /**
     * Get the host name of the target RFB server.
     *
     * @return RFB server host name.
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the host name of a RFB server to connect to. E.g. to connect to a server called myserver.mydomain.com:2
     * set the host to myserver.mydomain.com and port to 5902.
     *
     * @param host host name. It may be a server name or IP number.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get the port of the target RFB server. Note that it is the real port, not the display number.
     * If you are connecting e.g. to a server called myserver.mydomain.com:2, the port is 5902.
     *
     * @return RFB server port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port of the target RFB server. Note that it is the real port, not the display number.
     * If you are connecting e.g. to a server called myserver.mydomain.com:2, the port is 5902.
     *
     * @param port RFB server port number.
     */
    public void setPort(int port) {
        if (port < 0) {
            this.port = RFB_PORT_OFFSET;
        } else {
            if (port > 0xFFFE) {
                throw new IllegalArgumentException(res.getString("com.tplan.robot.rfb.RfbModule.errInvalidPort"));
            }
            this.port = port;
        }
    }

    /**
     * Get the remote desktop name. It is only available if the module is connected to an RFB server.
     *
     * @return remote desktop name if connected to an RFB server, null otherwise.
     */
    public String getDesktopName() {
        return desktopName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String passwd) {
        this.password = passwd;
    }

    /**
     * Get the remote frame buffer image. This implementation always returns an RGB BufferedImage instance.
     *
     * @return remote frame buffer image.
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Get the array of encoding types supported by this module. This method should be used by other components
     * to validate e.g. the user encoding preferences.
     * <p>This implementation supports six encodings: Raw (type=0), CopyRect (1),
     * RRE (2), CoRRE (4), Hextile (5) and Zlib (6).
     *
     * @return an array with encoding types supported by this module. Each array member is an integer specifying
     *         the encoding type number defined in the SetEncodings chapter of the RFB 3.3 protocol.
     */
    public Map<Integer, Encoding> getSupportedEncodingMap() {
        return new HashMap<Integer, Encoding>(encodingMap);
    }

    public boolean isConsoleMode() {
        return consoleMode;
    }

    public void setConsoleMode(boolean consoleMode) {
        this.consoleMode = consoleMode;
    }

    public void setConfiguration(UserConfiguration configuration) {
        if (this.cfg != null) {
            cfg.removeConfigurationListener(this);
        }
        this.cfg = configuration;
        if (configuration != null) {
            configuration.addConfigurationListener(this);
        }
    }

    public boolean isSharedMode() {
        if (cfg != null) {
            Boolean b = cfg.getBoolean("rfb.sharedDesktop");
            if (b != null) {
                return b.booleanValue();
            }
        }
        return sharedMode;
    }

    public void setSharedMode(boolean sharedMode) {
        if (cfg != null) {
            cfg.setBoolean("rfb.sharedDesktop", sharedMode);
        } else {
            this.sharedMode = sharedMode;
        }
    }

    public int getDesktopWidth() {
        return image == null ? 0 : image.getWidth();
    }

    public int getDesktopHeight() {
        return image == null ? 0 : image.getHeight();
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isConnecting() {
        return connecting;
    }

    public int getDefaultPort() {
        return RFB_PORT_OFFSET;
    }

    public String getCode() {
        return "RFB";
    }

    public String getDescription() {
        return ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.pluginDesc");
    }

    public String getVendorName() {
        return ApplicationSupport.APPLICATION_NAME;
    }

    public String getSupportContact() {
        return ApplicationSupport.APPLICATION_SUPPORT_CONTACT;
    }

    public int[] getVersion() {
        return Utils.getVersion();
    }

    public Class getImplementedInterface() {
        return RemoteDesktopClient.class;
    }

    public boolean requiresRestart() {
        return true;
    }

    public String getVendorHomePage() {
        return ApplicationSupport.APPLICATION_HOME_PAGE;
    }

    public Date getDate() {
        return Utils.getReleaseDate();
    }

    public String getDisplayName() {
        return ApplicationSupport.getString("com.tplan.robot.rfb.RfbModule.pluginName");
    }

    public String getUniqueId() {
        return "VNCRobot_native_RFB_Client";
    }

    public int[] getLowestSupportedVersion() {
        return Utils.getVersion();
    }

    public String getMessageBeforeInstall() {
        return null;
    }

    public String getMessageAfterInstall() {
        return null;
    }

    public void pluginEvent(PluginEvent e) {
        if (e.getPluginInfo().getImplementedInterface().equals(Encoding.class)) {
            this.encodingMap = initEncodings();
        }
    }

    // TODO: move messages to the separate bundle
    public List<Preference> getPreferences() {
        List<Preference> l = new ArrayList();
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        String containerName = res.getString("options.rfb.sessionPreferences");
        Preference o;
        o = new Preference("rfb.sharedDesktop", Preference.TYPE_BOOLEAN,
                res.getString("options.rfb.sharedDesktop"),
                res.getString("options.rfb.sharedDesktopDesc"));
        o.setPreferredContainerName(containerName);
        l.add(o);

        // New since 1.3.1
        o = new Preference("rfb.beepOnBell", Preference.TYPE_BOOLEAN,
                res.getString("options.rfb.beepOnBell"), null);
        o.setPreferredContainerName(containerName);
        l.add(o);

        o = new Preference("rfb.imageEncodings", Preference.TYPE_STRINGLIST,
                res.getString("options.rfb.imageEncodings"), null);
        o.setPreferredContainerName(containerName);

        // Create a table with descriptions of supported encodings
        Map t = new HashMap();

        RfbEncodingFactory fact = RfbEncodingFactory.getInstance();
        Encoding enc;
        for (Number n : fact.getSupportedEncodingCodes()) {
            enc = (Encoding) fact.getEncoding(n.intValue());
            t.put("" + enc.getEncodingCode(), enc.getDisplayName());
        }
        o.setDisplayValuesTable(t);
        l.add(o);

        containerName = res.getString("options.rfb.pixelFormatSettings");
        o = new Preference("rfb.useCustomPixelFormat", Preference.TYPE_BOOLEAN,
                res.getString("options.rfb.useCustomPixelFormat"), null);
        o.setPreferredContainerName(containerName);
        l.add(o);

        o = new Preference("rfb.customPixelFormat", Preference.TYPE_STRING,
                res.getString("options.rfb.pixelFormat.label"), null);
        o.setPreferredContainerName(containerName);
        o.setSelectOnly(true);
        List vals = new ArrayList();
        ArrayList displayVals = new ArrayList();
        Object val, s;
        int index = 0;
        while ((val = UserConfiguration.getInstance().getString("rfb.pixelFormat." + index)) != null) {
            vals.add(val);
            s = res.getObject("options.rfb.pixelFormat." + index);
            if (s == null) {
                Object obj[] = {val};
                s = MessageFormat.format(res.getString("options.rfb.pixelFormat.custom"), obj);
            }
            displayVals.add(s);
            index++;
        }
        o.setDisplayValues(displayVals);
        o.setValues(vals);
        o.setDependentOption("rfb.useCustomPixelFormat");
        l.add(o);

        containerName = "TCP/IP Connection Settings";
        o = new Preference("rfb.keepAlive", Preference.TYPE_BOOLEAN,
                "Keep Alive",
                null);
        o.setPreferredContainerName(containerName);
        l.add(o);
        o = new Preference("rfb.soTimeout", Preference.TYPE_INT,
                "SO_TIMEOUT (in miliseconds, 0 sets off)",
                null);
        o.setMinValue(0);
        o.setMaxValue(Integer.MAX_VALUE);
        o.setPreferredContainerName(containerName);
        l.add(o);

        containerName = res.getString("options.rfb.keyTransferOptions");
        o = new Preference("rfb.convertKeysWithModifiersToLowerCase",
                Preference.TYPE_BOOLEAN,
                res.getString("options.rfb.convertWinToLowerCase.name"),
                res.getString("options.rfb.convertWinToLowerCase.desc"));
        o.setPreferredContainerName(containerName);
        l.add(o);

//        o = new Preference("rfb.allowSingleModifiers",
//                Preference.TYPE_BOOLEAN,
//                res.getString("options.rfb.convertWinToLowerCase.name"),
//                res.getString("options.rfb.convertWinToLowerCase.desc"));
//        o.setPreferredContainerName(containerName);
//        l.add(o);
        return l;
    }

    public boolean isLocalDisplay() {
        return false;  // TODO: check if we are connecting to the local display or not
    }

    public void receiveBell() {
        fireRemoteServerEvent(new RemoteDesktopServerEvent(this, RemoteDesktopServerEvent.SERVER_BELL_EVENT));
        com.tplan.robot.ApplicationSupport.logFine("S2C: Bell event received");
    }

    public void checkDependencies(PluginManager manager) throws DependencyMissingException {
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     *
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Get the list of logon parameters. The RFB protocol requires just server,
     * port and password.
     *
     * @return list of preferences containing a server/port and password
     * parameter specification.
     */
    public List<Preference> getLoginParamsSpecification() {
        List<Preference> l = new ArrayList();

        // The following code makes sure the Login dlg displays the list of recent servers
        Preference o = new Preference(Preference.TYPE_STRING, LOGIN_PARAM_URI, "com.tplan.robot.gui.LoginDlg.labelServer", "com.tplan.robot.gui.LoginDlg.rfbDesc");
        o.setPreferredContainerName("dummy");
        List<String> displayValues = Utils.getRecentServersByProtocol(getProtocol());
        if (displayValues.size() > 0) {
            o.setDisplayValues(displayValues);
            o.setValues(displayValues);
            o.setSelectOnly(false);
        }
        l.add(o);

        o = new Preference(Preference.TYPE_PASSWORD, LOGIN_PARAM_PASSWORD, "com.tplan.robot.gui.LoginDlg.labelPassword", null);
        o.setPreferredContainerName("dummy");
        l.add(o);

        o = new Preference(Preference.TYPE_BOOLEAN, LOGIN_PARAM_SHARED_DESKTOP, "com.tplan.robot.gui.LoginDlg.checkBoxShared", null);
        o.setPreferredContainerName("dummy");
        o.setDefaultValue(UserConfiguration.getInstance().getBoolean("rfb.sharedDesktop"));
        l.add(o);
        return l;
    }

    public void setLoginParams(Map<String, Object> params) {
        if (debug) {
            String s = "[ ";
            Object value;
            for (String key : params.keySet()) {
                value = params.get(key);
                if (key.equals(LOGIN_PARAM_PASSWORD)) {
                    value = "******";
                }
                s += "{" + key + ", " + value + "} ";
            }
            s += "]";
            System.out.println("Login params: " + s);
        }
        if (params.containsKey(VAR_LISTEN)) {
            this.listenPort = ((Number) params.get(VAR_LISTEN)).intValue();
        } else {
            this.listenPort = -1;
        }
        if (params.containsKey(LOGIN_PARAM_PASSWORD)) {
            this.password = (String) params.get(LOGIN_PARAM_PASSWORD);
        }
        if (params.containsKey(LOGIN_PARAM_SHARED_DESKTOP)) {
            Object o = params.get(LOGIN_PARAM_SHARED_DESKTOP);
            Boolean b;
            if (o instanceof Boolean) {
                UserConfiguration.getInstance().setBoolean("rfb.sharedDesktop", (Boolean) o);
            } else if (o != null) {
                UserConfiguration.getInstance().setBoolean("rfb.sharedDesktop", Boolean.parseBoolean(o.toString()));
            }
        }
        if (params.containsKey(LOGIN_PARAM_URI)) {
            Object o = params.get(LOGIN_PARAM_URI);
            uri = null;
            if (o instanceof URI) {
                uri = (URI) o;
            } else {
                try {
                    uri = Utils.getURI(o.toString());
                    this.host = uri.getHost();
                    this.port = uri.getPort() >= 0 ? uri.getPort() : getDefaultPort();
                } catch (URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public boolean isBellTransferSupported() {
        return true;
    }

    public boolean isPointerTransferSupported() {
        return true;
    }

    public boolean isKeyTransferSupported() {
        return true;
    }

    @Override
    public void destroy() {
//        System.out.println("RfbClientImpl.destroy()");
        if (connected) {
            try {
                close();
            } catch (Exception ex) {
            }
        }
        super.destroy();
        socket = null;
        inStream = null;
        outStream = null;
        lastMouseEvent = null;
        rfbThread = null;
        cfg = null;
        if (image != null && image.getGraphics() != null) {
            image.getGraphics().dispose();
            image = null;
        }
        pixelFormat = null;
        serverPixelFormat = null;
        if (encodingMap != null) {
            encodingMap.clear();
            encodingMap = null;
        }
        outBuf = null;
        specialKeyCodeMap = null;
    }

    public String getConnectString() {
        String s = null;
        if (host != null) {
            s = "rfb://" + host.toLowerCase() + ":" + (port >= 0 ? port : RFB_PORT_OFFSET);
        }
        return s;
    }

    public void setPseudoCursorEnabled(boolean enable) throws IOException {

//        if (!isConnected()) {
//            throw new IOException("The client is not connected to any VNC server.");
//        }
        // Check if the encoding is supported or not
        if (!encodingMap.containsKey(ENCODING_CURSOR_PSEUDO)) {
            throw new IllegalStateException("Cursor pseudo encoding is not supported by this product version. Make sure to run T-Plan Robot Enterprise v2.3 or higher.");
        }

        // Create a new encoding list with or without the cursor one
        int encs[] = manualEncodings && encodings != null
                ? encodings
                : UserConfiguration.getInstance().getArrayOfInts("rfb.imageEncodings");
        boolean contains = isPseudoCursorEnabled();

        // Create a new encoding list
        int newEncodings[] = encs;

        if (enable) {  // Enable the encoding
            if (!contains) {  // Add the pseudo cursor encoding code only if not already there
                newEncodings = new int[encs.length + 1];
                System.arraycopy(encs, 0, newEncodings, 0, encs.length);
                newEncodings[newEncodings.length - 1] = ENCODING_CURSOR_PSEUDO;
            }
        } else {  // Disable the encoding
            if (contains) { // Remove it only if it is there
                newEncodings = new int[encs.length - 1];
                int i = 0;
                for (int enc : encs) {
                    if (enc != ENCODING_CURSOR_PSEUDO) {
                        newEncodings[i++] = enc;
                    }
                }
            }
        }

        if (debug) {
            System.out.println("setPseudoCursorEnabled(" + enable + "):\n"
                    + "  Old encodings: " + Arrays.toString(encs) + "\n"
                    + "  New encodings: " + Arrays.toString(newEncodings));
        }

        encodings = newEncodings;
        manualEncodings = true;

        // If connected, resend the encodings and refresh the whole screen
        if (isConnected()) {
            setEncodings(newEncodings);

            // Refresh the remote desktop
            sendFramebufferUpdateRequest(new Rectangle(0, 0, getDesktopWidth(), getDesktopHeight()), false);
        }
    }

    public boolean isPseudoCursorEnabled() {
        int encs[] = manualEncodings && encodings != null
                ? encodings
                : UserConfiguration.getInstance().getArrayOfInts("rfb.imageEncodings");
        boolean contains = false;
        for (int n : encs) {
            if (n == ENCODING_CURSOR_PSEUDO) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    public void resetModifiersIfNeeded() {
        if (previousEventModifiers != 0 && isConnected()) {
            Arrays.fill(outBuf, (byte) 0);
            int offset = bufferModifiers(outBuf, 0, null);
            synchronized (outStream) {
                try {
                    outStream.write(outBuf, 0, offset);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
