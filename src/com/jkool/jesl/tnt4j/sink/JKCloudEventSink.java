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
package com.jkool.jesl.tnt4j.sink;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

import com.jkool.jesl.net.JKClient;
import com.nastel.jkool.tnt4j.core.KeyValueStats;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.Snapshot;
import com.nastel.jkool.tnt4j.format.EventFormatter;
import com.nastel.jkool.tnt4j.sink.AbstractEventSink;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.tracker.TrackingActivity;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;
import com.nastel.jkool.tnt4j.utils.Utils;

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

	public static final String KEY_SENT_BYTES = "sink-sent-bytes";
	public static final String KEY_LAST_BYTES = "sink-last-bytes";
	public static final String KEY_SENT_MSGS = "sink-sent-messages";
	public static final String KEY_SERVICE_URL = "sink-service-url";

	private EventSink logSink;
	private JKClient jkHandle;

	private String url = "localhost";
	private String proxyHost;
	private String accessToken;
	private int proxyPort = 0;
	private AtomicLong sentBytes = new AtomicLong(0);
	private AtomicLong lastBytes = new AtomicLong(0);
	private AtomicLong sentMsgs = new AtomicLong(0);

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
		this(name, url, null, frm, sink);
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
	public void resetStats() {
		sentBytes.set(0);
		lastBytes.set(0);
		sentMsgs.set(0);
		super.resetStats();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public KeyValueStats getStats(Map<String, Object> stats) {
		super.getStats(stats);
		stats.put(Utils.qualify(this, KEY_SENT_BYTES), sentBytes);
		stats.put(Utils.qualify(this, KEY_LAST_BYTES), lastBytes);
		stats.put(Utils.qualify(this, KEY_SENT_MSGS), sentMsgs);
		stats.put(Utils.qualify(this, KEY_SERVICE_URL), url);
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getSinkHandle() {
		return jkHandle;
	}

	/**
	 * Indicates whether connection to remote destination is open and available.
	 *
	 * @return {@code true} if connection open, {@code false} otherwise.
	 */
	@Override
	public synchronized boolean isOpen() {
		return (jkHandle != null && jkHandle.isConnected());
	}

	@Override
	public synchronized void open() throws IOException {
		try {
			close();
			logger.log(OpLevel.DEBUG, "Open name={3}, url={0}, proxy.host={1}, proxy.port={2}", url, proxyHost, proxyPort, this.getName());
			jkHandle = new JKClient(url, proxyHost, proxyPort, logger);
			if (!StringUtils.isEmpty(accessToken)) {
				jkHandle.connect(accessToken);
			} else {
				jkHandle.connect();
			}
			if (logSink != null && !logSink.isOpen()) {
				logSink.open();
			}
		} catch (URISyntaxException e) {
			logger.log(OpLevel.ERROR, "Failed to open name={3}, url={0}, proxy.host={1}, proxy.port={2}", url, proxyHost, proxyPort, this.getName(), e);
			close();
			throw new IOException(e.getMessage(), e);
		}
	}

	@Override
	public synchronized void close() throws IOException {
		if (isOpen()) {
			logger.log(OpLevel.DEBUG, "Closing name={3}, url={0}, proxy.host={1}, proxy.port={2}", url, proxyHost, proxyPort, this.getName());
			jkHandle.close();
		}
		if (logSink != null && logSink.isOpen()) {
			logSink.close();
		}
	}

	@Override
	public String toString() {
		return super.toString() + "(url: " + url + ", jk.handle: " + jkHandle + ", piped.sink: " + logSink + ")";
	}

	private void writeLine(String msg) throws IOException {
		if (StringUtils.isEmpty(msg)) return;
		_checkState();
		String lineMsg = msg.endsWith("\n") ? msg : msg + "\n";
		jkHandle.send(lineMsg, false);
		sentMsgs.incrementAndGet();
		lastBytes.set(lineMsg.length());
		sentBytes.addAndGet(lineMsg.length());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void _write(Object msg, Object... args) throws IOException {
		writeLine(getEventFormatter().format(msg, args));
	}

	@Override
	protected void _log(TrackingEvent event) throws IOException {
		if (logSink != null && logSink.isSet(event.getSeverity())) {
			logSink.log(event);
		}
		writeLine(getEventFormatter().format(event));
	}

	@Override
	protected void _log(TrackingActivity activity) throws IOException {
		if (logSink != null && logSink.isSet(activity.getSeverity())) {
			logSink.log(activity);
		}
		writeLine(getEventFormatter().format(activity));
	}

	@Override
	protected void _log(long ttl, Source src, OpLevel sev, String msg, Object... args) throws IOException {
		if (logSink != null && logSink.isSet(sev)) {
			logSink.log(ttl, src, sev, msg, args);
		}
		writeLine(getEventFormatter().format(ttl, src, sev, msg, args));
	}

	@Override
	protected void _log(Snapshot snapshot) throws Exception {
		if (logSink != null && logSink.isSet(snapshot.getSeverity())) {
			logSink.log(snapshot);
		}
		writeLine(getEventFormatter().format(snapshot));
	}

	@Override
	public boolean isSet(OpLevel sev) {
		return true;
	}

	@Override
	protected void _checkState() throws IllegalStateException {
		if (!isOpen())
			throw new IllegalStateException("Sink closed name=" + getName() + ", url=" + url + ", handle=" + jkHandle);
	}
}
