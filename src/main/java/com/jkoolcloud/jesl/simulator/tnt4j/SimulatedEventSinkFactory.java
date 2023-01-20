/*
 * Copyright 2014-2023 JKOOL, LLC.
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
package com.jkoolcloud.jesl.simulator.tnt4j;

import java.util.Properties;

import com.jkoolcloud.jesl.simulator.TNT4JSimulator;
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.sink.AbstractEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * JESL event sink factory implements event sink factory simulation used by JESL Simulator.
 *
 * @see SimulatedEventSink
 * @version $Revision $
 */
public class SimulatedEventSinkFactory extends AbstractEventSinkFactory {

	public SimulatedEventSinkFactory() {
	}

	@Override
	public EventSink getEventSink(String name) {
		return configureSink(
				new SimulatedEventSink(name, TNT4JSimulator.getConnectUrl(), TNT4JSimulator.getAccessToken(),
						TNT4JSimulator.getConnectionTimeout(), null, getDefaultEventLimiter()));
	}

	@Override
	public EventSink getEventSink(String name, Properties props) {
		return getEventSink(name);
	}

	@Override
	public EventSink getEventSink(String name, Properties props, EventFormatter frmt) {
		return getEventSink(name);
	}
}
