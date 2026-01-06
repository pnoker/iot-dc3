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
ENV SERVER_NAME=dc3-driver-listening-virtual
ENV SERVER_PACKAGES=io.github.pnoker
ENV APM_AGENT_ENABLE=false
ENV APM_SERVICE=http://dc3-apm:9300
ENV TCP_PORT 6270
ENV UDP_PORT 6271

# Default JVM and GC environment variables
ENV JAVA_OPS="-Xms256m \
              -Xmx512m \
              -XX:+UseG1GC-XX:+AlwaysPreTouch \
              -XX:+HeapDumpOnOutOfMemoryError \
              -XX:HeapDumpPath=dc3/logs/driver/listening-virtual/gc/dump.hprof"
ENV GC_LOG="-Xlog:gc*:file=dc3/logs/driver/listening-virtual/gc/gc-%t.log:time,uptime,level,tags:filecount=20,filesize=10M"

# Set working directory
WORKDIR /dc3-driver/dc3-driver-listening-virtual

# Set timezone to Asia/Shanghai and create log directory
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /dc3-driver/dc3-driver-listening-virtual/dc3/logs/driver/listening-virtual/gc

# Copy application JAR file
COPY target/dc3-driver-listening-virtual.jar ./

# Copy entrypoint script
RUN cp /usr/share/dc3/entrypoint.sh ./entrypoint.sh

# Expose TCP and UDP ports
EXPOSE ${TCP_PORT}
EXPOSE ${UDP_PORT}
# Mount logs volume
VOLUME /dc3-driver/dc3-driver-listening-virtual/dc3/logs

# Set entrypoint script
ENTRYPOINT ["./entrypoint.sh"]

# Default command
CMD ["dc3-driver-listening-virtual.jar"]
