#!/usr/bin/env bash
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

# Regenerate the GraalVM native-image agent metadata snapshot for the six
# DC3 native services (dc3-center/* + dc3-gateway).
#
# Each service's repackaged JAR is booted once on a JVM with the
# native-image-agent attached; the agent records every reflective / resource /
# service access up to the point the context fails for lack of infrastructure
# (expected end state: the R2DBC connection factory cannot resolve a URL).
# The capture is what makes SLF4J->logback binding, R2DBC driver discovery and
# friends survive AOT closed-world analysis — Spring Boot AOT does not cover
# those ServiceLoader descriptors.
#
# The snapshots under src/main/resources/META-INF/native-image/...-agent/ are
# build inputs: re-run this script after dependency upgrades or startup-path
# changes, and whenever a native service dies with NoClassDefFoundError /
# IllegalStateException at boot. Booting a service against real infrastructure
# BEFORE it fails would extend coverage (gRPC serving, live queries) — pass
# SPRING_* env vars to do so.
#
# Requires: a Docker daemon hosting the dc3-jdk-native builder image
# (dc3-docker/dc3/dependencies/jdk-native/graalvm_25) and packaged JARs
# (mvn -DskipTests -pl <services> -am package).
set -e

BUILDER_IMAGE="${DC3_JDK_NATIVE_IMAGE:-dc3-jdk-native:25}"
SERVICES="dc3-gateway dc3-center/dc3-center-auth dc3-center/dc3-center-manager dc3-center/dc3-center-data dc3-center/dc3-center-agentic dc3-center/dc3-center-single"

for service in ${SERVICES}; do
    name=$(basename "${service}")
    jar="${service}/target/${name}.jar"
    if [ ! -f "${jar}" ]; then
        echo "${name}: missing ${jar} — package it first" >&2
        exit 1
    fi

    out_dir="${service}/src/main/resources/META-INF/native-image/io.github.pnoker/${name}-agent"
    abs_target=$(cd "$(dirname "${jar}")" && pwd)/target

    mkdir -p "${out_dir}"
    echo "${name}: booting with agent..."
    docker run --rm \
        -v "$(pwd)/${service}/target:/app" \
        -v "$(pwd)/${out_dir}:/out" \
        "${BUILDER_IMAGE}" \
        java -agentlib:native-image-agent=config-output-dir=/out \
        -jar "/app/${name}.jar" >/dev/null 2>&1 || \
        echo "${name}: JVM exit non-zero (expected without infrastructure); metadata still collected"
    echo "${name}: metadata written to ${out_dir}/reachability-metadata.json"
done
