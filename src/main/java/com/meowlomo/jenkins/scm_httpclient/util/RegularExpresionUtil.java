package com.meowlomo.jenkins.scm_httpclient.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegularExpresionUtil {
	public static void extractString(String regexString, String inputStr) {
		final Pattern p = Pattern.compile(regexString);
		Matcher m = p.matcher(inputStr);
		if (m.find()) {
			System.out.println("inputStr [" + inputStr + "] has matched item " + "[" + m.group(0) + "].");
		} else {
			System.out.println("inputStr [" + inputStr + "] hasn't matched item.");
		}
	}

	public static void main(String[] args) {
		 extractString("src/main/java", "appsrc/main/javahello.java");
//		extractString("^(!app)*$", "appgdfgfgdffgn");
	}

}
