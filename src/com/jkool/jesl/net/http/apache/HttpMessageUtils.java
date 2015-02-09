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
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;

/**
 * JESL HTTP common utilities
 *
 * @version $Revision: 1 $
 */
public class HttpMessageUtils {
	protected static HttpEntity getEntity(HttpMessage message) {
		if (message instanceof HttpEntityEnclosingRequest)
			return ((HttpEntityEnclosingRequest)message).getEntity();
		if (message instanceof HttpResponse)
			return ((HttpResponse)message).getEntity();
		return null;
	}

	protected static void setEntity(HttpMessage message, HttpEntity entity) {
		if (message instanceof HttpEntityEnclosingRequest)
			((HttpEntityEnclosingRequest)message).setEntity(entity);
		else if (message instanceof HttpResponse)
			((HttpResponse)message).setEntity(entity);
	}

	public static boolean hasContent(HttpMessage message) {
		return (getEntity(message) != null);
	}

	public static byte[] getContentBytes(HttpMessage message) throws IOException {
		HttpEntity entity = getEntity(message);
		if (entity == null)
			return null;
		return EntityUtils.toByteArray(entity);
	}

	public static String getContentString(HttpMessage message) throws IOException {
		HttpEntity entity = getEntity(message);
		if (entity == null)
			return null;
		return EntityUtils.toString(entity);
	}

	public static String getContentString(HttpMessage message, String charset) throws IOException {
		HttpEntity entity = getEntity(message);
		if (entity == null)
			return null;
		return EntityUtils.toString(entity, charset);
	}

	public static void setContent(HttpMessage message, String contentType, byte[] content, String contentEncoding) throws IOException {
		ByteArrayEntity httpContent = new ByteArrayEntity(content);
		if (!StringUtils.isEmpty(contentEncoding))
			httpContent.setContentEncoding(contentEncoding);
		message.addHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(httpContent.getContentLength()));
		if (!StringUtils.isEmpty(contentType))
			message.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
		setEntity(message, httpContent);
	}

	public static void setContent(HttpMessage message, String contentType, String content) throws IOException {
		setContent(message, contentType, content.getBytes(), null);
	}

	public static void setContent(HttpMessage message, String contentType, String content, String charset) throws IOException {
		setContent(message, contentType + "; charset=" + charset, content.getBytes(charset), charset);
	}
}
