package io.github.pnoker.center.manager.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.NodeRedFlowsPageQuery;
import io.github.pnoker.center.manager.mapper.NodeRedCredentialsMapper;
import io.github.pnoker.center.manager.mapper.NodeRedFlowsMapper;
import io.github.pnoker.center.manager.mapper.NodeRedLibraryMapper;
import io.github.pnoker.center.manager.mapper.NodeRedSettingsMapper;
import io.github.pnoker.center.manager.service.NodeRedService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.model.NodeRedCredentials;
import io.github.pnoker.common.model.NodeRedFlows;
import io.github.pnoker.common.model.NodeRedLibrary;
import io.github.pnoker.common.model.NodeRedSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/manager/ruleengine"})
public class NodeRedController {
    private static final Logger log = LoggerFactory.getLogger(NodeRedController.class);
    @Resource
    NodeRedFlowsMapper nodeRedFlowsMapper;
    @Resource
    NodeRedSettingsMapper nodeRedSettingsMapper;
    @Resource
    NodeRedCredentialsMapper nodeRedCredentialsMapper;
    @Resource
    NodeRedLibraryMapper nodeRedLibraryMapper;
    @Resource
    NodeRedService nodeRedService;

    @PostMapping({"flowsList"})
    public R<Page<NodeRedFlows>> flowsList(@RequestBody NodeRedFlowsPageQuery nodeRedFlowsPageQuery) {
        return R.ok(nodeRedService.flowsList(nodeRedFlowsPageQuery));
    }

    @GetMapping({"flows"})
    public R<JSONArray> getFlows(String id, HttpServletRequest request, HttpServletResponse response) {
        List<NodeRedFlows> nodeRedFlows = nodeRedFlowsMapper.selectList(new LambdaQueryWrapper<NodeRedFlows>().eq(StrUtil.isNotBlank(id) && !id.equals("DC3"), NodeRedFlows::getFlowId, id));
        List<JSONObject> collect = nodeRedFlows.stream().map(NodeRedFlows::getJsonData).map(JSONObject::parseObject).collect(Collectors.toList());
        return R.ok(new JSONArray(collect));
    }

    @PostMapping({"flows"})
    public R saveFlows(@RequestBody List<JSONObject> jsonData) {
        Optional<JSONObject> first = jsonData.stream().filter((json) -> json.getString("type").equals("tab")).findFirst();
        if (!first.isPresent()) {
            return R.fail("no tab");
        } else {
            nodeRedFlowsMapper.delete(new LambdaQueryWrapper<NodeRedFlows>().eq(NodeRedFlows::getFlowId, (first.get()).getString("id")));
            jsonData.forEach((json) -> {
                NodeRedFlows nodeRedFlows = new NodeRedFlows();
                nodeRedFlows.setFlowId((first.get()).getString("id"));
                nodeRedFlows.setJsonData(json.toJSONString());
                nodeRedFlows.setFlowLabel(json.getString("label"));
                nodeRedFlows.setFlowType(json.getString("type"));
                nodeRedFlows.setFlowDisabled(json.getBooleanValue("disabled", false) ? 1 : 0);
                nodeRedFlowsMapper.insert(nodeRedFlows);
            });
            return R.ok();
        }
    }

    @GetMapping({"settings"})
    public JSONObject getSettings() {
        NodeRedSettings nodeRedSettings = nodeRedSettingsMapper.selectOne(new QueryWrapper<>());
        return JSONObject.parseObject(nodeRedSettings.getJsonData());
    }

    @PostMapping({"settings"})
    public R saveSettings(@RequestBody JSONObject json) {
        NodeRedSettings nodeRedSettings = nodeRedSettingsMapper.selectOne(new QueryWrapper<>());
        if (nodeRedSettings == null) {
            nodeRedSettings = new NodeRedSettings();
            nodeRedSettings.setJsonData(json.toJSONString());
            nodeRedSettingsMapper.insert(nodeRedSettings);
        } else {
            nodeRedSettings.setJsonData(json.toJSONString());
            nodeRedSettingsMapper.updateById(nodeRedSettings);
        }

        return R.ok();
    }

    @GetMapping({"credentials"})
    public JSONObject getCredentials() {
        NodeRedCredentials nodeRedCredentials = nodeRedCredentialsMapper.selectOne(new QueryWrapper<>());
        return nodeRedCredentials == null ? new JSONObject() : JSONObject.parseObject(nodeRedCredentials.getJsonData());
    }

    @PostMapping({"credentials"})
    public R saveCredentials(@RequestBody JSONObject jsonData) {
        NodeRedCredentials nodeRedCredentials = nodeRedCredentialsMapper.selectOne(new QueryWrapper<>());
        if (nodeRedCredentials == null) {
            nodeRedCredentials = new NodeRedCredentials();
            nodeRedCredentials.setJsonData(jsonData.toJSONString());
            nodeRedCredentialsMapper.insert(nodeRedCredentials);
        } else {
            nodeRedCredentials.setJsonData(jsonData.toJSONString());
            nodeRedCredentialsMapper.updateById(nodeRedCredentials);
        }

        return R.ok();
    }

    @GetMapping({"library/{type}/{name}"})
    public JSONObject getLibrary(@PathVariable("type") String type, @PathVariable("name") String name) {
        NodeRedLibrary nodeRedLibrary = nodeRedLibraryMapper.selectOne(new LambdaQueryWrapper<NodeRedLibrary>().eq(NodeRedLibrary::getType, type).eq(NodeRedLibrary::getName, name));
        return JSONObject.from(nodeRedLibrary);
    }

    @PostMapping({"library/{type}/{name}"})
    public R saveLibrary(@PathVariable("type") String type, @PathVariable("name") String name, JSONObject jsonData) {
        NodeRedLibrary nodeRedLibrary = nodeRedLibraryMapper.selectOne(new LambdaQueryWrapper<NodeRedLibrary>().eq(NodeRedLibrary::getType, type).eq(NodeRedLibrary::getName, name));
        if (nodeRedLibrary == null) {
            nodeRedLibrary = new NodeRedLibrary();
            nodeRedLibrary.setType(type);
            nodeRedLibrary.setName(name);
            nodeRedLibrary.setJsonData(jsonData.getString("body"));
            nodeRedLibrary.setMeta(jsonData.getString("meta"));
            nodeRedLibraryMapper.insert(nodeRedLibrary);
        } else {
            nodeRedLibrary.setType(type);
            nodeRedLibrary.setName(name);
            nodeRedLibrary.setJsonData(jsonData.getString("body"));
            nodeRedLibrary.setMeta(jsonData.getString("meta"));
            nodeRedLibraryMapper.updateById(nodeRedLibrary);
        }

        return R.ok();
    }
}