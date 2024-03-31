/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.center.manager.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.model.AgingConfigDO;
import io.github.pnoker.center.manager.service.AgingConfigService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * AgingConfig Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping("/aging_config")
public class AgingConfigController implements BaseController {

    @Resource
    private AgingConfigService agingConfigService;

    /**
     * 新增
     *
     * @param entityDO {@link AgingConfigDO}
     * @return R of String
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Add.class) @RequestBody AgingConfigDO entityDO) {
        try {
            agingConfigService.save(entityDO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            agingConfigService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新
     *
     * @param entityDO {@link AgingConfigDO}
     * @return R of String
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody AgingConfigDO entityDO) {
        try {
            agingConfigService.update(entityDO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 单个查询
     *
     * @param id ID
     * @return AgingConfigDO {@link AgingConfigDO}
     */
    @GetMapping("/id/{id}")
    public R<AgingConfigDO> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            AgingConfigDO entityDO = agingConfigService.selectById(id);
            return R.ok(entityDO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询
     *
     * @param entityQuery {@link AgingConfigDO}
     * @return R Of AgingConfigDO Page
     */
    @PostMapping("/list")
    public R<Page<AgingConfigDO>> list(@RequestBody(required = false) AgingConfigDO entityQuery) {
        try {
            if (ObjectUtil.isEmpty(entityQuery)) {
                entityQuery = new AgingConfigDO();
            }
            Page<AgingConfigDO> entityPageDO = agingConfigService.selectByPage(entityQuery);
            return R.ok(entityPageDO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

}
