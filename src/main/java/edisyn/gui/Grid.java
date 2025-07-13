/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class Grid extends JComponent implements Gatherable {

    private static final long serialVersionUID = 1L;

    public Grid(final int x, final int y) {
        setLayout(new GridLayout(y, x));
        setBackground(Style.BACKGROUND_COLOR());
    }

    @Override
    public Insets getInsets() {
        return Style.HBOX_INSETS();
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
