package burp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import org.apache.commons.lang3.SerializationUtils;

public class ReplacePanel {
    IBurpExtenderCallbacks callbacks;
    BurpExtender extender;

    private static Color BURP_ORANGE  = new Color(255, 128, 0);
    private static Color BURP_ORANGE2 = new Color(220, 100, 0);
    private static Color SECTION_BG   = new Color(45, 45, 45);   // subtle card tint

    private Font headerFont    = new Font("Dialog", Font.BOLD, 15);
    private Font subHeaderFont = new Font("Dialog", Font.BOLD, 13);
    private Font labelFont     = new Font("Dialog", Font.PLAIN, 12);
    private Font buttonFont    = new Font("Dialog", Font.BOLD, 12);
    private Font reqresFont    = new Font("Dialog", Font.BOLD, 13);

    public static IMessageEditor ireqMessageEditor, iresMessageEditor;
    public static JTextField extractionNameStringField, startStringField,
                             stopStringField, extractedStringField, headerField;

    // "ADD" button for extra conditions — instance so the checkbox can toggle it
    private JButton addMultipleCondition = new JButton("+ Add Condition");

    public static ReplaceTable      replaceTable;
    public static ReplaceTableModel replaceTableModel;
    public static JButton extCreateButton;
    public static ArrayList<ReplaceEntry> replaceEntrylist = new ArrayList<ReplaceEntry>();

    public static JCheckBox              enableMultipleCondition        = new JCheckBox();
    public static JComboBox<String>      extractionreplaceComboNameList = new JComboBox<String>();
    public static ArrayList<MultipleErrorCondition> multipleErrorConditions = new ArrayList<MultipleErrorCondition>();
    public static JComboBox<String>      triggerConditionNameCombo      = new JComboBox<String>();

    // Static panel that holds condition rows.  Lives for the lifetime of the
    // extension.  clearCurrentState() calls removeAll() on it, so we expose a
    // static rebuild helper that puts the primary row back afterwards.
    public static JPanel secondscrollPanel = new JPanel();

    // The primary trigger row (combo + enable-multiple controls).  Kept static
    // so rebuildPrimaryRow() can re-add it after clearCurrentState().
    private static JPanel primaryTriggerRow = null;

    public static String replacementFlag;

    // Back-reference so the static rebuildPrimaryRow() can call instance methods
    private static ReplacePanel instance = null;

    public ReplacePanel(IBurpExtenderCallbacks callbacks, BurpExtender extender) {
        this.callbacks = callbacks;
        this.extender  = extender;
        instance = this;
    }

