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

package io.github.pnoker.center.auth.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.bo.LimitedIpBO;
import io.github.pnoker.center.auth.entity.builder.LimitedIpBuilder;
import io.github.pnoker.center.auth.entity.query.LimitedIpQuery;
import io.github.pnoker.center.auth.entity.vo.LimitedIpVO;
import io.github.pnoker.center.auth.service.LimitedIpService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 限制IP Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@Tag(name = "接口-限制IP")
@RequestMapping(value = AuthConstant.LIMITED_IP_URL_PREFIX)
public class LimitedIpController implements BaseController {

    @Resource
    private LimitedIpBuilder limitedIpBuilder;

    @Resource
    private LimitedIpService limitedIpService;

    /**
     * 新增 LimitedIp
     *
     * @param entityVO {@link LimitedIpVO}
     * @return R of String
     */
    @PostMapping("/add")
    @Operation(summary = "新增-限制IP")
    public R<String> add(@Validated(Add.class) @RequestBody LimitedIpVO entityVO) {
        try {
            LimitedIpBO entityBO = limitedIpBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            limitedIpService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 LimitedIp
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            limitedIpService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新限制IP
     * <ol>
     * <li>支持更新: Enable</li>
     * <li>不支持更新: Ip</li>
     * </ol>
     *
     * @param entityVO {@link LimitedIpVO}
     * @return R of String
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody LimitedIpVO entityVO) {
        try {
            LimitedIpBO entityBO = limitedIpBuilder.buildBOByVO(entityVO);
            limitedIpService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 LimitedIp
     *
     * @param id ID
     * @return LimitedIpVO {@link LimitedIpVO}
     */
    @GetMapping("/id/{id}")
    public R<LimitedIpVO> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            LimitedIpBO entityBO = limitedIpService.selectById(id);
            LimitedIpVO entityVO = limitedIpBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 Ip 查询 LimitedIp
     *
     * @param ip Limited Ip
     * @return {@link LimitedIpBO}
     */
    @GetMapping("/ip/{ip}")
    public R<LimitedIpVO> selectByIp(@NotNull @PathVariable(value = "ip") String ip) {
        try {
            LimitedIpBO entityBO = limitedIpService.selectByIp(ip);
            LimitedIpVO entityVO = limitedIpBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 LimitedIp
     *
     * @param entityQuery LimitedIp和分页参数
     * @return 带分页的 {@link LimitedIpBO}
     */
    @PostMapping("/list")
    public R<Page<LimitedIpVO>> list(@RequestBody(required = false) LimitedIpQuery entityQuery) {
        try {
            if (ObjectUtil.isEmpty(entityQuery)) {
                entityQuery = new LimitedIpQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<LimitedIpBO> entityPageBO = limitedIpService.selectByPage(entityQuery);
            Page<LimitedIpVO> entityPageVO = limitedIpBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 检测 Ip 是否在限制IP列表
     *
     * @param ip Limited Ip
     * @return 当前IP是否在限制IP中
     */
    @GetMapping("/check/{ip}")
    public R<Boolean> checkValid(@NotNull @PathVariable(value = "ip") String ip) {
        try {
            return Boolean.TRUE.equals(limitedIpService.checkValid(ip)) ? R.ok() : R.fail();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

}
