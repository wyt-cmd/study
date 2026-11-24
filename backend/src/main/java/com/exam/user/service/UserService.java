package com.exam.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BizException;
import com.exam.security.JwtAuthFilter;
import com.exam.user.dto.UserDtos;
import com.exam.user.entity.SysUser;
import com.exam.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> ROLES = Set.of("ADMIN", "TEACHER", "STUDENT");

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;

    public Page<UserDtos.UserVO> page(long current, long size, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(keyword), SysUser::getUsername, keyword)
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        Page<UserDtos.UserVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVo).toList());
        return result;
    }

    @Transactional
    public void create(UserDtos.SaveUser req) {
        checkRole(req.getRole());
        if (!StringUtils.hasText(req.getPassword()) || req.getPassword().length() < 6) {
            throw new BizException("新建用户必须设置 6 到 32 位密码");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername())) > 0) {
            throw new BizException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname().trim());
        user.setRole(req.getRole());
        user.setStatus(1);
        userMapper.insert(user);
    }

    @Transactional
    public void update(Long id, UserDtos.SaveUser req) {
        checkRole(req.getRole());
        SysUser user = require(id);
        user.setNickname(req.getNickname().trim());
        user.setRole(req.getRole());
        if (StringUtils.hasText(req.getPassword())) {
            if (req.getPassword().length() < 6) {
                throw new BizException("密码长度为 6 到 32 位");
            }
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            redis.delete(JwtAuthFilter.TOKEN_KEY + id);
        }
        userMapper.updateById(user);
    }

    @Transactional
    public void changeStatus(Long id, int status) {
        SysUser user = require(id);
        user.setStatus(status == 1 ? 1 : 0);
        userMapper.updateById(user);
        if (user.getStatus() == 0) {
            redis.delete(JwtAuthFilter.TOKEN_KEY + id);
        }
    }

    public SysUser require(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private void checkRole(String role) {
        if (!ROLES.contains(role)) {
            throw new BizException("角色只能是 ADMIN、TEACHER、STUDENT");
        }
    }

    private UserDtos.UserVO toVo(SysUser user) {
        UserDtos.UserVO vo = new UserDtos.UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
