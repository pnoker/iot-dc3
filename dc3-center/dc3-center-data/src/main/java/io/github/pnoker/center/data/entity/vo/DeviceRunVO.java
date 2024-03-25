package io.github.pnoker.center.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "Device", description = "设备7天在线/离线时长")
public class DeviceRunVO {
    @Schema(description ="设备名称")
    private String deviceName;

    @Schema(description ="设备状态")
    private String status;

    @Schema(description = "设备在线时长 /分钟")
    private List<Long> duration;


}
