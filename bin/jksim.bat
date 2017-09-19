@echo off
setlocal
set RUNDIR=%~dp0
set SIMCMD=run

set CLPATH="%RUNDIR%..;%RUNDIR%../*.jar;%RUNDIR%../lib/*"
set MAINCL=com.jkoolcloud.jesl.simulator.TNT4JSimulator
set TNT4JOPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dtnt4j.config="%RUNDIR%../config/tnt4j-simulator.properties" -Dtnt4j.token.repository="%RUNDIR%../config/tnt4j-tokens.properties"

java %TNT4JOPTS% -classpath %CLPATH% %MAINCL% %SIMCMD% -T:data.jkoolcloud.com -C:HTTPS -p:10 -ui -uc %*
endlocal