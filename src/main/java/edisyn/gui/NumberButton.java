/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import edisyn.Model;
import edisyn.Synth;

public class NumberButton extends NumericalComponent {

    private static final long serialVersionUID = 1L;

    private final JLabel label;

    private final JButton change;

    public NumberButton(final String text, final Synth synth, final String key, final int minVal, final int maxVal, final String instructions) {
        super(synth, key);
        setLayout(new BorderLayout());

        label = new JLabel("  " + text);
        if (text != null) {
            label.setFont(Style.SMALL_FONT());
            label.setBackground(Style.BACKGROUND_COLOR());
            label.setForeground(Style.TEXT_COLOR());
            add(label, BorderLayout.NORTH);
        }

        String txt = "" + maxVal;
        String mintxt = "" + minVal;
        if (mintxt.length() > txt.length()) {
            txt = mintxt;
        }
        final int maxLength = txt.length();
        change = new JButton(txt);
        change.putClientProperty("JComponent.sizeVariant", "small");
        change.setFont(Style.SMALL_FONT());
        change.setPreferredSize(change.getPreferredSize());
        change.setHorizontalAlignment(SwingConstants.CENTER);

        change.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                while (true) {
                    int val = synth.getModel().get(key, 0);
                    VBox vbox = new VBox();
                    vbox.add(new JLabel(getCommand()));
                    JTextField textField = new JTextField(maxLength);
                    textField.setText("" + val);

                    // The following hack is inspired by https://tips4java.wordpress.com/2010/03/14/dialog-focus/
                    // and results in the text field being selected (which is what should have happened in the first place)
                    textField.addAncestorListener(new AncestorListener() {
                        @Override
                        public void ancestorAdded(AncestorEvent e) {
                            JComponent component = e.getComponent();
                            component.requestFocusInWindow();
                            textField.selectAll();
                        }

                        @Override
                        public void ancestorMoved(AncestorEvent e) {
                        }

                        @Override
                        public void ancestorRemoved(AncestorEvent e) {
                        }
                    });
                    vbox.add(textField);
                    synth.disableMenuBar();
                    int option = JOptionPane.showOptionDialog(NumberButton.this, vbox, getTitle(),
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Enter", "Cancel", "Rules"}, "Enter");
                    synth.enableMenuBar();

                    if (option == JOptionPane.CANCEL_OPTION) // this is "Rules"
                    {
                        synth.disableMenuBar();
                        JOptionPane.showMessageDialog(NumberButton.this, instructions, "Rules", JOptionPane.INFORMATION_MESSAGE);
                        synth.enableMenuBar();
                    } else if (option == JOptionPane.NO_OPTION) // this is "Cancel"
                    {
                        return;
                    } else {
                        submitValue(read(textField.getText()));
                    }
                }
            }
        });

        add(change, BorderLayout.SOUTH);

        synth.getModel().setMax(key, maxVal);
        synth.getModel().setMin(key, minVal);
        synth.getModel().set(key, minVal);
    }

    @Override
    public void update(final String key, final Model model) {
        change.setText("" + model.get(key, 0));
    }

    public int read(final String val) {
        int num;
        try {
            num = Integer.parseInt(val);
        } catch (NumberFormatException ex) {
            num = synth.getModel().get(key, 0);
        }
        return num;
    }

    public JButton getButton() {
        return change;
    }

    public String getTitle() {
        return label.getText().trim();
    }

    public String getCommand() {
        return "Enter " + getTitle();
    }

    /**
     * Submits a new number in response to a user request. Override this as you
     * see fit.
     */
    public void submitValue(final int val) {
        setState(val);
    }

    @Override
    public void paintComponent(final Graphics g) {
        Graphics2D graphics = (Graphics2D) g;

        Rectangle rect = getBounds();
        rect.x = 0;
        rect.y = 0;
        graphics.setPaint(Style.BACKGROUND_COLOR());
        graphics.fill(rect);
    }
}
