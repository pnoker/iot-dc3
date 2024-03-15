package io.github.pnoker.center.data.mapper;

import io.github.pnoker.center.data.entity.model.DriverStatusHistoryDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 驱动状态历史表 Mapper 接口
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
public interface DriverStatusHistoryMapper extends BaseMapper<DriverStatusHistoryDO> {
    List<DriverStatusHistoryDO> selectRecently2Data(@Param("id") Long id);

}
