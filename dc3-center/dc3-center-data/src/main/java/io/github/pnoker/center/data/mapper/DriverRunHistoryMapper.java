package io.github.pnoker.center.data.mapper;

import io.github.pnoker.center.data.entity.model.DriverRunDO;
import io.github.pnoker.center.data.entity.model.DriverRunHistoryDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * <p>
 * 驱动运行时长历史表 Mapper 接口
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
public interface DriverRunHistoryMapper extends BaseMapper<DriverRunHistoryDO> {

    DriverRunDO getDurationDay(@Param("id") Long id,@Param("status") String status,@Param("startOfDay") LocalDateTime startOfDay,@Param("endOfDay") LocalDateTime endOfDay);
}
