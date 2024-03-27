package io.github.pnoker.center.manager.entity.bo;

import io.github.pnoker.center.manager.entity.model.PointDO;
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
public class PointConfigByDeviceBO {
    /**
     * 配置数量
     */
    private Long configCount;

    /**
     * 未配置数量
     */
    private Long unConfigCount;
    /**
     * 位号集合
     */
    private List<PointDO> points;
}
