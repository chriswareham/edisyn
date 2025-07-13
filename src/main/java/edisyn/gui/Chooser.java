/*
 * Copyright 2017 by Sean Luke
 * Licensed under the Apache License version 2.0
 */

package edisyn.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;

import javax.accessibility.AccessibleContext;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import edisyn.Model;
import edisyn.Synth;

/**
 * A wrapper for JComboBox which edits and responds to changes to a numerical
 * value in the model. The numerical value is assumed to of a min/max range
 * 0...n-1, which corresponds to the n elements displayed in the JComboBox.
 * However you can change this and in fact map each element to its own special
 * integer.
 *
 * For the Mac, the JComboBox is made small (JComponent.sizeVariant = small),
 * but this probably won't do anything in Linux or Windows.
 *
 * @author Sean Luke
 */
public class Chooser extends NumericalComponent {

    // Nimbus unfortunately varies the height of its combo boxes
    public static final int MIN_NIMBUS_HEIGHT = 23;

    private static final long serialVersionUID = 1L;

    private static int[] buildDefaultValues(final String[] elements) {
        int[] values = new int[elements.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }
        return values;
    }

    private final JComboBox<String> combo;
    private int addToWidth;
    // The integers corresponding to each element in the JComboBox.
    private int[] vals;
    private final String[] labels;
    private final ImageIcon[] icons;
    private boolean callActionListener = true;

    private final JLabel label = new JLabel("888", SwingConstants.LEFT) {
        @Override
        public Insets getInsets() {
            return new Insets(0, 0, 0, 0);
        }
    };

    /**
     * Creates a JComboBox with the given label, modifying the given key in the
     * Style. The elements in the box are given by elements, and their
     * corresponding numerical values in the model 0...n.
     */
    public Chooser(String text, Synth synth, String key, String[] elements) {
        this(text, synth, key, elements, buildDefaultValues(elements));
    }

    /**
     * Creates a JComboBox with the given label, modifying the given key in the
     * Style. The elements in the box are given by elements, and their
     * corresponding numerical values in the model 0...n.
     */
    public Chooser(String text, Synth synth, String key, String[] elements, int[] values) {
        this(text, synth, key, elements, values, null);
    }

    /**
     * Creates a JComboBox with the given label, modifying the given key in the
     * Style. The elements in the box are given by elements, with images in
     * icons, and their corresponding numerical values in the model 0...n. Note
     * that OS X won't behave properly with icons larger than about 34 high.
     */
    public Chooser(String text, final Synth synth, final String key, String[] elements, ImageIcon[] icons) {
        this(text, synth, key, elements, buildDefaultValues(elements), icons);
    }

    public Chooser(String text, final Synth synth, final String key, String[] elements, int[] values, ImageIcon[] icons) {
        super(synth, key);

        label.setFont(Style.SMALL_FONT());
        label.setBackground(Style.BACKGROUND_COLOR()); // TRANSPARENT);
        label.setForeground(Style.TEXT_COLOR());

        combo = new JComboBox<>(elements) {
            private AccessibleContext accessibleContext;

            // Generate and provide the context information when asked
            @Override
            public AccessibleContext getAccessibleContext() {
                if (accessibleContext == null) {
                    accessibleContext = new AccessibleJComboBox() {
                        @Override
                        public String getAccessibleName() {
                            String name = super.getAccessibleName();
                            // Find enclosing Category
                            Component component = combo;
                            while (component != null) {
                                if (component instanceof Category category) {
                                    return name + " " + category.getName();
                                } else {
                                    component = component.getParent();
                                }
                            }
                            return name;
                        }
                    };
                }
                return accessibleContext;
            }

            public void setPopupVisible(boolean val) {
                if (val == true
                    || synth == null
                    || // unlikely
                    synth.persistentChooserMenu == null
                    || // such as in Morph
                    !(synth.persistentChooserMenu.isSelected())) {
                    super.setPopupVisible(val);
                }
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width += addToWidth;
                if (Style.isNimbus()) {
                    d.height = Math.max(d.height, MIN_NIMBUS_HEIGHT);
                }
                return d;
            }

            @Override
            protected void processMouseEvent(final MouseEvent e) {
                super.processMouseEvent(e);
                if (e.getID() == MouseEvent.MOUSE_CLICKED) {
                    if (synth.isShowingMutation()) {
                        synth.mutationMap.setFree(key, !synth.mutationMap.isFree(key));
                        // wrap the repaint in an invokelater because the dial isn't responding right
                        SwingUtilities.invokeLater(() -> {
                            repaint();
                        });
                    }
                }
            }
        };

        combo.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {
                if (synth.persistentChooserMenu != null) {
                    synth.persistentChooserMenu.setSelected(false);
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
            }
        });

