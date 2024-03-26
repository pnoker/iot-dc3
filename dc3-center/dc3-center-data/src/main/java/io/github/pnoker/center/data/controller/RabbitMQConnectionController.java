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

package io.github.pnoker.center.data.controller;

import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;
import io.github.pnoker.center.data.service.ConnectionService;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping(DataConstant.RABBITMQ_CONNECTIONS_URL_PREFIX)
public class RabbitMQConnectionController {
    @Resource
    private ConnectionService connectionService;

    @GetMapping("/Conns")
    public R<RabbitMQDataVo> queryConns(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = connectionService.queryConn(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/ToConn")
    public R<RabbitMQDataVo> queryToConns(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = connectionService.queryToConn(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/ConnOpen")
    public R<RabbitMQDataVo> queryConnsOpen(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = connectionService.queryConnOpen(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/ConnClose")
    public R<RabbitMQDataVo> queryConnsClose(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = connectionService.queryConnClose(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
