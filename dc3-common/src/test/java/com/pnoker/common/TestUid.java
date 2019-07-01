package com.pnoker.common;

import com.pnoker.common.utils.uid.UidTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
public class TestUid {
    @Test
    public void guid() {
        log.info("secure is true {}", new UidTools(true).guid());
        log.info("secure is false {}", new UidTools().guid());
    }
}
