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
import io.github.pnoker.center.auth.entity.bo.BlackIpBO;
import io.github.pnoker.center.auth.entity.query.BlackIpQuery;
import io.github.pnoker.center.auth.service.BlackIpService;
import io.github.pnoker.common.base.Controller;
import io.github.pnoker.common.constant.enums.ResponseEnum;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
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
@RequestMapping(value = AuthServiceConstant.BLACK_IP_URL_PREFIX)
public class BlackIpController implements Controller {

    @Resource
    private BlackIpService blackIpService;

    /**
     * 新增 BlackIp
     *
     * @param blackIpBO BlackIp
     * @return {@link BlackIpBO}
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Add.class) @RequestBody BlackIpBO blackIpBO) {
        try {
            blackIpService.save(blackIpBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 BlackIp
     *
     * @param id ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            blackIpService.remove(Long.parseLong(id));
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 BlackIp
     * <ol>
     * <li>支持更新: Enable</li>
     * <li>不支持更新: Ip</li>
     * </ol>
     *
     * @param blackIpBO BlackIp
     * @return {@link BlackIpBO}
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody BlackIpBO blackIpBO) {
        try {
            blackIpService.update(blackIpBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 BlackIp
     *
     * @param id ID
     * @return {@link BlackIpBO}
     */
    @GetMapping("/id/{id}")
    public R<BlackIpBO> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            BlackIpBO select = blackIpService.selectById(Long.parseLong(id));
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

    /**
     * 根据 Ip 查询 BlackIp
     *
     * @param ip Black Ip
     * @return {@link BlackIpBO}
     */
    @GetMapping("/ip/{ip}")
    public R<BlackIpBO> selectByIp(@NotNull @PathVariable(value = "ip") String ip) {
        try {
            BlackIpBO select = blackIpService.selectByIp(ip);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

    /**
     * 分页查询 BlackIp
     *
     * @param blackIpPageQuery BlackIp和分页参数
     * @return 带分页的 {@link BlackIpBO}
     */
    @PostMapping("/list")
    public R<Page<BlackIpBO>> list(@RequestBody(required = false) BlackIpQuery blackIpPageQuery) {
        try {
            if (ObjectUtil.isEmpty(blackIpPageQuery)) {
                blackIpPageQuery = new BlackIpQuery();
            }
            Page<BlackIpBO> page = blackIpService.selectByPage(blackIpPageQuery);
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
    public R<Boolean> checkBlackIpValid(@NotNull @PathVariable(value = "ip") String ip) {
        try {
            return Boolean.TRUE.equals(blackIpService.checkBlackIpValid(ip)) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
