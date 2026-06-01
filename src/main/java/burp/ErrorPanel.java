package burp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class ErrorPanel {
    IBurpExtenderCallbacks callbacks;
    BurpExtender burpExtender;

    static Color BURP_ORANGE  = new Color(255, 128, 0);
    static Color BURP_ORANGE2 = new Color(220, 100, 0);

    private Font headerFont  = new Font("Dialog", Font.BOLD, 15);
    private Font triggerFont = new Font("Dialog", Font.BOLD, 13);
    private Font labelFont   = new Font("Dialog", Font.PLAIN, 12);
    private Font buttonFont  = new Font("Dialog", Font.BOLD, 12);

    public static ArrayList<ErrorEntry> errorEntrylist = new ArrayList<ErrorEntry>();
    public static IMessageEditor ireqMessageEditor, iresMessageEditor;
    public static JComboBox<String> triggerComboBox;
    public static JTextField triggerNameField;
    public static JRadioButton statusRadio;
    public static JRadioButton bodyRadio;
    public static JTextField triggerValue;
    public static ErrorTable errorTable;
    public static ErrorTableModel errorTableModel;
    public static String host, protocol, comment, highlight;
    public static int port;

    public ErrorPanel(IBurpExtenderCallbacks callbacks, BurpExtender burpExtender) {
        this.callbacks    = callbacks;
        this.burpExtender = burpExtender;
    }

    // =========================================================================
    // Root panel
    // =========================================================================
    public JPanel preparePanel() {
        JPanel root = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(0, 0, 0, 0);

        gc.gridy = 0; gc.weighty = 0;
        root.add(buildHeaderPanel(), gc);

        gc.gridy = 1;
        root.add(makeSeparator(), gc);

        // Trigger form + table — compact row (table is now 3 columns, no Description)
        gc.gridy = 2;
        JPanel triggerAndTable = buildTriggerAndTablePanel();
        triggerAndTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        root.add(triggerAndTable, gc);

        gc.gridy = 3;
        root.add(makeSeparator(), gc);

        // Request / Response fills remaining vertical space
        gc.gridy = 4; gc.fill = GridBagConstraints.BOTH; gc.weighty = 1.0;
        root.add(prepareRequestResponsePanel(), gc);

        callbacks.customizeUiComponent(root);
        return root;
    }

    // =========================================================================
    // Header
    // =========================================================================
    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(8, 16, 8, 16));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0; gc.anchor = GridBagConstraints.WEST;

        JLabel header = new JLabel("Spot Error Condition");
        header.setForeground(BURP_ORANGE); header.setFont(headerFont);
        callbacks.customizeUiComponent(header);

        JLabel desc = new JLabel("Select error condition which triggers ATOR and run the configured flow "
                + "to obtain token and replace on this request to make as valid and continue scan");
        desc.setFont(labelFont);
        callbacks.customizeUiComponent(desc);

        gc.gridy = 0; gc.insets = new Insets(0, 0, 4, 0); panel.add(header, gc);
        gc.gridy = 1; gc.insets = new Insets(0, 0, 0, 0); panel.add(desc,   gc);
        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Trigger form (left) + table (right)
    // =========================================================================
    private JPanel buildTriggerAndTablePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(8, 16, 8, 16));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.fill = GridBagConstraints.BOTH; gc.weighty = 1.0; gc.insets = new Insets(0, 0, 0, 0);

        // Left: trigger form — fixed preferred height matching input rows
        gc.gridx = 0; gc.weightx = 0.0;
        JPanel trigger = triggerConditionPanel();
        // compute a preferred height that covers the title, three input rows and the buttons
        int desiredHeight = 160; // tuned to inputs: radios(26) + name(26) + value(26) + buttons(28) + headers/gaps
        trigger.setPreferredSize(new Dimension(320, desiredHeight));
        trigger.setMaximumSize(new Dimension(320, desiredHeight));
        panel.add(trigger, gc);

        // Vertical divider
        gc.gridx = 1; gc.weightx = 0.0; gc.fill = GridBagConstraints.VERTICAL;
        gc.insets = new Insets(0, 8, 0, 8);
        Component sep = makeVerticalSeparator();
        sep.setPreferredSize(new Dimension(1, desiredHeight));
        panel.add(sep, gc);

        // Right: table fills remaining width
        gc.gridx = 2; gc.weightx = 1.0; gc.fill = GridBagConstraints.BOTH; gc.insets = new Insets(0, 0, 0, 0);
        JScrollPane tableScroll = generateTablePanel(trigger.getPreferredSize().height);
        panel.add(tableScroll, gc);

        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Trigger condition form
    //   - "Status Code" and "Body" only  (Header removed per request)
    //   - optional condition name field
    //   - "Add Condition" button
    //   - "Delete Condition" button (replaces right-click)
    // =========================================================================
    public JPanel triggerConditionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(0, 0, 0, 12));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0; gc.anchor = GridBagConstraints.NORTHWEST;

        JLabel lbl = new JLabel("Trigger Condition");
        lbl.setForeground(BURP_ORANGE); lbl.setFont(triggerFont);
        callbacks.customizeUiComponent(lbl);
        gc.gridy = 0; gc.insets = new Insets(0, 0, 6, 0); gc.weighty = 0;
        panel.add(lbl, gc);

        // Type (radio buttons) on the left, Name on the right
        JLabel typeLabel = new JLabel("Type");
        typeLabel.setFont(labelFont);
        callbacks.customizeUiComponent(typeLabel);
        gc.gridy = 1; gc.gridx = 0; gc.insets = new Insets(0, 0, 4, 12); gc.weightx = 0;
        panel.add(typeLabel, gc);

        // Radio buttons for type
        statusRadio = new JRadioButton("Status Code");
        bodyRadio   = new JRadioButton("Body");
        ButtonGroup tg = new ButtonGroup(); tg.add(statusRadio); tg.add(bodyRadio);
        statusRadio.setFont(labelFont); bodyRadio.setFont(labelFont);
        callbacks.customizeUiComponent(statusRadio); callbacks.customizeUiComponent(bodyRadio);
        JPanel radioRow = new JPanel(new GridBagLayout()); radioRow.setOpaque(false);
        GridBagConstraints r = new GridBagConstraints(); r.gridx = 0; r.gridy = 0; r.insets = new Insets(0,0,0,8);
        radioRow.add(statusRadio, r); r.gridx = 1; radioRow.add(bodyRadio, r);
        gc.gridx = 1; gc.weightx = 1.0; gc.insets = new Insets(0, 0, 4, 0);
        panel.add(radioRow, gc);

        // Hidden combo kept for backward compatibility with other callers
        triggerComboBox = new JComboBox<String>();
        triggerComboBox.addItem("Status Code");
        triggerComboBox.addItem("Body");
        triggerComboBox.setVisible(false);
        callbacks.customizeUiComponent(triggerComboBox);
        gc.gridx = 2; gc.weightx = 0; panel.add(triggerComboBox, gc);

        // Name label and field
        JLabel nameLabel = new JLabel("Name");
        nameLabel.setFont(labelFont);
        callbacks.customizeUiComponent(nameLabel);
        gc.gridx = 0; gc.gridy = 2; gc.insets = new Insets(0, 0, 4, 12); gc.weightx = 0;
        panel.add(nameLabel, gc);

        triggerNameField = new JTextField();
        triggerNameField.setPreferredSize(new Dimension(260, 26));
        triggerNameField.setMaximumSize(new Dimension(260, 26));
        callbacks.customizeUiComponent(triggerNameField);
        gc.gridx = 1; gc.gridy = 2; gc.insets = new Insets(0, 0, 4, 0); gc.weightx = 1.0;
        panel.add(triggerNameField, gc);

        JLabel valueLabel = new JLabel("Value");
        valueLabel.setFont(labelFont);
        callbacks.customizeUiComponent(valueLabel);
        gc.gridx = 0; gc.gridy = 3; gc.insets = new Insets(0, 0, 6, 12); gc.weightx = 0;
        panel.add(valueLabel, gc);

        // Single-line value input
        triggerValue = new JTextField();
        triggerValue.setPreferredSize(new Dimension(260, 26));
        triggerValue.setMaximumSize(new Dimension(260, 26));
        callbacks.customizeUiComponent(triggerValue);
        gc.gridx = 1; gc.gridy = 3; gc.insets = new Insets(0, 0, 6, 0); gc.weightx = 1.0;
        panel.add(triggerValue, gc);

        // Wire radio buttons to sync with hidden combo and set name behavior
        statusRadio.addActionListener(e -> {
            triggerComboBox.setSelectedItem("Status Code");
            // compute next Status-N
            int max = 0;
            for (ErrorEntry ee : errorEntrylist) {
                if ("Status Code".equals(ee.getCategory())) {
                    String n = ee.getConditionname();
                    if (n != null && n.startsWith("Status-")) {
                        try { int v = Integer.parseInt(n.substring(7)); if (v > max) max = v; } catch (Exception ex) {}
                    }
                }
            }
            triggerNameField.setText("Status-" + (max + 1));
            triggerNameField.setEnabled(true);
        });

        bodyRadio.addActionListener(e -> {
            triggerComboBox.setSelectedItem("Body");
            // compute next Body-N
            int max = 0;
            for (ErrorEntry ee : errorEntrylist) {
                if ("Body".equals(ee.getCategory())) {
                    String n = ee.getConditionname();
                    if (n != null && n.startsWith("Body-")) {
                        try { int v = Integer.parseInt(n.substring(5)); if (v > max) max = v; } catch (Exception ex) {}
                    }
                }
            }
            triggerNameField.setText("Body-" + (max + 1));
            triggerNameField.setEnabled(true);
        });

        // initialize default selection
        statusRadio.setSelected(true);
        triggerComboBox.setSelectedItem("Status Code");
        // set initial Status-N
        {
            int max = 0;
            for (ErrorEntry ee : errorEntrylist) {
                if ("Status Code".equals(ee.getCategory())) {
                    String n = ee.getConditionname();
                    if (n != null && n.startsWith("Status-")) {
                        try { int v = Integer.parseInt(n.substring(7)); if (v > max) max = v; } catch (Exception ex) {}
                    }
                }
            }
            triggerNameField.setText("Status-" + (max + 1));
            triggerNameField.setEnabled(true);
        }

        // Keep combo and radios in sync if other code manipulates the combo
        triggerComboBox.addItemListener(ev -> {
            Object sel = triggerComboBox.getSelectedItem();
            if (sel != null && sel.equals("Status Code")) {
                if (!statusRadio.isSelected()) statusRadio.setSelected(true);
                int max = 0;
                for (ErrorEntry ee : errorEntrylist) {
                    if ("Status Code".equals(ee.getCategory())) {
                        String n = ee.getConditionname();
                        if (n != null && n.startsWith("Status-")) {
                            try { int v = Integer.parseInt(n.substring(7)); if (v > max) max = v; } catch (Exception ex) {}
                        }
                    }
                }
                triggerNameField.setText("Status-" + (max + 1));
                triggerNameField.setEnabled(true);
            } else if (sel != null && sel.equals("Body")) {
                if (!bodyRadio.isSelected()) bodyRadio.setSelected(true);
                // compute next Body-N like bodyRadio handler
                int max = 0;
                for (ErrorEntry ee : errorEntrylist) {
                    if ("Body".equals(ee.getCategory())) {
                        String n = ee.getConditionname();
                        if (n != null && n.startsWith("Body-")) {
                            try { int v = Integer.parseInt(n.substring(5)); if (v > max) max = v; } catch (Exception ex) {}
                        }
                    }
                }
                triggerNameField.setText("Body-" + (max + 1));
                triggerNameField.setEnabled(true);
            }
        });

        // Add Condition button
        JButton addCondition = new JButton("Add");
        addCondition.setFont(buttonFont);
        addCondition.setForeground(Color.WHITE);
        addCondition.setBackground(BURP_ORANGE2);
        addCondition.setOpaque(true); addCondition.setBorderPainted(false); addCondition.setFocusPainted(false);
        addCondition.setPreferredSize(new Dimension(100, 28));
        addCondition.setMaximumSize(new Dimension(100, 28));
        callbacks.customizeUiComponent(addCondition);

        // Delete Condition button (replaces right-click popup)
        JButton deleteCondition = new JButton("Delete");
        deleteCondition.setFont(buttonFont);
        deleteCondition.setForeground(Color.WHITE);
        deleteCondition.setBackground(new Color(160, 60, 60));
        deleteCondition.setOpaque(true); deleteCondition.setBorderPainted(false); deleteCondition.setFocusPainted(false);
        deleteCondition.setPreferredSize(new Dimension(100, 28));
        deleteCondition.setMaximumSize(new Dimension(100, 28));
        callbacks.customizeUiComponent(deleteCondition);

        // ── Add Condition listener ─────────────────────────────────────────────
        addCondition.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedCategory = (String) triggerComboBox.getSelectedItem();
                String enteredValue = triggerValue.getText();
                String enteredName = triggerNameField.getText();
                if (enteredValue.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Error condition value should not be empty",
                            "Empty Value", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String descriptionValue = generateDescription(selectedCategory, enteredValue);
                String conditionName = enteredName == null ? null : enteredName.trim();
                if (conditionName != null && conditionName.isEmpty()) {
                    conditionName = null;
                }
                ErrorEntry errorEntry = new ErrorEntry(conditionName, selectedCategory, enteredValue, descriptionValue);
                ReplacePanel.triggerConditionNameCombo.addItem(errorEntry.getConditionname());
                for (MultipleErrorCondition ec : ReplacePanel.multipleErrorConditions)
                    ec.triggerComboBox.addItem(errorEntry.getConditionname());
                errorEntrylist.add(errorEntry);
                errorTableModel.fireTableDataChanged();
                // reset value field
                triggerValue.setText("");
                // reset name field based on selected type
                if (statusRadio.isSelected()) {
                    int max = 0;
                    for (ErrorEntry ee : errorEntrylist) {
                        if ("Status Code".equals(ee.getCategory())) {
                            String n = ee.getConditionname();
                            if (n != null && n.startsWith("Status-")) {
                                try { int v = Integer.parseInt(n.substring(7)); if (v > max) max = v; } catch (Exception ex) {}
                            }
                        }
                    }
                    triggerNameField.setText("Status-" + (max + 1));
                    triggerNameField.setEnabled(true);
                } else if (bodyRadio.isSelected()) {
                    int max = 0;
                    for (ErrorEntry ee : errorEntrylist) {
                        if ("Body".equals(ee.getCategory())) {
                            String n = ee.getConditionname();
                            if (n != null && n.startsWith("Body-")) {
                                try { int v = Integer.parseInt(n.substring(5)); if (v > max) max = v; } catch (Exception ex) {}
                            }
                        }
                    }
                    triggerNameField.setText("Body-" + (max + 1));
                    triggerNameField.setEnabled(true);
                }
            }
        });

        // ── Delete Condition listener ──────────────────────────────────────────
        deleteCondition.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (errorTable == null) return;
                int selectedRow = errorTable.getSelectedRow();
                if (selectedRow < 0) {
                    JOptionPane.showMessageDialog(null, "Select a condition row to delete.",
                            "No Selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String errorConditionName = (String) errorTable.getModel().getValueAt(selectedRow, 0);
                for (int index = 0; index < errorEntrylist.size(); index++) {
                    ErrorEntry errorEntry = errorEntrylist.get(index);
                    if (errorEntry.getConditionname().equals(errorConditionName)) {
                        ReplacePanel.triggerConditionNameCombo.removeItem(errorConditionName);
                        for (MultipleErrorCondition ec : ReplacePanel.multipleErrorConditions)
                            ec.triggerComboBox.removeItem(errorConditionName);
                        errorEntrylist.remove(index);
                        errorTableModel.fireTableDataChanged();
                        break;
                    }
                }
            }
        });

        // Button row: [Add]  [Delete]
        JPanel btnRow = new JPanel(new GridBagLayout());
        GridBagConstraints br = new GridBagConstraints();
        br.gridy = 0; br.fill = GridBagConstraints.NONE; br.anchor = GridBagConstraints.WEST;
        br.gridx = 0; br.insets = new Insets(0, 0, 0, 8); btnRow.add(addCondition, br);
        br.gridx = 1; br.insets = new Insets(0, 0, 0, 0); btnRow.add(deleteCondition, br);

        gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 2; gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(0, 0, 0, 0);
        panel.add(btnRow, gc);

        gc.gridy = 5; gc.fill = GridBagConstraints.VERTICAL; gc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gc);

        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Table — columns: Type | Name | Value   (Description column removed)
    // =========================================================================
    public JScrollPane generateTablePanel(int preferredHeight) {
        errorTableModel = new ErrorTableModel(errorEntrylist);
        errorTable = new ErrorTable(errorTableModel, callbacks);
        errorTable.setModel(errorTableModel);
        this.callbacks.customizeUiComponent(errorTable);

        // Move the Category column in front of Name so the table reads Type | Name | Value.
        if (errorTable.getColumnModel().getColumnCount() > 1) {
            errorTable.getColumnModel().moveColumn(1, 0);
            errorTable.getColumnModel().getColumn(0).setHeaderValue("Type");
            errorTable.getColumnModel().getColumn(1).setHeaderValue("Name");
        }

        // Pin the "Type" column to exactly the rendered width of the word "Type"
        int typeColWidth = errorTable.getFontMetrics(errorTable.getFont())
                .stringWidth("Status Code") + 16;   // +16 for standard cell padding
        errorTable.getColumnModel().getColumn(0).setMinWidth(typeColWidth);
        errorTable.getColumnModel().getColumn(0).setMaxWidth(typeColWidth);
        errorTable.getColumnModel().getColumn(0).setPreferredWidth(typeColWidth);

        if (errorTable.getColumnModel().getColumnCount() > 1) {
            int nameColWidth = errorTable.getFontMetrics(errorTable.getFont())
                    .stringWidth("condition-123") + 16;
            errorTable.getColumnModel().getColumn(1).setPreferredWidth(nameColWidth);
        }

        // Remove the "Description" column (view index 3) — data model stays intact
        if (errorTable.getColumnModel().getColumnCount() > 3) {
            errorTable.getColumnModel().removeColumn(
                    errorTable.getColumnModel().getColumn(3));
        }

        JScrollPane scroll = new JScrollPane(errorTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // Match the height of the trigger form on the left so the row looks balanced
        int height = preferredHeight > 0 ? preferredHeight : 140;
        scroll.setPreferredSize(new Dimension(500, height));
        this.callbacks.customizeUiComponent(scroll);
        return scroll;
    }

    // =========================================================================
    // Request / Response pane
    // =========================================================================
    public Component prepareRequestResponsePanel() {
        ErrorRequestResponse rr = new ErrorRequestResponse();
        ireqMessageEditor = callbacks.createMessageEditor(rr, true);
        iresMessageEditor = callbacks.createMessageEditor(rr, true);

        JPanel left  = buildEditorPanel("Request",  ireqMessageEditor);
        JPanel right = buildEditorPanel("Response", iresMessageEditor);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.5);
        split.setPreferredSize(new Dimension(900, 380));
        callbacks.customizeUiComponent(split);
        return split;
    }

    private JPanel buildEditorPanel(String title, IMessageEditor editor) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel lbl = new JLabel(title);
        lbl.setForeground(BURP_ORANGE); lbl.setFont(new Font("Dialog", Font.BOLD, 13));
        callbacks.customizeUiComponent(lbl);
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(editor.getComponent(), BorderLayout.CENTER);
        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Logic helpers
    // =========================================================================
    public String generateDescription(String selectedCategory, String value) {
        if (selectedCategory.equals("Status Code"))
            return "ATOR will get trigger if " + selectedCategory + " as " + value + " in network flows";
        else
            return "ATOR will get trigger if " + selectedCategory + " contains " + value + " in network flows";
    }

    // =========================================================================
    // Separator helpers
    // =========================================================================
    private JPanel makeSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        callbacks.customizeUiComponent(sep);
        JPanel band = new JPanel(new BorderLayout());
        band.setBorder(new EmptyBorder(0, 16, 0, 16));
        band.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        band.setPreferredSize(new Dimension(100, 2));
        band.setMinimumSize(new Dimension(0, 2));
        band.add(sep, BorderLayout.CENTER);
        callbacks.customizeUiComponent(band);
        return band;
    }

    private Component makeVerticalSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 200));
        callbacks.customizeUiComponent(sep);
        return sep;
    }

    public JPanel getSeperatorPanel() { return makeSeparator(); }
}