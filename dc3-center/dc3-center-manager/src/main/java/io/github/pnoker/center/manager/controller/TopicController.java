package io.github.pnoker.center.manager.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.vo.TopicVO;
import io.github.pnoker.center.manager.service.TopicService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;


@Slf4j
@RestController
@RequestMapping(ManagerConstant.TOPIC_URL_PREFIX)
public class TopicController implements BaseController{

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }


    @GetMapping("/list")
    public R<Page<List<TopicVO>>> queryList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            Page<List<TopicVO>> topicVOList = topicService.queryList(page, size);
            return R.ok(topicVOList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/querytopic")
    public R<Page<List<TopicVO>>> queryTopicList(
            @RequestParam() String topic,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            Page<List<TopicVO>> topicVOList = topicService.queryTopicList(topic, page, size);
            return R.ok(topicVOList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/querydevice")
    public R<Page<List<TopicVO>>> queryDeviceList(
            @RequestParam() String deviceName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            Page<List<TopicVO>> topicVOList = topicService.queryDeviceNameList(deviceName, page, size);
            return R.ok(topicVOList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

}
