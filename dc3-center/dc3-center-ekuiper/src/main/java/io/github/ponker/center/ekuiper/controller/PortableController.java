package io.github.ponker.center.ekuiper.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.utils.FileUtil;
import io.github.ponker.center.ekuiper.constant.ServiceConstant;
import io.github.ponker.center.ekuiper.entity.R;
import io.github.ponker.center.ekuiper.entity.vo.PortableDataVO;
import io.github.ponker.center.ekuiper.service.ApiService;
import io.github.ponker.center.ekuiper.service.PortableService;
import io.github.ponker.center.ekuiper.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping(ServiceConstant.Ekuiper.PORTABLE_URL_PREFIX)
public class PortableController {
    @Autowired
    private UrlService urlService;
    @Autowired
    private PortableService portableService;
    @Autowired
    private ApiService apiService;
    @GetMapping("/list")
    public Mono<R<Page<PortableDataVO>>> getAllPortables(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                                         @RequestParam(required = false, defaultValue = "10") Integer pageSize){
        Mono<Page<PortableDataVO>> pageMono=portableService.callApiWithPortableData(HttpMethod.GET,urlService.getPortableUrl(),pageNum,pageSize);
        return pageMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s, "successful"));
            } catch (Exception e) {
                log.error("Failed to call listAllPortable portable API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });

    }
    @GetMapping("/getOne/{name}")
    public Mono<R<PortableDataVO>> getOnePortable(@PathVariable String name){
        String url=urlService.getPortableUrl()+"/"+name;
        Mono<PortableDataVO> portableDataVOMono=portableService.callApiWithPortable(HttpMethod.GET,url);
        return portableDataVOMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s, "successful"));
            } catch (Exception e) {
                log.error("Failed to call getOnePortable portable API", e);
                return Mono.just(null);
            }
        });

    }

    @PostMapping("/upload")
    public Mono<R<String>> uploadFile(@RequestPart("file") FilePart file) {
        File realPath =new File("eKuiper-portable");
        if (!realPath.exists()) {
            realPath.mkdirs();
        }
        File destinationFile = new File(realPath , file.filename());
        return file.transferTo(destinationFile)
                .then(Mono.just(R.ok("file://"+destinationFile.getAbsolutePath())));
    }

    @PostMapping("/create")
    public Mono<R<String>> createPortable(@Validated @RequestBody Object form) {
        Mono<String> stringMono = apiService.callApiWithData(form, HttpMethod.POST, urlService.getPortableUrl());
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call create Portable API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }
    @DeleteMapping("/delete/{name}")
    public Mono<R<String>> deletePortable(@PathVariable String name){
        String url=urlService.getPortableUrl()+"/"+name;
        Mono<String> stringMono = apiService.callApi(HttpMethod.DELETE, url);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call delete Stream API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });

    }
    @PutMapping("/update/{name}")
    public Mono<R<String>> updatePortable(@Validated @RequestBody Object form,@PathVariable String name){
        String url=urlService.getPortableUrl()+"/"+name;
        Mono<String> stringMono = apiService.callApiWithData(form, HttpMethod.PUT, url);
        return stringMono.flatMap(s -> {
            try {
                return Mono.just(R.ok(s));
            } catch (Exception e) {
                log.error("Failed to call update Portables API", e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }
}
