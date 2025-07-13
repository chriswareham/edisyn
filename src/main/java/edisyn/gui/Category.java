/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import edisyn.Model;
import edisyn.Synth;
import edisyn.util.StringUtility;

/**
 * A pretty container for widgets to categorize them
 *
 * @author Sean Luke
 */
public class Category extends JPanel implements Gatherable {
    // we used to be a JComponent but now we're a JPanel because JPanels have concrete AccessibleContexts

    // even with modification for the insets, the text position value seems to be off by a bit
    private static final int STRING_WIDTH_COMPENSATION = 10;

    // In Nimbus on Windows, the font widths are wrong
    private static final int EXTRA_WINDOWS_COMPENSATION = 15;

    private static final int MIN_EXTRA_LINE = 20;

    private static final long serialVersionUID = 1L;

    private final Color color;
    private final Synth synth;
    private String name = "";

    private String preamble;
    private String distributePreamble;
    private boolean pasteable;
    private boolean distributable;
    private boolean sendsAllParameters;
    private Gatherable auxillary;

    private JMenuItem copy = new JMenuItem("Copy Category");
    private JMenuItem paste = new JMenuItem("Paste Category");
    private JMenuItem distribute = new JMenuItem("Distribute");
    private JMenuItem copyFromMutable = new JMenuItem("Copy Category (Mutation Parameters Only)");
    private JMenuItem pasteToMutable = new JMenuItem("Paste Category (Mutation Parameters Only)");
    private JMenuItem distributeToMutable = new JMenuItem("Distribute (Mutation Parameters Only)");
    private final JMenuItem reset = new JMenuItem("Reset Category");
    private final JMenuItem rand25 = new JMenuItem("Randomize by 25%");
    private final JMenuItem rand50 = new JMenuItem("Randomize by 50%");
    private final JMenuItem rand75 = new JMenuItem("Randomize by 75%");
    private final JMenuItem rand100 = new JMenuItem("Randomize by 100%");

    private JPopupMenu pop = new JPopupMenu();
    private int stringWidth;

