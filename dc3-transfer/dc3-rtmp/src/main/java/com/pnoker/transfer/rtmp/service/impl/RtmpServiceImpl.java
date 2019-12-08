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
import com.pnoker.common.base.bean.Response;
import com.pnoker.common.base.dto.PageInfo;
import com.pnoker.common.base.dto.transfer.RtmpDto;
import com.pnoker.common.base.model.rtmp.Rtmp;
import com.pnoker.dbs.api.rtmp.feign.RtmpDbsFeignClient;
import com.pnoker.transfer.rtmp.handler.Transcode;
import com.pnoker.transfer.rtmp.handler.TranscodePool;
import com.pnoker.transfer.rtmp.service.RtmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    private RtmpDbsFeignClient rtmpDbsFeignClient;

    @Override
    public Response<Boolean> add(Rtmp rtmp) {
        //todo 如何保持事务
        Response<Long> response = rtmpDbsFeignClient.add(rtmp);
        if (response.isOk()) {
            rtmp.setId(response.getData());
            Transcode transcode = new Transcode(rtmp);
            if (!TranscodePool.transcodeMap.containsKey(transcode.getId())) {
                TranscodePool.transcodeMap.put(transcode.getId(), transcode);
                return Response.ok();
            } else {
                return Response.fail("任务重复,表记录添加成功");
            }
        }
        return Response.fail(response.getMessage());
    }

    @Override
    public Response<Boolean> delete(Long id) {
        //todo 如何保持事务
        Transcode transcode = TranscodePool.transcodeMap.get(id);
        if (Optional.ofNullable(transcode).isPresent()) {
            if (!transcode.isRun()) {
                TranscodePool.transcodeMap.remove(id);
                Response<Boolean> response = rtmpDbsFeignClient.delete(id);
                if (response.isOk()) {
                    return Response.ok();
                } else {
                    return Response.fail("任务删除成功,表记录删除失败");
                }
            } else {
                return Response.fail("任务运行中");
            }
        }
        return Response.fail("任务不存在");
    }

    @Override
    public Response<Boolean> update(Rtmp rtmp) {
        //todo 如何保持事务
        Transcode transcode = TranscodePool.transcodeMap.get(rtmp.getId());
        if (Optional.ofNullable(transcode).isPresent()) {
            if (!transcode.isRun()) {
                TranscodePool.transcodeMap.put(transcode.getId(), new Transcode(rtmp));
                if (rtmpDbsFeignClient.update(rtmp).isOk()) {
                    return Response.ok();
                } else {
                    return Response.fail("任务更新成功,表记录更新失败");
                }
            } else {
                return Response.fail("任务运行中");
            }
        }
        return Response.fail("任务不存在");
    }

    @Override
    public Response<Page<Rtmp>> list(Rtmp rtmp, PageInfo pageInfo) {
        RtmpDto rtmpDto = new RtmpDto();
        BeanUtils.copyProperties(rtmp, rtmpDto);
        rtmpDto.setPage(pageInfo);
        return rtmpDbsFeignClient.list(rtmpDto);
    }

    @Override
    public Response<Rtmp> selectById(Long id) {
        return rtmpDbsFeignClient.selectById(id);
    }

    @Override
    public Response<Boolean> start(Long id) {
        Transcode transcode = TranscodePool.transcodeMap.get(id);
        if (Optional.ofNullable(transcode).isPresent()) {
            if (!transcode.isRun()) {
                transcode.start();
                Response<Boolean> response = rtmpDbsFeignClient.update(new Rtmp(id, true));
                if (response.isOk()) {
                    return Response.ok();
                } else {
                    return Response.fail("任务启动成功,表记录修改失败");
                }
            } else {
                return Response.fail("任务运行中");
            }
        }
        return Response.fail("任务不存在");
    }

    @Override
    public Response<Boolean> stop(Long id) {
        Transcode transcode = TranscodePool.transcodeMap.get(id);
        if (Optional.ofNullable(transcode).isPresent()) {
            if (!transcode.isRun()) {
                transcode.stop();
                Response<Boolean> response = rtmpDbsFeignClient.update(new Rtmp(id, false));
                if (response.isOk()) {
                    return Response.ok();
                } else {
                    return Response.fail("任务启动成功,表记录修改失败");
                }
            } else {
                return Response.fail("任务运行中");
            }
        }
        return Response.fail("任务不存在");
    }

}
