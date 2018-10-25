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
package com.jkoolcloud.jesl.tnt4j.sink;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.jesl.net.JKClient;
import com.jkoolcloud.tnt4j.core.KeyValueStats;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.sink.LoggedEventSink;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * <p>
 * This class implements {@link EventSink} with HTTP/S as the underlying sink implementation.
 * </p>
 *
 *
 * @version $Revision: 8 $
 *
 * @see TrackingActivity
 * @see TrackingEvent
 * @see OpLevel
 * @see EventSink
 * @see EventFormatter
 */
public class JKCloudEventSink extends LoggedEventSink {
	private static final EventSink logger = DefaultEventSinkFactory.defaultEventSink(JKCloudEventSink.class);

	public static final String KEY_IDLE_COUNT = "sink-idle-count";
	public static final String KEY_LAST_BYTES = "sink-last-bytes";
	public static final String KEY_SENT_MSGS = "sink-sent-messages";
	public static final String KEY_SERVICE_URL = "sink-service-url";
	public static final String KEY_PROXY_URL = "sink-proxy-url";
	public static final String KEY_LAST_WAGE = "sink-last-write-age-ms";

	private JKClient jkHandle;

	private String url = "localhost";
	private String accessToken;

	private String proxyScheme = "http";
	private String proxyHost;
	private int proxyPort = 0;
	private Integer connTimeout;

	private long idleTimeout = 0;

	private AtomicLong idleCount = new AtomicLong(0);
	private AtomicLong lastWrite = new AtomicLong(0);
	private AtomicLong lastBytes = new AtomicLong(0);
	private AtomicLong sentMsgs = new AtomicLong(0);

	/**
	 * Create a socket event sink based on a given URL and formatter. Another sink can be associated with this sink
	 * where all events are routed.
	 *
	 * @param name
	 *            sink name
	 * @param url
	 *            http/https URL to jKoolCloud service
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
	 *            http/https URL to jKoolCloud service
	 * @param token
	 *            api access token
	 * @param frm
	 *            event formatter associated with this sink
	 * @param sink
	 *            piped sink where all events are piped
	 *
	 */
	public JKCloudEventSink(String name, String url, String token, EventFormatter frm, EventSink sink) {
		super(name, frm, sink);
		this.url = url;
		this.accessToken = token;
	}

	/**
	 * Sets proxy communication parameters
	 *
	 * @param scheme
	 *            proxy communication scheme
	 * @param host
	 *            proxy host name if any, null if none
	 * @param port
	 *            proxy port number if any, 0 of none
	 */
	public void setProxyParms(String scheme, String host, int port) {
		this.proxyScheme = scheme;
		this.proxyHost = host;
		this.proxyPort = port;
	}

