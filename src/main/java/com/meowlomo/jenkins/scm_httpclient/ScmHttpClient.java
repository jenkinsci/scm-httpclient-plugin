package com.meowlomo.jenkins.scm_httpclient;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.collect.Range;
import com.google.common.collect.Ranges;
import com.meowlomo.jenkins.scm_httpclient.constant.HttpMode;
import com.meowlomo.jenkins.scm_httpclient.constant.MimeType;
import com.meowlomo.jenkins.scm_httpclient.model.CommitInfo;
import com.meowlomo.jenkins.scm_httpclient.model.JobBuildMessage;
import com.meowlomo.jenkins.scm_httpclient.util.HttpRequestNameValuePair;
import com.meowlomo.jenkins.scm_httpclient.util.UnescapeUtil;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;

public class ScmHttpClient extends Recorder implements SimpleBuildStep, Serializable {

	private static final long serialVersionUID = 1L;

	Map<String, String> variables = new HashMap<String, String>();

	private boolean saveAffectedPath;

	private boolean saveJobBuildMessage;

	private String regexString = "";

	private String addScmPath;

	private boolean sendHttpRequest;

	private @Nonnull String url;

	private HttpMode httpMode;

	private MimeType contentType;

	private String validResponseCodes;

	private String validResponseContent;

	private String requestBody;

	@DataBoundConstructor
	public ScmHttpClient(String url) {
		this.url = url;
	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {
		AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
		EnvVars envVars = build.getEnvironment(listener);
		PrintStream logger = listener.getLogger();
		new ScmExcution(build, envVars, logger, saveAffectedPath, saveJobBuildMessage, regexString, addScmPath,
				variables);

		if (sendHttpRequest) {
			HttpRequestExcution httpRequestExcution = new HttpRequestExcution();
			httpRequestExcution.from(this, envVars, run, listener);
			httpRequestExcution.request();
		}
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Extension
	@Symbol("scmHttpClient")
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		public static final String validResponseCodes = "100:399";

		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "SCM HttpClient";
		}

		public ListBoxModel doFillHttpModeItems() {
			return HttpMode.getFillItems();
		}

		public ListBoxModel doFillContentTypeItems() {
			return MimeType.getContentTypeFillItems();
		}

		public FormValidation doCheckValidResponseCodes(@QueryParameter String value) {
			return checkValidResponseCodes(value);
		}

		public static FormValidation checkValidResponseCodes(String value) {
			if (value == null || value.trim().isEmpty()) {
				return FormValidation.ok();
			}
			try {
				parseToRange(value);
			} catch (IllegalArgumentException iae) {
				return FormValidation.error("Response codes expected is wrong. " + iae.getMessage());
			}
			return FormValidation.ok();

		}

		public static List<Range<Integer>> parseToRange(String value) {
			List<Range<Integer>> validRanges = new ArrayList<Range<Integer>>();
			String[] codes = value.split(",");
			for (String code : codes) {
				String[] fromTo = code.trim().split(":");
				checkArgument(fromTo.length <= 2, "Code %s should be an interval from:to or a single value", code);

				Integer from;
				try {
					from = Integer.parseInt(fromTo[0]);
				} catch (NumberFormatException nfe) {
					throw new IllegalArgumentException("Invalid number " + fromTo[0]);
				}

				Integer to = from;
				if (fromTo.length != 1) {
					try {
						to = Integer.parseInt(fromTo[1]);
					} catch (NumberFormatException nfe) {
						throw new IllegalArgumentException("Invalid number " + fromTo[1]);
					}
				}
				checkArgument(from <= to, "Interval %s should be FROM less than TO", code);
				validRanges.add(Ranges.closed(from, to));
			}
			return validRanges;
		}
	}

	List<HttpRequestNameValuePair> resolveHeaders(EnvVars envVars) {
		final List<HttpRequestNameValuePair> headers = new ArrayList<>();
		if (contentType != null && contentType != MimeType.NOT_SET) {
			headers.add(new HttpRequestNameValuePair("Content-type", contentType.getContentType().toString()));
		}
		return headers;
	}

	public String resolveBody() {
		return UnescapeUtil.replaceSprcialString(requestBody, variables);
	}

	public boolean isSendHttpRequest() {
		return sendHttpRequest;
	}

	@DataBoundSetter
	public void setSendHttpRequest(Boolean sendHttpRequest) {
		this.sendHttpRequest = sendHttpRequest;
	}

	public boolean isSaveAffectedPath() {
		return saveAffectedPath;
	}

	@DataBoundSetter
	public void setSaveAffectedPath(Boolean saveAffectedPath) {
		this.saveAffectedPath = saveAffectedPath;
	}

	public boolean isSaveJobBuildMessage() {
		return saveJobBuildMessage;
	}

	@DataBoundSetter
	public void setSaveJobBuildMessage(Boolean saveJobBuildMessage) {
		this.saveJobBuildMessage = saveJobBuildMessage;
	}

	@Nonnull
	public String getUrl() {
		return url;
	}

	public HttpMode getHttpMode() {
		return httpMode;
	}

	@DataBoundSetter
	public void setHttpMode(HttpMode httpMode) {
		this.httpMode = httpMode;
	}

	public MimeType getContentType() {
		return contentType;
	}

	@DataBoundSetter
	public void setContentType(MimeType contentType) {
		this.contentType = contentType;
	}

	public String getRegexString() {
		return regexString;
	}

	@DataBoundSetter
	public void setRegexString(String regexString) {
		this.regexString = regexString;
	}

	public String getAddScmPath() {
		return addScmPath;
	}

	@DataBoundSetter
	public void setAddScmPath(String addScmPath) {
		this.addScmPath = addScmPath;
	}

	public String getRequestBody() {
		return requestBody;
	}

	@DataBoundSetter
	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}

	@Nonnull
	public String getValidResponseCodes() {
		return validResponseCodes;
	}

	@DataBoundSetter
	public void setValidResponseCodes(String validResponseCodes) {
		this.validResponseCodes = validResponseCodes;
	}

	public String getValidResponseContent() {
		return validResponseContent;
	}

	@DataBoundSetter
	public void setValidResponseContent(String validResponseContent) {
		this.validResponseContent = validResponseContent;
	}
}
