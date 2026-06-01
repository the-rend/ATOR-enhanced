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
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

public class PreviewPanel {
    IBurpExtenderCallbacks callbacks;
    BurpExtender extender;

    // ── Palette ───────────────────────────────────────────────────────────────
    private static Color BURP_ORANGE  = new Color(255, 128,   0);
    private static Color BURP_ORANGE2 = new Color(220, 100,   0);
    private static Color COL_ORIG     = new Color(180,  80,  80);
    private static Color COL_ATOR     = new Color(160, 120,   0);
    private static Color COL_MOD      = new Color( 50, 130,  80);
    // Subtle tinted background for the condition card
    private static Color CARD_BG      = new Color( 40,  40,  40);
    private static Color CARD_BORDER  = new Color( 70,  70,  70);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private Font mainHeaderFont = new Font("Dialog", Font.BOLD, 15);
    private Font subHeaderFont  = new Font("Dialog", Font.BOLD, 13);
    private Font labelFont      = new Font("Dialog", Font.PLAIN, 12);
    private Font monoFont       = new Font("Monospaced", Font.PLAIN, 12);
    private Font buttonFont     = new Font("Dialog", Font.BOLD, 12);
    private Font tabFont        = new Font("Dialog", Font.BOLD, 12);
    private Font badgeFont      = new Font("Dialog", Font.BOLD, 11);

    // ── State ─────────────────────────────────────────────────────────────────
    public static boolean isPreviewEnabled, isErrorConditionMatched = false;

    public static IMessageEditor ireqMessageEditor,         iresMessageEditor;
    public static IMessageEditor ireqatorMessageEditor,     iresatorMessageEditor;
    public static IMessageEditor ireqmodifiedMessageEditor, iresmodifiedMessageEditor;

    public static PreviewTable      previewTable;
    public static PreviewTableModel previewTableModel;
    public static ArrayList<PreviewEntry> previewEntryList = new ArrayList<PreviewEntry>();

    // conditionDetails is a non-editable JTextArea so it can wrap naturally.
    // It is NOT styled as an input field — it uses the panel's own background.
    public static JTextArea conditionDetails;

    public PreviewPanel(IBurpExtenderCallbacks callbacks, BurpExtender extender) {
        this.callbacks = callbacks;
        this.extender  = extender;
    }

    // =========================================================================
    // Root panel
    //
    //  ┌─────────────────────────────────────────────────────────────────────┐
    //  │  Preview                                [▶ Run Test]                │  header row
    //  │  Send a request to "Error Condition" …                              │
    //  ├─────────────────────────────────────────────────────────────────────┤
    //  │  ┌──────────────────────────────────────────────────────────────┐   │
    //  │  │ Active condition                                              │   │  condition card
    //  │  │  <auto-populated multi-line text — monospaced, wraps>        │   │
    //  │  └──────────────────────────────────────────────────────────────┘   │
    //  ├─────────────────────────────────────────────────────────────────────┤
    //  │  [ 1 · Original ] [ 2 · ATOR Macro ] [ 3 · Modified ]               │  tabs fill rest
    //  └─────────────────────────────────────────────────────────────────────┘
    // =========================================================================
    public JPanel preparePanel() {
        createAllEditors();

        JPanel root = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0; gc.insets = new Insets(0, 0, 0, 0);

        // Header row (title + description + Run Test button)
        gc.gridy = 0; gc.weighty = 0; gc.fill = GridBagConstraints.HORIZONTAL;
        root.add(buildHeaderRow(), gc);

        // Separator
        gc.gridy = 1;
        root.add(makeSeparator(), gc);

        // Condition card
        gc.gridy = 2;
        root.add(buildConditionCard(), gc);

        // Separator
        gc.gridy = 3;
        root.add(makeSeparator(), gc);

        // Tabbed editors — fills all remaining vertical space
        gc.gridy = 4; gc.weighty = 1.0; gc.fill = GridBagConstraints.BOTH;
        root.add(buildTabbedEditors(), gc);

        callbacks.customizeUiComponent(root);
        return root;
    }

