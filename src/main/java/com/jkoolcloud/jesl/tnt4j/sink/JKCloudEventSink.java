/*
 * Copyright 2014-2022 JKOOL, LLC.
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
package com.jkoolcloud.jesl.tnt4j.sink;

import java.io.IOException;
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
	public static final String KEY_ACK_COUNT = "sink-ack-count";
	public static final String KEY_LAST_ACK_MSG = "sink-ack-last-msg";
	public static final String KEY_LAST_ACK_ELAPSED = "sink-ack-last-elapsed-ms";

	public static final long DEFAULT_IDLE_TIMEOUT = TimeUnit.MINUTES.toMillis(4);
	public static final long DEFAULT_CONN_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	private JKClient jkHandle;

	private String url = "localhost";
	private String accessToken;
	private String lastAckMsg = "";

	private String proxyScheme = "http";
	private String proxyHost;
	private int proxyPort = 0;
	private String proxyUser;
	private String proxyPass;

	private long connTimeout = DEFAULT_CONN_TIMEOUT;
	private long idleTimeout = DEFAULT_IDLE_TIMEOUT;
	private boolean ackSends = false;

	private AtomicLong idleCount = new AtomicLong(0);
	private AtomicLong lastWrite = new AtomicLong(0);
	private AtomicLong lastBytes = new AtomicLong(0);
	private AtomicLong sentMsgs = new AtomicLong(0);
	private AtomicLong ackCount = new AtomicLong(0);
	private AtomicLong ackElapsed = new AtomicLong(0);

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
	 * Sets proxy communication parameters.
	 *
	 * @param scheme
	 *            proxy communication scheme
	 * @param host
	 *            proxy host name if any, null if none
	 * @param port
	 *            proxy port number if any, 0 of none
	 * @return itself
	 */
	public JKCloudEventSink setProxyParms(String scheme, String host, int port) {
		this.proxyScheme = scheme;
		this.proxyHost = host;
		this.proxyPort = port;
		return this;
	}

	/**
	 * Sets proxy basic authentication credentials.
	 *
	 * @param user
	 *            proxy authentication user name
	 * @param pass
	 *            proxy authentication password
	 * @return itself
	 */
	public JKCloudEventSink setProxyCredentials(String user, String pass) {
		this.proxyUser = user;
		this.proxyPass = pass;
		return this;
	}

	/**
	 * Sets connection timeout.
	 *
	 * @param timeout
	 *            connection timeout
	 * @param tunit
	 *            time out time units
	 * @return itself
	 */
	public JKCloudEventSink setConnectionTimeout(long timeout, TimeUnit tunit) {
		this.connTimeout = tunit.toMillis(timeout);
		return this;
	}

	/**
	 * Acknowledge every send (much slower if set to true)
	 *
	 * @param ackSends
	 *            true to acknowledge every sends, false -- send and forget
	 * @return itself
	 */
	public JKCloudEventSink ackSends(boolean ackSends) {
		this.ackSends = ackSends;
		return this;
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
	 * @return itself
	 */
	public JKCloudEventSink setAccessToken(String accessToken) {
		this.accessToken = accessToken;
		return this;
	}

	/**
	 * Sets idle timeout for the sink. Connection is dropped on next write after timeout.
	 *
	 * @param timeout
	 *            idle timeout
	 * @param tunit
	 *            time out time units
	 * @return itself
	 */
	public JKCloudEventSink setIdleTimeout(long timeout, TimeUnit tunit) {
		this.idleTimeout = tunit.toMillis(timeout);
		return this;
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
			reopen();
		}
	}

	@Override
	public void resetStats() {
		idleCount.set(0);
		lastBytes.set(0);
		sentMsgs.set(0);
		ackCount.set(0);
		ackElapsed.set(0);
		lastAckMsg = "";
		super.resetStats();
	}

	@Override
	public KeyValueStats getStats(Map<String, Object> stats) {
		super.getStats(stats);
		stats.put(Utils.qualify(this, KEY_LAST_BYTES), lastBytes.get());
		stats.put(Utils.qualify(this, KEY_SENT_MSGS), sentMsgs.get());
		stats.put(Utils.qualify(this, KEY_LAST_WAGE), getLastWriteAge());
		stats.put(Utils.qualify(this, KEY_IDLE_COUNT), idleCount.get());
		if (ackSends) {
			stats.put(Utils.qualify(this, KEY_ACK_COUNT), ackCount.get());
			stats.put(Utils.qualify(this, KEY_LAST_ACK_ELAPSED), ackElapsed.get());
			stats.put(Utils.qualify(this, KEY_LAST_ACK_MSG), lastAckMsg);
		}
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
	protected synchronized void _open() throws IOException {
		try {
			if (isOpen()) {
				_close();
			}
			setErrorState(null);
			logger.log(OpLevel.DEBUG,
					"Open name={6}, url={0}, timeout={5}, proxy.host={1}, proxy.port={2}, proxy.scheme={3}, proxy.user={4}, proxy.pass={5}",
					url, proxyHost, proxyPort, proxyScheme, proxyUser, proxyPass == null ? null : "xxxxxx", getName(),
					connTimeout);
			jkHandle = new JKClient(url, connTimeout, proxyHost, proxyPort, proxyScheme, proxyUser, proxyPass, logger);
			if (!StringUtils.isEmpty(accessToken)) {
				jkHandle.connect(accessToken);
			} else {
				jkHandle.connect();
			}
			lastWrite.set(System.currentTimeMillis());

			super._open();
		} catch (Throwable e) {
			logger.log(OpLevel.ERROR,
					"Failed to open sink name={6}, url={0}, proxy.host={1}, proxy.port={2}, proxy.scheme={3}, proxy.user={4}, proxy.pass={5}",
					url, proxyHost, proxyPort, proxyScheme, proxyUser, proxyPass == null ? null : "xxxxxx", getName(),
					e);
			_close();

			if (e instanceof IOException) {
				throw (IOException) e;
			} else {
				throw new IOException(e.getMessage(), e);
			}
		}
	}

	@Override
	protected synchronized void _close() throws IOException {
		logger.log(OpLevel.DEBUG, "Closing sink name={4}, url={0}, proxy.host={1}, proxy.port={2}, proxy.scheme={3}",
				url, proxyHost, proxyPort, proxyScheme, getName());
		Utils.close(jkHandle);

		super._close();
	}

	@Override
	public String toString() {
		return super.toString() //
				+ "{url: " + url //
				+ ", token: " + Utils.hide(accessToken, "x", 4) //
				+ ", jk.handle: " + jkHandle //
				+ "}";
	}

	@Override
	protected synchronized void writeLine(String msg) throws IOException {
		if (StringUtils.isEmpty(msg)) {
			return;
		}

		_checkState();
		handleIdleReconnect();

		String lineMsg = msg.endsWith("\n") ? msg : msg + "\n";
		incrementBytesSent(lineMsg.length());
		jkHandle.send(accessToken, lineMsg, ackSends);
		long timestamp = System.currentTimeMillis();
		lastWrite.set(timestamp);
		sentMsgs.incrementAndGet();
		lastBytes.set(lineMsg.length());
		if (ackSends) {
			_handleAcks(timestamp);
		}
	}

	private void _handleAcks(long timestamp) throws IOException {
		try {
			lastAckMsg = jkHandle.read();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			ackCount.incrementAndGet();
			ackElapsed.set(System.currentTimeMillis() - timestamp);
		}
	}
}
