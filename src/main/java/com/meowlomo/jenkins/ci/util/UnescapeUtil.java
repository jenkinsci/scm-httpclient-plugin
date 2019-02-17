package com.meowlomo.jenkins.ci.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnescapeUtil {

	/** eg: String text = "saasa$(a)xxxxx$(b)"; 
	        Map<String, String> variables = new HashMap<String,String>();
	        variables.put("a", "35");
	        variables.put("b", "36");
	        
		After function handled,the result returns saasa35xxxxx36
	 * */
	public static String replaceSprcialString(String text, Map<String, String> variables) {
		if (text.equals("") || text == null) {
			return "";
		}
		if (variables == null) {
			return text;
		}
		Pattern pattern = Pattern.compile("\\$\\(\\w+\\)");
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			String newText = matcher.group();
			Pattern newPattern = Pattern.compile("\\w+");
			Matcher newMatch = newPattern.matcher(newText);
			if (newMatch.find()) {
				String var = newMatch.group();
				for (Map.Entry<String, String> entry : variables.entrySet()) {
					if (var.equals(entry.getKey())) {
						text = text.replaceAll("\\$\\(" + var + "\\)", entry.getValue());
					}
				}
			}

		}
		return text;
	}

}
