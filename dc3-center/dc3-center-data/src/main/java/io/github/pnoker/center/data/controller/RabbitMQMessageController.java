package io.github.pnoker.center.data.controller;



import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;
import io.github.pnoker.center.data.service.MessageService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping(DataConstant.RABBITMQ_MESSAGES_URL_PREFIX)
public class RabbitMQMessageController implements BaseController {
    @Resource
    private MessageService messageService;

    @GetMapping("/InMess")
    public R<RabbitMQDataVo> queryInMess(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQInMess(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/ReMess")
    public R<RabbitMQDataVo> queryReMess(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQReMess(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/UnackMess")
    public R<RabbitMQDataVo> queryUnackMess(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQUnackMess(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @GetMapping("/OutMess")
    public R<RabbitMQDataVo> queryOutMess(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQOutMess(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/MessPub")
    public R<RabbitMQDataVo> queryMessPub(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQMessPub(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/ConfPub")
    public R<RabbitMQDataVo> queryConfPub(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQConfPub(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/RoutQue")
    public R<RabbitMQDataVo> queryRoutQue(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQRoutQue(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/UnConfPub")
    public R<RabbitMQDataVo> queryUnConfPub(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQUnConfPub(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/UnRoutDrop")
    public R<RabbitMQDataVo> queryUnRoutDrop(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQUnRoutDrop(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/UnRoutPub")
    public R<RabbitMQDataVo> queryUnRoutPub(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQUnRoutPub(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/MessDel")
    public R<RabbitMQDataVo> queryMessDel(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQMessDel(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/MessReDel")
    public R<RabbitMQDataVo> queryMessReDel(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQMessReDel(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/MessDelAck")
    public R<RabbitMQDataVo> queryMessDelAck(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQMessDelAck(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/MessDelAuto")
    public R<RabbitMQDataVo> queryMessDelAuto(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQMessDelAuto(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/MessAck")
    public R<RabbitMQDataVo> queryMessAck(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQMessAck(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getValues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/PoAutoAck")
    public R<RabbitMQDataVo> queryPoAutoAck(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQPoAutoAck(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/PoNoResult")
    public R<RabbitMQDataVo> queryPoNoResult(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQPoNoResult(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
    @GetMapping("/PoWithAck")
    public R<RabbitMQDataVo> queryPoWithAck(@RequestParam String cluster) {
        try{
            RabbitMQDataVo rabbbit= messageService.queryMQPoWithAck(cluster);
            if(!rabbbit.getTimes().isEmpty() && !rabbbit.getIvalues().isEmpty()){
                return R.ok(rabbbit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
