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
package com.jkoolcloud.jesl.tnt4j.sink;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.jkoolcloud.tnt4j.config.ConfigException;
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.format.JSONFormatter;
import com.jkoolcloud.tnt4j.sink.AbstractEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.sink.EventSinkFactory;
import com.jkoolcloud.tnt4j.sink.impl.FileEventSinkFactory;
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
	private String fileName = null;
	private String token = System.getProperty("tnt4j.sink.factory.socket.token", "");
	private String url = System.getProperty("tnt4j.sink.factory.socket.url", "http://localhost:6580");
	// NOTE: server side uses 5min. to close inactive connection by default
	private long idleTimeout = Long.getLong("tnt4j.sink.factory.socket.idle.timeout", TimeUnit.MINUTES.toMillis(4));

	private String proxyScheme = "http";
	private String proxyHost;
	private int proxyPort = 0;

	private EventSinkFactory eventSinkFactory = null;

	/**
	 * Create a jKool Cloud Event Sink factory.
	 *
	 */
	public JKCloudEventSinkFactory() {
	}

	/**
	 * Create a jKool Cloud Event Sink factory
	 *
	 * @param url host location of the jKool cloud service
	 *
	 */
	public JKCloudEventSinkFactory(String url) {
		this.url = url;
	}

	@Override
	public EventSink getEventSink(String name) {
		EventSink outSink = eventSinkFactory != null
				? eventSinkFactory.getEventSink(name, System.getProperties(), new JSONFormatter(false)) : null;
		return configureSink(new JKCloudEventSink(name, url, new JSONFormatter(false), outSink));
	}

	@Override
	public EventSink getEventSink(String name, Properties props) {
		EventSink outSink = eventSinkFactory != null
				? eventSinkFactory.getEventSink(name, System.getProperties(), new JSONFormatter(false)) : null;
		return configureSink(new JKCloudEventSink(name, url, new JSONFormatter(false), outSink));
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
	public Map<String, Object> getConfiguration() {
		return config;
	}

	@Override
	public void setConfiguration(Map<String, Object> settings) throws ConfigException {
		super.setConfiguration(settings);

		url = Utils.getString("Url", settings, url);
		token = Utils.getString("Token", settings, token);
		fileName = Utils.getString("Filename", settings, fileName);
		idleTimeout = Utils.getLong("IdleTimeout", settings, idleTimeout);
		proxyScheme = Utils.getString("ProxyScheme", settings, proxyScheme);
		proxyHost = Utils.getString("ProxyHost", settings, proxyHost);
		proxyPort = Utils.getInt("ProxyPort", settings, proxyPort);
		_applyConfig(settings);
	}

	private void _applyConfig(Map<String, Object> settings) throws ConfigException {
		if (fileName != null) {
			eventSinkFactory = new FileEventSinkFactory(fileName);
			eventSinkFactory.setTTL(getTTL());
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
