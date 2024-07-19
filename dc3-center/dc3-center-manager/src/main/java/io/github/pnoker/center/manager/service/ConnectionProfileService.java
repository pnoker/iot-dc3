package io.github.pnoker.center.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.pnoker.center.manager.entity.model.ConnectionProfile;
import io.github.pnoker.center.manager.entity.vo.ProfileNameListVO;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

/**
* @author 86150
* @description 针对表【dc3_connection_profile】的数据库操作Service
* @createDate 2024-07-16 14:14:04
*/
public interface ConnectionProfileService extends IService<ConnectionProfile> {
    List<ProfileNameListVO>  queryConnectionProfile();
}
