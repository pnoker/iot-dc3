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
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
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
    public void add(Resource entityDo) {
        //todo check if exists
        if (resourceMapper.insert(entityDo) < 1) {
            throw new AddException("The resource add failed");
        }
    }

    @Override
    public void delete(String id) {
        selectById(id);
        if (resourceMapper.deleteById(id) < 1) {
            throw new DeleteException("The resource delete failed");
        }
    }

    @Override
    public void update(Resource entityDo) {
        selectById(entityDo.getId());
        if (resourceMapper.updateById(entityDo) < 1) {
            throw new UpdateException("The resource update failed");
        }
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
        return resourceMapper.selectPage(pageQuery.getPage().convert(), buildQueryWrapper(pageQuery));
    }

    private LambdaQueryWrapper<Resource> buildQueryWrapper(ResourcePageQuery pageQuery) {
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