    /**
     * If synth is non-null, then double-clicking on the category will select or
     * deselect all the components inside it for mutation purposes.
     */
    public Category(final Synth synth, final String label, final Color color) {
        this.synth = synth;
        this.color = color;
        setName(label);
        setLayout(new BorderLayout());

        if (synth != null) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (!synth.isShowingMutation()) {
                        if (e.getY() < 20 && (stringWidth == 0 || e.getX() < stringWidth)) {
                            copy.setEnabled(pasteable);
                            copyFromMutable.setEnabled(pasteable);
                            paste.setEnabled(pasteable && isPasteCompatible(preamble));
                            pasteToMutable.setEnabled(pasteable && isPasteCompatible(preamble));
                            distribute.setEnabled(distributable && canDistributeKey());
                            distributeToMutable.setEnabled(distributable && canDistributeKey());
                            pop.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                }

                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (synth.isShowingMutation()) {
                        boolean inBorder = (e.getPoint().y < getInsets().top);
                        if (e.getClickCount() == 2 && inBorder) {
                            boolean turnOn = true;
                            List<Component> components = new ArrayList<>();
                            gatherAllComponents(components);
                            for (Component component : components) {
                                if (component instanceof NumericalComponent nc) {
                                    String key = nc.getKey();
                                    if (synth.mutationMap.isFree(key) && synth.getModel().getStatus(key) != Model.STATUS_IMMUTABLE) {
                                        turnOn = false;
                                        break;
                                    }
                                }
                            }

                            for (Component component : components) {
                                if (component instanceof NumericalComponent nc) {
                                    String key = nc.getKey();
                                    if (synth.getModel().getStatus(key) != Model.STATUS_IMMUTABLE) {
                                        synth.mutationMap.setFree(key, turnOn);
                                    }
                                }
                            }
                            repaint();
                        }
                    }
                }
            });
        }

        pop.add(copy);
        copy.addActionListener(e -> {
            copyCategory(true);
        });

        pop.add(paste);
        paste.addActionListener(e -> {
            synth.getUndo().push(synth.getModel());
            synth.getUndo().setWillPush(false);
            synth.setSendMIDI(false);
            pasteCategory(true);
            synth.setSendMIDI(true);
            // We do this TWICE because for some synthesizers, updating a parameter
            // will reveal other parameters which also must be updated but aren't yet
            // in the mapping.
            pasteCategory(true);
            synth.getUndo().setWillPush(true);
        });

        pop.add(distribute);
        distribute.addActionListener(e -> {
            synth.getUndo().push(synth.getModel());
            synth.getUndo().setWillPush(false);
            distributeCategory(true);
            synth.getUndo().setWillPush(true);
        });

        pop.addSeparator();

        pop.add(copyFromMutable);
        copyFromMutable.addActionListener(e -> {
            copyCategory(false);
        });

        pop.add(pasteToMutable);
        pasteToMutable.addActionListener(e -> {
            synth.getUndo().push(synth.getModel());
            synth.getUndo().setWillPush(false);
            synth.setSendMIDI(false);
            pasteCategory(false);
            synth.setSendMIDI(true);
            // We do this TWICE because for some synthesizers, updating a parameter
            // will reveal other parameters which also must be updated but aren't yet
            // in the mapping.
            pasteCategory(false);
            synth.getUndo().setWillPush(true);
        });

        pop.add(distributeToMutable);
        distributeToMutable.addActionListener(e -> {
            synth.getUndo().push(synth.getModel());
            synth.getUndo().setWillPush(false);
            distributeCategory(false);
            synth.getUndo().setWillPush(true);
        });

        pop.addSeparator();

        pop.add(reset);
        reset.addActionListener(e -> {
            synth.getUndo().push(synth.getModel());
            synth.getUndo().setWillPush(false);
            resetCategory();
            synth.getUndo().setWillPush(true);
        });

        pop.addSeparator();
        pop.add(rand25);
        rand25.addActionListener(e -> {
            randomizeCategory(0.5); // sqrt(0.25)
        });
        pop.add(rand50);
        rand50.addActionListener(e -> {
            randomizeCategory(0.7); // ~sqrt(0.5)
        });
        pop.add(rand75);
        rand75.addActionListener(e -> {
            randomizeCategory(0.85); // ~sqrt(0.75)
        });
        pop.add(rand100);
        rand100.addActionListener(e -> {
            randomizeCategory(1.0);
        });

        copy.setEnabled(false);
        copyFromMutable.setEnabled(false);
        paste.setEnabled(false);
        pasteToMutable.setEnabled(false);
        distribute.setEnabled(false);
        distributeToMutable.setEnabled(false);
        reset.setEnabled(true);
        rand25.setEnabled(true);
        rand50.setEnabled(true);
        rand75.setEnabled(true);
        rand100.setEnabled(true);

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    }

    public boolean isPasteCompatible(final String preamble) {
        String copyPreamble = synth.getCopyPreamble();
        if (copyPreamble == null) {
            return false;
        }

        if (preamble == null) {
            return false;
        }

        return (pasteable && StringUtility.reduceFirstDigitsAfterPreamble(copyPreamble, "").equals(StringUtility.reduceFirstDigitsAfterPreamble(preamble, "")));
    }

    public void makePasteable(String preamble) {
        pasteable = true;
        this.preamble = preamble;
    }

    public void makeDistributable(String preamble) {
        distributable = true;
        this.distributePreamble = preamble;
    }

    public void makeUnresettable() {
        reset.setEnabled(false);
        rand25.setEnabled(false);
        rand50.setEnabled(false);
        rand75.setEnabled(false);
        rand100.setEnabled(false);
    }

    public void setSendsAllParameters(boolean val) {
        sendsAllParameters = val;
    }

    public boolean getSendsAllParameters() {
        return sendsAllParameters;
    }

    /**
     * Returns an auxillary component. Sometimes a Category is broken into two
     * pieces (see KawaiK5 Harmonics (DHG) category for example), and when we
     * gather elements, we want to gather from the auxillary as well.
     */
    public Gatherable getAuxillary() {
        return auxillary;
    }

    /**
     * Sets an auxillary component. Sometimes a Category is broken into two
     * pieces (see KawaiK5 Harmonics (DHG) category for example), and when we
     * gather elements, we want to gather from the auxillary as well.
     */
    public void setAuxillary(final Gatherable auxillary) {
        this.auxillary = auxillary;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension minimumSize = super.getMinimumSize();

        Graphics2D graphics = (Graphics2D) getGraphics();
        if (graphics == null) {
            return minimumSize;
        }

        if (stringWidth == 0) {
            stringWidth = STRING_WIDTH_COMPENSATION + (Style.isWindows() ? EXTRA_WINDOWS_COMPENSATION : 0) + graphics.getFontMetrics(Style.CATEGORY_FONT()).stringWidth(name);
        }

        minimumSize.width = Math.max(minimumSize.width, stringWidth + MIN_EXTRA_LINE);
        return minimumSize;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        Dimension minimumSize = getMinimumSize();
        preferredSize.width = Math.max(preferredSize.width, minimumSize.width);
        return preferredSize;
    }

    @Override
    public Insets getInsets() {
        Insets insets = super.getInsets();
        return new Insets(insets.top, insets.left, 0, insets.right);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String label) {
        name = label != null ? label : "";
        stringWidth = 0; // reset
        getAccessibleContext().setAccessibleName(name);

        // here we're going to do a little hack.  TitledBorder doesn't put the title
        // on the FAR LEFT of the line, so when we draw the border we get a little square
        // dot to the left of the title which looks really annoying.  Rather than build a
        // totally new border, we're just going to change the insets.  Titled border uses
        // the insets of the underlying border as part of its calculation of where to put
        // the border, so if we subtract 5 from the insets of the underlying border this
        // counteracts the 5 pixels that titledBorder adds in to shift the title over to
        // the right annoyingly.  So during paintBorder, we turn off a flag, then when
        // super.paintBorder goes to grab the underlying border's insets, it gets a special
        // insets which are off by 5.  But other times the insets are requested (such as
        // in paintComponent) they return normal.
        final boolean[] paintingBorder = new boolean[1];

        final MatteBorder matteBorder = new MatteBorder(Style.CATEGORY_STROKE_WIDTH(), 0, 0, 0, color) {
            @Override
            public Insets getBorderInsets(Component c, Insets insets) {
                Insets ins = super.getBorderInsets(c, insets);
                if (paintingBorder[0]) {
                    ins.left = -5;
                }
                return ins;
            }
        };

        TitledBorder titledBorder = new TitledBorder(
            matteBorder,
            (name.isEmpty() ? "" : " " + name + " "),
            TitledBorder.LEFT,
            TitledBorder.TOP,
            Style.CATEGORY_FONT(),
            color) {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                paintingBorder[0] = true;
                super.paintBorder(c, g, x, y, width, height);
                paintingBorder[0] = false;
            }
        };

        Border b = BorderFactory.createCompoundBorder(Style.CATEGORY_BORDER(), titledBorder);
        setBorder(b);
        repaint();
    }

    @Override
    public void paintComponent(final Graphics g) {
        Graphics2D graphics = (Graphics2D) g;
        RenderingHints oldHints = graphics.getRenderingHints();
        Style.prepareGraphics(graphics);

        if (stringWidth == 0) {
            stringWidth = STRING_WIDTH_COMPENSATION + (Style.isWindows() ? EXTRA_WINDOWS_COMPENSATION : 0)
                + graphics.getFontMetrics(Style.CATEGORY_FONT()).stringWidth(name);
        }

        Rectangle rect = getBounds();
        rect.x = 0;
        rect.y = 0;
        graphics.setPaint(Style.BACKGROUND_COLOR());
        graphics.fill(rect);

        graphics.setRenderingHints(oldHints);
    }

    @Override
    public void gatherAllComponents(final List<Component> components) {
        gatherAllComponents(this, components);
        if (auxillary != null) {
            auxillary.gatherAllComponents(components);
        }
    }

    private boolean canDistributeKey() {
        String lastKey = synth.getModel().getLastKey();
        if (lastKey == null) {
            return false;
        }

        List<Component> components = new ArrayList<>();
        gatherAllComponents(components);
        for (Component component : components) {
            if (component instanceof HasKey nc) {
                String key = nc.getKey();
                if (key.equals(lastKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void resetCategory() {
        boolean currentMIDI = synth.getSendMIDI();
        synth.setSendMIDI(false);

        Synth other = Synth.instantiate(synth.getClass(), true, true, synth.tuple);
        List<Component> components = new ArrayList<>();
        gatherAllComponents(components);
        for (Component component : components) {
            if (component instanceof HasKey nc) {
                String key = nc.getKey();
                if (synth.getModel().exists(key) && other.getModel().exists(key)) {
                    if (synth.getModel().isString(key)) {
                        synth.getModel().set(key, other.getModel().get(key, ""));
                    } else {
                        synth.getModel().set(key, other.getModel().get(key, 0));
                    }
                } else {
                    System.err.println("Key missing in model : " + key);
                }
            }
        }

        synth.setSendMIDI(currentMIDI);
        if (sendsAllParameters) {
            synth.sendAllParameters();
        }

        // so we don't have independent updates in OS X
        repaint();
    }

    private void randomizeCategory(double weight) {
        boolean currentMIDI = synth.getSendMIDI();
        synth.setSendMIDI(false);

        List<String> keys = new ArrayList<>();

        // get all the components
        List<Component> components = new ArrayList<>();
        gatherAllComponents(components);
        for (Component component : components) {
            if (component instanceof HasKey nc) {
                String key = nc.getKey();
                if (synth.getModel().exists(key)) {
                    if (!synth.getModel().isString(key)) {
                        keys.add(key);
                    }
                } else {
                    System.err.println("Key missing in model : " + key);
                }
            }
        }

        // mutate
        String[] k = keys.toArray(String[]::new);
        synth.setModel(synth.getModel().mutate(synth.getRandom(), k, weight));

        // emit
        synth.setSendMIDI(currentMIDI);
        if (sendsAllParameters) {
            synth.sendAllParameters();
        }

        // so we don't have independent updates in OS X
        repaint();
    }

    private void copyCategory(boolean includeImmutable) {
        String[] mutationKeys = synth.getMutationKeys();
        if (mutationKeys == null) {
            mutationKeys = new String[0];
        }
        Set<String> mutationSet = new HashSet<>(Arrays.asList(mutationKeys));

        List<String> keys = new ArrayList<>();
        List<Component> components = new ArrayList<>();
        gatherAllComponents(components);
        for (Component component : components) {
            if (component instanceof HasKey nc) {
                String key = nc.getKey();
                if (mutationSet.contains(key) || includeImmutable) {
                    keys.add(key);
                }
            }
        }
        synth.setCopyKeys(keys);
        synth.setCopyPreamble(preamble);
    }

    private void pasteCategory(final boolean includeImmutable) {
        for (int i = 0; i < synth.getNumberOfPastes(); ++i) {
            pasteCategoryImpl(includeImmutable);
        }
    }

    private void pasteCategoryImpl(final boolean includeImmutable) {
        String copyPreamble = synth.getCopyPreamble();
        if (copyPreamble == null) {
            return;
        }

        String myPreamble = preamble;
        if (myPreamble == null) {
            return;
        }

        List<String> copyKeys = synth.getCopyKeys();
        if (copyKeys == null || copyKeys.isEmpty()) {
            return;  // oops
        }

        // First we need to map OUR keys
        Map<String, String> keys = new HashMap<>();
        List<Component> components = new ArrayList<>();
        gatherAllComponents(components);
        for (Component component : components) {
            if (component instanceof HasKey nc) {
                String key = nc.getKey();
                String reduced = StringUtility.reduceFirstDigitsAfterPreamble(key, myPreamble);
                reduced = StringUtility.reduceDigitsInPreamble(reduced, myPreamble);
                keys.put(reduced, key);
            }
        }

        boolean currentMIDI = synth.getSendMIDI();
        if (sendsAllParameters) {
            synth.setSendMIDI(false);
        }

        String[] mutationKeys = synth.getMutationKeys();
        if (mutationKeys == null) {
            mutationKeys = new String[0];
        }
        Set<String> mutationSet = new HashSet<>(Arrays.asList(mutationKeys));

        // Now we change keys as appropriate
        for (String key : copyKeys) {
            String reduced = StringUtility.reduceFirstDigitsAfterPreamble(key, copyPreamble);
            reduced = StringUtility.reduceDigitsInPreamble(reduced, copyPreamble);
            String mapped = keys.get(reduced);
            if (mapped != null) {
                Model model = synth.getModel();
                if (model.exists(mapped) && (mutationSet.contains(mapped) || includeImmutable)) {
                    if (model.isString(mapped)) {
                        model.set(mapped, model.get(key, model.get(mapped, "")));
                    } else {
                        model.set(mapped, model.get(key, model.get(mapped, 0)));
                    }
                } else {
                    System.err.println("Warning (Category) 2: Key didn't exist " + mapped);
                }
            } else {
                System.err.println("Warning (Category) 2: Null mapping for " + key + " (reduced to " + reduced + ")");
            }
        }

        synth.revise();

        if (sendsAllParameters) {
            synth.setSendMIDI(currentMIDI);
            synth.sendAllParameters();
        }

        // so we don't have independent updates in OS X
        repaint();
    }

    private void distributeCategory(final boolean includeImmutable) {
        Model model = synth.getModel();
        String lastKey = model.getLastKey();

        if (lastKey != null) {
            boolean currentMIDI = synth.getSendMIDI();
            if (sendsAllParameters) {
                synth.setSendMIDI(false);
            }

            String lastReduced = StringUtility.reduceAllDigitsAfterPreamble(lastKey, distributePreamble);

            String[] mutationKeys = synth.getMutationKeys();
            if (mutationKeys == null) {
                mutationKeys = new String[0];
            }
            Set<String> mutationSet = new HashSet<>(Arrays.asList(mutationKeys));

            // Now we change keys as appropriate
            List<Component> components = new ArrayList<>();
            gatherAllComponents(components);
            for (Component component : components) {
                if (component instanceof HasKey nc) {
                    String key = nc.getKey();
                    String reduced = StringUtility.reduceAllDigitsAfterPreamble(key, distributePreamble);

                    if (reduced.equals(lastReduced)) {
                        if (model.exists(key) && (mutationSet.contains(key) || includeImmutable)) {
                            if (model.isString(key)) {
                                model.set(key, model.get(lastKey, model.get(key, "")));
                            } else {
                                model.set(key, model.get(lastKey, model.get(key, 0)));
                            }
                        } else {
                            System.err.println("Warning (Category): Key didn't exist " + key);
                        }
                    } else {
                        System.err.println("Warning (Category): Null mapping for " + key + " (reduced to " + reduced + ")");
                    }
                }
            }

            synth.revise();

            if (sendsAllParameters) {
                synth.setSendMIDI(currentMIDI);
                synth.sendAllParameters();
            }
        }

        // so we don't have independent updates in OS X
        repaint();
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
