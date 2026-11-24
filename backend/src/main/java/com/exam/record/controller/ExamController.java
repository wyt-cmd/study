package com.exam.record.controller;

import com.exam.common.R;
import com.exam.record.dto.ExamDtos;
import com.exam.record.service.ExamService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "在线考试")
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping("/start")
    public R<ExamDtos.SessionView> start(@Valid @RequestBody ExamDtos.Start req) {
        return R.ok(examService.start(req.getPaperId()));
    }

    @GetMapping("/{id}")
    public R<ExamDtos.SessionView> detail(@PathVariable Long id) {
        return R.ok(examService.detail(id));
    }

    @PutMapping("/{id}/answers")
    public R<Void> save(@PathVariable Long id, @RequestBody ExamDtos.SaveAnswers req) {
        examService.saveAnswers(id, req.getAnswers());
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    public R<ExamDtos.SessionView> submit(@PathVariable Long id) {
        return R.ok(examService.submit(id));
    }
}
