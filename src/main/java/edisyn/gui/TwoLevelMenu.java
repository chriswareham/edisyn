package edisyn.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

public class TwoLevelMenu extends JPanel {

    JComboBox primaryBox;
    JComboBox[] secondaryBox;
    JComboBox currentSecondary;
    JPanel currentSecondaryContainer;
    WidgetList list;

    public TwoLevelMenu(Object[] primary, Object[][] secondary,
        String primaryLabel, String secondaryLabel,
        int initialPrimary, int initialSecondary) {
        double maxWidth = 0;
        primaryBox = new JComboBox(primary);
        primaryBox.setMaximumRowCount(25);
        secondaryBox = new JComboBox[secondary.length];

        // fix to standard widths
        for (int i = 0; i < secondary.length; i++) {
            secondaryBox[i] = new JComboBox(secondary[i]);
            secondaryBox[i].setMaximumRowCount(25);
            double w = secondaryBox[i].getPreferredSize().getWidth();
            if (maxWidth < w) {
                maxWidth = w;
            }
        }
        for (int i = 0; i < secondary.length; i++) {
            Dimension d = secondaryBox[i].getPreferredSize();
            d.width = (int) (maxWidth);
            secondaryBox[i].setPreferredSize(d);
        }

        setLayout(new BorderLayout());
        primaryBox.setSelectedIndex(initialPrimary);
        currentSecondary = secondaryBox[initialPrimary];
        currentSecondary.setSelectedIndex(initialSecondary);
        currentSecondaryContainer = new JPanel();
        currentSecondaryContainer.setLayout(new BorderLayout());
        currentSecondaryContainer.add(currentSecondary, BorderLayout.CENTER);
        if (primary.length == 1) {
            primaryBox.setEnabled(false);
        }

        list = new WidgetList(List.of(primaryLabel, secondaryLabel), List.of(primaryBox, currentSecondaryContainer));
        add(list, BorderLayout.CENTER);

        for (int i = 0; i < secondary.length; i++) {
            secondaryBox[i].addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) // not interested in deselection events
                    {
                        selection(getPrimary(), getSecondary());
                    }
                }
            });
        }

        primaryBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) // not interested in deselection events
                {
                    updateSecondaryBox();
                    selection(getPrimary(), getSecondary());
                }
            }
        });
    }

    /**
     * Override this to be informed when a selection is made.
     */
    public void selection(int primary, int secondary) {
    }

    void updateSecondaryBox() {
        currentSecondaryContainer.remove(currentSecondary);
        currentSecondary = secondaryBox[primaryBox.getSelectedIndex()];
        currentSecondaryContainer.add(currentSecondary, BorderLayout.SOUTH);
        currentSecondaryContainer.revalidate();
        currentSecondaryContainer.repaint();
    }

    public int getPrimary() {
        return primaryBox.getSelectedIndex();
    }

    public int getSecondary() {
        return currentSecondary.getSelectedIndex();
    }

}
