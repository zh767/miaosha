## 介绍
java实现的秒杀网站，基于Spring Boot 2.X。

GitHub 地址为https://github.com/zh767/miaosha/


## 技术栈
```
jdk8
Spring Boot 2.X
MyBatis
Redis
Redisson
MySQL+druid
Thymeleaf + Bootstrap
RabbitMQ
```

## 快速启动项目
git clone git@github.com:zh767/miaosha.git

执行sql脚本

修改application.properties里面的自己的Redis,MySQL,RabbitMQ的连接配置

运行MiaoshaApplication.java

访问http://localhost:8080/login/to_login 登录

## 调错排查
```
1.java.net.SocketException: Socket Closed--nested exception is com.rabbitmq.client.AuthenticationFailureException: ACCESS_REFUSED
03/10-16:51:28 [main] WARN  org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext- Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'initTask': Injection of resource dependencies failed; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'MQConsumer': Injection of resource dependencies failed; nested exception is org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'seckillServiceImpl': Unsatisfied dependency expressed through field 'mqProducer'; nested exception is org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'MQProducer': Unsatisfied dependency expressed through field 'mqChannelManager'; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'MQChannelManager': Injection of resource dependencies failed; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'mqConnectionSeckill' defined in class path resource [com/liushaoming/jseckill/backend/config/MQConfig.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [com.rabbitmq.client.Connection]: Factory method 'mqConnectionSeckill' threw exception; nested exception is com.rabbitmq.client.AuthenticationFailureException: ACCESS_REFUSED - Login was refused using authentication mechanism PLAIN. For details see the broker logfile.
03/10-16:51:28 [AMQP Connection 47.99.196.243:5672] ERROR com.rabbitmq.client.impl.ForgivingExceptionHandler- An unexpected connection driver error occured
java.net.SocketException: Socket Closed
	at java.net.SocketInputStream.socketRead0(Native Method)
	at java.net.SocketInputStream.socketRead(SocketInputStream.java:116)
	at java.net.SocketInputStream.read(SocketInputStream.java:170)
	at java.net.SocketInputStream.read(SocketInputStream.java:141)
	at java.io.BufferedInputStream.fill(BufferedInputStream.java:246)
	at java.io.BufferedInputStream.read(BufferedInputStream.java:265)
	at java.io.DataInputStream.readUnsignedByte(DataInputStream.java:288)
	at com.rabbitmq.client.impl.Frame.readFrom(Frame.java:91)
	at com.rabbitmq.client.impl.SocketFrameHandler.readFrame(SocketFrameHandler.java:164)
	at com.rabbitmq.client.impl.AMQConnection$MainLoop.run(AMQConnection.java:596)
	at java.lang.Thread.run(Thread.java:745)
03/10-16:51:28 [main] INFO  com.alibaba.druid.pool.DruidDataSource- {dataSource-1} closed
03/10-16:51:28 [main] INFO  org.apache.catalina.core.StandardService- Stopping service [Tomcat]
03/10-16:51:28 [main] INFO  org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener- 

Error starting ApplicationContext. To display the conditions report re-run your application with 'debug' enabled.
```
**分析**： 这里关键点是nested exception is com.rabbitmq.client.AuthenticationFailureException: ACCESS_REFUSED
并且进一步说了- Login was refused using authentication mechanism PLAINM

说明这里是RabbitMQ的用户名和密码认证失败。需要修改下application-dev.properties里RabbitMQ的用户名和密码相关配置。

### 一些问题
#### 如何解决卖超问题
```
每次秒杀前先获取是否已经秒杀过
redisson 解决一个多个请求重复卖 锁住userId_goodId
数据库层面更新时带条件stock > 0 保证不会出现负数
```
#### 你怎么做秒杀的优化:
```
1、预先加载商品到redis 减少服务器压力
2、设置秒杀未开始 按钮置灰
3、输入验证码 防止恶意刷单
4、md5隐藏秒杀地址 加密以后 和 redis放入的做对比 正确继续进行秒杀
5、正确以后 如果该用户达到访问次数 返回
6、再获取令牌桶 减少服务器压力
7、再redis中的预减库存 
8、再异步下单 削峰 返回前台排队中 秒杀流程结束将结果标志放到redis 
9、前台轮询请求redis是否成功
10、用户得到结果
```
#### 解决分布式session
```
--生成随机的uuid作为cookie返回并redis内存写入 
--拦截器每次拦截方法，来重新获根据cookie获取对象
--下一个页面拿到key重新获取对象
--HandlerMethodArgumentResolver 方法 supportsParameter 如果为true 执行 resolveArgument 方法获取miaoshauser对象
--如果有缓存的话 这个功能实现起来就和简单，在一个用户访问接口的时候我们把访问次数写到缓存中，在加上一个有效期。
   通过拦截器. 做一个注解 @AccessLimit 然后封装这个注解，可以有效的设置每次访问多少次，有效时间是否需要登录！
```

