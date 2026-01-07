#
# Copyright 2016-present the IoT DC3 original author or authors.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

# Runtime stage
FROM pnoker/dc3-jre:2025.9 AS runtime
LABEL dc3.author=pnoker
LABEL dc3.author.email=pnokers.icloud.com

# Default environment variables
ENV PARAMS=''
ENV NODE_ENV=test
ENV SERVER_PORT=8100
ENV SERVER_NAME=dc3-center-single
ENV SERVER_PACKAGES=io.github.pnoker
ENV APM_AGENT_ENABLE=false
ENV APM_SERVICE=http://dc3-apm:9300
ENV GRPC_PORT=9100

# Default JVM and GC environment variables
ENV JAVA_OPS="-Xms256m \
              -Xmx512m \
              -XX:+UseG1GC-XX:+AlwaysPreTouch \
              -XX:+HeapDumpOnOutOfMemoryError \
              -XX:HeapDumpPath=dc3/logs/center/single/gc/dump.hprof"
ENV GC_LOG="-Xlog:gc*:file=dc3/logs/center/single/gc/gc-%t.log:time,uptime,level,tags:filecount=20,filesize=10M"

# Set working directory
WORKDIR /dc3-center/dc3-center-single

# Set timezone to Asia/Shanghai and create log directory
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-center/dc3-center-single/dc3/logs/center/single/gc

# Copy application JAR file
COPY target/dc3-center-single.jar ./

# Copy entrypoint script
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh

# Expose server and gRPC ports
EXPOSE ${SERVER_PORT}
EXPOSE ${GRPC_PORT}
# Mount logs volume
VOLUME /dc3-center/dc3-center-single/dc3/logs

# Set entrypoint script
ENTRYPOINT ["./entrypoint.sh"]

# Default command
CMD ["dc3-center-single.jar"]
