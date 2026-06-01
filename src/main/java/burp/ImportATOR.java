package burp;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ImportATOR {
	IBurpExtenderCallbacks callbacks;
	private static boolean restoringProfile = false;
	
	public ImportATOR(IBurpExtenderCallbacks callbacks) {
		this.callbacks = callbacks;
	}

	public static void beginProfileRestore() {
		restoringProfile = true;
	}

	public static void endProfileRestore() {
		restoringProfile = false;
	}

	public static boolean isRestoringProfile() {
		return restoringProfile;
	}
	
	
	public void readJSONFile() {
		String filePath = null;
        JFileChooser fileSelector = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory()); 
        int fileChoosenState = fileSelector.showOpenDialog(null); 
        if (fileChoosenState == JFileChooser.APPROVE_OPTION) 
        	filePath = fileSelector.getSelectedFile().getAbsolutePath(); 
        if(filePath != null)
        {
        	JSONParser jsonParser = new JSONParser();
         
	        try (FileReader reader = new FileReader(filePath))
	        {
	        	JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
	        	
	        	JSONObject errorCondition = (JSONObject) jsonObject.get("errorCondition");
	        	
	        	parseErrorCondition(errorCondition);
	        	
	        	JSONObject obtainToken = (JSONObject) jsonObject.get("obtainToken");
	        	parseObtainToken(obtainToken);
	        	JSONObject errorConditionReplacement = (JSONObject) jsonObject.get("errorConditionReplacement");
	        	parseErrorConditionReplacement(errorConditionReplacement);
	        	JSONArray users = (JSONArray) jsonObject.get("users");
	        	UsersTab.importUsers(users, callbacks);
	        	String activeUser = (String) jsonObject.get("activeUser");
	        	if (activeUser != null && !activeUser.trim().isEmpty()) {
	        		UsersTab.activateImportedProfile(activeUser, callbacks);
	        	}
	        	
	        }
	        catch(Exception exp) {
	        	callbacks.printOutput("Exception while importing file.."+ exp.getMessage());
	        }
       }
        
	}
	
	
	public void parseErrorCondition(JSONObject jsonObject) {
		if (jsonObject == null) {
			BurpExtender.log("parseErrorCondition skipped: null");
			return;
		}
		BurpExtender.log("parseErrorCondition start keys=" + jsonObject.keySet());
		try {
			String request = (String) jsonObject.get("request");
			String response = (String) jsonObject.get("response");
			JSONArray jsonArray = (JSONArray) jsonObject.get("errorconditionlist");
			BurpExtender.log("parseErrorCondition values requestLen=" + (request == null ? -1 : request.length()) + " responseLen=" + (response == null ? -1 : response.length()) + " errorCount=" + (jsonArray == null ? -1 : jsonArray.size()));
			
			ErrorPanel.host = (String) jsonObject.get("host");
			Number port = (Number) jsonObject.get("port");
			ErrorPanel.port = port == null ? 0 : port.intValue();
			ErrorPanel.protocol = (String) jsonObject.get("protocol");
			BurpExtender.log("parseErrorCondition service host=" + ErrorPanel.host + " port=" + ErrorPanel.port + " protocol=" + ErrorPanel.protocol);
			
			IHttpServiceImpl iHttpService = new IHttpServiceImpl(ErrorPanel.host, ErrorPanel.port, ErrorPanel.protocol);
			String comment = (String) jsonObject.get("comment");
			String highlight = (String) jsonObject.get("highlight");
			byte[] byterequest = callbacks.getHelpers().stringToBytes(request == null ? "" : request);
			byte[] byteresponse = callbacks.getHelpers().stringToBytes(response == null ? "" : response);
			HttpRequestResponseImpl iHttpRequestResponse = new HttpRequestResponseImpl(byterequest, byteresponse, comment, highlight, iHttpService);
			BurpExtender.log("parseErrorCondition editors present req=" + (ErrorPanel.ireqMessageEditor != null) + " res=" + (ErrorPanel.iresMessageEditor != null) + " replaceReq=" + (ReplacePanel.ireqMessageEditor != null) + " replaceRes=" + (ReplacePanel.iresMessageEditor != null) + " previewReq=" + (PreviewPanel.ireqMessageEditor != null));
			SpotErrorMetaData spotErrorMetaData = new SpotErrorMetaData(iHttpRequestResponse);
			BurpExtender.spoterroMetaData = spotErrorMetaData;

			ErrorPanel.ireqMessageEditor.setMessage(iHttpRequestResponse.getRequest(), true);
			ErrorPanel.iresMessageEditor.setMessage(iHttpRequestResponse.getResponse(), false);
			ReplacePanel.ireqMessageEditor.setMessage(iHttpRequestResponse.getRequest(), true);
			ReplacePanel.iresMessageEditor.setMessage(iHttpRequestResponse.getResponse(), false);
			PreviewPanel.ireqMessageEditor.setMessage(iHttpRequestResponse.getRequest(), true);
			BurpExtender.log("parseErrorCondition seeded editors");
			
			if (jsonArray != null) {
				for(int i=0; i<jsonArray.size(); i++) {
					JSONObject errorCondition = (JSONObject) jsonArray.get(i);
					String catergory = (String) errorCondition.get("Category");
					String description = (String) errorCondition.get("Description");
					String value = (String) errorCondition.get("Value");
					String name = (String) errorCondition.get("Name");
					ErrorEntry errorEntry = new ErrorEntry(name, catergory, value, description);
					ReplacePanel.triggerConditionNameCombo.addItem(errorEntry.getConditionname());
					for(MultipleErrorCondition multipleerrorCondition: ReplacePanel.multipleErrorConditions) {
						multipleerrorCondition.triggerComboBox.addItem(errorEntry.getConditionname());
					}
					ErrorPanel.errorEntrylist.add(errorEntry);	
				}
			}
			ErrorPanel.errorTableModel.fireTableDataChanged();
			BurpExtender.log("parseErrorCondition loaded errorCount=" + (jsonArray == null ? 0 : jsonArray.size()));
		} catch (Exception e) {
			BurpExtender.log("parseErrorCondition exception=" + e.getClass().getSimpleName() + ": " + e.getMessage());
			callbacks.printOutput("[ATOR] parseErrorCondition exception=" + e.getClass().getSimpleName() + ": " + e.getMessage());
			throw e;
		}
		
	}
	
	public void parseObtainToken(JSONObject jsonObject) {
		if (jsonObject == null) {
			BurpExtender.log("parseObtainToken skipped: null");
			return;
		}
		BurpExtender.log("parseObtainToken start keys=" + jsonObject.keySet());
		try {
			JSONArray atorExtendedMacro = (JSONArray) jsonObject.get("Ator");
			JSONArray extractionList = (JSONArray) jsonObject.get("Extraction");
			JSONArray replacementList = (JSONArray) jsonObject.get("Replacement");
			BurpExtender.log("parseObtainToken incoming sizes ator=" + (atorExtendedMacro == null ? 0 : atorExtendedMacro.size()) + " extraction=" + (extractionList == null ? 0 : extractionList.size()) + " replacement=" + (replacementList == null ? 0 : replacementList.size()));

			for (int i = 0; atorExtendedMacro != null && i < atorExtendedMacro.size(); i++) {
				JSONObject atorelement = (JSONObject) atorExtendedMacro.get(i);
				BurpExtender.log("parseObtainToken obtain idx=" + i + " keys=" + atorelement.keySet());
				String httpServicehost = (String) atorelement.get("httpServicehost");
				Number httpServiceportValue = (Number) atorelement.get("httpServiceport");
				String httpServiceprotocol = (String) atorelement.get("httpServiceprotocol");
				String comment = (String) atorelement.get("Comment");
				String highlight = (String) atorelement.get("Highlight");
				String requestText = (String) atorelement.get("request");
				String responseText = (String) atorelement.get("response");
				Number msgIDValue = (Number) atorelement.get("MsgID");
				String host = (String) atorelement.get("Host");
				String method = (String) atorelement.get("Method");
				String url = (String) atorelement.get("URL");
				BurpExtender.log("parseObtainToken obtain idx=" + i + " host=" + httpServicehost + " portType=" + (httpServiceportValue == null ? "null" : httpServiceportValue.getClass().getName()) + " msgIdType=" + (msgIDValue == null ? "null" : msgIDValue.getClass().getName()) + " requestLen=" + (requestText == null ? -1 : requestText.length()) + " responseLen=" + (responseText == null ? -1 : responseText.length()));

				int httpServiceport = httpServiceportValue == null ? 0 : httpServiceportValue.intValue();
				IHttpServiceImpl iHttpService = new IHttpServiceImpl(httpServicehost, httpServiceport, httpServiceprotocol);
				byte[] request = callbacks.getHelpers().stringToBytes(requestText == null ? "" : requestText);
				byte[] response = callbacks.getHelpers().stringToBytes(responseText == null ? "" : responseText);
				HttpRequestResponseImpl iHttpRequestResponse = new HttpRequestResponseImpl(request, response, comment, highlight, iHttpService);
				int msgID = msgIDValue == null ? 0 : msgIDValue.intValue();
				ObtainEntry obtainEntry = new ObtainEntry(msgID, host, method, url, iHttpRequestResponse);
				ObtainPanel.obtainEntrylist.add(obtainEntry);
				if (ObtainPanel.obtainTableModel != null) {
					ObtainPanel.obtainTableModel.fireTableRowsInserted(ObtainPanel.obtainEntrylist.size() - 1, ObtainPanel.obtainEntrylist.size() - 1);
				}
				BurpExtender.log("parseObtainToken added obtain idx=" + i + " msgID=" + msgID + " currentObtainCount=" + ObtainPanel.obtainEntrylist.size());
			}

			for (int j = 0; extractionList != null && j < extractionList.size(); j++) {
				JSONObject extractionJSON = (JSONObject) extractionList.get(j);
				BurpExtender.log("parseObtainToken restoring extraction idx=" + j + " keys=" + extractionJSON.keySet());
				addToExtractionList(extractionJSON);
			}

			for (int k = 0; replacementList != null && k < replacementList.size(); k++) {
				JSONObject replacementJSON = (JSONObject) replacementList.get(k);
				BurpExtender.log("parseObtainToken restoring replacement idx=" + k + " keys=" + replacementJSON.keySet());
				addToReplacementList(replacementJSON);
			}
			BurpExtender.log("parseObtainToken loaded obtainCount=" + (atorExtendedMacro == null ? 0 : atorExtendedMacro.size()) + " extractionCount=" + (extractionList == null ? 0 : extractionList.size()) + " replacementCount=" + (replacementList == null ? 0 : replacementList.size()) + " liveObtainCount=" + ObtainPanel.obtainEntrylist.size() + " liveExtractionCount=" + ObtainPanel.extractionEntrylist.size() + " liveReplacementCount=" + ObtainPanel.replacementEntrylist.size());
		} catch (Exception e) {
			BurpExtender.log("parseObtainToken exception=" + e.getClass().getSimpleName() + ": " + e.getMessage());
			callbacks.printOutput("[ATOR] parseObtainToken exception=" + e.getClass().getSimpleName() + ": " + e.getMessage());
			throw e;
		}
		
	}
	
	
	public void parseErrorConditionReplacement(JSONObject jsonObject) {
		if (jsonObject == null) {
			BurpExtender.log("parseErrorConditionReplacement skipped: null");
			return;
		}
		BurpExtender.log("parseErrorConditionReplacement start keys=" + jsonObject.keySet());
		JSONObject triggerCondition = (JSONObject) jsonObject.get("TriggerCondition");
		if (triggerCondition != null) {
			String mainCondition = (String) triggerCondition.get("MainCondition");
			if (mainCondition != null) {
				ReplacePanel.triggerConditionNameCombo.setSelectedItem(mainCondition);
			}
		}
		JSONArray multipleerrorconditionlist = triggerCondition == null ? null : (JSONArray) triggerCondition.get("multipleerrorcondition");
		if (multipleerrorconditionlist != null) {
			for(int i=0; i<multipleerrorconditionlist.size(); i++) {
				JSONObject condition = (JSONObject) multipleerrorconditionlist.get(i);
				String triggerConditionName = (String) condition.get("triggerConditionName");
				String logicalName = (String) condition.get("logical");
				
				addMultipleTtriggerCondition(logicalName, triggerConditionName);
			}
		}
		
		JSONArray errorConditionReplacementList = (JSONArray) jsonObject.get("ErrorConditionReplacementList");
		if (errorConditionReplacementList != null) {
			for(int j=0;j<errorConditionReplacementList.size();j++) {
				JSONObject replacementEntry = (JSONObject)errorConditionReplacementList.get(j);
				addErrorConditionReplacementList(replacementEntry);
			}
		}
		PreviewPanel.conditionDetails.setText(FinalErrorCondition.addErrorCondition());
		BurpExtender.log("parseErrorConditionReplacement loaded triggerCount=" + (multipleerrorconditionlist == null ? 0 : multipleerrorconditionlist.size()) + " replaceCount=" + (errorConditionReplacementList == null ? 0 : errorConditionReplacementList.size()));
		
	}

	public void parseUiState(JSONObject jsonObject) {
		if (jsonObject == null) {
			BurpExtender.log("parseUiState skipped: null");
			return;
		}
		BurpExtender.log("parseUiState start keys=" + jsonObject.keySet());
		setEditorMessage(ErrorPanel.ireqMessageEditor, (String) jsonObject.get("errorRequest"), true);
		setEditorMessage(ErrorPanel.iresMessageEditor, (String) jsonObject.get("errorResponse"), false);
		setEditorMessage(ReplacePanel.ireqMessageEditor, (String) jsonObject.get("replaceRequest"), true);
		setEditorMessage(ReplacePanel.iresMessageEditor, (String) jsonObject.get("replaceResponse"), false);
		setEditorMessage(PreviewPanel.ireqMessageEditor, (String) jsonObject.get("previewRequest"), true);
		setEditorMessage(PreviewPanel.iresMessageEditor, (String) jsonObject.get("previewResponse"), false);
		setEditorMessage(PreviewPanel.ireqatorMessageEditor, (String) jsonObject.get("previewAtorRequest"), true);
		setEditorMessage(PreviewPanel.iresatorMessageEditor, (String) jsonObject.get("previewAtorResponse"), false);
		setEditorMessage(PreviewPanel.ireqmodifiedMessageEditor, (String) jsonObject.get("previewModifiedRequest"), true);
		setEditorMessage(PreviewPanel.iresmodifiedMessageEditor, (String) jsonObject.get("previewModifiedResponse"), false);
		BurpExtender.log("parseUiState applied ui snapshot");
	}

	private void setEditorMessage(IMessageEditor editor, String value, boolean isRequest) {
		if (editor == null) {
			return;
		}
		if (value == null) {
			value = "";
		}
		editor.setMessage(callbacks.getHelpers().stringToBytes(value), isRequest);
	}
	
	
	public void addErrorConditionReplacementList(JSONObject jsonObject) {
		
		String extractedName = (String) jsonObject.get("Name");
		String extractionListName = (String) jsonObject.get("ExtractionName");
		String replacementIn = (String) jsonObject.get("replacementIn");
		
		ReplaceEntry replaceEntry = new ReplaceEntry(extractedName, extractionListName, replacementIn);
		replaceEntry.startString = (String) jsonObject.get("startString");
		replaceEntry.stopString = (String) jsonObject.get("stopString");
		replaceEntry.selectedText = (String) jsonObject.get("selectedText");
		replaceEntry.headerName = (String) jsonObject.get("headerName");
		ReplacePanel.replaceEntrylist.add(replaceEntry);
		
		ReplacePanel.replaceTableModel.fireTableRowsInserted(ReplacePanel.replaceTableModel.getRowCount() - 1, 
				ReplacePanel.replaceTableModel.getRowCount() - 1);
	}
	
	public void addMultipleTtriggerCondition( String logicalCondition, String triggerName) {
		if (triggerName == null) {
			triggerName = "NA";
		}
		if (logicalCondition == null) {
			logicalCondition = "AND";
		}
		JButton closeButton = new JButton("Close");
		callbacks.customizeUiComponent(closeButton);
		
		JPanel addinnersecondPanel = new JPanel();
		addinnersecondPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		addinnersecondPanel.setBorder(new EmptyBorder(0, 0, 5, 15));
		callbacks.customizeUiComponent(addinnersecondPanel);
		
		MultipleErrorCondition multipleErrorCondition = new MultipleErrorCondition(callbacks);
		ReplacePanel.multipleErrorConditions.add(multipleErrorCondition);
		
		addinnersecondPanel.add(ReplacePanel.triggerConditionNameInnerPanel(multipleErrorCondition));
		multipleErrorCondition.triggerComboBox.setSelectedItem(triggerName);
		addinnersecondPanel.add(closeButton);
		
		JPanel secondscrollinnerPanel = new JPanel();
		secondscrollinnerPanel.setLayout(new BoxLayout(secondscrollinnerPanel, BoxLayout.Y_AXIS));
		secondscrollinnerPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		callbacks.customizeUiComponent(secondscrollinnerPanel);
		
		JPanel addConditionPanel = ReplacePanel.addlogicalCondition(multipleErrorCondition);
		multipleErrorCondition.logicalCondition.setSelectedItem(logicalCondition);
		secondscrollinnerPanel.add(addConditionPanel);
		secondscrollinnerPanel.add(addinnersecondPanel);
		
		closeButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplacePanel.secondscrollPanel.remove(secondscrollinnerPanel);
				ReplacePanel.multipleErrorConditions.remove(multipleErrorCondition);
				PreviewPanel.conditionDetails.setText(FinalErrorCondition.addErrorCondition());
				ReplacePanel.secondscrollPanel.repaint();
				
			}
		});
		
		ReplacePanel.secondscrollPanel.add(secondscrollinnerPanel);
		if (!isRestoringProfile()) {
			PreviewPanel.conditionDetails.setText(FinalErrorCondition.addErrorCondition());
		}
		ReplacePanel.secondscrollPanel.repaint();
	}
	public void addToExtractionList(JSONObject jsonObject) {
		
		String extractedName = (String)jsonObject.get("Name");
		String msgID = (String)jsonObject.get("MsgID");
		BurpExtender.log("addToExtractionList start name=" + extractedName + " msgID=" + msgID + " beforeCount=" + ObtainPanel.extractionEntrylist.size());
		
		ExtractionEntry extractionEntry = new ExtractionEntry(callbacks, extractedName, msgID);
		
		extractionEntry.startString = (String)jsonObject.get("startString");
		extractionEntry.stopString = (String)jsonObject.get("stopString");
		extractionEntry.selectedText = (String)jsonObject.get("selectedtext");
		extractionEntry.isencode_decode = (String)jsonObject.get("isUrlDecode");
		
		ObtainPanel.extractionEntrylist.add(extractionEntry);
		ObtainPanel.extractionListComboBox.addItem(extractedName);
		
		// Add to final extraction list
		ReplacePanel.extractionreplaceComboNameList.addItem(extractedName);
		
		for(ObtainEntry obtainEntry : ObtainPanel.obtainEntrylist) {
			if(obtainEntry.getMsgID().equals(msgID)) {
				obtainEntry.extractionlistNames.add(extractionEntry);
			}
		}
		
		ObtainPanel.extractionTableModel.fireTableRowsInserted(ObtainPanel.extractionTableModel.getRowCount() - 1, 
				ObtainPanel.extractionTableModel.getRowCount() - 1);
		BurpExtender.log("addToExtractionList end name=" + extractedName + " afterCount=" + ObtainPanel.extractionEntrylist.size());
	}
	
	
	public void addToReplacementList(JSONObject jsonObject) {
		
		String replacementName = (String) jsonObject.get("ReplacementName");
		String repmsgID = (String) jsonObject.get("RepalcementMsgID");
		String extractionName = (String) jsonObject.get("ExtractionName");
		String extractionMsgID = (String) jsonObject.get("ExtractionMsgID");
		BurpExtender.log("addToReplacementList start name=" + replacementName + " repmsgID=" + repmsgID + " extractionName=" + extractionName + " beforeCount=" + ObtainPanel.replacementEntrylist.size());
		
		String repstartString = (String) jsonObject.get("startString");
		String repstopString = (String) jsonObject.get("stopString");
		String repextractedString = (String) jsonObject.get("selectedtext");
		
		
		ReplacementEntry replacementEntry = new ReplacementEntry(callbacks, replacementName, repmsgID, extractionName, extractionMsgID);
		replacementEntry.startString = repstartString;
		replacementEntry.stopString = repstopString;
		replacementEntry.selectedString = repextractedString;
		
		ObtainPanel.replacementEntrylist.add(replacementEntry);
		
		for(ObtainEntry obtainEntry : ObtainPanel.obtainEntrylist) {
			if(obtainEntry.getMsgID().equals(repmsgID)) {
				obtainEntry.replacementlistNames.add(replacementEntry);
				break;
			}
		}
		
		ObtainPanel.replacementTableModel.fireTableRowsInserted(ObtainPanel.replacementTableModel.getRowCount() - 1, 
				ObtainPanel.replacementTableModel.getRowCount() - 1);
		BurpExtender.log("addToReplacementList end name=" + replacementName + " afterCount=" + ObtainPanel.replacementEntrylist.size());
	}
}