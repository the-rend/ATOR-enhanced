package burp;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

public class UserProfile {

	private String name;
	private final List<UserRequestMatcher> matchers = new ArrayList<UserRequestMatcher>();
	private boolean matchAllConditions = true;
	private JSONObject snapshot = new JSONObject();

	public UserProfile(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<UserRequestMatcher> getMatchers() {
		return matchers;
	}

	public boolean isMatchAllConditions() {
		return matchAllConditions;
	}

	public void setMatchAllConditions(boolean matchAllConditions) {
		this.matchAllConditions = matchAllConditions;
	}


	public JSONObject getSnapshot() {
		return snapshot;
	}

	public void setSnapshot(JSONObject snapshot) {
		this.snapshot = snapshot;
	}
}