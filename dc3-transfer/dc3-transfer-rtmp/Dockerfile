#
# Copyright (c) 2022. Pnoker. All Rights Reserved.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#     http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM registry.cn-beijing.aliyuncs.com/dc3/alpine-java:corretto-11 AS base
MAINTAINER pnoker <pnokers.icloud.com>

ENV JAVA_OPS -server \
             -javaagent:/usr/local/java/lib/aspectjweaver.jar \
             -Xms512m \
             -Xmx2048m \
             -Xss1m \
             -Djava.security.egd=file:/dev/./urandom \
             -XX:MaxDirectMemorySize=256m \
             -XX:+UseCompressedOops \
             -XX:+UseCompressedClassPointers \
             -XX:+SegmentedCodeCache \
             -XX:+PrintCommandLineFlags
ENV GC_LOG   -verbose:gc \
             -XX:+UseG1GC \
             -XX:+ExplicitGCInvokesConcurrent \
             -Xlog:gc*,safepoint:dc3/logs/transfer/rtmp/gc/dc3-transfer-rtmp-gc.log

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

FROM jrottenberg/ffmpeg:4.1-alpine AS build

FROM base AS release

WORKDIR /dc3-transfer/dc3-transfer-rtmp

ADD ./target/dc3-transfer-rtmp.jar ./
COPY --from=build /usr/local /usr/local

EXPOSE 8803
VOLUME /dc3-transfer/dc3-transfer-rtmp/dc3/logs

CMD mkdir -p /dc3-transfer/dc3-transfer-rtmp/dc3/logs/transfer/rtmp/gc \
    && java ${JAVA_OPS} ${GC_LOG} -jar dc3-transfer-rtmp.jar

