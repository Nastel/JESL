@echo off
set JARDIR=%~p0
java -Dlog4j.configuration=file:..\log4j.properties -Dtnt4j.config=..\tnt4j-simulator.properties -jar %JARDIR%\jkool-jesl.jar %*