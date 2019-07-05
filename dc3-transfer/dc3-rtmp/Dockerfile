FROM anapsix/alpine-java:8_server-jre_unlimited AS base

#修改时区，兼容CentOS7、RHEL7、Scientific Linux 7、Oracle Linux 7
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

FROM pnoker/dc3-base:3.0 AS buid

### Release Stage
FROM base AS release

MAINTAINER pnoker pnokers.icloud.com

RUN mkdir -p /dc3-transfer/dc3-rtmp

WORKDIR /dc3-transfer/dc3-rtmp

EXPOSE 8830

ADD ./target/lib ./lib
ADD ./target/classes ./classes
ADD ./target/dc3-rtmp.jar.original ./
COPY --from=build /usr/local /usr/local

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-rtmp.jar.original

