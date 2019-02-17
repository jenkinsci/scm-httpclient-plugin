package com.meowlomo.jenkins.scm_httpclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.meowlomo.jenkins.ci.constant.ExcutionConstant;
import com.meowlomo.jenkins.ci.model.CommitInfo;
import com.meowlomo.jenkins.ci.model.JobBuildMessage;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class ScmExcution {

	public void process(AbstractBuild<?, ?> build, TaskListener listener, EnvVars envVars,
			Map<String, String> variables) {
		try {
			if (isScmChange(build)) {
				listener.getLogger().println("the scm has changed...");
				JobBuildMessage jobBuildMessage = getJobBuildMessage(build, envVars);
				// do save jobBuildMessage to global map work
				String jobBuildMessageJson = saveJobBuildMessageToJson(jobBuildMessage, variables);
				listener.getLogger().println("jobBuildMessage : " + jobBuildMessageJson);
				// iterator
				Set<String> set = getAllAffectedPaths(build);
				listener.getLogger().println("getAllAffectedPaths iterator...");
				for (String str : set) {
					listener.getLogger().println(str);
				}
				// do save allAffectedPaths to global map work
				String affectPathJson = saveAffectedPathsToJson(getAllAffectedPaths(build), variables);
				listener.getLogger().println("affectPathJson : " + affectPathJson);
			} else {
				listener.getLogger().println("the scm hasn't changed.");
			}
		} catch (Exception e) {
			e.getStackTrace();
		}
	}

	public boolean isScmChange(AbstractBuild<?, ?> build) {
		boolean isChange = false;
		if (!getChangeSets(build).isEmpty()) {
			isChange = true;
		}
		return isChange;
	}

	public JobBuildMessage getJobBuildMessage(AbstractBuild<?, ?> build, EnvVars envVars) {
		String buildId = "";
		String scmUrl = "";
		String jobName = "";
		String scmBranchOrRevision = "";
		for (java.util.Map.Entry<String, String> entry : envVars.entrySet()) {
			if (entry.getKey().equals("BUILD_ID")) {
				buildId = entry.getValue();
			}
			if (entry.getKey().equals("JOB_NAME")) {
				jobName = entry.getValue();
			}

			if (entry.getKey().equals("GIT_URL")) {
				scmUrl = entry.getValue();
			} else if (entry.getKey().equals("SVN_URL")) {
				scmUrl = entry.getValue();
			}

			if (entry.getKey().equals("GIT_BRANCH")) {
				scmBranchOrRevision = entry.getValue();
			} else if (entry.getKey().equals("SVN_REVISION")) {
				scmBranchOrRevision = entry.getValue();
			}
		}
		return new JobBuildMessage(buildId, scmUrl, jobName, scmBranchOrRevision, getCommitInfos(build));
	}

	public Set<String> getAllAffectedPaths(AbstractBuild<?, ?> build) {
		Set<String> allAffectedPaths = new HashSet<String>();
		List<ChangeLogSet<? extends Entry>> clss = getChangeSets(build);
		for (ChangeLogSet<? extends Entry> cls : clss) {
			for (ChangeLogSet.Entry e : cls) {
				Collection<String> affectedPaths = e.getAffectedPaths();
				Iterator<String> it = affectedPaths.iterator();
				while (it.hasNext()) {
					String path = (String) it.next();
					allAffectedPaths.add(path);
				}
			}
		}
		return allAffectedPaths;
	}

	public List<ChangeLogSet<? extends Entry>> getChangeSets(AbstractBuild<?, ?> build) {
		List<ChangeLogSet<? extends Entry>> clss = build.getChangeSets();
		return clss;
	}

	public List<CommitInfo> getCommitInfos(AbstractBuild<?, ?> build) {
		List<CommitInfo> commits = new ArrayList<>();
		List<ChangeLogSet<? extends Entry>> clss = getChangeSets(build);
		for (ChangeLogSet<? extends Entry> cls : clss) {
			for (ChangeLogSet.Entry e : cls) {
				CommitInfo commitInfo = new CommitInfo();
				commitInfo.setAffectedPaths(e.getAffectedPaths()).setAuthor(e.getAuthor().toString())
						.setCommitId(e.getCommitId()).setMessage(e.getMsg());
				commits.add(commitInfo);
			}
		}
		return commits;
	}

	private String saveAffectedPathsToJson(Set<String> affectedPaths, Map<String, String> variables) {
		String AFFECTED_PATH = "";
		if (!affectedPaths.isEmpty()) {
			AFFECTED_PATH = JSON.toJSONString(affectedPaths);
			variables.put(ExcutionConstant.AFFECTED_PATH, AFFECTED_PATH);
		}
		return AFFECTED_PATH;
	}
	
	private String saveJobBuildMessageToJson(JobBuildMessage jobBuildMessage, Map<String, String> variables) {
		String jobBuildMessageJson = "";
		if (jobBuildMessage != null) {
			jobBuildMessageJson = JSON.toJSONString(jobBuildMessage);
			variables.put(ExcutionConstant.JOB_BUILD_MESSAGE, jobBuildMessageJson);
		}
		return jobBuildMessageJson;
	}

}
