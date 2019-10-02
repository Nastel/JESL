/*
 * Copyright 2015-2019 JKOOL, LLC.
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
package com.jkoolcloud.jesl.simulator;

import java.io.*;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.source.DefaultSourceFactory;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.tracker.DefaultTrackerFactory;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;
import com.jkoolcloud.tnt4j.utils.Utils;
import com.jkoolcloud.tnt4j.uuid.DefaultUUIDFactory;

/**
 * Implements the SAX DefaultHandler for parsing jKool TNT4J Activity Simulator XML.
 *
 * This is the guts of the simulator. As the activities and events are parsed, they are executed.
 *
 * @version $Revision: $
 */
public class TNT4JSimulatorParserHandler extends DefaultHandler {
	public static final String SIM_XML_ROOT = "tnt4j-simulator";
	public static final String SIM_XML_SOURCE = "source";
	public static final String SIM_XML_MSG = "msg";
	public static final String SIM_XML_SNAPSHOT = "snapshot";
	public static final String SIM_XML_DATASET = "dataset";
	public static final String SIM_XML_PROP = "prop";
	public static final String SIM_XML_VAR = "var";
	public static final String SIM_XML_OPTION = "option";
	public static final String SIM_XML_ACTIVITY = "activity";
	public static final String SIM_XML_EVENT = "event";
	public static final String SIM_XML_SLEEP = "sleep";

	public static final String SIM_XML_ATTR_ID = "id";
	public static final String SIM_XML_ATTR_NAME = "name";
	public static final String SIM_XML_ATTR_TYPE = "type";
	public static final String SIM_XML_ATTR_VALTYPE = "valtype";
	public static final String SIM_XML_ATTR_VALUE = "value";
	public static final String SIM_XML_ATTR_VARY = "vary";
	public static final String SIM_XML_ATTR_FQN = "fqn";
	public static final String SIM_XML_ATTR_USER = "user";
	public static final String SIM_XML_ATTR_URL = "url";
	public static final String SIM_XML_ATTR_MIME = "mime";
	public static final String SIM_XML_ATTR_ENC = "enc";
	public static final String SIM_XML_ATTR_CHARSET = "charset";
	public static final String SIM_XML_ATTR_FILE = "file";
	public static final String SIM_XML_ATTR_BINFILE = "binfile";
	public static final String SIM_XML_ATTR_SOURCE = SIM_XML_SOURCE;
	public static final String SIM_XML_ATTR_PID = "pid";
	public static final String SIM_XML_ATTR_TID = "tid";
	public static final String SIM_XML_ATTR_STATUS = "status";
	public static final String SIM_XML_ATTR_SEVERITY = "sev";
	public static final String SIM_XML_ATTR_CC = "cc";
	public static final String SIM_XML_ATTR_RC = "rc";
	public static final String SIM_XML_ATTR_EXC = "exc";
	public static final String SIM_XML_ATTR_ELAPSED = "elapsed";
	public static final String SIM_XML_ATTR_SNAPSHOTS = "snapshots";
	public static final String SIM_XML_ATTR_TAGS = "tags";
	public static final String SIM_XML_ATTR_CORRS = "corrs";
	public static final String SIM_XML_ATTR_MSG = SIM_XML_MSG;
	public static final String SIM_XML_ATTR_MSG_TEXT = "msgtext";
	public static final String SIM_XML_ATTR_MSGAGE = "msgage";
	public static final String SIM_XML_ATTR_WAIT = "wait";
	public static final String SIM_XML_ATTR_RES = "res";
	public static final String SIM_XML_ATTR_LOC = "loc";
	public static final String SIM_XML_ATTR_CAT = "cat";
	public static final String SIM_XML_ATTR_MSEC = "msec";
	public static final String SIM_XML_ATTR_USEC = "usec";

	private HashMap<String, Source> sourceNames = new HashMap<>();
	private HashMap<Integer, Source> sourceIds = new HashMap<>();
	private HashMap<Integer, Message> messageIds = new HashMap<>();
	private Stack<TrackingActivity> activeActivities = new Stack<>();
	private Stack<String> activeElements = new Stack<>();

	private HashMap<String, Long> genValues = new HashMap<>();
	private ConcurrentMap<String, String> vars = new ConcurrentHashMap<>();
	StringSubstitutor sub = new StringSubstitutor(vars);

	private Message curMsg;
	private TrackingActivity curActivity;
	private TrackingEvent curEvent;
	private PropertySnapshot curSnapshot;
	private UsecTimestamp curActivityStart;
	private UsecTimestamp simCurrTime;
	private String curElement;
	private StringBuilder curElmtValue = new StringBuilder();
	private Random rand = new Random();

	private String tagSuffix = "";
	private String corSuffix = "";

	private Locator saxLocator = null;

	private DefaultTrackerFactory trackerFactory = new DefaultTrackerFactory();
	private HashMap<String, Tracker> trackers = new HashMap<>();
	private Tracker curTracker = null;
	private Random random = new Random();

	public Map<String, Long> getSinkStats() {
		TreeMap<String, Long> sinkStats = new TreeMap<>();
		for (Tracker tracker : trackers.values()) {
			Map<String, Object> stats = tracker.getStats();
			for (String stat : stats.keySet()) {
				Object value = stats.get(stat);
				if (value instanceof Number) {
					TNT4JSimulator.incrementValue(sinkStats, stat, ((Number) value).longValue());
				}
			}
		}
		sinkStats.put(Utils.qualify(this, "tracker-sources"), (long) trackers.size());
		return sinkStats;
	}

