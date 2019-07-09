FROM anapsix/alpine-java:8_server-jre_unlimited AS base
MAINTAINER pnoker pnokers.icloud.com

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-transfer/dc3-rtmp

# 安装 FFmpeg4.1
FROM jrottenberg/ffmpeg:4.1-alpine AS build

### Release Stage
FROM base AS release

WORKDIR /dc3-transfer/dc3-rtmp

ADD ./target/dc3-rtmp.jar ./
COPY --from=build /usr/local /usr/local

EXPOSE 8830

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-rtmp.jar

