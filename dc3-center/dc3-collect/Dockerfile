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

FROM anapsix/alpine-java:8_server-jre_unlimited
MAINTAINER pnoker <pnokers.icloud.com>

#修改时区，兼容CentOS7、RHEL7、Scientific Linux 7、Oracle Linux 7
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-center/dc3-collect

WORKDIR /dc3-center/dc3-collect

ADD ./target/dc3-collect.jar ./

EXPOSE 8500

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-collect.jar
