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
package com.jkoolcloud.jesl.simulator;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import com.jkoolcloud.jesl.simulator.tnt4j.JKCloudConnection;
import com.jkoolcloud.tnt4j.TrackingLogger;
import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.UsecTimestamp;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * JESL Activity Simulator which uses TNT4J to simulate application activities and transactions. This will simulate
 * activities by loading activity definitions from an XML file.
 *
 * @version $Revision: $
 */
public class TNT4JSimulator {

	public enum SimulatorRunType {
		RUN_SIM, REPLAY_SIM
	}

	private static SimulatorRunType runType = SimulatorRunType.RUN_SIM;
	private static String jkProtocol = "https";
	private static String jkHost = null;
	private static int jkPort = 443;
	private static String jkAccessToken = null;
	private static long jKConnTimeout = 10000;
	private static String simFileName = "";
	private static boolean uniqueTags = false;
	private static boolean uniqueCorrs = false;
	private static boolean uniqueIds = false;
	private static OpLevel traceLevel = OpLevel.INFO;
	private static String jkFileName = "";
	private static int valuePctChg = 0;
	private static Random ranGen = new Random();
	private static long numIterations = 1;
	private static boolean generateValues = false;
	private static long ttl = 0L;
	private static long rateMPS = 0, rateBPS = 0;

	private static JKCloudConnection gwConn = null;
	private static TrackingLogger logger = null;
	private static long iteration = 0L;

	public static String readFromConsole(String prompt) throws IOException {
		return System.console().readLine(prompt);
	}

	public static void error(String msg, Throwable e) {
		logger.tnt(OpLevel.ERROR, null, null, msg, e);
	}

	public static void warn(String msg, Throwable e) {
		logger.tnt(OpLevel.WARNING, null, null, msg, e);
	}

	public static void info(String msg) {
		logger.tnt(OpLevel.INFO, null, null, msg, (Object[]) null);
	}

	public static boolean isDebugEnabled() {
		return (traceLevel == OpLevel.DEBUG || traceLevel == OpLevel.TRACE);
	}

	public static void debug(UsecTimestamp simTime, String msg) {
		if (isDebugEnabled()) {
			if (simTime == null) {
				simTime = new UsecTimestamp();
			}
			logger.tnt(OpLevel.DEBUG, null, null, "SimTime=" + simTime + ": " + msg);
		}
	}

	public static boolean isTraceEnabled() {
		return (traceLevel == OpLevel.TRACE);
	}

	public static void trace(UsecTimestamp simTime, String msg) {
		if (isTraceEnabled()) {
			if (simTime == null) {
				simTime = new UsecTimestamp();
			}
			logger.tnt(OpLevel.TRACE, null, null, "SimTime=" + simTime + ": " + msg);
		}
	}

	public static String getProtocol() {
		return jkProtocol;
	}

	public static String getHost() {
		return jkHost;
	}

	public static int getPort() {
		return jkPort;
	}

	public static String getFileName() {
		return jkFileName;
	}

	public static String getConnectUrl() {
		return (runType == SimulatorRunType.RUN_SIM && jkProtocol.equalsIgnoreCase("file")
				? (jkProtocol.toLowerCase() + "://" + jkFileName)
				: (jkProtocol.toLowerCase() + "://" + jkHost + ":" + jkPort));
	}

	public static String getAccessToken() {
		return jkAccessToken;
	}

	public static long getConnectionTimeout() {
		return jKConnTimeout;
	}

	public static boolean useUniqueTags() {
		return uniqueTags;
	}

	public static boolean useUniqueCorrs() {
		return uniqueCorrs;
	}

	public static boolean useUniqueIds() {
		return uniqueIds;
	}

	public static boolean isGenerateValues() {
		return generateValues;
	}

	public static boolean isVaryingValuesEnabled() {
		return valuePctChg != 0;
	}

	public static long varyValue(long value) {
		return (long) varyValue(value, 0);
	}

	public static long varyValue(int value) {
		return (long) varyValue(value, 0);
	}

	public static double varyValue(double value, int precision) {
		if (value == 0 || valuePctChg == 0) {
			return value;
		}

		// nextInt returns value in range [0,n). We want values in range [-n,n).
		double percentChg = (ranGen.nextInt(valuePctChg * 2) - valuePctChg) / 100.0;

		// let's keep a lid on variations
		if (percentChg > 0.99) {
			percentChg = 0.99;
		}
		if (percentChg < -0.99) {
			percentChg = -0.99;
		}

		double newValue = value * (1.0 + percentChg);

		if (precision > 0) {
			newValue = BigDecimal.valueOf(newValue).setScale(precision, RoundingMode.HALF_UP).doubleValue();
		}

		if (newValue < 0.0 && value > 0.0) {
			newValue = 0;
		}

		return newValue;
	}

