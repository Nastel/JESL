@echo off
set JARDIR=%~p0
java -Dtnt4j.config=..\tnt4j-simulator.properties -jar %JARDIR%\jkool-jesl.jar %*