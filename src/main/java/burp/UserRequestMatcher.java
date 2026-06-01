package burp;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class UserRequestMatcher {

	private final String matchType;
	private final String expression;
	private final boolean ignoreCase;

	public UserRequestMatcher(String matchType, String expression, boolean ignoreCase) {
		this.matchType = matchType;
		this.expression = expression;
		this.ignoreCase = ignoreCase;
	}

	public String getMatchType() {
		return matchType;
	}

	public String getExpression() {
		return expression;
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}

	public boolean matches(IHttpRequestResponse messageInfo, boolean messageIsRequest) {
		try {
			String target = extractPart(messageInfo, messageIsRequest);
			if (target == null || expression == null) {
				return false;
			}
			boolean matched;
			if ("Text".equalsIgnoreCase(matchType)) {
				matched = ignoreCase ? target.toLowerCase().contains(expression.toLowerCase()) : target.contains(expression);
			} else {
				int flags = ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
				matched = Pattern.compile(expression, flags).matcher(target).find();
			}
			return matched;
		} catch (PatternSyntaxException e) {
			BurpExtender.log("UserRequestMatcher pattern syntax error expression=" + expression + " err=" + e.getMessage());
			return false;
		} catch (Exception e) {
			BurpExtender.log("UserRequestMatcher exception=" + e.getClass().getSimpleName() + ": " + e.getMessage());
			return false;
		}
	}

	private String extractPart(IHttpRequestResponse messageInfo, boolean messageIsRequest) {
		if (messageInfo == null) return null;
		IExtensionHelpers helpers = BurpExtender.callbacks.getHelpers();
		try {
			return helpers.bytesToString(messageInfo.getRequest());
		} catch (Exception e) {
			BurpExtender.log("UserRequestMatcher.extractPart exception=" + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;
		}
	}
}