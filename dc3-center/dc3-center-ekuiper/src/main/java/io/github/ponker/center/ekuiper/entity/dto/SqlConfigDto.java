package io.github.ponker.center.ekuiper.entity.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * @author : Zhen
 * @date : 2024/2/23
 *         对象聚合
 */
@Data
public class SqlConfigDto {

    @NotBlank
    private String url;

    private Integer interval;

    private action internalSqlQueryCfg;
}

@Data
class action {

    @NotBlank
    private String indexField;

    private Integer indexValue;

    private Integer limit;

    @NotBlank
    private String table;
}
