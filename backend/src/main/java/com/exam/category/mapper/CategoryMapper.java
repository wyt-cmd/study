package com.exam.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.category.entity.QuestionCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CategoryMapper extends BaseMapper<QuestionCategory> {

    @Select("""
            SELECT c.id, c.parent_id AS parentId, c.name, c.sort_no AS sortNo, COUNT(q.id) AS questionCount
            FROM question_category c
            LEFT JOIN question q ON q.category_id = c.id AND q.deleted = 0
            WHERE c.deleted = 0
            GROUP BY c.id, c.parent_id, c.name, c.sort_no
            ORDER BY c.sort_no, c.id
            """)
    List<Map<String, Object>> listWithCount();
}
