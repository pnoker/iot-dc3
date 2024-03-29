package io.github.pnoker.center.manager.entity.bo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDataVolumeRunBO {
    /**
     * 位号名称
     */
    private String pointName;
    /**
     * 设备数据量 7天
     */
    private List<Long> total;
}
