package com.pnoker.center.manager.api;

import com.pnoker.api.center.manager.feign.DictionaryClient;
import com.pnoker.center.manager.service.DictionaryService;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.model.Dictionary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_DICTIONARY_URL_PREFIX)
public class DictionaryApi implements DictionaryClient {
    @Resource
    private DictionaryService dictionaryService;

    @Override
    public R<List<Dictionary>> driverDictionary() {
        try {
            List<Dictionary> dictionaryList = dictionaryService.driverDictionary();
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> driverAttributeDictionary() {
        try {
            List<Dictionary> dictionaryList = dictionaryService.driverAttributeDictionary();
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> pointAttributeDictionary() {
        try {
            List<Dictionary> dictionaryList = dictionaryService.pointAttributeDictionary();
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> profileDictionary() {
        try {
            List<Dictionary> dictionaryList = dictionaryService.profileDictionary();
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> groupDictionary() {
        try {
            List<Dictionary> dictionaryList = dictionaryService.groupDictionary();
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> deviceDictionary(@NotNull String parent) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.deviceDictionary(parent);
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> pointDictionary(@NotNull String parent) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.pointDictionary(parent);
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
