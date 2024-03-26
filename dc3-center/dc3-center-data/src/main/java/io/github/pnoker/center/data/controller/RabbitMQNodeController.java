package io.github.pnoker.center.data.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;
import io.github.pnoker.center.data.entity.vo.RabbitMQNodeVo;
import io.github.pnoker.center.data.service.NodeService;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(DataConstant.RABBITMQ_NODES_URL_PREFIX)
public class RabbitMQNodeController {
    @Resource
    private NodeService nodeService;

    @GetMapping("/Nodes")
    public R<RabbitMQDataVo> queryNodes(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= nodeService.queryNode(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/NodesTable")
    public R<List<RabbitMQNodeVo>> queryNodesTable(@RequestParam String cluster) {
        try{
            List<RabbitMQNodeVo> rabbbit= nodeService.queryNodeTable(cluster);
            if(rabbbit != null){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
