package com.zh.miaosha.rabbitmq;


import com.zh.miaosha.redis.GoodsKey;
import com.zh.miaosha.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQSender {

    private static Logger log = LoggerFactory.getLogger(MQSender.class);

    @Autowired
    AmqpTemplate amqpTemplate;
    @Autowired
    RedisService redisService;

    public void sendMiaoshaMessage(MiaoshaMessage mm) {
        String msg = RedisService.beanToString(mm);
        log.info("send message:" + msg);
        //rabbitTemplate.convertAndSend(MQConfig.MIAOSHA_QUEUE, msg);
        try {
            amqpTemplate.convertAndSend(MQConfig.MIAOSHA_QUEUE, msg);//消息不会丢失
        } catch (AmqpException e) {
            e.printStackTrace();
        }
    }


/*	public void send(Object message) {
		String msg = RedisService.beanToString(message);
		log.info("send message:"+msg);
		amqpTemplate.convertAndSend(MQConfig.QUEUE, msg);
	}

	public void sendTopic(Object message) {
		String msg = RedisService.beanToString(message);
		log.info("send topic message:"+msg);
		amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key1", msg+"1");
		amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key2", msg+"2");
	}

	public void sendFanout(Object message) {
		String msg = RedisService.beanToString(message);
		log.info("send fanout message:"+msg);
		amqpTemplate.convertAndSend(MQConfig.FANOUT_EXCHANGE, "", msg);
	}

	public void sendHeader(Object message) {
		String msg = RedisService.beanToString(message);
		log.info("send fanout message:"+msg);
		MessageProperties properties = new MessageProperties();
		properties.setHeader("header1", "value1");
		properties.setHeader("header2", "value2");
		Message obj = new Message(msg.getBytes(), properties);
		amqpTemplate.convertAndSend(MQConfig.HEADERS_EXCHANGE, "", obj);
	}*/


}
