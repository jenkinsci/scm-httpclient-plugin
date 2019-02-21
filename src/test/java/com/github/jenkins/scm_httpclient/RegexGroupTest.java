package com.github.jenkins.scm_httpclient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RegexGroupTest {
	public static void main(String[] asd) {
		String sourcestring = "abab";
		Pattern re = Pattern.compile("(a)");
		Matcher m = re.matcher(sourcestring);
		int mIdx = 0;
		while (m.find()) {
			for (int groupIdx = 0; groupIdx < m.groupCount() + 1; groupIdx++) {
				System.out.println(m.group(groupIdx));
			}
			mIdx++;
		}
	}
}
