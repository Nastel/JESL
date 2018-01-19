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

import com.jkoolcloud.tnt4j.format.DefaultFormatter;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;

/**
 * This class implements TNT4J event formatter that produced NULL output (nothing) for tracking activities and default
 * for tracking events.
 *
 * @version $Revision $
 */
public class NullFormatter extends DefaultFormatter {
	@Override
	public String format(TrackingEvent event) {
		return event.getMessage();
	}

	@Override
	public String format(TrackingActivity activity) {
		return "";
	}
}
