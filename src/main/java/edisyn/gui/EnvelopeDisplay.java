/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import edisyn.Model;
import edisyn.Synth;
import edisyn.Updatable;

/**
 * A tool to display envelopes. You will provide one of two collections of
 * items:
 *
 * <ul><li>
 * An array of XKEYS, YKEYS, XCONSTANTS, and YCONSTANTS. The xkeys and ykeys
 * define the x and y coordinates of each point in the envelope, including the
 * endpoints. They are multiplied by the xconstants and yconstants so that the y
 * values stay within the range [0...1] and the x values *summed together* will
 * not exceed the range [0...1]. Individual XKEYS and YKEYS can be null: if an
 * xkey is null, its value is 0. If a ykey is null, its value is 1.
 *
 * <p>
 * This approach is useful for envelopes where the xkeys are related to the
 * specific length of time that a synth will take to reach a next stage. An
 * example of a synth in this category: the Oberheim Matrix 1000.
 *
 * <ul><li>
 * An array of XKEYS, YKEYS, XCONSTANTS, YCONSTANTS, and ANGLES. The ykeys
 * define the y coordinates of each point in the envelope, including the
 * endpoints. xkey #0 defines the x coordinate of point 0 in the envelope;
 * thereafter x coordinates are computed by taking the ANGLES (angles between 0
 * and PI/2) and figuring the x distance needed for a line of the given angle,
 * starting at the previous y height, to reach the next y height. Angles are
 * always positive even if the resulting line has a negative slope. You will
 * probably have to scale the angles by a certain amount so that the largest
 * possible envelopes sum to 1.0. I have found that if you have three slopes,
 * you may need to scale by 0.314, and if you have four slopes, you may need to
 * scale by 0.25.  *
 * The xconstants now simply define the *maximum* x distance that a sloped line
 * will cover: they should sum to 1. The y constants again should be such that
 * the y values, multiplied by the y constants, stay within the range [0...1].
 * Individual XKEYS and YKEYS can be null: if an xkey is null, its value is 0.
 * If a ykey is null, its value is 1.
 *
 * <p>
 * This approach is useful for envelopes where the xkeys are related directly to
 * the angle of attack of the sloped line. An example of a synth in this
 * category: the Waldorf Blofeld.
 * <li>
 *
 * @author Sean Luke
 */
public class EnvelopeDisplay extends JComponent implements Updatable {

    private static final long serialVersionUID = 1L;

    public static final double TIME = -1;
    public static final double CURVE_RATIO = 1.0 / 3.0;
    public static final int NUM_INTERVALS = 2;
    public static final int NO_HIGHLIGHT = -1;

    private final List<Double> verticalDividers = new ArrayList<>();
    private final double xConstants[];
    private final double yConstants[];
    private final double angles[];
    private final String xKeys[];
    private final String yKeys[];
    private String curves[];
    private final Color color;
    private final Color semiTransparent;
    private final Synth synth;
    private int width = 128;
    private final int minWidth = 84;
    private int height = 84;
    private double yOffset;
    private boolean signed;
    private boolean filled = true;
    private EnvelopeDisplay[] children;
    private final String[] startKey = new String[NUM_INTERVALS];
    private final String[] endKey = new String[NUM_INTERVALS];
    private String finalStageKey;
    private String sustainStageKey;
    private boolean horizontalBorder = true;
    private boolean stepping;
    private int highlightIndex = NO_HIGHLIGHT;
    private AWTEventListener releaseListener;
    private double axis;

    public EnvelopeDisplay(Synth synth, Color color, String[] xKeys, String[] yKeys, double xConstants[], double yConstants[]) {
        this(synth, color, xKeys, yKeys, xConstants, yConstants, null);
    }

