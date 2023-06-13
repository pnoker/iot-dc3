package io.github.pnoker.center.manager.service;

import io.github.pnoker.common.dto.DataStatisticsDTO;
import io.github.pnoker.common.dto.WeatherDeviceStatisticsDTO;

import java.util.List;

public interface IndexService {

    DataStatisticsDTO dataStatistics();

    List<WeatherDeviceStatisticsDTO> weatherDeviceList();
}
