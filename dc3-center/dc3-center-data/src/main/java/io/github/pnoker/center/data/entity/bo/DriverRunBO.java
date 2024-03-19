package io.github.pnoker.center.data.entity.bo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverRunBO {

    /**
     * 日期
     */
    private LocalDateTime createTime;
    /**
     * 驱动在线时长 /分钟
     */
    private Long duration;
}
