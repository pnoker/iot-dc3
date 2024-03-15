package io.github.pnoker.center.data.mapper;

import io.github.pnoker.center.data.entity.model.DeviceRunDO;
import io.github.pnoker.center.data.entity.model.DeviceRunHistoryDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * <p>
 * 设备运行时长历史表 Mapper 接口
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
public interface DeviceRunHistoryMapper extends BaseMapper<DeviceRunHistoryDO> {

    DeviceRunDO getDurationDay(@Param("id") Long id, @Param("status") String status, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}
