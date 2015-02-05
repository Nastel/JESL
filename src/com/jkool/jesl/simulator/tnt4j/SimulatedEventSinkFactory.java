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

import java.util.Properties;

import com.jkool.jesl.simulator.TNT4JSimulator;
import com.nastel.jkool.tnt4j.format.EventFormatter;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.sink.EventSinkFactory;

public class SimulatedEventSinkFactory implements EventSinkFactory {

	public SimulatedEventSinkFactory() {
	}

	@Override
	public EventSink getEventSink(String name) {
		return new SimulatedEventSink(name,
				TNT4JSimulator.getConnectUrl(),
				TNT4JSimulator.getAccessToken(),
				null);
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
