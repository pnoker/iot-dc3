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
package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.exception.ImportException;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.operation.DeviceImportManifest;
import io.github.pnoker.common.manager.entity.operation.DeviceImportRow;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.PoiUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** POI codec for the device import workbook template. */
@Component
public class DeviceImportWorkbookCodec {

    private static final String DATA_SHEET = "Devices";
    private static final String MANIFEST_SHEET = "_dc3_manifest";
    private static final int MAX_IMPORT_ROWS = 10_000;

    /** Create the import workbook embedding the hidden schema manifest. */
    public byte[] create(DeviceImportManifest manifest) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(DATA_SHEET);
            sheet.setDefaultColumnWidth(25);
            CellStyle style = PoiUtil.getCenterCellStyle(workbook);
            Row title = sheet.createRow(0);
            PoiUtil.createCellWithStyle(title, 0, "Device Name", style);
            PoiUtil.createCellWithStyle(title, 1, "Description", style);
            int column = 2;
            for (DeviceImportManifest.AttributeColumn attribute : manifest.driverAttributes()) {
                PoiUtil.createCellWithStyle(title, column++, "Driver / " + attribute.name(), style);
            }
            for (DeviceImportManifest.PointColumn point : manifest.points()) {
                for (DeviceImportManifest.AttributeColumn attribute : point.attributes()) {
                    PoiUtil.createCellWithStyle(title, column++, point.name() + " / " + attribute.name(), style);
                }
            }
            Sheet manifestSheet = workbook.createSheet(MANIFEST_SHEET);
            PoiUtil.createCell(manifestSheet.createRow(0), 0, JsonUtil.toJsonString(manifest));
            workbook.setSheetHidden(workbook.getSheetIndex(manifestSheet), true);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception error) {
            throw new ImportException("Failed to generate device import template: {}", error.getMessage(), error);
        }
    }

    /** Parse and validate the workbook against the expected manifest, emitting device rows. */
    public List<DeviceImportRow> parse(byte[] content, DeviceImportManifest expected) {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet manifestSheet = workbook.getSheet(MANIFEST_SHEET);
            if (manifestSheet == null) throw new ImportException("The device import manifest is missing");
            DeviceImportManifest actual =
                    JsonUtil.parseObject(PoiUtil.getCellStringValue(manifestSheet, 0, 0), DeviceImportManifest.class);
            if (actual == null
                    || actual.schemaVersion() != DeviceImportManifest.CURRENT_SCHEMA_VERSION
                    || !expected.equals(actual)) {
                throw new ImportException(
                        "The device import template does not match the current driver/profile schema");
            }
            Sheet sheet = workbook.getSheet(DATA_SHEET);
            if (sheet == null) throw new ImportException("The device import data sheet is missing");
            if (sheet.getLastRowNum() > MAX_IMPORT_ROWS) {
                throw new ImportException("The device import file exceeds the {} row limit", MAX_IMPORT_ROWS);
            }
            List<DeviceImportRow> rows = new ArrayList<>();
            int driverColumns = expected.driverAttributes().size();
            int pointColumns = expected.points().stream()
                    .mapToInt(point -> point.attributes().size())
                    .sum();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                String deviceName =
                        PoiUtil.getCellStringValue(sheet, rowIndex, 0).trim();
                if (deviceName.isEmpty()) {
                    if (rowEmpty(sheet, rowIndex, 1 + driverColumns + pointColumns)) continue;
                    throw new ImportException("The device name in line {} is empty", rowIndex + 1);
                }
                List<String> driverValues = new ArrayList<>(driverColumns);
                for (int index = 0; index < driverColumns; index++) {
                    driverValues.add(PoiUtil.getCellStringValue(sheet, rowIndex, 2 + index));
                }
                List<String> pointValues = new ArrayList<>(pointColumns);
                for (int index = 0; index < pointColumns; index++) {
                    pointValues.add(PoiUtil.getCellStringValue(sheet, rowIndex, 2 + driverColumns + index));
                }
                rows.add(new DeviceImportRow(
                        rowIndex + 1,
                        deviceName,
                        PoiUtil.getCellStringValue(sheet, rowIndex, 1),
                        List.copyOf(driverValues),
                        List.copyOf(pointValues)));
            }
            if (rows.isEmpty()) throw new ImportException("The device import file contains no data rows");
            return List.copyOf(rows);
        } catch (ImportException error) {
            throw error;
        } catch (Exception error) {
            throw new ImportException("The device import file is invalid: {}", error.getMessage(), error);
        }
    }

    private boolean rowEmpty(Sheet sheet, int rowIndex, int lastColumn) {
        for (int column = 1; column <= lastColumn; column++) {
            if (!PoiUtil.getCellStringValue(sheet, rowIndex, column).isBlank()) return false;
        }
        return true;
    }

    /** Build the import manifest for the driver/profile attribute schema. */
    public DeviceImportManifest manifest(
            Long driverId,
            Long profileId,
            List<DriverAttributeBO> driverAttributes,
            List<PointAttributeBO> pointAttributes,
            List<PointBO> points) {
        List<DeviceImportManifest.AttributeColumn> driverColumns = driverAttributes.stream()
                .map(attribute ->
                        new DeviceImportManifest.AttributeColumn(attribute.getId(), attribute.getAttributeName()))
                .toList();
        List<DeviceImportManifest.AttributeColumn> pointColumns = pointAttributes.stream()
                .map(attribute ->
                        new DeviceImportManifest.AttributeColumn(attribute.getId(), attribute.getAttributeName()))
                .toList();
        List<DeviceImportManifest.PointColumn> pointManifest = points.stream()
                .map(point -> new DeviceImportManifest.PointColumn(point.getId(), point.getPointName(), pointColumns))
                .toList();
        return new DeviceImportManifest(
                DeviceImportManifest.CURRENT_SCHEMA_VERSION, driverId, profileId, driverColumns, pointManifest);
    }
}
