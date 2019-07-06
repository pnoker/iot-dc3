FROM anapsix/alpine-java:8_jdk_unlimited AS base
MAINTAINER pnoker <pnokers.icloud.com>

# 设置为国内下载源
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && sed -i 's/dl-cdn.alpinelinux.org/mirrors.ustc.edu.cn/g' /etc/apk/repositories \
    && apk add --no-cache --update --update-cache bash ca-certificates libgomp openssl-dev gcc libc-dev pcre-dev make expat curl git \
