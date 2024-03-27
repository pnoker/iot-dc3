package io.github.pnoker.center.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
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
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "PointByDevice", description = "当前设备位号数量")
public class PointConfigByDeviceVO {

    /**
     * 未配置数量
     */
    @Schema(description = "未配置位号数量")
    private Long unConfigCount;
    /**
     * 配置数量
     */
    @Schema(description = "已配置位号数量")
    private Long configCount;

    /**
     * 位号集合
     */
    @Schema(description = "位号集合")
    private List<PointDO> points;
}
