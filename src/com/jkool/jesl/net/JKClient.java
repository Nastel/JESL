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
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * JESL Client Stream class that enables client applications
 * to connect to JESL server using http, https, and tcp URI.
 * Example: http://data.jkoolcloud.com:6580.
 * Applications should use this class to connect to JESL servers
 * to stream events.
 * 
 * @version $Revision: 1 $
 */
public class JKClient implements JKStream {
	JKStream handle;
	boolean secure;
	URI uri;
	
	/**
	 * Create JESL client stream with given attributes
	 * 
	 * @param urlStr connection string to specified JESL server
	 * @param logger event sink used for logging, null if none
	 * 
	 */
	public JKClient(String urlStr, EventSink logger) throws URISyntaxException {
		this(urlStr, null, 0, logger);
	}

	/**
	 * Create JESL client stream with given attributes
	 * 
	 * @param urlStr connection string to specified JESL server
	 * @param proxyHost proxy host name if any, null if none
	 * @param proxyPort proxy port number if any, 0 of none
	 * @param logger event sink used for logging, null if none
	 * 
	 */
	public JKClient(String urlStr, String proxyHost, int proxyPort, EventSink logger) throws URISyntaxException {
		uri = new URI(urlStr);
		String scheme = uri.getScheme();
	    if (scheme.equalsIgnoreCase("tcp")) {
			String host = uri.getHost();
		    if (host == null) host = "localhost";

			secure = "tcps".equalsIgnoreCase(scheme);
		    int port = uri.getPort();
		    if (port <= 0) port = (secure ? 443 : 80);
	    	handle = new SocketClient(host, port, secure, proxyHost, proxyPort, logger);
	    } else {
			secure = "https".equalsIgnoreCase(scheme);
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
    public void send(String msg, boolean wantResponse) throws IOException {
		handle.send(msg, wantResponse);
	}

	@Override
    public String read() throws IOException {
	    return handle.read();
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
