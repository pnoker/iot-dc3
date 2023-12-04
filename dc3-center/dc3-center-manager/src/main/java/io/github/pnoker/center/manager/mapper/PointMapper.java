package io.github.pnoker.center.manager.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.model.PointDO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 设备位号表 Mapper 接口
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface PointMapper extends BaseMapper<PointDO> {
    Page<PointDO> selectPageWithDevice(Page<PointDO> page, @Param(Constants.WRAPPER) Wrapper<PointDO> queryWrapper, @Param("deviceId") Long deviceId);

}
