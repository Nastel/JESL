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
package com.jkool.jesl.net.socket;

import java.io.IOException;
import java.net.URI;

/**
 *
 *
 * @version $Revision: 1 $
 */
public interface SocketConnection {
	URI getURI();
	String getHost();
	int getPort();

	boolean isSecure();

	String getProxyHost();
	int getProxyPort();

	void connect() throws IOException;
	void connect(String token) throws IOException;
	boolean isConnected();

	void sendMessage(String msg, boolean wantResponse) throws IOException;
	void sendRequest(String msg, boolean wantResponse) throws IOException;
	String getReply() throws IOException;

	void close();
}
