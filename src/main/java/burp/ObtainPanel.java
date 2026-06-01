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
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class ObtainPanel {
    IBurpExtenderCallbacks callbacks;
    BurpExtender extender;

    static Color BURP_ORANGE  = new Color(255, 128, 0);
    static Color BURP_ORANGE2 = new Color(220, 100, 0);

    private Font headerFont      = new Font("Dialog", Font.BOLD, 15);
    private Font subHeaderFont   = new Font("Dialog", Font.BOLD, 13);
    private Font labelFont       = new Font("Dialog", Font.PLAIN, 12);
    private Font buttonFont      = new Font("Dialog", Font.BOLD, 12);
    private Font reqresFont      = new Font("Dialog", Font.BOLD, 13);

    public static IMessageEditor ireqMessageEditor, iresMessageEditor;
    public static JTextField startStringField, stopStringField, extractedStringField;
    public static JTextField extractionNameStringField;
    public static JButton extCreateButton;
    JPopupMenu extPopupMenu = new JPopupMenu();

    public static JTextField repstartStringField, repstopStringField, repextractedStringField;
    public static JComboBox<String> extractionListComboBox = new JComboBox<String>();
    public static JComboBox<String> urldecodeComboBox      = new JComboBox<String>();
    public static JTextField replacementNameStringField;
    public static JButton repCreateButton;

    public static ArrayList<ObtainEntry>      obtainEntrylist      = new ArrayList<ObtainEntry>();
    public static ArrayList<ExtractionEntry>  extractionEntrylist  = new ArrayList<ExtractionEntry>();
    public static ArrayList<ReplacementEntry> replacementEntrylist = new ArrayList<ReplacementEntry>();
    public static ObtainTable      obtainTable;
    public static ObtainTableModel obtainTableModel;
    public static ExtractionTable      extractionTable;
    public static ExtractionTableModel extractionTableModel;
    public static ReplacementTable      replacementTable;
    public static ReplacementTableModel replacementTableModel;

    public ObtainPanel(IBurpExtenderCallbacks callbacks, BurpExtender extender) {
        this.callbacks = callbacks;
        this.extender  = extender;
    }

    // =========================================================================
    // Root panel
    //
    //  ┌─────────────────────────────────────────────────────────────────────┐
    //  │  Header / description                                               │ fixed
    //  ├─────────────────────────────────────────────────────────────────────┤
    //  │  ATOR Macro table  │ sep │ Extraction form+list │ sep │ Replacement  │ fixed
    //  ├─────────────────────────────────────────────────────────────────────┤
    //  │  Request / Response  (fills remaining height)                       │ grows
    //  └─────────────────────────────────────────────────────────────────────┘
    // =========================================================================
    public JPanel preparePanel() {
        JPanel root = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx   = 0;
        gc.weightx = 1.0;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.insets  = new Insets(0, 0, 0, 0);

        gc.gridy = 0; gc.weighty = 0;
        root.add(buildHeaderPanel(), gc);

        gc.gridy = 1;
        root.add(makeSeparator(), gc);

        // Single compact row: macro table | extraction | replacement
        gc.gridy = 2; gc.fill = GridBagConstraints.HORIZONTAL; gc.weighty = 0;
        root.add(buildTopRow(), gc);

        gc.gridy = 3;
        root.add(makeSeparator(), gc);

        // Request / Response (grows)
        gc.gridy   = 4;
        gc.fill    = GridBagConstraints.BOTH;
        gc.weighty = 1.0;
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

        JLabel header = new JLabel("Obtain Token");
        header.setForeground(BURP_ORANGE);
        header.setFont(headerFont);
        callbacks.customizeUiComponent(header);

        JLabel step1 = new JLabel("1. Extraction: Select the value from response and add it to the extraction list");
        step1.setFont(labelFont);
        callbacks.customizeUiComponent(step1);

        JLabel step2 = new JLabel("2. Replacement (chain of requests only): Select the place from request, "
                + "pick the extraction name and add it to the replacement list");
        step2.setFont(labelFont);
        callbacks.customizeUiComponent(step2);

        gc.gridy = 0; gc.insets = new Insets(0, 0, 4, 0); panel.add(header, gc);
        gc.gridy = 1; gc.insets = new Insets(0, 0, 2, 0); panel.add(step1,  gc);
        gc.gridy = 2; gc.insets = new Insets(0, 0, 0, 0); panel.add(step2,  gc);

        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Top row: ATOR Macro | Extraction form+table | Replacement form+table
    // Uses nested JSplitPanes so the user can resize any section.
    // =========================================================================
    private Component buildTopRow() {
        // ── Extraction sub-split: form | list ─────────────────────────────────
        JSplitPane extractionSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildSectionCard("Extraction",      getExtractionStartEndStringPanel()),
                buildSectionCard("Extraction List", generateExtractionTablePanel()));
        extractionSplit.setResizeWeight(0.5);
        callbacks.customizeUiComponent(extractionSplit);

        // ── Replacement sub-split: form | list ────────────────────────────────
        JSplitPane replacementSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildSectionCard("Replacement",      getReplacementStartEndStringPanel()),
                buildSectionCard("Replacement List", generateReplacementTablePanel()));
        replacementSplit.setResizeWeight(0.5);
        callbacks.customizeUiComponent(replacementSplit);

        // ── Middle pair: extraction | replacement ──────────────────────────────
        JSplitPane middleSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, extractionSplit, replacementSplit);
        middleSplit.setResizeWeight(0.5);
        callbacks.customizeUiComponent(middleSplit);

        // ── Outer: ATOR Macro table | (extraction + replacement) ──────────────
        JPanel macroCard = buildSectionCard("ATOR Macro", generateTablePanel());
        JSplitPane outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, macroCard, middleSplit);
        macroCard.setPreferredSize(new Dimension(440, 220));
        middleSplit.setPreferredSize(new Dimension(760, 220));
        outerSplit.setResizeWeight(0.37);
        outerSplit.setDividerLocation(0.37);
        outerSplit.setPreferredSize(new Dimension(1200, 220));
        callbacks.customizeUiComponent(outerSplit);
        return outerSplit;
    }

    /** Wraps any component (form or scroll) with an orange section header. */
    private JPanel buildSectionCard(String title, Component content) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(6, 10, 6, 10));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0; gc.anchor = GridBagConstraints.NORTHWEST;

        JLabel lbl = new JLabel(title);
        lbl.setForeground(BURP_ORANGE);
        lbl.setFont(subHeaderFont);
        callbacks.customizeUiComponent(lbl);

        gc.gridy = 0; gc.insets = new Insets(0, 0, 4, 0); gc.weighty = 0;
        panel.add(lbl, gc);

        gc.gridy  = 1; gc.insets = new Insets(0, 0, 0, 0);
        gc.fill   = GridBagConstraints.BOTH; gc.weighty = 1.0;
        panel.add(content, gc);

        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Request / Response pane
    // =========================================================================
    public Component prepareRequestResponsePanel() {
        ErrorRequestResponse rr = new ErrorRequestResponse();
        ireqMessageEditor = callbacks.createMessageEditor(rr, true);
        iresMessageEditor = callbacks.createMessageEditor(rr, true);

        JPanel leftPanel  = buildEditorPanel("Request",  ireqMessageEditor);
        JPanel rightPanel = buildEditorPanel("Response", iresMessageEditor);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setPreferredSize(new Dimension(900, 300));
        callbacks.customizeUiComponent(splitPane);
        return splitPane;
    }

    private JPanel buildEditorPanel(String title, IMessageEditor editor) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel lbl = new JLabel(title);
        lbl.setForeground(BURP_ORANGE);
        lbl.setFont(reqresFont);
        callbacks.customizeUiComponent(lbl);
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(editor.getComponent(), BorderLayout.CENTER);
        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Extraction form  (logic unchanged)
    // =========================================================================
    public JPanel getExtractionStartEndStringPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0; gc.insets = new Insets(2, 0, 2, 0);

        extractionNameStringField = new JTextField();
        extractionNameStringField.setPreferredSize(new Dimension(110, 26));
        startStringField = new JTextField();
        startStringField.setPreferredSize(new Dimension(110, 26));
        stopStringField = new JTextField();
        stopStringField.setPreferredSize(new Dimension(110, 26));
        extractedStringField = new JTextField();
        extractedStringField.setPreferredSize(new Dimension(110, 26));
        extractedStringField.setEditable(false);

        startStringField.getDocument().addDocumentListener(new ConfigChangedListener(ConfigActions.A_EXT_CONFIG_CHANGED));
        stopStringField.getDocument().addDocumentListener(new ConfigChangedListener(ConfigActions.A_EXT_CONFIG_CHANGED));
        extractionNameStringField.getDocument().addDocumentListener(new ConfigChangedListener(ConfigActions.A_EXT_VALIDITY));

        urldecodeComboBox.setPreferredSize(new Dimension(110, 26));
        urldecodeComboBox.addItem("NA");
        urldecodeComboBox.addItem("Encode");
        urldecodeComboBox.addItem("Decode");

        extCreateButton = new JButton("Add");
        extCreateButton.setFont(buttonFont);
        extCreateButton.setForeground(Color.WHITE);
        extCreateButton.setBackground(BURP_ORANGE2);
        extCreateButton.setOpaque(true);
        extCreateButton.setBorderPainted(false);
        extCreateButton.setFocusPainted(false);
        extCreateButton.addActionListener(new MenuAllListener(callbacks, MenuActions.ADD_ITEM, this));
        extCreateButton.setEnabled(false);

        JButton extFromSelectionButton = new JButton("From selection");
        extFromSelectionButton.setFont(buttonFont);
        extFromSelectionButton.addActionListener(new MenuAllListener(callbacks, MenuActions.FROM_SELECTION, this));

        addFormRow(form, gc, 0, "Name:",                        extractionNameStringField);
        addFormRow(form, gc, 1, "Start string:",                startStringField);
        addFormRow(form, gc, 2, "Stop string:",                 stopStringField);
        addFormRow(form, gc, 3, "Selected from response:",      extractedStringField);
        addFormRow(form, gc, 4, "URL encode/decode:",           urldecodeComboBox);
        addButtonRow(form, gc, 5, extCreateButton, extFromSelectionButton);

        callbacks.customizeUiComponent(form);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.PAGE_START);
        callbacks.customizeUiComponent(wrapper);
        return wrapper;
    }

    // =========================================================================
    // Replacement form  (logic unchanged)
    // =========================================================================
    public JPanel getReplacementStartEndStringPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0; gc.insets = new Insets(2, 0, 2, 0);

        replacementNameStringField = new JTextField();
        replacementNameStringField.setPreferredSize(new Dimension(110, 26));
        repstartStringField = new JTextField();
        repstartStringField.setPreferredSize(new Dimension(110, 26));
        repstopStringField = new JTextField();
        repstopStringField.setPreferredSize(new Dimension(110, 26));
        repextractedStringField = new JTextField();
        repextractedStringField.setPreferredSize(new Dimension(110, 26));
        repextractedStringField.setEditable(false);

        repstartStringField.getDocument().addDocumentListener(new ConfigChangedListener(ConfigActions.A_REP_CONFIG_CHANGED));
        repstopStringField.getDocument().addDocumentListener(new ConfigChangedListener(ConfigActions.A_REP_CONFIG_CHANGED));
        replacementNameStringField.getDocument().addDocumentListener(new ConfigChangedListener(ConfigActions.A_REP_VALIDITY));

        extractionListComboBox.setPreferredSize(new Dimension(110, 26));
        extractionListComboBox.addItem("");
        extractionListComboBox.addItem("NA");
        extractionListComboBox.addItemListener(new ConfigChangedListener(ConfigActions.A_EXT_COMBO_CONFIG_CHANGED));

        repCreateButton = new JButton("Add");
        repCreateButton.setFont(buttonFont);
        repCreateButton.setForeground(Color.WHITE);
        repCreateButton.setBackground(BURP_ORANGE2);
        repCreateButton.setOpaque(true);
        repCreateButton.setBorderPainted(false);
        repCreateButton.setFocusPainted(false);
        repCreateButton.addActionListener(new MenuAllListener(callbacks, MenuActions.ADD_REP_ITEM, this));
        repCreateButton.setEnabled(false);

        JButton repFromSelectionButton = new JButton("From selection");
        repFromSelectionButton.setFont(buttonFont);
        repFromSelectionButton.addActionListener(new MenuAllListener(callbacks, MenuActions.REP_FROM_SELECTION, this));

        addFormRow(form, gc, 0, "Name:",                        replacementNameStringField);
        addFormRow(form, gc, 1, "Start string:",                repstartStringField);
        addFormRow(form, gc, 2, "Stop string:",                 repstopStringField);
        addFormRow(form, gc, 3, "Selected from request:",       repextractedStringField);
        addFormRow(form, gc, 4, "Pick from extraction:",        extractionListComboBox);
        addButtonRow(form, gc, 5, repCreateButton, repFromSelectionButton);

        callbacks.customizeUiComponent(form);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.PAGE_START);
        callbacks.customizeUiComponent(wrapper);
        return wrapper;
    }

    // =========================================================================
    // Form helpers
    // =========================================================================
    private void addFormRow(JPanel form, GridBagConstraints gc, int row, String labelText, Component field) {
        gc.gridy = row;
        gc.gridx = 0; gc.weightx = 0.4; gc.insets = new Insets(2, 0, 2, 6);
        JLabel lbl = new JLabel(labelText); lbl.setFont(labelFont); callbacks.customizeUiComponent(lbl); form.add(lbl, gc);
        gc.gridx = 1; gc.weightx = 0.6; gc.insets = new Insets(2, 0, 2, 0); form.add(field, gc);
    }

    // Buttons sit next to each other so the form stays compact.
    private void addButtonRow(JPanel form, GridBagConstraints gc, int row, JButton primary, JButton secondary) {
        gc.gridy = row;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.5;
        gc.gridwidth = 1;

        gc.gridx = 0;
        gc.insets = new Insets(6, 0, 2, 4);
        form.add(primary, gc);

        gc.gridx = 1;
        gc.insets = new Insets(6, 4, 2, 0);
        form.add(secondary, gc);

        gc.weightx = 1.0;
    }

    // =========================================================================
    // Table generators  (logic unchanged)
    // =========================================================================
    public JScrollPane generateTablePanel() {
        JPopupMenu obtainPopupMenu = new JPopupMenu();
        obtainPopupMenu.add("Delete").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedrow = obtainTable.getSelectedRow();
                String obtainmsgID = (String) obtainTable.getModel().getValueAt(selectedrow, 0);
                for (int index = 0; index < obtainEntrylist.size(); index++) {
                    ObtainEntry obtainEntry = obtainEntrylist.get(index);
                    if (obtainEntry.getMsgID().equals(obtainmsgID)) {
                        obtainEntrylist.remove(index);
                        int extractioncount = 0;
                        for (ExtractionEntry extractionEntry : ObtainPanel.extractionEntrylist) {
                            if (extractionEntry.getextractionmsgID().equals(obtainmsgID)) extractioncount++;
                        }
                        for (int i = 0; i < extractioncount; i++) {
                            for (ExtractionEntry extractionEntry : ObtainPanel.extractionEntrylist) {
                                if (extractionEntry.getextractionmsgID().equals(obtainmsgID)) {
                                    deleteEntryFromExtractionList(extractionEntry.getName()); break;
                                }
                            }
                        }
                        obtainTableModel.fireTableDataChanged(); break;
                    }
                }
                ireqMessageEditor.setMessage("".getBytes(), true);
                iresMessageEditor.setMessage("".getBytes(), true);
            }
        });
        callbacks.customizeUiComponent(obtainPopupMenu);
        obtainTableModel = new ObtainTableModel(obtainEntrylist);
        obtainTable = new ObtainTable(obtainTableModel, callbacks);
        obtainTable.setComponentPopupMenu(obtainPopupMenu);
        obtainTable.setModel(obtainTableModel);
        this.callbacks.customizeUiComponent(obtainTable);

        // Pin "MsgID" column (0) to the pixel-width of "MsgID"
        int msgIdWidth = obtainTable.getFontMetrics(obtainTable.getFont())
                .stringWidth("MsgID") + 16;
        obtainTable.getColumnModel().getColumn(0).setMinWidth(msgIdWidth);
        obtainTable.getColumnModel().getColumn(0).setMaxWidth(msgIdWidth);
        obtainTable.getColumnModel().getColumn(0).setPreferredWidth(msgIdWidth);

        // Pin "Method" column (1) to the width of "CONNECT" — the longest
        // standard HTTP method — so all methods always fit without waste.
        if (obtainTable.getColumnModel().getColumnCount() > 1) {
            int methodWidth = obtainTable.getFontMetrics(obtainTable.getFont())
                    .stringWidth("DELETE") + 16;
            obtainTable.getColumnModel().getColumn(1).setMinWidth(methodWidth);
            obtainTable.getColumnModel().getColumn(1).setMaxWidth(methodWidth);
            obtainTable.getColumnModel().getColumn(1).setPreferredWidth(methodWidth);
        }

        // Host column (2): keep it readable without forcing a fixed width.
        if (obtainTable.getColumnModel().getColumnCount() > 2) {
            int hostPreferred = obtainTable.getFontMetrics(obtainTable.getFont())
                .stringWidth("api.example.com") + 16;
            obtainTable.getColumnModel().getColumn(2).setPreferredWidth(hostPreferred);
        }

        JScrollPane scroll = new JScrollPane(obtainTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setPreferredSize(new Dimension(280, 130));
        this.callbacks.customizeUiComponent(scroll);
        return scroll;
    }

    public JScrollPane generateExtractionTablePanel() {
        extPopupMenu.add("Delete").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedrow = extractionTable.getSelectedRow();
                String extractionName = (String) extractionTable.getModel().getValueAt(selectedrow, 0);
                deleteEntryFromExtractionList(extractionName);
            }
        });
        callbacks.customizeUiComponent(extPopupMenu);
        extractionTableModel = new ExtractionTableModel(extractionEntrylist);
        extractionTable = new ExtractionTable(extractionTableModel, callbacks);
        extractionTable.setComponentPopupMenu(extPopupMenu);
        extractionTable.setModel(extractionTableModel);
        this.callbacks.customizeUiComponent(extractionTable);

        // Rename "ExtractionMsgID" → "MsgID" and pin to that width
        if (extractionTable.getColumnModel().getColumnCount() > 1) {
            extractionTable.getColumnModel().getColumn(1).setHeaderValue("MsgID");
            int msgIdWidth = extractionTable.getFontMetrics(extractionTable.getFont())
                    .stringWidth("MsgID") + 16;
            extractionTable.getColumnModel().getColumn(1).setMinWidth(msgIdWidth);
            extractionTable.getColumnModel().getColumn(1).setMaxWidth(msgIdWidth);
            extractionTable.getColumnModel().getColumn(1).setPreferredWidth(msgIdWidth);
        }

        JScrollPane scroll = new JScrollPane(extractionTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setPreferredSize(new Dimension(160, 130));
        this.callbacks.customizeUiComponent(scroll);
        return scroll;
    }

    public JScrollPane generateReplacementTablePanel() {
        JPopupMenu repPopupMenu = new JPopupMenu();
        repPopupMenu.add("Delete").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedrow = replacementTable.getSelectedRow();
                String replacementName = (String) replacementTable.getModel().getValueAt(selectedrow, 0);
                for (int index = 0; index < replacementEntrylist.size(); index++) {
                    ReplacementEntry replacementEntry = replacementEntrylist.get(index);
                    if (replacementEntry.getName().equals(replacementName)) {
                        replacementEntrylist.remove(index);
                        for (ObtainEntry obtainEntry : ObtainPanel.obtainEntrylist) {
                            if (obtainEntry.getMsgID().equals(replacementEntry.replacementMsgID)) {
                                obtainEntry.replacementlistNames.remove(replacementEntry); break;
                            }
                        }
                        AddEntryToReplacementList.clearAll();
                        replacementTableModel.fireTableDataChanged(); break;
                    }
                }
            }
        });
        callbacks.customizeUiComponent(repPopupMenu);
        replacementTableModel = new ReplacementTableModel(replacementEntrylist);
        replacementTable = new ReplacementTable(replacementTableModel, callbacks);
        replacementTable.setComponentPopupMenu(repPopupMenu);
        replacementTable.setModel(replacementTableModel);
        this.callbacks.customizeUiComponent(replacementTable);

        // Pin "MsgID" column (1) to the pixel-width of "MsgID"
        if (replacementTable.getColumnModel().getColumnCount() > 1) {
            int msgIdWidth = replacementTable.getFontMetrics(replacementTable.getFont())
                    .stringWidth("MsgID") + 16;
            replacementTable.getColumnModel().getColumn(1).setMinWidth(msgIdWidth);
            replacementTable.getColumnModel().getColumn(1).setMaxWidth(msgIdWidth);
            replacementTable.getColumnModel().getColumn(1).setPreferredWidth(msgIdWidth);
        }

        // Pin "Ext.Name" column (2) to the pixel-width of "Ext.Name"
        if (replacementTable.getColumnModel().getColumnCount() > 2) {
            int extNameWidth = replacementTable.getFontMetrics(replacementTable.getFont())
                    .stringWidth("Ext.Name") + 16;
            replacementTable.getColumnModel().getColumn(2).setMinWidth(extNameWidth);
            replacementTable.getColumnModel().getColumn(2).setMaxWidth(extNameWidth);
            replacementTable.getColumnModel().getColumn(2).setPreferredWidth(extNameWidth);
        }

        JScrollPane scroll = new JScrollPane(replacementTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setPreferredSize(new Dimension(160, 130));
        this.callbacks.customizeUiComponent(scroll);
        return scroll;
    }

    // =========================================================================
    // Business logic — completely unchanged
    // =========================================================================
    public void removeWhiteSpaceCharacter(String extractionName) {
        for (int index = 0; index < extractionEntrylist.size(); index++) {
            ExtractionEntry extractionEntry = extractionEntrylist.get(index);
            if (extractionEntry.getName().equals(extractionName)) { extractionEntry.clearWhiteSpaces = true; break; }
        }
    }

    public void deleteEntryFromExtractionList(String extractionName) {
        for (int index = 0; index < extractionEntrylist.size(); index++) {
            ExtractionEntry extractionEntry = extractionEntrylist.get(index);
            if (extractionEntry.getName().equals(extractionName)) {
                extractionEntrylist.remove(index);
                extractionListComboBox.removeItem(extractionName);
                ReplacePanel.extractionreplaceComboNameList.removeItem(extractionName);
                for (ObtainEntry obtainEntry : ObtainPanel.obtainEntrylist) {
                    if (obtainEntry.getMsgID().equals(extractionEntry.getextractionmsgID())) {
                        obtainEntry.extractionlistNames.remove(extractionEntry); break;
                    }
                }
                AddEntryToExtractionList.clearAll();
                extractionTableModel.fireTableDataChanged();
                int count = 0;
                for (ReplacementEntry replacementEntry : ObtainPanel.replacementEntrylist) {
                    if (replacementEntry.getextractionName().equals(extractionName)) count++;
                }
                for (int i = 0; i < count; i++) removeRelacementEntry(extractionName);
                AddEntryToReplacementList.clearAll();
                replacementTableModel.fireTableDataChanged();
                int errreplacecount = 0;
                for (ReplaceEntry replaceEntry : ReplacePanel.replaceEntrylist) {
                    if (replaceEntry.getextractionName().equals(extractionName)) errreplacecount++;
                }
                for (int c = 0; c < errreplacecount; c++) removeReplaceEntry(extractionName);
                ReplacePanel.replaceTableModel.fireTableDataChanged();
                break;
            }
        }
    }

    public void removeReplaceEntry(String extractionName) {
        try {
            for (ReplaceEntry replaceEntry : ReplacePanel.replaceEntrylist) {
                if (replaceEntry.getextractionName().equals(extractionName)) {
                    ReplacePanel.replaceEntrylist.remove(replaceEntry); break;
                }
            }
        } catch (Exception e) {
            callbacks.printOutput("Exception while clearing the error condition replacementlist --> " + e.getMessage());
        }
    }

    public void removeRelacementEntry(String extractionName) {
        try {
            for (ReplacementEntry replacementEntry : ObtainPanel.replacementEntrylist) {
                if (replacementEntry.getextractionName().equals(extractionName)) {
                    replacementEntrylist.remove(replacementEntry);
                    for (ObtainEntry obtainEntry : ObtainPanel.obtainEntrylist) {
                        if (obtainEntry.getMsgID().equals(replacementEntry.replacementMsgID)) {
                            obtainEntry.replacementlistNames.remove(replacementEntry); break;
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            callbacks.printOutput("Exception while clearing the replacementlist --> " + e.getMessage());
        }
    }

    public static void syncSelectedMessageEditors() {
        if (obtainTable == null || obtainTableModel == null) { clearSelectedMessageEditors(); return; }
        int selectedRow = obtainTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= obtainEntrylist.size()) {
            if (!obtainEntrylist.isEmpty()) { selectedRow = 0; obtainTable.setRowSelectionInterval(0, 0); }
            else { clearSelectedMessageEditors(); return; }
        }
        if (selectedRow < 0 || selectedRow >= obtainEntrylist.size()) { clearSelectedMessageEditors(); return; }
        Object rowValue = obtainTableModel.getValueAt(selectedRow, 0);
        if (!(rowValue instanceof String)) { clearSelectedMessageEditors(); return; }
        String msgID = (String) rowValue;
        for (ObtainEntry entry : obtainEntrylist) {
            if (entry.getMsgID().equals(msgID)) {
                ObtainTable.selectedMsgId = msgID;
                if (ireqMessageEditor != null) ireqMessageEditor.setMessage(entry.getRequest(), true);
                if (iresMessageEditor != null) iresMessageEditor.setMessage(entry.getResponse(), false);
                return;
            }
        }
        clearSelectedMessageEditors();
    }

    public static void clearSelectedMessageEditors() {
        ObtainTable.selectedMsgId = null;
        if (ireqMessageEditor != null) ireqMessageEditor.setMessage(new byte[0], true);
        if (iresMessageEditor != null) iresMessageEditor.setMessage(new byte[0], false);
    }

    // Legacy shims
    public JPanel preparethirdPanel()  { JPanel p = new JPanel(new BorderLayout()); p.add(prepareRequestResponsePanel(), BorderLayout.CENTER); callbacks.customizeUiComponent(p); return p; }
    public JPanel preparefourthPanel() { JPanel p = new JPanel(new BorderLayout()); callbacks.customizeUiComponent(p); return p; }
    public Component prepareExtractionReplacementPanel() { return buildTopRow(); }

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

    public JPanel getSeperatorPanel() { return makeSeparator(); }
}