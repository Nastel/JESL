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
package com.jkool.jesl.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.jkool.jesl.net.http.HttpClient;
import com.jkool.jesl.net.socket.SocketClient;
import com.jkool.jesl.net.socket.JKStream;
import com.nastel.jkool.tnt4j.sink.EventSink;

public class JKClient implements JKStream {
	JKStream handle;
	boolean secure;
	URI uri;
	
	public JKClient(String urlStr, String proxyHost, int proxyPort, EventSink logger) throws URISyntaxException {
		uri = new URI(urlStr);
		String scheme = uri.getScheme();
		secure = "https".equalsIgnoreCase(scheme);
	    if (scheme.equalsIgnoreCase("tcp")) {
			String host = uri.getHost();
		    if (host == null) host = "localhost";

		    int port = uri.getPort();
		    if (port <= 0) port = (secure ? 443 : 80);
	    	handle = new SocketClient(host, port, secure, proxyHost, proxyPort, logger);
	    } else {
	    	handle = new HttpClient(urlStr, proxyHost, proxyPort, logger);
	    }
	}

	@Override
    public String getHost() {
	    return uri.getHost();
    }

	@Override
    public int getPort() {
	    return uri.getPort();
    }

	@Override
    public boolean isSecure() {
	    return secure;
    }

	@Override
    public String getProxyHost() {
	    return handle.getProxyHost();
    }

	@Override
    public int getProxyPort() {
	    return handle.getProxyPort();
    }

	@Override
    public void connect() throws IOException {
		handle.connect();
	}

	@Override
    public void connect(String token) throws IOException {
		handle.connect(token);
	}

	@Override
    public boolean isConnected() {
	    return handle.isConnected();
    }

	@Override
    public void sendMessage(String msg, boolean wantResponse) throws IOException {
		handle.sendMessage(msg, wantResponse);
	}

	@Override
    public void sendRequest(String msg, boolean wantResponse) throws IOException {
		handle.sendRequest(msg, wantResponse);
    }

	@Override
    public String getReply() throws IOException {
	    return handle.getReply();
    }

	@Override
    public void close() {
		handle.close();
    }

	@Override
    public URI getURI() {
	    return handle.getURI();
    }
}
