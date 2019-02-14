package com.meowlomo.jenkins.scm_httpclient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.impl.client.HttpClientBuilder;

import com.alibaba.fastjson.JSON;
import com.meowlomo.jenkins.ci.model.JenkinsEnvs;
import com.meowlomo.jenkins.ci.util.HttpClientUtil;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class Excution {
	private String requestBody;

	Map<String, String> variables = new HashMap<String, String>();

	public Excution(String requestBody) {
		this.requestBody = requestBody;
	}

	public void doScmWork(AbstractBuild<?,?> build,TaskListener listener) {
		try {
			if (isScmChange(build)) {
				listener.getLogger().println("the scm has changed...");
				Collection<String> affectedPaths = getAffectedPaths(build);
				Iterator<String> it = affectedPaths.iterator();
				while (it.hasNext()) {
					String path = (String) it.next();
					listener.getLogger().println("the scm changed path > " + path);
				}
				// do save affectedPath to globe map work
				saveAffectedPathsToJson(affectedPaths);
			} else {
				listener.getLogger().println("the scm hasn't changed.");
			}
		} catch (Exception e) {
			listener.getLogger().println(e);
		}

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
	}

//	public void request() {
//		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
//		HttpClientUtil clientUtil = new HttpClientUtil();
	// handled special string on requestBody
	// if (variables) {
	//
	// }
	// body = UnescapeUtil.replaceSprcialString(requestBody, variables);

//	}
	public boolean isScmChange(Run<?, ?> run) {
		boolean isChange = false;
		ChangeLogSet<? extends Entry> cls = getChangeSet(run);
		if (!cls.isEmptySet()) {
			isChange = true;
		}
		return isChange;
	}

	public Collection<String> getAffectedPaths(Run<?, ?> run) {
		Collection<String> affectedPaths = null;
		ChangeLogSet<? extends Entry> cls = getChangeSet(run);
		for (ChangeLogSet.Entry e : cls) {
			affectedPaths = e.getAffectedPaths();
		}
		return affectedPaths;
	}

	public ChangeLogSet<? extends Entry> getChangeSet(Run<?, ?> run) {
		return ((AbstractBuild<?, ?>) run).getChangeSet();
	}

//	public boolean scmChangesHandle(Run<?, ?> run) {
//		boolean isChange = false;
//		Collection<String> affectedPaths = null;
//		ChangeLogSet<? extends Entry> cls = ((AbstractBuild<?, ?>) run).getChangeSet();
//		if (!cls.isEmptySet()) {
//			for (ChangeLogSet.Entry e : cls) {
//				affectedPaths = e.getAffectedPaths();
//			}
//		}
//		if (affectedPaths != null) {
//			isChange = true;
//			saveAffectedPaths(affectedPaths);
//		}
//		return isChange;
//	}

	private void saveAffectedPathsToJson(Collection<String> affectedPaths) {
		if (!affectedPaths.isEmpty()) {
			String AFFECTED_PATH = JSON.toJSONString(affectedPaths);
			variables.put("AFFECTED_PATH", AFFECTED_PATH);
		}
	}

	public JenkinsEnvs getPartOfJenkinsEnvs(Run<?, ?> run) {
		Map<String, String> envs = run.getEnvVars();
		for (Map.Entry<String, String> entry : envs.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
		return null;

	}

}
