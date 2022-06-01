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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.feign.PointAttributeClient;
import com.dc3.common.bean.R;
import com.dc3.common.dto.PointAttributeDto;
import com.dc3.common.model.PointAttribute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * PointAttributeClientFallback
 *
 * @author pnoker
 */
@Slf4j
@Component
public class PointAttributeClientFallback implements FallbackFactory<PointAttributeClient> {

    @Override
    public PointAttributeClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-CENTER-MANAGER" : throwable.getMessage();
        log.error("Fallback:{}", message);

        return new PointAttributeClient() {

            @Override
            public R<PointAttribute> add(PointAttribute pointAttribute) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> delete(String id) {
                return R.fail(message);
            }

            @Override
            public R<PointAttribute> update(PointAttribute pointAttribute) {
                return R.fail(message);
            }

            @Override
            public R<PointAttribute> selectById(String id) {
                return R.fail(message);
            }

            @Override
            public R<List<PointAttribute>> selectByDriverId(@NotNull String id) {
                return R.fail(message);
            }

            @Override
            public R<Page<PointAttribute>> list(PointAttributeDto pointAttributeDto) {
                return R.fail(message);
            }

        };
    }
}