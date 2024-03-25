package io.github.pnoker.center.data.controller;

import io.github.pnoker.center.data.service.ClusterService;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(DataConstant.RABBITMQ_CLUSTER_URL_PREFIX)
public class RabbitMQClusterController {

    @Resource
    private ClusterService clusterService;
    @GetMapping("/Cluster")
    public R<List<String>> queryCluster() {
        try{
            List<String> rabbbit= clusterService.queryCluster();
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
