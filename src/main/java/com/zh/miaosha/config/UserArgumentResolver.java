package com.zh.miaosha.config;


import com.zh.miaosha.access.UserContext;
import com.zh.miaosha.domain.MiaoshaUser;
import com.zh.miaosha.service.MiaoshaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 主要用途：
 * <p>
 * 统一封装登录的用户信息
 * 进行数据绑定，参数验证
 */
@Component
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    MiaoshaUserService userService;

    /**
     * 判断 HandlerMethodArgumentResolver 是否支持 MethodParameter
     * (PS: 一般都是通过 参数上面的注解|参数的类型)
     *
     * @param methodParameter
     * @return
     */
    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        //获取到所有方法的参数 如果是user类型 则返回true 进行处理操作
        Class<?> clazz = methodParameter.getParameterType();
        return clazz == MiaoshaUser.class;
    }

    /**
     * 获取数据, 参数绑定，解析出方法上的参数
     **/
    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        //设置参数user的值 这样我们在方法上传入user就有值了
        return UserContext.getUser();
    }
}
