@echo off
setlocal
set RUNDIR=%~p0
set SIMCMD=run
if "%1" == "help" (set SIMCMD=help) else set ACCTKN=%1
set SIMRUNS=1
if not "%2" == "" set SIMRUNS=%2

set CLPATH=%RUNDIR%..;%RUNDIR%../jkool-jesl.jar;%RUNDIR%../lib/*;%RUNDIR%../lib/slf4j-simple/*
set MAINCL=com.jkool.jesl.simulator.TNT4JSimulator
set TNT4JOPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dtnt4j.config=%RUNDIR%../config/tnt4j-simulator.properties -Dtnt4j.token.repository=%RUNDIR%../config/tnt4j-tokens.properties
set SIMFILE=%RUNDIR%\..\sims\order-process.xml
java %TNT4JOPTS% -classpath %CLPATH% %MAINCL% %SIMCMD% -A:%ACCTKN% -f:%SIMFILE% -T:data.jkoolcloud.com -P:6580 -C:HTTP -u -p:25 -i:%SIMRUNS%
endlocal