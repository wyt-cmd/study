package com.exam.practice.controller;

import com.exam.common.R;
import com.exam.practice.service.PracticeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "智能刷题")
@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/next")
    public R<?> next(@RequestParam(required = false) Long categoryId,
                     @RequestParam(required = false) String type) {
        return R.ok(practiceService.next(categoryId, type));
    }

    @PostMapping("/submit")
    public R<?> submit(@Valid @RequestBody SubmitReq req) {
        return R.ok(practiceService.submit(req.getQuestionId(), req.getUserAnswer()));
    }

    @GetMapping("/summary")
    public R<?> summary() {
        return R.ok(practiceService.summary());
    }

    @Data
    public static class SubmitReq {
        @NotNull(message = "题目不能为空")
        private Long questionId;
        private String userAnswer;
    }
}
