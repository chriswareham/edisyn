/*
 * Copyright 2006 by Sean Luke and George Mason University
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package edisyn.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import edisyn.Model;
import edisyn.Synth;

/**
 * A simple class that lets you specify a label and validate a numerical value.
 * NumberTextField assumes access to several image files for the widgets to the
 * right of the text field: a left-arrow button, a right-arrow button, and a
 * "belly button". The left-arrow button decreases the numerical value, the
 * right-arrow button increases it, and the belly button resets it to its
 * initial default value. You can also change the value in the text field
 * proper. Why use this class instead of a slider? Because it is not ranged: the
 * numbers can be any value.
 *
 * <p>NumberTextField lets users increase values according to a provided formula
 * of the form value = value * M + A, and similarly decrease values as value =
 * (value - A) / M. You specify the values of M and A and the initial default
 * value. This gives you some control on how values should change: linearly or
 * geometrically.</p>
 *
 * <p>You can exercise further control by subclassing the class and overriding
 * the newValue(val) method, which filters all newly user-set values and
 * "corrects" them. Programmatically set values (by calling setValue(...)) are
 * not filtered through newValue by default. If you need to filter, you should
 * do setValue(newValue(val));</p>
 *
 * <p>NumberTextFields can also be provided with an optional label.</p>
 */
public class NumberTextField extends NumericalComponent {

    private static final long serialVersionUID = 1L;

    private final JTextField valField;

    private final JLabel label = new JLabel("888", SwingConstants.LEFT) {
        @Override
        public Insets getInsets() {
            return new Insets(0, 0, 0, 0);
        }
    };

    private final KeyListener listener = new KeyListener() {
        @Override
        public void keyReleased(KeyEvent keyEvent) {
        }

        @Override
        public void keyTyped(KeyEvent keyEvent) {
        }

        @Override
        public void keyPressed(KeyEvent keyEvent) {
            switch (keyEvent.getKeyCode()) {
                case KeyEvent.VK_ENTER:
                    submit();
                    break;
                case KeyEvent.VK_ESCAPE:
                    update(null, synth.getModel());
                    break;
                default:
                    setEdited(true);
                    break;
            }
        }
    };

    private final FocusAdapter focusAdapter = new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
            submit();
        }
    };

    private final Color defaultColor;

    private final Color editedColor;

    private boolean edited;

    /**
     * Creates a NumberTextField which does not display the belly button or
     * arrows.
     */
    public NumberTextField(final String text, final Synth synth, final int columns, final Color editedColor, final String key) {
        super(synth, key);

        setBackground(Style.BACKGROUND_COLOR());
        setLayout(new BorderLayout());

        label.setFont(Style.SMALL_FONT());
        label.setBackground(Style.BACKGROUND_COLOR());
        label.setForeground(Style.TEXT_COLOR());
        label.setText(text);
        add(label, BorderLayout.NORTH);

        valField = new JTextField("", columns);
        valField.putClientProperty("JComponent.sizeVariant", "small");
        valField.addKeyListener(listener);
        valField.addFocusListener(focusAdapter);
        setValue(getValue());
        add(valField, BorderLayout.CENTER);

        this.editedColor = editedColor;
        this.defaultColor = valField.getBackground();
    }

    public void setEdited(final boolean edited) {
        if (this.edited != edited) {
            this.edited = edited;
            if (edited) {
                valField.setBackground(editedColor);
            } else {
                valField.setBackground(defaultColor);
            }
        }
    }

    public void submit() {
        if (edited) {
            int val;
            try {
                val = Integer.parseInt(valField.getText());
            } catch (NumberFormatException e) {
                val = getValue();
            }
            setValue(newValue(val));
        }
    }

    @Override
    public void update(final String key, final Model model) {
        valField.setText("" + getValue());
        setEdited(false);
    }

    /**
     * Sets the value without filtering first.
     */
    public void setValue(final int val) {
        valField.setText("" + val);
        setEdited(false);
        synth.getModel().set(key, val);
    }

    /**
     * Returns the most recently set value.
     */
    public int getValue() {
        return synth.getModel().get(key, 0);
    }

    public JTextField getField() {
        return valField;
    }

    /**
     * Override this to be informed when a new value has been set. The return
     * value should be the value you want the display to show instead.
     */
    public int newValue(int newValue) {
        if (newValue < getMin()) {
            newValue = getMin();
        }
        if (newValue > getMax()) {
            newValue = getMax();
        }
        return newValue;
    }

    /**
     * Only call this to access the value field directly
     */
    public void setText(final String val) {
        valField.setText(val);
    }

    public String getText() {
        return valField.getText();
    }
}
