package com.meowlomo.jenkins.scm_httpclient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.impl.client.HttpClientBuilder;

import com.meowlomo.jenkins.ci.model.JenkinsEnvs;
import com.meowlomo.jenkins.ci.util.HttpClientUtil;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class Excution {
	private String requestBody;

	public Excution(String requestBody) {
		this.requestBody = requestBody;
	}

	public boolean doMainWork(Run<?, ?> run) {
		// Collection<String> affectedPaths = getAffectedPaths(run);
		// if (affectedPaths != null) {
		// isChange = true;
		// Iterator<String> it = affectedPaths.iterator();
		// while (it.hasNext()) {
		// String path = (String) it.next();
		// System.out.println("path > " + path);
		// }
		// }
		// else {
		// isChange = false;
		// }

		// getPartOfJenkinsEnvs
		return false;
	}

	public void request() {
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		HttpClientUtil clientUtil = new HttpClientUtil();
		// handled special string on requestBody
		// if (variables) {
		//
		// }
		// body = UnescapeUtil.replaceSprcialString(requestBody, variables);

	}

	public boolean scmChangesHandle(Run<?, ?> run) {
		boolean isChange = false;
		Collection<String> affectedPaths = null;
		ChangeLogSet<? extends Entry> cls = ((AbstractBuild<?, ?>) run).getChangeSet();
		if (!cls.isEmptySet()) {
			for (ChangeLogSet.Entry e : cls) {
				affectedPaths = e.getAffectedPaths();
			}
		}
		if (affectedPaths != null) {
			isChange = true;
			saveAffectedPaths(affectedPaths);
		}
		return isChange;
	}

	private void saveAffectedPaths(Collection<String> affectedPaths) {
//		Map<String, ?> variables = new HashMap<String, String>();
//		variables.put("affectedPaths", affectedPaths);
		
	}

	public JenkinsEnvs getPartOfJenkinsEnvs(Run<?, ?> run) {
		Map<String, String> envs = run.getEnvVars();
		for (Map.Entry<String, String> entry : envs.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
		return null;

	}

}
