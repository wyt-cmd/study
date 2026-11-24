package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_record")
public class ExamRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long paperId;
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime deadline;
    private LocalDateTime submitTime;
    private String status;
    private Integer score;
    private Integer objectiveScore;
    private Integer subjectiveScore;
    private String aiComment;
}
