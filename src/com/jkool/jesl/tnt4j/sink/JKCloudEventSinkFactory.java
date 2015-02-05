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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import com.nastel.jkool.tnt4j.config.ConfigException;
import com.nastel.jkool.tnt4j.format.EventFormatter;
import com.nastel.jkool.tnt4j.format.JSONFormatter;
import com.nastel.jkool.tnt4j.sink.AbstractEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.sink.EventSinkFactory;

/**
 * <p>Concrete implementation of <code>EventSinkFactory</code> interface, which
 * creates instances of <code>EventSink</code>. This factory uses <code>JKCloudEventSink</code>
 * as the underlying sink provider provider and by default uses <code>JSONFormatter</code> to
 * format log messages.</p>
 *
 *
 * @see EventSink
 * @see EventFormatter
 * @see JSONFormatter
 *
 * @version $Revision: 6 $
 *
 */
public class JKCloudEventSinkFactory extends AbstractEventSinkFactory {
	private String token = System.getProperty("tnt4j.sink.factory.socket.token", "");
	private String url = System.getProperty("tnt4j.sink.factory.socket.url", "http://localhost:6580");
	private String sslKeystore = System.getProperty("tnt4j.sink.factory.socket.SSLKeystore", "");
	private String sslKeystorePwd = System.getProperty("tnt4j.sink.factory.socket.SSLKeystorePwd", "");

	private EventSinkFactory eventSinkFactory = DefaultEventSinkFactory.getInstance();

	/**
	 * Create a gateway event sink factory.
	 *
	 */
	public JKCloudEventSinkFactory() {
	}

	/**
	 * Create a gateway event sink factory
	 *
	 * @param url host location of the jkool cloud server
	 *
	 */
	public JKCloudEventSinkFactory(String url) {
		this.url = url;
	}

	@Override
    public EventSink getEventSink(String name) {
	    return new JKCloudEventSink(name, url, new JSONFormatter(false), eventSinkFactory.getEventSink(name, System.getProperties(), new JSONFormatter()));
    }

	@Override
    public EventSink getEventSink(String name, Properties props) {
	    return new JKCloudEventSink(name, url, new JSONFormatter(false), eventSinkFactory.getEventSink(name, props, new JSONFormatter()));
    }

	@Override
    public EventSink getEventSink(String name, Properties props, EventFormatter frmt) {
	    return new JKCloudEventSink(name, url,  token, sslKeystore, sslKeystorePwd, frmt, eventSinkFactory.getEventSink(name, props, new JSONFormatter()));
    }

	@Override
    public Map<String, Object> getConfiguration() {
	    return config;
    }

	@Override
    public void setConfiguration(Map<String, Object> settings) throws ConfigException {
		super.setConfiguration(settings);
		url = config.get("Url") != null? config.get("Url").toString(): url;
		token = config.get("Token") != null? config.get("Token").toString(): token;
		sslKeystore = config.get("SSLKeystore") != null ? config.get("SSLKeystore").toString(): sslKeystore;
		sslKeystorePwd = config.get("SSLKeystorePwd") != null ? config.get("SSLKeystorePwd").toString() : sslKeystorePwd;

		try {
			URI uri = new URI(url);
			url = uri.toString();
		}
		catch (URISyntaxException e1) {
			ConfigException ce = new ConfigException(e1.toString(), settings);
			ce.initCause(e1);
			throw ce;
		}
    }
}
