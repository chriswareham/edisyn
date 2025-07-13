/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * A container which lays widgets out vertically. This is a wrapper for Box with
 * built-in insets, and which automatically compresses (no glue), making it dead
 * easy to use: just create one and add stuff to it and you're done. This is
 * particularly useful because glue is broken for vertical boxes!
 *
 * <b>VBox is javax.swing.Scrollable by default, so you can easily override
 * those methods to customize how it scrolls.</b>
 *
 * @author Sean Luke
 */
public class VBox extends JComponent implements Gatherable, Scrollable {

    public static final int TOP_CONSUMES = 0;

    public static final int BOTTOM_CONSUMES = 1;

    private static final long serialVersionUID = 1L;

    private final Box box;

    private final JPanel panel;

    private JComponent lastComponent;

    public VBox() {
        this(BOTTOM_CONSUMES);
    }

    public VBox(final int consumes) {
        setLayout(new BorderLayout());
        setBackground(Style.BACKGROUND_COLOR());

        box = new Box(BoxLayout.Y_AXIS);
        if (consumes == TOP_CONSUMES) {
            add(box, BorderLayout.SOUTH);
        } else {
            add(box, BorderLayout.NORTH);
        }

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    public int getCount() {
        return box.getComponentCount();
    }

    @Override
    public Component add(final Component component) {
        return box.add(component);
    }

    public void addBottom(final JComponent component) {
        addLast(component);
    }

    public void addLast(final JComponent component) {
        lastComponent = component;
        panel.add(lastComponent, BorderLayout.CENTER);
    }

    public void removeLast() {
        if (lastComponent != null) {
            panel.remove(lastComponent);
        }
        lastComponent = null;
    }

    @Override
    public void remove(final Component component) {
        box.remove(component);
        if (lastComponent == component) {
            panel.remove(lastComponent);
        }
    }

    @Override
    public void remove(final int component) {
        box.remove(component);
    }

    @Override
    public void removeAll() {
        box.removeAll();
        if (lastComponent != null) {
            panel.remove(lastComponent);
        }
        lastComponent = null;
    }

    @Override
    public Insets getInsets() {
        return Style.VBOX_INSETS();
    }

    @Override
    public void setBackground(final Color color) {
        panel.setBackground(color);
        super.setBackground(color);
    }

    @Override
    public void revalidate() {
        panel.revalidate();
        box.revalidate();
        super.revalidate();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return null;
    }

    // for now we're not doing a snap to the nearest category
    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return 1;
        } else {
            return 1;
        }
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        return 1;
    }

    @Override
    public void gatherAllComponents(final List<Component> components) {
        gatherAllComponents(box, components);

        if (lastComponent != null) {
            components.add(lastComponent);
            if (lastComponent instanceof Gatherable gatherable) {
                gatherable.gatherAllComponents(components);
            }
        }
    }

    private static void gatherAllComponents(final Container container, final List<Component> components) {
        for (Component component : container.getComponents()) {
            components.add(component);
            if (component instanceof Gatherable gatherable) {
                gatherable.gatherAllComponents(components);
            } else if (component instanceof JPanel panel) {
                gatherAllComponents(panel, components);
            } else if (component instanceof Box box) {
                gatherAllComponents(box, components);
            }
        }
    }
}
