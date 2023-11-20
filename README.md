# JESL - jKool Event Streaming Library

-----------------------

**NOTE:** `jesl` version `1.x` migrated to Java 11 and bumped SLF4J from `1.7.x` to `2.x`.

Latest Java 8 compliant `tnt4j` version is `0.12.x`.

-----------------------

JESL allows application developers stream time-series data to [jKoolCloud](https://www.jkoolcloud.com). To stream data to jKoolCloud your 
application must:

1. Use TNT4J, or MQTT/slf4j/log4j/Logback in your application to log events, activities, metrics 
   (see [TNT4J-Streams](https://github.com/Nastel/tnt4j-streams/))

1. Obtain your jKool account and API access token at https://www.jkoolcloud.com. API access token is required to stream data to your 
   [jKoolCloud](https://jkool.jkoolcloud.com) repository.

1. Use JESL Event Sink implementation (contained in this project) within your TNT4J configuration. (Requires API access token)

   Use Maven dependency:
   ```xml
        <dependency>
            <groupId>com.jkoolcloud</groupId>
            <artifactId>jesl</artifactId>
            <version>1.1.1</version>
        </dependency>
   ```

1. Configure your application for streaming to [jKoolCloud](https://jkool.jkoolcloud.com) using JESL Event Sink (requires API access token). 
   See (`com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory`)

**NOTE:** To initiate `HTTPS` connection, JESL uses `TLSv1.2+` protocol:
 * `TLSv1.2` - for all `1.8+` Java versions.
 * `TLSv1.3` - for Java version `1.8` build `261+` (Oracle)/`262+` (OpenJDK) and later versions (`11`, `15`, `17`...).

JESL package includes the following components:

1. TNT4J streaming library with SLF4j support (https://github.com/Nastel/TNT4J)

1. JESL Simulator -- stream simulated events, activities and metrics to [jKoolCloud](https://jkool.jkoolcloud.com). Simulations are defined 
   in XML files. (See [`tnt4j-sim-template.xml`](sims/tnt4j-sim-template.xml) and [`order-process.xml`](sims/order-process.xml))

1. JESL Event Sink -- TNT4J Event Sink implementation to stream events to [jKoolCloud](https://jkool.jkoolcloud.com).

## JESL Simulator

The JESL Simulator provides the ability to simulate tracking activities and events. The activities, events, and their components are defined 
using an XML format. There are three major parts to a simulation definition:

1. `Sources` - defines the sources involved in simulated activities and events

1. `Messages` - defines the messages that will be exchanged during activities

1. `Activities` and `Events` - defines the actual activities and the sub-activities and events in them

The included file [`tnt4j-sim-template.xml`](sims/tnt4j-sim-template.xml) contains a simulation definition template, along with detailed 
descriptions of each element, showing the XML element hierarchy. The file order-process.xml contains a sample set of activities and their 
events, along with some sample snapshots.

To define a simulation, copy one of the supplied XML simulation definition files to use as a template and create the necessary activity 
elements.

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

The simplest way to run the simulator is to execute the file `jksim.bat` (or `jksim.sh`) as follows:

	`jksim -A:api-access-token -f:sim-file -i:iterations`
	Example: `jksim -A:MY-TOKEN -f:../sims/order-process.xml -i:5`

`api-access-token` is your API access token obtained during registration with jKool.
`sim-file` simulation file that defines all interactions, events, metrics.
Sample simulation files are located under `<jesl>/sims/` folder (e.g. `<jesl>/sims/order-process.xml`). 
`iterations` is the number of iterations for a given simulation (1 default).

**NOTE:** You may to alter `jksim` shell script to specify custom simulator parameters such as simulation type as well as other advanced
options.

The simulator also contains options for allowing the data values used for some tracking component attributes to be altered to provide 
unique values for these attributes for each tracking activity so that each activity definition in the input file serves as a template for 
the activities to generate, allowing each to be a unique instance of an activity with the defined components. An example of such an 
attribute is the Correlator.  If a Correlator is defined and the correlator value is not unique across each activity then all activities 
will get stitched together into one large activity.

Some available options are:
```
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
```
To see the full set of supported options, run: `jksim.bat help`

# Streaming Log4j to jKoolCloud

Requires [TNT4J Appender for Log4J](https://github.com/Nastel/tnt4j-log4j)

Log4J can be configured to stream events and metrics to jKoolCloud by using JESL log4j appender 
`com.jkoolcloud.tnt4j.logger.log4j.TNT4JAppender` as follows:

1. Add JESL log4j appender to your log4j configuration
   ```xml
   <!-- ### Default JESL Appender configuration ### -->
   <Tnt4j name="jkoolcloud" sourceName="com.jkoolcloud.jesl.stream" sourceType="APPL" metricsOnException="true" metricsFrequency="60">
       <PatternLayout>
           <Pattern>%d{ABSOLUTE} %-5p [%c{1}] %m%n</Pattern>
       </PatternLayout>
   </Tnt4j>

   <!-- ### jesl stream API logger -->
   <AsyncLogger name="com.jkoolcloud.jesl.stream" level="OFF"/>
   <!-- ### jkoolcloud API logger -->
   <AsyncLogger name="com.jkoolcloud" level="INFO"/>
   <!-- ### jesl API logger -->
   <AsyncLogger name="com.jkoolcloud.jesl" level="INFO"/>
   ```
   Define categories that you want mapped to `jkoolcloud` appender. Example:
   ```xml
   <Logger name="com.myco.mypackage" level="INFO">
       <AppenderRef ref="jkoolcloud"/>
   </Logger>
   ```

1. Add the following arguments to your java start-up
   ```
   -Dtnt4j.config=<jesl.home>/log4j/tnt4j.properties -Dtnt4j.token.repository=<jesl.home>/log4j/tnt4j-tokens.properties 
   ```
   To enable automatic application dump add the following arguments:
   ```
   -Dtnt4j.dump.on.vm.shutdown=true -Dtnt4j.dump.on.exception=true -Dtnt4j.dump.provider.default=true 
   ```
   Optionally you can add the following parameters to define default data center name and geolocation:
   ```
   -Dtnt4j.source.DATACENTER=YourDataCenterName -Dtnt4j.source.GEOADDR="Melville, NY" 
   ```
   Make sure `<jesl.home>/jesl-<version>.jar` and all dependent jar files in `<jesl.home>/lib` are in your class path. Also include 
   `tnt4j-log4j` appender library `tnt4j-log4j-<version>.jar`.

1. Edit `<jesl.home>/log4j/tnt4j.properties` and replace `YOUR-ACCESS-TOKEN` with your jKool API access token.

   This allows streaming data to be associated with your private repository.

   **NOTE**: Make sure your firewall allows outgoing `https` connections to [jKoolCloud](https://data.jkoolcloud.com).

1. Restart your application

   log4j messages which map to JESL `jkoolcloud` appender will stream to [jKoolCloud](https://data.jkoolcloud.com)

1. Login to "My Dashboard" @ [jKoolCloud](https://jkool.jkoolcloud.com/jKool/login.jsp)

**NOTE**: See [TNT4J documentation](https://github.com/Nastel/TNT4J#slf4j-event-sink-integration) for more information on `TNT4JAppender`.
Optionally you may annotate your log4j messages to provide better context, timing as well as report user defined metrics. Example:
```java
logger.info("Starting a tnt4j activity #beg=Test, #app=" + Log4JTest.class.getName());
logger.warn("First log message #app=" + Log4JTest.class.getName() + ", #msg='1 Test warning message'");
logger.error("Second log message #app=" + Log4JTest.class.getName() + ", #msg='2 Test error message'", new Exception("test exception"));
logger.info("Ending a tnt4j activity #end=Test, #app=" + Log4JTest.class.getName() + " #%i/order-no=" + orderNo);
```

# Streaming TNT4J to jKoolCloud

Applications that use TNT4J can be configured to stream events and metrics to jKoolCloud by configuring application source to use JESL Event 
Sink (`com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory`).

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
Below is an example of a sample TNT4J source (com.myco.myappl) configuration with JESL Event Sink 
(`com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory`):
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
**NOTE:** You will need to provide your actual API access token in (`event.sink.factory.EventSinkFactory.Token`).

**NOTE:** When streaming to jKoolCloud it may happen data containing special numeric (`double`/`float`) values: `Infinity`, `-Infinity`, 
`NaN`. While it is legal to have them in JSON, default JSON parser configuration on jKoolCloud may not allow them and skip complete 
activity entities to store in your repository. This `JSONFormatter` has configuration property `SpecNumbersHandling` to handle those 
special values on`TNT4J/JESL` side:
```properties
    event.formatter: com.jkoolcloud.tnt4j.format.JSONFormatter
    ; Configures special numeric values handling. Can be one of: SUPPRESS, ENQUOTE, MAINTAIN. Default value: SUPPRESS
    event.formatter.SpecNumbersHandling: MAINTAIN
```
`SpecNumbersHandling` property values:
* `SUPPRESS` - suppress properties having special numeric value. Produced JSON will not contain these activity entity properties.
* `ENQUOTE` - enquote special numeric value. Produced JSON will have en-quoted values e.g.`"Infinity"`, `"NaN"`. 
* `MAINTAIN` - maintain value as is. Produced JSON will have literal number values e.g. `Infnity`, `NaN`.

## Optional configuration

* `ConnTimeout` - defines connection timeout in milliseconds. Default - `10sec.`
* `IdleTimeout` - defines connection idle timeout in milliseconds. Default - `4min.`
* `AckSends` - indicates connection to wait for acknowledgments for sent packages. Default - `false`. 
* `DisableSSLVerification` - indicates connection to disable SSL validation (may be useful if HTTP endpoint has certificate expired). 
  Default - `false`.

Sample:
```properties
event.sink.factory.EventSinkFactory: com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory
event.sink.factory.EventSinkFactory.Url: https://data.jkoolcloud.com
event.sink.factory.EventSinkFactory.Token: YOUR-ACCESS-TOKEN
event.sink.factory.EventSinkFactory.ConnTimeout: 5000
event.sink.factory.EventSinkFactory.IdleTimeout: 300000
event.sink.factory.EventSinkFactory.AckSends: true
event.sink.factory.EventSinkFactory.DisableSSLVerification: true
```

## Proxy configuration

Proxy can be used do deliver data over JESL Event Sink (`com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory`) for both `HTTP(S)` and 
`TCP(S)` layers.

* To use `HTTP` proxy, set such configuration properties:
    * `ProxyScheme` - defines proxy scheme to be used, one of: `http` or `https`. Default value - `http`. Optional.
    * `ProxyHost` - defines proxy host name or IP address.
    * `ProxyPort` - defines proxy port number.
    * `ProxyUser` - defines proxy login user. Optional.
    * `ProxyPass` - defines proxy user password. Optional.

    Sample:
    ```properties
    event.sink.factory.EventSinkFactory: com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory
    event.sink.factory.EventSinkFactory.Url: https://data.jkoolcloud.com
    event.sink.factory.EventSinkFactory.Token: YOUR-ACCESS-TOKEN
    event.sink.factory.EventSinkFactory.ProxyScheme: http
    event.sink.factory.EventSinkFactory.ProxyHost: proxy.host.com
    event.sink.factory.EventSinkFactory.ProxyPort: 8060
    event.sink.factory.EventSinkFactory.ProxyUser: proxy-user
    event.sink.factory.EventSinkFactory.ProxyPass: proxy-pass
    ```

* To use `SOCKSv5` proxy, set such configuration properties:
    * `ProxyHost` - defines proxy host name or IP address.
    * `ProxyPort` - defines proxy port number.
    * `ProxyUser` - defines proxy login user. Optional.
    * `ProxyPass` - defines proxy user password. Optional.

    Sample:
    ```properties
    event.sink.factory.EventSinkFactory: com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory
    event.sink.factory.EventSinkFactory.Url: tcp://172.16.6.25:6004
    event.sink.factory.EventSinkFactory.ProxyHost: proxy.host.com
    event.sink.factory.EventSinkFactory.ProxyPort: 8060
    event.sink.factory.EventSinkFactory.ProxyUser: proxy-user
    event.sink.factory.EventSinkFactory.ProxyPass: proxy-pass
    ```
  Proxy authentication credentials can be also passed through JVM system properties `java.net.socks.username` and `java.net.socks.password`, 
  e.g.:
  ```cmd
  -Djava.net.socks.username=proxy-user -Djava.net.socks.password=proxy-pass
  ```

  Also see [Java SE documentation](https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html#proxies) for additional 
  proxy configuration details using JVM system properties.

# Sample jKQL Queries

Sample queries you can run against your data using jKool dashboard.

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

# Building

* To build the project, run Maven goals `clean package`
* To build the project and install to local repo, run Maven goals `clean install`
* To make distributable release assemblies use one of profiles: `pack-bin` or `pack-all`:
    * containing only binary (including `test` package) distribution: run `mvn -P pack-bin`
    * containing binary (including `test` package), `source` and `javadoc` distribution: run `mvn -P pack-all`
* To make Maven required `source` and `javadoc` packages, use profile `pack-maven`
* To make Maven central compliant release having `source`, `javadoc` and all signed packages, use `maven-release` profile

Release assemblies are built to `/build` directory.

# Project Dependencies

JESL requires the following (which will download automatically if using Maven):
* JDK 1.8+
* [TNT4J](https://github.com/Nastel/TNT4J/)
* [Apache HTTP Client](http://hc.apache.org/httpcomponents-client-ga/)
* [SLF4J-Simple](http://www.slf4j.org/) (runtime, optional)

# Related Projects

* [TNT4J](https://github.com/Nastel/TNT4J/)
* [TNT4J-Log4j Appender](https://github.com/Nastel/tnt4j-log4j/)
* [TNT4J-Logback Appender](https://github.com/Nastel/tnt4j-logback/)
* [TNT4J-Servlet-Filter](http://nastel.github.io/tnt4j-servlet-filter/)
* [TNT4J-Stream-JMX](http://nastel.github.io/tnt4j-stream-jmx/)
* [TNT4J-Stream-GC](http://nastel.github.io/tnt4j-stream-gc/)
* [TNT4J-Streams](https://github.com/Nastel/tnt4j-streams/)

# Available Integrations

* [jkoolcloud.com](https://www.jkoolcloud.com/)
