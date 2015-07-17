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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.productivity.java.syslog4j.impl.message.structured.StructuredSyslogMessage;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.SyslogServerSessionEventHandlerIF;
import org.productivity.java.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;

import com.nastel.jkool.tnt4j.TrackingLogger;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.OpType;
import com.nastel.jkool.tnt4j.core.PropertySnapshot;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.source.SourceFactory;
import com.nastel.jkool.tnt4j.source.SourceType;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;

/**
 * This class implements syslog event handler that outputs syslog
 * messages to TNT4J logging framework. 
 * <p>Syslog messages are mapped to TNT4J event structure as follows:</p>
 * <table>
 * <tr><td><b>timestamp</b></td>		<td>stop/time</td></tr>
 * <tr><td><b>level</b></td>			<td>severity</td></tr>
 * <tr><td><b>facility</b></td>			<td>event/operation name</td></tr>
 * <tr><td><b>host</b></td>				<td>location</td></tr>
 * <tr><td><b>applname</b></td>			<td>resource name</td></tr>
 * <tr><td><b>pid</b></td>				<td>process ID & thread ID</td></tr>
 * <tr><td><b>message</b></td>			<td>message</td></tr>
 * </table>
 *
 * @version $Revision $
 */
public class TNT4JEventHandler implements SyslogServerSessionEventHandlerIF, SyslogConstants {
    private static final long serialVersionUID = -3115399425996955812L;
    
	private static final ConcurrentHashMap<String, AtomicLong> EVENT_TIMER = new ConcurrentHashMap<String, AtomicLong>();

    TrackingLogger logger;
	Pattern pattern = Pattern.compile("(\\w+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");
	
	public TNT4JEventHandler(String source) {
		logger = TrackingLogger.getInstance(source);
	}
	
	@Override
    public void destroy(SyslogServerIF arg0) {
		logger.close();
    }

	@Override
    public void initialize(SyslogServerIF arg0) {
		try {
	        logger.open();
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
	}

	@Override
    public void event(Object arg0, SyslogServerIF arg1, SocketAddress arg2, SyslogServerEventIF event) {
		Date date = (event.getDate() == null ? new Date() : event.getDate());
		String facility = getFacilityString(event.getFacility());
		OpLevel level = getOpLevel(event.getLevel());	

		String fullMessage = new String(event.getRaw());
		TrackingEvent tevent = logger.newEvent(level, facility, null, fullMessage);
		tevent.getOperation().setType(OpType.EVENT);
		tevent.setLocation(event.getHost());
		
		if (event instanceof StructuredSyslogServerEvent) {
			// RFC 5424 
			StructuredSyslogServerEvent sevent = (StructuredSyslogServerEvent) event;
			tevent.getOperation().setResource(sevent.getApplicationName());
			tevent.getOperation().setPID(Long.parseLong(sevent.getProcessId()));
			tevent.getOperation().setTID(tevent.getOperation().getPID());
			
			// process structured message 
			StructuredSyslogMessage sm = sevent.getStructuredMessage();
			tevent.setTag(event.getHost(), sevent.getApplicationName(), sevent.getStructuredMessage().getMessageId());
			
			// set the appropriate source
			SourceFactory factory = logger.getConfiguration().getSourceFactory();
			Source rootSource = factory.getRootSource().getSource(SourceType.DATACENTER); // get to the datacenter source
			tevent.setSource(factory.newSource(sevent.getApplicationName(), SourceType.APPL, factory.newSource(event.getHost(), SourceType.SERVER, rootSource)));
			
			// process structured event attributes into snapshot
			Map<?, ?> map = sm.getStructuredData();
			PropertySnapshot snap = new PropertySnapshot("SyslogMap", sevent.getApplicationName(), level);
			snap.addAll(map);
			tevent.getOperation().addSnapshot(snap);
		} else {
			// RFC 3164 
			Map<String, Object> map = parseAttributes(event.getMessage());
			tevent.setTag(map.get("server.name").toString(), map.get("appl.name").toString());
			tevent.getOperation().setResource(map.get("appl.name").toString());
			tevent.getOperation().setPID((Long)map.get("appl.pid"));
			tevent.getOperation().setTID(tevent.getOperation().getPID());
			tevent.setCharset(arg1.getConfig().getCharSet());

			// set the appropriate source
			SourceFactory factory = logger.getConfiguration().getSourceFactory();
			Source rootSource = factory.getRootSource().getSource(SourceType.DATACENTER); // get to the datacenter source
			tevent.setSource(factory.newSource(map.get("appl.name").toString(), SourceType.APPL, factory.newSource(map.get("server.name").toString(), SourceType.SERVER, rootSource)));						
		}
		
		// extract nme=value pairs if available
		Map<String, Object> attr = parseVariables(event.getMessage());
		if (attr != null && attr.size() > 0) {
			PropertySnapshot snap = new PropertySnapshot("SyslogVars", tevent.getOperation().getResource(), level);
			snap.addAll(attr);
			tevent.getOperation().addSnapshot(snap);			
		}
		
		tevent.stop(date.getTime()*1000, getElapsedNanosSinceLastEvent(tevent.getLocation())/1000);
		logger.tnt(tevent);
	}

	private Map<String, Object> parseVariables(String message) {
		Map<String, Object> map = new HashMap<String, Object>();
		StringTokenizer tokens = new StringTokenizer(message, "[](){}");

		while (tokens.hasMoreTokens()) {
			String pair = tokens.nextToken();
			Matcher matcher = pattern.matcher(pair);
			while (matcher.find()) {
				String value = matcher.group(2);
				try {
					if (Character.isDigit(value.charAt(0))) {
						map.put(matcher.group(1), Long.parseLong(value));
						continue;
					}
				} catch (Throwable e) {
				}
				map.put(matcher.group(1), value);
			}
		}
		return map;
	}

	private Map<String, Object> parseAttributes(String message) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		String [] tokens = message.split(":| ");
		int first = tokens[1].indexOf("[");
		int last = tokens[1].indexOf("]");
		String applName = first >= 0? tokens[1].substring(0, first): tokens[1];
		
		map.put("server.name", tokens[0]);
		map.put("appl.part", tokens[1]);
		map.put("appl.name", applName);
		map.put("appl.pid", ((last >= first && (first >= 0))? Long.parseLong(tokens[1].substring(first+1, last)): 0));
		
	    return map;		
	}
	
