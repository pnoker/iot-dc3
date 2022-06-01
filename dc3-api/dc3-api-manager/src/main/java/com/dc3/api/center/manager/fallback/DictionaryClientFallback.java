/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.api.center.manager.fallback;

import com.dc3.api.center.manager.feign.DictionaryClient;
import com.dc3.common.bean.Dictionary;
import com.dc3.common.bean.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DictionaryClientFallback
 *
 * @author pnoker
 */
@Slf4j
@Component
public class DictionaryClientFallback implements FallbackFactory<DictionaryClient> {

    @Override
    public DictionaryClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-CENTER-MANAGER" : throwable.getMessage();
        log.error("Fallback:{}", message);

        return new DictionaryClient() {

            @Override
            public R<List<Dictionary>> driverDictionary(String tenantId) {
                return R.fail(message);
            }

            @Override
            public R<List<Dictionary>> driverAttributeDictionary(String tenantId) {
                return R.fail(message);
            }

            @Override
            public R<List<Dictionary>> pointAttributeDictionary(String tenantId) {
                return R.fail(message);
            }

            @Override
            public R<List<Dictionary>> profileDictionary(String tenantId) {
                return R.fail(message);
            }

            @Override
            public R<List<Dictionary>> deviceDictionary(String tenantId) {
                return R.fail(message);
            }

            @Override
            public R<List<Dictionary>> pointDictionary(String parent, String tenantId) {
                return R.fail(message);
            }
        };
    }
}