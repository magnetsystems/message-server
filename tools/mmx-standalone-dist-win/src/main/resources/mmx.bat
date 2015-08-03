@echo off

rem Copyright (c) 2015 Magnet Systems, Inc.

rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at

rem        http://www.apache.org/licenses/LICENSE-2.0

rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

rem Please change .\startup.properties if you would like to use ports other than the default ones.
rem For detail, please reference the troubleshooting guide.

setlocal

if "%selfWrapped%"=="" (
  SET selfWrapped=true
  %ComSpec% /s /c ""%~0" %*"
  GOTO :EOF
)



set check_port=true
set CONSOLENAME="Administration web interface"
set SERVERNAME="Messaging server"
set SUITENAME="Magnet Message"

call :check2Args %*
call :loadPorts startup.properties

if "-p"=="%1" (
	set check_port=false
	shift
)	

if "start"=="%1" (
	call :start
) else (
	if "stop"=="%1" (
		call :stop
	) else (
		if "restart"=="%1" (
			call :restart
		) else (
			call :print_usage
		)
	)
) 
endlocal
exit /b



:print_usage
echo Usage: mmx.bat [-p] {start^|stop^|restart}
echo.
echo Start, stop, or restart the %SERVERNAME% and %CONSOLENAME%.
echo. 
echo Options:
echo    -p    No port check when starting.
echo.
echo Usage: execute the command in a Command Prompt.
echo.
pause
exit /b



:start
	if exist messaging\bin\mmx-server.pid (
		echo.
		echo Error! %SERVERNAME% is already running or you have a stale pid file. If %SERVERNAME% is not running, then please delete mmx-standalone-dist-win\messaging\bin\mmx-server.pid file and try again.
		exit 1
	)

	if exist console\mmx-console.pid (
		echo.
		echo Error! %CONSOLENAME% is already running or you have a stale pid file. If %CONSOLENAME% is not running, then please delete mmx-standalone-dist-win\console\mmx-console.pid file and try again.
		exit 1
	)	

	if %check_port%==true (
		echo Checking port availability
	        call :check_port_list %xmppPort% %xmppPortSecure% %httpPort% %httpPortSecure% %mmxAdminPort% %mmxAdminPortSecure% %mmxPublicPort% %mmxPublicPortSecure% %consolePort%
	)

	echo.
	echo Starting %SERVERNAME% ...
	call :set_messaging_startup_properties
	pushd messaging\bin

	if %check_port%==true (
		call .\mmx-server.bat start
	) else (
		call .\mmx-server.bat -p start
	)

	if 0 equ %ERRORLEVEL% (
		popd
		echo.
		echo Starting %CONSOLENAME%...
		call :set_console_startup_properties
		pushd console
		if %check_port%==true (
			cmd /c .\mmx-console.bat start
		) else (
			cmd /c .\mmx-console.bat -p start
		)

		popd
		if not exist .\console\mmx-console.pid (
			pushd messaging\bin
			.\mmx-server.bat stop
			popd
		) else (
			call :check_mysql
			echo If it is the first time you launch %SUITENAME%, please open http://127.0.0.1:3000 with a browser and continue with the configuration.
		)
		popd
	) else (
		popd
	)
goto :eof



:stop
	echo.
	echo Stopping %SERVERNAME% ...
	pushd messaging\bin
	call .\mmx-server.bat stop
	popd
	echo.
	echo Stopping %CONSOLENAME% ...
	pushd console
	call .\mmx-console.bat stop
	popd
goto :eof



:restart
	call :stop
	call :start
goto :end



:check2Args
setlocal EnableDelayedExpansion
FOR %%A in (%*) DO (
        SET /A args_count+=1
        if !args_count! equ 3 (
                call :print_usage
                exit
        )
)

if !args_count! equ 0 (
        call :print_usage
        exit
)
endlocal
goto :eof



:loadPorts
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "dbHost=" %1') do SET dbHost=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "dbPort=" %1') do SET dbPort=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "dbName=" %1') do SET dbName=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "dbUser=" %1') do SET dbUser=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "dbPassword=" %1') do SET dbPassword=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "xmppDomain=" %1') do SET xmppDomain=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "xmppPort=" %1') do SET xmppPort=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "xmppPortSecure=" %1') do SET xmppPortSecure=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "httpPort=" %1') do SET httpPort=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "httpPortSecure=" %1') do SET httpPortSecure=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "mmxAdminPort=" %1') do SET mmxAdminPort=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "mmxAdminPortSecure=" %1') do SET mmxAdminPortSecure=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "mmxPublicPort=" %1') do SET mmxPublicPort=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "mmxPublicPortSecure=" %1') do SET mmxPublicPortSecure=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "standaloneServer=" %1') do SET standaloneServer=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "consolePort=" %1') do SET consolePort=%%i
goto :eof



