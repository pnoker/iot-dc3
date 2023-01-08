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

package io.github.pnoker.api.driver.fallback;

import io.github.pnoker.api.driver.feign.DriverCommandClient;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.bean.driver.CmdParameter;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.constant.service.DriverServiceConstant;
import io.github.pnoker.common.utils.ExceptionUtil;
import io.github.pnoker.common.bean.ValidatableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DriverCommandFallback
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverCommandFallback implements FallbackFactory<DriverCommandClient> {

    @Override
    public DriverCommandClient create(Throwable throwable) {
        String message = ExceptionUtil.getNotAvailableServiceMessage(DriverServiceConstant.SERVICE_NAME, throwable.getMessage());
        log.error("Fallback:{}", message);


        return new DriverCommandClient() {

            /**
             * {@inheritDoc}
             */
            @Override
            public R<List<PointValue>> read(ValidatableList<CmdParameter> cmdParameters) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Boolean> write(ValidatableList<CmdParameter> cmdParameters) {
                return R.fail(message);
            }
        };
    }
}