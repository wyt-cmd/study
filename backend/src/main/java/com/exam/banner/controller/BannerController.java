package com.exam.banner.controller;

import com.exam.banner.service.BannerService;
import com.exam.common.R;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "轮播")
@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/active")
    public R<?> active() {
        return R.ok(bannerService.active());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<?> list() {
        return R.ok(bannerService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Long> create(@RequestBody SaveReq req) {
        return R.ok(bannerService.save(null, req.getTitle(), req.getImageUrl(), req.getLinkUrl(), req.getSortNo(), req.getStatus()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Long> update(@PathVariable Long id, @RequestBody SaveReq req) {
        return R.ok(bannerService.save(id, req.getTitle(), req.getImageUrl(), req.getLinkUrl(), req.getSortNo(), req.getStatus()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return R.ok();
    }

    @Data
    public static class SaveReq {
        private String title;
        private String imageUrl;
        private String linkUrl;
        private Integer sortNo;
        private Integer status;
    }
}
