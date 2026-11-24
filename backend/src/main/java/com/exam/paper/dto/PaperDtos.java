package com.exam.paper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public final class PaperDtos {
    private PaperDtos() {
    }

    @Data
    public static class ManualItem {
        @NotNull
        private Long questionId;
        @NotNull
        private Integer score;
    }

    @Data
    public static class RuleItem {
        @NotNull
        private Long categoryId;
        @NotBlank
        private String type;
        @NotNull
        private Integer count;
        @NotNull
        private Integer scoreEach;
    }

    @Data
    public static class ManualCreate {
        @NotBlank
        private String title;
        private String description;
        @NotNull
        private Integer durationMin;
        @NotNull
        private Integer totalScore;
        @NotNull
        private Integer passScore;
        @NotEmpty
        private List<ManualItem> questions;
    }

    @Data
    public static class SmartCreate {
        @NotBlank
        private String title;
        private String description;
        @NotNull
        private Integer durationMin;
        @NotNull
        private Integer passScore;
        @NotEmpty
        private List<RuleItem> rules;
    }

    @Data
    public static class PaperVO {
        private Long id;
        private String title;
        private String description;
        private Integer durationMin;
        private Integer totalScore;
        private Integer passScore;
        private String status;
        private String assembleMode;
        private LocalDateTime createTime;
        private List<QuestionItem> questions;
        private List<RuleItem> rules;
    }

    @Data
    public static class QuestionItem {
        private Long questionId;
        private Integer score;
        private Integer sortNo;
        private String stem;
        private String type;
        private String optionsJson;
        private String answer;
        private String analysis;
        private String keywords;
    }
}
