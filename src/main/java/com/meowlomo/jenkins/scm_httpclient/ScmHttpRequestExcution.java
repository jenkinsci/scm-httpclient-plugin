package com.meowlomo.jenkins.scm_httpclient;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.meowlomo.jenkins.scm_httpclient.constant.ExcutionConstant;
import com.meowlomo.jenkins.scm_httpclient.constant.HttpMode;
import com.meowlomo.jenkins.scm_httpclient.constant.MimeType;
import com.meowlomo.jenkins.scm_httpclient.model.CommitInfo;
import com.meowlomo.jenkins.scm_httpclient.model.JobBuildMessage;
import com.meowlomo.jenkins.scm_httpclient.util.HttpRequestNameValuePair;
import com.meowlomo.jenkins.scm_httpclient.util.RegularExpressionUtil;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class ScmHttpRequestExcution {
	private String regexString;
	private String url;
	private HttpMode httpMode;
	private String body;
	private MimeType contentType;
	private List<HttpRequestNameValuePair> headers;
	private String validResponseCodes;
	private String validResponseContent;

	private AbstractBuild<?, ?> build;
	private transient PrintStream logger;
	private EnvVars envVars;

	private ScmHttpRequestExcution(String regexString, String url, HttpMode httpMode, String body, MimeType contentType,
			List<HttpRequestNameValuePair> headers, String validResponseCodes, String validResponseContent,
			AbstractBuild<?, ?> build, PrintStream logger, EnvVars envVars) {
		this.regexString = regexString;
		this.url = url;
		this.httpMode = httpMode;
		this.body = body;
		this.contentType = contentType;
		this.headers = headers;
		this.validResponseCodes = validResponseCodes;
		this.validResponseContent = validResponseContent != null ? validResponseContent : "";
		this.build = build;
		this.logger = logger;
		this.envVars = envVars;
	}

	public static ScmHttpRequestExcution from(ScmHttpClient shc, AbstractBuild<?, ?> build, TaskListener listener)
			throws IOException, InterruptedException {
		EnvVars envVars = build.getEnvironment(listener);
		List<HttpRequestNameValuePair> headers = shc.resolveHeaders(envVars);
		ScmHttpRequestExcution scmHttpRequestExcusion = new ScmHttpRequestExcution(shc.getRegexString(), shc.getUrl(),
				shc.getHttpMode(), shc.resolveBody(), shc.getContentType(), headers, shc.getValidResponseCodes(),
				shc.getValidResponseContent(), build, listener.getLogger(), envVars);
		return scmHttpRequestExcusion;
	}

	public void process(Map<String, String> variables) {
		try {

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

	public JobBuildMessage getJobBuildMessage() {
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
		return new JobBuildMessage(buildId, scmUrl, jobName, scmBranchOrRevision, getCommitInfos());
	}

	public Set<String> getAllAffectedPaths(String regexString) {
		Set<String> allAffectedPaths = new HashSet<String>();
		List<ChangeLogSet<? extends Entry>> clss = getChangeSets();
		for (ChangeLogSet<? extends Entry> cls : clss) {
			for (ChangeLogSet.Entry e : cls) {
				Collection<String> affectedPaths = e.getAffectedPaths();
				Iterator<String> it = affectedPaths.iterator();
				while (it.hasNext()) {
					String path = (String) it.next();
					if (!regexString.equals("")) {
						RegularExpressionUtil.handleString(regexString, path, logger);
					}
					allAffectedPaths.add(path);
				}
			}
		}
		return allAffectedPaths;
	}

	public List<ChangeLogSet<? extends Entry>> getChangeSets() {
		List<ChangeLogSet<? extends Entry>> clss = build.getChangeSets();
		return clss;
	}

	public List<CommitInfo> getCommitInfos() {
		List<CommitInfo> commits = new ArrayList<>();
		List<ChangeLogSet<? extends Entry>> clss = getChangeSets();
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
