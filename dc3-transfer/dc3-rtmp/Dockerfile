#
#  Copyright 2019 Pnoker. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#

FROM anapsix/alpine-java:8_server-jre_unlimited AS base
MAINTAINER pnoker pnokers.icloud.com

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-transfer/dc3-rtmp

FROM jrottenberg/ffmpeg:4.1-alpine AS build

FROM base AS release

WORKDIR /dc3-transfer/dc3-rtmp

ADD ./target/dc3-rtmp.jar ./
COPY --from=build /usr/local /usr/local

EXPOSE 8803

CMD java ${JAVA_OPS} -Djava.security.egd=file:/dev/./urandom -jar dc3-rtmp.jar

