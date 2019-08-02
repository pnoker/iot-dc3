package com.pnoker.device.group.dao.wia;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pnoker.device.group.model.wia.WiaDevice;
import com.pnoker.device.group.model.wia.WiaVariable;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: WiaDevice数据 数据库操作接口
 */
@Mapper
public interface WiaVariableMapper extends BaseMapper<WiaVariable> {
}
