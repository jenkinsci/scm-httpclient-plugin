package com.meowlomo.jenkins.scm_httpclient;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.google.common.collect.Range;
import com.meowlomo.jenkins.scm_httpclient.ScmHttpClient.DescriptorImpl;
import com.meowlomo.jenkins.scm_httpclient.auth.Authenticator;
import com.meowlomo.jenkins.scm_httpclient.constant.HttpMode;
import com.meowlomo.jenkins.scm_httpclient.constant.MimeType;
import com.meowlomo.jenkins.scm_httpclient.model.ResponseContentSupplier;
import com.meowlomo.jenkins.scm_httpclient.util.HttpClientUtil;
import com.meowlomo.jenkins.scm_httpclient.util.HttpRequestNameValuePair;
import com.meowlomo.jenkins.scm_httpclient.util.RequestAction;
import com.meowlomo.jenkins.scm_httpclient.util.UnescapeUtil;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;

public class HttpRequestExcution {

	private String url;
	private String credentialId;
	private HttpMode httpMode;
	private String body;
	private MimeType contentType;
	private List<HttpRequestNameValuePair> headers;
	private String validResponseCodes;
	private String validResponseContent;

	private transient PrintStream localLogger;

	public HttpRequestExcution() {

	}

	private HttpRequestExcution(String url, String credentialId, HttpMode httpMode, String body, MimeType contentType,
			List<HttpRequestNameValuePair> headers, String validResponseCodes, String validResponseContent) {
		this.url = url;
		this.credentialId = credentialId;
		this.httpMode = httpMode;
		this.body = body;
		this.contentType = contentType;
		this.headers = headers;
		this.validResponseCodes = validResponseCodes;
		this.validResponseContent = validResponseContent != null ? validResponseContent : "";
	}

	public HttpRequestExcution from(ScmHttpClient shc, EnvVars envVars, Run<?, ?> run, TaskListener taskListener) {
		this.url = shc.getUrl();
		this.credentialId = shc.getCredentialId();	
		this.httpMode = shc.getHttpMode();
		this.body = resolveBody(shc.getRequestBody(), shc.variables);
		this.contentType = shc.getContentType();
		this.validResponseCodes = shc.getValidResponseCodes();
		this.validResponseContent = shc.getValidResponseContent();
		List<HttpRequestNameValuePair> headers = resolveHeaders(envVars);
		HttpRequestExcution httpExcusion = new HttpRequestExcution(url, credentialId, httpMode, body, contentType, headers,
				validResponseCodes, validResponseContent);
		this.headers = headers;
		localLogger = taskListener.getLogger();
		return httpExcusion;
	}

	public ResponseContentSupplier request() {
		try {
			HttpClientBuilder clientBuilder = HttpClientBuilder.create();
			CloseableHttpClient httpclient = clientBuilder.build();
			HttpClientUtil clientUtil = new HttpClientUtil();
			localLogger.println("URL:" + url);
			localLogger.println("HttpMethod:" + httpMode);
			if (!body.equals("")) {
				localLogger.println("RequestBody:" + body);
			}
			HttpRequestBase httpRequestBase = clientUtil
					.createRequestBase(new RequestAction(new URL(url), httpMode, body, null, headers));
			HttpContext context = new BasicHttpContext();
			ResponseContentSupplier response = executeRequest(httpclient, clientUtil, httpRequestBase, context);
			processResponse(response);
			return response;
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	List<HttpRequestNameValuePair> resolveHeaders(EnvVars envVars) {
		final List<HttpRequestNameValuePair> headers = new ArrayList<>();
		if (contentType != null && contentType != MimeType.NOT_SET) {
			headers.add(new HttpRequestNameValuePair("Content-type", contentType.getContentType().toString()));
		}
		return headers;
	}

	private String resolveBody(String requestBody, Map<String, String> variables) {
		return UnescapeUtil.replaceSprcialString(requestBody, variables);
	}

	private ResponseContentSupplier executeRequest(CloseableHttpClient httpclient, HttpClientUtil clientUtil,
			HttpRequestBase httpRequestBase, HttpContext context) throws IOException, InterruptedException {
		ResponseContentSupplier responseContentSupplier;
		try {
			final HttpResponse response = clientUtil.execute(httpclient, context, httpRequestBase, localLogger);
			// The HttpEntity is consumed by the ResponseContentSupplier
			responseContentSupplier = new ResponseContentSupplier(response);
		} catch (UnknownHostException uhe) {
			localLogger.println("Treating UnknownHostException(" + uhe.getMessage() + ") as 404 Not Found");
			responseContentSupplier = new ResponseContentSupplier("UnknownHostException as 404 Not Found", 404);
		} catch (SocketTimeoutException | ConnectException ce) {
			localLogger.println("Treating " + ce.getClass() + "(" + ce.getMessage() + ") as 408 Request Timeout");
			responseContentSupplier = new ResponseContentSupplier(
					ce.getClass() + "(" + ce.getMessage() + ") as 408 Request Timeout", 408);
		}

		return responseContentSupplier;
	}

	private void processResponse(ResponseContentSupplier response) throws IOException, InterruptedException {
		// logs
//		localLogger.println("Response: \n" + response.getContent());

		// validate status code
		responseCodeIsValid(response);

		// validate content
//		if (!validResponseContent.isEmpty()) {
//			if (!response.getContent().contains(validResponseContent)) {
//				throw new AbortException(
//						"Fail: Response doesn't contain expected content '" + validResponseContent + "'");
//			}
//		}

	}

	private void responseCodeIsValid(ResponseContentSupplier response) throws AbortException {
		List<Range<Integer>> ranges = DescriptorImpl.parseToRange(validResponseCodes);
		for (Range<Integer> range : ranges) {
			if (range.contains(response.getStatus())) {
				localLogger.println("Success code from " + range);
				return;
			}
		}
		throw new AbortException(
				"Fail: the returned code " + response.getStatus() + " is not in the accepted range: " + ranges);
	}
}
