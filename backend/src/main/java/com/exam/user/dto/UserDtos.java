package com.exam.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

public final class UserDtos {
    private UserDtos() {
    }

    @Data
    public static class SaveUser {
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名过长")
        private String username;
        @Size(max = 32, message = "密码最长 32 位")
        private String password;
        @NotBlank(message = "昵称不能为空")
        private String nickname;
        @NotBlank(message = "角色不能为空")
        private String role;
    }

    @Data
    public static class UserVO {
        private Long id;
        private String username;
        private String nickname;
        private String role;
        private Integer status;
        private LocalDateTime createTime;
    }
}
