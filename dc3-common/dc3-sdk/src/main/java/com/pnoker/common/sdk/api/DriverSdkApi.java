package com.pnoker.common.sdk.api;

import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.model.Group;
import com.pnoker.common.valid.Insert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_DRIVER_URL_PREFIX)
public class DriverSdkApi {
    @PostMapping("/add")
    public R<Group> add(@Validated(Insert.class) @RequestBody Group group) {
        return null;
    }
}
