/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.DevicePageQuery;
import io.github.pnoker.center.manager.mapper.DeviceMapper;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.constant.driver.StorageConstant;
import io.github.pnoker.common.entity.base.Base;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.*;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.PoiUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DeviceService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DeviceServiceImpl implements DeviceService {

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DriverAttributeService driverAttributeService;
    @Resource
    private DriverAttributeConfigService driverAttributeConfigService;
    @Resource
    private PointAttributeService pointAttributeService;
    @Resource
    private PointAttributeConfigService pointAttributeConfigService;
    @Resource
    private ProfileService profileService;
    @Resource
    private PointService pointService;
    @Resource
    private ProfileBindService profileBindService;
    @Resource
    private NotifyService notifyService;
    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(Device entityDO) {
        if (deviceMapper.insert(entityDO) < 1) {
            throw new AddException("The device {} add failed", entityDO.getDeviceName());
        }

        addProfileBind(entityDO.getId(), entityDO.getProfileIds());

        // 通知驱动新增
        Device device = deviceMapper.selectById(entityDO.getId());
        List<Profile> profiles = profileService.selectByDeviceId(entityDO.getId());
        // ?/pnoker 同步给驱动的设备需要profile id set吗
        device.setProfileIds(profiles.stream().map(Profile::getId).collect(Collectors.toSet()));
        notifyService.notifyDriverDevice(MetadataCommandTypeEnum.ADD, device);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void delete(String id) {
        Device device = selectById(id);
        if (ObjectUtil.isNull(device)) {
            throw new NotFoundException("The device does not exist");
        }

        if (!profileBindService.deleteByDeviceId(id)) {
            throw new DeleteException("The profile bind delete failed");
        }

        if (deviceMapper.deleteById(id) < 1) {
            throw new DeleteException("The device delete failed");
        }

        // 通知驱动删除设备
        notifyService.notifyDriverDevice(MetadataCommandTypeEnum.DELETE, device);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Device entityDO) {
        selectById(entityDO.getId());

        Set<String> newProfileIds = ObjectUtil.isNotNull(entityDO.getProfileIds()) ? entityDO.getProfileIds() : new HashSet<>();
        Set<String> oldProfileIds = profileBindService.selectProfileIdsByDeviceId(entityDO.getId());

        // 新增的模板
        Set<String> add = new HashSet<>(newProfileIds);
        add.removeAll(oldProfileIds);

        // 删除的模板
        Set<String> delete = new HashSet<>(oldProfileIds);
        delete.removeAll(newProfileIds);

        addProfileBind(entityDO.getId(), add);
        delete.forEach(profileId -> profileBindService.deleteByDeviceIdAndProfileId(entityDO.getId(), profileId));

        entityDO.setOperateTime(null);

        if (deviceMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The device update failed");
        }

        Device select = deviceMapper.selectById(entityDO.getId());
        select.setProfileIds(newProfileIds);
        entityDO.setDeviceName(select.getDeviceName());
        // 通知驱动更新设备
        notifyService.notifyDriverDevice(MetadataCommandTypeEnum.UPDATE, select);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Device selectById(String id) {
        Device device = deviceMapper.selectById(id);
        if (ObjectUtil.isNull(device)) {
            throw new NotFoundException();
        }
        device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(id));
        return device;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Device selectByName(String name, String tenantId) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        queryWrapper.eq(Device::getDeviceName, name);
        queryWrapper.eq(Device::getTenantId, tenantId);
        queryWrapper.last("limit 1");
        Device device = deviceMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(device)) {
            throw new NotFoundException();
        }
        device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId()));
        return device;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Device> selectByDriverId(String driverId) {
        DevicePageQuery devicePageQuery = new DevicePageQuery();
        devicePageQuery.setDriverId(driverId);
        List<Device> devices = deviceMapper.selectList(fuzzyQuery(devicePageQuery));
        if (ObjectUtil.isNull(devices) || devices.isEmpty()) {
            throw new NotFoundException();
        }
        devices.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return devices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Device> selectByProfileId(String profileId) {
        return selectByIds(profileBindService.selectDeviceIdsByProfileId(profileId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Device> selectByIds(Set<String> ids) {
        List<Device> devices = deviceMapper.selectBatchIds(ids);
        if (CollUtil.isEmpty(devices)) {
            throw new NotFoundException();
        }
        devices.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return devices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Device> list(DevicePageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return deviceMapper.selectPageWithProfile(queryDTO.getPage().convert(), customFuzzyQuery(queryDTO), queryDTO.getProfileId());
    }

    private LambdaQueryWrapper<Device> fuzzyQuery(DevicePageQuery query) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        if (ObjectUtil.isNotEmpty(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getDeviceName()), Device::getDeviceName, query.getDeviceName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getDeviceCode()), Device::getDeviceCode, query.getDeviceCode());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getDriverId()), Device::getDriverId, query.getDriverId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getEnableFlag()), Device::getEnableFlag, query.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getTenantId()), Device::getTenantId, query.getTenantId());
        }
        return queryWrapper;
    }

    @Override
    @SneakyThrows
    @Transactional
    public void importDevice(Device device, MultipartFile multipartFile) {
        List<DriverAttribute> driverAttributes = driverAttributeService.selectByDriverId(device.getDriverId(), false);
        List<PointAttribute> pointAttributes = pointAttributeService.selectByDriverId(device.getDriverId(), false);
        List<Point> points = pointService.selectByProfileIds(device.getProfileIds(), false);

        Workbook workbook = new XSSFWorkbook(multipartFile.getInputStream());
        Sheet mainSheet = workbook.getSheet("设备导入");

        if (!configIsEqual(driverAttributes, pointAttributes, points, workbook)) {
            throw new ImportException("The import template is formatted incorrectly");
        }

        for (int i = 4; i <= mainSheet.getLastRowNum(); i++) {
            // 导入设备
            Device importDevice = importDevice(device, mainSheet, i);
            log.info("正在导入设备：{}, index: {}", importDevice.getDeviceName(), 1);

            // 导入驱动属性配置
            importDriverAttributeConfig(importDevice, driverAttributes, mainSheet, i);

            // 导入位号属性配置
            importPointAttributeConfig(driverAttributes, pointAttributes, points, mainSheet, i, importDevice);
        }
    }

    @Override
    @SneakyThrows
    public Path generateImportTemplate(Device device) {
        List<DriverAttribute> driverAttributes = driverAttributeService.selectByDriverId(device.getDriverId(), false);
        List<PointAttribute> pointAttributes = pointAttributeService.selectByDriverId(device.getDriverId(), false);
        List<Point> points = pointService.selectByProfileIds(device.getProfileIds(), false);

        Workbook workbook = new XSSFWorkbook();
        CellStyle cellStyle = PoiUtil.getCenterCellStyle(workbook);

        // 设置主工作表
        Sheet mainSheet = workbook.createSheet("设备导入");
        mainSheet.setDefaultColumnWidth(25);

        // 设置配置工作表
        configConfigSheet(driverAttributes, pointAttributes, points, workbook);

        // 设置说明
        Row remarkRow = mainSheet.createRow(0);
        PoiUtil.createCell(remarkRow, 0, "说明：请从第5行开始添加待导入的设备数据");
        PoiUtil.mergedRegion(mainSheet, 0, 0, 0, 2 + driverAttributes.size() + pointAttributes.size() * points.size() - 1);

        // 设置设备列
        Row titleRow = mainSheet.createRow(1);
        PoiUtil.createCellWithStyle(titleRow, 0, "设备名称", cellStyle);
        PoiUtil.createCellWithStyle(titleRow, 1, "设备描述", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 3, 0, 0);
        PoiUtil.mergedRegion(mainSheet, 1, 3, 1, 1);

        Row attributeRow = mainSheet.createRow(3);
        // 设置驱动属性配置列
        configAttributeCell(driverAttributes, mainSheet, titleRow, attributeRow, cellStyle);
        // 设置位号属性配置列
        configPointCell(driverAttributes, pointAttributes, points, mainSheet, titleRow, attributeRow, cellStyle);

        // 生成设备导入模板
        return generateTemplate(workbook);
    }

    @Override
    public Long count() {
        return deviceMapper.selectCount(new QueryWrapper<>());
    }

    @Override
    public Long dataCount() {
        return deviceMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .map(Base::getId)
                .mapToLong(deviceId -> mongoTemplate.getCollection(StorageConstant.POINT_VALUE_PREFIX + deviceId).countDocuments())
                .sum();
    }

    @Override
    public List<Device> selectAllByDriverId(String driverId, String tenantId) {
        return deviceMapper.selectList(new LambdaQueryWrapper<Device>().eq(Device::getDriverId, driverId).eq(Device::getTenantId, tenantId));
    }

    /**
     * 导入设备
     *
     * @param device    Device
     * @param mainSheet Sheet
     * @param rowIndex  Row Index
     * @return
     */
    private Device importDevice(Device device, Sheet mainSheet, int rowIndex) {
        Device importDevice = getDevice(device, mainSheet, rowIndex);
        try {
            add(importDevice);
        } catch (Exception e) {
            log.error("导入设备: {}, 错误：{}", device, rowIndex);
            throw new ServiceException(e.getMessage());
        }
        return importDevice;
    }

    /**
     * @param importDevice     Device
     * @param driverAttributes DriverAttribute
     * @param mainSheet        Sheet
     * @param rowIndex         Row Index
     */
    private void importDriverAttributeConfig(Device importDevice, List<DriverAttribute> driverAttributes, Sheet mainSheet, int rowIndex) {
        for (int j = 0; j < driverAttributes.size(); j++) {
            DriverAttributeConfig importAttributeConfig = getDriverAttributeConfig(importDevice, driverAttributes.get(j), mainSheet, rowIndex, 2 + j);
            driverAttributeConfigService.add(importAttributeConfig);
        }
    }

    private static Device getDevice(Device device, Sheet mainSheet, int rowIndex) {
        String deviceName = PoiUtil.getCellStringValue(mainSheet, rowIndex, 0);
        if (CharSequenceUtil.isEmpty(deviceName)) {
            throw new ImportException("The device name in line {} of the import file is empty", rowIndex + 1);
        }

        Device importDevice = new Device();
        importDevice.setDeviceName(deviceName);
        importDevice.setDriverId(device.getDriverId());
        importDevice.setProfileIds(device.getProfileIds());
        String deviceRemark = PoiUtil.getCellStringValue(mainSheet, rowIndex, 1);
        importDevice.setRemark(deviceRemark);
        importDevice.setTenantId(device.getTenantId());

        return importDevice;
    }

    private static DriverAttributeConfig getDriverAttributeConfig(Device device, DriverAttribute driverAttribute, Sheet mainSheet, int rowIndex, int cellIndex) {
        DriverAttributeConfig importAttributeConfig = new DriverAttributeConfig();
        importAttributeConfig.setDriverAttributeId(driverAttribute.getId());
        importAttributeConfig.setDeviceId(device.getId());
        String attributeValue = PoiUtil.getCellStringValue(mainSheet, rowIndex, cellIndex);
        importAttributeConfig.setConfigValue(attributeValue);
        importAttributeConfig.setTenantId(device.getTenantId());

        return importAttributeConfig;
    }

    private void importPointAttributeConfig(List<DriverAttribute> driverAttributes, List<PointAttribute> pointAttributes, List<Point> points, Sheet mainSheet, int i, Device importDevice) {
        for (int j = 0; j < points.size(); j++) {
            for (int k = 0; k < pointAttributes.size(); k++) {
                PointAttributeConfig importAttributeConfig = getPointAttributeConfig(importDevice, points.get(j), pointAttributes.get(k), mainSheet, i, 2 + driverAttributes.size() + k * pointAttributes.size() + j);
                pointAttributeConfigService.add(importAttributeConfig);
            }
        }
    }

    private static PointAttributeConfig getPointAttributeConfig(Device device, Point point, PointAttribute pointAttribute, Sheet mainSheet, int rowIndex, int cellIndex) {
        PointAttributeConfig importAttributeConfig = new PointAttributeConfig();
        importAttributeConfig.setPointAttributeId(pointAttribute.getId());
        importAttributeConfig.setDeviceId(device.getId());
        importAttributeConfig.setPointId(point.getId());
        String attributeValue = PoiUtil.getCellStringValue(mainSheet, rowIndex, cellIndex);
        importAttributeConfig.setConfigValue(attributeValue);
        importAttributeConfig.setTenantId(device.getTenantId());

        return importAttributeConfig;
    }

    /**
     * 判断配置数据是否一致
     *
     * @param driverAttributes DriverAttribute Array
     * @param pointAttributes  PointAttribute Array
     * @param points           Point Array
     * @param workbook         Workbook
     */
    private boolean configIsEqual(List<DriverAttribute> driverAttributes, List<PointAttribute> pointAttributes, List<Point> points, Workbook workbook) {
        Sheet configSheet = workbook.getSheet("配置（忽略）");
        String driverAttributesValueNew = JsonUtil.toJsonString(driverAttributes);
        String driverAttributesValueOld = PoiUtil.getCellStringValue(configSheet, 0, 0);
        if (!driverAttributesValueNew.equals(driverAttributesValueOld)) {
            return false;
        }

        String pointAttributesValueNewd = JsonUtil.toJsonString(pointAttributes);
        String pointAttributesValueOld = PoiUtil.getCellStringValue(configSheet, 1, 0);
        if (!pointAttributesValueNewd.equals(pointAttributesValueOld)) {
            return false;
        }

        String pointsValueNew = JsonUtil.toJsonString(points);
        String pointsValueOld = PoiUtil.getCellStringValue(configSheet, 2, 0);

        return pointsValueNew.equals(pointsValueOld);
    }

    /**
     * 设置驱动属性配置列
     *
     * @param driverAttributes DriverAttribute Array
     * @param mainSheet        Main Sheet
     * @param titleRow         Title Row
     * @param attributeRow     Attribute Row
     */
    private void configAttributeCell(List<DriverAttribute> driverAttributes, Sheet mainSheet, Row titleRow, Row attributeRow, CellStyle cellStyle) {
        if (driverAttributes.isEmpty()) {
            return;
        }

        PoiUtil.createCellWithStyle(titleRow, 2, "驱动属性配置", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 2, 2, 2 + driverAttributes.size() - 1);
        for (int i = 0; i < driverAttributes.size(); i++) {
            PoiUtil.createCellWithStyle(attributeRow, 2 + i, driverAttributes.get(i).getDisplayName(), cellStyle);
        }

    }

    /**
     * 设置配置工作表
     *
     * @param driverAttributes DriverAttribute Array
     * @param pointAttributes  PointAttribute Array
     * @param points           Point Array
     * @param workbook         Workbook
     */
    private void configConfigSheet(List<DriverAttribute> driverAttributes, List<PointAttribute> pointAttributes, List<Point> points, Workbook workbook) {
        Sheet configSheet = workbook.createSheet("配置（忽略）");
        Row driverAttributesRow = configSheet.createRow(0);
        Row pointAttributesRow = configSheet.createRow(1);
        Row pointsRow = configSheet.createRow(2);
        PoiUtil.createCell(driverAttributesRow, 0, JsonUtil.toJsonString(driverAttributes));
        PoiUtil.createCell(pointAttributesRow, 0, JsonUtil.toJsonString(pointAttributes));
        PoiUtil.createCell(pointsRow, 0, JsonUtil.toJsonString(points));
    }

    /**
     * 设置位号属性配置列
     *
     * @param driverAttributes DriverAttribute Array
     * @param pointAttributes  PointAttribute Array
     * @param points           Point  Array
     * @param mainSheet        Main Sheet
     * @param titleRow         Title Row
     * @param attributeRow     Attribute Row
     * @param cellStyle        CellStyle
     */
    private void configPointCell(List<DriverAttribute> driverAttributes, List<PointAttribute> pointAttributes, List<Point> points, Sheet mainSheet, Row titleRow, Row attributeRow, CellStyle cellStyle) {
        if (pointAttributes.isEmpty()) {
            return;
        }

        Row pointRow = mainSheet.createRow(2);
        PoiUtil.createCellWithStyle(titleRow, 2 + driverAttributes.size(), "位号属性配置", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 1, 2 + driverAttributes.size(), 2 + driverAttributes.size() + pointAttributes.size() * points.size() - 1);
        for (int i = 0; i < points.size(); i++) {
            PoiUtil.createCellWithStyle(pointRow, 2 + driverAttributes.size() + i * pointAttributes.size(), points.get(i).getPointName(), cellStyle);
            PoiUtil.mergedRegion(mainSheet, 2, 2, 2 + driverAttributes.size() + i * pointAttributes.size(), 2 + driverAttributes.size() + i * pointAttributes.size() + pointAttributes.size() - 1);
            for (int j = 0; j < pointAttributes.size(); j++) {
                PoiUtil.createCellWithStyle(attributeRow, 2 + driverAttributes.size() + i * pointAttributes.size() + j, pointAttributes.get(j).getDisplayName(), cellStyle);
            }
        }
    }

    /**
     * 生成设备导入模板
     *
     * @param workbook Workbook
     * @return Path
     * @throws IOException
     */
    private Path generateTemplate(Workbook workbook) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        org.springframework.core.io.Resource resource = resolver.getResource("classpath:/");
        Path resourcePath = Paths.get(resource.getURI());
        String fileName = CharSequenceUtil.format("dc3_device_import_template_{}.xlsx", System.currentTimeMillis());
        Path path = resourcePath.resolve(fileName);
        FileOutputStream outputStream = new FileOutputStream(path.toUri().getPath());
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
        return path;
    }

    public LambdaQueryWrapper<Device> customFuzzyQuery(DevicePageQuery devicePageQuery) {
        QueryWrapper<Device> queryWrapper = Wrappers.query();
        queryWrapper.eq("dd.deleted", 0);
        if (ObjectUtil.isNotNull(devicePageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(devicePageQuery.getDeviceName()), "dd.device_name", devicePageQuery.getDeviceName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(devicePageQuery.getDeviceCode()), "dd.device_code", devicePageQuery.getDeviceCode());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(devicePageQuery.getDriverId()), "dd.driver_id", devicePageQuery.getDriverId());
            queryWrapper.eq(ObjectUtil.isNotNull(devicePageQuery.getEnableFlag()), "dd.enable_flag", devicePageQuery.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(devicePageQuery.getTenantId()), "dd.tenant_id", devicePageQuery.getTenantId());
        }
        return queryWrapper.lambda();
    }

    private void addProfileBind(String deviceId, Set<String> profileIds) {
        if (CollUtil.isEmpty(profileIds)) {
            return;
        }

        profileIds.forEach(profileId -> {
            try {
                profileService.selectById(profileId);
                profileBindService.add(new ProfileBind(profileId, deviceId));

                List<Point> points = pointService.selectByProfileId(profileId);
                // 通知驱动新增位号
                points.forEach(point -> notifyService.notifyDriverPoint(MetadataCommandTypeEnum.ADD, point));
            } catch (Exception ignored) {
                // nothing to do
            }
        });

    }

}
