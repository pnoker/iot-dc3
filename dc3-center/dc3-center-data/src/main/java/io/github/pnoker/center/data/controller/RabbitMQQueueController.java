package io.github.pnoker.center.data.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;
import io.github.pnoker.center.data.service.QueueService;
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
@RequestMapping(DataConstant.RABBITMQ_QUEUES_URL_PREFIX)
public class RabbitMQQueueController {
    @Resource
    private QueueService queueService;
    @GetMapping("/Ques")
    public R<RabbitMQDataVo> queryQues(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= queueService.queryQue(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/RToC")
    public R<RabbitMQDataVo> queryRToC(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= queueService.readyToCons(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/PToC")
    public R<RabbitMQDataVo> queryPToC(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= queueService.pendToCons(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/ToQue")
    public R<RabbitMQDataVo> queryToQue(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= queueService.queryToQue(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/QueDec")
    public R<RabbitMQDataVo> queryQueDec(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= queueService.queryQueDec(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/QueCre")
    public R<RabbitMQDataVo> queryQueCre(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= queueService.queryQueCre(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/QueDel")
    public R<RabbitMQDataVo> queryQueDel(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= queueService.queryQueDel(cluster);
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