    public EnvelopeDisplay(Synth synth, Color color, String[] xKeys, String[] yKeys, double xConstants[], double yConstants[], double[] angles) {
        this.synth = synth;
        this.color = color;
        this.semiTransparent = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * Style.ENVELOPE_DISPLAY_FILL_TRANSPARENCY()));
        this.angles = angles;

        int len = 0;

        if (xKeys != null) {
            len = xKeys.length;
        } else if (yKeys != null) {
            len = yKeys.length;
        } else if (xConstants != null) {
            len = xConstants.length;
        } else if (yConstants != null) {
            len = yConstants.length;
        }

        if (xKeys == null) {
            xKeys = new String[len];
        }
        if (yKeys == null) {
            yKeys = new String[len];
        }

        this.xKeys = xKeys;
        this.yKeys = yKeys;
        this.xConstants = xConstants;
        this.yConstants = yConstants;

        if (xKeys.length != yKeys.length
            || xKeys.length != xConstants.length
            || xKeys.length != yConstants.length) {
            throw new IllegalArgumentException("Not all arrays have the same length.");
        }

        if (xKeys.length < 2) {
            throw new IllegalArgumentException("Length must be >= 2");
        }

        for (String xKey : xKeys) {
            if (xKey != null) {
                synth.getModel().register(xKey, this);
            }
        }
        for (String yKey : yKeys) {
            if (yKey != null) {
                synth.getModel().register(yKey, this);
            }
        }

        setBackground(Style.BACKGROUND_COLOR());

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                highlightIndex = highlightIndex(mouseToX(e.getX()), mouseToY(e.getY()), true);
                updateFromMouse(mouseToX(e.getX()), mouseToY(e.getY()), true);
                updateHighlightIndex(highlightIndex);
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                highlightIndex = highlightIndex(mouseToX(e.getX()), mouseToY(e.getY()), false);
                updateHighlightIndex(highlightIndex);
                repaint();
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                highlightIndex = NO_HIGHLIGHT;
                updateHighlightIndex(highlightIndex);
                repaint();
            }

            @Override
            public void mouseMoved(final MouseEvent e) {
                highlightIndex = highlightIndex(mouseToX(e.getX()), mouseToY(e.getY()), false);
                updateHighlightIndex(highlightIndex);
                repaint();
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                mouseDown();
                highlightIndex = highlightIndex(mouseToX(e.getX()), mouseToY(e.getY()), false);
                updateFromMouse(mouseToX(e.getX()), mouseToY(e.getY()), false);
                updateHighlightIndex(highlightIndex);
                repaint();
                if (releaseListener != null) {
                    releaseListener = null;
                }

                // This gunk fixes a BAD MISFEATURE in Java: mouseReleased isn't sent to the
                // same component that received mouseClicked.  What the ... ? Asinine.
                // So we create a global event listener which checks for mouseReleased and
                // calls our own private function.  EVERYONE is going to do this.
                Toolkit.getDefaultToolkit().addAWTEventListener(releaseListener = new AWTEventListener() {
                    @Override
                    public void eventDispatched(final AWTEvent evt) {
                        if (evt instanceof MouseEvent && evt.getID() == MouseEvent.MOUSE_RELEASED) {
                            MouseEvent e = (MouseEvent) evt;
                            if (releaseListener != null) {
                                mouseUp();
                                highlightIndex = highlightIndex(mouseToX(e.getX()), mouseToY(e.getY()), true);
                                updateHighlightIndex(highlightIndex);
                                Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
                                repaint();
                            }
                        }
                    }
                }, AWTEvent.MOUSE_EVENT_MASK);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                if (releaseListener == null) {
                    mouseUp();
                    highlightIndex = highlightIndex(mouseToX(e.getX()), mouseToY(e.getY()), true);
                    updateHighlightIndex(highlightIndex);
                    repaint();
                }
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void link(final EnvelopeDisplay display) {
        if (children == null) {
            children = new EnvelopeDisplay[0];
        }
        EnvelopeDisplay[] temp = new EnvelopeDisplay[children.length + 1];
        System.arraycopy(children, 0, temp, 0, children.length);
        temp[temp.length - 1] = display;
        children = temp;
        display.children = null;         // unlink it
    }

    public boolean getSigned() {
        return signed;
    }

    public void setSigned(final boolean signed) {
        this.signed = signed;
    }

    public void addVerticalDivider(double location) {
        verticalDividers.add(location);
    }

    public void setFinalStageKey(final String key) {
        finalStageKey = key;
        synth.getModel().register(key, this);
    }

    public void setSustainStageKey(final String key) {
        sustainStageKey = key;
        synth.getModel().register(key, this);
    }

    public void setLoopKeys(final int interval, final String startKey, final String endKey) {
        this.startKey[interval] = startKey;
        this.endKey[interval] = endKey;
        synth.getModel().register(startKey, this);
        synth.getModel().register(endKey, this);
    }

    public int postProcessLoopOrStageKey(final String key, final int val) {
        return val;
    }

    public void setPreferredWidth(final int width) {
        this.width = width;
    }

    public double getYOffset() {
        return yOffset;
    }

    public void setYOffset(final double yOffset) {
        this.yOffset = yOffset;
    }

    public int getPreferredWidth() {
        return this.width;
    }

    @Override
    public void update(final String key, final Model model) {
        repaint();
    }

    public boolean isFilled() {
        return filled;
    }

    public void setFilled(final boolean filled) {
        this.filled = filled;
    }

    public void postProcess(double[] xVals, double[] yVals) {
    }

    public int getPreferredHeight() {
        return height;
    }

    public void setPreferredHeight(final int height) {
        this.height = height;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public Dimension getMinimiumSize() {
        return new Dimension(minWidth, height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(100000, 100000);
    }

    public Color getColor() {
        return color;
    }

    public void setHorizontalBorder(boolean horizontalBorder) {
        this.horizontalBorder = horizontalBorder;
    }

    public boolean getHorizontalBorder() {
        return horizontalBorder;
    }

    public void setStepping(final boolean stepping) {
        this.stepping = stepping;
    }

    public boolean isStepping() {
        return stepping;
    }

    /**
     * Empty hook, called when mouse is pressed
     */
    public void mouseDown() {
    }

    /**
     * Empty hook, called when mouse is released
     */
    public void mouseUp() {
    }

    public int highlightIndex(double x, double y, boolean continuation) {
        return NO_HIGHLIGHT;
    }

    public void updateHighlightIndex(int index) {
    }

    double mouseToX(double x) {
        return (x - (horizontalBorder ? Style.ENVELOPE_DISPLAY_BORDER_THICKNESS() : 0))
            / (getWidth() - (horizontalBorder ? Style.ENVELOPE_DISPLAY_BORDER_THICKNESS() * 2 : 0));
    }

    double mouseToY(double y) {
        // I'm pretty sure this is wrong -- it should just be y - Style.ENVELOPE_DISPLAY_TOP_BORDER_THICKNESS(),
        // but it looks more correct this way
        return 1.0 - (y - verticalBorderThickness() + Style.ENVELOPE_DISPLAY_TOP_BORDER_THICKNESS())
            / (getHeight() - verticalBorderThickness() * 2);
    }

    public void updateFromMouse(double x, double y, boolean continuation) {
    }

    public double preprocessXKey(int index, String key, double value) {
        return value;
    }

    public double preprocessYKey(int index, String key, double value) {
        return value;
    }

    public void setCurveKeys(String[] keys) {
        if (curves != null) {
            for (String curve : curves) {
                if (curve != null) {
                    synth.getModel().unregister(curve, this);
                }
            }
        }

        curves = keys;

        if (curves != null) {
            for (String curve : curves) {
                if (curve != null) {
                    synth.getModel().register(curve, this);
                }
            }
        }
    }

    public double getControlValue(double x1, double x2, double y1, double y2, String key, int value) {
        return 0;
    }

    public double getCurveControlPointX1(double x1, double x2, double y1, double y2, String key, int value) {
        double amt = getControlValue(x1, x2, y1, y2, key, value);
        if (amt < 0) {
            amt = 0 - amt;
            return (x2 * CURVE_RATIO + x1 * (1 - CURVE_RATIO)) * amt + x1 * (1 - amt);
        } else {
            return x1;
        }
    }

    /**
     * Returns the Y coordinate (between y1 and y2) of the curve control point.
     */
    public double getCurveControlPointY1(double x1, double x2, double y1, double y2, String key, int value) {
        double amt = getControlValue(x1, x2, y1, y2, key, value);
        if (amt < 0) {
            return y1;
        } else {
            return (y1 * (1 - amt) + y2 * amt);
        }
    }

    public double getCurveControlPointX2(double x1, double x2, double y1, double y2, String key, int value) {
        double amt = getControlValue(x1, x2, y1, y2, key, value);
        if (amt < 0) {
            return x2;
        } else {
            return (x1 * CURVE_RATIO + x2 * (1 - CURVE_RATIO)) * amt + x2 * (1 - amt);
        }
    }

    /**
     * Returns the Y coordinate (between y1 and y2) of the curve control point.
     */
    public double getCurveControlPointY2(double x1, double x2, double y1, double y2, String key, int value) {
        double amt = getControlValue(x1, x2, y1, y2, key, value);
        if (amt < 0) {
            amt = 0 - amt;
            return (y2 * (1 - amt) + y1 * amt);
        } else {
            return y2;
        }
    }

    /**
     * Mostly fills the background appropriately.
     */
    @Override
    public void paintComponent(Graphics g) {
        paintComponent(g, false);
    }

    public void paintComponent(Graphics g, boolean asLink) {
        Graphics2D graphics = (Graphics2D) g;

        // count loop intervals
        int numLoops = 0;
        for (String key : startKey) {
            if (key != null) {
                numLoops++;
            }
        }

        double[] xs = new double[xKeys.length];
        double[] ys = new double[xKeys.length];

        System.arraycopy(xConstants, 0, xs, 0, xKeys.length);
        System.arraycopy(yConstants, 0, ys, 0, yKeys.length);

        for (int i = 0; i < xs.length; i++) {
            if (yKeys[i] != null) {
                ys[i] *= preprocessYKey(i, yKeys[i], synth.getModel().get(yKeys[i], 1));
            }
            ys[i] += yOffset;

            if (xKeys[i] != null) {
                if (angles != null && i > 0 && angles[i] != TIME) // we're doing angles
                {
                    double yd = Math.abs(ys[i] - ys[i - 1]);
                    double xd = Math.abs(yd / Math.tan(Math.PI / 2.0 - angles[i] * preprocessXKey(i, xKeys[i], synth.getModel().get(xKeys[i], 0))));
                    xs[i] *= xd;
                } else {
                    xs[i] *= preprocessXKey(i, xKeys[i], synth.getModel().get(xKeys[i], 1));
                }
            }
        }

        postProcess(xs, ys);

        Rectangle rect = getBounds();
        rect.x = 0;
        rect.y = 0;

        // first fill
        graphics.setPaint(Style.BACKGROUND_COLOR());
        if (!asLink) {
            graphics.fill(rect);
        }

        // next draw the kids underneath
        if (!asLink && children != null) {
            for (EnvelopeDisplay child : children) {
                child.setBounds(getBounds());
                child.paintComponent(g, true); // draw it as a link
            }
        }

        // Now continue drawing
        graphics.setColor(color);

        if (horizontalBorder) {
            rect.width -= Style.ENVELOPE_DISPLAY_BORDER_THICKNESS() * 2;
        }
        rect.height -= verticalBorderThickness() * 2;
        //rect.height -= spaceForLoopInterval * numLoops;

        if (horizontalBorder) {
            rect.x += Style.ENVELOPE_DISPLAY_BORDER_THICKNESS();
        }
        rect.y += Style.ENVELOPE_DISPLAY_TOP_BORDER_THICKNESS();

        Line2D.Double line = new Line2D.Double(rect.x, rect.y, rect.x + rect.width, rect.y);
        if (!asLink) {
            graphics.draw(line);
        }
        line = new Line2D.Double(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y + rect.height);
        if (!asLink) {
            graphics.draw(line);
        }

        double xcurrent = 0;
        for (int i = 0; i < xs.length; i++) {
            xs[i] *= rect.width;
            double f = xs[i];
            xs[i] += xcurrent;
            xcurrent += f;
        }

        for (int i = 0; i < ys.length; i++) {
            if (signed) // instead of going 0...1 it goes -1...1, so we have to map it to 0...1
            {
                ys[i] /= 2.0;
                ys[i] += 0.5;
            }

            ys[i] *= rect.height;
        }

        double startHeight = rect.height;
        if (axis != 0) {
            startHeight = rect.height - (rect.height * axis);
        }

        Path2D.Double fillp = new Path2D.Double();
        Path2D.Double p = new Path2D.Double();
        Ellipse2D.Double marker[] = new Ellipse2D.Double[xs.length];

        if (stepping) {
            double centering = rect.width / xs.length / 2.0;

            fillp.moveTo(rect.x + xs[0], rect.y + startHeight);

            p.moveTo(rect.x + xs[0], rect.y + rect.height - ys[0]);
            fillp.lineTo(rect.x + xs[0], rect.y + rect.height - ys[0]);
            marker[0] = new Ellipse2D.Double((rect.x + xs[0] + centering - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0),
                (rect.y + rect.height - ys[0] - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0),
                Style.ENVELOPE_DISPLAY_MARKER_WIDTH(), Style.ENVELOPE_DISPLAY_MARKER_WIDTH());

            for (int i = 1; i < xs.length; i++) {
                p.lineTo(rect.x + xs[i], rect.y + rect.height - ys[i - 1]);
                fillp.lineTo(rect.x + xs[i], rect.y + rect.height - ys[i - 1]);
                p.moveTo(rect.x + xs[i], rect.y + rect.height - ys[i]);
                fillp.lineTo(rect.x + xs[i], rect.y + rect.height - ys[i]);
                marker[i] = new Ellipse2D.Double((rect.x + xs[i] + centering - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0),
                    (rect.y + rect.height - ys[i] - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0),
                    Style.ENVELOPE_DISPLAY_MARKER_WIDTH(), Style.ENVELOPE_DISPLAY_MARKER_WIDTH());
            }

            int end = xs.length - 1;

            p.lineTo(rect.x + rect.width, rect.y + rect.height - ys[end]);
            fillp.lineTo(rect.x + rect.width, rect.y + rect.height - ys[end]);
            p.moveTo(rect.x + rect.width, rect.y + startHeight);
            fillp.lineTo(rect.x + rect.width, rect.y + startHeight);

            fillp.lineTo(rect.x + xs[0], rect.y + startHeight);
            fillp.closePath();
        } else {
            fillp.moveTo(rect.x + xs[0], rect.y + startHeight);
            p.moveTo(rect.x + xs[0], rect.y + rect.height - ys[0]);
            fillp.lineTo(rect.x + xs[0], rect.y + rect.height - ys[0]);

            marker[0] = new Ellipse2D.Double((rect.x + xs[0] - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0),
                (rect.y + rect.height - ys[0] - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0),
                Style.ENVELOPE_DISPLAY_MARKER_WIDTH(), Style.ENVELOPE_DISPLAY_MARKER_WIDTH());

            for (int i = 1; i < xs.length; i++) {
                if (curves == null || curves[i] == null) {
                    p.lineTo(rect.x + xs[i], rect.y + rect.height - ys[i]);
                    fillp.lineTo(rect.x + xs[i], rect.y + rect.height - ys[i]);
                } else {
                    double xx1 = getCurveControlPointX1(rect.x + xs[i - 1], rect.x + xs[i], rect.y + rect.height - ys[i - 1], rect.y + rect.height - ys[i], curves[i], synth.getModel().get(curves[i]));
                    double yy1 = getCurveControlPointY1(rect.x + xs[i - 1], rect.x + xs[i], rect.y + rect.height - ys[i - 1], rect.y + rect.height - ys[i], curves[i], synth.getModel().get(curves[i]));
                    double xx2 = getCurveControlPointX2(rect.x + xs[i - 1], rect.x + xs[i], rect.y + rect.height - ys[i - 1], rect.y + rect.height - ys[i], curves[i], synth.getModel().get(curves[i]));
                    double yy2 = getCurveControlPointY2(rect.x + xs[i - 1], rect.x + xs[i], rect.y + rect.height - ys[i - 1], rect.y + rect.height - ys[i], curves[i], synth.getModel().get(curves[i]));
                    p.curveTo(xx1, yy1, xx2, yy2, rect.x + xs[i], rect.y + rect.height - ys[i]);
                    fillp.curveTo(xx1, yy1, xx2, yy2, rect.x + xs[i], rect.y + rect.height - ys[i]);
                }
                marker[i] = new Ellipse2D.Double((rect.x + xs[i] - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0),
                    (rect.y + rect.height - ys[i] - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0),
                    Style.ENVELOPE_DISPLAY_MARKER_WIDTH(), Style.ENVELOPE_DISPLAY_MARKER_WIDTH());
            }

            int end = xs.length - 1;

            fillp.lineTo(rect.x + xs[end], rect.y + startHeight);
            fillp.lineTo(rect.x + xs[0], rect.y + startHeight);
            fillp.closePath();
        }

        graphics.setColor(semiTransparent);
        if (!asLink && filled) {
            graphics.fill(fillp);
        }

        graphics.setColor(color);
        if (asLink) {
            graphics.setColor(Style.ENVELOPE_UNSET_COLOR());
        }
        graphics.draw(p);

        // draw dividers
        if (!verticalDividers.isEmpty()) {
            graphics.setStroke(Style.ENVELOPE_AXIS_STROKE());
            graphics.setColor(color);
            for (int i = 0; i < verticalDividers.size(); i++) {
                double pos = verticalDividers.get(i);
                double x = rect.x + rect.width * pos;
                line = new Line2D.Double(x, rect.y, x, rect.y + rect.height);
                if (!asLink) {
                    graphics.draw(line);
                }
            }
        }

        // draw markers
        Color unset = Style.ENVELOPE_UNSET_COLOR();
        for (int i = 0; i < marker.length; i++) {
            if (!constrainTo(i)) {
                graphics.setColor(unset);
            } else if (highlightIndex != NO_HIGHLIGHT && highlightIndex == highlightIndex(mouseToX(marker[i].x + marker[i].width / 2.0),
                mouseToY(marker[i].y + marker[i].height / 2.0), false)) {
                graphics.setColor(Style.TEXT_COLOR());
            } else {
                graphics.setColor(getMarkerColor(i, color));
            }
            if (!asLink) {
                graphics.fill(marker[i]);
            }
        }

        // draw axis
        if (axis != 0) {
            graphics.setColor(color);
            line = new Line2D.Double(rect.x, rect.y + startHeight, rect.x + rect.width, rect.y + startHeight);
            graphics.setStroke(Style.ENVELOPE_AXIS_STROKE());
            if (!asLink) {
                graphics.draw(line);
            }
        }

        // draw stage ends
        if (sustainStageKey != null) {
            int sustainStage = postProcessLoopOrStageKey(sustainStageKey, synth.getModel().get(sustainStageKey, 0));
            double xpos = rect.x + (sustainStage == xs.length ? rect.width : xs[sustainStage]);
            line = new Line2D.Double(xpos, rect.y, xpos, rect.y + rect.height);
            graphics.setStroke(Style.ENVELOPE_AXIS_STROKE());
            if (!asLink) {
                graphics.draw(line);
            }
        }

        if (finalStageKey != null) {
            int finalStage = postProcessLoopOrStageKey(finalStageKey, synth.getModel().get(finalStageKey, 0));
            double xpos = rect.x + (finalStage == xs.length ? rect.width : xs[finalStage]);
            line = new Line2D.Double(xpos, rect.y, xpos, rect.y + rect.height);
            graphics.setStroke(new BasicStroke(1.0f));
            if (!asLink) {
                graphics.draw(line);
            }
        }

        graphics.setStroke(new BasicStroke(1.0f));
        // draw intervals
        for (int i = 0; i < numLoops; i++) {
            double loopStart = rect.x + xs[postProcessLoopOrStageKey(startKey[i], synth.getModel().get(startKey[i], 0))];
            double loopEnd = rect.x + xs[postProcessLoopOrStageKey(endKey[i], synth.getModel().get(endKey[i], 0))];
            double loopHeight = rect.y + rect.height + 6 * (i + 1);
            line = new Line2D.Double(loopStart, loopHeight, loopEnd, loopHeight);
            if (!asLink) {
                graphics.draw(line);
            }
            Ellipse2D.Double loopEndMarker = new Ellipse2D.Double(loopEnd - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0,
                loopHeight - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0,
                Style.ENVELOPE_DISPLAY_MARKER_WIDTH(), Style.ENVELOPE_DISPLAY_MARKER_WIDTH());
            graphics.setColor(Style.BACKGROUND_COLOR());
            if (!asLink) {
                graphics.fill(loopEndMarker);
            }
            graphics.setColor(color);
            if (!asLink) {
                graphics.draw(loopEndMarker);
            }
            Ellipse2D.Double loopStartMarker = new Ellipse2D.Double(loopStart - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0,
                loopHeight - Style.ENVELOPE_DISPLAY_MARKER_WIDTH() / 2.0,
                Style.ENVELOPE_DISPLAY_MARKER_WIDTH(), Style.ENVELOPE_DISPLAY_MARKER_WIDTH());
            if (!asLink) {
                graphics.fill(loopStartMarker);
            }
        }
    }

    public Color getMarkerColor(int marker, Color defaultColor) {
        return defaultColor;
    }

    public void setAxis(final double axis) {
        if (axis >= 0.0 && axis < 1.0) {
            this.axis = axis;
        }
    }

    public double getAxis() {
        return axis;
    }

    public boolean constrainTo(int index) {
        return true;
    }

    public int verticalBorderThickness() {
        return Style.ENVELOPE_DISPLAY_BORDER_THICKNESS();
    }
}
