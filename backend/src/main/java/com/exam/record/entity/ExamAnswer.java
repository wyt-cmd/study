package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exam_answer")
public class ExamAnswer {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long recordId;
    private Long questionId;
    private String userAnswer;
    private Integer score;
    private Integer correct;
    private String comment;
}
