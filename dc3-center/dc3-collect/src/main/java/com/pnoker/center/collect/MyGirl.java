package com.pnoker.center.collect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * @Author: lyang
 * @Date: 2019/1/1 17:55
 */
@Data
@JsonIgnoreProperties
public class MyGirl {
    private String name;

    private String age;
}
