package com.meowlomo.jenkins.scm_httpclient.auth;

import java.io.PrintStream;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;

public class BasicDigestAuthentication
        implements Authenticator {
	private static final long serialVersionUID = 4818288270720177069L;

	private final String keyName;
    private final String userName;
    private final String password;

    public BasicDigestAuthentication(String keyName, String userName,
            String password) {
        this.keyName = keyName;
        this.userName = userName;
        this.password = password;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

	@Override
	public CloseableHttpClient authenticate(HttpClientBuilder clientBuilder, HttpContext context,
											HttpRequestBase requestBase, PrintStream logger) {
		return CredentialBasicAuthentication.auth(clientBuilder, context, requestBase, userName, password);
	}
}
