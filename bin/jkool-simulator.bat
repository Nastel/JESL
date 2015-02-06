@echo off
set JARDIR=%~p0
set OPTIONS=-cp "%JARDIR%\..\lib" -Dlog4j.configuration="file:%JARDIR%\..\config\log4j-simulator.properties" -Dtnt4j.config="%JARDIR%\..\config\tnt4j-simulator.properties" -Dtnt4j.token.repository="%JARDIR%\..\tnt4j-tokens.properties"
java -%OPTIONS% -jar "%JARDIR%\..\jkool-jesl.jar" %*