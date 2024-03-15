package io.github.pnoker.center.data.mapper;

import io.github.pnoker.center.data.entity.model.DeviceStatusHistoryDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.pnoker.center.data.entity.model.DriverStatusHistoryDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 设备状态历史表 Mapper 接口
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
public interface DeviceStatusHistoryMapper extends BaseMapper<DeviceStatusHistoryDO> {

    List<DeviceStatusHistoryDO> selectRecently2Data(@Param("id") long id);
}
