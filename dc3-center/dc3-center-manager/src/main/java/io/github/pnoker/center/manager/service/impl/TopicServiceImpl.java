package io.github.pnoker.center.manager.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.github.pnoker.center.manager.entity.model.PointDO;
import io.github.pnoker.center.manager.entity.model.ProfileBindDO;
import io.github.pnoker.center.manager.entity.query.TopicQuery;
import io.github.pnoker.center.manager.entity.vo.TopicVO;
import io.github.pnoker.center.manager.mapper.DeviceMapper;
import io.github.pnoker.center.manager.service.TopicService;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;



@Service
public class TopicServiceImpl extends ServiceImpl<DeviceMapper, DeviceDO>
        implements TopicService {


    @Override
    public Page<List<TopicVO>> query(TopicQuery topicQuery) {
        int page =(int)topicQuery.getPage().getCurrent();
        int size = (int)topicQuery.getPage().getSize();
        Page<List<TopicVO>> resultPage = new Page<>(page, size);
        List<TopicVO> topicVOList = new ArrayList<>();
        String topic = topicQuery.getTopic();
        Long deviceIdL = null;
        if (topic != null && topic.length() > 0) {
            String[] parts = topic.split("/");
            deviceIdL = Long.parseLong(parts[parts.length - 1]);
        }
        String dName = topicQuery.getDeviceName();
        List<DeviceDO> deviceList = lambdaQuery()
                .eq(deviceIdL != null,DeviceDO::getId,deviceIdL)
                .eq(dName != null,DeviceDO::getDeviceName,dName)
                .eq(DeviceDO::getEnableFlag,1)
                .eq(DeviceDO::getDeleted,0)
                .list();
        for(DeviceDO device:deviceList){
            String deviceName = device.getDeviceName();
            Long deviceId = device.getId();
            ProfileBindDO profileBind= Db.lambdaQuery(ProfileBindDO.class)
                    .eq(ProfileBindDO::getDeviceId, deviceId)
                    .eq(ProfileBindDO::getDeleted,0)
                    .one();
            if(profileBind != null){
                Long profileBindId = profileBind.getProfileId();
                List<PointDO> points = Db.lambdaQuery(PointDO.class)
                        .eq(PointDO::getProfileId,profileBindId)
                        .eq(PointDO::getEnableFlag,1)
                        .eq(PointDO::getDeleted,0)
                        .list();
                for (PointDO point : points) {
                    TopicVO topicVO = new TopicVO();
                    topicVO.setTopic("dc3/dc3-center-data/device/"+deviceId);
                    topicVO.setDeviceName(deviceName);
                    topicVO.setPointName(point.getPointName());
                    topicVOList.add(topicVO);
                }
            }
        }
        int totalItems = topicVOList.size();
        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<List<TopicVO>> paginatedData = new ArrayList<>();
        if (fromIndex < toIndex) {
            paginatedData.add(topicVOList.subList(fromIndex, toIndex));
        }
        resultPage.setRecords(paginatedData);
        resultPage.setTotal(totalItems);
        return resultPage;
    }

}