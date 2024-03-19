package io.github.pnoker.center.data.mapper;

import io.github.pnoker.center.data.entity.model.DriverRunDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 驱动运行时长历史表 Mapper 接口
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
public interface DriverRunMapper extends BaseMapper<DriverRunDO> {

    List<DriverRunDO> get7daysDuration(@Param("driverId") Long driverId,@Param("status") String status);
}
