FROM anapsix/alpine-java:8_jdk_unlimited AS base

MAINTAINER pnoker <pnokers.icloud.com>

ARG MAVEN_VERSION=3.6.1
ARG USER_HOME_DIR="/root"
ARG SHA=b4880fb7a3d81edd190a029440cdf17f308621af68475a4fe976296e71ff4a4b546dd6d8a58aaafba334d309cc11e638c52808a4b0e818fc0fd544226d952544
ARG BASE_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries

# 安装 Maven3.6
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /usr/share/maven /usr/share/maven/ref \
    && apk add --no-cache --update libgcc libstdc++ ca-certificates libcrypto1.0 libssl1.0 libgomp openssl expat git curl  \
    && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
    && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
    && rm -f /tmp/apache-maven.tar.gz \
    && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

# 安装 FFmpeg4.1
FROM jrottenberg/ffmpeg:4.1-alpine AS build

### Release Stage
FROM base AS release

MAINTAINER pnoker pnokers.icloud.com

COPY --from=build /usr/local /usr/local
