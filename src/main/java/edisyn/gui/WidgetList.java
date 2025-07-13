package edisyn.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class WidgetList extends JPanel {

    private static final long serialVersionUID = 1L;

    public WidgetList(final List<String> labels, final List<? extends JComponent> widgets) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        int limit = Math.min(labels.size(), widgets.size());

        for (int i = 0; i < limit; ++i) {
            c.gridx = 0;
            c.gridy = i;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.LINE_END;
            c.weightx = 0;
            c.weighty = 1;
            panel.add(new JLabel(labels.get(i) + " ", SwingConstants.RIGHT), c);

            c.gridx = 1;
            c.anchor = GridBagConstraints.LINE_START;
            c.weightx = 1;
            panel.add(widgets.get(i), c);
        }

        setLayout(new BorderLayout());
        add(panel, BorderLayout.SOUTH);
    }
}
