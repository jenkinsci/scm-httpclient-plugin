package com.meowlomo.jenkins.ci.model;

import java.util.List;

/**
 *  @author rich.chen
 *  
 * */
public class JobBuildMessage {
	private String buildId;
	private String scmUrl;
	private String jobName;
	private String scmBranchOrRevision;
	List<CommitInfo> commitInfo;
	
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
	@Override
	public String toString() {
		return "JobBuildMessage [buildId=" + buildId + ", scmUrl=" + scmUrl + ", jobName=" + jobName + ", scmBranchOrRevision="
				+ scmBranchOrRevision + ", commitInfo=" + commitInfo + "]";
	}
	

}
