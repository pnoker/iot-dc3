package io.github.pnoker.common.entity.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

/** Read-only option projection used by selection and tree controls. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Read-only dictionary option")
public record DictionaryOption(
        @Schema(description = "Logical option group", example = "device_type") String type,
        @Schema(description = "Human-readable display label", example = "Modbus TCP", requiredMode = Schema.RequiredMode.REQUIRED) String label,
        @Schema(description = "Stable machine-readable value", example = "MODBUS_TCP", requiredMode = Schema.RequiredMode.REQUIRED) String value,
        @Schema(description = "Whether the option is unavailable", example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean disabled,
        @Schema(description = "Whether the tree node is expanded by default", example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean expand,
        @Schema(description = "Child options; empty for a leaf", requiredMode = Schema.RequiredMode.REQUIRED) List<DictionaryOption> children) {

    public DictionaryOption {
        label = Objects.requireNonNull(label, "label must not be null");
        value = Objects.requireNonNull(value, "value must not be null");
        children = children == null ? List.of() : List.copyOf(children);
    }

    public static DictionaryOption leaf(String label, String value) {
        return new DictionaryOption(null, label, value, false, false, List.of());
    }

}
