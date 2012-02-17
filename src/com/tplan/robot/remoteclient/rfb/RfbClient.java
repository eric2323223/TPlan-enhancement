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

import com.tplan.robot.preferences.Configurable;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.capabilities.BellTransferCapable;
import com.tplan.robot.remoteclient.capabilities.DesktopUpdateCapable;
import com.tplan.robot.remoteclient.capabilities.KeyTransferCapable;
import com.tplan.robot.remoteclient.capabilities.PointerTransferCapable;
import com.tplan.robot.remoteclient.rfb.encoding.Encoding;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Map;

/**
 * This interface defines methods of an RFB client.
 * <p>
 * RFB communication is described by the RFB protocol. See e.g.
 * <a href=http://www.realvnc.com/docs/rfbproto.pdf>The RFB Protocol v. 3.8</a>
 * at the RealVNC site. A typical session starts with a few initial handshaking
 * messages which negotiate protocol version, authentication, connection
 * parameters, format of the image data and the initial remote desktop image.
 * After the communication is successfuly started, the client keeps sending
 * the mouse, keyboard and clipboard events to the server and the server sends
 * back updated parts of the remote desktop image and server clipboard
 * and bell events.
 * <p>
 * The client interface defines four types of methods:
 * <ul>
 * <li>Get- and set-methods of the client parameters like server host name and
 * port, password, protocol version used for the communication with the server,
 * remote desktop name, geometry (size) and image.<li>
 * <li>Status methods showing whether the client is connecting (i.e. an initial
 * handshake is in progress) or connected to an RFB server.</li>
 * <li>Methods implementing the mouse, keyboard and clipboard client to server
 * messages.</li>
 * <li>RFB listener methods which allow registered objects to receive server
 * messages.</li>
 * </ul>
 * <p>
 * The client is not expected to expose methods of the initial handshaking
 * messages. These should be implemented internally in the <code>connect()</code>
 * method. The client should be typically used as follows:
 * <ul>
 * <li>Set the host name, port and password (if required).</li>
 * <li>Invoke the <code>connect()</code> method. If the connection fails,
 * the method throws an exception.</li>
 * <li>If the connection succeeds, the application uses the exposed client
 * methods to send mouse, keyboard and clipboard events to the server. The RFB
 * listener interface allows to implement custom actions reacting on the updates
 * of the remote desktop, clipboard changes and bell events.</li>
 * </ul>
 *
 * @product.signature
 */
