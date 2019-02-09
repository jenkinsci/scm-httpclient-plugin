package com.meowlomo.jenkins.scm_httpclient;

import java.util.Collection;
import org.apache.http.impl.client.HttpClientBuilder;
import com.meowlomo.jenkins.ci.util.HttpClientUtil;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class Excution {
	public Excution(String body) {
		
	}
	public boolean doMainWork(Run<?, ?> run) {
		Collection<String> affectedPaths = getAffectedPaths(run);
//		Iterator<String> it = affectedPaths.iterator();
//		while (it.hasNext()) {
//			String path = (String) it.next();
//			System.out.println("path > " + path);
//		}
		return false;
	}
	public void request(){
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		HttpClientUtil clientUtil = new HttpClientUtil();
		// handled special string on body
//		body = UnescapeUtil.replaceSprcialString(body, variables);
		
	}
	public Collection<String> getAffectedPaths(Run<?, ?> run) {
		AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
		ChangeLogSet<? extends Entry> cls = build.getChangeSet();
		if (!cls.isEmptySet()) {
			Collection<String> paths = null;
			for (ChangeLogSet.Entry e : cls) {
				paths = e.getAffectedPaths();
			}
			return paths;
		} 
		return null;
	}

}
