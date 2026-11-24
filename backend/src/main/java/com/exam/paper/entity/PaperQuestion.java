package com.exam.paper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("paper_question")
public class PaperQuestion {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long paperId;
    private Long questionId;
    private Integer score;
    private Integer sortNo;
}
