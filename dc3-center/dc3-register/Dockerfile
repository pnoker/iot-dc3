FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER pnokers.icloud.com

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

RUN mkdir -p /dc3-center/dc3-eureka

WORKDIR /dc3-center/dc3-eureka

EXPOSE 8100

ADD ./dc3-center/dc3-eureka/target/dc3-eureka.jar ./

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-eureka.jar
