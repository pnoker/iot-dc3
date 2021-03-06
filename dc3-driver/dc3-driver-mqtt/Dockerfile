#
#  Copyright 2016-2021 Pnoker. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

FROM registry.cn-beijing.aliyuncs.com/dc3/alpine-java:1.8.251
MAINTAINER pnoker <pnokers.icloud.com>

ENV JAVA_OPS -server -Xms128m -Xmx1024m -XX:CompressedClassSpaceSize=128m -XX:MetaspaceSize=200m -XX:MaxMetaspaceSize=200m
ENV GC_LOG -XX:+PrintGCDetails \
           -XX:+PrintGCDateStamps \
           -XX:+PrintTenuringDistribution \
           -XX:+PrintHeapAtGC \
           -XX:+PrintReferenceGC \
           -XX:+PrintGCApplicationStoppedTime \
           -XX:+UseGCLogFileRotation \
           -XX:NumberOfGCLogFiles=10 \
           -XX:GCLogFileSize=10M \
           -Xloggc:dc3/logs/driver/mqtt/gc/dc3-driver-mqtt-gc.log

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

WORKDIR /dc3-driver/dc3-driver-mqtt

ADD ./target/dc3-driver-mqtt.jar ./

EXPOSE 8701
VOLUME /dc3-driver/dc3-driver-mqtt/dc3/logs

CMD mkdir -p /dc3-driver/dc3-driver-mqtt/dc3/logs/driver/mqtt/gc \
    && java ${JAVA_OPS} ${GC_LOG} -Djava.security.egd=file:/dev/./urandom -jar dc3-driver-mqtt.jar
