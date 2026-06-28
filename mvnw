#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# Maven Wrapper Start Up Batch script

set -e

# Find the project base directory
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG="`dirname "$PRG"`/$link"
  fi
done
SAVED="`pwd`"
cd "`dirname "$PRG"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

# Locate the maven-wrapper.jar
MAVEN_WRAPPER_JAR="$APP_HOME/.mvn/wrapper/maven-wrapper.jar"
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
  echo "Downloading Maven Wrapper JAR..."
  MAVEN_WRAPPER_DOWNLOAD_URL="https://dlcdn.apache.org/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL "$MAVEN_WRAPPER_DOWNLOAD_URL" -o "$MAVEN_WRAPPER_JAR"
  elif command -v wget > /dev/null 2>&1; then
    wget -q "$MAVEN_WRAPPER_DOWNLOAD_URL" -O "$MAVEN_WRAPPER_JAR"
  else
    echo "ERROR: Neither curl nor wget found. Please install one to download the Maven Wrapper JAR."
    exit 1
  fi
fi

# Use the Maven Wrapper JAR to run Maven
exec "$JAVACMD" \
  $MAVEN_OPTS \
  $MAVEN_DEBUG_OPTS \
  -classpath "$MAVEN_WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$APP_HOME" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
