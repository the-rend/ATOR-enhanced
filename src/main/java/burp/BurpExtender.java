package burp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class BurpExtender implements IBurpExtender, IContextMenuFactory, ITab, IHttpListener {

    // ── Extension identity ────────────────────────────────────────────────────
    private static final String EXTENSION_NAME     = "ATOR";
    private static final String EXTENSION_TAB_NAME = "ATOR";
    private static final String BRAND_LABEL_TEXT   = "ATOR v3.0.0";

    // ── Colours / fonts shared across the top bar ─────────────────────────────
    private static final Color BURP_ORANGE  = new Color(255, 128,   0);
    private static final Color BURP_ORANGE2 = new Color(220, 100,   0);
    private static final Color BAR_BG       = new Color( 30,  30,  30);   // dark strip
    private static final Color DIVIDER_COL  = new Color( 70,  70,  70);
    private static final Font  TITLE_FONT   = new Font("Dialog", Font.BOLD,  14);
    private static final Font  BUTTON_FONT  = new Font("Dialog", Font.BOLD,  12);
    private static final Font  MODE_FONT    = new Font("Dialog", Font.PLAIN, 12);

    // ── Static callbacks (used by other classes) ──────────────────────────────
    public static IBurpExtenderCallbacks callbacks;
    public static SpotErrorMetaData spoterroMetaData = null;
    public static String bodyContentType;

    // ── Instance state ────────────────────────────────────────────────────────
    IExtensionHelpers helpers;
    UsersTab    usersTab;
    SetttingsTab settingsTab;

    boolean lockATOR          = true;
    boolean multiUserMode     = false;
    private boolean suppressModeEvents = false;
    private boolean modeChanging       = false;

    // Top-bar controls (kept as fields so syncModeControls can reach them)
    private JRadioButton singleUserButton;
    private JRadioButton multiUserButton;
    private JButton      usersButton;
    private JPanel       userSelectorPanel;

    // Lazily-built popup content
    private JDialog  settingsDialog, usersDialog;
    private JComponent settingsContent, usersContent;

    // Workflow tabs
    private JTabbedPane workflowTabs;

    // =========================================================================
    // IBurpExtender
    // =========================================================================
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks cb) {
        callbacks = cb;
        helpers   = cb.getHelpers();
        cb.setExtensionName(EXTENSION_NAME);
        cb.registerHttpListener(this);
        cb.registerContextMenuFactory(this);
        cb.addSuiteTab(this);
        cb.printOutput("ATOR loaded successfully");
    }

    public BurpExtender getComponent() { return this; }

    // =========================================================================
    // ITab
    // =========================================================================
    @Override public String    getTabCaption()  { return EXTENSION_TAB_NAME; }

    @Override
    public Component getUiComponent() {
        log("getUiComponent start");
        usersTab    = new UsersTab(callbacks, getComponent());
        settingsTab = new SetttingsTab(callbacks);

        // Build workflow tabs (initialises all panel instances)
        workflowTabs = buildWorkflowTabs();
        UsersTab.setSingleUserProfileSnapshot(UsersTab.snapshotCurrentState(callbacks));
        log("getUiComponent captured initial single-user snapshot keys="
                + UsersTab.getSingleUserProfileSnapshot().keySet());

        // Root: top bar (NORTH) + workflow tabs (CENTER)
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(workflowTabs,  BorderLayout.CENTER);
        customiseToBurp(root);

        log("getUiComponent end mode=" + (multiUserMode ? "multi-user" : "single-user"));
        return root;
    }

    // =========================================================================
    // Top bar
    //
    //  ┌──────────────────────────────────────────────────────────────────────┐
    //  │  ATOR v3.0.0  │  ⚙ Settings  │  ● Single-user  ○ Multi-user        │
    //  │               │              │  [Users ▼]  [user selector combo]    │
    //  └──────────────────────────────────────────────────────────────────────┘
    //
    //  The bar has three logical sections, separated by thin vertical dividers:
    //    1. Brand label   (fixed left)
    //    2. Settings btn  (fixed)
    //    3. Mode radios + Users button + user-selector combo (right, groups)
    // =========================================================================
    private JPanel buildTopBar() {
        log("buildTopBar start");

        // ── Controls ──────────────────────────────────────────────────────────
        JLabel brandLabel = new JLabel(BRAND_LABEL_TEXT);
        brandLabel.setFont(TITLE_FONT);
        brandLabel.setForeground(BURP_ORANGE);
        brandLabel.setBorder(new EmptyBorder(0, 4, 0, 0));

        JButton settingsButton = makeBarButton("⚙  Settings", false);
        usersButton            = makeBarButton("Users  ▸",    false);
        usersButton.setEnabled(false);   // only active in multi-user mode

        singleUserButton = new JRadioButton("Single-user", true);
        multiUserButton  = new JRadioButton("Multi-user",  false);
        singleUserButton.setFont(MODE_FONT);
        multiUserButton.setFont(MODE_FONT);
        singleUserButton.setOpaque(false);
        multiUserButton.setOpaque(false);
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(singleUserButton);
        modeGroup.add(multiUserButton);

        userSelectorPanel = usersTab.createUserSelectorPanel();
        userSelectorPanel.setEnabled(false);

        // ── Listeners (unchanged logic) ───────────────────────────────────────
        settingsButton.addActionListener(e -> showPopup("Settings", getSettingsContent()));
        usersButton.addActionListener(e -> showPopup("Users", getUsersContent()));

        singleUserButton.addItemListener(e -> {
            if (e.getStateChange() != java.awt.event.ItemEvent.SELECTED) return;
            if (suppressModeEvents) { log("singleUserButton ignored during sync"); return; }
            if (!multiUserMode)     { log("singleUserButton ignored: already single-user"); return; }
            requestModeChange(false, "single-user radio");
        });

        multiUserButton.addItemListener(e -> {
            if (e.getStateChange() != java.awt.event.ItemEvent.SELECTED) return;
            if (suppressModeEvents) { log("multiUserButton ignored during sync"); return; }
            if (multiUserMode)      { log("multiUserButton ignored: already multi-user"); return; }
            requestModeChange(true, "multi-user radio");
        });

        // ── Layout ────────────────────────────────────────────────────────────
        //   brand  |div|  settings  |div|  single ● ● multi  [users btn]  [combo]
        JPanel bar = new JPanel(new GridBagLayout());
        bar.setBorder(new EmptyBorder(6, 12, 6, 12));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy  = 0;
        gc.fill   = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 0, 0);

        // Brand
        gc.gridx = 0; gc.weightx = 0; gc.insets = new Insets(0, 0, 0, 16);
        bar.add(brandLabel, gc);

        // Settings button
        gc.gridx = 1; gc.insets = new Insets(0, 0, 0, 16);
        bar.add(settingsButton, gc);

        // Vertical divider
        gc.gridx = 2; gc.fill = GridBagConstraints.VERTICAL; gc.insets = new Insets(0, 0, 0, 16);
        bar.add(makeThinVDivider(), gc);

        // Mode radios group
        JPanel modeGroup2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        modeGroup2.setOpaque(false);
        modeGroup2.add(singleUserButton);
        modeGroup2.add(multiUserButton);
        gc.gridx = 3; gc.fill = GridBagConstraints.NONE; gc.insets = new Insets(0, 0, 0, 8);
        bar.add(modeGroup2, gc);

        // Users button
        gc.gridx = 4; gc.insets = new Insets(0, 0, 0, 8);
        bar.add(usersButton, gc);

        // User selector combo
        gc.gridx = 5; gc.insets = new Insets(0, 0, 0, 0);
        bar.add(userSelectorPanel, gc);

        // Filler pushes everything left
        gc.gridx = 6; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(0, 0, 0, 0);
        bar.add(new JLabel(), gc);

        // Bottom accent line (orange)
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(bar, BorderLayout.CENTER);
        JSeparator accent = new JSeparator(SwingConstants.HORIZONTAL);
        accent.setForeground(BURP_ORANGE);
        accent.setPreferredSize(new Dimension(0, 2));
        wrapper.add(accent, BorderLayout.SOUTH);

        // Customise
        customiseToBurp(bar);
        customiseToBurp(settingsButton);
        customiseToBurp(usersButton);
        customiseToBurp(singleUserButton);
        customiseToBurp(multiUserButton);
        customiseToBurp(userSelectorPanel);
        customiseToBurp(wrapper);

        syncModeControls(false);
        log("buildTopBar end singleSelected=" + singleUserButton.isSelected()
                + " multiSelected=" + multiUserButton.isSelected()
                + " usersEnabled=" + usersButton.isEnabled());
        return wrapper;
    }

    /** Styled bar button — orange if primary, plain if secondary. */
    private JButton makeBarButton(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFont(BUTTON_FONT);
        if (primary) {
            btn.setForeground(Color.WHITE);
            btn.setBackground(BURP_ORANGE2);
            btn.setOpaque(true);
            btn.setBorderPainted(false);
        }
        btn.setFocusPainted(false);
        return btn;
    }

    private Component makeThinVDivider() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 24));
        sep.setForeground(DIVIDER_COL);
        return sep;
    }

    // =========================================================================
    // Workflow tabs
    //
    //  Tab labels follow the existing naming convention but are cleaned up
    //  slightly and given number badges so the flow is obvious.
    //  Each panel is wrapped in a JScrollPane so content is never clipped.
    // =========================================================================
    private JTabbedPane buildWorkflowTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Dialog", Font.BOLD, 12));

        tabs.addTab("1  ·  Error Condition",       wrapInScroll(new ErrorPanel(callbacks, getComponent()).preparePanel()));
        tabs.addTab("2  ·  Obtain Token",           wrapInScroll(new ObtainPanel(callbacks, getComponent()).preparePanel()));
        tabs.addTab("3  ·  Error Replacement",      wrapInScroll(new ReplacePanel(callbacks, getComponent()).preparePanel()));
        tabs.addTab("4  ·  Preview",                wrapInScroll(new PreviewPanel(callbacks, getComponent()).preparePanel()));

        customiseToBurp(tabs);
        return tabs;
    }

    private JScrollPane wrapInScroll(JPanel panel) {
        JScrollPane scroll = new JScrollPane(panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        customiseToBurp(scroll);
        return scroll;
    }

    // =========================================================================
    // IHttpListener — completely unchanged logic
    // =========================================================================
    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
        IRequestInfo reqInfo = helpers.analyzeRequest(messageInfo);
        if (!multiUserMode) {
            log("HTTP " + (messageIsRequest ? "request" : "response") + " tool=" + toolFlag
                    + " mode=single-user selectedProfile=" + UsersTab.getActiveProfileName());
            processHttpMessageForCurrentProfile(toolFlag, messageIsRequest, messageInfo, reqInfo);
            return;
        }
        String matchedProfile = UsersTab.resolveProfileForMessageStrict(messageInfo, messageIsRequest);
        log("HTTP " + (messageIsRequest ? "request" : "response") + " tool=" + toolFlag
                + " mode=multi-user matchedProfile=" + matchedProfile
                + " selectedProfile=" + UsersTab.getActiveProfileName());
        if (matchedProfile == null) return;
        UsersTab.runWithProfile(matchedProfile, callbacks,
                () -> processHttpMessageForCurrentProfile(toolFlag, messageIsRequest, messageInfo, reqInfo));
    }

    private void processHttpMessageForCurrentProfile(int toolFlag, boolean messageIsRequest,
                                                     IHttpRequestResponse messageInfo, IRequestInfo reqInfo) {
        log("processHttpMessageForCurrentProfile enter request=" + messageIsRequest
                + " tool=" + toolFlag + " url=" + (reqInfo == null ? "null" : reqInfo.getUrl()));
        if (!SetttingsTab.isToolEnabled(toolFlag)) { log("processHttpMessageForCurrentProfile skipped: tool disabled"); return; }
        if (SetttingsTab.isInScopeEnabled()) {
            boolean inScope = callbacks.isInScope(reqInfo.getUrl());
            if (!inScope) { log("processHttpMessageForCurrentProfile skipped: out of scope"); return; }
        }
        populateFlowEditors(messageInfo, messageIsRequest);
        if (!PreviewPanel.isPreviewEnabled) {
            if (messageIsRequest) {
                String newRequest = ExecuteATORMacro.replaceOnRequest(messageInfo);
                byte[] updatedRequest = Utils.checkContentLength(newRequest.getBytes(), helpers);
                messageInfo.setRequest(updatedRequest);
            }
            if (!messageIsRequest && lockATOR) {
                boolean isConditionMatched = CheckCondition.evaluteErrorCondition(messageInfo);
                if (isConditionMatched) {
                    lockATOR = false;
                    new ExecuteATORMacro(callbacks).executeATORMacro();
                    IHttpService iHttpService = messageInfo.getHttpService();
                    String newRequestAfterATOR = ExecuteATORMacro.replaceOnRequest(messageInfo);
                    byte[] updatedRequest = Utils.checkContentLength(helpers.stringToBytes(newRequestAfterATOR), helpers);
                    IHttpRequestResponse updated = callbacks.makeHttpRequest(iHttpService, updatedRequest);
                    messageInfo.setResponse(updated.getResponse());
                    lockATOR = true;
                }
            }
        }
    }

    private void populateFlowEditors(IHttpRequestResponse messageInfo, boolean messageIsRequest) {
        if (messageInfo == null) { log("populateFlowEditors skipped: messageInfo null"); return; }
        int reqLen = messageInfo.getRequest()  == null ? 0 : messageInfo.getRequest().length;
        int resLen = messageInfo.getResponse() == null ? 0 : messageInfo.getResponse().length;
        log("populateFlowEditors " + (messageIsRequest ? "request" : "response")
                + " reqLen=" + reqLen + " resLen=" + resLen);
        if (messageIsRequest) {
            if (ErrorPanel.ireqMessageEditor   != null) ErrorPanel.ireqMessageEditor.setMessage(messageInfo.getRequest(), true);
            if (ReplacePanel.ireqMessageEditor != null) ReplacePanel.ireqMessageEditor.setMessage(messageInfo.getRequest(), true);
            if (PreviewPanel.ireqMessageEditor != null) PreviewPanel.ireqMessageEditor.setMessage(messageInfo.getRequest(), true);
        } else {
            if (ErrorPanel.iresMessageEditor   != null) ErrorPanel.iresMessageEditor.setMessage(messageInfo.getResponse(), false);
            if (ReplacePanel.iresMessageEditor != null) ReplacePanel.iresMessageEditor.setMessage(messageInfo.getResponse(), false);
            if (PreviewPanel.iresMessageEditor != null) PreviewPanel.iresMessageEditor.setMessage(messageInfo.getResponse(), false);
        }
    }

    // =========================================================================
    // Context menu — completely unchanged logic
    // =========================================================================
    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        IHttpRequestResponse[] messages = invocation.getSelectedMessages();
        if (messages == null || messages.length == 0) return null;

        List<JMenuItem> menu = new LinkedList<>();
        JMenu mainmenu = new JMenu(EXTENSION_NAME);

        if (!multiUserMode) {
            mainmenu.add(createDisabledHeaderItem("Single-User Mode"));
            mainmenu.addSeparator();
            mainmenu.add(createActionItem("Send to Error Condition", messages, MenuActions.ATOR_ERROR));
            mainmenu.add(createActionItem("Send to Obtain Token", messages, MenuActions.ATOR_MACRO));
            menu.add(mainmenu);
            return menu;
        }

        mainmenu.add(createDisabledHeaderItem("Multi-User Mode"));
        mainmenu.addSeparator();
        for (String userName : UsersTab.getUserNames()) {
            JMenu userMenu       = new JMenu(userName);
            userMenu.add(createActionItemForUser("Send to Error Condition", messages, MenuActions.ATOR_ERROR, userName));
            userMenu.add(createActionItemForUser("Send to Obtain Token", messages, MenuActions.ATOR_MACRO, userName));
            mainmenu.add(userMenu);
        }
        menu.add(mainmenu);
        return menu;
    }

    private JMenuItem createDisabledHeaderItem(String label) {
        JMenuItem item = new JMenuItem(label);
        item.setEnabled(false);
        return item;
    }

    private JMenuItem createActionItem(String label, IHttpRequestResponse[] messages, MenuActions action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> new MenuAllListener(callbacks, messages, action, getComponent()).actionPerformed(null));
        return item;
    }

    private JMenuItem createActionItemForUser(String label, IHttpRequestResponse[] messages, MenuActions action, String userName) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> {
            suppressModeEvents = true;
            try {
                UsersTab.runWithProfile(userName, callbacks, () ->
                        new MenuAllListener(callbacks, messages, action, getComponent()).actionPerformed(null));
            } finally {
                suppressModeEvents = false;
                syncModeControls(false);
            }
        });
        return item;
    }

    // =========================================================================
    // Settings / Users popup dialogs — unchanged logic
    // =========================================================================
    private JComponent getSettingsContent() {
        if (settingsContent == null) { settingsContent = settingsTab.initSettingsGui(); customiseToBurp(settingsContent); }
        return settingsContent;
    }

    private JComponent getUsersContent() {
        if (usersContent == null) { usersContent = usersTab.initUsersGui(); customiseToBurp(usersContent); }
        return usersContent;
    }

    private void showPopup(String title, JComponent content) {
        JDialog dialog = "Settings".equals(title) ? settingsDialog : usersDialog;
        if (dialog == null || !dialog.isDisplayable()) {
            dialog = new JDialog((Frame) null, title, false);
            if ("Users".equals(title)) dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            else dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.getContentPane().setLayout(new BorderLayout());
            Container parent = content.getParent();
            if (parent != null) parent.remove(content);
            dialog.getContentPane().add(content, BorderLayout.CENTER);
            if ("Users".equals(title)) {
                final JDialog usersDialogRef = dialog;
                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        if (warnAboutUsersWithoutMatchers(usersDialogRef)) return;
                        usersDialogRef.dispose();
                    }
                });
            }
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            if ("Settings".equals(title)) settingsDialog = dialog;
            else                          usersDialog    = dialog;
        }
        dialog.pack();
        dialog.setVisible(true);
        dialog.toFront();
    }

    private boolean warnAboutUsersWithoutMatchers(JDialog dialog) {
        List<String> missingUsers = UsersTab.getProfilesWithoutMatchers();
        if (missingUsers.isEmpty()) return false;

        StringBuilder message = new StringBuilder();
        if (missingUsers.size() == 1) {
            message.append("The following user has no request matcher condition:\n\n");
        } else {
            message.append("The following users have no request matcher conditions:\n\n");
        }
        for (String userName : missingUsers) {
            message.append("- ").append(userName).append('\n');
        }
        message.append('\n').append("Remove the user or add a request matcher condition before closing the window.");

        JOptionPane.showMessageDialog(dialog, message.toString(),
                "Users Missing Conditions", JOptionPane.WARNING_MESSAGE);
    return true;
    }

    // =========================================================================
    // Mode switching — unchanged logic
    // =========================================================================
    private void requestModeChange(boolean enabled, String source) {
        log("requestModeChange source=" + source + " enabled=" + enabled
                + " currentMode=" + multiUserMode
                + " selectedProfile=" + UsersTab.getActiveProfileName()
                + " singleSnapshotKeys=" + (UsersTab.getSingleUserProfileSnapshot() == null
                        ? "null" : UsersTab.getSingleUserProfileSnapshot().keySet()));
        if (suppressModeEvents) { log("requestModeChange ignored during sync source=" + source); return; }
        if (modeChanging)       { log("requestModeChange ignored: already changing source=" + source); return; }
        if (singleUserButton == null || multiUserButton == null) {
            multiUserMode = enabled; log("requestModeChange deferred: controls null"); return;
        }
        if (multiUserMode == enabled) { syncModeControls(true); log("requestModeChange no-op"); return; }
        modeChanging = true;
        suppressModeEvents = true;
        try {
            if (!enabled) {
                UsersTab.saveSelectedProfileProfile(callbacks);
                UsersTab.loadSingleUserProfile(callbacks);
                log("Mode switched to single-user activeProfile=" + UsersTab.getActiveProfileName());
            } else {
                UsersTab.saveSingleUserProfile(callbacks);
                UsersTab.loadSelectedProfileProfile(callbacks);
                log("Mode switched to multi-user activeProfile=" + UsersTab.getActiveProfileName());
            }
            multiUserMode = enabled;
            syncModeControls(true);
            log("requestModeChange complete mode=" + (multiUserMode ? "multi-user" : "single-user"));
        } finally {
            modeChanging = false;
            SwingUtilities.invokeLater(() -> suppressModeEvents = false);
        }
    }

    private void syncModeControls(boolean logState) {
        if (singleUserButton == null || multiUserButton == null) { log("syncModeControls skipped: buttons null"); return; }
        suppressModeEvents = true;
        try {
            if (logState) log("syncModeControls before singleSelected=" + singleUserButton.isSelected()
                    + " multiSelected=" + multiUserButton.isSelected() + " mode=" + (multiUserMode ? "multi-user" : "single-user"));
            singleUserButton.setSelected(!multiUserMode);
            multiUserButton.setSelected(multiUserMode);
            if (usersButton != null) usersButton.setEnabled(multiUserMode);
            if (userSelectorPanel != null) {
                setComponentEnabled(userSelectorPanel, multiUserMode);
                userSelectorPanel.setEnabled(multiUserMode);
            }
            if (usersTab != null) usersTab.refreshUserSelectorState();
            if (logState) log("syncModeControls after singleSelected=" + singleUserButton.isSelected()
                    + " multiSelected=" + multiUserButton.isSelected());
        } finally {
            suppressModeEvents = false;
        }
    }

    private void setComponentEnabled(Component component, boolean enabled) {
        if (component == null) return;
        component.setEnabled(enabled);
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents())
                setComponentEnabled(child, enabled);
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    private void customiseToBurp(Component component) { callbacks.customizeUiComponent(component); }

    public UsersTab getUsersTab() { return usersTab; }

    public boolean isMultiUserMode() { return multiUserMode; }

    public static void log(String message) {
    }
}