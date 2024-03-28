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

import io.github.pnoker.center.data.service.RabbitMQClusterService;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
/**
 * RabbitMQ集群 Controller
 *
 * @author wangshuai
 * @since 2024.3.26
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.RABBITMQ_CLUSTER_URL_PREFIX)
public class RabbitMQClusterController {

    @Resource
    private RabbitMQClusterService rabbitMQClusterService;

    @GetMapping("/clusters")
    public R<List<String>> queryCluster() {
        try {
            List<String> rabbbit = rabbitMQClusterService.queryCluster();
            if (rabbbit != null) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
