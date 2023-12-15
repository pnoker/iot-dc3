/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.service;

import io.github.pnoker.center.auth.entity.bo.BlackIpBO;
import io.github.pnoker.center.auth.entity.query.BlackIpQuery;
import io.github.pnoker.common.base.Service;

/**
 * User Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface BlackIpService extends Service<BlackIpBO, BlackIpQuery> {
    /**
     * 根据 Ip 查询 BlackIp
     *
     * @param ip IP
     * @return BlackIp
     */
    BlackIpBO selectByIp(String ip);

    /**
     * 根据 Ip 是否在Ip黑名单列表
     *
     * @param ip IP
     * @return boolean
     */
    Boolean checkBlackIpValid(String ip);
}