#### redis的库存如何与数据库的库存保持一致
```
redis的数量不是真的库存,他的作用仅仅只是为了阻挡多余的请求透穿到DB，起到一个保护的作用
因为秒杀的商品有限，比如10个，让1万个请求区访问DB是没有意义的，因为最多也就只能10个
请求下单成功。
我们只要保证数据库的最终一致性即可，我们redis的库存为0了以后,查询数据库是否真的为0,
如果是，则设置一个内存标记，比如map，key是商品id，value是boolean值，
判断是否售罄,没有售罄则设置库存回redis，否则设置售罄，后面的请求直接返回。

```
## 代码解析
### 一、总体流程
1、先获取隐藏的秒杀地址 再进行真正的秒杀
2、根据个人请求次数和令牌桶限流
3、获取到redis 预减库存
4、rabbitmq 异步减库 削峰 前端返回排队中
5、前端轮询获取异步执行结果
### 二、Java后端限流
使用Google guava的RateLimiter来进行限流
例如：每秒钟只允许10个人进入秒杀步骤. (可能是拦截掉90%的用户请求，拦截后直接返回"很遗憾，没抢到")
AccessLimitServiceImpl.java代码

```java
/**
 * 秒杀前的限流.
 * 使用了Google guava的RateLimiter
 */
@Service
public class AccessLimitServiceImpl implements AccessLimitService {
    /**
     * 每秒钟只发出10个令牌，拿到令牌的请求才可以进入秒杀过程
     */
    private RateLimiter seckillRateLimiter = RateLimiter.create(10);

    /**
     * 尝试获取令牌
     * @return
     */
    @Override
    public boolean tryAcquireSeckill() {
        return seckillRateLimiter.tryAcquire();
    }
}
```

### 三、隐藏秒杀地址
```
    public String createMiaoshaPath(MiaoshaUser user, long goodsId) {
        if (user == null || goodsId <= 0) {
            return null;
        }
        String str = MD5Util.md5(UUIDUtil.uuid() + "123456");
        redisService.set(MiaoshaKey.getMiaoshaPath, "" + user.getId() + "_" + goodsId, str);
        return str;
    }
```
秒杀时 验证前端的path是否与redis存放的一致
```
@RequestMapping(value = "/{path}/do_miaosha", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
                                   @RequestParam("goodsId") long goodsId,
                                   @PathVariable("path") String path) {
        model.addAttribute("user", user);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        //验证path
        boolean check = miaoshaService.checkPath(user, goodsId, path);
        if (!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        //·····
}
```
### 四、异步下单
```
    @RequestMapping(value = "/{path}/do_miaosha", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
                                   @RequestParam("goodsId") long goodsId,
                                   @PathVariable("path") String path) {
        model.addAttribute("user", user);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        //验证path
        boolean check = miaoshaService.checkPath(user, goodsId, path);
        if (!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        //内存标记，减少redis访问
        boolean over = localOverMap.get(goodsId);
        if (over) {
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //预减库存
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);//10
        if (stock < 0) {//由于 失败 和 重复秒杀 等情况 这个数可能还没有卖完 删掉缓存 查数据库
            redisService.delete(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);
            long stockFromDataBase = goodsService.getStock(goodsId);
            if (stockFromDataBase == 0) {
                localOverMap.put(goodsId, true);//确实卖完了 返回
            } else {
                redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goodsId, stockFromDataBase);
            }
        }

        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if (order != null) {
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }

        //入队
        MiaoshaMessage mm = new MiaoshaMessage();
        mm.setUser(user);
        mm.setGoodsId(goodsId);
        sender.sendMiaoshaMessage(mm);
        return Result.success(0);//排队中
```
mq消费消息
```
@RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
    public void receive(String message) {
        log.info("receive message:" + message);
        MiaoshaMessage mm = RedisService.stringToBean(message, MiaoshaMessage.class);
        MiaoshaUser user = mm.getUser();
        long goodsId = mm.getGoodsId();

        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        int stock = goods.getStockCount();
        if (stock <= 0) {
            return;
        }
        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if (order != null) {
            return;
        }
        //减库存 下订单 写入秒杀订单
        miaoshaService.miaosha(user, goods);
    }
```
异步秒杀逻辑
```
@Transactional
    public OrderInfo miaosha2(MiaoshaUser user, GoodsVo goods) {
        final String lockKey = new StringBuffer().append(goods.getId()).append(user.getId()).append("-RedissonLock").toString();
        RLock lock = redissonClient.getLock(lockKey);
        OrderInfo orderInfo = null;
        try {
            boolean cacheRes = lock.tryLock(30, 10, TimeUnit.SECONDS);
            if (cacheRes) {
                //处理逻辑
                boolean success = goodsService.reduceStock(goods);
                if (success) {
                    orderInfo = orderService.createOrder(user, goods);//带事务
                } else {//抢购过改商品了
                    setGoodsOver(goods.getId());
                    //return null;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return orderInfo;
    }
```
### 五、30分钟超时订单失效
定时任务
```
@Scheduled(cron = "0 0/30 * * * ?")
    public void schedulerExpireOrders() {
        try {
            List<OrderInfo> list = orderService.selectExpireOrders();
            if (list != null && !list.isEmpty()) {
                //java8的写法
                list.stream().forEach(i -> {
                    if (i != null && i.getDiffTime() > env.getProperty("scheduler.expire.orders.time", Integer.class)) {
                        orderService.expireOrder(i);
                    }
                });
            }
        } catch (Exception e) {
            log.error("定时获取status=0的订单并判断是否超过TTL，然后进行失效-发生异常：", e.fillInStackTrace());
        }
    }
```
service 扣除订单
```
    public void expireOrder(OrderInfo orderInfo) {
        orderDao.deleteOrder(orderInfo.getId());
        orderDao.deleteMiaoshaOrder(orderInfo.getUserId(), orderInfo.getGoodsId(), orderInfo.getId());
    }
```