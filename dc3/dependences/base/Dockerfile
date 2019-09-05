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
 
FROM anapsix/alpine-java:8_jdk_unlimited AS base
MAINTAINER pnoker <pnokers.icloud.com>

# 设置为国内下载源
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && sed -i 's/dl-cdn.alpinelinux.org/mirrors.ustc.edu.cn/g' /etc/apk/repositories \
    && apk add --no-cache --update --update-cache bash ca-certificates libgomp openssl-dev gcc libc-dev pcre-dev make expat curl git \
