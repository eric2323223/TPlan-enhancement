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

import com.tplan.robot.scripting.RelativePoint;
import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.scripting.ScriptEvent;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.TokenParser;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.capabilities.PointerTransferCapable;
import com.tplan.robot.remoteclient.rfb.RfbClient;
import com.tplan.robot.scripting.ScriptListener;
import com.tplan.robot.scripting.ScriptManager;
import com.tplan.robot.scripting.ScriptManagerImpl;

import com.tplan.robot.scripting.ScriptingContext;
import com.tplan.robot.scripting.commands.AdvancedCommandHandler;
import com.tplan.robot.scripting.commands.CommandEvent;
import com.tplan.robot.scripting.interpret.TestScriptInterpret;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Handler implementing functionality of the {@doc.cmd Mouse} command.
 * @product.signature
 */
public class MouseCommand extends AbstractCommandHandler implements AdvancedCommandHandler, ScriptListener {

    public static final String MOUSE_MOVE = "move";
    public static final String MOUSE_CLICK = "click";
    public static final String MOUSE_DRAG = "drag";
    public static final String MOUSE_PRESS = "press";
    public static final String MOUSE_RELEASE = "release";
    public static final String MOUSE_WHEEL_UP = "wheelup";
    public static final String MOUSE_WHEEL_DOWN = "wheeldown";
    public static final String PARAM_TO = "to";
    public static final String PARAM_FROM = "from";
    public static final String PARAM_BUTTON = "button";
    public static final String PARAM_BUTTON_SHORT = "btn";
    public static final String PARAM_MODIFIER = "modifiers";
    public static final String PARAM_MODIFIER_SHORT = "m";
    public static final String PARAM_PATTERN = "pattern";
    final String PARAM_BUTTON_VALUE_LEFT = "left";
    final String PARAM_BUTTON_VALUE_MIDDLE = "middle";
    final String PARAM_BUTTON_VALUE_RIGHT = "right";
    final String PARAM_EVENT = "event";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static Map contextAttributes;
    private Boolean modifierResetNeeded = null;
    // Bug 2934231 - Composed mouse drags not supported.
    // This variable is used to track the button pressed through "Mouse press".
    private Integer pressedButton = null;

