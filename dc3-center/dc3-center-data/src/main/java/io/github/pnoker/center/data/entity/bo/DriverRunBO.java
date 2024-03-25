package io.github.pnoker.center.data.entity.bo;

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
public class DriverRunBO {
    /**
     * 驱动名称
     */
    private String driverName;
    /**
     * 驱动状态
     */
    private String status;

    /**
     * 驱动在线时长 /分钟
     */
    private List<Long> duration;
}
