package com.meowlomo.jenkins.scm_httpclient;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.meowlomo.jenkins.ci.constant.HttpMode;
import com.meowlomo.jenkins.ci.constant.MimeType;
import com.meowlomo.jenkins.ci.util.HttpClientUtil;
import com.meowlomo.jenkins.ci.util.HttpRequestNameValuePair;
import com.meowlomo.jenkins.ci.util.RequestAction;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;

public class HttpExcusion {

	private String url;
	private HttpMode httpMode;
	private String body;
	private MimeType contentType;
	private List<HttpRequestNameValuePair> headers;
	private String validResponseCodes;
	private String validResponseContent;

	private transient PrintStream localLogger;

	public HttpExcusion() {

	}

	private HttpExcusion(String url, HttpMode httpMode, String body, MimeType contentType,
			List<HttpRequestNameValuePair> headers, String validResponseCodes, String validResponseContent) {
		this.url = url;
		this.httpMode = httpMode;
		this.body = body;
		this.contentType = contentType;
		this.headers = headers;
		this.validResponseCodes = validResponseCodes;
		this.validResponseContent = validResponseContent != null ? validResponseContent : "";
	}

	public HttpExcusion from(ScmHttpClient shc, EnvVars envVars, Run<?, ?> run, TaskListener taskListener) {
		this.url = shc.getUrl();
		this.httpMode = shc.getHttpMode();
		this.body = shc.getRequestBody();
		this.contentType = shc.getContentType();
		this.validResponseCodes = shc.getValidResponseCodes();
		this.validResponseContent = shc.getValidResponseContent();
		List<HttpRequestNameValuePair> headers = resolveHeaders(envVars);
		HttpExcusion httpExcusion = new HttpExcusion(url, httpMode, body, contentType, headers, validResponseCodes,
				validResponseContent);
		this.headers = headers;
		localLogger = taskListener.getLogger();
		return httpExcusion;
	}

	public HttpResponse request() {
		System.out.println("print now > ");
		System.out.println(url.toString() + "?" + httpMode + "?" + body + "?" + headers.get(0));
		try {
			HttpClientBuilder clientBuilder = HttpClientBuilder.create();
			CloseableHttpClient httpclient = clientBuilder.build();
			HttpClientUtil clientUtil = new HttpClientUtil();
			HttpRequestBase httpRequestBase = clientUtil
					.createRequestBase(new RequestAction(new URL(url), httpMode, body, null, headers));
			HttpContext context = new BasicHttpContext();
			HttpResponse response = executeRequest(httpclient, clientUtil, httpRequestBase, context);
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

	private HttpResponse executeRequest(CloseableHttpClient httpclient, HttpClientUtil clientUtil,
			HttpRequestBase httpRequestBase, HttpContext context) throws IOException, InterruptedException {
		try {
			final HttpResponse response = clientUtil.execute(httpclient, context, httpRequestBase, localLogger);
			return response;
		} finally {

			if (httpclient != null) {
				httpclient.close();
			}
		}

	}
}
