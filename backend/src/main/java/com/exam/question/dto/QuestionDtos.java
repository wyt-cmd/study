package com.exam.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class QuestionDtos {
    private QuestionDtos() {
    }

    @Data
    public static class Option {
        private String key;
        private String text;
    }

    @Data
    public static class Save {
        @NotNull(message = "分类不能为空")
        private Long categoryId;
        @NotBlank(message = "题型不能为空")
        private String type;
        @NotNull(message = "难度不能为空")
        private Integer difficulty;
        @NotBlank(message = "题干不能为空")
        private String stem;
        private List<Option> options = new ArrayList<>();
        @NotBlank(message = "答案不能为空")
        private String answer;
        private String analysis;
        private String keywords;
        @NotNull(message = "分值不能为空")
        private Integer score;
        private String imageUrl;
    }

    @Data
    public static class VO {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String type;
        private Integer difficulty;
        private String stem;
        private List<Option> options;
        private String answer;
        private String analysis;
        private String keywords;
        private Integer score;
        private String imageUrl;
        private LocalDateTime createTime;
    }

    @Data
    public static class ImportResult {
        private int inserted;
        private long elapsedMs;
        private List<String> errors = new ArrayList<>();
    }

    @Data
    public static class AiRequest {
        @NotNull(message = "分类不能为空")
        private Long categoryId;
        @NotBlank(message = "题型不能为空")
        private String type;
        @NotNull(message = "数量不能为空")
        private Integer count;
        @NotBlank(message = "主题不能为空")
        private String topic;
        private Integer difficulty = 2;
        private Integer score = 5;
    }
}
