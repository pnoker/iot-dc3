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
import io.github.pnoker.center.data.service.RabbitMQMessageService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
/**
 * RabbitMQ消息 Controller
 *
 * @author wangshuai
 * @since 2024.3.26
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.RABBITMQ_MESSAGE_URL_PREFIX)
public class RabbitMQMessageController implements BaseController {
    @Resource
    private RabbitMQMessageService rabbitMQMessageService;

    @GetMapping("/message_in")
    public R<RabbitMQDataVo> queryInMess(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQInMess(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_ready")
    public R<RabbitMQDataVo> queryReMess(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQReMess(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_unack")
    public R<RabbitMQDataVo> queryUnackMess(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQUnackMess(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_out")
    public R<RabbitMQDataVo> queryOutMess(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQOutMess(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_published")
    public R<RabbitMQDataVo> queryMessPub(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQMessPub(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_confirm")
    public R<RabbitMQDataVo> queryConfPub(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQConfPub(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_queue")
    public R<RabbitMQDataVo> queryRoutQue(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQRoutQue(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_unconfirm")
    public R<RabbitMQDataVo> queryUnConfPub(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQUnConfPub(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_drop")
    public R<RabbitMQDataVo> queryUnRoutDrop(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQUnRoutDrop(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_unroutable")
    public R<RabbitMQDataVo> queryUnRoutPub(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQUnRoutPub(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_deliver")
    public R<RabbitMQDataVo> queryMessDel(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQMessDel(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_redeliver")
    public R<RabbitMQDataVo> queryMessReDel(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQMessReDel(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_manual")
    public R<RabbitMQDataVo> queryMessDelAck(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQMessDelAck(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_auto")
    public R<RabbitMQDataVo> queryMessDelAuto(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQMessDelAuto(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/message_ack")
    public R<RabbitMQDataVo> queryMessAck(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQMessAck(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/poll_auto_ack")
    public R<RabbitMQDataVo> queryPoAutoAck(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQPoAutoAck(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/poll_no_result")
    public R<RabbitMQDataVo> queryPoNoResult(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQPoNoResult(cluster);
            if (!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()) {
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/poll_manual_ack")
    public R<RabbitMQDataVo> queryPoWithAck(@RequestParam String cluster) {
        try {
            RabbitMQDataVo rabbbit = rabbitMQMessageService.queryMQPoWithAck(cluster);
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
