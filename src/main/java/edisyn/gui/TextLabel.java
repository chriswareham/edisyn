/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class TextLabel extends JLabel {

    private static final long serialVersionUID = 1L;

    public TextLabel(String text) {
        super(text, SwingConstants.LEFT);
        setFont(Style.SMALL_FONT());
        setBackground(Style.BACKGROUND_COLOR());
        setForeground(Style.TEXT_COLOR());
    }
}
