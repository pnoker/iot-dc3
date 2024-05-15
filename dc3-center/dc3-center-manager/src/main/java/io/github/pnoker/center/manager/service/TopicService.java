package io.github.pnoker.center.manager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.github.pnoker.center.manager.entity.vo.TopicVO;
import java.util.List;

/**
* @author jiaping
* @description 针对表【dc3_device(设备表)】的数据库操作Service
* @createDate 2024-05-13 09:40:00
*/
public interface TopicService extends IService<DeviceDO> {

    Page<List<TopicVO>> queryList(int page, int size);
    Page<List<TopicVO>> queryTopicList(String topic,int page, int size);
    Page<List<TopicVO>> queryDeviceNameList(String deviceName,int page, int size);

}
