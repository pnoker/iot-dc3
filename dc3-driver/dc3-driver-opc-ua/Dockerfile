#
# Copyright 2016-present the IoT DC3 original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# runtime
FROM pnoker/dc3-jre:2025.2 AS runtime
LABEL dc3.author=pnoker
LABEL dc3.author.email=pnokers.icloud.com

# default env
ENV PARAMS=''
ENV NODE_ENV=test

# default jvm & gc env
ENV JAVA_OPS="-server \
              -Xms256m \
              -Xmx512m \
              -Djava.security.egd=file:/dev/./urandom \
              -XX:+UseG1GC"

WORKDIR /dc3-driver/dc3-driver-opc-ua

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-driver/dc3-driver-opc-ua/dc3/logs/driver/opcua/gc

COPY target/dc3-driver-opc-ua.jar ./

RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh

VOLUME /dc3-driver/dc3-driver-opc-ua/dc3/logs

ENTRYPOINT ["./entrypoint.sh"]

CMD ["dc3-driver-opc-ua.jar"]
