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

import java.io.PrintStream;
import java.net.SocketAddress;
import java.util.Date;

import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.impl.event.printstream.PrintStreamSyslogServerEventHandler;
import org.productivity.java.syslog4j.util.SyslogUtility;

/**
 * This class implements simple syslog event handler that outputs
 * formatted syslog messages to a print stream.
 *
 * @version $Revision $
 */
class PrintStreamEventHandler extends PrintStreamSyslogServerEventHandler {
    private static final long serialVersionUID = 8964244723777923472L;

	public PrintStreamEventHandler(PrintStream out) {
		super(out);
	}
	
	@Override
	public void event(Object session, SyslogServerIF syslogServer, SocketAddress socketAddress, SyslogServerEventIF event) {
		String date = (event.getDate() == null ? new Date() : event.getDate()).toString();
		String facility = SyslogTNT4JEventHandler.getFacilityString(event.getFacility());
		String level = SyslogUtility.getLevelString(event.getLevel());
		String host = event.getHost();
		
		this.stream.println("{" + host + "}{" + facility + "}{" + date + "}{" + level + "}{" + event.getMessage() + "}");
	}	
}