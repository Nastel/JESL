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
package com.jkool.jesl.simulator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.nastel.jkool.tnt4j.config.DefaultConfigFactory;
import com.nastel.jkool.tnt4j.config.TrackerConfig;
import com.nastel.jkool.tnt4j.core.ActivityStatus;
import com.nastel.jkool.tnt4j.core.Message;
import com.nastel.jkool.tnt4j.core.OpCompCode;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.OpType;
import com.nastel.jkool.tnt4j.core.PropertySnapshot;
import com.nastel.jkool.tnt4j.core.UsecTimestamp;
import com.nastel.jkool.tnt4j.core.ValueTypes;
import com.nastel.jkool.tnt4j.source.DefaultSourceFactory;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.source.SourceType;
import com.nastel.jkool.tnt4j.tracker.DefaultTrackerFactory;
import com.nastel.jkool.tnt4j.tracker.Tracker;
import com.nastel.jkool.tnt4j.tracker.TrackingActivity;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;
import com.nastel.jkool.tnt4j.utils.Utils;

/**
 * Implements the SAX DefaultHandler for parsing jKool TNT4J Activity Simulator XML.
 *
 * This is the guts of the simulator.  As the activities and events are parsed, they
 * are executed.
 *
 * @version $Revision: $
 */
public class TNT4JSimulatorParserHandler extends DefaultHandler {
	public static final String SIM_XML_ROOT         = "tnt4j-simulator";
	public static final String SIM_XML_SOURCE       = "source";
	public static final String SIM_XML_MSG          = "msg";
	public static final String SIM_XML_SNAPSHOT     = "snapshot";
	public static final String SIM_XML_PROP         = "prop";
	public static final String SIM_XML_ACTIVITY     = "activity";
	public static final String SIM_XML_EVENT        = "event";
	public static final String SIM_XML_SLEEP        = "sleep";

	public static final String SIM_XML_ATTR_ID        = "id";
	public static final String SIM_XML_ATTR_NAME      = "name";
	public static final String SIM_XML_ATTR_TYPE      = "type";
	public static final String SIM_XML_ATTR_VALTYPE   = "valtype";
	public static final String SIM_XML_ATTR_VALUE     = "value";
	public static final String SIM_XML_ATTR_FQN       = "fqn";
	public static final String SIM_XML_ATTR_USER      = "user";
	public static final String SIM_XML_ATTR_INFO      = "info";
	public static final String SIM_XML_ATTR_URL       = "url";
	public static final String SIM_XML_ATTR_MIME      = "mime";
	public static final String SIM_XML_ATTR_ENC       = "enc";
	public static final String SIM_XML_ATTR_CHARSET   = "charset";
	public static final String SIM_XML_ATTR_FILE      = "file";
	public static final String SIM_XML_ATTR_BINFILE   = "binfile";
	public static final String SIM_XML_ATTR_SOURCE    = SIM_XML_SOURCE;
	public static final String SIM_XML_ATTR_PID       = "pid";
	public static final String SIM_XML_ATTR_TID       = "tid";
	public static final String SIM_XML_ATTR_STATUS    = "status";
	public static final String SIM_XML_ATTR_SEVERITY  = "sev";
	public static final String SIM_XML_ATTR_CC        = "cc";
	public static final String SIM_XML_ATTR_RC        = "rc";
	public static final String SIM_XML_ATTR_EXC       = "exc";
	public static final String SIM_XML_ATTR_ELAPSED   = "elapsed";
	public static final String SIM_XML_ATTR_SNAPSHOTS = "snapshots";
	public static final String SIM_XML_ATTR_TAGS      = "tags";
	public static final String SIM_XML_ATTR_CORRS     = "corrs";
	public static final String SIM_XML_ATTR_MSG       = SIM_XML_MSG;
	public static final String SIM_XML_ATTR_MSGAGE    = "msgage";
	public static final String SIM_XML_ATTR_WAIT      = "wait";
	public static final String SIM_XML_ATTR_RES       = "res";
	public static final String SIM_XML_ATTR_LOC       = "loc";
	public static final String SIM_XML_ATTR_CAT       = "cat";
	public static final String SIM_XML_ATTR_MSEC      = "msec";
	public static final String SIM_XML_ATTR_USEC      = "usec";

	private HashMap<String,Source>		sourceNames      = new HashMap<String,Source>();
	private HashMap<Integer,Source>		sourceIds        = new HashMap<Integer,Source>();
	private HashMap<Integer,Message>	messageIds       = new HashMap<Integer,Message>();
	private Stack<TrackingActivity>		activeActivities = new Stack<TrackingActivity>();
	private Stack<String>				activeElements   = new Stack<String>();

