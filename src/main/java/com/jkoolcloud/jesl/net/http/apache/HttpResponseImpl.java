/*
 * Copyright 2015-2019 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jkoolcloud.jesl.net.http.apache;

import java.io.IOException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.message.BasicHttpResponse;

import com.jkoolcloud.jesl.net.http.HttpResponse;

/**
 * Implementation of HttpResponse wrapping Apache HttpClient BasicHttpResponse class.
 *
 * @version $Revision: 1 $
 */
public class HttpResponseImpl extends BasicHttpResponse implements HttpResponse {
	protected org.apache.http.HttpResponse response;

	/**
	 * Create HTTP response object
	 * 
	 * @param response
	 *            apache HTTP response
	 */
	public HttpResponseImpl(org.apache.http.HttpResponse response) {
		super(response.getStatusLine());
		this.response = response;
	}

	/**
	 * Create HTTP response object
	 * 
	 * @param version
	 *            protocol version
	 * @param statusCode
	 *            HTTP status code
	 */
	public HttpResponseImpl(ProtocolVersion version, int statusCode) {
		super(version, statusCode, null);
	}

	/**
	 * Obtain apache response implementation object instance
	 *
	 * @return apache response implementation object instance
	 */
	protected org.apache.http.HttpResponse getRawResp() {
		return (response != null ? response : this);
	}

	///////////////////// HttpResponse methods

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStatus(int status) {
		getRawResp().setStatusCode(status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getStatus() {
		return getRawResp().getStatusLine().getStatusCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHeader(String name) {
		Header header = getRawResp().getFirstHeader(name);
		return (header == null ? null : header.getValue());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addHeader(String name, String value) {
		if (response != null) {
			response.addHeader(name, value);
		} else {
			super.addHeader(name, value);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHeader(String name, String value) {
		if (response != null) {
			response.setHeader(name, value);
		} else {
			super.setHeader(name, value);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeHeader(String name) {
		getRawResp().removeHeaders(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasContent() {
		return HttpMessageUtils.hasContent(getRawResp());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getContentBytes() throws IOException {
		return HttpMessageUtils.getContentBytes(getRawResp());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getContentString() throws IOException {
		return HttpMessageUtils.getContentString(getRawResp());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getContentString(String charset) throws IOException {
		return HttpMessageUtils.getContentString(getRawResp(), charset);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setContent(String contentType, byte[] content, String contentEncoding) throws IOException {
		HttpMessageUtils.setContent(getRawResp(), contentType, content, contentEncoding);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setContent(String contentType, String content) throws IOException {
		HttpMessageUtils.setContent(getRawResp(), contentType, content);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setContent(String contentType, String content, String charset) throws IOException {
		HttpMessageUtils.setContent(getRawResp(), contentType, content, charset);
	}

	///////////////////// BasicHttpResponse methods

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProtocolVersion getProtocolVersion() {
		return (response != null ? response.getProtocolVersion() : super.getProtocolVersion());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StatusLine getStatusLine() {
		return (response != null ? response.getStatusLine() : super.getStatusLine());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStatusLine(StatusLine statusline) {
		if (response != null) {
			response.setStatusLine(statusline);
		} else {
			super.setStatusLine(statusline);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStatusLine(ProtocolVersion ver, int code) {
		if (response != null) {
			response.setStatusLine(ver, code);
		} else {
			super.setStatusLine(ver, code);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStatusLine(ProtocolVersion ver, int code, String reason) {
		if (response != null) {
			response.setStatusLine(ver, code, reason);
		} else {
			super.setStatusLine(ver, code, reason);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStatusCode(int code) {
		if (response != null) {
			response.setStatusCode(code);
		} else {
			super.setStatusCode(code);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setReasonPhrase(String reason) {
		if (response != null) {
			response.setReasonPhrase(reason);
		} else {
			super.setReasonPhrase(reason);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Header getFirstHeader(String name) {
		return (response != null ? response.getFirstHeader(name) : super.getFirstHeader(name));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Header getLastHeader(String name) {
		return (response != null ? response.getLastHeader(name) : super.getLastHeader(name));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsHeader(String name) {
		return (response != null ? response.containsHeader(name) : super.containsHeader(name));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Header[] getHeaders(String name) {
		return (response != null ? response.getHeaders(name) : super.getHeaders(name));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Header[] getAllHeaders() {
		return (response != null ? response.getAllHeaders() : super.getAllHeaders());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addHeader(Header header) {
		if (response != null) {
			response.addHeader(header);
		} else {
			super.addHeader(header);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHeader(Header header) {
		if (response != null) {
			response.setHeader(header);
		} else {
			super.setHeader(header);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHeaders(Header[] headers) {
		if (response != null) {
			response.setHeaders(headers);
		} else {
			super.setHeaders(headers);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeHeaders(String name) {
		if (response != null) {
			response.removeHeaders(name);
		} else {
			super.removeHeaders(name);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeHeader(Header header) {
		if (response != null) {
			response.removeHeader(header);
		} else {
			super.removeHeader(header);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HeaderIterator headerIterator() {
		return (response != null ? response.headerIterator() : super.headerIterator());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HeaderIterator headerIterator(String name) {
		return (response != null ? response.headerIterator(name) : super.headerIterator(name));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpEntity getEntity() {
		return (response != null ? response.getEntity() : super.getEntity());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEntity(HttpEntity entity) {
		if (response != null) {
			response.setEntity(entity);
		} else {
			super.setEntity(entity);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Locale getLocale() {
		return (response != null ? response.getLocale() : super.getLocale());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLocale(Locale loc) {
		if (response != null) {
			response.setLocale(loc);
		} else {
			super.setLocale(loc);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(response != null ? response.toString() : super.toString());
		try {
			String content = getContentString();
			if (!StringUtils.isEmpty(content)) {
				str.append("\n").append(content);
			}
		} catch (Exception e) {
		}
		return str.toString();
	}
}
