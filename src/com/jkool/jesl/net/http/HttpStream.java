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
package com.jkool.jesl.net.http;

import java.io.IOException;

import com.jkool.jesl.net.JKStream;

/**
 *
 *
 * @version $Revision: 1 $
 */
public interface HttpStream extends JKStream {
	void sendRequest(HttpRequest request, boolean wantResponse) throws IOException;
	void sendRequest(String method, String reqUri, String contentType, String content, boolean wantResponse) throws IOException;

	HttpResponse getResponse() throws IOException;
	HttpRequest  newRequest(String method, String uri);
	HttpResponse newResponse(String protocol, int major, int minor, int status);
}
