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
package com.jkool.jesl.net.syslogd;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import com.nastel.jkool.tnt4j.dump.DefaultDumpProvider;
import com.nastel.jkool.tnt4j.dump.Dump;
import com.nastel.jkool.tnt4j.dump.DumpCollection;

/**
 * This class implements a dump handler for syslog event handler {@link SyslogTNT4JEventHandler}.
 * It dumps the contents of a timing table maintained by the syslog handler. 
 * The timings maintain the number of nanoseconds since last event for a specific server/application
 * combo.
 *
 * @version $Revision $
 */
public class SyslogHandlerDumpProvider extends DefaultDumpProvider{
	private Map<String, AtomicLong> map;
	
	public SyslogHandlerDumpProvider(String name, Map<String, AtomicLong> m) {
	    super(name, "SyslogTimings");
	    this.map = m;
    }

	@Override
    public DumpCollection getDump() {
		Dump dump = new Dump("TimerTable", this);	
		for (Entry<String, AtomicLong> entry: map.entrySet()) {
			dump.add(entry.getKey(), System.nanoTime() - entry.getValue().get());
		}
	    return dump;
    }
}
