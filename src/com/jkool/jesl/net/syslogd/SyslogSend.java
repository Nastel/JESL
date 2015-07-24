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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.SyslogConfigIF;
import org.productivity.java.syslog4j.SyslogIF;
import org.productivity.java.syslog4j.impl.message.processor.structured.StructuredSyslogMessageProcessor;
import org.productivity.java.syslog4j.util.SyslogUtility;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SyslogSend {
	public static boolean CALL_SYSTEM_EXIT_ON_FAILURE = true;

	private static JsonParser jparser = new JsonParser();

	public static void usage(String problem) {
		if (problem != null) {
			System.out.println("Error: " + problem);
			System.out.println();
		}

		System.out.println("Usage:");
		System.out.println();
		System.out.println("Syslog [-h <host>] [-p <port>] [-l <level>] [-f <facility>]");
		System.out.println("       <protocol>");
		System.out.println();
		System.out.println("Syslog [-h <host>] [-p <port>] [-l <level>] [-f <facility>]");
		System.out.println("       <protocol> [message...]");
		System.out.println();
		System.out.println("Syslog [-h <host>] [-p <port>] [-l <level>] [-f <facility>]");
		System.out.println("       -i <file> <protocol>");
		System.out.println();
		System.out.println("-h <host>      host or IP to send message (default: localhost)");
		System.out.println("-p <port>      port to send message (default: 514)");
		System.out.println("-l <level>     syslog level to use (default: INFO)");
		System.out.println("-f <facility>  syslog facility to use (default: USER)");
		System.out.println("-i <file>      input taken from the specified file");
		System.out.println();
		System.out.println("-q             do not write anything to standard out");
		System.out.println();
		System.out.println("protocol       Syslog protocol implementation");
		System.out.println("message        syslog message text");
		System.out.println();
		System.out.println("Notes:");
		System.out.println();
		System.out.println("Additional message arguments will be concatenated into the same");
		System.out.println("syslog message; calling SyslogMain will only send one message per call.");
		System.out.println();
		System.out.println("If the message argument is ommited, lines will be taken from the");
		System.out.println("standard input.");
	}

	public static SendOptions parseOptions(String[] args) {
		SendOptions sendOptions = new SendOptions();

		int i = 0;
		while (i < args.length) {
			String arg = args[i++];
			boolean match = false;

			if ("-h".equals(arg)) {
				if (i == args.length) {
					sendOptions.usage = "Must specify host with -h";
					return sendOptions;
				}
				match = true;
				sendOptions.host = args[i++];
			}
			if ("-p".equals(arg)) {
				if (i == args.length) {
					sendOptions.usage = "Must specify port with -p";
					return sendOptions;
				}
				match = true;
				sendOptions.port = args[i++];
			}
			if ("-l".equals(arg)) {
				if (i == args.length) {
					sendOptions.usage = "Must specify level with -l";
					return sendOptions;
				}
				match = true;
				sendOptions.level = args[i++];
			}
			if ("-f".equals(arg)) {
				if (i == args.length) {
					sendOptions.usage = "Must specify facility with -f";
					return sendOptions;
				}
				match = true;
				sendOptions.facility = args[i++];
			}
			if ("-i".equals(arg)) {
				if (i == args.length) {
					sendOptions.usage = "Must specify file with -i";
					return sendOptions;
				}
				match = true;
				sendOptions.fileName = args[i++];
			}

			if ("-q".equals(arg)) {
				match = true;
				sendOptions.quiet = true;
			}

			if (sendOptions.protocol == null && !match) {
				match = true;
				sendOptions.protocol = arg;
			}

			if (!match) {
				if (sendOptions.message == null) {
					sendOptions.message = arg;

				} else {
					sendOptions.message += " " + arg;
				}
			}
		}

		if (sendOptions.protocol == null) {
			sendOptions.usage = "Must specify protocol";
			return sendOptions;
		}

		if (sendOptions.message != null && sendOptions.fileName != null) {
			sendOptions.usage = "Must specify either -i <file> or <message>, not both";
			return sendOptions;
		}

		return sendOptions;
	}

	public static void main(String[] args) throws Exception {
		main(args, true);
	}

	public static void main(String[] args, boolean shutdown) throws Exception {
		SendOptions sendOptions = parseOptions(args);

		if (sendOptions.usage != null) {
			usage(sendOptions.usage);
			if (CALL_SYSTEM_EXIT_ON_FAILURE) {
				System.exit(1);
			} else {
				return;
			}
		}

		if (!sendOptions.quiet) {
			System.out.println("Syslog " + Syslog.getVersion());
		}

		if (!Syslog.exists(sendOptions.protocol)) {
			usage("Protocol \"" + sendOptions.protocol + "\" not supported");
			if (CALL_SYSTEM_EXIT_ON_FAILURE) {
				System.exit(2);
			} else {
				return;
			}
		}

		SyslogIF syslog = Syslog.getInstance(sendOptions.protocol);
		SyslogConfigIF syslogConfig = syslog.getConfig();

		if (sendOptions.host != null) {
			syslogConfig.setHost(sendOptions.host);
			if (!sendOptions.quiet) {
				System.out.println("Sending to host: " + sendOptions.host);
			}
		}

		if (sendOptions.port != null) {
			syslogConfig.setPort(Integer.parseInt(sendOptions.port));
			if (!sendOptions.quiet) {
				System.out.println("Sending to port: " + sendOptions.port);
			}
		}

		int level = SyslogUtility.getLevel(sendOptions.level);
		syslogConfig.setFacility(sendOptions.facility);

		if (sendOptions.message != null) {
			if (!sendOptions.quiet) {
				System.out.println("Sending " + sendOptions.facility + "." + sendOptions.level + " message \""
				        + sendOptions.message + "\"");
			}
			syslog.log(level, sendOptions.message);
		} else {
			sendFromTextFile(syslog, sendOptions);
		}

		if (shutdown) {
			Syslog.shutdown();
		}
	}

	private static void sendFromTextFile(SyslogIF syslog, SendOptions sendOptions) throws IOException, InterruptedException {
		InputStream is = null;
		int level = SyslogUtility.getLevel(sendOptions.level);

		if (sendOptions.fileName != null) {
			is = new FileInputStream(sendOptions.fileName);

		} else {
			is = System.in;
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		try {
			String line = br.readLine();
			while (line != null && line.length() > 0) {
				if (!line.startsWith("{")) {
					if (!sendOptions.quiet) {
						System.out.println("Sending: " + sendOptions.facility + "." + sendOptions.level + " \"" + line + "\" ");
					}
					syslog.log(level, line);
				} else {
					jsonSyslog(syslog, sendOptions, line);
				}
				line = br.readLine();
			}
		} finally {
			br.close();
		}
	}

	private static void jsonSyslog(SyslogIF syslog, SendOptions sendOptions, String line) throws InterruptedException {
		JsonElement jelement = jparser.parse(line);
		JsonObject jobject = jelement.getAsJsonObject();
		long offset_usec = jobject.get("offset.usec").getAsLong();
		String facility = jobject.get("facility").getAsString();
		String level = jobject.get("level").getAsString();
		String msg = jobject.get("msg").getAsString();
		JsonElement appl = jobject.get("appl");

		if (appl != null) {
			StructuredSyslogMessageProcessor mpr = new StructuredSyslogMessageProcessor(appl.getAsString());
			mpr.setProcessId(jobject.get("pid").getAsString());
			syslog.setStructuredMessageProcessor(mpr);
			syslog.getConfig().setUseStructuredData(true);
		} else {
			syslog.getConfig().setUseStructuredData(false);
		}
		if (!sendOptions.quiet) {
			if (!syslog.getConfig().isUseStructuredData()) {
				System.out.println("Sending(" + offset_usec + ")(" + syslog.getConfig().isUseStructuredData() + "): "
				        + facility + "." + level + " \"" + msg + "\"");
			} else {
				System.out.println("Sending(" + offset_usec + ")(" + syslog.getConfig().isUseStructuredData() + "): "
				        + facility + "." + level + "." + appl.getAsString() + "." + jobject.get("pid").getAsString()
				        + " \"" + msg + "\"");
			}
		}
		Thread.sleep(offset_usec / 1000);

		syslog.getConfig().setFacility(facility);
		syslog.log(SyslogUtility.getLevel(level), msg);
	}
}

class SendOptions {
	String host = null;
	String port = null;
	String level = "INFO";
	String facility = "USER";
	String protocol = null;
	String message = null;
	String fileName = null;
	boolean quiet = false;
	String usage = null;
}
