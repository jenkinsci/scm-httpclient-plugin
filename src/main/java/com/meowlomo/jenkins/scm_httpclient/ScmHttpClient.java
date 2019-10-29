package com.meowlomo.jenkins.scm_httpclient;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.alibaba.fastjson.JSON;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.AbstractIdCredentialsListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;
import com.meowlomo.jenkins.scm_httpclient.auth.Authenticator;
import com.meowlomo.jenkins.scm_httpclient.auth.BasicDigestAuthentication;
import com.meowlomo.jenkins.scm_httpclient.constant.HttpMode;
import com.meowlomo.jenkins.scm_httpclient.constant.MimeType;
import com.meowlomo.jenkins.scm_httpclient.model.ResponseContentSupplier;
import com.meowlomo.jenkins.scm_httpclient.util.HttpClientUtil;
import com.meowlomo.jenkins.scm_httpclient.util.HttpRequestNameValuePair;
import com.meowlomo.jenkins.scm_httpclient.util.RequestAction;
import com.meowlomo.jenkins.scm_httpclient.util.UnescapeUtil;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

public class ScmHttpClient extends Recorder implements SimpleBuildStep, Serializable {

	private static final long serialVersionUID = 1L;

	Map<String, String> variables = new HashMap<String, String>();
	
	static HttpRequestGlobalConf hgc = new HttpRequestGlobalConf();

	private boolean saveAffectedPath;

	private boolean saveJobBuildMessage;

	private String regexString = "";

	private String addScmPath;

	private boolean sendHttpRequest;

	private @Nonnull String url;
	
	private String credentialId;

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

		if(sendHttpRequest) {
			HttpRequestExcution httpRequestExcution = new HttpRequestExcution();
			httpRequestExcution.from(this, envVars, run, listener);
			httpRequestExcution.request(getLoginToken(url,credentialId));
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
		
		public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item project, @QueryParameter String url) {
			return fillCredentialIdItems(project, url);
		}
		
		public static ListBoxModel fillCredentialIdItems(Item project, String url) {
			if (project == null || !project.hasPermission(Item.CONFIGURE)) {
				return new StandardListBoxModel();
			}
			List<Option> options = new ArrayList<>();
			for (BasicDigestAuthentication basic : hgc.getBasicDigestAuthentications()) {
				options.add(new Option("(deprecated - use Jenkins Credentials) " +
						basic.getKeyName(), basic.getKeyName()));
            }

			AbstractIdCredentialsListBoxModel<StandardListBoxModel, StandardCredentials> items = new StandardListBoxModel()
					.includeEmptyValue()
					.includeAs(ACL.SYSTEM,
							project, StandardUsernamePasswordCredentials.class,
							URIRequirementBuilder.fromUri(url).build());
			items.addMissing(options);
            return items;
        }

		public ListBoxModel doFillHttpModeItems() {
			return HttpMode.getFillItems();
		}

		public ListBoxModel doFillContentTypeItems() {
			return MimeType.getContentTypeFillItems();
		}
		
