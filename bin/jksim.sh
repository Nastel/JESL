#! /bin/bash
ORIGDIR=`pwd`
cd `dirname $0`
SIMDIR=`pwd`/
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

SIMCP="$SIMDIR..:$SIMDIR../jkool-jesl.jar:$SIMDIR../lib/*"
SIMMAIN=com.jkool.jesl.simulator.TNT4JSimulator
TNT4JOPTS="-Dlog4j.configuration=file:$SIMDIR../config/log4j-simulator.properties -Dtnt4j.config=$SIMDIR../config/tnt4j-simulator.properties -Dtnt4j.token.repository=$SIMDIR../config/tnt4j-tokens.properties"
SIMFILE=$SIMDIR../sims/order-process.xml
java $TNT4JOPTS -classpath $SIMCP $SIMMAIN $SIMCMD -A:$ACCTKN -f:$SIMFILE -T:data.jkoolcloud.com -P:6580 -C:HTTP -i:$SIMRUNS -p:98 -u