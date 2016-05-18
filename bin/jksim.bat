@echo off
setlocal
set RUNDIR=%~p0
set SIMCMD=run

set CLPATH="%RUNDIR%..;%RUNDIR%../jkool-jesl.jar;%RUNDIR%../lib/*;%RUNDIR%../lib/slf4j-simple/*"
set MAINCL=com.jkool.jesl.simulator.TNT4JSimulator
set TNT4JOPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dtnt4j.config="%RUNDIR%../config/tnt4j-simulator.properties" -Dtnt4j.token.repository="%RUNDIR%../config/tnt4j-tokens.properties"

java %TNT4JOPTS% -classpath %CLPATH% %MAINCL% %SIMCMD% -T:data.jkoolcloud.com -C:HTTPS -p:10 -ui -uc %*
endlocal