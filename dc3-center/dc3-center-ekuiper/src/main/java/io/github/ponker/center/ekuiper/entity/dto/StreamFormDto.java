package io.github.ponker.center.ekuiper.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author : Zhen
 */
@Data
public class StreamFormDto {

    @NotBlank(message = "SQL不能为空")
    private String sql;

}
