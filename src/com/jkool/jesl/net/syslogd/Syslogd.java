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


import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;
import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.impl.net.tcp.TCPNetSyslogServerConfigIF;
import org.productivity.java.syslog4j.util.SyslogUtility;

/**
 * This class implements Syslog Server that accepts syslog messages
 * and routes them to system out and TNT4J streaming framework.
 *
 * @version $Revision $
 */
public class Syslogd {
		
	public static void main(String[] args) throws Exception {
		Options options = parseOptions(args);

		if (options.usage != null) {
			options(options.usage);
			return;
		}
		
		if (!options.quiet) {
			System.out.println("Syslogd starting: " + SyslogServer.getVersion());
			System.out.println("Options: " + options);
		}
		
		if (!SyslogServer.exists(options.protocol)) {
			options("Protocol \"" + options.protocol + "\" not supported");
			return;
		}
		
		SyslogServerIF syslogServer = SyslogServer.getInstance(options.protocol);
		
		SyslogServerConfigIF syslogServerConfig = syslogServer.getConfig();
		syslogServerConfig.setUseStructuredData(true);
		
		if (options.host != null) {
			syslogServerConfig.setHost(options.host);
			if (!options.quiet) {
				System.out.println("Listening on host: " + options.host);
			}
		}

		if (options.port != null) {
			syslogServerConfig.setPort(Integer.parseInt(options.port));
			if (!options.quiet) {
				System.out.println("Listening on port: " + options.port);
			}
		}

		if (options.timeout != null) {
			if (syslogServerConfig instanceof TCPNetSyslogServerConfigIF) {
				((TCPNetSyslogServerConfigIF) syslogServerConfig).setTimeout(Integer.parseInt(options.timeout));
				if (!options.quiet) {
					System.out.println("Timeout: " + options.timeout);
				}
			} else {
				System.err.println("Timeout not supported for protocol \"" + options.protocol + "\" (ignored)");
			}
		}
		
		if (options.source != null) {
			SyslogServerEventHandlerIF eventHandler = new SyslogTNT4JEventHandler(options.source);
			syslogServerConfig.addEventHandler(eventHandler);
		}
		
		if (!options.quiet) {
			SyslogServerEventHandlerIF eventHandler = new PrintStreamEventHandler(System.out);
			syslogServerConfig.addEventHandler(eventHandler);
		}

		if (!options.quiet) {
			System.out.println();
		}

		SyslogServer.getThreadedInstance(options.protocol);
		if (!options.quiet) {
			System.out.println("Syslogd ready: " + SyslogServer.getVersion());
		}
		
		while(true) {
			SyslogUtility.sleep(1000);
		}
	}
	
	public static void options(String reason) {
		if (reason != null) {
			System.out.println("Notice: " + reason);
			System.out.println();
		}
		
		System.out.println("Syslogd Options:");
		System.out.println();
		System.out.println("[-h <host>] [-p <port>] [-s <source>] [-q] <protocol>");
		System.out.println();
		System.out.println("-h <host>    host or IP to bind");
		System.out.println("-p <port>    port to bind");
		System.out.println("-t <timeout> socket timeout (in milliseconds)");
		System.out.println("-s <source>  tnt4j source name (default: " + Syslogd.class.getName() + ")");
		System.out.println();
		System.out.println("-q           do not write anything to standard out");
		System.out.println();
		System.out.println("protocol     syslog protocol implementation (tcp, udp, ...)");
	}
	
	public static Options parseOptions(String[] args) {
		Options options = new Options(Syslogd.class.getName());
	
		int i = 0;
		while(i < args.length) {
			String arg = args[i++];
			boolean match = false;
			
			if ("-h".equals(arg)) { if (i == args.length) { options.usage = "Must specify host with -h"; return options; } match = true; options.host = args[i++]; }
			if ("-p".equals(arg)) { if (i == args.length) { options.usage = "Must specify port with -p"; return options; } match = true; options.port = args[i++]; }
			if ("-t".equals(arg)) { if (i == args.length) { options.usage = "Must specify value (in milliseconds)"; return options; } match = true; options.timeout = args[i++]; }
			if ("-s".equals(arg)) { if (i == args.length) { options.usage = "Must specify source with -s"; return options; } match = true; options.source = args[i++]; }
			
			if ("-q".equals(arg)) { match = true; options.quiet = true; }
			
			if (!match) {
				if (options.protocol != null) {
					options.usage = "Only one protocol definition allowed";
					return options;
				}		
				options.protocol = arg;
			}
		}
		
		if (options.protocol == null) {
			options.usage = "Must specify protocol";
			return options;
		}		
		return options;
	}	
}

class Options {
	public String source = null;
	public String protocol = null;
	public boolean quiet = false;
	
	public String host = "0.0.0.0";
	public String port = "514";
	public String timeout = null;
	public String usage = null;
	
	public Options(String name) {
		source = name;
	}
	
	public String toString() {
		return source + ", " + protocol + "://" + host + ":" + port;
	}
}
