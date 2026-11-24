package com.exam.question.controller;

import com.exam.common.R;
import com.exam.question.dto.QuestionDtos;
import com.exam.question.service.QuestionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "题目管理")
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public R<?> page(@RequestParam(defaultValue = "1") long current,
                     @RequestParam(defaultValue = "10") long size,
                     @RequestParam(required = false) Long categoryId,
                     @RequestParam(required = false) String type,
                     @RequestParam(required = false) Integer difficulty,
                     @RequestParam(required = false) String keyword) {
        return R.ok(questionService.page(current, size, categoryId, type, difficulty, keyword));
    }

    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(questionService.detail(id, true));
    }

    @GetMapping("/hot")
    @PreAuthorize("isAuthenticated()")
    public R<?> hot() {
        return R.ok(questionService.hot());
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody QuestionDtos.Save req) {
        return R.ok(questionService.save(null, req));
    }

    @PutMapping("/{id}")
    public R<Long> update(@PathVariable Long id, @Valid @RequestBody QuestionDtos.Save req) {
        return R.ok(questionService.save(id, req));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return R.ok();
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=questions.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(questionService.template());
    }

    @PostMapping("/import/preview")
    public R<?> preview(@RequestParam("file") MultipartFile file) {
        return R.ok(questionService.preview(file));
    }

    @PostMapping("/import")
    public R<?> importExcel(@RequestParam("file") MultipartFile file) {
        return R.ok(questionService.importExcel(file));
    }

    @PostMapping("/ai-draft")
    public R<?> aiDraft(@Valid @RequestBody QuestionDtos.AiRequest req) {
        return R.ok(questionService.aiDraft(req));
    }
}
