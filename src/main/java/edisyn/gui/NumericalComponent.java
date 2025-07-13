/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import edisyn.Model;
import edisyn.Synth;
import edisyn.Updatable;

/**
 * Abstract superclass of widgets which maintain numerical values in the model.
 * Each such widget maintains a KEY which is the parameter name in the model.
 * Widgets can share the same KEY and thus must update to reflect changes by the
 * other widget.
 *
 * <p>You will notably have to implement the <code>update(...)</code> method to
 * revise the widget in response to changes in the model.</p>
 *
 * @author Sean Luke
 */
public abstract class NumericalComponent extends JPanel implements Updatable, HasKey {

    private static final long serialVersionUID = 1L;

    protected final Synth synth;

    protected String key;

    private Color borderColor;

    public NumericalComponent(final Synth synth, final String key) {
        this.key = key;
        this.synth = synth;
        register(key);
        setBackground(Style.BACKGROUND_COLOR());
        Border border = new LineBorder(Style.BACKGROUND_COLOR(), 1) {
            @Override
            public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
                super.lineColor = borderColor;
                super.paintBorder(c, g, x, y, width, height);
            }
        };
        setBorder(border);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (synth.isShowingMutation() && synth.getModel().getStatus(key) != Model.STATUS_IMMUTABLE) {
                    synth.mutationMap.setFree(key, !synth.mutationMap.isFree(key));
                    // wrap the repaint in an invokelater because the dial isn't responding right
                    SwingUtilities.invokeLater(() -> {
                        repaint();
                    });
                }
            }
        });
    }

    public Synth getSynth() {
        return synth;
    }

    /**
     * Sets the component's key. Does not update.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the component's key. Does not update.
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * Returns the min value for the key in the model.
     */
    public int getMin() {
        return synth.getModel().getMin(key);
    }

    /**
     * Returns the max value for the key in the model.
     */
    public int getMax() {
        return synth.getModel().getMax(key);
    }

    /**
     * Returns whether the max value exists for the key in the model.
     */
    public boolean maxExists() {
        return synth.getModel().maxExists(key);
    }

    /**
     * Returns whether the min value exists for the key in the model.
     */
    public boolean minExists() {
        return synth.getModel().minExists(key);
    }

    /**
     * Returns the current value for the key in the model.
     */
    public int getState() {
        int defaultState = 0;
        if (minExists()) {
            defaultState = getMin();
        }
        return synth.getModel().get(key, defaultState);
    }

    /**
     * Sets the min value for the key in the model.
     */
    public void setMin(final int min) {
        synth.getModel().setMin(key, min);
        setState(getState());
    }

    /**
     * Sets the max value for the key in the model.
     */
    public void setMax(final int max) {
        synth.getModel().setMax(key, max);
        setState(getState());
    }

    /**
     * Sets the current value for the key in the model.
     */
    public void setState(int state) {
        if (maxExists()) // we presume we're set up so we can do bounds checking
        {
            int max = getMax();
            if (state > max) {
                state = max;
            }
        }

        if (minExists()) // we presume we're set up so we can do bounds checking
        {
            int min = getMin();
            if (state < min) {
                state = min;
            }
        }

        int old = synth.getModel().get(key);
        synth.getModel().set(key, state);
        repaint();
        stateSet(old, state);
    }

    protected void stateSet(final int oldVal, final int newVal) {
    }

    /**
     * Registers the NumericalComponent as a listener for changes to the key in
     * the model.
     */
    // this is here so we can override it in LabelledDial
    // so it's not registered.  That way we can call update()
    // manually on LabelledDial, but update() won't get called
    // automatically on it.  See addLFO(...)
    public void register(final String key) {
        synth.getModel().register(key, this);
    }

    @Override
    public abstract void update(final String key, final Model model);

    public void updateBorder() {
        // If we're supposed to show our mutation, and I'm free to mutate, but I'm not showing it, show it
        if (synth.isShowingMutation() && synth.mutationMap.isFree(key) && synth.getModel().getStatus(key) != Model.STATUS_IMMUTABLE) {
            borderColor = Style.DYNAMIC_COLOR();
        } // In all other situations, I should not be showing mutation.  If I am, stop it.
        else {
            borderColor = Style.BACKGROUND_COLOR();
        }
    }

    /**
     * Mostly fills the background appropriately.
     */
    @Override
    public void paintComponent(Graphics g) {
        updateBorder();  // might require paintComponent to get called twice
        super.paintComponent(g);
    }
}
