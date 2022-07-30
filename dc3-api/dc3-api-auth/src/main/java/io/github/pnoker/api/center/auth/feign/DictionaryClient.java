/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.api.center.auth.feign;

import io.github.pnoker.api.center.auth.fallback.DictionaryClientFallback;
import io.github.pnoker.common.bean.Dictionary;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * 字典 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = ServiceConstant.Auth.DICTIONARY_URL_PREFIX, name = ServiceConstant.Auth.SERVICE_NAME, fallbackFactory = DictionaryClientFallback.class)
public interface DictionaryClient {

    /**
     * 查询租户 Dictionary
     *
     * @return List<Dictionary>
     */
    @GetMapping("/tenant")
    R<List<Dictionary>> tenantDictionary();

    /**
     * 查询用户 Dictionary
     *
     * @return List<Dictionary>
     */
    @GetMapping("/user")
    R<List<Dictionary>> userDictionary(@RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 查询 Ip 黑名单 Dictionary
     *
     * @return List<Dictionary>
     */
    @GetMapping("/black_ip")
    R<List<Dictionary>> blackIpDictionary(@RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

}