    // =========================================================================
    // Called by UsersTab.clearCurrentState() after it wipes secondscrollPanel.
    // Puts the primary trigger row back so the section is never blank.
    // =========================================================================
    public static void rebuildPrimaryTriggerRow() {
        if (instance == null) return;
        secondscrollPanel.removeAll();   // belt-and-braces: make sure it's clear
        if (primaryTriggerRow == null) return;
        secondscrollPanel.add(primaryTriggerRow);
        secondscrollPanel.revalidate();
        secondscrollPanel.repaint();
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

        // Compact top row: trigger | replacement form | replace table
        gc.gridy = 2;
        root.add(buildTopRow(), gc);

        gc.gridy = 3;
        root.add(makeSeparator(), gc);

        // Request/Response editors fill remaining height
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

        JLabel header = new JLabel("Spot Error Replacement");
        header.setForeground(BURP_ORANGE); header.setFont(headerFont);
        callbacks.customizeUiComponent(header);

        JLabel step1 = new JLabel("1. Replace the selected portion in request with the appropriate extraction name");
        step1.setFont(labelFont); callbacks.customizeUiComponent(step1);

        JLabel step2 = new JLabel("2. ATOR uses the regex pattern from step 1 and replaces extracted token for all incoming requests");
        step2.setFont(labelFont); callbacks.customizeUiComponent(step2);

        gc.gridy = 0; gc.insets = new Insets(0, 0, 4, 0); panel.add(header, gc);
        gc.gridy = 1; gc.insets = new Insets(0, 0, 2, 0); panel.add(step1,  gc);
        gc.gridy = 2; gc.insets = new Insets(0, 0, 0, 0); panel.add(step2,  gc);
        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Top row: Trigger | Replacement form | Replace table
    // =========================================================================
    private Component buildTopRow() {
        JPanel triggerCard     = buildSectionCard("Mark Trigger Condition", buildTriggerScrollable());
        JPanel replacementCard = buildSectionCard("Replacement Entry",      buildReplacementForm());
        JPanel tableCard       = buildSectionCard("Replace List",           generateTablePanel());

        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, triggerCard, replacementCard);
        leftSplit.setResizeWeight(0.4);
        callbacks.customizeUiComponent(leftSplit);

        JSplitPane outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, tableCard);
        outerSplit.setResizeWeight(0.5);
        outerSplit.setPreferredSize(new Dimension(1000, 260));
        callbacks.customizeUiComponent(outerSplit);
        return outerSplit;
    }

    private JPanel buildSectionCard(String title, Component content) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(6, 10, 6, 10));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0; gc.anchor = GridBagConstraints.NORTHWEST;
        JLabel lbl = new JLabel(title);
        lbl.setForeground(BURP_ORANGE); lbl.setFont(subHeaderFont);
        callbacks.customizeUiComponent(lbl);
        gc.gridy = 0; gc.insets = new Insets(0, 0, 6, 0); gc.weighty = 0; panel.add(lbl, gc);
        gc.gridy = 1; gc.insets = new Insets(0, 0, 0, 0); gc.fill = GridBagConstraints.BOTH; gc.weighty = 1.0;
        panel.add(content, gc);
        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Trigger condition panel — themed, non-truncating, survives mode switches
    //
    // Layout (all inside secondscrollPanel / BoxLayout Y_AXIS):
    //
    //   ┌─ primaryTriggerRow (always present, rebuilt after clearCurrentState) ─┐
    //   │  Trigger Condition:  [combo ▾]                                        │
    //   │  [ ] Enable multiple    [+ Add Condition]                             │
    //   └───────────────────────────────────────────────────────────────────────┘
    //   ┌─ extra row (appended per click, closed with × button) ────────────────┐
    //   │  [AND ▾]  Other Condition: [combo ▾]  [×]                             │
    //   └───────────────────────────────────────────────────────────────────────┘
    // =========================================================================
    private JScrollPane buildTriggerScrollable() {
        secondscrollPanel.setLayout(new BoxLayout(secondscrollPanel, BoxLayout.Y_AXIS));
        secondscrollPanel.setBorder(new EmptyBorder(4, 0, 4, 0));
        callbacks.customizeUiComponent(secondscrollPanel);

        // ── Primary row ────────────────────────────────────────────────────────
        primaryTriggerRow = buildPrimaryTriggerRow();
        secondscrollPanel.add(primaryTriggerRow);

        // ── Wrapper & scroll ──────────────────────────────────────────────────
        JPanel borderPanel = new JPanel(new BorderLayout());
        borderPanel.add(secondscrollPanel, BorderLayout.NORTH);
        callbacks.customizeUiComponent(borderPanel);

        JScrollPane scroll = new JScrollPane(borderPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setMinimumSize(new Dimension(300, 160));
        scroll.setPreferredSize(new Dimension(360, 200));
        return scroll;
    }

    // Builds (or re-builds) the always-present primary condition row.
    // Called once at construction and again from rebuildPrimaryTriggerRow().
    private JPanel buildPrimaryTriggerRow() {
        // GridBagLayout so the combo fills width and nothing clips
        JPanel row = new JPanel(new GridBagLayout());
        row.setBorder(new EmptyBorder(2, 4, 2, 4));
        callbacks.customizeUiComponent(row);

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(3, 4, 3, 4);

        // Label
        JLabel condLbl = new JLabel("Condition:");
        condLbl.setFont(labelFont);
        callbacks.customizeUiComponent(condLbl);
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        row.add(condLbl, gc);

        // Trigger name combo (fills remaining width)
        triggerConditionNameCombo.setFont(labelFont);
        triggerConditionNameCombo.addItemListener(new ConfigChangedListener(ConfigActions.A_ERROR_CONDITION_CHANGED));
        callbacks.customizeUiComponent(triggerConditionNameCombo);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1.0;
        row.add(triggerConditionNameCombo, gc);

        // Separator between primary condition and multiple-condition controls
        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2; gc.weightx = 1.0;
        gc.insets = new Insets(4, 0, 4, 0);
        JSeparator rowSep = new JSeparator(SwingConstants.HORIZONTAL);
        row.add(rowSep, gc);
        gc.gridwidth = 1;

        // "Enable Multiple" checkbox
        enableMultipleCondition.setFont(labelFont);
        JLabel enableLbl = new JLabel("Enable Multiple");
        enableLbl.setFont(labelFont);
        callbacks.customizeUiComponent(enableMultipleCondition);
        callbacks.customizeUiComponent(enableLbl);

        enableMultipleCondition.addActionListener(e ->
                addMultipleCondition.setEnabled(!addMultipleCondition.isEnabled()));

        JPanel checkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        checkRow.setOpaque(false);
        checkRow.add(enableMultipleCondition);
        checkRow.add(enableLbl);
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 1.0; gc.gridwidth = 2;
        gc.insets = new Insets(2, 4, 2, 4);
        row.add(checkRow, gc);

        // "+ Add Condition" button — styled consistently
        addMultipleCondition.setFont(buttonFont);
        addMultipleCondition.setForeground(Color.WHITE);
        addMultipleCondition.setBackground(BURP_ORANGE2);
        addMultipleCondition.setOpaque(true);
        addMultipleCondition.setBorderPainted(false);
        addMultipleCondition.setFocusPainted(false);
        addMultipleCondition.setEnabled(false);
        callbacks.customizeUiComponent(addMultipleCondition);

        // Remove existing listeners to avoid duplicates on rebuild
        for (ActionListener al : addMultipleCondition.getActionListeners())
            addMultipleCondition.removeActionListener(al);

        addMultipleCondition.addActionListener(e -> {
            JButton closeButton = new JButton("×");
            closeButton.setFont(buttonFont);
            closeButton.setForeground(Color.WHITE);
            closeButton.setBackground(new Color(160, 60, 60));
            closeButton.setOpaque(true);
            closeButton.setBorderPainted(false);
            closeButton.setFocusPainted(false);
            closeButton.setPreferredSize(new Dimension(30, 24));
            callbacks.customizeUiComponent(closeButton);

            MultipleErrorCondition multipleErrorCondition = new MultipleErrorCondition(callbacks);
            multipleErrorConditions.add(multipleErrorCondition);

            // Extra condition row: [AND▾]  Other: [combo▾]  [×]
            JPanel extraRow = new JPanel(new GridBagLayout());
            extraRow.setBorder(new EmptyBorder(2, 4, 2, 4));
            callbacks.customizeUiComponent(extraRow);

            GridBagConstraints er = new GridBagConstraints();
            er.gridy = 0; er.fill = GridBagConstraints.HORIZONTAL; er.anchor = GridBagConstraints.WEST;
            er.insets = new Insets(2, 4, 2, 4);

            // AND/OR combo
            JComboBox<String> logicalCondition = new JComboBox<String>();
            logicalCondition.addItem("AND"); logicalCondition.addItem("OR");
            logicalCondition.setFont(labelFont);
            BurpExtender.callbacks.customizeUiComponent(logicalCondition);
            logicalCondition.addItemListener(new ConfigChangedListener(ConfigActions.A_ERROR_CONDITION_CHANGED));
            multipleErrorCondition.logicalCondition = logicalCondition;
            er.gridx = 0; er.weightx = 0; extraRow.add(logicalCondition, er);

            // "Other:" label
            JLabel otherLbl = new JLabel("Other:");
            otherLbl.setFont(labelFont);
            BurpExtender.callbacks.customizeUiComponent(otherLbl);
            er.gridx = 1; er.weightx = 0; extraRow.add(otherLbl, er);

            // Cloned trigger combo
            JComboBox<String> innerCombo = SerializationUtils.clone(triggerConditionNameCombo);
            multipleErrorCondition.triggerComboBox = innerCombo;
            innerCombo.setFont(labelFont);
            innerCombo.addItemListener(new ConfigChangedListener(ConfigActions.A_ERROR_CONDITION_CHANGED));
            BurpExtender.callbacks.customizeUiComponent(innerCombo);
            er.gridx = 2; er.weightx = 1.0; extraRow.add(innerCombo, er);

            // Close button
            er.gridx = 3; er.weightx = 0; er.fill = GridBagConstraints.NONE;
            extraRow.add(closeButton, er);

            closeButton.addActionListener(ce -> {
                secondscrollPanel.remove(extraRow);
                multipleErrorConditions.remove(multipleErrorCondition);
                PreviewPanel.conditionDetails.setText(FinalErrorCondition.addErrorCondition());
                secondscrollPanel.revalidate();
                secondscrollPanel.repaint();
            });

            secondscrollPanel.add(extraRow);
            PreviewPanel.conditionDetails.setText(FinalErrorCondition.addErrorCondition());
            secondscrollPanel.revalidate();
            secondscrollPanel.repaint();
        });

        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2; gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.WEST; gc.insets = new Insets(4, 4, 4, 4);
        row.add(addMultipleCondition, gc);

        return row;
    }

    // =========================================================================
    // Legacy static helpers kept so external callers (FinalErrorCondition etc.)
    // still compile.  They now delegate to the instance-based approach above.
    // =========================================================================
    public static JPanel addlogicalCondition(MultipleErrorCondition multipleErrorCondition) {
        // No longer used for UI building — kept for any reflective callers
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBorder(new EmptyBorder(0, 0, 5, 15));
        BurpExtender.callbacks.customizeUiComponent(p);
        JComboBox<String> lc = new JComboBox<String>();
        lc.addItem("AND"); lc.addItem("OR");
        BurpExtender.callbacks.customizeUiComponent(lc);
        lc.addItemListener(new ConfigChangedListener(ConfigActions.A_ERROR_CONDITION_CHANGED));
        multipleErrorCondition.logicalCondition = lc;
        p.add(lc);
        return p;
    }

    public JPanel triggerConditionNamePanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        callbacks.customizeUiComponent(p);
        JLabel lbl = new JLabel("Trigger Condition Name : ");
        callbacks.customizeUiComponent(lbl);
        p.add(lbl);
        triggerConditionNameCombo.setPreferredSize(new Dimension(150, 26));
        triggerConditionNameCombo.addItemListener(new ConfigChangedListener(ConfigActions.A_ERROR_CONDITION_CHANGED));
        p.add(triggerConditionNameCombo);
        return p;
    }

    public static JPanel triggerConditionNameInnerPanel(MultipleErrorCondition multipleErrorCondition) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        BurpExtender.callbacks.customizeUiComponent(p);
        JLabel lbl = new JLabel("Other Trigger Condition : ");
        BurpExtender.callbacks.customizeUiComponent(lbl);
        p.add(lbl);
        JComboBox<String> innerCombo = SerializationUtils.clone(triggerConditionNameCombo);
        multipleErrorCondition.triggerComboBox = innerCombo;
        innerCombo.setPreferredSize(new Dimension(150, 26));
        innerCombo.addItemListener(new ConfigChangedListener(ConfigActions.A_ERROR_CONDITION_CHANGED));
        BurpExtender.callbacks.customizeUiComponent(innerCombo);
        p.add(innerCombo);
        return p;
    }

    public JPanel triggermultipleCondition() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        callbacks.customizeUiComponent(p);
        callbacks.customizeUiComponent(enableMultipleCondition);
        callbacks.customizeUiComponent(addMultipleCondition);
        addMultipleCondition.setEnabled(false);
        JLabel lbl = new JLabel("Enable Multiple condition");
        callbacks.customizeUiComponent(lbl);
        p.add(enableMultipleCondition); p.add(lbl); p.add(addMultipleCondition);
        return p;
    }

    // =========================================================================
    // Replacement entry form
    // =========================================================================
    private JPanel buildReplacementForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;

        extractionNameStringField = new JTextField(); extractionNameStringField.setPreferredSize(new Dimension(160, 26));
        startStringField          = new JTextField(); startStringField.setPreferredSize(new Dimension(160, 26));
        stopStringField           = new JTextField(); stopStringField.setPreferredSize(new Dimension(160, 26));
        extractedStringField      = new JTextField(); extractedStringField.setEditable(false); extractedStringField.setPreferredSize(new Dimension(160, 26));
        headerField               = new JTextField(); headerField.setPreferredSize(new Dimension(160, 26));

        extractionreplaceComboNameList.setPreferredSize(new Dimension(160, 26));
        extractionreplaceComboNameList.addItem("NA");

        startStringField.getDocument().addDocumentListener(new ConfigChangedListener(ConfigActions.A_EXT_ERROR_CONFIG_CHANGED));
        stopStringField.getDocument().addDocumentListener(new ConfigChangedListener(ConfigActions.A_EXT_ERROR_CONFIG_CHANGED));
        extractionNameStringField.getDocument().addDocumentListener(new ConfigChangedListener(ConfigActions.A_EXT_ERROR_VALIDITY));
        headerField.getDocument().addDocumentListener(new ConfigChangedListener(ConfigActions.A_EXT_ERROR_CONFIG_CHANGED));
        extractionreplaceComboNameList.addItemListener(new ConfigChangedListener(ConfigActions.A_ERROR_CONDITION_CHANGED));
        extractionreplaceComboNameList.addItemListener(new ConfigChangedListener(ConfigActions.A_EXT_COMBO_CHNAGED_ON_SPOTERROR));

        extCreateButton = new JButton("Add");
        extCreateButton.setFont(buttonFont); extCreateButton.setForeground(Color.WHITE);
        extCreateButton.setBackground(BURP_ORANGE2); extCreateButton.setOpaque(true);
        extCreateButton.setBorderPainted(false); extCreateButton.setFocusPainted(false);
        extCreateButton.addActionListener(new MenuAllListener(callbacks, MenuActions.ADD_EXTRACTION_FOR_REP_ITEM, this));
        extCreateButton.setEnabled(false);

        JButton extFromSelectionButton = new JButton("From selection");
        extFromSelectionButton.setFont(buttonFont);
        extFromSelectionButton.addActionListener(new MenuAllListener(callbacks, MenuActions.FROM_SELECTION_EXTRACTION_FOR_REP, this));

        addFormRow(form, gc, 0, "Name:",              extractionNameStringField);
        addFormRow(form, gc, 1, "Start string:",      startStringField);
        addFormRow(form, gc, 2, "Stop string:",       stopStringField);
        addFormRow(form, gc, 3, "Replace position:",  extractedStringField);
        addFormRow(form, gc, 4, "Extraction name:",   extractionreplaceComboNameList);
        addFormRow(form, gc, 5, "Header:",            headerField);
        addButtonRow(form, gc, 6, extCreateButton, extFromSelectionButton);

        callbacks.customizeUiComponent(form);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.PAGE_START);
        callbacks.customizeUiComponent(wrapper);
        return wrapper;
    }

    public JPanel getStartEndStringPanel() { return buildSectionCard("Replacement Entry", buildReplacementForm()); }

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
        split.setPreferredSize(new Dimension(900, 400));
        callbacks.customizeUiComponent(split);
        return split;
    }

    private JPanel buildEditorPanel(String title, IMessageEditor editor) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel lbl = new JLabel(title); lbl.setForeground(BURP_ORANGE); lbl.setFont(reqresFont);
        callbacks.customizeUiComponent(lbl);
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(editor.getComponent(), BorderLayout.CENTER);
        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Table generator — logic unchanged
    // =========================================================================
    public JScrollPane generateTablePanel() {
        JPopupMenu extPopupMenu = new JPopupMenu();
        extPopupMenu.add("Delete").addActionListener(e -> {
            int selectedrow = replaceTable.getSelectedRow();
            String replaceName = (String) replaceTable.getModel().getValueAt(selectedrow, 0);
            for (int index = 0; index < replaceEntrylist.size(); index++) {
                ReplaceEntry replaceEntry = replaceEntrylist.get(index);
                if (replaceEntry.getName().equals(replaceName)) {
                    replaceEntrylist.remove(index);
                    replaceTableModel.fireTableDataChanged();
                    break;
                }
            }
        });
        callbacks.customizeUiComponent(extPopupMenu);
        replaceTableModel = new ReplaceTableModel(replaceEntrylist);
        replaceTable = new ReplaceTable(replaceTableModel, callbacks);
        replaceTable.setComponentPopupMenu(extPopupMenu);
        replaceTable.setModel(replaceTableModel);
        this.callbacks.customizeUiComponent(replaceTable);
        JScrollPane scroll = new JScrollPane(replaceTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setPreferredSize(new Dimension(400, 200));
        this.callbacks.customizeUiComponent(scroll);
        return scroll;
    }

    // =========================================================================
    // Form layout helpers
    // =========================================================================
    private void addFormRow(JPanel form, GridBagConstraints gc, int row, String labelText, Component field) {
        gc.gridy = row;
        gc.gridx = 0; gc.weightx = 0.4; gc.insets = new Insets(2, 0, 2, 6);
        JLabel lbl = new JLabel(labelText); lbl.setFont(labelFont); callbacks.customizeUiComponent(lbl); form.add(lbl, gc);
        gc.gridx = 1; gc.weightx = 0.6; gc.insets = new Insets(2, 0, 2, 0); form.add(field, gc);
    }

    private void addButtonRow(JPanel form, GridBagConstraints gc, int row, JButton primary, JButton secondary) {
        gc.gridy = row;
        gc.gridx = 0; gc.weightx = 0.5; gc.insets = new Insets(6, 0, 2, 4); form.add(primary, gc);
        gc.gridx = 1; gc.weightx = 0.5; gc.insets = new Insets(6, 0, 2, 0); form.add(secondary, gc);
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
    public JSeparator getSeperatorVerticalPanel() {
        JSeparator sep = new JSeparator(); sep.setOrientation(SwingConstants.VERTICAL);
        callbacks.customizeUiComponent(sep); return sep;
    }
}