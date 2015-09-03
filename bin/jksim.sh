#! /bin/bash
ORIGDIR=`pwd`
cd `dirname $0`
RUNDIR=`pwd`/
cd $ORIGDIR

SIMCMD=run
if [ "$1" == "help" ]; then
	SIMCMD=help
else
	ACCTKN=$1
fi

SIMRUNS=1
if [ "$2" != "" ]; then
	SIMRUNS=$2
fi

CLPATH="$RUNDIR..:$RUNDIR../jkool-jesl.jar:$RUNDIR../lib/*"
MAINCL=com.jkool.jesl.simulator.TNT4JSimulator
TNT4JOPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dtnt4j.config=$RUNDIR../config/tnt4j-simulator.properties -Dtnt4j.token.repository=$RUNDIR../config/tnt4j-tokens.properties"
SIMFILE=$RUNDIR../sims/order-process.xml
java $TNT4JOPTS -classpath $CLPATH $MAINCL $SIMCMD -A:$ACCTKN -f:$SIMFILE -T:data.jkoolcloud.com -P:6580 -C:HTTP -i:$SIMRUNS -p:25 -u