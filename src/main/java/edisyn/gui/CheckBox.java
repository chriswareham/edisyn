/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.accessibility.AccessibleContext;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;

import edisyn.Model;
import edisyn.Synth;

/**
 * A wrapper for JCheckBox so that it updates itself in response to the model.  *
 * @author Sean Luke
 */
public class CheckBox extends NumericalComponent {

    private static final long serialVersionUID = 1L;

    private final JCheckBox check;

    private boolean enabled = true;

    private boolean flipped;

    private int addToWidth;

    public CheckBox(final String label, final Synth synth, final String key) {
        this(label, synth, key, false);
    }

    public CheckBox(final String label, final Synth synth, final String key, final boolean flipped) {
        super(synth, key);

        this.flipped = flipped;

        check = new JCheckBox(label) {
            AccessibleContext accessibleContext = null;

            // Generate and provide the context information when asked
            @Override
            public AccessibleContext getAccessibleContext() {
                if (accessibleContext == null) {
                    accessibleContext = new JCheckBox.AccessibleJCheckBox() {
                        @Override
                        public String getAccessibleName() {
                            String name = super.getAccessibleName();
                            // Find enclosing Category
                            Component obj = check;
                            while (obj != null) {
                                if (obj instanceof Category category) {
                                    return name + " " + category.getName();
                                } else {
                                    obj = obj.getParent();
                                }
                            }
                            return name;
                        }
                    };
                }
                return accessibleContext;
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width += addToWidth;
                return d;
            }
        };

        check.getAccessibleContext().setAccessibleName(label);
        check.setFont(Style.SMALL_FONT());
        check.setOpaque(false);
        check.setForeground(Style.TEXT_COLOR());

        setMax(1);
        setMin(0);
        setState(getState());

        setLayout(new BorderLayout());
        add(check, BorderLayout.CENTER);

        check.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (synth.isShowingMutation()) {
                    synth.mutationMap.setFree(key, !synth.mutationMap.isFree(key));
                    // wrap the repaint in an invokelater because the dial isn't responding right
                    SwingUtilities.invokeLater(() -> {
                        repaint();
                    });
                }
            }
        });

        check.addActionListener(e -> {
            if (CheckBox.this.flipped) {
                setState(check.isSelected() ? getMin() : getMax());
            } else {
                setState(check.isSelected() ? getMax() : getMin());
            }
        });
    }

    @Override
    public void update(String key, Model model) {
        // we don't compare against min or max here because they
        // could be used by other widgets.  See for example Blofeld parameter 8
        if (flipped) {
            check.setSelected(getState() == 0);
        } else {
            check.setSelected(getState() != 0);
        }
    }

    public boolean isFlipped() {
        return flipped;
    }

    public void addToWidth(final int val) {
        addToWidth = val;
    }

    public JCheckBox getCheckBox() {
        return check;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        updateBorder();
    }

    @Override
    public void updateBorder() {
        super.updateBorder();
        if (synth.isShowingMutation()) {
            check.setEnabled(false);
        } else {
            check.setEnabled(true && enabled);
        }
    }
}
