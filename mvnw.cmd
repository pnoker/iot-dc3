@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Maven Wrapper Start Up Batch script

@setlocal
set MAVEN_PROJECTBASEDIR=%CD%
if not "%MAVEN_PROJECTBASEDIR%"=="" goto endDetectBaseDir

:findBaseDir
set "WDIR=%~dp0"
if exist "%WDIR%\.mvn" goto baseDirFound
cd ..
goto findBaseDir

:baseDirFound
set "MAVEN_PROJECTBASEDIR=%WDIR%"
cd "%WDIR%"

:endDetectBaseDir
set "CLASSWORLDS_LAUNCHER=org.codehaus.plexus.classworlds.launcher.Launcher"

set "MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
if not exist "%MAVEN_WRAPPER_JAR%" (
  echo Downloading Maven Wrapper JAR...
  powershell -Command "Invoke-WebRequest -Uri 'https://dlcdn.apache.org/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar' -OutFile '%MAVEN_WRAPPER_JAR%'"
)

"%JAVACMD%" ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath "%MAVEN_WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  %*
@endlocal
