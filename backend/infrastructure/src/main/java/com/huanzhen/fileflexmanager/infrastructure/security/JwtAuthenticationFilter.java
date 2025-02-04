package com.huanzhen.fileflexmanager.infrastructure.security;

import com.huanzhen.fileflexmanager.application.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import com.alibaba.fastjson2.JSON;
import com.huanzhen.fileflexmanager.domain.model.BaseResponse;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 首先尝试从header中获取token
            String jwt = null;
            final String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            } else {
                // 如果header中没有，尝试从URL参数中获取
                String authParam = request.getParameter("authorization");
                if (authParam != null) {
                    jwt = authParam;
                }
            }

            // 如果没有找到token，继续过滤器链
            if (jwt == null) {
                filterChain.doFilter(request, response);
                return;
            }

            final String username = jwtTokenProvider.getUsernameFromToken(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtTokenProvider.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            BaseResponse<Object> resp = BaseResponse.error(BaseResponse.TOKEN_INVALID_CODE, "Token无效");
            if (!userService.hasAnyUser()) {
                resp = BaseResponse.error(BaseResponse.REGISTER_ERROR, "请注册");
            }
            response.getWriter().write(JSON.toJSONString(resp));
            return;
        }

        filterChain.doFilter(request, response);
    }
} 