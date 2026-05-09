/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * Controller
 *
 * @author pnoker
 * @version 2025.9.0
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
     * Profile
     *
     * @param entityVO {@link ProfileVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody ProfileVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            ProfileBO entityBO = profileBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            profileService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID Profile
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            profileService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        });
    }

    /**
     * Profile
     *
     * @param entityVO {@link ProfileVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody ProfileVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            ProfileBO entityBO = profileBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            profileService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID Profile
     *
     * @param id ID
     * @return ProfileVO {@link ProfileVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<ProfileVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            ProfileBO entityBO = profileService.selectById(id);
            ProfileVO entityVO = profileBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    /**
     * ID Profile
     *
     * @param profileIds ID
     * @return Map(ID, ProfileVO)
     */
    @PostMapping("/ids")
    public Mono<R<Map<Long, ProfileVO>>> selectByIds(@RequestBody Set<Long> profileIds) {
        return async(() -> {
            List<ProfileBO> entityBOList = profileService.selectByIds(profileIds);
            Map<Long, ProfileVO> deviceMap = entityBOList.stream()
                    .collect(Collectors.toMap(ProfileBO::getId, entityBO -> profileBuilder.buildVOByBO(entityBO)));
            return R.ok(deviceMap);
        });
    }

    /**
     * Device ID Profile
     *
     * @param deviceId Device ID
     * @return Profile
     */
    @GetMapping("/device_id/{deviceId}")
    public Mono<R<List<ProfileVO>>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        return async(() -> {
            List<ProfileBO> entityBOList = profileService.selectByDeviceId(deviceId);
            List<ProfileVO> entityVOList = profileBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        });
    }

    /**
     * Profile
     *
     * @param entityQuery Profile Dto
     * @return Page Of Profile
     */
    @PostMapping("/list")
    public Mono<R<Page<ProfileVO>>> list(@RequestBody(required = false) ProfileQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            ProfileQuery query = Objects.isNull(entityQuery) ? new ProfileQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<ProfileBO> entityPageBO = profileService.selectByPage(query);
            Page<ProfileVO> entityPageVO = profileBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
