package com.meowlomo.jenkins.model;

/**
 * Created by rich chen on 1/29/19.
 */
public class BuildDetail {
	private String commitId;
	private String commitMessage;
	private String committerName;
	private String committerEmail;
	private String commitDate;

	public String getCommitId() {
		return commitId;
	}

	public void setCommitId(String commitId) {
		this.commitId = commitId;
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	public String getCommitterName() {
		return committerName;
	}

	public void setCommitterName(String committerName) {
		this.committerName = committerName;
	}

	public String getCommitterEmail() {
		return committerEmail;
	}

	public void setCommitterEmail(String committerEmail) {
		this.committerEmail = committerEmail;
	}

	public String getCommitDate() {
		return commitDate;
	}

	public void setCommitDate(String commitDate) {
		this.commitDate = commitDate;
	}

}
