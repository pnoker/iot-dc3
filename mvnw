#!/bin/sh
#
# Copyright 2016-present the IoT DC3 original author or authors.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

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
