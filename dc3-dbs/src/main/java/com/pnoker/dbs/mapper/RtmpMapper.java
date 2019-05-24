package com.pnoker.dbs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pnoker.common.model.rtmp.Rtmp;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Rtmp 数据库操作接口
 */
@Mapper
public interface RtmpMapper extends BaseMapper<Rtmp> {
}
