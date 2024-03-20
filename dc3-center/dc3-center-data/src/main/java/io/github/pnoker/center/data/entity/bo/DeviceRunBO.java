package io.github.pnoker.center.data.entity.bo;

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
public class DeviceRunBO {

    /**
     * 驱动状态
     */
    private String status;

    /**
     * 设备在线时长 /分钟
     */
    private List<Long> duration;
}
