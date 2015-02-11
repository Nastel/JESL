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
package com.jkool.jesl.tnt4j.sink;

import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;

import com.jkool.jesl.net.JKClient;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.Snapshot;
import com.nastel.jkool.tnt4j.format.EventFormatter;
import com.nastel.jkool.tnt4j.sink.AbstractEventSink;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.tracker.TrackingActivity;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;

/**
 * <p>
 * This class implements <code>EventSink</code> with HTTP/S as the underlying sink implementation.
 * </p>
 *
 *
 * @version $Revision: 7 $
 *
 * @see TrackingActivity
 * @see TrackingEvent
 * @see OpLevel
 * @see EventSink
 * @see EventFormatter
 */
public class JKCloudEventSink extends AbstractEventSink {
	private static final EventSink logger = DefaultEventSinkFactory.defaultEventSink(JKCloudEventSink.class);

	private Socket socketSink = null;
	private EventSink logSink = null;
	private JKClient jkHandle;

	private String url = "localhost";
	private String proxyHost = null;
	private int proxyPort = 0;
	private String accessToken = null;

	/**
	 * Create a socket event sink based on a given URL and formatter. Another sink can be associated with this sink
	 * where all events are routed.
	 *
	 * @param name
	 *            sink name
	 * @param url
	 *            http/https URL to jkool cloud service
	 * @param frm
	 *            event formatter associated with this sink
	 * @param sink
	 *            piped sink where all events are piped
	 *
	 */
	public JKCloudEventSink(String name, String url, EventFormatter frm, EventSink sink) {
		super(name, frm);
		this.url = url;
		logSink = sink;
	}

	/**
	 * Create a socket event sink based on a given URL and formatter. Another sink can be associated with this sink
	 * where all events are routed.
	 *
	 * @param name
	 *            sink name
	 * @param url
	 *            http/https URL to jkool cloud service
	 * @param token
	 *            api access token
	 * @param frm
	 *            event formatter associated with this sink
	 * @param sink
	 *            piped sink where all events are piped
	 *
	 */
	public JKCloudEventSink(String name, String url, String token, EventFormatter frm, EventSink sink) {
		super(name, frm);
		this.url = url;
		this.logSink = sink;
		this.accessToken = token;
	}

	/**
	 * Gets the access token used to establish authenticated connections to Analyzer.
	 *
	 * @return access token
	 */
	public String getAccessToken() {
		return accessToken;
	}

	/**
	 * Sets the access token to use when establishing authenticated connections to Analyzer.
	 *
	 * @param accessToken
	 *            the access token
	 */
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(Object msg, Object... args) throws IOException {
		if (isOpen()) {
			writeLine(getEventFormatter().format(msg, args));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getSinkHandle() {
		return socketSink;
	}

	/**
	 * Indicates whether connection to remote destination is open and available.
	 *
	 * @return {@code true} if connection open, {@code false} otherwise.
	 */
	@Override
	public synchronized boolean isOpen() {
		return (jkHandle != null);
	}

	@Override
	public synchronized void open() throws IOException {
		try {
			jkHandle = new JKClient(url, proxyHost, proxyPort, logger);
			if (!StringUtils.isEmpty(accessToken)) {
				jkHandle.connect(accessToken);
			} else {
				jkHandle.connect();
			}
		} catch (URISyntaxException e) {
			close();
			throw new IOException(e.getMessage(), e);
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
		return super.toString() + "(url: " + url + ", piped.sink: " + logSink + ")";
	}

	private void writeLine(String msg) throws IOException {
		if (StringUtils.isEmpty(msg))
			return;
		String lineMsg = msg.endsWith("\n") ? msg : msg + "\n";
		jkHandle.send(lineMsg, false);
	}

	@Override
	protected void _log(TrackingEvent event) throws IOException {
		if (logSink != null) {
			logSink.log(event);
		}
		writeLine(getEventFormatter().format(event));
	}

	@Override
	protected void _log(TrackingActivity activity) throws IOException {
		if (logSink != null) {
			logSink.log(activity);
		}
		writeLine(getEventFormatter().format(activity));
	}

	@Override
	protected void _log(Source src, OpLevel sev, String msg, Object... args) throws IOException {
		if (logSink != null) {
			logSink.log(src, sev, msg, args);
		}
		writeLine(getEventFormatter().format(src, sev, msg, args));
	}

	@Override
	protected void _log(Snapshot snapshot) throws Exception {
		if (logSink != null) {
			logSink.log(snapshot);
		}
		writeLine(getEventFormatter().format(snapshot));
	}

	@Override
	public boolean isSet(OpLevel sev) {
		return logSink != null ? logSink.isSet(sev) : true;
	}

	@Override
	protected void _checkState() throws IllegalStateException {
		if (!isOpen())
			throw new IllegalStateException("Sink closed url=" + url + ", socket=" + socketSink);
	}
}