	/**
	 * Sets connection timeout.
	 *
	 * @param connTimeout
	 *            connection timeout in seconds
	 */
	public void setConnectionTimeout(Integer connTimeout) {
		this.connTimeout = connTimeout;
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
	 * Sets idle timeout for the sink. Connection is dropped on next write after timeout.
	 *
	 * @param timeOut
	 *            idle timeout
	 * @param tunit
	 *            time out time units
	 */
	public void setIdleTimeout(long timeOut, TimeUnit tunit) {
		this.idleTimeout = tunit.toMillis(timeOut);
	}

	/**
	 * Gets idle timeout in milliseconds
	 *
	 * @return idle timeout in ms.
	 */
	public long getIdleTimeout() {
		return idleTimeout;
	}

	/**
	 * Gets last write time stamp
	 *
	 * @return last write time stamp
	 */
	public long getLastWriteTime() {
		return lastWrite.get();
	}

	/**
	 * Gets last write age in ms
	 *
	 * @return last write age in ms
	 */
	public long getLastWriteAge() {
		return lastWrite.get() > 0 ? System.currentTimeMillis() - lastWrite.get() : 0;
	}

	/**
	 * Handle connection idle timeout, attempt to close and reopen to avoid data loss.
	 *
	 * @throws IOException
	 *             if IO error occurs while closing/opening sink handle
	 */
	protected void handleIdleReconnect() throws IOException {
		if ((idleTimeout > 0) && (getLastWriteAge() > idleTimeout)) {
			idleCount.incrementAndGet();
			Utils.close(this);
			this.open();
		}
	}

	@Override
	public void resetStats() {
		idleCount.set(0);
		lastBytes.set(0);
		sentMsgs.set(0);
		super.resetStats();
	}

	@Override
	public KeyValueStats getStats(Map<String, Object> stats) {
		super.getStats(stats);
		stats.put(Utils.qualify(this, KEY_LAST_BYTES), lastBytes.get());
		stats.put(Utils.qualify(this, KEY_SENT_MSGS), sentMsgs.get());
		stats.put(Utils.qualify(this, KEY_LAST_WAGE), getLastWriteAge());
		stats.put(Utils.qualify(this, KEY_IDLE_COUNT), idleCount.get());
		stats.put(Utils.qualify(this, KEY_SERVICE_URL), url);
		if (!Utils.isEmpty(proxyHost)) {
			stats.put(Utils.qualify(this, KEY_PROXY_URL), (proxyScheme + "//" + proxyHost + ":" + proxyPort));
		}
		return this;
	}

	@Override
	public Object getSinkHandle() {
		return jkHandle;
	}

	@Override
	public synchronized boolean isOpen() {
		return (jkHandle != null && jkHandle.isConnected());
	}

	@Override
	public synchronized void open() throws IOException {
		try {
			close();
			logger.log(OpLevel.DEBUG,
					"Open name={4}, url={0}, timeout={5}, proxy.host={1}, proxy.port={2}, proxy.scheme={3}", url,
					proxyHost, proxyPort, proxyScheme, this.getName(), connTimeout);
			jkHandle = new JKClient(url, connTimeout, proxyHost, proxyPort, proxyScheme, logger);
			if (!StringUtils.isEmpty(accessToken)) {
				jkHandle.connect(accessToken);
			} else {
				jkHandle.connect();
			}
			lastWrite.set(System.currentTimeMillis());

			super.open();
		} catch (URISyntaxException e) {
			logger.log(OpLevel.ERROR,
					"Failed to open name={4}, url={0}, proxy.host={1}, proxy.port={2}, proxy.scheme={3}", url,
					proxyHost, proxyPort, proxyScheme, this.getName(), e);
			close();
			throw new IOException(e.getMessage(), e);
		}
	}

	@Override
	public synchronized void close() throws IOException {
		try {
			if (isOpen()) {
				logger.log(OpLevel.DEBUG, "Closing name={4}, url={0}, proxy.host={1}, proxy.port={2}, proxy.scheme={3}",
						url, proxyHost, proxyPort, proxyScheme, this.getName());
				jkHandle.close();
			}
		} finally {
			super.close();
		}
	}

	@Override
	public String toString() {
		return super.toString() + "{url: " + url + ", token: " + Utils.hide(accessToken, "x", 4) + ", jk.handle: "
				+ jkHandle + "}";
	}

	@Override
	protected void _checkState() throws IllegalStateException {
		if (!isOpen()) {
			throw new IllegalStateException(
					"EventSink closed: name=" + getName() + ", url=" + url + ", handle=" + jkHandle);
		}
	}

	@Override
	protected void writeLine(String msg) throws IOException {
		if (StringUtils.isEmpty(msg)) {
			return;
		}

		_checkState();
		handleIdleReconnect();

		String lineMsg = msg.endsWith("\n") ? msg : msg + "\n";
		incrementBytesSent(lineMsg.length());
		jkHandle.send(lineMsg, false);

		lastWrite.set(System.currentTimeMillis());
		sentMsgs.incrementAndGet();
		lastBytes.set(lineMsg.length());
	}
}
