FROM anapsix/alpine-java:8_server-jre_unlimited
MAINTAINER pnoker <pnokers.icloud.com>

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-center/dc3-register

WORKDIR /dc3-center/dc3-register

ADD ./target/dc3-register.jar ./

EXPOSE 8100

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-register.jar
