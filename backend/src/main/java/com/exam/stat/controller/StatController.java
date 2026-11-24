package com.exam.stat.controller;

import com.exam.common.R;
import com.exam.stat.service.StatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "成绩统计")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping("/mine")
    public R<?> mine(@RequestParam(defaultValue = "1") long current,
                     @RequestParam(defaultValue = "10") long size) {
        return R.ok(statService.records(current, size, null, true));
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<?> records(@RequestParam(defaultValue = "1") long current,
                        @RequestParam(defaultValue = "10") long size,
                        @RequestParam(required = false) Long paperId) {
        return R.ok(statService.records(current, size, paperId, false));
    }

    @GetMapping("/aggregate")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<?> aggregate() {
        return R.ok(statService.aggregate());
    }

    @GetMapping("/rank")
    public R<?> rank(@RequestParam Long paperId) {
        return R.ok(statService.rank(paperId));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Long paperId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=scores.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(statService.export(paperId));
    }
}
