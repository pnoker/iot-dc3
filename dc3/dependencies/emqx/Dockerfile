#
# Copyright (c) 2022. Pnoker. All Rights Reserved.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#     http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM registry.cn-beijing.aliyuncs.com/dc3/emqx:4.3.6
MAINTAINER pnoker <pnokers.icloud.com>

COPY --chown=emqx:emqx ./emqx.conf /opt/emqx/etc/emqx.conf

COPY --chown=emqx:emqx ./certs/ca.crt /opt/emqx/etc/certs
COPY --chown=emqx:emqx ./certs/server.key /opt/emqx/etc/certs
COPY --chown=emqx:emqx ./certs/server.crt /opt/emqx/etc/certs

COPY --chown=emqx:emqx ./data/* /opt/emqx/data/
COPY --chown=emqx:emqx ./plugins/*.conf /opt/emqx/etc/plugins/