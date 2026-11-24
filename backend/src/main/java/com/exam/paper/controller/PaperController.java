package com.exam.paper.controller;

import com.exam.common.R;
import com.exam.paper.dto.PaperDtos;
import com.exam.paper.service.PaperService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "试卷管理")
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<?> page(@RequestParam(defaultValue = "1") long current,
                     @RequestParam(defaultValue = "10") long size,
                     @RequestParam(required = false) String status,
                     @RequestParam(required = false) String keyword) {
        return R.ok(paperService.page(current, size, status, keyword, false));
    }

    @GetMapping("/available")
    public R<?> available(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size) {
        return R.ok(paperService.page(current, size, null, null, true));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(paperService.detail(id, true));
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Long> manual(@Valid @RequestBody PaperDtos.ManualCreate req) {
        return R.ok(paperService.manual(req));
    }

    @PostMapping("/smart")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Long> smart(@Valid @RequestBody PaperDtos.SmartCreate req) {
        return R.ok(paperService.smart(req));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> status(@PathVariable Long id, @RequestParam String status) {
        paperService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> delete(@PathVariable Long id) {
        paperService.delete(id);
        return R.ok();
    }
}
