package io.github.pnoker.center.manager.controller;


import io.github.pnoker.center.manager.entity.model.ConnectionProfile;
import io.github.pnoker.center.manager.entity.vo.ProfileNameListVO;
import io.github.pnoker.center.manager.service.ConnectionProfileService;
import io.github.pnoker.center.manager.service.MqttService;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


/**
 * @version 1.0
 * @Author 嘉平十二
 * @Date 2024/7/15 16:12
 * @注释
 */

@Slf4j
@RestController
@RequestMapping(ManagerConstant.MQTT_URL_PREFIX)
public class MqttController {

    private final MqttService mqttService;
    @Autowired
    private ConnectionProfileService connectionProfileService;

    @Autowired
    public MqttController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @PostMapping("/connect")
    public String connect(@RequestBody ConnectionProfile request) {
        try {
            return mqttService.connect(request);
        } catch (MqttException e) {
            e.printStackTrace();
            return "Failed to connect: " + e.getMessage();
        }
    }

    @PostMapping("/subscribe")
    public String subscribe(@RequestParam String topic) {
        try {
            return mqttService.subscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
            return "Failed to subscribe: " + e.getMessage();
        }
    }
    @PostMapping("/unsubscribe")
    public String unsubscribe(@RequestParam String topic) {
        try {
            return mqttService.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
            return "Failed to unsubscribe: " + e.getMessage();
        }
    }

    @PostMapping("/disconnect")
    public String disconnect() {
        try {
            return mqttService.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
            return "Failed to disconnect: " + e.getMessage();
        }
    }

    @GetMapping("/list")
    public Mono<R<List<ProfileNameListVO>>> queryList() {
        try {
            List<ProfileNameListVO> list = connectionProfileService.queryConnectionProfile();
            return Mono.just(R.ok(list));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @GetMapping("/query/{id}")
    public Mono<R<ConnectionProfile>> queryConnectionProfileById(@PathVariable("id") Integer id) {
        try {
            ConnectionProfile connectionProfile = connectionProfileService.getById(id);
            return Mono.just(R.ok(connectionProfile));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }
    @PutMapping("/update/{id}")
    public Mono<R<String>> updateConnectionProfile(@PathVariable("id") Integer id, @RequestBody ConnectionProfile profile) {
        try {
            profile.setId(id);
            boolean updated = connectionProfileService.updateById(profile);
            if(updated){
                return Mono.just(R.ok("修改成功"));
            }else {
                Mono.just(R.fail("修改失败"));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
        return null;
    }
}