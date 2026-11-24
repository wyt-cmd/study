package com.exam.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.practice.entity.PracticeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface PracticeMapper extends BaseMapper<PracticeRecord> {

    @Select("""
            SELECT COUNT(*) AS total, COALESCE(SUM(correct), 0) AS hit
            FROM practice_record
            WHERE user_id = #{userId}
            """)
    Map<String, Object> summary(Long userId);
}
