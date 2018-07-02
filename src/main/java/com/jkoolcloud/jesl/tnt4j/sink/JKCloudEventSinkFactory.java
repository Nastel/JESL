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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import com.jkoolcloud.tnt4j.config.ConfigException;
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.format.JSONFormatter;
import com.jkoolcloud.tnt4j.sink.AbstractEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.sink.EventSinkFactory;
import com.jkoolcloud.tnt4j.sink.impl.FileEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.impl.slf4j.SLF4JEventSinkFactory;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * <p>
 * Concrete implementation of {@link EventSinkFactory} interface, which creates instances of {@link EventSink}. This
 * factory uses {@link JKCloudEventSink} as the underlying sink provider and by default uses {@link JSONFormatter} to
 * format messages.
 * </p>
 *
 *
 * @see JKCloudEventSink
 *
 * @version $Revision: 7 $
 *
 */
public class JKCloudEventSinkFactory extends AbstractEventSinkFactory {
	private static final String SINK_PREF_FILE = "file:";
	private static final String SINK_PREF_SLF4J = "slf4j:";

	private String sinkDescriptor = null;
	private String token = System.getProperty("tnt4j.sink.factory.socket.token", "");
	private String url = System.getProperty("tnt4j.sink.factory.socket.url", "http://localhost:6580");
	// NOTE: server side uses 5min. to close inactive connection by default
	private long idleTimeout = Long.getLong("tnt4j.sink.factory.socket.idle.timeout", TimeUnit.MINUTES.toMillis(4));

	private String proxyScheme = "http";
	private String proxyHost;
	private int proxyPort = 0;

	private EventSinkFactory eventSinkFactory = null;

	/**
	 * Create a jKoolCloud Event Sink factory.
	 *
	 */
	public JKCloudEventSinkFactory() {
	}

	/**
	 * Create a jKoolCloud Event Sink factory
	 *
	 * @param url
	 *            host location of the jKoolCloud service
	 *
	 */
	public JKCloudEventSinkFactory(String url) {
		this.url = url;
	}

	@Override
	public EventSink getEventSink(String name) {
		return getEventSink(name, System.getProperties());
	}

	@Override
	public EventSink getEventSink(String name, Properties props) {
		return getEventSink(name, props, new JSONFormatter(false));
	}

	@Override
	public EventSink getEventSink(String name, Properties props, EventFormatter frmt) {
		EventSink outSink = eventSinkFactory != null ? eventSinkFactory.getEventSink(name, props, frmt) : null;
		return configureSink(new JKCloudEventSink(name, url, token, frmt, outSink));
	}

	@Override
	protected EventSink configureSink(EventSink sink) {
		EventSink jsink = super.configureSink(sink);
		((JKCloudEventSink) jsink).setIdleTimeout(idleTimeout, TimeUnit.MILLISECONDS);
		((JKCloudEventSink) jsink).setProxyParms(proxyScheme, proxyHost, proxyPort);
		return jsink;
	}

	@Override
	public void setConfiguration(Map<String, ?> settings) throws ConfigException {
		super.setConfiguration(settings);

		url = Utils.getString("Url", settings, url);
		token = Utils.getString("Token", settings, token);
		String fileName = Utils.getString("Filename", settings, null);
		sinkDescriptor = Utils.getString("BranchSink", settings, sinkDescriptor);
		idleTimeout = Utils.getLong("IdleTimeout", settings, idleTimeout);
		proxyScheme = Utils.getString("ProxyScheme", settings, proxyScheme);
		proxyHost = Utils.getString("ProxyHost", settings, proxyHost);
		proxyPort = Utils.getInt("ProxyPort", settings, proxyPort);
		eventSinkFactory = (EventSinkFactory) Utils.createConfigurableObject("eventSinkFactory", "eventSinkFactory.",
				settings);

		if (StringUtils.isEmpty(sinkDescriptor)) {
			if (StringUtils.isNotEmpty(fileName)) {
				sinkDescriptor = SINK_PREF_FILE + fileName;
			}
		}
		_applyConfig(settings);
	}

	private void _applyConfig(Map<String, ?> settings) throws ConfigException {
		if (eventSinkFactory == null && StringUtils.isNotEmpty(sinkDescriptor)) {
			if (sinkDescriptor.startsWith(SINK_PREF_SLF4J)) {
				eventSinkFactory = new SLF4JEventSinkFactory(sinkDescriptor.substring(SINK_PREF_SLF4J.length()));
			} else if (sinkDescriptor.startsWith(SINK_PREF_FILE)) {
				eventSinkFactory = new FileEventSinkFactory(sinkDescriptor.substring(SINK_PREF_FILE.length()));
			} else {
				throw new ConfigException("Unknown branch sink descriptor: " + sinkDescriptor, settings);
			}

			if (eventSinkFactory != null) {
				eventSinkFactory.setTTL(getTTL());
			}
		}
		try {
			URI uri = new URI(url);
			url = uri.toString();
		} catch (URISyntaxException e1) {
			ConfigException ce = new ConfigException(e1.toString(), settings);
			ce.initCause(e1);
			throw ce;
		}
	}
}
