/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.api.center.auth.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.auth.fallback.BlackIpClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.BlackIpDto;
import io.github.pnoker.common.model.BlackIp;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;

/**
 * Ip 黑名单 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = ServiceConstant.Auth.BLACK_IP_URL_PREFIX, name = ServiceConstant.Auth.SERVICE_NAME, fallbackFactory = BlackIpClientFallback.class)
public interface BlackIpClient {

    /**
     * 新增 BlackIp
     *
     * @param blackIp BlackIp
     * @return BlackIp
     */
    @PostMapping("/add")
    R<BlackIp> add(@Validated(Insert.class) @RequestBody BlackIp blackIp);

    /**
     * 根据 ID 删除 BlackIp
     *
     * @param id BlackIp Id
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 修改 BlackIp
     * <p>
     * 支  持: Enable
     * 不支持: Ip
     *
     * @param blackIp BlackIp
     * @return BlackIp
     */
    @PostMapping("/update")
    R<BlackIp> update(@Validated(Update.class) @RequestBody BlackIp blackIp);

    /**
     * 根据 ID 查询 BlackIp
     *
     * @param id BlackIp Id
     * @return BlackIp
     */
    @GetMapping("/id/{id}")
    R<BlackIp> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 Ip 查询 BlackIp
     *
     * @param ip Black Ip
     * @return BlackIp
     */
    @GetMapping("/ip/{ip}")
    R<BlackIp> selectByIp(@NotNull @PathVariable(value = "ip") String ip);

    /**
     * 分页查询 BlackIp
     *
     * @param blackIpDto Dto
     * @return Page<BlackIp>
     */
    @PostMapping("/list")
    R<Page<BlackIp>> list(@RequestBody(required = false) BlackIpDto blackIpDto);

    /**
     * 检测 Ip 是否在 Ip 黑名单列表
     *
     * @param ip Black Ip
     * @return Boolean
     */
    @GetMapping("/check/{ip}")
    R<Boolean> checkBlackIpValid(@NotNull @PathVariable(value = "ip") String ip);

}
