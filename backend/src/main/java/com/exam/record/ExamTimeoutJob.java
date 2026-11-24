package com.exam.record;

import com.exam.record.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExamTimeoutJob {

    private final ExamService examService;

    @Scheduled(fixedDelay = 30_000)
    public void closeExpired() {
        examService.sweepTimeouts();
    }
}
