package burp;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Font;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class SetttingsTab {
    private static boolean repeaterEnabled = true;
    private static boolean intruderEnabled = true;
    private static boolean scannerEnabled = true;
    private static boolean sequencerEnabled = true;
    private static boolean spiderEnabled = true;
    private static boolean proxyEnabled = false;
    private static boolean extenderEnabled = false;
    private static boolean inScopeEnabled = false;
	private static JCheckBox boxRepeater;
    private static JCheckBox boxIntruder;
    private static JCheckBox boxScanner;
    private static JCheckBox boxSequencer;
    private static JCheckBox boxSpider;
    private static JCheckBox boxProxy;
    private static JCheckBox boxExtender;
    public static JCheckBox inScope;
    static Color BURP_ORANGE = new Color(255, 128, 0);
    private Font headerFont = new Font("Nimbus", Font.BOLD, 13);
    private JButton exportATOR;
    private JButton importATOR;
    
    IBurpExtenderCallbacks callbacks;
	public SetttingsTab(IBurpExtenderCallbacks callbacks) {
		this.callbacks = callbacks;
	}
	
    public JPanel initSettingsGui(){
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(new EmptyBorder(10, 12, 12, 12));
		
        	boxRepeater = new JCheckBox("Repeater", repeaterEnabled);
            boxIntruder = new JCheckBox("Intruder", intruderEnabled);
            boxScanner = new JCheckBox("Scanner", scannerEnabled);
            boxSequencer = new JCheckBox("Sequencer", sequencerEnabled);
            boxSpider = new JCheckBox("Spider", spiderEnabled);
            boxProxy = new JCheckBox("Proxy", proxyEnabled);
            boxExtender = new JCheckBox("Extender", extenderEnabled);
            inScope = new JCheckBox("InScope", inScopeEnabled);

            boxRepeater.addItemListener(e -> repeaterEnabled = boxRepeater.isSelected());
            boxIntruder.addItemListener(e -> intruderEnabled = boxIntruder.isSelected());
            boxScanner.addItemListener(e -> scannerEnabled = boxScanner.isSelected());
            boxSequencer.addItemListener(e -> sequencerEnabled = boxSequencer.isSelected());
            boxSpider.addItemListener(e -> spiderEnabled = boxSpider.isSelected());
            boxProxy.addItemListener(e -> proxyEnabled = boxProxy.isSelected());
            boxExtender.addItemListener(e -> extenderEnabled = boxExtender.isSelected());
            inScope.addItemListener(e -> inScopeEnabled = inScope.isSelected());
        

        JLabel header1 = new JLabel("Tools scope");
        header1.setAlignmentX(Component.LEFT_ALIGNMENT);
        header1.setForeground(BURP_ORANGE);
        header1.setFont(headerFont);
        header1.setBorder(new EmptyBorder(5, 0, 5, 0));

        JLabel label2 = new JLabel("Select the tools that the pre-request macro will be applied to.");
        label2.setAlignmentX(Component.LEFT_ALIGNMENT);
        label2.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Scope
        JPanel scopePanel = new JPanel();
        scopePanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        scopePanel.setLayout(new GridLayout(1, 2, 12, 0));
        scopePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel col1 = new JPanel();
        col1.setLayout(new BoxLayout(col1, BoxLayout.PAGE_AXIS));
        col1.add(boxRepeater);
        col1.add(boxIntruder);
        col1.add(boxSpider);
        col1.add(boxExtender);
        col1.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel col2 = new JPanel();
        col2.setLayout(new BoxLayout(col2, BoxLayout.PAGE_AXIS));
        col2.add(boxScanner);
        col2.add(boxSequencer);
        col2.add(boxProxy);
        col2.add(inScope);
        col2.setAlignmentY(Component.TOP_ALIGNMENT);

        scopePanel.add(col1);
        scopePanel.add(col2);

        exportATOR = new JButton("Export ATOR");
        exportATOR.setEnabled(true);
        exportATOR.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportATOR.addActionListener(new MenuAllListener(callbacks, this, MenuActions.EXPORT_CONFIG));
		
        JLabel importExportTitle = new JLabel("Import/Export config");
        importExportTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        importExportTitle.setForeground(BURP_ORANGE);
        importExportTitle.setFont(headerFont);
        importExportTitle.setBorder(new EmptyBorder(5, 0, 5, 0));
        
        importATOR = new JButton("Import ATOR");
        importATOR.setEnabled(true);
        importATOR.setAlignmentX(Component.LEFT_ALIGNMENT);
        importATOR.addActionListener(new MenuAllListener(callbacks, this, MenuActions.IMPORT_CONFIG));

        JPanel importExportRow = new JPanel(new GridLayout(1, 2, 8, 0));
        importExportRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        importExportRow.add(importATOR);
        importExportRow.add(exportATOR);
        
        // Put it all together
        JPanel confPanel = new JPanel();
        confPanel.setLayout(new BoxLayout(confPanel, BoxLayout.Y_AXIS));
        confPanel.setBorder(new EmptyBorder(5, 15, 5, 15));

        confPanel.add(header1);
        confPanel.add(label2);
        confPanel.add(scopePanel);

      
            confPanel.add(importExportTitle);
            confPanel.add(importExportRow);
	        
            settingsPanel.add(confPanel);
	        
            return settingsPanel;
    }
	
	public static boolean isToolEnabled(int toolFlag) {
    	switch (toolFlag) {
            case IBurpExtenderCallbacks.TOOL_INTRUDER:
                    return intruderEnabled;

            case IBurpExtenderCallbacks.TOOL_REPEATER:
                    return repeaterEnabled;

            case IBurpExtenderCallbacks.TOOL_SCANNER:
                    return scannerEnabled;

            case IBurpExtenderCallbacks.TOOL_SEQUENCER:
                    return sequencerEnabled;

            case IBurpExtenderCallbacks.TOOL_SPIDER:
                    return spiderEnabled;

            case IBurpExtenderCallbacks.TOOL_PROXY:
                    return proxyEnabled;
            
            case IBurpExtenderCallbacks.TOOL_EXTENDER:
                    return extenderEnabled;
        }
        return false;
    }

    public static boolean isInScopeEnabled() {
        return inScopeEnabled;
    }
	
	public boolean isEnabledAtLeastOne() {
        return  intruderEnabled ||
                repeaterEnabled ||
                scannerEnabled ||
                sequencerEnabled ||
                proxyEnabled ||
                spiderEnabled ||
                extenderEnabled;
	}
	
	public void setAllTools(boolean enabled) {
        repeaterEnabled = enabled;
        intruderEnabled = enabled;
        scannerEnabled = enabled;
        sequencerEnabled = enabled;
        spiderEnabled = enabled;
        proxyEnabled = enabled;
        extenderEnabled = enabled;
        inScopeEnabled = enabled;
		if (boxRepeater != null) boxRepeater.setSelected(enabled);
		if (boxIntruder != null) boxIntruder.setSelected(enabled);
		if (boxScanner != null) boxScanner.setSelected(enabled);
		if (boxSequencer != null) boxSequencer.setSelected(enabled);
		if (boxSpider != null) boxSpider.setSelected(enabled);
		if (boxProxy != null) boxProxy.setSelected(enabled);
		if (boxExtender != null) boxExtender.setSelected(enabled);
		if (inScope != null) inScope.setSelected(enabled);
    }
	
	
}
