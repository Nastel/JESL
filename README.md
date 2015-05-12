#JESL - jKool Event Streaming Library
JESL allows application developers stream time-series data to jKool Cloud.
To stream data to jKool Cloud your application must:
	
	1. Use TNT4J, or log4J+TNT4JAppender in your application
	  to log events, activities, metrics
	
	2. Obtain your jKool account and API access token 
	   at https://www.jkoolcloud.com
	
	3. Use JESL Event Sink implementation (contained in this project) 
	   within your tnt4j configuration. API access token will be required
	   to stream data to jKool Cloud.
	
	4. Configure your application for streaming to jKool Cloud 
	   using JESL Event Sink (requires API access token).
	   See (`com.jkool.jesl.tnt4j.sink.JKCloudEventSinkFactory`)
	
JESL consists of the following main components:

	1. TNT4J streaming library 
	   (https://github.com/Nastel/TNT4J)
	
	2. JESL Simulator -- stream simulated events, activities and metrics
	   to jKool Cloud. Simulations are defined in xml files.
	   (see `sims/tnt4j-sim-template.xml` and `sims/order-process.xml`) 
	
	3. JESL Event Sink -- TNT4J Event Sink implementation
	   to stream events to jKool Cloud.

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
	   Runs default simulation file (`sims/order-process.xml`) and sends the tracking data to
	   the JESL Event Sink and/or writes the tracking data to the
	   specified file.  In this mode, the simulator can be configured to
	   run the simulation file a specified number of iterations, optionally
	   generating unique correlators and tags for each iteration of
	   the simulation file (appends a unique value to end of defined
	   correlator and tags in simulation definition file).
	
	2. Replay (simulation type: `replay`)
	   Reads previously-saved tracking data from the specified file
	   and sends it to the JESL Event Sink

The simplest way to run the simulator is to execute the file `jksim.bat`
(or `jksim.sh`) as follows:

	Windows: `jksim.bat <access_token> [<iterations>]`
	Unix: `jksim.sh <access_token> [<iterations>]`
		
`<iterations>` is the number of iterations for a given simulation (1 default).

<b>NOTE:</b> You will need to alter `jksim` scripts to specify
custom simulator parameters such as simulation type as well as options described
further in this document.

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

Streaming Log4j to jKool Cloud 
==============================
Log4J can be configured to stream events and metrics to jKool Cloud by using 
JESL log4j appender (`com.nastel.jkool.tnt4j.logger.TNT4JAppender`) as follows:

1. Add JESL log4j appender to your log4j configuration
```
### Default JESL Appender configuration
log4j.appender.jkoolcloud=com.nastel.jkool.tnt4j.logger.TNT4JAppender
log4j.appender.jkoolcloud.SourceName=com.jkool.jesl.stream
log4j.appender.jkoolcloud.SourceType=APPL
log4j.appender.jkoolcloud.MetricsOnException=true
log4j.appender.jkoolcloud.MetricsFrequency=60
log4j.appender.jkoolcloud.layout=org.apache.log4j.PatternLayout
log4j.appender.jkoolcloud.layout.ConversionPattern=%d{ABSOLUTE} %-5p [%c{1}] %m%n

## JESL Configuration
log4j.logger.com.jkool.jesl.stream=off
log4j.logger.com.nastel.jkool=info
log4j.logger.com.jkool.jesl=info
```
Define categories that should map to this JESL appender. example:
```
log4j.logger.com.myco.mypackage=info,jkoolcloud
```

2. Add the following arguments to your java startup
```
-Dtnt4j.config=<jesl.home>/log4j/tnt4j.properties -Dtnt4j.token.repository=<jesl.home>/log4j/tnt4j-tokens.properties 
```
Make sure all JESl jar files in `<jesl.home>/lib` are your classpath.

3. Edit `<jesl.home>/log4j/tnt4j.properties` and replace `YOUR-ACCESS-TOKEN` with your jKool API access token.

4. Restart your application and messages which map to JESL appender will stream to jKool Cloud.

NOTE: Visit https://github.com/Nastel/TNT4J#log4j-integration for more information on `TNT4JAppender`.
Optionally you may annotate your log4j messages to provide better context, timing as well as report
custom metrics.

Streaming TNT4J to jKool Cloud 
==============================
Applications that use TNT4J can be configured to stream events and metrics to jKool Cloud
by configuring application source to use JESL Event Sink (`com.jkool.jesl.tnt4j.sink.JKCloudEventSinkFactory`)
Configure your TNT4J source as follows (using `tnt4j.properties` file):
```
{
	....
	; event sink configuration: destination and data format
	event.sink.factory: com.nastel.jkool.tnt4j.sink.BufferedEventSinkFactory
	event.sink.factory.EventSinkFactory: com.jkool.jesl.tnt4j.sink.JKCloudEventSinkFactory
	event.sink.factory.EventSinkFactory.Url: http://data.jkoolcloud.com:6580
	event.sink.factory.EventSinkFactory.Token: YOUR-ACCESS-TOKEN
	event.formatter: com.nastel.jkool.tnt4j.format.JSONFormatter
	....
}
```
Below is an example of a sample TNT4J source (com.myco.myappl) configuration with 
JESL Event Sink (`com.jkool.jesl.tnt4j.sink.JKCloudEventSinkFactory`):
```
{
	source: com.myco.myappl
	source.factory: com.nastel.jkool.tnt4j.source.SourceFactoryImpl
	source.factory.GEOADDR: NewYork
	source.factory.DATACENTER: HQDC
	source.factory.RootFQN: SERVER=?#DATACENTER=?#GEOADDR=?	
	
	tracker.factory: com.nastel.jkool.tnt4j.tracker.DefaultTrackerFactory
	dump.sink.factory: com.nastel.jkool.tnt4j.dump.DefaultDumpSinkFactory

	; event sink configuration: destination and data format
	event.sink.factory: com.nastel.jkool.tnt4j.sink.BufferedEventSinkFactory
	event.sink.factory.EventSinkFactory: com.jkool.jesl.tnt4j.sink.JKCloudEventSinkFactory
	event.sink.factory.EventSinkFactory.Url: http://data.jkoolcloud.com:6580
	event.sink.factory.EventSinkFactory.Token: YOUR-ACCESS-TOKEN
	event.formatter: com.nastel.jkool.tnt4j.format.JSONFormatter

	; Configure default sink filter based on level and time (elapsed/wait)
	event.sink.factory.Filter: com.nastel.jkool.tnt4j.filters.EventLevelTimeFilter
	event.sink.factory.Filter.Level: TRACE
	; Uncomment lines below to filter out events based on elapsed time and wait time
	; Timed event/activities greater or equal to given values will be logged
	;event.sink.factory.Filter.ElapsedUsec: 100
	;event.sink.factory.Filter.WaitUsec: 100
	
	tracking.selector: com.nastel.jkool.tnt4j.selector.DefaultTrackingSelector
	tracking.selector.Repository: com.nastel.jkool.tnt4j.repository.FileTokenRepository
}
```
<b>NOTE:</b> You will need to provide your actual API access token in (`event.sink.factory.EventSinkFactory.Token`).

# Project Dependencies
JESL requires the following:
* JDK 1.6+
* TNT4J (https://github.com/Nastel/TNT4J)
* Apache HTTP Client 4.2.5 (http://hc.apache.org/httpcomponents-client-ga/)
* Apache HTTP Core 4.3.4 (http://hc.apache.org/httpcomponents-client-ga/)

# Related Projects
* TrackingFilter (http://nastel.github.io/TrackingFilter/)
* PingJMX (http://nastel.github.io/PingJMX/)

# Available Integrations
* TNT4J (https://github.com/Nastel/TNT4J)
* Log4J (http://logging.apache.org/log4j/1.2/)
* jkoolcloud.com (https://www.jkoolcloud.com)
