package com.meowlomo.jenkins.scm_httpclient.model;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.meowlomo.jenkins.scm_httpclient.constant.ExcutionConstant;
import com.meowlomo.jenkins.scm_httpclient.util.RegularExpressionUtil;

import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

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

	public List<CommitInfo> getCommitInfos(List<ChangeLogSet<? extends Entry>> changeSets) {
		List<CommitInfo> commits = new ArrayList<>();
		if (!changeSets.isEmpty()) {
			for (ChangeLogSet<? extends Entry> cls : changeSets) {
				for (ChangeLogSet.Entry e : cls) {
					CommitInfo commitInfo = new CommitInfo();
					commitInfo.setAffectedPaths(e.getAffectedPaths()).setAuthor(e.getAuthor().toString())
							.setCommitId(e.getCommitId()).setMessage(e.getMsg());
					commits.add(commitInfo);
				}
			}
		}
		return commits;
	}

	public void doSaveAffectedPathsWork(String regexString, String addScmPath,
			List<ChangeLogSet<? extends Entry>> changeSets, PrintStream logger, Map<String, String> variables) {
		// Set element is unique
		Set<String> allAffectedPaths = new HashSet<String>();
		if (!changeSets.isEmpty()) {
			for (ChangeLogSet<? extends Entry> cls : changeSets) {
				for (ChangeLogSet.Entry e : cls) {
					Collection<String> affectedPaths = e.getAffectedPaths();
					Iterator<String> it = affectedPaths.iterator();
					while (it.hasNext()) {
						String path = (String) it.next();
						// do regular expression work, each affectedpath will be handled.
						if (!regexString.equals("")) {
							path = RegularExpressionUtil.handleString(regexString, path, logger);
						}
						allAffectedPaths.add(path);
					}
				}
			}
			saveAffectedPathsToJson(AddscmPathHandle(addScmPath, allAffectedPaths, logger), variables);
		}
	}

	private void saveAffectedPathsToJson(Set<List<String>> mutil_affected_path, Map<String, String> variables) {
		if (!mutil_affected_path.isEmpty()) {
			String AFFECTED_PATH = JSON.toJSONString(mutil_affected_path);
			variables.put(ExcutionConstant.AFFECTED_PATH, AFFECTED_PATH);
		}
	}

	private Set<List<String>> AddscmPathHandle(String addScmPath, Set<String> allAffectedPaths, PrintStream logger) {
		if (allAffectedPaths.isEmpty()) {
			logger.println("[ERROR]the scm affected path is empty.");
			return null;
		}
		Set<List<String>> mutil_affected_path = new HashSet<List<String>>();
		if (!addScmPath.equals("")) {
			List<String> result = Arrays.asList(addScmPath.split(","));
			for (String affectedpath : allAffectedPaths) {
				List<String> temp = new ArrayList<String>();
				temp.addAll(result);
				temp.add(affectedpath);
				mutil_affected_path.add(temp);
			}
		} else {
			for (String affectedpath : allAffectedPaths) {
				List<String> temp = new ArrayList<String>();
				temp.add(affectedpath);
				mutil_affected_path.add(temp);
			}

		}
		return mutil_affected_path;
	}

	@Override
	public String toString() {
		return "CommitInfo [author=" + author + ", message=" + message + ", commitId=" + commitId + ", affectedPaths="
				+ affectedPaths + "]";
	}

}
