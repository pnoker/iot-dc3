package io.github.pnoker.center.data.entity.vo;

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
@Schema(title = "Driver", description = "驱动7天在线/离线时长")
public class DriverRunVO {

    @Schema(description ="驱动名称")
    private String driverName;

    @Schema(description ="驱动状态")
    private String status;

    @Schema(description = "驱动在线时长 /分钟")
    private List<Long> duration;


}
