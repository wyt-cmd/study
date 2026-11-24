package com.exam.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.question.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    IPage<Question> pageQuery(Page<Question> page, @Param("categoryId") Long categoryId,
                              @Param("type") String type, @Param("difficulty") Integer difficulty,
                              @Param("keyword") String keyword);

    @Select("""
            SELECT id FROM question
            WHERE deleted = 0 AND category_id = #{categoryId} AND type = #{type}
            """)
    List<Long> listIds(@Param("categoryId") Long categoryId, @Param("type") String type);

    @Update("UPDATE question SET stem_hash = CONCAT(stem_hash, '#', id), deleted = 1 WHERE id = #{id} AND deleted = 0")
    int logicDeleteFreeHash(@Param("id") Long id);

    @Select("""
            SELECT q.id, q.stem, q.type, q.difficulty, COUNT(p.id) AS hot
            FROM question q
            LEFT JOIN practice_record p ON p.question_id = q.id
            WHERE q.deleted = 0
            GROUP BY q.id, q.stem, q.type, q.difficulty
            ORDER BY hot DESC, q.id DESC
            LIMIT #{limit}
            """)
    List<java.util.Map<String, Object>> hot(@Param("limit") int limit);

    @Select("""
            SELECT * FROM question
            WHERE deleted = 0
              AND (#{categoryId} IS NULL OR category_id = #{categoryId})
              AND (#{type} IS NULL OR #{type} = '' OR type = #{type})
            ORDER BY RAND()
            LIMIT 1
            """)
    Question randomOne(@Param("categoryId") Long categoryId, @Param("type") String type);
}
