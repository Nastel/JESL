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
package com.jkoolcloud.jesl.simulator.tnt4j;

import java.io.IOException;
import java.util.Map;

import com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSink;
import com.jkoolcloud.tnt4j.core.KeyValueStats;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.Snapshot;
import com.jkoolcloud.tnt4j.format.DefaultFormatter;
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.format.JSONFormatter;
import com.jkoolcloud.tnt4j.sink.AbstractEventSink;
import com.jkoolcloud.tnt4j.sink.EventLimiter;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.sink.Sink;
import com.jkoolcloud.tnt4j.sink.impl.FileSink;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;

/**
 * JESL event sink implements event sink simulation used by JESL Simulator.
 *
 * @version $Revision $
 */
public class SimulatedEventSink extends AbstractEventSink {
	private static final String FILE_PREFIX = "file://";

	private EventFormatter formatter = new JSONFormatter();
	private Sink outSink;

	public SimulatedEventSink(String name, String url, String gwAccessToken, Integer connTimeout,
			EventFormatter formatter, EventLimiter limiter) {
		super(name, formatter);

		if (url.startsWith("http://") || url.startsWith("https://")) {
			outSink = new JKCloudEventSink(name, url, gwAccessToken, new DefaultFormatter(), null);
			((JKCloudEventSink) outSink).setConnectionTimeout(connTimeout);
			((EventSink) outSink).setLimiter(limiter);
		} else if (url.startsWith("file://")) {
			String fileName = url.substring(FILE_PREFIX.length());
			outSink = new FileSink(fileName, true, new DefaultFormatter());
		} else {
			throw new IllegalArgumentException("Invalid ur=" + url);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getSinkHandle() {
		return outSink;
	}

	private void writeFormattedMsg(String msg) throws IOException {
		if (isOpen()) {
			incrementBytesSent(msg.length());
			try {
				outSink.write(msg);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void _write(Object msg, Object... args) throws IOException, InterruptedException {
		writeFormattedMsg(formatter.format(msg, args));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void _log(TrackingEvent event) throws IOException {
		writeFormattedMsg(formatter.format(event));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void _log(TrackingActivity activity) throws IOException {
		writeFormattedMsg(formatter.format(activity));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void _log(Snapshot snapshot) throws IOException {
		writeFormattedMsg(formatter.format(snapshot));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void _log(long ttl, Source src, OpLevel sev, String msg, Object... args) throws IOException {
		writeFormattedMsg(formatter.format(ttl, src, msg, args));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void open() throws IOException {
		if (outSink != null) {
			outSink.open();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOpen() {
		return (outSink != null && outSink.isOpen());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void close() throws IOException {
		try {
			if (outSink != null) {
				outSink.close();
			}
		} finally {
			outSink = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public KeyValueStats getStats(Map<String, Object> stats) {
		if (outSink instanceof EventSink) {
			((EventSink) outSink).getStats(stats);
		}
		return super.getStats(stats);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void resetStats() {
		super.resetStats();
		if (outSink instanceof EventSink) {
			((EventSink) outSink).resetStats();
		}
	}
}
