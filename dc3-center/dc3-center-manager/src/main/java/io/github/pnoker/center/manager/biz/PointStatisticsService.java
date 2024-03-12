package io.github.pnoker.center.manager.biz;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 积分统计服务
 *
 * @Author fukq
 * create by 2024/3/5 14:40
 * @Version 1.0
 * @date 2024/03/05
 */
public interface PointStatisticsService {
   /**
    * 统计点历史
    */
   void statisticsPointHistory(LocalDateTime datetime);
}
