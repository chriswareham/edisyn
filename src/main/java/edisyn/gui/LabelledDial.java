/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.List;

import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import edisyn.Model;
import edisyn.Synth;

/**
 * A labelled dial which the user can modify with the mouse. The dial updates
 * the model and changes in response to it. For an unlabelled dial, see Dial.
 *
 * <p>
 * You can add a second label (or in fact, though it's not obvious, additional
 * labels!)</p>
 *
 * @author Sean Luke
 */
public class LabelledDial extends NumericalComponent {

    public static final DialRole ROLE = new DialRole("dial");

    private static final long serialVersionUID = 1L;

    private final List<String> labels = new ArrayList<>();

    private final Dial dial;

    private final JLabel label;

    private Box labelBox;

    private Component glue;

    private boolean updatesDynamically = true;

    private boolean updatingDynamically;

    /**
     * Makes a labelled dial for the given key parameter on the given synth, and
     * with the given color and minimum and maximum. Prior to display,
     * subtractForDisplay is SUBTRACTED from the parameter value. You can use
     * this to convert 0...127 in the model to -64...63 on-screen, for example.
     */
    public LabelledDial(final String text, final Synth synth, final String key, final Color staticColor, final int min, final int max, final int subtractForDisplay) {
        this(text, synth, key, staticColor, min, max);
        dial.subtractForDisplay = subtractForDisplay;
        update(key, synth.getModel());
        if (isVisible()) {
            repaint();
        }
    }

    /**
     * Makes a labelled dial for the given key parameter on the given synth, and
     * with the given color and minimum and maximum.
     */
    public LabelledDial(final String text, final Synth synth, final String key, final Color staticColor, final int min, final int max) {
        this(text, synth, key, staticColor);
        if (min > max) {
            System.err.println("Warning (LabelledDial): min (" + min + ") is > max (" + max + ") for " + key);
        }
        setMin(min);
        setMax(max);
        synth.getModel().setMetricMin(key, min);
        synth.getModel().setMetricMax(key, max);
        setState(getState());
    }

    /**
     * Makes a labelled dial for the given key parameter on the given synth, and
     * with the given color. No minimum or maximum is set.
     */
    private LabelledDial(final String text, final Synth synth, final String key, final Color staticColor) {
        super(synth, key);
        setBackground(Style.BACKGROUND_COLOR());
        dial = new Dial(staticColor);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(Style.BACKGROUND_COLOR());
        panel.add(dial, BorderLayout.CENTER);

        label = new JLabel(text);

        if (text != null) {
            labels.add(text);
            label.setFont(Style.SMALL_FONT());
            label.setBackground(Style.BACKGROUND_COLOR());  // TRANSPARENT);
            label.setForeground(Style.TEXT_COLOR());

            labelBox = new Box(BoxLayout.Y_AXIS);
            Box box = new Box(BoxLayout.X_AXIS);
            box.add(Box.createGlue());
            box.add(label);
            box.add(Box.createGlue());
            labelBox.add(box);
            labelBox.add(glue = Box.createGlue());
            panel.add(labelBox, BorderLayout.SOUTH);
        }
        dial.updateAccessibleName();

        setLayout(new BorderLayout());
        add(panel, BorderLayout.NORTH);
    }

    public void repaintDial() {
        dial.repaint();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        dial.setEnabled(enabled);
        label.setEnabled(enabled);
    }

    @Override
    public Insets getInsets() {
        return Style.LABELLED_DIAL_INSETS();
    }

    @Override
    public void update(final String key, final Model model) {
        if (dial.isVisible()) {
            dial.repaint();
        }
    }

    public LabelledDial setLabel(final String text) {
        if (labels.isEmpty()) {
            labels.add(text);
        } else {
            labels.set(0, text);
        }
        dial.updateAccessibleName();

        label.setText(text);
        label.revalidate();
        if (label.isVisible()) {
            label.repaint();
        }
        return this;
    }

    public JLabel getJLabel() {
        return label;
    }

    public Color getTextColor() {
        return dial.field.getForeground();
    }

    public void setTextColor(final Color color) {
        dial.field.setForeground(color);
        if (dial.isVisible()) {
            dial.repaint();
        }
    }

    public boolean getUpdatesDyamically() {
        return updatesDynamically;
    }

    public void setUpdatesDynamically(final boolean updatesDynamically) {
        this.updatesDynamically = updatesDynamically;
    }

