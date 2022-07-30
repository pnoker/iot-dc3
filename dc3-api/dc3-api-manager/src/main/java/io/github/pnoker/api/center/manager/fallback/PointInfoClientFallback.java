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

package io.github.pnoker.api.center.manager.fallback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.feign.PointInfoClient;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.dto.PointInfoDto;
import io.github.pnoker.common.model.PointInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PointInfoClientFallback
 *
 * @author pnoker
 */
@Slf4j
@Component
public class PointInfoClientFallback implements FallbackFactory<PointInfoClient> {

    @Override
    public PointInfoClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-CENTER-MANAGER" : throwable.getMessage();
        log.error("Fallback:{}", message);

        return new PointInfoClient() {

            @Override
            public R<PointInfo> add(PointInfo pointInfo) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> delete(String id) {
                return R.fail(message);
            }

            @Override
            public R<PointInfo> update(PointInfo pointInfo) {
                return R.fail(message);
            }

            @Override
            public R<PointInfo> selectById(String id) {
                return R.fail(message);
            }

            @Override
            public R<PointInfo> selectByAttributeIdAndDeviceIdAndPointId(String attributeId, String deviceId, String pointId) {
                return R.fail(message);
            }

            @Override
            public R<List<PointInfo>> selectByDeviceIdAndPointId(String deviceId, String pointId) {
                return R.fail(message);
            }

            @Override
            public R<List<PointInfo>> selectByDeviceId(String deviceId) {
                return R.fail(message);
            }

            @Override
            public R<Page<PointInfo>> list(PointInfoDto pointInfoDto) {
                return R.fail(message);
            }

        };
    }
}