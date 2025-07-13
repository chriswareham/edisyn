/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class Marginalizer extends JComponent implements Gatherable {

    private static final long serialVersionUID = 1L;

    private final JPanel panel;

    public Marginalizer() {
        setLayout(new BorderLayout());
        setBackground(Style.BACKGROUND_COLOR());

        panel = new JPanel();
        panel.setBackground(Style.BACKGROUND_COLOR());
        add(panel, BorderLayout.CENTER);
    }

    public void addBottom(final JComponent component) {
        add(component, BorderLayout.SOUTH);
    }

    public void addTop(final JComponent component) {
        add(component, BorderLayout.NORTH);
    }

    public void addLeft(final JComponent component) {
        add(component, BorderLayout.WEST);
    }

    public void addRight(final JComponent component) {
        add(component, BorderLayout.EAST);
    }

    @Override
    public void removeAll() {
        super.removeAll();
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void gatherAllComponents(final List<Component> components) {
        gatherAllComponents(this, components);
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
