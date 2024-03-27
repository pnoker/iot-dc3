package io.github.pnoker.center.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@Schema(title = "DeviceDataVolumeRun", description = "设备在不同位号下的数据量 7天")
public class DeviceDataVolumeRunVO {

    @Schema(description = "位号名称")
    private  String deviceName;
    @Schema(description = "7天数据量")
    private List<Long> total;
}
