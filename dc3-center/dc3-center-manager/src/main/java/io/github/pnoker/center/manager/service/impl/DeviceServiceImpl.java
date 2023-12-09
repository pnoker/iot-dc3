/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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
import io.github.pnoker.center.manager.entity.bo.DeviceBO;
import io.github.pnoker.center.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.center.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.github.pnoker.center.manager.entity.query.DeviceBOPageQuery;
import io.github.pnoker.center.manager.manager.DeviceManager;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.constant.driver.StorageConstant;
import io.github.pnoker.common.entity.base.Base;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.*;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.PageUtil;
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
    private DeviceManager deviceManager;

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
    public void save(DeviceBO entityBO) {
        if (deviceManager.save(entityBO) < 1) {
            throw new AddException("The device {} add failed", entityBO.getDeviceName());
        }

        addProfileBind(entityBO.getId(), entityBO.getProfileIds());

        // 通知驱动新增
        DeviceBO deviceBO = deviceManager.getById(entityBO.getId());
        List<Profile> profiles = profileService.selectByDeviceId(entityBO.getId());
        // ?/pnoker 同步给驱动的设备需要profile id set吗
        deviceBO.setProfileIds(profiles.stream().map(Profile::getId).collect(Collectors.toSet()));
        notifyService.notifyDriverDevice(MetadataCommandTypeEnum.ADD, deviceBO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void remove(Long id) {
        DeviceBO deviceBO = selectById(id);
        if (ObjectUtil.isNull(deviceBO)) {
            throw new NotFoundException("The device does not exist");
        }

        if (!profileBindService.deleteByDeviceId(id)) {
            throw new DeleteException("The profile bind delete failed");
        }

        if (deviceManager.removeById(id) < 1) {
            throw new DeleteException("The device delete failed");
        }

        // 通知驱动删除设备
        notifyService.notifyDriverDevice(MetadataCommandTypeEnum.DELETE, deviceBO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(DeviceBO entityBO) {
        selectById(entityBO.getId());

        Set<Long> newProfileIds = ObjectUtil.isNotNull(entityBO.getProfileIds()) ? entityBO.getProfileIds() : new HashSet<>();
        Set<Long> oldProfileIds = profileBindService.selectProfileIdsByDeviceId(entityBO.getId());

        // 新增的模板
        Set<Long> add = new HashSet<>(newProfileIds);
        add.removeAll(oldProfileIds);

        // 删除的模板
        Set<Long> delete = new HashSet<>(oldProfileIds);
        delete.removeAll(newProfileIds);

        addProfileBind(entityBO.getId(), add);
        delete.forEach(profileId -> profileBindService.deleteByDeviceIdAndProfileId(entityBO.getId(), profileId));

        entityBO.setOperateTime(null);

        if (deviceManager.updateById(entityBO) < 1) {
            throw new UpdateException("The device update failed");
        }

        DeviceBO select = deviceManager.selectById(entityBO.getId());
        select.setProfileIds(newProfileIds);
        entityBO.setDeviceName(select.getDeviceName());
        // 通知驱动更新设备
        notifyService.notifyDriverDevice(MetadataCommandTypeEnum.UPDATE, select);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeviceBO selectById(Long id) {
        DeviceBO deviceBO = deviceManager.selectById(id);
        if (ObjectUtil.isNull(deviceBO)) {
            throw new NotFoundException();
        }
        deviceBO.setProfileIds(profileBindService.selectProfileIdsByDeviceId(id));
        return deviceBO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeviceBO selectByName(String name, Long tenantId) {
        LambdaQueryWrapper<DeviceBO> queryWrapper = Wrappers.<DeviceBO>query().lambda();
        queryWrapper.eq(DeviceBO::getDeviceName, name);
        queryWrapper.eq(DeviceBO::getTenantId, tenantId);
        queryWrapper.last("limit 1");
        DeviceBO deviceBO = deviceManager.selectOne(queryWrapper);
        if (ObjectUtil.isNull(deviceBO)) {
            throw new NotFoundException();
        }
        deviceBO.setProfileIds(profileBindService.selectProfileIdsByDeviceId(deviceBO.getId()));
        return deviceBO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DeviceBO> selectByDriverId(Long driverId) {
        DeviceBOPageQuery devicePageQuery = new DeviceBOPageQuery();
        devicePageQuery.setDriverId(driverId);
        List<DeviceBO> deviceBOS = deviceManager.selectList(fuzzyQuery(devicePageQuery));
        if (ObjectUtil.isNull(deviceBOS) || deviceBOS.isEmpty()) {
            throw new NotFoundException();
        }
        deviceBOS.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return deviceBOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DeviceBO> selectByProfileId(Long profileId) {
        return selectByIds(profileBindService.selectDeviceIdsByProfileId(profileId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DeviceBO> selectByIds(Set<Long> ids) {
        List<DeviceBO> deviceBOS = deviceManager.selectBatchIds(ids);
        if (CollUtil.isEmpty(deviceBOS)) {
            throw new NotFoundException();
        }
        deviceBOS.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return deviceBOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DeviceBO> selectByPage(DeviceBOPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return deviceManager.selectPageWithProfile(PageUtil.page(entityQuery.getPage()), customFuzzyQuery(entityQuery), entityQuery.getProfileId());
    }

    private LambdaQueryWrapper<DeviceBO> fuzzyQuery(DeviceBOPageQuery query) {
        LambdaQueryWrapper<DeviceBO> queryWrapper = Wrappers.<DeviceBO>query().lambda();
        if (ObjectUtil.isNotEmpty(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getDeviceName()), DeviceBO::getDeviceName, query.getDeviceName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getDeviceCode()), DeviceBO::getDeviceCode, query.getDeviceCode());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getDriverId()), DeviceBO::getDriverId, query.getDriverId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getEnableFlag()), DeviceBO::getEnableFlag, query.getEnableFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getTenantId()), DeviceBO::getTenantId, query.getTenantId());
        }
        return queryWrapper;
    }

    @Override
    @SneakyThrows
    @Transactional
    public void importDevice(DeviceBO deviceBO, MultipartFile multipartFile) {
        List<DriverAttributeBO> driverAttributeBOS = driverAttributeService.selectByDriverId(deviceBO.getDriverId(), false);
        List<PointAttribute> pointAttributes = pointAttributeService.selectByDriverId(deviceBO.getDriverId(), false);
        List<Point> points = pointService.selectByProfileIds(deviceBO.getProfileIds(), false);

        Workbook workbook = new XSSFWorkbook(multipartFile.getInputStream());
        Sheet mainSheet = workbook.getSheet("设备导入");

        if (!configIsEqual(driverAttributeBOS, pointAttributes, points, workbook)) {
            throw new ImportException("The import template is formatted incorrectly");
        }

        for (int i = 4; i <= mainSheet.getLastRowNum(); i++) {
            // 导入设备
            DeviceBO importDeviceBO = importDevice(deviceBO, mainSheet, i);
            log.info("正在导入设备：{}, index: {}", importDeviceBO.getDeviceName(), 1);

            // 导入驱动属性配置
            importDriverAttributeConfig(importDeviceBO, driverAttributeBOS, mainSheet, i);

            // 导入位号属性配置
            importPointAttributeConfig(driverAttributeBOS, pointAttributes, points, mainSheet, i, importDeviceBO);
        }
    }

    @Override
    @SneakyThrows
    public Path generateImportTemplate(DeviceBO deviceBO) {
        List<DriverAttributeBO> driverAttributeBOS = driverAttributeService.selectByDriverId(deviceBO.getDriverId(), false);
        List<PointAttribute> pointAttributes = pointAttributeService.selectByDriverId(deviceBO.getDriverId(), false);
        List<Point> points = pointService.selectByProfileIds(deviceBO.getProfileIds(), false);

        Workbook workbook = new XSSFWorkbook();
        CellStyle cellStyle = PoiUtil.getCenterCellStyle(workbook);

        // 设置主工作表
        Sheet mainSheet = workbook.createSheet("设备导入");
        mainSheet.setDefaultColumnWidth(25);

        // 设置配置工作表
        configConfigSheet(driverAttributeBOS, pointAttributes, points, workbook);

        // 设置说明
        Row remarkRow = mainSheet.createRow(0);
        PoiUtil.createCell(remarkRow, 0, "说明：请从第5行开始添加待导入的设备数据");
        PoiUtil.mergedRegion(mainSheet, 0, 0, 0, 2 + driverAttributeBOS.size() + pointAttributes.size() * points.size() - 1);

        // 设置设备列
        Row titleRow = mainSheet.createRow(1);
        PoiUtil.createCellWithStyle(titleRow, 0, "设备名称", cellStyle);
        PoiUtil.createCellWithStyle(titleRow, 1, "设备描述", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 3, 0, 0);
        PoiUtil.mergedRegion(mainSheet, 1, 3, 1, 1);

        Row attributeRow = mainSheet.createRow(3);
        // 设置驱动属性配置列
        configAttributeCell(driverAttributeBOS, mainSheet, titleRow, attributeRow, cellStyle);
        // 设置位号属性配置列
        configPointCell(driverAttributeBOS, pointAttributes, points, mainSheet, titleRow, attributeRow, cellStyle);

        // 生成设备导入模板
        return generateTemplate(workbook);
    }

    @Override
    public Long count() {
        return deviceManager.selectCount(new QueryWrapper<>());
    }

    @Override
    public Long dataCount() {
        return deviceManager.selectList(new LambdaQueryWrapper<>()).stream()
                .map(Base::getId)
                .mapToLong(deviceId -> mongoTemplate.getCollection(StorageConstant.POINT_VALUE_PREFIX + deviceId).countDocuments())
                .sum();
    }

    @Override
    public List<DeviceBO> selectAllByDriverId(Long driverId, Long tenantId) {
        return deviceManager.selectList(new LambdaQueryWrapper<DeviceBO>().eq(DeviceBO::getDriverId, driverId).eq(DeviceBO::getTenantId, tenantId));
    }

    /**
     * 导入设备
     *
     * @param deviceBO  Device
     * @param mainSheet Sheet
     * @param rowIndex  Row Index
     * @return
     */
    private DeviceBO importDevice(DeviceBO deviceBO, Sheet mainSheet, int rowIndex) {
        DeviceBO importDeviceBO = getDevice(deviceBO, mainSheet, rowIndex);
        try {
            save(importDeviceBO);
        } catch (Exception e) {
            log.error("导入设备: {}, 错误：{}", deviceBO, rowIndex);
            throw new ServiceException(e.getMessage());
        }
        return importDeviceBO;
    }

    /**
     * @param importDeviceBO     Device
     * @param driverAttributeBOS DriverAttribute
     * @param mainSheet          Sheet
     * @param rowIndex           Row Index
     */
    private void importDriverAttributeConfig(DeviceBO importDeviceBO, List<DriverAttributeBO> driverAttributeBOS, Sheet mainSheet, int rowIndex) {
        for (int j = 0; j < driverAttributeBOS.size(); j++) {
            DriverAttributeConfigBO importAttributeConfig = getDriverAttributeConfig(importDeviceBO, driverAttributeBOS.get(j), mainSheet, rowIndex, 2 + j);
            driverAttributeConfigService.save(importAttributeConfig);
        }
    }

    private static DeviceBO getDevice(DeviceBO deviceBO, Sheet mainSheet, int rowIndex) {
        String deviceName = PoiUtil.getCellStringValue(mainSheet, rowIndex, 0);
        if (CharSequenceUtil.isEmpty(deviceName)) {
            throw new ImportException("The device name in line {} of the import file is empty", rowIndex + 1);
        }

        DeviceBO importDeviceBO = new DeviceBO();
        importDeviceBO.setDeviceName(deviceName);
        importDeviceBO.setDriverId(deviceBO.getDriverId());
        importDeviceBO.setProfileIds(deviceBO.getProfileIds());
        String deviceRemark = PoiUtil.getCellStringValue(mainSheet, rowIndex, 1);
        importDeviceBO.setRemark(deviceRemark);
        importDeviceBO.setTenantId(deviceBO.getTenantId());

        return importDeviceBO;
    }

    private static DriverAttributeConfigBO getDriverAttributeConfig(DeviceBO deviceBO, DriverAttributeBO driverAttributeBO, Sheet mainSheet, int rowIndex, int cellIndex) {
        DriverAttributeConfigBO importAttributeConfig = new DriverAttributeConfigBO();
        importAttributeConfig.setDriverAttributeId(driverAttributeBO.getId());
        importAttributeConfig.setDeviceId(deviceBO.getId());
        String attributeValue = PoiUtil.getCellStringValue(mainSheet, rowIndex, cellIndex);
        importAttributeConfig.setConfigValue(attributeValue);
        importAttributeConfig.setTenantId(deviceBO.getTenantId());

        return importAttributeConfig;
    }

    private void importPointAttributeConfig(List<DriverAttributeBO> driverAttributeBOS, List<PointAttribute> pointAttributes, List<Point> points, Sheet mainSheet, int i, DeviceBO importDeviceBO) {
        for (int j = 0; j < points.size(); j++) {
            for (int k = 0; k < pointAttributes.size(); k++) {
                PointAttributeConfig importAttributeConfig = getPointAttributeConfig(importDeviceBO, points.get(j), pointAttributes.get(k), mainSheet, i, 2 + driverAttributeBOS.size() + k * pointAttributes.size() + j);
                pointAttributeConfigService.save(importAttributeConfig);
            }
        }
    }

    private static PointAttributeConfig getPointAttributeConfig(DeviceBO deviceBO, Point point, PointAttribute pointAttribute, Sheet mainSheet, int rowIndex, int cellIndex) {
        PointAttributeConfig importAttributeConfig = new PointAttributeConfig();
        importAttributeConfig.setPointAttributeId(pointAttribute.getId());
        importAttributeConfig.setDeviceId(deviceBO.getId());
        importAttributeConfig.setPointId(point.getId());
        String attributeValue = PoiUtil.getCellStringValue(mainSheet, rowIndex, cellIndex);
        importAttributeConfig.setConfigValue(attributeValue);
        importAttributeConfig.setTenantId(deviceBO.getTenantId());

        return importAttributeConfig;
    }

    /**
     * 判断配置数据是否一致
     *
     * @param driverAttributeBOS DriverAttribute Array
     * @param pointAttributes    PointAttribute Array
     * @param points             Point Array
     * @param workbook           Workbook
     */
    private boolean configIsEqual(List<DriverAttributeBO> driverAttributeBOS, List<PointAttribute> pointAttributes, List<Point> points, Workbook workbook) {
        Sheet configSheet = workbook.getSheet("配置（忽略）");
        String driverAttributesValueNew = JsonUtil.toJsonString(driverAttributeBOS);
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
     * @param driverAttributeBOS DriverAttribute Array
     * @param mainSheet          Main Sheet
     * @param titleRow           Title Row
     * @param attributeRow       Attribute Row
     */
    private void configAttributeCell(List<DriverAttributeBO> driverAttributeBOS, Sheet mainSheet, Row titleRow, Row attributeRow, CellStyle cellStyle) {
        if (driverAttributeBOS.isEmpty()) {
            return;
        }

        PoiUtil.createCellWithStyle(titleRow, 2, "驱动属性配置", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 2, 2, 2 + driverAttributeBOS.size() - 1);
        for (int i = 0; i < driverAttributeBOS.size(); i++) {
            PoiUtil.createCellWithStyle(attributeRow, 2 + i, driverAttributeBOS.get(i).getDisplayName(), cellStyle);
        }

    }

    /**
     * 设置配置工作表
     *
     * @param driverAttributeBOS DriverAttribute Array
     * @param pointAttributes    PointAttribute Array
     * @param points             Point Array
     * @param workbook           Workbook
     */
    private void configConfigSheet(List<DriverAttributeBO> driverAttributeBOS, List<PointAttribute> pointAttributes, List<Point> points, Workbook workbook) {
        Sheet configSheet = workbook.createSheet("配置（忽略）");
        Row driverAttributesRow = configSheet.createRow(0);
        Row pointAttributesRow = configSheet.createRow(1);
        Row pointsRow = configSheet.createRow(2);
        PoiUtil.createCell(driverAttributesRow, 0, JsonUtil.toJsonString(driverAttributeBOS));
        PoiUtil.createCell(pointAttributesRow, 0, JsonUtil.toJsonString(pointAttributes));
        PoiUtil.createCell(pointsRow, 0, JsonUtil.toJsonString(points));
    }

    /**
     * 设置位号属性配置列
     *
     * @param driverAttributeBOS DriverAttribute Array
     * @param pointAttributes    PointAttribute Array
     * @param points             Point  Array
     * @param mainSheet          Main Sheet
     * @param titleRow           Title Row
     * @param attributeRow       Attribute Row
     * @param cellStyle          CellStyle
     */
    private void configPointCell(List<DriverAttributeBO> driverAttributeBOS, List<PointAttribute> pointAttributes, List<Point> points, Sheet mainSheet, Row titleRow, Row attributeRow, CellStyle cellStyle) {
        if (pointAttributes.isEmpty()) {
            return;
        }

        Row pointRow = mainSheet.createRow(2);
        PoiUtil.createCellWithStyle(titleRow, 2 + driverAttributeBOS.size(), "位号属性配置", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 1, 2 + driverAttributeBOS.size(), 2 + driverAttributeBOS.size() + pointAttributes.size() * points.size() - 1);
        for (int i = 0; i < points.size(); i++) {
            PoiUtil.createCellWithStyle(pointRow, 2 + driverAttributeBOS.size() + i * pointAttributes.size(), points.get(i).getPointName(), cellStyle);
            PoiUtil.mergedRegion(mainSheet, 2, 2, 2 + driverAttributeBOS.size() + i * pointAttributes.size(), 2 + driverAttributeBOS.size() + i * pointAttributes.size() + pointAttributes.size() - 1);
            for (int j = 0; j < pointAttributes.size(); j++) {
                PoiUtil.createCellWithStyle(attributeRow, 2 + driverAttributeBOS.size() + i * pointAttributes.size() + j, pointAttributes.get(j).getDisplayName(), cellStyle);
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

    public LambdaQueryWrapper<DeviceBO> customFuzzyQuery(DeviceBOPageQuery devicePageQuery) {
        QueryWrapper<DeviceBO> queryWrapper = Wrappers.query();
        queryWrapper.eq("dd.deleted", 0);
        if (ObjectUtil.isNotNull(devicePageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(devicePageQuery.getDeviceName()), "dd.device_name", devicePageQuery.getDeviceName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(devicePageQuery.getDeviceCode()), "dd.device_code", devicePageQuery.getDeviceCode());
            queryWrapper.eq(ObjectUtil.isNotEmpty(devicePageQuery.getDriverId()), "dd.driver_id", devicePageQuery.getDriverId());
            queryWrapper.eq(ObjectUtil.isNotNull(devicePageQuery.getEnableFlag()), "dd.enable_flag", devicePageQuery.getEnableFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(devicePageQuery.getTenantId()), "dd.tenant_id", devicePageQuery.getTenantId());
        }
        return queryWrapper.lambda();
    }

    private void addProfileBind(Long deviceId, Set<Long> profileIds) {
        if (CollUtil.isEmpty(profileIds)) {
            return;
        }

        profileIds.forEach(profileId -> {
            try {
                profileService.selectById(profileId);
                profileBindService.save(new ProfileBind(profileId, deviceId));

                List<Point> points = pointService.selectByProfileId(profileId);
                // 通知驱动新增位号
                points.forEach(point -> notifyService.notifyDriverPoint(MetadataCommandTypeEnum.ADD, point));
            } catch (Exception ignored) {
                // nothing to do
            }
        });

    }

    /**
     * 重复性校验
     *
     * @param entityDO {@link DeviceDO}
     * @param isUpdate 是否为更新操作
     * @return 是否重复
     */
    private boolean checkDuplicate(DeviceDO entityDO, boolean isUpdate) {
        LambdaQueryWrapper<DeviceDO> queryWrapper = Wrappers.<DeviceDO>query().lambda();
        queryWrapper.eq(DeviceDO::getDeviceName, entityDO.getDeviceName());
        queryWrapper.eq(DeviceDO::getTenantId, entityDO.getTenantId());
        queryWrapper.last("limit 1");
        DeviceDO one = deviceManager.getOne(queryWrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityDO.getId());
    }

    /**
     * 根据 ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link DeviceDO}
     */
    private DeviceDO getDOById(Long id, boolean throwException) {
        DeviceDO entityDO = deviceManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("The label does not exist");
        }
        return entityDO;
    }

}
