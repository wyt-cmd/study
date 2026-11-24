package com.exam.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.user.entity.SysUser;
import com.exam.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.password:}")
    private String seedPassword;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(seedPassword)) {
            return;
        }
        ensure("admin", "管理员", "ADMIN");
        ensure("teacher", "教师", "TEACHER");
        ensure("student", "考生", "STUDENT");
    }

    private void ensure(String username, String nickname, String role) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(1);
        user.setPassword(passwordEncoder.encode(seedPassword));
        userMapper.insert(user);
        log.info("已初始化账号 {}", username);
    }
}