	private String generateValues(String base) {
		if (base == null) {
			return base;
		}

		String newStr = base;

		String[] valLabels = base.split("%");
		for (int i = 1; i < valLabels.length; i += 2) {
			String valLabel = valLabels[i];
			if (StringUtils.isEmpty(valLabel)) {
				newStr = newStr.replace("%%", TNT4JSimulator.newUUID());
			} else {
				Long val = genValues.get(valLabel);
				if (val == null) {
					val = Utils.currentTimeUsec() + (genValues.size() + 1);
					genValues.put(valLabel, val);
				}
				newStr = newStr.replaceAll("%" + valLabel + "%", val.toString());
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

		curMsg = null;
		curActivity = null;
		curActivityStart = null;
		curElement = null;

		setDocumentLocator(saxLocator);

		if (TNT4JSimulator.useUniqueTags()) {
			tagSuffix = "@" + String.valueOf(Utils.currentTimeUsec()) + "@" + TNT4JSimulator.getIteration();
		}

		if (TNT4JSimulator.useUniqueIds()) {
			for (Message m : messageIds.values()) {
				m.setTrackingId(TNT4JSimulator.newUUID());
			}
		}

		if (TNT4JSimulator.useUniqueCorrs()) {
			corSuffix = "@" + String.valueOf(Utils.currentTimeUsec()) + "@" + TNT4JSimulator.getIteration();
		}
		simCurrTime = new UsecTimestamp();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		curElmtValue.setLength(0);

		if (name.equals(SIM_XML_ROOT)) {
			TNT4JSimulator.trace(simCurrTime, "Starting next iteration...");
		} else if (name.equals(SIM_XML_SOURCE)) {
			recordSource(attributes);
		} else if (name.equals(SIM_XML_SNAPSHOT)) {
			recordSnapshot(attributes);
		} else if (name.equals(SIM_XML_DATASET)) {
			recordDataset(attributes);
		} else if (name.equals(SIM_XML_PROP)) {
			recordProperty(attributes);
		} else if (name.equals(SIM_XML_MSG)) {
			recordMessage(attributes);
		} else if (name.equals(SIM_XML_ACTIVITY)) {
			startActivity(attributes);
		} else if (name.equals(SIM_XML_EVENT)) {
			runEvent(attributes);
		} else if (name.equals(SIM_XML_SLEEP)) {
			pauseSimulator(attributes);
		} else if (name.equals(SIM_XML_VAR)) {
			defineVar(attributes);
		} else if (name.equals(SIM_XML_OPTION)) {
			defineOption(attributes);
		} else {
			throw new SAXParseException("Unrecognized element <" + name + ">", saxLocator);
		}

		activeElements.push(curElement);
		curElement = name;
	}

	protected String processVarValue(String value) throws SAXParseException {
		double valueNext = 0;
		double totalValue = 0;
		String symbol = null;

		// If addition or multiplication are specified, then do the math.
		// For now, only one or the other is permitted.
		if (value.indexOf("+") > 0 && (value.indexOf("*") > 0)) {
			throw new SAXParseException("Either multiplicaton or addition but not both are allowed", saxLocator);
		} else if (value.indexOf("bet") > 0) {
			if ((value.substring(value.indexOf("bet") + 4, value.length()).length() > 9)) {
				BigInteger min = new BigInteger(value.substring(0, value.indexOf("bet") - 1));
				BigInteger max = new BigInteger(value.substring(value.indexOf("bet") + 4, value.length()));
				BigInteger diff = max.subtract(min);
				diff = diff.add(new BigInteger("1"));
				BigInteger rnd = new BigInteger(diff.bitLength(), random);
				BigInteger finalVal = rnd.add(min);
				value = "" + finalVal;
			} else {
				int min = Integer.parseInt(value.substring(0, value.indexOf("bet") - 1));
				int max = Integer.parseInt(value.substring(value.indexOf("bet") + 4, value.length()));
				value = "" + (random.nextInt(max - min + 1) + min);
			}
		} else if ((value.indexOf("+") > 0 && !value.contains("*"))
				|| (value.indexOf("*") > 0 && !value.contains("+"))) {
			symbol = (value.indexOf("+") > 0) ? "+" : "*";
			totalValue = symbol.equals("*") ? 1 : 0;
			while (value.indexOf(symbol) > 0) {
				valueNext = Double.parseDouble(vars.get(value.substring(0, value.indexOf(symbol) - 1)));
				if (symbol.equals("+")) {
					totalValue = totalValue + valueNext;
				} else {
					totalValue = totalValue * valueNext;
				}
				value = value.substring(value.indexOf(symbol) + 2, value.length());
			}
			totalValue = symbol.equals("*") ? totalValue * Double.parseDouble(value)
					: totalValue + Double.parseDouble(vars.get(value.substring(0, value.length())));
			value = "" + totalValue;
		} else if (value.indexOf("|") > 0) {
			StringTokenizer tk = new StringTokenizer(value, "|");
			ArrayList<String> tokens = new ArrayList<>();
			while (tk.hasMoreElements()) {
				tokens.add((String) tk.nextElement());
			}
			Random randomGenerator = new Random();
			int index = randomGenerator.nextInt(tokens.size());
			value = tokens.get(index);
		}
		return value;
	}

	private void defineOption(Attributes attributes) throws SAXException {
		String name = null;
		String value = null;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName = attributes.getQName(i);
				String attValue = expandEnvVars(attributes.getValue(i));

				if (attName.equals(SIM_XML_ATTR_NAME)) {
					name = attValue;
				} else if (attName.equals(SIM_XML_ATTR_VALUE)) {
					value = attValue;
					String[] args = value.split(",");
					TNT4JSimulator.processArgs(this, args);
				} else {
					throw new SAXParseException("Unknown <" + SIM_XML_PROP + "> attribute '" + attName + "'",
							saxLocator);
				}
			}

			if (StringUtils.isEmpty(name)) {
				throw new SAXParseException("<" + SIM_XML_VAR + ">: must specify '" + SIM_XML_ATTR_NAME + "'",
						saxLocator);
			}
			TNT4JSimulator.trace(simCurrTime, "Defining option: '" + name + "=" + value + "'");
		} catch (Exception e) {
			if (e instanceof SAXException) {
				throw (SAXException) e;
			}
			throw new SAXException("Failed processing definition for option '" + name + "': " + e, e);
		}
	}

	private String mapValue(String value) {
		if (value.equals("=0x")) {
			value = "0x" + Hex.encodeHexString(DefaultUUIDFactory.getInstance().newUUID().getBytes());
		}
		return value;
	}

	private void defineVar(Attributes attributes) throws SAXException {
		String name = null;
		String value = null;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName = attributes.getQName(i);
				String attValue = expandEnvVars(attributes.getValue(i));

				if (attName.equals(SIM_XML_ATTR_NAME)) {
					name = attValue;
				} else if (attName.equals(SIM_XML_ATTR_VALUE)) {
					value = processVarValue(attValue);
				} else {
					throw new SAXParseException("Unknown <" + SIM_XML_PROP + "> attribute '" + attName + "'",
							saxLocator);
				}
			}

			if (StringUtils.isEmpty(name)) {
				throw new SAXParseException("<" + SIM_XML_VAR + ">: must specify '" + SIM_XML_ATTR_NAME + "'",
						saxLocator);
			}

			if (value.startsWith("=?")) {
				// requires input if not defined
				String[] vals = value.split(":");
				String oVal = vars.get(name);

				// extract default value if one defined
				String defVal = vals.length > 1 ? vals[1] : null;

				if (oVal == null) {
					value = processVarValue(
							TNT4JSimulator.readFromConsole("\nDefine variable [" + name + "][" + defVal + "]:"));
				} else {
					TNT4JSimulator.trace(simCurrTime,
							"Skipping duplicate variable: '" + name + "=" + value + "', existing.value='" + oVal + "'");
				}
				value = (value == null || value.length() == 0) ? defVal : value;
			}

