/*
 * Copyright 2006 by Sean Luke and George Mason University
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package edisyn.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

public class ColorWell extends JPanel {

    private static final long serialVersionUID = 1L;

    private Color color;

    public ColorWell() {
        this(new Color(0, 0, 0, 0));
    }

    public ColorWell(final Color color) {
        this.color = color;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(final MouseEvent e) {
                Color col = JColorChooser.showDialog(null, "Choose Color", color);
                setColor(col);
            }
        });
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }

    @Override
    public Insets getInsets() {
        return new Insets(4, 4, 4, 4);
    }

    // maybe in the future we'll add an opacity mechanism
    @Override
    public void paintComponent(Graphics g) {
        g.setColor(color);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    public Color getColor() {
        return color;
    }

    public void setColor(final Color color) {
        if (color != null) {
            this.color = changeColor(color);
        }
        repaint();
    }

    public Color changeColor(final Color c) {
        return c;
    }
}
