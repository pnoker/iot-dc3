package io.github.pnoker.center.data.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;
import io.github.pnoker.center.data.service.ChannelService;
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
@RequestMapping(DataConstant.RABBITMQ_CHANNELS_URL_PREFIX)
public class RabbitMQChannelController {


    @Resource
    private ChannelService channelService;
    @GetMapping("/Chans")
    public R<RabbitMQDataVo> queryChans(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= channelService.queryChan(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
        }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/ToChan")
    public R<RabbitMQDataVo> queryToChans(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= channelService.queryToChan(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/ChanOpen")
    public R<RabbitMQDataVo> queryChansOpen(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= channelService.queryChanOpen(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/ChanClose")
    public R<RabbitMQDataVo> queryChansClose(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= channelService.queryChanClose(cluster);
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