    public boolean isUpdatingDynamically() {
        return updatingDynamically;
    }

    public String map(final int val) {
        return "" + (val - dial.subtractForDisplay);
    }

    public boolean isSymmetric() {
        return dial.getCanonicalSymmetric();
    }

    public double getStartAngle() {
        return dial.getCanonicalStartAngle();
    }

    /**
     * Adds a second (or third or fourth or more!) label to the dial, to allow
     * for multiline labels.
     */
    public JLabel addAdditionalLabel(final String text) {
        if (labels.isEmpty()) {
            labels.add("");
        }
        labels.add(text);
        dial.updateAccessibleName();

        JLabel label2 = new JLabel(text);

        label2.setFont(Style.SMALL_FONT());
        label2.setBackground(Style.BACKGROUND_COLOR()); // TRANSPARENT);
        label2.setForeground(Style.TEXT_COLOR());

        Box box = new Box(BoxLayout.X_AXIS);
        box.add(Box.createGlue());
        box.add(label2);
        box.add(Box.createGlue());

        labelBox.remove(glue);
        labelBox.add(box);
        labelBox.add(glue = Box.createGlue());

        revalidate();
        if (isVisible()) {
            repaint();
        }
        return label2;
    }

    public void setLabelFont(Font font) {
        dial.field.setFont(font);
        dial.revalidate();
        if (dial.isVisible()) {
            dial.repaint();
        }
    }

    public Font getLabelFont() {
        return dial.field.getFont();
    }

    public int reviseToAltValue(final int val) {
        return val;
    }

    public int getDefaultValue() {
        if (isSymmetric()) {
            return (int) Math.ceil((getMin() + getMax()) / 2.0);             // we do ceiling so we push to 64 on 0...127
        } else {
            return getMin();
        }
    }

    // a hook for ASMHydrasynth
    public int updateProposedState(int proposedState) {
        return proposedState;
    }

    private class Dial extends JPanel {

        // What's going on?  Is the user changing the dial?
        public static final int STATUS_STATIC = 0;
        public static final int STATUS_DIAL_DYNAMIC = 1;

        // The largest vertical range that a dial ought to go.
        public static final int MAX_EXTENT = 256;
        // The typical vertical range that the dial goes.  128 is reasonable
        public static final int MIN_EXTENT = 128;

        private static final long serialVersionUID = 1L;

        private int status = STATUS_STATIC;

        private Color staticColor;

        // The state when the mouse was pressed
        private int startState;
        // The mouse position when the mouse was pressed
        private int startX;
        private int startY;

        // Is the mouse pressed?  This is part of a mechanism for dealing with
        // a stupidity in Java: if you PRESS in a widget, it'll be told. But if
        // you then drag elsewhere and RELEASE, the widget is never told.
        private boolean mouseDown;

        // how much should be subtracted from the value in the model before
        // it is displayed onscreen?
        private int subtractForDisplay;

        // Field in the center of the dial
        private JLabel field = new JLabel("88888", SwingConstants.CENTER);

        private boolean enabled = true;

        private AccessibleContext accessibleContext;

