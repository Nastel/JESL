@echo off
setlocal
set SIMDIR=%~p0
set SIMCMD=run
if "%1" == "help" (set SIMCMD=help) else set ACCTKN=%1
set SIMRUNS=1
if not "%2" == "" set SIMRUNS=%2

set SIMCP=%SIMDIR%..;%SIMDIR%..\jkool-jesl.jar;%SIMDIR%../lib/*
set SIMMAIN=com.jkool.jesl.simulator.TNT4JSimulator
set TNT4JOPTS=-Dlog4j.configuration=file:%SIMDIR%..\config\log4j-simulator.properties -Dtnt4j.config=%SIMDIR%..\config\tnt4j-simulator.properties -Dtnt4j.token.repository=%SIMDIR%..\config\tnt4j-tokens.properties
set SIMFILE=%SIMDIR%\..\sims\order-process.xml
java %TNT4JOPTS% -classpath %SIMCP% %SIMMAIN% %SIMCMD% -A:%ACCTKN% -f:%SIMFILE% -T:data.jkoolcloud.com -P:6580 -C:HTTP -u -p:98 -i:%SIMRUNS%
endlocal