package com.github.pnoker.center.manager.service;

import com.github.pnoker.common.model.Dictionary;

import java.util.List;

/**
 * Dictionary Interface
 *
 * @author pnoker
 */
public interface DictionaryService {

    /**
     * 获取驱动字典
     *
     * @return
     */
    List<Dictionary> driverDictionary();

    /**
     * 获取驱动配置属性字典
     *
     * @return
     */
    List<Dictionary> driverAttributeDictionary();

    /**
     * 获取位号配置属性字典
     *
     * @return
     */
    List<Dictionary> pointAttributeDictionary();

    /**
     * 获取模板字典
     *
     * @return
     */
    List<Dictionary> profileDictionary();

    /**
     * 获取分组字典
     *
     * @return
     */
    List<Dictionary> groupDictionary();

    /**
     * 获取设备字典
     * group/driver/profile
     *
     * @param parent
     * @return
     */
    List<Dictionary> deviceDictionary(String parent);

    /**
     * 获取位号字典
     * driver/profile/device
     *
     * @param parent
     * @return
     */
    List<Dictionary> pointDictionary(String parent);

}