        combo.putClientProperty("JComponent.sizeVariant", "small");
        combo.setEditable(false);
        combo.setFont(Style.SMALL_FONT());
        combo.setMaximumRowCount(34);           // 34, not 32, to accommodate modulation sources for the Waldorf Kyra

        setElements(text, elements, values);

        this.icons = icons;
        this.labels = elements;
        if (icons != null) {
            combo.setRenderer(new ComboBoxRenderer());
            combo.putClientProperty("JComponent.sizeVariant", "regular");
        }

        setState(getState());

        setLayout(new BorderLayout());
        add(combo, BorderLayout.CENTER);

        JPanel label2 = new JPanel();
        label2.setLayout(new BorderLayout());
        label2.add(label, BorderLayout.CENTER);
        label2.setBackground(Style.BACKGROUND_COLOR());
        if (isLabelToLeft()) {
            add(label2, BorderLayout.WEST);
        } else {
            add(label2, BorderLayout.NORTH);
        }

        if (Style.isNimbus()) {
            //add(Strut.makeVerticalStrut(4), BorderLayout.SOUTH);
            label2.add(Strut.makeHorizontalStrut(3), BorderLayout.EAST);

            UIDefaults defaults = new UIDefaults();
            defaults.put("ComboBox.contentMargins", new Insets(0, 0, 0, 0)); // the default for Nimbus is 0, 6, 0, 3
            defaults.put("ComboBox:\"ComboBox.textField\".contentMargins", new Insets(0, 100, 0, 0)); // the default for Nimbus is 0, 6, 0, 3
            combo.putClientProperty("Nimbus.Overrides", defaults);
            combo.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
        }

