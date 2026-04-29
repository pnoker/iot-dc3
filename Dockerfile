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

# Use JDK 21 base image
FROM pnoker/dc3-jdk:21
LABEL dc3.author=pnoker
LABEL dc3.author.email=pnokers.icloud.com

# Build profile argument (default: dev)
ARG PROFILE=dev

# Set working directory
WORKDIR /build

# Set timezone to Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# Copy project files
COPY ./ ./

# Build project with Maven (skip tests)
RUN mvn -U -e -B -s .mvn/settings.xml clean package -DskipTests -P ${PROFILE}