	private HashMap<String,Long> genValues = new HashMap<String,Long>();

	private Message				curMsg;
	private TrackingActivity	curActivity;
	private PropertySnapshot	curSnapshot;
	private UsecTimestamp		curActivityStart;
	private UsecTimestamp		simCurrTime;
	private String				curElement;
	private StringBuilder		curElmtValue = new StringBuilder();

	private String tagSuffix     = "";

	private Locator saxLocator = null;

	private DefaultTrackerFactory	trackerFactory = new DefaultTrackerFactory();
	private HashMap<String,Tracker>	trackers       = new HashMap<String,Tracker>();
	private Tracker					curTracker     = null;

	public Map<String,Long> getSinkStats() {
		TreeMap<String,Long> sinkStats = new TreeMap<String,Long>();
		for (Tracker tracker : trackers.values()) {
			Map<String,Object> stats = tracker.getStats();
			for (String stat : stats.keySet()) {
				Object value = stats.get(stat);
				if (value instanceof Number) {
					TNT4JSimulator.incrementValue(sinkStats, stat, ((Number)value).longValue());
				}
			}
		}
		sinkStats.put(Utils.qualify(this, "tracker-sources"), (long)trackers.size());
		return sinkStats;
	}

	private String generateValues(String base) {
		if (base == null)
			return base;

		String newStr = base;

		String[] valLabels = base.split("%");
		for (int i = 1; i < valLabels.length; i += 2) {
			String valLabel = valLabels[i];
			if (StringUtils.isEmpty(valLabel))
				newStr = newStr.replace("%%", Utils.newUUID());
			else {
				Long val = genValues.get(valLabel);
				if (val == null) {
					val = new Long(Utils.currentTimeUsec() + (genValues.size()+1));
					genValues.put(valLabel, val);
				}
				newStr = newStr.replaceAll("%"+valLabel+"%", val.toString());
			}
		}

		return newStr;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDocumentLocator(Locator locator) {
		saxLocator = locator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startDocument() throws SAXException {
		activeActivities.clear();
		activeElements.clear();
		genValues.clear();

		curMsg           = null;
		curActivity      = null;
		curActivityStart = null;
		curElement       = null;

		setDocumentLocator(saxLocator);

		if (TNT4JSimulator.useUniqueTags()) {
			tagSuffix = "@" + String.valueOf(Utils.currentTimeUsec()) + "@" + TNT4JSimulator.getIteration();

			for (Message m : messageIds.values())
				m.setTrackingId(Utils.newUUID());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		curElmtValue.setLength(0);

		if (name.equals(SIM_XML_ROOT))
			TNT4JSimulator.trace(simCurrTime, "Starting next iteration...");
		else if (name.equals(SIM_XML_SOURCE))
			recordSource(attributes);
		else if (name.equals(SIM_XML_SNAPSHOT))
			recordSnapshot(attributes);
		else if (name.equals(SIM_XML_PROP))
			recordProperty(attributes);
		else if (name.equals(SIM_XML_MSG))
			recordMessage(attributes);
		else if (name.equals(SIM_XML_ACTIVITY))
			startActivity(attributes);
		else if (name.equals(SIM_XML_EVENT))
			runEvent(attributes);
		else if (name.equals(SIM_XML_SLEEP))
			pauseSimulator(attributes);
		else
			throw new SAXParseException("Unrecognized element <" + name + ">", saxLocator);

		activeElements.push(curElement);
		curElement = name;
	}

	private void recordSource(Attributes attributes) throws SAXException {
		if (TNT4JSimulator.getIteration() > 1L)
			return;

		if (!SIM_XML_ROOT.equals(curElement))
			throw new SAXParseException("<" + SIM_XML_SOURCE + ">: must have <" + SIM_XML_ROOT + "> as parent element", saxLocator);

		int    id     = 0;
		String fqn    = null;
		String info   = null;
		String url    = null;
		String user   = null;
		String server = null;
		String ip     = null;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName  = attributes.getQName(i);
				String attValue = attributes.getValue(i);

				if (attName.equals(SIM_XML_ATTR_ID))
					id = Integer.parseInt(attValue);
				else if (attName.equals(SIM_XML_ATTR_FQN))
					fqn = attValue;
				else if (attName.equals(SIM_XML_ATTR_USER))
					user = attValue;
				else if (attName.equals(SIM_XML_ATTR_URL))
					url = attValue;
				else if (attName.equals(SIM_XML_ATTR_INFO))
					info = attValue;
				else
					throw new SAXParseException("Unknown <" + SIM_XML_SOURCE + "> attribute " + attName, saxLocator);
			}

			if (id == 0)
				throw new SAXParseException("<" + SIM_XML_SOURCE + "> element has missing or invalid " + SIM_XML_ATTR_ID + " attribute ", saxLocator);

			if (sourceIds.containsKey(id))
				throw new SAXParseException("<" + SIM_XML_SOURCE + "> duplicate " + SIM_XML_ATTR_ID + " attribute: " + id, saxLocator);

			if (StringUtils.isEmpty(fqn))
				throw new SAXParseException("<" + SIM_XML_SOURCE + "> element is missing " + SIM_XML_ATTR_FQN + " attribute ", saxLocator);

			if (sourceNames.containsKey(fqn))
				throw new SAXParseException("<" + SIM_XML_SOURCE + "> duplicate " + SIM_XML_ATTR_FQN + " attribute: " + fqn, saxLocator);

			String[] srcComps = fqn.split("#");
			for (int c = 0; c < srcComps.length; c++) {
				String[] comp = srcComps[c].split("=");
				if (comp.length != 2)
					throw new SAXParseException("<" + SIM_XML_SOURCE + "> malformed " + SIM_XML_ATTR_FQN + " attribute ", saxLocator);

				SourceType srcType = SourceType.valueOf(comp[0]);
				String     srcName = (comp[1].equals("?") ? null : comp[1]);

				if (srcType == SourceType.SERVER)
					server = srcName;
				else if (srcType == SourceType.NETADDR)
					ip = srcName;
			}

			if (StringUtils.isEmpty(server) || StringUtils.isEmpty(ip)) {
				if (StringUtils.isEmpty(server) && StringUtils.isEmpty(ip)) {
					InetAddress hostAddr = InetAddress.getLocalHost();
					server = hostAddr.getHostName();
					ip     = hostAddr.getHostAddress();
					if (StringUtils.isEmpty(info))
						info = System.getProperty("os.name") + " " + System.getProperty("os.version");
				}
				else if (StringUtils.isEmpty(server)) {
					InetAddress hostAddr = InetAddress.getByName(ip);
					server = hostAddr.getHostName();
				}
				if (StringUtils.isEmpty(ip)) {
					InetAddress hostAddr = InetAddress.getByName(server);
					ip = hostAddr.getHostAddress();
				}

				StringBuilder sb = new StringBuilder();
				for (int c = 0; c < srcComps.length; c++) {
					String[] comp = srcComps[c].split("=");
					SourceType srcType = SourceType.valueOf(comp[0]);
					String     srcName = comp[1];

					if (c > 0)
						sb.append("#");
					sb.append(srcType).append("=");
					if (srcType == SourceType.SERVER)
						sb.append(server);
					else if (srcType == SourceType.NETADDR)
						sb.append(ip);
					else
						sb.append(srcName);
				}

				fqn = sb.toString();
			}

			Source src = DefaultSourceFactory.getInstance().newFromFQN(fqn);
			src.setInfo(info);
			if (!StringUtils.isEmpty(user))
				src.setUser(user);
			if (!StringUtils.isEmpty(url))
				src.setUrl(url);

			sourceNames.put(fqn, src);
			sourceIds.put(id, src);

			TrackerConfig srcCfg = DefaultConfigFactory.getInstance().getConfig(fqn);
			srcCfg.setSource(src);

			srcCfg.setProperty("Url",   TNT4JSimulator.getConnectUrl());
			srcCfg.setProperty("Token", TNT4JSimulator.getAccessToken());

			Tracker tracker = trackerFactory.getInstance(srcCfg);
			trackers.put(fqn, tracker);
		}
		catch (Exception e) {
			if (e instanceof SAXException)
				throw (SAXException)e;
			throw new SAXParseException("Failed processing definition for source: ", saxLocator, e);
		}

		TNT4JSimulator.trace(simCurrTime, "Recording Server: " + fqn + " ...");
	}

	private void recordSnapshot(Attributes attributes) throws SAXException {
		if (curActivity == null || !SIM_XML_ACTIVITY.equals(curElement))
			throw new SAXParseException("<" + SIM_XML_SNAPSHOT + ">: must have <" + SIM_XML_ACTIVITY + "> as parent element", saxLocator);

		String  name     = null;
		String  category = null;
		OpLevel severity = OpLevel.INFO;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName  = attributes.getQName(i);
				String attValue = attributes.getValue(i);

				if (attName.equals(SIM_XML_ATTR_NAME)) {
					name = attValue;
					TNT4JSimulator.trace(simCurrTime, "Recording Snapshot: " + attValue + " ...");
				}
				else if (attName.equals(SIM_XML_ATTR_CAT)) {
					category = attValue;
				}
				else if (attName.equals(SIM_XML_ATTR_SEVERITY)) {
					severity = OpLevel.valueOf(attValue);
				}
				else {
					throw new SAXParseException("Unknown <" + SIM_XML_SNAPSHOT + "> attribute '" + attName + "'", saxLocator);
				}
			}

			if (StringUtils.isEmpty(name))
				throw new SAXParseException("<" + SIM_XML_SNAPSHOT + ">: missing '" + SIM_XML_ATTR_NAME + "'", saxLocator);

			if (StringUtils.isEmpty(category))
				category = null;

			curSnapshot = new PropertySnapshot(category, name, severity, simCurrTime);
		}
		catch (Exception e) {
			if (e instanceof SAXException)
				throw (SAXException)e;
			throw new SAXException("Failed processing definition for snapshot '" + name + "': " + e, e);
		}
	}

	private void recordProperty(Attributes attributes) throws SAXException {
		if (curSnapshot == null || !SIM_XML_SNAPSHOT.equals(curElement))
			throw new SAXParseException("<" + SIM_XML_PROP + ">: Must have <" + SIM_XML_SNAPSHOT + "> as parent element", saxLocator);

		String name    = null;
		String type    = null;
		String value   = null;
		String valType = null;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName  = attributes.getQName(i);
				String attValue = attributes.getValue(i);

				if (attName.equals(SIM_XML_ATTR_NAME))
					name = attValue;
				else if (attName.equals(SIM_XML_ATTR_TYPE))
					type = attValue;
				else if (attName.equals(SIM_XML_ATTR_VALUE))
					value = attValue;
				else if (attName.equals(SIM_XML_ATTR_VALTYPE))
					valType = attValue;
				else
					throw new SAXParseException("Unknown <" + SIM_XML_PROP + "> attribute '" + attName + "'", saxLocator);
			}

			if (StringUtils.isEmpty(name))
				throw new SAXParseException("<" + SIM_XML_PROP + ">: must specify '" + SIM_XML_ATTR_NAME + "'", saxLocator);

			TNT4JSimulator.trace(simCurrTime, "Recording Snapshot Property: " + name  + " ...");

			if (StringUtils.isEmpty(value)) {
				curSnapshot.add(name, "", valType);
			}
			else if ("NUMBER".equalsIgnoreCase(type)) {
				TNT4JSimulator.warn("Line: " + saxLocator.getLineNumber() + ", Column: " + saxLocator.getColumnNumber()
									+ ", 'NUMBER' datatype has been deprecated.  Use either 'INTEGER' or 'DECIMAL' for numeric values");
				Number num = NumberUtils.createNumber(value);
				if (num instanceof Double || num instanceof Float)
					curSnapshot.add(name, TNT4JSimulator.varyValue(num.doubleValue()), valType);
				else
					curSnapshot.add(name, (long)TNT4JSimulator.varyValue(num.doubleValue()), valType);
			}
			else if ("INTEGER".equalsIgnoreCase(type)) {
				Long num = Long.parseLong(value);
				curSnapshot.add(name, (long)TNT4JSimulator.varyValue(num.longValue()), valType);
			}
			else if ("DECIMAL".equalsIgnoreCase(type)) {
				Double num = Double.parseDouble(value);
				curSnapshot.add(name, TNT4JSimulator.varyValue(num.doubleValue()), valType);
			}
			else if ("BOOLEAN".equalsIgnoreCase(type)) {
				if (StringUtils.isEmpty(valType))
					valType = "boolean";
				curSnapshot.add(name, Boolean.parseBoolean(value), valType);
			}
			else if ("TIMESTAMP".equalsIgnoreCase(type)) {
				UsecTimestamp ts = null;
				try {
					ts = new UsecTimestamp((long)TNT4JSimulator.varyValue(Long.parseLong(value)));
				}
				catch (NumberFormatException e) {
					ts = new UsecTimestamp(value, "yyyy-MM-dd HH:mm:ss.SSSSSS", (String)null);
				}
				if (ts != null) {
					if (StringUtils.isEmpty(valType))
						valType = ValueTypes.VALUE_TYPE_TIMESTAMP;
					curSnapshot.add(name, ts, valType);
				}
			}
			else {
				curSnapshot.add(name, value, valType);
			}
		}
		catch (Exception e) {
			if (e instanceof SAXException)
				throw (SAXException)e;
			throw new SAXException("Failed processing definition for snapshot '" + name + "': " + e, e);
		}
	}

	private void recordMessage(Attributes attributes) throws SAXException {
		if (TNT4JSimulator.getIteration() > 1L)
			return;

		if (!SIM_XML_ROOT.equals(curElement))
			throw new SAXParseException("<" + SIM_XML_MSG + ">: must have <" + SIM_XML_ROOT + "> as parent element", saxLocator);

		int     id       = 0;
		String  mimeType = null;
		String  encoding = null;
		String  charset  = null;
		String  fileName = null;
		boolean isBinary = false;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName  = attributes.getQName(i);
				String attValue = attributes.getValue(i);

				if (attName.equals(SIM_XML_ATTR_ID)) {
					id = Integer.parseInt(attValue);
					TNT4JSimulator.trace(simCurrTime, "Recording Message: " + attValue + " ...");
				}
				else if (attName.equals(SIM_XML_ATTR_MIME)) {
					mimeType = attValue;
				}
				else if (attName.equals(SIM_XML_ATTR_ENC)) {
					encoding = attValue;
				}
				else if (attName.equals(SIM_XML_ATTR_CHARSET)) {
					charset = attValue;
				}
				else if (attName.equals(SIM_XML_ATTR_FILE)) {
					fileName = attValue;
				}
				else if (attName.equals(SIM_XML_ATTR_BINFILE)) {
					isBinary = Boolean.parseBoolean(attValue);
				}
				else {
					throw new SAXParseException("Unknown <" + SIM_XML_MSG + "> attribute '" + attName + "'", saxLocator);
				}
			}

			if (id == 0)
				throw new SAXParseException("<" + SIM_XML_MSG + ">: missing or invalid '" + SIM_XML_ATTR_ID + "'", saxLocator);

			curMsg = messageIds.get(id);
			if (curMsg == null) {
				curMsg = new Message(Utils.newUUID());
				messageIds.put(id, curMsg);
			}

			if (fileName != null) {
				if (isBinary) {
					BufferedInputStream fileReader = null;
					try {
						File f = new File(fileName);
						fileReader = new BufferedInputStream(new FileInputStream(f));
						byte[] binData = new byte[(int)f.length()];
				        int totalBytesRead = 0;
				        while (totalBytesRead < binData.length) {
				        	int bytesRemaining = binData.length - totalBytesRead;
				        	int bytesRead = fileReader.read(binData, totalBytesRead, bytesRemaining);
				        	if (bytesRead > 0)
				        		totalBytesRead += bytesRead;
				        	curMsg.setMessage(binData);
				        }
					}
					catch (Exception e) {
						throw new SAXParseException("Failed loading message data from " + fileName, saxLocator, e);
					}
					finally {
						if (fileReader != null)
							try {fileReader.close();} catch (Exception e1) {}
					}
				}
				else {
					FileReader fileReader = null;
					try {
						fileReader = new FileReader(fileName);
						StringBuffer msgData = new StringBuffer();
						char[] text = new char[2048];
						int numRead = 0;
						while ((numRead = fileReader.read(text, 0, text.length)) > 0)
							msgData.append(text, 0, numRead);
						curMsg.setMessage(msgData.toString());
					}
					catch (Exception e) {
						throw new SAXParseException("Failed loading message data from " + fileName, saxLocator, e);
					}
					finally {
						if (fileReader != null)
							try {fileReader.close();} catch (Exception e1) {}
					}
				}

				curMsg = null;	// to ignore msg element value
			}

			if (!StringUtils.isEmpty(mimeType))
				curMsg.setMimeType(	mimeType);
			if (!StringUtils.isEmpty(encoding))
				curMsg.setEncoding(encoding);
			if (!StringUtils.isEmpty(charset))
				curMsg.setCharset(charset);
		}
		catch (Exception e) {
			if (e instanceof SAXException)
				throw (SAXException)e;
			throw new SAXException("Failed processing definition for message '" + id + "': " + e, e);
		}
	}

	private void recordMsgData() throws SAXException {
		if (curMsg == null)
			return;

		String msgData = curElmtValue.toString();
		if (TNT4JSimulator.isGenerateValues())
			msgData = generateValues(msgData);
		curMsg.setMessage(msgData);
	}

	private void startActivity(Attributes attributes) throws SAXException {
		if (!SIM_XML_ROOT.equals(curElement))
			throw new SAXParseException("<" + SIM_XML_ACTIVITY + ">: must have <" + SIM_XML_ROOT + "> as parent element", saxLocator);

		TNT4JSimulator.trace(simCurrTime, "Started activity ...");

		TrackingActivity parentActivity = curActivity;

		activeActivities.push(curActivity);

		curActivity = null;

		if (simCurrTime == null)
			simCurrTime = new UsecTimestamp();

		curActivityStart = new UsecTimestamp(simCurrTime);

		String         name   = null;
		int            srcId  = 0;
		ActivityStatus status = null;
		OpLevel        sev    = null;
		OpCompCode     cc     = null;
		int            rc     = 0;
		long           pid    = 0L;
		long           tid    = 0L;
		String         exc    = null;
		String         loc    = null;
		String         res    = null;
		String         user   = null;
		String[]       corrs  = null;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName  = attributes.getQName(i);
				String attValue = attributes.getValue(i);

				if (attName.equals(SIM_XML_ATTR_NAME)) {
					name = attValue;
				}
				else if (attName.equals(SIM_XML_ATTR_SOURCE)) {
					srcId = Integer.parseInt(attValue);
					if (srcId <= 0)
						throw new SAXParseException("Invalid <" + SIM_XML_ACTIVITY + "> attribute '" + attName + "', must be > 0", saxLocator);
				}
				else if (attName.equals(SIM_XML_ATTR_STATUS)) {
					status = ActivityStatus.valueOf(attValue);
				}
				else if (attName.equals(SIM_XML_ATTR_SEVERITY)) {
					sev = OpLevel.valueOf(attValue);
				}
				else if (attName.equals(SIM_XML_ATTR_CC)) {
					cc = OpCompCode.valueOf(attValue);
				}
				else if (attName.equals(SIM_XML_ATTR_RC)) {
					rc = Integer.parseInt(attValue);
				}
				else if (attName.equals(SIM_XML_ATTR_PID)) {
					pid = Long.parseLong(attValue);
					if (pid <= 0L)
						throw new SAXParseException("Invalid <" + SIM_XML_ACTIVITY + "> attribute '" + attName + "', must be > 0", saxLocator);
				}
				else if (attName.equals(SIM_XML_ATTR_TID)) {
					tid = Long.parseLong(attValue);
					if (tid <= 0L)
						throw new SAXParseException("Invalid <" + SIM_XML_ACTIVITY + "> attribute '" + attName + "', must be > 0", saxLocator);
				}
				else if (attName.equals(SIM_XML_ATTR_EXC)) {
					exc = attValue;
				}
				else if (attName.equals(SIM_XML_ATTR_LOC)) {
					loc = attValue;
				}
				else if (attName.equals(SIM_XML_ATTR_RES)) {
					res = attValue;
				}
				else if (attName.equals(SIM_XML_ATTR_USER)) {
					user = attValue;
				}
				else if (attName.equals(SIM_XML_ATTR_CORRS)) {
					corrs = attValue.split(",");
					for (int c = 0; c < corrs.length; c++) {
						if (TNT4JSimulator.isGenerateValues())
							corrs[c] = generateValues(corrs[c]);
						corrs[c] += tagSuffix;
					}
				}
				else {
					throw new SAXParseException("Unknown <" + SIM_XML_ACTIVITY + "> attribute '" + attName + "'", saxLocator);
				}
			}

			if (!sourceIds.containsKey(srcId))
				throw new SAXParseException("<" + SIM_XML_ACTIVITY + "> attribute '" + SIM_XML_ATTR_SOURCE + "' is missing", saxLocator);

			Source source = sourceIds.get(srcId);
			if (source == null)
				throw new SAXParseException("<" + SIM_XML_ACTIVITY + ">: " + SIM_XML_ATTR_SOURCE + " '" + srcId + "' is not defined", saxLocator);

			curTracker = trackers.get(source.getFQName());
			if (curTracker == null)
				throw new SAXParseException("<" + SIM_XML_ACTIVITY + ">: " + SIM_XML_ATTR_SOURCE + " '" + srcId + "' is not defined", saxLocator);

			curActivity = curTracker.newActivity();
			curActivity.appendDefaultSnapshot(false);
			curActivity.setSource(source);
			curActivity.setUser(user == null ? source.getUser() : user);
			curActivity.setStatus(status == null ? ActivityStatus.BEGIN : status);
			curActivity.setSeverity(sev == null ? OpLevel.SUCCESS : sev);
			curActivity.setCompCode(cc == null ? OpCompCode.SUCCESS : cc);
			if (pid > 0L)
				curActivity.setPID(pid);
			if (tid > 0L)
				curActivity.setTID(tid);
			if (!StringUtils.isEmpty(name))
				curActivity.setName(name);
			if (!StringUtils.isEmpty(loc))
				curActivity.setLocation(loc);
			if (!StringUtils.isEmpty(res))
				curActivity.setResource(res);
			if (status != null)
				curActivity.setStatus(status);
			if (rc != 0)
				curActivity.setReasonCode(rc);
			if (!StringUtils.isEmpty(exc))
				curActivity.setException(exc);
			if (!ArrayUtils.isEmpty(corrs))
				curActivity.setCorrelator(corrs[0]);

			TNT4JSimulator.debug(simCurrTime, "Started activity: " + name);
			curActivity.start(curActivityStart);

			if (parentActivity != null)
				parentActivity.add(curActivity);
		}
		catch (Exception e) {
			if (e instanceof SAXException)
				throw (SAXException)e;
			throw new SAXException("Failed processing event '" + name + "': " + e, e);
		}
	}

	private void stopActivity() throws SAXException {
		long elapsed = simCurrTime.difference(curActivityStart);
		curActivity.stop(simCurrTime, elapsed);
		TNT4JSimulator.debug(simCurrTime, "Stopped activity " + curActivity.getName() + ", elapsed.usec: " + elapsed);

		if (curActivity.getStatus() == ActivityStatus.BEGIN)
			curActivity.setStatus(ActivityStatus.END);

		Tracker tracker = trackers.get(curActivity.getSource().getFQName());
		if (tracker != null)
			tracker.tnt(curActivity);

		curActivity = activeActivities.pop();
		curActivityStart = null;
		curTracker = null;
	}

	private void runEvent(Attributes attributes) throws SAXException {
		if (curActivity == null || !SIM_XML_ACTIVITY.equals(curElement))
			throw new SAXParseException("<" + SIM_XML_EVENT + ">: Must have <" + SIM_XML_ACTIVITY+ "> as parent element", saxLocator);

		TNT4JSimulator.trace(simCurrTime, "Started event ...");

		String name = attributes.getValue(SIM_XML_ATTR_NAME);

		if (StringUtils.isEmpty(name))
			throw new SAXParseException("<" + SIM_XML_EVENT + ">: '" + SIM_XML_ATTR_NAME + "' must be specified", saxLocator);

		OpType     type     = OpType.EVENT;
		OpLevel    severity = OpLevel.SUCCESS;
		String     valStr;

		valStr = attributes.getValue(SIM_XML_ATTR_TYPE);
		if (!StringUtils.isEmpty(valStr))
			type = OpType.valueOf(valStr);

		valStr = attributes.getValue(SIM_XML_ATTR_SEVERITY);
		if (!StringUtils.isEmpty(valStr))
			severity = OpLevel.valueOf(valStr);

		TrackingEvent event = curTracker.newEvent(severity, type, name, null, null, (String)null, (Object[])null);

		event.setLocation(curActivity.getLocation());
		event.getOperation().setPID(curActivity.getPID());
		event.getOperation().setTID(curActivity.getTID());
		event.getOperation().setResource(curActivity.getResource());
		event.getOperation().setUser(curActivity.getUser());

		long elapsed  = 0L;
		int  msgId    = 0;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName  = attributes.getQName(i);
				String attValue = attributes.getValue(i);

				if (attName.equals(SIM_XML_ATTR_NAME)) {
					// handled above
				}
				else if (attName.equals(SIM_XML_ATTR_TYPE)) {
					// handled above
				}
				else if (attName.equals(SIM_XML_ATTR_SEVERITY)) {
					// handled above
				}
				else if (attName.equals(SIM_XML_ATTR_CC)) {
					event.getOperation().setCompCode(OpCompCode.valueOf(attValue));
				}
				else if (attName.equals(SIM_XML_ATTR_RC)) {
					event.getOperation().setReasonCode(Integer.parseInt(attValue));
				}
				else if (attName.equals(SIM_XML_ATTR_PID)) {
					long pid = Long.parseLong(attValue);
					if (pid <= 0L)
						throw new SAXParseException("Invalid <" + SIM_XML_ACTIVITY + "> attribute '" + attName + "', must be > 0", saxLocator);
					event.getOperation().setPID(pid);
				}
				else if (attName.equals(SIM_XML_ATTR_TID)) {
					long tid = Long.parseLong(attValue);
					if (tid <= 0L)
						throw new SAXParseException("Invalid <" + SIM_XML_ACTIVITY + "> attribute '" + attName + "', must be > 0", saxLocator);
					event.getOperation().setTID(tid);
				}
				else if (attName.equals(SIM_XML_ATTR_EXC)) {
					event.getOperation().setException(attValue);
				}
				else if (attName.equals(SIM_XML_ATTR_LOC)) {
					event.setLocation(attValue);
				}
				else if (attName.equals(SIM_XML_ATTR_RES)) {
					event.getOperation().setResource(attValue);
				}
				else if (attName.equals(SIM_XML_ATTR_USER)) {
					event.getOperation().setUser(attValue);
				}
				else if (attName.equals(SIM_XML_ATTR_ELAPSED)) {
					elapsed = Long.parseLong(attValue);
					if (elapsed < 0L)
						throw new SAXParseException("<" + SIM_XML_EVENT + ">: '" + SIM_XML_ATTR_ELAPSED + "' must be >= 0", saxLocator);
				}
				else if (attName.equals(SIM_XML_ATTR_MSGAGE)) {
					long msgAge = Long.parseLong(attValue);
					if (msgAge < 0L)
						throw new SAXParseException("Invalid <" + SIM_XML_ACTIVITY + "> attribute '" + attName + "', must be >= 0", saxLocator);
					event.setMessageAge((long)TNT4JSimulator.varyValue(msgAge));
				}
				else if (attName.equals(SIM_XML_ATTR_TAGS)) {
					String[] labels = attValue.split(",");
					for (int l = 0; l < labels.length; l++) {
						if (TNT4JSimulator.isGenerateValues())
							labels[l] = generateValues(labels[l]);
						labels[l] += tagSuffix;
					}
					event.setTag(labels[0]);
				}
				else if (attName.equals(SIM_XML_ATTR_CORRS)) {
					String[] corrs = attValue.split(",");
					for (int c = 0; c < corrs.length; c++) {
						if (TNT4JSimulator.isGenerateValues())
							corrs[c] = generateValues(corrs[c]);
						corrs[c] += tagSuffix;
					}
					event.setCorrelator(corrs[0]);
				}
				else if (attName.equals(SIM_XML_ATTR_MSG)) {
					msgId = Integer.parseInt(attValue);
				}
				else {
					throw new SAXParseException("Unknown <" + SIM_XML_EVENT + "> attribute '" + attName + "'", saxLocator);
				}
			}

			elapsed = (long)TNT4JSimulator.varyValue(elapsed);

			if (msgId != 0) {
				Message eventMsg = messageIds.get(msgId);
				if (eventMsg == null)
					throw new SAXParseException("Undefined " + SIM_XML_ATTR_MSG + " '" + msgId + "' for <" + SIM_XML_EVENT + ">", saxLocator);

				event.setTrackingId(eventMsg.getTrackingId());

//				if (eventMsg.isDataBinary())
//					event.setMessage(eventMsg.getBinaryMessage());
//				else
					event.setMessage(eventMsg.getMessage());
			}

			event.start(simCurrTime);
			simCurrTime.add(0, elapsed);
			event.stop(simCurrTime, elapsed);

			curActivity.tnt(event);

			TNT4JSimulator.debug(simCurrTime, "Ran event: " + name + ", elapsed.usec=" + elapsed);
		}
		catch (Exception e) {
			if (e instanceof SAXException)
				throw (SAXException)e;
			throw new SAXException("Failed processing event '" + name + "': " + e, e);
		}
	}

	private void pauseSimulator(Attributes attributes) throws SAXException {
		long usec = 0;
		int i;

		for (i = 0; i < attributes.getLength(); i++) {
			String attName  = attributes.getQName(i);
			String attValue = attributes.getValue(i);

			if (attName.equals(SIM_XML_ATTR_MSEC))
				usec = Long.parseLong(attValue) * 1000L;
			else if (attName.equals(SIM_XML_ATTR_USEC))
				usec = Long.parseLong(attValue);
			else
				throw new SAXParseException("Unknown <" + SIM_XML_SLEEP + "> attribute '" + attName + "'", saxLocator);
		}

		if (usec > 0) {
			simCurrTime.add(0L, (long)TNT4JSimulator.varyValue(usec));
			TNT4JSimulator.trace(simCurrTime, "Executed sleep, usec=" + usec);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		if (name.equals(SIM_XML_MSG)) {
			recordMsgData();
			curMsg = null;
		}
		else if (name.equals(SIM_XML_SNAPSHOT)) {
			curActivity.add(curSnapshot);
			curSnapshot = null;
		}
		else if (name.equals(SIM_XML_ACTIVITY)) {
			stopActivity();
		}

		curElement = activeElements.pop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		curElmtValue.append(ch, start, length);
	}
}
