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
import io.github.pnoker.center.data.service.RabbitMQQueueService;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
/**
 * RabbitMQ队列 Controller
 *
 * @author wangshuai
 * @since 2024.3.26
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.RABBITMQ_QUEUE_URL_PREFIX)
public class RabbitMQQueueController {
    @Resource
    private RabbitMQQueueService rabbitMQQueueService;

    @GetMapping("/queues")
    public R<RabbitMQDataVo> queryQues(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQQueueService.queryQue(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/queues_ready")
    public R<RabbitMQDataVo> queryRToC(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQQueueService.readyToCons(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/queues_pend")
    public R<RabbitMQDataVo> queryPToC(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQQueueService.pendToCons(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/queues_total")
    public R<RabbitMQDataVo> queryToQue(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQQueueService.queryToQue(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/queues_declared")
    public R<RabbitMQDataVo> queryQueDec(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQQueueService.queryQueDec(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/queues_create")
    public R<RabbitMQDataVo> queryQueCre(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQQueueService.queryQueCre(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/queues_deleted")
    public R<RabbitMQDataVo> queryQueDel(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQQueueService.queryQueDel(cluster);
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
