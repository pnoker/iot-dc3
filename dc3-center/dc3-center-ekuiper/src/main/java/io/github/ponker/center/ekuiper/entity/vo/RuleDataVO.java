package io.github.ponker.center.ekuiper.entity.vo;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * @author : Zhen
 */
@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleDataVO {

    @NotBlank
    private String id;

    private String name;

    @NotBlank
    private String status;
}
