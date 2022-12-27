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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.auth.feign.BlackIpClient;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.api.center.auth.dto.BlackIpDto;
import io.github.pnoker.common.entity.BlackIp;
import io.github.pnoker.common.utils.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * BlackIpClientFallback
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class BlackIpClientFallback implements FallbackFactory<BlackIpClient> {

    @Override
    public BlackIpClient create(Throwable throwable) {
        String message = ExceptionUtil.getNotAvailableServiceMessage(AuthServiceConstant.SERVICE_NAME, throwable.getMessage());
        log.error("Fallback:{}", message);

        return new BlackIpClient() {

            /**
             * {@inheritDoc}
             */
            @Override
            public R<BlackIp> add(BlackIp blackIp) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Boolean> delete(String id) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<BlackIp> update(BlackIp blackIp) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<BlackIp> selectById(String id) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<BlackIp> selectByIp(String ip) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Page<BlackIp>> list(BlackIpDto blackIpDto) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Boolean> checkBlackIpValid(String ip) {
                return R.fail(message);
            }
        };
    }
}