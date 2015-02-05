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
import java.util.Map;

import com.jkool.jesl.tnt4j.sink.JKCloudEventSink;
import com.nastel.jkool.tnt4j.core.KeyValueStats;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.Snapshot;
import com.nastel.jkool.tnt4j.format.DefaultFormatter;
import com.nastel.jkool.tnt4j.format.EventFormatter;
import com.nastel.jkool.tnt4j.format.JSONFormatter;
import com.nastel.jkool.tnt4j.sink.AbstractEventSink;
import com.nastel.jkool.tnt4j.sink.FileSink;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.tracker.TrackingActivity;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;

/**
 *
 *
 * @version $Revision $
 */
public class SimulatedEventSink extends AbstractEventSink {
	private static final String FILE_PREFIX = "file://";
	
	private EventFormatter		formatter = new JSONFormatter();
	private JKCloudEventSink	gwSink;
	private FileSink			fileSink;

	public SimulatedEventSink(String name, String url, String gwAccessToken, EventFormatter formatter) {
		super(name);

		if (formatter != null)
			this.formatter = formatter;

		if (!url.startsWith("file://")) {
			gwSink = new JKCloudEventSink(name, url, gwAccessToken, new DefaultFormatter(), null);
		} else { 
			String fileName = url.substring(FILE_PREFIX.length());
			fileSink = new FileSink(fileName, true, new DefaultFormatter());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSet(OpLevel sev) {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getSinkHandle() {
		return this;
	}

	private void writeFormattedMsg(String msg) throws IOException {
		if (fileSink != null && fileSink.isOpen())
			fileSink.write(msg);
		if (gwSink != null && gwSink.isOpen())
			gwSink.write(msg);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(Object msg, Object... args) throws IOException {
		writeFormattedMsg(formatter.format(msg, args));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void _checkState() throws IllegalStateException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void _log(TrackingEvent event) throws Exception {
		writeFormattedMsg(formatter.format(event));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void _log(TrackingActivity activity) throws Exception {
		writeFormattedMsg(formatter.format(activity));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void _log(Snapshot snapshot) throws Exception {
		writeFormattedMsg(formatter.format(snapshot));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void _log(Source src, OpLevel sev, String msg, Object... args) throws Exception {
		writeFormattedMsg(formatter.format(src, msg, args));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void open() throws IOException {
		if (gwSink != null)
			gwSink.open();
		if (fileSink != null)
			fileSink.open();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOpen() {
		return ((fileSink != null && fileSink.isOpen()) || (gwSink != null && gwSink.isOpen()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		try {
			if (fileSink != null)
				fileSink.close();
			if (gwSink != null)
				gwSink.close();
		}
		finally {
			fileSink = null;
			gwSink   = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Object> getStats() {
		if (gwSink != null)
			return gwSink.getStats();
		return super.getStats();
	}

	@Override
	public KeyValueStats getStats(Map<String, Object> stats) {
		if (gwSink != null)
			return gwSink.getStats(stats);
		return super.getStats(stats);
	}
}
