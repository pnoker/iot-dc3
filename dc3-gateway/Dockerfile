FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER pnokers.icloud.com

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

RUN mkdir -p /dc3-gateway

WORKDIR /dc3-gateway

EXPOSE 8300

ADD ./dc3-gateway/target/dc3-gateway.jar ./

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-gateway.jar
