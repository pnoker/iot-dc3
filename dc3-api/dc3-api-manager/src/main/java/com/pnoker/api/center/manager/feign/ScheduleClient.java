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

package com.pnoker.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.manager.hystrix.ScheduleClientHystrix;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.ScheduleDto;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.Schedule;
import com.pnoker.common.valid.Insert;
import com.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * <p>调度 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_MANAGER_SCHEDULE_URL_PREFIX, name = Common.Service.DC3_MANAGER, fallbackFactory = ScheduleClientHystrix.class)
public interface ScheduleClient {

    /**
     * 新增 Schedule
     *
     * @param schedule
     * @return Schedule
     */
    @PostMapping("/add")
    R<Schedule> add(@Validated(Insert.class) @RequestBody Schedule schedule);

    /**
     * 根据 ID 删除 Schedule
     *
     * @param id scheduleId
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 Schedule
     *
     * @param schedule
     * @return Schedule
     */
    @PostMapping("/update")
    R<Schedule> update(@Validated(Update.class) @RequestBody Schedule schedule);

    /**
     * 根据 ID 查询 Schedule
     *
     * @param id
     * @return Schedule
     */
    @GetMapping("/id/{id}")
    R<Schedule> selectById(@PathVariable(value = "id") Long id);

    /**
     * 分页查询 Schedule
     *
     * @param scheduleDto
     * @return Page<Schedule>
     */
    @PostMapping("/list")
    R<Page<Schedule>> list(@RequestBody(required = false) ScheduleDto scheduleDto);

    /**
     * 查询 Schedule 字典
     *
     * @return List<Schedule>
     */
    @GetMapping("/dictionary")
    R<List<Dic>> dictionary();
}
