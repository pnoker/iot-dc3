/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.api.center.auth.fallback;

import io.github.pnoker.api.center.auth.feign.TokenClient;
import io.github.pnoker.common.bean.auth.Login;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.utils.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * TokenClientFallback
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class TokenClientFallback implements FallbackFactory<TokenClient> {

    @Override
    public TokenClient create(Throwable throwable) {
        String message = ExceptionUtil.getNotAvailableServiceMessage(AuthServiceConstant.SERVICE_NAME, throwable.getMessage());
        log.error("Fallback:{}", message);

        return new TokenClient() {

            /**
             * {@inheritDoc}
             */
            @Override
            public R<String> generateSalt(Login login) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<String> generateToken(Login login) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<String> checkTokenValid(Login login) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Boolean> cancelToken(Login login) {
                return R.fail(message);
            }

        };
    }
}