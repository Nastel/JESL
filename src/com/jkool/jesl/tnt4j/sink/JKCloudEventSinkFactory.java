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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import com.nastel.jkool.tnt4j.config.ConfigException;
import com.nastel.jkool.tnt4j.format.EventFormatter;
import com.nastel.jkool.tnt4j.format.JSONFormatter;
import com.nastel.jkool.tnt4j.sink.AbstractEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.sink.EventSinkFactory;
import com.nastel.jkool.tnt4j.sink.FileEventSinkFactory;

/**
 * <p>Concrete implementation of <code>EventSinkFactory</code> interface, which
 * creates instances of <code>EventSink</code>. This factory uses <code>JKCloudEventSink</code>
 * as the underlying sink provider and by default uses <code>JSONFormatter</code> to
 * format messages.</p>
 *
 *
 * @see JKCloudEventSink
 *
 * @version $Revision: 6 $
 *
 */
public class JKCloudEventSinkFactory extends AbstractEventSinkFactory {
	private String fileName = null;
	private String token = System.getProperty("tnt4j.sink.factory.socket.token", "");
	private String url = System.getProperty("tnt4j.sink.factory.socket.url", "http://localhost:6580");
	
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
		EventSink outSink = eventSinkFactory != null? eventSinkFactory.getEventSink(name, System.getProperties(), new JSONFormatter(false)): null;
	    return configureSink(new JKCloudEventSink(name, url, new JSONFormatter(false), outSink));
    }

	@Override
    public EventSink getEventSink(String name, Properties props) {
		EventSink outSink = eventSinkFactory != null? eventSinkFactory.getEventSink(name, System.getProperties(), new JSONFormatter(false)): null;
	    return configureSink(new JKCloudEventSink(name, url, new JSONFormatter(false), outSink));
    }

	@Override
    public EventSink getEventSink(String name, Properties props, EventFormatter frmt) {
		EventSink outSink = eventSinkFactory != null? eventSinkFactory.getEventSink(name, props, frmt): null;
	    return configureSink(new JKCloudEventSink(name, url,  token, frmt, outSink));
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
		fileName = config.get("Filename") != null? config.get("Filename").toString(): fileName;
		if (fileName != null) {
			eventSinkFactory = new FileEventSinkFactory(fileName);
			eventSinkFactory.setTTL(getTTL());
		}
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
