package com.pnoker.center.manager.service.impl;

import com.baomidou.mybatisplus.extension.api.R;
import com.pnoker.center.manager.service.DeviceService;
import com.pnoker.center.manager.service.DriverService;
import com.pnoker.center.manager.service.NotifyService;
import com.pnoker.center.manager.service.ProfileService;
import com.pnoker.center.manager.service.pool.ThreadPool;
import com.pnoker.common.bean.driver.DriverOperation;
import com.pnoker.common.constant.Operation;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Driver;
import com.pnoker.common.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * NotifyService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {
    @Resource
    private ThreadPool threadPool;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private DriverService driverService;
    @Resource
    private ProfileService profileService;
    @Resource
    private DeviceService deviceService;

    @Override
    public void notifyDriverAddProfile(Long profileId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByProfile(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Profile.ADD).setId(profileId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverDeleteProfile(Long profileId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByProfile(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Profile.DELETE).setId(profileId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverAddDevice(Long deviceId, Long profileId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByProfile(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Device.ADD).setId(deviceId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverDeleteDevice(Long deviceId, Long profileId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByProfile(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Device.DELETE).setId(deviceId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverUpdateDevice(Long deviceId, Long profileId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByProfile(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Device.UPDATE).setId(deviceId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverAddPoint(Long pointId, Long profileId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByProfile(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Point.ADD).setId(pointId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverDeletePoint(Long pointId, Long profileId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByProfile(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Point.DELETE).setId(pointId).setParentId(profileId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverUpdatePoint(Long pointId, Long profileId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByProfile(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Point.UPDATE).setId(pointId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverAddDriverInfo(Long driverInfoId, Long profileId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByProfile(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.DriverInfo.ADD).setId(driverInfoId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverDeleteDriverInfo(Long driverInfoId, Long attributeId, Long profileId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByProfile(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.DriverInfo.DELETE).setParentId(profileId).setAttributeId(attributeId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverUpdateDriverInfo(Long driverInfoId, Long profileId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByProfile(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.PointInfo.ADD).setId(driverInfoId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverAddPointInfo(Long pointInfoId, Long deviceId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByDevice(deviceId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.PointInfo.ADD).setId(pointInfoId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverDeletePointInfo(Long pointId, Long attributeId, Long deviceId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByDevice(deviceId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.PointInfo.DELETE).setId(pointId).setParentId(deviceId).setAttributeId(attributeId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void notifyDriverUpdatePointInfo(Long pointInfoId, Long deviceId) {
        threadPool.poolExecutor.schedule(() -> {
            Driver driver = getDriverByDevice(deviceId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.PointInfo.UPDATE).setId(pointInfoId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    public Driver getDriverByDevice(Long id) {
        Device device = deviceService.selectById(id);
        if (null != device) {
            Profile profile = profileService.selectById(device.getProfileId());
            if (null != profile) {
                return driverService.selectById(profile.getDriverId());
            }
        }
        return null;
    }

    public Driver getDriverByProfile(Long id) {
        Profile profile = profileService.selectById(id);
        if (null != profile) {
            return driverService.selectById(profile.getDriverId());
        }
        return null;
    }
}
