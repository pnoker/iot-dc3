package com.dc3.center.auth.bean;

import com.dc3.common.constant.Common;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;
import java.util.Date;

@Data
@Document(indexName = "book")
public class Book implements Serializable {
    @Id
    private String id;

    private String title;

    @JsonFormat(pattern = Common.COMPLETE_DATE_FORMAT, timezone = Common.TIMEZONE)
    private Date createTime;
}