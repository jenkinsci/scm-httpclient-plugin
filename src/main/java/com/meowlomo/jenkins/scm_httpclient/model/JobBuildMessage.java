package com.meowlomo.jenkins.scm_httpclient.model;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSON;
import com.meowlomo.jenkins.scm_httpclient.constant.ExcutionConstant;

import hudson.EnvVars;

/**
 * @author rich.chen
 * 
 */
public class JobBuildMessage {
	private String buildId;
	private String scmUrl;
	private String jobName;
	private String scmBranchOrRevision;
	List<CommitInfo> commitInfo;
	
	public JobBuildMessage() {
		
	}
	
	public JobBuildMessage(String buildId, String scmUrl, String jobName, String scmBranchOrRevision,
			List<CommitInfo> commitInfo) {
		super();
		this.buildId = buildId;
		this.scmUrl = scmUrl;
		this.jobName = jobName;
		this.scmBranchOrRevision = scmBranchOrRevision;
		this.commitInfo = commitInfo;
	}

	public String getBuildId() {
		return buildId;
	}

	public void setBuildId(String buildId) {
		this.buildId = buildId;
	}

	public String getScmUrl() {
		return scmUrl;
	}

	public void setScmUrl(String scmUrl) {
		this.scmUrl = scmUrl;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getscmBranchOrRevision() {
		return scmBranchOrRevision;
	}

	public void setscmBranchOrRevisionOrRevision(String scmBranchOrRevision) {
		this.scmBranchOrRevision = scmBranchOrRevision;
	}

	public List<CommitInfo> getCommitInfo() {
		return commitInfo;
	}

	public void setCommitInfo(List<CommitInfo> commitInfo) {
		this.commitInfo = commitInfo;
	}

	public void doSaveJobBuildMessageWork(EnvVars envVars, List<CommitInfo> commitInfos,
			Map<String, String> variables) {
		for (Entry<String, String> entry : envVars.entrySet()) {
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
		// do save jobBuildMessage to global map
		saveJobBuildMessageToJson(new JobBuildMessage(buildId, scmUrl, jobName, scmBranchOrRevision, commitInfos), variables);
	}

	private void saveJobBuildMessageToJson(JobBuildMessage jobBuildMessage, Map<String, String> variables) {
		String jobBuildMessageJson = "";
		if (jobBuildMessage != null) {
			jobBuildMessageJson = JSON.toJSONString(jobBuildMessage);
			variables.put(ExcutionConstant.JOB_BUILD_MESSAGE, jobBuildMessageJson);
		}
	}

	@Override
	public String toString() {
		return "JobBuildMessage [buildId=" + buildId + ", scmUrl=" + scmUrl + ", jobName=" + jobName
				+ ", scmBranchOrRevision=" + scmBranchOrRevision + ", commitInfo=" + commitInfo + "]";
	}

}
