package io.github.pnoker.center.manager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.github.pnoker.center.manager.entity.query.TopicQuery;
import io.github.pnoker.center.manager.entity.vo.TopicVO;
import java.util.List;



public interface TopicService extends IService<DeviceDO> {

    Page<List<TopicVO>> query(TopicQuery topicQuery);

}
