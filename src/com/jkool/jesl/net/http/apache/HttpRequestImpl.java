/*
 * Copyright 2015 Nastel Technologies, Inc.
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
package com.jkool.jesl.net.http.apache;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.HttpParams;
import org.jboss.netty.handler.codec.http.HttpMethod;

import com.jkool.jesl.net.http.HttpRequest;

/**
 *
 *
 * @version $Revision: 1 $
 */
public class HttpRequestImpl extends BasicHttpEntityEnclosingRequest implements HttpRequest {
	protected HttpEntityEnclosingRequest request;

	public HttpRequestImpl(HttpEntityEnclosingRequest request) {
		super(request.getRequestLine());
		this.request = request;
	}

	public HttpRequestImpl(String method, String uri) {
		super(method, uri, HttpVersion.HTTP_1_1);
	}

	protected HttpEntityEnclosingRequest getRawReq() {
		return (request != null ? request : this);
	}

	///////////////////// HttpRequest methods

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(getRawReq().getRequestLine().getMethod());
	}

	@Override
	public String getUri() {
		return getRawReq().getRequestLine().getUri();
	}

	@Override
	public String getHeader(String name) {
		Header header = getRawReq().getFirstHeader(name);
		return (header == null ? null : header.getValue());
	}

	@Override
	public void addHeader(String name, String value) {
		if (request != null)
			request.addHeader(name, value);
		else
			super.addHeader(name, value);
	}

	@Override
	public void setHeader(String name, String value) {
		if (request != null)
			request.setHeader(name, value);
		else
			super.setHeader(name, value);
	}

	@Override
	public void removeHeader(String name) {
		getRawReq().removeHeaders(name);
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
	public String getContentString() throws IOException {
		return HttpMessageUtils.getContentString(getRawReq());
	}

	@Override
	public String getContentString(String charset) throws IOException {
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
	public ProtocolVersion getProtocolVersion() {
		return (request != null ? request.getProtocolVersion() : super.getProtocolVersion());
	}

	@Override
	public RequestLine getRequestLine() {
		return (request != null ? request.getRequestLine() : super.getRequestLine());
	}

	@Override
	public Header getFirstHeader(String name) {
		return (request != null ? request.getFirstHeader(name) : super.getFirstHeader(name));
	}

	@Override
	public Header getLastHeader(String name) {
		return (request != null ? request.getLastHeader(name) : super.getLastHeader(name));
	}

	@Override
	public Header[] getAllHeaders() {
		return (request != null ? request.getAllHeaders() : super.getAllHeaders());
	}

	@Override
	public void addHeader(Header header) {
		if (request != null)
			request.addHeader(header);
		else
			super.addHeader(header);
	}

	@Override
	public void setHeader(Header header) {
		if (request != null)
			request.setHeader(header);
		else
			super.setHeader(header);
	}

	@Override
	public void setHeaders(Header[] headers) {
		if (request != null)
			request.setHeaders(headers);
		else
			super.setHeaders(headers);
	}

	@Override
	public void removeHeader(Header header) {
		if (request != null)
			request.removeHeader(header);
		else
			super.removeHeader(header);
	}

	@Override
	public HeaderIterator headerIterator() {
		return (request != null ? request.headerIterator() : super.headerIterator());
	}

	@Override
	public HeaderIterator headerIterator(String name) {
		return (request != null ? request.headerIterator(name) : super.headerIterator(name));
	}

	@Override
	public boolean containsHeader(String name) {
		return (request != null ? request.containsHeader(name) : super.containsHeader(name));
	}

	@Override
	public Header[] getHeaders(String name) {
		return (request != null ? request.getHeaders(name) : super.getHeaders(name));
	}

	@Override
	public void removeHeaders(String name) {
		if (request != null)
			request.removeHeaders(name);
		else
			super.removeHeaders(name);
	}

	@Override
	public HttpParams getParams() {
		return (request != null ? request.getParams() : super.getParams());
	}

	@Override
	public void setParams(HttpParams params) {
		if (request != null)
			request.setParams(params);
		else
			super.setParams(params);
	}

	@Override
	public HttpEntity getEntity() {
		return (request != null ? request.getEntity() : super.getEntity());
	}

	@Override
	public void setEntity(HttpEntity entity) {
		if (request != null)
			request.setEntity(entity);
		else
			super.setEntity(entity);
	}

	@Override
	public boolean expectContinue() {
		return (request != null ? request.expectContinue() : super.expectContinue());
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(request != null ? request.toString() : super.toString());

		try {
			String content = getContentString();
			if (!StringUtils.isEmpty(content))
				str.append("\n").append(content);
		}
		catch (Exception e) {}
		return str.toString();
	}
}
