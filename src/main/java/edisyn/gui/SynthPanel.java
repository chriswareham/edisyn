/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import edisyn.Model;
import edisyn.Synth;
import edisyn.util.StringUtility;

/**
 * A pretty panel for a synthesizer
 *
 * @author Sean Luke
 */
public class SynthPanel extends JPanel implements Gatherable {

    private static final long serialVersionUID = 1L;

    protected final Synth synth;

    private String preamble = "";

    private boolean unresettable;

    private boolean pasteable;

    private boolean sendsAllParameters = true;

    public SynthPanel(final Synth synth) {
        this.synth = synth;
        setLayout(new BorderLayout());
        setBackground(Style.BACKGROUND_COLOR());
        setBorder(BorderFactory.createMatteBorder(2, 2, 0, 4, Style.BACKGROUND_COLOR()));
    }

    public Synth getSynth() {
        return synth;
    }

    @Override
    public Insets getInsets() {
        return Style.SYNTH_PANEL_INSETS();
    }

    public boolean isPasteable() {
        return pasteable;
    }

    public boolean isPasteCompatible(final String preamble) {
        String copyPreamble = synth.getCopyPreamble();
        if (copyPreamble == null) {
            return false;
        }

        if (preamble == null) {
            return false;
        }

        return pasteable && StringUtility.reduceAllDigitsAfterPreamble(copyPreamble, "").equals(StringUtility.reduceAllDigitsAfterPreamble(preamble, ""));
    }

    public void makePasteable(final String preamble) {
        pasteable = true;
        this.preamble = preamble;
    }

    public boolean isUnresettable() {
        return unresettable;
    }

    public void makeUnresettable() {
        unresettable = true;
    }

    public boolean getSendsAllParameters() {
        return sendsAllParameters;
    }

    public void setSendsAllParameters(final boolean sendsAllParameters) {
        this.sendsAllParameters = sendsAllParameters;
    }

    public void resetPanel() {
        boolean currentMIDI = synth.getSendMIDI();
        if (sendsAllParameters) {
            synth.setSendMIDI(false);
        }

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
                    System.err.println("Warning (SynthPanel): Key missing in model : " + key);
                }
            }
        }

        if (sendsAllParameters) {
            synth.setSendMIDI(currentMIDI);
            synth.sendAllParameters();
        }

        // so we don't have independent updates in OS X
        repaint();
    }

    public void copyPanel(final boolean includeImmutable) {
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

    public void pastePanel(boolean includeImmutable) // ugly hack
    {
        for (int i = 0; i < synth.getNumberOfPastes(); i++) {
            pastePanel1(includeImmutable);
        }
    }

    void pastePanel1(boolean includeImmutable) {
        String copyPreamble = synth.getCopyPreamble();
        String myPreamble = preamble;
        if (copyPreamble == null) {
            return;
        }
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
                    System.err.println("Warning (SynthPanel): Key didn't exist " + mapped);
                }
            } else {
                System.err.println("Warning (SynthPanel): Null mapping for " + key + " (reduced to " + reduced + ")");
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
