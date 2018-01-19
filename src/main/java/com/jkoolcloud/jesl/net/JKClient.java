/*
 * Copyright 2015-2018 JKOOL, LLC.
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
package com.jkoolcloud.jesl.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.jkoolcloud.jesl.net.http.HttpClient;
import com.jkoolcloud.jesl.net.socket.SocketClient;
import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * JESL Client Stream class that enables client applications to connect to JESL server using http, https, and tcp[s]
 * URI. Example: http://data.jkoolcloud.com:6580. Applications should use this class to connect to JESL servers to
 * stream events.
 *
 * @version $Revision: 1 $
 */
public class JKClient implements JKStream {
	JKStream handle;
	URI uri;

	/**
	 * Create JESL client stream with given attributes
	 *
	 * @param urlStr
	 *            connection string to specified JESL server
	 * @param logger
	 *            event sink used for logging, null if none
	 * @throws URISyntaxException
	 *             if invalid connection string
	 */
	public JKClient(String urlStr, EventSink logger) throws URISyntaxException {
		this(urlStr, null, 0, null, logger);
	}

	/**
	 * Create JESL client stream with given attributes
	 *
	 * @param urlStr
	 *            connection string to specified JESL server
	 * @param proxyHost
	 *            proxy host name if any, null if none
	 * @param proxyPort
	 *            proxy port number if any, 0 of none
	 * @param proxyScheme
	 *            proxy communication scheme
	 * @param logger
	 *            event sink used for logging, null if none
	 * @throws URISyntaxException
	 *             if invalid connection string
	 */
	public JKClient(String urlStr, String proxyHost, int proxyPort, String proxyScheme, EventSink logger)
			throws URISyntaxException {
		uri = new URI(urlStr);
		String scheme = uri.getScheme();
		if (scheme.equalsIgnoreCase("tcp") || scheme.equalsIgnoreCase("tcps")) {
			String host = uri.getHost();
			if (host == null) {
				host = "localhost";
			}
			boolean secure = "tcps".equalsIgnoreCase(scheme);
			int port = uri.getPort();
			if (port <= 0) {
				port = (secure ? 443 : 80);
			}
			handle = new SocketClient(host, port, secure, proxyHost, proxyPort, logger);
		} else {
			handle = new HttpClient(urlStr, proxyHost, proxyPort, proxyScheme, logger);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHost() {
		return uri.getHost();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPort() {
		return uri.getPort();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSecure() {
		return handle.isSecure();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getProxyHost() {
		return handle.getProxyHost();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getProxyPort() {
		return handle.getProxyPort();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void connect() throws IOException {
		handle.connect();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void connect(String token) throws IOException {
		handle.connect(token);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isConnected() {
		return handle.isConnected();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void send(String msg, boolean wantResponse) throws IOException {
		handle.send(msg, wantResponse);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String read() throws IOException {
		return handle.read();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		handle.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public URI getURI() {
		return handle.getURI();
	}
}
