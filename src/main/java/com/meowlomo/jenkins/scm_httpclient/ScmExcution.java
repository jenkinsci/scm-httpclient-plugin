package com.meowlomo.jenkins.scm_httpclient;

import java.io.PrintStream;
import java.util.Map;

import com.meowlomo.jenkins.scm_httpclient.model.CommitInfo;
import com.meowlomo.jenkins.scm_httpclient.model.JobBuildMessage;

import hudson.EnvVars;
import hudson.model.AbstractBuild;

public class ScmExcution {
	private PrintStream logger;
	private EnvVars envVars;

	public ScmExcution(AbstractBuild<?, ?> build, EnvVars envVars, PrintStream logger, boolean saveAffectedPath,
			boolean saveJobBuildMessage, String regexString, String addScmPath, Map<String, String> variables) {
		this.logger = logger;
		this.envVars = envVars;
		process(build, saveAffectedPath, saveJobBuildMessage, regexString, addScmPath, variables);
	}

	public void process(AbstractBuild<?, ?> build, boolean saveAffectedPath, boolean saveJobBuildMessage,
			String regexString, String addScmPath, Map<String, String> variables) {
		if (!build.getChangeSets().isEmpty()) {
			logger.println("the scm has changed...");

			CommitInfo commitInfo = new CommitInfo();
			if (saveAffectedPath) {
				commitInfo.doSaveAffectedPathsWork(regexString, addScmPath, build.getChangeSets(), logger, variables);
			}

			JobBuildMessage jobBuildMessage = new JobBuildMessage();
			if (saveJobBuildMessage) {
				jobBuildMessage.doSaveJobBuildMessageWork(envVars, commitInfo.getCommitInfos(build.getChangeSets()),
						variables);
			}

		} else {
			logger.println("the scm hasn't changed.");
		}
	}

}
