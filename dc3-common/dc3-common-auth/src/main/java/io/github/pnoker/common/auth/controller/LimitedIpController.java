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

package io.github.pnoker.common.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.LimitedIpBO;
import io.github.pnoker.common.auth.entity.builder.LimitedIpBuilder;
import io.github.pnoker.common.auth.entity.query.LimitedIpQuery;
import io.github.pnoker.common.auth.entity.vo.LimitedIpVO;
import io.github.pnoker.common.auth.service.LimitedIpService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 限制IP Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(value = AuthConstant.LIMITED_IP_URL_PREFIX)
public class LimitedIpController implements BaseController {

    private final LimitedIpBuilder limitedIpBuilder;
    private final LimitedIpService limitedIpService;

    public LimitedIpController(LimitedIpBuilder limitedIpBuilder, LimitedIpService limitedIpService) {
        this.limitedIpBuilder = limitedIpBuilder;
        this.limitedIpService = limitedIpService;
    }

    /**
     * 新增 LimitedIp
     *
     * @param entityVO {@link LimitedIpVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody LimitedIpVO entityVO) {
        try {
            LimitedIpBO entityBO = limitedIpBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            limitedIpService.save(entityBO);
            return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 删除 LimitedIp
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            limitedIpService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
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
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody LimitedIpVO entityVO) {
        try {
            LimitedIpBO entityBO = limitedIpBuilder.buildBOByVO(entityVO);
            limitedIpService.update(entityBO);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 查询 LimitedIp
     *
     * @param id ID
     * @return LimitedIpVO {@link LimitedIpVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<LimitedIpVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            LimitedIpBO entityBO = limitedIpService.selectById(id);
            LimitedIpVO entityVO = limitedIpBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 Ip 查询 LimitedIp
     *
     * @param ip Limited Ip
     * @return {@link LimitedIpBO}
     */
    @GetMapping("/ip/{ip}")
    public Mono<R<LimitedIpVO>> selectByIp(@NotNull @PathVariable(value = "ip") String ip) {
        try {
            LimitedIpBO entityBO = limitedIpService.selectByIp(ip);
            LimitedIpVO entityVO = limitedIpBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 分页查询 LimitedIp
     *
     * @param entityQuery LimitedIp和分页参数
     * @return 带分页的 {@link LimitedIpBO}
     */
    @PostMapping("/list")
    public Mono<R<Page<LimitedIpVO>>> list(@RequestBody(required = false) LimitedIpQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new LimitedIpQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<LimitedIpBO> entityPageBO = limitedIpService.selectByPage(entityQuery);
            Page<LimitedIpVO> entityPageVO = limitedIpBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 检测 Ip 是否在限制IP列表
     *
     * @param ip Limited Ip
     * @return 当前IP是否在限制IP中
     */
    @GetMapping("/check/{ip}")
    public Mono<R<Boolean>> checkValid(@NotNull @PathVariable(value = "ip") String ip) {
        try {
            return Boolean.TRUE.equals(limitedIpService.checkValid(ip)) ? Mono.just(R.ok()) : Mono.just(R.fail());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
