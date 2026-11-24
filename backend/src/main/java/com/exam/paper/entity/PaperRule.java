package com.exam.paper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("paper_rule")
public class PaperRule {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long paperId;
    private Long categoryId;
    private String type;
    private Integer questionCount;
    private Integer scoreEach;
}
