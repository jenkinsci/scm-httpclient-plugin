package com.meowlomo.jenkins.scm_httpclient.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ResponseContentSupplier implements Serializable, AutoCloseable {

	private static final long serialVersionUID = 1L;

	private int status;
	private Map<String, List<String>> headers = new HashMap<>();
	private String charset;

	private String content;

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	private transient InputStream contentStream;

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	private transient CloseableHttpClient httpclient;

	public ResponseContentSupplier(String content, int status) {
		this.content = content;
		this.status = status;
	}

	public ResponseContentSupplier(HttpResponse response) {
		this.status = response.getStatusLine().getStatusCode();
		readHeaders(response);
		readCharset(response);

		try {
			HttpEntity entity = response.getEntity();
			InputStream entityContent = entity != null ? entity.getContent() : null;

			if (entityContent != null) {
				byte[] bytes = IOUtils.toByteArray(entityContent);
				contentStream = new ByteArrayInputStream(bytes);
				content = new String(bytes, charset == null || charset.isEmpty() ? Charset.defaultCharset().name() : charset);
			}
//			else {
//				contentStream = entityContent;
//			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public int getStatus() {
		return this.status;
	}

	public Map<String, List<String>> getHeaders() {
		return this.headers;
	}

	public String getCharset() {
		return charset;
	}

	public String getContent() {
		return content;
	}

	public InputStream getContentStream() {
		return contentStream;
	}

	@Override
	public void close() throws Exception {
		if (httpclient != null) {
			httpclient.close();
		}
		if (contentStream != null) {
			contentStream.close();
		}
	}

	private void readCharset(HttpResponse response) {
		Charset charset = null;
		ContentType contentType = ContentType.get(response.getEntity());
		if (contentType != null) {
			charset = contentType.getCharset();
			if (charset == null) {
				ContentType defaultContentType = ContentType.getByMimeType(contentType.getMimeType());
				if (defaultContentType != null) {
					charset = defaultContentType.getCharset();
				}
			}
		}
		if (charset != null) {
			this.charset = charset.name();
		}
	}

	private void readHeaders(HttpResponse response) {
		Header[] respHeaders = response.getAllHeaders();
		for (Header respHeader : respHeaders) {
			List<String> hs = headers.get(respHeader.getName());
			if (hs == null) {
				headers.put(respHeader.getName(), hs = new ArrayList<>());
			}
			hs.add(respHeader.getValue());
		}
	}

	void setHttpClient(CloseableHttpClient httpclient) {
		this.httpclient = httpclient;
	}

}
