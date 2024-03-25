package io.github.pnoker.center.data.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;
import io.github.pnoker.center.data.service.MessageService;
import io.github.pnoker.center.data.service.PublisherService;
import io.github.pnoker.common.base.BaseController;
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
@RequestMapping(DataConstant.RABBITMQ_PUBLISHERS_URL_PREFIX)
public class RabbitMQPublisherController implements BaseController {
    @Resource
    private PublisherService publisherService;
    @GetMapping("/Pubs")
    public R<RabbitMQDataVo> queryPubs(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= publisherService.queryPub(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
