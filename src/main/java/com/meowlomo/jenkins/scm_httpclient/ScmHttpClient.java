package com.meowlomo.jenkins.scm_httpclient;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.collect.Range;
import com.google.common.collect.Ranges;
import com.meowlomo.jenkins.ci.constant.HttpMode;
import com.meowlomo.jenkins.ci.constant.MimeType;
import com.meowlomo.jenkins.ci.model.FormatType;
import com.meowlomo.jenkins.ci.model.MatchingType;
import com.meowlomo.jenkins.ci.model.SinceType;
import com.meowlomo.jenkins.ci.util.HttpRequestNameValuePair;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;

public class ScmHttpClient extends Recorder implements SimpleBuildStep, Serializable {

	private static final long serialVersionUID = 1L;

	Map<String, String> variables = new HashMap<String, String>();

	private boolean printChangeLog;

	private boolean handleAffectedPaths;

	private String cut;

	private String contain;

	private boolean sendHttpRequest;

	private @Nonnull String url;
	
	private HttpMode httpMode;
	
	private MimeType acceptType;
	
	private MimeType contentType;
	
	private String validResponseCodes;
	
	private String validResponseContent;
	
	private String requestBody;

	@DataBoundConstructor
	public ScmHttpClient(boolean printChangeLog, boolean sendHttpRequest, @Nonnull String url, boolean handleAffectedPaths,
			String cut, String contain) {
		this.printChangeLog = printChangeLog;
		this.handleAffectedPaths = handleAffectedPaths;
		this.cut = cut;
		this.contain = contain;
		this.sendHttpRequest = sendHttpRequest;
		this.url = url;
	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {
		System.out.println("requestBody"+requestBody+"validResponseContent"+validResponseContent+"validResponseCodes"+validResponseCodes);
		System.out.println("contentType"+contentType+"acceptType"+acceptType+"httpMode"+httpMode+"url"+url);
		// execute scm work
//		ScmExcution se = new ScmExcution();
//		se.from(this, build, workspace, launcher, listener, variables, printChangeLog);
//
//		if (isSendHttpRequest()) {
//			EnvVars envVars = build.getEnvironment(listener);
//			HttpRequestExecution exec = HttpRequestExecution.from(this, envVars, build, listener, variables);
//			launcher.getChannel().call(exec);
//		}
//		build.setResult(Result.SUCCESS);
		Excution excution = new Excution(requestBody);
		excution.doMainWork(run);
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "SCM HttpClient";
		}

		public ListBoxModel doFillHttpModeItems() {
			return HttpMode.getFillItems();
		}

		public ListBoxModel doFillAcceptTypeItems() {
			return MimeType.getContentTypeFillItems();
		}

		public ListBoxModel doFillContentTypeItems() {
			return MimeType.getContentTypeFillItems();
		}

		@Restricted(NoExternalUse.class) // Only for UI calls
		public ListBoxModel doFillFormatItems() {
			ListBoxModel items = new ListBoxModel();
			for (FormatType formatType : FormatType.values()) {
				items.add(formatType.getFormat(), formatType.name());
			}
			return items;
		}

		@Restricted(NoExternalUse.class) // Only for UI calls
		public ListBoxModel doFillMatchingItems() {
			ListBoxModel items = new ListBoxModel();
			for (MatchingType matchingType : MatchingType.values()) {
				items.add(matchingType.getMatching(), matchingType.name());
			}
			return items;
		}

		@Restricted(NoExternalUse.class) // Only for UI calls
		public ListBoxModel doFillSinceItems() {
			ListBoxModel items = new ListBoxModel();
			for (SinceType sinceType : SinceType.values()) {
				items.add(sinceType.getName(), sinceType.name());
			}
			return items;
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

	public boolean isSendHttpRequest() {
		return sendHttpRequest;
	}

	@DataBoundSetter
	public void setSendHttpRequest(Boolean sendHttpRequest) {
		this.sendHttpRequest = sendHttpRequest;
	}

	public boolean isPrintChangeLog() {
		return printChangeLog;
	}

	@DataBoundSetter
	public void setPrintChangeLog(Boolean printChangeLog) {
		this.printChangeLog = printChangeLog;
	}

	@Nonnull
	public String getUrl() {
		return url;
	}

	public boolean isHandleAffectedPaths() {
		return handleAffectedPaths;
	}

	@DataBoundSetter
	public void setHandleAffectedPaths(boolean handleAffectedPaths) {
		this.handleAffectedPaths = handleAffectedPaths;
	}

	public String getCut() {
		return cut;
	}

	@DataBoundSetter
	public void setCut(String cut) {
		this.cut = cut;
	}

	public String getContain() {
		return contain;
	}

	@DataBoundSetter
	public void setContain(String contain) {
		this.contain = contain;
	}

	public HttpMode getHttpMode() {
		return httpMode;
	}

	@DataBoundSetter
	public void setHttpMode(HttpMode httpMode) {
		this.httpMode = httpMode;
	}

	public MimeType getAcceptType() {
		return acceptType;
	}

	@DataBoundSetter
	public void setAcceptType(MimeType acceptType) {
		this.acceptType = acceptType;
	}

	public MimeType getContentType() {
		return contentType;
	}

	@DataBoundSetter
	public void setContentType(MimeType contentType) {
		this.contentType = contentType;
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

	///////// http request
	String resolveUrl(EnvVars envVars, Run<?, ?> build, TaskListener listener) throws IOException {
		String url = envVars.expand(getUrl());
		return url;
	}

	String resolveBody(EnvVars envVars, Run<?, ?> build, TaskListener listener) throws IOException {
		String body = envVars.expand(getRequestBody());
		return body;
	}

	List<HttpRequestNameValuePair> resolveHeaders(EnvVars envVars) {
		final List<HttpRequestNameValuePair> headers = new ArrayList<>();
		if (contentType != null && contentType != MimeType.NOT_SET) {
			headers.add(new HttpRequestNameValuePair("Content-type", contentType.getContentType().toString()));
		}
		if (acceptType != null && acceptType != MimeType.NOT_SET) {
			headers.add(new HttpRequestNameValuePair("Accept", acceptType.getValue()));
		}
		return headers;
	}

}
