/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.ImportException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.manager.biz.ImportDeviceService;
import io.github.pnoker.common.manager.dal.DeviceManager;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverAttributeService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointAttributeService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.manager.service.ProfileService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.PageUtil;
import io.github.pnoker.common.utils.PoiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Business service implementation for device operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceBuilder deviceBuilder;

    private final DeviceManager deviceManager;

    private final DeviceMapper deviceMapper;

    private final PointService pointService;

    private final DriverService driverService;

    private final ProfileService profileService;

    private final DriverAttributeService driverAttributeService;

    private final PointAttributeService pointAttributeService;

    private final ImportDeviceService importDeviceService;

    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(DeviceBO entityBO) {
        validateTenantRelations(entityBO);
        entityBO.setDeviceCode(null);

        boolean duplicate = checkDuplicate(entityBO, false);
        if (duplicate) {
            throw new DuplicateException("Failed to create device: device has been duplicated");
        }

        DeviceDO entityDO = deviceBuilder.buildDOByBO(entityBO);
        if (!deviceManager.save(entityDO)) {
            throw new AddException("Failed to create device");
        }

        //
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.ADD, driverServiceNames(entityDO.getDriverId()));
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DeviceDO entityDO = getDOById(id, true);
        Set<String> targetServices = driverServiceNames(entityDO.getDriverId());

        //
        if (!deviceManager.removeById(id)) {
            throw new DeleteException("Failed to remove device");
        }

        //
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.DELETE, targetServices);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DeviceBO entityBO) {
        DeviceDO entityDO = getDOById(entityBO.getId(), true);
        Long oldDriverId = entityDO.getDriverId();
        if (!Objects.equals(entityBO.getTenantId(), entityDO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        entityBO.setDeviceCode(entityDO.getDeviceCode());
        validateTenantRelations(entityBO);

        boolean duplicate = checkDuplicate(entityBO, true);
        if (duplicate) {
            throw new DuplicateException("Failed to update device: device has been duplicated");
        }

        entityDO = deviceBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!deviceManager.updateById(entityDO)) {
            throw new UpdateException("The device update failed");
        }

        DeviceBO deviceBO = getById(entityBO.getId());
        entityBO.setDeviceName(deviceBO.getDeviceName());

        //
        if (Objects.equals(oldDriverId, entityBO.getDriverId())) {
            MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.DEVICE,
                    MetadataOperateTypeEnum.UPDATE, driverServiceNames(entityBO.getDriverId()));
            metadataEventPublisher.publishEvent(metadataEvent);
        } else {
            metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.DEVICE,
                    MetadataOperateTypeEnum.DELETE, driverServiceNames(oldDriverId)));
            metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.DEVICE,
                    MetadataOperateTypeEnum.ADD, driverServiceNames(entityBO.getDriverId())));
        }
    }

    @Override
    public DeviceBO getById(Long id) {
        DeviceDO entityDO = getDOById(id, true);
        return deviceBuilder.buildBOByDO(entityDO);
    }

    @Override
    public DeviceBO getByName(String name, Long tenantId) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDeviceName, name);
        wrapper.eq(DeviceDO::getTenantId, tenantId);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DeviceDO entityDO = deviceManager.getOne(wrapper);
        return deviceBuilder.buildBOByDO(entityDO);
    }

    @Override
    public DeviceBO getByCode(String code, Long tenantId) {
        LambdaQueryChainWrapper<DeviceDO> wrapper = deviceManager.lambdaQuery()
                .eq(DeviceDO::getDeviceCode, code)
                .eq(DeviceDO::getTenantId, tenantId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        DeviceDO entityDO = wrapper.one();
        return deviceBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<DeviceBO> listByDriverId(Long driverId, Long tenantId) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDriverId, driverId);
        if (Objects.nonNull(tenantId)) {
            wrapper.eq(DeviceDO::getTenantId, tenantId);
        }
        List<DeviceDO> entityDOList = deviceManager.list(wrapper);
        return deviceBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<Long> listIdsByDriverId(Long driverId, Long tenantId) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDriverId, driverId).select(DeviceDO::getId);
        if (Objects.nonNull(tenantId)) {
            wrapper.eq(DeviceDO::getTenantId, tenantId);
        }
        return deviceManager.list(wrapper).stream().map(DeviceDO::getId).toList();
    }

    @Override
    public List<DeviceBO> listByProfileId(Long profileId, Long tenantId) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getProfileId, profileId);
        if (Objects.nonNull(tenantId)) {
            wrapper.eq(DeviceDO::getTenantId, tenantId);
        }
        List<DeviceDO> entityDOList = deviceManager.list(wrapper);
        return deviceBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<DeviceBO> listByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<DeviceDO> entityDOList = deviceManager.listByIds(ids);
        return deviceBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public Page<DeviceBO> list(DeviceQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<DeviceDO> entityPageDO = deviceMapper.selectPageWithProfile(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return deviceBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public void importDevice(DeviceBO entityBO, File file) {
        validateTenantRelations(entityBO);

        List<PointBO> pointBOList = pointService.listByProfileId(entityBO.getProfileId(), entityBO.getTenantId()).stream()
                .filter(pointBO -> Objects.equals(entityBO.getTenantId(), pointBO.getTenantId()))
                .toList();
        List<DriverAttributeBO> driverAttributeBOList = driverAttributeService.listByDriverId(entityBO.getDriverId())
                .stream()
                .filter(attributeBO -> Objects.equals(entityBO.getTenantId(), attributeBO.getTenantId()))
                .toList();
        List<PointAttributeBO> pointAttributeBOList = pointAttributeService.listByDriverId(entityBO.getDriverId())
                .stream()
                .filter(attributeBO -> Objects.equals(entityBO.getTenantId(), attributeBO.getTenantId()))
                .toList();

        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(file);
        } catch (Exception e) {
            throw new ImportException("The import template file incorrectly: {}", e.getMessage(), e);
        }

        if (!configIsEqual(workbook, driverAttributeBOList, pointAttributeBOList, pointBOList)) {
            throw new ImportException("The import template is formatted incorrectly");
        }

        Sheet sheet = workbook.getSheet("");
        for (int i = 4; i <= sheet.getLastRowNum(); i++) {
            DeviceBO importDeviceBO;
            try {
                importDeviceBO = importDeviceService.importDevice(entityBO, pointBOList, driverAttributeBOList,
                        pointAttributeBOList, sheet, i);
                log.info("Import device succeeded, row index: {}", i + 1);
            } catch (Exception e) {
                log.info("Skip import device, row index: {}, {}", i + 1, e.getMessage(), e);
                continue;
            }

            //
            MetadataEvent metadataEvent = new MetadataEvent(this, importDeviceBO.getId(), MetadataTypeEnum.DEVICE,
                    MetadataOperateTypeEnum.ADD);
            metadataEventPublisher.publishEvent(metadataEvent);
        }

        //
        try {
            FileUtils.delete(file);
        } catch (IOException e) {
            log.error("Failed to delete imported device file: {}", file, e);
        }
    }

    @Override
    public Path generateImportTemplate(DeviceBO entityBO) {
        validateTenantRelations(entityBO);

        List<DriverAttributeBO> driverAttributeBOList = driverAttributeService.listByDriverId(entityBO.getDriverId())
                .stream()
                .filter(attributeBO -> Objects.equals(entityBO.getTenantId(), attributeBO.getTenantId()))
                .toList();
        List<PointAttributeBO> pointAttributeBOList = pointAttributeService.listByDriverId(entityBO.getDriverId())
                .stream()
                .filter(attributeBO -> Objects.equals(entityBO.getTenantId(), attributeBO.getTenantId()))
                .toList();
        List<PointBO> pointBOList = pointService.listByProfileId(entityBO.getProfileId(), entityBO.getTenantId()).stream()
                .filter(pointBO -> Objects.equals(entityBO.getTenantId(), pointBO.getTenantId()))
                .toList();

        Workbook workbook = new XSSFWorkbook();
        CellStyle cellStyle = PoiUtil.getCenterCellStyle(workbook);

        //
        Sheet mainSheet = workbook.createSheet("");
        mainSheet.setDefaultColumnWidth(25);

        //
        configConfigSheet(driverAttributeBOList, pointAttributeBOList, pointBOList, workbook);

        //
        Row remarkRow = mainSheet.createRow(0);
        PoiUtil.createCell(remarkRow, 0, ": 5");
        PoiUtil.mergedRegion(mainSheet, 0, 0, 0,
                2 + driverAttributeBOList.size() + pointAttributeBOList.size() * pointBOList.size() - 1);

        //
        Row titleRow = mainSheet.createRow(1);
        PoiUtil.createCellWithStyle(titleRow, 0, "Device Name", cellStyle);
        PoiUtil.createCellWithStyle(titleRow, 1, "Description", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 3, 0, 0);
        PoiUtil.mergedRegion(mainSheet, 1, 3, 1, 1);

        Row attributeRow = mainSheet.createRow(3);
        //
        configAttributeCell(driverAttributeBOList, mainSheet, titleRow, attributeRow, cellStyle);
        //
        configPointCell(driverAttributeBOList, pointAttributeBOList, pointBOList, mainSheet, titleRow, attributeRow,
                cellStyle);

        //
        return generateTemplate(workbook);
    }

    /**
     * @param workbook              Workbook
     * @param driverAttributeBOList Array
     * @param pointAttributeBOList  Array
     * @param pointBOList           Point
     */
    private boolean configIsEqual(Workbook workbook, List<DriverAttributeBO> driverAttributeBOList,
                                  List<PointAttributeBO> pointAttributeBOList, List<PointBO> pointBOList) {
        Sheet configSheet = workbook.getSheet("()");
        String driverAttributesValueNew = JsonUtil.toJsonString(driverAttributeBOList);
        String driverAttributesValueOld = PoiUtil.getCellStringValue(configSheet, 0, 0);
        if (!driverAttributesValueNew.equals(driverAttributesValueOld)) {
            return false;
        }

        String newPointAttributesValue = JsonUtil.toJsonString(pointAttributeBOList);
        String oldPointAttributesValue = PoiUtil.getCellStringValue(configSheet, 1, 0);
        if (!newPointAttributesValue.equals(oldPointAttributesValue)) {
            return false;
        }

        String pointsValueNew = JsonUtil.toJsonString(pointBOList);
        String pointsValueOld = PoiUtil.getCellStringValue(configSheet, 2, 0);

        return pointsValueNew.equals(pointsValueOld);
    }

    /**
     * @param driverAttributeBOList Array
     * @param mainSheet             Main Sheet
     * @param titleRow              Title Row
     * @param attributeRow          Attribute Row
     */
    private void configAttributeCell(List<DriverAttributeBO> driverAttributeBOList, Sheet mainSheet, Row titleRow,
                                     Row attributeRow, CellStyle cellStyle) {
        if (driverAttributeBOList.isEmpty()) {
            return;
        }

        PoiUtil.createCellWithStyle(titleRow, 2, "", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 2, 2, 2 + driverAttributeBOList.size() - 1);
        for (int i = 0; i < driverAttributeBOList.size(); i++) {
            PoiUtil.createCellWithStyle(attributeRow, 2 + i, driverAttributeBOList.get(i).getAttributeName(),
                    cellStyle);
        }

    }

    /**
     * @param driverAttributeBOList Array
     * @param pointAttributeBOList  Array
     * @param pointBOList           Point Array
     * @param workbook              Workbook
     */
    private void configConfigSheet(List<DriverAttributeBO> driverAttributeBOList,
                                   List<PointAttributeBO> pointAttributeBOList, List<PointBO> pointBOList, Workbook workbook) {
        Sheet configSheet = workbook.createSheet("()");
        Row driverAttributesRow = configSheet.createRow(0);
        Row pointAttributesRow = configSheet.createRow(1);
        Row pointsRow = configSheet.createRow(2);
        PoiUtil.createCell(driverAttributesRow, 0, JsonUtil.toJsonString(driverAttributeBOList));
        PoiUtil.createCell(pointAttributesRow, 0, JsonUtil.toJsonString(pointAttributeBOList));
        PoiUtil.createCell(pointsRow, 0, JsonUtil.toJsonString(pointBOList));
    }

    /**
     * @param driverAttributeBOList Array
     * @param pointAttributeBOList  Array
     * @param pointBOList           Point Array
     * @param mainSheet             Main Sheet
     * @param titleRow              Title Row
     * @param attributeRow          Attribute Row
     * @param cellStyle             CellStyle
     */
    private void configPointCell(List<DriverAttributeBO> driverAttributeBOList,
                                 List<PointAttributeBO> pointAttributeBOList, List<PointBO> pointBOList, Sheet mainSheet, Row titleRow,
                                 Row attributeRow, CellStyle cellStyle) {
        if (pointAttributeBOList.isEmpty()) {
            return;
        }

        Row pointRow = mainSheet.createRow(2);
        PoiUtil.createCellWithStyle(titleRow, 2 + driverAttributeBOList.size(), "", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 1, 2 + driverAttributeBOList.size(),
                2 + driverAttributeBOList.size() + pointAttributeBOList.size() * pointBOList.size() - 1);
        for (int i = 0; i < pointBOList.size(); i++) {
            PoiUtil.createCellWithStyle(pointRow, 2 + driverAttributeBOList.size() + i * pointAttributeBOList.size(),
                    pointBOList.get(i).getPointName(), cellStyle);
            PoiUtil.mergedRegion(mainSheet, 2, 2, 2 + driverAttributeBOList.size() + i * pointAttributeBOList.size(), 2
                    + driverAttributeBOList.size() + i * pointAttributeBOList.size() + pointAttributeBOList.size() - 1);
            for (int j = 0; j < pointAttributeBOList.size(); j++) {
                PoiUtil.createCellWithStyle(attributeRow,
                        2 + driverAttributeBOList.size() + i * pointAttributeBOList.size() + j,
                        pointAttributeBOList.get(j).getAttributeName(), cellStyle);
            }
        }
    }

    /**
     * @param workbook Workbook
     * @return Path
     */
    private Path generateTemplate(Workbook workbook) {
        Path path;
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            org.springframework.core.io.Resource resource = resolver.getResource("classpath:/");
            Path resourcePath = Paths.get(resource.getURI());
            String fileName = MessageFormat.format("dc3_device_import_template_{0}.xlsx", System.currentTimeMillis());
            path = resourcePath.resolve(fileName);
            try (FileOutputStream outputStream = new FileOutputStream(path.toUri().getPath())) {
                workbook.write(outputStream);
            }
        } catch (IOException e) {
            throw new ServiceException("Generate template error: {}", e.getMessage(), e);
        }
        return path;
    }

    private void validateTenantRelations(DeviceBO entityBO) {
        Long tenantId = entityBO.getTenantId();
        DriverBO driverBO = driverService.getById(entityBO.getDriverId());
        if (Objects.isNull(driverBO) || !Objects.equals(tenantId, driverBO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }

        if (Objects.isNull(entityBO.getProfileId())) {
            return;
        }

        ProfileBO profileBO = profileService.getById(entityBO.getProfileId());
        if (Objects.isNull(profileBO) || !Objects.equals(tenantId, profileBO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    /**
     * @param id Device ID
     * @return
     */
    private boolean checkExist(Long id) {
        DeviceDO entityDO = deviceManager.getById(id);
        return Objects.nonNull(entityDO);
    }

    /**
     * @param entityQuery {@link DeviceQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<DeviceDO> fuzzyQuery(DeviceQuery entityQuery) {
        QueryWrapper<DeviceDO> wrapper = Wrappers.query();
        wrapper.eq("dd.deleted", 0);
        if (Objects.nonNull(entityQuery)) {
            wrapper.like(StringUtils.isNotEmpty(entityQuery.getDeviceName()), "dd.device_name",
                    entityQuery.getDeviceName());
            wrapper.eq(StringUtils.isNotEmpty(entityQuery.getDeviceCode()), "dd.device_code",
                    entityQuery.getDeviceCode());
            wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDriverId()), "dd.driver_id", entityQuery.getDriverId());
            wrapper.eq(FieldUtil.isValidIdField(entityQuery.getProfileId()), "dd.profile_id", entityQuery.getProfileId());
            wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), "dd.enable_flag",
                    Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
            wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), "dd.tenant_id", entityQuery.getTenantId());
            wrapper.eq(Objects.nonNull(entityQuery.getVersion()), "dd.version", entityQuery.getVersion());
            wrapper.exists(FieldUtil.isValidIdField(entityQuery.getGroupId()),
                    "select 1 from dc3_group_bind dgb where dgb.deleted = 0 "
                            + "and dgb.tenant_id = dd.tenant_id "
                            + "and dgb.entity_type_flag = {0} "
                            + "and dgb.entity_id = dd.id "
                            + "and dgb.group_id = {1}",
                    EntityTypeEnum.DEVICE.getIndex(), entityQuery.getGroupId());
            wrapper.exists(FieldUtil.isValidIdField(entityQuery.getLabelId()),
                    "select 1 from dc3_label_bind dlb where dlb.deleted = 0 "
                            + "and dlb.tenant_id = dd.tenant_id "
                            + "and dlb.entity_type_flag = {0} "
                            + "and dlb.entity_id = dd.id "
                            + "and dlb.label_id = {1}",
                    EntityTypeEnum.DEVICE.getIndex(), entityQuery.getLabelId());
        }
        return wrapper.lambda();
    }

    /**
     * @param entityBO {@link DeviceBO}
     * @param isUpdate
     * @return
     */
    private boolean checkDuplicate(DeviceBO entityBO, boolean isUpdate) {
        if (StringUtils.isEmpty(entityBO.getDeviceName())) {
            return false;
        }
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDeviceName, entityBO.getDeviceName());
        wrapper.eq(DeviceDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DeviceDO one = deviceManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    private Set<String> driverServiceNames(Long... driverIds) {
        Set<String> services = new HashSet<>();
        if (Objects.isNull(driverIds)) {
            return services;
        }

        for (Long driverId : driverIds) {
            if (Objects.isNull(driverId)) {
                continue;
            }
            DriverBO driverBO = driverService.getById(driverId);
            if (Objects.nonNull(driverBO) && StringUtils.isNotBlank(driverBO.getServiceName())) {
                services.add(driverBO.getServiceName());
            }
        }
        return services;
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link DeviceDO}
     */
    private DeviceDO getDOById(Long id, boolean throwException) {
        DeviceDO entityDO = deviceManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Device does not exist");
        }
        return entityDO;
    }

}
