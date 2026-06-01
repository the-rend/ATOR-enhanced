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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class UsersTab {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String DEFAULT_PROFILE_NAME    = "default";
    private static final String SINGLE_USER_PROFILE_NAME = "__single_user__";
    private static final String COMMENT_PREFIX          = "ATOR_USER:";
    private static final String[] MATCH_LOGICS          = new String[]{"AND", "OR"};

    // ── Static state ──────────────────────────────────────────────────────────
    private static final Map<String, UserProfile> profiles = new LinkedHashMap<String, UserProfile>();
    private static final UserProfile singleUserProfile = new UserProfile(SINGLE_USER_PROFILE_NAME);
    private static String selectedProfileName = null;
    private static UsersTab uiInstance = null;

    // ── Instance fields ───────────────────────────────────────────────────────
    private final IBurpExtenderCallbacks callbacks;
    private final BurpExtender extender;

    private final DefaultListModel<String>  userListModel  = new DefaultListModel<String>();
    private final DefaultComboBoxModel<String> userComboModel = new DefaultComboBoxModel<String>();
    private JList<String>  userList;
    private JComboBox<String> userCombo;
    private JTable matcherTable;
    private JTextField matchField;
    private JCheckBox regexBox;

    private JButton deleteUserButton;
    private JButton renameUserButton;
    private JButton deleteMatcherButton;
    private final UserMatcherTableModel matcherTableModel = new UserMatcherTableModel();
    private boolean updatingUi = false;

    // ── Style constants ───────────────────────────────────────────────────────
    private static final Color BURP_ORANGE  = new Color(255, 128,  0);
    private static final Color BURP_ORANGE2 = new Color(220, 100,  0);
    private static final Font  HEADER_FONT  = new Font("Dialog", Font.BOLD, 15);
    private static final Font  SUB_FONT     = new Font("Dialog", Font.BOLD, 13);
    private static final Font  LABEL_FONT   = new Font("Dialog", Font.PLAIN, 12);
    private static final Font  BUTTON_FONT  = new Font("Dialog", Font.BOLD, 12);

    public UsersTab(IBurpExtenderCallbacks callbacks, BurpExtender extender) {
        this.callbacks = callbacks;
        this.extender  = extender;
        uiInstance = this;
        ensureDefaultProfile();
        BurpExtender.log("UsersTab constructed profiles=" + profiles.keySet()
                + " selectedProfile=" + selectedProfileName
                + " singleProfileKeys=" + singleUserProfile.getSnapshot().keySet());
    }

    // =========================================================================
    // Main GUI
    //
    //  ┌──────────────────────────────────────────────────────────────────┐
    //  │  Header: "Users"  description                                   │ fixed
    //  ├──────────────────────────────────────────────────────────────────┤
    //  │  [Add User]  [Rename]  [Delete]                                 │ fixed
    //  ├──────────────────────────────────────────────────────────────────┤
    //  │  User list (left) │  Matcher editor (right, fills remaining)     │ grows
    //  └──────────────────────────────────────────────────────────────────┘
    // =========================================================================
    public JPanel initUsersGui() {
        JPanel root = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(0, 0, 0, 0);

        gc.gridy = 0; gc.weighty = 0;
        root.add(buildHeaderPanel(), gc);

        gc.gridy = 1;
        root.add(makeSeparator(), gc);

        gc.gridy = 2;
        root.add(buildUserButtonBar(), gc);

        gc.gridy = 3;
        root.add(makeSeparator(), gc);

        gc.gridy = 4; gc.fill = GridBagConstraints.BOTH; gc.weighty = 1.0;
        root.add(buildContentSplit(), gc);

        refreshUi();
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

        JLabel header = new JLabel("Users");
        header.setForeground(BURP_ORANGE); header.setFont(HEADER_FONT);

        JLabel desc = new JLabel("Create user profiles and define request-matching conditions to route traffic per user.");
        desc.setFont(LABEL_FONT);

        gc.gridy = 0; gc.insets = new Insets(0, 0, 4, 0); panel.add(header, gc);
        gc.gridy = 1; gc.insets = new Insets(0, 0, 0, 0); panel.add(desc,   gc);
        return panel;
    }

    // =========================================================================
    // User management button bar
    // =========================================================================
    private JPanel buildUserButtonBar() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(6, 16, 6, 16));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.fill = GridBagConstraints.NONE; gc.anchor = GridBagConstraints.WEST; gc.weighty = 0;

        gc.gridx = 0; gc.insets = new Insets(0, 0, 0, 8);
        panel.add(buildAddUserButton(), gc);
        gc.gridx = 1; gc.insets = new Insets(0, 0, 0, 8);
        panel.add(buildRenameUserButton(), gc);
        gc.gridx = 2; gc.insets = new Insets(0, 0, 0, 0);
        panel.add(buildDeleteUserButton(), gc);

        // Filler so buttons stay left-aligned
        gc.gridx = 3; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(), gc);

        return panel;
    }

    // =========================================================================
    // Main content split: user list (left) | matcher editor (right)
    // =========================================================================
    private JSplitPane buildContentSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildUserListPanel(), buildMatcherEditorPanel());
        split.setResizeWeight(0.20);
        split.setDividerLocation(200);
        split.setOneTouchExpandable(false);
        split.setDividerSize(4);
        return split;
    }

    // =========================================================================
    // User list panel (left side of split)
    // =========================================================================
    private JPanel buildUserListPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(8, 16, 8, 8));
        panel.setMinimumSize(new Dimension(160, 200));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0; gc.anchor = GridBagConstraints.NORTHWEST;

        JLabel lbl = new JLabel("User Profiles");
        lbl.setForeground(BURP_ORANGE); lbl.setFont(SUB_FONT);
        gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.weighty = 0; gc.insets = new Insets(0, 0, 6, 0);
        panel.add(lbl, gc);

        userList = new JList<String>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() || updatingUi) return;
                String selected = userList.getSelectedValue();
                if (selected != null) { activateProfile(selected, false); refreshUi(); }
            }
        });

        JScrollPane scroll = new JScrollPane(userList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(180, 300));

        gc.gridy = 1; gc.fill = GridBagConstraints.BOTH; gc.weighty = 1.0; gc.insets = new Insets(0, 0, 0, 0);
        panel.add(scroll, gc);

        return panel;
    }

    // =========================================================================
    // Matcher editor panel (right side of split)
    // =========================================================================
    private JPanel buildMatcherEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(8, 8, 8, 16));
        panel.setMinimumSize(new Dimension(400, 200));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0;

        // Section header
        JLabel lbl = new JLabel("Request Matchers");
        lbl.setForeground(BURP_ORANGE); lbl.setFont(SUB_FONT);
        gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.weighty = 0; gc.insets = new Insets(0, 0, 4, 0);
        panel.add(lbl, gc);

        // Help text
        JTextArea helpLabel = new JTextArea(
                "Add a plain string or regex to match against the full request. "
                + "Matching is case-sensitive.");
        helpLabel.setEditable(false); helpLabel.setOpaque(false); helpLabel.setFocusable(false);
        helpLabel.setLineWrap(true); helpLabel.setWrapStyleWord(true); helpLabel.setFont(LABEL_FONT);
        helpLabel.setBorder(new EmptyBorder(0, 0, 6, 0));
        gc.gridy = 1; gc.insets = new Insets(0, 0, 6, 0);
        panel.add(helpLabel, gc);

        // Input row: label | text field | checkbox
        JPanel inputRow = new JPanel(new GridBagLayout());
        GridBagConstraints ir = new GridBagConstraints();
        ir.gridy = 0; ir.fill = GridBagConstraints.HORIZONTAL;

        JLabel conditionLabel = new JLabel("String:");
        conditionLabel.setFont(LABEL_FONT);
        regexBox = new JCheckBox("Regex");
        matchField = new JTextField();
        matchField.setPreferredSize(new Dimension(220, 28));

        ir.gridx = 0; ir.weightx = 0; ir.insets = new Insets(0, 0, 0, 6); inputRow.add(conditionLabel, ir);
        ir.gridx = 1; ir.weightx = 1; ir.insets = new Insets(0, 0, 0, 6); inputRow.add(matchField, ir);
        ir.gridx = 2; ir.weightx = 0; ir.insets = new Insets(0, 0, 0, 0); inputRow.add(regexBox, ir);

        gc.gridy = 2; gc.insets = new Insets(0, 0, 6, 0);
        panel.add(inputRow, gc);

        // Button row
        JButton addMatcher = new JButton("Add Condition");
        addMatcher.setFont(BUTTON_FONT);
        addMatcher.setForeground(Color.WHITE);
        addMatcher.setBackground(BURP_ORANGE2);
        addMatcher.setOpaque(true); addMatcher.setBorderPainted(false); addMatcher.setFocusPainted(false);

        deleteMatcherButton = new JButton("Delete Condition");
        deleteMatcherButton.setFont(BUTTON_FONT);

        JPanel buttonRow = new JPanel(new GridBagLayout());
        GridBagConstraints br = new GridBagConstraints();
        br.gridy = 0; br.fill = GridBagConstraints.NONE; br.anchor = GridBagConstraints.WEST;
        br.gridx = 0; br.insets = new Insets(0, 0, 0, 8); buttonRow.add(addMatcher, br);
        br.gridx = 1; br.insets = new Insets(0, 0, 0, 0); buttonRow.add(deleteMatcherButton, br);
        br.gridx = 2; br.weightx = 1; br.fill = GridBagConstraints.HORIZONTAL; buttonRow.add(new JLabel(), br);

        gc.gridy = 3; gc.insets = new Insets(0, 0, 8, 0);
        panel.add(buttonRow, gc);

        // Matcher table
        matcherTable = new JTable(matcherTableModel);
        matcherTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matcherTable.setRowSelectionAllowed(true);
        matcherTable.setColumnSelectionAllowed(false);
        matcherTable.setFillsViewportHeight(true);
        matcherTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                int sel = matcherTable.getSelectedRow();
                if (deleteMatcherButton != null) deleteMatcherButton.setEnabled(sel >= 0);
            }
        });

        int typeColWidth = matcherTable.getFontMetrics(matcherTable.getFont()).stringWidth("Regex") + 20;
        if (matcherTable.getColumnModel().getColumnCount() > 0) {
            matcherTable.getColumnModel().getColumn(0).setPreferredWidth(typeColWidth);
            matcherTable.getColumnModel().getColumn(0).setMaxWidth(typeColWidth);
            matcherTable.getColumnModel().getColumn(0).setMinWidth(typeColWidth);
        }

        JScrollPane matcherScroll = new JScrollPane(matcherTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        matcherScroll.setPreferredSize(new Dimension(500, 260));
        matcherScroll.setMinimumSize(new Dimension(300, 160));

        gc.gridy = 4; gc.fill = GridBagConstraints.BOTH; gc.weighty = 1.0; gc.insets = new Insets(0, 0, 0, 0);
        panel.add(matcherScroll, gc);

        // ── Listeners ─────────────────────────────────────────────────────────
        addMatcher.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                UserProfile profile = profiles.get(getSelectedProfileName());
                if (profile == null) return;
                String expression = matchField.getText();
                if (expression == null || expression.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(panel, "Condition text cannot be empty.", "Empty Condition", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                profile.getMatchers().add(new UserRequestMatcher(
                        regexBox.isSelected() ? "Regex" : "Text", expression.trim(), false));
                matchField.setText("");
                refreshMatcherEditor();
            }
        });

        deleteMatcherButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                UserProfile profile = profiles.get(getSelectedProfileName());
                if (profile == null) return;
                int selectedRow = matcherTable == null ? -1 : matcherTable.getSelectedRow();
                if (selectedRow < 0 || selectedRow >= profile.getMatchers().size()) return;
                profile.getMatchers().remove(selectedRow);
                refreshMatcherEditor();
            }
        });

        return panel;
    }

    // =========================================================================
    // User selector panel (used elsewhere in the extension's tab bar)
    // =========================================================================
    public JPanel createUserSelectorPanel() {
        BurpExtender.log("createUserSelectorPanel start profiles=" + profiles.keySet() + " selectedProfile=" + selectedProfileName);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(0, 8, 0, 0));
        panel.setOpaque(false);

        userCombo = new JComboBox<String>(userComboModel);
        userCombo.setMinimumSize(new Dimension(160, 28));
        userCombo.setPreferredSize(new Dimension(160, 28));
        userCombo.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (updatingUi) return;
                String selected = (String) userCombo.getSelectedItem();
                if (selected != null) activateProfile(selected, false);
            }
        });

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.fill = GridBagConstraints.NONE; gc.anchor = GridBagConstraints.WEST;
        panel.add(userCombo, gc);

        refreshUserSelectorState();
        BurpExtender.log("createUserSelectorPanel end comboCount=" + userComboModel.getSize());
        return panel;
    }

    // =========================================================================
    // Buttons
    // =========================================================================
    private JButton buildAddUserButton() {
        JButton addUser = new JButton("Add User");
        addUser.setFont(BUTTON_FONT);
        addUser.setForeground(Color.WHITE);
        addUser.setBackground(BURP_ORANGE2);
        addUser.setOpaque(true); addUser.setBorderPainted(false); addUser.setFocusPainted(false);
        addUser.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String name = JOptionPane.showInputDialog(null, "User name:", "Add User", JOptionPane.PLAIN_MESSAGE);
                if (name == null || name.trim().isEmpty()) return;
                name = name.trim();
                if (profiles.containsKey(name)) {
                    JOptionPane.showMessageDialog(null, "A user with that name already exists.", "Duplicate User", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                UserProfile profile = new UserProfile(name);
                profile.setSnapshot(new JSONObject());
                profiles.put(name, profile);
                selectedProfileName = name;
                refreshUi();
                loadSnapshotIntoGlobals(profile.getSnapshot(), callbacks);
            }
        });
        return addUser;
    }

    private JButton buildDeleteUserButton() {
        deleteUserButton = new JButton("Delete User");
        deleteUserButton.setFont(BUTTON_FONT);
        deleteUserButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String selected = userList == null ? null : userList.getSelectedValue();
                if (selected == null || !profiles.containsKey(selected)) return;
                UserProfile profile = profiles.get(selected);
                if (profile != null && !profile.getMatchers().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Delete the user's conditions before removing the user.", "User Has Conditions", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(null, "Delete user \"" + selected + "\"?", "Delete User", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
                profiles.remove(selected);
                if (profiles.isEmpty()) {
                    selectedProfileName = null;
                    refreshUi();
                    loadSnapshotIntoGlobals(null, callbacks);
                    return;
                }
                ensureDefaultProfile();
                String nextName = selectedProfileName != null && profiles.containsKey(selectedProfileName)
                        ? selectedProfileName : profiles.keySet().iterator().next();
                selectedProfileName = nextName;
                refreshUi();
                loadSnapshotIntoGlobals(profiles.get(nextName).getSnapshot(), callbacks);
            }
        });
        return deleteUserButton;
    }

    private JButton buildRenameUserButton() {
        JButton renameUser = new JButton("Rename User");
        renameUser.setFont(BUTTON_FONT);
        renameUser.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String selected = userList == null ? null : userList.getSelectedValue();
                if (selected == null || !profiles.containsKey(selected)) return;
                String newName = JOptionPane.showInputDialog(null, "New name:", selected);
                if (newName == null || newName.trim().isEmpty() || newName.trim().equals(selected)) return;
                newName = newName.trim();
                if (profiles.containsKey(newName)) {
                    JOptionPane.showMessageDialog(null, "A user with that name already exists.", "Duplicate User", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                UserProfile profile = profiles.remove(selected);
                profile.setName(newName);
                profiles.put(newName, profile);
                if (selected.equals(selectedProfileName)) selectedProfileName = newName;
                refreshUi();
            }
        });
        renameUserButton = renameUser;
        return renameUser;
    }

    // =========================================================================
    // All static methods and business logic — completely unchanged
    // =========================================================================
    public static String getActiveProfileName() { ensureDefaultProfile(); return selectedProfileName; }

    public static List<String> getUserNames() {
        ensureDefaultProfile();
        BurpExtender.log("getUserNames profiles=" + profiles.keySet());
        return new ArrayList<String>(profiles.keySet());
    }

    public static JSONArray exportUsers() {
        ensureDefaultProfile();
        JSONArray users = new JSONArray();
        BurpExtender.log("exportUsers start count=" + profiles.size() + " selectedProfile=" + selectedProfileName);
        for (UserProfile profile : profiles.values()) {
            JSONObject user = new JSONObject();
            user.put("name", profile.getName());
            user.put("snapshot", profile.getSnapshot());
            user.put("matchers", exportMatchers(profile));
            user.put("matchLogic", profile.isMatchAllConditions() ? "AND" : "OR");
            users.add(user);
        }
        BurpExtender.log("exportUsers end exported=" + users.size());
        return users;
    }

    public static void saveSingleUserProfile(IBurpExtenderCallbacks callbacks) {
        BurpExtender.log("saveSingleUserProfile start currentSelectedProfile=" + selectedProfileName);
        singleUserProfile.setSnapshot(snapshotCurrentState(callbacks));
        BurpExtender.log("saveSingleUserProfile snapshotKeys=" + singleUserProfile.getSnapshot().keySet());
    }

    public static void saveSelectedProfileProfile(IBurpExtenderCallbacks callbacks) {
        String selectedProfile = getSelectedProfileName();
        BurpExtender.log("saveSelectedProfileProfile start selectedProfile=" + selectedProfile);
        if (selectedProfile == null) { BurpExtender.log("saveSelectedProfileProfile skipped: no selected multi-user profile"); return; }
        UserProfile profile = profiles.get(selectedProfile);
        if (profile == null) { BurpExtender.log("saveSelectedProfileProfile skipped: profile missing " + selectedProfile); return; }
        profile.setSnapshot(snapshotCurrentState(callbacks));
        BurpExtender.log("saveSelectedProfileProfile snapshotKeys=" + profile.getSnapshot().keySet());
    }

    public static void loadSingleUserProfile(IBurpExtenderCallbacks callbacks) {
        BurpExtender.log("loadSingleUserProfile hasSnapshot=" + (singleUserProfile.getSnapshot() != null));
        loadSnapshotIntoGlobals(singleUserProfile.getSnapshot(), callbacks);
    }

    public static void loadSelectedProfileProfile(IBurpExtenderCallbacks callbacks) {
        String selectedProfile = getSelectedProfileName();
        BurpExtender.log("loadSelectedProfileProfile start selectedProfile=" + selectedProfile);
        if (selectedProfile == null) { clearCurrentState(); return; }
        UserProfile profile = profiles.get(selectedProfile);
        if (profile == null) { clearCurrentState(); return; }
        loadSnapshotIntoGlobals(profile.getSnapshot(), callbacks);
        BurpExtender.log("loadSelectedProfileProfile loaded snapshotKeys=" + profile.getSnapshot().keySet());
    }

    public static JSONObject getSingleUserProfileSnapshot() { return singleUserProfile.getSnapshot(); }

    public static void setSingleUserProfileSnapshot(JSONObject snapshot) {
        BurpExtender.log("setSingleUserProfileSnapshot incomingKeys=" + (snapshot == null ? "null" : snapshot.keySet()));
        singleUserProfile.setSnapshot(snapshot == null ? new JSONObject() : snapshot);
    }

    public static void importUsers(JSONArray usersArray, IBurpExtenderCallbacks callbacks) {
        profiles.clear(); selectedProfileName = null;
        BurpExtender.log("importUsers start count=" + (usersArray == null ? 0 : usersArray.size()));
        if (usersArray != null) {
            for (Object item : usersArray) {
                if (!(item instanceof JSONObject)) continue;
                JSONObject jsonObject = (JSONObject) item;
                String name = (String) jsonObject.get("name");
                if (name == null || name.trim().isEmpty()) continue;
                UserProfile profile = new UserProfile(name.trim());
                Object snapshot = jsonObject.get("snapshot");
                if (snapshot instanceof JSONObject) profile.setSnapshot((JSONObject) snapshot);
                importMatchers(profile, (JSONArray) jsonObject.get("matchers"));
                Object matchLogic = jsonObject.get("matchLogic");
                if (matchLogic instanceof String) profile.setMatchAllConditions(!"OR".equalsIgnoreCase((String) matchLogic));
                profiles.put(profile.getName(), profile);
            }
        }
        if (selectedProfileName == null || !profiles.containsKey(selectedProfileName))
            selectedProfileName = profiles.isEmpty() ? null : profiles.keySet().iterator().next();
        BurpExtender.log("importUsers end selectedProfile=" + selectedProfileName);
        if (uiInstance != null) uiInstance.refreshUi();
    }

    public static JSONObject snapshotCurrentState(IBurpExtenderCallbacks callbacks) {
        BurpExtender.log("snapshotCurrentState start selectedProfile=" + selectedProfileName);
        JSONObject snapshot = new ExportATOR(callbacks).exportCurrentProfile();
        BurpExtender.log("snapshotCurrentState end keys=" + snapshot.keySet());
        return snapshot;
    }

    public static void loadSnapshotIntoGlobals(JSONObject snapshot, IBurpExtenderCallbacks callbacks) {
        BurpExtender.log("loadSnapshotIntoGlobals start hasSnapshot=" + (snapshot != null));
        ImportATOR.beginProfileRestore();
        try {
            clearCurrentState();
            if (snapshot == null) { BurpExtender.log("loadSnapshotIntoGlobals cleared empty snapshot"); return; }
            JSONObject errorCondition = (JSONObject) snapshot.get("errorCondition");
            JSONObject obtainToken = (JSONObject) snapshot.get("obtainToken");
            JSONObject errorConditionReplacement = (JSONObject) snapshot.get("errorConditionReplacement");
            JSONObject uiState = (JSONObject) snapshot.get("uiState");
            ImportATOR importATOR = new ImportATOR(callbacks);
            importATOR.parseErrorCondition(errorCondition);
            importATOR.parseObtainToken(obtainToken);
            importATOR.parseErrorConditionReplacement(errorConditionReplacement);
            importATOR.parseUiState(uiState);
            ObtainPanel.syncSelectedMessageEditors();
            BurpExtender.log("loadSnapshotIntoGlobals applied snapshot keys=" + snapshot.keySet());
        } finally {
            ImportATOR.endProfileRestore();
            BurpExtender.log("loadSnapshotIntoGlobals end");
        }
    }

    public static void runWithProfile(String name, IBurpExtenderCallbacks callbacks, Runnable action) {
        ensureDefaultProfile();
        if (action == null || name == null || !profiles.containsKey(name)) {
            BurpExtender.log("runWithProfile skipped name=" + name); return;
        }
        String previousProfileName = selectedProfileName;
        BurpExtender.log("runWithProfile enter target=" + name + " previous=" + previousProfileName);
        if (name.equals(previousProfileName)) {
            action.run(); profiles.get(name).setSnapshot(snapshotCurrentState(callbacks)); return;
        }
        if (previousProfileName != null && profiles.containsKey(previousProfileName) && !previousProfileName.equals(name))
            profiles.get(previousProfileName).setSnapshot(snapshotCurrentState(callbacks));
        UserProfile targetProfile = profiles.get(name);
        if (targetProfile == null) { BurpExtender.log("runWithProfile target missing=" + name); return; }
        loadSnapshotIntoGlobals(targetProfile.getSnapshot(), callbacks);
        selectedProfileName = name;
        if (uiInstance != null) uiInstance.refreshUi();
        try {
            action.run();
        } finally {
            targetProfile.setSnapshot(snapshotCurrentState(callbacks));
            if (uiInstance != null) uiInstance.refreshUi();
        }
    }

    public static void activateImportedProfile(String name, IBurpExtenderCallbacks callbacks) {
        ensureDefaultProfile();
        if (name == null || !profiles.containsKey(name)) return;
        selectedProfileName = name;
        loadSnapshotIntoGlobals(profiles.get(name).getSnapshot(), callbacks);
        if (uiInstance != null) uiInstance.refreshUi();
    }

    public void activateProfile(String name, boolean updateUi) {
        ensureDefaultProfile();
        if (name == null || !profiles.containsKey(name)) return;
        String currentName = selectedProfileName;
        if (name.equals(currentName)) { if (updateUi) refreshUi(); return; }
        if (currentName != null && profiles.containsKey(currentName) && !currentName.equals(name))
            profiles.get(currentName).setSnapshot(snapshotCurrentState(callbacks));
        selectedProfileName = name;
        loadSnapshotIntoGlobals(profiles.get(name).getSnapshot(), callbacks);
        if (updateUi) refreshUi();
    }

    private boolean profileHasMatchers(String name) {
        UserProfile profile = name == null ? null : profiles.get(name);
        return profile != null && !profile.getMatchers().isEmpty();
    }

    public static List<String> getProfilesWithoutMatchers() {
        ensureDefaultProfile();
        List<String> missingProfiles = new ArrayList<String>();
        for (UserProfile profile : profiles.values()) {
            if (profile == null) continue;
            String name = profile.getName();
            if (name == null || DEFAULT_PROFILE_NAME.equals(name)) continue;
            if (profile.getMatchers().isEmpty()) missingProfiles.add(name);
        }
        return missingProfiles;
    }

    public static boolean hasProfilesWithoutMatchers() {
        return !getProfilesWithoutMatchers().isEmpty();
    }

    public static String resolveProfileForMessage(IHttpRequestResponse messageInfo, boolean messageIsRequest) {
        return resolveProfileForMessage(messageInfo, messageIsRequest, false);
    }

    public static String resolveProfileForMessageStrict(IHttpRequestResponse messageInfo, boolean messageIsRequest) {
        return resolveProfileForMessage(messageInfo, messageIsRequest, true);
    }

    private static String resolveProfileForMessage(IHttpRequestResponse messageInfo, boolean messageIsRequest, boolean strictMode) {
        ensureDefaultProfile();
        if (messageInfo == null) return strictMode ? null : selectedProfileName;
        String comment = messageInfo.getComment();
        if (!messageIsRequest && comment != null && comment.startsWith(COMMENT_PREFIX)) {
            String userName = comment.substring(COMMENT_PREFIX.length());
            if (profiles.containsKey(userName)) return userName;
        }
        for (UserProfile profile : profiles.values()) {
            if (profile.getMatchers().isEmpty()) continue;
            boolean matchAll = profile.isMatchAllConditions();
            boolean matchedAny = false, matchedAll = true;
            for (UserRequestMatcher matcher : profile.getMatchers()) {
                boolean matched = matcher.matches(messageInfo, messageIsRequest);
                matchedAny = matchedAny || matched;
                if (!matched) matchedAll = false;
                if (matchAll && !matched) break;
                if (!matchAll && matched) break;
            }
            boolean profileMatched = matchAll ? matchedAll : matchedAny;
            if (profileMatched) {
                if (messageIsRequest) messageInfo.setComment(COMMENT_PREFIX + profile.getName());
                return profile.getName();
            }
        }
        return strictMode ? null : selectedProfileName;
    }

    public void snapshotActiveProfile() {
        ensureDefaultProfile();
        UserProfile profile = profiles.get(selectedProfileName);
        if (profile != null) profile.setSnapshot(new ExportATOR(callbacks).exportCurrentProfile());
    }

    // =========================================================================
    // UI refresh helpers
    // =========================================================================
    private void refreshUi() {
        BurpExtender.log("refreshUi start selectedProfile=" + getSelectedProfileName());
        updatingUi = true;
        try {
            userListModel.clear(); userComboModel.removeAllElements();
            for (String name : profiles.keySet()) { userListModel.addElement(name); userComboModel.addElement(name); }
            String current = getSelectedProfileName();
            if (userList != null)  userList.setSelectedValue(current, true);
            if (userCombo != null) userCombo.setSelectedItem(current);
        } finally {
            updatingUi = false;
        }
        refreshUserSelectorState();
        refreshMatcherEditor();
        if (userList  != null) { userList.revalidate();  userList.repaint(); }
        if (userCombo != null) { userCombo.revalidate(); userCombo.repaint(); }
    }

    public void refreshUserSelectorState() {
        boolean hasSelection   = userList != null && userList.getSelectedValue() != null;
        boolean hasDefinedUser = profiles.size() > 1;
        boolean multiUserMode = extender != null && extender.isMultiUserMode();
        if (userCombo != null) {
            userCombo.setEnabled(multiUserMode && hasDefinedUser && userComboModel.getSize() > 0);
            userCombo.setMaximumSize(userCombo.getPreferredSize());
            adjustUserComboWidth();
        }
        if (deleteUserButton != null)
            deleteUserButton.setEnabled(hasSelection && profiles.containsKey(userList.getSelectedValue())
                    && !profileHasMatchers(userList.getSelectedValue()));
        if (renameUserButton != null)
            renameUserButton.setEnabled(hasSelection);
    }

    private void adjustUserComboWidth() {
        if (userCombo == null) return;
        int minWidth = 180, bestWidth = minWidth;
        for (int i = 0; i < userComboModel.getSize(); i++) {
            Object val = userComboModel.getElementAt(i); if (val == null) continue;
            Component r = userCombo.getRenderer().getListCellRendererComponent(new JList<String>(), String.valueOf(val), i, false, false);
            bestWidth = Math.max(bestWidth, r.getPreferredSize().width + 40);
        }
        userCombo.setPreferredSize(new Dimension(bestWidth, 28));
        userCombo.setMaximumSize(new Dimension(Math.max(bestWidth, minWidth + 120), 28));
    }

    private void refreshMatcherEditor() {
        UserProfile profile = profiles.get(getSelectedProfileName());
        matcherTableModel.setProfile(profile);
        if (deleteMatcherButton != null)
            deleteMatcherButton.setEnabled(profile != null && matcherTable != null && matcherTable.getSelectedRow() >= 0);
        if (matchField != null) matchField.setText(matchField.getText() == null ? "" : matchField.getText());
        matcherTableModel.fireTableDataChanged();
        if (matcherTable != null) { matcherTable.revalidate(); matcherTable.repaint(); }
        refreshUserSelectorState();
    }

    // =========================================================================
    // Static helpers
    // =========================================================================
    private static void ensureDefaultProfile() {
        if (selectedProfileName == null || !profiles.containsKey(selectedProfileName))
            selectedProfileName = profiles.isEmpty() ? null : profiles.keySet().iterator().next();
    }

    private static String getSelectedProfileName() {
        ensureDefaultProfile();
        if (selectedProfileName != null && profiles.containsKey(selectedProfileName)) return selectedProfileName;
        selectedProfileName = profiles.isEmpty() ? null : profiles.keySet().iterator().next();
        return selectedProfileName;
    }

    private static JSONArray exportMatchers(UserProfile profile) {
        JSONArray array = new JSONArray();
        for (UserRequestMatcher matcher : profile.getMatchers()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("matchType",  matcher.getMatchType());
            jsonObject.put("expression", matcher.getExpression());
            jsonObject.put("ignoreCase", matcher.isIgnoreCase());
            array.add(jsonObject);
        }
        return array;
    }

    private static void importMatchers(UserProfile profile, JSONArray matchers) {
        profile.getMatchers().clear();
        if (matchers == null) return;
        for (Object item : matchers) {
            if (!(item instanceof JSONObject)) continue;
            JSONObject jsonObject = (JSONObject) item;
            String matchType = (String) jsonObject.get("matchType");
            if (matchType == null) matchType = "Regex";
            String expression = (String) jsonObject.get("expression");
            if (expression == null) expression = (String) jsonObject.get("regex");
            boolean ignoreCase = Boolean.TRUE.equals(jsonObject.get("ignoreCase"));
            profile.getMatchers().add(new UserRequestMatcher(matchType, expression, ignoreCase));
            Object logic = jsonObject.get("matchLogic");
            if (logic instanceof String) profile.setMatchAllConditions(!"OR".equalsIgnoreCase((String) logic));
        }
    }

    private static void clearCurrentState() {
        BurpExtender.log("clearCurrentState start");
        if (ErrorPanel.ireqMessageEditor != null) ErrorPanel.ireqMessageEditor.setMessage(new byte[0], true);
        if (ErrorPanel.iresMessageEditor != null) ErrorPanel.iresMessageEditor.setMessage(new byte[0], false);
        if (ReplacePanel.ireqMessageEditor != null) ReplacePanel.ireqMessageEditor.setMessage(new byte[0], true);
        if (ReplacePanel.iresMessageEditor != null) ReplacePanel.iresMessageEditor.setMessage(new byte[0], false);
        if (PreviewPanel.ireqMessageEditor != null) PreviewPanel.ireqMessageEditor.setMessage(new byte[0], true);
        if (PreviewPanel.iresMessageEditor != null) PreviewPanel.iresMessageEditor.setMessage(new byte[0], false);
        if (PreviewPanel.ireqatorMessageEditor != null) PreviewPanel.ireqatorMessageEditor.setMessage(new byte[0], true);
        if (PreviewPanel.iresatorMessageEditor != null) PreviewPanel.iresatorMessageEditor.setMessage(new byte[0], false);
        if (PreviewPanel.ireqmodifiedMessageEditor != null) PreviewPanel.ireqmodifiedMessageEditor.setMessage(new byte[0], true);
        if (PreviewPanel.iresmodifiedMessageEditor != null) PreviewPanel.iresmodifiedMessageEditor.setMessage(new byte[0], false);

        ErrorPanel.errorEntrylist.clear();
        ObtainPanel.obtainEntrylist.clear();
        ObtainPanel.extractionEntrylist.clear();
        ObtainPanel.replacementEntrylist.clear();
        ReplacePanel.replaceEntrylist.clear();
        ReplacePanel.multipleErrorConditions.clear();
        ReplacePanel.secondscrollPanel.removeAll();
        ReplacePanel.secondscrollPanel.revalidate();
        ReplacePanel.secondscrollPanel.repaint();
        ReplacePanel.triggerConditionNameCombo.removeAllItems();
        ObtainPanel.extractionListComboBox.removeAllItems();
        ObtainPanel.extractionListComboBox.addItem("");
        ObtainPanel.extractionListComboBox.addItem("NA");
        ReplacePanel.extractionreplaceComboNameList.removeAllItems();
        ReplacePanel.extractionreplaceComboNameList.addItem("NA");
        PreviewPanel.previewEntryList.clear();
        if (PreviewPanel.conditionDetails != null) PreviewPanel.conditionDetails.setText("");
        ObtainPanel.clearSelectedMessageEditors();
        if (ErrorPanel.errorTableModel   != null) ErrorPanel.errorTableModel.fireTableDataChanged();
        if (ObtainPanel.obtainTableModel != null) ObtainPanel.obtainTableModel.fireTableDataChanged();
        if (ObtainPanel.extractionTableModel  != null) ObtainPanel.extractionTableModel.fireTableDataChanged();
        if (ObtainPanel.replacementTableModel != null) ObtainPanel.replacementTableModel.fireTableDataChanged();
        if (ReplacePanel.replaceTableModel != null) ReplacePanel.replaceTableModel.fireTableDataChanged();
        if (PreviewPanel.previewTableModel != null) PreviewPanel.previewTableModel.fireTableDataChanged();
        BurpExtender.log("clearCurrentState end");
    }

    // =========================================================================
    // Separator helper
    // =========================================================================
    private JPanel makeSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        JPanel band = new JPanel(new BorderLayout());
        band.setBorder(new EmptyBorder(0, 16, 0, 16));
        band.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        band.setPreferredSize(new Dimension(100, 2));
        band.setMinimumSize(new Dimension(0, 2));
        band.add(sep, BorderLayout.CENTER);
        return band;
    }

    // =========================================================================
    // Inner table model
    // =========================================================================
    private class UserMatcherTableModel extends AbstractTableModel {
        private UserProfile profile;

        public void setProfile(UserProfile profile) { this.profile = profile; }

        @Override public int getRowCount()    { return profile == null ? 0 : profile.getMatchers().size(); }
        @Override public int getColumnCount() { return 2; }

        @Override public String getColumnName(int column) {
            switch (column) { case 0: return "Type"; case 1: return "Condition"; default: return super.getColumnName(column); }
        }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            if (profile == null || rowIndex < 0 || rowIndex >= profile.getMatchers().size()) return null;
            UserRequestMatcher matcher = profile.getMatchers().get(rowIndex);
            switch (columnIndex) { case 0: return matcher.getMatchType(); case 1: return matcher.getExpression(); default: return null; }
        }
    }
}