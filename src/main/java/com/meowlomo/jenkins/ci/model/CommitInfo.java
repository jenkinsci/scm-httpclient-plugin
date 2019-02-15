package com.meowlomo.jenkins.ci.model;

import java.util.Collection;

public class CommitInfo {
	private String author;
	private String message;
	private String commitId;
	private Collection<String> affectedPaths;

	public String getAuthor() {
		return author;
	}

	public CommitInfo setAuthor(String author) {
		this.author = author;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public CommitInfo setMessage(String message) {
		this.message = message;
		return this;
	}

	public String getCommitId() {
		return commitId;
	}

	public CommitInfo setCommitId(String commitId) {
		this.commitId = commitId;
		return this;
	}

	public Collection<String> getAffectedPaths() {
		return affectedPaths;
	}

	public CommitInfo setAffectedPaths(Collection<String> affectedPaths) {
		this.affectedPaths = affectedPaths;
		return this;
	}

	@Override
	public String toString() {
		return "CommitInfo [author=" + author + ", message=" + message + ", commitId=" + commitId + ", affectedPaths="
				+ affectedPaths + "]";
	}
	

}
