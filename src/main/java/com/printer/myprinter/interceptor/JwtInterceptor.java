package com.printer.myprinter.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.printer.myprinter.WebConfig;
import com.printer.myprinter.annotation.RequireAuth;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor{

    @Override
    public boolean preHandle (
        HttpServletRequest request,
        HttpServletResponse response, Object handler)
        throws Exception{

            if (request.getMethod().equals("OPTIONS")){
                return true;
            }

        if (!(handler instanceof HandlerMethod)){
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        boolean hasRequireAuth = handlerMethod.getMethodAnnotation(RequireAuth.class) != null;

        if (!hasRequireAuth){
            return true;
        }

        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        try {
            String tokenWithOutBearer = token.replace("Bearer ", "").trim();
            JWT.require(Algorithm.HMAC256(WebConfig.getSecret()))
            .build()
            .verify(tokenWithOutBearer);

            return true;
        } catch (Exception e){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        }
    
}
