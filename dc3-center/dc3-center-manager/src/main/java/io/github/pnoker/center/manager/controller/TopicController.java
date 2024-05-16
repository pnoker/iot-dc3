package io.github.pnoker.center.manager.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.TopicQuery;
import io.github.pnoker.center.manager.entity.vo.TopicVO;
import io.github.pnoker.center.manager.service.TopicService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Slf4j
@RestController
@RequestMapping(ManagerConstant.TOPIC_URL_PREFIX)
public class TopicController implements BaseController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping("/list")
    public R<Page<List<TopicVO>>> query(@RequestBody(required = false) TopicQuery topicQuery) {
        try {
            Page<List<TopicVO>> topicVOList = topicService.query(topicQuery);
            return R.ok(topicVOList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }
}