	public static void incrementValue(Map<String, Long> map, String item, long amount) {
		Long prev = map.put(item, amount);
		if (prev != null) {
			map.put(item, prev + amount);
		}
	}

	public static long getIteration() {
		return iteration;
	}

	public static long getBPS() {
		return rateBPS;
	}

	public static long getMPS() {
		return rateMPS;
	}

	public static long getTTL() {
		return ttl;
	}

	public static String newUUID() {
		return logger.newUUID();
	}

	private static void printUsage(String error) {
		if (!StringUtils.isEmpty(error)) {
			System.out.println(error);
		}

		String usageStr = "\nValid arguments:\n"
				+ "  to run simulation:      run -A:<access_token> [-T:<jk_host>] [-P:<jk_port>] [-C:tcp|http|https] [-O:<timeout_sec>] [-f:<sim_def_file_name>]\n"
				+ "                              [-p:<percentage>] [-V:name=value] [-G:<jk_file_name>] [-i:<iterations>] [-u] [-t:<ttl_hours>]\n\n"
				+ "  to limit streaming:         [-LM:max-msgs-sec] [-LB:max-bytes-sec]\n\n"
				+ "  to replay simulation:   replay -A:<access_token> -T:<jk_host> [-P:<jk_port>] [-C:tcp|http|https] [-O:<timeout_sec>] -G:<jk_file_name>\n\n"
				+ "  for usage information:  help\n\n"
				+ "where:                                                      \n"
				+ "    -A    -  Streaming access token (required with '-T')\n"
				+ "    -T    -  Host name or IP address of the streaming data service\n"
				+ "             (if not specified, data is not sent to data streaming service)\n"
				+ "    -V    -  Define a global variable (property) name=value pair\n"
				+ "    -P    -  Data streaming port where service is listening on (default: SSL 443)\n"
				+ "    -C    -  Connection type to use with data streaming service (default: https)\n"
				+ "    -O    -  Data streaming service connection timeout in seconds (default: 10)\n"
				+ "    -LB   -  Limit streaming to a maximum of a given bytes/sec rate per defined source\n"
				+ "    -LM   -  Limit streaming to a maximum of a given msgs/sec rate per defined source\n"
				+ "    -f    -  Use <sim_def_file_name> as simulation configuration\n"
				+ "    -p    -  Vary all numeric values by +/- <percentage>\n"
				+ "    -G    -  Read/Write messages from/to <jk_file_name>\n"
				+ "             (if writing, data is appended to an existing file)\n"
				+ "    -i    -  Number of iterations to make on <sim_file_name>\n"
				+ "    -u    -  Make tags, correlators, ids unique between iterations\n"
				+ "    -ut   -  Make tags unique between iterations\n"
				+ "    -uc   -  Make correlators unique between iterations\n"
				+ "    -ui   -  Make tracking ids unique between iterations\n"
				+ "    -t    -  Time-to-live (TTL) for all tracking items, in hours\n"
				+ "                > 0 : tracking events are deleted after the specified number of hours\n"
				+ "                = 0 : tracking events are deleted based on Retention quota for repository (default)\n"
				+ "                < 0 : tracking events are not persisted to data store (only processed by Real-time Grid)\n\n"
				+ "For 'run' mode, must specify at least of one: '-T', '-G'";

		System.out.println(usageStr);

		System.exit(StringUtils.isEmpty(error) ? 0 : 1);
	}

	private static void printUsedArgs() {
		System.out.format(
				"Arguments: runype=%s, url=%s, generateValues=%s, uniqueTags=%s,  uniqueCorrs=%s, uniqueIds=%s, percent=%d, simFile=%s, jkFile=%s\n",
				runType, getConnectUrl(), generateValues, uniqueTags, uniqueCorrs, uniqueIds, valuePctChg, simFileName,
				jkFileName);
		if (rateMPS > 0 || rateBPS > 0) {
			System.out.format("Limits: msgs/sec=%s, bytes/sec=%s\n", rateMPS, rateBPS);
		}
	}

