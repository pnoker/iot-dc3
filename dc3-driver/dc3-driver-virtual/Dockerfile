#
#  Copyright 2018-2020 Pnoker. All Rights Reserved.
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

FROM pnoker/alpine-java:1.8.251
MAINTAINER pnoker <pnokers.icloud.com>

ENV JAVA_OPS -server -Xms128m -Xmx512m -XX:CompressedClassSpaceSize=128m -XX:MetaspaceSize=200m -XX:MaxMetaspaceSize=200m

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-driver/dc3-driver-virtual \
    && mkdir -p /dc3-driver/dc3-driver-virtual/dc3/logs

WORKDIR /dc3-driver/dc3-driver-virtual

ADD ./target/dc3-driver-virtual.jar ./

EXPOSE 8600
VOLUME /dc3-driver/dc3-driver-virtual/dc3/logs

CMD java ${JAVA_OPS} -Djava.security.egd=file:/dev/./urandom -jar dc3-driver-virtual.jar
