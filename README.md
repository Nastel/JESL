#JESL - jKool Event Streaming Library
JESL allows application developers stream time-series data to jKool Cloud.
To stream data to jKool Cloud your application must:
	
	1. Use TNT4J, or MQTT/slf4j/log4j/Logback in your application
	   to log events, activities, metrics (see https://github.com/Nastel/tnt4j-streams)
	
	2. Obtain your jKool account and API access token 
	   at https://www.jkoolcloud.com. API access token is required to stream
	   data to your jKool Cloud repository.
	
	3. Use JESL Event Sink implementation (contained in this project) 
	   within your tnt4j configuration. (Requires API access token)
	
	4. Configure your application for streaming to jKool Cloud 
	   using JESL Event Sink (requires API access token).
	   See (`com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory`)
	
JESL package includes the following components:

	1. TNT4J streaming library with SLF4j support (https://github.com/Nastel/TNT4J)
	   	
	2. TNT4J Syslog for streaming syslog to jkoolcloud.com. (https://github.com/Nastel/tnt4j-syslogd)
	
	3. JESL Simulator -- stream simulated events, activities and metrics
	   to jKool Cloud. Simulations are defined in XML files.
	   (see `sims/tnt4j-sim-template.xml` and `sims/order-process.xml`) 
	
	4. JESL Event Sink -- TNT4J Event Sink implementation to stream events to jKoolCloud.

## JESL Simulator
The JESL Simulator provides the ability to simulate tracking activities
and events. The activities, events, and their components are defined using
a XML format.  There are three major parts to a simulation definition:

	1. Sources
	   Defines the sources involved in simulated activities and events

	2. Messages
	   Defines the messages that will be exchanged during activities

	3. Activities and Events
	   Defines the actual activities and the sub-activities and events in them

The included file `sims/tnt4j-sim-template.xml` contains a simulation definition template,
along with detailed descriptions of each element, showing the XML element
hierarchy. The file order-process.xml contains a sample set of activities and
their events, along with some sample snapshots.

To define a simulation, copy one of the supplied XML simulation definition
files to use as a template and create the necessary activity elements.

The simulator can be run in one of two modes:

	1. Simulation (simulation type: `run`)
	   Runs a user specified simulation file (e.g. `sims/order-process.xml`) 
	   and sends the tracking data to the JESL Event Sink and/or writes 
	   the tracking data to the specified file.  In this mode, the simulator 
	   can be configured to run the simulation file a specified number of iterations,
	   optionally generating unique correlators and tags for each iteration of
	   the simulation file (appends a unique value to end of defined correlator 
	   and tags in simulation definition file).
	
	2. Replay (simulation type: `replay`)
	   Reads previously-saved tracking data from the specified file
	   and sends it to the JESL Event Sink

The simplest way to run the simulator is to execute the file `jksim.bat`
(or `jksim.sh`) as follows:

	`jksim -A:api-access-token -f:sim-file -i:iterations`
	Example: `jksim -A:MY-TOKEN -f:../sims/order-process.xml -i:5`
		
`api-access-token` is your API access token obtained during registration with jKool.
`sim-file` simulation file that defines all interactions, events, metrics.
Sample simulation files are located under `<jesl>/sims/` folder (e.g. `<jesl>/sims/order-process.xml`). 
`iterations` is the number of iterations for a given simulation (1 default).

<b>NOTE:</b> You will may to alter `jksim` shell script to specify custom simulator 
parameters such as simulation type as well as other advanced options.

The simulator also contains options for allowing the data values used for some of
the tracking component attributes to be altered to provide unique values for these
attributes for each tracking activity so that each activity definition in the input
file serves as a template for the activities to generate, allowing each to be a unique
instance of an activity with the defined components.  An example of such an attribute
is the Correlator.  If a Correlator is defined and the correlator value is not unique
across each activity then all activities will get stitched together into one large
activity.

Some of the available options are:

	-A		This option specifies the access token to use to validate
			connection to JESL Event Sink.  This option is required
			when using `-T` option

	-p		This option will cause simulator to scale each time attribute
			up or down by the specified percentage.  This prevents each
			activity from looking exactly the same, showing some variation
			in the event durations, as well as the times between events
	
	-u		Makes correlators and tags unique between iterations on the
			input file by appending a time stamp to each one, so that each
			iteration over the file	will generate independent activities
			and events
	
To see the full set of supported options, run:

	`jksim.bat help`

Streaming Syslog to jKool Cloud 
===============================
JESL includes Syslog Daemon implementation. Please follow these steps to stream syslog to `jkoolcloud.com`:

* Obtain jkool cloud account. Edit `config/tnt4j.properties`, 
	* Locate `com.jkoolcloud.jesl.net.syslogd` stanza and provide your API access token.
* Run JESL syslogd `<jesl-home>/bin/jksysd > jksysd.json`. 
	* By default JESL `jksysd` binds to TCP port `5140` and writes JSON formatted syslog messages.
	* JSON output can be played back using `<jesl-home>/bin/jksys` utility.
* Configure `syslog/rsyslog` to forward to JESL syslog daemon over TCP (`hostname` is where JESL `jksysd` is running)
	* RFC 3164 (e.g. `*.* @@hostname:5140`)
	* RFC 5424 (e.g. `*.* @@hostname:5140;RSYSLOG_SyslogProtocol23Format`)
* Sending syslog messages from command line (`<jesl-home>/bin/jksys`):
```
$ jksys -h localhost -p 5140 -l error -f user tcp "appl-name[883]: my syslog mesasge about appl-name pid=883"
```
* Sending PCI messages from command line (`<jesl-home>/bin/jksys`):
```
$ jksys -h localhost -p 5140 -l error -f user tcp "#pci(userId=john,eventType=audit,status=success,origination=CreditCards,affectedResource=Payment)"
```
* Playback syslog JSON messages from command line (`<jesl-home>/bin/jksys`):
```
$ jksys -h localhost -p 5140 -f jksysd.json tcp
```
where `jksysd.json` is JSON output of JESL syslog daemon.
	
That should do it.

<b>NOTE:</b> JESL currently supports (RFC 3164) and the Structured Syslog protocol (RFC 5424).

Streaming Log4j to jKool Cloud 
===============================
Requires TNT4J Appender for Log4J 1.2 (https://github.com/Nastel/tnt4j-log4j12)	 
	
Log4J can be configured to stream events and metrics to jKool Cloud by using 
JESL log4j appender (`com.jkoolcloud.tnt4j.logger.log4j.TNT4JAppender`) as follows:

#### Add JESL log4j appender to your log4j configuration
```
### Default JESL Appender configuration
log4j.appender.jkoolcloud=com.jkoolcloud.tnt4j.logger.log4j.TNT4JAppender
log4j.appender.jkoolcloud.SourceName=com.jkoolcloud.jesl.stream
log4j.appender.jkoolcloud.SourceType=APPL
log4j.appender.jkoolcloud.MetricsOnException=true
log4j.appender.jkoolcloud.MetricsFrequency=60
log4j.appender.jkoolcloud.layout=org.apache.log4j.PatternLayout
log4j.appender.jkoolcloud.layout.ConversionPattern=%d{ABSOLUTE} %-5p [%c{1}] %m%n

## JESL Configuration
log4j.logger.com.jkoolcloud.jesl.stream=off
log4j.logger.com.jkoolcloud=info
log4j.logger.com.jkoolcloud.jesl=info
```
Define categories that you want mapped to `jkoolcloud` appender. Example:
```
log4j.logger.com.myco.mypackage=info,jkoolcloud
```

#### Add the following arguments to your java start-up
```
-Dtnt4j.config=<jesl.home>/log4j/tnt4j.properties -Dtnt4j.token.repository=<jesl.home>/log4j/tnt4j-tokens.properties 
```
To enable automatic application dump add the following arguments:
```
-Dtnt4j.dump.on.vm.shutdown=true -Dtnt4j.dump.on.exception=true -Dtnt4j.dump.provider.default=true 
```
Optionally you can add the following parameters to define default data center name and geo location:
```
-Dtnt4j.source.DATACENTER=YourDataCenterName -Dtnt4j.source.GEOADDR="Melville, NY" 
```
Make sure `<jesl.home>/jesl-<version>.jar` and all dependent jar files in `<jesl.home>/lib` are in your class path.
Also include tnt4-log4j12 appender library `tnt4j-log4j12-<version>.jar`.

#### Edit `<jesl.home>/log4j/tnt4j.properties` and replace `YOUR-ACCESS-TOKEN` with your jKool API access token.
This allows streaming data to be associated with your private repository.

<b>NOTE</b>: Make sure your firewall allows outgoing `https` connections to https://data.jkoolcloud.com

#### Restart your application
log4j messages which map to JESL `jkoolcloud` appender will stream to jKool Cloud @ https://data.jkoolcloud.com

#### Login to "My Dashboard" @ https://www.jkoolcloud.com/

<b>NOTE</b>: Visit https://github.com/Nastel/TNT4J#log4j-integration for more information on `TNT4JAppender`.
Optionally you may annotate your log4j messages to provide better context, timing as well as report
user defined metrics. Example: 

```java
logger.info("Starting a tnt4j activity #beg=Test, #app=" + Log4JTest.class.getName());
logger.warn("First log message #app=" + Log4JTest.class.getName() + ", #msg='1 Test warning message'");
logger.error("Second log message #app=" + Log4JTest.class.getName() + ", #msg='2 Test error message'", new Exception("test exception"));
logger.info("Ending a tnt4j activity #end=Test, #app=" + Log4JTest.class.getName() + " #%i/order-no=" + orderNo);
```

Streaming TNT4J to jKool Cloud 
==============================
Applications that use TNT4J can be configured to stream events and metrics to jKool Cloud
by configuring application source to use JESL Event Sink (`com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory`)
Configure your TNT4J source as follows (using `tnt4j.properties` file):
```
{
	....
	; event sink configuration: destination and data format
	event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.BufferedEventSinkFactory
	event.sink.factory.PooledLoggerFactory: com.jkoolcloud.tnt4j.sink.impl.PooledLoggerFactoryImpl
	
	event.sink.factory.EventSinkFactory: com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory
	event.sink.factory.EventSinkFactory.Url: https://data.jkoolcloud.com
	event.sink.factory.EventSinkFactory.Token: YOUR-ACCESS-TOKEN
	event.formatter: com.jkoolcloud.tnt4j.format.JSONFormatter
	....
}
```
Below is an example of a sample TNT4J source (com.myco.myappl) configuration with 
JESL Event Sink (`com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory`):
```
{
	source: com.myco.myappl
	source.factory: com.jkoolcloud.tnt4j.source.SourceFactoryImpl
	source.factory.GEOADDR: New York
	source.factory.DATACENTER: HQDC
	source.factory.RootFQN: SERVER=?#DATACENTER=?#GEOADDR=?
	source.factory.RootSSN: tnt4j-myapp
	
	tracker.factory: com.jkoolcloud.tnt4j.tracker.DefaultTrackerFactory
	dump.sink.factory: com.jkoolcloud.tnt4j.dump.DefaultDumpSinkFactory

	; event sink configuration: destination and data format
	event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.BufferedEventSinkFactory
	event.sink.factory.PooledLoggerFactory: com.jkoolcloud.tnt4j.sink.impl.PooledLoggerFactoryImpl
	
	event.sink.factory.EventSinkFactory: com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory
	event.sink.factory.EventSinkFactory.Url: https://data.jkoolcloud.com
	event.sink.factory.EventSinkFactory.Token: YOUR-ACCESS-TOKEN
	event.formatter: com.jkoolcloud.tnt4j.format.JSONFormatter

	; Configure default sink filter based on level and time (elapsed/wait)
	event.sink.factory.Filter: com.jkoolcloud.tnt4j.filters.EventLevelTimeFilter
	event.sink.factory.Filter.Level: TRACE
	; Uncomment lines below to filter out events based on elapsed time and wait time
	; Timed event/activities greater or equal to given values will be logged
	;event.sink.factory.Filter.ElapsedUsec: 100
	;event.sink.factory.Filter.WaitUsec: 100
	
	tracking.selector: com.jkoolcloud.tnt4j.selector.DefaultTrackingSelector
	tracking.selector.Repository: com.jkoolcloud.tnt4j.repository.FileTokenRepository
}
```
<b>NOTE:</b> You will need to provide your actual API access token in (`event.sink.factory.EventSinkFactory.Token`).

# Sample jKQL Queries
Sample queries you can run against your data using jkool dashboard.
jKQL queries follow this convention:

`<verb> <expression> show as <widget>`

Example:
```sql
Get events where severity > INFO show as linechart
```

## Sample Get queries
```sql
Get relatives show as topology
Get relatives show as geomap
Get events where severity > INFO show as linechart
Get worst 10 events for last day show as linechart
Get number of events where severity > INFO group by servername, applname, severity show as scorecard
Get number of events where exception is not null group by applname ORDER BY applname show as piechart
Get number of events group by servername ORDER BY servername show as piechart
Get number of events where severity in (Error, Warning) for latest day group by starttime bucketed by hour, eventname show as stackchart
Get number of events group by starttime bucketed by minute show as anomalychart
Get number of activities group by starttime bucketed by minute show as anomalychart
Get number of events for latest 4 hours group by location show as barchart
Get number of events fields avg elapsedtime, max elapsedtime, min elapsedtime for latest 4 hours where severity in (Critical, Warning, Error) group by starttime bucketed by hour show as linechart
Get number of events group by eventname, severity, starttime bucketed by minute order by severity show as stackchart
Get number of activities group by Properties('browser') show as piechart
```

## Sample Compare queries
```sql
Compare only diffs latest 3 events
Compare only diffs longest 5 events show as table
```

## Sample Snapshot queries
```sql
Get number of snapshots where Category In (Log4J, Java, GarbageCollector) group by severity, category show as piechart
Get snapshots Memory fields snapshottime, map(FreeBytes) for latest day where map(FreeBytes) < 1500000000 show as colchart
Get snapshots Memory avg map(FreeBytes), max map(FreeBytes), max map(FreeBytes) show as colchart
Get snapshots where properties(TotalCpuUserUsec) > 4000 order by activityname
Get snapshot ShopingCart avg map(ShippingCost), sum map(ShippingCost) group by location show as scorecard
```

## Sample Real-time queries
```sql
Subscribe to events show as linechart
Subscribe to number of events show as linechart
Subscribe to number of activities show as linechart
Subscribe to events where message contains 'failure' show as linechart
```

# Project Dependencies
JESL requires the following (which will download automatically if using Maven):
* JDK 1.6+
* TNT4J (https://github.com/Nastel/TNT4J)
* Apache HTTP Client 4.2.5 (http://hc.apache.org/httpcomponents-client-ga/)
* Apache HTTP Core 4.3.4 (http://hc.apache.org/httpcomponents-client-ga/)
* Joda Time (http://www.joda.org/joda-time/)
* GSON (https://github.com/google/gson)

# Related Projects
* TNT4J (https://github.com/Nastel/TNT4J)
* TNT4J-Log4J12 Appender (https://github.com/Nastel/tnt4j-log4j12)
* TNT4J-Logback Appender (https://github.com/Nastel/tnt4j-logback)
* TNT4J-Servlet-Filter (http://nastel.github.io/tnt4j-servlet-filter/)
* TNT4J-Stream-JMX (http://nastel.github.io/tnt4j-stream-jmx/)
* TNT4J-Stream-GC (http://nastel.github.io/tnt4j-stream-gc/)
* TNT4J-Syslogd (https://github.com/Nastel/tnt4j-syslogd)

# Available Integrations
* jkoolcloud.com (https://www.jkoolcloud.com)
