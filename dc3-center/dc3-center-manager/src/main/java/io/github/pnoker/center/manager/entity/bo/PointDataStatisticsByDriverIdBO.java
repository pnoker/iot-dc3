package io.github.pnoker.center.manager.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointDataStatisticsByDriverIdBO {
    /**
     * 驱动名称
     */
    private String driverName;

    /**
     * 7天数据量
     */
    private List<Long> total;
}
