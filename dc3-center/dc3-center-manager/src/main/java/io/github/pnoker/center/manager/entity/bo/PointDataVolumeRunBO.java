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
public class PointDataVolumeRunBO {
    /**
     * 设备名称
     */
    private  String deviceName;
    /**
     * 设备数据量 7天
     */
    private List<Long> total;
}
