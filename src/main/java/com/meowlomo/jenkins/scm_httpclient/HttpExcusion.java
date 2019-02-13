package com.meowlomo.jenkins.scm_httpclient;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;

public class HttpExcusion {

	private Map<String, String> variables = new HashMap<String, String>();
	private String url;
	private HttpMode httpMode;
	private String body;
	private MimeType contentType;
	private List<HttpRequestNameValuePair> headers;
	private String validResponseCodes;
	private String validResponseContent;
	private transient PrintStream localLogger;

	private HttpExcusion(String url, HttpMode httpMode, String body, MimeType contentType, List<HttpRequestNameValuePair> headers,
			String validResponseCodes, String validResponseContent, Map<String, String> variables) {
		this.url = url;
		this.httpMode = httpMode;
		this.body = body;
		this.contentType = contentType;
		this.headers = headers;
		this.validResponseCodes = validResponseCodes;
		this.validResponseContent = validResponseContent != null ? validResponseContent : "";
		this.variables = variables;
	}

	public void request(AbstractBuild<?, ?> build, TaskListener taskListener) {
		try {
			CloseableHttpClient httpclient = null;
			EnvVars envVars = build.getEnvironment(taskListener);
			for (Map.Entry<String, String> e : build.getBuildVariables().entrySet()) {
				envVars.put(e.getKey(), e.getValue());
				System.out.println("e.getKey()" + e.getKey() + "e.getValue()" + e.getValue());
			}
			List<HttpRequestNameValuePair> headers = resolveHeaders(envVars);

			HttpClientBuilder clientBuilder = HttpClientBuilder.create();
			HttpClientUtil clientUtil = new HttpClientUtil();
			HttpRequestBase httpRequestBase = clientUtil.createRequestBase(
					new RequestAction(new URL(url), httpMode, body, null, headers));
			HttpContext context = new BasicHttpContext();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

	}

	List<HttpRequestNameValuePair> resolveHeaders(EnvVars envVars) {
		final List<HttpRequestNameValuePair> headers = new ArrayList<>();
		if (contentType != null && contentType != MimeType.NOT_SET) {
			headers.add(new HttpRequestNameValuePair("Content-type", contentType.getContentType().toString()));
		}
		return headers;
	}
	private HttpResponse executeRequest(CloseableHttpClient httpclient, HttpClientUtil clientUtil, HttpRequestBase httpRequestBase, HttpContext context)
			throws IOException, InterruptedException {
		try {
			final HttpResponse response = clientUtil.execute(httpclient, context, httpRequestBase, logger());
			return response;
		}
		finally {

				if (httpclient != null) {
					httpclient.close();
				}
		}

	}

	private PrintStream logger(TaskListener taskListener) {
		localLogger = taskListener.getLogger();
		return localLogger;
	}

}
