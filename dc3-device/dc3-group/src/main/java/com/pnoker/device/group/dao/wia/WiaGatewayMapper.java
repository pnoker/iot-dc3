package com.pnoker.device.group.dao.wia;

import com.pnoker.device.group.model.wia.WiaGateway;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Mapper
@Component
public interface WiaGatewayMapper {
    @Select("select * from dc3_group_wia_gateway order by id asc")
    List<WiaGateway> list();
}
