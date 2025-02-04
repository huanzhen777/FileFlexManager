package com.huanzhen.fileflexmanager.interfaces.api.controller;

import com.huanzhen.fileflexmanager.application.service.UserService;
import com.huanzhen.fileflexmanager.domain.model.entity.User;
import com.huanzhen.fileflexmanager.infrastructure.security.JwtTokenProvider;
import com.huanzhen.fileflexmanager.interfaces.converter.UserVOConverter;
import com.huanzhen.fileflexmanager.domain.model.BaseResponse;
import com.huanzhen.fileflexmanager.interfaces.model.req.LoginRequest;
import com.huanzhen.fileflexmanager.interfaces.model.resp.LoginResponse;
import com.huanzhen.fileflexmanager.interfaces.model.resp.RegisterRequest;
import com.huanzhen.fileflexmanager.interfaces.model.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final UserVOConverter userVOConverter;
    private final PasswordEncoder passwordEncoder;


    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);

        return BaseResponse.success(new LoginResponse(token));
    }

    @GetMapping("/user")
    public BaseResponse<UserVO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);

        if (user != null) {
            return BaseResponse.success(userVOConverter.toVO(user));
        }

        return BaseResponse.error("用户不存在");
    }

    @PostMapping("/register")
    public BaseResponse<?> register(@RequestBody RegisterRequest request) {
        if (userService.hasAnyUser()) {
            return BaseResponse.error("已存在用户，不能注册管理员");
        }

        userService.createAdmin(request.username(), passwordEncoder.encode(request.password()));
        return BaseResponse.success(null);
    }
}

