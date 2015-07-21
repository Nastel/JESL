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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import com.jkool.jesl.simulator.tnt4j.JKCloudConnection;
import com.nastel.jkool.tnt4j.TrackingLogger;
import com.nastel.jkool.tnt4j.config.DefaultConfigFactory;
import com.nastel.jkool.tnt4j.config.TrackerConfig;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.UsecTimestamp;

/**
 * JESL Activity Simulator which uses TNT4J to simulate application activities and transactions.
 * This will simulate activities by loading activity definitions from an XML file.
 *
 * @version $Revision: $
 */
public class TNT4JSimulator {

	public enum SimulatorRunType {RUN_SIM, REPLAY_SIM}

	private static SimulatorRunType	runType        = SimulatorRunType.RUN_SIM;
	private static String			jkProtocol     = "tcp";
	private static String			jkHost         = null;
	private static int				jkPort         = 6500;
	private static String			jkAccessToken  = null;
	private static String			simFileName    = null;
	private static boolean			uniqueTags     = false;
	private static Level			traceLevel     = Level.INFO;
	private static String			jkFileName     = null;
	private static int				valuePctChg    = 0;
	private static Random			ranGen         = new Random();
	private static long				numIterations  = 1;
	private static boolean			generateValues = false;

	private static JKCloudConnection	gwConn          = null;
	private static TrackingLogger		logger          = null;
	private static long					iteration       = 0L;

	public static void error(String msg, Throwable e) {
		logger.tnt(OpLevel.ERROR, null, null, msg, e);
	}

	public static void warn(String msg) {
		logger.tnt(OpLevel.WARNING, null, null, msg, (Object[])null);
	}

	public static void info(String msg) {
		logger.tnt(OpLevel.INFO, null, null, msg, (Object[])null);
	}

	public static boolean isDebugEnabled() {
		return (traceLevel == Level.DEBUG || traceLevel == Level.TRACE);
	}

	public static void debug(UsecTimestamp simTime, String msg) {
		if (isDebugEnabled()) {
			if (simTime == null)
				simTime = new UsecTimestamp();
			logger.tnt(OpLevel.DEBUG, null, null, "SimTime=" + simTime + ": " + msg);
		}
	}

	public static boolean isTraceEnabled() {
		return (traceLevel == Level.TRACE);
	}

