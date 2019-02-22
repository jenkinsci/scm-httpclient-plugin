package com.meowlomo.jenkins.scm_httpclient.util;

import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegularExpressionUtil {
	public static String handleString(String regexString, String inputStr, PrintStream logger) {
		final Pattern p = Pattern.compile(regexString);
		Matcher m = p.matcher(inputStr);
		if (m.find()) {
			String result = m.group(0);
			if (logger != null) {
				logger.println("inputStr [" + inputStr + "] has matched item " + "[" + result + "].");
			} else {
				System.out.println("inputStr [" + inputStr + "] has matched item " + "[" + result + "].");
			}
			return result;
		} else {
			if (logger != null) {
				logger.println("inputStr [" + inputStr + "] hasn't matched item.");
			} else {
				System.out.println("inputStr [" + inputStr + "] hasn't matched item.");
			}
			return inputStr;
		}
	}
}
