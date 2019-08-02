package com.pnoker.device.group.dao.wia;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pnoker.device.group.model.wia.WiaGateway;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: WiaGateway数据 数据库操作接口
 */
@Mapper
public interface WiaGatewayMapper extends BaseMapper<WiaGateway> {
}
