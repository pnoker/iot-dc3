/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.transfer.rtmp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.bean.Response;
import com.pnoker.common.dto.transfer.RtmpDto;
import com.pnoker.common.entity.rtmp.Rtmp;
import com.pnoker.dbs.api.rtmp.feign.RtmpDbsFeignClient;
import com.pnoker.transfer.rtmp.handler.Transcode;
import com.pnoker.transfer.rtmp.handler.TranscodePool;
import com.pnoker.transfer.rtmp.service.RtmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Service
public class RtmpServiceImpl implements RtmpService {

    @Resource
    private RtmpDbsFeignClient rtmpDbsFeignClient;

    @Override
    public Response<Boolean> add(Rtmp rtmp) {
        Response<Long> response = rtmpDbsFeignClient.add(rtmp);
        if (response.isOk()) {
            rtmp.setId(response.getData());
            Transcode transcode = new Transcode(rtmp);
            if (!TranscodePool.transcodeMap.containsKey(transcode.getId())) {
                TranscodePool.transcodeMap.put(transcode.getId(), transcode);
                return Response.ok();
            }
            return Response.fail("任务重复,表记录添加成功");
        }
        return Response.fail(response.getMessage());
    }

    @Override
    public Response<Boolean> delete(Long id) {
        Response<Rtmp> response = rtmpDbsFeignClient.selectById(id);
        if (response.isOk()) {
            Transcode transcode = TranscodePool.transcodeMap.get(id);
            if (Optional.ofNullable(transcode).isPresent()) {
                if (transcode.isRun()) {
                    return Response.fail("任务运行中");
                }
                TranscodePool.transcodeMap.remove(id);
            }
            return rtmpDbsFeignClient.delete(id).isOk() ? Response.ok() : Response.fail("任务删除成功,表记录删除失败");
        }
        return Response.fail("任务不存在");
    }

    @Override
    public Response<Boolean> update(Rtmp rtmp) {
        Response<Rtmp> response = rtmpDbsFeignClient.selectById(rtmp.getId());
        if (response.isOk()) {
            Transcode transcode = TranscodePool.transcodeMap.get(rtmp.getId());
            if (Optional.ofNullable(transcode).isPresent()) {
                if (transcode.isRun()) {
                    return Response.fail("任务运行中");
                }
                TranscodePool.transcodeMap.put(transcode.getId(), new Transcode(rtmp));
            }
            return rtmpDbsFeignClient.update(rtmp).isOk() ? Response.ok() : Response.fail("任务更新成功,表记录更新失败");
        }
        return Response.fail("任务不存在");
    }

    @Override
    public Response<Rtmp> selectById(Long id) {
        return rtmpDbsFeignClient.selectById(id);
    }

    @Override
    public Response<Page<Rtmp>> list(Rtmp rtmp, Pages pages) {
        RtmpDto rtmpDto = new RtmpDto();
        rtmpDto.convertToDto(rtmp);
        return rtmpDbsFeignClient.list(rtmpDto.setPage(pages));
    }

    @Override
    public Response<Boolean> start(Long id) {
        Response<Rtmp> response = rtmpDbsFeignClient.selectById(id);
        if (response.isOk()) {
            Transcode transcode = TranscodePool.transcodeMap.get(id);
            if (Optional.ofNullable(transcode).isPresent()) {
                if (transcode.isRun()) {
                    return Response.fail("任务已是启动状态");
                }
            } else {
                transcode = new Transcode(response.getData());
                TranscodePool.transcodeMap.put(transcode.getId(), transcode);
            }
            TranscodePool.threadPoolExecutor.execute(() -> TranscodePool.transcodeMap.get(id).start());
            return rtmpDbsFeignClient.update(response.getData().setRun(true)).isOk() ? Response.ok() : Response.fail("任务启动成功，表记录更新失败");
        }
        return Response.fail("任务不存在");
    }

    @Override
    public Response<Boolean> stop(Long id) {
        Response<Rtmp> response = rtmpDbsFeignClient.selectById(id);
        if (response.isOk()) {
            Transcode transcode = TranscodePool.transcodeMap.get(id);
            if (Optional.ofNullable(transcode).isPresent()) {
                if (transcode.isRun()) {
                    transcode.stop();
                    return rtmpDbsFeignClient.update(response.getData().setRun(false)).isOk() ? Response.ok() : Response.fail("任务停止成功，表记录更新失败");
                }
            }
            return Response.fail("任务已是停止状态");
        }
        return Response.fail("任务不存在");
    }

}
