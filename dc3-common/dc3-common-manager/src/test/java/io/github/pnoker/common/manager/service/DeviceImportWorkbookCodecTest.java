package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.exception.ImportException;
import io.github.pnoker.common.manager.entity.operation.DeviceImportManifest;
import io.github.pnoker.common.manager.entity.operation.DeviceImportRow;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceImportWorkbookCodecTest {

    private final DeviceImportWorkbookCodec codec = new DeviceImportWorkbookCodec();

    @Test
    void roundTripPreservesPointThenAttributeColumnOrder() throws Exception {
        DeviceImportManifest manifest = manifest(DeviceImportManifest.CURRENT_SCHEMA_VERSION);
        byte[] template = codec.create(manifest);
        byte[] populated;
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(template));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheet("Devices");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("Boiler-A");
            row.createCell(1).setCellValue("Primary boiler");
            row.createCell(2).setCellValue("192.168.1.10");
            row.createCell(3).setCellValue("point-a-address");
            row.createCell(4).setCellValue("point-a-scale");
            row.createCell(5).setCellValue("point-b-address");
            row.createCell(6).setCellValue("point-b-scale");
            workbook.write(output);
            populated = output.toByteArray();
        }

        List<DeviceImportRow> rows = codec.parse(populated, manifest);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.deviceName()).isEqualTo("Boiler-A");
            assertThat(row.driverAttributeValues()).containsExactly("192.168.1.10");
            assertThat(row.pointAttributeValues()).containsExactly(
                    "point-a-address", "point-a-scale", "point-b-address", "point-b-scale");
        });
    }

    @Test
    void rejectsTemplateWithDifferentSchemaVersion() {
        byte[] content = codec.create(manifest(DeviceImportManifest.CURRENT_SCHEMA_VERSION + 1));

        assertThatThrownBy(() -> codec.parse(content, manifest(DeviceImportManifest.CURRENT_SCHEMA_VERSION)))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejectsTemplateForDifferentDriverOrProfile() {
        byte[] content = codec.create(manifest(DeviceImportManifest.CURRENT_SCHEMA_VERSION));
        DeviceImportManifest different = new DeviceImportManifest(DeviceImportManifest.CURRENT_SCHEMA_VERSION,
                99L, 20L, List.of(), List.of());

        assertThatThrownBy(() -> codec.parse(content, different))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejectsEmptyWorkbookAndTemplateWithoutRows() {
        assertThatThrownBy(() -> codec.parse(new byte[0], manifest(DeviceImportManifest.CURRENT_SCHEMA_VERSION)))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("invalid");
        byte[] emptyTemplate = codec.create(manifest(DeviceImportManifest.CURRENT_SCHEMA_VERSION));
        assertThatThrownBy(() -> codec.parse(emptyTemplate, manifest(DeviceImportManifest.CURRENT_SCHEMA_VERSION)))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("no data rows");
    }

    private DeviceImportManifest manifest(int schemaVersion) {
        List<DeviceImportManifest.AttributeColumn> pointAttributes = List.of(
                new DeviceImportManifest.AttributeColumn(31L, "Address"),
                new DeviceImportManifest.AttributeColumn(32L, "Scale"));
        return new DeviceImportManifest(schemaVersion, 10L, 20L,
                List.of(new DeviceImportManifest.AttributeColumn(30L, "Host")),
                List.of(
                        new DeviceImportManifest.PointColumn(40L, "Point A", pointAttributes),
                        new DeviceImportManifest.PointColumn(41L, "Point B", pointAttributes)));
    }
}
