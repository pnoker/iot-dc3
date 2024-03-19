package io.github.pnoker.center.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "Driver", description = "驱动7天在线/离线时长")
public class DriverRunVO {


    @Schema(description = "日期 ")
    private LocalDateTime createTime;

    @Schema(description = "驱动在线时长 /分钟")
    private Long duration;


}
