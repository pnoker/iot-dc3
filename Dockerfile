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

# syntax=docker/dockerfile:1.6

# =============================================================================
# Unified multi-target Dockerfile for IoT DC3.
#
# Build context MUST be the repo root. Maven runs INSIDE the builder stage —
# no host JDK / Maven required. When the build context and args are unchanged,
# Docker BuildKit / Podman Buildah reuse the builder stage cache across service
# targets, so multiple service images do not rerun Maven.
#
# Build a single service:
#     podman build --target dc3-gateway -t pnoker/dc3-gateway:dev .
#
# Build against a locally rebuilt JRE:
#     podman build --build-arg DC3_JRE_IMAGE=localhost/pnoker/dc3-jre:21 \
#         --target dc3-gateway -t localhost/dc3-gateway:dev .
#
# Build everything via compose (BuildKit reuses the builder stage):
#     podman compose -f dc3/docker-compose-dev.yml build
#
# Multi-arch (CI):
#     docker buildx build --platform linux/arm64,linux/amd64 \
#         --target dc3-gateway --push --tag ... .
# =============================================================================

ARG DC3_JDK_IMAGE=docker.io/pnoker/dc3-jdk:21
ARG DC3_JRE_IMAGE=docker.io/pnoker/dc3-jre:21


# -----------------------------------------------------------------------------
# Stage 1: builder — runs `mvn package` ONCE for the whole multi-module reactor.
#
# --platform=$BUILDPLATFORM forces the JDK to run on the host's native arch.
# JARs are platform-agnostic, so we never want Maven to run under QEMU
# emulation when producing arm64 + amd64 images.
#
# --mount=type=cache caches ~/.m2/repository across builds.
# -----------------------------------------------------------------------------
FROM --platform=$BUILDPLATFORM ${DC3_JDK_IMAGE} AS builder
LABEL dc3.author=pnoker
LABEL dc3.author.email=pnokers.icloud.com

ARG PROFILE=dev
ARG MAVEN_OPTS_EXTRA=""
ENV MAVEN_OPTS="${MAVEN_OPTS_EXTRA}"

WORKDIR /build
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

COPY . .
# settings-container.xml goes straight to Maven Central (no Aliyun mirror), so
# brand-new releases are visible immediately even when the mirror lags behind.
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -U -B -e -T 1C -s .mvn/settings-container.xml clean package -DskipTests -P ${PROFILE}


# -----------------------------------------------------------------------------
# Stage 2: runtime base — the only place where JRE image, timezone and common
# environment defaults are declared. Every service target builds on top of it.
# -----------------------------------------------------------------------------
FROM ${DC3_JRE_IMAGE} AS runtime-base
LABEL dc3.author=pnoker
LABEL dc3.author.email=pnokers.icloud.com

ENV PARAMS=''
ENV NODE_ENV=test
ENV SERVER_PACKAGES=io.github.pnoker
ENV APM_AGENT_ENABLE=false
ENV APM_SERVICE=http://dc3-apm:8200

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime


# =============================================================================
# Service targets — one per microservice. Each target:
#   1. inherits runtime-base
#   2. sets service-specific ENV / WORKDIR / EXPOSE / VOLUME
#   3. COPYs its jar from the builder stage
#   4. copies the prebaked entrypoint.sh from the base image
# =============================================================================


# ---------- dc3-gateway ----------
FROM runtime-base AS dc3-gateway
ENV DC3_GATEWAY_PORT=8000
ENV SERVER_NAME=dc3-gateway
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/gateway/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/gateway/gc/gc-%t.log
WORKDIR /dc3-gateway
RUN mkdir -p /dc3-gateway/dc3/logs/gateway/gc
COPY --from=builder /build/dc3-gateway/target/dc3-gateway.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
EXPOSE ${DC3_GATEWAY_PORT}
VOLUME /dc3-gateway/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-gateway.jar"]


# ---------- dc3-center-auth ----------
FROM runtime-base AS dc3-center-auth
ENV DC3_AUTH_PORT=8300
ENV DC3_AUTH_GRPC_PORT=9300
ENV SERVER_NAME=dc3-center-auth
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/center/auth/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/center/auth/gc/gc-%t.log
WORKDIR /dc3-center/dc3-center-auth
RUN mkdir -p /dc3-center/dc3-center-auth/dc3/logs/center/auth/gc
COPY --from=builder /build/dc3-center/dc3-center-auth/target/dc3-center-auth.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
EXPOSE ${DC3_AUTH_PORT}
EXPOSE ${DC3_AUTH_GRPC_PORT}
VOLUME /dc3-center/dc3-center-auth/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-center-auth.jar"]


