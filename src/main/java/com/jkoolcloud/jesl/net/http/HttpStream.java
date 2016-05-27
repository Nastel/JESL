/*
 * Copyright 2015 JKOOL, LLC.
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
package com.jkoolcloud.jesl.net.http;

import java.io.IOException;

import com.jkoolcloud.jesl.net.JKStream;

/**
 * This interface defines JESL HTTP based Stream based on
 * <code>JKStream</code>
 *
 * @see JKStream
 *
 * @version $Revision: 1 $
 */
public interface HttpStream extends JKStream {
	/**
	 * Send HTTP request
	 *
	 * @param request HTTP request message
	 * @param wantResponse block to wait for response from server
	 * @throws IOException if error writing request
	 */
	void sendRequest(HttpRequest request, boolean wantResponse) throws IOException;

	/**
	 * Send HTTP request
	 *
	 * @param method HTTP method
	 * @param reqUri request URI
	 * @param contentType content type
	 * @param content content message to be sent
	 * @param wantResponse block to wait for response from server
	 * @throws IOException if error writing request
	 */
	void sendRequest(String method, String reqUri, String contentType, String content, boolean wantResponse) throws IOException;

	/**
	 * Get/receive HTTP response
	 *
	 * @return HTTP response object
	 * @throws IOException if error reading response
	 */
	HttpResponse getResponse() throws IOException;

	/**
	 * Create a new response object with
	 * a given method and URI
	 *
	 * @param method HTTP method
	 * @param uri associated with HTTP request
	 * @return HTTP response object
	 */
	HttpRequest  newRequest(String method, String uri);

	/**
	 * Create a new response object with
	 *
	 * @param protocol HTTP protocol
	 * @param major protocol major version
	 * @param minor protocol minor version
	 * @param status HTTP status code
	 * @return HTTP response object
	 */
	HttpResponse newResponse(String protocol, int major, int minor, int status);
}