			String eVal = vars.putIfAbsent(name, mapValue(value));
			if (eVal != null) {
				TNT4JSimulator.trace(simCurrTime,
						"Skipping duplicate variable: '" + name + "=" + value + "', existing.value='" + eVal + "'");
			}
			TNT4JSimulator.trace(simCurrTime, "Defining variable: '" + name + "=" + value + "'");
		} catch (Exception e) {
			if (e instanceof SAXException) {
				throw (SAXException) e;
			}
			throw new SAXException("Failed processing definition for variable '" + name + "': " + e, e);
		}
	}

	private void recordSource(Attributes attributes) throws SAXException {
		if (TNT4JSimulator.getIteration() > 1L) {
			return;
		}

		if (!SIM_XML_ROOT.equals(curElement)) {
			throw new SAXParseException("<" + SIM_XML_SOURCE + ">: must have <" + SIM_XML_ROOT + "> as parent element",
					saxLocator);
		}

		int id = 0;
		String fqn = null;
		String url = null;
		String user = null;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName = attributes.getQName(i);
				String attValue = expandEnvVars(attributes.getValue(i));

				if (attName.equals(SIM_XML_ATTR_ID)) {
					id = Integer.parseInt(attValue);
				} else if (attName.equals(SIM_XML_ATTR_FQN)) {
					fqn = attValue;
				} else if (attName.equals(SIM_XML_ATTR_USER)) {
					user = attValue;
				} else if (attName.equals(SIM_XML_ATTR_URL)) {
					url = attValue;
				} else {
					throw new SAXParseException("Unknown <" + SIM_XML_SOURCE + "> attribute " + attName, saxLocator);
				}
			}

			if (id <= 0) {
				throw new SAXParseException(
						"<" + SIM_XML_SOURCE + "> element has missing or invalid " + SIM_XML_ATTR_ID + " attribute ",
						saxLocator);
			}

			if (sourceIds.containsKey(id)) {
				throw new SAXParseException(
						"<" + SIM_XML_SOURCE + "> duplicate " + SIM_XML_ATTR_ID + " attribute: " + id, saxLocator);
			}

			if (StringUtils.isEmpty(fqn)) {
				throw new SAXParseException(
						"<" + SIM_XML_SOURCE + "> element is missing " + SIM_XML_ATTR_FQN + " attribute ", saxLocator);
			}

			if (sourceNames.containsKey(fqn)) {
				throw new SAXParseException(
						"<" + SIM_XML_SOURCE + "> duplicate " + SIM_XML_ATTR_FQN + " attribute: " + fqn, saxLocator);
			}

			Source src = DefaultSourceFactory.getInstance().newFromFQN(fqn);
			src.setSSN("tnt4j-simulator");
			if (!StringUtils.isEmpty(user)) {
				src.setUser(user);
			}
			if (!StringUtils.isEmpty(url)) {
				src.setUrl(url);
			}

			sourceNames.put(fqn, src);
			sourceIds.put(id, src);
			TrackerConfig srcCfg = DefaultConfigFactory.getInstance().getConfig(fqn);
			srcCfg.setSource(src);

			srcCfg.setProperty("Url", TNT4JSimulator.getConnectUrl());
			String token = TNT4JSimulator.getAccessToken();
			if (!StringUtils.isEmpty(token)) {
				srcCfg.setProperty("Token", token);
			}
			if (TNT4JSimulator.getMPS() > 0) {
				srcCfg.setProperty("RateMaxMPS", String.valueOf(TNT4JSimulator.getMPS()));
				srcCfg.setProperty("RateLimit", String.valueOf(Boolean.TRUE));
			}
			if (TNT4JSimulator.getBPS() > 0) {
				srcCfg.setProperty("RateMaxBPS", String.valueOf(TNT4JSimulator.getBPS()));
				srcCfg.setProperty("RateLimit", String.valueOf(Boolean.TRUE));
			}
			Tracker tracker = trackerFactory.getInstance(srcCfg.build());
			trackers.put(fqn, tracker);
			TNT4JSimulator.info("Source: id=" + id + ", tracker=" + tracker + ", fqn=" + fqn);
		} catch (Exception e) {
			if (e instanceof SAXException) {
				throw (SAXException) e;
			}
			throw new SAXParseException("Failed processing definition for source: ", saxLocator, e);
		}
		TNT4JSimulator.trace(simCurrTime, "Recording Server: " + fqn + " ...");
	}

	private void recordSnapshot(Attributes attributes) throws SAXException {
		if ((curActivity == null || !SIM_XML_ACTIVITY.equals(curElement))
				&& (curEvent == null || !SIM_XML_EVENT.equals(curElement))) {
			throw new SAXParseException("<" + SIM_XML_SNAPSHOT + ">: must have <" + SIM_XML_ACTIVITY + "> or <"
					+ SIM_XML_EVENT + "> as parent element", saxLocator);
		}

		String name = null;
		String category = null;
		OpLevel severity = OpLevel.INFO;
		int srcId = 0;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName = attributes.getQName(i);
				String attValue = expandEnvVars(attributes.getValue(i));

				if (attName.equals(SIM_XML_ATTR_NAME)) {
					name = attValue;
					TNT4JSimulator.trace(simCurrTime, "Recording Snapshot: " + attValue + " ...");
				} else if (attName.equals(SIM_XML_ATTR_SOURCE)) {
					srcId = Integer.parseInt(attValue);
					if (srcId <= 0) {
						throw new SAXParseException(
								"Invalid <" + SIM_XML_SNAPSHOT + "> attribute '" + attName + "', must be > 0",
								saxLocator);
					}
				} else if (attName.equals(SIM_XML_ATTR_CAT)) {
					category = attValue;
				} else if (attName.equals(SIM_XML_ATTR_SEVERITY)) {
					severity = getLevel(attValue);
				} else {
					throw new SAXParseException("Unknown <" + SIM_XML_SNAPSHOT + "> attribute '" + attName + "'",
							saxLocator);
				}
			}
			if (StringUtils.isEmpty(name)) {
				throw new SAXParseException("<" + SIM_XML_SNAPSHOT + ">: missing '" + SIM_XML_ATTR_NAME + "'",
						saxLocator);
			}

			if (StringUtils.isEmpty(category)) {
				category = null;
			}

			if (srcId > 0) {
				Source source = sourceIds.get(srcId);
				if (source == null) {
					throw new SAXParseException(
							"<" + SIM_XML_SNAPSHOT + ">: " + SIM_XML_ATTR_SOURCE + " '" + srcId + "' is not defined",
							saxLocator);
				}

				curTracker = trackers.get(source.getFQName());
				if (curTracker == null) {
					throw new SAXParseException(
							"<" + SIM_XML_SNAPSHOT + ">: " + SIM_XML_ATTR_SOURCE + " '" + srcId + "' is not defined",
							saxLocator);
				}
			}
			curSnapshot = new PropertySnapshot(category, name, severity, simCurrTime);
			curSnapshot.setTTL(TNT4JSimulator.getTTL());
		} catch (Exception e) {
			if (e instanceof SAXException) {
				throw (SAXException) e;
			}
			throw new SAXException("Failed processing definition for snapshot '" + name + "': " + e, e);
		}
	}

	private void recordDataset(Attributes attributes) throws SAXException {
		String name = null;
		String category = null;
		int srcId = 0;
		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName = attributes.getQName(i);
				String attValue = expandEnvVars(attributes.getValue(i));

				if (attName.equals(SIM_XML_ATTR_NAME)) {
					name = attValue;
					TNT4JSimulator.trace(simCurrTime, "Recording Dataset: " + attValue + " ...");
				} else if (attName.equals(SIM_XML_ATTR_CAT)) {
					category = attValue;
				} else if (attName.equals(SIM_XML_ATTR_SOURCE)) {
					srcId = Integer.parseInt(attValue);
					if (srcId <= 0) {
						throw new SAXParseException(
								"Invalid <" + SIM_XML_DATASET + "> attribute '" + attName + "', must be > 0",
								saxLocator);
					}
				} else {
					throw new SAXParseException("Unknown <" + SIM_XML_DATASET + "> attribute '" + attName + "'",
							saxLocator);
				}
			}
			if (StringUtils.isEmpty(name)) {
				throw new SAXParseException("<" + SIM_XML_DATASET + ">: missing '" + SIM_XML_ATTR_NAME + "'",
						saxLocator);
			}

			if (StringUtils.isEmpty(category)) {
				category = null;
			}

			Source source = sourceIds.get(srcId);
			if (source == null) {
				throw new SAXParseException(
						"<" + SIM_XML_DATASET + ">: " + SIM_XML_ATTR_SOURCE + " '" + srcId + "' is not defined",
						saxLocator);
			}
			curTracker = trackers.get(source.getFQName());
			if (curTracker == null) {
				throw new SAXParseException(
						"<" + SIM_XML_DATASET + ">: " + SIM_XML_ATTR_SOURCE + " '" + srcId + "' is not defined",
						saxLocator);
			}
			curSnapshot = new Dataset(category, name);
			curSnapshot.setTimeStamp(simCurrTime);
			curSnapshot.setTTL(TNT4JSimulator.getTTL());
		} catch (Exception e) {
			if (e instanceof SAXException) {
				throw (SAXException) e;
			}
			throw new SAXException("Failed processing definition for dataset '" + name + "': " + e, e);
		}
	}

	private void recordProperty(Attributes attributes) throws SAXException {
		if (curElement == null) {
			throw new SAXParseException("<" + SIM_XML_PROP + ">: Must have <" + SIM_XML_ACTIVITY + ">, <"
					+ SIM_XML_EVENT + ">, or <" + SIM_XML_SNAPSHOT + "> as parent element", saxLocator);
		}

		String name = null;
		String type = null;
		String value = null;
		String valType = null;
		Boolean vary = Boolean.TRUE;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName = attributes.getQName(i);
				String attValue = expandEnvVars(attributes.getValue(i));

				if (attName.equals(SIM_XML_ATTR_NAME)) {
					name = attValue;
				} else if (attName.equals(SIM_XML_ATTR_TYPE)) {
					type = attValue;
				} else if (attName.equals(SIM_XML_ATTR_VALUE)) {
					value = mapValue(attValue);
				} else if (attName.equals(SIM_XML_ATTR_VALTYPE)) {
					valType = attValue;
				} else if (attName.equals(SIM_XML_ATTR_VARY)) {
					vary = Boolean.valueOf(attValue);
				} else {
					throw new SAXParseException("Unknown <" + SIM_XML_PROP + "> attribute '" + attName + "'",
							saxLocator);
				}
			}

			if (StringUtils.isEmpty(name)) {
				throw new SAXParseException("<" + SIM_XML_PROP + ">: must specify '" + SIM_XML_ATTR_NAME + "'",
						saxLocator);
			}

			TNT4JSimulator.trace(simCurrTime, "Recording " + curElement + " property: " + name + " ...");

			Property prop = processPropertyValue(name, type, value, valType, vary);

			if (SIM_XML_SNAPSHOT.equals(curElement)) {
				curSnapshot.add(prop);
			} else if (SIM_XML_DATASET.equals(curElement)) {
				curSnapshot.add(prop);
			} else if (SIM_XML_EVENT.equals(curElement)) {
				curEvent.getOperation().addProperty(prop);
			} else if (SIM_XML_ACTIVITY.equals(curElement)) {
				curActivity.addProperty(prop);
			}
		} catch (Exception e) {
			if (e instanceof SAXException) {
				throw (SAXException) e;
			}
			throw new SAXException("Failed processing definition for property '" + name + "': " + e, e);
		}
	}

	private Property processPropertyValue(String name, String type, String value, String valType, boolean vary)
			throws SAXParseException {
		Object propValue = value;

		if ("INTEGER".equalsIgnoreCase(type) || "INT".equalsIgnoreCase(type)) {
			Integer num = Integer.parseInt(generateFromRange(type, value));
			propValue = vary ? (int) TNT4JSimulator.varyValue(num) : num;
		} else if ("LONG".equalsIgnoreCase(type)) {
			Long num = Long.parseLong(generateFromRange(type, value));
			propValue = vary ? (long) TNT4JSimulator.varyValue(num) : num;
		} else if ("DECIMAL".equalsIgnoreCase(type)) {
			Double num = Double.parseDouble(generateFromRange(type, value));
			if (!vary) {
				propValue = num;
			} else {
				int precision = 2;
				String numStr = Double.toString(num);
				int dec = numStr.indexOf(DecimalFormatSymbols.getInstance().getDecimalSeparator());
				if (dec >= 0) {
					precision = numStr.length() - dec - 1;
				}
				propValue = TNT4JSimulator.varyValue(num, precision);
			}
		} else if ("BOOLEAN".equalsIgnoreCase(type)) {
			if (StringUtils.isEmpty(valType)) {
				valType = "boolean";
			}
			propValue = Boolean.parseBoolean(value);
		} else if ("TIMESTAMP".equalsIgnoreCase(type)) {
			try {
				try {
					long tstamp = Long.parseLong(value);
					propValue = new UsecTimestamp((vary ? TNT4JSimulator.varyValue(tstamp) : tstamp));
				} catch (NumberFormatException e) {
					propValue = new UsecTimestamp(value, "yyyy-MM-dd HH:mm:ss.SSSSSS", (String) null);
				}
			} catch (Exception e) {
				throw new SAXParseException("Failed parsing timestamp", saxLocator, e);
			}
			if (StringUtils.isEmpty(valType)) {
				valType = ValueTypes.VALUE_TYPE_TIMESTAMP;
			}
		} else if ("STRING".equalsIgnoreCase(type)) {
			propValue = mapValue(value.toString());
		} else if (!StringUtils.isEmpty(type)) {
			throw new SAXParseException("<" + SIM_XML_PROP + ">: invalid type: " + type, saxLocator);
		}
		return new Property(name, propValue, valType);
	}

	private void recordMessage(Attributes attributes) throws SAXException {
		if (TNT4JSimulator.getIteration() > 1L) {
			return;
		}

		if (!SIM_XML_ROOT.equals(curElement)) {
			throw new SAXParseException("<" + SIM_XML_MSG + ">: must have <" + SIM_XML_ROOT + "> as parent element",
					saxLocator);
		}

		int id = 0;
		String mimeType = null;
		String encoding = null;
		String charset = null;
		String fileName = null;
		boolean isBinary = false;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName = attributes.getQName(i);
				String attValue = expandEnvVars(attributes.getValue(i));

				if (attName.equals(SIM_XML_ATTR_ID)) {
					id = Integer.parseInt(attValue);
					TNT4JSimulator.trace(simCurrTime, "Recording Message: " + attValue + " ...");
				} else if (attName.equals(SIM_XML_ATTR_MIME)) {
					mimeType = attValue;
				} else if (attName.equals(SIM_XML_ATTR_ENC)) {
					encoding = attValue;
				} else if (attName.equals(SIM_XML_ATTR_CHARSET)) {
					charset = attValue;
				} else if (attName.equals(SIM_XML_ATTR_FILE)) {
					fileName = attValue;
				} else if (attName.equals(SIM_XML_ATTR_BINFILE)) {
					isBinary = Boolean.parseBoolean(attValue);
				} else {
					throw new SAXParseException("Unknown <" + SIM_XML_MSG + "> attribute '" + attName + "'",
							saxLocator);
				}
			}

			if (id == 0) {
				throw new SAXParseException("<" + SIM_XML_MSG + ">: missing or invalid '" + SIM_XML_ATTR_ID + "'",
						saxLocator);
			}

			curMsg = messageIds.get(id);
			if (curMsg == null) {
				curMsg = new Message(TNT4JSimulator.newUUID());
				messageIds.put(id, curMsg);
			}

			if (fileName != null) {
				if (isBinary) {
					BufferedInputStream fileReader = null;
					try {
						File f = new File(fileName);
						fileReader = new BufferedInputStream(new FileInputStream(f));
						byte[] binData = new byte[(int) f.length()];
						int totalBytesRead = 0;
						while (totalBytesRead < binData.length) {
							int bytesRemaining = binData.length - totalBytesRead;
							int bytesRead = fileReader.read(binData, totalBytesRead, bytesRemaining);
							if (bytesRead > 0) {
								totalBytesRead += bytesRead;
							}
							curMsg.setMessage(binData);
						}
					} catch (Exception e) {
						throw new SAXParseException("Failed loading message data from " + fileName, saxLocator, e);
					} finally {
						if (fileReader != null) {
							try {
								fileReader.close();
							} catch (Exception e1) {
							}
						}
					}
				} else {
					FileReader fileReader = null;
					try {
						fileReader = new FileReader(fileName);
						StringBuilder msgData = new StringBuilder();
						char[] text = new char[2048];
						int numRead = 0;
						while ((numRead = fileReader.read(text, 0, text.length)) > 0) {
							msgData.append(text, 0, numRead);
						}
						curMsg.setMessage(msgData.toString());
					} catch (Exception e) {
						throw new SAXParseException("Failed loading message data from " + fileName, saxLocator, e);
					} finally {
						if (fileReader != null) {
							try {
								fileReader.close();
							} catch (Exception e1) {
							}
						}
					}
				}

				curMsg = null; // to ignore msg element value
			}

			if (!StringUtils.isEmpty(mimeType)) {
				curMsg.setMimeType(mimeType);
			}
			if (!StringUtils.isEmpty(encoding)) {
				curMsg.setEncoding(encoding);
			}
			if (!StringUtils.isEmpty(charset)) {
				curMsg.setCharset(charset);
			}
		} catch (Exception e) {
			if (e instanceof SAXException) {
				throw (SAXException) e;
			}
			throw new SAXException("Failed processing definition for message '" + id + "': " + e, e);
		}
	}

	private void recordMsgData() throws SAXException {
		if (curMsg == null) {
			return;
		}

		String msgData = curElmtValue.toString();
		if (TNT4JSimulator.isGenerateValues()) {
			msgData = generateValues(msgData);
		}
		curMsg.setMessage(msgData);
	}

	private void startActivity(Attributes attributes) throws SAXException {
		// if (!SIM_XML_ROOT.equals(curElement))
		// throw new SAXParseException("<" + SIM_XML_ACTIVITY + ">: must have <" + SIM_XML_ROOT + "> as parent element",
		// saxLocator);

		if (simCurrTime == null) {
			simCurrTime = new UsecTimestamp();
		}

		TNT4JSimulator.trace(simCurrTime, "Started activity ...");

		TrackingActivity parentActivity = curActivity;

		activeActivities.push(curActivity);

		curActivity = null;

		curActivityStart = new UsecTimestamp(simCurrTime);

		String name = null;
		int srcId = 0;
		ActivityStatus status = null;
		OpLevel sev = null;
		OpCompCode cc = null;
		int rc = 0;
		long pid = 0L;
		long tid = 0L;
		String exc = null;
		String loc = null;
		String res = null;
		String user = null;
		String[] corrs = null;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName = attributes.getQName(i);
				String attValue = expandEnvVars(attributes.getValue(i));

				if (attName.equals(SIM_XML_ATTR_NAME)) {
					name = mapValue(attValue);
				} else if (attName.equals(SIM_XML_ATTR_SOURCE)) {
					srcId = Integer.parseInt(attValue);
					if (srcId <= 0) {
						throw new SAXParseException(
								"Invalid <" + SIM_XML_ACTIVITY + "> attribute '" + attName + "', must be > 0",
								saxLocator);
					}
				} else if (attName.equals(SIM_XML_ATTR_STATUS)) {
					status = ActivityStatus.valueOf(attValue);
				} else if (attName.equals(SIM_XML_ATTR_SEVERITY)) {
					sev = getLevel(attValue);
				} else if (attName.equals(SIM_XML_ATTR_CC)) {
					cc = OpCompCode.valueOf(attValue);
				} else if (attName.equals(SIM_XML_ATTR_RC)) {
					rc = Integer.parseInt(attValue);
				} else if (attName.equals(SIM_XML_ATTR_PID)) {
					pid = Long.parseLong(attValue);
					if (pid <= 0L) {
						throw new SAXParseException(
								"Invalid <" + SIM_XML_ACTIVITY + "> attribute '" + attName + "', must be > 0",
								saxLocator);
					}
				} else if (attName.equals(SIM_XML_ATTR_TID)) {
					tid = Long.parseLong(attValue);
					if (tid <= 0L) {
						throw new SAXParseException(
								"Invalid <" + SIM_XML_ACTIVITY + "> attribute '" + attName + "', must be > 0",
								saxLocator);
					}
				} else if (attName.equals(SIM_XML_ATTR_EXC)) {
					exc = attValue;
				} else if (attName.equals(SIM_XML_ATTR_LOC)) {
					loc = attValue;
				} else if (attName.equals(SIM_XML_ATTR_RES)) {
					res = attValue;
				} else if (attName.equals(SIM_XML_ATTR_USER)) {
					user = attValue;
				} else if (attName.equals(SIM_XML_ATTR_CORRS)) {
					corrs = attValue.split(",");
					for (int c = 0; c < corrs.length; c++) {
						if (TNT4JSimulator.isGenerateValues()) {
							corrs[c] = generateValues(corrs[c]);
						}
						if (!Utils.isEmpty(corSuffix)) {
							corrs[c] += corSuffix;
						}
					}
				} else {
					throw new SAXParseException("Unknown <" + SIM_XML_ACTIVITY + "> attribute '" + attName + "'",
							saxLocator);
				}
			}

			if (srcId <= 0) {
				throw new SAXParseException(
						"<" + SIM_XML_ACTIVITY + "> attribute '" + SIM_XML_ATTR_SOURCE + "' is missing", saxLocator);
			}

			Source source = sourceIds.get(srcId);
			if (source == null) {
				throw new SAXParseException(
						"<" + SIM_XML_ACTIVITY + ">: " + SIM_XML_ATTR_SOURCE + " '" + srcId + "' is not defined",
						saxLocator);
			}

			curTracker = trackers.get(source.getFQName());
			if (curTracker == null) {
				throw new SAXParseException(
						"<" + SIM_XML_ACTIVITY + ">: " + SIM_XML_ATTR_SOURCE + " '" + srcId + "' is not defined",
						saxLocator);
			}

			curActivity = curTracker.newActivity();
			curActivity.setTTL(TNT4JSimulator.getTTL());
			curActivity.setSource(source);
			curActivity.setUser(user == null ? source.getUser() : user);
			curActivity.setStatus(status == null ? ActivityStatus.BEGIN : status);
			curActivity.setSeverity(sev == null ? OpLevel.INFO : sev);
			curActivity.setCompCode(cc == null ? OpCompCode.SUCCESS : cc);
			if (pid > 0L) {
				curActivity.setPID(pid);
			}
			if (tid > 0L) {
				curActivity.setTID(tid);
			}
			if (!StringUtils.isEmpty(name)) {
				curActivity.setName(name);
			}
			if (!StringUtils.isEmpty(loc)) {
				curActivity.setLocation(loc);
			}
			if (!StringUtils.isEmpty(res)) {
				curActivity.setResource(res);
			}
			if (status != null) {
				curActivity.setStatus(status);
			}
			if (rc != 0) {
				curActivity.setReasonCode(rc);
			}
			if (!StringUtils.isEmpty(exc)) {
				curActivity.setException(exc);
			}
			if (!ArrayUtils.isEmpty(corrs)) {
				curActivity.setCorrelator(corrs);
			}

			TNT4JSimulator.debug(simCurrTime, "Started activity: " + name);
			curActivity.start(curActivityStart);

			if (parentActivity != null) {
				parentActivity.add(curActivity);
			}
		} catch (Exception e) {
			if (e instanceof SAXException) {
				throw (SAXException) e;
			}
			throw new SAXException("Failed processing event '" + name + "': " + e, e);
		}
	}

	private void stopActivity() throws SAXException {
		long elapsed = simCurrTime.difference(curActivity.getStartTime());
		curActivity.stop(simCurrTime, elapsed);
		TNT4JSimulator.debug(simCurrTime, "Stopped activity " + curActivity.getName() + ", elapsed.usec: " + elapsed);

		if (curActivity.getStatus() == ActivityStatus.BEGIN) {
			curActivity.setStatus(ActivityStatus.END);
		}

		Tracker tracker = trackers.get(curActivity.getSource().getFQName());
		if (tracker != null) {
			tracker.tnt(curActivity);
			try {
				tracker.getEventSink().flush();
			} catch (IOException e) {
				TNT4JSimulator.warn("Failed flushing event sink on stop of activity " + curActivity.getName(), e);
			}
		}

		curActivity = activeActivities.pop();
		curActivityStart = null;
		curTracker = null;
	}

	private void runEvent(Attributes attributes) throws SAXException {
		TNT4JSimulator.trace(simCurrTime, "Started event ...");

		String name = mapValue(expandEnvVars(attributes.getValue(SIM_XML_ATTR_NAME)));

		if (StringUtils.isEmpty(name)) {
			throw new SAXParseException("<" + SIM_XML_EVENT + ">: '" + SIM_XML_ATTR_NAME + "' must be specified",
					saxLocator);
		}

		if (simCurrTime == null) {
			simCurrTime = new UsecTimestamp();
		}

		OpType type = OpType.EVENT;
		OpLevel severity = OpLevel.INFO;
		String valStr;

		valStr = expandEnvVars(attributes.getValue(SIM_XML_ATTR_TYPE));
		if (!StringUtils.isEmpty(valStr)) {
			type = OpType.valueOf(valStr);
		}

		valStr = expandEnvVars(attributes.getValue(SIM_XML_ATTR_SEVERITY));
		if (!StringUtils.isEmpty(valStr)) {
			severity = getLevel(valStr);
		}

		int srcId = 0;
		OpCompCode cc = null;
		int rc = 0;
		long pid = 0L;
		long tid = 0L;
		String exc = null;
		String loc = null;
		String res = null;
		String user = null;
		String msgtext = null;
		String[] corrs = null;
		String[] labels = null;
		long elapsed = 0L;
		long msgAge = 0L;
		Integer msgId = 0;

		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName = attributes.getQName(i);
				String attValue = expandEnvVars(attributes.getValue(i));

				if (attName.equals(SIM_XML_ATTR_NAME)) {
					// handled above
				} else if (attName.equals(SIM_XML_ATTR_TYPE)) {
					// handled above
				} else if (attName.equals(SIM_XML_ATTR_SEVERITY)) {
					// handled above
				} else if (attName.equals(SIM_XML_ATTR_SOURCE)) {
					srcId = Integer.parseInt(attValue);
					if (srcId <= 0) {
						throw new SAXParseException(
								"Invalid <" + SIM_XML_EVENT + "> attribute '" + attName + "', must be > 0", saxLocator);
					}
				} else if (attName.equals(SIM_XML_ATTR_CC)) {
					cc = OpCompCode.valueOf(attValue);
				} else if (attName.equals(SIM_XML_ATTR_RC)) {
					rc = Integer.parseInt(attValue);
				} else if (attName.equals(SIM_XML_ATTR_PID)) {
					pid = Long.parseLong(attValue);
					if (pid <= 0L) {
						throw new SAXParseException(
								"Invalid <" + SIM_XML_EVENT + "> attribute '" + attName + "', must be > 0", saxLocator);
					}
				} else if (attName.equals(SIM_XML_ATTR_TID)) {
					tid = Long.parseLong(attValue);
					if (tid <= 0L) {
						throw new SAXParseException(
								"Invalid <" + SIM_XML_EVENT + "> attribute '" + attName + "', must be > 0", saxLocator);
					}
				} else if (attName.equals(SIM_XML_ATTR_EXC)) {
					exc = attValue;
				} else if (attName.equals(SIM_XML_ATTR_LOC)) {
					loc = attValue;
				} else if (attName.equals(SIM_XML_ATTR_RES)) {
					res = attValue;
				} else if (attName.equals(SIM_XML_ATTR_USER)) {
					user = attValue;
				} else if (attName.equals(SIM_XML_ATTR_ELAPSED)) {
					elapsed = Long.parseLong(attValue);
					if (elapsed < 0L) {
						throw new SAXParseException(
								"<" + SIM_XML_EVENT + ">: '" + SIM_XML_ATTR_ELAPSED + "' must be >= 0", saxLocator);
					}
				} else if (attName.equals(SIM_XML_ATTR_MSGAGE)) {
					msgAge = Long.parseLong(attValue);
					if (msgAge < 0L) {
						throw new SAXParseException(
								"Invalid <" + SIM_XML_EVENT + "> attribute '" + attName + "', must be >= 0",
								saxLocator);
					}
				} else if (attName.equals(SIM_XML_ATTR_TAGS)) {
					labels = attValue.split(",");
					for (int l = 0; l < labels.length; l++) {
						if (TNT4JSimulator.isGenerateValues()) {
							labels[l] = generateValues(labels[l]);
						}
						if (!Utils.isEmpty(tagSuffix)) {
							labels[l] += tagSuffix;
						}
					}
				} else if (attName.equals(SIM_XML_ATTR_CORRS)) {
					corrs = attValue.split(",");
					for (int c = 0; c < corrs.length; c++) {
						if (TNT4JSimulator.isGenerateValues()) {
							corrs[c] = generateValues(corrs[c]);
						}
						if (!Utils.isEmpty(corSuffix)) {
							corrs[c] += corSuffix;
						}
					}
				} else if (attName.equals(SIM_XML_ATTR_MSG)) {
					msgId = Integer.parseInt(attValue);
				} else if (attName.equals(SIM_XML_ATTR_MSG_TEXT)) {
					msgtext = attValue;
				} else {
					throw new SAXParseException("Unknown <" + SIM_XML_EVENT + "> attribute '" + attName + "'",
							saxLocator);
				}
			}

			if (srcId <= 0 && curActivity == null) {
				throw new SAXParseException("<" + SIM_XML_EVENT + "> attribute '" + SIM_XML_ATTR_SOURCE
						+ "' is missing for event without parent activity", saxLocator);
			}

			if ((msgId != null) && (msgtext != null)) {
				throw new SAXParseException("<" + SIM_XML_EVENT + "> has both attributes '" + SIM_XML_ATTR_MSG
						+ "' and '" + SIM_XML_ATTR_MSG_TEXT + "'", saxLocator);
			}

			Source source = null;
			if (srcId > 0) {
				source = sourceIds.get(srcId);
				if (source == null) {
					throw new SAXParseException(
							"<" + SIM_XML_EVENT + ">: " + SIM_XML_ATTR_SOURCE + " '" + srcId + "' is not defined",
							saxLocator);
				}

				curTracker = trackers.get(source.getFQName());
				if (curTracker == null) {
					throw new SAXParseException(
							"<" + SIM_XML_ACTIVITY + ">: " + SIM_XML_ATTR_SOURCE + " '" + srcId + "' is not defined",
							saxLocator);
				}
			}

			curEvent = curTracker.newEvent(severity, type, name, (String) null, (String) null, (String) null,
					(Object[]) null);
			source = (curEvent != null ? curEvent.getSource() : null);
			if (source == null) {
				source = (curActivity != null ? curActivity.getSource() : null);
			}

			if (source == null || curTracker == null) {
				throw new SAXParseException("<" + SIM_XML_EVENT + "> attribute '" + SIM_XML_ATTR_SOURCE
						+ "' is missing for event without parent activity", saxLocator);
			}

			if (curActivity != null) {
				curEvent.setLocation(curActivity.getLocation());
				curEvent.getOperation().setPID(curActivity.getPID());
				curEvent.getOperation().setTID(curActivity.getTID());
				curEvent.getOperation().setResource(curActivity.getResource());
				curEvent.getOperation().setUser(curActivity.getUser());
			}

			curEvent.setTTL(TNT4JSimulator.getTTL());
			curEvent.getOperation().setSeverity(severity == null ? OpLevel.INFO : severity);
			curEvent.getOperation().setCompCode(cc == null ? OpCompCode.SUCCESS : cc);
			if (srcId > 0) {
				curEvent.setSource(source);
			}
			if (!StringUtils.isEmpty(user)) {
				curEvent.getOperation().setUser(user);
			} else if (curActivity == null) {
				curEvent.getOperation().setUser(source.getUser());
			}
			if (pid > 0L) {
				curEvent.getOperation().setPID(pid);
			}
			if (tid > 0L) {
				curEvent.getOperation().setTID(tid);
			}
			if (!StringUtils.isEmpty(name)) {
				curEvent.getOperation().setName(name);
			}
			if (!StringUtils.isEmpty(loc)) {
				curEvent.setLocation(loc);
			}
			if (!StringUtils.isEmpty(res)) {
				curEvent.getOperation().setResource(res);
			}
			if (rc != 0) {
				curEvent.getOperation().setReasonCode(rc);
			}
			if (!StringUtils.isEmpty(exc)) {
				curEvent.getOperation().setException(exc);
			}
			if (!ArrayUtils.isEmpty(corrs)) {
				curEvent.setCorrelator(corrs);
			}
			if (!ArrayUtils.isEmpty(labels)) {
				curEvent.setTag(labels);
			}
			if (msgAge > 0L) {
				curEvent.setMessageAge(TNT4JSimulator.varyValue(msgAge));
			}

			elapsed = TNT4JSimulator.varyValue(elapsed);

			if (msgId != null && msgId > 0) {
				Message eventMsg = messageIds.get(msgId);
				if (eventMsg == null) {
					throw new SAXParseException(
							"Undefined " + SIM_XML_ATTR_MSG + " '" + msgId + "' for <" + SIM_XML_EVENT + ">",
							saxLocator);
				}

				curEvent.setTrackingId(eventMsg.getTrackingId());
				curEvent.setMessage(expandEnvVars(eventMsg.getMessage()));
			} else if (msgtext != null) {
				curEvent.setMessage(expandEnvVars(msgtext));
			}

			curEvent.start(simCurrTime);
			simCurrTime.add(0, elapsed);
			curEvent.stop(simCurrTime, elapsed);
			TNT4JSimulator.debug(simCurrTime, "Ran event: " + name + ", elapsed.usec=" + elapsed);
		} catch (Exception e) {
			if (e instanceof SAXException) {
				throw (SAXException) e;
			}
			throw new SAXException("Failed processing event '" + name + "': " + e, e);
		}
	}

	private void pauseSimulator(Attributes attributes) throws SAXException {
		long usec = 0;
		int i;

		for (i = 0; i < attributes.getLength(); i++) {
			String attName = attributes.getQName(i);
			String attValue = expandEnvVars(attributes.getValue(i));

			if (attName.equals(SIM_XML_ATTR_MSEC)) {
				usec = Long.parseLong(attValue) * 1000L;
			} else if (attName.equals(SIM_XML_ATTR_USEC)) {
				usec = Long.parseLong(attValue);
			} else {
				throw new SAXParseException("Unknown <" + SIM_XML_SLEEP + "> attribute '" + attName + "'", saxLocator);
			}
		}

		if (usec > 0) {
			simCurrTime.add(0L, TNT4JSimulator.varyValue(usec));
			TNT4JSimulator.trace(simCurrTime, "Executed sleep, usec=" + usec);
		}
	}

	/**
	 * Map string value to {@code OpLevel}
	 *
	 * @param valStr
	 *            level value string
	 * @return mapped {@code OpLevel} object instance
	 */
	public OpLevel getLevel(String valStr) {
		if (valStr.equalsIgnoreCase(OpLevel.ANY_LEVEL)) {
			return OpLevel.anyLevel();
		} else if (valStr.equalsIgnoreCase("SUCCESS")) {
			return OpLevel.INFO;
		}
		return OpLevel.valueOf(valStr);
	}

	/**
	 * Define a global variable usable for variable substitution
	 *
	 * @param name
	 *            variable name
	 * @param value
	 *            associated with the variable
	 */
	public void setVar(String name, String value) {
		vars.put(name, value);
	}

	/**
	 * Resolve variable given name to a global variable. Global variables are referenced using: ${var} convention.
	 *
	 * @param text
	 *            object to use
	 * @return resolved variable or itself if not a variable
	 */
	public String expandEnvVars(String text) {
		return StringSubstitutor.replaceSystemProperties(sub.replace(text));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		if (name.equals(SIM_XML_MSG)) {
			recordMsgData();
			curMsg = null;
		} else if (name.equals(SIM_XML_SNAPSHOT) || name.equals(SIM_XML_DATASET)) {
			if (curEvent != null) {
				curEvent.getOperation().addSnapshot(curSnapshot);
			} else if (curActivity != null) {
				curActivity.add(curSnapshot);
			} else if (curTracker != null) {
				curTracker.tnt(curSnapshot);
			} else {
				curSnapshot = null;
				throw new RuntimeException("Missing handling for " + uri + ", " + localName + ", " + name);
			}
		} else if (name.equals(SIM_XML_ACTIVITY)) {
			stopActivity();
		} else if (name.equals(SIM_XML_EVENT)) {
			if (curActivity != null) {
				curActivity.tnt(curEvent);
			} else {
				Tracker tracker = trackers.get(curEvent.getSource().getFQName());
				if (tracker != null) {
					tracker.tnt(curEvent);
				}
			}
			curEvent = null;
		}
		curElement = activeElements.pop();
	}

	long nextLong(Random rng, long n) {
		if (n <= 0) {
			throw new IllegalArgumentException("n must be positive");
		}

		long bits, val;
		do {
			bits = (rng.nextLong() << 1) >>> 1;
			val = bits % n;
		} while (bits - val + (n - 1) < 0L);
		return val;
	}

	public String generateFromRange(String type, String in) {
		if (in.indexOf('[') == 0 && in.indexOf(']') > 0) {
			StringTokenizer tk = new StringTokenizer(in, "[:]");
			if (type.equalsIgnoreCase("INTEGER")) {
				int min = Integer.parseInt(tk.nextToken());
				int max = Integer.parseInt(tk.nextToken());
				int range = max - min + 1;
				int randomNum = rand.nextInt(range) + min;
				return String.valueOf(randomNum);
			} else if (type.equalsIgnoreCase("LONG")) {
				long min = Long.parseLong(tk.nextToken());
				long max = Long.parseLong(tk.nextToken());
				long range = max - min + 1;
				long randomNum = nextLong(rand, range) + min;
				return String.valueOf(randomNum);
			} else if (type.equalsIgnoreCase("DECIMAL") || type.equalsIgnoreCase("FLOAT")
					|| type.equalsIgnoreCase("DOUBLE")) {
				double min = Double.parseDouble(tk.nextToken());
				double max = Double.parseDouble(tk.nextToken());
				double range = max - min + 1;
				double randomNum = nextLong(rand, (long) range) + min;
				double rdnFloat = randomNum + rand.nextDouble();
				return String.valueOf(rdnFloat);
			} else if (type.equalsIgnoreCase("BOOLEAN")) {
				int min = Integer.parseInt(tk.nextToken());
				int max = Integer.parseInt(tk.nextToken());
				int range = max - min + 1;
				int randomNum = rand.nextInt(range) + min;
				return String.valueOf(randomNum > min);
			}
		}
		return in;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		curElmtValue.append(ch, start, length);
	}
}
