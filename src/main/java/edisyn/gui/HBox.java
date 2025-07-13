/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A container which lays widgets out horizontally. This is a wrapper for Box
 * with built-in insets, and which automatically compresses (no glue), making it
 * dead easy to use: just create one and add stuff to it and you're done.
 *
 * <p>If you want the box to expand the rightmost component to fill all
 * remaining space, add the component using the addLast() method.</p>
 *
 * @author Sean Luke
 */
public class HBox extends JComponent implements Gatherable {

    public static final int LEFT_CONSUMES = 0;

    public static final int RIGHT_CONSUMES = 1;

    private static final long serialVersionUID = 1L;

    private final Box box;

    private final JPanel panel;

    private JComponent lastComponent;

    public HBox() {
        this(RIGHT_CONSUMES);
    }

    public HBox(final int consumes) {
        setLayout(new BorderLayout());
        setBackground(Style.BACKGROUND_COLOR());

        box = new Box(BoxLayout.X_AXIS);
        if (consumes == LEFT_CONSUMES) {
            add(box, BorderLayout.EAST);
        } else {
            add(box, BorderLayout.WEST);
        }

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(Style.BACKGROUND_COLOR());
        add(panel, BorderLayout.CENTER);
    }

    public int getCount() {
        return box.getComponentCount();
    }

    @Override
    public Component add(final Component component) {
        return box.add(component);
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
        return Style.HBOX_INSETS();
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
