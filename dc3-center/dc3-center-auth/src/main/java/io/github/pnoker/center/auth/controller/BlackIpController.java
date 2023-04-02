/*
 * Copyright 2016-present the original author or authors.
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
import io.github.pnoker.center.auth.entity.query.BlackIpPageQuery;
import io.github.pnoker.center.auth.service.BlackIpService;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.model.BlackIp;
import io.github.pnoker.common.valid.Insert;
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
public class BlackIpController {

    @Resource
    private BlackIpService blackIpService;

    /**
     * 新增 BlackIp
     *
     * @param blackIp BlackIp
     * @return {@link BlackIp}
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Insert.class) @RequestBody BlackIp blackIp) {
        try {
            blackIpService.add(blackIp);
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
            blackIpService.delete(id);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 修改 BlackIp
     * <ol>
     * <li>支持修改: Enable</li>
     * <li>不支持修改: Ip</li>
     * </ol>
     *
     * @param blackIp BlackIp
     * @return {@link BlackIp}
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody BlackIp blackIp) {
        try {
            blackIpService.update(blackIp);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 BlackIp
     *
     * @param id ID
     * @return {@link BlackIp}
     */
    @GetMapping("/id/{id}")
    public R<BlackIp> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            BlackIp select = blackIpService.selectById(id);
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
     * @return {@link BlackIp}
     */
    @GetMapping("/ip/{ip}")
    public R<BlackIp> selectByIp(@NotNull @PathVariable(value = "ip") String ip) {
        try {
            BlackIp select = blackIpService.selectByIp(ip, false);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

    /**
     * 模糊分页查询 BlackIp
     *
     * @param blackIpPageQuery BlackIp和分页参数
     * @return 带分页的 {@link BlackIp}
     */
    @PostMapping("/list")
    public R<Page<BlackIp>> list(@RequestBody(required = false) BlackIpPageQuery blackIpPageQuery) {
        try {
            if (ObjectUtil.isEmpty(blackIpPageQuery)) {
                blackIpPageQuery = new BlackIpPageQuery();
            }
            Page<BlackIp> page = blackIpService.list(blackIpPageQuery);
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
