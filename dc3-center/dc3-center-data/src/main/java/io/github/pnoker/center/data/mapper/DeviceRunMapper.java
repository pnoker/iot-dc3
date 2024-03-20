package io.github.pnoker.center.data.mapper;

import io.github.pnoker.center.data.entity.model.DeviceRunDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 设备运行时长历史表 Mapper 接口
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
public interface DeviceRunMapper extends BaseMapper<DeviceRunDO> {

    List<DeviceRunDO> get7daysDuration(@Param("driverId") Long driverId, @Param("status") String status);
}
