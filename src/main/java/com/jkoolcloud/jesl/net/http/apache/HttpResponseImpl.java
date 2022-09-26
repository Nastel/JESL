/*
 * Copyright 2014-2022 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jkoolcloud.jesl.net.http.apache;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.message.HeaderGroup;
import org.apache.hc.core5.io.Closer;

import com.jkoolcloud.jesl.net.http.HttpResponse;

/**
 * Implementation of HttpResponse wrapping Apache HttpClient BasicHttpResponse class.
 *
 * @version $Revision: 1 $
 */
public class HttpResponseImpl extends BasicClassicHttpResponse implements HttpResponse {
	protected ClassicHttpResponse response;

	/**
	 * Create HTTP response object
	 * 
	 * @param response
	 *            apache HTTP response
	 */
	public HttpResponseImpl(ClassicHttpResponse response) {
		super(response.getCode());
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
		super(statusCode);
	}

	/**
	 * Obtain apache response implementation object instance
	 *
	 * @return apache response implementation object instance
	 */
	protected ClassicHttpResponse getRawResp() {
		return (response != null ? response : this);
	}

	///////////////////// HttpResponse methods

	@Override
	public void setStatus(int status) {
		if (response != null) {
			response.setCode(status);
		} else {
			super.setCode(status);
		}
	}

	@Override
	public int getStatus() {
		return response != null ? response.getCode() : super.getCode();
	}

	@Override
	public Header getHeader(String name) {
		return response != null ? response.getFirstHeader(name) : super.getFirstHeader(name);
	}

	@Override
	public String getHeaderStr(String name) {
		Header header = getHeader(name);
		return (header == null ? null : header.getValue());
	}

	@Override
	public void addHeader(String name, String value) {
		if (response != null) {
			response.addHeader(name, value);
		} else {
			super.addHeader(name, value);
		}
	}

	@Override
	public void setHeader(String name, String value) {
		if (response != null) {
			response.setHeader(name, value);
		} else {
			super.setHeader(name, value);
		}
	}

	@Override
	public void removeHeader(String name) {
		if (response != null) {
			response.removeHeaders(name);
		} else {
			super.removeHeaders(name);
		}
	}

	@Override
	public boolean hasContent() {
		return HttpMessageUtils.hasContent(getRawResp());
	}

	@Override
	public byte[] getContentBytes() throws IOException {
		return HttpMessageUtils.getContentBytes(getRawResp());
	}

	@Override
	public String getContentString() throws IOException, ParseException {
		return HttpMessageUtils.getContentString(getRawResp());
	}

	@Override
	public String getContentString(String charset) throws IOException, ParseException {
		return HttpMessageUtils.getContentString(getRawResp(), charset);
	}

	@Override
	public void setContent(String contentType, byte[] content, String contentEncoding) throws IOException {
		HttpMessageUtils.setContent(getRawResp(), contentType, content, contentEncoding);
	}

	@Override
	public void setContent(String contentType, String content) throws IOException {
		HttpMessageUtils.setContent(getRawResp(), contentType, content);
	}

	@Override
	public void setContent(String contentType, String content, String charset) throws IOException {
		HttpMessageUtils.setContent(getRawResp(), contentType, content, charset);
	}

	///////////////////// BasicClassicHttpResponse methods

	@Override
	public HttpEntity getEntity() {
		return (response != null ? response.getEntity() : super.getEntity());
	}

	@Override
	public void setEntity(HttpEntity entity) {
		if (response != null) {
			response.setEntity(entity);
		} else {
			super.setEntity(entity);
		}
	}

	@Override
	public void close() throws IOException {
		if (response != null) {
			Closer.close(response);
		} else {
			super.close();
		}
	}

	///////////////////// BasicHttpResponse methods

	@Override
	public void setVersion(ProtocolVersion version) {
		if (response != null) {
			response.setVersion(version);
		} else {
			super.setVersion(version);
		}
	}

	@Override
	public void addHeader(String name, Object value) {
		if (response != null) {
			response.addHeader(name, value);
		} else {
			super.addHeader(name, value);
		}
	}

	@Override
	public void setHeader(String name, Object value) {
		if (response != null) {
			response.setHeader(name, value);
		} else {
			super.setHeader(name, value);
		}
	}

	@Override
	public ProtocolVersion getVersion() {
		return (response != null ? response.getVersion() : super.getVersion());
	}

	@Override
	public void setCode(int code) {
		if (response != null) {
			response.setCode(code);
		} else {
			super.setCode(code);
		}
	}

	@Override
	public int getCode() {
		return response != null ? response.getCode() : super.getCode();
	}

	@Override
	public String getReasonPhrase() {
		return response != null ? response.getReasonPhrase() : super.getReasonPhrase();
	}

	@Override
	public void setReasonPhrase(String reason) {
		if (response != null) {
			response.setReasonPhrase(reason);
		} else {
			super.setReasonPhrase(reason);
		}
	}

	@Override
	public Header getFirstHeader(String name) {
		return (response != null ? response.getFirstHeader(name) : super.getFirstHeader(name));
	}

	@Override
	public Header getLastHeader(String name) {
		return (response != null ? response.getLastHeader(name) : super.getLastHeader(name));
	}

	@Override
	public boolean containsHeader(String name) {
		return (response != null ? response.containsHeader(name) : super.containsHeader(name));
	}

	@Override
	public Header getCondensedHeader(String name) {
		return (response instanceof HeaderGroup ? ((HeaderGroup) response).getCondensedHeader(name)
				: super.getCondensedHeader(name));
	}

	@Override
	public Header[] getHeaders(String name) {
		return (response != null ? response.getHeaders(name) : super.getHeaders(name));
	}

	@Override
	public Header[] getHeaders() {
		return (response != null ? response.getHeaders() : super.getHeaders());
	}

	@Override
	public void addHeader(Header header) {
		if (response != null) {
			response.addHeader(header);
		} else {
			super.addHeader(header);
		}
	}

	@Override
	public void clear() {
		if (response instanceof HeaderGroup) {
			((HeaderGroup) response).clear();
		} else {
			super.clear();
		}
	}

	@Override
	public boolean removeHeaders(Header header) {
		return response instanceof HeaderGroup ? ((HeaderGroup) response).removeHeaders(header)
				: super.removeHeaders(header);
	}

	@Override
	public void setHeader(Header header) {
		if (response != null) {
			response.setHeader(header);
		} else {
			super.setHeader(header);
		}
	}

	@Override
	public void setHeaders(Header... headers) {
		if (response != null) {
			response.setHeaders(headers);
		} else {
			super.setHeaders(headers);
		}
	}

	@Override
	public boolean removeHeaders(String name) {
		return response != null ? response.removeHeaders(name) : super.removeHeaders(name);
	}

	@Override
	public boolean removeHeader(Header header) {
		return response != null ? response.removeHeader(header) : super.removeHeader(header);
	}

	@Override
	public int countHeaders(String name) {
		return response != null ? response.countHeaders(name) : super.countHeaders(name);
	}

	@Override
	public Iterator<Header> headerIterator() {
		return (response != null ? response.headerIterator() : super.headerIterator());
	}

	@Override
	public Iterator<Header> headerIterator(String name) {
		return (response != null ? response.headerIterator(name) : super.headerIterator(name));
	}

	@Override
	public Locale getLocale() {
		return (response != null ? response.getLocale() : super.getLocale());
	}

	@Override
	public void setLocale(Locale loc) {
		if (response != null) {
			response.setLocale(loc);
		} else {
			super.setLocale(loc);
		}
	}

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
