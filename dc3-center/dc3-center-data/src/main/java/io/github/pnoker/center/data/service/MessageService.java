package io.github.pnoker.center.data.service;




import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;

public interface MessageService {
    RabbitMQDataVo queryMQInMess(String cluster);
    RabbitMQDataVo queryMQReMess(String cluster);
    RabbitMQDataVo queryMQUnackMess(String cluster);
    RabbitMQDataVo queryMQOutMess(String cluster);
    RabbitMQDataVo queryMQMessPub(String cluster);
    RabbitMQDataVo queryMQConfPub(String cluster);
    RabbitMQDataVo queryMQRoutQue(String cluster);
    RabbitMQDataVo queryMQUnConfPub(String cluster);
    RabbitMQDataVo queryMQUnRoutDrop(String cluster);
    RabbitMQDataVo queryMQUnRoutPub(String cluster);
    RabbitMQDataVo queryMQMessDel(String cluster);
    RabbitMQDataVo queryMQMessReDel(String cluster);
    RabbitMQDataVo queryMQMessDelAck(String cluster);
    RabbitMQDataVo queryMQMessDelAuto(String cluster);
    RabbitMQDataVo queryMQMessAck(String cluster);
    RabbitMQDataVo queryMQPoAutoAck(String cluster);
    RabbitMQDataVo queryMQPoNoResult(String cluster);
    RabbitMQDataVo queryMQPoWithAck(String cluster);
}
