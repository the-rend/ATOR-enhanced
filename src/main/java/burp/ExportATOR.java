package burp;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import javax.swing.JFileChooser;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ExportATOR {
	IBurpExtenderCallbacks callbacks;
	
	public ExportATOR(IBurpExtenderCallbacks callbacks) {
		this.callbacks = callbacks;	
	}
	
	public void writeFile() {
		File directory = null;
		String currentTime = DateTime.now().toString("MMddyyyyHHmmss");
        JFileChooser fileSelector = new JFileChooser(); 
		fileSelector.setFileSelectionMode(JFileChooser.FILES_ONLY); 
		fileSelector.setSelectedFile(new File("ATOR_" + currentTime + ".json"));
		int fileChoosenState = fileSelector.showSaveDialog(null);
        if (fileChoosenState == JFileChooser.APPROVE_OPTION) {
			directory = fileSelector.getSelectedFile();
        }
      
        if(directory != null) {
        	try {
	        		String exportATORConfig = directory.getAbsolutePath();
	        		if (!exportATORConfig.toLowerCase().endsWith(".json")) {
	        			exportATORConfig = exportATORConfig + ".json";
	        		}
				FileWriter file = new FileWriter(exportATORConfig);
				
				PrintWriter out = new PrintWriter(file);
				JSONObject results = exportATOR();
		        out.write(results.toString());
		        out.close();
			}
			catch(Exception e) {
				callbacks.printOutput("Exception while writing JSON file"+ e.getMessage());
			}
        }
	}
	
	public JSONObject exportATOR() {
		JSONObject jsonObject = exportCurrentProfile();
		jsonObject.put("users", UsersTab.exportUsers());
		jsonObject.put("activeUser", UsersTab.getActiveProfileName());
		
		return jsonObject;
	}

	public JSONObject exportCurrentProfile() {
		JSONObject jsonObject = new JSONObject();
		JSONArray errorCondition = getErrorCondition().get("errorconditionlist") instanceof JSONArray ? (JSONArray) getErrorCondition().get("errorconditionlist") : null;
		JSONObject obtainToken = getObtainToken();
		JSONArray ator = obtainToken.get("Ator") instanceof JSONArray ? (JSONArray) obtainToken.get("Ator") : null;
		JSONArray replacement = obtainToken.get("Replacement") instanceof JSONArray ? (JSONArray) obtainToken.get("Replacement") : null;
		JSONArray extraction = obtainToken.get("Extraction") instanceof JSONArray ? (JSONArray) obtainToken.get("Extraction") : null;
		JSONObject errorConditionReplacement = getErrorConditionReplacement();
		JSONArray triggerCondition = errorConditionReplacement.get("TriggerCondition") instanceof JSONObject ? (JSONArray) ((JSONObject) errorConditionReplacement.get("TriggerCondition")).get("multipleerrorcondition") : null;
		JSONArray errorConditionReplacementList = errorConditionReplacement.get("ErrorConditionReplacementList") instanceof JSONArray ? (JSONArray) errorConditionReplacement.get("ErrorConditionReplacementList") : null;
		BurpExtender.log("exportCurrentProfile counts error=" + (errorCondition == null ? 0 : errorCondition.size()) + " ator=" + (ator == null ? 0 : ator.size()) + " extraction=" + (extraction == null ? 0 : extraction.size()) + " replacement=" + (replacement == null ? 0 : replacement.size()) + " trigger=" + (triggerCondition == null ? 0 : triggerCondition.size()) + " errReplace=" + (errorConditionReplacementList == null ? 0 : errorConditionReplacementList.size()));
		jsonObject.put("errorCondition", getErrorCondition());
		jsonObject.put("obtainToken", obtainToken);
		jsonObject.put("errorConditionReplacement", errorConditionReplacement);
		jsonObject.put("uiState", getUiState());
		
		return jsonObject;
	}

	public JSONObject getUiState() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("errorRequest", getMessageText(ErrorPanel.ireqMessageEditor));
		jsonObject.put("errorResponse", getMessageText(ErrorPanel.iresMessageEditor));
		jsonObject.put("replaceRequest", getMessageText(ReplacePanel.ireqMessageEditor));
		jsonObject.put("replaceResponse", getMessageText(ReplacePanel.iresMessageEditor));
		jsonObject.put("previewRequest", getMessageText(PreviewPanel.ireqMessageEditor));
		jsonObject.put("previewResponse", getMessageText(PreviewPanel.iresMessageEditor));
		jsonObject.put("previewAtorRequest", getMessageText(PreviewPanel.ireqatorMessageEditor));
		jsonObject.put("previewAtorResponse", getMessageText(PreviewPanel.iresatorMessageEditor));
		jsonObject.put("previewModifiedRequest", getMessageText(PreviewPanel.ireqmodifiedMessageEditor));
		jsonObject.put("previewModifiedResponse", getMessageText(PreviewPanel.iresmodifiedMessageEditor));
		return jsonObject;
	}

	private String getMessageText(IMessageEditor editor) {
		if (editor == null || editor.getMessage() == null) {
			return "";
		}
		return this.callbacks.getHelpers().bytesToString(editor.getMessage());
	}
	
	public JSONObject getErrorCondition() {
		String requestMessage = this.callbacks.getHelpers().bytesToString(ErrorPanel.ireqMessageEditor.getMessage());
		String responseMessage = this.callbacks.getHelpers().bytesToString(ErrorPanel.iresMessageEditor.getMessage());	

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("request", requestMessage);
		jsonObject.put("response", responseMessage);
		jsonObject.put("comment", ErrorPanel.comment);
		jsonObject.put("highlight", ErrorPanel.highlight);
		jsonObject.put("host", ErrorPanel.host);
		jsonObject.put("port", ErrorPanel.port);
		jsonObject.put("protocol", ErrorPanel.protocol);
		jsonObject.put("errorconditionlist", getErrorList());
		
		return jsonObject;
	}
	
	public JSONObject getObtainToken() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Ator", getATORMacro());
		jsonObject.put("Replacement", getReplacementList());
		jsonObject.put("Extraction", getExtractionList());
		return jsonObject;	
	}
	
	public JSONObject getErrorConditionReplacement() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("TriggerCondition", getMarkTriggerCondition());
		jsonObject.put("ErrorConditionReplacementList", getErrorConditionReplacementList());
		return jsonObject;	
	}
	
	
	public JSONArray getErrorList() {
		JSONArray jsonArray = new JSONArray();
		for(ErrorEntry errorEntry:ErrorPanel.errorEntrylist) {
			JSONObject jsonObject = new JSONObject();
			
			jsonObject.put("Name", errorEntry.getConditionname());
			jsonObject.put("Category", errorEntry.getCategory());
			jsonObject.put("Value", errorEntry.getValue());
			jsonObject.put("Description", errorEntry.getDescription());
			
			jsonArray.add(jsonObject);
		}
		return jsonArray;
	}
	
	
	public JSONArray getATORMacro() {
		JSONArray jsonArray = new JSONArray();
		for(ObtainEntry obtainEntry:ObtainPanel.obtainEntrylist) {
			JSONObject jsonObject = new JSONObject();
			
			jsonObject.put("MsgID", obtainEntry.entrymsgid);
			jsonObject.put("Host", obtainEntry.getHost());
			jsonObject.put("Method", obtainEntry.getMethod());
			jsonObject.put("URL", obtainEntry.getUrl());
			
			jsonObject.put("Comment", obtainEntry.iHttpRequestResponse.getComment());
			jsonObject.put("Highlight", obtainEntry.iHttpRequestResponse.getHighlight());
			
			jsonObject.put("httpServicehost", obtainEntry.iHttpRequestResponse.getHttpService().getHost());
			jsonObject.put("httpServiceport", obtainEntry.iHttpRequestResponse.getHttpService().getPort());
			jsonObject.put("httpServiceprotocol", obtainEntry.iHttpRequestResponse.getHttpService().getProtocol());
		
			String request = this.callbacks.getHelpers().bytesToString(obtainEntry.req);
			String response = this.callbacks.getHelpers().bytesToString(obtainEntry.res);
			
			jsonObject.put("request", request);
			jsonObject.put("response", response);
			jsonArray.add(jsonObject);
		}
		BurpExtender.log("getATORMacro exported count=" + jsonArray.size());
		return jsonArray;
	}
	
	public JSONArray getReplacementList() {
		JSONArray jsonArray = new JSONArray();
		for(ReplacementEntry replacementEntry:ObtainPanel.replacementEntrylist) {
			JSONObject jsonObject = new JSONObject();
			
			jsonObject.put("ReplacementName", replacementEntry.getName());
			jsonObject.put("RepalcementMsgID", replacementEntry.getreplacementMsgID());
			jsonObject.put("ExtractionName", replacementEntry.getextractionName());
			jsonObject.put("ExtractionMsgID", replacementEntry.getextractionMsgID());
			
			jsonObject.put("startString", replacementEntry.startString);
			jsonObject.put("stopString", replacementEntry.stopString);
			jsonObject.put("selectedtext", replacementEntry.selectedString);
			
			jsonArray.add(jsonObject);
		}
		BurpExtender.log("getReplacementList exported count=" + jsonArray.size());
		return jsonArray;
	}
	
	public JSONArray getExtractionList() {
		JSONArray jsonArray = new JSONArray();
		for(ExtractionEntry extractionEntry:ObtainPanel.extractionEntrylist) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("Name", extractionEntry.getName());
			jsonObject.put("MsgID", extractionEntry.getextractionmsgID());
			
			jsonObject.put("startString", extractionEntry.startString);
			jsonObject.put("stopString", extractionEntry.stopString);
			jsonObject.put("selectedtext", extractionEntry.selectedText);
			jsonObject.put("isUrlDecode", extractionEntry.isencode_decode);
			
			jsonArray.add(jsonObject);
		}
		BurpExtender.log("getExtractionList exported count=" + jsonArray.size());
		return jsonArray;
	}
	
	
	public JSONObject getMarkTriggerCondition() {
		JSONObject triggerjsonObject = new JSONObject();
		
		String triggerCombox = (String) ReplacePanel.triggerConditionNameCombo.getSelectedItem();
		triggerjsonObject.put("MainCondition", triggerCombox);
		
		JSONArray jsonArray = new JSONArray();
		for(MultipleErrorCondition multipleErrorCondition: ReplacePanel.multipleErrorConditions) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("logical", (String)multipleErrorCondition.logicalCondition.getSelectedItem());
			jsonObject.put("triggerConditionName", (String)multipleErrorCondition.triggerComboBox.getSelectedItem());
			jsonArray.add(jsonObject);
		}
		
		triggerjsonObject.put("multipleerrorcondition", jsonArray);
		return triggerjsonObject;
	}
	
	
	public JSONArray getErrorConditionReplacementList() {
		JSONArray jsonArray = new JSONArray();
		for(ReplaceEntry replaceEntry:ReplacePanel.replaceEntrylist) {
			JSONObject jsonObject = new JSONObject();
			
			jsonObject.put("Name", replaceEntry.getName());
			jsonObject.put("ExtractionName", replaceEntry.getextractionName());
			jsonObject.put("headerName", replaceEntry.headerName);
			jsonObject.put("startString", replaceEntry.startString);
			jsonObject.put("stopString", replaceEntry.stopString);
			jsonObject.put("selectedText", replaceEntry.selectedText);
			jsonObject.put("replacementIn", replaceEntry.getReplacementIn());
			
			jsonArray.add(jsonObject);
		}
		
		return jsonArray;
	}
	
}