	/**
	 * Obtain string representation of syslog facility
	 *
	 * @param facility syslog facility
	 * @return string representation of syslog facility
	 */
	public static String getFacilityString(int facility) {
	    return facility >= FACILITY.length? FACILITY[FACILITY.length-1]: FACILITY[facility];
    }

	/**
	 * Obtain syslog level to {@link OpLevel} mapping
	 *
	 * @param level syslog level
	 * @return {@link OpLevel} mapping
	 */
	public static OpLevel getOpLevel(int level) {
	    return level >= LEVELS.length? LEVELS[LEVELS.length-1]: LEVELS[level];
    }

	/**
	 * Obtain elapsed nanoseconds since last event
	 * 
	 * @param key timer key
	 * @return elapsed nanoseconds since last event
	 */
	protected long getElapsedNanosSinceLastEvent(String key) {
		AtomicLong last = EVENT_TIMER.get(key);
		if (last == null) {
			EVENT_TIMER.putIfAbsent(key, new AtomicLong(System.nanoTime()));
			last = EVENT_TIMER.get(key);
			last.set(System.nanoTime());
			return 0;
		}	
		long lastTimer = last.get();
		long now = System.nanoTime();
		last.compareAndSet(lastTimer, now);

		long elapsedNanos = last != null? (now - lastTimer): 0;
		return elapsedNanos < 0? 0: elapsedNanos;
	}

	@Override
    public void exception(Object arg0, SyslogServerIF arg1, SocketAddress arg2, Exception arg3) {
		logger.log(OpLevel.ERROR, "Sylog exception: obj={0}, syslog.server.if={1}, socket={2}", arg0, arg1, arg2, arg3);
	}

	@Override
    public void sessionClosed(Object arg0, SyslogServerIF arg1, SocketAddress arg2, boolean arg3) {
		logger.log(OpLevel.INFO, "Session closed: obj={0}, syslog.server.if={1}, socket={2}, timeout={3}", arg0, arg1, arg2, arg3);
    }

	@Override
    public Object sessionOpened(SyslogServerIF arg0, SocketAddress arg1) {
		logger.log(OpLevel.DEBUG, "Session opened: syslog.server.if={0}, socket={1}", arg0, arg1);
		return null;
    }
}
