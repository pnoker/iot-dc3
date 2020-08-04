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

FROM mysql:5.7
MAINTAINER pnoker <pnokers.icloud.com>

ENV MYSQL_ROOT_PASSWORD dc3

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

COPY ./iot-dc3.sql /docker-entrypoint-initdb.d