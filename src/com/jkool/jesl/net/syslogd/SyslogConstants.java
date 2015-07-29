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

import com.nastel.jkool.tnt4j.core.OpLevel;

/**
 * This interface defines basic syslog constants and mappings.
 *
 * @version $Revision: 1$
 */
public interface SyslogConstants {
	static final int FACILITY_KERN = 0;
	static final int FACILITY_USER = 1;
	static final int FACILITY_MAIL = 2;
	static final int FACILITY_DAEMON = 3;
	static final int FACILITY_AUTH = 4;
	static final int FACILITY_SYSLOG = 5;
	static final int FACILITY_LPR = 6;
	static final int FACILITY_NEWS = 7;
	static final int FACILITY_UUCP = 8;
	static final int FACILITY_CRON = 9;
	static final int FACILITY_AUTHPRIV = 10;
	static final int FACILITY_FTP = 11;
	static final int FACILITY_NTP = 12;
	static final int FACILITY_AUDIT = 13;
	static final int FACILITY_ALERT = 14;
	static final int FACILITY_CLOCK = 15;
	static final int FACILITY_LOCAL0 = 16;
	static final int FACILITY_LOCAL1 = 17;
	static final int FACILITY_LOCAL2 = 18;
	static final int FACILITY_LOCAL3 = 19;
	static final int FACILITY_LOCAL4 = 20;
	static final int FACILITY_LOCAL5 = 21;
	static final int FACILITY_LOCAL6 = 22;
	static final int FACILITY_LOCAL7 = 23;

	static final String[] FACILITY = { "kern", "user", "mail", "daemon", "auth", "syslog", "lpr", "news", "uucp",
	        "cron", "authpriv", "ftp", "ntp", "logaudit", "logalert", "clock", "local0", "local1", "local2", "local3",
	        "local4", "local5", "local6", "local7", "unknown" };

	static final OpLevel[] LEVELS = { OpLevel.FATAL, OpLevel.FAILURE, OpLevel.CRITICAL, OpLevel.ERROR, OpLevel.WARNING,
	        OpLevel.INFO, OpLevel.INFO, OpLevel.DEBUG };
}
