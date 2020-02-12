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

package com.pnoker.api.center.manager.hystrix;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.manager.feign.ScheduleClient;
import com.pnoker.common.bean.R;
import com.pnoker.common.dto.ScheduleDto;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.Schedule;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p> Schedule FeignHystrix
 *
 * @author pnoker
 */
@Slf4j
@Component
public class ScheduleClientHystrix implements FallbackFactory<ScheduleClient> {

    @Override
    public ScheduleClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-MANAGER" : throwable.getMessage();
        log.error("ScheduleClientHystrix:{},hystrix服务降级处理", message, throwable);

        return new ScheduleClient() {

            @Override
            public R<Schedule> add(Schedule schedule) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> delete(Long id) {
                return R.fail(message);
            }

            @Override
            public R<Schedule> update(Schedule schedule) {
                return R.fail(message);
            }

            @Override
            public R<Schedule> selectById(Long id) {
                return R.fail(message);
            }

            @Override
            public R<Schedule> selectByName(String name) {
                return R.fail(message);
            }

            @Override
            public R<Page<Schedule>> list(ScheduleDto scheduleDto) {
                return R.fail(message);
            }

            @Override
            public R<List<Dic>> dictionary() {
                return R.fail(message);
            }
        };
    }
}