	public static void trace(UsecTimestamp simTime, String msg) {
		if (isTraceEnabled()) {
			if (simTime == null)
				simTime = new UsecTimestamp();
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
		return jkProtocol.equalsIgnoreCase("file")?
				(jkProtocol.toLowerCase() + "://" + jkFileName):
				(jkProtocol.toLowerCase() + "://" + jkHost + ":" + jkPort);
	}

	public static String getAccessToken() {
		return jkAccessToken;
	}

	public static boolean useUniqueTags() {
		return uniqueTags;
	}

	public static boolean isGenerateValues() {
		return generateValues;
	}

	public static boolean isVaryingValuesEnabled() {
		return valuePctChg != 0;
	}

	public static double varyValue(double value) {
		if (value == 0 || valuePctChg == 0)
			return value;

		// nextInt returns value in range [0,n).  We want values in range [-n,n).
		double percentChg = (ranGen.nextInt(valuePctChg*2) - valuePctChg) / 100.0;

		// let's keep a lid on variations
		if (percentChg > 0.99)
			percentChg = 0.99;
		if (percentChg < -0.99)
			percentChg = -0.99;

		double newValue = value * (1.0+percentChg);
		if (newValue < 0.0 && value > 0.0)
			newValue = 0;

		return newValue;
	}

	public static void incrementValue(Map<String,Long> map, String item, long amount) {
		Long prev = map.put(item, amount);
		if (prev != null) {
			map.put(item, prev.longValue() + amount);
		}
	}

	public static long getIteration() {
		return iteration;
	}

	private static void printUsage(String error) {
		if (!StringUtils.isEmpty(error))
			System.out.println(error);

		System.out.println("\nValid arguments:\n");
		System.out.println("  to run simulation:      run -A:<access_token> [-T:<jk_host>] [-P:<jk_port>] [-C:tcp|http|https] [-f:<sim_def_file_name>]");
		System.out.println("                              [-p:<percentage>] [-G:<jk_file_name>] [-i:<iterations>] [-u] [-t] \n");
		System.out.println("  to replay simulation:   replay -A:<access_token> -T:<jk_host> [-P:<jk_port>] [-C:tcp|http|https] -G:<jk_file_name> [-t[v]]\n");
		System.out.println("  for usage information:  help\n");
		System.out.println("where:");
		System.out.println("    -A    -  jKoolCloud access token (required)");
		System.out.println("    -T    -  Host name or IP address of jKoolCloud service");
		System.out.println("             (if not specified, data is not sent to jKoolCloud service)");
		System.out.println("    -P    -  Port jKoolCloud service is listening on (default: 6500)");
		System.out.println("    -C    -  Connection type to use for jKoolCloud service (default: tcp)");
		System.out.println("    -f    -  Use <sim_def_file_name> as simulation configuration");
		System.out.println("    -p    -  Vary all numeric values by +/- <percentage>");
		System.out.println("    -G    -  Read/Write jKoolCloud service messages from/to <jk_file_name>");
		System.out.println("             (if writing, data is appended to an existing file)");
		System.out.println("    -i    -  Number of iterations to make on <sim_file_name>");
		System.out.println("    -u    -  Make message signatures, correlators, and labels unique between iterations");
		System.out.println("    -t[v] -  Enable tracing (include 'v' for verbose tracing)");
		System.out.println("\nFor 'run' mode, must specify at least of one: '-T', '-G'");

		System.exit(StringUtils.isEmpty(error) ? 0 : 1);

	}

	private static void processArgs(String[] args) {
		if (args.length == 0)
			printUsage("Missing simulation type");

		String runTypeArg = args[0];
		if (runTypeArg.equals("run"))
			runType = SimulatorRunType.RUN_SIM;
		else if (runTypeArg.equals("replay"))
			runType = SimulatorRunType.REPLAY_SIM;
		else if (runTypeArg.equals("help"))
			printUsage(null);
		else
			printUsage("Invalid simulation type");

		boolean invalidArg = false;
		for (int i = 1; i < args.length; i++) {
			String arg = args[i];

			if (arg == null)
				continue;

			// Process arguments common to all simulation types first
			if (arg.startsWith("-A:")) {
				jkAccessToken = arg.substring(3);
				if (StringUtils.isEmpty(jkAccessToken))
					printUsage("Missing <access_token> for '-A' argument");
			}
			else if (arg.startsWith("-T:")) {
				jkHost = arg.substring(3);
				if (StringUtils.isEmpty(jkHost))
					printUsage("Missing <jk_host> for '-T' argument");
			}
			else if (arg.startsWith("-P:")) {
				try {
					jkPort = Integer.parseInt(arg.substring(3));
				}
				catch (NumberFormatException e) {
					printUsage("Missing or invalid <jk_port> for '-P' argument");
				}
			}
			else if (arg.startsWith("-C:")) {
				jkProtocol = arg.substring(3).toLowerCase();
				if (!"tcp".equals(jkProtocol) && !"http".equals(jkProtocol) && !"https".equals(jkProtocol))
					printUsage("Invalid connection protocol for '-C' argument (must be one of: 'tcp', 'http', 'https')");
			}
			else if (arg.startsWith("-t")) {
				if (arg.length() > 2 && arg.charAt(2) != 'v')
					printUsage("Invalid qualifier for '-t' argument");
				traceLevel = (arg.length() > 2 ? Level.TRACE : Level.DEBUG);
			}
			else if (runType == SimulatorRunType.RUN_SIM || runType == SimulatorRunType.REPLAY_SIM) {
				if (arg.startsWith("-G:")) {
					jkFileName = arg.substring(3);
					if (StringUtils.isEmpty(jkFileName))
						printUsage("Missing <jk_file_name> for '-G' argument");
					jkProtocol = "file";
				}
				else if (runType == SimulatorRunType.RUN_SIM) {
					if (arg.startsWith("-f:")) {
						simFileName = arg.substring(3);
						if (StringUtils.isEmpty(simFileName))
							printUsage("Missing <sim_def_file_name> for '-f' argument");
					}
					else if (arg.startsWith("-i:")) {
						String iterations = arg.substring(3);
						try {
							numIterations = Long.parseLong(iterations);
						}
						catch (NumberFormatException e) {
							if (StringUtils.isEmpty(iterations))
								printUsage("Missing <iterations> for '-i' argument");
							else
								printUsage("Invalid <iterations> for '-i' argument (" + arg.substring(3) + ")");
						}
						if (numIterations <= 0)
							printUsage("<iterations> for '-i' argument must be > 0");
					}
					else if (arg.startsWith("-p:")) {
						String percentStr = arg.substring(3);
						if (StringUtils.isEmpty(percentStr))
							printUsage("Missing <percentage> for '-p' argument");
						valuePctChg = Integer.parseInt(percentStr);
						if (valuePctChg < 0 || valuePctChg > 100)
							printUsage("Percentage for varying values ('-p' argument) must be in the range [0,100)");
					}
					else if (arg.equals("-u")) {
						uniqueTags = true;
					}
					else if (arg.equals("-g")) {
						generateValues = true;
					}
					else {
						invalidArg = true;
					}
				}
				else {
					invalidArg = true;
				}
			}
			else {
				invalidArg = true;
			}

			if (invalidArg) {
				if (arg.startsWith("-"))
					printUsage("Invalid argument: " + arg);
				else
					printUsage("Invalid argument list");
			}
		}

		if (runType == SimulatorRunType.RUN_SIM) {
			if (StringUtils.isEmpty(jkAccessToken))
				printUsage("Must specify '-A'");

			if (StringUtils.isEmpty(jkHost) && StringUtils.isEmpty(jkFileName))
				printUsage("Must specify one of '-T' or '-G'");
		}

		if (runType == SimulatorRunType.REPLAY_SIM) {
			if (StringUtils.isEmpty(jkAccessToken))
				printUsage("Must specify '-A'");

			if (StringUtils.isEmpty(jkHost))
				printUsage("Missing host or URL");

			if (StringUtils.isEmpty(jkFileName))
				printUsage("Missing XML file name");
		}
	}

	public static void main(String[] args) {
		boolean isTTY = (System.console() != null);
		long startTime = System.currentTimeMillis();

		try {
			SAXParserFactory            parserFactory = SAXParserFactory.newInstance();
			SAXParser                   theParser     = parserFactory.newSAXParser();
			TNT4JSimulatorParserHandler xmlHandler    = new TNT4JSimulatorParserHandler();

			processArgs(args);

			TrackerConfig simConfig = DefaultConfigFactory.getInstance().getConfig(TNT4JSimulator.class.getName());
			logger = TrackingLogger.getInstance(simConfig.build());
			if (isTraceEnabled())
				logger.set(OpLevel.TRACE, ".*");
			else if (isDebugEnabled())
				logger.set(OpLevel.DEBUG, ".*");

			Object logSink = simConfig.getEventSink().getSinkHandle();
			if (logSink instanceof Logger) {
				Logger log4jSink = (Logger) logSink;
				log4jSink.setLevel(traceLevel);
			}

			if (runType == SimulatorRunType.RUN_SIM) {
				if (StringUtils.isEmpty(simFileName)) {
					simFileName = "tnt4j-sim.xml";
					System.out.print("Simulation definition file name [" + simFileName + "]: ");
					Scanner input = new Scanner(System.in);
					String fileName = input.nextLine();

					if (!StringUtils.isEmpty(fileName))
						simFileName = fileName;
					input.close();
				}

				StringBuffer simDef = new StringBuffer();
				BufferedReader simLoader = new BufferedReader(new FileReader(simFileName));
				String line;
				while ((line = simLoader.readLine()) != null)
					simDef.append(line).append("\n");
				simLoader.close();

				info("jKool Activity Simulator Run starting: file=" + simFileName + ", iterations=" + numIterations);
				startTime = System.currentTimeMillis();

				if (isTTY && numIterations > 1)
					System.out.print("Iteration: ");
				int itTrcWidth = 0;
				for (iteration = 1; iteration <= numIterations; iteration++) {
					itTrcWidth = printProgress("Executing Iteration", iteration, itTrcWidth);

					theParser.parse(new InputSource(new StringReader(simDef.toString())), xmlHandler);

					if (jkFileName != null) {
						PrintWriter gwFile = new PrintWriter(new FileOutputStream(jkFileName, true));
						gwFile.println("");
						gwFile.close();
					}
				}
				if (numIterations > 1)
					System.out.println("");

				info("jKool Activity Simulator Run finished, elapsed time = " +
					 DurationFormatUtils.formatDurationHMS(System.currentTimeMillis()-startTime));
				printMetrics(xmlHandler.getSinkStats(), "Total Sink Statistics");
			}
			else if (runType == SimulatorRunType.REPLAY_SIM) {
				info("jKool Activity Simulator Replay starting: file=" + jkFileName + ", iterations=" + numIterations);

				connect();

				startTime = System.currentTimeMillis();

				// Determine number of lines in file
				BufferedReader gwFile = new BufferedReader(new java.io.FileReader(jkFileName));
				for (numIterations = 0; gwFile.readLine() != null; numIterations++);
				gwFile.close();

				// Reopen the file and
				gwFile = new BufferedReader(new java.io.FileReader(jkFileName));
				if (isTTY && numIterations > 1)
					System.out.print("Processing Line: ");
				int itTrcWidth = 0;
				String gwMsg;
				iteration = 0;
				while ((gwMsg=gwFile.readLine()) != null) {
					iteration++;
					if (isTTY)
						itTrcWidth = printProgress("Processing Line", iteration, itTrcWidth);
					gwConn.write(gwMsg);
				}
				if (isTTY && numIterations > 1)
					System.out.println("");
				long endTime = System.currentTimeMillis();

				info("jKool Activity Simulator Replay finished, elasped.time = " +
					 DurationFormatUtils.formatDurationHMS(endTime-startTime));
			}
		}
		catch (Exception e) {
			if (e instanceof SAXParseException) {
				SAXParseException spe = (SAXParseException)e;
				error("Error at line: " + spe.getLineNumber() + ", column: " + spe.getColumnNumber(), e);
			}
			else {
				error("Error running simulator", e);
			}
		}
		finally {
			try {Thread.sleep(1000L);} catch (Exception e) {}
			TNT4JSimulator.disconnect();
		}

		System.exit(0);
	}

	public static JKCloudConnection connect() throws IOException {
		if (StringUtils.isEmpty(jkHost))
			return null;

		if (gwConn != null)
			return gwConn;

		String gwUrl = TNT4JSimulator.getConnectUrl();
		TNT4JSimulator.debug(new UsecTimestamp(), "Connecting to service=" + gwUrl + " with access token=" + jkAccessToken + " ...");
		gwConn = new JKCloudConnection(gwUrl, jkAccessToken, logger);
		gwConn.open();

		return gwConn;
	}

	public static void disconnect() {
		if (gwConn != null) {
			try {gwConn.close();} catch (Exception e) {}
			gwConn = null;
		}
	}

	private static int printProgress(String text, long iteration, int trcWidth) {
		if ( numIterations == 1)
			return 0;

		int itPct = (int)((double)iteration/numIterations*100.0);
		int maxItWidth = (int)(Math.log10(numIterations)+1);
		String bkSpStr = StringUtils.leftPad("", trcWidth, '\b');
		String trcMsg = String.format(bkSpStr + "%" + maxItWidth + "d (%02d%%)", iteration, itPct);
		trcWidth = trcMsg.length() - bkSpStr.length();
		System.out.print(trcMsg);
		return trcWidth;
	}

	private static <V extends Object> void printMetrics(Map<String, V> map, String label) {
		if (map == null)
			return;
		info(label + ":");
		for (String key : map.keySet()) {
			V value = map.get(key);
			String valueStr = (value instanceof Number ? String.format("%,15d", value) : String.format("%s", value.toString()));
			info(String.format("    %-50s %s", key+":", valueStr));
		}
	}
}