        Dial(final Color staticColor) {
            this.staticColor = staticColor;

            setFocusable(true);
            setRequestFocusEnabled(false);

            addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    int state = getState();
                    int max = getMax();
                    int min = getMin();

                    int modifiers = e.getModifiersEx();
                    boolean shift = (modifiers & KeyEvent.SHIFT_DOWN_MASK) == KeyEvent.SHIFT_DOWN_MASK;
                    // boolean ctrl = (modifiers & KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK;
                    // Can't use alt, MacOS uses it already for moving around
                    // boolean alt = (modifiers & KeyEvent.ALT_DOWN_MASK) == KeyEvent.ALT_DOWN_MASK;

                    int multiplier = 1;
                    if (shift) {
                        multiplier *= 16;
                    }
                    // if (ctrl) multiplier *= 256;
                    // if (alt) multiplier *= 256;

                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        state += multiplier;
                        if (state > max) {
                            state = max;
                        }
                        setState(state);
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        state -= multiplier;
                        if (state < min) {
                            state = min;
                        }
                        setState(state);
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        state += multiplier * 256;
                        if (state > max) {
                            state = max;
                        }
                        setState(state);
                    } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        state -= multiplier * 256;
                        if (state < min) {
                            state = min;
                        }
                        setState(state);
                    } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        setState(getDefaultValue());
                    } else if (e.getKeyCode() == KeyEvent.VK_HOME) {
                        setState(getMin());
                    } else if (e.getKeyCode() == KeyEvent.VK_END) {
                        setState(getMax());
                    }
                }
            });

            field.setFont(Style.DIAL_FONT());
            field.setBackground(Style.BACKGROUND_COLOR()); // TRANSPARENT);
            field.setForeground(Style.TEXT_COLOR());

            addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (!enabled) {
                        return;
                    }

                    int val = getState() - e.getWheelRotation();
                    if (val > getMax()) {
                        val = getMax();
                    }
                    if (val < getMin()) {
                        val = getMin();
                    }

                    setState(val);
                }
            });

            addMouseListener(new MouseAdapter() {
                private MouseEvent lastRelease;

                @Override
                public void mousePressed(final MouseEvent e) {
                    if (!enabled) {
                        return;
                    }
                    mouseDown = true;
                    startX = e.getX();
                    startY = e.getY();
                    startState = getState();
                    status = STATUS_DIAL_DYNAMIC;
                    if (isVisible()) {
                        repaint();
                    }

                    if (releaseListener != null) {
                        Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
                    }

                    // This gunk fixes a BAD MISFEATURE in Java: mouseReleased isn't sent to the
                    // same component that received mouseClicked.  What the ... ? Asinine.
                    // So we create a global event listener which checks for mouseReleased and
                    // calls our own private function.  EVERYONE is going to do this.
                    Toolkit.getDefaultToolkit().addAWTEventListener(releaseListener = new AWTEventListener() {
                        @Override
                        public void eventDispatched(AWTEvent e) {
                            if (e.getID() == MouseEvent.MOUSE_RELEASED && e instanceof MouseEvent mouseEvent) {
                                mouseReleased(mouseEvent);
                            }
                        }
                    }, AWTEvent.MOUSE_EVENT_MASK);
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    if (!enabled) {
                        return;
                    }
                    if (e == lastRelease) {
                        // we just had this event because we're in the AWT Event Listener.  So we ignore it
                        return;
                    }

                    if (!updatesDynamically) {
                        int proposedState = getProposedState(e);

                        // at present we're just going to use y.  It's confusing to use either y or x.
                        if (startState != proposedState) {
                            setState(proposedState);
                        }
                    }

                    status = STATUS_STATIC;
                    if (isVisible()) {
                        repaint();
                    }
                    if (releaseListener != null) {
                        Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
                    }
                    lastRelease = e;
                }

                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (synth.isShowingMutation()) {
                        synth.mutationMap.setFree(key, !synth.mutationMap.isFree(key));
                        if (LabelledDial.this.isVisible()) {
                            LabelledDial.this.repaint();
                        }
                    } else if (e.getClickCount() == 2) {
                        setState(getDefaultValue());
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(final MouseEvent e) {
                    if (!enabled) {
                        return;
                    }
                    int proposedState = getProposedState(e);

                    // at present we're just going to use y.  It's confusing to use either y or x.
                    if (getState() != proposedState) {
                        if (!updatesDynamically) {
                            synth.setSendMIDI(false);
                        }
                        updatingDynamically = true;
                        setState(proposedState);
                        updatingDynamically = false;
                        if (!updatesDynamically) {
                            synth.setSendMIDI(true);
                        }
                    }
                }
            });

            setLayout(new BorderLayout());
            add(field, BorderLayout.CENTER);
            if (isVisible()) {
                repaint();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(Style.LABELLED_DIAL_WIDTH(), Style.LABELLED_DIAL_WIDTH());
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(Style.LABELLED_DIAL_WIDTH(), Style.LABELLED_DIAL_WIDTH());
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            field.setEnabled(enabled);
            if (isVisible()) {
                repaint();
            }
        }

        public int getProposedState(MouseEvent e) {
            int y = -(e.getY() - startY);
            int min = getMin();
            int max = getMax();
            int range = (max - min + 1);

            double extent = range;
            if (extent < MIN_EXTENT) {
                extent = MIN_EXTENT;
            }
            if (extent > MAX_EXTENT) {
                extent = MAX_EXTENT;
            }

            double multiplicand = extent / range;

            int proposedState = startState + (int) (y / multiplicand);

            if (((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) && (((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) || ((e.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK))) {
                proposedState = startState + (int) (y / multiplicand / 64);
            } else if ((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) {
                proposedState = startState + (int) (y / multiplicand / 16);
            } else if (((e.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK)) {
                proposedState = startState + (int) (y / multiplicand / 4);
            } else if (((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) || ((e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK)) {
                proposedState = reviseToAltValue(proposedState);
            }

            if (proposedState < min) {
                proposedState = min;
            } else if (proposedState > max) {
                proposedState = max;
            }
            return updateProposedState(proposedState);
        }

        AWTEventListener releaseListener = null;

        /**
         * Returns the actual square within which the Dial's circle is drawn.
         */
        public Rectangle getDrawSquare() {
            Insets insets = getInsets();
            Dimension size = getSize();
            int width = size.width - insets.left - insets.right;
            int height = size.height - insets.top - insets.bottom;

            // How big do we draw our circle?
            if (width > height) {
                // base it on height
                int h = height;
                int w = h;
                int y = insets.top;
                int x = insets.left + (width - w) / 2;
                return new Rectangle(x, y, w, h);
            } else {
                // base it on width
                int w = width;
                int h = w;
                int x = insets.left;
                int y = insets.top + (height - h) / 2;
                return new Rectangle(x, y, w, h);
            }
        }

        public boolean getCanonicalSymmetric() {
            return subtractForDisplay == 64
                || subtractForDisplay == 50
                || getMax() == (0 - getMin())
                || (getMax() == 127 && getMin() == -128)
                || (getMax() == 63 && getMin() == -64);
        }

        public double getCanonicalStartAngle() {
            if (isSymmetric()) {
                return 90 + (270 / 2);
            }
            return 270;
        }

        @Override
        public void paintComponent(Graphics g) {
            // revise label if needed
            String val = (Style.showRaw ? ("" + getState()) : map(getState()));
            if (!(val.equals(dial.field.getText()))) {
                dial.field.setText(val);
            }

            super.paintComponent(g);

            int min = getMin();
            int max = getMax();

            Graphics2D graphics = (Graphics2D) g;
            RenderingHints oldHints = graphics.getRenderingHints();
            Style.prepareGraphics(graphics);

            Rectangle rect = getBounds();
            rect.x = 0;
            rect.y = 0;
            graphics.setPaint(Style.BACKGROUND_COLOR());
            graphics.fill(rect);
            rect = getDrawSquare();
            graphics.setPaint(Style.DIAL_UNSET_COLOR());
            graphics.setStroke(Style.DIAL_THIN_STROKE());
            Arc2D.Double arc = new Arc2D.Double();

            double startAngle = getStartAngle();
            double interval = -270;

            arc.setArc(rect.getX() + Style.DIAL_STROKE_WIDTH() / 2, rect.getY() + Style.DIAL_STROKE_WIDTH() / 2, rect.getWidth() - Style.DIAL_STROKE_WIDTH(), rect.getHeight() - Style.DIAL_STROKE_WIDTH(), startAngle, interval, Arc2D.OPEN);

            graphics.draw(arc);

            if (!enabled) {
                graphics.setRenderingHints(oldHints);
                return;
            }

            graphics.setStroke(Style.DIAL_THICK_STROKE());
            arc = new Arc2D.Double();

            int state = getState();
            interval = -((state - min) / (double) (max - min) * 265) - 5;

            if (status == STATUS_DIAL_DYNAMIC) {
                graphics.setPaint(Style.DIAL_DYNAMIC_COLOR());
                if (state == min) {
                    interval = 0;
                    // This is NO LONGER TRUE:
                    // interval = -5;
                    // If we're basically at zero, we still want to show a little bit while the user is scrolling so
                    // he gets some feedback.
                    //arc.setArc(rect.getX() + Style.DIAL_STROKE_WIDTH() / 2, rect.getY() + Style.DIAL_STROKE_WIDTH()/2, rect.getWidth() - Style.DIAL_STROKE_WIDTH(), rect.getHeight() - Style.DIAL_STROKE_WIDTH(), 270,  -5, Arc2D.OPEN);
                } else {
                    //arc.setArc(rect.getX() + Style.DIAL_STROKE_WIDTH() / 2, rect.getY() + Style.DIAL_STROKE_WIDTH()/2, rect.getWidth() - Style.DIAL_STROKE_WIDTH(), rect.getHeight() - Style.DIAL_STROKE_WIDTH(), 270,  -((state - min) / (double)(max - min) * 265) - 5, Arc2D.OPEN);
                }
            } else {
                graphics.setPaint(staticColor);
                if (state == min) {
                    interval = 0;
                    // do nothing.  Here we'll literally draw a zero
                } else {
                    //arc.setArc(rect.getX() + Style.DIAL_STROKE_WIDTH() / 2, rect.getY() + Style.DIAL_STROKE_WIDTH()/2, rect.getWidth() - Style.DIAL_STROKE_WIDTH(), rect.getHeight() - Style.DIAL_STROKE_WIDTH(), 270,  -((state - min) / (double)(max - min) * 265) - 5, Arc2D.OPEN);
                }
            }

            arc.setArc(rect.getX() + Style.DIAL_STROKE_WIDTH() / 2, rect.getY() + Style.DIAL_STROKE_WIDTH() / 2, rect.getWidth() - Style.DIAL_STROKE_WIDTH(), rect.getHeight() - Style.DIAL_STROKE_WIDTH(), startAngle, interval, Arc2D.OPEN);
            graphics.draw(arc);

            graphics.setRenderingHints(oldHints);
        }

        // Generate and provide the context information when asked
        @Override
        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleDial();
            }
            return accessibleContext;
        }

        protected void stateSet(int oldVal, int newVal) {
            AccessibleContext context = getAccessibleContext();
            if (context != null) {
                context.firePropertyChange(AccessibleContext.ACCESSIBLE_VALUE_PROPERTY, Integer.valueOf(oldVal), Integer.valueOf(newVal));
                context.firePropertyChange(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, "yo", "mama");
            }
        }

        // LabelledDial is a custom widget and must provide its own accessibility features.
        // Fortunately Java has good accessibility capabilities.  Unfortunately they're a little
        // broken in that they rely on the assumption that you're using standard Swing widgets.

        // We need a function which updates the name of our widget.  It appears that accessible
        // tools for the blind will announce the widget as "NAME ROLE" (as in "Envelope Attack Slider").
        // Unfortunately we do not have an easy way to encorporate the enclosing Category into the name
        // at this point.  We'll add that later below.
        private void updateAccessibleName() {
            String str = "";
            for (String l : labels) {
                str += (l + " ");
            }
            dial.getAccessibleContext().setAccessibleName(str);
        }

        // This is the top-level accessible context for the Dial.  It gives accessibility information.
        private class AccessibleDial extends AccessibleJComponent implements AccessibleValue, AccessibleComponent {

            private static final long serialVersionUID = 1L;

            // Here we try to tack the Category onto the END of the accessible name so
            // the user can skip it if he knows what it is.
            @Override
            public String getAccessibleName() {
                String name = super.getAccessibleName();
                // Find enclosing Category
                Component component = LabelledDial.this;
                while (component != null) {
                    if (component instanceof Category category) {
                        return name + " " + category.getName() + ", " + map(getState());
                    }
                    component = component.getParent();
                }
                return name + ", " + map(getState());
            }

            // Provide myself as the AccessibleValue (I implement that interface) so I don't
            // need to have a separate object
            @Override
            public AccessibleValue getAccessibleValue() {
                return this;
            }

            // We must define a ROLE that our widget fulfills.  We can't be a JPanel because
            // that causes the system to freak.  Notionally you're supposed to be
            // be able to  can provide custom roles, but in reality, if you do so, Java accessibility
            // will simply break for your widget.  So here we're borrowing the role from the closest
            // widget to our own: a JSlider.
            @Override
            public AccessibleRole getAccessibleRole() {
                return ROLE;    //ccessibleRole.SLIDER; // AccessibleRole.SLIDER;
            }

            // Add whether the user is frobbing me to my current state
            @Override
            public AccessibleStateSet getAccessibleStateSet() {
                AccessibleStateSet states = super.getAccessibleStateSet();
                /*
                  if (dial.mouseDown)
                  {
                  states.add(AccessibleState.BUSY);
                  }
                 */
                return states;
            }

            // My current numerical value
            @Override
            public Number getCurrentAccessibleValue() {
                return getState();
            }

            // You can't set my value
            @Override
            public boolean setCurrentAccessibleValue(Number n) {
                return false;
            }

            // My minimum numerical value
            @Override
            public Number getMinimumAccessibleValue() {
                return getMin();
            }

            // My maximum numerical value
            @Override
            public Number getMaximumAccessibleValue() {
                return getMax();
            }
        }
    }

    public static class DialRole extends AccessibleRole {

        public DialRole(String key) {
            super(key);
        }
    }
}
