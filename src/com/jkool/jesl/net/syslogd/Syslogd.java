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
 * <p>Syslog messages are mapped to TNT4J event structure as follows:</p>
 * <table>
 * <tr><th>Syslog Field</th><th>TNT4J Field</th></tr>
 * <tbody>
 * <tr><td><b>timestamp</b></td>		<td>stop/time</td></tr>
 * <tr><td><b>level</b></td>			<td>severity</td></tr>
 * <tr><td><b>applname/facility</b></td><td>event/operation name</td></tr>
 * <tr><td><b>host</b></td>				<td>location</td></tr>
 * <tr><td><b>applname</b></td>			<td>resource name</td></tr>
 * <tr><td><b>pid</b></td>				<td>process ID & thread ID</td></tr>
 * <tr><td><b>message</b></td>			<td>message</td></tr>
 * <tr><td><b>RFC5424 map</b></td>		<td>SyslogMap snapshot</td></tr>
 * <tr><td><b>name=value pairs</b></td>	<td>SyslogVars snapshot</td></tr>
 * </tbody>
 * </table>
 * 
 * <p>The following (name=value) pairs have special meaning and mapped to TNT4J tracking events when included in syslog message:</p>
 * <table>
 * <tr><th>Tag name</th><th>TNT4J Field</th></tr>
 * <tbody>
 * <tr><td><b>usr</b></td><td>User name</td></tr>
 * <tr><td><b>cid</b></td><td>Correlator for relating events across threads, applications, servers</td></tr>
 * <tr><td><b>tag</b></td><td>User defined tag</td></tr>
 * <tr><td><b>loc</b></td><td>Location specifier</td></tr>
 * <tr><td><b>opn</b></td><td>Event/Operation name</td></tr>
 * <tr><td><b>opt</b></td><td>Event/Operation Type</td></tr>
 * <tr><td><b>rsn</b></td><td>Resource name on which operation/event took place</td></tr>
 * </tbody>
 * </table>
 * 
 * <p>Syslog severity level to TN4J OpLevel mapping</p>
 * <table>
 * <tr><th>Syslog Level</th><th>TNT4J Level</th></tr>
 * <tbody>
 * <tr><td><b>Emergency</b></td>		<td>HALT</td></tr>
 * <tr><td><b>Alert</b></td>			<td>FATAL</td></tr>
 * <tr><td><b>Critical</b></td>			<td>CRITICAL</td></tr>
 * <tr><td><b>Error</b></td>			<td>ERROR</td></tr>
 * <tr><td><b>Warning</b></td>			<td>WARNING</td></tr>
 * <tr><td><b>Notice</b></td>			<td>WARNING</td></tr>
 * <tr><td><b>Informational</b></td>	<td>INFO</td></tr>
 * <tr><td><b>Debugging</b></td>		<td>DEBUG</td></tr>
 * </tbody>
 * </table>
 * <p>
 * Event elapsed time is computed based on time since last event from the same
 * source (source is host/application combo).
 * </p>
 * @version $Revision: 1$
 */
public class Syslogd {
		
	public static void main(String[] args) throws Exception {
		ServerOptions options = parseOptions(args);

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
			SyslogServerEventHandlerIF eventHandler = new JsonSyslogServerEventHandler(System.out);
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
	
	public static ServerOptions parseOptions(String[] args) {
		ServerOptions options = new ServerOptions(Syslogd.class.getName());
	
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

class ServerOptions {
	public String source = null;
	public String protocol = null;
	public boolean quiet = false;
	
	public String host = "0.0.0.0";
	public String port = "514";
	public String timeout = null;
	public String usage = null;
	
	public ServerOptions(String name) {
		source = name;
	}
	
	public String toString() {
		return source + ", " + protocol + "://" + host + ":" + port;
	}
}