	protected static void processArgs(TNT4JSimulatorParserHandler xmlHandler, String[] args) {
		if (args.length == 0) {
			printUsage("Missing simulation type");
		}

		String runTypeArg = args[0];
		if (runTypeArg.equals("run")) {
			runType = SimulatorRunType.RUN_SIM;
		} else if (runTypeArg.equals("replay")) {
			runType = SimulatorRunType.REPLAY_SIM;
		} else if (runTypeArg.equals("help")) {
			printUsage(null);
		} else {
			printUsage("Invalid simulation type");
		}

		boolean invalidArg = false;
		for (int i = 1; i < args.length; i++) {
			String arg = args[i];

			if (arg == null) {
				continue;
			}

			// Process arguments common to all simulation types first
			if (arg.startsWith("-A:")) {
				jkAccessToken = arg.substring(3);
				if (StringUtils.isEmpty(jkAccessToken)) {
					printUsage("Missing <access_token> for '-A' argument");
				}
			} else if (arg.startsWith("-V:")) {
				String[] var = arg.substring(3).split("=");
				if (var.length == 2) {
					System.out.println("Defining variable: '" + var[0] + "=" + var[1] + "'");
					xmlHandler.setVar(var[0], var[1]);
				} else {
					printUsage("Variable must have name=value pair");
				}
			} else if (arg.startsWith("-T:")) {
				jkHost = arg.substring(3);
				if (StringUtils.isEmpty(jkHost)) {
					printUsage("Missing <jk_host> for '-T' argument");
				}
			} else if (arg.startsWith("-P:")) {
				try {
					jkPort = Integer.parseInt(arg.substring(3));
				} catch (NumberFormatException e) {
					printUsage("Missing or invalid <jk_port> for '-P' argument");
				}
			} else if (arg.startsWith("-C:")) {
				jkProtocol = arg.substring(3).toLowerCase();
				if (!"tcp".equals(jkProtocol) && !"http".equals(jkProtocol) && !"https".equals(jkProtocol)) {
					printUsage(
							"Invalid connection protocol for '-C' argument (must be one of: 'tcp', 'http', 'https')");
				}
			} else if (arg.startsWith("-O:")) {
				try {
					jKConnTimeout = TimeUnit.SECONDS.toMillis(Long.parseLong(arg.substring(3)));
				} catch (NumberFormatException e) {
					printUsage("Missing or invalid <timeout_sec> for '-O' argument");
				}
			} else if (runType == SimulatorRunType.RUN_SIM || runType == SimulatorRunType.REPLAY_SIM) {
				if (arg.startsWith("-G:")) {
					jkFileName = arg.substring(3);
					if (StringUtils.isEmpty(jkFileName)) {
						printUsage("Missing <jk_file_name> for '-G' argument");
					}
					if (runType == SimulatorRunType.RUN_SIM) {
						jkProtocol = "file";
					}
				} else if (runType == SimulatorRunType.RUN_SIM) {
					if (arg.startsWith("-f:")) {
						simFileName = arg.substring(3);
						if (StringUtils.isEmpty(simFileName)) {
							printUsage("Missing <sim_def_file_name> for '-f' argument");
						}
					} else if (arg.startsWith("-i:")) {
						String iterations = arg.substring(3);
						try {
							numIterations = Long.parseLong(iterations);
						} catch (NumberFormatException e) {
							if (StringUtils.isEmpty(iterations)) {
								printUsage("Missing <iterations> for '-i' argument");
							} else {
								printUsage("Invalid <iterations> for '-i' argument (" + arg.substring(3) + ")");
							}
						}
						if (numIterations <= 0) {
							printUsage("<iterations> for '-i' argument must be > 0");
						}
					} else if (arg.startsWith("-p:")) {
						String percentStr = arg.substring(3);
						if (StringUtils.isEmpty(percentStr)) {
							printUsage("Missing <percentage> for '-p' argument");
						}
						valuePctChg = Integer.parseInt(percentStr);
						if (valuePctChg < 0 || valuePctChg > 100) {
							printUsage("Percentage for varying values ('-p' argument) must be in the range [0,100)");
						}
					} else if (arg.startsWith("-t:")) {
						String ttlStr = arg.substring(3);
						try {
							ttl = Long.parseLong(ttlStr);
							if (ttl > 0L) {
								ttl *= 3600;
							}
						} catch (NumberFormatException e) {
							if (StringUtils.isEmpty(ttlStr)) {
								printUsage("Missing <ttl_hours> for '-t' argument");
							} else {
								printUsage("Invalid <ttl_hours> for '-t' argument (" + arg.substring(3) + ")");
							}
						}
					} else if (arg.startsWith("-LM:")) {
						String rate = arg.substring(4);
						try {
							rateMPS = Long.parseLong(rate);
						} catch (NumberFormatException e) {
							if (StringUtils.isEmpty(rate)) {
								printUsage("Missing <msgs/sec> for '-LM' argument");
							} else {
								printUsage("Invalid <msgs/sec> for '-LM' argument (" + rate + ")");
							}
						}
					} else if (arg.startsWith("-LB:")) {
						String rate = arg.substring(4);
						try {
							rateBPS = Long.parseLong(rate);
						} catch (NumberFormatException e) {
							if (StringUtils.isEmpty(rate)) {
								printUsage("Missing <bytes/sec> for '-LB' argument");
							} else {
								printUsage("Invalid <bytes/sec> for '-LB' argument (" + rate + ")");
							}
						}
					} else if (arg.equals("-ut")) {
						uniqueTags = true;
					} else if (arg.equals("-uc")) {
						uniqueCorrs = true;
					} else if (arg.equals("-ui")) {
						uniqueIds = true;
					} else if (arg.equals("-u")) {
						uniqueTags = true;
						uniqueCorrs = true;
						uniqueIds = true;
					} else if (arg.equals("-g")) {
						generateValues = true;
					} else {
						invalidArg = true;
					}
				} else {
					invalidArg = true;
				}
			} else {
				invalidArg = true;
			}

			if (invalidArg) {
				if (arg.startsWith("-")) {
					printUsage("Invalid argument: " + arg);
				} else {
					printUsage("Invalid argument list");
				}
			}
		}

		if (runType == SimulatorRunType.RUN_SIM) {
			if (StringUtils.isEmpty(jkHost) && StringUtils.isEmpty(jkFileName)) {
				printUsage("Must specify one of '-T' or '-G'");
			}

			if (StringUtils.isEmpty(jkAccessToken) && !StringUtils.isEmpty(jkHost)) {
				printUsage("Must specify '-A'");
			}
		}

		if (runType == SimulatorRunType.REPLAY_SIM) {
			if (StringUtils.isEmpty(jkAccessToken)) {
				printUsage("Must specify '-A'");
			}

			if (StringUtils.isEmpty(jkHost)) {
				printUsage("Missing host or URL");
			}

			if (StringUtils.isEmpty(jkFileName)) {
				printUsage("Missing XML file name");
			}
		}
		printUsedArgs();
	}