    // =========================================================================
    // Header row
    //
    //  LEFT column (grows):  orange title + two description lines
    //  RIGHT column (fixed): ▶ Run Test button
    //
    //  This mirrors the pattern of every other panel in the tool, but puts the
    //  primary action in the header itself rather than a separate toolbar.
    // =========================================================================
    private JPanel buildHeaderRow() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(8, 16, 8, 16));

        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;

        // ── Left: title + description ─────────────────────────────────────────
        JPanel textBlock = new JPanel(new GridBagLayout());
        textBlock.setOpaque(false);
        GridBagConstraints tc = new GridBagConstraints();
        tc.gridx = 0; tc.fill = GridBagConstraints.HORIZONTAL; tc.weightx = 1.0; tc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Preview");
        title.setFont(mainHeaderFont);
        title.setForeground(BURP_ORANGE);
        callbacks.customizeUiComponent(title);

        JLabel desc1 = new JLabel("Send a request to \"1 · Error Condition\" first, then click Run Test to fire the full ATOR flow.");
        desc1.setFont(labelFont);
        callbacks.customizeUiComponent(desc1);

        JLabel desc2 = new JLabel("The three tabs below show the traffic at each stage: original → ATOR macro → modified.");
        desc2.setFont(labelFont);
        callbacks.customizeUiComponent(desc2);

        tc.gridy = 0; tc.insets = new Insets(0, 0, 4, 0); textBlock.add(title, tc);
        tc.gridy = 1; tc.insets = new Insets(0, 0, 2, 0); textBlock.add(desc1, tc);
        tc.gridy = 2; tc.insets = new Insets(0, 0, 0, 0); textBlock.add(desc2, tc);

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 16);
        panel.add(textBlock, gc);

        // ── Right: Run Test button ─────────────────────────────────────────────
        JButton testRun = new JButton("▶  Run Test");
        testRun.setFont(new Font("Dialog", Font.BOLD, 13));
        testRun.setForeground(Color.WHITE);
        testRun.setBackground(BURP_ORANGE2);
        testRun.setOpaque(true);
        testRun.setBorderPainted(false);
        testRun.setFocusPainted(false);
        testRun.setPreferredSize(new Dimension(130, 40));
        testRun.setMinimumSize(new Dimension(130, 40));
        callbacks.customizeUiComponent(testRun);

        testRun.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String text = conditionDetails == null ? null : conditionDetails.getText();
                if (text != null && !text.trim().isEmpty()) {
                    resetPreviewPanel();
                    isErrorConditionMatched = false;
                    executeDryRun();
                } else {
                    JOptionPane.showMessageDialog(null,
                            "At least one error condition should be configured in the \"Error Condition\" tab "
                            + "before running the test.",
                            "No Condition Defined", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        gc.gridx = 1; gc.gridy = 0; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER; gc.insets = new Insets(0, 0, 0, 0);
        panel.add(testRun, gc);

        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Condition card
    //
    // A visually distinct, borderless read-only text area that shows the
    // currently active condition expression (auto-populated by FinalErrorCondition).
    // It has a subtle tinted background so it reads as "data" rather than UI,
    // and an orange left accent border to tie it to the theme.
    //
    // No scroll wrapper — it wraps at 2 rows max and the user can't type in it.
    // =========================================================================
    private JPanel buildConditionCard() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBorder(new EmptyBorder(4, 16, 8, 16));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL; gc.anchor = GridBagConstraints.WEST;

        // "Active Condition" section label
        JLabel cardTitle = new JLabel("Active Condition");
        cardTitle.setFont(subHeaderFont);
        cardTitle.setForeground(BURP_ORANGE);
        callbacks.customizeUiComponent(cardTitle);
        gc.gridy = 0; gc.insets = new Insets(0, 0, 4, 0);
        outer.add(cardTitle, gc);

        // The text area itself — non-editable, no border, monospaced, 2 visible rows
        conditionDetails = new JTextArea(2, 80);
        conditionDetails.setEditable(false);
        conditionDetails.setFocusable(false);
        conditionDetails.setFont(monoFont);
        conditionDetails.setLineWrap(true);
        conditionDetails.setWrapStyleWord(true);
        conditionDetails.setOpaque(true);
        // Apply Burp theming BEFORE we set our own colours so Burp's dark theme
        // gets applied first; then we only override the border.
        callbacks.customizeUiComponent(conditionDetails);

        // Wrap in a panel that gives it the orange left accent + subtle border
        JPanel cardInner = new JPanel(new BorderLayout());
        cardInner.setBorder(new CompoundBorder(
                new MatteBorder(1, 3, 1, 1, BURP_ORANGE),   // orange left accent, 1px elsewhere
                new EmptyBorder(6, 10, 6, 8)                 // inner padding
        ));
        cardInner.add(conditionDetails, BorderLayout.CENTER);
        callbacks.customizeUiComponent(cardInner);

        gc.gridy = 1; gc.insets = new Insets(0, 0, 0, 0);
        outer.add(cardInner, gc);

        callbacks.customizeUiComponent(outer);
        return outer;
    }

    // =========================================================================
    // Tabbed editors
    // =========================================================================
    private JTabbedPane buildTabbedEditors() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(tabFont);
        callbacks.customizeUiComponent(tabs);

        tabs.addTab("1 · Original",
                buildReqResTab("Original Request",   COL_ORIG, ireqMessageEditor,
                               "Original Response",  COL_ORIG, iresMessageEditor));
        tabs.addTab("2 · ATOR Macro", buildAtorMacroTab());
        tabs.addTab("3 · Modified",
                buildReqResTab("Modified Request",   COL_MOD,  ireqmodifiedMessageEditor,
                               "Modified Response",  COL_MOD,  iresmodifiedMessageEditor));
        return tabs;
    }

    private JPanel buildReqResTab(String reqTitle, Color labelColor, IMessageEditor reqEditor,
                                   String resTitle, Color resLabelColor, IMessageEditor resEditor) {
        JPanel left  = buildEditorPane(reqTitle,  labelColor,    reqEditor);
        JPanel right = buildEditorPane(resTitle,  resLabelColor, resEditor);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.5);
        split.setMinimumSize(new Dimension(0, 0));
        callbacks.customizeUiComponent(split);
        JPanel tab = new JPanel(new BorderLayout());
        tab.add(split, BorderLayout.CENTER);
        tab.setMinimumSize(new Dimension(0, 0));
        callbacks.customizeUiComponent(tab);
        return tab;
    }

    private JPanel buildAtorMacroTab() {
        JPanel tab = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0; gc.insets = new Insets(0, 0, 0, 0);

        // Compact section label for the table
        JLabel tableLbl = new JLabel("Executed ATOR Macro requests");
        tableLbl.setFont(subHeaderFont);
        tableLbl.setForeground(BURP_ORANGE);
        tableLbl.setBorder(new EmptyBorder(8, 10, 4, 10));
        callbacks.customizeUiComponent(tableLbl);
        gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.weighty = 0;
        tab.add(tableLbl, gc);

        // Macro table
        gc.gridy = 1;
        tab.add(generateTablePanel(), gc);

        gc.gridy = 2;
        tab.add(makeSeparator(), gc);

        // ATOR req/res editors fill remaining height
        JPanel left  = buildEditorPane("ATOR Macro Request",  COL_ATOR, ireqatorMessageEditor);
        JPanel right = buildEditorPane("ATOR Macro Response", COL_ATOR, iresatorMessageEditor);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.5);
        split.setMinimumSize(new Dimension(0, 0));
        callbacks.customizeUiComponent(split);
        gc.gridy = 3; gc.weighty = 1.0; gc.fill = GridBagConstraints.BOTH;
        tab.add(split, gc);

        callbacks.customizeUiComponent(tab);
        return tab;
    }

    private JPanel buildEditorPane(String title, Color labelColor, IMessageEditor editor) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(new EmptyBorder(6, 8, 6, 8));
        // CRITICAL: without a (0,0) minimum the editor's preferredSize leaks
        // through the JSplitPane and causes the tab to overflow the window when
        // a large request/response is loaded.
        panel.setMinimumSize(new Dimension(0, 0));
        JLabel lbl = new JLabel(title);
        lbl.setFont(badgeFont);
        lbl.setForeground(labelColor);
        callbacks.customizeUiComponent(lbl);
        panel.add(lbl,                   BorderLayout.NORTH);
        Component editorComponent = editor.getComponent();
        editorComponent.setMinimumSize(new Dimension(0, 0));
        editorComponent.setPreferredSize(new Dimension(0, 0));
        panel.add(editorComponent, BorderLayout.CENTER);
        callbacks.customizeUiComponent(panel);
        return panel;
    }

    // =========================================================================
    // Editors init
    // =========================================================================
    private void createAllEditors() {
        ErrorRequestResponse rr1 = new ErrorRequestResponse();
        ireqMessageEditor = callbacks.createMessageEditor(rr1, true);
        iresMessageEditor = callbacks.createMessageEditor(rr1, true);

        ErrorRequestResponse rr2 = new ErrorRequestResponse();
        ireqatorMessageEditor = callbacks.createMessageEditor(rr2, true);
        iresatorMessageEditor = callbacks.createMessageEditor(rr2, true);

        ErrorRequestResponse rr3 = new ErrorRequestResponse();
        ireqmodifiedMessageEditor = callbacks.createMessageEditor(rr3, true);
        iresmodifiedMessageEditor = callbacks.createMessageEditor(rr3, true);
    }

    // =========================================================================
    // Table — logic unchanged
    // =========================================================================
    public JScrollPane generateTablePanel() {
        previewTableModel = new PreviewTableModel(previewEntryList);
        previewTable = new PreviewTable(previewTableModel, callbacks);
        previewTable.setModel(previewTableModel);
        this.callbacks.customizeUiComponent(previewTable);
        applyPreviewMacroTableColumnWidths(previewTable);

        JScrollPane scroll = new JScrollPane(previewTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setPreferredSize(new Dimension(900, 100));
        this.callbacks.customizeUiComponent(scroll);
        return scroll;
    }

    public static void applyPreviewMacroTableColumnWidths(PreviewTable table) {
        if (table == null || table.getColumnModel().getColumnCount() == 0) return;

        int msgIdWidth = table.getFontMetrics(table.getFont()).stringWidth("MsgID") + 16;
        table.getColumnModel().getColumn(0).setMinWidth(msgIdWidth);
        table.getColumnModel().getColumn(0).setMaxWidth(msgIdWidth);
        table.getColumnModel().getColumn(0).setPreferredWidth(msgIdWidth);

        if (table.getColumnModel().getColumnCount() > 1) {
            int methodWidth = table.getFontMetrics(table.getFont()).stringWidth("CONNECT") + 16;
            table.getColumnModel().getColumn(1).setMinWidth(methodWidth);
            table.getColumnModel().getColumn(1).setMaxWidth(methodWidth);
            table.getColumnModel().getColumn(1).setPreferredWidth(methodWidth);
        }

        if (table.getColumnModel().getColumnCount() > 2) {
            int hostPreferred = table.getFontMetrics(table.getFont()).stringWidth("api.example.com") + 16;
            table.getColumnModel().getColumn(2).setPreferredWidth(hostPreferred);
        }
    }

    // =========================================================================
    // Business logic — completely unchanged
    // =========================================================================
    public void executeDryRun() {
        BurpExtender.log("executeDryRun start previewEntries=" + previewEntryList.size());
        SwingWorker swing = new SwingWorker() {
            @Override protected Object doInBackground() throws Exception {
                executePreviewPanel(); isPreviewEnabled = false; return "Executed";
            }
        };
        swing.execute();
    }

    public void executePreviewPanel() {
        try {
            BurpExtender.log("executePreviewPanel start spotMeta=" + (BurpExtender.spoterroMetaData != null));
            if (BurpExtender.spoterroMetaData != null) {
                IHttpService iHttpService = BurpExtender.spoterroMetaData.iHttpService;
                byte[] request = BurpExtender.spoterroMetaData.request;
                isPreviewEnabled = true;

                Thread beforeReplacement = new Thread() {
                    public void run() {
                        IHttpRequestResponse iHttpRequestResponse = callbacks.makeHttpRequest(iHttpService, request);
                        isErrorConditionMatched = CheckCondition.evaluteErrorCondition(iHttpRequestResponse);
                        if (!isErrorConditionMatched) {
                            JOptionPane.showMessageDialog(null,
                                    "ATOR Macro will execute only if specified error condition is matched. "
                                    + "Check Spot Error Condition(Level 1) & Replace(Level 3) to add appropriate condition",
                                    "Error Condition Mismatch", JOptionPane.WARNING_MESSAGE);
                            return;
                        } else {
                            PreviewPanel.iresMessageEditor.setMessage(iHttpRequestResponse.getResponse(), false);
                        }
                    }
                };
                beforeReplacement.start();
                try { beforeReplacement.join(); }
                catch (InterruptedException e) { callbacks.printOutput("Exception in SPOT Error condition call: " + e.getMessage()); }

                ExecuteDryRun executeDryRun = new ExecuteDryRun(callbacks);
                if (isErrorConditionMatched) {
                    executeDryRun.start();
                    try { executeDryRun.join(); }
                    catch (InterruptedException e) { callbacks.printOutput("Exception in ATOR MACRO call: " + e.getMessage()); }
                    new Thread() { public void run() { executeErrorConditionAfterExtraction(); } }.start();
                }
            }
        } catch (Exception e) {
            BurpExtender.log("executePreviewPanel exception=" + e.getClass().getSimpleName() + ": " + e.getMessage());
            callbacks.printOutput("Exception in making HTTP call: " + e.getMessage());
        }
    }

    public static String executeErrorConditionAfterExtraction() {
        BurpExtender.log("executeErrorConditionAfterExtraction start");
        IHttpRequestResponse iHttpRequestResponse = BurpExtender.spoterroMetaData.iHttpRequestResponse;
        String requestmsg = ExecuteATORMacro.replaceOnRequest(iHttpRequestResponse);
        IExtensionHelpers helpers = BurpExtender.callbacks.getHelpers();
        byte[] updatedRequest = Utils.checkContentLength(requestmsg.getBytes(), helpers);
        IHttpRequestResponse updatedIHttpRequestResponse = BurpExtender.callbacks.makeHttpRequest(
                BurpExtender.spoterroMetaData.iHttpService, updatedRequest);
        PreviewPanel.ireqmodifiedMessageEditor.setMessage(updatedIHttpRequestResponse.getRequest(), true);
        PreviewPanel.iresmodifiedMessageEditor.setMessage(updatedIHttpRequestResponse.getResponse(), false);
        BurpExtender.log("executeErrorConditionAfterExtraction done");
        return requestmsg;
    }

    public void resetPreviewPanel() {
        try {
            BurpExtender.log("resetPreviewPanel before previewEntries=" + PreviewPanel.previewEntryList.size());
            String emptyData = "";
            PreviewPanel.iresMessageEditor.setMessage(emptyData.getBytes(), false);
            PreviewPanel.previewEntryList.clear();
            PreviewPanel.previewTableModel.fireTableRowsInserted(
                    PreviewPanel.previewTableModel.getRowCount() - 1,
                    PreviewPanel.previewTableModel.getRowCount() - 1);
            PreviewPanel.ireqatorMessageEditor.setMessage(emptyData.getBytes(), true);
            PreviewPanel.iresatorMessageEditor.setMessage(emptyData.getBytes(), false);
            PreviewPanel.ireqmodifiedMessageEditor.setMessage(emptyData.getBytes(), true);
            PreviewPanel.iresmodifiedMessageEditor.setMessage(emptyData.getBytes(), false);
            BurpExtender.log("resetPreviewPanel after previewEntries=" + PreviewPanel.previewEntryList.size());
        } catch (Exception e) {
            BurpExtender.callbacks.printOutput("Exception while resetting the preview panel");
        }
    }

    // =========================================================================
    // Legacy shims — kept so other classes compile
    // =========================================================================
    public JPanel preparefirstPanel() { return buildHeaderRow(); }
    public JPanel firstPanel()        { return buildHeaderRow(); }

    public java.awt.Component prepareRequestResponsePanel() {
        JPanel p = new JPanel(new BorderLayout());
        if (ireqMessageEditor != null) p.add(ireqMessageEditor.getComponent(), BorderLayout.CENTER);
        return p;
    }
    public java.awt.Component prepareRequestResponseATORPanel() {
        JPanel p = new JPanel(new BorderLayout());
        if (ireqatorMessageEditor != null) p.add(ireqatorMessageEditor.getComponent(), BorderLayout.CENTER);
        return p;
    }
    public java.awt.Component prepareRequestResponseModifiedPanel() {
        JPanel p = new JPanel(new BorderLayout());
        if (ireqmodifiedMessageEditor != null) p.add(ireqmodifiedMessageEditor.getComponent(), BorderLayout.CENTER);
        return p;
    }

    public JPanel generateImage() {
        JPanel panel = new JPanel(new BorderLayout());
        try {
            GenerateImage.getcreateImageData();
            BufferedImage myPicture = ImageIO.read(
                    new File(System.getProperty("java.io.tmpdir") + "/" + "burppreviewpanel" + ".png"));
            panel.add(new JLabel(new ImageIcon(myPicture)), BorderLayout.CENTER);
        } catch (Exception e) {
            callbacks.printOutput("Exception while showing image: " + e.getMessage());
        }
        return panel;
    }

    // =========================================================================
    // Separator helper
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