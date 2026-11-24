package com.exam.category.controller;

import com.exam.category.service.CategoryService;
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

@Tag(name = "题目分类")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public R<?> tree() {
        return R.ok(categoryService.tree());
    }

    @GetMapping
    public R<?> list() {
        return R.ok(categoryService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> create(@RequestBody SaveReq req) {
        categoryService.save(null, req.getName(), req.getParentId(), req.getSortNo());
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> update(@PathVariable Long id, @RequestBody SaveReq req) {
        categoryService.save(id, req.getName(), req.getParentId(), req.getSortNo());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }

    @Data
    public static class SaveReq {
        private String name;
        private Long parentId;
        private Integer sortNo;
    }
}
