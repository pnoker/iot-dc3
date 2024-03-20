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
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.biz.DriverNotifyService;
import io.github.pnoker.center.manager.dal.DeviceManager;
import io.github.pnoker.center.manager.entity.bo.*;
import io.github.pnoker.center.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.github.pnoker.center.manager.entity.query.DeviceQuery;
import io.github.pnoker.center.manager.mapper.DeviceMapper;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.exception.*;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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
    private DeviceBuilder deviceBuilder;

    @Resource
    private DeviceManager deviceManager;
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
    private DriverNotifyService driverNotifyService;

    @Override
    public void save(DeviceBO entityBO) {
        checkDuplicate(entityBO, false, true);

        DeviceDO entityDO = deviceBuilder.buildDOByBO(entityBO);
        if (!deviceManager.save(entityDO)) {
            throw new AddException("设备创建失败");
        }

        addProfileBind(entityDO, entityBO.getProfileIds());

        // 通知驱动新增
        DeviceBO deviceBO = selectById(entityDO.getId());
        List<ProfileBO> profileBOS = profileService.selectByDeviceId(entityDO.getId());
        deviceBO.setProfileIds(profileBOS.stream().map(ProfileBO::getId).collect(Collectors.toSet()));
        driverNotifyService.notifyDevice(MetadataCommandTypeEnum.ADD, deviceBO);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        DeviceDO entityDO = getDOById(id, true);

        // 删除设备之前需要检查该设备是否存在关联
        if (!profileBindService.removeByDeviceId(id)) {
            throw new DeleteException("The profile bind delete failed");
        }

        if (!deviceManager.removeById(id)) {
            throw new DeleteException("The device delete failed");
        }

        // 通知驱动删除设备
        DeviceBO entityBO = deviceBuilder.buildBOByDO(entityDO);
        driverNotifyService.notifyDevice(MetadataCommandTypeEnum.DELETE, entityBO);
    }


    @Override
    public void update(DeviceBO entityBO) {
        DeviceDO entityDO = getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        Set<Long> newProfileIds = ObjectUtil.isNotNull(entityBO.getProfileIds()) ? entityBO.getProfileIds() : new HashSet<>(4);
        Set<Long> oldProfileIds = profileBindService.selectProfileIdsByDeviceId(entityBO.getId());

        // 新增的模板
        Set<Long> add = new HashSet<>(newProfileIds);
        add.removeAll(oldProfileIds);
        addProfileBind(entityDO, add);

        // 删除的模板
        Set<Long> delete = new HashSet<>(oldProfileIds);
        delete.removeAll(newProfileIds);
        delete.forEach(profileId -> profileBindService.removeByDeviceIdAndProfileId(entityBO.getId(), profileId));

        entityDO = deviceBuilder.buildDOByBO(entityBO);
        entityBO.setOperateTime(null);
        if (!deviceManager.updateById(entityDO)) {
            throw new UpdateException("The device update failed");
        }

        DeviceBO select = selectById(entityBO.getId());
        select.setProfileIds(CollUtil.isEmpty(newProfileIds) ? oldProfileIds : newProfileIds);
        entityBO.setDeviceName(select.getDeviceName());
        // 通知驱动更新设备
        driverNotifyService.notifyDevice(MetadataCommandTypeEnum.UPDATE, select);
    }


    @Override
    public DeviceBO selectById(Long id) {
        DeviceDO entityDO = getDOById(id, true);
        DeviceBO entityBO = deviceBuilder.buildBOByDO(entityDO);
        entityBO.setProfileIds(profileBindService.selectProfileIdsByDeviceId(id));
        return entityBO;
    }

    @Override
    public DeviceBO selectByName(String name, Long tenantId) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDeviceName, name);
        wrapper.eq(DeviceDO::getTenantId, tenantId);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DeviceDO entityDO = deviceManager.getOne(wrapper);
        DeviceBO entityBO = deviceBuilder.buildBOByDO(entityDO);
        entityBO.setProfileIds(profileBindService.selectProfileIdsByDeviceId(entityDO.getId()));
        return entityBO;
    }

    @Override
    public DeviceBO selectByCode(String code, Long tenantId) {
        LambdaQueryChainWrapper<DeviceDO> wrapper = deviceManager.lambdaQuery()
                .eq(DeviceDO::getDeviceCode, code)
                .eq(DeviceDO::getTenantId, tenantId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        DeviceDO entityDO = wrapper.one();
        DeviceBO entityBO = deviceBuilder.buildBOByDO(entityDO);
        entityBO.setProfileIds(profileBindService.selectProfileIdsByDeviceId(entityDO.getId()));
        return entityBO;
    }

    @Override
    public List<DeviceBO> selectByDriverId(Long driverId) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDriverId, driverId);
        List<DeviceDO> entityDOS = deviceManager.list(wrapper);
        List<DeviceBO> deviceBOS = deviceBuilder.buildBOListByDOList(entityDOS);
        deviceBOS.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return deviceBOS;
    }

    @Override
    public List<DeviceBO> selectByProfileId(Long profileId) {
        return selectByIds(profileBindService.selectDeviceIdsByProfileId(profileId));
    }

    @Override
    public List<DeviceBO> selectByIds(Set<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<DeviceDO> entityDOS = deviceManager.listByIds(ids);
        List<DeviceBO> deviceBOS = deviceBuilder.buildBOListByDOList(entityDOS);
        deviceBOS.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return deviceBOS;
    }

    @Override
    public Page<DeviceBO> selectByPage(DeviceQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<DeviceDO> entityPageDO = deviceMapper.selectPageWithProfile(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery), entityQuery.getProfileId());
        return deviceBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    @SneakyThrows
    @Transactional
    public void importDevice(DeviceBO entityBO, MultipartFile multipartFile) {
        List<DriverAttributeBO> driverAttributeBOS = driverAttributeService.selectByDriverId(entityBO.getDriverId());
        List<PointAttributeBO> pointAttributeBOS = pointAttributeService.selectByDriverId(entityBO.getDriverId(), false);
        List<PointBO> pointBOS = pointService.selectByProfileIds(entityBO.getProfileIds());

        Workbook workbook = new XSSFWorkbook(multipartFile.getInputStream());
        Sheet mainSheet = workbook.getSheet("设备导入");

        if (!configIsEqual(driverAttributeBOS, pointAttributeBOS, pointBOS, workbook)) {
            throw new ImportException("The import template is formatted incorrectly");
        }

        for (int i = 4; i <= mainSheet.getLastRowNum(); i++) {
            // 导入设备
            DeviceBO importDeviceBO = importDevice(entityBO, mainSheet, i);
            log.info("正在导入设备：{}, index: {}", importDeviceBO.getDeviceName(), 1);

            // 导入驱动属性配置
            importDriverAttributeConfig(importDeviceBO, driverAttributeBOS, mainSheet, i);

            // 导入位号属性配置
            importPointAttributeConfig(driverAttributeBOS, pointAttributeBOS, pointBOS, mainSheet, i, importDeviceBO);
        }
    }

    @Override
    @SneakyThrows
    public Path generateImportTemplate(DeviceBO entityBO) {
        List<DriverAttributeBO> driverAttributeBOS = driverAttributeService.selectByDriverId(entityBO.getDriverId());
        List<PointAttributeBO> pointAttributeBOS = pointAttributeService.selectByDriverId(entityBO.getDriverId(), false);
        List<PointBO> pointBOS = pointService.selectByProfileIds(entityBO.getProfileIds());

        Workbook workbook = new XSSFWorkbook();
        CellStyle cellStyle = PoiUtil.getCenterCellStyle(workbook);

        // 设置主工作表
        Sheet mainSheet = workbook.createSheet("设备导入");
        mainSheet.setDefaultColumnWidth(25);

        // 设置配置工作表
        configConfigSheet(driverAttributeBOS, pointAttributeBOS, pointBOS, workbook);

        // 设置说明
        Row remarkRow = mainSheet.createRow(0);
        PoiUtil.createCell(remarkRow, 0, "说明：请从第5行开始添加待导入的设备数据");
        PoiUtil.mergedRegion(mainSheet, 0, 0, 0, 2 + driverAttributeBOS.size() + pointAttributeBOS.size() * pointBOS.size() - 1);

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
        configPointCell(driverAttributeBOS, pointAttributeBOS, pointBOS, mainSheet, titleRow, attributeRow, cellStyle);

        // 生成设备导入模板
        return generateTemplate(workbook);
    }

    private LambdaQueryWrapper<DeviceDO> fuzzyQuery(DeviceQuery entityQuery) {
        QueryWrapper<DeviceDO> wrapper = Wrappers.query();
        wrapper.eq("dd.deleted", 0);
        if (ObjectUtil.isNotNull(entityQuery)) {
            wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getDeviceName()), "dd.device_name", entityQuery.getDeviceName());
            wrapper.eq(CharSequenceUtil.isNotEmpty(entityQuery.getDeviceCode()), "dd.device_code", entityQuery.getDeviceCode());
            wrapper.eq(ObjectUtil.isNotEmpty(entityQuery.getDriverId()), "dd.driver_id", entityQuery.getDriverId());
            wrapper.eq(ObjectUtil.isNotNull(entityQuery.getEnableFlag()), "dd.enable_flag", entityQuery.getEnableFlag());
            wrapper.eq("dd.tenant_id", entityQuery.getTenantId());
        }
        return wrapper.lambda();
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

    private DeviceBO getDevice(DeviceBO deviceBO, Sheet mainSheet, int rowIndex) {
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

    private DriverAttributeConfigBO getDriverAttributeConfig(DeviceBO deviceBO, DriverAttributeBO driverAttributeBO, Sheet mainSheet, int rowIndex, int cellIndex) {
        DriverAttributeConfigBO importAttributeConfig = new DriverAttributeConfigBO();
        importAttributeConfig.setDriverAttributeId(driverAttributeBO.getId());
        importAttributeConfig.setDeviceId(deviceBO.getId());
        String attributeValue = PoiUtil.getCellStringValue(mainSheet, rowIndex, cellIndex);
        importAttributeConfig.setConfigValue(attributeValue);
        importAttributeConfig.setTenantId(deviceBO.getTenantId());

        return importAttributeConfig;
    }

    private void importPointAttributeConfig(List<DriverAttributeBO> driverAttributeBOS, List<PointAttributeBO> pointAttributeBOS, List<PointBO> pointBOS, Sheet mainSheet, int i, DeviceBO importDeviceBO) {
        for (int j = 0; j < pointBOS.size(); j++) {
            for (int k = 0; k < pointAttributeBOS.size(); k++) {
                PointAttributeConfigBO importAttributeConfig = getPointAttributeConfig(importDeviceBO, pointBOS.get(j), pointAttributeBOS.get(k), mainSheet, i, 2 + driverAttributeBOS.size() + k * pointAttributeBOS.size() + j);
                pointAttributeConfigService.save(importAttributeConfig);
            }
        }
    }

    private PointAttributeConfigBO getPointAttributeConfig(DeviceBO deviceBO, PointBO pointBO, PointAttributeBO pointAttributeBO, Sheet mainSheet, int rowIndex, int cellIndex) {
        PointAttributeConfigBO importAttributeConfig = new PointAttributeConfigBO();
        importAttributeConfig.setPointAttributeId(pointAttributeBO.getId());
        importAttributeConfig.setDeviceId(deviceBO.getId());
        importAttributeConfig.setPointId(pointBO.getId());
        String attributeValue = PoiUtil.getCellStringValue(mainSheet, rowIndex, cellIndex);
        importAttributeConfig.setConfigValue(attributeValue);
        importAttributeConfig.setTenantId(deviceBO.getTenantId());

        return importAttributeConfig;
    }

    /**
     * 判断配置数据是否一致
     *
     * @param driverAttributeBOS DriverAttribute Array
     * @param pointAttributeBOS  PointAttribute Array
     * @param pointBOS           Point Array
     * @param workbook           Workbook
     */
    private boolean configIsEqual(List<DriverAttributeBO> driverAttributeBOS, List<PointAttributeBO> pointAttributeBOS, List<PointBO> pointBOS, Workbook workbook) {
        Sheet configSheet = workbook.getSheet("配置（忽略）");
        String driverAttributesValueNew = JsonUtil.toJsonString(driverAttributeBOS);
        String driverAttributesValueOld = PoiUtil.getCellStringValue(configSheet, 0, 0);
        if (!driverAttributesValueNew.equals(driverAttributesValueOld)) {
            return false;
        }

        String pointAttributesValueNewd = JsonUtil.toJsonString(pointAttributeBOS);
        String pointAttributesValueOld = PoiUtil.getCellStringValue(configSheet, 1, 0);
        if (!pointAttributesValueNewd.equals(pointAttributesValueOld)) {
            return false;
        }

        String pointsValueNew = JsonUtil.toJsonString(pointBOS);
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
     * @param pointAttributeBOS  PointAttribute Array
     * @param pointBOS           Point Array
     * @param workbook           Workbook
     */
    private void configConfigSheet(List<DriverAttributeBO> driverAttributeBOS, List<PointAttributeBO> pointAttributeBOS, List<PointBO> pointBOS, Workbook workbook) {
        Sheet configSheet = workbook.createSheet("配置（忽略）");
        Row driverAttributesRow = configSheet.createRow(0);
        Row pointAttributesRow = configSheet.createRow(1);
        Row pointsRow = configSheet.createRow(2);
        PoiUtil.createCell(driverAttributesRow, 0, JsonUtil.toJsonString(driverAttributeBOS));
        PoiUtil.createCell(pointAttributesRow, 0, JsonUtil.toJsonString(pointAttributeBOS));
        PoiUtil.createCell(pointsRow, 0, JsonUtil.toJsonString(pointBOS));
    }

    /**
     * 设置位号属性配置列
     *
     * @param driverAttributeBOS DriverAttribute Array
     * @param pointAttributeBOS  PointAttribute Array
     * @param pointBOS           Point  Array
     * @param mainSheet          Main Sheet
     * @param titleRow           Title Row
     * @param attributeRow       Attribute Row
     * @param cellStyle          CellStyle
     */
    private void configPointCell(List<DriverAttributeBO> driverAttributeBOS, List<PointAttributeBO> pointAttributeBOS, List<PointBO> pointBOS, Sheet mainSheet, Row titleRow, Row attributeRow, CellStyle cellStyle) {
        if (pointAttributeBOS.isEmpty()) {
            return;
        }

        Row pointRow = mainSheet.createRow(2);
        PoiUtil.createCellWithStyle(titleRow, 2 + driverAttributeBOS.size(), "位号属性配置", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 1, 2 + driverAttributeBOS.size(), 2 + driverAttributeBOS.size() + pointAttributeBOS.size() * pointBOS.size() - 1);
        for (int i = 0; i < pointBOS.size(); i++) {
            PoiUtil.createCellWithStyle(pointRow, 2 + driverAttributeBOS.size() + i * pointAttributeBOS.size(), pointBOS.get(i).getPointName(), cellStyle);
            PoiUtil.mergedRegion(mainSheet, 2, 2, 2 + driverAttributeBOS.size() + i * pointAttributeBOS.size(), 2 + driverAttributeBOS.size() + i * pointAttributeBOS.size() + pointAttributeBOS.size() - 1);
            for (int j = 0; j < pointAttributeBOS.size(); j++) {
                PoiUtil.createCellWithStyle(attributeRow, 2 + driverAttributeBOS.size() + i * pointAttributeBOS.size() + j, pointAttributeBOS.get(j).getDisplayName(), cellStyle);
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

    private void addProfileBind(DeviceDO entityDO, Set<Long> profileIds) {
        if (CollUtil.isEmpty(profileIds)) {
            return;
        }

        profileIds.forEach(profileId -> {
            try {
                ProfileBindBO entityBO = new ProfileBindBO();
                entityBO.setProfileId(profileId);
                entityBO.setDeviceId(entityDO.getId());
                entityBO.setTenantId(entityDO.getTenantId());
                profileBindService.save(entityBO);

                List<PointBO> pointBOS = pointService.selectByProfileId(profileId);
                // 通知驱动新增位号
                pointBOS.forEach(point -> driverNotifyService.notifyPoint(MetadataCommandTypeEnum.ADD, point));
            } catch (Exception ignored) {
                // nothing to do
            }
        });

    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link DeviceBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(DeviceBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDeviceName, entityBO.getDeviceName());
        wrapper.eq(DeviceDO::getDeviceCode, entityBO.getDeviceCode());
        wrapper.eq(DeviceDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DeviceDO one = deviceManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("设备重复");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link DeviceDO}
     */
    private DeviceDO getDOById(Long id, boolean throwException) {
        DeviceDO entityDO = deviceManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("设备不存在");
        }
        return entityDO;
    }

}
