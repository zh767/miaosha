package com.zh.miaosha.access;


import com.alibaba.fastjson.JSON;
import com.zh.miaosha.domain.MiaoshaUser;
import com.zh.miaosha.redis.AccessKey;
import com.zh.miaosha.redis.RedisService;
import com.zh.miaosha.result.CodeMsg;
import com.zh.miaosha.result.Result;
import com.zh.miaosha.service.MiaoshaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

@Component
public class AccessInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        //限流处理
        if (handler instanceof HandlerMethod) {
            //获取到user 设置threadLocal 绑定一个user
            MiaoshaUser user = getUser(request, response);
            UserContext.setUser(user);//token失效了就成null了
            HandlerMethod hm = (HandlerMethod) handler;
            //获取到方法上 有没有AccessLimit注解 来判断是否限流处理
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            if (accessLimit == null) {
                return true;
            }
            //获取到限流要求的数据
            int seconds = accessLimit.seconds();
            int maxCount = accessLimit.maxCount();
            boolean needLogin = accessLimit.needLogin();
            String key = request.getRequestURI();
            if (needLogin) {
                if (user == null) {
                    render(response, CodeMsg.SESSION_ERROR);
                    return false;
                }
                key += "_" + user.getId();
            } //do nothing
            //redis记录过期时间和访问次数 例如5秒访问5次 超过则直接渲染 返回false
            AccessKey ak = AccessKey.withExpire(seconds);
            Integer count = redisService.get(ak, key, Integer.class);
            if (count == null) {
                redisService.set(ak, key, 1);
            } else if (count < maxCount) {
                redisService.incr(ak, key);
            } else {
                render(response, CodeMsg.ACCESS_LIMIT_REACHED_ONE);//个人请求次数上限
                return false;
            }
        }
        return true;
    }

    private void render(HttpServletResponse response, CodeMsg cm) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        OutputStream out = response.getOutputStream();
        String str = JSON.toJSONString(Result.error(cm));
        out.write(str.getBytes("UTF-8"));
        out.flush();
        out.close();
    }

    private MiaoshaUser getUser(HttpServletRequest request, HttpServletResponse response) {
        //从cookie和参数中获取token
        String paramToken = request.getParameter(MiaoshaUserService.COOKI_NAME_TOKEN);
        String cookieToken = getCookieValue(request, MiaoshaUserService.COOKI_NAME_TOKEN);
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        //用userService获取到返回
        return userService.getByToken(response, token);
    }

    //遍历cookie 找到token
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length <= 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieName)) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
