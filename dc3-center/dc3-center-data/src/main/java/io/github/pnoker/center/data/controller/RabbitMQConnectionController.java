package io.github.pnoker.center.data.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        try{
            RabbitMQDataVo rabbbit= connectionService.queryConn(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
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
        try{
            RabbitMQDataVo rabbbit= connectionService.queryToConn(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
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
        try{
            RabbitMQDataVo rabbbit= connectionService.queryConnOpen(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
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
        try{
            RabbitMQDataVo rabbbit= connectionService.queryConnClose(cluster);
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