public interface RfbClient extends RfbConstants, Configurable, RemoteDesktopClient,
        BellTransferCapable, PointerTransferCapable, KeyTransferCapable, DesktopUpdateCapable {


    public static final String LOGIN_PARAM_SHARED_DESKTOP = "SHARED_DESKTOP";

    /**
     * Get a map of encodings supported by the client where key is
     * their numeric code as is stated in the SetEncodings message of the
     * RFB protocol and value is an encoding handler (Encoding instance).
     *
     * @return Integer array with numeric codes of image encodings supported
     * by the client.
     */
    Map<Integer, Encoding> getSupportedEncodingMap();


    /**
     * Implementation of the SetEncodings client-to-server RFB v3.3 message. The
     * method is supposed to send the encodings specfied by the array of integer
     * numbers where the array members are encoding type numbers defined by the
     * RFB protocol, such as 0 (Raw encoding), 1 (CopyRect) etc.
     * @param encodings array of encodings supported by this server in the order
     * of preference.
     * @throws IOException if an I/O error is received when the new list of encodings
     * is sent to the client.
     */
    void setEncodings(int encodings[]) throws IOException;

    /**
     * Get the array of preferred encoding codes. See the {@link #setEncodings(int[])} method for details.
     * @return array of preferred encoding codes.
     */
    int[] getEncodings();

    /**
     * Find out whether the client is set to connect in the shared mode or not.
     * Be aware that the method returns value of a member variable flag and it
     * doesn't have to reflect status of the current connection. If you have
     * connected to a desktop and then changed the flag through {@link #setSharedMode(boolean)},
     * the method will return the flag value rather than the connection property.
     * @return true if the client is configured to connect in shared mode, false otherwise.
     */
    boolean isSharedMode();

    /**
     * Set whether to connect in a shared or exclusive mode. As this flag applies
     * only in the connection init phase, any change requires to reconnect an
     * eventual existing connection.
     * @param sharedMode true will set the shared mode, false sets the exclusive one.
     */
    void setSharedMode(boolean sharedMode);

    /**
     * Set the pixel format used by this client.
     * @param bitsPerPixel number of bits per pixel. See the RFB 3.3 SetPixelFormat specification for details.
     * @param colorDepth color depth in bits. See the RFB 3.3 SetPixelFormat specification for details.
     * @param bigEndian true sets the big endian order, false the little endian one. See the RFB 3.3 SetPixelFormat specification for details.
     * @param trueColor true means that pixels contain real RGB values while false indicates that the pixel values are pointers to a color map. See the RFB 3.3 SetPixelFormat specification for details.
     * @param redMax maximum value of the red color. See the RFB 3.3 SetPixelFormat specification for details.
     * @param greenMax maximum value of the green color. See the RFB 3.3 SetPixelFormat specification for details.
     * @param blueMax maximum value of the blue color. See the RFB 3.3 SetPixelFormat specification for details.
     * @param redShift the position (bit) where the red color starts in the pixel data. See the RFB 3.3 SetPixelFormat specification for details.
     * @param greenShift the position (bit) where the green color starts in the pixel data. See the RFB 3.3 SetPixelFormat specification for details.
     * @param blueShift the position (bit) where the blue color starts in the pixel data. See the RFB 3.3 SetPixelFormat specification for details.
     */
    void setPixelFormat(byte bitsPerPixel,
            byte colorDepth, boolean bigEndian, boolean trueColor,
            int redMax, int greenMax, int blueMax,
            byte redShift, byte greenShift, byte blueShift);

    /**
     * Implementation of the FramebufferUpdateRequest client-to-server RFB v3.3 message.
     * The method is supposed to send a request for the update of the remote
     * desktop image (or it's part).
     *
     * @param rect rectangle of the remote desktop image the client is
     * interested in. This argument should never be null - use the full remote
     * desktop rectangle if you are requesting full image.
     *
     * @param incremental this flag is defined by the RFB protocol and
     * enables/disables incremental sending of the updated image.
     * @throws IOException if an I/O error is received when the message
     * is sent to the client.
     */
    void sendFramebufferUpdateRequest(Rectangle rect, boolean incremental) throws IOException;

    /**
     * <p>Convenience method allowing to set on/off the cursor pseudo encoding.
     * This functionality is supported from the Enterprise version 2.3 and it is
     * only applicable when the Pseudo Cursor Plugin is available. When the
     * encoding is on, the mouse pointer is not part of the desktop image and
     * it is rendered locally by the client. This makes image comparison more
     * reliable because there are no image differences caused by conflicting mouse
     * pointer.</p>
     *
     * <p>When the encoding is off and it is being switched on, the method
     * adds the cursor pseudo encoding code (-239) to the list of supported encodings,
     * calls the {@link #setEncodings(int[])} method to notify the server of
     * the change and also calls {@link #sendFramebufferUpdateRequest(java.awt.Rectangle, boolean)}
     * to get a new copy of the screen image without the cursor. When the encoding is being
     * switched off, the steps are the same but the encoding code is removed
     * from the list.</p>
     *
     * <p>Unless this method is called, the client follows the list of encodings
     * which is stored in the local configuration. As the client listens to
     * configuration changes, any change to the list is picked up immediately and
     * applied to the existing VNC connection. A call of this method makes the
     * client switch to a "manual encoding mode" which breaks these rules and makes
     * it bypass the configuration. The impact is that any outside change
     * of the configuration file will not be picked up by this client until this
     * method gets called again.</p>
     *
     * @param enable true enables cursor pseudo encoding, false disables.
     *
     * @throws java.io.IOException the method throws any exceptions produced
     * by the the client-server method calls (<code>setEncodings()</code>,
     * <code>sendFramebufferUpdateRequest()</code>).
     *
     * @throws IllegalStateException when the required cursor pseudo encoding
     * plugin is not available.
     */
    void setPseudoCursorEnabled(boolean enable) throws IOException, IllegalStateException;

    /**
     * Find out whether the pseudo cursor is enabled or not.
     * @return <code>true</code> if the pseudo cursor support is on, <code>false</code> otherwise.
     */
    boolean isPseudoCursorEnabled();

    /**
     * Reset all modifiers (Shift, Ctrl, Alt, Meta) which has been previously
     * pressed but haven't been released yet.
     */
    void resetModifiersIfNeeded();
}
