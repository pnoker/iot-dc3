package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.query.ResourcePageQuery;
import io.github.pnoker.center.auth.mapper.ResourceMapper;
import io.github.pnoker.center.auth.service.ResourceService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author linys
 * @since 2023.04.02
 */
@Slf4j
@Service
public class ResourceServiceImpl implements ResourceService {

    @javax.annotation.Resource
    private ResourceMapper resourceMapper;


    @Override
    public Resource add(Resource resource) {
        if (resourceMapper.insert(resource) > 0){
            return resourceMapper.selectById(resource.getId());
        }
        throw new ServiceException("The resource add failed");
    }

    @Override
    public Boolean delete(String id) {
        selectById(id);
        return resourceMapper.deleteById(id) > 0;
    }

    @Override
    public Resource update(Resource resource) {
        selectById(resource.getId());
        resource.setOperateTime(null);
        if (resourceMapper.updateById(resource) > 0) {
            return resourceMapper.selectById(resource.getId());
        }
        throw new ServiceException("The resource update failed");
    }

    @Override
    public Resource selectById(String id) {
        Resource resource = resourceMapper.selectById(id);
        if (ObjectUtil.isNull(resource)) {
            throw new NotFoundException();
        }
        return resource;
    }

    @Override
    public Page<Resource> list(ResourcePageQuery pageQuery) {
        if (ObjectUtil.isNull(pageQuery.getPage())) {
            pageQuery.setPage(new Pages());
        }
        return resourceMapper.selectPage(pageQuery.getPage().convert(), fuzzyQuery(pageQuery));
    }

    @Override
    public LambdaQueryWrapper<Resource> fuzzyQuery(ResourcePageQuery pageQuery) {
        LambdaQueryWrapper<Resource> queryWrapper = Wrappers.<Resource>query().lambda();
        if (ObjectUtil.isNotNull(pageQuery)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getTenantId()), Resource::getTenantId, pageQuery.getTenantId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getParentResourceId()), Resource::getParentResourceId, pageQuery.getParentResourceId());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(pageQuery.getResourceName()), Resource::getResourceName, pageQuery.getResourceName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getResourceCode()), Resource::getResourceCode, pageQuery.getResourceCode());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pageQuery.getResourceTypeFlag()), Resource::getResourceTypeFlag, pageQuery.getResourceTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pageQuery.getEnableFlag()), Resource::getEnableFlag, pageQuery.getEnableFlag());

        }
        return queryWrapper;
    }
}
