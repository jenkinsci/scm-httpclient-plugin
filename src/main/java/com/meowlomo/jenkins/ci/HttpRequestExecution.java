/*The MIT License

Copyright (c) 2012-, Janario Oliveira, and a number of other of contributors
Modifications copyright (C) 2018 meowlomo.com <dev.support@meowlomo.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package com.meowlomo.jenkins.ci;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.google.common.collect.Range;
import com.meowlomo.jenkins.ci.CI.DescriptorImpl;
import com.meowlomo.jenkins.ci.constant.HttpMode;
import com.meowlomo.jenkins.ci.constant.ResponseHandle;
import com.meowlomo.jenkins.ci.util.HttpClientUtil;
import com.meowlomo.jenkins.ci.util.HttpRequestNameValuePair;
import com.meowlomo.jenkins.ci.util.RequestAction;
import com.meowlomo.jenkins.ci.util.UnescapeUtil;

import hudson.AbortException;
import hudson.CloseProofOutputStream;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.RemoteOutputStream;
import jenkins.security.MasterToSlaveCallable;


public class HttpRequestExecution extends MasterToSlaveCallable<ResponseContentSupplier, RuntimeException> {

	private static final long serialVersionUID = -2066857816168989599L;
	private Map<String, String> variables = new HashMap<String, String>();
	private final String url;
	private final HttpMode httpMode;
	private String body;
	private final List<HttpRequestNameValuePair> headers;
	private transient PrintStream localLogger;
	private final OutputStream remoteLogger;
	private final ResponseHandle responseHandle;
	private final String validResponseCodes;
	private final String validResponseContent;

	static HttpRequestExecution from(CI http, EnvVars envVars, Run<?, ?> build, TaskListener taskListener,
			Map<String, String> variables) {
		try {
			String url = http.resolveUrl(envVars, build, taskListener);
			String body = http.resolveBody(envVars, build, taskListener);
			List<HttpRequestNameValuePair> headers = http.resolveHeaders(envVars);
			return new HttpRequestExecution(url, http.getHttpMode(), body, headers, taskListener.getLogger(),
					ResponseHandle.NONE, http.getValidResponseCodes(), http.getValidResponseContent(), variables);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private HttpRequestExecution(String url, HttpMode httpMode, String body, List<HttpRequestNameValuePair> headers,
			PrintStream logger, ResponseHandle responseHandle, String validResponseCodes, String validResponseContent,
			Map<String, String> variables) {
		this.url = url;
		this.httpMode = httpMode;
		this.body = body;
		this.headers = headers;
		this.localLogger = logger;
		this.remoteLogger = new RemoteOutputStream(new CloseProofOutputStream(logger));
		this.responseHandle = ResponseHandle.STRING;
		this.validResponseCodes = validResponseCodes;
		this.validResponseContent = validResponseContent != null ? validResponseContent : "";
		this.variables = variables;
	}

	@Override
	public ResponseContentSupplier call() throws RuntimeException {
		try {
			return authAndRequest();
		} catch (IOException | InterruptedException | KeyStoreException | NoSuchAlgorithmException
				| KeyManagementException e) {
			throw new IllegalStateException(e);
		}
	}

	private ResponseContentSupplier authAndRequest() throws IOException, InterruptedException, KeyStoreException,
			NoSuchAlgorithmException, KeyManagementException {
		// only leave open if no error happen
		ResponseHandle responseHandle = ResponseHandle.NONE;
		CloseableHttpClient httpclient = null;
		try {
			HttpClientBuilder clientBuilder = HttpClientBuilder.create();
			HttpClientUtil clientUtil = new HttpClientUtil();
			// handled special string on body
			body = UnescapeUtil.replaceSprcialString(body, variables);
			logger().println("body >" + body);
			
			HttpRequestBase httpRequestBase = clientUtil
					.createRequestBase(new RequestAction(new URL(url), httpMode, body, null, headers));

			HttpContext context = new BasicHttpContext();
			httpclient = clientBuilder.build();

			ResponseContentSupplier response = executeRequest(httpclient, clientUtil, httpRequestBase, context);
			// do response code check
			processResponse(response);
			return response;
		} finally {
			if (responseHandle != ResponseHandle.LEAVE_OPEN) {
				if (httpclient != null) {
					httpclient.close();
				}
			}
		}

	}

	private ResponseContentSupplier executeRequest(CloseableHttpClient httpclient, HttpClientUtil clientUtil,
			HttpRequestBase httpRequestBase, HttpContext context) throws IOException, InterruptedException {
		ResponseContentSupplier responseContentSupplier;
		try {
			final HttpResponse response = clientUtil.execute(httpclient, context, httpRequestBase, logger());
			// The HttpEntity is consumed by the ResponseContentSupplier
			responseContentSupplier = new ResponseContentSupplier(responseHandle, response);
		} catch (UnknownHostException uhe) {
			logger().println("Treating UnknownHostException(" + uhe.getMessage() + ") as 404 Not Found");
			responseContentSupplier = new ResponseContentSupplier("UnknownHostException as 404 Not Found", 404);
		} catch (SocketTimeoutException | ConnectException ce) {
			logger().println("Treating " + ce.getClass() + "(" + ce.getMessage() + ") as 408 Request Timeout");
			responseContentSupplier = new ResponseContentSupplier(
					ce.getClass() + "(" + ce.getMessage() + ") as 408 Request Timeout", 408);
		}

		return responseContentSupplier;
	}

	private void responseCodeIsValid(ResponseContentSupplier response) throws AbortException {
		List<Range<Integer>> ranges = DescriptorImpl.parseToRange(validResponseCodes);
		for (Range<Integer> range : ranges) {
			if (range.contains(response.getStatus())) {
				logger().println("Success code from " + range);
				return;
			}
		}
		throw new AbortException(
				"Fail: the returned code " + response.getStatus() + " is not in the accepted range: " + ranges);
	}

	private void processResponse(ResponseContentSupplier response) throws IOException, InterruptedException {
		// validate status code
		responseCodeIsValid(response);
		logger().println("[responseContent] Match [expectContent] ?");
		logger().println("[responseContent]: \n" + response.getContent());
		logger().println("[expectContent]: \n" + validResponseContent);
		// validate content
		if (!validResponseContent.isEmpty()) {
			if (!response.getContent().contains(validResponseContent)) {
				throw new AbortException(
						"Fail: Response doesn't contain expected content '" + validResponseContent + "'");
			}
		}
	}

	private PrintStream logger() {
		if (localLogger == null) {
			try {
				localLogger = new PrintStream(remoteLogger, true, StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
		}
		return localLogger;
	}

	@SuppressWarnings("unused")
	private static class NoopTrustManager extends X509ExtendedTrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
				throws CertificateException {
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
				throws CertificateException {
		}
	}
}
