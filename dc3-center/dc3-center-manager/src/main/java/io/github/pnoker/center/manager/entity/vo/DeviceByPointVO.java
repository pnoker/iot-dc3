package io.github.pnoker.center.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.center.manager.entity.bo.DeviceBO;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
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
@Schema(title = "DeviceByPoint", description = "当前位号设备数量")
public class DeviceByPointVO {

    /**
     * 数量
     */
    @Schema(description = "设备数量")
    private Long count;

    /**
     * 设备集合
     */
    @Schema(description = "设备集合")
    private List<DeviceDO> devices;

}
