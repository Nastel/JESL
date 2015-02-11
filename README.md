#JESL - jKool Event Streaming Library
JESL allows application developers stream time-series data to jKool Cloud.
To stream data to jKool Cloud your application must:
	
	1. Use TNT4J, or log4J+TNT4JAppender to log events, activities, metrics
	2. Obtain your jKool account and API access token at https://www.jkoolcloud.com
	2. JESL Event Sink implementation (contained in this project) 
	3. Configure your application for streaming to jKool Cloud using JESL Event Sink (requires API access token)
	
JESL consists of the following main components:

	1. TNT4J streaming library (https://github.com/Nastel/TNT4J)
	
	2. JESL Simulator -- allows streaming user simulated events, activities and metrics to jKool Cloud.
	
	3. JESL Event Sink -- TNT4J Event Sink implementation to stream events to jKool Cloud

## JESL Simulator
The JESL Simulator provides the ability to simulate tracking activities
and events. The activities, events, and their components are defined using
a XML format.  There are three major parts to a simulation definition:

	1. Sources
	   Defines the sources involved in simulated activities and events
	   
	2. Messages
	   Defines the messages that will be exchanged during activities
	   
	3. Activities and Events
	   Defines the actual activites and the subactivities and events in them

The included file `tnt4j-sim-template.xml` contains a simulation definition template,
along with detailed descriptions of each element, showing the XML element
hierarchy.  The file order-process.xml contains a sample set of activities and
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

The simplest way to run the simulator is to execute the file jkool-simulator.bat
(or jkool-simulator.sh) as follows:

	Windows: `jkool-simulator.bat <access_token> [<iterations>]`
	Unix: `jkool-simulator.sh <access_token> [<iterations>]`
		
`<iterations>` is the number of interations for a given simulation (1 default).

<b>NOTE:</b> You will need to alter `jkool-simulator` scripts to specify
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
			input file by appending a timestamp to each one, so that each
			iteration over the file	will generate independent activities
			and events
	
To see the full set of supported options, run:

	`jkool-simulator.bat help`

Streaming TNT4J to jKool Cloud 
==============================
Applications that use TNT4J can be configured to stream events and metrics to jKool Cloud
by configuring application source to use JESL Event Sink (`com.jkool.jesl.tnt4j.sink.JKCloudEventSink`)
Configure your TNT4J source as follows (using `tnt4j.properties` file):
```
{
	....
	; event sink configuration: destination and data format
	event.sink.factory: com.nastel.jkool.tnt4j.sink.BufferedEventSinkFactory
	event.sink.factory.EventSinkFactory: com.jkool.jesl.tnt4j.sink.JKCloudEventSink
	event.sink.factory.EventSinkFactory.Url: http://data.jkoolcloud.com
	event.sink.factory.EventSinkFactory.Token: YOUR-ACCESS-TOKEN
	event.formatter: com.nastel.jkool.tnt4j.format.JSONFormatter
	....
}
```
Below is an example of a sample TNT4J source (com.myco.myappl) configuration with 
JESL Event Sink (com.jkool.jesl.tnt4j.sink.JKCloudEventSink):
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
	event.sink.factory.EventSinkFactory: com.jkool.jesl.tnt4j.sink.JKCloudEventSink
	event.sink.factory.EventSinkFactory.Url: http://data.jkoolcloud.com
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

