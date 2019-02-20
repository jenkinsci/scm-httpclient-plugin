package com.meowlomo.jenkins.scm_httpclient;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import com.meowlomo.jenkins.scm_httpclient.model.CommitInfo;
import com.meowlomo.jenkins.scm_httpclient.model.JobBuildMessage;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class ScmExcution {
	private String regexString;
	
	private AbstractBuild<?, ?> build;
	
	private transient PrintStream logger;
	
	private TaskListener listener;
	
	private EnvVars envVars;

	public ScmExcution(String regexString, AbstractBuild<?, ?> build, TaskListener listener, EnvVars envVars) {
		this.regexString = regexString;
		this.build = build;
		this.logger = listener.getLogger();
		this.envVars = envVars;
	}

	public void process(Map<String, String> variables) {
		try {
			if (isScmChange()) {
				logger.println("the scm has changed...");

				CommitInfo commitInfo = new CommitInfo();
				commitInfo.doSaveAffectedPathsWork(regexString, getChangeSets(), logger, variables);

				JobBuildMessage jobBuildMessage = new JobBuildMessage();
				jobBuildMessage.doSaveJobBuildMessageWork(envVars, commitInfo.getCommitInfos(getChangeSets()),
						variables);
			} else {
				logger.println("the scm hasn't changed.");
			}
		} catch (Exception e) {
			e.getStackTrace();
		}
	}

	public boolean isScmChange() {
		boolean isChange = false;
		if (!getChangeSets().isEmpty()) {
			isChange = true;
		}
		return isChange;
	}

	public List<ChangeLogSet<? extends Entry>> getChangeSets() {
		List<ChangeLogSet<? extends Entry>> clss = build.getChangeSets();
		return clss;
	}
}
