/*
 * Copyright 2014-2023 JKOOL, LLC.
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
package com.jkoolcloud.jesl.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.jesl.net.http.HttpClient;
import com.jkoolcloud.jesl.net.socket.SocketClient;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.utils.Utils;

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
	 * @param connTimeout
	 *            connection timeout in milliseconds
	 * @param disableSSLVerification
	 *            flag indicating to disable SSL validation
	 * @param logger
	 *            event sink used for logging, null if none
	 * @throws URISyntaxException
	 *             if invalid connection string
	 */
	public JKClient(String urlStr, long connTimeout, boolean disableSSLVerification, EventSink logger)
			throws URISyntaxException {
		this(urlStr, connTimeout, disableSSLVerification, null, 0, null, logger);
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
		this(urlStr, HttpClient.DEFAULT_CONN_TIMEOUT, false, proxyHost, proxyPort, proxyScheme, logger);
	}

	/**
	 * Create JESL client stream with given attributes
	 *
	 * @param urlStr
	 *            connection string to specified JESL server
	 * @param connTimeout
	 *            connection timeout in seconds
	 * @param disableSSLVerification
	 *            flag indicating to disable SSL validation
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
	public JKClient(String urlStr, long connTimeout, boolean disableSSLVerification, String proxyHost, int proxyPort,
			String proxyScheme, EventSink logger) throws URISyntaxException {
		this(urlStr, connTimeout, disableSSLVerification, proxyHost, proxyPort, proxyScheme, null, null, logger);
	}

	/**
	 * Create JESL client stream with given attributes
	 *
	 * @param urlStr
	 *            connection string to specified JESL server
	 * @param connTimeout
	 *            connection timeout in seconds
	 * @param disableSSLVerification
	 *            flag indicating to disable SSL validation
	 * @param proxyHost
	 *            proxy host name if any, null if none
	 * @param proxyPort
	 *            proxy port number if any, 0 of none
	 * @param proxyScheme
	 *            proxy communication scheme
	 * @param proxyUser
	 *            proxy authentication user
	 * @param proxyPass
	 *            proxy authentication password
	 * @param logger
	 *            event sink used for logging, null if none
	 * @throws URISyntaxException
	 *             if invalid connection string
	 */
	public JKClient(String urlStr, long connTimeout, boolean disableSSLVerification, String proxyHost, int proxyPort,
			String proxyScheme, String proxyUser, String proxyPass, EventSink logger) throws URISyntaxException {
		uri = new URI(urlStr);
		String scheme = uri.getScheme();
		if (StringUtils.equalsAnyIgnoreCase(scheme, "tcp", "tcps")) {
			String host = uri.getHost();
			if (host == null) {
				host = "localhost";
			}
			boolean secure = "tcps".equalsIgnoreCase(scheme);
			int port = uri.getPort();
			if (port <= 0) {
				port = (secure ? 443 : 80);
			}

			if (StringUtils.isNotEmpty(proxyUser)) {
				if (System.getProperty("java.net.socks.username") == null) {
					System.setProperty("java.net.socks.username", proxyUser);
				}
				if (System.getProperty("java.net.socks.password") == null) {
					System.setProperty("java.net.socks.password", proxyPass);
				}
			}

			handle = new SocketClient(host, port, secure, proxyHost, proxyPort, logger);
		} else {
			handle = new HttpClient(urlStr, connTimeout, disableSSLVerification, proxyHost, proxyPort, proxyScheme,
					proxyUser, proxyPass, logger);
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
		return handle.isSecure();
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
	public void send(String token, String msg, boolean wantResponse) throws IOException {
		handle.send(token, msg, wantResponse);
	}

	@Override
	public String read() throws Exception {
		return handle.read();
	}

	@Override
	public void close() {
		Utils.close(handle);
	}

	@Override
	public URI getURI() {
		return handle.getURI();
	}

	@Override
	public String toString() {
		return handle.toString();
	}
}
