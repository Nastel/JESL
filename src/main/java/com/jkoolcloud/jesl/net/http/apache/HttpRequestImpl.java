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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.ProtocolVersion;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.net.URIAuthority;

import com.jkoolcloud.jesl.net.http.HttpRequest;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * JESL HTTP Request implementation class based on Apache HTTP Core package.
 *
 * @version $Revision: 1 $
 */
public class HttpRequestImpl extends BasicClassicHttpRequest implements HttpRequest {
	public static final String CLIENT_HOSTNAME = "X-API-Host-Name";
	public static final String CLIENT_HOSTADDR = "X-API-Host-Addr";
	public static final String CLIENT_RUNTIME = "X-API-Runtime";
	public static final String CLIENT_VERSION = "X-API-Version";

	private static final String VALUE_VERSION = HttpRequestImpl.class.getPackage().getImplementationVersion();
	private static final String VALUE_HOSTNAME = Utils.getLocalHostName();
	private static final String VALUE_HOSTADDR = Utils.getLocalHostAddress();
	private static final String VALUE_VMNAME = Utils.getVMName();

	protected BasicClassicHttpRequest request;

	/**
	 * Create HTTP request object
	 * 
	 * @param request
	 *            apache HTTP request
	 */
	public HttpRequestImpl(BasicClassicHttpRequest request) throws URISyntaxException {
		super(request.getMethod(), request.getUri());
		this.request = request;
		initHeader(this);
	}

	/**
	 * Create HTTP request object
	 * 
	 * @param method
	 *            HTTP method
	 * @param uri
	 *            URI
	 */
	public HttpRequestImpl(String method, String uri) {
		super(method, URI.create(uri));
		initHeader(this);
	}

	/**
	 * Obtain apache request implementation object instance
	 *
	 * @return apache request implementation object instance
	 */
	protected BasicClassicHttpRequest getRawReq() {
		return (request != null ? request : this);
	}

	/**
	 * Initialize HTTP header with default fields
	 *
	 * @param request
	 *            apache HTTP request
	 */
	protected static void initHeader(BasicClassicHttpRequest request) {
		request.addHeader(CLIENT_HOSTNAME, VALUE_HOSTNAME);
		request.addHeader(CLIENT_HOSTADDR, VALUE_HOSTADDR);
		request.addHeader(CLIENT_RUNTIME, VALUE_VMNAME);
		if (VALUE_VERSION != null) {
			request.addHeader(CLIENT_VERSION, VALUE_VERSION);
		}
	}
	///////////////////// BasicHttpRequest methods

	@Override
	public void addHeader(String name, Object value) {
		if (request != null) {
			request.addHeader(name, value);
		} else {
			super.addHeader(name, value);
		}
	}

	@Override
	public void setHeader(String name, Object value) {
		if (request != null) {
			request.setHeader(name, value);
		} else {
			super.setHeader(name, value);
		}
	}

	@Override
	public void setVersion(ProtocolVersion version) {
		if (request != null) {
			request.setVersion(version);
		} else {
			super.setVersion(version);
		}
	}

	@Override
	public ProtocolVersion getVersion() {
		return request != null ? request.getVersion() : super.getVersion();
	}

	@Override
	public String getMethod() {
		return request != null ? request.getMethod() : super.getMethod();
	}

	@Override
	public String getPath() {
		return request != null ? request.getPath() : super.getPath();
	}

	@Override
	public void setPath(String path) {
		if (request != null) {
			request.setPath(path);
		} else {
			super.setPath(path);
		}
	}

	@Override
	public String getScheme() {
		return request != null ? request.getScheme() : super.getScheme();
	}

	@Override
	public void setScheme(String scheme) {
		if (request != null) {
			request.setScheme(scheme);
		} else {
			super.setScheme(scheme);
		}
	}

	@Override
	public URIAuthority getAuthority() {
		return request != null ? request.getAuthority() : super.getAuthority();
	}

	@Override
	public void setAuthority(URIAuthority authority) {
		if (request != null) {
			request.setAuthority(authority);
		} else {
			super.setAuthority(authority);
		}
	}

	@Override
	public void setUri(URI requestUri) {
		if (request != null) {
			request.setUri(requestUri);
		} else {
			super.setUri(requestUri);
		}
	}

	@Override
	public URI getUri() throws URISyntaxException {
		return request != null ? request.getUri() : super.getUri();
	}

	@Override
	public String getRequestUri() {
		return request != null ? request.getRequestUri() : super.getRequestUri();
	}