	public static void main(String[] args) {
		boolean isTTY = (System.console() != null);
		boolean showProgress = false;
		int itTrcWidth = 0;
		long startTime = System.currentTimeMillis();

		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser theParser = parserFactory.newSAXParser();
			TNT4JSimulatorParserHandler xmlHandler = new TNT4JSimulatorParserHandler();

			processArgs(xmlHandler, args);

			TrackerConfig simConfig = DefaultConfigFactory.getInstance().getConfig(TNT4JSimulator.class.getName());
			logger = TrackingLogger.getInstance(simConfig.build());
			if (logger.isSet(OpLevel.TRACE)) {
				traceLevel = OpLevel.TRACE;
			} else if (logger.isSet(OpLevel.DEBUG)) {
				traceLevel = OpLevel.DEBUG;
			}

			if (runType == SimulatorRunType.RUN_SIM) {
				showProgress = (isTTY && numIterations > 1);

				if (StringUtils.isEmpty(simFileName)) {
					simFileName = "tnt4j-sim.xml";
					String fileName = readFromConsole("Simulation file [" + simFileName + "]: ");

					if (!StringUtils.isEmpty(fileName)) {
						simFileName = fileName;
					}
				}

				StringBuilder simDef = new StringBuilder();
				BufferedReader simLoader = new BufferedReader(new FileReader(simFileName));
				String line;
				while ((line = simLoader.readLine()) != null) {
					simDef.append(line).append("\n");
				}
				simLoader.close();

				info("jKool Activity Simulator Run starting: file=" + simFileName + ", iterations=" + numIterations
						+ ", ttl.sec=" + ttl);

				startTime = System.currentTimeMillis();
				if (showProgress) {
					System.out.print("Iteration: ");
				}
				for (iteration = 1; iteration <= numIterations; iteration++) {
					if (showProgress) {
						itTrcWidth = printProgress("Executing Iteration", iteration, itTrcWidth);
					}

					theParser.parse(new InputSource(new StringReader(simDef.toString())), xmlHandler);

					if (!Utils.isEmpty(jkFileName)) {
						PrintWriter gwFile = new PrintWriter(new FileOutputStream(jkFileName, true));
						gwFile.println("");
						gwFile.close();
					}
				}
				if (showProgress) {
					System.out.println("");
				}

				info("jKool Activity Simulator Run finished, elapsed time="
						+ DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - startTime));
				printMetrics(xmlHandler.getSinkStats(), "Total Sink Statistics");
			} else if (runType == SimulatorRunType.REPLAY_SIM) {
				showProgress = isTTY;
				info("jKool Activity Simulator Replay starting: file=" + jkFileName);
				connect();
				startTime = System.currentTimeMillis();
				String gwMsg;

				// Determine number of lines in file
				info("Analyzing file ...");
				BufferedReader gwFile = new BufferedReader(new java.io.FileReader(jkFileName));
				for (numIterations = 0; (gwMsg = gwFile.readLine()) != null;) {
					if (!StringUtils.isEmpty(gwMsg)) {
						numIterations++;
					}
				}
				gwFile.close();

				// Reopen the file and send each line (tracking msg) to gateway
				gwFile = new BufferedReader(new java.io.FileReader(jkFileName));
				if (showProgress) {
					System.out.print("Processing Line: ");
				}
				iteration = 0;
				while ((gwMsg = gwFile.readLine()) != null) {
					if (showProgress) {
						itTrcWidth = printProgress("Processing Line", iteration, itTrcWidth);
					}
					if (!StringUtils.isEmpty(gwMsg)) {
						gwConn.write(gwMsg);
						iteration++;
					}
				}
				if (showProgress) {
					System.out.println("");
				}
				long endTime = System.currentTimeMillis();

				info("jKool Activity Simulator Replay finished, tracking.msg.count=" + iteration + ", elasped.time="
						+ DurationFormatUtils.formatDurationHMS(endTime - startTime));
			}
		} catch (Exception e) {
			if (e instanceof SAXParseException) {
				SAXParseException spe = (SAXParseException) e;
				error("Error at line: " + spe.getLineNumber() + ", column: " + spe.getColumnNumber(), e);
			} else {
				error("Error running simulator", e);
				if (runType == SimulatorRunType.RUN_SIM) {
					info("jKool Activity Simulator Run failed, completed " + iteration + " out of " + numIterations
							+ " iterations");
				} else if (runType == SimulatorRunType.REPLAY_SIM) {
					info("jKool Activity Simulator Replay failed, processed " + iteration + " out of " + numIterations
							+ " records");
				}
			}
		} finally {
			try {
				Thread.sleep(2000L);
			} catch (Exception e) {
			}
			TNT4JSimulator.disconnect();
		}

		System.exit(0);
	}

	public static JKCloudConnection connect() throws IOException {
		if (StringUtils.isEmpty(jkHost)) {
			return null;
		}

		if (gwConn != null) {
			return gwConn;
		}

		String gwUrl = TNT4JSimulator.getConnectUrl();
		TNT4JSimulator.debug(new UsecTimestamp(), "Connecting to service=" + gwUrl + " with access token="
				+ jkAccessToken + " and connection timeout=" + jKConnTimeout + " ...");
		gwConn = new JKCloudConnection(gwUrl, jkAccessToken, jKConnTimeout, false, logger);
		gwConn.open();

		return gwConn;
	}

	public static void disconnect() {
		if (gwConn != null) {
			try {
				gwConn.close();
			} catch (Exception e) {
			}
			gwConn = null;
		}
	}

	private static int printProgress(String text, long iteration, int trcWidth) {
		int itPct = (int) ((double) iteration / numIterations * 100.0);
		int maxItWidth = (int) (Math.log10(numIterations) + 1);
		String bkSpStr = StringUtils.leftPad("", trcWidth, '\b');
		String trcMsg = String.format(bkSpStr + "%" + maxItWidth + "d (%02d%%)", iteration, itPct);
		trcWidth = trcMsg.length() - bkSpStr.length();
		System.out.print(trcMsg);
		return trcWidth;
	}

	private static <V extends Object> void printMetrics(Map<String, V> map, String label) {
		if (map == null) {
			return;
		}
		info(label + ":");
		for (String key : map.keySet()) {
			V value = map.get(key);
			String valueStr = (value instanceof Number ? String.format("%,15d", value)
					: String.format("%s", value.toString()));
			info(String.format("    %-50s %s", key + ":", valueStr));
		}
	}
}
