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
import io.github.pnoker.center.auth.entity.query.LimitedIpQuery;
import io.github.pnoker.center.auth.service.LimitedIpService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.enums.ResponseEnum;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * Ip 黑名单 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(value = AuthConstant.LIMITED_IP_URL_PREFIX)
public class LimitedIpController implements BaseController {

    @Resource
    private LimitedIpService limitedIpService;

    /**
     * 新增 LimitedIp
     *
     * @param limitedIpBO LimitedIp
     * @return {@link LimitedIpBO}
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Add.class) @RequestBody LimitedIpBO limitedIpBO) {
        try {
            limitedIpService.save(limitedIpBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 LimitedIp
     *
     * @param id ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            limitedIpService.remove(Long.parseLong(id));
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 LimitedIp
     * <ol>
     * <li>支持更新: Enable</li>
     * <li>不支持更新: Ip</li>
     * </ol>
     *
     * @param limitedIpBO LimitedIp
     * @return {@link LimitedIpBO}
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody LimitedIpBO limitedIpBO) {
        try {
            limitedIpService.update(limitedIpBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 LimitedIp
     *
     * @param id ID
     * @return {@link LimitedIpBO}
     */
    @GetMapping("/id/{id}")
    public R<LimitedIpBO> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            LimitedIpBO select = limitedIpService.selectById(Long.parseLong(id));
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

    /**
     * 根据 Ip 查询 LimitedIp
     *
     * @param ip Limited Ip
     * @return {@link LimitedIpBO}
     */
    @GetMapping("/ip/{ip}")
    public R<LimitedIpBO> selectByIp(@NotNull @PathVariable(value = "ip") String ip) {
        try {
            LimitedIpBO select = limitedIpService.selectByIp(ip);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

    /**
     * 分页查询 LimitedIp
     *
     * @param limitedIpPageQuery LimitedIp和分页参数
     * @return 带分页的 {@link LimitedIpBO}
     */
    @PostMapping("/list")
    public R<Page<LimitedIpBO>> list(@RequestBody(required = false) LimitedIpQuery limitedIpPageQuery) {
        try {
            if (ObjectUtil.isEmpty(limitedIpPageQuery)) {
                limitedIpPageQuery = new LimitedIpQuery();
            }
            Page<LimitedIpBO> page = limitedIpService.selectByPage(limitedIpPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

    /**
     * 检测 Ip 是否在 Ip 黑名单列表
     *
     * @param ip Black Ip
     * @return 当前IP是否在黑名单中
     */
    @GetMapping("/check/{ip}")
    public R<Boolean> checkLimitedIpValid(@NotNull @PathVariable(value = "ip") String ip) {
        try {
            return Boolean.TRUE.equals(limitedIpService.checkLimitedIpValid(ip)) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
