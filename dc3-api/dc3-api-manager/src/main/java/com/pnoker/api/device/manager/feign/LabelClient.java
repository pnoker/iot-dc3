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

package com.pnoker.api.device.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.device.manager.hystrix.LabelClientHystrix;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.LabelDto;
import com.pnoker.common.model.Label;
import com.pnoker.common.valid.Insert;
import com.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>设备分组 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_MANAGER_POINT_URL_PREFIX, name = Common.Service.DC3_MANAGER, fallbackFactory = LabelClientHystrix.class)
public interface LabelClient {

    /**
     * 新增 Label 记录
     *
     * @param label
     * @return Label
     */
    @PostMapping("/add")
    R<Label> add(@Validated(Insert.class) @RequestBody Label label);

    /**
     * 根据 ID 删除 Label
     *
     * @param id labelId
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 Label 记录
     *
     * @param label
     * @return Label
     */
    @PostMapping("/update")
    R<Label> update(@Validated(Update.class) @RequestBody Label label);

    /**
     * 根据 ID 查询 Label
     *
     * @param id
     * @return Label
     */
    @GetMapping("/id/{id}")
    R<Label> selectById(@PathVariable(value = "id") Long id);

    /**
     * 根据 Name 查询 Label
     *
     * @param name
     * @return Label
     */
    @GetMapping("/name/{name}")
    R<Label> selectByName(@PathVariable(value = "name") String name);

    /**
     * 分页查询 Label
     *
     * @param labelDto
     * @return Page<Label>
     */
    @PostMapping("/list")
    R<Page<Label>> list(@RequestBody(required = false) LabelDto labelDto);

}
