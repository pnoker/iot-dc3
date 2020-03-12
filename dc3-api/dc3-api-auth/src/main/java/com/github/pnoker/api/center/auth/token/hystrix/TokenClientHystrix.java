/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pnoker.api.center.auth.token.hystrix;

import com.github.pnoker.api.center.auth.token.feign.TokenClient;
import com.github.pnoker.common.bean.R;
import com.github.pnoker.common.model.User;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>TokenClientHystrix
 *
 * @author pnoker
 */
@Slf4j
@Component
public class TokenClientHystrix implements FallbackFactory<TokenClient> {

    @Override
    public TokenClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-AUTH" : throwable.getMessage();
        log.error("Hystrix:{}", message, throwable);

        return new TokenClient() {

            @Override
            public R<String> generateToken(User user) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> checkTokenValid(String token) {
                return R.fail(message);
            }

        };
    }
}