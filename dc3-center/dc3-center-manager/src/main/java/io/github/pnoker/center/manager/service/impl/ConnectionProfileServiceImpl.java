package io.github.pnoker.center.manager.service.impl;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.manager.entity.model.ConnectionProfile;
import io.github.pnoker.center.manager.entity.vo.ProfileNameListVO;
import io.github.pnoker.center.manager.mapper.ConnectionProfileMapper;
import io.github.pnoker.center.manager.service.ConnectionProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author 86150
* @description 针对表【dc3_connection_profile】的数据库操作Service实现
* @createDate 2024-07-16 14:14:04
*/
@Service
public class ConnectionProfileServiceImpl extends ServiceImpl<ConnectionProfileMapper, ConnectionProfile>
    implements ConnectionProfileService {
    @Autowired
    private ConnectionProfileMapper connectionProfileMapper;

    @Override
    public List<ProfileNameListVO> queryConnectionProfile() {
        LambdaQueryWrapper<ConnectionProfile> wrapper = new LambdaQueryWrapper<>();
        List<ConnectionProfile> list = connectionProfileMapper.selectList(wrapper);
        List<ProfileNameListVO> profileNameListVOS = new ArrayList<ProfileNameListVO>();
        for (ConnectionProfile connectionProfile : list) {
            ProfileNameListVO profileNameListVO = new ProfileNameListVO();
            profileNameListVO.setId(connectionProfile.getId());
            profileNameListVO.setProfilename(connectionProfile.getProfileName());
            profileNameListVOS.add(profileNameListVO);
        }
        return profileNameListVOS;
    }
}




