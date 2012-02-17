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
package com.tplan.robot.gui.preferences.list;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.List;

/**
 * {@link javax.swing.JList} enhanced with drag and drop functionality.
 * @product.signature
 */
public class DragAndDropList extends JList
        implements DropTargetListener, DragSourceListener, DragGestureListener {


    /**
     * A separator used to separate dragged items in the transferred data
     */
    private final String DELIMETER = ";";

    /**
     * A drop target for this component
     */
    DropTarget dropTarget = null;

    /**
     * A drag source for this component
     */
    DragSource dragSource = null;

    /**
     * Flag indicating if it is the source or target list
     */
    boolean isSource = false;

    JPanel panel;

    /**
     * Constructor - initializes the DropTarget and DragSource.
     *
     * @param panel    reference to the parent panel
     * @param isSource flag indicating if it is the source or target list
     */
    public DragAndDropList(JPanel panel, boolean isSource) {
        this.panel = panel;
        this.isSource = isSource;
        dropTarget = new DropTarget(this, this);
        dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
        int size = (int) getFont().getSize2D() + 6;

        setFixedCellHeight(size);
    }

    /**
     * Method is invoked when you are dragging over the DropSite.
     *
     * @param event a DropTargetDragEvent
     */
    public void dragEnter(DropTargetDragEvent event) {
        event.acceptDrag(DnDConstants.ACTION_MOVE);
    }

    /**
     * Method is invoked when you are exit the DropSite without dropping.
     * Here does nothing.
     *
     * @param event a DropTargetEvent
     */
    public void dragExit(DropTargetEvent event) {
    }

    /**
     * Method is invoked when a drag operation is going on.
     * Here does nothing.
     *
     * @param event a DropTargetDragEvent
     */
    public void dragOver(DropTargetDragEvent event) {
    }

    /**
     * Method is called when a drop has occurred.
     *
     * @param event a DropTargetDropEvent
     */
    public void drop(DropTargetDropEvent event) {

        try {
            Transferable transferable = event.getTransferable();

            if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                event.acceptDrop(DnDConstants.ACTION_MOVE);
                Object obj = transferable.getTransferData(DataFlavor.stringFlavor);
                Object array[] = getArray(obj.toString());

                if (array != null && array.length > 0) {
                    migrateElement(array, !contains(array[0]), event);
                    event.getDropTargetContext().dropComplete(true);
                } else {
                    event.rejectDrop();
                }
            } else {
                event.rejectDrop();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            event.rejectDrop();
        }
    }

    /**
     * Method is invoked if the use modifies the current drop gesture.
     * Here does nothing.
     *
     * @param event a DropTargetDragEvent
     */
    public void dropActionChanged(DropTargetDragEvent event) {
    }

    /**
     * Method called when a drag gesture has been initiated
     *
     * @param event a DragGestureEvent
     */
    public void dragGestureRecognized(DragGestureEvent event) {
        Object selected[] = getSelectedValues();

        if (selected != null && selected.length > 0) {
            StringSelection text = new StringSelection(getList(selected));

            // as the name suggests, starts the dragging
            dragSource.startDrag(event, DragSource.DefaultMoveDrop, text, this);
        }
    }

    /**
     * Method called when the dragging has ended.
     * Here does nothing.
     *
     * @param event a DragSourceDropEvent
     */
    public void dragDropEnd(DragSourceDropEvent event) {
    }

    /**
     * Method gets called when the dragging has entered the DropSite
     * Here does nothing.
     *
     * @param event a DragSourceDragEvent
     */
    public void dragEnter(DragSourceDragEvent event) {
    }

    /**
     * Method gets called when the dragging has exited the DropSite.
     * Here does nothing.
     *
     * @param event a DragSourceEvent event
     */
    public void dragExit(DragSourceEvent event) {
    }

    /**
     * Method gets called when the dragging is currently ocurring over the
     * DropSite. Here does nothing.
     *
     * @param event a DragSourceDragEvent event
     */
    public void dragOver(DragSourceDragEvent event) {
    }

    /**
     * Method gets invoked when the user changes the dropAction.
     * Here does nothing.
     *
     * @param event a DragSourceDragEvent event
     */
    public void dropActionChanged(DragSourceDragEvent event) {
    }

    /**
     * Method moves the element to the other list or to another position in the
     * same list.
     *
     * @param value dragged values
     * @param move  true if it is a move within the same list, false when the
     *              items have been dropped onto another list
     * @param event a DropTargetDropEvent
     */
    public void migrateElement(Object value[], boolean move, DropTargetDropEvent event) {
        Point p = event.getLocation();
        float rowShift = (float) p.getY() / (float) getFixedCellHeight();
        int index = (int) rowShift;

    }

    /**
     * Return true when the model of this list contains the argument item
     *
     * @param item an object to search for
     * @return true when the model of this list contains the argument item, false
     *         if not
     */
    private boolean contains(Object item) {
        for (int i = 0; i < getModel().getSize(); i++) {
            if (getModel().getElementAt(i).equals(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method converts an array of objects to a string list of items separated
     * by a delimeter.
     *
     * @param obj an array of dragged items
     * @return a string list of items separated by a delimeter
     */
    private String getList(Object obj[]) {
        String str = "";
        int size = obj == null ? 0 : obj.length;

        for (int i = 0; i < size; i++) {
            Object o = obj[i];

            str += o.toString();
            str = (i != size - 1) ? str + DELIMETER : str;
        }
        return str;
    }

    /**
     * Method converts string list of items separated by a delimeter
     * to an array of objects.
     *
     * @param list a string list of items separated by a delimeter
     * @return an array of items in the list
     */
    private Object[] getArray(String list) {
        List v = new ArrayList();
        StringTokenizer tokenizer =
                new StringTokenizer(list, DELIMETER);

        while (tokenizer.hasMoreTokens()) {
            v.add(tokenizer.nextToken());
        }
        return v.toArray();
    }
}

