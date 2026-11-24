package com.exam.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.auth.dto.LoginRequest;
import com.exam.common.BizException;
import com.exam.security.AuthUser;
import com.exam.security.JwtAuthFilter;
import com.exam.security.JwtService;
import com.exam.user.entity.SysUser;
import com.exam.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redis;

    public Map<String, Object> login(LoginRequest req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "账号已停用");
        }
        String jti = UUID.randomUUID().toString();
        String token = jwtService.create(user, jti);
        redis.opsForValue().set(JwtAuthFilter.TOKEN_KEY + user.getId(), jti, Duration.ofSeconds(jwtService.expireSeconds()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("userId", user.getId());
        body.put("username", user.getUsername());
        body.put("nickname", user.getNickname());
        body.put("role", user.getRole());
        return body;
    }

    public void logout() {
        AuthUser user = AuthUser.require();
        redis.delete(JwtAuthFilter.TOKEN_KEY + user.id());
    }

    public Map<String, Object> me() {
        AuthUser current = AuthUser.require();
        SysUser user = userMapper.selectById(current.id());
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", user.getId());
        body.put("username", user.getUsername());
        body.put("nickname", user.getNickname());
        body.put("role", user.getRole());
        return body;
    }
}
