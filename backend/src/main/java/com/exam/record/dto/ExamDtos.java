package com.exam.record.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ExamDtos {
    private ExamDtos() {
    }

    @Data
    public static class Start {
        @NotNull(message = "试卷不能为空")
        private Long paperId;
    }

    @Data
    public static class AnswerItem {
        @NotNull(message = "题目不能为空")
        private Long questionId;
        private String userAnswer;
    }

    @Data
    public static class SaveAnswers {
        private List<AnswerItem> answers = new ArrayList<>();
    }

    @Data
    public static class QuestionView {
        private Long questionId;
        private Integer sortNo;
        private Integer score;
        private String type;
        private String stem;
        private String optionsJson;
        private String userAnswer;
        private String standardAnswer;
        private String analysis;
        private Integer gotScore;
        private Integer correct;
        private String comment;
    }

    @Data
    public static class SessionView {
        private Long recordId;
        private Long paperId;
        private String paperTitle;
        private Integer durationMin;
        private Integer totalScore;
        private Integer passScore;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime deadline;
        private LocalDateTime submitTime;
        private Integer score;
        private Integer objectiveScore;
        private Integer subjectiveScore;
        private String aiComment;
        private boolean showAnswer;
        private List<QuestionView> questions = new ArrayList<>();
    }
}
