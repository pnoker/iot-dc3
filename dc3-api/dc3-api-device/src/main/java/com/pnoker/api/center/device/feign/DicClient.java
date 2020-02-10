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

package com.pnoker.api.center.device.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.device.hystrix.DicClientHystrix;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.DicDto;
import com.pnoker.common.model.Dic;
import com.pnoker.common.valid.Insert;
import com.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>字典 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_DEVICE_DIC_URL_PREFIX, name = Common.Service.DC3_DEVICE, fallbackFactory = DicClientHystrix.class)
public interface DicClient {

    /**
     * 新增 Dic 记录
     *
     * @param dic
     * @return Dic
     */
    @PostMapping("/add")
    R<Dic> add(@Validated(Insert.class) @RequestBody Dic dic);

    /**
     * 根据 ID 删除 Dic
     *
     * @param id dicId
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 Dic 记录
     *
     * @param dic
     * @return Dic
     */
    @PostMapping("/update")
    R<Dic> update(@Validated(Update.class) @RequestBody Dic dic);

    /**
     * 根据 ID 查询 Dic
     *
     * @param id
     * @return Dic
     */
    @GetMapping("/id/{id}")
    R<Dic> selectById(@PathVariable(value = "id") Long id);

    /**
     * 根据 Name 查询 Dic
     *
     * @param label
     * @param type
     * @return Dic
     */
    @GetMapping("/label/{label}/type/{type}")
    R<Dic> selectByLabelAndType(@PathVariable(value = "label") String label, @PathVariable(value = "type") String type);

    /**
     * 分页查询 Dic
     *
     * @param dicDto
     * @return Page<Dic>
     */
    @PostMapping("/list")
    R<Page<Dic>> list(@RequestBody(required = false) DicDto dicDto);

}