        /// Apparent OS X Java bug: sometimes after you programmatically change
        /// the value of a JComboBox, it no longer sends ActionListener events.  :-(
        combo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) // we're not interested in deselection events
                {
                    // This is due to a Java bug.
                    // Unlike other widgets (like JCheckBox), JComboBox calls
                    // the actionlistener even when you programmatically change
                    // its value.  OOPS.
                    if (callActionListener) {
                        setState(vals[combo.getSelectedIndex()]);
                        userSelected(key, synth.getModel());
                    }
                }
            }
        });
    }

    public JLabel getLabel() {
        return label;
    }

    public String getLabelText() {
        return label.getText().trim();
    }

    public String map(final int val) {
        return "" + combo.getItemAt(val);
    }

    public void setCallActionListener(final boolean callActionListener) {
        this.callActionListener = callActionListener;
    }

    @Override
    public void updateBorder() {
        super.updateBorder();
        // this part prevents us from repeatedly calling setEnabled()... which creates a repaint loop
        if (combo != null && combo.isEnabled() == synth.isShowingMutation()) {
            if (synth.isShowingMutation()) {
                combo.setEnabled(false);
            } else {
                combo.setEnabled(true);
            }
        }
    }

    @Override
    public void update(final String key, final Model model) {
        if (combo == null) {
            return;  // we're not ready yet
        }

        int state = getState();

        // it's possible that we're sharing a parameter
        // (see for example Blofeld Parameter 9), so here
        // we need to make sure we're within bounds
        if (state < 0) {
            state = 0;
        }
        if (state > vals.length) {
            state = vals.length - 1;
        }

        // look for it...
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == state) {
                if (combo.getSelectedIndex() != i) {
                    // This is due to a Java bug.
                    // Unlike other widgets (like JCheckBox), JComboBox calls
                    // the actionlistener even when you programmatically change
                    // its value.  OOPS.
                    setCallActionListener(false);
                    combo.setSelectedIndex(i);
                    setCallActionListener(true);
                }
                return;
            }
        }
    }

    @Override
    public Insets getInsets() {
        if (Style.CHOOSER_INSETS() == null) {
            return super.getInsets();
        } else {
            return Style.CHOOSER_INSETS();
        }
    }

    public void userSelected(String key, Model model) {
    }

    public boolean isLabelToLeft() {
        return false;
    }

    public void addToWidth(final int val) {
        addToWidth = val;
    }

    public JComboBox<String> getCombo() {
        return combo;
    }

    public String getElement(int position) {
        return combo.getItemAt(position);
    }

    public int getNumElements() {
        return combo.getItemCount();
    }

    public int getIndex() {
        return combo.getSelectedIndex();
    }

    public void setIndex(int index) {
        setCallActionListener(false);
        if (index < getNumElements()) {
            combo.setSelectedIndex(index);
        }
        setCallActionListener(true);
    }

    public void setLabel(String _label) {
        if (Style.isNimbus()) {
            label.setText(" " + _label);
        } else if (Style.isMac()) {
            label.setText("  " + _label);
        } else {
            label.setText(_label);
        }
        combo.getAccessibleContext().setAccessibleName(_label);
    }

    public void setElements(String[] elements) {
        setElements(getLabelText(), elements);
    }

    public void setElements(String _label, String[] elements) {
        setElements(_label, elements, buildDefaultValues(elements));
    }

    // Simply replaces the item text.  The number of elements should be identical.
    public void replaceElements(String[] elements) {
        setCallActionListener(false);
        int val = combo.getSelectedIndex();
        combo.removeAllItems();
        for (String element : elements) {
            combo.addItem(element);
        }
        if (val >= 0) {
            combo.setSelectedIndex(val);
        }
        setCallActionListener(true);
    }

    public void setElements(String _label, String[] elements, int[] values) {
        if (Style.showRaw) {
            elements = elements.clone();
            for (int i = 0; i < elements.length; i++) {
                elements[i] = "" + i + ": " + elements[i];
            }
        }

        setCallActionListener(false);
        if (Style.isNimbus()) {
            label.setText(" " + _label);
        } else if (Style.isMac()) {
            label.setText("  " + _label);
        } else {
            label.setText(_label);
        }
        combo.getAccessibleContext().setAccessibleName(_label);

        combo.removeAllItems();

        for (String element : elements) {
            combo.addItem(element);
        }

        vals = values.clone();

        int _min = Integer.MAX_VALUE;
        int _max = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (_min > values[i]) {
                _min = values[i];
            }
            if (_max < values[i]) {
                _max = values[i];
            }
        }

        setMin(_min);
        setMax(_max);
        setCallActionListener(true);

        if (getState() >= elements.length) {
            System.err.println("resetting from " + getState());
            setState(0);
        }
        revalidate();
        if (isVisible()) {
            repaint();
        }
    }

    private class ComboBoxRenderer extends JLabel implements ListCellRenderer {

        private static final long serialVersionUID = 1L;

        ComboBoxRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
        }

        /*
         * This method finds the image and text corresponding
         * to the selected value and returns the label, set up
         * to display the text and image.
         */
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            // Get the selected index. (The index param isn't always valid, so just use the value.)
            //int selectedIndex = ((Integer)value).intValue();

            if (index == -1) {
                index = combo.getSelectedIndex();
            }
            if (index == -1) {
                return this;
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            // Set the icon and text.  If icon was null, say so.
            ImageIcon icon = icons[index];
            String label = labels[index];
            setIcon(icon);
            setText(label);
            setFont(list.getFont());
            return this;
        }
    }
}
