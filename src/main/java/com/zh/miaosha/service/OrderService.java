package com.zh.miaosha.service;


import com.zh.miaosha.dao.OrderDao;
import com.zh.miaosha.domain.MiaoshaOrder;
import com.zh.miaosha.domain.MiaoshaUser;
import com.zh.miaosha.domain.OrderInfo;
import com.zh.miaosha.redis.OrderKey;
import com.zh.miaosha.redis.RedisService;
import com.zh.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    OrderDao orderDao;

    @Autowired
    RedisService redisService;

    public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(long userId, long goodsId) {
        MiaoshaOrder miaoshaOrder = redisService.get(OrderKey.getMiaoshaOrderByUidGid,
                "" + userId + "_" + goodsId, MiaoshaOrder.class);
        if (miaoshaOrder == null) {
            miaoshaOrder = orderDao.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);
        }
        return miaoshaOrder;
    }

    public OrderInfo getOrderById(long orderId) {
        return orderDao.getOrderById(orderId);
    }


    @Transactional
    public OrderInfo createOrder(MiaoshaUser user, GoodsVo good) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsCount(1);
        orderInfo.setGoodsId(good.getId());
        orderInfo.setGoodsName(good.getGoodsName());
        orderInfo.setGoodsPrice(good.getMiaoshaPrice());
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);
        orderInfo.setUserId(user.getId());
        orderDao.insert(orderInfo);
        MiaoshaOrder miaoshaOrder = new MiaoshaOrder();
        miaoshaOrder.setGoodsId(good.getId());
        miaoshaOrder.setOrderId(orderInfo.getId());
        miaoshaOrder.setUserId(user.getId());
        orderDao.insertMiaoshaOrder(miaoshaOrder);
        //这里使用redis页应该 设置过期时间
        redisService.set(OrderKey.getMiaoshaOrderByUidGid, "" + user.getId() + "_" + good.getId(), miaoshaOrder);

        return orderInfo;
    }

    public void deleteOrders() {
        orderDao.deleteOrders();
        orderDao.deleteMiaoshaOrders();
    }

    public void expireOrder(OrderInfo orderInfo) {
        orderDao.deleteOrder(orderInfo.getId());
        orderDao.deleteMiaoshaOrder(orderInfo.getUserId(), orderInfo.getGoodsId(), orderInfo.getId());
    }

    public List<OrderInfo> selectExpireOrders() {
        return orderDao.selectExpireOrders();
    }
}
