package com.github.jenkins.scm_httpclient;
//package com.github.jenkins.lastchanges;
//
//import java.net.ConnectException;
//import java.net.SocketTimeoutException;
//import java.net.UnknownHostException;
//
//import org.apache.http.HttpResponse;
//import org.apache.http.impl.client.HttpClientBuilder;
//import org.apache.http.protocol.BasicHttpContext;
//import org.apache.http.protocol.HttpContext;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.JUnit4;
//
//
//import com.meowlomo.jenkins.lastchanges.util.HttpClientUtil;
//
//@RunWith(JUnit4.class)
//public class HttpRequestTest {
//	@Test
//	public void sendHttpRequest() {
//		String method = "http://www.baidu.com";
//		HttpContext context = new BasicHttpContext();
//		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
//		HttpClientUtil clientUtil = new HttpClientUtil();
//		final HttpResponse httpResponse = clientUtil.execute(method, context);
//        System.out.println("Response Code: " + httpResponse.getStatusLine());
//		
//	}
//
//}