		public FormValidation doCheckCredentialId(@AncestorInPath Item project, @QueryParameter String url, @QueryParameter String value) {
			return checkCredentialId(project,url,value);
		}
		public static FormValidation checkCredentialId(Item project, String url, String authentication) {
			if (url == null)
			// not set, can't check
			{
				return FormValidation.ok();
	        }
			if (authentication != null && !authentication.isEmpty()) {
				Authenticator auth = hgc.getAuthentication(authentication);

				if (auth == null) {
					StandardUsernamePasswordCredentials credential = CredentialsMatchers.firstOrNull(
							CredentialsProvider.lookupCredentials(
									StandardUsernamePasswordCredentials.class,
									project, ACL.SYSTEM,
									URIRequirementBuilder.fromUri(url).build()),
							CredentialsMatchers.withId(authentication));
					if (credential != null) {
						String userName = credential.getUsername();
						String password = credential.getPassword().getPlainText();
						List<HttpRequestNameValuePair> headers = new ArrayList<>();
						headers.add(new HttpRequestNameValuePair("content-type","application/x-www-form-urlencoded"));
						String body = "email=" + userName + "&" + "password=" + password;
						try {
							HttpClientBuilder clientBuilder = HttpClientBuilder.create();
							CloseableHttpClient httpclient = clientBuilder.build();
							HttpClientUtil clientUtil = new HttpClientUtil();
							HttpRequestBase httpRequestBase = clientUtil
									.createRequestBase(new RequestAction(new URL(url), HttpMode.POST, body, null, headers));
							
							String authUrl = "http://" + httpRequestBase.getURI().getHost()+"/api/auth/login";
							HttpRequestBase hrb = clientUtil
									.createRequestBase(new RequestAction(new URL(authUrl), HttpMode.POST, body, null, headers));
							HttpContext context = new BasicHttpContext();
							final HttpResponse response = httpclient.execute(hrb, context);
							
							@SuppressWarnings("resource")
							ResponseContentSupplier responseContentSupplier = new ResponseContentSupplier(response);
							if(responseContentSupplier.getStatus() == 200) {
//								String content = responseContentSupplier.getContent();
//								access_token = (String) JSON.parseObject(content).get("access_token");
							} else {
//								access_token = "";
								return FormValidation.error("Authentication failed for \'" + authentication + "\'." + "returned status code" + responseContentSupplier.getStatus());
							}
						} catch (Exception e) {
							return FormValidation.error("Authentication failed for \'" + authentication + "\'." + e);
						}
					}
				}
			}
			return FormValidation.ok();
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
	
	String getLoginToken(String url, String authentication) {
		String access_token = "";
		if (authentication != null && !authentication.isEmpty()) {
			Authenticator auth = hgc.getAuthentication(authentication);
			if (auth == null) {
				@SuppressWarnings("deprecation")
				StandardUsernamePasswordCredentials credential = CredentialsMatchers.firstOrNull(
						CredentialsProvider.lookupCredentials(
								StandardUsernamePasswordCredentials.class,
								Jenkins.getInstance(), ACL.SYSTEM,
								URIRequirementBuilder.fromUri(url).build()),
						CredentialsMatchers.withId(authentication));
				if (credential != null) {
					String userName = credential.getUsername();
					String password = credential.getPassword().getPlainText();
					List<HttpRequestNameValuePair> headers = new ArrayList<>();
					headers.add(new HttpRequestNameValuePair("content-type","application/x-www-form-urlencoded"));
					String body = "email=" + userName + "&" + "password=" + password;
					try {
						HttpClientBuilder clientBuilder = HttpClientBuilder.create();
						CloseableHttpClient httpclient = clientBuilder.build();
						HttpClientUtil clientUtil = new HttpClientUtil();
						HttpRequestBase httpRequestBase = clientUtil
								.createRequestBase(new RequestAction(new URL(url), HttpMode.POST, body, null, headers));
						
						String authUrl = "http://" + httpRequestBase.getURI().getHost()+"/api/auth/login";
						HttpRequestBase hrb = clientUtil
								.createRequestBase(new RequestAction(new URL(authUrl), HttpMode.POST, body, null, headers));
						HttpContext context = new BasicHttpContext();
						final HttpResponse response = httpclient.execute(hrb, context);
						
						@SuppressWarnings("resource")
						ResponseContentSupplier responseContentSupplier = new ResponseContentSupplier(response);
						if(responseContentSupplier.getStatus() == 200) {
							String content = responseContentSupplier.getContent();
							access_token = (String) JSON.parseObject(content).get("access_token");
						}
					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				}
			}
		}
		return access_token;
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
	
	public String getCredentialId() {
		return credentialId;
	}
	
	@DataBoundSetter
	public void setCredentialId(String credentialId) {
		this.credentialId = credentialId;
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
