package com.meowlomo.jenkins.scm_httpclient.auth;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;

/**
 * @author Janario Oliveira
 */
public interface Authenticator extends Serializable {

	String getKeyName();

	CloseableHttpClient authenticate(HttpClientBuilder clientBuilder, HttpContext context, HttpRequestBase requestBase,
					  PrintStream logger) throws IOException, InterruptedException;
}
