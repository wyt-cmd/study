package com.exam.user.controller;

import com.exam.common.R;
import com.exam.user.dto.UserDtos;
import com.exam.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public R<?> page(@RequestParam(defaultValue = "1") long current,
                     @RequestParam(defaultValue = "10") long size,
                     @RequestParam(required = false) String keyword) {
        return R.ok(userService.page(current, size, keyword));
    }

    @PostMapping
    public R<Void> create(@Valid @RequestBody UserDtos.SaveUser req) {
        userService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserDtos.SaveUser req) {
        userService.update(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    public R<Void> status(@PathVariable Long id, @RequestParam int status) {
        userService.changeStatus(id, status);
        return R.ok();
    }
}
