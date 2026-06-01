package burp;

public class FinalErrorCondition {
	public static String addErrorCondition() {
		String condition = "";
		try {
		String conditionname = (String) ReplacePanel.triggerConditionNameCombo.getSelectedItem();
		
		if(conditionname != null && !conditionname.equals("NA")) {
			condition = conditionname;
		}
		
		for(MultipleErrorCondition mulCondition: ReplacePanel.multipleErrorConditions) {
			String name = (String) mulCondition.triggerComboBox.getSelectedItem();
			if(name != null && !name.equals("NA")) {
				String logical = (String) mulCondition.logicalCondition.getSelectedItem();
				if (logical != null) {
					condition += " " + logical + " " + name;
				} else {
					condition += " " + name;
				}
			}
		}
		
		return condition;
		}
		catch(Exception e) {
			BurpExtender.callbacks.printOutput("Exception in adding trigger condition"+ e.getLocalizedMessage());
		}
		return condition;
	}
}
