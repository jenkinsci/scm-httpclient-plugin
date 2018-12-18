package com.meowlomo.jenkins.ci.model;

/**
 * 
 * created by rich on 15/11/2018
 * 
 */
public class JenkinsEnvs {
	public static final String newLine = System.getProperty("line.separator");
	private String build_id;
	private String scm_branch;
	private String scm_url;
	private String job_name;
	private String logname;

	public JenkinsEnvs(String build_id, String scm_branch, String scm_url, String job_name, String logname) {
		super();
		this.build_id = build_id;
		this.scm_branch = scm_branch;
		this.scm_url = scm_url;
		this.job_name = job_name;
		this.logname = logname;
	}

	public JenkinsEnvs() {
		// TODO Auto-generated constructor stub
	}
	

	public String getBuild_id() {
		return build_id;
	}

	public void setBuild_id(String build_id) {
		this.build_id = build_id;
	}

	public String getScm_branch() {
		return scm_branch;
	}

	public void setScm_branch(String scm_branch) {
		this.scm_branch = scm_branch;
	}

	public String getScm_url() {
		return scm_url;
	}

	public void setScm_url(String scm_url) {
		this.scm_url = scm_url;
	}

	public String getJob_name() {
		return job_name;
	}

	public void setJob_name(String job_name) {
		this.job_name = job_name;
	}

	public String getLogname() {
		return logname;
	}

	public void setLogname(String logname) {
		this.logname = logname;
	}

	public static String getNewline() {
		return newLine;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder().append("BUILD_ID: ").append(build_id).append(newLine)
				.append("SCM_REVISION: ").append(scm_branch).append(newLine).append("SCM_URL: ").append(scm_url)
				.append(newLine).append("JOB_NAME: ").append(job_name).append(newLine).append("LOGNAME: ")
				.append(logname).append(newLine);

		return sb.toString();
	}

}
