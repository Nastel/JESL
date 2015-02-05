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
package com.jkool.jesl.simulator.tnt4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import org.apache.commons.lang3.StringUtils;

import com.jkool.jesl.net.http.HttpClient;
import com.jkool.jesl.net.socket.SocketClient;
import com.nastel.jkool.tnt4j.TrackingLogger;

public class JKCloudConnection {
	private String gwUrl;
	private String accessToken;

	private SocketClient	taTcpConn;
	private HttpClient		taHttpConn;
	private TrackingLogger	logger;

	public JKCloudConnection(String url, String accessToken, TrackingLogger logger) {
		this.gwUrl = url.toLowerCase();
		this.accessToken = accessToken;
		this.logger = logger;
	}

	public boolean isOpen() {
		return (taTcpConn != null || taHttpConn != null);
	}
	public void open() throws IOException {
		if (isOpen())
			return;

		try {
			URI uri = new URI(gwUrl);
			String protocol = uri.getScheme();
			if (StringUtils.isEmpty(protocol))
				throw new MalformedURLException("Protocol not specified");

			if ("http".equalsIgnoreCase(protocol)) {
				taHttpConn = new HttpClient(gwUrl, (logger == null ? null : logger.getEventSink()));

				if (!StringUtils.isEmpty(accessToken))
					taHttpConn.connect(accessToken);
				else
					taHttpConn.connect();
			}
			else if ("https".equalsIgnoreCase(protocol)) {
				taHttpConn = new HttpClient(gwUrl, (logger == null ? null : logger.getEventSink()));
//				if (!StringUtils.isEmpty(sslKeystore))
//					taHttpConn.setSslKeystore(sslKeystore, sslKeystorePwd);

				if (!StringUtils.isEmpty(accessToken))
					taHttpConn.connect(accessToken);
				else
					taHttpConn.connect();
			}
			else if ("tcp".equalsIgnoreCase(protocol)) {
				String host = uri.getHost();
				int port = uri.getPort();
				taTcpConn = new SocketClient(host, port, false, (logger == null ? null : logger.getEventSink()));
				taTcpConn.connect();
			}
			else {
				throw new IOException("Unsupported protocol " + protocol + " for gateway connection");
			}
		}
		catch (Throwable e) {
			if ((e instanceof IOException))
				throw (IOException)e;
			close();
			IOException ioe = new IOException("Failed opening connection");
			ioe.initCause(e);
			throw ioe;
		}
	}

	public void write(String msg) throws IOException {
		if (StringUtils.isEmpty(msg))
			return;

		if (!isOpen())
			open();

		try {
			if (taHttpConn != null)
				taHttpConn.sendMessage(msg, false);
			else if (taTcpConn != null)
				taTcpConn.sendMessage(msg, false);
			else
				throw new IOException("Connection not opened");
		}
		catch (Throwable e) {
			close();
			if (e instanceof IOException)
				throw (IOException)e;
			IOException ioe = new IOException("Failed writing to connection");
			ioe.initCause(e);
			throw ioe;
		}
	}

	public void close() throws IOException {
		if (taTcpConn != null) {
			taTcpConn.close();
			taTcpConn = null;
		}

		if (taHttpConn != null) {
			taHttpConn.close();
			taHttpConn = null;
		}
	}

	@Override
	public String toString() {
		return gwUrl;
	}
}
