package io.github.ponker.center.ekuiper.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ponker.center.ekuiper.entity.dto.*;
import io.github.ponker.center.ekuiper.entity.vo.PortableDataVO;
import io.github.ponker.center.ekuiper.entity.vo.RuleDataVO;
import io.github.ponker.center.ekuiper.exception.EkuiperException;
import io.github.ponker.center.ekuiper.service.PortableService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Service
public class PortableServiceImpl implements PortableService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;
    private List<Map<String, Map<String, Object>>> actions = new ArrayList<>();
    @Override
    public Mono<Page<PortableDataVO>> callApiWithPortableData(HttpMethod method, String url, Integer current, Integer size) {

        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    //获取响应后进行MyBatisPlus分页查询
                    List<PortableDataVO> portableDataVoList = getDataList(response);
                    Page<PortableDataVO> page = getPageSubset(current, size, portableDataVoList);
                    return Mono.just(page);
                });
    }

    @Override
    public Mono<PortableDataVO> callApiWithPortable(HttpMethod method, String url) {
        return webClient
                .method(method)
                .uri(url)
                .retrieve()
                .bodyToMono(PortableDataVO.class);

    }

    private PortableDataVO getDataOne(String response) {
        PortableDataVO data=null;
        try {
            // 解析成 JSON 数组
            JSONArray jsonArray = JSONArray.parseArray(response);
            // 遍历 JSON 数组中的每个元素, 将其转换为 Data 对象, 并添加到 dataList 中

                // 获取 JSON 对象
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                // 解析 JSON 对象的属性
                String name = jsonObject.getString("name");
                String version = jsonObject.getString("version");
                String language = jsonObject.getString("language");
                String executable = jsonObject.getString("executable");
                String[] sources = getStrings(jsonObject.getJSONArray("sources"));
                String[] sinks = getStrings(jsonObject.getJSONArray("sinks"));
                String[] functions = getStrings(jsonObject.getJSONArray("functions"));


                // 创建 Data 对象, 并添加到 dataList 中
                data = new PortableDataVO(name, version, language,executable,sources,sinks,functions);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static Page<PortableDataVO> getPageSubset(int current, int size, List<PortableDataVO> portableDataVOList) {
        int fromIndex = Math.max((current - 1) * size, 0);
        int toIndex = Math.min(current * size, portableDataVOList.size());
        List<PortableDataVO> subset = portableDataVOList.subList(fromIndex, toIndex);
        Page<PortableDataVO> page = new Page<>(current, size, portableDataVOList.size());
        page.setRecords(subset);
        return page;
    }
    private List<PortableDataVO> getDataList(String response) {
        List<PortableDataVO> portableDataVOList = new ArrayList<>();
        try {
            // 解析成 JSON 数组
            JSONArray jsonArray = JSONArray.parseArray(response);
            // 遍历 JSON 数组中的每个元素, 将其转换为 Data 对象, 并添加到 dataList 中
            for (int i = 0; i < jsonArray.size(); i++) {
                // 获取 JSON 对象
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                // 解析 JSON 对象的属性
                String name = jsonObject.getString("name");
                String version = jsonObject.getString("version");
                String language = jsonObject.getString("language");
                String executable = jsonObject.getString("executable");
                String[] sources = getStrings(jsonObject.getJSONArray("sources"));
                String[] sinks = getStrings(jsonObject.getJSONArray("sinks"));
                String[] functions = getStrings(jsonObject.getJSONArray("functions"));


                // 创建 Data 对象, 并添加到 dataList 中
                PortableDataVO data = new PortableDataVO(name, version, language,executable,sources,sinks,functions);
                portableDataVOList.add(data);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return portableDataVOList;
    }

    @NotNull
    private static String[] getStrings(JSONArray jsonArray) {
        String[] sourcesTemp=new String[jsonArray.size()];
        String[] sources=jsonArray.toArray(sourcesTemp);
        return sources;
    }
}