:printPorts
	ECHO dbHost=%dbHost%
	ECHO dbPort=%dbPort%
	ECHO dbName=%dbName%
	ECHO dbUser=%dbUser%
	ECHO dbPassword=%dbPassword%
	ECHO xmppDomain=%xmppDomain%
	ECHO xmppPort=%xmppPort%
	ECHO xmppPortSecure=%xmppPortSecure%
	ECHO httpPort=%httpPort%
	ECHO httpPortSecure=%httpPortSecure%
	ECHO mmxAdminPort=%mmxAdminPort%
	ECHO mmxAdminPortSecure=%mmxAdminPortSecure%
	ECHO mmxPublicPort=%mmxPublicPort%
	ECHO mmxPublicPortSecure=%mmxPublicPortSecure%
	ECHO consolePort=%consolePort%
goto :eof



:check_port
        netstat -aon | findstr ":%1 " 1>NUL
        if %ERRORLEVEL% equ 0 (
                echo.
                echo Error! TCP port "%1" is already in use; thus, cannot start %SUITENAME%. Please refer to readme.htm on how to change the ports.
                exit 1
        )
goto :eof



:check_port_list
	for %%a in (%*) do (
        	call :check_port %%a
	)
goto :eof



:set_startup_property
	echo %2=%3 >> %1
goto :eof



:set_console_startup_properties
	del console\startup.properties
	call :set_startup_property console\startup.properties consolePort %consolePort%
	call :set_startup_property console\startup.properties httpPort %httpPort%
goto :eof



:loadPorts
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "dbHost=" %1') do SET dbHost=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "dbPort=" %1') do SET dbPort=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "dbName=" %1') do SET dbName=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "dbUser=" %1') do SET dbUser=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "dbPassword=" %1') do SET dbPassword=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "xmppDomain=" %1') do SET xmppDomain=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "xmppPort=" %1') do SET xmppPort=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "xmppPortSecure=" %1') do SET xmppPortSecure=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "httpPort=" %1') do SET httpPort=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "httpPortSecure=" %1') do SET httpPortSecure=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "mmxAdminPort=" %1') do SET mmxAdminPort=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "mmxAdminPortSecure=" %1') do SET mmxAdminPortSecure=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "mmxPublicPort=" %1') do SET mmxPublicPort=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "mmxPublicPortSecure=" %1') do SET mmxPublicPortSecure=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "standaloneServer=" %1') do SET standaloneServer=%%i
	FOR /F "eol=; tokens=2 delims==" %%i IN ('findstr /i "consolePort=" %1') do SET consolePort=%%i
goto :eof




:set_messaging_startup_properties
	del messaging\conf\startup.properties
	call :set_startup_property messaging\conf\startup.properties dbHost %dbHost%
	call :set_startup_property messaging\conf\startup.properties dbPort %dbPort%
	call :set_startup_property messaging\conf\startup.properties dbName %dbName%
	call :set_startup_property messaging\conf\startup.properties dbUser %dbUser%
	call :set_startup_property messaging\conf\startup.properties dbPassword %dbPassword%
	call :set_startup_property messaging\conf\startup.properties xmppDomain %xmppDomain%
	call :set_startup_property messaging\conf\startup.properties xmppPort %xmppPort%
	call :set_startup_property messaging\conf\startup.properties xmppPortSecure %xmppPortSecure%
	call :set_startup_property messaging\conf\startup.properties httpPort %httpPort%
	call :set_startup_property messaging\conf\startup.properties httpPortSecure %httpPortSecure%
	call :set_startup_property messaging\conf\startup.properties mmxAdminPort %mmxAdminPort%
	call :set_startup_property messaging\conf\startup.properties mmxAdminPortSecure %mmxAdminPortSecure%
	call :set_startup_property messaging\conf\startup.properties mmxPublicPort %mmxPublicPort%
	call :set_startup_property messaging\conf\startup.properties mmxPublicPortSecure %mmxPublicPortSecure%
	call :set_startup_property messaging\conf\startup.properties standaloneServer %standaloneServer%
goto :eof



:check_mysql
	sc query mysql56 | findstr /i RUNNING 1>NUL 2>NUL
		if %ERRORLEVEL% NEQ 0 (
			echo !!! No local MySQL 5.6 detected !!!
			echo Either a local or remote MySQL is required for Magnet Message
			echo.
		)
goto :eof

:end
