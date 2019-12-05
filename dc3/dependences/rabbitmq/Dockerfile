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

FROM rabbitmq:3.8-alpine
MAINTAINER pnoker <pnokers.icloud.com>

ENV RABBITMQ_DEFAULT_USER iotdc3 \
    RABBITMQ_DEFAULT_PASS iotdc3

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && rabbitmq-plugins enable rabbitmq_management rabbitmq_mqtt rabbitmq_stomp