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

package io.github.pnoker.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.ProfileBuilder;
import io.github.pnoker.common.manager.entity.query.ProfileQuery;
import io.github.pnoker.common.manager.entity.vo.ProfileVO;
import io.github.pnoker.common.manager.service.ProfileService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模版 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.PROFILE_URL_PREFIX)
public class ProfileController implements BaseController {

    private final ProfileBuilder profileBuilder;
    private final ProfileService profileService;

    public ProfileController(ProfileBuilder profileBuilder, ProfileService profileService) {
        this.profileBuilder = profileBuilder;
        this.profileService = profileService;
    }

    /**
     * 新增 Profile
     *
     * @param entityVO {@link ProfileVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody ProfileVO entityVO) {
        try {
            ProfileBO entityBO = profileBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            profileService.save(entityBO);
            return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 删除 Profile
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            profileService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 更新 Profile
     *
     * @param entityVO {@link ProfileVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody ProfileVO entityVO) {
        try {
            ProfileBO entityBO = profileBuilder.buildBOByVO(entityVO);
            profileService.update(entityBO);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 查询 Profile
     *
     * @param id ID
     * @return ProfileVO {@link ProfileVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<ProfileVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            ProfileBO entityBO = profileService.selectById(id);
            ProfileVO entityVO = profileBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 集合查询 Profile
     *
     * @param profileIds 模版ID集
     * @return Map(ID, ProfileVO)
     */
    @PostMapping("/ids")
    public Mono<R<Map<Long, ProfileVO>>> selectByIds(@RequestBody Set<Long> profileIds) {
        try {
            List<ProfileBO> entityBOList = profileService.selectByIds(profileIds);
            Map<Long, ProfileVO> deviceMap = entityBOList.stream().collect(Collectors.toMap(ProfileBO::getId, entityBO -> profileBuilder.buildVOByBO(entityBO)));
            return Mono.just(R.ok(deviceMap));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 设备ID 查询 Profile 集合
     *
     * @param deviceId 设备ID
     * @return Profile 集合
     */
    @GetMapping("/device_id/{deviceId}")
    public Mono<R<List<ProfileVO>>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            List<ProfileBO> entityBOList = profileService.selectByDeviceId(deviceId);
            List<ProfileVO> entityVOList = profileBuilder.buildVOListByBOList(entityBOList);
            return Mono.just(R.ok(entityVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 分页查询 Profile
     *
     * @param entityQuery Profile Dto
     * @return Page Of Profile
     */
    @PostMapping("/list")
    public Mono<R<Page<ProfileVO>>> list(@RequestBody(required = false) ProfileQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new ProfileQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<ProfileBO> entityPageBO = profileService.selectByPage(entityQuery);
            Page<ProfileVO> entityPageVO = profileBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
