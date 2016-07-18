@echo off
setlocal
set RUNDIR=%~p0

set CLPATH=%RUNDIR%../*.jar;%RUNDIR%../lib/*
set MAINCL=com.jkoolcloud.jesl.net.syslogd.SyslogSend
java -classpath %CLPATH% %MAINCL% %*
endlocal