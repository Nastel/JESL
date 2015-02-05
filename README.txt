jKool(TM) Simulator
===================

Version: @VERSION@

The jKool Simulator provides the ability to simulate tracking activities
and events. The activities, events, and their components are defined using
a XML format.  There are three major parts to a simulation definition:

	1. Sources
	   Defines the sources involved in simulated activities and events
	   
	2. Messages
	   Defines the messages that will be exchanged during activities
	   
	3. Activities and Events
	   Defines the actual activites and the subactivities and events in them

The included file tnt4j-sim-template.xml contains a simulation definition template,
along with detailed descriptions of each element, showing the XML element
hierarchy.  The file order-process.xml contains a sample set of activities and
their events, along with some sample snapshots.

To define a simulation, copy one of the supplied XML simulation definition
files to use as a template and create the necessary activity elements.

The simulator can be run in one of two modes:

	1. Simulation (simulation type: "run")
	   Runs the specified simulation file and sends the tracking data to
	   the jKool Cloud Gateway and/or writes the tracking data to the
	   specified file.  In this mode, the simulator can be configured to
	   run the simulation file a specified number of times, optionally
	   generating unique correlators and tags for each iteration of
	   the simulation file (appends a unique value to end of defined
	   correlator and tags in simulation definition file).
	
	2. Replay (simulation type: "replay")
	   Reads previously-saved tracking data from the specified file
	   and sends it to the jKool Cloud Gateway

The simplest way to run the simulator is to execute the file jkool-simulator.bat
(or jkool-simulator.sh) as follows:

	jkool-simulator.bat run -A:<access_token> -f:<sim_def_file> -T:<jkool_host> -C:HTTP
	
If '-f' is omitted, the simulator will prompt for the simulation definition
filename.  One of -T (to send tracking data to jKool Cloud Gateway) or -x (to send
tracking data to file) must be specified.

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
			connection to jKool Cloud Gateway.  This option is required
			when using -T option

	-p		This option will cause simulator to scale each time attribute
			up or down by the specified percentage.  This prevents each
			activity from looking exactly the same, showing some variation
			in the event durations, as well as the times between events
	
	-u		Makes correlators and tags unique between iterations on the
			input file by appending a timestamp to each one, so that each
			iteration over the file	will generate independent activities
			and events
	
To see the full set of supported options, run:

	jkool-simulator.bat help