# ---------- dc3-center-manager ----------
FROM runtime-base AS dc3-center-manager
ENV DC3_MANAGER_PORT=8400
ENV DC3_MANAGER_GRPC_PORT=9400
ENV SERVER_NAME=dc3-center-manager
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/center/manager/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/center/manager/gc/gc-%t.log
WORKDIR /dc3-center/dc3-center-manager
RUN mkdir -p /dc3-center/dc3-center-manager/dc3/logs/center/manager/gc
COPY --from=builder /build/dc3-center/dc3-center-manager/target/dc3-center-manager.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
EXPOSE ${DC3_MANAGER_PORT}
EXPOSE ${DC3_MANAGER_GRPC_PORT}
VOLUME /dc3-center/dc3-center-manager/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-center-manager.jar"]


# ---------- dc3-center-data ----------
FROM runtime-base AS dc3-center-data
ENV DC3_DATA_PORT=8500
ENV DC3_DATA_GRPC_PORT=9500
ENV SERVER_NAME=dc3-center-data
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/center/data/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/center/data/gc/gc-%t.log
WORKDIR /dc3-center/dc3-center-data
RUN mkdir -p /dc3-center/dc3-center-data/dc3/logs/center/data/gc
COPY --from=builder /build/dc3-center/dc3-center-data/target/dc3-center-data.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
EXPOSE ${DC3_DATA_PORT}
EXPOSE ${DC3_DATA_GRPC_PORT}
VOLUME /dc3-center/dc3-center-data/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-center-data.jar"]


# ---------- dc3-center-agentic ----------
FROM runtime-base AS dc3-center-agentic
ENV DC3_AGENTIC_PORT=8600
ENV SERVER_NAME=dc3-center-agentic
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/center/agentic/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/center/agentic/gc/gc-%t.log
WORKDIR /dc3-center/dc3-center-agentic
RUN mkdir -p /dc3-center/dc3-center-agentic/dc3/logs/center/agentic/gc
COPY --from=builder /build/dc3-center/dc3-center-agentic/target/dc3-center-agentic.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
EXPOSE ${DC3_AGENTIC_PORT}
VOLUME /dc3-center/dc3-center-agentic/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-center-agentic.jar"]


# ---------- dc3-center-single ----------
FROM runtime-base AS dc3-center-single
ENV DC3_SINGLE_PORT=8100
ENV DC3_SINGLE_GRPC_PORT=9100
ENV SERVER_NAME=dc3-center-single
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/center/single/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/center/single/gc/gc-%t.log
WORKDIR /dc3-center/dc3-center-single
RUN mkdir -p /dc3-center/dc3-center-single/dc3/logs/center/single/gc
COPY --from=builder /build/dc3-center/dc3-center-single/target/dc3-center-single.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
EXPOSE ${DC3_SINGLE_PORT}
EXPOSE ${DC3_SINGLE_GRPC_PORT}
VOLUME /dc3-center/dc3-center-single/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-center-single.jar"]


# ---------- dc3-driver-listening-virtual ----------
FROM runtime-base AS dc3-driver-listening-virtual
ENV TCP_PORT=6270
ENV UDP_PORT=6271
ENV SERVER_NAME=dc3-driver-listening-virtual
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/driver/listening-virtual/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/driver/listening-virtual/gc/gc-%t.log
WORKDIR /dc3-driver/dc3-driver-listening-virtual
RUN mkdir -p /dc3-driver/dc3-driver-listening-virtual/dc3/logs/driver/listening-virtual/gc
COPY --from=builder /build/dc3-driver/dc3-driver-listening-virtual/target/dc3-driver-listening-virtual.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
EXPOSE ${TCP_PORT}
EXPOSE ${UDP_PORT}
VOLUME /dc3-driver/dc3-driver-listening-virtual/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-driver-listening-virtual.jar"]


# ---------- dc3-driver-modbus-tcp ----------
FROM runtime-base AS dc3-driver-modbus-tcp
ENV SERVER_NAME=dc3-driver-modbus-tcp
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/driver/modbus-tcp/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/driver/modbus-tcp/gc/gc-%t.log
WORKDIR /dc3-driver/dc3-driver-modbus-tcp
RUN mkdir -p /dc3-driver/dc3-driver-modbus-tcp/dc3/logs/driver/modbus-tcp/gc
COPY --from=builder /build/dc3-driver/dc3-driver-modbus-tcp/target/dc3-driver-modbus-tcp.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
VOLUME /dc3-driver/dc3-driver-modbus-tcp/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-driver-modbus-tcp.jar"]


