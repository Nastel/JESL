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
package com.jkoolcloud.jesl.simulator.tnt4j;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.jesl.net.JKClient;
import com.jkoolcloud.tnt4j.TrackingLogger;
import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * JESL Cloud connection class that encapsulates streaming to jKoolCloud servers using HTTP[S] or TCP[S] protocols.
 *
 * @version $Revision: $
 */
public class JKCloudConnection implements Closeable {
	private String gwUrl;
	private String accessToken;
	private long connTimeout;
	private boolean ackSends = false;
	private boolean disableSSLVerification = false;

	private JKClient jkHandle;
	private TrackingLogger logger;

	public JKCloudConnection(String url, String accessToken, long connTimeout, boolean acks,
			boolean disableSSLVerification, TrackingLogger logger) {
		this.gwUrl = url.toLowerCase();
		this.accessToken = accessToken;
		this.connTimeout = connTimeout;
		this.ackSends = acks;
		this.disableSSLVerification = disableSSLVerification;
		this.logger = logger;
	}

	protected EventSink getEventSink() {
		return (logger == null ? null : logger.getEventSink());
	}

	public boolean isOpen() {
		return (jkHandle != null);
	}

	public synchronized void open() throws IOException {
		if (isOpen()) {
			return;
		}

		try {
			jkHandle = new JKClient(gwUrl, connTimeout, connTimeout / 5, disableSSLVerification, getEventSink());
			jkHandle.connect(accessToken);
		} catch (URISyntaxException e) {
			close();
			throw new IOException(e.getMessage(), e);
		}
	}

	public synchronized void write(String msg) throws Exception {
		if (StringUtils.isEmpty(msg)) {
			return;
		}

		if (!isOpen()) {
			open();
		}

		try {
			if (jkHandle != null) {
				jkHandle.send(accessToken, msg, ackSends);
				if (ackSends) {
					jkHandle.read();
				}
			} else {
				throw new IOException("Connection closed");
			}
		} catch (Exception e) {
			close();
			throw e;
		}
	}

	@Override
	public synchronized void close() throws IOException {
		if (jkHandle != null) {
			jkHandle.close();
			jkHandle = null;
		}
	}

	@Override
	public String toString() {
		return gwUrl;
	}
}
