package io.github.ponker.center.ekuiper.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * @author : Zhen
 */
@Data
public class SqlSinkDto {

    @NotBlank
    private String url;

    @NotBlank
    private String table;

    private String keyField;

    @JsonProperty("rowkindField")
    private String rowkindField;

    @NotBlank
    private String format;

    private Boolean sendSingle;

    private Boolean omitIfEmpty;

    private String tableDataField;

//    @JsonProperty("resourceId")
//    private String resourceId;

}
