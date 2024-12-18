package com.atcumt.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.atcumt.common.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1.获取请求头中的用户信息
        String userId = request.getHeader("User-ID");
        // 2.判断是否为空
        if (StrUtil.isNotBlank(userId)) {
            // 不为空，保存到ThreadLocal
            UserContext.setUserId(userId);
        }
        // 3.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 移除用户
        UserContext.removeUserId();
    }
}