    /**
     * Get a map with context attributes.
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes() {
        if (contextAttributes == null) {
            contextAttributes = new HashMap();
            ResourceBundle res = ApplicationSupport.getResourceBundle();
            contextAttributes.put(PARAM_WAIT, res.getString("command.param.wait"));
            contextAttributes.put(PARAM_COUNT, res.getString("command.param.count"));
            contextAttributes.put(PARAM_TO, res.getString("mouse.param.to"));
            contextAttributes.put(PARAM_FROM, res.getString("mouse.param.from"));
            contextAttributes.put(PARAM_BUTTON, PARAM_BUTTON_VALUE_LEFT + "|" + PARAM_BUTTON_VALUE_MIDDLE + "|" + PARAM_BUTTON_VALUE_RIGHT);
            contextAttributes.put(PARAM_MODIFIER, "[" + PressCommand.CTRL + "[+" + PressCommand.ALT + "[+" + PressCommand.SHIFT + "]]]");
        }
        return contextAttributes;
    }

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    /**
     * Implementation of the getContextArgument() method. It is necessary as this command has a mandatory argument,
     * which is an event identifier.
     *
     * @return description of the mandatory argument.
     */
    public String getContextArgument() {
        return MOUSE_MOVE + "|" + MOUSE_CLICK + "|" + MOUSE_DRAG + "|" + MOUSE_PRESS + "|" + MOUSE_RELEASE + "|" + MOUSE_WHEEL_UP + "|" + MOUSE_WHEEL_DOWN;
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext repository) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        String parName;
        Object value;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        //First token must be a command
        if (args.size() > 0) {
            parName = (String) args.get(0);
            int lastPlus = parName.lastIndexOf('+');
            if (lastPlus >= 0) {
                args.add(PARAM_MODIFIER);
                values.put(PARAM_MODIFIER, parName.substring(0, lastPlus));
                parName = parName.substring(lastPlus + 1);
            }
            if (!MOUSE_MOVE.equals(parName) && !MOUSE_CLICK.equals(parName) && !MOUSE_DRAG.equals(parName) && !MOUSE_PRESS.equals(parName) && !MOUSE_RELEASE.equals(parName) && !MOUSE_WHEEL_UP.equals(parName) && !MOUSE_WHEEL_DOWN.equals(parName)) {
                String s = res.getString("mouse.syntaxErr.unknownEvent");
                throw new SyntaxErrorException(MessageFormat.format(s, parName));
            }

            vt.put(PARAM_EVENT, parName);
            TokenParser parser = repository.getParser();

            for (int i = 1; i < args.size(); i++) {
                parName = args.get(i).toString().toLowerCase();
                value = values.get(parName);
                value = value == null ? "" : value;

                if (parName.equals(PARAM_WAIT)) {
                    vt.put(PARAM_WAIT, parser.parseTime(value, PARAM_WAIT));
                } else if (parName.equals(PARAM_COUNT)) {
                    vt.put(PARAM_COUNT, parser.parseNumber(value, PARAM_COUNT));
                } else if (parName.equals(PARAM_PATTERN)) {
                    vt.put(PARAM_PATTERN, value);
                } else if (parName.equals(PARAM_BUTTON) || parName.equals(PARAM_BUTTON_SHORT)) {
                    if (value instanceof Number) {
                        Number n = (Number) value;
                        switch (n.intValue()) {
                            case MouseEvent.BUTTON1:
                            case MouseEvent.BUTTON2:
                            case MouseEvent.BUTTON3:
                                vt.put(PARAM_BUTTON, n);
                                break;
                            default:
                                String s = res.getString("command.syntaxErr.oneOf");
                                throw new SyntaxErrorException(MessageFormat.format(s, parName, MouseEvent.BUTTON1 + ", " + MouseEvent.BUTTON2 + ", " + MouseEvent.BUTTON3));
                        }
                    } else {
                        vt.put(PARAM_BUTTON, parser.parseMouseButton(value.toString().toLowerCase()));
                    }
                } else if (parName.equals(PARAM_MODIFIER) || parName.equals(PARAM_MODIFIER_SHORT)) {
                    Number n = value instanceof Number ? n = (Number) value : parser.parseModifiers(value.toString());
                    vt.put(PARAM_MODIFIER, n);
                } else if (parName.equalsIgnoreCase(PARAM_TO) || parName.equalsIgnoreCase(PARAM_FROM)) {
                    if (!repository.isCompilationContext() || (value.toString().indexOf("{") < 0 && value.toString().indexOf("}") < 0)) {
                        RelativePoint p = parser.parsePoint(value, parName);
                        String key = parName.equalsIgnoreCase(PARAM_TO) ? PARAM_TO : PARAM_FROM;
                        vt.put(key, p);
                    }
                } else {
                    String s = res.getString("command.syntaxErr.unknownParam");
                    throw new SyntaxErrorException(MessageFormat.format(s, parName));
                }
            }
        } else {
            throw new SyntaxErrorException(res.getString("mouse.syntaxErr.missingEventId"));
        }
        if (repository.getClient() != null && (!(repository.getClient() instanceof PointerTransferCapable)
                || !((PointerTransferCapable) repository.getClient()).isPointerTransferSupported())) {
            String s = res.getString("mouse.syntaxErr.pointerNotSupportedByClient");
            throw new SyntaxErrorException(MessageFormat.format(s, repository.getClient().getDisplayName()));
        }
    }

    public String[] getCommandNames() {
        return new String[]{"mouse"};
    }

    public int execute(List args, Map values, ScriptingContext context) throws SyntaxErrorException, IOException {

        // Validate
        Map params = new HashMap();
        validate(args, values, params, context);

        try {
            handleMouseEvent(context, params);
            return 0;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 1;
    }

    protected void handleMouseEvent(ScriptingContext context, Map params) throws InterruptedException, IOException {

        String event = (String) params.get(PARAM_EVENT);
        ScriptManager handler = context.getScriptManager();

        Component evtSource = context.getEventSource();
        RemoteDesktopClient client = context.getClient();
        TestScriptInterpret interpret = context.getInterpret();

        int x = client.getLastMouseEvent() == null ? 0 : client.getLastMouseEvent().getX();
        int y = client.getLastMouseEvent() == null ? 0 : client.getLastMouseEvent().getY();

        RelativePoint to = params.containsKey(PARAM_TO) ? (RelativePoint) params.get(PARAM_TO) : new RelativePoint(-1, -1);
        RelativePoint from = params.containsKey(PARAM_FROM) ? (RelativePoint) params.get(PARAM_FROM) : new RelativePoint(-1, -1);
        Dimension resolution = new Dimension(client.getDesktopWidth(), client.getDesktopHeight());

        to.x = to.x < 0 ? x : to.x;
        to.y = to.y < 0 ? y : to.y;
        to.updateCoordinates(resolution);

        from.x = from.x < 0 ? x : from.x;
        from.y = from.y < 0 ? y : from.y;
        from.updateCoordinates(resolution);

        int count = params.containsKey(PARAM_COUNT) ? ((Number) params.get(PARAM_COUNT)).intValue() : 1;
        int button = params.containsKey(PARAM_BUTTON) ? ((Number) params.get(PARAM_BUTTON)).intValue() : MouseEvent.BUTTON1;
        int modifiers = params.containsKey(PARAM_MODIFIER) ? ((Number) params.get(PARAM_MODIFIER)).intValue() : 0;

        // Update in 2.3.1/2.0.6 - Loading of parameters made more robust
        UserConfiguration cfg = context.getConfiguration();
        int delay = 120;
        if (params.containsKey(PARAM_WAIT)) {
            delay = ((Number) params.get(PARAM_WAIT)).intValue();
        } else {
            delay = getIntegerSafely(cfg, "MouseCommand.multiClickDelay", 120);
        }

        int pressReleaseDelay = getIntegerSafely(cfg, "MouseCommand.pressReleaseDelay", 100);
        boolean generateMouseMove = getBooleanSafely(cfg, "MouseCommand.generateMouseMove", true);

        // Smoothness params added in 2.0.5
        int smoothDelay = getIntegerSafely(cfg, "MouseCommand.smoothDelay", 30);
        boolean makeMovesSmooth = getBooleanSafely(cfg, "MouseCommand.enableSmoothMoves", true);
        int smoothDistance = getIntegerSafely(cfg, "MouseCommand.smoothDistance", 10);

        // Bug 2934231 - Composed mouse drags not supported.
        // Button ID for the mouse move events.
        int defaultButton = pressedButton == null ? MouseEvent.NOBUTTON : pressedButton;

        // If a button has been pressed by "Mouse press", generate drags instead of moves.
        int moveEventId = defaultButton == MouseEvent.NOBUTTON ? MouseEvent.MOUSE_MOVED : MouseEvent.MOUSE_DRAGGED;

        int dragDelay = getIntegerSafely(cfg, "MouseCommand.dragDelay", 140);
        int dragDistance = getIntegerSafely(cfg, "MouseCommand.dragDistance", 10);

        final boolean isMove = event.equals(MOUSE_MOVE);
        // --- Since 1.3.16 - smooth mouse moves
        if (generateMouseMove && makeMovesSmooth && !event.equals(MOUSE_DRAG)) {
            int mod = isMove ? modifiers : 0;
            List<Point> pts = createSmoothMoveCoords(from.x, from.y, to.x, to.y, smoothDistance);
            for (Point p : pts) {
                sendPointerEvent(context, new MouseEvent(evtSource, moveEventId,
                        System.currentTimeMillis(), mod, p.x, p.y, 1, false, defaultButton), isMove);
                Thread.sleep(smoothDelay);
            }
        }
        // ---

        MouseEvent e = null;
        if (isMove) {
            // If a button was previously pressed through "Mouse press",
            // generate a drag event instead of the move one
            e = new MouseEvent(evtSource, moveEventId,
                    System.currentTimeMillis(), modifiers, to.x, to.y, 1, false, defaultButton);
            sendPointerEvent(context, e, true);
        } else if (MOUSE_DRAG.equals(event)) {
            e = new MouseEvent(evtSource, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), modifiers, from.x, from.y, 1, false, button);
            sendPointerEvent(context, e, true);
            Thread.sleep(dragDelay);
            e = new MouseEvent(evtSource, MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), modifiers, from.x, from.y, 1, false, button);
            sendPointerEvent(context, e, true);
            Thread.sleep(dragDelay);

            // --- Since 1.3.16 - Smooth drag
            List<Point> pts = createSmoothMoveCoords(from.x, from.y, to.x, to.y, dragDistance);
            for (Point p : pts) {
                e = new MouseEvent(evtSource, MouseEvent.MOUSE_DRAGGED,
                        System.currentTimeMillis(), modifiers, p.x, p.y, 0, false, button);
                sendPointerEvent(context, e, true);
                // Bug 2919928 fix: the delay is now applied also to the individual drag events
                Thread.sleep(dragDelay);
            }
            // ---

            e = new MouseEvent(evtSource, MouseEvent.MOUSE_DRAGGED,
                    System.currentTimeMillis(), modifiers, to.x, to.y, 0, false, button);
            sendPointerEvent(context, e, true);
            e = new MouseEvent(evtSource, MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(), modifiers, to.x, to.y, 0, false, button);
            sendPointerEvent(context, e, true);
            Thread.sleep(dragDelay);
        } else {
            boolean isWheelUp = MOUSE_WHEEL_UP.equals(event);

            for (int i = 0; i < count && !interpret.isStop(); i++) {
                if (MOUSE_PRESS.equals(event)) {
                    e = new MouseEvent(evtSource, MouseEvent.MOUSE_PRESSED,
                            System.currentTimeMillis(), modifiers, to.x, to.y, 1, false, button);
                    sendPointerEvent(context, e, true);
                } else if (MOUSE_RELEASE.equals(event)) {
                    e = new MouseEvent(evtSource, MouseEvent.MOUSE_RELEASED,
                            System.currentTimeMillis(), modifiers, to.x, to.y, 1, false, button);
                    sendPointerEvent(context, e, true);
                } else if (MOUSE_CLICK.equals(event)) {
                    e = new MouseEvent(evtSource, MouseEvent.MOUSE_PRESSED,
                            System.currentTimeMillis(), modifiers, to.x, to.y, 1, false, button);
                    sendPointerEvent(context, e, true);
                    if (i == count - 1 && handler instanceof ScriptManagerImpl) {
                        ((ScriptManagerImpl) handler).setRfbServerEventRecording(true);
                    }
                    Thread.sleep(pressReleaseDelay);
                    sendPointerEvent(context, new MouseEvent(evtSource, MouseEvent.MOUSE_RELEASED,
                            System.currentTimeMillis(), modifiers, to.x, to.y, 1, false, button), true);
                } else if (MOUSE_WHEEL_DOWN.equals(event) || MOUSE_WHEEL_UP.equals(event)) {
                    e = new MouseWheelEvent(evtSource, MouseWheelEvent.MOUSE_WHEEL,
                            System.currentTimeMillis(), modifiers, to.x, to.y, 1, false,
                            MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, isWheelUp ? -1 : 1);
                    sendPointerEvent(context, e, true);
                }
            }
        }
        if (e != null) {
            context.put(ScriptingContext.CONTEXT_LAST_GENERATED_CLIENT_EVENT, e);
        }
        wait(context, delay);
    }

    private void sendPointerEvent(ScriptingContext ctx, MouseEvent e, boolean pressModifiers) throws IOException {
        // Bug 2934231 - Composed mouse drags not supported.
        // Cache the button on "press" and reset on "release"
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            pressedButton = e.getButton();
        } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            pressedButton = null;
        }
        ((PointerTransferCapable) ctx.getClient()).sendPointerEvent(e, pressModifiers);
        boolean modifiersDown = e.getModifiers() > 0 && pressModifiers;
        if (modifierResetNeeded == null) {
            if (modifiersDown) {  // First mouse event with modifiers
                ctx.getScriptManager().removeScriptListener(this);
                ctx.getScriptManager().addScriptListener(this);
                modifierResetNeeded = true;
            }
        } else {  // Flag not null
            modifierResetNeeded = modifiersDown;
        }
        fireCommandEvent(this, ctx, CommandEvent.POINTER_EVENT, e);
    }

    @Override
    public List<Preference> getPreferences() {
        List v = new ArrayList(5);
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        String containerName = res.getString("options.mouse.group.calibration");

        Preference o = new Preference("MouseCommand.pressReleaseDelay",
                Preference.TYPE_INT,
                res.getString("options.mouse.pressReleaseDelay.name"),
                MessageFormat.format(res.getString("options.mouse.pressReleaseDelay.desc"), ApplicationSupport.APPLICATION_NAME));
        o.setPreferredContainerName(containerName);
        o.setMinValue(0);
        v.add(o);

        o = new Preference("MouseCommand.multiClickDelay",
                Preference.TYPE_INT,
                res.getString("options.mouse.clickDelay.name"),
                null);
        o.setPreferredContainerName(containerName);
        o.setMinValue(0);
        v.add(o);

        o = new Preference("MouseCommand.generateMouseMove",
                Preference.TYPE_BOOLEAN,
                res.getString("options.mouse.generateMouseMove.name"),
                null);
        o.setPreferredContainerName(containerName);
        v.add(o);

        containerName = res.getString("options.mouse.group.calibrationDrags");
        o = new Preference("MouseCommand.dragDistance",
                Preference.TYPE_INT,
                res.getString("options.mouse.dragDistance.name"),
                res.getString("options.mouse.dragDistance.desc"));
        o.setPreferredContainerName(containerName);
        o.setMinValue(0);
        v.add(o);

        o = new Preference("MouseCommand.dragDelay",
                Preference.TYPE_INT,
                res.getString("options.mouse.dragDelay.name"),
                null);
        o.setPreferredContainerName(containerName);
        o.setMinValue(0);
        v.add(o);

        containerName = res.getString("options.mouse.group.calibrationMoves");
        o = new Preference("MouseCommand.enableSmoothMoves",
                Preference.TYPE_BOOLEAN,
                res.getString("options.mouse.enableSmoothMoves.name"),
                res.getString("options.mouse.enableSmoothMoves.desc"));
        o.setPreferredContainerName(containerName);
        v.add(o);

        o = new Preference("MouseCommand.smoothDistance",
                Preference.TYPE_INT,
                res.getString("options.mouse.smoothDistance.name"),
                null);
        o.setPreferredContainerName(containerName);
        o.setMinValue(0);
        o.setDependentOption("MouseCommand.enableSmoothMoves");
        v.add(o);

        o = new Preference("MouseCommand.smoothDelay",
                Preference.TYPE_INT,
                res.getString("options.mouse.smoothDelay.name"),
                null);
        o.setPreferredContainerName(containerName);
        o.setMinValue(0);
        o.setDependentOption("MouseCommand.smoothDistance");
        v.add(o);

        return v;
    }

    private List<Point> createSmoothMoveCoords(int startX, int startY, int endX, int endY, int delta) {
        List<Point> pts = new ArrayList();

        // Line length
        double length = Math.sqrt((startX - endX) * (startX - endX) + (startY - endY) * (startY - endY));

        // Number of points along the line (minus one to skip the end point)
        int count = (int) (length / delta) - 1;

        // Always use count at least 3 to generate at least two events along the trajection
        count = Math.max(count, 3);

        final float dx = (endX - startX) / count;
        final float dy = (endY - startY) / count;
        for (int i = 1; i < count; i++) {
            pts.add(new Point(Math.round(startX + i * dx), Math.round(startY + i * dy)));
        }
        //Eric: add end point
        pts.add(new Point(endX, endY));
        return pts;
    }

    public List getArguments(String command, ScriptingContext context) {
        return Arrays.asList(new String[]{MOUSE_CLICK, MOUSE_DRAG, MOUSE_MOVE, MOUSE_PRESS, MOUSE_RELEASE, MOUSE_WHEEL_DOWN, MOUSE_WHEEL_UP});
    }

    public List getParameters(String command, ScriptingContext context) {
        List params = new ArrayList();
        context.getParser().parseParameters(command, params);
        if (params.size() > 0) {
            String arg = (String) params.get(0);
            int lastPlus = arg.lastIndexOf('+');
            if (lastPlus >= 0) {
                arg = arg.substring(lastPlus + 1);
            }
            if (arg.equalsIgnoreCase(MOUSE_CLICK)) {
                return Arrays.asList(new String[]{PARAM_TO, PARAM_BUTTON, PARAM_COUNT, PARAM_MODIFIER, PARAM_WAIT});
            } else if (arg.equalsIgnoreCase(MOUSE_PRESS) || arg.equalsIgnoreCase(MOUSE_RELEASE)) {
                return Arrays.asList(new String[]{PARAM_TO, PARAM_BUTTON, PARAM_MODIFIER, PARAM_WAIT});
            } else if (arg.equalsIgnoreCase(MOUSE_WHEEL_DOWN) || arg.equalsIgnoreCase(MOUSE_WHEEL_UP)) {
                return Arrays.asList(new String[]{PARAM_TO, PARAM_COUNT, PARAM_MODIFIER, PARAM_WAIT});
            } else if (arg.equalsIgnoreCase(MOUSE_DRAG)) {
                return Arrays.asList(new String[]{PARAM_BUTTON, PARAM_TO, PARAM_FROM, PARAM_MODIFIER, PARAM_WAIT});
            } else if (arg.equalsIgnoreCase(MOUSE_MOVE)) {
                return Arrays.asList(new String[]{PARAM_TO, PARAM_FROM, PARAM_MODIFIER, PARAM_WAIT});
            }
        }
        return null;
    }

    public List getParameterValues(String paramName, String command, ScriptingContext context) {
        if (paramName != null) {
            if (paramName.equalsIgnoreCase(PARAM_BUTTON)) {
                return Arrays.asList(new String[]{PARAM_BUTTON_VALUE_LEFT, PARAM_BUTTON_VALUE_MIDDLE, PARAM_BUTTON_VALUE_RIGHT});
            } else if (paramName.equalsIgnoreCase(PARAM_MODIFIER)) {
                return Arrays.asList(new String[]{
                            "Alt", "Ctrl", "Shift",});
            } else if (paramName.equalsIgnoreCase(PARAM_TO) || paramName.equalsIgnoreCase(PARAM_FROM)) {
                List l = new ArrayList();
                l.add(new Point(-1, -1));
                return l;
            }
        }
        return null;
    }

    public void scriptEvent(ScriptEvent event) {
        if (event.getType() == ScriptEvent.SCRIPT_EXECUTION_FINISHED) {
            if (modifierResetNeeded != null && modifierResetNeeded) {
                ScriptingContext ctx = event.getContext();
                RemoteDesktopClient client = ctx.getClient();
                if (client != null && client instanceof RfbClient) {
                    ((RfbClient) client).resetModifiersIfNeeded();
                }

                event.getContext().getScriptManager().removeScriptListener(this);
                modifierResetNeeded = null;
            }
        }
    }
}
