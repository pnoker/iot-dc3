FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER pnokers.icloud.com

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

RUN mkdir -p /dc3-center/dc3-dbs

WORKDIR /dc3-center/dc3-dbs

EXPOSE 8300

ADD ./dc3-center/dc3-dbs/target/dc3-dbs.jar ./

CMD java -Djava.security.egd=file:/dev/./urandom -jar dc3-dbs.jar
