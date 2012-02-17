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

import com.tplan.robot.ApplicationSupport;
import java.awt.event.KeyEvent;

/**
 * Set of constants defined by the RFB version 3.3 protocol.
 * @product.signature
 */
public interface RfbConstants {

    // These are constants that are defined by the RFB protocol.
    // They are used mainly in the RFB messages.

    /**
     * VNC protocol uses key mapping defined in /usr/X11R6/include/X11/keysymdef.h.
     * Java provides different codes for certain keys (mainly the control/action ones)
     * and the following array provides a mapping among them.
     *
     * The codes are listed here in the same order as they are defined in
     * java.awt.event.KeyEvent. When a key has value -1, it will be ignored
     * (i.e. "swallowed" when the key is pressed).
     *
     * http://cvsweb.xfree86.org/cvsweb/xc/include/keysymdef.h?rev=1.1.1.4
     */
    final int[][] SPECIAL_KEY_CODES = {

        { KeyEvent.VK_ENTER, 0xFF0D },   // #define XK_Return 0xFF0D	/* Return, enter */

        { KeyEvent.VK_BACK_SPACE, 0xFF08 }, // #define XK_BackSpace	0xFF08	/* back space, back char */
        { KeyEvent.VK_TAB, 0xFF09 }, // #define XK_Tab 0xFF09
        { KeyEvent.VK_CANCEL, 0xFF69 }, // #define XK_Cancel 0xFF69	/* Cancel, stop, abort, exit */
        { KeyEvent.VK_CLEAR, 0xFF0B }, // #define XK_Clear 0xFF0B

        // Next on the list in KeyEvent are VK_SHIFT (0x10), VK_CONTROL (0x11)
        // and VK_ALT (0x12). There are more codes depending on the location.
        { KeyEvent.VK_SHIFT, 0xFFE1, KeyEvent.KEY_LOCATION_STANDARD },  //#define XK_Shift_L		0xFFE1	/* Left shift */
        { KeyEvent.VK_SHIFT, 0xFFE1, KeyEvent.KEY_LOCATION_LEFT },  //#define XK_Shift_L		0xFFE1	/* Left shift */
        { KeyEvent.VK_SHIFT, 0xFFE2, KeyEvent.KEY_LOCATION_RIGHT }, //#define XK_Shift_R		0xFFE2	/* Right shift */
        { KeyEvent.VK_CONTROL, 0xFFE3, KeyEvent.KEY_LOCATION_STANDARD },  //#define XK_Control_L		0xFFE3	/* Left control */
        { KeyEvent.VK_CONTROL, 0xFFE3, KeyEvent.KEY_LOCATION_LEFT },  //#define XK_Control_L		0xFFE3	/* Left control */
        { KeyEvent.VK_CONTROL, 0xFFE4, KeyEvent.KEY_LOCATION_RIGHT },  //#define XK_Control_R		0xFFE4	/* Right control */
        { KeyEvent.VK_META, 0xFFE7, KeyEvent.KEY_LOCATION_STANDARD },  // #define XK_Meta_L 0xFFE7	/* Left meta */
        { KeyEvent.VK_META, 0xFFE7, KeyEvent.KEY_LOCATION_LEFT },  // #define XK_Meta_L 0xFFE7	/* Left meta */
        { KeyEvent.VK_META, 0xFFE8, KeyEvent.KEY_LOCATION_RIGHT },  //#define XK_Meta_R		0xFFE8	/* Right meta */
        { KeyEvent.VK_ALT, 0xFFE9, KeyEvent.KEY_LOCATION_STANDARD },  //#define XK_Alt_L		0xFFE9	/* Left alt */
        { KeyEvent.VK_ALT, 0xFFE9, KeyEvent.KEY_LOCATION_LEFT },  //#define XK_Alt_L		0xFFE9	/* Left alt */
        { KeyEvent.VK_ALT, 0xFFEA, KeyEvent.KEY_LOCATION_RIGHT },  //#define XK_Alt_R		0xFFEA	/* Right alt */

        { KeyEvent.VK_PAUSE, 0xFF13 }, // #define XK_Pause 0xFF13	/* Pause, hold */

        // Next on the list is Caps Lock which is also ignored
        { KeyEvent.VK_CAPS_LOCK, -1 },
        { KeyEvent.VK_ESCAPE, 0xFF1B }, // #define XK_Escape 0xFF1B

        // Next is Space which corresponds to the ASCII value and that's why I don't list it.
        // The list follows by the cursor control & motion keys.
        { KeyEvent.VK_PAGE_UP, 0xFF55 },  // #define XK_Page_Up 0xFF55
        { KeyEvent.VK_PAGE_DOWN, 0xFF56 },  // #define XK_Page_Down	0xFF56
        { KeyEvent.VK_END, 0xFF57 },  // #define XK_End 0xFF57	/* EOL */
        { KeyEvent.VK_HOME, 0xFF50 },  // #define XK_Home 0xFF50
        { KeyEvent.VK_LEFT, 0xFF51 },  // #define XK_Left 0xFF51	/* Move left, left arrow */
        { KeyEvent.VK_UP, 0xFF52 },  // #define XK_Up 0xFF52	/* Move up, up arrow */
        { KeyEvent.VK_RIGHT, 0xFF53 },  // #define XK_Right 0xFF53	/* Move right, right arrow */
        { KeyEvent.VK_DOWN, 0xFF54 }, // #define XK_Down 0xFF54	/* Move down, down arrow */

        // All keys listed in KeyEvent up to the Fx keys can be ignored
        // because their code value corresponds to the ASCII one.
        // Now we define the F1-F24 keys.
        { KeyEvent.VK_F1, 0xFFBE }, // #define XK_F1 0xFFBE
        { KeyEvent.VK_F2, 0xFFBF }, // #define XK_F2 0xFFBF
        { KeyEvent.VK_F3, 0xFFC0 }, // #define XK_F3 0xFFC0
        { KeyEvent.VK_F4, 0xFFC1 }, // #define XK_F4 0xFFC1
        { KeyEvent.VK_F5, 0xFFC2 }, // #define XK_F5 0xFFC2
        { KeyEvent.VK_F6, 0xFFC3 }, // #define XK_F6 0xFFC3
        { KeyEvent.VK_F7, 0xFFC4 }, // #define XK_F7 0xFFC4
        { KeyEvent.VK_F8, 0xFFC5 }, // #define XK_F8 0xFFC5
        { KeyEvent.VK_F9, 0xFFC6 }, // #define XK_F9 0xFFC6
        { KeyEvent.VK_F10, 0xFFC7 }, // #define XK_F10 0xFFC7
        { KeyEvent.VK_F11, 0xFFC8 }, // #define XK_F11 0xFFC8
        { KeyEvent.VK_F12, 0xFFC9 }, // #define XK_F12 0xFFC9
        { KeyEvent.VK_F13, 0xFFCA }, // #define XK_F13 0xFFCA
        { KeyEvent.VK_F14, 0xFFCB }, // #define XK_F14 0xFFCB
        { KeyEvent.VK_F15, 0xFFCC }, // #define XK_F15 0xFFCC
        { KeyEvent.VK_F16, 0xFFCD }, // #define XK_F16 0xFFCD
        { KeyEvent.VK_F17, 0xFFCE }, // #define XK_F17 0xFFCE
        { KeyEvent.VK_F18, 0xFFCF }, // #define XK_F18 0xFFCF
        { KeyEvent.VK_F19, 0xFFD0 }, // #define XK_F19 0xFFD0
        { KeyEvent.VK_F20, 0xFFD1 }, // #define XK_F20 0xFFD1
        { KeyEvent.VK_F21, 0xFFD2 }, // #define XK_F21 0xFFD2
        { KeyEvent.VK_F22, 0xFFD3 }, // #define XK_F22 0xFFD3
        { KeyEvent.VK_F23, 0xFFD4 }, // #define XK_F23 0xFFD4
        { KeyEvent.VK_F24, 0xFFD5 }, // #define XK_F24 0xFFD5

        // Further keys with special codes.
        // Meta is considered as modifier.
        { KeyEvent.VK_DELETE, 0xFFFF },  // #define XK_Delete 0xFFFF	/* Delete, rubout */
        { KeyEvent.VK_INSERT, 0xFF63 },  // #define XK_Insert 0xFF63	/* Insert, insert here */
        { KeyEvent.VK_HELP, 0xFF6A },  // #define XK_Help 0xFF6A	/* Help */
        { KeyEvent.VK_PRINTSCREEN, 0xFF61 },  // #define XK_Print 0xFF61
        { KeyEvent.VK_FIND, 0xFF68 },  // #define XK_Find 0xFF68	/* Find, search */

        // Windows specific keys - Context_Menu
        { KeyEvent.VK_CONTEXT_MENU, 0xFF67 }, // See http://www.realvnc.com/pipermail/vnc-list/2000-April/013624.html

        // Bug 2903858 - "Press Windows" doesn't work.
        // The key code was changed to 0xFFEB which corresponds to the left Super key
        // in the X11 keysym.h. This has proven to work fine with UltraVNC and RealVNC.
        { KeyEvent.VK_WINDOWS, 0xFFEB }, // #define XK_Meta_L 0xFFE7	/* Left meta */

        // Bug fix in 1.3.12 - Numpad keys
        { KeyEvent.VK_NUMPAD0, 0xFFB0 }, // #define XK_KP_0 0xffb0
        { KeyEvent.VK_NUMPAD1, 0xFFB1 }, // #define XK_KP_1 0xffb1
        { KeyEvent.VK_NUMPAD2, 0xFFB2 }, // #define XK_KP_2 0xffb2
        { KeyEvent.VK_NUMPAD3, 0xFFB3 }, // #define XK_KP_3 0xffb3
        { KeyEvent.VK_NUMPAD4, 0xFFB4 }, // #define XK_KP_4 0xffb4
        { KeyEvent.VK_NUMPAD5, 0xFFB5 }, // #define XK_KP_5 0xffb5
        { KeyEvent.VK_NUMPAD6, 0xFFB6 }, // #define XK_KP_6 0xffb6
        { KeyEvent.VK_NUMPAD7, 0xFFB7 }, // #define XK_KP_7 0xffb7
        { KeyEvent.VK_NUMPAD8, 0xFFB8 }, // #define XK_KP_8 0xffb8
        { KeyEvent.VK_NUMPAD9, 0xFFB9 }, // #define XK_KP_9 0xffb9

        // Enhanced in 2.0.3 to allow number keys with location=numpad
        { KeyEvent.VK_0, 0xFFB0, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_0 0xffb0
        { KeyEvent.VK_1, 0xFFB1, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_1 0xffb1
        { KeyEvent.VK_2, 0xFFB2, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_2 0xffb2
        { KeyEvent.VK_3, 0xFFB3, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_3 0xffb3
        { KeyEvent.VK_4, 0xFFB4, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_4 0xffb4
        { KeyEvent.VK_5, 0xFFB5, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_5 0xffb5
        { KeyEvent.VK_6, 0xFFB6, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_6 0xffb6
        { KeyEvent.VK_7, 0xFFB7, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_7 0xffb7
        { KeyEvent.VK_8, 0xFFB8, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_8 0xffb8
        { KeyEvent.VK_9, 0xFFB9, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_9 0xffb9

        // Numpad operators
        { 0x2A, 0xFFAA, KeyEvent.KEY_LOCATION_NUMPAD },   // Multiply '*' (ASCII 0x2A) mapped onto VK_MULTIPLY
        { 0x2B, 0xFFAB, KeyEvent.KEY_LOCATION_NUMPAD },   // Add '+' (ASCII 0x2B) mapped onto VK_ADD
        { KeyEvent.VK_COMMA, 0xFFAC, KeyEvent.KEY_LOCATION_NUMPAD },   // Comma mapped onto VK_SEPARATOR
        { KeyEvent.VK_MINUS, 0xFFAD, KeyEvent.KEY_LOCATION_NUMPAD },   // Minus mapped onto VK_SUBTRACT
        { KeyEvent.VK_PERIOD, 0xFFAE, KeyEvent.KEY_LOCATION_NUMPAD },   // Period mapped onto VK_DECIMAL
        { KeyEvent.VK_SLASH, 0xFFAF, KeyEvent.KEY_LOCATION_NUMPAD },   // Slash mapped onto VK_DIVIDE

        // Enhanced in 2.0Beta - support of numpad keys
        { KeyEvent.VK_SPACE, 0xFF80, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Space		0xFF80	/* space */
        { KeyEvent.VK_TAB, 0xFF89, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Tab		0xFF89
        { KeyEvent.VK_ENTER, 0xFF8D, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Enter		0xFF8D	/* enter */
        { KeyEvent.VK_F1, 0xFF91, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_F1		0xFF91	/* PF1, KP_A, ... */
        { KeyEvent.VK_F2, 0xFF92, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_F2		0xFF92
        { KeyEvent.VK_F3, 0xFF93, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_F3		0xFF93
        { KeyEvent.VK_F4, 0xFF94, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_F4		0xFF94
        { KeyEvent.VK_HOME, 0xFF95, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Home		0xFF95
        { KeyEvent.VK_LEFT, 0xFF96, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Left		0xFF96
        { KeyEvent.VK_UP, 0xFF97, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Up		0xFF97
        { KeyEvent.VK_RIGHT, 0xFF98, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Right		0xFF98
        { KeyEvent.VK_DOWN, 0xFF99, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Down		0xFF99
//        { KeyEvent.VK_, 0xFF9A, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Prior		0xFF9A
        { KeyEvent.VK_PAGE_UP, 0xFF9A, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Page_Up		0xFF9A
//        { KeyEvent.VK_, 0xFF9B, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Next		0xFF9B
        { KeyEvent.VK_PAGE_DOWN, 0xFF9B, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Page_Down		0xFF9B
        { KeyEvent.VK_END, 0xFF9C, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_End		0xFF9C
        { KeyEvent.VK_BEGIN, 0xFF9D, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Begin		0xFF9D
        { KeyEvent.VK_INSERT, 0xFF9E, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Insert		0xFF9E
        { KeyEvent.VK_DELETE, 0xFF9F, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Delete		0xFF9F
        { KeyEvent.VK_EQUALS, 0xFFBD, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Equal		0xFFBD	/* equals */
        { KeyEvent.VK_MULTIPLY, 0xFFAA, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Multiply		0xFFAA
        { KeyEvent.VK_ADD, 0xFFAB, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Add		0xFFAB
        { KeyEvent.VK_SEPARATOR, 0xFFAC, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Separator		0xFFAC	/* separator, often comma */
        { KeyEvent.VK_SUBTRACT, 0xFFAD, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Subtract		0xFFAD
        { KeyEvent.VK_DECIMAL, 0xFFAE, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Decimal		0xFFAE
        { KeyEvent.VK_DIVIDE, 0xFFAF, KeyEvent.KEY_LOCATION_NUMPAD },   //#define XK_KP_Divide		0xFFAF

        // Fix in 2.0.2 - these keys are present just on the numpad so make them default without any location
        { KeyEvent.VK_MULTIPLY, 0xFFAA },   //#define XK_KP_Multiply		0xFFAA
        { KeyEvent.VK_ADD, 0xFFAB },   //#define XK_KP_Add		0xFFAB
        { KeyEvent.VK_SEPARATOR, 0xFFAC },   //#define XK_KP_Separator		0xFFAC	/* separator, often comma */
        { KeyEvent.VK_SUBTRACT, 0xFFAD },   //#define XK_KP_Subtract		0xFFAD
        { KeyEvent.VK_DECIMAL, 0xFFAE },   //#define XK_KP_Decimal		0xFFAE
        { KeyEvent.VK_DIVIDE, 0xFFAF },   //#define XK_KP_Divide		0xFFAF

        // Enhancement in 2.0Beta - numpad digit keys may be also referenced through location,
        // such as for example 'Press 1 location=numpad'
        { KeyEvent.VK_0, 0xFFB0, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_0 0xffb0
        { KeyEvent.VK_1, 0xFFB1, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_1 0xffb1
        { KeyEvent.VK_2, 0xFFB2, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_2 0xffb2
        { KeyEvent.VK_3, 0xFFB3, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_3 0xffb3
        { KeyEvent.VK_4, 0xFFB4, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_4 0xffb4
        { KeyEvent.VK_5, 0xFFB5, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_5 0xffb5
        { KeyEvent.VK_6, 0xFFB6, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_6 0xffb6
        { KeyEvent.VK_7, 0xFFB7, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_7 0xffb7
        { KeyEvent.VK_8, 0xFFB8, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_8 0xffb8
        { KeyEvent.VK_9, 0xFFB9, KeyEvent.KEY_LOCATION_NUMPAD }, // #define XK_KP_9 0xffb9

        // New keys in 2.0 Beta: Currency signs
//#define XK_EcuSign					0x20a0
//#define XK_ColonSign					0x20a1
//#define XK_CruzeiroSign					0x20a2
//#define XK_FFrancSign					0x20a3
//#define XK_LiraSign					0x20a4
//#define XK_MillSign					0x20a5
//#define XK_NairaSign					0x20a6
//#define XK_PesetaSign					0x20a7
//#define XK_RupeeSign					0x20a8
//#define XK_WonSign					0x20a9
//#define XK_NewSheqelSign				0x20aa
//#define XK_DongSign					0x20ab

        { KeyEvent.VK_EURO_SIGN, 0x20AC},   //#define XK_EuroSign	0x20ac
    };

    /**
     * Constant for the Microsoft Windows "Windows" key.
     * It is used for both the left and right version of the key.
     */
    public static final int VK_WINDOWS = 0x020C;

    /**
     * Constant for the Microsoft Windows Context Menu key.
     */
    public static final int VK_CONTEXT_MENU = 0x020D;

    public static final String VAR_LISTEN = ApplicationSupport.APPLICATION_NAME+".listen";

    // ProtocolVersion messages - 3.3, 3.7 and 3.8
    final String PROTOCOL_VERSION_3_3 = "RFB 003.003\n";
    final String PROTOCOL_VERSION_3_7 = "RFB 003.007\n";
    final String PROTOCOL_VERSION_3_8 = "RFB 003.008\n";

    final int RFB_PORT_OFFSET = 5900;
    final int RFB_LISTEN_PORT_OFFSET = 5500;

    // Constants identifying security types
    final int SECURITY_INVALID = 0;
    final int SECURITY_NONE = 1;
    final int SECURITY_VNC_AUTH = 2;
    final int SECURITY_RA2 = 5;
    final int SECURITY_RA2NE = 6;
    final int SECURITY_TIGHT = 16;
    final int SECURITY_ULTRA = 17;
    final int SECURITY_TLS = 18;
    final int SECURITY_VENCRYPT = 19;
    final int SECURITY_SASL = 20;
    final int SECURITY_MD5 = 21;
    final int SECURITY_CDEAN = 22;

    final int[] SECURITY_TYPES = {
        SECURITY_NONE,
        SECURITY_VNC_AUTH,
        SECURITY_RA2,
        SECURITY_RA2NE,
        SECURITY_TIGHT,
        SECURITY_ULTRA,
        SECURITY_TLS,
        SECURITY_VENCRYPT,
        SECURITY_SASL,
        SECURITY_MD5,
        SECURITY_CDEAN,
    };

    final String[] SECURITY_TYPE_NAMES = {
        "None",                     //SECURITY_NONE,
        "VNC Authentication",       //SECURITY_VNC_AUTH,
        "RA2",                      //SECURITY_RA2,
        "RA2ne",                    //SECURITY_RA2NE,
        "Tight",                    //SECURITY_TIGHT,
        "Ultra",                    //SECURITY_ULTRA,
        "TLS",                      //SECURITY_TLS,
        "VeNCrypt",                 //SECURITY_VENCRYPT,
        "GTK-VNC SASL",             //SECURITY_SASL,
        "MD5 hash authentication",  //SECURITY_MD5,
        "Colin Dean xvp",           //SECURITY_CDEAN,
    };
    // Constants of server responses on security msgs
    final int SECURITY_RESPONSE_OK = 0;
    final int SECURITY_RESPONSE_FAILED = 1;
    final int SECURITY_RESPONSE_TOO_MANY_ATTEMPTS = 2;

    // Client init msg - shared or exclusive access to the desktop
    final int CINIT_EXCLUSIVE_DESKTOP = 0;
    final int CINIT_SHARE_DESKTOP = 1;

    // Client to server message type codes
    final int MSG_C2S_SET_PIXEL_FORMAT = 0;
    final int MSG_C2S_FIX_COLOR_MAP_ENTRIES = 1;
    final int MSG_C2S_SET_ENCODINGS = 2;
    final int MSG_C2S_FRAMEBUFFER_UPDATE_REQUEST = 3;
    final int MSG_C2S_KEY_EVENT = 4;
    final int MSG_C2S_POINTER_EVENT = 5;
    final int MSG_C2S_CLIENT_CUT_TEXT = 6;

    final int MSG_S2C_COMMUNICATION_ERROR = -1;

    // Server to client message type codes
    final int MSG_S2C_FRAMEBUFFER_UPDATE = 0;
    final int MSG_S2C_SET_COLOR_MAP_ENTRIES = 1;
    final int MSG_S2C_BELL = 2;
    final int MSG_S2C_SERVER_CUT_TEXT = 3;

    // These are pseudo message code types used for the RFB module
    final int MSG_S2C_SERVER_INIT = 100;
    final int MSG_S2C_CONNECTING = 101;
    final int MSG_S2C_CONNECTED = 102;
    final int MSG_S2C_DISCONNECTING = 103;
    final int MSG_S2C_DISCONNECTED = 104;


    final int ENCODING_RAW = 0;
    final int ENCODING_COPY_RECT = 1;
    final int ENCODING_RRE = 2;
    final int ENCODING_CORRE = 4;
    final int ENCODING_HEXTILE = 5;
    final int ENCODING_ZLIB = 6;
    final int ENCODING_CURSOR_PSEUDO = -239;

    final int ENCODING_MAX_VALUE = 6;

    final int ENCODING_HEXTILE_RAW = 1;
    final int ENCODING_HEXTILE_BG_SPECIFIED = 2;
    final int ENCODING_HEXTILE_FG_SPECIFIED = 4;
    final int ENCODING_HEXTILE_ANY_SUBRECTS = 8;
    final int ENCODING_HEXTILE_SUBRECTS_COLORED = 16;


    /**
     * Byte mask (0xFF).
     */
    final int BYTEMASK = 0xFF;

}
