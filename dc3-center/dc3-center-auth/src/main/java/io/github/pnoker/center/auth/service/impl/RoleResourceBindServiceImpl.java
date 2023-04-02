package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.query.RoleResourceBindPageQuery;
import io.github.pnoker.center.auth.mapper.ResourceMapper;
import io.github.pnoker.center.auth.mapper.RoleResourceBindMapper;
import io.github.pnoker.center.auth.service.RoleResourceBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Role;
import io.github.pnoker.common.model.RoleResourceBind;
import io.github.pnoker.common.model.RoleUserBind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author linys
 * @since 2023.04.02
 */
@Slf4j
@Service
public class RoleResourceBindServiceImpl implements RoleResourceBindService {

    @Resource
    private RoleResourceBindMapper bindMapper;

    @Resource
    private ResourceMapper resourceMapper;

    @Override
    public RoleResourceBind add(RoleResourceBind roleResourceBind) {
        if (bindMapper.insert(roleResourceBind) > 0){
            return bindMapper.selectById(roleResourceBind.getId());
        }
        throw new ServiceException("The tenant bind add failed");
    }

    @Override
    public Boolean delete(String id) {
        selectById(id);
        return bindMapper.deleteById(id) > 0;
    }

    @Override
    public RoleResourceBind update(RoleResourceBind bind) {
        selectById(bind.getId());
        bind.setOperateTime(null);
        if (bindMapper.updateById(bind) > 0) {
            return bindMapper.selectById(bind.getId());
        }
        throw new ServiceException("The role resource bind update failed");
    }

    @Override
    public RoleResourceBind selectById(String id) {
        RoleResourceBind bind = bindMapper.selectById(id);
        if (ObjectUtil.isNull(bind)) {
            throw new NotFoundException();
        }
        return bind;
    }

    @Override
    public Page<RoleResourceBind> list(RoleResourceBindPageQuery pageQuery) {
        if (ObjectUtil.isNull(pageQuery.getPage())) {
            pageQuery.setPage(new Pages());
        }
        return bindMapper.selectPage(pageQuery.getPage().convert(), fuzzyQuery(pageQuery));
    }

    @Override
    public LambdaQueryWrapper<RoleResourceBind> fuzzyQuery(RoleResourceBindPageQuery pageQuery) {
        LambdaQueryWrapper<RoleResourceBind> queryWrapper = Wrappers.<RoleResourceBind>query().lambda();
        if (ObjectUtil.isNotNull(pageQuery)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getRoleId()), RoleResourceBind::getResourceId, pageQuery.getRoleId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getResourceId()), RoleResourceBind::getResourceId, pageQuery.getResourceId());
        }
        return queryWrapper;
    }

    @Override
    public List<io.github.pnoker.common.model.Resource> listResourceByRoleId(String RoleId) {
        LambdaQueryWrapper<RoleResourceBind> queryWrapper = Wrappers.<RoleResourceBind>query().lambda();
        queryWrapper.eq(RoleResourceBind::getRoleId, RoleId);
        List<RoleResourceBind> roleResourceBinds = bindMapper.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(roleResourceBinds)){
            List<io.github.pnoker.common.model.Resource> resources = resourceMapper.selectBatchIds(roleResourceBinds.stream()
                    .map(RoleResourceBind::getResourceId).collect(Collectors.toList()));
            return resources.stream().filter(e -> EnableFlagEnum.ENABLE.equals(e.getEnableFlag()))
                    .collect(Collectors.toList());
        }

        return null;
    }
}
