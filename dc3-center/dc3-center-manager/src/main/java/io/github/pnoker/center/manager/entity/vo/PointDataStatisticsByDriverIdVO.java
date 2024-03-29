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
@Schema(title = " PointDataStatisticsByDriver", description = "统计7天驱动下位号数据量")
public class PointDataStatisticsByDriverIdVO {

    @Schema(description = "驱动名称")
    private String driverName;

    @Schema(description = "7天数据量")
    private List<Long> total;
}
