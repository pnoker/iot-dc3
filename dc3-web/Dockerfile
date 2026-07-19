#
# Copyright 2016-present the IoT DC3 original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# builder
# Build context is the repository root so that dc3/dependencies/ (nginx
# configs, SSL certs) is reachable at runtime.
FROM pnoker/dc3-node:2025.9 AS builder
LABEL dc3.author=pnokers
LABEL dc3.author.email=pnokers.icloud.com

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

WORKDIR /build

COPY dc3-web/ ./

RUN corepack enable && corepack prepare pnpm@11.3.0 --activate
RUN pnpm install --frozen-lockfile
RUN pnpm build

# runtime
FROM pnoker/dc3-nginx:2025.2 AS runtime
LABEL dc3.author=pnokers
LABEL dc3.author.email=pnokers.icloud.com

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

COPY dc3/dependencies/nginx/ /etc/nginx/
COPY --from=builder /build/dist/ /usr/share/nginx/html/
# nginx.conf references /etc/letsencrypt/live/dc3.site/ (the acme.sh layout),
# so the certs shipped under nginx/certs/ are placed there.
COPY dc3/dependencies/nginx/certs/ /etc/letsencrypt/live/

EXPOSE 80 443
VOLUME /var/log/nginx

CMD envsubst '${APP_API_HOST} ${APP_API_PORT}' < /etc/nginx/location/default.env > /etc/nginx/location/default.conf ; nginx -g "daemon off;"
