package io.github.pnoker.center.data.dal;

import io.github.pnoker.center.data.entity.model.DriverRunDO;
import io.github.pnoker.center.data.entity.model.DriverRunHistoryDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * <p>
 * 驱动运行时长历史表 服务类
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
public interface DriverRunHistoryManager extends IService<DriverRunHistoryDO> {
}