	@Override
	public String getUriStr() {
		try {
			return getUri().toString();
		} catch (URISyntaxException exc) {
			return getRequestUri();
		}
	}

	@Override
	public Header getHeader(String name) {
		return request != null ? request.getFirstHeader(name) : super.getFirstHeader(name);
	}

	@Override
	public String getHeaderStr(String name) {
		Header header = getHeader(name);
		return (header == null ? null : header.getValue());
	}

	@Override
	public void addHeader(String name, String value) {
		if (request != null) {
			request.addHeader(name, value);
		} else {
			super.addHeader(name, value);
		}
	}

	@Override
	public void setHeader(String name, String value) {
		if (request != null) {
			request.setHeader(name, value);
		} else {
			super.setHeader(name, value);
		}
	}

	@Override
	public void removeHeader(String name) {
		if (request != null) {
			request.removeHeaders(name);
		} else {
			super.removeHeaders(name);
		}
	}

	@Override
	public boolean hasContent() {
		return HttpMessageUtils.hasContent(getRawReq());
	}

	@Override
	public byte[] getContentBytes() throws IOException {
		return HttpMessageUtils.getContentBytes(getRawReq());
	}

	@Override
	public String getContentString() throws IOException, ParseException {
		return HttpMessageUtils.getContentString(getRawReq());
	}

	@Override
	public String getContentString(String charset) throws IOException, ParseException {
		return HttpMessageUtils.getContentString(getRawReq(), charset);
	}

	@Override
	public void setContent(String contentType, byte[] content, String contentEncoding) throws IOException {
		HttpMessageUtils.setContent(getRawReq(), contentType, content, contentEncoding);
	}

	@Override
	public void setContent(String contentType, String content) throws IOException {
		HttpMessageUtils.setContent(getRawReq(), contentType, content);
	}

	@Override
	public void setContent(String contentType, String content, String charset) throws IOException {
		HttpMessageUtils.setContent(getRawReq(), contentType, content, charset);
	}

	///////////////////// BasicHttpEntityEnclosingRequest methods

	@Override
	public Header getFirstHeader(String name) {
		return (request != null ? request.getFirstHeader(name) : super.getFirstHeader(name));
	}

	@Override
	public Header getLastHeader(String name) {
		return (request != null ? request.getLastHeader(name) : super.getLastHeader(name));
	}

	@Override
	public Header[] getHeaders() {
		return (request != null ? request.getHeaders() : super.getHeaders());
	}

	@Override
	public void clear() {
		if (request != null) {
			request.clear();
		} else {
			super.clear();
		}
	}

	@Override
	public void addHeader(Header header) {
		if (request != null) {
			request.addHeader(header);
		} else {
			super.addHeader(header);
		}
	}

	@Override
	public boolean removeHeaders(Header header) {
		return request != null ? request.removeHeaders(header) : super.removeHeaders(header);
	}

	@Override
	public void setHeader(Header header) {
		if (request != null) {
			request.setHeader(header);
		} else {
			super.setHeader(header);
		}
	}

	@Override
	public void setHeaders(Header... headers) {
		if (request != null) {
			request.setHeaders(headers);
		} else {
			super.setHeaders(headers);
		}
	}

	@Override
	public boolean removeHeader(Header header) {
		return request != null ? request.removeHeader(header) : super.removeHeader(header);
	}

	@Override
	public Iterator<Header> headerIterator() {
		return (request != null ? request.headerIterator() : super.headerIterator());
	}

	@Override
	public int countHeaders(String name) {
		return request != null ? request.countHeaders(name) : super.countHeaders(name);
	}

	@Override
	public Iterator<Header> headerIterator(String name) {
		return (request != null ? request.headerIterator(name) : super.headerIterator(name));
	}

	@Override
	public boolean containsHeader(String name) {
		return (request != null ? request.containsHeader(name) : super.containsHeader(name));
	}

	@Override
	public Header getCondensedHeader(String name) {
		return (request != null ? request.getCondensedHeader(name) : super.getCondensedHeader(name));
	}

	@Override
	public Header[] getHeaders(String name) {
		return (request != null ? request.getHeaders(name) : super.getHeaders(name));
	}

	@Override
	public boolean removeHeaders(String name) {
		return request != null ? request.removeHeaders(name) : super.removeHeaders(name);
	}

	@Override
	public HttpEntity getEntity() {
		return (request != null ? request.getEntity() : super.getEntity());
	}

	@Override
	public void setEntity(HttpEntity entity) {
		if (request != null) {
			request.setEntity(entity);
		} else {
			super.setEntity(entity);
		}
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(request != null ? request.toString() : super.toString());

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