# ---------- dc3-driver-modbus-rtu ----------
FROM runtime-base AS dc3-driver-modbus-rtu
ENV SERVER_NAME=dc3-driver-modbus-rtu
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/driver/modbus-rtu/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/driver/modbus-rtu/gc/gc-%t.log
WORKDIR /dc3-driver/dc3-driver-modbus-rtu
RUN mkdir -p /dc3-driver/dc3-driver-modbus-rtu/dc3/logs/driver/modbus-rtu/gc
COPY --from=builder /build/dc3-driver/dc3-driver-modbus-rtu/target/dc3-driver-modbus-rtu.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
VOLUME /dc3-driver/dc3-driver-modbus-rtu/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-driver-modbus-rtu.jar"]


# ---------- dc3-driver-mqtt ----------
FROM runtime-base AS dc3-driver-mqtt
ENV SERVER_NAME=dc3-driver-mqtt
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/driver/mqtt/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/driver/mqtt/gc/gc-%t.log
WORKDIR /dc3-driver/dc3-driver-mqtt
RUN mkdir -p /dc3-driver/dc3-driver-mqtt/dc3/logs/driver/mqtt/gc
COPY --from=builder /build/dc3-driver/dc3-driver-mqtt/target/dc3-driver-mqtt.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
VOLUME /dc3-driver/dc3-driver-mqtt/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-driver-mqtt.jar"]


# ---------- dc3-driver-opc-da ----------
FROM runtime-base AS dc3-driver-opc-da
ENV SERVER_NAME=dc3-driver-opc-da
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/driver/opc-da/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/driver/opc-da/gc/gc-%t.log
WORKDIR /dc3-driver/dc3-driver-opc-da
RUN mkdir -p /dc3-driver/dc3-driver-opc-da/dc3/logs/driver/opc-da/gc
COPY --from=builder /build/dc3-driver/dc3-driver-opc-da/target/dc3-driver-opc-da.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
VOLUME /dc3-driver/dc3-driver-opc-da/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-driver-opc-da.jar"]


# ---------- dc3-driver-opc-ua ----------
FROM runtime-base AS dc3-driver-opc-ua
ENV SERVER_NAME=dc3-driver-opc-ua
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/driver/opc-ua/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/driver/opc-ua/gc/gc-%t.log
WORKDIR /dc3-driver/dc3-driver-opc-ua
RUN mkdir -p /dc3-driver/dc3-driver-opc-ua/dc3/logs/driver/opc-ua/gc
COPY --from=builder /build/dc3-driver/dc3-driver-opc-ua/target/dc3-driver-opc-ua.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
VOLUME /dc3-driver/dc3-driver-opc-ua/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-driver-opc-ua.jar"]


# ---------- dc3-driver-plcs7 ----------
FROM runtime-base AS dc3-driver-plcs7
ENV SERVER_NAME=dc3-driver-plcs7
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/driver/plcs7/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/driver/plcs7/gc/gc-%t.log
WORKDIR /dc3-driver/dc3-driver-plcs7
RUN mkdir -p /dc3-driver/dc3-driver-plcs7/dc3/logs/driver/plcs7/gc
COPY --from=builder /build/dc3-driver/dc3-driver-plcs7/target/dc3-driver-plcs7.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
VOLUME /dc3-driver/dc3-driver-plcs7/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-driver-plcs7.jar"]


# ---------- dc3-driver-virtual ----------
FROM runtime-base AS dc3-driver-virtual
ENV SERVER_NAME=dc3-driver-virtual
ENV JAVA_HEAP_DUMP_PATH=dc3/logs/driver/virtual/gc/dump.hprof
ENV JAVA_GC_LOG_PATH=dc3/logs/driver/virtual/gc/gc-%t.log
WORKDIR /dc3-driver/dc3-driver-virtual
RUN mkdir -p /dc3-driver/dc3-driver-virtual/dc3/logs/driver/virtual/gc
COPY --from=builder /build/dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar ./
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh
VOLUME /dc3-driver/dc3-driver-virtual/dc3/logs
ENTRYPOINT ["./entrypoint.sh"]
CMD ["dc3-driver-virtual.jar"]
